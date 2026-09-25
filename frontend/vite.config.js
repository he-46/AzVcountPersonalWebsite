import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// dev 代理：/api 和 /uploads 都转发到后端 8080（免 CORS）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/uploads': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
