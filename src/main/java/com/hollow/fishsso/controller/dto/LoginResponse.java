package com.hollow.fishsso.controller.dto;

/**
 * 登录响应DTO
 * @param session_id 会话ID
 * @param expires_at 过期时间戳（秒）
 */
public record LoginResponse(String session_id, long expires_at) {
}