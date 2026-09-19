package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_entry")
public class ResumeEntry {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer resumeId;
    private String type;
    private String title;
    private String subtitle;
    private String timeRange;
    private String description;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
