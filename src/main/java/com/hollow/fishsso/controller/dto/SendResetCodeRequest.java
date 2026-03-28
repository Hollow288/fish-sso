package com.hollow.fishsso.controller.dto;

/**
 * 发送重置验证码请求DTO
 * @param username 用户名
 * @param email 邮箱
 */
public record SendResetCodeRequest(String username, String email) {
}
