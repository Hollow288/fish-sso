package com.hollow.fishsso.controller.support;

import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.util.SsoCookieNames;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 统一构建会话 Cookie。
 */
@Component
public class SessionCookieFactory {

    private final SsoProperties properties;

    /**
     * 构造函数。
     * @param properties SSO配置属性
     */
    public SessionCookieFactory(SsoProperties properties) {
        this.properties = properties;
    }

    /**
     * 构建登录后的会话Cookie。
     * @param sessionId 会话ID
     * @return 会话Cookie
     */
    public ResponseCookie createSessionCookie(String sessionId) {
        return ResponseCookie.from(SsoCookieNames.SESSION, sessionId)
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .path("/")
                .sameSite(properties.getCookie().getSameSite())
                .maxAge(properties.getSessionTtl())
                .build();
    }

    /**
     * 构建清除会话的Cookie。
     * @return 清除会话Cookie
     */
    public ResponseCookie clearSessionCookie() {
        return ResponseCookie.from(SsoCookieNames.SESSION, "")
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .path("/")
                .sameSite(properties.getCookie().getSameSite())
                .maxAge(0)
                .build();
    }
}
