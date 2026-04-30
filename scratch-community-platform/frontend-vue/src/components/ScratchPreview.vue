<template>
  <div
    class="scratch-preview"
    :class="[`size-${size}`, { 'is-fullscreen': isFullscreen, 'is-loading': loading, 'has-error': hasError }]"
    ref="containerRef"
  >
    <div class="preview-wrapper" ref="wrapperRef">
      <div
        v-if="!loaded && !loading"
        class="preview-placeholder"
        :class="{ 'has-cover': coverUrl }"
        @click="handleLoadPreview"
      >
        <img v-if="coverUrl" :src="coverUrl" class="cover-img" :alt="title" />
        <div class="placeholder-content">
          <span class="placeholder-icon">🎮</span>
          <span class="placeholder-title">{{ title || 'Scratch 项目预览' }}</span>
          <span class="placeholder-hint" v-if="showPlayHint">
            <el-icon><VideoPlay /></el-icon>
            点击加载预览
          </span>
        </div>
        <div v-if="author || likeCount !== undefined" class="placeholder-meta">
          <span v-if="author" class="meta-author">
            <el-icon><User /></el-icon>
            {{ author }}
          </span>
          <span v-if="likeCount !== undefined" class="meta-likes">
            <el-icon><Star /></el-icon>
            {{ formatCount(likeCount) }}
          </span>
        </div>
      </div>

      <div v-if="loading" class="preview-loading">
        <el-icon class="loading-icon is-loading"><Loading /></el-icon>
        <span class="loading-text">{{ loadingText }}</span>
        <el-progress
          v-if="loadProgress > 0"
          :percentage="loadProgress"
          :show-text="false"
          :stroke-width="3"
          class="loading-progress"
        />
      </div>

      <div v-if="hasError && !loading" class="preview-error">
        <el-icon class="error-icon"><WarningFilled /></el-icon>
        <p class="error-text">{{ errorMessage }}</p>
        <div class="error-actions">
          <el-button type="primary" size="small" @click="handleRetry">
            <el-icon><Refresh /></el-icon>
            重试
          </el-button>
          <el-button size="small" @click="handleDismissError">
            关闭
          </el-button>
        </div>
      </div>

      <iframe
        v-if="loaded && !hasError"
        ref="iframeRef"
        :src="playerUrl"
        class="preview-iframe"
        allow="autoplay; clipboard-read; clipboard-write"
        sandbox="allow-scripts allow-same-origin allow-popups allow-forms allow-downloads"
        @load="handleIframeLoad"
        @error="handleIframeError"
      />

      <div v-if="loaded && showControls && !hasError" class="preview-controls">
        <div class="control-bar">
          <div class="control-left">
            <el-tooltip content="重新加载" placement="top">
              <button class="ctrl-btn" @click="handleReload" :disabled="reloading">
                <el-icon :class="{ 'is-loading': reloading }"><RefreshRight /></el-icon>
              </button>
            </el-tooltip>
          </div>

          <div class="control-center" v-if="showPlaybackControls">
            <el-tooltip :content="playbackState === 'running' ? '暂停' : '播放'" placement="top">
              <button class="ctrl-btn ctrl-btn-play" @click="togglePlayback">
                <el-icon v-if="playbackState === 'running'"><VideoPause /></el-icon>
                <el-icon v-else><VideoPlay /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="停止" placement="top">
              <button class="ctrl-btn" @click="stopPlayback">
                <el-icon><VideoCamera /></el-icon>
              </button>
            </el-tooltip>
          </div>

          <div class="control-right">
            <el-tooltip v-if="showEditBtn" content="在编辑器中打开" placement="top">
              <button class="ctrl-btn" @click="handleGoToEditor">
                <el-icon><Edit /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip :content="isFullscreen ? '退出全屏 (ESC)' : '全屏'" placement="top">
              <button class="ctrl-btn" @click="toggleFullscreen">
                <el-icon>
                  <FullScreen v-if="!isFullscreen" />
                  <Close v-else />
                </el-icon>
              </button>
            </el-tooltip>
          </div>
        </div>

        <div v-if="showBridgeStatus" class="bridge-status">
          <span class="status-indicator" :class="{ connected: bridgeConnected }"></span>
          <span class="status-text">{{ bridgeConnected ? '已连接' : '连接中...' }}</span>
        </div>
      </div>

      <div v-if="isFullscreen" class="fullscreen-hint">
        按 ESC 退出全屏
      </div>
    </div>

    <div v-if="showInfo && (title || author || likeCount !== undefined)" class="preview-info">
      <div class="info-main">
        <h3 class="info-title">{{ title || '未命名项目' }}</h3>
        <div class="info-tags">
          <el-tag v-if="projectStatus" size="small" :type="projectStatus === 'published' ? 'success' : 'info'">
            {{ projectStatus === 'published' ? '已发布' : '草稿' }}
          </el-tag>
        </div>
      </div>
      <div class="info-meta">
        <span v-if="author" class="info-author">
          <el-icon><User /></el-icon>
          {{ author }}
        </span>
        <span v-if="likeCount !== undefined" class="info-likes">
          <el-icon><Star /></el-icon>
          {{ formatCount(likeCount) }}
        </span>
        <span v-if="viewCount !== undefined" class="info-views">
          <el-icon><View /></el-icon>
          {{ formatCount(viewCount) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  VideoPlay,
  VideoPause,
  VideoCamera,
  Refresh,
  RefreshRight,
  FullScreen,
  Close,
  Edit,
  User,
  Star,
  View,
  Loading,
  WarningFilled
} from '@element-plus/icons-vue'
import { buildPlayerUrl, getDownloadUrl } from '@/utils/turbowarpConfig'
import { createScratchBridge, type PlaybackState, type BridgeCallbacks } from '@/utils/scratchBridge'

export interface ScratchPreviewProps {
  projectId?: number | string
  projectUrl?: string
  title?: string
  author?: string
  likeCount?: number
  viewCount?: number
  coverUrl?: string
  size?: 'small' | 'medium' | 'large'
  autoLoad?: boolean
  showControls?: boolean
  showEditBtn?: boolean
  showInfo?: boolean
  showPlayHint?: boolean
  showPlaybackControls?: boolean
  showBridgeStatus?: boolean
  projectStatus?: 'draft' | 'published'
}

const props = withDefaults(defineProps<ScratchPreviewProps>(), {
  size: 'medium',
  autoLoad: false,
  showControls: true,
  showEditBtn: true,
  showInfo: false,
  showPlayHint: true,
  showPlaybackControls: true,
  showBridgeStatus: false,
  projectStatus: undefined
})

const emit = defineEmits<{
  load: [projectId: number | string | undefined]
  loaded: [projectId: number | string | undefined]
  error: [error: string]
  play: []
  pause: []
  stop: []
  fullscreen: [isFullscreen: boolean]
  edit: [projectId: number | string | undefined]
}>()

const router = useRouter()

const containerRef = ref<HTMLDivElement>()
const wrapperRef = ref<HTMLDivElement>()
const iframeRef = ref<HTMLIFrameElement>()

const loaded = ref(false)
const loading = ref(false)
const reloading = ref(false)
const hasError = ref(false)
const errorMessage = ref('')
const loadProgress = ref(0)
const loadingText = ref('正在加载预览...')
const isFullscreen = ref(false)
const playbackState = ref<PlaybackState>('stopped')
const bridgeConnected = ref(false)

const bridge = ref<ReturnType<typeof createScratchBridge> | null>(null)

const playerUrl = computed(() => {
  if (props.projectUrl) {
    return buildPlayerUrl(props.projectUrl, { autoplay: true })
  }
  if (props.projectId) {
    const downloadUrl = getDownloadUrl(props.projectId)
    return buildPlayerUrl(downloadUrl, { autoplay: true })
  }
  return ''
})

const numericProjectId = computed(() => {
  if (!props.projectId) return undefined
  return typeof props.projectId === 'string' ? parseInt(props.projectId, 10) : props.projectId
})

function formatCount(count: number): string {
  if (count >= 1000000) {
    return (count / 1000000).toFixed(1) + 'M'
  }
  if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'K'
  }
  return count.toString()
}

