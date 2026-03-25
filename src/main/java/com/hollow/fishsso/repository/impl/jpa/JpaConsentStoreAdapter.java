package com.hollow.fishsso.repository.impl.jpa;

import com.hollow.fishsso.model.ConsentGrant;
import com.hollow.fishsso.repository.ConsentStore;
import com.hollow.fishsso.repository.jpa.ConsentGrantJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * JPA授权同意记录存储适配器
 */
@Repository
public class JpaConsentStoreAdapter implements ConsentStore {

    private final ConsentGrantJpaRepository repository;

    /**
     * 构造函数
     * @param repository 授权同意记录JPA仓储
     */
    public JpaConsentStoreAdapter(ConsentGrantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ConsentGrant> find(String userId, String clientId) {
        return repository.findByUserIdAndClientId(userId, clientId);
    }

    @Override
    public void save(String userId, String clientId, List<String> scopes) {
        ConsentGrant consentGrant = repository.findByUserIdAndClientId(userId, clientId)
                .orElseGet(ConsentGrant::new);
        consentGrant.setUserId(userId);
        consentGrant.setClientId(clientId);
        consentGrant.setScopes(scopes);
        consentGrant.setUpdatedAt(Instant.now());
        repository.save(consentGrant);
    }
}
