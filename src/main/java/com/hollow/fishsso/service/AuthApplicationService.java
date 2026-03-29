package com.hollow.fishsso.service;

import com.hollow.fishsso.exception.SsoException;
import com.hollow.fishsso.model.SessionInfo;
import com.hollow.fishsso.service.dto.AuthorizedClientView;
import com.hollow.fishsso.service.dto.LoginResult;
import com.hollow.fishsso.service.dto.TokenSet;
import com.hollow.fishsso.service.dto.UserInfoView;
import java.util.List;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 认证应用服务
 */
@Service
public class AuthApplicationService {

    private static final String GRANT_AUTHORIZATION_CODE = "authorization_code";
    private static final String GRANT_REFRESH_TOKEN = "refresh_token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SsoService ssoService;

    /**
     * 构造函数
     * @param ssoService SSO核心服务
     */
    public AuthApplicationService(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    /**
     * 构建授权重定向URI，若已有同意记录则自动批准并携带授权码重定向，否则重定向到同意页面
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param state 状态参数
     * @param nonce OIDC nonce参数
     * @param sessionId 当前会话ID
     * @return 重定向URI
     */
    public URI buildAuthorizeRedirect(String clientId, String redirectUri, String scope, String state, String nonce, String sessionId) {
        return ssoService.tryAutoApproveAuthorization(clientId, redirectUri, scope, nonce, sessionId)
                .map(authCode -> buildCodeRedirect(redirectUri, authCode.getCode(), state))
                .orElseGet(() -> buildConsentRedirect(clientId, redirectUri, scope, state, nonce));
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @param sourceIp 来源IP地址
     * @param userAgent 用户代理字符串
     * @return 登录结果（含会话ID和过期时间）
     */
    public LoginResult login(String username, String password, String sourceIp, String userAgent) {
        SessionInfo session = ssoService.login(username, password, sourceIp, userAgent);
        return new LoginResult(session.getSessionId(), session.getExpiresAt());
    }

    /**
     * 统一令牌端点处理（根据 grant_type 分发到授权码换取或刷新令牌）
     * @param grantType 授权类型
     * @param code 授权码
     * @param redirectUri 重定向URI
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @param refreshToken 刷新令牌
     * @return 令牌集合（access_token、id_token、refresh_token）
     */
    public TokenSet handleTokenRequest(String grantType,
                                       String code,
                                       String redirectUri,
                                       String clientId,
                                       String clientSecret,
                                       String refreshToken) {
        if (GRANT_AUTHORIZATION_CODE.equals(grantType)) {
            return ssoService.exchangeCode(clientId, clientSecret, code, redirectUri);
        } else if (GRANT_REFRESH_TOKEN.equals(grantType)) {
            if (!StringUtils.hasText(refreshToken)) {
                throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_request", "缺少 refresh_token 参数");
            }
            return ssoService.refreshToken(clientId, clientSecret, refreshToken);
        } else {
            throw new SsoException(HttpStatus.BAD_REQUEST, "unsupported_grant_type",
                    "仅支持 authorization_code 和 refresh_token");
        }
    }

    /**
     * 获取用户信息
     * @param authorization Authorization请求头（Bearer {access_token}）
     * @return 用户信息视图
     */
    public UserInfoView getUserInfo(String authorization) {
        String accessToken = resolveBearerToken(authorization);
        return ssoService.userInfo(accessToken);
    }

    /**
     * 撤销令牌
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @param token 待撤销的令牌
     */
    public void revokeToken(String clientId, String clientSecret, String token) {
        ssoService.revokeToken(clientId, clientSecret, token);
    }

    /**
     * 查询当前登录用户已授权的所有客户端
     * @param sessionId 当前会话ID
     * @return 已授权客户端列表
     */
    public List<AuthorizedClientView> listAuthorizedClients(String sessionId) {
        return ssoService.listAuthorizedClients(sessionId);
    }

    /**
     * 撤销当前登录用户对指定客户端的授权
     * @param sessionId 当前会话ID
     * @param clientId 待撤销的客户端ID
     */
    public void revokeClientAuthorization(String sessionId, String clientId) {
        ssoService.revokeClientAuthorization(sessionId, clientId);
    }

    /**
     * 登出，删除指定会话
     * @param sessionId 会话ID
     */
    public void logout(String sessionId) {
        ssoService.logout(sessionId);
    }

    /**
     * 构建跳转到同意页面的重定向URI
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param state 状态参数
     * @param nonce OIDC nonce参数
     * @return 同意页面URI
     */
    private URI buildConsentRedirect(String clientId, String redirectUri, String scope, String state, String nonce) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/consent")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri);
        if (StringUtils.hasText(scope)) {
            builder.queryParam("scope", scope);
        }
        if (StringUtils.hasText(state)) {
            builder.queryParam("state", state);
        }
        if (StringUtils.hasText(nonce)) {
            builder.queryParam("nonce", nonce);
        }
        return builder.build().encode().toUri();
    }


    /**
     * 构建携带授权码的重定向URI
     * @param redirectUri 重定向URI
     * @param code 授权码
     * @param state 状态参数
     * @return 携带code参数的重定向URI
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
     * 从Authorization头中解析Bearer令牌
     * @param authorization Authorization请求头
     * @return 令牌字符串
     * @throws SsoException 当缺少或格式不正确时抛出
     */
    private String resolveBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new SsoException(HttpStatus.UNAUTHORIZED, "invalid_token", "缺少 Bearer 令牌");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
