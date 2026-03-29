package com.hollow.fishsso.repository;

import com.hollow.fishsso.model.AuthCode;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 授权码存储接口
 */
public interface AuthCodeStore {
    /**
     * 创建授权码
     * @param clientId 客户端ID
     * @param userId 用户ID
     * @param redirectUri 重定向URI
     * @param scopes 授权范围列表
     * @param ttl 生存时间
     * @return 授权码
     * @param nonce OIDC nonce 参数
     */
    AuthCode create(String clientId, String userId, String redirectUri, List<String> scopes, String nonce, Duration ttl);

    /**
     * 消费授权码（使用后删除）
     * @param code 授权码
     * @return 授权码（可选）
     */
    Optional<AuthCode> consume(String code);
}

