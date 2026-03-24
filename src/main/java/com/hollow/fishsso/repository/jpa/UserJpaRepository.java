package com.hollow.fishsso.repository.jpa;

import com.hollow.fishsso.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户JPA仓储接口
 */
public interface UserJpaRepository extends JpaRepository<UserAccount, String> {
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户账户（可选）
     */
    Optional<UserAccount> findByUsername(String username);
}
