package com.hollow.fishsso.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 用户信息响应DTO
 * @param sub 用户唯一标识
 * @param preferredUsername 用户名
 * @param name 显示名称
 * @param email 电子邮箱
 * @param emailVerified 邮箱是否已验证
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfoResponse(String sub,
                               @JsonProperty("preferred_username") String preferredUsername,
                               String name,
                               String email,
                               @JsonProperty("email_verified") Boolean emailVerified) {
}
