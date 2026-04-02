package com.hollow.fishsso.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * SSO配置属性。
 */
@Component
@ConfigurationProperties(prefix = "app.sso")
public class SsoProperties {

    private String issuer;
    private Duration sessionTtl;
    private Duration authCodeTtl;
    private Duration accessTokenTtl;
    private Duration idTokenTtl;
    private Duration refreshTokenTtl;
    private Jwt jwt = new Jwt();
    private Cookie cookie = new Cookie();
    private LoginProtection loginProtection;
    private PasswordReset passwordReset;
    private List<String> allowedReturnToPrefixes = new ArrayList<>(List.of("/consent"));

    /**
     * 校验必须配置项。
     */
    @PostConstruct
    void validateRequiredProperties() {
        Assert.isTrue(StringUtils.hasText(issuer), "Missing required property: app.sso.issuer");
        Assert.notNull(sessionTtl, "Missing required property: app.sso.session-ttl");
        Assert.notNull(authCodeTtl, "Missing required property: app.sso.auth-code-ttl");
        Assert.notNull(accessTokenTtl, "Missing required property: app.sso.access-token-ttl");
        Assert.notNull(idTokenTtl, "Missing required property: app.sso.id-token-ttl");
        Assert.notNull(refreshTokenTtl, "Missing required property: app.sso.refresh-token-ttl");
        Assert.notNull(jwt, "Missing required property group: app.sso.jwt");
        Assert.isTrue(StringUtils.hasText(jwt.getKeyStorePath()),
                "Missing required property: app.sso.jwt.key-store-path");
        Assert.notNull(cookie, "Missing required property group: app.sso.cookie");
        Assert.isTrue(StringUtils.hasText(cookie.getSameSite()),
                "Missing required property: app.sso.cookie.same-site");
        Assert.notEmpty(allowedReturnToPrefixes,
                "Missing required property: app.sso.allowed-return-to-prefixes");
        allowedReturnToPrefixes.forEach(this::validateReturnToPrefix);
        if (jwt.getRotationPeriod() != null) {
            Assert.isTrue(!jwt.getRotationPeriod().isNegative() && !jwt.getRotationPeriod().isZero(),
                    "Property must be > 0: app.sso.jwt.rotation-period");
        }
        if (jwt.getPreviousKeyRetention() != null) {
            Assert.isTrue(!jwt.getPreviousKeyRetention().isNegative() && !jwt.getPreviousKeyRetention().isZero(),
                    "Property must be > 0: app.sso.jwt.previous-key-retention");
        }
        Assert.notNull(loginProtection, "Missing required property group: app.sso.login-protection");
        validateRule(loginProtection.getAccount(), "app.sso.login-protection.account");
        validateRule(loginProtection.getIp(), "app.sso.login-protection.ip");
        validateRule(loginProtection.getAccountIp(), "app.sso.login-protection.account-ip");
    }

    /**
     * 校验登录防护规则。
     * @param rule 规则对象
     * @param path 配置路径
     */
    private void validateRule(LoginRule rule, String path) {
        Assert.notNull(rule, "Missing required property group: " + path);
        Assert.isTrue(rule.getMaxFailures() > 0, "Property must be > 0: " + path + ".max-failures");
        Assert.notNull(rule.getWindow(), "Missing required property: " + path + ".window");
        Assert.isTrue(!rule.getWindow().isNegative() && !rule.getWindow().isZero(),
                "Property must be > 0: " + path + ".window");
        Assert.notNull(rule.getBlockDuration(), "Missing required property: " + path + ".block-duration");
        Assert.isTrue(!rule.getBlockDuration().isNegative() && !rule.getBlockDuration().isZero(),
                "Property must be > 0: " + path + ".block-duration");
    }

    /**
     * 校验允许回跳的路径前缀。
     * @param prefix 路径前缀
     */
    private void validateReturnToPrefix(String prefix) {
        Assert.isTrue(StringUtils.hasText(prefix), "return_to prefix must not be blank");
        Assert.isTrue(prefix.startsWith("/") && !prefix.startsWith("//"),
                "return_to prefix must start with a single '/': " + prefix);
        Assert.isTrue(!prefix.contains("\\") && !prefix.contains("\r") && !prefix.contains("\n"),
                "return_to prefix contains illegal characters: " + prefix);
    }

    /**
     * 获取签发者。
     * @return 签发者
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * 设置签发者。
     * @param issuer 签发者
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * 获取会话TTL。
     * @return 会话TTL
     */
    public Duration getSessionTtl() {
        return sessionTtl;
    }

