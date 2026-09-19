import axios from 'axios'

const request = axios.create({
    baseURL: 'http://localhost:8090/api', // 后端接口基础路径
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

// 响应拦截：401 统一跳转登录
request.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('auth')
            if (!location.pathname.startsWith('/login')) {
                location.href = '/login'
            }
        }
        return Promise.reject(error)
    }
)

export default request
