import { createRouter, createWebHistory } from 'vue-router'

import Home from '../views/Home.vue'
import { isLoggedIn, isAdmin, checkAuth } from '../utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    meta: { hideNav: true },
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/resume',
    name: 'Resume',
    component: () => import('../views/Resume.vue')
  },
  {
    path: '/projects',
    name: 'Projects',
    component: () => import('../views/Projects.vue')
  },
  { path: '/message', redirect: '/messages' },
  { path: '/blog', component: () => import('../views/Blog.vue') },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('../views/ArticleDetail.vue')
  },
  {
    path: '/messages',
    name: 'MessageBoard',
    component: () => import('../views/MessageBoard.vue')
  },
  // ---- Admin routes ----
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { hideNav: true, requiresAdmin: true },
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'articles', component: () => import('../views/admin/ArticleManagement.vue') },
      { path: 'articles/create', component: () => import('../views/admin/ArticleEditor.vue') },
      { path: 'articles/edit/:id', component: () => import('../views/admin/ArticleEditor.vue') },
      { path: 'categories', component: () => import('../views/admin/CategoryManagement.vue') },
      { path: 'comments', component: () => import('../views/admin/CommentManagement.vue') },
      { path: 'messages', component: () => import('../views/admin/MessageManagement.vue') },
      { path: 'music', component: () => import('../views/admin/MusicManagement.vue') },
      { path: 'visitors', component: () => import('../views/admin/VisitorManagement.vue') },
      { path: 'logs', component: () => import('../views/admin/OperationLogs.vue') },
      { path: 'profile', component: () => import('../views/admin/Profile.vue') },
      { path: 'settings', component: () => import('../views/admin/SystemSettings.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  checkAuth()

  // 登录页放行
  if (to.path === '/login') {
    next()
    return
  }

  // 需要管理员权限但当前不是管理员
  if (to.meta.requiresAdmin && !isAdmin.value) {
    next('/')
    return
  }

  // 未登录跳转登录页
  if (!isLoggedIn.value) {
    next('/login')
  } else {
    next()
  }
})

export default router
