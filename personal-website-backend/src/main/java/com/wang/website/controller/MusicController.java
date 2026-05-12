package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.website.common.Result;
import com.wang.website.entity.Music;
import com.wang.website.mapper.MusicMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/music")
@CrossOrigin
public class MusicController {

    @Autowired
    private MusicMapper musicMapper;

    @GetMapping("/page")
    public Result<IPage<Music>> getMusicPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            QueryWrapper<Music> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("create_time");
            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.and(w -> w.like("name", keyword)
                        .or().like("artist", keyword)
                        .or().like("album", keyword));
            }
            IPage<Music> result = musicMapper.selectPage(new Page<>(page, pageSize), wrapper);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取音乐列表失败");
        }
    }

    @GetMapping("/enabled")
    public Result<List<Music>> getEnabledMusic() {
        try {
            QueryWrapper<Music> wrapper = new QueryWrapper<>();
            wrapper.eq("enabled", 1);
            wrapper.orderByDesc("create_time");
            List<Music> list = musicMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取音乐列表失败");
        }
    }

    @GetMapping("/list")
    public Result<List<Music>> getAllMusic() {
        try {
            List<Music> list = musicMapper.selectList(null);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取音乐列表失败");
        }
    }

    @PostMapping("/add")
    public Result<String> addMusic(@RequestBody Music music) {
        try {
            music.setCreateTime(LocalDateTime.now());
            if (music.getEnabled() == null) {
                music.setEnabled(true);
            }
            musicMapper.insert(music);
            return Result.success("新增音乐成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("新增音乐失败");
        }
    }

    @PutMapping("/update")
    public Result<String> updateMusic(@RequestBody Music music) {
        try {
            musicMapper.updateById(music);
            return Result.success("更新音乐成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新音乐失败");
        }
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteMusic(@PathVariable Integer id) {
        try {
            musicMapper.deleteById(id);
            return Result.success("删除音乐成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除音乐失败");
        }
    }

    @PostMapping("/batchDelete")
    public Result<String> batchDelete(@RequestBody List<Integer> ids) {
        try {
            musicMapper.deleteBatchIds(ids);
            return Result.success("批量删除成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量删除失败");
        }
    }

    @PutMapping("/batchStatus")
    public Result<String> batchStatus(@RequestBody BatchStatusRequest request) {
        try {
            QueryWrapper<Music> wrapper = new QueryWrapper<>();
            wrapper.in("id", request.getIds());
            List<Music> list = musicMapper.selectList(wrapper);
            for (Music music : list) {
                music.setEnabled(request.getEnabled());
                musicMapper.updateById(music);
            }
            return Result.success("批量操作成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量操作失败");
        }
    }

    @Data
    public static class BatchStatusRequest {
        private List<Integer> ids;
        private Boolean enabled;
    }
}
