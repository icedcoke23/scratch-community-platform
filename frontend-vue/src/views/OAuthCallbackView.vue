<template>
  <div class="oauth-callback">
    <div class="callback-loading" v-if="loading">
      <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
      <p>正在处理第三方登录...</p>
    </div>

    <div class="callback-error" v-else-if="error">
      <el-icon :size="48" color="#f56c6c"><CircleCloseFilled /></el-icon>
      <p>{{ error }}</p>
      <el-button @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading, CircleCloseFilled } from '@element-plus/icons-vue'
import { oauthLogin } from '@/api/oauth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const error = ref('')

onMounted(async () => {
  const provider = route.params.provider as string
  const code = route.query.code as string

  if (!code) {
    error.value = '缺少授权码，请重新登录'
    loading.value = false
    return
  }

  try {
    const res = await oauthLogin({ provider, code })
    if (res.data) {
      // 保存登录状态
      userStore.setAuth(res.data.token, res.data.userInfo)

      // 通知父窗口（如果是弹窗模式）
      if (window.opener) {
        window.opener.postMessage({
          type: 'oauth_callback',
          provider,
          code,
        }, window.location.origin)
        window.close()
      } else {
        // 直接跳转模式
        router.push('/feed')
      }
    }
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : '第三方登录失败，请重试'
    loading.value = false
  }
})

function goHome() {
  router.push('/feed')
}
</script>

<style scoped>
.oauth-callback {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  text-align: center;
}

.callback-loading,
.callback-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
