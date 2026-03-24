package com.hollow.fishsso.repository.impl.redis;

import com.hollow.fishsso.model.AuthCode;
import com.hollow.fishsso.repository.redis.AuthCodeRedisRepository;
import com.hollow.fishsso.repository.AuthCodeStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Redis授权码存储实现
 */
@Repository
public class RedisAuthCodeStore implements AuthCodeStore {

    private final AuthCodeRedisRepository repository;

    /**
     * 构造函数
     * @param repository 授权码Redis仓储
     */
    public RedisAuthCodeStore(AuthCodeRedisRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthCode create(String clientId, String userId, String redirectUri, List<String> scopes, Duration ttl) {
        String code = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(ttl);
        AuthCode authCode = new AuthCode(code, clientId, userId, redirectUri, scopes, expiresAt);
        authCode.setTtlSeconds(ttl.getSeconds());
        repository.save(authCode);
        return authCode;
    }

    @Override
    public Optional<AuthCode> consume(String code) {
        Optional<AuthCode> authCode = repository.findById(code);
        authCode.ifPresent(item -> repository.deleteById(code));
        return authCode;
    }
}
