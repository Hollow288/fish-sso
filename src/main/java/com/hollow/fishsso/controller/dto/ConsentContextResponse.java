package com.hollow.fishsso.controller.dto;

import java.util.List;

/**
 * 授权同意上下文响应DTO
 * @param clientId 客户端ID
 * @param redirectUri 重定向URI
 * @param scopes 授权范围列表
 * @param username 用户名
 * @param displayName 显示名称
 * @param state 状态参数
 * @param scope 授权范围字符串
 */
public record ConsentContextResponse(String clientId,
                                     String redirectUri,
                                     List<String> scopes,
                                     String username,
                                     String displayName,
                                     String state,
                                     String scope) {
}