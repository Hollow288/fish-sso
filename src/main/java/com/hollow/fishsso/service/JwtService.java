package com.hollow.fishsso.service;

import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.model.UserAccount;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * JWT密钥管理与签发服务
 */
@Service
public class JwtService {

    private static final String DEFAULT_KEY_STORE_PATH = "keys/sso-jwt-keys.properties";
    private static final Duration DEFAULT_ROTATION_PERIOD = Duration.ofDays(30);
    private static final Duration DEFAULT_RETENTION_PADDING = Duration.ofMinutes(5);

    private static final String CURRENT_KID_KEY = "current.kid";
    private static final String CURRENT_PRIVATE_KEY = "current.private-key";
    private static final String CURRENT_PUBLIC_KEY = "current.public-key";
    private static final String CURRENT_CREATED_AT = "current.created-at-epoch";
    private static final String PREVIOUS_KID_KEY = "previous.kid";
    private static final String PREVIOUS_PUBLIC_KEY = "previous.public-key";
    private static final String PREVIOUS_RETIRED_AT = "previous.retired-at-epoch";

    private final SsoProperties properties;

    private volatile RSAPrivateKey privateKey;
    private volatile String kid;
    private volatile Map<String, RSAPublicKey> verificationKeys = Map.of();
    private volatile List<JWK> jwks = List.of();

    /**
     * 构造函数。
     * @param properties SSO 配置属性
     */
    public JwtService(SsoProperties properties) {
        this.properties = properties;
    }

    /**
     * 初始化 JWT 密钥材料（支持本地持久化与按周期轮换）。
     */
    @PostConstruct
    synchronized void init() {
        try {
            Path keyStorePath = resolveKeyStorePath();
            KeyState state = loadState(keyStorePath);
            Instant now = Instant.now();
            boolean changed = false;

            if (state.current == null) {
                state.current = generateCurrentKey(now);
                changed = true;
            }
            if (rotateIfDue(state, now)) {
                changed = true;
            }
            if (cleanupExpiredPreviousKey(state, now)) {
                changed = true;
            }
            if (changed) {
                saveState(keyStorePath, state);
            }
            applyState(state);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 JWT 密钥失败", e);
        }
    }

