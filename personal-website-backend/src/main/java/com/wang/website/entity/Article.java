package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String summary;
    private String content;
    private Integer views;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String cover; // 【新增】封面图字段
    private String category; // 【新增】分类字段
    private String tags; // 【新增】标签字段，多个标签用逗号分隔
    private String status; // 状态：已发布 / 草稿
}