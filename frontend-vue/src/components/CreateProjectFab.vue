<template>
  <div class="create-project-fab" :class="{ expanded: isExpanded }">
    <!-- 主按钮 -->
    <button class="fab-main" @click="toggleExpand" :class="{ 'pulse-anim': !isExpanded }">
      <span class="fab-icon" :class="{ rotated: isExpanded }">✏️</span>
      <span class="fab-label" v-if="isExpanded">创建作品</span>
    </button>

    <!-- 展开菜单 -->
    <transition name="fab-menu">
      <div v-if="isExpanded" class="fab-menu">
        <button class="fab-option" @click="createNew">
          <span class="option-icon">🎨</span>
          <div class="option-text">
            <span class="option-title">新建空白项目</span>
            <span class="option-desc">打开 Scratch 编辑器</span>
          </div>
        </button>
        <button class="fab-option" @click="importSb3">
          <span class="option-icon">📦</span>
          <div class="option-text">
            <span class="option-title">导入 SB3 文件</span>
            <span class="option-desc">从本地上传</span>
          </div>
        </button>
        <button class="fab-option" @click="goToEditor">
          <span class="option-icon">🚀</span>
          <div class="option-text">
            <span class="option-title">进入编辑器</span>
            <span class="option-desc">管理我的项目</span>
          </div>
        </button>
      </div>
    </transition>

    <!-- 遮罩 -->
    <div v-if="isExpanded" class="fab-overlay" @click="isExpanded = false" />

    <!-- 隐藏的文件上传 -->
    <input ref="fileInput" type="file" accept=".sb3" style="display:none" @change="handleFileSelect" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isExpanded = ref(false)
const fileInput = ref<HTMLInputElement>()

function toggleExpand() {
  isExpanded.value = !isExpanded.value
}

function createNew() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  isExpanded.value = false
  router.push('/editor')
}

function importSb3() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  isExpanded.value = false
  fileInput.value?.click()
}

function goToEditor() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  isExpanded.value = false
  router.push('/editor')
}

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.name.endsWith('.sb3')) {
    ElMessage.warning('请选择 .sb3 文件')
    return
  }
  // Navigate to editor with file info
  router.push('/editor')
  ElMessage.success(`已选择: ${file.name}`)
  input.value = ''
}
</script>

<style scoped>
.create-project-fab {
  position: fixed;
  bottom: 90px;
  right: 24px;
  z-index: 200;
}

.fab-main {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #3B82F6, #8B5CF6);
  color: #fff;
  border: none;
  border-radius: 28px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fab-main:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 24px rgba(59, 130, 246, 0.5);
}

.fab-main:active {
  transform: scale(0.98);
}

.fab-icon {
  font-size: 20px;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: inline-block;
}

.fab-icon.rotated {
  transform: rotate(45deg);
}

.fab-label {
  white-space: nowrap;
}

/* Pulse animation */
.pulse-anim {
  animation: fab-pulse 2s ease-in-out infinite;
}

@keyframes fab-pulse {
  0%, 100% { box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4); }
  50% { box-shadow: 0 4px 24px rgba(59, 130, 246, 0.6), 0 0 0 8px rgba(59, 130, 246, 0.1); }
}

/* Menu */
.fab-menu {
  position: absolute;
  bottom: 60px;
  right: 0;
  width: 240px;
  background: var(--card, #fff);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
  border: 1px solid var(--border, #e2e8f0);
  overflow: hidden;
  z-index: 201;
}

.fab-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.fab-option:hover {
  background: var(--primary-bg, #eff6ff);
}

.fab-option:not(:last-child) {
  border-bottom: 1px solid var(--border, #e2e8f0);
}

.option-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.option-text {
  display: flex;
  flex-direction: column;
}

.option-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text, #1e293b);
}

.option-desc {
  font-size: 12px;
  color: var(--text2, #64748b);
}

/* Overlay */
.fab-overlay {
  position: fixed;
  inset: 0;
  z-index: 199;
}

/* Transitions */
.fab-menu-enter-active {
  animation: menu-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fab-menu-leave-active {
  animation: menu-out 0.2s ease;
}

@keyframes menu-in {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes menu-out {
  to {
    opacity: 0;
    transform: translateY(10px) scale(0.95);
  }
}

@media (max-width: 768px) {
  .create-project-fab {
    bottom: 80px;
    right: 16px;
  }
  .fab-main {
    padding: 12px 16px;
    font-size: 14px;
  }
  .fab-menu {
    width: 220px;
  }
}

@media (max-width: 480px) {
  .fab-label {
    display: none;
  }
  .fab-main {
    padding: 14px;
    border-radius: 50%;
  }
}
</style>
