package com.wang.website.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wang.website.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 留言板 Mapper 接口
 * 继承 BaseMapper 即可获得常用的增删改查能力
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /** 点赞数原子自增 */
    @Update("UPDATE message SET likes = IFNULL(likes, 0) + 1 WHERE id = #{id}")
    int incrLikes(@Param("id") Integer id);

    /** 点赞数原子自减，下限为 0 */
    @Update("UPDATE message SET likes = GREATEST(IFNULL(likes, 0) - 1, 0) WHERE id = #{id}")
    int decrLikes(@Param("id") Integer id);
}