package com.hollow.fishsso.repository.jpa;

import com.hollow.fishsso.model.ClientRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 客户端JPA仓储接口
 */
public interface ClientJpaRepository extends JpaRepository<ClientRegistration, String> {
    /**
     * 根据客户端ID查找客户端
     * @param clientId 客户端ID
     * @return 客户端注册信息（可选）
     */
    Optional<ClientRegistration> findByClientId(String clientId);
}
