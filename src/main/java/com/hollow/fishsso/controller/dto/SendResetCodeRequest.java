package com.hollow.fishsso.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 发送重置验证码请求DTO
 * @param preferredUsername 用户名
 * @param email 邮箱
 */
public record SendResetCodeRequest(@JsonProperty("preferred_username") String preferredUsername, String email) {
}
