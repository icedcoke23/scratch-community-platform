<template>
  <div v-if="error" class="error-boundary">
    <div class="error-content">
      <div class="error-icon">😵</div>
      <h2>页面出错了</h2>
      <p class="error-message">{{ userFriendlyMessage }}</p>
      <details class="error-details" v-if="error.stack">
        <summary>技术详情</summary>
        <pre>{{ error.stack }}</pre>
      </details>
      <div class="error-meta" v-if="retryCount > 0">
        <span class="retry-badge">已重试 {{ retryCount }} 次</span>
      </div>
      <div class="error-actions">
        <el-button type="primary" :disabled="retryDisabled" @click="retry">
          🔄 {{ retryDisabled ? '重试次数已用尽' : '重试' }}
        </el-button>
        <el-button @click="copyError">📋 复制错误</el-button>
        <el-button @click="goHome">🏠 返回首页</el-button>
      </div>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { ref, computed, onErrorCaptured, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { createLogger } from '@/utils/logger'
import { ElMessage } from 'element-plus'

const logger = createLogger('ErrorBoundary')
const router = useRouter()

const MAX_RETRIES = 3
const error = ref<Error | null>(null)
const errorInfo = ref<string>('')
const retryCount = ref(0)

const retryDisabled = computed(() => retryCount.value >= MAX_RETRIES)

/** 用户友好的错误信息 */
const userFriendlyMessage = computed(() => {
  if (!error.value) return ''
  const msg = error.value.message || ''
  // 网络错误
  if (msg.includes('Network Error') || msg.includes('Failed to fetch')) {
    return '网络连接失败，请检查网络后重试'
  }
  // 超时
  if (msg.includes('timeout')) {
    return '请求超时，请稍后重试'
  }
  // Chunk 加载失败（部署更新后旧资源 404）
  if (msg.includes('ChunkLoadError') || msg.includes('Loading chunk')) {
    return '页面资源加载失败，可能有新版本发布，请刷新页面'
  }
  return msg || '发生了未知错误'
})

onErrorCaptured((err, instance, info) => {
  error.value = err
  errorInfo.value = info
  logger.error('组件错误:', {
    message: err.message,
    stack: err.stack,
    componentInfo: info,
    componentName: instance?.$options?.name || 'unknown'
  })
  reportError(err, info)
  return false // 阻止错误向上传播
})

/** 全局 JS 错误捕获 */
function handleGlobalError(event: ErrorEvent) {
  if (!error.value) {
    error.value = event.error || new Error(event.message)
    errorInfo.value = 'window.onerror'
    reportError(error.value, 'window.onerror')
  }
}

/** 全局未处理 Promise 拒绝捕获 */
function handleUnhandledRejection(event: PromiseRejectionEvent) {
  if (!error.value) {
    const reason = event.reason
    error.value = reason instanceof Error ? reason : new Error(String(reason))
    errorInfo.value = 'unhandledrejection'
    reportError(error.value, 'unhandledrejection')
  }
}

onMounted(() => {
  window.addEventListener('error', handleGlobalError)
  window.addEventListener('unhandledrejection', handleUnhandledRejection)
})

onUnmounted(() => {
  window.removeEventListener('error', handleGlobalError)
  window.removeEventListener('unhandledrejection', handleUnhandledRejection)
})

/**
 * 错误上报 — 预留接口，可接入 Sentry/自建监控
 */
function reportError(err: Error, info: string) {
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
  if (retryDisabled.value) return
  retryCount.value++
  error.value = null
  errorInfo.value = ''
}

function copyError() {
  const text = [
    `Error: ${error.value?.message}`,
    `URL: ${window.location.href}`,
    `Time: ${new Date().toISOString()}`,
    `Stack:\n${error.value?.stack}`
  ].join('\n')
  navigator.clipboard.writeText(text).then(
    () => ElMessage.success('错误信息已复制'),
    () => ElMessage.error('复制失败')
  )
}

function goHome() {
  error.value = null
  retryCount.value = 0
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
.error-meta {
  margin: 8px 0;
}
.retry-badge {
  font-size: 12px;
  color: var(--text3);
  background: var(--bg);
  padding: 2px 8px;
  border-radius: 4px;
}
.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}
</style>
