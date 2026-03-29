package com.hollow.fishsso.service.dto;

import java.util.List;

/**
 * 授权同意上下文视图DTO
 * @param clientId 客户端ID
 * @param redirectUri 重定向URI
 * @param scopes 授权范围列表
 * @param username 用户名
 * @param displayName 显示名称
 * @param state 状态参数
 * @param nonce OIDC nonce参数
 * @param scope 授权范围字符串
 */
public record ConsentContextView(String clientId,
                                 String redirectUri,
                                 List<String> scopes,
                                 String username,
                                 String displayName,
                                 String state,
                                 String nonce,
                                 String scope) {

    public ConsentContextView {
        scopes = List.copyOf(scopes);
        scope = scope == null ? "" : scope;
    }
}
