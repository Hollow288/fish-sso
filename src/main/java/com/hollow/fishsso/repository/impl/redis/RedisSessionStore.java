package com.hollow.fishsso.repository.impl.redis;

import com.hollow.fishsso.model.SessionInfo;
import com.hollow.fishsso.repository.redis.SessionRedisRepository;
import com.hollow.fishsso.repository.SessionStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Redis会话存储实现
 */
@Repository
public class RedisSessionStore implements SessionStore {

    private final SessionRedisRepository repository;

    /**
     * 构造函数
     * @param repository 会话Redis仓储
     */
    public RedisSessionStore(SessionRedisRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建并保存数据。
     * @param userId 用户 ID
     * @param ttl 生存时长
     * @return 创建后的对象
     */
    @Override
    public SessionInfo create(String userId, Duration ttl) {
        String sessionId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(ttl);
        SessionInfo sessionInfo = new SessionInfo(sessionId, userId, expiresAt);
        sessionInfo.setTtlSeconds(ttl.getSeconds());
        repository.save(sessionInfo);
        return sessionInfo;
    }

    /**
     * 查询指定数据。
     * @param sessionId 会话 ID
     * @return 查询结果
     */
    @Override
    public Optional<SessionInfo> find(String sessionId) {
        return repository.findById(sessionId);
    }

    /**
     * 删除指定数据。
     * @param sessionId 会话 ID
     */
    @Override
    public void delete(String sessionId) {
        repository.deleteById(sessionId);
    }
}
