package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("visitor")
public class Visitor {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String ip;
    private String userAgent;
    private String path;
    private LocalDate visitDate;
    private LocalDateTime createTime;
}
