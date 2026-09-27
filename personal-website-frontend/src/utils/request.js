import axios from 'axios'
import { API_BASE } from './api'

const request = axios.create({
    baseURL: API_BASE, // 后端接口基础路径（默认同源 /api，由开发代理转发；可用 VITE_API_BASE 覆盖）
    timeout: 5000
})

// 请求拦截：自动携带管理员 Token
request.interceptors.request.use((config) => {
    try {
        const stored = localStorage.getItem('auth')
        if (stored) {
            const data = JSON.parse(stored)
            if (data?.token) {
                config.headers.Authorization = 'Bearer ' + data.token
            }
        }
    } catch { /* ignore */ }
    return config
}, (error) => Promise.reject(error))

// 响应拦截：401 处理——
// 管理后台：清除登录态并跳回登录页（需要管理员身份才能看）；
// 游客区：不打扰浏览，清掉失效令牌由调用方自行决定提示。
request.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            if (location.pathname.startsWith('/admin')) {
                localStorage.removeItem('auth')
                location.href = '/login'
            }
        }
        return Promise.reject(error)
    }
)

export default request
