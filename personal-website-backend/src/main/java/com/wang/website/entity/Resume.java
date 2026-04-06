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
    private String education;
    private String skill;
    private String award;
    private String intro;
    private LocalDateTime createTime;
}