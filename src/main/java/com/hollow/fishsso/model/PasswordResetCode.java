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

    /**
     * 构造函数。
     */
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

    /**
     * 获取用户名。
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
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
     * 获取邮箱地址。
     * @return 邮箱地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱地址。
     * @param email 邮箱地址
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取失败次数。
     * @return 失败次数
     */
    public int getFailureCount() {
        return failureCount;
    }

    /**
     * 设置失败次数。
     * @param failureCount 失败次数
     */
    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    /**
     * 获取created At。
     * @return created At
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置created At。
     * @param createdAt created At
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
}
