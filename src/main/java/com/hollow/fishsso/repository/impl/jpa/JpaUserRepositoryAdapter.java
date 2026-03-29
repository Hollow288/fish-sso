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

    /**
     * 根据用户名查询用户信息。
     * @param username 用户名
     * @return 匹配的用户信息
     */
    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    /**
     * 根据 ID 查询数据。
     * @param id ID
     * @return 匹配的数据
     */
    @Override
    public Optional<UserAccount> findById(String id) {
        return repository.findById(id);
    }

    /**
     * 保存数据。
     * @param user 用户对象
     * @return 保存后的对象
     */
    @Override
    public UserAccount save(UserAccount user) {
        return repository.save(user);
    }
}
