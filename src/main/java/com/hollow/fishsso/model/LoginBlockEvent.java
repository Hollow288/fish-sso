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

    public LoginBlockEvent() {
    }

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

    @PrePersist
    public void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getFailures() {
        return failures;
    }

    public void setFailures(Long failures) {
        this.failures = failures;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public Long getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public Long getBlockDurationSeconds() {
        return blockDurationSeconds;
    }

    public void setBlockDurationSeconds(Long blockDurationSeconds) {
        this.blockDurationSeconds = blockDurationSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
