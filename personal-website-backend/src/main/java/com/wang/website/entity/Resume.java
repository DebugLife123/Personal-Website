package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume") // 对应数据库表名 [cite: 73]
public class Resume {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String avatar; // 头像
    private String email; // 邮箱
    private String phone; // 电话
    private String address; // 地址
    private String education; // 教育背景
    private String skill; // 技能
    private String award; // 奖项
    private String intro; // 个人简介
    private String workExperience; // 工作经历
    private String projectExperience; // 项目经验
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}