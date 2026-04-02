package com.hollow.fishsso.repository.redis;

import com.hollow.fishsso.model.SessionInfo;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * 会话Redis仓储接口
 */
public interface SessionRedisRepository extends CrudRepository<SessionInfo, String> {

    /**
     * 根据用户 ID 查询会话列表
     * @param userId 用户 ID
     * @return 会话列表
     */
    List<SessionInfo> findByUserId(String userId);
}
