package com.hollow.fishsso.service;

import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.exception.SsoException;
import com.hollow.fishsso.model.LoginBlockEvent;
import com.hollow.fishsso.repository.jpa.LoginBlockEventJpaRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 登录保护服务：按账号、IP、账号+IP 三个维度统计失败次数并触发临时封禁。
 */
@Service
public class LoginProtectionService {

    private static final Logger log = LoggerFactory.getLogger(LoginProtectionService.class);

    private static final String FAIL_ACCOUNT_PREFIX = "sso:login:fail:account:";
    private static final String FAIL_IP_PREFIX = "sso:login:fail:ip:";
    private static final String FAIL_ACCOUNT_IP_PREFIX = "sso:login:fail:account_ip:";

    private static final String BLOCK_ACCOUNT_PREFIX = "sso:login:block:account:";
    private static final String BLOCK_IP_PREFIX = "sso:login:block:ip:";
    private static final String BLOCK_ACCOUNT_IP_PREFIX = "sso:login:block:account_ip:";

    private static final String EMPTY_ACCOUNT = "empty-account";
    private static final String UNKNOWN_IP = "unknown-ip";
    private static final String LOCKED_ERROR = "too_many_login_attempts";
    private static final String LOCKED_MESSAGE = "登录失败次数过多，请稍后再试";
    private static final int MAX_USER_AGENT_LOG_LENGTH = 256;

    private final StringRedisTemplate redisTemplate;
    private final SsoProperties ssoProperties;
    private final LoginBlockEventJpaRepository loginBlockEventJpaRepository;

    public LoginProtectionService(StringRedisTemplate redisTemplate,
                                  SsoProperties ssoProperties,
                                  LoginBlockEventJpaRepository loginBlockEventJpaRepository) {
        this.redisTemplate = redisTemplate;
        this.ssoProperties = ssoProperties;
        this.loginBlockEventJpaRepository = loginBlockEventJpaRepository;
    }

    /**
     * 登录前检查是否已被封禁。
     */
    public void assertLoginAllowed(String username, String sourceIp, String userAgent) {
        LoginFingerprint fingerprint = fingerprint(username, sourceIp, userAgent);

        checkBlocked("account", blockAccountKey(fingerprint.accountKeyPart()), fingerprint);
        checkBlocked("ip", blockIpKey(fingerprint.ipKeyPart()), fingerprint);
        checkBlocked("account_ip", blockAccountIpKey(fingerprint.accountKeyPart(), fingerprint.ipKeyPart()), fingerprint);
    }

    /**
     * 记录登录失败并在达到阈值时触发封禁。
     */
    public void recordFailure(String username, String sourceIp, String userAgent, String reason) {
        LoginFingerprint fingerprint = fingerprint(username, sourceIp, userAgent);

        SsoProperties.LoginProtection rules = ssoProperties.getLoginProtection();

        long accountFailures = incrementFailures(
                failAccountKey(fingerprint.accountKeyPart()),
                rules.getAccount().getWindow()
        );
        long ipFailures = incrementFailures(
                failIpKey(fingerprint.ipKeyPart()),
                rules.getIp().getWindow()
        );
        long accountIpFailures = incrementFailures(
                failAccountIpKey(fingerprint.accountKeyPart(), fingerprint.ipKeyPart()),
                rules.getAccountIp().getWindow()
        );

        boolean accountBlocked = applyBlockIfThresholdReached(
                "account",
                blockAccountKey(fingerprint.accountKeyPart()),
                accountFailures,
                rules.getAccount(),
                fingerprint
        );
        boolean ipBlocked = applyBlockIfThresholdReached(
                "ip",
                blockIpKey(fingerprint.ipKeyPart()),
                ipFailures,
                rules.getIp(),
                fingerprint
        );
        boolean accountIpBlocked = applyBlockIfThresholdReached(
                "account_ip",
                blockAccountIpKey(fingerprint.accountKeyPart(), fingerprint.ipKeyPart()),
                accountIpFailures,
                rules.getAccountIp(),
                fingerprint
        );

        log.warn(
                "login_failed reason={} account={} ip={} userAgent={} failures[account={},ip={},accountIp={}] blockedNow[account={},ip={},accountIp={}]",
                reason,
                fingerprint.accountForLog(),
                fingerprint.ipForLog(),
                fingerprint.userAgentForLog(),
                accountFailures,
                ipFailures,
                accountIpFailures,
                accountBlocked,
                ipBlocked,
                accountIpBlocked
        );
    }

    /**
     * 记录登录成功，清理账号相关失败计数。
     */
    public void recordSuccess(String username, String sourceIp) {
        LoginFingerprint fingerprint = fingerprint(username, sourceIp, null);
        List<String> keysToDelete = new ArrayList<>();
        keysToDelete.add(failAccountKey(fingerprint.accountKeyPart()));
        keysToDelete.add(failAccountIpKey(fingerprint.accountKeyPart(), fingerprint.ipKeyPart()));
        redisTemplate.delete(keysToDelete);
    }

