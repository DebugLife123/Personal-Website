package com.wang.website.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wang.website.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
