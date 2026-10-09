package com.hollow.fishsso.exception;

import com.hollow.fishsso.controller.ConsentController;
import com.hollow.fishsso.controller.dto.ConsentRequest;
import com.hollow.fishsso.controller.dto.ErrorResponse;
import com.hollow.fishsso.controller.dto.LoginRequiredResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 同意页相关异常处理。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ConsentController.class)
public class ConsentExceptionHandler {

    /**
     * 处理同意流程中的SSO异常。
     * @param ex SSO异常
     * @param request HTTP请求对象
     * @return 错误响应
     */
    @ExceptionHandler(SsoException.class)
    public ResponseEntity<?> handleConsentException(SsoException ex, HttpServletRequest request) {
        if (ex.getStatus() == HttpStatus.UNAUTHORIZED && "login_required".equals(ex.getError())) {
            LoginRequiredResponse response = new LoginRequiredResponse(
                    "login_required",
                    "需要先登录",
                    buildLoginUrl(request)
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getError(), ex.getErrorDescription()));
    }

    /**
     * 构建登录地址，登录后重新检查已有授权。
     * @param request 当前请求，包含查询串或提交的授权参数
     * @return 登录地址
     */
    private String buildLoginUrl(HttpServletRequest request) {
        String authorizeUrl = "/sso/authorize";
        if (request.getAttribute(ConsentRequest.class.getName()) instanceof ConsentRequest consentRequest) {
            authorizeUrl = buildAuthorizeUrl(consentRequest);
        } else if (StringUtils.hasText(request.getQueryString())) {
            authorizeUrl = authorizeUrl + "?" + request.getQueryString();
        }
        return UriComponentsBuilder.fromPath("/login")
                .queryParam("return_to", "{returnTo}")
                .encode()
                .buildAndExpand(authorizeUrl)
                .toUriString();
    }

    /** 将提交请求中的授权参数恢复为授权入口的查询串，不重放批准或拒绝操作。 */
    private String buildAuthorizeUrl(ConsentRequest request) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("client_id", request.clientId());
        parameters.put("redirect_uri", request.redirectUri());
        parameters.put("scope", request.scope());
        parameters.put("state", request.state());
        parameters.put("nonce", request.nonce());
        parameters.put("code_challenge", request.codeChallenge());
        parameters.put("code_challenge_method", request.codeChallengeMethod());

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/sso/authorize");
        parameters.forEach((name, value) -> {
            if (value != null) {
                builder.queryParam(name, "{" + name + "}");
            }
        });
        return builder.encode().buildAndExpand(parameters).toUriString();
    }
}
