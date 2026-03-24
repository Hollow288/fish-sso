package com.hollow.fishsso.repository.redis;

import com.hollow.fishsso.model.SessionInfo;
import org.springframework.data.repository.CrudRepository;

/**
 * 会话Redis仓储接口
 */
public interface SessionRedisRepository extends CrudRepository<SessionInfo, String> {
}
