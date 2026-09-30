package com.hollow.fishsso.repository;

public interface ClientAccessRepository {
    boolean isUserAllowed(String clientId, String userId);
}
