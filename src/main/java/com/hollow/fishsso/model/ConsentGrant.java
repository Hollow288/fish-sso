package com.hollow.fishsso.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户授权同意记录模型
 */
@Entity
@Table(
        name = "sso_consent_grant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sso_consent_user_client",
                columnNames = {"user_id", "client_id"}
        )
)
public class ConsentGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sso_consent_grant_scope", joinColumns = @JoinColumn(name = "consent_id"))
    @Column(name = "scope", nullable = false, length = 128)
    private List<String> scopes = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 构造函数。
     */
    public ConsentGrant() {
    }

    /**
     * 构造函数
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @param scopes 已同意权限范围
     * @param updatedAt 更新时间
     */
    public ConsentGrant(String userId, String clientId, List<String> scopes, Instant updatedAt) {
        this.userId = userId;
        this.clientId = clientId;
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
        this.updatedAt = updatedAt;
    }

    /**
     * 确保更新时间已初始化。
     */
    @PrePersist
    @PreUpdate
    public void ensureUpdatedAt() {
        updatedAt = Instant.now();
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
     * 获取更新时间。
     * @return 更新时间
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
