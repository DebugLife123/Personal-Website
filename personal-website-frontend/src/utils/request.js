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
// 后台区域：清掉登录态并跳回管理入口；
// 前台：交给路由守卫处理（身份失效会被送回访客入口），这里只清令牌不打断请求。
request.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            if (location.pathname.startsWith('/admin')) {
                localStorage.removeItem('auth')
                location.href = '/admin/login'
            }
        }
        return Promise.reject(error)
    }
)

export default request
