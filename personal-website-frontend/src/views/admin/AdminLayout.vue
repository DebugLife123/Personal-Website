<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <div class="admin-sidebar" :style="{ width: store.sidebarWidth }">
      <div class="sidebar-header">
        <span class="sidebar-logo" v-show="!store.sidebarCollapsed">yu翔 管理</span>
        <span class="sidebar-logo-mini" v-show="store.sidebarCollapsed">Y</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="store.sidebarCollapsed"
        :router="true"
        class="sidebar-menu"
        :background-color="menuColors.bg"
        :text-color="menuColors.text"
        :active-text-color="menuColors.active"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/articles">
          <el-icon><Document /></el-icon>
          <span>文章管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><CollectionTag /></el-icon>
          <span>分类/标签</span>
        </el-menu-item>
        <el-menu-item index="/admin/comments">
          <el-icon><ChatDotSquare /></el-icon>
          <span>评论管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/messages">
          <el-icon><ChatLineRound /></el-icon>
          <span>留言管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/gallery">
          <el-icon><Picture /></el-icon>
          <span>图库管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><Avatar /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/music">
          <el-icon><Headset /></el-icon>
          <span>音乐管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/visitors">
          <el-icon><User /></el-icon>
          <span>访客管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Tickets /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
        <el-menu-item index="/admin/profile">
          <el-icon><UserFilled /></el-icon>
          <span>个人资料</span>
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- Main area -->
    <div class="admin-main" :style="{ marginLeft: store.sidebarWidth }">
      <!-- Top bar -->
      <div class="admin-topbar">
        <div class="topbar-left">
          <el-icon class="collapse-btn" @click="store.toggleSidebar">
            <Fold v-if="!store.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <span class="topbar-title">{{ pageTitle }}</span>
        </div>
        <div class="topbar-right">
          <el-button text @click="goHome">
            <el-icon><HomeFilled /></el-icon>
            返回前台
          </el-button>
          <!-- 后台主题开关：只作用于后台，与前台主题互不影响 -->
          <el-tooltip :content="adminDark ? '后台切换到亮色' : '后台切换到暗色'" placement="bottom">
            <el-icon class="theme-toggle" @click="handleAdminThemeToggle">
              <Sunny v-if="adminDark" />
              <Moon v-else />
            </el-icon>
          </el-tooltip>
          <span class="admin-user">
            <el-icon><UserFilled /></el-icon>
            {{ username }}
          </span>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </div>

      <!-- Content -->
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAdminStore } from '../../stores/admin'
import {
  Odometer, Document, CollectionTag, ChatDotSquare, ChatLineRound,
  Headset, User, Tickets, UserFilled, Setting, Avatar, Picture,
  Fold, Expand, HomeFilled, Moon, Sunny
} from '@element-plus/icons-vue'
import { logout, isAdmin, currentUser } from '../../utils/auth'
import { adminDark, toggleTheme } from '../../utils/theme'

const router = useRouter()
const route = useRoute()
const store = useAdminStore()

// 后台菜单配色随后台主题走（Element Plus 菜单颜色是 props，需要动态传）
const menuColors = computed(() => adminDark.value
  ? { bg: '#131316', text: '#c8c4cc', active: '#b9aef5' }
  : { bg: '#fff', text: '#555', active: '#6c5fa0' })

// 仅切换后台主题，前台主题不受影响
const handleAdminThemeToggle = () => { toggleTheme('admin') }

const pageTitles = {
  '/admin/dashboard': '仪表盘',
  '/admin/articles': '文章管理',
  '/admin/categories': '分类/标签管理',
  '/admin/comments': '评论管理',
  '/admin/messages': '留言管理',
  '/admin/gallery': '图库管理',
  '/admin/users': '用户管理',
  '/admin/music': '音乐管理',
  '/admin/visitors': '访客管理',
  '/admin/logs': '操作日志',
  '/admin/profile': '个人资料',
  '/admin/settings': '系统设置',
}

const pageTitle = computed(() => pageTitles[route.path] || '管理后台')
const activeMenu = computed(() => route.path)
const username = computed(() => currentUser.value?.username || '管理员')

const goHome = () => {
  router.push('/')
}

const handleLogout = () => {
  logout()
  router.push('/login')
}

onMounted(() => {
  if (!isAdmin.value) {
    router.push('/')
  }
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  display: flex;
  background: #f5f6fa;
}

/* ---- Sidebar ---- */
.admin-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  background: #fff;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
  z-index: 100;
  transition: width 0.25s ease;
  overflow-x: hidden;
  overflow-y: auto;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-logo {
  font-size: 1.15rem;
  font-weight: 700;
  color: #6c5fa0;
  letter-spacing: 1px;
  white-space: nowrap;
}

.sidebar-logo-mini {
  font-size: 1.4rem;
  font-weight: 800;
  color: #6c5fa0;
}

.sidebar-menu {
  border-right: none !important;
  --el-menu-item-height: 46px;
}

.sidebar-menu .el-menu-item {
  margin: 2px 8px;
  border-radius: 8px;
  font-size: 0.9rem;
}

.sidebar-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, #f0ecf6, #e8e4f0) !important;
  font-weight: 600;
}

.sidebar-menu .el-menu-item:hover {
  background: #f5f5f8 !important;
}

/* ---- Top bar ---- */
.admin-topbar {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 0;
  z-index: 10;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  font-size: 1.2rem;
  cursor: pointer;
  color: #888;
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #6c5fa0;
}

.topbar-title {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.9rem;
  color: #555;
}

.theme-toggle {
  font-size: 1.15rem;
  color: #888;
  cursor: pointer;
  transition: color 0.2s, transform 0.35s ease;
}
.theme-toggle:hover {
  color: #6c5fa0;
  transform: rotate(25deg);
}

/* ---- Content ---- */
.admin-main {
  flex: 1;
  transition: margin-left 0.25s ease;
  display: flex;
  flex-direction: column;
}

.admin-content {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .admin-sidebar {
    width: 64px !important;
  }
  .admin-sidebar:hover {
    width: 220px !important;
  }
  .admin-main {
    margin-left: 64px !important;
  }
  .topbar-title {
    display: none;
  }
}
</style>
