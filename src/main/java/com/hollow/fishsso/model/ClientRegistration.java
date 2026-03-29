package com.hollow.fishsso.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端注册模型
 */
@Entity
@Table(name = "sso_client")
public class ClientRegistration {

    @Id
    @Column(name = "client_id", length = 128)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false, length = 255)
    private String clientSecretHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sso_client_redirect_uri", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "redirect_uri", nullable = false, length = 512)
    private List<String> redirectUris = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sso_client_scope", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "scope", nullable = false, length = 128)
    private List<String> scopes = new ArrayList<>();

    /**
     * 构造函数。
     */
    public ClientRegistration() {
    }

    /**
     * 构造函数
     *
     * @param clientId         客户端ID
     * @param clientSecretHash 客户端密钥哈希
     * @param redirectUris     重定向URI列表
     * @param scopes           授权范围列表
     */
    public ClientRegistration(String clientId, String clientSecretHash, List<String> redirectUris, List<String> scopes) {
        this.clientId = clientId;
        this.clientSecretHash = clientSecretHash;
        this.redirectUris = redirectUris == null ? new ArrayList<>() : new ArrayList<>(redirectUris);
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
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
     * 获取客户端密钥哈希。
     * @return 客户端密钥哈希
     */
    public String getClientSecretHash() {
        return clientSecretHash;
    }

    /**
     * 设置客户端密钥哈希。
     * @param clientSecretHash 客户端密钥哈希
     */
    public void setClientSecretHash(String clientSecretHash) {
        this.clientSecretHash = clientSecretHash;
    }

    /**
     * 获取重定向 URI 列表。
     * @return 重定向 URI 列表
     */
    public List<String> getRedirectUris() {
        return redirectUris;
    }

    /**
     * 设置重定向 URI 列表。
     * @param redirectUris 重定向 URI 列表
     */
    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris == null ? new ArrayList<>() : new ArrayList<>(redirectUris);
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
}
