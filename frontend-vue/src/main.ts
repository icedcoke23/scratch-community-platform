import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { vLazy } from './directives/lazy'

// 路由加载进度条样式
const progressBarStyle = document.createElement('style')
progressBarStyle.textContent = `
#route-loading-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 2px;
  background: var(--primary, #4F46E5);
  z-index: 9999;
  transition: width 0.3s ease;
  pointer-events: none;
}
#route-loading-bar.active {
  width: 80%;
  animation: loading-progress 2s ease-in-out forwards;
}
#route-loading-bar.done {
  width: 100%;
  transition: width 0.2s ease;
}
@keyframes loading-progress {
  0% { width: 0%; }
  50% { width: 70%; }
  80% { width: 85%; }
  100% { width: 90%; }
}
`
document.head.appendChild(progressBarStyle)

const bar = document.createElement('div')
bar.id = 'route-loading-bar'
document.body.appendChild(bar)

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.directive('lazy', vLazy)

app.mount('#app')

// PWA: 注册 Service Worker（仅生产环境）
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // Service Worker 注册失败，静默处理
    })
  })
}
