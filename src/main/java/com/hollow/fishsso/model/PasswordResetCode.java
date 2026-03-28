package com.hollow.fishsso.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * 密码重置验证码模型
 */
@RedisHash("sso:pwd_reset")
public class PasswordResetCode {

    @Id
    private String username;
    private String code;
    private String email;
    private int failureCount;
    private Instant createdAt;
    @TimeToLive
    private Long ttlSeconds;

    public PasswordResetCode() {
    }

    /**
     * 构造函数
     * @param username 用户名
     * @param code 验证码
     * @param email 邮箱
     * @param createdAt 创建时间
     * @param ttlSeconds TTL秒数
     */
    public PasswordResetCode(String username, String code, String email, Instant createdAt, Long ttlSeconds) {
        this.username = username;
        this.code = code;
        this.email = email;
        this.failureCount = 0;
        this.createdAt = createdAt;
        this.ttlSeconds = ttlSeconds;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
