package com.hollow.fishsso.model;

import java.io.Serializable;
import java.util.Objects;

public class ClientUserAccessId implements Serializable {
    private String clientId;
    private String userId;

    public ClientUserAccessId() {
    }

    public ClientUserAccessId(String clientId, String userId) {
        this.clientId = clientId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ClientUserAccessId that)) return false;
        return Objects.equals(clientId, that.clientId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, userId);
    }
}
