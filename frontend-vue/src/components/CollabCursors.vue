<template>
  <div class="collab-cursors">
    <div
      v-for="p in remoteCursors"
      :key="p.userId"
      class="remote-cursor"
      :style="{ left: p.cursorX + 'px', top: p.cursorY + 'px' }"
    >
      <svg width="16" height="20" viewBox="0 0 16 20">
        <path d="M0 0L16 12L6 14L4 20L0 0Z" :fill="getCursorColor(p.userId)" />
      </svg>
      <span class="cursor-label" :style="{ background: getCursorColor(p.userId) }">
        {{ p.nickname }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import type { CollabParticipant } from '@/api/collab'

const props = defineProps<{
  participants: CollabParticipant[]
}>()

const userStore = useUserStore()

const remoteCursors = computed(() =>
  props.participants.filter(p => p.userId !== userStore.user?.id && (p.cursorX > 0 || p.cursorY > 0))
)

/** 根据用户 ID 生成固定颜色 */
function getCursorColor(userId: number): string {
  const colors = ['#f56c6c', '#67c23a', '#e6a23c', '#409eff', '#909399', '#b37feb', '#36cfc9', '#ff85c0']
  return colors[userId % colors.length]
}
</script>

<style scoped>
.collab-cursors {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
}

.remote-cursor {
  position: absolute;
  transition: left 0.1s ease, top 0.1s ease;
}

.cursor-label {
  display: inline-block;
  padding: 0.1rem 0.4rem;
  border-radius: 3px;
  color: #fff;
  font-size: 0.7rem;
  white-space: nowrap;
  margin-left: 12px;
  margin-top: -4px;
}
</style>
