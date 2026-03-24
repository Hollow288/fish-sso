package com.hollow.fishsso.repository.redis;

import com.hollow.fishsso.model.AuthCode;
import org.springframework.data.repository.CrudRepository;

/**
 * 授权码Redis仓储接口
 */
public interface AuthCodeRedisRepository extends CrudRepository<AuthCode, String> {
}
