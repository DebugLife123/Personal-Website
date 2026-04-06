package com.wang.website.controller;

import com.wang.website.entity.Resume;
import com.wang.website.mapper.ResumeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.wang.website.common.Result;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin // 解决前后端跨域问题 [cite: 95]
public class ResumeController {

    @Autowired
    private ResumeMapper resumeMapper;

    @GetMapping("/get")
    public Result<Resume> getResume() {
        Resume resume = resumeMapper.selectById(1);
        if (resume != null) {
            return Result.success(resume);
        } else {
            return Result.error("未找到简历信息，请检查数据库 ID 为 1 的记录");
        }
    }
}