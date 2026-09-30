package com.hollow.fishsso.service;

import com.hollow.fishsso.exception.SsoException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/** RFC 7636 PKCE validation; only the S256 challenge method is accepted. */
final class PkceSupport {
    private static final Pattern CHALLENGE = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final Pattern VERIFIER = Pattern.compile("[A-Za-z0-9._~-]{43,128}");

    private PkceSupport() {
    }

    static void validateAuthorization(String challenge, String method, boolean required) {
        if (challenge == null) {
            if (required || method != null) {
                throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_request", "此客户端必须使用 PKCE S256");
            }
            return;
        }
        if (!CHALLENGE.matcher(challenge).matches() || !"S256".equals(method)) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_request", "无效的 PKCE code_challenge 或方法");
        }
    }

    static void verify(String storedChallenge, String verifier, boolean required) {
        if (storedChallenge == null) {
            if (required || verifier != null) {
                throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "授权码缺少 PKCE 绑定");
            }
            return;
        }
        if (verifier == null || !VERIFIER.matcher(verifier).matches()) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "无效的 PKCE code_verifier");
        }
        String actual = challengeFor(verifier);
        if (!MessageDigest.isEqual(storedChallenge.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII))) {
            throw new SsoException(HttpStatus.BAD_REQUEST, "invalid_grant", "PKCE 校验失败");
        }
    }

    private static String challengeFor(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
