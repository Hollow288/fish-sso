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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public void setClientSecretHash(String clientSecretHash) {
        this.clientSecretHash = clientSecretHash;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris == null ? new ArrayList<>() : new ArrayList<>(redirectUris);
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
    }
}
