package com.hollow.fishsso.controller.dto;

/**
 * 用户信息响应DTO
 * @param sub 用户唯一标识
 * @param username 用户名
 * @param name 显示名称
 * @param email 电子邮箱
 */
public record UserInfoResponse(String sub, String username, String name, String email) {
}