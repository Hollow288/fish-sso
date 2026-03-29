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

    /**
     * 创建并保存数据。
     * @param clientId 客户端 ID
     * @param userId 用户 ID
     * @param redirectUri 重定向 URI
     * @param scopes 授权范围列表
     * @param nonce OIDC nonce 参数
     * @param ttl 生存时长
     * @return 创建后的对象
     */
    @Override
    public AuthCode create(String clientId, String userId, String redirectUri, List<String> scopes, String nonce, Duration ttl) {
        String code = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(ttl);
        AuthCode authCode = new AuthCode(code, clientId, userId, redirectUri, scopes, nonce, expiresAt);
        authCode.setTtlSeconds(ttl.getSeconds());
        repository.save(authCode);
        return authCode;
    }

    /**
     * 消费并返回已存储的数据。
     * @param code 授权码
     * @return 消费结果
     */
    @Override
    public Optional<AuthCode> consume(String code) {
        Optional<AuthCode> authCode = repository.findById(code);
        authCode.ifPresent(item -> repository.deleteById(code));
        return authCode;
    }
}
