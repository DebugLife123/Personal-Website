package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.website.common.Result;
import com.wang.website.entity.Article;
import com.wang.website.mapper.ArticleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/article")
@CrossOrigin
public class ArticleController {

    @Autowired
    private ArticleMapper articleMapper;

    // 获取文章列表（不分页）
    @GetMapping("/list")
    public Result<List<Article>> getArticleList() {
        try {
            List<Article> list = articleMapper.selectList(null);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取文章列表失败");
        }
    }

    // 分页查询文章（支持搜索/分类/日期过滤）
    @GetMapping("/page")
    public Result<IPage<Article>> getArticlePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("create_time");

            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.and(w -> w.like("title", keyword)
                        .or().like("summary", keyword)
                        .or().like("tags", keyword));
            }
            if (category != null && !category.trim().isEmpty()) {
                wrapper.eq("category", category);
            }
            if (status != null && !status.trim().isEmpty()) {
                wrapper.eq("status", status);
            }
            if (startDate != null && !startDate.trim().isEmpty()) {
                wrapper.ge("create_time", startDate + " 00:00:00");
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                wrapper.le("create_time", endDate + " 23:59:59");
            }

            IPage<Article> result = articleMapper.selectPage(new Page<>(page, pageSize), wrapper);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取文章列表失败");
        }
    }

    // 根据分类获取文章列表
    @GetMapping("/listByCategory")
    public Result<List<Article>> getArticleListByCategory(@RequestParam String category) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<Article>().eq("category", category);
            List<Article> list = articleMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取文章列表失败");
        }
    }

    // 根据标签获取文章列表
    @GetMapping("/listByTag")
    public Result<List<Article>> getArticleListByTag(@RequestParam String tag) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<Article>().like("tags", tag);
            List<Article> list = articleMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取文章列表失败");
        }
    }

    // 获取所有分类
    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        try {
            List<Article> articles = articleMapper.selectList(null);
            List<String> categories = articles.stream()
                    .map(Article::getCategory)
                    .distinct()
                    .filter(c -> c != null && !c.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            return Result.success(categories);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取分类列表失败");
        }
    }

    // 获取所有标签
    @GetMapping("/tags")
    public Result<List<String>> getTags() {
        try {
            List<Article> articles = articleMapper.selectList(null);
            List<String> tags = articles.stream()
                    .map(Article::getTags)
                    .filter(t -> t != null && !t.isEmpty())
                    .flatMap(t -> java.util.Arrays.stream(t.split(",")))
                    .map(String::trim)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            return Result.success(tags);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取标签列表失败");
        }
    }

    // 搜索文章
    @GetMapping("/search")
    public Result<List<Article>> searchArticles(@RequestParam String keyword) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<Article>()
                    .like("title", keyword)
                    .or()
                    .like("summary", keyword)
                    .or()
                    .like("content", keyword)
                    .or()
                    .like("tags", keyword);
            List<Article> list = articleMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("搜索文章失败");
        }
    }

    // 新增文章
    @PostMapping("/add")
    public Result<String> addArticle(@RequestBody Article article) {
        try {
            article.setViews(0);
            if (article.getStatus() == null || article.getStatus().isEmpty()) {
                article.setStatus("已发布");
            }
            articleMapper.insert(article);
            return Result.success("文章发布成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文章发布失败");
        }
    }

    // 更新文章
    @PutMapping("/update")
    public Result<String> updateArticle(@RequestBody Article article) {
        try {
            articleMapper.updateById(article);
            return Result.success("文章更新成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文章更新失败");
        }
    }

    // 删除文章
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteArticle(@PathVariable Integer id) {
        try {
            articleMapper.deleteById(id);
            return Result.success("文章删除成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文章删除失败");
        }
    }

    // 根据 ID 获取文章详情（前台用，阅读量+1）
    @GetMapping("/get")
    public Result<Article> getArticleById(@RequestParam Integer id) {
        try {
            Article article = articleMapper.selectById(id);
            if (article != null) {
                article.setViews(article.getViews() + 1);
                articleMapper.updateById(article);
                return Result.success(article);
            }
            return Result.error("文章不存在");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取文章详情失败");
        }
    }

    // 根据 ID 获取文章详情（管理后台用，不增加阅读量）
    @GetMapping("/getById")
    public Result<Article> getArticleByIdAdmin(@RequestParam Integer id) {
        try {
            Article article = articleMapper.selectById(id);
            if (article != null) {
                return Result.success(article);
            }
            return Result.error("文章不存在");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取文章详情失败");
        }
    }
}
