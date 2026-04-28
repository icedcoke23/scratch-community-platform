<template>
  <div class="activity-timeline">
    <div class="timeline-header">
      <span class="timeline-title">{{ title }}</span>
      <el-button v-if="showRefresh" text size="small" @click="$emit('refresh')">
        🔄 刷新
      </el-button>
    </div>
    <div v-if="items.length === 0" class="timeline-empty">
      暂无活动记录
    </div>
    <div v-else class="timeline-list">
      <div
        v-for="(item, index) in items"
        :key="index"
        class="timeline-item"
      >
        <div class="timeline-dot" :style="{ background: item.color || 'var(--primary)' }" />
        <div class="timeline-content">
          <div class="timeline-text">{{ item.text }}</div>
          <div class="timeline-time">{{ item.time }}</div>
        </div>
        <div v-if="item.icon" class="timeline-icon">{{ item.icon }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
export interface ActivityItem {
  text: string
  time: string
  icon?: string
  color?: string
}

defineProps<{
  title?: string
  items: ActivityItem[]
  showRefresh?: boolean
}>()

defineEmits<{
  refresh: []
}>()
</script>

<style scoped>
.activity-timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.timeline-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text);
}

.timeline-empty {
  text-align: center;
  padding: 24px;
  color: var(--text2);
  font-size: 13px;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
  position: relative;
}

.timeline-item:last-child {
  border-bottom: none;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.timeline-content {
  flex: 1;
  min-width: 0;
}

.timeline-text {
  font-size: 13px;
  color: var(--text);
  line-height: 1.5;
}

.timeline-time {
  font-size: 11px;
  color: var(--text2);
  margin-top: 2px;
}

.timeline-icon {
  font-size: 16px;
  flex-shrink: 0;
}

@media (max-width: 480px) {
  .timeline-text {
    font-size: 12px;
  }
}
</style>
