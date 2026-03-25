package com.hollow.fishsso.service;

import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.exception.SsoException;
import com.hollow.fishsso.model.AccessToken;
import com.hollow.fishsso.model.AuthCode;
import com.hollow.fishsso.model.ClientRegistration;
import com.hollow.fishsso.model.ConsentGrant;
import com.hollow.fishsso.model.SessionInfo;
import com.hollow.fishsso.model.UserAccount;
import com.hollow.fishsso.repository.AuthCodeStore;
import com.hollow.fishsso.repository.ClientRepository;
import com.hollow.fishsso.repository.ConsentStore;
import com.hollow.fishsso.repository.SessionStore;
import com.hollow.fishsso.repository.TokenStore;
import com.hollow.fishsso.repository.UserRepository;
import com.hollow.fishsso.service.dto.AuthorizationContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * SSO核心服务
 */
@Service
public class SsoService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final SessionStore sessionStore;
    private final AuthCodeStore authCodeStore;
    private final TokenStore tokenStore;
    private final ConsentStore consentStore;
    private final PasswordEncoder passwordEncoder;
    private final SsoProperties properties;
    private final LoginProtectionService loginProtectionService;

    /**
     * 构造函数
     * @param userRepository 用户仓储
     * @param clientRepository 客户端仓储
     * @param sessionStore 会话存储
     * @param authCodeStore 授权码存储
     * @param tokenStore 令牌存储
     * @param consentStore 授权同意记录存储
     * @param passwordEncoder 密码编码器
     * @param properties SSO配置属性
     * @param loginProtectionService 登录防护服务
     */
    public SsoService(UserRepository userRepository,
                      ClientRepository clientRepository,
                      SessionStore sessionStore,
                      AuthCodeStore authCodeStore,
                      TokenStore tokenStore,
                      ConsentStore consentStore,
                      PasswordEncoder passwordEncoder,
                      SsoProperties properties,
                      LoginProtectionService loginProtectionService) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.sessionStore = sessionStore;
        this.authCodeStore = authCodeStore;
        this.tokenStore = tokenStore;
        this.consentStore = consentStore;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.loginProtectionService = loginProtectionService;
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @param sourceIp 客户端IP
     * @param userAgent 客户端UA
     * @return 会话信息
     */
    public SessionInfo login(String username, String password, String sourceIp, String userAgent) {
        loginProtectionService.assertLoginAllowed(username, sourceIp, userAgent);

        UserAccount user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !StringUtils.hasText(password) || !passwordEncoder.matches(password, user.getPasswordHash())) {
            loginProtectionService.recordFailure(username, sourceIp, userAgent, "invalid_credentials");
            throw invalidCredentials();
        }

        loginProtectionService.recordSuccess(username, sourceIp);
        return sessionStore.create(user.getId(), properties.getSessionTtl());
    }

    /**
     * 验证授权请求参数
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     */
    public void validateAuthorizationRequest(String clientId, String redirectUri, String scope) {
        ClientRegistration client = requireClient(clientId);
        validateRedirectUri(client, redirectUri);
        resolveScopes(scope, client);
    }

    /**
     * 构建授权同意上下文
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param sessionId 会话ID
     * @return 授权上下文
     */
    public AuthorizationContext buildConsentContext(String clientId, String redirectUri, String scope, String sessionId) {
        SessionInfo session = requireSession(sessionId);
        ClientRegistration client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new SsoException(HttpStatus.BAD_REQUEST, "invalid_client", "客户端未注册"));
        List<String> scopes = resolveScopes(scope, client);
        UserAccount user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new SsoException(HttpStatus.UNAUTHORIZED, "login_required", "用户不存在"));
        return new AuthorizationContext(
                clientId,
                redirectUri,
                scopes,
                user.getId(),
                user.getUsername(),
                user.getDisplayName()
        );
    }

    /**
     * 批准授权
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param sessionId 会话ID
     * @return 授权码
     */
    public AuthCode approveAuthorization(String clientId, String redirectUri, String scope, String sessionId) {
        ClientRegistration client = requireClient(clientId);
        validateRedirectUri(client, redirectUri);
        SessionInfo session = requireSession(sessionId);
        List<String> scopes = resolveScopes(scope, client);
        recordConsent(session.getUserId(), client.getClientId(), scopes);
        return authCodeStore.create(client.getClientId(), session.getUserId(), redirectUri, scopes, properties.getAuthCodeTtl());
    }

    /**
     * 尝试基于已同意记录自动批准授权
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param sessionId 会话ID
     * @return 授权码（可选）
     */
    public Optional<AuthCode> tryAutoApproveAuthorization(String clientId,
                                                          String redirectUri,
                                                          String scope,
                                                          String sessionId) {
        ClientRegistration client = requireClient(clientId);
        validateRedirectUri(client, redirectUri);
        List<String> requestedScopes = resolveScopes(scope, client);

        Optional<SessionInfo> sessionOptional = findValidSession(sessionId);
        if (sessionOptional.isEmpty()) {
            return Optional.empty();
        }
        SessionInfo session = sessionOptional.get();

        Optional<ConsentGrant> consentOptional = consentStore.find(session.getUserId(), clientId);
        if (consentOptional.isEmpty()) {
            return Optional.empty();
        }

        if (!hasGrantedAllScopes(consentOptional.get().getScopes(), requestedScopes)) {
            return Optional.empty();
        }

        AuthCode authCode = authCodeStore.create(
                clientId,
                session.getUserId(),
                redirectUri,
                requestedScopes,
                properties.getAuthCodeTtl()
        );
        return Optional.of(authCode);
    }

    /**
     * 授权码换取访问令牌
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @param code 授权码
     * @param redirectUri 重定向URI
     * @return 访问令牌
     */
    public AccessToken exchangeCode(String clientId, String clientSecret, String code, String redirectUri) {
        ClientRegistration client = requireClient(clientId);
        if (!passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
            throw new SsoException(HttpStatus.UNAUTHORIZED, "invalid_client", "客户端认证失败");
        }
        AuthCode authCode = authCodeStore.consume(code)
                .orElseThrow(() -> new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "授权码无效"));
        if (authCode.isExpired(Instant.now())) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "授权码已过期");
        }
        if (!authCode.getClientId().equals(clientId)) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "授权码与客户端不匹配");
        }
        if (!authCode.getRedirectUri().equals(redirectUri)) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "回调地址不匹配");
        }
        return tokenStore.create(clientId, authCode.getUserId(), authCode.getScopes(), properties.getAccessTokenTtl());
    }

    /**
     * 获取用户信息
     * @param accessToken 访问令牌
     * @return 用户账户
     */
    public UserAccount userInfo(String accessToken) {
        AccessToken token = tokenStore.find(accessToken)
                .orElseThrow(() -> new SsoException(HttpStatus.UNAUTHORIZED, "invalid_token", "访问令牌不存在"));
        if (token.isExpired(Instant.now())) {
            throw new SsoException(HttpStatus.UNAUTHORIZED, "invalid_token", "访问令牌已过期");
        }
        return userRepository.findById(token.getUserId())
                .orElseThrow(() -> new SsoException(HttpStatus.UNAUTHORIZED, "invalid_token", "用户不存在"));
    }

    /**
     * 获取客户端（必须存在）
     * @param clientId 客户端ID
     * @return 客户端注册信息
     */
    private ClientRegistration requireClient(String clientId) {
        return clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new SsoException(HttpStatus.BAD_REQUEST, "invalid_client", "客户端未注册"));
    }

    /**
     * 获取会话（必须存在且有效）
     * @param sessionId 会话ID
     * @return 会话信息
     */
    private SessionInfo requireSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new SsoException(HttpStatus.UNAUTHORIZED, "login_required", "需要先登录");
        }
        SessionInfo session = sessionStore.find(sessionId)
                .orElseThrow(() -> new SsoException(HttpStatus.UNAUTHORIZED, "login_required", "需要先登录"));
        if (session.isExpired(Instant.now())) {
            sessionStore.delete(sessionId);
            throw new SsoException(HttpStatus.UNAUTHORIZED, "login_required", "会话已过期");
        }
        return session;
    }

    /**
     * 解析授权范围
     * @param scope 授权范围字符串
     * @param client 客户端注册信息
     * @return 授权范围列表
     */
    private List<String> resolveScopes(String scope, ClientRegistration client) {
        List<String> allowed = client.getScopes();
        if (!StringUtils.hasText(scope)) {
            return allowed;
        }
        List<String> requested = Arrays.stream(scope.split(" "))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        for (String item : requested) {
            String normalized = item.toLowerCase(Locale.ROOT);
            boolean supported = allowed.stream().anyMatch(scopeItem -> scopeItem.equalsIgnoreCase(normalized));
            if (!supported) {
                throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_scope", "不允许的权限范围: " + item);
            }
        }
        return requested;
    }

    /**
     * 验证重定向URI
     * @param client 客户端注册信息
     * @param redirectUri 重定向URI
     */
    private void validateRedirectUri(ClientRegistration client, String redirectUri) {
        if (!client.getRedirectUris().contains(redirectUri)) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_redirect_uri", "回调地址未注册");
        }
    }

    /**
     * 查找有效会话
     * @param sessionId 会话ID
     * @return 会话信息（可选）
     */
    private Optional<SessionInfo> findValidSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Optional.empty();
        }
        Optional<SessionInfo> sessionOptional = sessionStore.find(sessionId);
        if (sessionOptional.isEmpty()) {
            return Optional.empty();
        }
        SessionInfo session = sessionOptional.get();
        if (session.isExpired(Instant.now())) {
            sessionStore.delete(sessionId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    /**
     * 记录并合并用户授权同意范围
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @param requestedScopes 本次同意范围
     */
    private void recordConsent(String userId, String clientId, List<String> requestedScopes) {
        List<String> existingScopes = consentStore.find(userId, clientId)
                .map(ConsentGrant::getScopes)
                .orElseGet(List::of);
        List<String> mergedScopes = mergeScopes(existingScopes, requestedScopes);
        consentStore.save(userId, clientId, mergedScopes);
    }

    /**
     * 判断已同意范围是否覆盖请求范围
     * @param grantedScopes 已同意范围
     * @param requestedScopes 请求范围
     * @return 是否完全覆盖
     */
    private boolean hasGrantedAllScopes(List<String> grantedScopes, List<String> requestedScopes) {
        Set<String> grantedScopeSet = grantedScopes.stream()
                .map(this::normalizeScope)
                .collect(Collectors.toSet());
        return requestedScopes.stream()
                .map(this::normalizeScope)
                .allMatch(grantedScopeSet::contains);
    }

    /**
     * 合并范围并去重
     * @param existingScopes 已有范围
     * @param requestedScopes 请求范围
     * @return 合并后的范围
     */
    private List<String> mergeScopes(List<String> existingScopes, List<String> requestedScopes) {
        Set<String> merged = new LinkedHashSet<>();
        existingScopes.stream()
                .map(this::normalizeScope)
                .forEach(merged::add);
        requestedScopes.stream()
                .map(this::normalizeScope)
                .forEach(merged::add);
        return new ArrayList<>(merged);
    }

    /**
     * 标准化scope
     * @param scope 原始scope
     * @return 标准化scope
     */
    private String normalizeScope(String scope) {
        return scope == null ? "" : scope.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 构建统一的凭证错误异常
     * @return 认证异常
     */
    private SsoException invalidCredentials() {
        return new SsoException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "用户名或密码错误");
    }
}
