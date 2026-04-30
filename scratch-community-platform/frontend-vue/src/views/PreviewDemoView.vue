<template>
  <div class="preview-demo">
    <header class="demo-header">
      <h1>ScratchPreview 组件演示</h1>
      <p>基于 TurboWarp 的项目预览组件</p>
    </header>

    <div class="demo-content">
      <section class="demo-section">
        <h2>基础用法</h2>
        <div class="demo-grid">
          <ScratchPreview
            :project-id="1"
            title="我的第一个游戏"
            author="小明"
            :like-count="128"
            size="medium"
          />
        </div>
      </section>

      <section class="demo-section">
        <h2>不同尺寸</h2>
        <div class="demo-row">
          <div class="demo-item">
            <h3>Small</h3>
            <ScratchPreview
              :project-id="2"
              title="小项目"
              size="small"
            />
          </div>
          <div class="demo-item">
            <h3>Medium</h3>
            <ScratchPreview
              :project-id="3"
              title="中等项目"
              size="medium"
            />
          </div>
          <div class="demo-item">
            <h3>Large</h3>
            <ScratchPreview
              :project-id="4"
              title="大项目"
              size="large"
            />
          </div>
        </div>
      </section>

      <section class="demo-section">
        <h2>显示信息</h2>
        <div class="demo-grid">
          <ScratchPreview
            :project-id="5"
            title="完整的项目卡片"
            author="创作者"
            :like-count="999"
            :view-count="5000"
            :cover-url="coverUrl"
            size="large"
            show-info
            :project-status="'published'"
          />
        </div>
      </section>

      <section class="demo-section">
        <h2>事件监听</h2>
        <div class="demo-grid">
          <ScratchPreview
            :project-id="6"
            title="点击事件演示"
            size="medium"
            :show-bridge-status="true"
            @load="onLoad"
            @loaded="onLoaded"
            @error="onError"
            @play="onPlay"
            @pause="onPause"
            @stop="onStop"
            @fullscreen="onFullscreen"
            @edit="onEdit"
          />
        </div>
        <div class="event-log">
          <h3>事件日志</h3>
          <div class="log-list">
            <div v-for="(log, index) in eventLogs" :key="index" class="log-item">
              <span class="log-time">{{ log.time }}</span>
              <span class="log-type" :class="log.type">{{ log.type }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
          <el-button size="small" @click="clearLogs">清除日志</el-button>
        </div>
      </section>

      <section class="demo-section">
        <h2>带封面的预览</h2>
        <div class="demo-grid">
          <ScratchPreview
            :project-id="7"
            title="艺术作品"
            author="艺术家"
            :like-count="42"
            :cover-url="coverUrl"
            size="large"
          />
        </div>
      </section>

      <section class="demo-section">
        <h2>自动加载</h2>
        <div class="demo-grid">
          <ScratchPreview
            :project-id="8"
            title="自动加载预览"
            author="开发者"
            :like-count="256"
            size="medium"
            :auto-load="true"
          />
        </div>
      </section>

      <section class="demo-section">
        <h2>功能列表</h2>
        <div class="feature-list">
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>基础预览功能 - 点击加载预览</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>TurboWarp player.html 集成</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>加载状态管理 - 显示加载进度</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>错误处理 - 友好的错误提示和重试机制</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>全屏功能 - 支持键盘快捷键 (ESC/F11)</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>控制按钮 - 编辑/重新加载/全屏</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>播放控制 - 播放/暂停/停止</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>项目信息展示 - 标题/作者/点赞数</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>响应式设计 - small/medium/large 尺寸</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>移动端适配 - 触摸友好</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>ScratchBridge 集成 - 播放器事件监听</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✅</span>
            <span>TypeScript 类型安全</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import ScratchPreview from '@/components/ScratchPreview.vue'

const coverUrl = 'https://picsum.photos/800/600?random=1'

interface LogEntry {
  time: string
  type: string
  message: string
}

const eventLogs = ref<LogEntry[]>([])

function getCurrentTime(): string {
  return new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function addLog(type: string, message: string) {
  eventLogs.value.unshift({
    time: getCurrentTime(),
    type,
    message
  })

  if (eventLogs.value.length > 10) {
    eventLogs.value.pop()
  }
}

function clearLogs() {
  eventLogs.value = []
}

function onLoad(projectId: number | string | undefined) {
  addLog('load', `加载项目 ID: ${projectId}`)
  ElMessage.info(`开始加载项目 ${projectId}`)
}

function onLoaded(projectId: number | string | undefined) {
  addLog('loaded', `项目已加载 ID: ${projectId}`)
  ElMessage.success(`项目 ${projectId} 加载完成`)
}

function onError(error: string) {
  addLog('error', error)
  ElMessage.error(error)
}

function onPlay() {
  addLog('play', '播放')
}

function onPause() {
  addLog('pause', '暂停')
}

function onStop() {
  addLog('stop', '停止')
}

function onFullscreen(isFullscreen: boolean) {
  addLog('fullscreen', isFullscreen ? '进入全屏' : '退出全屏')
}

function onEdit(projectId: number | string | undefined) {
  addLog('edit', `跳转到编辑器 ID: ${projectId}`)
  ElMessage.info(`跳转到编辑器: ${projectId}`)
}
</script>

<style scoped>
.preview-demo {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 20px;
}

.demo-header {
  max-width: 1200px;
  margin: 0 auto 40px;
  text-align: center;
}

.demo-header h1 {
  font-size: 36px;
  color: #1e293b;
  margin-bottom: 12px;
}

.demo-header p {
  font-size: 18px;
  color: #64748b;
}

.demo-content {
  max-width: 1200px;
  margin: 0 auto;
}

.demo-section {
  background: white;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.demo-section h2 {
  font-size: 24px;
  color: #1e293b;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e2e8f0;
}

.demo-grid {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.demo-grid > * {
  max-width: 600px;
  width: 100%;
}

.demo-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

.demo-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.demo-item h3 {
  font-size: 16px;
  color: #64748b;
  font-weight: 600;
}

.event-log {
  margin-top: 24px;
  padding: 20px;
  background: #1e1e2e;
  border-radius: 12px;
  color: #cdd6f4;
}

.event-log h3 {
  font-size: 16px;
  margin-bottom: 16px;
  color: #cdd6f4;
}

.log-list {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.log-item {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #313244;
  font-size: 13px;
  align-items: center;
}

.log-time {
  color: #6c7086;
  font-family: monospace;
}

.log-type {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
}

.log-type.load { background: #89b4fa; color: #1e1e2e; }
.log-type.loaded { background: #a6e3a1; color: #1e1e2e; }
.log-type.error { background: #f38ba8; color: #1e1e2e; }
.log-type.play { background: #cba6f7; color: #1e1e2e; }
.log-type.pause { background: #fab387; color: #1e1e2e; }
.log-type.stop { background: #f5c2e7; color: #1e1e2e; }
.log-type.fullscreen { background: #94e2d5; color: #1e1e2e; }
.log-type.edit { background: #f9e2af; color: #1e1e2e; }

.log-message {
  color: #cdd6f4;
  flex: 1;
}

.feature-list {
  display: grid;
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 15px;
  color: #334155;
}

.feature-icon {
  font-size: 20px;
}

@media (max-width: 768px) {
  .preview-demo {
    padding: 20px 12px;
  }

  .demo-header h1 {
    font-size: 28px;
  }

  .demo-section {
    padding: 20px;
  }

  .demo-section h2 {
    font-size: 20px;
  }

  .demo-grid {
    flex-direction: column;
  }

  .demo-row {
    grid-template-columns: 1fr;
  }
}
</style>
