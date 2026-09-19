package com.wang.website.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wang.website.common.Result;
import com.wang.website.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 管理员写操作鉴权拦截器。
 * 读的 GET 全部放行；写操作（POST/PUT/DELETE）需要有效 Token，
 * 但登录接口、留言/统计等游客操作在白名单中放行。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 无需登录即可写操作的路径前缀（游客功能） */
    private static final Set<String> WRITE_WHITELIST = Set.of(
            "/api/user/login",
            "/api/message/add",
            "/api/message/like",
            "/api/message/unlike",
            "/api/statistic/update",
            "/api/upload"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检与 GET 一律放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || "GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        // 白名单（游客可写）
        for (String prefix : WRITE_WHITELIST) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = TokenUtil.verify(token);
        if (username != null) {
            request.setAttribute("adminUser", username);
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(Result.error("未登录或登录已过期，请重新登录")));
        return false;
    }
}
