package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站点访客身份（昵称即唯一标识，无密码）。
 *
 * 与管理员账号 admin_user 完全独立：
 *   - 访客选一个形象 + 填昵称即可「同步身份」进入，不需要密码/验证码；
 *   - 昵称唯一，作为后台用户管理的唯一标识；
 *   - username 为历史列（与昵称同值），password 为保留列，现行逻辑均不再使用。
 */
@Data
@TableName("website_user")
public class WebUser {
    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 身份标识，与 nickname 同值（兼容历史唯一约束） */
    private String username;
    /** 昵称：登录身份的唯一标识 */
    private String nickname;
    private String email;
    private String avatar;
    /** normal / banned */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private String lastLoginUa;
    private Integer loginCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
