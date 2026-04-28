<template>
  <div class="project-card" @click="$emit('click')">
    <div class="card-header">
      <img
        v-if="project.coverUrl"
        v-lazy="project.coverUrl"
        class="card-cover"
        :alt="project.title"
      />
      <div v-else class="card-cover-placeholder">
        <span>🎮</span>
      </div>
    </div>
    <div class="card-body">
      <h3 class="card-title">{{ project.title }}</h3>
      <p v-if="project.description" class="card-desc">
        {{ truncatedDesc }}
      </p>
      <div class="card-meta">
        <span class="meta-author">
          <span class="avatar-sm">{{ authorInitial }}</span>
          {{ project.nickname || project.username || '匿名' }}
        </span>
        <span class="meta-stats">
          <span title="点赞">❤️ {{ project.likeCount }}</span>
          <span title="评论">💬 {{ project.commentCount }}</span>
          <span title="浏览">👁️ {{ project.viewCount }}</span>
        </span>
      </div>
      <div class="card-footer">
        <div class="card-tags" v-if="project.tags">
          <span v-for="tag in tagList" :key="tag" class="tag">{{ tag }}</span>
        </div>
        <span class="card-time">{{ timeAgo(project.createdAt) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Project } from '@/types'
import { timeAgo } from '@/utils'

const props = defineProps<{
  project: Project
}>()

defineEmits<{
  click: []
}>()

const truncatedDesc = computed(() => {
  const desc = props.project.description || ''
  return desc.length > 100 ? desc.substring(0, 100) + '...' : desc
})

const authorInitial = computed(() => {
  const name = props.project.nickname || props.project.username || '?'
  return name.charAt(0).toUpperCase()
})

const tagList = computed(() => {
  return (props.project.tags || '').split(',').filter(t => t.trim()).slice(0, 3)
})
</script>

<style scoped>
.project-card {
  background: var(--card-bg, #fff);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid var(--border, #e5e7eb);
}
.project-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.08);
}
.card-header { position: relative; }
.card-cover {
  width: 100%;
  height: 160px;
  object-fit: cover;
}
.card-cover-placeholder {
  width: 100%;
  height: 160px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
}
.card-body { padding: 14px; }
.card-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 6px;
  color: var(--text, #1f2937);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-desc {
  font-size: 13px;
  color: var(--text2, #6b7280);
  margin: 0 0 10px;
  line-height: 1.5;
}
.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text2, #6b7280);
}
.meta-author {
  display: flex;
  align-items: center;
  gap: 6px;
}
.avatar-sm {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary, #6366f1);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 600;
}
.meta-stats {
  display: flex;
  gap: 8px;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.card-tags { display: flex; gap: 4px; }
.tag {
  font-size: 11px;
  padding: 2px 8px;
  background: var(--tag-bg, #f3f4f6);
  border-radius: 10px;
  color: var(--text2, #6b7280);
}
.card-time {
  font-size: 11px;
  color: var(--text3, #9ca3af);
}
</style>
