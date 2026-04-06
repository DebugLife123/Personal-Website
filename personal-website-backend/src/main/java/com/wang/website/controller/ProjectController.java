package com.wang.website.controller;

import com.wang.website.common.Result;
import com.wang.website.entity.Project;
import com.wang.website.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/project")
@CrossOrigin
public class ProjectController {

    @Autowired
    private ProjectMapper projectMapper;

    @GetMapping("/list")
    public Result<List<Project>> getProjectList() {
        // 查询所有项目，按 sort 字段排序
        List<Project> list = projectMapper.selectList(null);
        return Result.success(list);
    }
}