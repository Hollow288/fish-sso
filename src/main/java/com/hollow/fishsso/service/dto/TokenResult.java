package com.hollow.fishsso.service.dto;

/**
 * 令牌结果DTO
 * @param accessToken 访问令牌
 * @param tokenType 令牌类型
 * @param expiresIn 过期时间（秒）
 * @param scope 授权范围
 */
public record TokenResult(String accessToken, String tokenType, long expiresIn, String scope) {
}