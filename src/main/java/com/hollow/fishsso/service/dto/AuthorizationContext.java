package com.hollow.fishsso.service.dto;

import java.util.List;

/**
 * 授权上下文DTO
 * @param clientId 客户端ID
 * @param redirectUri 重定向URI
 * @param scopes 授权范围列表
 * @param userId 用户ID
 * @param username 用户名
 * @param displayName 显示名称
 */
public record AuthorizationContext(String clientId,
                                   String redirectUri,
                                   List<String> scopes,
                                   String userId,
                                   String username,
                                   String displayName) {

    public AuthorizationContext {
        scopes = List.copyOf(scopes);
    }
}