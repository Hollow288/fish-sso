package com.hollow.fishsso.repository.impl.jpa;

import com.hollow.fishsso.model.ClientRegistration;
import com.hollow.fishsso.repository.jpa.ClientJpaRepository;
import com.hollow.fishsso.repository.ClientRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * JPA客户端仓储适配器
 */
@Repository
public class JpaClientRepositoryAdapter implements ClientRepository {

    private final ClientJpaRepository repository;

    /**
     * 构造函数
     * @param repository 客户端JPA仓储
     */
    public JpaClientRepositoryAdapter(ClientJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ClientRegistration> findByClientId(String clientId) {
        return repository.findByClientId(clientId);
    }
}
