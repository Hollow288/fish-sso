package com.hollow.fishsso.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 登录请求DTO
 * @param username 用户名
 * @param password 密码
 * @param returnTo 登录成功后返回的URL
 */
public record LoginRequest(String username,
                           String password,
                           @JsonProperty("return_to") String returnTo) {
}