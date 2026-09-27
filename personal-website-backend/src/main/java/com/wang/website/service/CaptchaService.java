package com.wang.website.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 算术验证码：避免依赖图形字体（无头 JRE 在无 fontconfig 的服务器上画不了字符）。
 * 每次生成 "a + b = ?" 或 "a - b = ?" 的算式，答案缓存在内存中 5 分钟，一次性使用。
 */
@Service
public class CaptchaService {

    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final int MAX_ENTRIES = 10000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    private record Entry(String answer, long expireAt) {
        boolean expired() { return Instant.now().toEpochMilli() > expireAt; }
    }

    /** 生成一道题，返回 id 与题干 */
    public Map<String, String> generate() {
        int a = 2 + RANDOM.nextInt(13);   // 2~14
        int b = 2 + RANDOM.nextInt(13);
        boolean plus = RANDOM.nextBoolean();
        if (!plus && b > a) { int t = a; a = b; b = t; }
        int answer = plus ? a + b : a - b;
        String question = a + (plus ? " + " : " - ") + b + " = ?";

        sweep();
        if (store.size() >= MAX_ENTRIES) {
            // 极端情况下拒绝服务前先清空兜底
            store.clear();
        }
        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, new Entry(String.valueOf(answer), Instant.now().toEpochMilli() + TTL_MS));
        return Map.of("captchaId", id, "question", question);
    }

    /** 校验答案（一次性），答错或过期都销毁题目 */
    public boolean check(String captchaId, String answer) {
        if (captchaId == null || answer == null) return false;
        Entry e = store.remove(captchaId);
        if (e == null || e.expired()) return false;
        return e.answer().equals(answer.trim());
    }

    private void sweep() {
        long now = Instant.now().toEpochMilli();
        store.entrySet().removeIf(en -> en.getValue().expireAt() < now);
    }
}
