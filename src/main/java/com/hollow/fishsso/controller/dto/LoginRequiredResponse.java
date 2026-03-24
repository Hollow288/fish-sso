package com.hollow.fishsso.controller.dto;

/**
 * 需要登录响应DTO
 * @param error 错误代码
 * @param error_description 错误描述
 * @param login_url 登录URL
 */
public record LoginRequiredResponse(String error, String error_description, String login_url) {
}