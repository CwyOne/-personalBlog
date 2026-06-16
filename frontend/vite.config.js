import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0', // 新增：监听所有网卡，允许内网/隧道访问
    port: 5173,
    //https: true, // 新增，永久开启本地HTTPS
    allowedHosts: ['cwy.mars-tunnel.ccwu.cc'], // 新增：放行你的穿透域名
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets'
  }
})
