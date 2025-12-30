import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  //  server: {
  //   host: '0.0.0.0',
  //   // 允许访问的主机列表
  //   allowedHosts: [
  //     'y286962c.natappfree.cc', // 添加需要允许的主机
  //     // 可选：如果需要允许所有主机（不推荐生产环境），可以用 '*'
  //     // '*'
  //   ],
  //   port: 5174,
  // }
    //   server: {
    //   // 服务启动时是否自动打开浏览器
    //   open: true,
    //   // 本地跨域代理 -> 代理到服务器的接口地址
    //   proxy: {
    //     '/api': {
    //       target: 'http://101.132.70.68:8069',
    //       changeOrigin: true,
    //       rewrite: (path) => path.replace(/^\/api/, '')
    //     },
    //   },
    // },
})
