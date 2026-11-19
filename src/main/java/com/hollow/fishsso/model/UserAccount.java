package com.hollow.fishsso.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/**
 * 用户账户模型
 */
@Entity
@Table(name = "sso_user")
public class UserAccount {

    @Id
    @Column(name = "id", length = 64, updatable = false)
    private String id;

    @Column(name = "preferred_username", nullable = false, unique = true, length = 128)
    private String preferredUsername;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /**
     * 构造函数。
     */
    public UserAccount() {
    }

    /**
     * 构造函数
     * @param id 用户ID
     * @param preferredUsername 用户名
     * @param passwordHash 密码哈希
     * @param name 显示名称
     * @param email 电子邮箱
     */
    public UserAccount(String id, String preferredUsername, String passwordHash, String name, String email) {
        this.id = id;
        this.preferredUsername = preferredUsername;
        this.passwordHash = passwordHash;
        this.name = name;
        this.email = email;
    }

    /**
     * 持久化前确保ID存在
     */
    @PrePersist
    public void ensureId() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }

    /**
     * 获取ID。
     * @return ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置ID。
     * @param id ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取用户名。
     * @return 用户名
     */
    public String getPreferredUsername() {
        return preferredUsername;
    }

    /**
     * 设置用户名。
     * @param preferredUsername 用户名
     */
    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    /**
     * 获取密码哈希。
     * @return 密码哈希
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置密码哈希。
     * @param passwordHash 密码哈希
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 获取显示名称。
     * @return 显示名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置显示名称。
     * @param name 显示名称
     */
    public void setName(String name) {
        this.name = name;
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
        if (!Objects.equals(this.email, email)) {
            this.emailVerified = false;
        }
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    /** 仅在完成邮箱所有权验证后设置为 true。 */
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified && email != null && !email.isBlank();
    }
}
