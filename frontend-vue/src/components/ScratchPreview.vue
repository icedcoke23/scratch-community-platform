<template>
  <div class="scratch-preview" :class="[`size-${size}`, { 'is-fullscreen': isFullscreen }]">
    <!-- 预览 iframe -->
    <div class="preview-wrapper" ref="wrapperRef">
      <iframe
        v-if="loaded"
        ref="iframeRef"
        :src="playerUrl"
        class="preview-iframe"
        :allow="allowPermissions"
        sandbox="allow-scripts allow-same-origin allow-popups allow-forms"
        @load="onLoad"
      />
      <div v-else class="preview-placeholder" @click="loadPreview">
        <div class="placeholder-content">
          <span class="placeholder-icon">🎮</span>
          <span class="placeholder-text">{{ placeholderText }}</span>
          <span class="placeholder-hint" v-if="showPlayHint">点击加载预览</span>
        </div>
        <img v-if="coverUrl" :src="coverUrl" class="cover-img" :alt="title" />
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="preview-loading">
        <div class="loading-spinner" />
        <span>加载中...</span>
      </div>

      <!-- 全屏按钮 -->
      <button v-if="loaded && showControls" class="ctrl-btn fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
        {{ isFullscreen ? '✕' : '⛶' }}
      </button>

      <!-- 编辑按钮 -->
      <button v-if="loaded && showControls && showEditBtn" class="ctrl-btn edit-btn" @click="goToEditor">
        ✏️ 编辑
      </button>

      <!-- 重新加载 -->
      <button v-if="loaded && showControls" class="ctrl-btn reload-btn" @click="reload" title="重新加载">
        🔄
      </button>
    </div>

    <!-- 底部信息栏 -->
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
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { projectApi } from '@/api'

const props = withDefaults(defineProps<{
  /** 项目ID */
  projectId?: number
  /** 项目标题 */
  title?: string
  /** 作者名 */
  author?: string
  /** 点赞数 */
  likeCount?: number
  /** 封面图URL */
  coverUrl?: string
  /** 预览尺寸: small(卡片) / medium(详情) / large(全页) */
  size?: 'small' | 'medium' | 'large'
  /** 是否自动加载 */
  autoLoad?: boolean
  /** 显示控制按钮 */
  showControls?: boolean
  /** 显示编辑按钮 */
  showEditBtn?: boolean
  /** 显示底部信息 */
  showInfo?: boolean
  /** 占位文字 */
  placeholderText?: string
  /** 显示点击提示 */
  showPlayHint?: boolean
}>(), {
  size: 'medium',
  autoLoad: false,
  showControls: true,
  showEditBtn: true,
  showInfo: false,
  placeholderText: 'Scratch 项目',
  showPlayHint: true
})

const emit = defineEmits<{
  loaded: []
  error: [msg: string]
}>()

const router = useRouter()

const iframeRef = ref<HTMLIFrameElement>()
const wrapperRef = ref<HTMLDivElement>()
const loaded = ref(false)
const loading = ref(false)
const isFullscreen = ref(false)

const allowPermissions = 'clipboard-read; clipboard-write; autoplay'

// TurboWarp embed URL（播放器模式，用于预览）
// 注意：不能用 turbowarp.org/{id}/embed，因为 {id} 是我们平台的项目 ID，不是 Scratch 官方项目 ID
// 使用空 embed + autostart，后续通过 postMessage 加载 sb3 数据
const playerUrl = computed(() => {
  if (!props.projectId) return ''
  return `https://turbowarp.org/embed?autostart=true&controls=${props.showControls ? 'true' : 'false'}`
})

function loadPreview() {
  if (!props.projectId) return
  loading.value = true
  loaded.value = true
}

function onLoad() {
  loading.value = false
  emit('loaded')
  // iframe 加载完成后，获取 sb3 URL 并通过 postMessage 加载项目
  loadProjectIntoPreview()
}

/**
 * 获取项目的 sb3 URL 并通过 postMessage 发送给 TurboWarp embed
 */
async function loadProjectIntoPreview() {
  if (!props.projectId || !iframeRef.value?.contentWindow) return
  try {
    const res = await projectApi.getSb3Url(props.projectId)
    if (res.code === 0 && res.data) {
      const sb3Url = res.data as unknown as string
      // TurboWarp embed 支持 load-project 消息来加载 sb3 文件
      iframeRef.value.contentWindow.postMessage(
        { type: 'load-project', url: sb3Url },
        'https://turbowarp.org'
      )
    }
  } catch (e) {
    // 静默处理，预览加载失败不影响主流程
    console.warn('Scratch 预览加载项目失败:', e)
  }
}

function reload() {
  if (iframeRef.value) {
    loading.value = true
    iframeRef.value.src = iframeRef.value.src
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
  border: 2px solid var(--border, #e2e8f0);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.scratch-preview:hover {
  border-color: var(--primary-light, #60a5fa);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.15);
}

/* Sizes */
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

/* Placeholder */
.preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  transition: all 0.3s ease;
}

.preview-placeholder:hover {
  filter: brightness(1.1);
}

.preview-placeholder:hover .placeholder-icon {
  transform: scale(1.2);
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
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  filter: drop-shadow(0 2px 8px rgba(0,0,0,0.3));
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

/* Loading */
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
  border-top-color: var(--primary, #3B82F6);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Control buttons */
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

.fullscreen-btn {
  top: 8px;
  right: 8px;
}

.edit-btn {
  top: 8px;
  left: 8px;
}

.reload-btn {
  top: 8px;
  right: 50px;
}

/* Info bar */
.preview-info {
  padding: 12px 16px;
  background: var(--card, #fff);
  border-top: 1px solid var(--border, #e2e8f0);
}

.info-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text, #1e293b);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 13px;
  color: var(--text2, #64748b);
}

.info-author {
  font-weight: 500;
}

.info-likes {
  font-weight: 600;
  color: #EF4444;
}

/* Fullscreen */
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

/* Responsive */
@media (max-width: 768px) {
  .size-small .preview-wrapper { height: 160px; }
  .size-medium .preview-wrapper { height: 260px; }
  .size-large .preview-wrapper { height: 360px; }
  .ctrl-btn { opacity: 1; }
}
</style>
