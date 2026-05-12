import { ref } from 'vue'
import request from './request'

// 登录状态
const isLoggedIn = ref(false)
const isAdmin = ref(false)
const currentUser = ref(null)

// 登录
const login = async (username, password) => {
  try {
    const res = await request.post('/user/login', { username, password })
    if (res.data.code === 200) {
      currentUser.value = res.data.data
      isAdmin.value = true
      isLoggedIn.value = true
      localStorage.setItem('auth', JSON.stringify({ type: 'admin', username }))
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
  localStorage.setItem('auth', JSON.stringify({ type: 'guest' }))
}

// 退出登录
const logout = () => {
  isLoggedIn.value = false
  isAdmin.value = false
  currentUser.value = null
  localStorage.removeItem('auth')
}

// 检查登录状态
const checkAuth = () => {
  const stored = localStorage.getItem('auth')
  if (stored) {
    try {
      const data = JSON.parse(stored)
      isLoggedIn.value = true
      isAdmin.value = data.type === 'admin'
      if (data.type === 'admin') {
        currentUser.value = { username: data.username }
      }
    } catch {
      localStorage.removeItem('auth')
    }
  }
}

export { isLoggedIn, isAdmin, currentUser, login, guestLogin, logout, checkAuth }
