<script setup>
import { onMounted, ref } from 'vue'
import { recordPageView } from './utils/statistic'
import { initTheme, toggleTheme, getTheme } from './utils/theme'
import Navbar from './components/Navbar.vue'

const isDarkMode = ref(getTheme() === 'dark')

const handleThemeToggle = () => {
  const newTheme = toggleTheme()
  isDarkMode.value = newTheme === 'dark'
}

onMounted(() => {
  recordPageView()
  initTheme()
})
</script>

<template>
  <div class="app-wrapper" :class="{ 'dark-mode': isDarkMode }">
    <Navbar v-if="!$route.meta?.hideNav" @toggle-theme="handleThemeToggle" :is-dark-mode="isDarkMode" />
    <router-view v-slot="{ Component, route }">
      <transition name="fade" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </transition>
    </router-view>
  </div>
</template>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Arial, sans-serif;
  background-color: #f8f9fa;
  -webkit-font-smoothing: antialiased;
}

/* Smooth theme transitions */
body {
  transition: background-color 0.35s ease;
}
.app-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  transition: background-color 0.35s ease, color 0.35s ease;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-thumb {
  background-color: #ccc;
  border-radius: 10px;
}
::-webkit-scrollbar-track {
  background: transparent;
}

/* ============================================================
   🌙 沉浸式暗色主题
   — 低饱和冷调深色基底，温暖文字，层次分明
   ============================================================ */
.dark-mode {
  /* ---- 页面级 CSS 设计变量 ---- */
  --bg-page:       #09090c;
  --bg-card:       #131316;
  --bg-elevated:   #1a1a20;
  --bg-hover:      #22222a;
  --border:        #24242c;
  --border-light:  #1a1a22;
  --text-primary:  #ece8e4;
  --text-secondary:#c8c4cc;     /* 提亮：AA 7:1+ ✓ */
  --text-muted:    #9896a2;      /* 提亮：AA 4.5:1 ✓ 适合小字 */
  --shadow-card:   0 4px 24px rgba(0,0,0,0.5);
  --shadow-elevated: 0 8px 40px rgba(0,0,0,0.6);

  background-color: var(--bg-page);
  color: var(--text-primary);
}
.dark-mode .app-wrapper {
  background-color: var(--bg-page);
}

/* ---- Element Plus 官方暗色模式 CSS 变量覆盖 ----
     开启 EP 内置暗色模式后，所有 EP 组件自动适配。
     以下覆盖将其颜色对齐到自定义调色板。             */
html.dark {
  /* 背景层 */
  --el-bg-color: #09090c;
  --el-bg-color-overlay: #131316;
  --el-fill-color: #131316;
  --el-fill-color-light: #1a1a20;
  --el-fill-color-lighter: #22222a;
  --el-fill-color-blank: #09090c;
  --el-fill-color-dark: #0d0d10;
  --el-mask-color: rgba(0, 0, 0, 0.6);

  /* 边框 */
  --el-border-color: #24242c;
  --el-border-color-light: #1e1e26;
  --el-border-color-lighter: #2a2a34;
  --el-border-color-extra-light: #1a1a22;
  --el-border-color-dark: #2e2e3a;

  /* 文字 */
  --el-text-color-primary: #ece8e4;
  --el-text-color-regular: #c8c4cc;
  --el-text-color-secondary: #9896a2;
  --el-text-color-placeholder: #6a6878;
  --el-text-color-disabled: #4a4854;

  /* 输入框 */
  --el-input-bg-color: #101014;
  --el-input-border-color: #24242c;
  --el-input-hover-border-color: #3a3a46;
  --el-input-focus-border-color: #409eff;

  /* 文字标签 */
  --el-tag-bg-color: #16161e;
  --el-tag-border-color: #262632;

  /* 卡片 */
  --el-card-bg-color: #131316;
  --el-card-border-color: #24242c;

  /* 弹窗 */
  --el-dialog-bg-color: #131316;
  --el-dialog-title-font-color: #ece8e4;

  /* 下拉菜单 */
  --el-select-dropdown-bg-color: #1a1a20;
  --el-select-dropdown-border-color: #24242c;

  /* 弹出框/气泡 */
  --el-popover-bg-color: #1a1a20;
  --el-popover-border-color: #24242c;

  /* 表格 */
  --el-table-bg-color: #131316;
  --el-table-tr-bg-color: #131316;
  --el-table-header-bg-color: #1a1a20;
  --el-table-border-color: #24242c;
  --el-table-row-hover-bg-color: #1a1a20;

  /* 菜单 */
  --el-menu-bg-color: #131316;
  --el-menu-item-hover-fill: #1a1a20;

  /* 消息提示 */
  --el-message-bg-color: #1a1a20;
  --el-message-border-color: #24242c;

  /* 通知 */
  --el-notification-bg-color: #131316;

  /* 分页 */
  --el-pagination-bg-color: transparent;

  /* 步骤条 */
  --el-step-title-font-color: #a8a4b0;
  --el-step-description-font-color: #6a6878;

  /* 进度条 */
  --el-progress-text-color: #c8c4cc;

  /* 开关 */
  --el-switch-off-color: #2a2a34;
  --el-switch-border-color: #2a2a34;

  /* 评分 */
  --el-rate-icon-color: #2a2a34;

  /* 上传 */
  --el-upload-list-item-file-name-color: #a8a4b0;

  /* 空状态 */
  --el-empty-fill-color-0: #1a1a20;
  --el-empty-fill-color-1: #22222a;
  --el-empty-fill-color-2: #2a2a34;
  --el-empty-fill-color-3: #32323e;
  --el-empty-fill-color-4: #3a3a48;
  --el-empty-fill-color-5: #424250;
  --el-empty-fill-color-6: #4a4a58;
  --el-empty-fill-color-7: #525262;
  --el-empty-fill-color-8: #5a5a6c;

  /* 级联选择器 (如有使用) */
  --el-cascader-menu-bg-color: #1a1a20;
  --el-cascader-menu-selected-bg-color: #22222a;

  /* 时间线 */
  --el-timeline-node-color: #2a2a34;

  /* 日历 */
  --el-calendar-bg-color: #131316;
  --el-calendar-border-color: #24242c;

  /* 容器背景 */
  background-color: #09090c;
}

