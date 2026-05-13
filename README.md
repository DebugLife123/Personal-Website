# yu翔 · 个人网站

全栈个人品牌展示网站，包含公开展示页面与后台管理系统。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 · Vite 8 · Vue Router · Pinia · Axios |
| UI 框架 | Element Plus 2.x · ECharts 6.x |
| Markdown | @kangc/v-md-editor |
| 后端 | Spring Boot 3.3 · MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0 |
| API 文档 | Knife4j |
| 工具 | Lombok · Maven |

## 功能概览

### 前端公共页面

| 路由 | 页面 | 说明 |
|---|---|---|
| `/` | 个人主页 | 头像、社交链接、页面路由跳转 |
| `/blog` | 博客列表 | 文章卡片、阅读统计 |
| `/article/:id` | 文章详情 | Markdown 渲染、目录导航 |
| `/resume` | 简历 | 教育/工作经历、技能展示 |
| `/projects` | 项目展示 | 卡片筛选、技术标签 |
| `/messages` | 留言板 | 留言/回复/点赞/置顶，楼中楼对话 |
| `/login` | 管理员登录 | — |

### 后台管理 (`/admin/*`)

- **仪表盘** — 数据概览 + ECharts 图表
- **文章管理** — 文章 CRUD + Markdown 编辑器
- **分类管理** — 文章分类维护
- **评论管理** — 文章评论审核
- **留言管理** — 留言审核/置顶
- **音乐管理** — 音乐上传/启禁用/播放预览
- **访客管理** — 访问统计
- **操作日志** — 后台操作记录
- **个人资料** — 管理员信息维护
- **系统设置** — 站点配置

### 特色

- **深色主题** — 全量覆盖的自定义暗色调色板，适配所有 Element Plus 组件及业务页面
- **路由守卫** — 前端鉴权，未登录跳转登录页，非管理员禁止访问后台
- **音乐播放器** — Navbar 胶囊式播放控件，数据库驱动，后台管理曲库
- **留言互动** — 支持楼中楼回复、点赞、置顶，CSS 动画过渡

## 项目结构

```
Personal-Website/
├── personal-website-frontend/    # Vue 3 前端
│   ├── src/
│   │   ├── views/               # 页面组件
│   │   │   ├── admin/           # 后台管理页面
│   │   │   ├── Home.vue         # 个人主页
│   │   │   ├── Blog.vue         # 博客列表
│   │   │   ├── ArticleDetail.vue# 文章详情
│   │   │   ├── Resume.vue       # 简历
│   │   │   ├── Projects.vue     # 项目展示
│   │   │   ├── MessageBoard.vue # 留言板
│   │   │   └── Login.vue        # 登录
│   │   ├── components/          # 公共组件
│   │   ├── router/              # 路由配置
│   │   ├── stores/              # Pinia 状态
│   │   └── utils/               # 工具函数 (auth/music/request/theme)
│   ├── public/                  # 静态资源
│   ├── vite.config.js
│   └── package.json
│
└── personal-website-backend/     # Spring Boot 后端
    └── src/main/
        ├── java/com/wang/website/
        │   ├── controller/      # REST API
        │   ├── entity/          # 数据实体
        │   ├── mapper/          # MyBatis-Plus Mapper
        │   ├── config/          # 配置类
        │   └── common/          # 统一响应 Result
        └── resources/
            ├── application.yml  # 应用配置
            └── init.sql         # 数据库初始化脚本
```

## 快速开始

### 环境要求

- Node.js ≥ 18
- Java 17
- MySQL 8.0
- Maven 3.8+

### 1. 初始化数据库

在 MySQL 中执行 `personal-website-backend/src/main/resources/init.sql`，会自动建库建表并插入示例数据。

### 2. 启动后端

```bash
cd personal-website-backend
./mvnw spring-boot:run      # Linux/macOS
mvnw.cmd spring-boot:run     # Windows
```

后端默认运行在 `http://localhost:8080`，Knife4j 文档地址为 `http://localhost:8080/doc.html`。

### 3. 启动前端

```bash
cd personal-website-frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`。

### 4. 访问

- 前台：`http://localhost:5173`
- 后台：`http://localhost:5173/login` → 登录后跳转 `/admin`