    /**
     * 设置会话TTL。
     * @param sessionTtl 会话TTL
     */
    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    /**
     * 获取授权码TTL。
     * @return 授权码TTL
     */
    public Duration getAuthCodeTtl() {
        return authCodeTtl;
    }

    /**
     * 设置授权码TTL。
     * @param authCodeTtl 授权码TTL
     */
    public void setAuthCodeTtl(Duration authCodeTtl) {
        this.authCodeTtl = authCodeTtl;
    }

    /**
     * 获取访问令牌TTL。
     * @return 访问令牌TTL
     */
    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    /**
     * 设置访问令牌TTL。
     * @param accessTokenTtl 访问令牌TTL
     */
    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    /**
     * 获取ID Token TTL。
     * @return ID Token TTL
     */
    public Duration getIdTokenTtl() {
        return idTokenTtl;
    }

    /**
     * 设置ID Token TTL。
     * @param idTokenTtl ID Token TTL
     */
    public void setIdTokenTtl(Duration idTokenTtl) {
        this.idTokenTtl = idTokenTtl;
    }

    /**
     * 获取刷新令牌 TTL。
     * @return 刷新令牌 TTL
     */
    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    /**
     * 设置刷新令牌 TTL。
     * @param refreshTokenTtl 刷新令牌 TTL
     */
    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * 获取 JWT 配置。
     * @return JWT 配置
     */
    public Jwt getJwt() {
        return jwt;
    }

    /**
     * 设置 JWT 配置。
     * @param jwt JWT 配置
     */
    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    /**
     * 获取 Cookie 配置。
     * @return Cookie 配置
     */
    public Cookie getCookie() {
        return cookie;
    }

    /**
     * 设置 Cookie 配置。
     * @param cookie Cookie 配置
     */
    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    /**
     * 获取登录防护配置。
     * @return 登录防护配置
     */
    public LoginProtection getLoginProtection() {
        return loginProtection;
    }

    /**
     * 设置登录防护配置。
     * @param loginProtection 登录防护配置
     */
    public void setLoginProtection(LoginProtection loginProtection) {
        this.loginProtection = loginProtection;
    }

    /**
     * 获取密码重置配置。
     * @return 密码重置配置
     */
    public PasswordReset getPasswordReset() {
        return passwordReset;
    }

    /**
     * 设置密码重置配置。
     * @param passwordReset 密码重置配置
     */
    public void setPasswordReset(PasswordReset passwordReset) {
        this.passwordReset = passwordReset;
    }

    /**
     * 获取允许的 return_to 路径前缀。
     * @return 路径前缀列表
     */
    public List<String> getAllowedReturnToPrefixes() {
        return allowedReturnToPrefixes;
    }

    /**
     * 设置允许的 return_to 路径前缀。
     * @param allowedReturnToPrefixes 路径前缀列表
     */
    public void setAllowedReturnToPrefixes(List<String> allowedReturnToPrefixes) {
        this.allowedReturnToPrefixes = allowedReturnToPrefixes == null
                ? new ArrayList<>()
                : new ArrayList<>(allowedReturnToPrefixes);
    }

    /**
     * Cookie 配置。
     */
    public static class Cookie {

        private boolean secure = true;
        private String sameSite = "Lax";

        /**
         * 获取是否启用 Secure。
         * @return 是否启用 Secure
         */
        public boolean isSecure() {
            return secure;
        }

        /**
         * 设置是否启用 Secure。
         * @param secure 是否启用 Secure
         */
        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        /**
         * 获取 SameSite 策略。
         * @return SameSite 策略
         */
        public String getSameSite() {
            return sameSite;
        }

