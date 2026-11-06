import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 端口 5176；/api 与 /ws 代理到后端 8081（Vite proxy 打通跨域）
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5176,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8081',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})