package com.wang.website.controller;

import com.wang.website.entity.Resume;
import com.wang.website.mapper.ResumeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.wang.website.common.Result;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin // 解决前后端跨域问题 [cite: 95]
public class ResumeController {

    @Autowired
    private ResumeMapper resumeMapper;

    @GetMapping("/get")
    public Result<Resume> getResume() {
        try {
            Resume resume = resumeMapper.selectById(1);
            if (resume != null) {
                return Result.success(resume);
            } else {
                return Result.error("未找到简历信息，请检查数据库 ID 为 1 的记录");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取个人信息失败");
        }
    }

    @PostMapping("/update")
    public Result<String> updateResume(@RequestBody Resume resume) {
        try {
            Resume existingResume = resumeMapper.selectById(1);
            if (existingResume == null) {
                // 如果不存在，创建一个新的
                resume.setId(1);
                resume.setCreateTime(LocalDateTime.now());
                resume.setUpdateTime(LocalDateTime.now());
                resumeMapper.insert(resume);
            } else {
                // 如果存在，更新
                resume.setId(1);
                resume.setCreateTime(existingResume.getCreateTime());
                resume.setUpdateTime(LocalDateTime.now());
                resumeMapper.updateById(resume);
            }
            return Result.success("个人信息更新成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新个人信息失败");
        }
    }
}