function initBridge() {
  if (!iframeRef.value) return

  const callbacks: BridgeCallbacks = {
    onReady: (_mode, state) => {
      bridgeConnected.value = true
      console.log('[ScratchPreview] Bridge ready, state:', state)
    },
    onError: (error) => {
      console.error('[ScratchPreview] Bridge error:', error)
      handleBridgeError(error)
    },
    onPlaybackStateChange: (state) => {
      playbackState.value = state
      console.log('[ScratchPreview] Playback state changed:', state)
    },
    onGreenFlag: () => {
      console.log('[ScratchPreview] Green flag clicked')
      emit('play')
    },
    onStopAll: () => {
      console.log('[ScratchPreview] Stop all')
      emit('stop')
    }
  }

  bridge.value = createScratchBridge({
    iframe: iframeRef.value,
    debug: true,
    callbacks
  })

  bridge.value.start()
}

function handleBridgeError(error: string) {
  hasError.value = true
  errorMessage.value = `播放器错误: ${error}`
  emit('error', errorMessage.value)
}

function handleLoadPreview() {
  if (!props.projectId && !props.projectUrl) {
    ElMessage.warning('没有可用的项目')
    return
  }

  loading.value = true
  hasError.value = false
  loadProgress.value = 0
  loadingText.value = '正在准备播放器...'

  emit('load', numericProjectId.value)

  simulateLoading()
}

