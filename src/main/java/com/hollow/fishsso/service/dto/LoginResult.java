package com.hollow.fishsso.service.dto;

import java.time.Instant;

/**
 * 登录结果DTO
 * @param sessionId 会话ID
 * @param expiresAt 过期时间
 */
public record LoginResult(String sessionId, Instant expiresAt) {
}