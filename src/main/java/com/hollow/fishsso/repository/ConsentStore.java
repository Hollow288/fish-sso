package com.hollow.fishsso.repository;

import com.hollow.fishsso.model.ConsentGrant;
import java.util.List;
import java.util.Optional;

/**
 * 授权同意记录存储接口
 */
public interface ConsentStore {

    /**
     * 查找授权同意记录
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @return 授权同意记录（可选）
     */
    Optional<ConsentGrant> find(String userId, String clientId);

    /**
     * 保存授权同意记录
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @param scopes 已同意权限范围
     */
    void save(String userId, String clientId, List<String> scopes);
}