function simulateLoading() {
  let progress = 0
  const interval = setInterval(() => {
    progress += Math.random() * 15
    if (progress >= 90) {
      clearInterval(interval)
      loadProgress.value = 90
    } else {
      loadProgress.value = Math.min(progress, 85)
      loadingText.value = `加载中 ${Math.round(loadProgress.value)}%`
    }
  }, 200)
}

function handleIframeLoad() {
  loading.value = false
  loaded.value = true
  loadProgress.value = 100
  loadingText.value = '加载完成'

  nextTick(() => {
    initBridge()
    emit('loaded', numericProjectId.value)
  })
}

function handleIframeError() {
  loading.value = false
  hasError.value = true
  errorMessage.value = '播放器加载失败，请检查网络连接后重试'
  emit('error', errorMessage.value)
}

function handleRetry() {
  hasError.value = false
  errorMessage.value = ''

  if (loaded.value && iframeRef.value) {
    handleReload()
  } else {
    handleLoadPreview()
  }
}

function handleDismissError() {
  hasError.value = false
  errorMessage.value = ''
}

function handleReload() {
  if (!iframeRef.value?.contentWindow) return

  reloading.value = true
  loadingText.value = '正在重新加载...'
  loading.value = true
  bridgeConnected.value = false

  try {
    iframeRef.value.contentWindow.location.reload()
  } catch (e) {
    console.error('[ScratchPreview] Reload failed:', e)
    handleRetry()
  }

  setTimeout(() => {
    reloading.value = false
  }, 2000)
}

function togglePlayback() {
  if (!bridge.value?.isReady()) {
    ElMessage.warning('播放器尚未就绪')
    return
  }

  if (playbackState.value === 'running') {
    bridge.value.pause()
    emit('pause')
  } else {
    bridge.value.play()
    emit('play')
  }
}

function stopPlayback() {
  if (!bridge.value?.isReady()) return

  bridge.value.stopPlayback()
  emit('stop')
}

