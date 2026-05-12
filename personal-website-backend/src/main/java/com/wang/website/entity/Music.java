package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("music")
public class Music {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String artist;
    private String album;
    private String cover;
    private String url;
    private Integer duration;
    private Boolean enabled;
    private LocalDateTime createTime;
}
