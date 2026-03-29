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
    private String nonce;
    private Instant expiresAt;
    @TimeToLive
    private Long ttlSeconds;

    /**
     * 构造函数。
     */
    public AuthCode() {
    }

    /**
     * 构造函数
     * @param code 授权码
     * @param clientId 客户端ID
     * @param userId 用户ID
     * @param redirectUri 重定向URI
     * @param scopes 授权范围列表
     * @param nonce OIDC nonce 参数
     * @param expiresAt 过期时间
     */
    public AuthCode(String code, String clientId, String userId, String redirectUri, List<String> scopes, String nonce, Instant expiresAt) {
        this.code = code;
        this.clientId = clientId;
        this.userId = userId;
        this.redirectUri = redirectUri;
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
        this.nonce = nonce;
        this.expiresAt = expiresAt;
    }

    /**
     * 获取授权码。
     * @return 授权码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置授权码。
     * @param code 授权码
     */
    public void setCode(String code) {
        this.code = code;
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
     * 获取重定向 URI。
     * @return 重定向 URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * 设置重定向 URI。
     * @param redirectUri 重定向 URI
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
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
     * 获取OIDC nonce 参数。
     * @return OIDC nonce 参数
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * 设置OIDC nonce 参数。
     * @param nonce OIDC nonce 参数
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
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
     * 判断授权码是否过期
     * @param now 当前时间
     * @return 是否过期
     */
    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }
}
