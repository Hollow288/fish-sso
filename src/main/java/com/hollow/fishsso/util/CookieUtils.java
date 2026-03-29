package com.hollow.fishsso.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie工具类
 */
public final class CookieUtils {

    /**
     * 私有构造函数。
     */
    private CookieUtils() {
    }

    /**
     * 获取Cookie值
     * @param request HTTP请求对象
     * @param name Cookie名称
     * @return Cookie值（可选）
     */
    public static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
