import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 企业端前端:端口5175(骨架文档分工表)
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: true,
    port: 5175,
    proxy: {
      // 企业视角信用接口:转发到监管端 8081(注意需置于 /api 之前,按顺序匹配)
      '/api/v1/ent-view': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 开发代理:所有 /api 请求转发到企业端后端 8084
      '/api': {
        target: 'http://localhost:8084',
        changeOrigin: true
      },
      // 上传文件访问
      '/files': {
        target: 'http://localhost:8084',
        changeOrigin: true
      }
    }
  }
})
