package com.hollow.fishsso.exception;

import com.hollow.fishsso.controller.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * SSO异常处理器
 */
@RestControllerAdvice
public class SsoExceptionHandler {

    /**
     * 处理SSO异常
     * @param ex SSO异常对象
     * @return 错误响应
     */
    @ExceptionHandler(SsoException.class)
    public ResponseEntity<ErrorResponse> handleSsoException(SsoException ex) {
        ErrorResponse response = new ErrorResponse(ex.getError(), ex.getErrorDescription());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
}