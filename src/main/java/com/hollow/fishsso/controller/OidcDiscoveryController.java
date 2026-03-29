package com.hollow.fishsso.controller;

import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.service.JwtService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OIDC Discovery 与 JWKS 端点
 */
@RestController
public class OidcDiscoveryController {

    private final SsoProperties properties;
    private final JwtService jwtService;

    /**
     * 构造函数
     * @param properties SSO配置属性
     * @param jwtService JWT服务
     */
    public OidcDiscoveryController(SsoProperties properties, JwtService jwtService) {
        this.properties = properties;
        this.jwtService = jwtService;
    }

    /**
     * OIDC Provider Metadata（RFC 8414 / OpenID Connect Discovery 1.0）
     * @return 包含发现端点信息的Map
     */
    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> openidConfiguration() {
        String issuer = properties.getIssuer();
        return Map.ofEntries(
                Map.entry("issuer", issuer),
                Map.entry("authorization_endpoint", issuer + "/sso/authorize"),
                Map.entry("token_endpoint", issuer + "/sso/token"),
                Map.entry("userinfo_endpoint", issuer + "/sso/userinfo"),
                Map.entry("jwks_uri", issuer + "/sso/jwks"),
                Map.entry("revocation_endpoint", issuer + "/sso/revoke"),
                Map.entry("end_session_endpoint", issuer + "/sso/logout"),
                Map.entry("scopes_supported", List.of("openid", "profile", "email")),
                Map.entry("response_types_supported", List.of("code")),
                Map.entry("grant_types_supported", List.of("authorization_code", "refresh_token")),
                Map.entry("subject_types_supported", List.of("public")),
                Map.entry("id_token_signing_alg_values_supported", List.of("RS256")),
                Map.entry("token_endpoint_auth_methods_supported", List.of("client_secret_post"))
        );
    }

    /**
     * JSON Web Key Set（RFC 7517），返回公钥集合
     * @return JWK Set的Map表示
     */
    @GetMapping("/sso/jwks")
    public Map<String, Object> jwks() {
        return jwtService.getJwkSet();
    }
}
