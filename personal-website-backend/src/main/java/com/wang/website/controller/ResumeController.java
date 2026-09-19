package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.website.common.Result;
import com.wang.website.entity.Resume;
import com.wang.website.entity.ResumeEntry;
import com.wang.website.mapper.ResumeEntryMapper;
import com.wang.website.mapper.ResumeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin
public class ResumeController {

    private static final Integer DEFAULT_RESUME_ID = 1;
    private static final List<String> ENTRY_TYPES = Arrays.asList("education", "work", "project");

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private ResumeEntryMapper resumeEntryMapper;

    @GetMapping("/get")
    public Result<Resume> getResume() {
        try {
            Resume resume = resumeMapper.selectById(DEFAULT_RESUME_ID);
            if (resume != null) {
                return Result.success(resume);
            }
            return Result.error("未找到简历信息，请检查数据库 ID 为 1 的记录");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取个人信息失败");
        }
    }

    @PostMapping("/update")
    public Result<String> updateResume(@RequestBody Resume resume) {
        try {
            Resume existingResume = resumeMapper.selectById(DEFAULT_RESUME_ID);
            resume.setId(DEFAULT_RESUME_ID);
            if (existingResume == null) {
                resume.setCreateTime(LocalDateTime.now());
                resume.setUpdateTime(LocalDateTime.now());
                resumeMapper.insert(resume);
            } else {
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

    @GetMapping("/entries")
    public Result<List<ResumeEntry>> listEntries(@RequestParam(required = false) String type) {
        try {
            QueryWrapper<ResumeEntry> wrapper = new QueryWrapper<ResumeEntry>()
                    .eq("resume_id", DEFAULT_RESUME_ID)
                    .orderByAsc("sort")
                    .orderByDesc("id");
            if (type != null && !type.isBlank()) {
                if (!ENTRY_TYPES.contains(type)) {
                    return Result.error("经历类型不合法");
                }
                wrapper.eq("type", type);
            }
            return Result.success(resumeEntryMapper.selectList(wrapper));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取经历列表失败");
        }
    }

    @PostMapping("/entries")
    public Result<ResumeEntry> addEntry(@RequestBody ResumeEntry entry) {
        if (!isValidEntry(entry)) {
            return Result.error("请填写经历类型和名称");
        }
        entry.setId(null);
        entry.setResumeId(DEFAULT_RESUME_ID);
        if (entry.getSort() == null) {
            entry.setSort(0);
        }
        resumeEntryMapper.insert(entry);
        return Result.success(entry);
    }

    @PutMapping("/entries/{id}")
    public Result<ResumeEntry> updateEntry(@PathVariable Integer id, @RequestBody ResumeEntry entry) {
        if (id == null || !isValidEntry(entry)) {
            return Result.error("请填写有效的经历信息");
        }
        ResumeEntry existing = resumeEntryMapper.selectById(id);
        if (existing == null || !DEFAULT_RESUME_ID.equals(existing.getResumeId())) {
            return Result.error("经历不存在");
        }
        entry.setId(id);
        entry.setResumeId(DEFAULT_RESUME_ID);
        resumeEntryMapper.updateById(entry);
        return Result.success(resumeEntryMapper.selectById(id));
    }

    @DeleteMapping("/entries/{id}")
    public Result<String> deleteEntry(@PathVariable Integer id) {
        ResumeEntry existing = resumeEntryMapper.selectById(id);
        if (existing == null || !DEFAULT_RESUME_ID.equals(existing.getResumeId())) {
            return Result.error("经历不存在");
        }
        resumeEntryMapper.deleteById(id);
        return Result.success("经历删除成功");
    }

    private boolean isValidEntry(ResumeEntry entry) {
        return entry != null
                && entry.getType() != null
                && ENTRY_TYPES.contains(entry.getType())
                && entry.getTitle() != null
                && !entry.getTitle().trim().isEmpty();
    }
}
