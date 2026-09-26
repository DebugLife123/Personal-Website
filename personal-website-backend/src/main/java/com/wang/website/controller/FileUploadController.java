package com.wang.website.controller;

import com.wang.website.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传（需要登录 Token，见 AuthInterceptor 白名单）。
 * 只允许白名单扩展名 + 匹配的 MIME 类型，返回相对路径（域名/协议交给部署层）。
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    /** 允许的扩展名（小写，含点） */
    private static final Map<String, Set<String>> ALLOWED_EXT = Map.of(
            "image", Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp"),
            "audio", Set.of(".mp3", ".wav", ".m4a", ".flac", ".ogg")
    );

    /** 存储子目录名 */
    private static final Map<String, String> DIRS = Map.of(
            "image", "images",
            "audio", "audio"
    );

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return store(file, "image");
    }

    @PostMapping("/audio")
    public Result<String> uploadAudio(@RequestParam("file") MultipartFile file) {
        return store(file, "audio");
    }

    private Result<String> store(MultipartFile file, String kind) {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 扩展名校验
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXT.get(kind).contains(ext)) {
            return Result.error("不支持的文件类型：" + (ext.isEmpty() ? "无扩展名" : ext));
        }

        // MIME 类型粗校验（与文件类别匹配）
        String contentType = file.getContentType();
        boolean mimeOk = contentType != null && (contentType.startsWith(kind + "/")
                || ("audio".equals(kind) && "application/ogg".equals(contentType)));
        if (!mimeOk) {
            return Result.error("文件内容与类型不匹配");
        }

        try {
            String userDir = System.getProperty("user.dir");
            String uploadDir = userDir + File.separator + "uploads" + File.separator + DIRS.get(kind) + File.separator;
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = UUID.randomUUID().toString() + ext;
            File dest = new File(uploadDir + fileName);
            file.transferTo(dest);

            // 只返回相对路径，避免硬编码域名/端口写死进数据库
            return Result.success("/uploads/" + DIRS.get(kind) + "/" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
