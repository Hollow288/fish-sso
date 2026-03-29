package com.hollow.fishsso.repository.impl.redis;

import com.hollow.fishsso.model.AccessToken;
import com.hollow.fishsso.repository.redis.AccessTokenRedisRepository;
import com.hollow.fishsso.repository.TokenStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Redis令牌存储实现
 */
@Repository
public class RedisTokenStore implements TokenStore {

    private final AccessTokenRedisRepository repository;

    /**
     * 构造函数
     * @param repository 访问令牌Redis仓储
     */
    public RedisTokenStore(AccessTokenRedisRepository repository) {
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
    public AccessToken create(String token, String clientId, String userId, List<String> scopes, Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl);
        AccessToken accessToken = new AccessToken(token, clientId, userId, scopes, expiresAt);
        accessToken.setTtlSeconds(ttl.getSeconds());
        repository.save(accessToken);
        return accessToken;
    }

    /**
     * 查询指定数据。
     * @param token 令牌
     * @return 查询结果
     */
    @Override
    public Optional<AccessToken> find(String token) {
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
