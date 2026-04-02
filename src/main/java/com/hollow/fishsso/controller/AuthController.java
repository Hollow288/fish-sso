package com.hollow.fishsso.controller;

import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.controller.dto.AuthorizedClientResponse;
import com.hollow.fishsso.controller.dto.LoginRequest;
import com.hollow.fishsso.controller.dto.LoginResponse;
import com.hollow.fishsso.controller.dto.TokenResponse;
import com.hollow.fishsso.controller.dto.UserInfoResponse;
import com.hollow.fishsso.exception.SsoException;
import com.hollow.fishsso.service.dto.AuthorizedClientView;
import com.hollow.fishsso.service.AuthApplicationService;
import com.hollow.fishsso.service.dto.LoginResult;
import com.hollow.fishsso.service.dto.TokenSet;
import com.hollow.fishsso.service.dto.UserInfoView;
import com.hollow.fishsso.util.CookieUtils;
import com.hollow.fishsso.util.SsoCookieNames;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/sso")
public class AuthController {

    private static final String UNKNOWN_IP = "unknown-ip";

    private final AuthApplicationService authApplicationService;
    private final SsoProperties properties;

    /**
     * 构造函数
     * @param authApplicationService 认证应用服务
     * @param properties SSO配置属性
     */
    public AuthController(AuthApplicationService authApplicationService, SsoProperties properties) {
        this.authApplicationService = authApplicationService;
        this.properties = properties;
    }

    /**
     * OAuth2/OIDC授权端点，发起授权流程
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param state 状态参数，用于防CSRF
     * @param nonce OIDC nonce参数
     * @param request HTTP请求对象
     * @return 302重定向响应
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(@RequestParam("client_id") String clientId,
                                          @RequestParam("redirect_uri") String redirectUri,
                                          @RequestParam(value = "scope", required = false) String scope,
                                          @RequestParam(value = "state", required = false) String state,
                                          @RequestParam(value = "nonce", required = false) String nonce,
                                          HttpServletRequest request) {
        URI location = authApplicationService.buildAuthorizeRedirect(
                clientId,
                redirectUri,
                scope,
                state,
                nonce,
                currentSessionId(request)
        );
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    /**
     * 用户登录接口
     * @param loginRequest 登录请求体（用户名、密码、可选返回URL）
     * @param httpRequest HTTP请求对象，用于获取客户端IP和UA
     * @return 登录成功响应（含会话Cookie），若有returnTo则303重定向
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest httpRequest) {
        String sourceIp = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String safeReturnTo = validateReturnTo(loginRequest.returnTo());
        LoginResult loginResult = authApplicationService.login(
                loginRequest.username(),
                loginRequest.password(),
                sourceIp,
                userAgent
        );
        ResponseCookie cookie = buildSessionCookie(loginResult.sessionId());
        if (StringUtils.hasText(safeReturnTo)) {
            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .header(HttpHeaders.LOCATION, safeReturnTo)
                    .build();
        }
        LoginResponse response = new LoginResponse(loginResult.sessionId(), loginResult.expiresAt().getEpochSecond());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * 令牌端点（支持 authorization_code 和 refresh_token 两种 grant_type）
     * @param grantType 授权类型（authorization_code 或 refresh_token）
     * @param code 授权码（authorization_code 模式必传）
     * @param redirectUri 重定向URI（authorization_code 模式必传）
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @param refreshToken 刷新令牌（refresh_token 模式必传）
     * @return 令牌响应（包含 access_token、id_token、refresh_token 等）
     */
    @PostMapping("/token")
    public TokenResponse token(@RequestParam("grant_type") String grantType,
                               @RequestParam(value = "code", required = false) String code,
                               @RequestParam(value = "redirect_uri", required = false) String redirectUri,
                               @RequestParam("client_id") String clientId,
                               @RequestParam("client_secret") String clientSecret,
                               @RequestParam(value = "refresh_token", required = false) String refreshToken) {
        TokenSet tokenSet = authApplicationService.handleTokenRequest(
                grantType, code, redirectUri, clientId, clientSecret, refreshToken);
        return new TokenResponse(
                tokenSet.accessToken(),
                tokenSet.tokenType(),
                tokenSet.expiresIn(),
                tokenSet.scope(),
                tokenSet.idToken(),
                tokenSet.refreshToken()
        );
    }

    /**
     * 获取用户信息接口
     * @param authorization Authorization请求头（Bearer {access_token}）
     * @return 用户信息响应
     */
    @GetMapping("/userinfo")
    public UserInfoResponse userInfo(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        UserInfoView user = authApplicationService.getUserInfo(authorization);
        return new UserInfoResponse(user.sub(), user.username(), user.name(), user.email());
    }

