package com.wang.website.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wang.website.entity.Statistic;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StatisticMapper extends BaseMapper<Statistic> {

    /**
     * 记录一次页面访问（原子操作）。
     * 行不存在则插入当天记录，存在则累加；uv=1 时同时累加独立访客。
     */
    @Insert("INSERT INTO statistic(date, page_views, unique_visitors, article_reads) "
            + "VALUES (CURDATE(), 1, #{uv}, 0) "
            + "ON DUPLICATE KEY UPDATE page_views = page_views + 1, unique_visitors = unique_visitors + #{uv}")
    int recordVisit(@Param("uv") int uv);

    /** 记录一次文章阅读（原子操作） */
    @Insert("INSERT INTO statistic(date, page_views, unique_visitors, article_reads) "
            + "VALUES (CURDATE(), 0, 0, 1) "
            + "ON DUPLICATE KEY UPDATE article_reads = article_reads + 1")
    int recordArticleRead();
}
