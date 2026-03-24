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
     * 获取授权同意上下文
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @param state 状态参数
     * @param request HTTP请求对象
     * @return 授权同意上下文响应
     */
    @GetMapping("/consent")
    public ResponseEntity<?> consentContext(@RequestParam("client_id") String clientId,
                                            @RequestParam("redirect_uri") String redirectUri,
                                            @RequestParam(value = "scope", required = false) String scope,
                                            @RequestParam(value = "state", required = false) String state,
                                            HttpServletRequest request) {
        try {
            ConsentContextView context = consentApplicationService.getConsentContext(
                    clientId,
                    redirectUri,
                    scope,
                    state,
                    currentSessionId(request)
            );
            ConsentContextResponse response = new ConsentContextResponse(
                    context.clientId(),
                    context.redirectUri(),
                    context.scopes(),
                    context.username(),
                    context.displayName(),
                    context.state(),
                    context.scope()
            );
            return ResponseEntity.ok(response);
        } catch (SsoException ex) {
            return handleConsentException(ex, request);
        }
    }

    /**
     * 提交授权同意
     * @param consentRequest 授权同意请求对象
     * @param request HTTP请求对象
     * @return 重定向响应
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
                    consentRequest.action(),
                    currentSessionId(request)
            );
            return ResponseEntity.ok(new RedirectResponse(redirectTarget.url()));
        } catch (SsoException ex) {
            return handleConsentException(ex, request);
        }
    }

    /**
     * 获取当前会话ID
     * @param request HTTP请求对象
     * @return 会话ID
     */
    private String currentSessionId(HttpServletRequest request) {
        return CookieUtils.getCookieValue(request, SsoCookieNames.SESSION).orElse(null);
    }

    /**
     * 处理授权同意异常
     * @param ex SSO异常对象
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