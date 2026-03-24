package com.hollow.fishsso.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * 访问令牌模型
 */
@RedisHash("sso:access_tokens")
public class AccessToken {

    @Id
    private String token;
    private String clientId;
    private String userId;
    private List<String> scopes = new ArrayList<>();
    private Instant expiresAt;
    @TimeToLive
    private Long ttlSeconds;

    public AccessToken() {
    }

    /**
     * 构造函数
     * @param token 令牌
     * @param clientId 客户端ID
     * @param userId 用户ID
     * @param scopes 授权范围列表
     * @param expiresAt 过期时间
     */
    public AccessToken(String token, String clientId, String userId, List<String> scopes, Instant expiresAt) {
        this.token = token;
        this.clientId = clientId;
        this.userId = userId;
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
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
     * 判断令牌是否过期
     * @param now 当前时间
     * @return 是否过期
     */
    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }
}
