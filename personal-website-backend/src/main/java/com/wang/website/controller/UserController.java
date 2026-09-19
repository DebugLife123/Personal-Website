package com.wang.website.controller;

import com.wang.website.common.Result;
import com.wang.website.entity.User;
import com.wang.website.mapper.UserMapper;
import com.wang.website.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        try {
            String username = params.get("username");
            String password = params.get("password");

            if (username == null || password == null) {
                return Result.error("用户名或密码不能为空");
            }

            User user = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                            .eq("username", username)
            );

            if (user != null && passwordEncoder.matches(password, user.getPassword())) {
                String token = TokenUtil.create(username);
                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("username", user.getUsername());
                return Result.success(data);
            } else {
                return Result.error("用户名或密码错误");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("登录失败");
        }
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            TokenUtil.remove(auth.substring(7));
        }
        return Result.success("已退出登录");
    }

    /** 修改密码：需要有效 Token，并校验旧密码 */
    @PostMapping("/changePassword")
    public Result<String> changePassword(@RequestBody Map<String, String> params,
                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            String username = null;
            if (auth != null && auth.startsWith("Bearer ")) {
                username = TokenUtil.verify(auth.substring(7));
            }
            if (username == null) {
                return Result.error("未登录或登录已过期");
            }

            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");
            if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
                return Result.error("新密码长度至少 6 位");
            }

            User user = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                            .eq("username", username)
            );
            if (user == null) {
                return Result.error("用户不存在");
            }
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return Result.error("原密码不正确");
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("密码修改失败");
        }
    }
}