    /**
     * 令牌撤销端点（RFC 7009）
     * @param token 待撤销的令牌
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @return 200空响应
     */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@RequestParam("token") String token,
                                       @RequestParam("client_id") String clientId,
                                       @RequestParam("client_secret") String clientSecret) {
        authApplicationService.revokeToken(clientId, clientSecret, token);
        return ResponseEntity.ok().build();
    }

    /**
     * 查询当前登录用户已授权的所有客户端及权限
     * @param request HTTP请求对象
     * @return 已授权客户端列表
     */
    @GetMapping("/authorized-clients")
    public List<AuthorizedClientResponse> listAuthorizedClients(HttpServletRequest request) {
        String sessionId = currentSessionId(request);
        List<AuthorizedClientView> clients = authApplicationService.listAuthorizedClients(sessionId);
        return clients.stream()
                .map(view -> new AuthorizedClientResponse(
                        view.clientId(),
                        view.scopes(),
                        view.authorizedAt().getEpochSecond(),
                        view.homeUrl()
                ))
                .toList();
    }

    /**
     * 撤销当前登录用户对指定客户端的授权（删除同意记录及相关令牌）
     * @param clientId 待撤销的客户端ID
     * @param request HTTP请求对象
     * @return 200空响应
     */
    @DeleteMapping("/authorized-clients/{clientId}")
    public ResponseEntity<Void> revokeClientAuthorization(@PathVariable String clientId,
                                                          HttpServletRequest request) {
        String sessionId = currentSessionId(request);
        authApplicationService.revokeClientAuthorization(sessionId, clientId);
        return ResponseEntity.ok().build();
    }

    /**
     * 登出端点，清除会话
     * @param request HTTP请求对象
     * @return 登出成功响应（含清除会话Cookie）
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String sessionId = currentSessionId(request);
        authApplicationService.logout(sessionId);
        ResponseCookie cookie = clearSessionCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "已登出"));
    }

    /**
     * 从请求中获取当前会话ID
     * @param request HTTP请求对象
     * @return 会话ID，不存在时返回null
     */
    private String currentSessionId(HttpServletRequest request) {
        return CookieUtils.getCookieValue(request, SsoCookieNames.SESSION).orElse(null);
    }

    /**
     * 构建会话Cookie
     * @param sessionId 会话ID
     * @return 会话Cookie
     */
    private ResponseCookie buildSessionCookie(String sessionId) {
        return ResponseCookie.from(SsoCookieNames.SESSION, sessionId)
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .path("/")
                .sameSite(properties.getCookie().getSameSite())
                .maxAge(properties.getSessionTtl())
                .build();
    }

    /**
     * 构建清除会话的Cookie（maxAge=0）
     * @return 清除会话Cookie
     */
    private ResponseCookie clearSessionCookie() {
        return ResponseCookie.from(SsoCookieNames.SESSION, "")
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .path("/")
                .sameSite(properties.getCookie().getSameSite())
                .maxAge(0)
                .build();
    }

    /**
     * 校验登录完成后的回跳路径，只允许站内相对路径且必须命中白名单前缀。
     * @param returnTo 原始回跳路径
     * @return 校验后的回跳路径
     */
    private String validateReturnTo(String returnTo) {
        if (!StringUtils.hasText(returnTo)) {
            return null;
        }
        String candidate = returnTo.trim();
        if (!candidate.startsWith("/") || candidate.startsWith("//")
                || candidate.contains("\\") || candidate.contains("\r") || candidate.contains("\n")) {
            throw invalidReturnTo();
        }

        URI uri;
        try {
            uri = URI.create(candidate);
        } catch (IllegalArgumentException ex) {
            throw invalidReturnTo();
        }

        if (uri.isAbsolute()
                || StringUtils.hasText(uri.getScheme())
                || StringUtils.hasText(uri.getAuthority())
                || StringUtils.hasText(uri.getHost())
                || uri.getPort() != -1
                || StringUtils.hasText(uri.getUserInfo())) {
            throw invalidReturnTo();
        }

        String path = uri.getPath();
        if (!StringUtils.hasText(path) || !isAllowedReturnToPath(path)) {
            throw invalidReturnTo();
        }
        return candidate;
    }

    /**
     * 判断回跳路径是否命中允许的前缀。
     * @param path 路径
     * @return 是否允许
     */
    private boolean isAllowedReturnToPath(String path) {
        return properties.getAllowedReturnToPrefixes().stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    /**
     * 构造非法 return_to 异常。
     * @return 非法请求异常
     */
    private SsoException invalidReturnTo() {
        return new SsoException(HttpStatus.BAD_REQUEST, "invalid_request", "非法 return_to 参数");
    }

    /**
     * 解析客户端真实IP地址，依次尝试 X-Forwarded-For、X-Real-IP、RemoteAddr
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor;
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        if (!StringUtils.hasText(remoteAddr)) {
            return UNKNOWN_IP;
        }
        return remoteAddr.trim();
    }

    /**
     * 从 X-Forwarded-For 头中提取第一个IP地址
     * @param xForwardedFor X-Forwarded-For头的值
     * @return 第一个IP地址，为空时返回null
     */
    private String firstForwardedIp(String xForwardedFor) {
        if (!StringUtils.hasText(xForwardedFor)) {
            return null;
        }
        int firstComma = xForwardedFor.indexOf(',');
        if (firstComma < 0) {
            return xForwardedFor.trim();
        }
        return xForwardedFor.substring(0, firstComma).trim();
    }
}
