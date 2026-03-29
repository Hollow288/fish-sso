package com.hollow.fishsso.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * 用户账户模型
 */
@Entity
@Table(name = "sso_user")
public class UserAccount {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "username", nullable = false, unique = true, length = 128)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "email", length = 255)
    private String email;

    /**
     * 构造函数。
     */
    public UserAccount() {
    }

    /**
     * 构造函数
     * @param id 用户ID
     * @param username 用户名
     * @param passwordHash 密码哈希
     * @param displayName 显示名称
     * @param email 电子邮箱
     */
    public UserAccount(String id, String username, String passwordHash, String displayName, String email) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
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
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 设置显示名称。
     * @param displayName 显示名称
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
}
