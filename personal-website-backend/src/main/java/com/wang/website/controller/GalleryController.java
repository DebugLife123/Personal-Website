package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.website.common.Result;
import com.wang.website.entity.Gallery;
import com.wang.website.mapper.GalleryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图库接口。
 *
 * 前台：/api/gallery/list 只返回可见照片（无需登录，见 AuthInterceptor 公开白名单）；
 * 后台：/api/gallery/admin/** 需要管理员 Token（默认规则，非白名单即要求管理员）。
 */
@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    @Autowired
    private GalleryMapper galleryMapper;

    // ==================== 前台 ====================

    /** 可见照片列表（排序值升序，其次按上传时间倒序） */
    @GetMapping("/list")
    public Result<List<Gallery>> list() {
        QueryWrapper<Gallery> wrapper = new QueryWrapper<Gallery>()
                .eq("status", "visible")
                .orderByAsc("sort")
                .orderByDesc("create_time");
        return Result.success(galleryMapper.selectList(wrapper));
    }

    // ==================== 后台 ====================

    /** 分页查询（含隐藏照片，可按状态/关键词筛选） */
    @GetMapping("/admin/page")
    public Result<IPage<Gallery>> adminPage(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status) {
        QueryWrapper<Gallery> wrapper = new QueryWrapper<>();
        if (status != null && !status.trim().isEmpty() && !"all".equals(status)) {
            wrapper.eq("status", status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("title", keyword)
                    .or().like("description", keyword)
                    .or().like("location", keyword));
        }
        wrapper.orderByAsc("sort").orderByDesc("create_time");
        return Result.success(galleryMapper.selectPage(new Page<>(page, pageSize), wrapper));
    }

    /** 新增照片 */
    @PostMapping("/add")
    public Result<String> add(@RequestBody Gallery gallery) {
        String invalid = validate(gallery);
        if (invalid != null) return Result.error(invalid);
        if (gallery.getSort() == null) gallery.setSort(0);
        if (gallery.getStatus() == null || gallery.getStatus().isEmpty()) gallery.setStatus("visible");
        galleryMapper.insert(gallery);
        return Result.success("已添加");
    }

    /** 修改照片信息 */
    @PutMapping("/update")
    public Result<String> update(@RequestBody Gallery gallery) {
        if (gallery.getId() == null) return Result.error("参数错误");
        String invalid = validate(gallery);
        if (invalid != null) return Result.error(invalid);
        galleryMapper.updateById(gallery);
        return Result.success("已保存");
    }

    /** 显示 / 隐藏 */
    @PutMapping("/admin/status/{id}")
    public Result<String> updateStatus(@PathVariable Integer id, @RequestParam String status) {
        Gallery gallery = galleryMapper.selectById(id);
        if (gallery == null) return Result.error("照片不存在");
        if (!"visible".equals(status) && !"hidden".equals(status)) return Result.error("非法状态");
        gallery.setStatus(status);
        galleryMapper.updateById(gallery);
        return Result.success("visible".equals(status) ? "已显示" : "已隐藏");
    }

    /** 删除照片（只删记录，不动 uploads 里的文件） */
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        if (galleryMapper.deleteById(id) == 0) return Result.error("照片不存在");
        return Result.success("已删除");
    }

    private String validate(Gallery gallery) {
        if (gallery.getUrl() == null || gallery.getUrl().trim().isEmpty()) {
            return "请先上传照片或填写图片地址";
        }
        if (gallery.getDescription() != null && gallery.getDescription().length() > 500) {
            return "简介最长 500 字";
        }
        if (gallery.getTitle() != null && gallery.getTitle().length() > 100) {
            return "标题最长 100 字";
        }
        return null;
    }
}
