package com.hollow.fishsso.repository;

import com.hollow.fishsso.model.UserAccount;
import java.util.Optional;

/**
 * 用户仓储接口
 */
public interface UserRepository {
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户账户（可选）
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * 根据用户ID查找用户
     * @param id 用户ID
     * @return 用户账户（可选）
     */
    Optional<UserAccount> findById(String id);

    /**
     * 保存用户账户
     * @param user 用户账户
     * @return 保存后的用户账户
     */
    UserAccount save(UserAccount user);
}

