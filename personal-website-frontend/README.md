# personal-website-frontend

个人网站前端，基于 Vue 3 + Vite + Element Plus 构建。

## 技术栈

- Vue 3 (Composition API + `<script setup>`)
- Vite 8
- Vue Router 4
- Pinia 3
- Element Plus 2.x
- ECharts 6.x
- @kangc/v-md-editor (Markdown 编辑与预览)
- Axios

## 项目结构

```
src/
├── views/                  # 页面组件
│   ├── admin/              #   后台管理页面
│   │   ├── AdminLayout.vue
│   │   ├── Dashboard.vue           # 仪表盘
│   │   ├── ArticleManagement.vue   # 文章列表
│   │   ├── ArticleEditor.vue       # Markdown 编辑器
│   │   ├── MusicManagement.vue     # 音乐管理
│   │   ├── MessageManagement.vue   # 留言管理
│   │   ├── CommentManagement.vue   # 评论管理
│   │   ├── CategoryManagement.vue  # 分类管理
│   │   ├── VisitorManagement.vue   # 访客管理
│   │   ├── OperationLogs.vue       # 操作日志
│   │   ├── Profile.vue             # 个人资料
│   │   └── SystemSettings.vue      # 系统设置
│   ├── Home.vue
│   ├── Blog.vue
│   ├── ArticleDetail.vue
│   ├── Resume.vue
│   ├── Projects.vue
│   ├── MessageBoard.vue
│   └── Login.vue
├── components/
│   ├── Navbar.vue           # 全局导航栏（含音乐胶囊播放器）
│   └── MarkdownEditor.vue   # 后台 Markdown 编辑器组件
├── router/
│   └── index.js             # 路由配置 + 鉴权守卫
├── stores/
│   └── admin.js             # 管理员状态
├── utils/
│   ├── auth.js              # 登录/登出/鉴权
│   ├── request.js           # Axios 封装
│   ├── theme.js             # 暗色主题切换
│   ├── musicPlayer.js       # 音乐播放器单例
│   └── statistic.js         # 页面访问统计
├── App.vue                  # 根组件（全局样式 + 暗色主题变量）
├── main.js
└── style.css
```

## 开发

```bash
npm install
npm run dev        # 启动开发服务器 → http://localhost:5173
npm run build      # 生产构建 → dist/
npm run preview    # 预览构建产物
```

## 路由一览

| 路径 | 页面 | 权限 |
|---|---|---|
| `/` | 个人主页 | 公开 |
| `/blog` | 博客列表 | 公开 |
| `/article/:id` | 文章详情 | 公开 |
| `/resume` | 简历 | 公开 |
| `/projects` | 项目展示 | 公开 |
| `/messages` | 留言板 | 公开 |
| `/login` | 管理员登录 | 公开 |
| `/admin/*` | 后台管理 | 需管理员 |

## 暗色主题

全部 CSS 变量定义在 `App.vue` 中，覆盖 Element Plus 官方变量 + 各业务页面样式。使用 `html.dark` 选择器统一控制，优先级高于 scoped 组件样式。`utils/theme.js` 通过 `html` 标签 class 切换并持久化到 localStorage。
