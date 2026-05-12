package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.website.common.Result;
import com.wang.website.entity.Project;
import com.wang.website.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/project")
@CrossOrigin
public class ProjectController {

    @Autowired
    private ProjectMapper projectMapper;

    @GetMapping("/list")
    public Result<List<Project>> list() {
        QueryWrapper<Project> wrapper = new QueryWrapper<Project>().orderByAsc("sort");
        return Result.success(projectMapper.selectList(wrapper));
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        List<Project> all = projectMapper.selectList(null);
        List<String> cats = all.stream()
                .map(Project::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return Result.success(cats);
    }

    @GetMapping("/search")
    public Result<List<Project>> search(@RequestParam String keyword) {
        QueryWrapper<Project> wrapper = new QueryWrapper<Project>()
                .like("title", keyword)
                .or()
                .like("description", keyword)
                .or()
                .like("tech_stack", keyword)
                .orderByAsc("sort");
        return Result.success(projectMapper.selectList(wrapper));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Project project) {
        if (project.getTitle() == null || project.getTitle().trim().isEmpty()) {
            return Result.error("项目名称不能为空");
        }
        projectMapper.insert(project);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Project project) {
        if (project.getId() == null) {
            return Result.error("参数错误");
        }
        projectMapper.updateById(project);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        projectMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
