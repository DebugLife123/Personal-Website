package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.website.common.Result;
import com.wang.website.entity.Visitor;
import com.wang.website.mapper.VisitorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visitor")
@CrossOrigin
public class VisitorController {

    @Autowired
    private VisitorMapper visitorMapper;

    /** 记录一次访问（前台调用，白名单放行） */
    @PostMapping("/record")
    public Result<String> record(@RequestBody Map<String, String> body, jakarta.servlet.http.HttpServletRequest request) {
        try {
            Visitor v = new Visitor();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
            v.setIp(ip.split(",")[0].trim());
            v.setUserAgent(trim(request.getHeader("User-Agent"), 250));
            v.setPath(trim(body.get("path"), 200));
            v.setVisitDate(LocalDate.now());
            v.setCreateTime(LocalDateTime.now());
            visitorMapper.insert(v);
            return Result.success("ok");
        } catch (Exception e) {
            return Result.error("记录失败");
        }
    }

    private String trim(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    @GetMapping("/page")
    public Result<Page<Visitor>> page(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "15") Integer pageSize,
                                      @RequestParam(required = false) String date) {
        try {
            QueryWrapper<Visitor> w = new QueryWrapper<>();
            if (date != null && !date.trim().isEmpty()) w.eq("visit_date", date);
            w.orderByDesc("create_time");
            return Result.success(visitorMapper.selectPage(new Page<>(page, pageSize), w));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取访客记录失败");
        }
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        try {
            Map<String, Object> m = new HashMap<>();
            LocalDate today = LocalDate.now();
            m.put("total", visitorMapper.selectCount(null));
            m.put("today", visitorMapper.selectCount(new QueryWrapper<Visitor>().eq("visit_date", today)));
            // 最近7天按日统计（PV：访问次数）
            List<Map<String, Object>> byDay = visitorMapper.selectMaps(new QueryWrapper<Visitor>()
                    .select("visit_date, COUNT(*) as cnt")
                    .ge("visit_date", today.minusDays(6))
                    .groupBy("visit_date").orderByAsc("visit_date"));
            m.put("byDay", byDay);
            // 最近7天按日统计（UV：去重 IP 数）
            List<Map<String, Object>> byDayUv = visitorMapper.selectMaps(new QueryWrapper<Visitor>()
                    .select("visit_date, COUNT(DISTINCT ip) as uv")
                    .ge("visit_date", today.minusDays(6))
                    .groupBy("visit_date").orderByAsc("visit_date"));
            m.put("byDayUv", byDayUv);
            return Result.success(m);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取访客汇总失败");
        }
    }

    @DeleteMapping("/clear")
    public Result<String> clear() {
        try {
            visitorMapper.delete(null);
            return Result.success("访客记录已清空");
        } catch (Exception e) {
            return Result.error("清空失败");
        }
    }
}
