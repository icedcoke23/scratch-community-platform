<template>
  <div class="oauth-login">
    <div class="oauth-divider">
      <span class="oauth-divider-text">其他登录方式</span>
    </div>

    <div class="oauth-buttons">
      <button
        class="oauth-btn oauth-btn--wechat"
        @click="handleOAuthLogin('wechat')"
        :disabled="loading"
        title="微信登录"
      >
        <svg class="oauth-icon" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178A1.17 1.17 0 014.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178 1.17 1.17 0 01-1.162-1.178c0-.651.52-1.18 1.162-1.18zm3.97 4.125c-3.978 0-7.345 2.68-7.345 6.243 0 3.562 3.367 6.242 7.345 6.242.867 0 1.697-.134 2.464-.367a.72.72 0 01.575.076l1.545.904a.268.268 0 00.135.043.235.235 0 00.234-.238c0-.058-.023-.114-.038-.172l-.316-1.196a.48.48 0 01.171-.537C22.143 19.328 23 17.63 23 15.702v-.374c0-3.563-3.234-6.214-7.432-6.214zm-2.13 3.462c.52 0 .942.43.942.957a.95.95 0 01-.943.956.95.95 0 01-.942-.956c0-.528.422-.957.943-.957zm4.26 0c.52 0 .943.43.943.957a.95.95 0 01-.943.956.95.95 0 01-.942-.956c0-.528.422-.957.943-.957z"/>
        </svg>
        <span>微信</span>
      </button>

      <button
        class="oauth-btn oauth-btn--qq"
        @click="handleOAuthLogin('qq')"
        :disabled="loading"
        title="QQ 登录"
      >
        <svg class="oauth-icon" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12.003 2c-2.265 0-6.29 1.364-6.29 7.325v1.195S3.55 14.96 3.55 17.474c0 .665.17 1.025.395 1.025.19 0 .475-.36 1.045-1.025 0 0-.07 2.51 2.155 3.435-.19.665-.575 1.69-.575 2.41 0 .36.19.72.575.72 1.15 0 2.725-2.395 3.26-4.285.265.04.535.06.805.06.27 0 .535-.02.8-.06.54 1.89 2.11 4.285 3.26 4.285.385 0 .57-.36.57-.72 0-.72-.38-1.75-.57-2.41 2.225-.925 2.155-3.435 2.155-3.435.575.665.86 1.025 1.05 1.025.225 0 .395-.36.395-1.025 0-2.515-2.16-6.954-2.16-6.954V9.325C18.29 3.364 14.268 2 12.003 2z"/>
        </svg>
        <span>QQ</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { oauthLogin } from '@/api/oauth'

const { t } = useI18n()
const loading = ref(false)

const emit = defineEmits<{
  success: [token: string, isNewUser: boolean]
  error: [message: string]
}>()

/**
 * 发起第三方登录
 * 实际项目中，这里需要跳转到第三方授权页面
 * 授权后回调到 /oauth/{provider}/callback 页面
 */
async function handleOAuthLogin(provider: string) {
  loading.value = true

  try {
    // 方式1: 跳转到第三方授权页（生产环境推荐）
    // const redirectUri = `${window.location.origin}/oauth/${provider}/callback`
    // window.location.href = getOAuthUrl(provider, redirectUri)

    // 方式2: 弹窗模式（开发环境/演示用）
    // 实际项目中，这里应该打开第三方授权页面
    // 用户授权后，通过 postMessage 或 URL 参数传回 code
    const code = await openOAuthWindow(provider)
    if (!code) {
      loading.value = false
      return
    }

    // 调用后端登录接口
    const res = await oauthLogin({ provider, code })
    if (res.data) {
      emit('success', res.data.token, res.data.newUser)
    }
  } catch (error: unknown) {
    emit('error', error instanceof Error ? error.message : '第三方登录失败')
  } finally {
    loading.value = false
  }
}

/**
 * 打开第三方授权窗口
 * 返回授权码 code
 */
function openOAuthWindow(provider: string): Promise<string | null> {
  return new Promise((resolve) => {
    // 实际项目中，这里需要跳转到真实的第三方授权 URL
    // 以下为示例 URL 格式
    const urls: Record<string, string> = {
      wechat: `https://open.weixin.qq.com/connect/qrconnect?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=snsapi_login&state=STATE#wechat_redirect`,
      qq: `https://graph.qq.com/oauth2.0/authorize?client_id=APPID&redirect_uri=REDIRECT_URI&response_type=code&state=STATE`,
    }

    const url = urls[provider]
    if (!url) {
      resolve(null)
      return
    }

    // 打开授权窗口
    const width = 600
    const height = 700
    const left = (window.innerWidth - width) / 2
    const top = (window.innerHeight - height) / 2
    const authWindow = window.open(
      url,
      `oauth_${provider}`,
      `width=${width},height=${height},left=${left},top=${top}`
    )

    // 监听授权回调
    // 实际项目中，回调页面会通过 postMessage 发送 code
    const handler = (event: MessageEvent) => {
      if (event.data?.type === 'oauth_callback' && event.data?.provider === provider) {
        window.removeEventListener('message', handler)
        authWindow?.close()
        resolve(event.data.code || null)
      }
    }
    window.addEventListener('message', handler)

    // 超时处理
    setTimeout(() => {
      window.removeEventListener('message', handler)
      authWindow?.close()
      resolve(null)
    }, 300_000) // 5 分钟超时
  })
}
</script>

<style scoped>
.oauth-login {
  margin-top: 1rem;
}

.oauth-divider {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
  color: var(--text-secondary, #999);
  font-size: 0.85rem;
}

.oauth-divider::before,
.oauth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-color, #eee);
}

.oauth-divider-text {
  padding: 0 0.75rem;
}

.oauth-buttons {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.oauth-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.2rem;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
  color: var(--text-primary, #333);
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.oauth-btn:hover {
  border-color: var(--primary-color, #409eff);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.oauth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.oauth-btn--wechat {
  color: #07c160;
  border-color: #07c16033;
}

.oauth-btn--wechat:hover {
  background: #07c16008;
  border-color: #07c160;
}

.oauth-btn--qq {
  color: #12b7f5;
  border-color: #12b7f533;
}

.oauth-btn--qq:hover {
  background: #12b7f508;
  border-color: #12b7f5;
}

.oauth-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}
</style>
