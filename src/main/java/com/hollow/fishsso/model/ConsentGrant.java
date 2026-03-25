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

    @PrePersist
    @PreUpdate
    public void ensureUpdatedAt() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
