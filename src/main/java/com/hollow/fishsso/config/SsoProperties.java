package com.hollow.fishsso.config;

import java.time.Duration;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * SSO properties.
 */
@Component
@ConfigurationProperties(prefix = "app.sso")
public class SsoProperties {

    private String issuer;
    private Duration sessionTtl;
    private Duration authCodeTtl;
    private Duration accessTokenTtl;

    @PostConstruct
    void validateRequiredProperties() {
        Assert.isTrue(StringUtils.hasText(issuer), "Missing required property: app.sso.issuer");
        Assert.notNull(sessionTtl, "Missing required property: app.sso.session-ttl");
        Assert.notNull(authCodeTtl, "Missing required property: app.sso.auth-code-ttl");
        Assert.notNull(accessTokenTtl, "Missing required property: app.sso.access-token-ttl");
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    public Duration getAuthCodeTtl() {
        return authCodeTtl;
    }

    public void setAuthCodeTtl(Duration authCodeTtl) {
        this.authCodeTtl = authCodeTtl;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }
}
