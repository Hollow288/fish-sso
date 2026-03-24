package com.hollow.fishsso.controller.dto;

/**
 * 错误响应DTO
 * @param error 错误代码
 * @param error_description 错误描述
 */
public record ErrorResponse(String error, String error_description) {
}