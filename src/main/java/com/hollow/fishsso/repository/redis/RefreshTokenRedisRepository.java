package com.hollow.fishsso.repository.redis;

import com.hollow.fishsso.model.RefreshToken;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * 刷新令牌Redis仓储
 */
public interface RefreshTokenRedisRepository extends CrudRepository<RefreshToken, String> {

    /**
     * 根据用户ID和客户端ID查询刷新令牌
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @return 刷新令牌列表
     */
    List<RefreshToken> findByUserIdAndClientId(String userId, String clientId);
}
