package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.website.common.Result;
import com.wang.website.entity.User;
import com.wang.website.mapper.UserMapper;
import com.wang.website.service.TokenService;
import com.wang.website.util.RateLimitUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员账号接口（与站点注册用户 /api/webuser 独立）。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TokenService tokenService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params,
                                             HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String username = params.get("username");
        String password = params.get("password");
        if (username == null || password == null) {
            return Result.error("用户名或密码不能为空");
        }
        String failKey = "admin-login:" + ip;
        if (!RateLimitUtil.hit(failKey, 8, 900_000L)) {
            return Result.error("尝试次数过多，请 15 分钟后再试");
        }

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        RateLimitUtil.reset(failKey);
        String token = tokenService.create(TokenService.TYPE_ADMIN, user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        return Result.success(data);
    }

    /**
     * 校验当前 Token 是否有效（供前端回源验证管理员登录态）。
     * 能走到这里说明已通过 AuthInterceptor 的管理员 Token 校验。
     */
    @GetMapping("/check")
    public Result<Map<String, Object>> check(HttpServletRequest request) {
        String username = (String) request.getAttribute("adminUser");
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenService.revoke(auth.substring(7));
        }
        return Result.success("已退出登录");
    }

    /** 修改密码：需要有效 Token，并校验旧密码；成功后所有管理员令牌失效 */
    @PostMapping("/changePassword")
    public Result<String> changePassword(@RequestBody Map<String, String> params,
                                         HttpServletRequest request) {
        String username = (String) request.getAttribute("adminUser");
        if (username == null) {
            return Result.error("未登录或登录已过期");
        }
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
            return Result.error("新密码长度至少 6 位");
        }

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("原密码不正确");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        tokenService.revokeAll(TokenService.TYPE_ADMIN, user.getId());
        return Result.success("密码修改成功，请重新登录");
    }
}
