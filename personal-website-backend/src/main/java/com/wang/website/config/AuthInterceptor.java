package com.wang.website.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wang.website.common.Result;
import com.wang.website.entity.AuthToken;
import com.wang.website.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 接口鉴权拦截器（白名单模型）。
 *
 * 身份模型：
 *   - 请求携带有效 Token 时，解析出 admin / user 两类身份写入 request 属性：
 *     adminUser(管理员用户名)、authUserId/authUserName(注册用户)
 *   - 默认规则：/api/** 需要有效管理员 Token；
 *     PUBLIC_* 中的路径对游客开放；
 *     USER_AUTH_WRITE 中的路径仅要求「注册用户或管理员已登录」。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private TokenService tokenService;

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
            "/api/setting/all",
            "/api/webuser/captcha"
    );

    /** 游客可写的精确路径（登录/注册、统计上报、访客记录） */
    private static final Set<String> PUBLIC_WRITE = Set.of(
            "/api/user/login",
            "/api/webuser/register",
            "/api/webuser/login",
            "/api/statistic/visit",
            "/api/visitor/record"
    );

    /** 需要登录（注册用户或管理员皆可）才能写的精确路径 */
    private static final Set<String> USER_AUTH_WRITE = Set.of(
            "/api/message/add"
    );

    /** 需要登录（注册用户或管理员皆可）才能写的前缀路径 */
    private static final Set<String> USER_AUTH_WRITE_PREFIX = Set.of(
            "/api/message/mine/"
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

        // 解析身份：有效 Token 写入 request 属性
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        AuthToken auth = tokenService.verify(token);
        if (auth != null) {
            if (TokenService.TYPE_ADMIN.equals(auth.getPrincipalType())) {
                request.setAttribute("adminUser", auth.getPrincipalName());
            } else {
                request.setAttribute("authUserId", auth.getPrincipalId());
                request.setAttribute("authUserName", auth.getPrincipalName());
            }
        }

        // 管理员 Token：全量放行
        if (request.getAttribute("adminUser") != null) {
            return true;
        }

        String path = request.getRequestURI();

        // 注册用户 Token：放行自身资料接口与「登录可写」清单
        if (request.getAttribute("authUserId") != null) {
            if (path.startsWith("/api/webuser/") || USER_AUTH_WRITE.contains(path)) {
                return true;
            }
            for (String prefix : USER_AUTH_WRITE_PREFIX) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }
        }

        // 游客白名单
        if (isPublic(request.getMethod(), path)) {
            return true;
        }

        deny(response, "未登录或登录已过期，请重新登录");
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

    private void deny(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(Result.error(message)));
    }
}
