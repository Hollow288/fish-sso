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
     * 构造函数
     * @param ssoService SSO服务
     */
    public ConsentApplicationService(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    /**
     * 获取授权同意上下文
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param state 状态参数
     * @param sessionId 会话ID
     * @return 授权同意上下文视图
     */
    public ConsentContextView getConsentContext(String clientId,
                                                String redirectUri,
                                                String scope,
                                                String state,
                                                String sessionId) {
        AuthorizationContext context = ssoService.buildConsentContext(clientId, redirectUri, scope, sessionId);
        return new ConsentContextView(
                context.clientId(),
                context.redirectUri(),
                context.scopes(),
                context.username(),
                context.displayName(),
                state,
                scope
        );
    }

    /**
     * 提交授权同意
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param state 状态参数
     * @param action 操作类型（approve/deny）
     * @param sessionId 会话ID
     * @return 重定向目标
     */
    public RedirectTarget submitConsent(String clientId,
                                        String redirectUri,
                                        String scope,
                                        String state,
                                        String action,
                                        String sessionId) {
        if ("approve".equalsIgnoreCase(action)) {
            AuthCode authCode = ssoService.approveAuthorization(clientId, redirectUri, scope, sessionId);
            return new RedirectTarget(buildCodeRedirect(redirectUri, authCode.getCode(), state).toString());
        }

        ssoService.buildConsentContext(clientId, redirectUri, scope, sessionId);
        return new RedirectTarget(buildErrorRedirect(redirectUri, "access_denied", "用户拒绝授权", state).toString());
    }

    /**
     * 构建需要登录的结果
     * @param requestUri 请求URI
     * @param queryString 查询字符串
     * @return 需要登录结果
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
     * 构建错误重定向URI
     * @param redirectUri 重定向URI
     * @param error 错误代码
     * @param description 错误描述
     * @param state 状态参数
     * @return 重定向URI
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