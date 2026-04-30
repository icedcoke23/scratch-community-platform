<template>
  <div class="empty-state">
    <div class="empty-icon-wrapper">
      <div class="empty-icon">{{ icon }}</div>
      <div class="empty-sparkles">
        <span class="sparkle s1">✨</span>
        <span class="sparkle s2">⭐</span>
        <span class="sparkle s3">💫</span>
      </div>
    </div>
    <div class="empty-text">{{ text }}</div>
    <slot />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  icon?: string
  text?: string
}>(), {
  icon: '📭',
  text: '暂无数据'
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 24px;
  color: var(--text2, #64748b);
}

.empty-icon-wrapper {
  position: relative;
  margin-bottom: 16px;
}

.empty-icon {
  font-size: 64px;
  animation: empty-float 3s ease-in-out infinite;
}

@keyframes empty-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.empty-sparkles {
  position: absolute;
  top: -10px;
  left: -10px;
  right: -10px;
  bottom: -10px;
  pointer-events: none;
}

.sparkle {
  position: absolute;
  font-size: 16px;
  animation: sparkle-twinkle 2s ease-in-out infinite;
}

.s1 { top: 0; right: 0; animation-delay: 0s; }
.s2 { bottom: 0; left: 0; animation-delay: 0.5s; }
.s3 { top: 50%; left: -10px; animation-delay: 1s; }

@keyframes sparkle-twinkle {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

.empty-text {
  font-size: 17px;
  font-weight: 500;
  color: var(--text2, #64748b);
  text-align: center;
}
</style>
