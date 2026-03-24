package com.hollow.fishsso.repository;

import com.hollow.fishsso.model.AccessToken;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 令牌存储接口
 */
public interface TokenStore {
    /**
     * 创建访问令牌
     * @param clientId 客户端ID
     * @param userId 用户ID
     * @param scopes 授权范围列表
     * @param ttl 生存时间
     * @return 访问令牌
     */
    AccessToken create(String clientId, String userId, List<String> scopes, Duration ttl);

    /**
     * 查找访问令牌
     * @param token 令牌
     * @return 访问令牌（可选）
     */
    Optional<AccessToken> find(String token);
}

