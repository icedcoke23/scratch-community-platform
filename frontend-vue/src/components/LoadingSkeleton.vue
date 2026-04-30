<template>
  <div class="skeleton-container" :class="'skeleton-' + variant">
    <div v-for="n in count" :key="n" class="skeleton-item">
      <!-- 卡片变体 -->
      <div v-if="variant === 'card'" class="skeleton-card">
        <div class="skeleton-image skeleton-pulse" />
        <div class="skeleton-content">
          <div class="skeleton-title skeleton-pulse" />
          <div class="skeleton-text skeleton-pulse" />
          <div class="skeleton-text short skeleton-pulse" />
        </div>
      </div>
      <!-- 列表变体 -->
      <div v-else-if="variant === 'list'" class="skeleton-list-item">
        <div class="skeleton-avatar skeleton-pulse" />
        <div class="skeleton-content">
          <div class="skeleton-title skeleton-pulse" />
          <div class="skeleton-text skeleton-pulse" />
        </div>
      </div>
      <!-- 排行榜变体 -->
      <div v-else-if="variant === 'rank'" class="skeleton-rank-item">
        <div class="skeleton-rank-num skeleton-pulse" />
        <div class="skeleton-avatar-sm skeleton-pulse" />
        <div class="skeleton-content" style="flex: 1">
          <div class="skeleton-title skeleton-pulse" style="width: 60%" />
        </div>
        <div class="skeleton-rank-score skeleton-pulse" />
      </div>
      <!-- 详情页变体 -->
      <div v-else-if="variant === 'detail'" class="skeleton-detail">
        <div class="skeleton-detail-header skeleton-pulse" />
        <div class="skeleton-content" style="padding: 16px">
          <div class="skeleton-title skeleton-pulse" style="width: 50%; height: 24px" />
          <div class="skeleton-text skeleton-pulse" />
          <div class="skeleton-text skeleton-pulse" />
          <div class="skeleton-text short skeleton-pulse" />
          <div style="display: flex; gap: 12px; margin-top: 16px">
            <div class="skeleton-btn skeleton-pulse" />
            <div class="skeleton-btn skeleton-pulse" />
          </div>
        </div>
      </div>
      <!-- 题目列表变体 -->
      <div v-else-if="variant === 'problem'" class="skeleton-problem-item">
        <div class="skeleton-content" style="flex: 1">
          <div class="skeleton-title skeleton-pulse" style="width: 70%" />
          <div class="skeleton-text short skeleton-pulse" style="width: 40%" />
        </div>
        <div class="skeleton-badge skeleton-pulse" />
      </div>
      <!-- 默认块 -->
      <div v-else class="skeleton-block skeleton-pulse" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'LoadingSkeleton' })

withDefaults(defineProps<{
  count?: number
  variant?: 'card' | 'list' | 'block' | 'rank' | 'detail' | 'problem'
}>(), {
  count: 6,
  variant: 'card'
})
</script>

<style scoped>
.skeleton-container {
  display: grid;
  gap: 16px;
}

.skeleton-card {
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color, #fff);
  box-shadow: var(--el-box-shadow-light);
}

.skeleton-image {
  height: 160px;
  background: var(--el-fill-color-light, #f0f0f0);
}

.skeleton-content {
  padding: 12px;
}

.skeleton-title {
  height: 18px;
  width: 70%;
  margin-bottom: 8px;
  border-radius: 4px;
  background: var(--el-fill-color-light, #f0f0f0);
}

.skeleton-text {
  height: 14px;
  width: 100%;
  margin-bottom: 6px;
  border-radius: 4px;
  background: var(--el-fill-color-light, #f0f0f0);
}

.skeleton-text.short {
  width: 50%;
}

.skeleton-list-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: var(--el-bg-color, #fff);
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--el-fill-color-light, #f0f0f0);
}

.skeleton-block {
  height: 100px;
  border-radius: 8px;
  background: var(--el-fill-color-light, #f0f0f0);
}

/* 排行榜变体 */
.skeleton-rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-bg-color, #fff);
}

.skeleton-rank-num {
  width: 28px;
  height: 20px;
  border-radius: 4px;
  background: var(--el-fill-color-light, #f0f0f0);
}

.skeleton-avatar-sm {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--el-fill-color-light, #f0f0f0);
}

.skeleton-rank-score {
  width: 50px;
  height: 16px;
  border-radius: 4px;
  background: var(--el-fill-color-light, #f0f0f0);
}

/* 详情页变体 */
.skeleton-detail {
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color, #fff);
  box-shadow: var(--el-box-shadow-light);
}

.skeleton-detail-header {
  height: 200px;
  background: var(--el-fill-color-light, #f0f0f0);
}

.skeleton-btn {
  width: 80px;
  height: 36px;
  border-radius: 6px;
  background: var(--el-fill-color-light, #f0f0f0);
}

/* 题目列表变体 */
.skeleton-problem-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 8px;
  background: var(--el-bg-color, #fff);
  box-shadow: var(--el-box-shadow-light);
}

.skeleton-badge {
  width: 56px;
  height: 24px;
  border-radius: 12px;
  background: var(--el-fill-color-light, #f0f0f0);
}

.skeleton-pulse {
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

@keyframes skeleton-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* 卡片网格布局 */
@media (min-width: 769px) {
  .skeleton-container:has(.skeleton-card) {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}
</style>
