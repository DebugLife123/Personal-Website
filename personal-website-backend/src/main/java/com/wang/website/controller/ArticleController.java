package com.wang.website.controller;

import com.wang.website.common.Result;
import com.wang.website.entity.Article;
import com.wang.website.mapper.ArticleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/article")
@CrossOrigin // 允许前端跨域访问
public class ArticleController {

    @Autowired
    private ArticleMapper articleMapper;

    // 获取文章列表接口
    @GetMapping("/list")
    public Result<List<Article>> getArticleList() {
        // 使用 MyBatis-Plus 直接查询所有文章
        List<Article> list = articleMapper.selectList(null);
        return Result.success(list);
    }
    // 根据 ID 获取文章详情
    @GetMapping("/get")
    public Result<Article> getArticleById(@RequestParam Integer id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            // 每次访问详情，阅读量 +1 (可选增强功能)
            article.setViews(article.getViews() + 1);
            articleMapper.updateById(article);
            return Result.success(article);
        }
        return Result.error("文章不存在");
    }
}