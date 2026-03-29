package com.hollow.fishsso.repository.redis;

import com.hollow.fishsso.model.AccessToken;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * 访问令牌Redis仓储接口
 */
public interface AccessTokenRedisRepository extends CrudRepository<AccessToken, String> {

    /**
     * 根据用户ID和客户端ID查询访问令牌
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @return 访问令牌列表
     */
    List<AccessToken> findByUserIdAndClientId(String userId, String clientId);
}
