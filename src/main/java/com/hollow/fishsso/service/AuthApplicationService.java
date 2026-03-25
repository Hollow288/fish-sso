package com.hollow.fishsso.service;

import com.hollow.fishsso.exception.SsoException;
import com.hollow.fishsso.model.AccessToken;
import com.hollow.fishsso.model.SessionInfo;
import com.hollow.fishsso.model.UserAccount;
import com.hollow.fishsso.service.dto.LoginResult;
import com.hollow.fishsso.service.dto.TokenResult;
import com.hollow.fishsso.service.dto.UserInfoView;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 认证应用服务
 */
@Service
public class AuthApplicationService {

    private static final String SUPPORTED_GRANT_TYPE = "authorization_code";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SsoService ssoService;

    /**
     * 构造函数
     * @param ssoService SSO服务
     */
    public AuthApplicationService(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    /**
     * 构建授权重定向URI
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param state 状态参数
     * @param sessionId 会话ID
     * @return 重定向URI
     */
    public URI buildAuthorizeRedirect(String clientId, String redirectUri, String scope, String state, String sessionId) {
        return ssoService.tryAutoApproveAuthorization(clientId, redirectUri, scope, sessionId)
                .map(authCode -> buildCodeRedirect(redirectUri, authCode.getCode(), state))
                .orElseGet(() -> buildConsentRedirect(clientId, redirectUri, scope, state));
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @param sourceIp 客户端IP
     * @param userAgent 客户端UA
     * @return 登录结果
     */
    public LoginResult login(String username, String password, String sourceIp, String userAgent) {
        SessionInfo session = ssoService.login(username, password, sourceIp, userAgent);
        return new LoginResult(session.getSessionId(), session.getExpiresAt());
    }

    /**
     * 授权码换取令牌
     * @param grantType 授权类型
     * @param code 授权码
     * @param redirectUri 重定向URI
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @return 令牌结果
     */
    public TokenResult exchangeCode(String grantType,
                                    String code,
                                    String redirectUri,
                                    String clientId,
                                    String clientSecret) {
        if (!SUPPORTED_GRANT_TYPE.equals(grantType)) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "unsupported_grant_type", "仅支持 authorization_code");
        }
        AccessToken token = ssoService.exchangeCode(clientId, clientSecret, code, redirectUri);
        long expiresIn = Math.max(Duration.between(Instant.now(), token.getExpiresAt()).getSeconds(), 0);
        return new TokenResult(token.getToken(), "bearer", expiresIn, String.join(" ", token.getScopes()));
    }

    /**
     * 获取用户信息
     * @param authorization 授权头
     * @return 用户信息视图
     */
    public UserInfoView getUserInfo(String authorization) {
        String accessToken = resolveBearerToken(authorization);
        UserAccount user = ssoService.userInfo(accessToken);
        return new UserInfoView(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail());
    }

    /**
     * 构建同意页重定向URI
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param state 状态参数
     * @return 重定向URI
     */
    private URI buildConsentRedirect(String clientId, String redirectUri, String scope, String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/consent")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri);
        if (StringUtils.hasText(scope)) {
            builder.queryParam("scope", scope);
        }
        if (StringUtils.hasText(state)) {
            builder.queryParam("state", state);
        }
        return builder.build().encode().toUri();
    }

    /**
     * 构建授权码重定向URI
     * @param redirectUri 重定向URI
     * @param code 授权码
     * @param state 状态参数
     * @return 重定向URI
     */
    private URI buildCodeRedirect(String redirectUri, String code, String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code);
        if (StringUtils.hasText(state)) {
            builder.queryParam("state", state);
        }
        return builder.build().encode().toUri();
    }

    /**
     * 解析Bearer令牌
     * @param authorization 授权头
     * @return 访问令牌
     */
    private String resolveBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new SsoException(HttpStatus.UNAUTHORIZED, "invalid_token", "缺少 Bearer 令牌");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
