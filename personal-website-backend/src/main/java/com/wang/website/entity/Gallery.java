package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图库照片：图片地址 + 简介，可在后台维护显示状态与排序。
 */
@Data
@TableName("gallery")
public class Gallery {
    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 标题（可选，留空时前台只显示简介） */
    private String title;
    /** 照片简介 */
    private String description;
    /** 图片地址（/uploads/images/xxx 或外链） */
    private String url;
    /** 拍摄地（可选） */
    private String location;
    /** 拍摄时间文本（可选，如 2025-10-01） */
    private String shotTime;
    /** 排序值，越小越靠前 */
    private Integer sort;
    /** visible / hidden */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
