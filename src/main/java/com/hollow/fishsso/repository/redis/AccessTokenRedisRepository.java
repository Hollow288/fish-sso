package com.hollow.fishsso.repository.redis;

import com.hollow.fishsso.model.AccessToken;
import org.springframework.data.repository.CrudRepository;

/**
 * 访问令牌Redis仓储接口
 */
public interface AccessTokenRedisRepository extends CrudRepository<AccessToken, String> {
}
