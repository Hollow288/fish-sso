package com.hollow.fishsso.exception;

import org.springframework.http.HttpStatus;

/**
 * SSO异常类
 */
public class SsoException extends RuntimeException {

    private final HttpStatus status;
    private final String error;
    private final String errorDescription;

    /**
     * 构造函数
     * @param status HTTP状态码
     * @param error 错误代码
     * @param errorDescription 错误描述
     */
    public SsoException(HttpStatus status, String error, String errorDescription) {
        super(errorDescription);
        this.status = status;
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}