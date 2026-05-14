package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String nickname;
    private String content;
    private String avatar;
    private String email;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private Integer parentId;
    private String replyTo;
    private Integer likes;
    private Boolean isPinned;
    private String status; // pending/approved/rejected/deleted

    @TableField(exist = false)
    private List<Message> replies;

    @TableField(exist = false)
    private Boolean isAdmin; // 前端传入：是否管理员发言
    @TableField(exist = false)
    private Boolean adminPost; // 后端返回：是否管理员发的留言
    @TableField(exist = false)
    private Integer replyCount; // 回复数
}
