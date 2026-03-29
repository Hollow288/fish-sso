package com.hollow.fishsso.repository.redis;

import com.hollow.fishsso.model.RefreshToken;
import org.springframework.data.repository.CrudRepository;

/**
 * 刷新令牌Redis仓储
 */
public interface RefreshTokenRedisRepository extends CrudRepository<RefreshToken, String> {
}
