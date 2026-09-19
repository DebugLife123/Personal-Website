package com.wang.website.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易 Token 管理：登录签发 UUID 令牌，内存保存，带过期时间。
 * 单机个人网站足够用，重启后令牌失效需重新登录。
 */
public class TokenUtil {

    /** 令牌有效期：12 小时 */
    private static final long EXPIRE_MS = 12 * 60 * 60 * 1000L;

    private static final Map<String, TokenInfo> TOKENS = new ConcurrentHashMap<>();

    private record TokenInfo(String username, long expireAt) {
        boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    /** 签发令牌 */
    public static String create(String username) {
        String token = UUID.randomUUID().toString().replace("-", "")
                + Long.toHexString(System.currentTimeMillis());
        TOKENS.put(token, new TokenInfo(username, System.currentTimeMillis() + EXPIRE_MS));
        return token;
    }

    /** 校验令牌，返回用户名；无效或过期返回 null */
    public static String verify(String token) {
        if (token == null || token.isEmpty()) return null;
        TokenInfo info = TOKENS.get(token);
        if (info == null) return null;
        if (info.expired()) {
            TOKENS.remove(token);
            return null;
        }
        return info.username;
    }

    public static void remove(String token) {
        if (token != null) TOKENS.remove(token);
    }

    /** 对字符串做 SHA-256，用于派生签名等 */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
