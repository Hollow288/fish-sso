package com.hollow.fishsso.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

/**
 * 刷新令牌模型
 */
@RedisHash("sso:refresh_tokens")
public class RefreshToken {

    @Id
    private String token;
    @Indexed
    private String clientId;
    @Indexed
    private String userId;
    private List<String> scopes = new ArrayList<>();
    private Instant expiresAt;
    @TimeToLive
    private Long ttlSeconds;

    /**
     * 构造函数。
     */
    public RefreshToken() {
    }

    /**
     * 构造函数。
     * @param token 令牌
     * @param clientId 客户端 ID
     * @param userId 用户 ID
     * @param scopes 授权范围列表
     * @param expiresAt 过期时间
     */
    public RefreshToken(String token, String clientId, String userId, List<String> scopes, Instant expiresAt) {
        this.token = token;
        this.clientId = clientId;
        this.userId = userId;
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
        this.expiresAt = expiresAt;
    }

    /**
     * 获取令牌。
     * @return 令牌
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置令牌。
     * @param token 令牌
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取客户端 ID。
     * @return 客户端 ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 设置客户端 ID。
     * @param clientId 客户端 ID
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
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
     * 获取授权范围列表。
     * @return 授权范围列表
     */
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * 设置授权范围列表。
     * @param scopes 授权范围列表
     */
    public void setScopes(List<String> scopes) {
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
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
     * 判断当前数据是否已过期。
     * @param now 当前时间
     * @return 是否expired
     */
    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }
}
