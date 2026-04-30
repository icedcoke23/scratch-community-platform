<template>
  <el-dialog v-model="visible" title="分享项目" width="420px" @close="$emit('close')">
    <div class="share-content">
      <!-- 分享链接 -->
      <div class="share-section">
        <div class="share-label">分享链接</div>
        <div class="share-link-row">
          <el-input v-model="shareUrl" readonly size="large" />
          <el-button type="primary" @click="copyLink" :loading="copying">
            {{ copied ? '已复制' : '复制' }}
          </el-button>
        </div>
      </div>

      <!-- 分享到社交平台 -->
      <div class="share-section">
        <div class="share-label">分享到</div>
        <div class="share-platforms">
          <button class="share-btn share-wechat" @click="shareToWechat" title="微信">
            💬
            <span>微信</span>
          </button>
          <button class="share-btn share-qq" @click="shareToQQ" title="QQ">
            🐧
            <span>QQ</span>
          </button>
          <button class="share-btn share-weibo" @click="shareToWeibo" title="微博">
            📢
            <span>微博</span>
          </button>
          <button class="share-btn share-twitter" @click="shareToTwitter" title="Twitter">
            🐦
            <span>Twitter</span>
          </button>
        </div>
      </div>

      <!-- 项目海报 -->
      <div class="share-section">
        <div class="share-label">项目海报</div>
        <div class="poster-preview">
          <div class="poster-card">
            <div class="poster-icon">🧩</div>
            <div class="poster-title">{{ title }}</div>
            <div class="poster-author">{{ author }}</div>
            <div class="poster-stats">❤️ {{ likes }} · 👁️ {{ views }}</div>
          </div>
        </div>
        <el-button text @click="downloadPoster" style="margin-top: 8px">
          📥 下载海报图
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  projectId: number
  title: string
  author?: string
  likes?: number
  views?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const shareUrl = computed(() => {
  return `${window.location.origin}/project/${props.projectId}`
})

const copying = ref(false)
const copied = ref(false)

async function copyLink() {
  copying.value = true
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    copied.value = true
    ElMessage.success('链接已复制')
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    // 降级方案
    const input = document.createElement('input')
    input.value = shareUrl.value
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    copied.value = true
    ElMessage.success('链接已复制')
    setTimeout(() => { copied.value = false }, 2000)
  } finally {
    copying.value = false
  }
}

function shareToWechat() {
  ElMessage.info('请复制链接后在微信中粘贴分享')
  copyLink()
}

function shareToQQ() {
  const url = `https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(shareUrl.value)}&title=${encodeURIComponent(props.title)}`
  window.open(url, '_blank')
}

function shareToWeibo() {
  const url = `https://service.weibo.com/share/share.php?url=${encodeURIComponent(shareUrl.value)}&title=${encodeURIComponent(`来看看我的 Scratch 作品：${props.title}`)}`
  window.open(url, '_blank')
}

function shareToTwitter() {
  const url = `https://twitter.com/intent/tweet?url=${encodeURIComponent(shareUrl.value)}&text=${encodeURIComponent(`Check out my Scratch project: ${props.title}`)}`
  window.open(url, '_blank')
}

function downloadPoster() {
  ElMessage.info('海报生成功能开发中')
}
</script>

<style scoped>
.share-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.share-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.share-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.share-link-row {
  display: flex;
  gap: 8px;
}

.share-link-row .el-input {
  flex: 1;
}

.share-platforms {
  display: flex;
  gap: 12px;
}

.share-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--card);
  cursor: pointer;
  transition: all 0.15s;
  font-size: 24px;
  color: var(--text);
}

.share-btn span {
  font-size: 11px;
  color: var(--text2);
}

.share-btn:hover {
  border-color: var(--primary);
  background: var(--primary-bg);
}

.poster-preview {
  display: flex;
  justify-content: center;
}

.poster-card {
  width: 280px;
  padding: 24px;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border-radius: 12px;
  text-align: center;
  color: #fff;
}

.poster-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.poster-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
}

.poster-author {
  font-size: 13px;
  opacity: 0.8;
  margin-bottom: 8px;
}

.poster-stats {
  font-size: 12px;
  opacity: 0.7;
}

@media (max-width: 480px) {
  .share-platforms {
    flex-wrap: wrap;
  }
  .share-btn {
    flex: 1;
    min-width: 60px;
  }
  .poster-card {
    width: 100%;
  }
}
</style>
