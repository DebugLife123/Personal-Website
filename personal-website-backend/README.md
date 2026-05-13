# personal-website-backend

个人网站后端服务，基于 Spring Boot 3 + MyBatis-Plus + MySQL 构建。

## 技术栈

- Spring Boot 3.3
- MyBatis-Plus 3.5
- MySQL 8.0
- Knife4j (OpenAPI 文档)
- Lombok
- Maven

## 项目结构

```
src/main/java/com/wang/website/
├── PersonalWebsiteBackendApplication.java   # 启动类 + @MapperScan
├── common/
│   └── Result.java                          # 统一响应封装 {code, message, data}
├── config/
│   ├── MybatisPlusConfig.java               # MyBatis-Plus 分页插件
│   └── WebMvcConfig.java                    # CORS / 静态资源映射
├── controller/
│   ├── ArticleController.java               # 文章 CRUD
│   ├── MessageController.java               # 留言板 API
│   ├── ProjectController.java               # 项目展示
│   ├── ResumeController.java                # 简历信息
│   ├── StatisticController.java             # 访问统计
│   ├── UserController.java                  # 用户登录
│   ├── MusicController.java                 # 音乐管理
│   └── FileUploadController.java            # 文件上传
├── entity/
│   ├── Article.java
│   ├── Message.java
│   ├── Project.java
│   ├── Resume.java
│   ├── Statistic.java
│   ├── User.java
│   └── Music.java
└── mapper/
    ├── ArticleMapper.java
    ├── MessageMapper.java
    ├── ProjectMapper.java
    ├── ResumeMapper.java
    ├── StatisticMapper.java
    ├── UserMapper.java
    └── MusicMapper.java

src/main/resources/
├── application.yml      # 数据库连接 / MyBatis-Plus 配置 / 文件上传限制
└── init.sql             # 数据库初始化（建库建表 + 示例数据）
```

## API 概览

| 模块 | 路径前缀 | 说明 |
|---|---|---|
| 文章 | `/api/article` | 分页查询、详情、新增、更新、删除 |
| 留言 | `/api/message` | 留言 CRUD、点赞、置顶、楼中楼回复 |
| 项目 | `/api/project` | 项目列表、详情 |
| 简历 | `/api/resume` | 简历查询、更新 |
| 统计 | `/api/statistic` | 页面访问统计 |
| 用户 | `/api/user` | 登录认证 |
| 音乐 | `/api/music` | 音乐 CRUD、批量操作 |
| 文件 | `/api/upload` | 图片/音频上传 |

## 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

- `code` 200 表示成功，其他值表示异常
- `data` 泛型，根据接口返回不同结构

## 快速开始

### 1. 初始化数据库

```sql
source src/main/resources/init.sql
```

会自动创建 `personal_website` 库、所有业务表及示例数据。

### 2. 修改数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/personal_website?...
    username: root
    password: 你的密码
```

### 3. 启动

```bash
./mvnw spring-boot:run      # Linux/macOS
mvnw.cmd spring-boot:run     # Windows
```

服务运行在 `http://localhost:8080`。

### 4. API 文档

启动后访问 `http://localhost:8080/doc.html` 查看 Knife4j 接口文档。

## 文件上传

上传的文件存储在项目运行目录下的 `uploads/` 文件夹：

```
uploads/
├── images/    # 文章封面、头像等
└── audio/     # 音乐文件

# 访问路径
http://localhost:8080/uploads/images/xxx.jpg
http://localhost:8080/uploads/audio/xxx.mp3
```

文件自动以 UUID 重命名，避免冲突。
