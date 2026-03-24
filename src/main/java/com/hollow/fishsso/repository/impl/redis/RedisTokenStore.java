package com.hollow.fishsso.repository.impl.redis;

import com.hollow.fishsso.model.AccessToken;
import com.hollow.fishsso.repository.redis.AccessTokenRedisRepository;
import com.hollow.fishsso.repository.TokenStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    @Override
    public AccessToken create(String clientId, String userId, List<String> scopes, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(ttl);
        AccessToken accessToken = new AccessToken(token, clientId, userId, scopes, expiresAt);
        accessToken.setTtlSeconds(ttl.getSeconds());
        repository.save(accessToken);
        return accessToken;
    }

    @Override
    public Optional<AccessToken> find(String token) {
        return repository.findById(token);
    }
}
