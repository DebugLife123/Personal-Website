package com.wang.website.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wang.website.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /** 阅读量原子自增，避免并发读-改-写丢计数 */
    @Update("UPDATE article SET views = IFNULL(views, 0) + 1 WHERE id = #{id}")
    int incrViews(@Param("id") Integer id);
}
