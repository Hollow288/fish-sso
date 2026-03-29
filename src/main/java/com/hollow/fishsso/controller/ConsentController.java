package com.hollow.fishsso.controller;

import com.hollow.fishsso.controller.dto.ConsentContextResponse;
import com.hollow.fishsso.controller.dto.ConsentRequest;
import com.hollow.fishsso.controller.dto.ErrorResponse;
import com.hollow.fishsso.controller.dto.LoginRequiredResponse;
import com.hollow.fishsso.controller.dto.RedirectResponse;
import com.hollow.fishsso.exception.SsoException;
import com.hollow.fishsso.service.ConsentApplicationService;
import com.hollow.fishsso.service.dto.ConsentContextView;
import com.hollow.fishsso.service.dto.LoginRequiredResult;
import com.hollow.fishsso.service.dto.RedirectTarget;
import com.hollow.fishsso.util.CookieUtils;
import com.hollow.fishsso.util.SsoCookieNames;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * 构造函数
     * @param consentApplicationService 授权同意应用服务
     */
    public ConsentController(ConsentApplicationService consentApplicationService) {
        this.consentApplicationService = consentApplicationService;
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
    public ResponseEntity<?> consentContext(@RequestParam("client_id") String clientId,
                                            @RequestParam("redirect_uri") String redirectUri,
                                            @RequestParam(value = "scope", required = false) String scope,
                                            @RequestParam(value = "state", required = false) String state,
                                            @RequestParam(value = "nonce", required = false) String nonce,
                                            HttpServletRequest request) {
        try {
            ConsentContextView context = consentApplicationService.getConsentContext(
                    clientId, redirectUri, scope, state, nonce, currentSessionId(request)
            );
            ConsentContextResponse response = new ConsentContextResponse(
                    context.clientId(),
                    context.redirectUri(),
                    context.scopes(),
                    context.username(),
                    context.displayName(),
                    context.state(),
                    context.nonce(),
                    context.scope()
            );
            return ResponseEntity.ok(response);
        } catch (SsoException ex) {
            return handleConsentException(ex, request);
        }
    }

    /**
     * 提交授权同意（批准或拒绝）
     * @param consentRequest 授权同意请求体
     * @param request HTTP请求对象
     * @return 重定向URL响应，未登录时返回401
     */
    @PostMapping("/consent")
    public ResponseEntity<?> consentSubmit(@RequestBody ConsentRequest consentRequest,
                                           HttpServletRequest request) {
        try {
            RedirectTarget redirectTarget = consentApplicationService.submitConsent(
                    consentRequest.clientId(),
                    consentRequest.redirectUri(),
                    consentRequest.scope(),
                    consentRequest.state(),
                    consentRequest.nonce(),
                    consentRequest.action(),
                    currentSessionId(request)
            );
            return ResponseEntity.ok(new RedirectResponse(redirectTarget.url()));
        } catch (SsoException ex) {
            return handleConsentException(ex, request);
        }
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
     * 处理授权同意相关异常，login_required时返回登录URL，其他错误返回ErrorResponse
     * @param ex SSO异常
     * @param request HTTP请求对象
     * @return 错误响应
     */
    private ResponseEntity<?> handleConsentException(SsoException ex, HttpServletRequest request) {
        if (ex.getStatus() == HttpStatus.UNAUTHORIZED && "login_required".equals(ex.getError())) {
            LoginRequiredResult loginRequired = consentApplicationService.buildLoginRequired(
                    request.getRequestURI(),
                    request.getQueryString()
            );
            LoginRequiredResponse response = new LoginRequiredResponse(
                    "login_required",
                    "需要先登录",
                    loginRequired.loginUrl()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getError(), ex.getErrorDescription()));
    }
}
