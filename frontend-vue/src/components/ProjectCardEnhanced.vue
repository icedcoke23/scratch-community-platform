<template>
  <div class="project-card" @click="$emit('click')">
    <div class="card-thumbnail">
      <img :src="project.thumbnail || defaultThumbnail" :alt="project.title" class="thumbnail-image">
      <div class="card-overlay">
        <div class="overlay-actions">
          <el-button type="primary" size="small" circle>
            <span class="action-icon">▶</span>
          </el-button>
        </div>
      </div>
      <div class="card-badge" v-if="project.isFeatured">
        <span class="badge-icon">⭐</span>
        <span class="badge-text">精选</span>
      </div>
    </div>
    
    <div class="card-content">
      <h3 class="card-title" :title="project.title">{{ project.title }}</h3>
      <p class="card-description" :title="project.description">{{ project.description || '暂无描述' }}</p>
      
      <div class="card-meta">
        <div class="meta-item author">
          <span class="author-avatar">{{ (project.author?.nickname || project.author?.username || '?')[0].toUpperCase() }}</span>
          <span class="author-name">{{ project.author?.nickname || project.author?.username || '未知作者' }}</span>
        </div>
        
        <div class="meta-stats">
          <span class="stat-item">
            <span class="stat-icon">👁</span>
            <span class="stat-value">{{ formatNumber(project.views || 0) }}</span>
          </span>
          <span class="stat-item">
            <span class="stat-icon">❤️</span>
            <span class="stat-value">{{ formatNumber(project.likes || 0) }}</span>
          </span>
          <span class="stat-item">
            <span class="stat-icon">💬</span>
            <span class="stat-value">{{ formatNumber(project.comments || 0) }}</span>
          </span>
        </div>
      </div>
      
      <div class="card-tags" v-if="project.tags && project.tags.length > 0">
        <el-tag v-for="(tag, index) in project.tags.slice(0, 3)" :key="index" size="small" class="project-tag">
          {{ tag }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProjectCardEnhanced' })

interface Project {
  id: number
  title: string
  description?: string
  thumbnail?: string
  author?: { nickname?: string; username?: string }
  views?: number
  likes?: number
  comments?: number
  tags?: string[]
  isFeatured?: boolean
  createdAt?: string
}

defineProps<{
  project: Project
}>()

defineEmits<{
  (e: 'click'): void
}>()

const defaultThumbnail = 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=scratch%20programming%20project%20placeholder%20colorful%20blocks&image_size=square'

function formatNumber(num: number): string {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}
</script>

<style scoped>
.project-card {
  background: var(--card);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.08);
  border: 1px solid var(--border);
}

.project-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.2);
  border-color: var(--primary-light);
}

.card-thumbnail {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: var(--bg);
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.project-card:hover .thumbnail-image {
  transform: scale(1.08);
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.5) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.3s ease;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 20px;
}

.project-card:hover .card-overlay {
  opacity: 1;
}

.overlay-actions {
  display: flex;
  gap: 12px;
}

.action-icon {
  font-size: 16px;
}

.card-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, var(--accent-orange), #f59e0b);
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
}

.badge-icon {
  font-size: 14px;
}

.card-content {
  padding: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-description {
  font-size: 13px;
  color: var(--text2);
  margin: 0 0 12px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.meta-item.author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--accent-purple));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.author-name {
  font-size: 13px;
  color: var(--text2);
  font-weight: 500;
}

.meta-stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text3);
}

.stat-icon {
  font-size: 14px;
}

.stat-value {
  font-weight: 600;
}

.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.project-tag {
  background: var(--primary-bg);
  color: var(--primary);
  border: none;
  font-weight: 500;
}

@media (max-width: 768px) {
  .project-card {
    border-radius: 12px;
  }
  
  .card-content {
    padding: 12px;
  }
  
  .card-title {
    font-size: 14px;
  }
  
  .card-description {
    font-size: 12px;
  }
  
  .meta-stats {
    gap: 8px;
  }
}
</style>
