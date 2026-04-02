package com.hollow.fishsso.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

/**
 * 会话信息模型
 */
@RedisHash("sso:sessions")
public class SessionInfo {

    @Id
    private String sessionId;
    @Indexed
    private String userId;
    private Instant expiresAt;
    @TimeToLive
    private Long ttlSeconds;

    /**
     * 构造函数。
     */
    public SessionInfo() {
    }

    /**
     * 构造函数
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param expiresAt 过期时间
     */
    public SessionInfo(String sessionId, String userId, Instant expiresAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    /**
     * 获取会话 ID。
     * @return 会话 ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话 ID。
     * @param sessionId 会话 ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取用户 ID。
     * @return 用户 ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID。
     * @param userId 用户 ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取过期时间。
     * @return 过期时间
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * 设置过期时间。
     * @param expiresAt 过期时间
     */
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * 获取TTL 秒数。
     * @return TTL 秒数
     */
    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    /**
     * 设置TTL 秒数。
     * @param ttlSeconds TTL 秒数
     */
    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
    /**
     * 判断会话是否过期
     * @param now 当前时间
     * @return 是否过期
     */
    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }
}
