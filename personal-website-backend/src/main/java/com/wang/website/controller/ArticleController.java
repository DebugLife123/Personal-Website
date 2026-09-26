package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.website.common.Result;
import com.wang.website.entity.Article;
import com.wang.website.mapper.ArticleMapper;
import com.wang.website.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/article")
@CrossOrigin
public class ArticleController {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private com.wang.website.mapper.OperationLogMapper operationLogMapper;

    @Autowired
    private com.wang.website.mapper.StatisticMapper statisticMapper;

    /** 是否管理员请求（由 AuthInterceptor 解析 Token 后写入） */
    private boolean isAdmin(HttpServletRequest request) {
        return request != null && request.getAttribute("adminUser") != null;
    }

    /** 非管理员强制只看已发布文章 */
    private void publishedOnly(QueryWrapper<Article> wrapper, HttpServletRequest request) {
        if (!isAdmin(request)) {
            wrapper.eq("status", "已发布");
        }
    }

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

    // 分页查询文章（支持搜索/分类/日期过滤；非管理员只能看已发布）
    @GetMapping("/page")
    public Result<IPage<Article>> getArticlePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("create_time");
            publishedOnly(wrapper, request);

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
    public Result<List<Article>> getArticleListByCategory(@RequestParam String category, HttpServletRequest request) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<Article>().eq("category", category);
            publishedOnly(wrapper, request);
            List<Article> list = articleMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取文章列表失败");
        }
    }

    // 根据标签获取文章列表
    @GetMapping("/listByTag")
    public Result<List<Article>> getArticleListByTag(@RequestParam String tag, HttpServletRequest request) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<Article>().like("tags", tag);
            publishedOnly(wrapper, request);
            List<Article> list = articleMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取文章列表失败");
        }
    }

    // 获取所有分类（非管理员只统计已发布文章）
    @GetMapping("/categories")
    public Result<List<String>> getCategories(HttpServletRequest request) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            publishedOnly(wrapper, request);
            List<Article> articles = articleMapper.selectList(wrapper);
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

    // 获取所有标签（非管理员只统计已发布文章）
    @GetMapping("/tags")
    public Result<List<String>> getTags(HttpServletRequest request) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            publishedOnly(wrapper, request);
            List<Article> articles = articleMapper.selectList(wrapper);
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
    public Result<List<Article>> searchArticles(@RequestParam String keyword, HttpServletRequest request) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<Article>()
                    .like("title", keyword)
                    .or()
                    .like("summary", keyword)
                    .or()
                    .like("content", keyword)
                    .or()
                    .like("tags", keyword);
            publishedOnly(wrapper, request);
            List<Article> list = articleMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("搜索文章失败");
        }
    }

    // 新增文章
    @PostMapping("/add")
    public Result<String> addArticle(@RequestBody Article article, HttpServletRequest request) {
        try {
            article.setViews(0);
            if (article.getStatus() == null || article.getStatus().isEmpty()) {
                article.setStatus("已发布");
            }
            articleMapper.insert(article);
            log(request, "发布文章", article.getTitle());
            return Result.success("文章发布成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文章发布失败");
        }
    }

    // 更新文章
    @PutMapping("/update")
    public Result<String> updateArticle(@RequestBody Article article, HttpServletRequest request) {
        try {
            articleMapper.updateById(article);
            log(request, "更新文章", article.getTitle());
            return Result.success("文章更新成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文章更新失败");
        }
    }

    // 删除文章
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteArticle(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Article a = articleMapper.selectById(id);
            articleMapper.deleteById(id);
            log(request, "删除文章", a != null ? a.getTitle() : ("id=" + id));
            return Result.success("文章删除成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文章删除失败");
        }
    }

    // 根据 ID 获取文章详情（前台用，阅读量原子 +1，并同步累加站点文章阅读统计）
    @GetMapping("/get")
    public Result<Article> getArticleById(@RequestParam Integer id, HttpServletRequest request) {
        try {
            Article article = articleMapper.selectById(id);
            // 草稿对非管理员不可见
            if (article == null || ("草稿".equals(article.getStatus()) && !isAdmin(request))) {
                return Result.error("文章不存在");
            }
            articleMapper.incrViews(id);
            statisticMapper.recordArticleRead();
            article.setViews((article.getViews() == null ? 0 : article.getViews()) + 1);
            return Result.success(article);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取文章详情失败");
        }
    }

    private void log(HttpServletRequest request, String action, String detail) {
        String username = (String) request.getAttribute("adminUser");
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        OperationLogController.record(operationLogMapper, username, action, detail, ip);
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