/* ---- 滚动条 ---- */
html.dark ::-webkit-scrollbar-thumb {
  background-color: #2a2a34;
  border-radius: 10px;
}
html.dark ::-webkit-scrollbar-thumb:hover {
  background-color: #3a3a46;
}

/* ---- 弹窗遮罩毛玻璃 ---- */
html.dark .el-overlay-dialog {
  backdrop-filter: blur(4px);
}

/* ---- v-md-editor Markdown 预览暗色适配 ---- */
html.dark .v-md-editor-preview {
  background-color: transparent !important;
  color: var(--text-secondary);
}
html.dark .v-md-editor-preview h1,
html.dark .v-md-editor-preview h2,
html.dark .v-md-editor-preview h3,
html.dark .v-md-editor-preview h4,
html.dark .v-md-editor-preview h5,
html.dark .v-md-editor-preview h6 {
  color: var(--text-primary) !important;
  border-bottom-color: var(--border) !important;
}
html.dark .v-md-editor-preview p,
html.dark .v-md-editor-preview li {
  color: var(--text-secondary);
}
html.dark .v-md-editor-preview code {
  background-color: var(--bg-elevated) !important;
  color: var(--text-primary) !important;
}
html.dark .v-md-editor-preview pre {
  background-color: var(--bg-elevated) !important;
  border: 1px solid var(--border);
}
html.dark .v-md-editor-preview pre code {
  background-color: transparent !important;
}
html.dark .v-md-editor-preview blockquote {
  border-left-color: var(--border) !important;
  color: var(--text-muted) !important;
  background-color: transparent;
}
html.dark .v-md-editor-preview table {
  border-color: var(--border);
}
html.dark .v-md-editor-preview th,
html.dark .v-md-editor-preview td {
  border-color: var(--border);
  background-color: transparent;
}
html.dark .v-md-editor-preview th {
  background-color: var(--bg-elevated);
}
html.dark .v-md-editor-preview hr {
  background-color: var(--border);
}

/* ============================================================
   🌙 页面级暗色适配 — 使用 html.dark 选择器确保
   高于 scoped 组件样式优先级，彻底解决白底问题
   ============================================================ */

/* ---- Home ---- */
html.dark .home-wrapper {
  background-color: var(--bg-page);
}
html.dark .main-card {
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}
html.dark .nickname {
  color: var(--text-primary);
}
html.dark .motto {
  color: var(--text-secondary);
}
html.dark .motto span {
  color: var(--border);
}
html.dark .card-footer {
  color: var(--text-muted);
  border-top-color: var(--border);
}
html.dark .social-btn {
  background: var(--bg-elevated) !important;
  border-color: var(--border) !important;
  color: var(--text-secondary) !important;
}
html.dark .social-btn:hover {
  background: var(--bg-hover) !important;
  border-color: #3a3a46 !important;
  color: var(--text-primary) !important;
  box-shadow: 0 4px 16px rgba(0,0,0,0.4) !important;
}

