package com.hollow.fishsso.service.dto;

import java.time.Instant;
import java.util.List;

/**
 * 用户已授权客户端视图DTO
 * @param clientId 客户端ID
 * @param scopes 已授权的权限范围
 * @param authorizedAt 授权时间
 * @param homeUrl 客户端首页地址
 */
public record AuthorizedClientView(String clientId,
                                   List<String> scopes,
                                   Instant authorizedAt,
                                   String homeUrl) {

    public AuthorizedClientView {
        scopes = List.copyOf(scopes);
    }
}
