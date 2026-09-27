// 主题管理工具（分区：前台 site / 管理后台 admin）
//
// 两套主题互不影响：各自独立的 localStorage key 与切换入口。
// 同一时刻只有一套生效——由当前路由决定（见 App.vue），
// 因此后台切暗色不会影响前台，反之亦然。

import { ref } from 'vue'

const SCOPES = {
  site:  { key: 'theme',       fallback: 'light' },
  admin: { key: 'theme-admin', fallback: 'light' },
}

/** 当前管理后台主题是否为暗色（供后台组件响应式取用） */
export const adminDark = ref(false)

const scopeOf = (scope) => SCOPES[scope] || SCOPES.site

// 读取指定分区的主题
const getTheme = (scope = 'site') => {
  return localStorage.getItem(scopeOf(scope).key) || scopeOf(scope).fallback
}

// 应用主题到文档（同时标记是否处于后台作用域，便于后台专属样式隔离）
const apply = (theme, scope = 'site') => {
  const root = document.documentElement
  const isDark = theme === 'dark'

  root.setAttribute('data-theme', theme)
  root.classList.toggle('dark', isDark)
  root.classList.toggle('admin-scope', scope === 'admin')

  document.body.style.backgroundColor = isDark
    ? '#09090c'
    : (scope === 'admin' ? '#f5f6fa' : '#f8f9fa')

  if (scope === 'admin') adminDark.value = isDark
}

// 设置指定分区的主题
const setTheme = (theme, scope = 'site') => {
  localStorage.setItem(scopeOf(scope).key, theme)
  apply(theme, scope)
}

// 切换指定分区的主题，返回新主题
const toggleTheme = (scope = 'site') => {
  const next = getTheme(scope) === 'light' ? 'dark' : 'light'
  setTheme(next, scope)
  return next
}

// 按分区初始化（App.vue 在路由变化时调用）
const initTheme = (scope = 'site') => {
  apply(getTheme(scope), scope)
}

export { getTheme, setTheme, toggleTheme, initTheme }
