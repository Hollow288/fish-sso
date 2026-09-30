package com.hollow.fishsso.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** 客户端允许访问的 SSO 用户，使用稳定的用户 ID。 */
@Entity
@IdClass(ClientUserAccessId.class)
@Table(name = "sso_client_user_access")
public class ClientUserAccess {
    @Id
    @Column(name = "client_id", length = 128)
    private String clientId;

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    public ClientUserAccess() {
    }

    public ClientUserAccess(String clientId, String userId) {
        this.clientId = clientId;
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUserId() {
        return userId;
    }
}
