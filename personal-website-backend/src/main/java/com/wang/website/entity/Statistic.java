package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("statistic")
public class Statistic {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private LocalDate date;
    private Integer pageViews;
    private Integer uniqueVisitors;
    private Integer articleReads;
}