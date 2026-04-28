<template>
  <div v-if="error" class="error-boundary">
    <div class="error-content">
      <div class="error-icon">😵</div>
      <h2>页面出错了</h2>
      <p class="error-message">{{ error.message }}</p>
      <details class="error-details" v-if="error.stack">
        <summary>技术详情</summary>
        <pre>{{ error.stack }}</pre>
      </details>
      <div class="error-actions">
        <el-button type="primary" @click="retry">🔄 重试</el-button>
        <el-button @click="goHome">🏠 返回首页</el-button>
      </div>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'
import { createLogger } from '@/utils/logger'

const logger = createLogger('ErrorBoundary')
const router = useRouter()
const error = ref<Error | null>(null)

onErrorCaptured((err, instance, info) => {
  error.value = err
  logger.error('组件错误:', {
    message: err.message,
    stack: err.stack,
    componentInfo: info,
    componentName: instance?.$options?.name || 'unknown'
  })

  // 错误上报（可接入 Sentry 等监控平台）
  reportError(err, info)

  return false // 阻止错误向上传播
})

/**
 * 错误上报 — 预留接口，可接入 Sentry/自建监控
 */
function reportError(err: Error, info: string) {
  // 上报到控制台（生产环境可替换为实际监控 SDK）
  console.error('[ErrorBoundary] 上报错误:', {
    message: err.message,
    stack: err.stack,
    info,
    url: window.location.href,
    userAgent: navigator.userAgent,
    timestamp: new Date().toISOString()
  })
}

function retry() {
  error.value = null
}

function goHome() {
  error.value = null
  router.push('/feed')
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 20px;
}
.error-content {
  text-align: center;
  max-width: 500px;
}
.error-icon {
  font-size: 64px;
  margin-bottom: 16px;
}
.error-message {
  color: var(--text2);
  margin: 12px 0 24px;
  font-size: 14px;
}
.error-details {
  text-align: left;
  margin: 12px 0;
  font-size: 12px;
  color: var(--text2);
}
.error-details pre {
  background: var(--bg);
  padding: 12px;
  border-radius: var(--radius);
  overflow-x: auto;
  font-size: 11px;
  max-height: 200px;
}
.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
