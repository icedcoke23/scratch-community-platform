<template>
  <div class="not-found">
    <div class="not-found-content">
      <div class="not-found-icon">🧩</div>
      <h1 class="not-found-title">404</h1>
      <p class="not-found-desc">{{ t('notFound.desc', '页面走丢了，可能被 Scratch 小猫带跑了') }}</p>
      <div class="not-found-actions">
        <el-button type="primary" @click="router.push('/feed')">
          {{ t('notFound.backHome', '回到首页') }}
        </el-button>
        <el-button @click="router.back()">
          {{ t('notFound.goBack', '返回上页') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'NotFound' })

import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'

const router = useRouter()
const { t } = useI18n()
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.not-found-content {
  text-align: center;
}

.not-found-icon {
  font-size: 80px;
  margin-bottom: 16px;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

.not-found-title {
  font-size: 72px;
  font-weight: 800;
  color: var(--primary);
  line-height: 1;
  margin-bottom: 12px;
}

.not-found-desc {
  font-size: 16px;
  color: var(--text2);
  margin-bottom: 28px;
}

.not-found-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
