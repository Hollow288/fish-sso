package com.hollow.fishsso.service;

import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.exception.SsoException;
import com.hollow.fishsso.model.PasswordResetCode;
import com.hollow.fishsso.model.UserAccount;
import com.hollow.fishsso.repository.RefreshTokenStore;
import com.hollow.fishsso.repository.SessionStore;
import com.hollow.fishsso.repository.TokenStore;
import com.hollow.fishsso.repository.UserRepository;
import com.hollow.fishsso.repository.redis.PasswordResetCodeRedisRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 密码重置服务
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final SessionStore sessionStore;
    private final TokenStore tokenStore;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordResetCodeRedisRepository resetCodeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SsoProperties properties;

    /**
     * 构造函数
     * @param userRepository 用户仓储
     * @param resetCodeRepository 验证码仓储
     * @param emailService 邮件服务
     * @param passwordEncoder 密码编码器
     * @param properties SSO配置属性
     */
    public PasswordResetService(UserRepository userRepository,
                                SessionStore sessionStore,
                                TokenStore tokenStore,
                                RefreshTokenStore refreshTokenStore,
                                PasswordResetCodeRedisRepository resetCodeRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder,
                                SsoProperties properties) {
        this.userRepository = userRepository;
        this.sessionStore = sessionStore;
        this.tokenStore = tokenStore;
        this.refreshTokenStore = refreshTokenStore;
        this.resetCodeRepository = resetCodeRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    /**
     * 发送密码重置验证码
     * @param username 用户名
     * @param email 邮箱
     */
    public void sendResetCode(String username, String email) {
        if (username == null || username.isBlank() || email == null || email.isBlank()) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_request", "用户名和邮箱不能为空");
        }

        // 检查发送频率
        Optional<PasswordResetCode> existing = resetCodeRepository.findById(username);
        if (existing.isPresent()) {
            Duration sendInterval = getPasswordResetConfig().getSendInterval();
            Instant earliestNextSend = existing.get().getCreatedAt().plus(sendInterval);
            if (Instant.now().isBefore(earliestNextSend)) {
                throw new SsoException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited", "发送过于频繁，请稍后再试");
            }
        }

        // 查找用户并校验邮箱（无论是否匹配都返回相同响应，防止用户枚举）
        Optional<UserAccount> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !email.equalsIgnoreCase(userOpt.get().getEmail())) {
            log.warn("密码重置请求用户名或邮箱不匹配: username={}", username);
            return;
        }

        UserAccount user = userOpt.get();
        String code = generateCode();
        Duration codeTtl = getPasswordResetConfig().getCodeTtl();

        PasswordResetCode resetCode = new PasswordResetCode(
                username,
                code,
                user.getEmail(),
                Instant.now(),
                codeTtl.getSeconds()
        );
        resetCodeRepository.save(resetCode);

        emailService.sendResetCode(user.getEmail(), code);
        log.info("已为用户 {} 发送密码重置验证码", username);
    }

    /**
     * 重置密码
     * @param username 用户名
     * @param newPassword 新密码
     * @param code 验证码
     */
    public void resetPassword(String username, String newPassword, String code) {
        if (username == null || username.isBlank()
                || newPassword == null || newPassword.isBlank()
                || code == null || code.isBlank()) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_request", "用户名、新密码和验证码不能为空");
        }

        PasswordResetCode resetCode = resetCodeRepository.findById(username)
                .orElseThrow(() -> new SsoException(HttpStatus.BAD_REQUEST, "invalid_code", "验证码无效或已过期"));

        // 检查失败次数
        int maxFailures = getPasswordResetConfig().getMaxVerifyFailures();
        if (resetCode.getFailureCount() >= maxFailures) {
            resetCodeRepository.deleteById(username);
            throw new SsoException(HttpStatus.BAD_REQUEST, "code_exhausted", "验证码错误次数过多，请重新获取");
        }

        // 校验验证码
        if (!resetCode.getCode().equals(code)) {
            resetCode.setFailureCount(resetCode.getFailureCount() + 1);
            if (resetCode.getFailureCount() >= maxFailures) {
                resetCodeRepository.deleteById(username);
                throw new SsoException(HttpStatus.BAD_REQUEST, "code_exhausted", "验证码错误次数过多，请重新获取");
            }
            resetCodeRepository.save(resetCode);
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_code",
                    "验证码错误，剩余尝试次数: " + (maxFailures - resetCode.getFailureCount()));
        }

        // 验证通过，更新密码
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new SsoException(HttpStatus.BAD_REQUEST, "invalid_request", "用户不存在"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        invalidateUserCredentials(user.getId());

        // 删除已使用的验证码
        resetCodeRepository.deleteById(username);
        log.info("用户 {} 密码重置成功", username);
    }

    /**
     * 生成6位数字验证码
     * @return 验证码
     */
    private String generateCode() {
        int code = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    /**
     * 获取密码重置配置，如未配置则使用默认值
     * @return 密码重置配置
     */
    private SsoProperties.PasswordReset getPasswordResetConfig() {
        SsoProperties.PasswordReset config = properties.getPasswordReset();
        if (config == null) {
            config = new SsoProperties.PasswordReset();
        }
        return config;
    }

    /**
     * 密码重置后立即失效用户现有登录态与令牌。
     * @param userId 用户ID
     */
    private void invalidateUserCredentials(String userId) {
        sessionStore.deleteByUserId(userId);
        tokenStore.deleteByUserId(userId);
        refreshTokenStore.deleteByUserId(userId);
    }
}
