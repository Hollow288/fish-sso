package com.hollow.fishsso.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 授权同意请求DTO
 * @param clientId 客户端ID
 * @param redirectUri 重定向URI
 * @param scope 授权范围
 * @param state 状态参数
 * @param action 操作类型（允许/拒绝）
 */
public record ConsentRequest(
        @JsonProperty("client_id") String clientId,
        @JsonProperty("redirect_uri") String redirectUri,
        String scope,
        String state,
        String action
) {
}