package com.wang.website.util;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存滑动窗口限流（单机足够）：
 * 同一个 key 在 windowMs 内最多允许 max 次 hit，超出返回 false。
 * 进程重启清零——对个人站可以接受，主打挡住脚本扫号/灌水。
 */
public class RateLimitUtil {

    private static final Map<String, Deque<Long>> HITS = new ConcurrentHashMap<>();

    /** 记录一次访问；true=放行，false=超限 */
    public static boolean hit(String key, int max, long windowMs) {
        long now = Instant.now().toEpochMilli();
        Deque<Long> q = HITS.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
            if (q.size() >= max) return false;
            q.addLast(now);
            return true;
        }
    }

    /** 只查询不记录（用于展示剩余冷却时间等场景） */
    public static long remainingMs(String key, int max, long windowMs) {
        long now = Instant.now().toEpochMilli();
        Deque<Long> q = HITS.get(key);
        if (q == null) return 0;
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
            if (q.size() < max) return 0;
            return windowMs - (now - q.peekFirst());
        }
    }

    /** 清空某个 key（登录成功等场景解除失败计数） */
    public static void reset(String key) {
        HITS.remove(key);
    }
}
