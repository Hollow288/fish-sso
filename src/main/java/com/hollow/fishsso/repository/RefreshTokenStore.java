package com.hollow.fishsso.repository;

import com.hollow.fishsso.model.RefreshToken;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 刷新令牌存储接口
 */
public interface RefreshTokenStore {

    /**
     * 创建并保存数据。
     * @param clientId 客户端 ID
     * @param userId 用户 ID
     * @param scopes 授权范围列表
     * @param ttl 生存时长
     * @return 创建后的对象
     */
    RefreshToken create(String clientId, String userId, List<String> scopes, Duration ttl);

    /**
     * 查询指定数据。
     * @param token 令牌
     * @return 查询结果
     */
    Optional<RefreshToken> find(String token);

    /**
     * 删除指定数据。
     * @param token 令牌
     */
    void delete(String token);

    /**
     * 删除用户在指定客户端下的所有刷新令牌
     * @param userId 用户ID
     * @param clientId 客户端ID
     */
    void deleteByUserIdAndClientId(String userId, String clientId);
}
