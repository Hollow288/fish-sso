package com.hollow.fishsso.controller;

import com.hollow.fishsso.controller.dto.ConsentContextResponse;
import com.hollow.fishsso.controller.dto.ConsentRequest;
import com.hollow.fishsso.controller.dto.RedirectResponse;
import com.hollow.fishsso.controller.support.SsoRequestContextResolver;
import com.hollow.fishsso.service.ConsentApplicationService;
import com.hollow.fishsso.service.dto.ConsentContextView;
import com.hollow.fishsso.service.dto.RedirectTarget;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权同意控制器
 */
@RestController
public class ConsentController {

    private final ConsentApplicationService consentApplicationService;
    private final SsoRequestContextResolver requestContextResolver;

    /**
     * 构造函数
     * @param consentApplicationService 授权同意应用服务
     * @param requestContextResolver 请求上下文解析器
     */
    public ConsentController(ConsentApplicationService consentApplicationService,
                             SsoRequestContextResolver requestContextResolver) {
        this.consentApplicationService = consentApplicationService;
        this.requestContextResolver = requestContextResolver;
    }

    /**
     * 获取授权同意上下文，返回需要用户确认的授权信息
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param state 状态参数
     * @param nonce OIDC nonce参数
     * @param request HTTP请求对象
     * @return 授权同意上下文响应，未登录时返回401
     */
    @GetMapping("/consent")
    public ConsentContextResponse consentContext(@RequestParam("client_id") String clientId,
                                                 @RequestParam("redirect_uri") String redirectUri,
                                                 @RequestParam(value = "scope", required = false) String scope,
                                                 @RequestParam(value = "state", required = false) String state,
                                                 @RequestParam(value = "nonce", required = false) String nonce,
                                                 HttpServletRequest request) {
        ConsentContextView context = consentApplicationService.getConsentContext(
                clientId, redirectUri, scope, state, nonce, requestContextResolver.resolveSessionId(request)
        );
        return new ConsentContextResponse(
                context.clientId(),
                context.redirectUri(),
                context.scopes(),
                context.username(),
                context.displayName(),
                context.state(),
                context.nonce(),
                context.scope()
        );
    }

    /**
     * 提交授权同意（批准或拒绝）
     * @param consentRequest 授权同意请求体
     * @param request HTTP请求对象
     * @return 重定向URL响应，未登录时返回401
     */
    @PostMapping("/consent")
    public RedirectResponse consentSubmit(@RequestBody ConsentRequest consentRequest,
                                          HttpServletRequest request) {
        RedirectTarget redirectTarget = consentApplicationService.submitConsent(
                consentRequest.clientId(),
                consentRequest.redirectUri(),
                consentRequest.scope(),
                consentRequest.state(),
                consentRequest.nonce(),
                consentRequest.action(),
                requestContextResolver.resolveSessionId(request)
        );
        return new RedirectResponse(redirectTarget.url());
    }
}
