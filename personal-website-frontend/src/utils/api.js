// API 基础配置：默认走 Vite 开发代理 / 生产反向代理的同源 /api，
// 需要直连其它后端地址时通过环境变量 VITE_API_BASE 覆盖（如 http://localhost:8090/api）。
export const API_BASE = import.meta.env.VITE_API_BASE || '/api'

// 上传接口地址（与 API_BASE 同源，避免硬编码端口/域名）
export const uploadUrl = (kind) => `${API_BASE}/upload/${kind}`

// 为不走 axios 的 el-upload 等组件构造鉴权请求头
export const authHeaders = () => {
  try {
    const data = JSON.parse(localStorage.getItem('auth') || '{}')
    return data?.token ? { Authorization: 'Bearer ' + data.token } : {}
  } catch {
    return {}
  }
}

/**
 * 规范化媒体地址：历史数据里可能有写死的本机地址（如 http://localhost:8080/uploads/...），
 * 这类地址在别的机器上必然访问不到，统一剥掉协议+主机，退回同源相对路径。
 */
export const mediaUrl = (url) => {
  if (!url) return ''
  return String(url).replace(/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?/i, '')
}
