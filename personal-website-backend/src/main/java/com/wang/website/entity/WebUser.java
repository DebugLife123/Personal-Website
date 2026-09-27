package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站点注册用户（与管理员账号 admin_user 相互独立）。
 */
@Data
@TableName("website_user")
public class WebUser {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    @JsonIgnore // 任何接口响应都不携带密码哈希
    private String password;
    private String nickname;
    private String email;
    private String avatar;
    /** normal / banned */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
