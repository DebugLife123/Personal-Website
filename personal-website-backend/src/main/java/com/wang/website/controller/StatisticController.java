package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.website.common.Result;
import com.wang.website.entity.Statistic;
import com.wang.website.mapper.StatisticMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/statistic")
@CrossOrigin
public class StatisticController {
    @Autowired
    private StatisticMapper statisticMapper;

    // 获取今日统计数据
    @GetMapping("/today")
    public Result<Statistic> getTodayStatistic() {
        try {
            LocalDate today = LocalDate.now();
            QueryWrapper<Statistic> wrapper = new QueryWrapper<Statistic>().eq("date", today);
            Statistic statistic = statisticMapper.selectOne(wrapper);
            if (statistic == null) {
                // 如果今天还没有统计数据，创建一个新的
                statistic = new Statistic();
                statistic.setDate(today);
                statistic.setPageViews(0);
                statistic.setUniqueVisitors(0);
                statistic.setArticleReads(0);
                statisticMapper.insert(statistic);
            }
            return Result.success(statistic);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取今日统计数据失败");
        }
    }

    // 获取总统计数据
    @GetMapping("/total")
    public Result<Map<String, Integer>> getTotalStatistic() {
        try {
            QueryWrapper<Statistic> wrapper = new QueryWrapper<Statistic>();
            Integer totalPageViews = 0;
            Integer totalUniqueVisitors = 0;
            Integer totalArticleReads = 0;

            for (Statistic statistic : statisticMapper.selectList(wrapper)) {
                totalPageViews += statistic.getPageViews();
                totalUniqueVisitors += statistic.getUniqueVisitors();
                totalArticleReads += statistic.getArticleReads();
            }

            Map<String, Integer> total = new HashMap<>();
            total.put("pageViews", totalPageViews);
            total.put("uniqueVisitors", totalUniqueVisitors);
            total.put("articleReads", totalArticleReads);

            return Result.success(total);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取总统计数据失败");
        }
    }

    // 更新统计数据
    @PostMapping("/update")
    public Result<String> updateStatistic(@RequestBody Map<String, Integer> data) {
        try {
            LocalDate today = LocalDate.now();
            QueryWrapper<Statistic> wrapper = new QueryWrapper<Statistic>().eq("date", today);
            Statistic statistic = statisticMapper.selectOne(wrapper);

            if (statistic == null) {
                statistic = new Statistic();
                statistic.setDate(today);
                statistic.setPageViews(0);
                statistic.setUniqueVisitors(0);
                statistic.setArticleReads(0);
            }

            // 更新页面访问量
            if (data.containsKey("pageViews")) {
                statistic.setPageViews(statistic.getPageViews() + data.get("pageViews"));
            }

            // 更新独立访客数
            if (data.containsKey("uniqueVisitors")) {
                statistic.setUniqueVisitors(statistic.getUniqueVisitors() + data.get("uniqueVisitors"));
            }

            // 更新文章阅读量
            if (data.containsKey("articleReads")) {
                statistic.setArticleReads(statistic.getArticleReads() + data.get("articleReads"));
            }

            if (statistic.getId() == null) {
                statisticMapper.insert(statistic);
            } else {
                statisticMapper.updateById(statistic);
            }

            return Result.success("统计数据更新成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新统计数据失败");
        }
    }
}