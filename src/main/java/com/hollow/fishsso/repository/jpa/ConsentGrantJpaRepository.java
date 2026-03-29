package com.hollow.fishsso.repository.jpa;

import com.hollow.fishsso.model.ConsentGrant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 授权同意记录JPA仓储接口
 */
public interface ConsentGrantJpaRepository extends JpaRepository<ConsentGrant, Long> {

    /**
     * 根据用户ID和客户端ID查询授权记录
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @return 授权记录（可选）
     */
    Optional<ConsentGrant> findByUserIdAndClientId(String userId, String clientId);

    /**
     * 查询用户所有授权记录
     * @param userId 用户ID
     * @return 授权记录列表
     */
    List<ConsentGrant> findAllByUserId(String userId);

    /**
     * 删除用户对指定客户端的授权记录
     * @param userId 用户ID
     * @param clientId 客户端ID
     */
    void deleteByUserIdAndClientId(String userId, String clientId);
}
