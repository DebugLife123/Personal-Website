import { createRouter, createWebHistory } from 'vue-router'

// 1. 首页通常直接导入，保证首屏加载速度
import Home from '../views/Home.vue'

const routes = [
  { 
    path: '/', 
    name: 'Home', 
    component: Home // 直接使用上面导入的组件
  },
  { 
    path: '/resume', 
    name: 'Resume',
    component: () => import('../views/Resume.vue') // 简历模块懒加载 
  },
  { 
    path: '/projects', 
    name: 'Projects',
    component: () => import('../views/Projects.vue') // 项目展示模块懒加载 [cite: 64]
  },
  { 
    path: '/message', 
    name: 'Message',
    component: () => import('../views/Message.vue') // 留言模块懒加载 [cite: 65]
  },
  { path: '/blog', component: () => import('../views/Blog.vue') },

  // router/index.js 核心代码检查
{
  path: '/article/:id', // 必须有冒号 :id
  name: 'ArticleDetail',
  component: () => import('../views/ArticleDetail.vue')
},
{ 
    path: '/messages', 
    name: 'MessageBoard',
    component: () => import('../views/MessageBoard.vue') 
  }

]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router