import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts'
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    // 生产环境开启 sourcemap 用于错误追踪（可选，会增加构建体积）
    sourcemap: false,
    // Vite 8 使用 Rolldown 默认压缩，terser 已废弃
    // 如需移除 console/debugger，可通过 esbuild 选项配置
    esbuild: {
      // 生产环境移除 console 和 debugger
      drop: ['console', 'debugger'],
    },
    rollupOptions: {
      output: {
        // Vite 8 使用 Function 形式的 manualChunks（Object 形式已废弃）
        manualChunks(id) {
          // Vue 核心
          if (id.includes('vue/') || id.includes('pinia') || id.includes('vue-router')) {
            return 'vue-vendor'
          }
          // Element Plus 单独分包
          if (id.includes('element-plus')) {
            return 'element-plus'
          }
          // Element Plus 图标
          if (id.includes('@element-plus/icons')) {
            return 'element-icons'
          }
        },
        // 静态资源文件名包含内容哈希，便于长期缓存
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
      },
    },
    // 分包警告阈值（KB）
    chunkSizeWarningLimit: 1000,
  },
})
