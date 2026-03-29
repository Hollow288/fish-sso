package com.hollow.fishsso.repository.impl.jpa;

import com.hollow.fishsso.model.ConsentGrant;
import com.hollow.fishsso.repository.ConsentStore;
import com.hollow.fishsso.repository.jpa.ConsentGrantJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA授权同意记录存储适配器
 */
@Repository
public class JpaConsentStoreAdapter implements ConsentStore {

    private final ConsentGrantJpaRepository repository;

    /**
     * 构造函数
     * @param repository 授权同意记录JPA仓储
     */
    public JpaConsentStoreAdapter(ConsentGrantJpaRepository repository) {
        this.repository = repository;
    }

    /**
     * 查询指定数据。
     * @param userId 用户 ID
     * @param clientId 客户端 ID
     * @return 查询结果
     */
    @Override
    public Optional<ConsentGrant> find(String userId, String clientId) {
        return repository.findByUserIdAndClientId(userId, clientId);
    }

    /**
     * 查询用户所有授权同意记录
     * @param userId 用户ID
     * @return 授权同意记录列表
     */
    @Override
    public List<ConsentGrant> findAllByUserId(String userId) {
        return repository.findAllByUserId(userId);
    }

    /**
     * 保存数据。
     * @param userId 用户 ID
     * @param clientId 客户端 ID
     * @param scopes 授权范围列表
     */
    @Override
    public void save(String userId, String clientId, List<String> scopes) {
        ConsentGrant consentGrant = repository.findByUserIdAndClientId(userId, clientId)
                .orElseGet(ConsentGrant::new);
        consentGrant.setUserId(userId);
        consentGrant.setClientId(clientId);
        consentGrant.setScopes(scopes);
        consentGrant.setUpdatedAt(Instant.now());
        repository.save(consentGrant);
    }

    /**
     * 删除用户对指定客户端的授权同意记录
     * @param userId 用户ID
     * @param clientId 客户端ID
     */
    @Override
    @Transactional
    public void delete(String userId, String clientId) {
        repository.deleteByUserIdAndClientId(userId, clientId);
    }
}
