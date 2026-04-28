<template>
  <div class="project-preview" :class="{ 'preview-mini': size === 'mini' }">
    <div class="preview-cover" @click="$emit('click')">
      <img v-if="project.coverUrl" :src="project.coverUrl" :alt="project.title" class="cover-img" />
      <div v-else class="cover-placeholder">
        <span class="cover-icon">🧩</span>
        <span class="cover-text">{{ project.title }}</span>
      </div>
      <div class="cover-overlay">
        <span class="play-btn">▶</span>
      </div>
    </div>
    <div v-if="showInfo" class="preview-info">
      <div class="preview-title">{{ project.title }}</div>
      <div class="preview-meta">
        <span v-if="project.blockCount">🧱 {{ project.blockCount }}</span>
        <span v-if="project.complexityScore">📊 {{ project.complexityScore }}</span>
        <span>❤️ {{ project.likeCount ?? 0 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProjectPreview' })

defineProps<{
  project: {
    id: number
    title: string
    coverUrl?: string
    blockCount?: number
    complexityScore?: number
    likeCount?: number
  }
  showInfo?: boolean
  size?: 'normal' | 'mini'
}>()

defineEmits<{
  click: []
}>()
</script>

<style scoped>
.project-preview {
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--card);
  box-shadow: var(--shadow);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.project-preview:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.preview-cover {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--bg);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, var(--primary-bg), var(--bg));
}

.cover-icon {
  font-size: 32px;
}

.cover-text {
  font-size: 13px;
  color: var(--text2);
  text-align: center;
  padding: 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0);
  transition: background 0.2s;
}

.project-preview:hover .cover-overlay {
  background: rgba(0,0,0,0.3);
}

.play-btn {
  font-size: 24px;
  color: white;
  opacity: 0;
  transition: opacity 0.2s;
  text-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.project-preview:hover .play-btn {
  opacity: 1;
}

.preview-info {
  padding: 10px 12px;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: var(--text2);
}

/* Mini 变体 */
.preview-mini .preview-cover {
  aspect-ratio: 16 / 9;
}

.preview-mini .cover-icon {
  font-size: 24px;
}

.preview-mini .preview-info {
  padding: 6px 8px;
}

.preview-mini .preview-title {
  font-size: 12px;
}
</style>
