package com.wang.website.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.website.entity.AuthToken;
import com.wang.website.mapper.AuthTokenMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * 登录令牌服务：签发/校验/吊销，DB 持久化。
 * 管理员令牌 12 小时有效，注册用户令牌 7 天有效。
 */
@Service
public class TokenService {

    public static final String TYPE_ADMIN = "admin";
    public static final String TYPE_USER = "user";

    private static final long ADMIN_TTL_HOURS = 12;
    private static final long USER_TTL_DAYS = 7;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private AuthTokenMapper tokenMapper;

    /** 签发新令牌 */
    public String create(String principalType, Integer principalId, String principalName) {
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        String token = HexFormat.of().formatHex(buf);

        AuthToken t = new AuthToken();
        t.setToken(token);
        t.setPrincipalType(principalType);
        t.setPrincipalId(principalId);
        t.setPrincipalName(principalName);
        t.setExpireAt(TYPE_ADMIN.equals(principalType)
                ? LocalDateTime.now().plusHours(ADMIN_TTL_HOURS)
                : LocalDateTime.now().plusDays(USER_TTL_DAYS));
        tokenMapper.insert(t);

        // 惰性清理：每次签发时顺带清掉过期令牌
        tokenMapper.delete(new QueryWrapper<AuthToken>().lt("expire_at", LocalDateTime.now()));

        return token;
    }

    /** 校验令牌，有效则返回记录，无效或过期返回 null（过期顺手删除） */
    public AuthToken verify(String token) {
        if (token == null || token.isBlank()) return null;
        AuthToken t = tokenMapper.selectOne(new QueryWrapper<AuthToken>().eq("token", token));
        if (t == null) return null;
        if (t.getExpireAt().isBefore(LocalDateTime.now())) {
            tokenMapper.deleteById(t.getId());
            return null;
        }
        return t;
    }

    /** 吊销单个令牌 */
    public void revoke(String token) {
        if (token == null || token.isBlank()) return;
        tokenMapper.delete(new QueryWrapper<AuthToken>().eq("token", token));
    }

    /** 吊销某主体全部令牌（踢下线 / 改密后强制重登） */
    public void revokeAll(String principalType, Integer principalId) {
        tokenMapper.delete(new QueryWrapper<AuthToken>()
                .eq("principal_type", principalType)
                .eq("principal_id", principalId));
    }
}