/* ---- Blog ---- */
html.dark .blog-container {
  background-color: var(--bg-page) !important;
}
html.dark .blog-container .title,
html.dark .blog-container .card-header {
  color: var(--text-primary);
}
html.dark .blog-container .summary,
html.dark .blog-container .stat-row,
html.dark .blog-container .author-info p,
html.dark .blog-container .stat-item {
  color: var(--text-secondary);
}
html.dark .blog-container .date,
html.dark .blog-container .stats {
  color: var(--text-muted);
}
html.dark .blog-container .stat-item span {
  color: var(--text-secondary);
}
html.dark .blog-container .author-info h3 {
  color: var(--text-primary);
}

/* ---- Resume ---- */
html.dark .resume-wrapper {
  background-color: var(--bg-page) !important;
}
html.dark .resume-wrapper .name {
  color: var(--text-primary);
}
html.dark .resume-wrapper .section-title {
  color: var(--text-primary);
}
html.dark .resume-wrapper .content-card {
  background-color: var(--bg-card) !important;
  border-color: var(--border) !important;
}
html.dark .resume-wrapper .content-card .org-info h4,
html.dark .resume-wrapper .content-card h4,
html.dark .resume-wrapper .skill-card h4 {
  color: var(--text-primary);
}
html.dark .resume-wrapper .content-card .sub-title {
  color: var(--text-secondary);
}
html.dark .resume-wrapper .content-card .date {
  color: var(--text-muted);
}
html.dark .resume-wrapper .content-card .desc,
html.dark .resume-wrapper .skill-card p {
  color: var(--text-secondary);
}
html.dark .resume-wrapper .skill-card {
  background-color: var(--bg-card) !important;
  border-color: var(--border) !important;
}
html.dark .resume-wrapper .skill-icon-fallback {
  background: linear-gradient(135deg, #4a3f60, #5a4e70);
}
html.dark .resume-wrapper .edit-hint {
  color: var(--text-muted);
}
html.dark .resume-wrapper .loading-skeleton {
  background: var(--bg-card);
  border: 1px solid var(--border);
}

/* ---- ArticleDetail ---- */
html.dark .article-detail-container {
  background-color: var(--bg-page) !important;
}
html.dark .article-detail-container .content-card {
  background-color: var(--bg-card) !important;
}
html.dark .article-detail-container .article-footer {
  color: var(--text-muted);
}
html.dark .article-detail-container .toc-card {
  background-color: var(--bg-card) !important;
}
html.dark .article-detail-container .toc-card .card-header {
  color: var(--text-primary) !important;
}
html.dark .article-detail-container .toc-item {
  color: var(--text-secondary);
}
html.dark .article-detail-container .toc-item:hover {
  color: #409eff;
}

/* ---- Projects (深紫色沉浸) ---- */
html.dark .projects-page {
  background: linear-gradient(135deg, #0e0d14 0%, #0b0b10 100%) !important;
}
html.dark .top-card-toolbar,
html.dark .project-card,
html.dark .loading-wrapper,
html.dark .empty-state {
  background: #14131a !important;
  box-shadow: 0 4px 24px rgba(0,0,0,0.4);
}
html.dark .filter-pill {
  background: #1c1b24;
  color: var(--text-muted);
}
html.dark .filter-pill:hover {
  background: #26242e;
  color: var(--text-secondary);
}
html.dark .filter-pill.active {
  background: linear-gradient(135deg, #4a3f60, #5a4e70);
  color: #fff;
}
html.dark .card-title {
  color: var(--text-primary);
}
html.dark .card-desc {
  color: var(--text-secondary);
}
html.dark .card-img {
  background: #14131a !important;
}
html.dark .tech-chip {
  background: #1c1b24;
  color: var(--text-secondary);
}
html.dark .tech-chip:hover {
  background: #3a3450;
  color: #ece8e4;
}
html.dark .card-footer {
  border-top-color: #1e1d26;
}
html.dark .card-date {
  color: var(--text-muted);
}
html.dark .action-icon-btn {
  background: #1c1b24 !important;
  color: var(--text-muted) !important;
}
html.dark .search-input .el-input__wrapper {
  background: #14131a !important;
  border-color: #1e1d26 !important;
}
html.dark .detail-section-title,
html.dark .detail-project-title {
  color: var(--text-primary);
}
html.dark .detail-meta-item {
  color: var(--text-muted);
}
html.dark .detail-desc-content {
  background: var(--bg-elevated);
}
html.dark .detail-desc-content p {
  color: var(--text-secondary);
}
html.dark .detail-tech-tag {
  background: #1c1b24;
  color: var(--text-secondary);
}
html.dark .detail-tech-tag:hover {
  background: linear-gradient(135deg, #4a3f60, #5a4e70);
  color: #ece8e4;
}
html.dark .detail-hero-fallback {
  background: #14131a !important;
}

/* ---- MessageBoard (深紫色沉浸) ---- */
html.dark .message-page {
  background: linear-gradient(135deg, #0e0d14 0%, #0b0b10 100%) !important;
}
html.dark .post-card,
html.dark .empty-state,
html.dark .skeleton-card,
html.dark .side-card {
  background: #14131a !important;
  box-shadow: 0 4px 24px rgba(0,0,0,0.4);
}
html.dark .post-card-header {
  background: linear-gradient(135deg, #4a3f60 0%, #3e4260 100%);
}
html.dark .post-card-body {
  background: #14131a;
}
html.dark .soft-input .el-textarea__inner,
html.dark .soft-input .el-input__wrapper {
  background: #1c1b24 !important;
  border-color: #24222e !important;
  color: var(--text-secondary);
}
html.dark .soft-input .el-textarea__inner:focus,
html.dark .soft-input .el-input__wrapper.is-focus {
  border-color: #5a5070 !important;
  background: #1e1d26 !important;
}
html.dark .soft-input .el-textarea__inner:hover,
html.dark .soft-input .el-input__wrapper:hover {
  border-color: #3a3650 !important;
}
html.dark .message-card {
  background: #14131a !important;
}
html.dark .message-card.is-own {
  background: linear-gradient(135deg, #1a1822 0%, #14131a 100%) !important;
  border-color: #24222e !important;
}
html.dark .message-card.is-pinned {
  border-color: #4a3f60 !important;
  background: linear-gradient(135deg, #1a1822 0%, #14131a 100%) !important;
}
html.dark .msg-nickname { color: var(--text-primary); }
html.dark .self-tag { background: #24222e; color: var(--text-secondary); }
html.dark .msg-time { color: var(--text-muted); }
html.dark .msg-content { color: var(--text-primary); }
html.dark .action-btn { color: var(--text-muted); }
html.dark .action-btn:hover { background: #1e1d26; }
html.dark .like-btn:hover,
html.dark .like-btn.liked { background: #2a1820 !important; color: #d9534f; }
html.dark .reply-btn:hover { background: #1e1d26; color: var(--text-secondary); }
html.dark .pin-btn:hover,
html.dark .pin-btn.pinned { background: #2a2518 !important; color: #b8953a; }
html.dark .delete-btn:hover { background: #2a1820 !important; color: #d9534f; }
html.dark .reply-item {
  background: #1c1b24 !important;
  border-left-color: #2a2638;
}
html.dark .reply-item:hover { background: #1e1d26; }
html.dark .reply-own {
  background: #1e1c26 !important;
  border-left-color: #4a3f60;
}
html.dark .reply-own:hover { background: #22202a; }
html.dark .reply-avatar { background: #2a2638; }
html.dark .reply-nickname { color: var(--text-primary); }
html.dark .reply-content { color: var(--text-secondary); }
html.dark .reply-target { background: #1e1c26; color: var(--text-secondary); }
html.dark .reply-time { color: var(--text-muted); }
html.dark .reply-form { background: #1c1b24 !important; border-color: #24222e !important; }
html.dark .reply-form-header { color: var(--text-secondary); }
html.dark .reply-submit-btn {
  background: linear-gradient(135deg, #4a3f60, #3e4260) !important;
}
html.dark .message-count { color: var(--text-muted); }
html.dark .message-count strong { color: var(--text-secondary); }
html.dark .bar-line { background: linear-gradient(90deg, #24222e, transparent); }
html.dark .side-card-header {
  color: var(--text-primary);
  border-bottom-color: #1e1d26;
}
html.dark .profile-name { color: var(--text-primary); }
html.dark .profile-desc { color: var(--text-muted); }
html.dark .profile-stat-num { color: var(--text-secondary); }
html.dark .profile-stat-label { color: var(--text-muted); }
html.dark .stat-key { color: var(--text-muted); }
html.dark .stat-val { color: var(--text-secondary); }
html.dark .stat-val.highlight { color: var(--text-secondary); }
html.dark .stat-divider { background: #1e1d26; }
html.dark .divider-line { background: #24222e; }
html.dark .divider-text { color: var(--text-muted); }
html.dark .skeleton-avatar,
html.dark .skeleton-line {
  background: linear-gradient(90deg, #1c1b24 25%, #22202a 50%, #1c1b24 75%) !important;
  background-size: 200% 100%;
}

/* ---- Navbar ---- */
html.dark .global-nav {
  background: rgba(9, 9, 12, 0.8) !important;
  backdrop-filter: saturate(180%) blur(24px);
  box-shadow: 0 1px 12px rgba(0, 0, 0, 0.5);
}
</style>
