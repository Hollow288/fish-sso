package com.hollow.fishsso.config;

import java.time.Duration;
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
    private LoginProtection loginProtection;

    /**
     * 校验必须配置项。
     */
    @PostConstruct
    void validateRequiredProperties() {
        Assert.isTrue(StringUtils.hasText(issuer), "Missing required property: app.sso.issuer");
        Assert.notNull(sessionTtl, "Missing required property: app.sso.session-ttl");
        Assert.notNull(authCodeTtl, "Missing required property: app.sso.auth-code-ttl");
        Assert.notNull(accessTokenTtl, "Missing required property: app.sso.access-token-ttl");
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
