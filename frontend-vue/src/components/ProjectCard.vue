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
        <span class="placeholder-icon">🎮</span>
        <span class="placeholder-text">点击探索</span>
      </div>
      <!-- 项目类型徽章 -->
      <div v-if="projectType" class="card-badge" :class="`badge-${projectType}`">
        {{ typeIcons[projectType] }} {{ typeLabels[projectType] }}
      </div>
      <!-- 快速预览按钮 -->
      <button class="quick-preview-btn" @click.stop="openPreview" title="快速预览">
        ▶️
      </button>
    </div>
    <div class="card-body">
      <h3 class="card-title">{{ project.title }}</h3>
      <p v-if="project.description" class="card-desc">
        {{ truncatedDesc }}
      </p>
      <div class="card-stats">
        <div class="stat" title="点赞">
          <span class="stat-icon">❤️</span>
          <span class="stat-num">{{ project.likeCount }}</span>
        </div>
        <div class="stat" title="评论">
          <span class="stat-icon">💬</span>
          <span class="stat-num">{{ project.commentCount }}</span>
        </div>
        <div class="stat" title="浏览">
          <span class="stat-icon">👁️</span>
          <span class="stat-num">{{ project.viewCount }}</span>
        </div>
      </div>
      <div class="card-footer">
        <div class="author-info">
          <div class="author-avatar">{{ authorInitial }}</div>
          <span class="author-name">{{ project.nickname || project.username || '匿名小画家' }}</span>
        </div>
        <span class="card-time">{{ timeAgo(project.createdAt) }}</span>
      </div>
      <div class="card-tags" v-if="project.tags">
        <span v-for="tag in tagList" :key="tag" class="tag" :class="tagColorClass(tag)">{{ tag }}</span>
      </div>
    </div>

    <!-- 快速预览弹窗 -->
    <ScratchPreviewDialog
      v-model="showPreview"
      :project-id="project.id"
      :title="project.title"
      :author="project.nickname || project.username"
      :likes="project.likeCount"
      :cover-url="project.coverUrl"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Project } from '@/types'
import { timeAgo } from '@/utils'
import ScratchPreviewDialog from './ScratchPreviewDialog.vue'

const props = defineProps<{
  project: Project
}>()

defineEmits<{
  click: []
}>()

const showPreview = ref(false)

function openPreview() {
  showPreview.value = true
}

const typeIcons: Record<string, string> = {
  animation: '🎬',
  game: '🎮',
  story: '📖',
  music: '🎵',
  art: '🎨',
  other: '✨'
}

const typeLabels: Record<string, string> = {
  animation: '动画',
  game: '游戏',
  story: '故事',
  music: '音乐',
  art: '美术',
  other: '创意'
}

const projectType = computed(() => {
  const tags = Array.isArray(props.project.tags) 
    ? props.project.tags.join(',') 
    : (props.project.tags || '')
  const lowerTags = tags.toLowerCase()
  if (lowerTags.includes('动画') || lowerTags.includes('animation')) return 'animation'
  if (lowerTags.includes('游戏') || lowerTags.includes('game')) return 'game'
  if (lowerTags.includes('故事') || lowerTags.includes('story')) return 'story'
  if (lowerTags.includes('音乐') || lowerTags.includes('music')) return 'music'
  if (lowerTags.includes('美术') || lowerTags.includes('art')) return 'art'
  return null
})

const truncatedDesc = computed(() => {
  const desc = props.project.description || ''
  return desc.length > 80 ? desc.substring(0, 80) + '...' : desc
})

const authorInitial = computed(() => {
  const name = props.project.nickname || props.project.username || '?'
  return name.charAt(0).toUpperCase()
})

const tagList = computed(() => {
  const tags = props.project.tags
  if (Array.isArray(tags)) {
    return tags.filter(t => t.trim()).slice(0, 3)
  }
  return (tags || '').split(',').filter(t => t.trim()).slice(0, 3)
})

function tagColorClass(tag: string): string {
  const colors = ['tag-blue', 'tag-green', 'tag-orange', 'tag-purple', 'tag-pink']
  let hash = 0
  for (let i = 0; i < tag.length; i++) {
    hash = tag.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}
</script>

<style scoped>
.project-card {
  background: var(--card, #fff);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2px solid var(--border, #e2e8f0);
  position: relative;
}

.project-card:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.15);
  border-color: var(--primary-light, #60a5fa);
}

.card-header {
  position: relative;
  overflow: hidden;
}

.card-cover {
  width: 100%;
  height: 200px;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.project-card:hover .card-cover {
  transform: scale(1.05);
}

.card-cover-placeholder {
  width: 100%;
  height: 200px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.placeholder-icon {
  font-size: 56px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.placeholder-text {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
  font-weight: 500;
}

.card-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  backdrop-filter: blur(8px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

/* 快速预览按钮 */
.quick-preview-btn {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border: none;
  font-size: 18px;
  cursor: pointer;
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 5;
}

.project-card:hover .quick-preview-btn {
  opacity: 1;
  transform: scale(1);
}

.quick-preview-btn:hover {
  background: var(--primary, #3B82F6);
  transform: scale(1.15) !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.badge-animation { background: rgba(168, 85, 247, 0.85); }
.badge-game { background: rgba(239, 68, 68, 0.85); }
.badge-story { background: rgba(59, 130, 246, 0.85); }
.badge-music { background: rgba(236, 72, 153, 0.85); }
.badge-art { background: rgba(249, 115, 22, 0.85); }

.card-body {
  padding: 16px;
}

.card-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--text, #1e293b);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 14px;
  color: var(--text2, #64748b);
  margin: 0 0 12px;
  line-height: 1.6;
}

.card-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--text2, #64748b);
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s ease;
}

.stat:hover {
  background: var(--primary-bg, #eff6ff);
}

.stat-icon {
  font-size: 16px;
}

.stat-num {
  font-weight: 600;
  color: var(--text, #1e293b);
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary, #3b82f6), var(--accent-purple, #a855f7));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.author-name {
  font-size: 14px;
  color: var(--text2, #64748b);
  font-weight: 500;
}

.card-time {
  font-size: 12px;
  color: var(--text3, #94a3b8);
}

.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 500;
  transition: transform 0.2s ease;
}

.tag:hover {
  transform: scale(1.05);
}

.tag-blue { background: #DBEAFE; color: #2563EB; }
.tag-green { background: #DCFCE7; color: #16A34A; }
.tag-orange { background: #FFEDD5; color: #EA580C; }
.tag-purple { background: #F3E8FF; color: #9333EA; }
.tag-pink { background: #FCE7F3; color: #DB2777; }

@media (max-width: 480px) {
  .card-cover { height: 160px; }
  .placeholder-icon { font-size: 40px; }
  .card-body { padding: 12px; }
  .card-title { font-size: 15px; }
  .card-stats { gap: 10px; }
  .stat { font-size: 13px; padding: 3px 6px; }
}
</style>
