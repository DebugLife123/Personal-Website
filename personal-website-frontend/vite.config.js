import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  server: {
    port: 5174,
    strictPort: true,
    host: true,
    // 开发环境代理：前端同源请求转发到本地后端，避免跨域与硬编码地址
    proxy: {
      '/api': { target: 'http://localhost:8090', changeOrigin: true },
      '/uploads': { target: 'http://localhost:8090', changeOrigin: true },
    },
  },
  plugins: [vue()],
})
