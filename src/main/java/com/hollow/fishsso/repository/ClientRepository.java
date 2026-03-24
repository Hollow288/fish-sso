package com.hollow.fishsso.repository;

import com.hollow.fishsso.model.ClientRegistration;
import java.util.Optional;

/**
 * 客户端仓储接口
 */
public interface ClientRepository {
    /**
     * 根据客户端ID查找客户端
     * @param clientId 客户端ID
     * @return 客户端注册信息（可选）
     */
    Optional<ClientRegistration> findByClientId(String clientId);
}

