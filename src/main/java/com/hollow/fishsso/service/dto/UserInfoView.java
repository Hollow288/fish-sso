package com.hollow.fishsso.service.dto;

/**
 * 用户信息视图DTO
 * @param sub 用户唯一标识
 * @param preferredUsername 用户名
 * @param name 显示名称
 * @param email 电子邮箱
 * @param emailVerified 邮箱是否已验证，未授权 email 或未设置邮箱时为空
 */
public record UserInfoView(String sub, String preferredUsername, String name, String email, Boolean emailVerified) {
}
