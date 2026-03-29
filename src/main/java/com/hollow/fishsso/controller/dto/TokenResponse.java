package com.hollow.fishsso.controller.dto;

/**
 * 令牌响应DTO
 * @param access_token 访问令牌(JWT)
 * @param token_type 令牌类型
 * @param expires_in 过期时间（秒）
 * @param scope 授权范围
 * @param id_token ID令牌(JWT)
 * @param refresh_token 刷新令牌
 */
public record TokenResponse(String access_token, String token_type, long expires_in, String scope,
                             String id_token, String refresh_token) {
}
