package com.hollow.fishsso.repository;

import com.hollow.fishsso.model.SessionInfo;
import java.time.Duration;
import java.util.Optional;

/**
 * 会话存储接口
 */
public interface SessionStore {
    /**
     * 创建会话
     * @param userId 用户ID
     * @param ttl 生存时间
     * @return 会话信息
     */
    SessionInfo create(String userId, Duration ttl);

    /**
     * 查找会话
     * @param sessionId 会话ID
     * @return 会话信息（可选）
     */
    Optional<SessionInfo> find(String sessionId);

    /**
     * 删除会话
     * @param sessionId 会话ID
     */
    void delete(String sessionId);
}