function toggleFullscreen() {
  if (!wrapperRef.value) return

  if (!document.fullscreenElement) {
    wrapperRef.value.requestFullscreen().then(() => {
      isFullscreen.value = true
      emit('fullscreen', true)
    }).catch((e) => {
      console.error('[ScratchPreview] Fullscreen error:', e)
      ElMessage.error('无法进入全屏模式')
    })
  } else {
    document.exitFullscreen().then(() => {
      isFullscreen.value = false
      emit('fullscreen', false)
    }).catch((e) => {
      console.error('[ScratchPreview] Exit fullscreen error:', e)
    })
  }
}

function handleGoToEditor() {
  if (numericProjectId.value) {
    emit('edit', numericProjectId.value)
    router.push(`/editor/${numericProjectId.value}`)
  } else {
    router.push('/editor/new')
  }
}

function handleFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
  emit('fullscreen', isFullscreen.value)
}

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape' && isFullscreen.value) {
    e.preventDefault()
    toggleFullscreen()
  }

  if (e.key === 'F11') {
    e.preventDefault()
    toggleFullscreen()
  }

  if (e.key === ' ' && loaded.value && !hasError.value) {
    if (document.activeElement === document.body || document.activeElement === containerRef.value) {
      e.preventDefault()
      togglePlayback()
    }
  }
}

watch(() => props.projectId, (newId) => {
  if (newId && props.autoLoad) {
    handleLoadPreview()
  } else if (!newId) {
    loaded.value = false
    hasError.value = false
    if (bridge.value) {
      bridge.value.destroy()
      bridge.value = null
    }
  }
})

watch(isFullscreen, (fullscreen) => {
  if (fullscreen && wrapperRef.value) {
    wrapperRef.value.style.height = '100vh'
  } else if (wrapperRef.value) {
    const heightMap = {
      small: '200px',
      medium: '360px',
      large: '520px'
    }
    wrapperRef.value.style.height = heightMap[props.size]
  }
})

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('keydown', handleKeyDown)

  if (props.autoLoad && (props.projectId || props.projectUrl)) {
    handleLoadPreview()
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('keydown', handleKeyDown)

  if (bridge.value) {
    bridge.value.destroy()
    bridge.value = null
  }
})
</script>

<style scoped>
.scratch-preview {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: #1e1e2e;
  border: 2px solid #313244;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.scratch-preview:hover {
  border-color: #585b70;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.2);
}

.scratch-preview.is-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 9999;
  border-radius: 0;
  border: none;
}

.scratch-preview.has-error {
  border-color: #f38ba8;
}

.size-small .preview-wrapper { height: 200px; }
.size-medium .preview-wrapper { height: 360px; }
.size-large .preview-wrapper { height: 520px; }

.preview-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #000;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
  background: #000;
}

.preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transition: all 0.3s ease;
}

.preview-placeholder:hover {
  transform: scale(1.02);
}

.preview-placeholder:hover .placeholder-content {
  transform: translateY(-8px);
}

.preview-placeholder:hover .placeholder-hint {
  opacity: 1;
  transform: translateY(0);
}

.cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.3;
  transition: opacity 0.3s ease;
}

.preview-placeholder:hover .cover-img {
  opacity: 0.4;
}

.placeholder-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #fff;
  transition: transform 0.3s ease;
}

.placeholder-icon {
  font-size: 64px;
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.3));
}

.placeholder-title {
  font-size: 18px;
  font-weight: 700;
  text-shadow: 0 2px 8px rgba(0,0,0,0.3);
  max-width: 80%;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.placeholder-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  opacity: 0.9;
  padding: 8px 20px;
  background: rgba(255,255,255,0.2);
  border-radius: 24px;
  backdrop-filter: blur(8px);
  transition: all 0.3s ease;
  transform: translateY(8px);
}

.placeholder-meta {
  position: absolute;
  bottom: 12px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 16px;
  font-size: 13px;
  color: rgba(255,255,255,0.9);
  z-index: 1;
}

.meta-author,
.meta-likes {
  display: flex;
  align-items: center;
  gap: 4px;
}

