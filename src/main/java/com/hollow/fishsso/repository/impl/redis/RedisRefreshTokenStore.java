package com.hollow.fishsso.repository.impl.redis;

import com.hollow.fishsso.model.RefreshToken;
import com.hollow.fishsso.repository.RefreshTokenStore;
import com.hollow.fishsso.repository.redis.RefreshTokenRedisRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Redis刷新令牌存储实现
 */
@Repository
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenRedisRepository repository;

    /**
     * 构造函数。
     * @param repository 仓储对象
     */
    public RedisRefreshTokenStore(RefreshTokenRedisRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建并保存数据。
     * @param clientId 客户端 ID
     * @param userId 用户 ID
     * @param scopes 授权范围列表
     * @param ttl 生存时长
     * @return 创建后的对象
     */
    @Override
    public RefreshToken create(String clientId, String userId, List<String> scopes, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(ttl);
        RefreshToken refreshToken = new RefreshToken(token, clientId, userId, scopes, expiresAt);
        refreshToken.setTtlSeconds(ttl.getSeconds());
        repository.save(refreshToken);
        return refreshToken;
    }

    /**
     * 查询指定数据。
     * @param token 令牌
     * @return 查询结果
     */
    @Override
    public Optional<RefreshToken> find(String token) {
        return repository.findById(token);
    }

    /**
     * 删除指定数据。
     * @param token 令牌
     */
    @Override
    public void delete(String token) {
        repository.deleteById(token);
    }
}
