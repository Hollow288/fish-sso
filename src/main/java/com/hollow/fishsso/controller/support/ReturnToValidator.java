package com.hollow.fishsso.controller.support;

import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.exception.SsoException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 校验登录完成后的回跳路径。
 */
@Component
public class ReturnToValidator {

    private final SsoProperties properties;

    /**
     * 构造函数。
     * @param properties SSO配置属性
     */
    public ReturnToValidator(SsoProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验回跳路径；若为空则返回null。
     * @param returnTo 原始回跳路径
     * @return 校验后的回跳路径
     */
    public String validate(String returnTo) {
        if (!StringUtils.hasText(returnTo)) {
            return null;
        }
        String candidate = returnTo.trim();
        if (!candidate.startsWith("/") || candidate.startsWith("//")
                || candidate.contains("\\") || candidate.contains("\r") || candidate.contains("\n")) {
            throw invalidReturnTo();
        }

        URI uri;
        try {
            uri = URI.create(candidate);
        } catch (IllegalArgumentException ex) {
            throw invalidReturnTo();
        }

        if (uri.isAbsolute()
                || StringUtils.hasText(uri.getScheme())
                || StringUtils.hasText(uri.getAuthority())
                || StringUtils.hasText(uri.getHost())
                || uri.getPort() != -1
                || StringUtils.hasText(uri.getUserInfo())) {
            throw invalidReturnTo();
        }

        String path = uri.getPath();
        if (!StringUtils.hasText(path) || !isAllowedReturnToPath(path)) {
            throw invalidReturnTo();
        }
        return candidate;
    }

    /**
     * 判断回跳路径是否命中允许的前缀。
     * @param path 路径
     * @return 是否允许
     */
    private boolean isAllowedReturnToPath(String path) {
        return properties.getAllowedReturnToPrefixes().stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    /**
     * 构造非法 return_to 异常。
     * @return 非法请求异常
     */
    private SsoException invalidReturnTo() {
        return new SsoException(HttpStatus.BAD_REQUEST, "invalid_request", "非法 return_to 参数");
    }
}
