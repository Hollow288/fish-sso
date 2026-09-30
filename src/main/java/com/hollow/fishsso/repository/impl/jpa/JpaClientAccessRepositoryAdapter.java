package com.hollow.fishsso.repository.impl.jpa;

import com.hollow.fishsso.repository.ClientAccessRepository;
import com.hollow.fishsso.repository.jpa.ClientUserAccessJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaClientAccessRepositoryAdapter implements ClientAccessRepository {
    private final ClientUserAccessJpaRepository repository;

    public JpaClientAccessRepositoryAdapter(ClientUserAccessJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isUserAllowed(String clientId, String userId) {
        return repository.existsByClientIdAndUserId(clientId, userId);
    }
}
