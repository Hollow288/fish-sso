package com.hollow.fishsso.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 重置密码请求DTO
 * @param preferredUsername 用户名
 * @param newPassword 新密码
 * @param code 验证码
 */
public record ResetPasswordRequest(@JsonProperty("preferred_username") String preferredUsername,
                                   @JsonProperty("new_password") String newPassword,
                                   String code) {
}
