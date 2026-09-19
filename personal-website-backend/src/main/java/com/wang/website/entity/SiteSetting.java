package com.wang.website.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("site_setting")
public class SiteSetting {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String settingKey;
    private String settingValue;
    private String description;
}
