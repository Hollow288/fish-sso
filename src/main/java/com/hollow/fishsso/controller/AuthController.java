package com.hollow.fishsso.controller;

import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.controller.dto.LoginRequest;
import com.hollow.fishsso.controller.dto.LoginResponse;
import com.hollow.fishsso.controller.dto.TokenResponse;
import com.hollow.fishsso.controller.dto.UserInfoResponse;
import com.hollow.fishsso.service.AuthApplicationService;
import com.hollow.fishsso.service.dto.LoginResult;
import com.hollow.fishsso.service.dto.TokenResult;
import com.hollow.fishsso.service.dto.UserInfoView;
import com.hollow.fishsso.util.SsoCookieNames;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
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
     * OAuth2授权端点
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param state 状态参数
     * @return 重定向响应
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(@RequestParam("client_id") String clientId,
                                          @RequestParam("redirect_uri") String redirectUri,
                                          @RequestParam(value = "scope", required = false) String scope,
                                          @RequestParam(value = "state", required = false) String state) {
        URI location = authApplicationService.buildAuthorizeRedirect(clientId, redirectUri, scope, state);
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    /**
     * 用户登录接口
     * @param request 登录请求对象
     * @return 登录响应
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        LoginResult loginResult = authApplicationService.login(request.username(), request.password());
        ResponseCookie cookie = buildSessionCookie(loginResult.sessionId());
        if (StringUtils.hasText(request.returnTo())) {
            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .header(HttpHeaders.LOCATION, request.returnTo())
                    .build();
        }
        LoginResponse response = new LoginResponse(loginResult.sessionId(), loginResult.expiresAt().getEpochSecond());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * 令牌交换接口
     * @param grantType 授权类型
     * @param code 授权码
     * @param redirectUri 重定向URI
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @return 令牌响应
     */
    @PostMapping("/token")
    public TokenResponse token(@RequestParam("grant_type") String grantType,
                               @RequestParam("code") String code,
                               @RequestParam("redirect_uri") String redirectUri,
                               @RequestParam("client_id") String clientId,
                               @RequestParam("client_secret") String clientSecret) {
        TokenResult token = authApplicationService.exchangeCode(grantType, code, redirectUri, clientId, clientSecret);
        return new TokenResponse(token.accessToken(), token.tokenType(), token.expiresIn(), token.scope());
    }

    /**
     * 获取用户信息接口
     * @param authorization 授权头信息
     * @return 用户信息响应
     */
    @GetMapping("/userinfo")
    public UserInfoResponse userInfo(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        UserInfoView user = authApplicationService.getUserInfo(authorization);
        return new UserInfoResponse(user.sub(), user.username(), user.name(), user.email());
    }

    /**
     * 构建会话Cookie
     * @param sessionId 会话ID
     * @return 响应Cookie对象
     */
    private ResponseCookie buildSessionCookie(String sessionId) {
        return ResponseCookie.from(SsoCookieNames.SESSION, sessionId)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(properties.getSessionTtl())
                .build();
    }
}