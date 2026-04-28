<template>
  <div class="route-loading-bar" :class="{ active: isLoading, done: isDone }"></div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

/**
 * 路由加载进度条组件
 *
 * 替代原有的原生 DOM 操作方式，使用 Vue 组件 + CSS transition 实现。
 * 在路由切换时显示顶部进度条动画。
 */
const isLoading = ref(false)
const isDone = ref(false)

const router = useRouter()

router.beforeEach(() => {
  isLoading.value = true
  isDone.value = false
})

router.afterEach(() => {
  isLoading.value = false
  isDone.value = true
  setTimeout(() => { isDone.value = false }, 300)
})
</script>

<style scoped>
.route-loading-bar {
  position: fixed;
  top: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--primary, #4F46E5);
  z-index: 9999;
  transition: width 0.3s ease;
  pointer-events: none;
}

.route-loading-bar.active {
  width: 70%;
  transition: width 0.8s ease;
}

.route-loading-bar.done {
  width: 100%;
  opacity: 0;
  transition: width 0.2s ease, opacity 0.3s ease 0.2s;
}
</style>
