package com.hollow.fishsso.repository.impl.jpa;

import com.hollow.fishsso.model.UserAccount;
import com.hollow.fishsso.repository.jpa.UserJpaRepository;
import com.hollow.fishsso.repository.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * JPA用户仓储适配器
 */
@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository repository;

    /**
     * 构造函数
     * @param repository 用户JPA仓储
     */
    public JpaUserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    public Optional<UserAccount> findById(String id) {
        return repository.findById(id);
    }
}
