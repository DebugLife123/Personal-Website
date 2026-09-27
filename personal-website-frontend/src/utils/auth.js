import { ref, computed } from 'vue'
import request from './request'

// 身份类型：guest（未登录浏览）/ user（注册用户）/ admin（管理员）
const authType = ref('guest')       // 'guest' | 'user' | 'admin'
const isLoggedIn = ref(false)       // 是否已登录（user 或 admin）
const currentUser = ref(null)       // { username, nickname?, avatar?, id? }

// 管理员身份是否已通过服务端校验（防止 localStorage 伪造）
let adminVerified = false
let verifyPromise = null

const isAdmin = computed(() => authType.value === 'admin' && adminVerified)
const isUser  = computed(() => authType.value === 'user')

const _clearAuth = () => {
  authType.value = 'guest'
  isLoggedIn.value = false
  currentUser.value = null
  adminVerified = false
  verifyPromise = null
  localStorage.removeItem('auth')
}

// 管理员登录
const adminLogin = async (username, password) => {
  try {
    const res = await request.post('/user/login', { username, password })
    if (res.data.code === 200) {
      const data = res.data.data
      adminVerified = true
      verifyPromise = null
      authType.value = 'admin'
      isLoggedIn.value = true
      currentUser.value = { username: data.username }
      localStorage.setItem('auth', JSON.stringify({ type: 'admin', token: data.token, username: data.username }))
      return { success: true }
    }
    return { success: false, message: res.data.message || '登录失败' }
  } catch {
    return { success: false, message: '网络错误，请重试' }
  }
}

// 用户登录
const userLogin = async (username, password) => {
  try {
    const res = await request.post('/webuser/login', { username, password })
    if (res.data.code === 200) {
      const data = res.data.data
      const p = data.profile || {}
      authType.value = 'user'
      isLoggedIn.value = true
      adminVerified = false
      verifyPromise = null
      currentUser.value = { id: p.id, username: p.username, nickname: p.nickname, avatar: p.avatar, email: p.email }
      localStorage.setItem('auth', JSON.stringify({ type: 'user', token: data.token, username: p.username }))
      return { success: true }
    }
    return { success: false, message: res.data.message || '登录失败' }
  } catch {
    return { success: false, message: '网络错误，请重试' }
  }
}

// 用户注册（成功后即登录）
const userRegister = async (payload) => {
  try {
    const res = await request.post('/webuser/register', payload)
    if (res.data.code === 200) {
      const data = res.data.data
      const p = data.profile || {}
      authType.value = 'user'
      isLoggedIn.value = true
      adminVerified = false
      verifyPromise = null
      currentUser.value = { id: p.id, username: p.username, nickname: p.nickname, avatar: p.avatar, email: p.email }
      localStorage.setItem('auth', JSON.stringify({ type: 'user', token: data.token, username: p.username }))
      return { success: true }
    }
    return { success: false, message: res.data.message || '注册失败' }
  } catch {
    return { success: false, message: '网络错误，请重试' }
  }
}

// 游客进入（只读浏览）
const guestLogin = () => {
  _clearAuth()
  localStorage.setItem('auth', JSON.stringify({ type: 'guest' }))
}

// 退出登录：吊销对应令牌
const logout = () => {
  const t = authType.value
  try {
    if (t === 'admin') request.post('/user/logout').catch(() => {})
    else if (t === 'user') request.post('/webuser/logout').catch(() => {})
  } catch { /* ignore */ }
  _clearAuth()
  localStorage.setItem('auth', JSON.stringify({ type: 'guest' }))
}

// 回源校验管理员 Token（同一轮会话只校验一次）
const verifyAdminToken = async () => {
  if (adminVerified && authType.value === 'admin') return true
  verifyPromise ??= request.get('/user/check')
    .then((res) => {
      if (res.data.code === 200) {
        adminVerified = true
        return true
      }
      _clearAuth()
      return false
    })
    .catch((err) => {
      if (err.response?.status === 401) {
        _clearAuth()
      } else {
        verifyPromise = null
      }
      return false
    })
  return verifyPromise
}

// 回源校验用户 Token（结论与管理员一致，但 endpoint 不同）
const verifyUserToken = async () => {
  try {
    const res = await request.get('/webuser/profile')
    if (res.data.code === 200) {
      const p = res.data.data || {}
      currentUser.value = { id: p.id, username: p.username, nickname: p.nickname, avatar: p.avatar, email: p.email }
      return true
    }
    _clearAuth()
    return false
  } catch (err) {
    if (err.response?.status === 401) _clearAuth()
    return false
  }
}

// 检查登录状态（路由守卫调用）
//
// 有本地存储的 auth 记录时：
//   - type=admin：回源校验，失败清除；
//   - type=user：回源校验，失败清除；
//   - type=guest 或无记录：保持游客浏览。
const checkAuth = async () => {
  const stored = localStorage.getItem('auth')
  if (!stored) { _clearAuth(); return }
  try {
    const data = JSON.parse(stored)
    if (data.type === 'admin') {
      authType.value = 'admin'
      isLoggedIn.value = await verifyAdminToken()
      if (!isLoggedIn.value) guestLogin()
    } else if (data.type === 'user') {
      authType.value = 'user'
      isLoggedIn.value = true
      // 后台回源校验；失败则静默降级为游客，不打断浏览
      verifyUserToken().then(ok => { if (!ok) guestLogin() })
    } else {
      _clearAuth()
      localStorage.setItem('auth', JSON.stringify({ type: 'guest' }))
    }
  } catch {
    _clearAuth()
    localStorage.setItem('auth', JSON.stringify({ type: 'guest' }))
  }
}

export {
  authType, isLoggedIn, isAdmin, isUser, currentUser,
  adminLogin, userLogin, userRegister, guestLogin, logout, checkAuth
}
