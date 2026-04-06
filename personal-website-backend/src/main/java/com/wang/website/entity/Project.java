package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("project") // 对应数据库表名 [cite: 73]

public class Project {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String description;
    private String tech_stack;
    private String github_url;
    private String image_url;
    private Integer sort;
    private LocalDateTime createTime;






}
