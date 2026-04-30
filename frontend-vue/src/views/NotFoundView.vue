<template>
  <div class="not-found">
    <div class="not-found-content">
      <div class="not-found-mascot">
        <span class="mascot-emoji">🐱</span>
        <span class="mascot-question">❓</span>
      </div>
      <h1 class="not-found-title">404</h1>
      <p class="not-found-desc">哎呀！页面走丢了，可能被 Scratch 小猫带跑了 🏃‍♂️</p>
      <div class="not-found-actions">
        <el-button type="primary" size="large" @click="router.push('/feed')" class="home-btn">
          🏠 回到首页
        </el-button>
        <el-button size="large" @click="router.back()" class="back-btn">
          ← 返回上页
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
  padding: 40px;
}

.not-found-mascot {
  position: relative;
  display: inline-block;
  margin-bottom: 20px;
}

.mascot-emoji {
  font-size: 96px;
  display: block;
  animation: mascot-float 3s ease-in-out infinite;
}

.mascot-question {
  position: absolute;
  top: -10px;
  right: -20px;
  font-size: 32px;
  animation: question-bounce 1.5s ease-in-out infinite;
}

@keyframes mascot-float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  25% { transform: translateY(-10px) rotate(-5deg); }
  75% { transform: translateY(-5px) rotate(5deg); }
}

@keyframes question-bounce {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-8px) scale(1.1); }
}

.not-found-title {
  font-size: 80px;
  font-weight: 900;
  background: linear-gradient(135deg, var(--primary), var(--accent-purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 16px;
}

.not-found-desc {
  font-size: 18px;
  color: var(--text2);
  margin-bottom: 32px;
  line-height: 1.6;
}

.not-found-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.home-btn {
  min-width: 160px;
  font-weight: 600;
  border-radius: 14px;
}

.back-btn {
  min-width: 140px;
  font-weight: 600;
  border-radius: 14px;
}

@media (max-width: 480px) {
  .mascot-emoji { font-size: 72px; }
  .not-found-title { font-size: 60px; }
  .not-found-desc { font-size: 16px; }
  .not-found-actions { flex-direction: column; align-items: center; }
  .home-btn, .back-btn { width: 100%; max-width: 240px; }
}
</style>