        /**
         * 设置 SameSite 策略。
         * @param sameSite SameSite 策略
         */
        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
        }
    }

    /**
     * 密码重置配置。
     */
    public static class PasswordReset {

        private Duration codeTtl = Duration.ofMinutes(5);
        private int maxVerifyFailures = 5;
        private Duration sendInterval = Duration.ofSeconds(60);

        /**
         * 获取验证码有效期。
         * @return 验证码有效期
         */
        public Duration getCodeTtl() {
            return codeTtl;
        }

        /**
         * 设置验证码有效期。
         * @param codeTtl 验证码有效期
         */
        public void setCodeTtl(Duration codeTtl) {
            this.codeTtl = codeTtl;
        }

        /**
         * 获取最大校验失败次数。
         * @return 最大校验失败次数
         */
        public int getMaxVerifyFailures() {
            return maxVerifyFailures;
        }

        /**
         * 设置最大校验失败次数。
         * @param maxVerifyFailures 最大校验失败次数
         */
        public void setMaxVerifyFailures(int maxVerifyFailures) {
            this.maxVerifyFailures = maxVerifyFailures;
        }

        /**
         * 获取发送间隔。
         * @return 发送间隔
         */
        public Duration getSendInterval() {
            return sendInterval;
        }

        /**
         * 设置发送间隔。
         * @param sendInterval 发送间隔
         */
        public void setSendInterval(Duration sendInterval) {
            this.sendInterval = sendInterval;
        }
    }

    /**
     * JWT 密钥管理配置。
     */
    public static class Jwt {

        private String keyStorePath = "keys/sso-jwt-keys.properties";
        private Duration rotationPeriod = Duration.ofDays(30);
        private Duration previousKeyRetention;

        /**
         * 获取密钥文件路径。
         * @return 密钥文件路径
         */
        public String getKeyStorePath() {
            return keyStorePath;
        }

        /**
         * 设置密钥文件路径。
         * @param keyStorePath 密钥文件路径
         */
        public void setKeyStorePath(String keyStorePath) {
            this.keyStorePath = keyStorePath;
        }

        /**
         * 获取轮换周期。
         * @return 轮换周期
         */
        public Duration getRotationPeriod() {
            return rotationPeriod;
        }

        /**
         * 设置轮换周期。
         * @param rotationPeriod 轮换周期
         */
        public void setRotationPeriod(Duration rotationPeriod) {
            this.rotationPeriod = rotationPeriod;
        }

        /**
         * 获取旧密钥保留时长。
         * @return 保留时长
         */
        public Duration getPreviousKeyRetention() {
            return previousKeyRetention;
        }

        /**
         * 设置旧密钥保留时长。
         * @param previousKeyRetention 保留时长
         */
        public void setPreviousKeyRetention(Duration previousKeyRetention) {
            this.previousKeyRetention = previousKeyRetention;
        }
    }

    /**
     * 登录防护配置。
     */
    public static class LoginProtection {

        private LoginRule account;
        private LoginRule ip;
        private LoginRule accountIp;

        /**
         * 获取账号维度规则。
         * @return 账号维度规则
         */
        public LoginRule getAccount() {
            return account;
        }

        /**
         * 设置账号维度规则。
         * @param account 账号维度规则
         */
        public void setAccount(LoginRule account) {
            this.account = account;
        }

        /**
         * 获取IP维度规则。
         * @return IP维度规则
         */
        public LoginRule getIp() {
            return ip;
        }

        /**
         * 设置IP维度规则。
         * @param ip IP维度规则
         */
        public void setIp(LoginRule ip) {
            this.ip = ip;
        }

        /**
         * 获取账号+IP维度规则。
         * @return 账号+IP维度规则
         */
        public LoginRule getAccountIp() {
            return accountIp;
        }

        /**
         * 设置账号+IP维度规则。
         * @param accountIp 账号+IP维度规则
         */
        public void setAccountIp(LoginRule accountIp) {
            this.accountIp = accountIp;
        }
    }

    /**
     * 单个维度的登录防护规则。
     */
    public static class LoginRule {

        private int maxFailures;
        private Duration window;
        private Duration blockDuration;

        /**
         * 无参构造。
         */
        public LoginRule() {
        }

        /**
         * 全参构造。
         * @param maxFailures 最大失败次数
         * @param window 统计窗口
         * @param blockDuration 封禁时长
         */
        public LoginRule(int maxFailures, Duration window, Duration blockDuration) {
            this.maxFailures = maxFailures;
            this.window = window;
            this.blockDuration = blockDuration;
        }

        /**
         * 获取最大失败次数。
         * @return 最大失败次数
         */
        public int getMaxFailures() {
            return maxFailures;
        }

        /**
         * 设置最大失败次数。
         * @param maxFailures 最大失败次数
         */
        public void setMaxFailures(int maxFailures) {
            this.maxFailures = maxFailures;
        }

        /**
         * 获取统计窗口。
         * @return 统计窗口
         */
        public Duration getWindow() {
            return window;
        }

        /**
         * 设置统计窗口。
         * @param window 统计窗口
         */
        public void setWindow(Duration window) {
            this.window = window;
        }

        /**
         * 获取封禁时长。
         * @return 封禁时长
         */
        public Duration getBlockDuration() {
            return blockDuration;
        }

        /**
         * 设置封禁时长。
         * @param blockDuration 封禁时长
         */
        public void setBlockDuration(Duration blockDuration) {
            this.blockDuration = blockDuration;
        }
    }
}
