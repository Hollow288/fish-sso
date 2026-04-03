package com.hollow.fishsso.exception;

import com.hollow.fishsso.controller.ConsentController;
import com.hollow.fishsso.controller.dto.ErrorResponse;
import com.hollow.fishsso.controller.dto.LoginRequiredResponse;
import jakarta.servlet.http.HttpServletRequest;
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
                    buildLoginUrl(request.getRequestURI(), request.getQueryString())
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getError(), ex.getErrorDescription()));
    }

    /**
     * 构建登录地址，并保留当前请求作为回跳目标。
     * @param requestUri 当前请求路径
     * @param queryString 当前请求查询串
     * @return 登录地址
     */
    private String buildLoginUrl(String requestUri, String queryString) {
        String currentUrl = requestUri;
        if (StringUtils.hasText(queryString)) {
            currentUrl = currentUrl + "?" + queryString;
        }
        return UriComponentsBuilder.fromPath("/login")
                .queryParam("return_to", currentUrl)
                .build()
                .encode()
                .toUriString();
    }
}
