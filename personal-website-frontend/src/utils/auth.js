import { ref } from 'vue'
import request from './request'

// 登录状态
const isLoggedIn = ref(false)
const isAdmin = ref(false)
const currentUser = ref(null)

// 管理员身份是否已通过服务端校验（防止 localStorage 伪造）
let adminVerified = false
let verifyPromise = null

const clearAuth = () => {
  isLoggedIn.value = false
  isAdmin.value = false
  currentUser.value = null
  adminVerified = false
  verifyPromise = null
  localStorage.removeItem('auth')
}

// 登录
const login = async (username, password) => {
  try {
    const res = await request.post('/user/login', { username, password })
    if (res.data.code === 200) {
      const data = res.data.data
      currentUser.value = { username: data.username }
      isAdmin.value = true
      isLoggedIn.value = true
      adminVerified = true // 刚登录签发的 token 视为已验证
      verifyPromise = null
      localStorage.setItem('auth', JSON.stringify({ type: 'admin', username: data.username, token: data.token }))
      return { success: true }
    } else {
      return { success: false, message: res.data.message || '登录失败' }
    }
  } catch (error) {
    return { success: false, message: '网络错误，请重试' }
  }
}

// 游客登录
const guestLogin = () => {
  isAdmin.value = false
  isLoggedIn.value = true
  currentUser.value = null
  adminVerified = false
  verifyPromise = null
  localStorage.setItem('auth', JSON.stringify({ type: 'guest' }))
}

// 退出登录
const logout = () => {
  try { request.post('/user/logout').catch(() => {}) } catch { /* ignore */ }
  isLoggedIn.value = false
  isAdmin.value = false
  currentUser.value = null
  adminVerified = false
  verifyPromise = null
  localStorage.removeItem('auth')
}

// 回源校验管理员 Token（同一轮会话只校验一次）
const verifyAdminToken = async () => {
  if (adminVerified) return true
  verifyPromise ??= request.get('/user/check')
    .then((res) => {
      if (res.data.code === 200) {
        adminVerified = true
        return true
      }
      clearAuth()
      return false
    })
    .catch((err) => {
      // 401：Token 无效/过期，清除登录态；网络故障则保留状态下次重试
      if (err.response?.status === 401) {
        clearAuth()
      } else {
        verifyPromise = null
      }
      return false
    })
  return verifyPromise
}

// 检查登录状态（异步：管理员身份需经服务端确认后才生效）
const checkAuth = async () => {
  const stored = localStorage.getItem('auth')
  if (!stored) return
  try {
    const data = JSON.parse(stored)
    isLoggedIn.value = true
    if (data.type === 'admin') {
      if (!data.token) {
        // 旧版登录态缺少 token，强制重新登录
        clearAuth()
        return
      }
      currentUser.value = { username: data.username }
      isAdmin.value = await verifyAdminToken()
    } else {
      isAdmin.value = false
      currentUser.value = null
    }
  } catch {
    clearAuth()
  }
}

export { isLoggedIn, isAdmin, currentUser, login, guestLogin, logout, checkAuth }
