package com.hollow.fishsso.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * 会话信息模型
 */
@RedisHash("sso:sessions")
public class SessionInfo {

    @Id
    private String sessionId;
    private String userId;
    private Instant expiresAt;
    @TimeToLive
    private Long ttlSeconds;

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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

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
