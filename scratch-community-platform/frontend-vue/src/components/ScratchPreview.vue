<template>
  <div class="scratch-preview" :class="[`size-${size}`, { 'is-fullscreen': isFullscreen }]">
    <div class="preview-wrapper" ref="wrapperRef">
      <div v-if="!loaded" class="preview-placeholder" @click="loadPreview">
        <div class="placeholder-content">
          <span class="placeholder-icon">🎮</span>
          <span class="placeholder-text">{{ title || 'Scratch 项目预览' }}</span>
          <span class="placeholder-hint" v-if="showPlayHint">点击加载预览</span>
        </div>
        <img v-if="coverUrl" :src="coverUrl" class="cover-img" :alt="title" />
      </div>
      <iframe
        v-if="loaded"
        ref="iframeRef"
        :src="playerUrl"
        class="preview-iframe"
        allow="autoplay; clipboard-read; clipboard-write"
        sandbox="allow-scripts allow-same-origin allow-popups allow-forms allow-downloads"
        @load="onIframeLoad"
      />
      <div v-if="loading" class="preview-loading">
          <div class="loading-spinner" />
          <span>加载中...</span>
      </div>
      <button v-if="loaded && showControls" class="ctrl-btn fullscreen-btn" @click="toggleFullscreen">
        {{ isFullscreen ? '退出' : '全屏' }}
      </button>
      <button v-if="loaded && showControls && showEditBtn" class="ctrl-btn edit-btn" @click="goToEditor">
        ✏️ 编辑
      </button>
      <button v-if="loaded && showControls" class="ctrl-btn reload-btn" @click="reload" title="重新加载">
        🔄
      </button>
    </div>
    <div v-if="showInfo && title" class="preview-info">
      <div class="info-title">{{ title }}</div>
      <div class="info-meta" v-if="author || likeCount !== undefined">
        <span v-if="author" class="info-author">{{ author }}</span>
        <span v-if="likeCount !== undefined" class="info-likes">❤️ {{ likeCount }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { buildPlayerUrl, getDownloadUrl } from '@/utils/turbowarpConfig'

const props = withDefaults(defineProps<{
  projectId?: number
  title?: string
  author?: string
  likeCount?: number
  coverUrl?: string
  size?: 'small' | 'medium' | 'large'
  autoLoad?: boolean
  showControls?: boolean
  showEditBtn?: boolean
  showInfo?: boolean
  showPlayHint?: boolean
}>(), {
  size: 'medium',
  autoLoad: false,
  showControls: true,
  showEditBtn: true,
  showInfo: false,
  showPlayHint: true
})

const router = useRouter()

const iframeRef = ref<HTMLIFrameElement>()
const wrapperRef = ref<HTMLDivElement>()

const loaded = ref(false)
const loading = ref(false)
const isFullscreen = ref(false)

const playerUrl = ref('')

async function loadPreview() {
  if (!props.projectId) return

  loading.value = true
  const downloadUrl = getDownloadUrl(props.projectId)
  playerUrl.value = buildPlayerUrl(downloadUrl)
  loaded.value = true
}

function onIframeLoad() {
  loading.value = false
}

function reload() {
  if (iframeRef.value?.contentWindow?.location) {
    iframeRef.value.contentWindow.location.reload()
  }
}

function toggleFullscreen() {
  if (!wrapperRef.value) return
  if (!document.fullscreenElement) {
    wrapperRef.value.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

function goToEditor() {
  if (props.projectId) {
    router.push(`/editor/${props.projectId}`)
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

onMounted(() => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
  if (props.autoLoad && props.projectId) {
    loadPreview()
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})

watch(() => props.projectId, (newId) => {
  if (newId && props.autoLoad) {
    loadPreview()
  } else if (!newId) {
    loaded.value = false
  }
})
</script>

<style scoped>
.scratch-preview {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: #1e1e2e;
  border: 2px solid #e2e8f0;
  transition: all 0.3s ease;
}

.scratch-preview:hover {
  border-color: #3B82F6;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.15);
}

.size-small .preview-wrapper { height: 200px; }
.size-medium .preview-wrapper { height: 360px; }
.size-large .preview-wrapper { height: 520px; }

.preview-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

.preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.4;
}

.placeholder-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #fff;
}

.placeholder-icon {
  font-size: 48px;
}

.placeholder-text {
  font-size: 16px;
  font-weight: 700;
  text-shadow: 0 1px 4px rgba(0,0,0,0.3);
}

.placeholder-hint {
  font-size: 13px;
  opacity: 0.8;
  padding: 4px 14px;
  background: rgba(255,255,255,0.2);
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

.preview-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(30, 30, 46, 0.9);
  color: #cdd6f4;
  font-size: 14px;
  z-index: 5;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(255,255,255,0.2);
  border-top-color: #3B82F6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.ctrl-btn {
  position: absolute;
  z-index: 10;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 14px;
  cursor: pointer;
  backdrop-filter: blur(4px);
  transition: all 0.2s ease;
  opacity: 0;
}

.scratch-preview:hover .ctrl-btn {
  opacity: 1;
}

.ctrl-btn:hover {
  background: rgba(0,0,0,0.7);
  transform: scale(1.05);
}

.fullscreen-btn { top: 8px; right: 8px; }
.edit-btn { top: 8px; left: 8px; }
.reload-btn { top: 8px; right: 50px; }

.preview-info {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #e2e8f0;
}

.info-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.info-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
}

.info-likes {
  font-weight: 600;
  color: #EF4444;
}

.is-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 9999;
  border-radius: 0;
  border: none;
}

.is-fullscreen .preview-wrapper {
  height: 100vh;
}

.is-fullscreen .ctrl-btn {
  opacity: 1;
}

@media (max-width: 768px) {
  .size-small .preview-wrapper { height: 160px; }
  .size-medium .preview-wrapper { height: 260px; }
  .size-large .preview-wrapper { height: 360px; }
  .ctrl-btn { opacity: 1; }
}
</style>
