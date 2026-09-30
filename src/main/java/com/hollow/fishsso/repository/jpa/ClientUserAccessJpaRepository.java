package com.hollow.fishsso.repository.jpa;

import com.hollow.fishsso.model.ClientUserAccess;
import com.hollow.fishsso.model.ClientUserAccessId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientUserAccessJpaRepository extends JpaRepository<ClientUserAccess, ClientUserAccessId> {
    boolean existsByClientIdAndUserId(String clientId, String userId);
}
