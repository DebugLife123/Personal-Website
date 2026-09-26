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
 * 接口鉴权拦截器（白名单模型）。
 *
 * 默认规则：/api/** 下所有请求都需要有效管理员 Token；
 * 仅 PUBLIC_GET / PUBLIC_WRITE 中列出的路径对游客开放。
 * 无论路径是否在白名单中，只要请求携带了有效 Token，
 * 都会把用户名写入 request 的 adminUser 属性，供 Controller 识别管理员身份。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 游客可读的精确路径 */
    private static final Set<String> PUBLIC_GET = Set.of(
            "/api/article/page",
            "/api/article/get",
            "/api/article/categories",
            "/api/article/tags",
            "/api/article/search",
            "/api/article/listByCategory",
            "/api/article/listByTag",
            "/api/message/list",
            "/api/music/enabled",
            "/api/project/list",
            "/api/project/categories",
            "/api/project/search",
            "/api/resume/get",
            "/api/resume/entries",
            "/api/statistic/today",
            "/api/statistic/total",
            "/api/setting/all"
    );

    /** 游客可写的精确路径（登录、留言互动、统计上报、访客记录） */
    private static final Set<String> PUBLIC_WRITE = Set.of(
            "/api/user/login",
            "/api/message/add",
            "/api/statistic/visit",
            "/api/visitor/record"
    );

    /** 游客可写的前缀路径 */
    private static final Set<String> PUBLIC_WRITE_PREFIX = Set.of(
            "/api/message/like/",
            "/api/message/unlike/"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检一律放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 无论目标路径是否公开，只要带了有效 Token 就识别管理员身份
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = TokenUtil.verify(token);
        if (username != null) {
            request.setAttribute("adminUser", username);
            return true;
        }

        String path = request.getRequestURI();
        if (isPublic(request.getMethod(), path)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(Result.error("未登录或登录已过期，请重新登录")));
        return false;
    }

    private boolean isPublic(String method, String path) {
        if ("GET".equalsIgnoreCase(method)) {
            return PUBLIC_GET.contains(path);
        }
        if (PUBLIC_WRITE.contains(path)) {
            return true;
        }
        for (String prefix : PUBLIC_WRITE_PREFIX) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
