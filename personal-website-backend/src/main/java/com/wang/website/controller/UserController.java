package com.wang.website.controller;

import com.wang.website.common.Result;
import com.wang.website.entity.User;
import com.wang.website.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> params) {
        try {
            String username = params.get("username");
            String password = params.get("password");

            if (username == null || password == null) {
                return Result.error("用户名或密码不能为空");
            }

            User user = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                            .eq("username", username)
                            .eq("password", password)
            );

            if (user != null) {
                user.setPassword(null); // 不返回密码
                return Result.success(user);
            } else {
                return Result.error("用户名或密码错误");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("登录失败");
        }
    }
}
