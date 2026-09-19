package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.website.common.Result;
import com.wang.website.entity.OperationLog;
import com.wang.website.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/log")
@CrossOrigin
public class OperationLogController {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /** 统一记录入口，供其他 Controller 静态调用 */
    public static void record(OperationLogMapper mapper, String username, String action, String detail, String ip) {
        try {
            OperationLog log = new OperationLog();
            log.setUsername(username == null ? "anonymous" : username);
            log.setAction(action);
            log.setDetail(detail == null ? "" : (detail.length() > 500 ? detail.substring(0, 500) : detail));
            log.setIp(ip == null ? "" : ip);
            log.setCreateTime(LocalDateTime.now());
            mapper.insert(log);
        } catch (Exception ignored) { }
    }

    @GetMapping("/page")
    public Result<Page<OperationLog>> page(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "15") Integer pageSize,
                                           @RequestParam(required = false) String action) {
        try {
            QueryWrapper<OperationLog> w = new QueryWrapper<>();
            if (action != null && !action.trim().isEmpty()) w.like("action", action);
            w.orderByDesc("create_time");
            return Result.success(operationLogMapper.selectPage(new Page<>(page, pageSize), w));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取操作日志失败");
        }
    }

    @DeleteMapping("/clear")
    public Result<String> clear() {
        try {
            operationLogMapper.delete(null);
            return Result.success("日志已清空");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("清空失败");
        }
    }
}
