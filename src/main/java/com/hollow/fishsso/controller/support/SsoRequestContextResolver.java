package com.hollow.fishsso.controller.support;

import com.hollow.fishsso.util.CookieUtils;
import com.hollow.fishsso.util.SsoCookieNames;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 解析当前请求中的会话与来源信息。
 */
@Component
public class SsoRequestContextResolver {

    private static final String UNKNOWN_IP = "unknown-ip";

    /**
     * 获取当前会话ID。
     * @param request HTTP请求对象
     * @return 会话ID，不存在时返回null
     */
    public String resolveSessionId(HttpServletRequest request) {
        return CookieUtils.getCookieValue(request, SsoCookieNames.SESSION).orElse(null);
    }

    /**
     * 解析客户端真实IP地址，依次尝试 X-Forwarded-For、X-Real-IP、RemoteAddr。
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    public String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor;
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        if (!StringUtils.hasText(remoteAddr)) {
            return UNKNOWN_IP;
        }
        return remoteAddr.trim();
    }

    /**
     * 从 X-Forwarded-For 头中提取第一个IP地址。
     * @param xForwardedFor X-Forwarded-For头的值
     * @return 第一个IP地址，为空时返回null
     */
    private String firstForwardedIp(String xForwardedFor) {
        if (!StringUtils.hasText(xForwardedFor)) {
            return null;
        }
        int firstComma = xForwardedFor.indexOf(',');
        if (firstComma < 0) {
            return xForwardedFor.trim();
        }
        return xForwardedFor.substring(0, firstComma).trim();
    }
}
