<template>
  <div class="oauth-callback">
    <div class="callback-card" v-if="loading">
      <div class="loading-spinner">🐱</div>
      <p class="loading-text">正在处理第三方登录...</p>
      <div class="loading-dots">
        <span class="dot">.</span><span class="dot">.</span><span class="dot">.</span>
      </div>
    </div>

    <div class="callback-card" v-else-if="error">
      <div class="error-icon">😿</div>
      <p class="error-text">{{ error }}</p>
      <el-button type="primary" size="large" @click="goHome" class="home-btn">
        🏠 返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
      userStore.setAuth(res.data.token, res.data.userInfo)
      if (window.opener) {
        window.opener.postMessage({
          type: 'oauth_callback',
          provider,
          code,
        }, window.location.origin)
        window.close()
      } else {
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
  background: var(--bg);
}

.callback-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 48px;
  background: var(--card);
  border-radius: 24px;
  box-shadow: var(--shadow-xl);
  max-width: 400px;
}

.loading-spinner {
  font-size: 64px;
  animation: bounce 1s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-16px); }
}

.loading-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.loading-dots {
  display: flex;
  gap: 4px;
}

.dot {
  font-size: 24px;
  color: var(--primary);
  animation: dot-pulse 1.4s ease-in-out infinite;
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes dot-pulse {
  0%, 80%, 100% { opacity: 0; }
  40% { opacity: 1; }
}

.error-icon {
  font-size: 64px;
}

.error-text {
  font-size: 16px;
  color: var(--text2);
  margin: 0;
  line-height: 1.6;
}

.home-btn {
  margin-top: 8px;
  border-radius: 12px;
  font-weight: 600;
}
</style>