.preview-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: rgba(30, 30, 46, 0.95);
  color: #cdd6f4;
  z-index: 10;
}

.loading-icon {
  font-size: 48px;
  color: #89b4fa;
}

.loading-text {
  font-size: 16px;
  font-weight: 500;
}

.loading-progress {
  width: 200px;
}

.loading-progress :deep(.el-progress-bar__outer) {
  background: rgba(255,255,255,0.1);
}

.loading-progress :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.preview-error {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: rgba(30, 30, 46, 0.98);
  color: #cdd6f4;
  z-index: 10;
  padding: 24px;
}

.error-icon {
  font-size: 48px;
  color: #f38ba8;
}

.error-text {
  font-size: 15px;
  color: #f38ba8;
  text-align: center;
  max-width: 300px;
  line-height: 1.6;
  margin: 0;
}

.error-actions {
  display: flex;
  gap: 12px;
}

.preview-controls {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  pointer-events: none;
  z-index: 5;
}

.preview-controls > * {
  pointer-events: auto;
}

.control-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: linear-gradient(180deg, rgba(0,0,0,0.6) 0%, transparent 100%);
}

.control-left,
.control-center,
.control-right {
  display: flex;
  gap: 8px;
}

.ctrl-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: all 0.2s ease;
  font-size: 18px;
}

.ctrl-btn:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.8);
  transform: scale(1.1);
}

.ctrl-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.ctrl-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ctrl-btn-play {
  width: 48px;
  height: 48px;
  font-size: 22px;
  background: rgba(102, 126, 234, 0.8);
  border-radius: 50%;
}

.ctrl-btn-play:hover:not(:disabled) {
  background: rgba(102, 126, 234, 1);
}

.bridge-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  border-radius: 8px;
  font-size: 12px;
  color: #cdd6f4;
  margin: 12px;
  width: fit-content;
  align-self: flex-end;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f38ba8;
  animation: pulse 1.5s infinite;
}

.status-indicator.connected {
  background: #a6e3a1;
  animation: none;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.fullscreen-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 12px 24px;
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  border-radius: 8px;
  font-size: 14px;
  backdrop-filter: blur(8px);
  z-index: 20;
  opacity: 0;
  animation: fadeInHint 0.3s ease 0.5s forwards;
  pointer-events: none;
}

@keyframes fadeInHint {
  to { opacity: 1; }
}

.preview-info {
  padding: 16px 20px;
  background: #1e1e2e;
  border-top: 1px solid #313244;
}

.info-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.info-title {
  font-size: 16px;
  font-weight: 700;
  color: #cdd6f4;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.info-tags {
  flex-shrink: 0;
  margin-left: 12px;
}

.info-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #6c7086;
}

.info-author,
.info-likes,
.info-views {
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-likes {
  color: #f38ba8;
}

.info-views {
  color: #89b4fa;
}

@media (max-width: 768px) {
  .size-small .preview-wrapper { height: 160px; }
  .size-medium .preview-wrapper { height: 260px; }
  .size-large .preview-wrapper { height: 360px; }

  .placeholder-icon {
    font-size: 48px;
  }

  .placeholder-title {
    font-size: 16px;
  }

  .ctrl-btn {
    width: 44px;
    height: 44px;
    opacity: 1;
  }

  .control-bar {
    padding: 8px;
  }

  .preview-info {
    padding: 12px 16px;
  }

  .info-title {
    font-size: 15px;
  }

  .info-meta {
    gap: 12px;
    font-size: 12px;
  }

  .placeholder-meta {
    font-size: 12px;
    bottom: 8px;
    gap: 12px;
  }

  .bridge-status {
    font-size: 11px;
    padding: 6px 10px;
    margin: 8px;
  }
}

@media (max-width: 480px) {
  .control-center {
    gap: 4px;
  }

  .ctrl-btn-play {
    width: 44px;
    height: 44px;
  }

  .error-actions {
    flex-direction: column;
    width: 100%;
    max-width: 200px;
  }
}
</style>
