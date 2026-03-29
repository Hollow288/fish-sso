package com.hollow.fishsso.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "sso_login_block_event")
public class LoginBlockEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "dimension", nullable = false, length = 32)
    private String dimension;

    @Column(name = "account", nullable = false, length = 128)
    private String account;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "user_agent", nullable = false, length = 256)
    private String userAgent;

    @Column(name = "failures", nullable = false)
    private Long failures;

    @Column(name = "threshold_value", nullable = false)
    private Integer threshold;

    @Column(name = "window_seconds", nullable = false)
    private Long windowSeconds;

    @Column(name = "block_duration_seconds", nullable = false)
    private Long blockDurationSeconds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 构造函数。
     */
    public LoginBlockEvent() {
    }

    /**
     * 构造函数。
     * @param dimension 维度标识
     * @param account 账号
     * @param ipAddress IP 地址
     * @param userAgent 用户代理
     * @param failures 失败次数
     * @param threshold 阈值
     * @param windowSeconds 统计窗口（秒）
     * @param blockDurationSeconds 封禁时长（秒）
     */
    public LoginBlockEvent(String dimension,
                           String account,
                           String ipAddress,
                           String userAgent,
                           Long failures,
                           Integer threshold,
                           Long windowSeconds,
                           Long blockDurationSeconds) {
        this.dimension = dimension;
        this.account = account;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.failures = failures;
        this.threshold = threshold;
        this.windowSeconds = windowSeconds;
        this.blockDurationSeconds = blockDurationSeconds;
    }

    /**
     * 根据输入参数创建实例。
     * @param dimension 维度标识
     * @param account 账号
     * @param ipAddress IP 地址
     * @param userAgent 用户代理
     * @param failures 失败次数
     * @param threshold 阈值
     * @param window 统计窗口
     * @param blockDuration 封禁时长
     * @return 创建后的实例
     */
    public static LoginBlockEvent of(String dimension,
                                     String account,
                                     String ipAddress,
                                     String userAgent,
                                     long failures,
                                     int threshold,
                                     Duration window,
                                     Duration blockDuration) {
        return new LoginBlockEvent(
                dimension,
                account,
                ipAddress,
                userAgent,
                failures,
                threshold,
                window.getSeconds(),
                blockDuration.getSeconds()
        );
    }

    /**
     * 确保创建时间已初始化。
     */
    @PrePersist
    public void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * 获取ID。
     * @return ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置ID。
     * @param id ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取维度标识。
     * @return 维度标识
     */
    public String getDimension() {
        return dimension;
    }

    /**
     * 设置维度标识。
     * @param dimension 维度标识
     */
    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    /**
     * 获取账号。
     * @return 账号
     */
    public String getAccount() {
        return account;
    }

    /**
     * 设置账号。
     * @param account 账号
     */
    public void setAccount(String account) {
        this.account = account;
    }

    /**
     * 获取IP 地址。
     * @return IP 地址
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * 设置IP 地址。
     * @param ipAddress IP 地址
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * 获取用户代理。
     * @return 用户代理
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * 设置用户代理。
     * @param userAgent 用户代理
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * 获取失败次数。
     * @return 失败次数
     */
    public Long getFailures() {
        return failures;
    }

    /**
     * 设置失败次数。
     * @param failures 失败次数
     */
    public void setFailures(Long failures) {
        this.failures = failures;
    }

    /**
     * 获取阈值。
     * @return 阈值
     */
    public Integer getThreshold() {
        return threshold;
    }

    /**
     * 设置阈值。
     * @param threshold 阈值
     */
    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    /**
     * 获取统计窗口（秒）。
     * @return 统计窗口（秒）
     */
    public Long getWindowSeconds() {
        return windowSeconds;
    }

    /**
     * 设置统计窗口（秒）。
     * @param windowSeconds 统计窗口（秒）
     */
    public void setWindowSeconds(Long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    /**
     * 获取封禁时长（秒）。
     * @return 封禁时长（秒）
     */
    public Long getBlockDurationSeconds() {
        return blockDurationSeconds;
    }

    /**
     * 设置封禁时长（秒）。
     * @param blockDurationSeconds 封禁时长（秒）
     */
    public void setBlockDurationSeconds(Long blockDurationSeconds) {
        this.blockDurationSeconds = blockDurationSeconds;
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
}
