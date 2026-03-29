package com.hollow.fishsso.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 用户已授权客户端响应DTO
 * @param clientId 客户端ID
 * @param scopes 已授权的权限范围
 * @param authorizedAt 授权时间（Unix 秒）
 * @param homeUrl 客户端首页地址
 */
public record AuthorizedClientResponse(@JsonProperty("client_id") String clientId,
                                       List<String> scopes,
                                       @JsonProperty("authorized_at") long authorizedAt,
                                       @JsonProperty("home_url") String homeUrl) {
}
