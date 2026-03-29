package com.hollow.fishsso.service;

import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.exception.SsoException;
import com.hollow.fishsso.model.AuthCode;
import com.hollow.fishsso.model.ClientRegistration;
import com.hollow.fishsso.model.ConsentGrant;
import com.hollow.fishsso.model.AccessToken;
import com.hollow.fishsso.model.RefreshToken;
import com.hollow.fishsso.model.SessionInfo;
import com.hollow.fishsso.model.UserAccount;
import com.hollow.fishsso.repository.AuthCodeStore;
import com.hollow.fishsso.repository.ClientRepository;
import com.hollow.fishsso.repository.ConsentStore;
import com.hollow.fishsso.repository.RefreshTokenStore;
import com.hollow.fishsso.repository.SessionStore;
import com.hollow.fishsso.repository.TokenStore;
import com.hollow.fishsso.repository.UserRepository;
import com.hollow.fishsso.service.dto.AuthorizationContext;
import com.hollow.fishsso.service.dto.TokenSet;
import com.hollow.fishsso.service.dto.UserInfoView;
import com.nimbusds.jwt.JWTClaimsSet;
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
    private final RefreshTokenStore refreshTokenStore;
    private final ConsentStore consentStore;
    private final PasswordEncoder passwordEncoder;
    private final SsoProperties properties;
    private final LoginProtectionService loginProtectionService;
    private final JwtService jwtService;

    /**
     * 构造函数
     * @param userRepository 用户仓储
     * @param clientRepository 客户端仓储
     * @param sessionStore 会话存储
     * @param authCodeStore 授权码存储
     * @param tokenStore 访问令牌存储
     * @param refreshTokenStore 刷新令牌存储
     * @param consentStore 授权同意记录存储
     * @param passwordEncoder 密码编码器
     * @param properties SSO配置属性
     * @param loginProtectionService 登录保护服务
     * @param jwtService JWT服务
     */
    public SsoService(UserRepository userRepository,
                      ClientRepository clientRepository,
                      SessionStore sessionStore,
                      AuthCodeStore authCodeStore,
                      TokenStore tokenStore,
                      RefreshTokenStore refreshTokenStore,
                      ConsentStore consentStore,
                      PasswordEncoder passwordEncoder,
                      SsoProperties properties,
                      LoginProtectionService loginProtectionService,
                      JwtService jwtService) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.sessionStore = sessionStore;
        this.authCodeStore = authCodeStore;
        this.tokenStore = tokenStore;
        this.refreshTokenStore = refreshTokenStore;
        this.consentStore = consentStore;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.loginProtectionService = loginProtectionService;
        this.jwtService = jwtService;
    }

    /**
     * 用户登录，校验凭证并创建会话
     * @param username 用户名
     * @param password 密码
     * @param sourceIp 来源IP地址
     * @param userAgent 用户代理字符串
     * @return 会话信息
     * @throws SsoException 凭证无效时抛出
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
     * 验证授权请求参数（客户端、重定向URI、权限范围）
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @throws SsoException 参数无效时抛出
     */
    public void validateAuthorizationRequest(String clientId, String redirectUri, String scope) {
        ClientRegistration client = requireClient(clientId);
        validateRedirectUri(client, redirectUri);
        resolveScopes(scope, client);
    }

    /**
     * 构建授权同意上下文，包含客户端、用户和权限范围信息
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param sessionId 当前会话ID
     * @return 授权上下文
     * @throws SsoException 客户端未注册或未登录时抛出
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
     * 批准授权，记录同意并生成授权码
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param nonce OIDC nonce参数
     * @param sessionId 当前会话ID
     * @return 授权码
     */
    public AuthCode approveAuthorization(String clientId, String redirectUri, String scope, String nonce, String sessionId) {
        ClientRegistration client = requireClient(clientId);
        validateRedirectUri(client, redirectUri);
        SessionInfo session = requireSession(sessionId);
        List<String> scopes = resolveScopes(scope, client);
        recordConsent(session.getUserId(), client.getClientId(), scopes);
        return authCodeStore.create(client.getClientId(), session.getUserId(), redirectUri, scopes, nonce, properties.getAuthCodeTtl());
    }

    /**
     * 尝试基于已同意记录自动批准授权，若用户已同意所有请求的权限范围则直接生成授权码
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param nonce OIDC nonce参数
     * @param sessionId 当前会话ID
     * @return 授权码（若无法自动批准则为空）
     */
    public Optional<AuthCode> tryAutoApproveAuthorization(String clientId,
                                                          String redirectUri,
                                                          String scope,
                                                          String nonce,
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
                nonce,
                properties.getAuthCodeTtl()
        );
        return Optional.of(authCode);
    }

    /**
     * 授权码换取令牌集合（access_token JWT + id_token JWT + refresh_token）
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @param code 授权码
     * @param redirectUri 重定向URI（需与授权码中记录的一致）
     * @return 令牌集合
     * @throws SsoException 客户端认证失败、授权码无效或过期时抛出
     */
    public TokenSet exchangeCode(String clientId, String clientSecret, String code, String redirectUri) {
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

        UserAccount user = userRepository.findById(authCode.getUserId())
                .orElseThrow(() -> new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "用户不存在"));

        String accessToken = jwtService.generateAccessToken(user.getId(), clientId, authCode.getScopes());
        tokenStore.create(accessToken, clientId, user.getId(), authCode.getScopes(), properties.getAccessTokenTtl());
        String idToken = jwtService.generateIdToken(user.getId(), clientId, authCode.getScopes(), authCode.getNonce(), user);
        RefreshToken refreshToken = refreshTokenStore.create(clientId, user.getId(), authCode.getScopes(), properties.getRefreshTokenTtl());

        long expiresIn = properties.getAccessTokenTtl().getSeconds();
        String scope = String.join(" ", authCode.getScopes());

        return new TokenSet(accessToken, idToken, refreshToken.getToken(), "Bearer", expiresIn, scope);
    }

    /**
     * 刷新令牌换取新令牌集合（refresh token rotation，旧令牌作废）
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @param refreshTokenStr 刷新令牌
     * @return 新的令牌集合
     * @throws SsoException 客户端认证失败、刷新令牌无效或过期时抛出
     */
    public TokenSet refreshToken(String clientId, String clientSecret, String refreshTokenStr) {
        ClientRegistration client = requireClient(clientId);
        if (!passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
            throw new SsoException(HttpStatus.UNAUTHORIZED, "invalid_client", "客户端认证失败");
        }

        RefreshToken oldRefresh = refreshTokenStore.find(refreshTokenStr)
                .orElseThrow(() -> new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "刷新令牌无效"));
        if (oldRefresh.isExpired(Instant.now())) {
            refreshTokenStore.delete(refreshTokenStr);
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "刷新令牌已过期");
        }
        if (!oldRefresh.getClientId().equals(clientId)) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "刷新令牌与客户端不匹配");
        }

        // rotation: 删除旧的，创建新的
        refreshTokenStore.delete(refreshTokenStr);

        UserAccount user = userRepository.findById(oldRefresh.getUserId())
                .orElseThrow(() -> new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "用户不存在"));

        String accessToken = jwtService.generateAccessToken(user.getId(), clientId, oldRefresh.getScopes());
        tokenStore.create(accessToken, clientId, user.getId(), oldRefresh.getScopes(), properties.getAccessTokenTtl());
        String idToken = jwtService.generateIdToken(user.getId(), clientId, oldRefresh.getScopes(), null, user);
        RefreshToken newRefresh = refreshTokenStore.create(clientId, user.getId(), oldRefresh.getScopes(), properties.getRefreshTokenTtl());

        long expiresIn = properties.getAccessTokenTtl().getSeconds();
        String scope = String.join(" ", oldRefresh.getScopes());

        return new TokenSet(accessToken, idToken, newRefresh.getToken(), "Bearer", expiresIn, scope);
    }

    /**
     * 撤销令牌（refresh_token），遵循 RFC 7009，即使令牌不存在也不报错
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @param token 待撤销的令牌
     * @throws SsoException 客户端认证失败时抛出
     */
    public void revokeToken(String clientId, String clientSecret, String token) {
        ClientRegistration client = requireClient(clientId);
        if (!passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
            throw new SsoException(HttpStatus.UNAUTHORIZED, "invalid_client", "客户端认证失败");
        }
        refreshTokenStore.find(token)
                .filter(refreshToken -> refreshToken.getClientId().equals(clientId))
                .ifPresent(refreshToken -> refreshTokenStore.delete(token));
        tokenStore.find(token)
                .filter(accessToken -> accessToken.getClientId().equals(clientId))
                .ifPresent(accessToken -> tokenStore.delete(token));
    }

    /**
     * 登出：删除会话
     * @param sessionId 会话ID，为空时不做任何操作
     */
    public void logout(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            sessionStore.delete(sessionId);
        }
    }

    /**
     * 获取用户信息（通过 JWT access_token 自校验）
     * @param accessToken JWT访问令牌
     * @return 用户账户
     * @throws SsoException 令牌无效或用户不存在时抛出
     */
    public UserInfoView userInfo(String accessToken) {
        JWTClaimsSet claims = jwtService.parseAndVerify(accessToken);
        if (claims == null) {
            throw new SsoException(HttpStatus.UNAUTHORIZED, "invalid_token", "访��令牌无效或已过期");
        }
        if (!isAccessTokenActive(accessToken, claims)) {
            throw new SsoException(HttpStatus.UNAUTHORIZED, "invalid_token", "访问令牌无效或已撤销");
        }
        Set<String> scopes = parseScopes(claims.getClaim("scope"));
        if (!scopes.contains("openid")) {
            throw new SsoException(HttpStatus.FORBIDDEN, "insufficient_scope", "访问 userinfo 需要 openid 权限");
        }
        String userId = claims.getSubject();
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new SsoException(HttpStatus.UNAUTHORIZED, "invalid_token", "用户不存在"));
        boolean hasProfile = scopes.contains("profile");
        boolean hasEmail = scopes.contains("email");
        String username = hasProfile ? user.getUsername() : null;
        String name = hasProfile ? user.getDisplayName() : null;
        String email = hasEmail ? user.getEmail() : null;
        return new UserInfoView(user.getId(), username, name, email);
    }

    /**
     * 根据客户端ID查找客户端，不存在则抛异常
     * @param clientId 客户端ID
     * @return 客户端注册信息
     * @throws SsoException 客户端未注册时抛出
     */
    private ClientRegistration requireClient(String clientId) {
        return clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new SsoException(HttpStatus.BAD_REQUEST, "invalid_client", "客户端未注册"));
    }

    /**
     * 根据会话ID查找有效会话，不存在或已过期则抛异常
     * @param sessionId 会话ID
     * @return 会话信息
     * @throws SsoException 未登录或会话过期时抛出
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
     * 解析并校验授权范围，未指定时返回客户端允许的全部范围
     * @param scope 请求的授权范围（空格分隔）
     * @param client 客户端注册信息
     * @return 解析后的授权范围列表
     * @throws SsoException 包含不允许的权限范围时抛出
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
     * 校验重定向URI是否在客户端注册的白名单中
     * @param client 客户端注册信息
     * @param redirectUri 重定向URI
     * @throws SsoException 未注册的回调地址时抛出
     */
    private void validateRedirectUri(ClientRegistration client, String redirectUri) {
        if (!client.getRedirectUris().contains(redirectUri)) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_redirect_uri", "回调地址未注册");
        }
    }

    /**
     * 查找有效会话，过期会话将被删除
     * @param sessionId 会话ID
     * @return 有效会话（可选）
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
     * 记录用户授权同意，与已有同意范围合并
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @param requestedScopes 请求的授权范围
     */
    private void recordConsent(String userId, String clientId, List<String> requestedScopes) {
        List<String> existingScopes = consentStore.find(userId, clientId)
                .map(ConsentGrant::getScopes)
                .orElseGet(List::of);
        List<String> mergedScopes = mergeScopes(existingScopes, requestedScopes);
        consentStore.save(userId, clientId, mergedScopes);
    }

    /**
     * 判断已授权范围是否包含所有请求的范围
     * @param grantedScopes 已授权范围列表
     * @param requestedScopes 请求的范围列表
     * @return 是否全部包含
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
     * 合并已有和新请求的权限范围（去重）
     * @param existingScopes 已有权限范围
     * @param requestedScopes 请求的权限范围
     * @return 合并后的权限范围列表
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
     * 将权限范围字符串统一转为小写并去除空白
     * @param scope 权限范围字符串
     * @return 标准化后的权限范围
     */
    private String normalizeScope(String scope) {
        return scope == null ? "" : scope.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 判断访问令牌是否仍然处于活动状态（未被撤销且与声明一致）
     * @param accessToken 访问令牌
     * @param claims JWT声明
     * @return 是否有效
     */
    private boolean isAccessTokenActive(String accessToken, JWTClaimsSet claims) {
        Optional<AccessToken> tokenOptional = tokenStore.find(accessToken);
        if (tokenOptional.isEmpty()) {
            return false;
        }
        AccessToken storedToken = tokenOptional.get();
        if (storedToken.isExpired(Instant.now())) {
            tokenStore.delete(accessToken);
            return false;
        }
        if (!storedToken.getUserId().equals(claims.getSubject())) {
            return false;
        }
        List<String> audience = claims.getAudience();
        return audience != null && audience.contains(storedToken.getClientId());
    }

    /**
     * 解析访问令牌中的 scope 声明。
     * @param scopeClaim scope 声明原始值
     * @return 标准化后的 scope 集合
     */
    private Set<String> parseScopes(Object scopeClaim) {
        if (!(scopeClaim instanceof String scopeValue) || !StringUtils.hasText(scopeValue)) {
            return Set.of();
        }
        return Arrays.stream(scopeValue.split(" "))
                .map(this::normalizeScope)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    /**
     * 创建凭证无效的异常
     * @return 凭证无效异常
     */
    private SsoException invalidCredentials() {
        return new SsoException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "用户名或密码错误");
    }
}
