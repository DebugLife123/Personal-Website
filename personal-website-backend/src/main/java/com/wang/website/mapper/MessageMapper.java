package com.wang.website.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wang.website.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 留言板 Mapper 接口
 * 继承 BaseMapper 即可获得常用的增删改查能力
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}