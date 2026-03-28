package com.hollow.fishsso.repository.redis;

import com.hollow.fishsso.model.PasswordResetCode;
import org.springframework.data.repository.CrudRepository;

/**
 * 密码重置验证码Redis仓储接口
 */
public interface PasswordResetCodeRedisRepository extends CrudRepository<PasswordResetCode, String> {
}
