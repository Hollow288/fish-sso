package com.hollow.fishsso.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * 授权码模型
 */
@RedisHash("sso:auth_codes")
public class AuthCode {

    @Id
    private String code;
    private String clientId;
    private String userId;
    private String redirectUri;
    private List<String> scopes = new ArrayList<>();
    private Instant expiresAt;
    @TimeToLive
    private Long ttlSeconds;

    public AuthCode() {
    }

    /**
     * 构造函数
     * @param code 授权码
     * @param clientId 客户端ID
     * @param userId 用户ID
     * @param redirectUri 重定向URI
     * @param scopes 授权范围列表
     * @param expiresAt 过期时间
     */
    public AuthCode(String code, String clientId, String userId, String redirectUri, List<String> scopes, Instant expiresAt) {
        this.code = code;
        this.clientId = clientId;
        this.userId = userId;
        this.redirectUri = redirectUri;
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
        this.expiresAt = expiresAt;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
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
     * 判断授权码是否过期
     * @param now 当前时间
     * @return 是否过期
     */
    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }
}