    /**
     * 签发 access_token JWT
     * @param userId 用户 ID
     * @param clientId 客户端 ID
     * @param scopes 授权范围列表
     * @return 访问令牌
     */
    public String generateAccessToken(String userId, String clientId, List<String> scopes) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getAccessTokenTtl());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.getIssuer())
                .subject(userId)
                .audience(clientId)
                .claim("scope", String.join(" ", scopes))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .build();
        return sign(claims);
    }

    /**
     * 签发 id_token JWT
     * @param userId 用户 ID
     * @param clientId 客户端 ID
     * @param scopes 授权范围列表
     * @param nonce OIDC nonce 参数
     * @param user 用户对象
     * @return ID Token
     */
    public String generateIdToken(String userId, String clientId, List<String> scopes,
                                  String nonce, UserAccount user) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getIdTokenTtl());
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(properties.getIssuer())
                .subject(userId)
                .audience(clientId)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("auth_time", now.getEpochSecond());

        if (nonce != null && !nonce.isBlank()) {
            builder.claim("nonce", nonce);
        }

        if (containsScope(scopes, "profile")) {
            if (user.getDisplayName() != null) {
                builder.claim("name", user.getDisplayName());
            }
            builder.claim("preferred_username", user.getUsername());
        }
        if (containsScope(scopes, "email")) {
            if (user.getEmail() != null) {
                builder.claim("email", user.getEmail());
            }
        }

        return sign(builder.build());
    }

    /**
     * 解析并验证 JWT
     * @param token 令牌
     * @return JWT 声明集
     */
    public JWTClaimsSet parseAndVerify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            RSAPublicKey verifyKey = resolveVerifyKey(jwt.getHeader().getKeyID());
            if (verifyKey == null) {
                return null;
            }
            if (!jwt.verify(new RSASSAVerifier(verifyKey))) {
                return null;
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date now = new Date();
            if (claims.getExpirationTime() != null && claims.getExpirationTime().before(now)) {
                return null;
            }
            if (claims.getNotBeforeTime() != null && claims.getNotBeforeTime().after(now)) {
                return null;
            }
            if (!properties.getIssuer().equals(claims.getIssuer())) {
                return null;
            }
            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 返回JWKS（公钥集合）的 Map 表示
     * @return JWK 集合
     */
    public Map<String, Object> getJwkSet() {
        JWKSet jwkSet = new JWKSet(jwks);
        return jwkSet.toJSONObject();
    }

    /**
     * 对 JWT 声明集进行签名。
     * @param claims JWT 声明集
     * @return 签名后的 JWT 字符串
     */
    private String sign(JWTClaimsSet claims) {
        try {
            RSAPrivateKey signingKey = privateKey;
            if (signingKey == null || !StringUtils.hasText(kid)) {
                throw new IllegalStateException("JWT 签名密钥未初始化");
            }
            JWSSigner signer = new RSASSASigner(signingKey);
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(kid)
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("JWT签名失败", e);
        }
    }

    /**
     * 解析持久化的密钥状态。
     * @param keyStorePath 密钥文件路径
     * @return 密钥状态
     * @throws IOException 读取失败
     */
    private KeyState loadState(Path keyStorePath) throws IOException {
        if (!Files.exists(keyStorePath)) {
            return new KeyState();
        }
        Properties props = new Properties();
        try (InputStream inputStream = Files.newInputStream(keyStorePath)) {
            props.load(inputStream);
        }
        KeyState state = new KeyState();
        state.current = parseCurrentKey(props);
        state.previous = parsePreviousKey(props);
        return state;
    }

    /**
     * 保存密钥状态到本地文件。
     * @param keyStorePath 密钥文件路径
     * @param state 密钥状态
     * @throws IOException 写入失败
     */
    private void saveState(Path keyStorePath, KeyState state) throws IOException {
        Path parent = keyStorePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Properties props = new Properties();
        props.setProperty(CURRENT_KID_KEY, state.current.kid);
        props.setProperty(CURRENT_PRIVATE_KEY, state.current.privateKeyBase64);
        props.setProperty(CURRENT_PUBLIC_KEY, state.current.publicKeyBase64);
        props.setProperty(CURRENT_CREATED_AT, String.valueOf(state.current.createdAtEpoch));

        if (state.previous != null) {
            props.setProperty(PREVIOUS_KID_KEY, state.previous.kid);
            props.setProperty(PREVIOUS_PUBLIC_KEY, state.previous.publicKeyBase64);
            props.setProperty(PREVIOUS_RETIRED_AT, String.valueOf(state.previous.retiredAtEpoch));
        }

        try (OutputStream outputStream = Files.newOutputStream(keyStorePath)) {
            props.store(outputStream, "Fish SSO JWT keys");
        }
    }

    /**
     * 应用密钥状态到内存。
     * @param state 密钥状态
     */
    private void applyState(KeyState state) {
        if (state.current == null) {
            throw new IllegalStateException("缺少 current JWT key");
        }

        RSAPrivateKey currentPrivate = decodePrivateKey(state.current.privateKeyBase64);
        RSAPublicKey currentPublic = decodePublicKey(state.current.publicKeyBase64);

        Map<String, RSAPublicKey> keys = new LinkedHashMap<>();
        List<JWK> jwkList = new ArrayList<>();

        keys.put(state.current.kid, currentPublic);
        jwkList.add(buildPublicJwk(currentPublic, state.current.kid));

        if (state.previous != null) {
            RSAPublicKey previousPublic = decodePublicKey(state.previous.publicKeyBase64);
            keys.put(state.previous.kid, previousPublic);
            jwkList.add(buildPublicJwk(previousPublic, state.previous.kid));
        }

        this.privateKey = currentPrivate;
        this.kid = state.current.kid;
        this.verificationKeys = Map.copyOf(keys);
        this.jwks = List.copyOf(jwkList);
    }

    /**
     * 若达到轮换周期则执行密钥轮换。
     * @param state 密钥状态
     * @param now 当前时间
     * @return 是否发生变更
     */
    private boolean rotateIfDue(KeyState state, Instant now) {
        if (state.current == null) {
            return false;
        }
        Duration rotationPeriod = resolveRotationPeriod();
        Instant createdAt = Instant.ofEpochSecond(state.current.createdAtEpoch);
        if (createdAt.plus(rotationPeriod).isAfter(now)) {
            return false;
        }

        state.previous = new PreviousKeyRecord(
                state.current.kid,
                state.current.publicKeyBase64,
                now.getEpochSecond()
        );
        state.current = generateCurrentKey(now);
        return true;
    }

    /**
     * 清理超过保留期的旧密钥。
     * @param state 密钥状态
     * @param now 当前时间
     * @return 是否发生变更
     */
    private boolean cleanupExpiredPreviousKey(KeyState state, Instant now) {
        if (state.previous == null) {
            return false;
        }
        Duration retention = resolvePreviousKeyRetention();
        Instant retiredAt = Instant.ofEpochSecond(state.previous.retiredAtEpoch);
        if (retiredAt.plus(retention).isAfter(now)) {
            return false;
        }
        state.previous = null;
        return true;
    }

    /**
     * 生成新的当前密钥记录。
     * @param now 当前时间
     * @return 当前密钥记录
     */
    private CurrentKeyRecord generateCurrentKey(Instant now) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            RSAPrivateKey generatedPrivate = (RSAPrivateKey) keyPair.getPrivate();
            RSAPublicKey generatedPublic = (RSAPublicKey) keyPair.getPublic();
            return new CurrentKeyRecord(
                    generateKid(generatedPublic),
                    encodeKey(generatedPrivate.getEncoded()),
                    encodeKey(generatedPublic.getEncoded()),
                    now.getEpochSecond()
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("生成 JWT 密钥失败", e);
        }
    }

    /**
     * 解析当前密钥配置。
     * @param props 配置
     * @return 当前密钥记录
     */
    private CurrentKeyRecord parseCurrentKey(Properties props) {
        String currentKid = trimToNull(props.getProperty(CURRENT_KID_KEY));
        String currentPrivate = trimToNull(props.getProperty(CURRENT_PRIVATE_KEY));
        String currentPublic = trimToNull(props.getProperty(CURRENT_PUBLIC_KEY));

        if (currentKid == null && currentPrivate == null && currentPublic == null) {
            return null;
        }
        if (currentKid == null || currentPrivate == null || currentPublic == null) {
            throw new IllegalStateException("JWT 密钥文件损坏：current key 配置不完整");
        }

        long createdAtEpoch = parseEpoch(props.getProperty(CURRENT_CREATED_AT), Instant.now().getEpochSecond());
        return new CurrentKeyRecord(currentKid, currentPrivate, currentPublic, createdAtEpoch);
    }

    /**
     * 解析旧密钥配置。
     * @param props 配置
     * @return 旧密钥记录
     */
    private PreviousKeyRecord parsePreviousKey(Properties props) {
        String previousKid = trimToNull(props.getProperty(PREVIOUS_KID_KEY));
        String previousPublic = trimToNull(props.getProperty(PREVIOUS_PUBLIC_KEY));
        String retiredEpochText = trimToNull(props.getProperty(PREVIOUS_RETIRED_AT));

        if (previousKid == null && previousPublic == null && retiredEpochText == null) {
            return null;
        }
        if (previousKid == null || previousPublic == null) {
            throw new IllegalStateException("JWT 密钥文件损坏：previous key 配置不完整");
        }

        long retiredAt = parseEpoch(retiredEpochText, Instant.now().getEpochSecond());
        return new PreviousKeyRecord(previousKid, previousPublic, retiredAt);
    }

    /**
     * 根据 kid 解析验证公钥。
     * @param keyId 令牌头中的 kid
     * @return 验证公钥
     */
    private RSAPublicKey resolveVerifyKey(String keyId) {
        if (StringUtils.hasText(keyId)) {
            return verificationKeys.get(keyId);
        }
        return verificationKeys.get(kid);
    }

    /**
     * 计算轮换周期。
     * @return 轮换周期
     */
    private Duration resolveRotationPeriod() {
        SsoProperties.Jwt jwtConfig = properties.getJwt();
        if (jwtConfig == null || jwtConfig.getRotationPeriod() == null) {
            return DEFAULT_ROTATION_PERIOD;
        }
        return jwtConfig.getRotationPeriod();
    }

    /**
     * 计算旧密钥保留时长。
     * @return 保留时长
     */
    private Duration resolvePreviousKeyRetention() {
        SsoProperties.Jwt jwtConfig = properties.getJwt();
        if (jwtConfig != null && jwtConfig.getPreviousKeyRetention() != null) {
            return jwtConfig.getPreviousKeyRetention();
        }
        Duration accessTtl = properties.getAccessTokenTtl();
        Duration idTtl = properties.getIdTokenTtl();
        Duration maxTtl = accessTtl.compareTo(idTtl) >= 0 ? accessTtl : idTtl;
        return maxTtl.plus(DEFAULT_RETENTION_PADDING);
    }

    /**
     * 解析密钥存储路径。
     * @return 路径
     */
    private Path resolveKeyStorePath() {
        String configured = properties.getJwt() == null ? null : properties.getJwt().getKeyStorePath();
        String path = StringUtils.hasText(configured) ? configured.trim() : DEFAULT_KEY_STORE_PATH;
        return Paths.get(path).toAbsolutePath().normalize();
    }

    /**
     * 解码 RSA 私钥。
     * @param encodedBase64 Base64 字符串
     * @return RSA 私钥
     */
    private RSAPrivateKey decodePrivateKey(String encodedBase64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encodedBase64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("JWT 私钥解析失败", e);
        }
    }

    /**
     * 解码 RSA 公钥。
     * @param encodedBase64 Base64 字符串
     * @return RSA 公钥
     */
    private RSAPublicKey decodePublicKey(String encodedBase64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encodedBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("JWT 公钥解析失败", e);
        }
    }

    /**
     * 构造公开 JWK。
     * @param publicKey 公钥
     * @param keyId key id
     * @return JWK
     */
    private JWK buildPublicJwk(RSAPublicKey publicKey, String keyId) {
        return new RSAKey.Builder(publicKey)
                .keyID(keyId)
                .algorithm(JWSAlgorithm.RS256)
                .build();
    }

    /**
     * Base64 编码密钥字节。
     * @param keyBytes 密钥字节
     * @return 编码字符串
     */
    private String encodeKey(byte[] keyBytes) {
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    /**
     * 解析 epoch 秒配置。
     * @param value 原始值
     * @param defaultValue 默认值
     * @return epoch 秒
     */
    private long parseEpoch(String value, long defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 去除空白并处理空值。
     * @param value 原始字符串
     * @return 处理后字符串
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 判断授权范围列表中是否包含目标范围。
     * @param scopes 授权范围列表
     * @param target 目标值
     * @return 是否包含目标范围
     */
    private boolean containsScope(List<String> scopes, String target) {
        return scopes.stream().anyMatch(s -> s.equalsIgnoreCase(target));
    }

    /**
     * 生成密钥 ID。
     * @param publicKey RSA 公钥
     * @return 密钥 ID
     */
    private static String generateKid(RSAPublicKey publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
    }

    /**
     * 密钥状态容器。
     */
    private static final class KeyState {
        private CurrentKeyRecord current;
        private PreviousKeyRecord previous;
    }

    /**
     * 当前密钥记录。
     */
    private static final class CurrentKeyRecord {
        private final String kid;
        private final String privateKeyBase64;
        private final String publicKeyBase64;
        private final long createdAtEpoch;

        private CurrentKeyRecord(String kid, String privateKeyBase64, String publicKeyBase64, long createdAtEpoch) {
            this.kid = kid;
            this.privateKeyBase64 = privateKeyBase64;
            this.publicKeyBase64 = publicKeyBase64;
            this.createdAtEpoch = createdAtEpoch;
        }
    }

    /**
     * 旧密钥记录。
     */
    private static final class PreviousKeyRecord {
        private final String kid;
        private final String publicKeyBase64;
        private final long retiredAtEpoch;

        private PreviousKeyRecord(String kid, String publicKeyBase64, long retiredAtEpoch) {
            this.kid = kid;
            this.publicKeyBase64 = publicKeyBase64;
            this.retiredAtEpoch = retiredAtEpoch;
        }
    }
}
