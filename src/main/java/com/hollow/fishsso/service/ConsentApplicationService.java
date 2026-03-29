package com.hollow.fishsso.service;

import com.hollow.fishsso.model.AuthCode;
import com.hollow.fishsso.service.dto.AuthorizationContext;
import com.hollow.fishsso.service.dto.ConsentContextView;
import com.hollow.fishsso.service.dto.LoginRequiredResult;
import com.hollow.fishsso.service.dto.RedirectTarget;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 授权同意应用服务
 */
@Service
public class ConsentApplicationService {

    private final SsoService ssoService;

    /**
     * 构造函数。
     * @param ssoService SSO 核心服务
     */
    public ConsentApplicationService(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    /**
     * 获取授权同意上下文
     * @param clientId 客户端 ID
     * @param redirectUri 重定向 URI
     * @param scope 授权范围
     * @param state 状态参数
     * @param nonce OIDC nonce 参数
     * @param sessionId 会话 ID
     * @return 授权同意上下文
     */
    public ConsentContextView getConsentContext(String clientId,
                                                String redirectUri,
                                                String scope,
                                                String state,
                                                String nonce,
                                                String sessionId) {
        AuthorizationContext context = ssoService.buildConsentContext(clientId, redirectUri, scope, sessionId);
        return new ConsentContextView(
                context.clientId(),
                context.redirectUri(),
                context.scopes(),
                context.username(),
                context.displayName(),
                state,
                nonce,
                scope
        );
    }

    /**
     * 提交授权同意
     * @param clientId 客户端 ID
     * @param redirectUri 重定向 URI
     * @param scope 授权范围
     * @param state 状态参数
     * @param nonce OIDC nonce 参数
     * @param action 操作类型
     * @param sessionId 会话 ID
     * @return 重定向目标
     */
    public RedirectTarget submitConsent(String clientId,
                                        String redirectUri,
                                        String scope,
                                        String state,
                                        String nonce,
                                        String action,
                                        String sessionId) {
        if ("approve".equalsIgnoreCase(action)) {
            AuthCode authCode = ssoService.approveAuthorization(clientId, redirectUri, scope, nonce, sessionId);
            return new RedirectTarget(buildCodeRedirect(redirectUri, authCode.getCode(), state).toString());
        }

        ssoService.buildConsentContext(clientId, redirectUri, scope, sessionId);
        return new RedirectTarget(buildErrorRedirect(redirectUri, "access_denied", "用户拒绝授权", state).toString());
    }

    /**
     * 构建需要登录的结果
     * @param requestUri 请求 URI
     * @param queryString 查询字符串
     * @return 需要登录的结果
     */
    public LoginRequiredResult buildLoginRequired(String requestUri, String queryString) {
        String currentUrl = requestUri;
        if (StringUtils.hasText(queryString)) {
            currentUrl = currentUrl + "?" + queryString;
        }
        String loginUrl = UriComponentsBuilder.fromPath("/login")
                .queryParam("return_to", currentUrl)
                .build()
                .encode()
                .toUriString();
        return new LoginRequiredResult(loginUrl);
    }

    /**
     * 构建携带授权码的重定向 URI。
     * @param redirectUri 重定向 URI
     * @param code 授权码
     * @param state 状态参数
     * @return 重定向 URI
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
     * 构建携带错误信息的重定向 URI。
     * @param redirectUri 重定向 URI
     * @param error 错误码
     * @param description 描述信息
     * @param state 状态参数
     * @return 重定向 URI
     */
    private URI buildErrorRedirect(String redirectUri, String error, String description, String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", error)
                .queryParam("error_description", description);
        if (StringUtils.hasText(state)) {
            builder.queryParam("state", state);
        }
        return builder.build().encode().toUri();
    }
}