    /**
     * 检查指定维度是否处于封禁状态。
     */
    private void checkBlocked(String dimension, String blockKey, LoginFingerprint fingerprint) {
        if (!StringUtils.hasText(redisTemplate.opsForValue().get(blockKey))) {
            return;
        }
        long ttlSeconds = getTtlSeconds(blockKey);
        log.warn(
                "login_blocked dimension={} account={} ip={} userAgent={} ttlSeconds={}",
                dimension,
                fingerprint.accountForLog(),
                fingerprint.ipForLog(),
                fingerprint.userAgentForLog(),
                ttlSeconds
        );
        throw new SsoException(HttpStatus.TOO_MANY_REQUESTS, LOCKED_ERROR, LOCKED_MESSAGE);
    }

    /**
     * 失败次数 +1，并设置统计窗口过期时间。
     */
    private long incrementFailures(String key, Duration window) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (Long.valueOf(1L).equals(value)) {
            redisTemplate.expire(key, window);
        }
        return value == null ? 0L : value;
    }

    /**
     * 达到阈值时尝试加锁并记录封禁事件。
     */
    private boolean applyBlockIfThresholdReached(String dimension,
                                                 String blockKey,
                                                 long failures,
                                                 SsoProperties.LoginRule rule,
                                                 LoginFingerprint fingerprint) {
        if (failures < rule.getMaxFailures()) {
            return false;
        }
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(blockKey, "1", rule.getBlockDuration());
        if (Boolean.TRUE.equals(locked)) {
            persistLoginBlockEvent(dimension, fingerprint, failures, rule);
            return true;
        }
        return false;
    }

    /**
     * 持久化封禁事件，失败时仅记录告警日志，不中断主流程。
     */
    private void persistLoginBlockEvent(String dimension,
                                        LoginFingerprint fingerprint,
                                        long failures,
                                        SsoProperties.LoginRule rule) {
        try {
            loginBlockEventJpaRepository.save(LoginBlockEvent.of(
                    dimension,
                    fingerprint.accountForLog(),
                    fingerprint.ipForLog(),
                    fingerprint.userAgentForLog(),
                    failures,
                    rule.getMaxFailures(),
                    rule.getWindow(),
                    rule.getBlockDuration()
            ));
        } catch (Exception ex) {
            log.error(
                    "SECURITY_ALERT login_block_triggered_persist_failed dimension={} account={} ip={} userAgent={} failures={} threshold={} window={} blockDuration={}",
                    dimension,
                    fingerprint.accountForLog(),
                    fingerprint.ipForLog(),
                    fingerprint.userAgentForLog(),
                    failures,
                    rule.getMaxFailures(),
                    rule.getWindow(),
                    rule.getBlockDuration(),
                    ex
            );
        }
    }

    private long getTtlSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? 0L : ttl;
    }

    private String failAccountKey(String accountKeyPart) {
        return FAIL_ACCOUNT_PREFIX + accountKeyPart;
    }

    private String failIpKey(String ipKeyPart) {
        return FAIL_IP_PREFIX + ipKeyPart;
    }

    private String failAccountIpKey(String accountKeyPart, String ipKeyPart) {
        return FAIL_ACCOUNT_IP_PREFIX + accountKeyPart + ":" + ipKeyPart;
    }

    private String blockAccountKey(String accountKeyPart) {
        return BLOCK_ACCOUNT_PREFIX + accountKeyPart;
    }

    private String blockIpKey(String ipKeyPart) {
        return BLOCK_IP_PREFIX + ipKeyPart;
    }

    private String blockAccountIpKey(String accountKeyPart, String ipKeyPart) {
        return BLOCK_ACCOUNT_IP_PREFIX + accountKeyPart + ":" + ipKeyPart;
    }

    /**
     * 生成用于 Redis 键和日志输出的登录指纹。
     */
    private LoginFingerprint fingerprint(String username, String sourceIp, String userAgent) {
        String normalizedAccount = normalizeAccount(username);
        String normalizedIp = normalizeIp(sourceIp);
        return new LoginFingerprint(
                encodeForRedisKey(normalizedAccount),
                encodeForRedisKey(normalizedIp),
                normalizedAccount,
                normalizedIp,
                normalizeUserAgent(userAgent)
        );
    }

    private String normalizeAccount(String username) {
        if (!StringUtils.hasText(username)) {
            return EMPTY_ACCOUNT;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String sourceIp) {
        if (!StringUtils.hasText(sourceIp)) {
            return UNKNOWN_IP;
        }
        return sourceIp.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "-";
        }
        String trimmed = userAgent.trim();
        if (trimmed.length() <= MAX_USER_AGENT_LOG_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_USER_AGENT_LOG_LENGTH);
    }

    private String encodeForRedisKey(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record LoginFingerprint(String accountKeyPart,
                                    String ipKeyPart,
                                    String accountForLog,
                                    String ipForLog,
                                    String userAgentForLog) {
    }
}
