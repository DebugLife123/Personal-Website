# yu翔 · 个人品牌网站

全栈个人品牌网站：展示博客文章、项目作品、简历与音乐，支持访客留言互动与完整的后台管理。

**在线演示**：`http://103.39.64.76:24142`（NAT 转发至服务器内网 8086 端口）

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 · Vite 8 (Rolldown) · Vue Router · Pinia · Axios |
| UI / 图表 | Element Plus 2.x · ECharts 6.x |
| Markdown | @kangc/v-md-editor |
| 后端 | Spring Boot 3.3 · MyBatis-Plus 3.5（Java 17） |
| 数据库 | MySQL 8.0（utf8mb4） |
| API 文档 | Knife4j（开发环境 `http://localhost:8090/doc.html`） |
| 部署 | Spring Boot 一体化托管（前端静态产物 + API 单进程），systemd 常驻 |

## 功能概览

### 公开页面

| 路由 | 页面 | 说明 |
|---|---|---|
| `/` | 个人主页 | 头像、社交链接、路由入口 |
| `/blog` | 博客列表 | 文章卡片、分类筛选、阅读统计 |
| `/article/:id` | 文章详情 | Markdown 渲染、浏览计数 |
| `/projects` | 项目展示 | 卡片网格、分类筛选、关键词检索、详情弹窗 |
| `/resume` | 简历 | 教育/实习/项目经历时间线、技能墙 |
| `/messages` | 留言板 | 留言/回复/点赞/置顶，楼中楼对话 |
| `/login` | 登录 | 管理员 / 游客 |

### 后台管理（`/admin/*`）

- **仪表盘** — PV/UV 趋势、文章分类分布（真实数据）
- **文章 / 分类 / 评论 / 留言 / 音乐 / 访客 / 操作日志 / 个人资料 / 系统设置**

### 特色

- **深色主题** — 全量自定义暗色调色板，覆盖 Element Plus 组件与业务页面
- **音乐播放器** — Navbar 胶囊式播放控件，后台管理曲库
- **白名单式鉴权** — 后端拦截器公开接口白名单，管理操作强制 Token 校验并回源确认
- **数据安全** — BCrypt 密码、上传类型白名单、计数器原子自增（`ON DUPLICATE KEY UPDATE`）、公开接口不泄露草稿与待审核内容
- **密钥隔离** — 仓库不含真实密码：本地用 `application-local.yml`（gitignored），服务器用环境变量

## 项目结构

```
├─ personal-website-frontend/      # Vue 3 前端（Vite）
│  ├─ src/views/                   # 公开页面
│  ├─ src/views/admin/             # 后台管理页面
│  └─ src/utils/                   # auth / request / musicPlayer / theme 等
├─ personal-website-backend/       # Spring Boot 后端（Maven）
│  ├─ src/main/java/com/wang/website/
│  │  ├─ controller/  mapper/  entity/  config/  common/
│  │  └─ PersonalWebsiteBackendApplication.java
│  ├─ src/main/resources/
│  │  ├─ application.yml           # 公共配置（密钥走占位符）
│  │  ├─ application-local.example.yml
│  │  └─ init.sql                  # 建库 + 种子数据（幂等）
│  └─ uploads/                     # 运行时上传目录（gitignore）
└─ deploy/                         # 服务器部署脚本与 systemd 单元
```

## 本地开发

**前置**：JDK 17+、Node 18+、MySQL 8

```bash
# 1. 初始化数据库（幂等，可随时重跑）
mysql -u root -p < personal-website-backend/src/main/resources/init.sql

# 2. 配置本地数据库密码（不进 Git）
cp personal-website-backend/src/main/resources/application-local.example.yml \
   personal-website-backend/src/main/resources/application-local.yml
# 编辑 application-local.yml，填入本地 MySQL 密码

# 3. 启动后端（http://localhost:8090）
cd personal-website-backend
mvnw.cmd spring-boot:run        # Windows；Linux/macOS 用 ./mvnw

# 4. 启动前端（http://localhost:5174，/api 与 /uploads 自动代理到 8090）
cd personal-website-frontend
npm install
npm run dev
```

> 首次登录后台后请**立即修改管理员密码**（初始账号见 `init.sql` 的 `admin_user` 种子）。

## 服务器部署（Ubuntu 22.04 示例）

一体化方案：一个 Java 进程同时提供前端页面与 API，不依赖 Nginx。

```bash
# 0. 依赖
apt-get update && apt-get install -y openjdk-17-jre-headless mysql-server

# 1. 建库与专用账号（示例密码务必替换）
mysql -e "CREATE DATABASE IF NOT EXISTS personal_website DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
          CREATE USER IF NOT EXISTS 'website'@'localhost' IDENTIFIED BY 'YOUR_DB_PASSWORD';
          GRANT ALL PRIVILEGES ON personal_website.* TO 'website'@'localhost'; FLUSH PRIVILEGES;"

# 2. 本地产物
cd personal-website-backend && mvnw.cmd -q package -DskipTests   # → target/*.jar
cd ../personal-website-frontend && npm run build                  # → dist/
mysqldump -u root -p --databases personal_website > personal_website.sql   # 首次部署带数据

# 3. 目录约定
#   /opt/personal-website/
#   ├─ app.jar          ← 后端产物
#   ├─ web/             ← 前端 dist 内容
#   ├─ uploads/         ← 上传文件（运行期追加）
#   └─ app.env          ← 环境变量（参照 deploy/app.env.example，chmod 600）

# 4. 安装并启动（setup.sh 幂等，可重复执行）
scp -P 56220 app.jar      root@<server>:/opt/personal-website/
scp -P 56220 -r dist/.    root@<server>:/opt/personal-website/web/
scp -P 56220 -r uploads/. root@<server>:/opt/personal-website/uploads/
scp -P 56220 personal_website.sql deploy/app.env deploy/personal-website.service deploy/setup.sh root@<server>:/tmp/psite/
ssh -p 56220 root@<server> 'bash /tmp/psite/setup.sh'
```

日常升级只需替换 `app.jar` 与 `web/` 后 `systemctl restart personal-website`；
数据库导入仅在建库为空时执行，不会覆盖线上数据。

## 演示环境清单

| 项 | 值 |
|---|---|
| 应用目录 | `/opt/personal-website/` |
| 服务 | `systemctl status personal-website`（日志：`/opt/personal-website/logs/app.log`） |
| 端口 | 内网 `8086`（外网经 NAT `103.39.64.76:24142` 转发） |
| 数据库 | `personal_website`，专用账号 `website@localhost`（仅授权本站库） |

## 许可

MIT
