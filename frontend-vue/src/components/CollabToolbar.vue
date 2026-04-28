<template>
  <div class="collab-toolbar">
    <!-- 连接状态 -->
    <div class="collab-status" :class="{ connected }">
      <span class="status-dot"></span>
      <span>{{ connected ? '协作中' : '未连接' }}</span>
    </div>

    <!-- 参与者列表 -->
    <div class="collab-participants">
      <div
        v-for="p in participants"
        :key="p.userId"
        class="participant"
        :class="{ editor: p.role === 'editor' }"
        :title="`${p.nickname} (${p.role === 'editor' ? '编辑者' : '观察者'})`"
      >
        <img
          v-if="p.avatarUrl"
          :src="p.avatarUrl"
          :alt="p.nickname"
          class="avatar"
        />
        <span v-else class="avatar-placeholder">{{ p.nickname?.charAt(0) || '?' }}</span>
      </div>
      <span class="participant-count">{{ participants.length }} 人</span>
    </div>

    <!-- 版本号 -->
    <div class="collab-version">
      v{{ version }}
    </div>

    <!-- 操作按钮 -->
    <div class="collab-actions">
      <button class="btn-chat" @click="$emit('toggle-chat')" title="聊天">
        💬
      </button>
      <button class="btn-leave" @click="$emit('leave')" title="退出协作">
        退出
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CollabParticipant } from '@/api/collab'

defineProps<{
  connected: boolean
  participants: CollabParticipant[]
  version: number
}>()

defineEmits<{
  'toggle-chat': []
  leave: []
}>()
</script>

<style scoped>
.collab-toolbar {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.5rem 1rem;
  background: var(--bg-secondary, #f5f5f5);
  border-bottom: 1px solid var(--border-color, #eee);
  font-size: 0.85rem;
}

.collab-status {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: #999;
}

.collab-status.connected {
  color: #52c41a;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999;
}

.collab-status.connected .status-dot {
  background: #52c41a;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.collab-participants {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.participant {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid #ddd;
  overflow: hidden;
  transition: border-color 0.2s;
}

.participant.editor {
  border-color: #409eff;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e8e8e8;
  font-size: 0.75rem;
  color: #666;
}

.participant-count {
  margin-left: 0.3rem;
  color: #999;
  font-size: 0.8rem;
}

.collab-version {
  color: #999;
  font-family: monospace;
}

.collab-actions {
  margin-left: auto;
  display: flex;
  gap: 0.5rem;
}

.btn-chat,
.btn-leave {
  padding: 0.3rem 0.8rem;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  background: var(--bg-primary, #fff);
  cursor: pointer;
  font-size: 0.8rem;
  transition: all 0.2s;
}

.btn-leave {
  color: #f56c6c;
  border-color: #f56c6c33;
}

.btn-leave:hover {
  background: #f56c6c08;
}

.btn-chat:hover {
  border-color: #409eff;
}
</style>
