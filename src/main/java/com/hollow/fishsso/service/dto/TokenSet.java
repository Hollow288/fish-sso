package com.hollow.fishsso.service.dto;

/**
 * OIDC令牌结果集
 * @param accessToken 访问令牌(JWT)
 * @param idToken ID令牌(JWT)
 * @param refreshToken 刷新令牌
 * @param tokenType 令牌类型
 * @param expiresIn 过期时间（秒）
 * @param scope 授权范围
 */
public record TokenSet(String accessToken, String idToken, String refreshToken,
                       String tokenType, long expiresIn, String scope) {
}
