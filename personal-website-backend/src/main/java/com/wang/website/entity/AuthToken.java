package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录令牌（DB 持久化）：管理员与注册用户共用一张表，重启不掉线，可服务端踢下线。
 */
@Data
@TableName("auth_token")
public class AuthToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String token;
    /** admin / user */
    private String principalType;
    private Integer principalId;
    private String principalName;
    private LocalDateTime expireAt;
    private LocalDateTime createTime;
}
