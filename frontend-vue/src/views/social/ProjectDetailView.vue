<template>
  <div>
    <div class="back-link" @click="router.push('/feed')">← {{ t('common.back') }}</div>
    <LoadingSkeleton v-if="loading" variant="detail" />
    <template v-else-if="project">
      <!-- 项目信息 -->
      <div class="page-card project-info-card">
        <div class="project-title-row">
          <h1 class="project-title">{{ project.title }}</h1>
          <button
            v-if="userStore.isLoggedIn"
            class="like-btn"
            :class="{ liked: isLiked }"
            @click="toggleLike"
          >
            <span class="like-icon">{{ isLiked ? '❤️' : '🤍' }}</span>
            <span class="like-count">{{ project.likeCount || 0 }}</span>
          </button>
        </div>
        <div class="project-meta-bar">
          <span class="meta-author">
            <span class="author-badge">{{ (project.username || '?')[0] }}</span>
            {{ project.username }}
          </span>
          <span class="meta-stat"><span class="stat-icon">👁️</span> {{ project.viewCount || 0 }}</span>
          <span class="meta-stat"><span class="stat-icon">💬</span> {{ project.commentCount || 0 }}</span>
          <span v-if="project.remixCount" class="meta-stat"><span class="stat-icon">🔄</span> {{ project.remixCount }} Remix</span>
          <span class="meta-time">{{ timeAgo(project.createdAt) }}</span>
        </div>
        <div v-if="project.remixProjectId" class="remix-source">
          🔄 Remix 自
          <router-link :to="`/project/${project.remixProjectId}`" class="remix-link">
            项目 #{{ project.remixProjectId }}
          </router-link>
        </div>
        <div v-if="project.description" class="project-desc">
          {{ project.description }}
        </div>
        <div class="action-buttons">
          <el-button v-if="userStore.isLoggedIn" type="primary" size="large" @click="doRemix" class="action-btn remix-action">
            🔄 {{ t('project.remix') }}
          </el-button>
          <el-button v-if="project.remixCount" size="large" @click="showRemixes" class="action-btn">
            🔄 {{ t('project.remix') }} ({{ project.remixCount }})
          </el-button>
          <el-button v-if="userStore.isLoggedIn && userStore.user?.id === project.userId" size="large" type="warning" @click="router.push(`/editor/${project.id}`)" class="action-btn">
            ✏️ {{ t('common.edit') }}
          </el-button>
          <el-button v-if="userStore.isLoggedIn" size="large" @click="generateAiReview" :loading="aiLoading" :disabled="isStreaming" class="action-btn ai-action">
            {{ isStreaming ? '🤖 分析中...' : '🤖 ' + t('project.aiReview') }}
          </el-button>
          <el-button size="large" @click="showShareDialog = true" class="action-btn">
            📤 {{ t('project.share') }}
          </el-button>
        </div>
      </div>

      <!-- AI 点评（流式生成中） -->
      <div v-if="isStreaming && streamingContent" class="page-card streaming-card">
        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px">
          <span class="streaming-dot" />
          <span style="font-weight: 600; font-size: 14px">🤖 AI 正在分析...</span>
        </div>
        <div class="streaming-text">{{ streamingContent }}</div>
      </div>

      <!-- AI 点评 -->
      <div v-if="aiReview" class="page-card" style="border: 2px solid var(--primary)">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
          <div style="font-weight: 700; font-size: 16px">🤖 AI 点评</div>
          <div style="font-size: 20px; letter-spacing: 2px">{{ aiReview.starDisplay || '☆☆☆☆☆' }}</div>
        </div>
        <p v-if="aiReview.summary" style="font-size: 14px; line-height: 1.6; margin-bottom: 12px">{{ aiReview.summary }}</p>
        <div v-if="aiReview.dimensionScores" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 8px; margin-bottom: 12px">
          <div v-for="(score, dim) in aiReview.dimensionScores" :key="dim" style="text-align: center">
            <div style="font-size: 11px; color: var(--text2)">{{ dimNames[dim as string] || dim }}</div>
            <div style="font-size: 14px; color: var(--primary); font-family: monospace">
              {{ '█'.repeat(score) }}{{ '░'.repeat(5 - score) }}
            </div>
            <div style="font-size: 12px; font-weight: 600">{{ score }}/5</div>
          </div>
        </div>
        <div v-if="aiReview.strengths?.length" style="margin-bottom: 10px">
          <div style="font-weight: 600; font-size: 13px; margin-bottom: 4px">✅ 优点</div>
          <div v-for="s in aiReview.strengths" :key="s" style="font-size: 13px; color: var(--text2); padding: 2px 0">• {{ s }}</div>
        </div>
        <div v-if="aiReview.suggestions?.length" style="margin-bottom: 10px">
          <div style="font-weight: 600; font-size: 13px; margin-bottom: 4px">💡 改进建议</div>
          <div v-for="s in aiReview.suggestions" :key="s" style="font-size: 13px; color: var(--text2); padding: 2px 0">• {{ s }}</div>
        </div>
      </div>

      <!-- 评论 -->
      <div class="page-card">
        <div style="font-weight: 600; margin-bottom: 14px">💬 {{ t('project.comment') }} ({{ comments.length }})</div>
        <div v-if="userStore.isLoggedIn" style="display: flex; gap: 8px; margin-bottom: 16px">
          <el-input v-model="newComment" :placeholder="t('project.comment') + '...'" @keyup.enter="submitComment" />
          <el-button type="primary" @click="submitComment" :loading="commentLoading">{{ t('common.submit') }}</el-button>
        </div>
        <div v-if="comments.length === 0" class="empty-state" style="padding: 20px">{{ t('common.empty') }}</div>
        <div v-for="c in comments" :key="c.id" class="comment-box">
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px">
            <div class="comment-avatar">{{ (c.nickname || c.username || '?')[0] }}</div>
            <span style="font-size: 13px; font-weight: 600">{{ c.nickname || c.username }}</span>
            <span style="font-size: 11px; color: var(--text2)">{{ timeAgo(c.createdAt) }}</span>
            <el-link
              v-if="userStore.user && (userStore.user.id === c.userId || userStore.isAdmin)"
              type="danger"
              style="font-size: 12px; margin-left: auto"
              @click="deleteComment(c.id)"
            >
              {{ t('common.delete') }}
            </el-link>
          </div>
          <div style="font-size: 14px; line-height: 1.5">{{ c.content }}</div>
        </div>
      </div>
    </template>

    <!-- Remix 列表弹窗 -->
    <el-dialog v-model="showRemixDialog" :title="'🔄 ' + t('project.remix')" width="500px">
      <div v-if="remixLoading" class="empty-state">{{ t('common.loading') }}</div>
      <div v-else-if="remixList.length === 0" class="empty-state">{{ t('common.empty') }}</div>
      <div v-else>
        <div
          v-for="r in remixList"
          :key="r.id"
          class="page-card"
          style="cursor: pointer; margin-bottom: 8px"
          @click="showRemixDialog = false; router.push(`/project/${r.id}`)"
        >
          <div style="font-weight: 600">{{ r.title }}</div>
          <div style="font-size: 12px; color: var(--text2)">❤️ {{ r.likeCount }} | 💬 {{ r.commentCount }} | {{ timeAgo(r.createdAt) }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- 分享弹窗 -->
    <ShareDialog
      v-model="showShareDialog"
      :project-id="projectId"
      :title="project?.title || ''"
      :author="project?.nickname || project?.username"
      :likes="project?.likeCount"
      :views="project?.viewCount"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProjectDetailView' })
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { projectApi, socialApi, aiReviewApi } from '@/api'
import type { ProjectDetail, Comment, AiReview } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getErrorMessage } from '@/utils/error'
import { timeAgo } from '@/utils'
import { useI18n } from '@/composables/useI18n'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import ShareDialog from '@/components/ShareDialog.vue'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const project = ref<ProjectDetail | null>(null)
const comments = ref<Comment[]>([])
const isLiked = ref(false)
const newComment = ref('')
const commentLoading = ref(false)
const aiReview = ref<AiReview | null>(null)
const aiLoading = ref(false)
const isStreaming = ref(false)
const streamingContent = ref('')
let eventSource: EventSource | null = null
let streamTimeout: ReturnType<typeof setTimeout> | null = null

const dimNames: Record<string, string> = {
  codeStructure: '代码结构', creativity: '创意表现', complexity: '复杂度',
  readability: '可读性', bestPractice: '最佳实践'
}

const projectId = Number(route.params.id)

async function toggleLike() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await socialApi.toggleLike(projectId, isLiked.value)
    if (res.code === 0) {
      isLiked.value = !isLiked.value
      if (project.value) project.value.likeCount += isLiked.value ? 1 : -1
    }
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
}

async function submitComment() {
  if (!newComment.value.trim()) return
  commentLoading.value = true
  try {
    const res = await socialApi.addComment(projectId, newComment.value.trim())
    if (res.code === 0) {
      newComment.value = ''
      ElMessage.success('评论成功')
      await loadComments()
    } else ElMessage.error(res.msg)
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
  finally { commentLoading.value = false }
}

async function deleteComment(commentId: number) {
  try {
    await ElMessageBox.confirm('确定删除此评论？', '提示', { type: 'warning' })
    const res = await socialApi.deleteComment(commentId)
    if (res.code === 0) { ElMessage.success('已删除'); await loadComments() }
  } catch { /* 取消 */ }
}

async function loadComments() {
  try {
    const res = await socialApi.getComments(projectId)
    if (res.code === 0) comments.value = res.data?.records || []
  } catch { /* 忽略 */ }
}

async function doRemix() {
  try {
    await ElMessageBox.confirm('确定要 Remix 这个项目吗？', 'Remix')
    const res = await projectApi.remix(projectId)
    if (res.code === 0) { ElMessage.success('Remix 成功！'); router.push(`/project/${res.data?.id}`) }
    else ElMessage.error(res.msg)
  } catch { /* 取消 */ }
}

const showRemixDialog = ref(false)
const remixList = ref<any[]>([])
const remixLoading = ref(false)
const showShareDialog = ref(false)

async function showRemixes() {
  showRemixDialog.value = true
  remixLoading.value = true
  try {
    const res = await projectApi.getRemixes(projectId, 1, 20)
    if (res.code === 0) remixList.value = res.data?.records || []
  } catch { /* 忽略 */ }
  finally { remixLoading.value = false }
}

async function generateAiReview() {
  aiLoading.value = true
  isStreaming.value = true
  streamingContent.value = ''

  // 尝试 SSE 流式点评（使用一次性 Token）
  try {
    eventSource = await aiReviewApi.stream(projectId, {
      onToken(token) {
        streamingContent.value += token
      },
      onComplete(review) {
        aiReview.value = review
        isStreaming.value = false
        aiLoading.value = false
        streamingContent.value = ''
        clearStreamTimeout()
      },
      onError() {
        // 降级到非流式
        fallbackGenerate()
      }
    })
  } catch {
    fallbackGenerate()
    return
  }

  // 10 秒超时降级
  clearStreamTimeout()
  streamTimeout = setTimeout(() => {
    if (isStreaming.value) {
      eventSource?.close()
      fallbackGenerate()
    }
  }, 10000)
}

function clearStreamTimeout() {
  if (streamTimeout) {
    clearTimeout(streamTimeout)
    streamTimeout = null
  }
}

async function fallbackGenerate() {
  if (!isStreaming.value) return
  eventSource?.close()
  try {
    const res = await aiReviewApi.generate(projectId)
    if (res.code === 0) aiReview.value = res.data || null
    else ElMessage.error(res.msg)
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
  finally { isStreaming.value = false; aiLoading.value = false; streamingContent.value = '' }
}

onMounted(async () => {
  try {
    const [pr, cr, lr] = await Promise.all([
      projectApi.getDetail(projectId),
      socialApi.getComments(projectId).catch(() => ({ code: -1, data: null } as { code: number; data: null })),
      userStore.isLoggedIn
        ? socialApi.isLiked(projectId).catch(() => ({ code: -1, data: false } as { code: number; data: boolean }))
        : Promise.resolve({ code: -1, data: false })
    ])
    if (pr.code === 0) {
      project.value = pr.data || null
      comments.value = cr.code === 0 ? (cr.data?.records || []) : []
      isLiked.value = lr.code === 0 ? !!lr.data : false
      try {
        const ar = await aiReviewApi.getLatest(projectId)
        if (ar.code === 0 && ar.data) aiReview.value = ar.data
      } catch { /* 忽略 */ }
    } else { ElMessage.error(pr.msg); router.push('/feed') }
  } catch { /* 忽略 */ }
  finally { loading.value = false }
})

onBeforeUnmount(() => {
  eventSource?.close()
  clearStreamTimeout()
})
</script>

<style scoped>
/* 项目信息卡片 */
.project-info-card {
  padding: 24px;
}

.project-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.project-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
  margin: 0;
  flex: 1;
  min-width: 0;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 2px solid var(--border);
  border-radius: 20px;
  background: var(--card);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  font-size: 18px;
  flex-shrink: 0;
}

.like-btn:hover {
  transform: scale(1.1);
  border-color: #FCA5A5;
  background: #FEF2F2;
}

.like-btn.liked {
  border-color: #EF4444;
  background: #FEF2F2;
}

.like-icon {
  font-size: 22px;
}

.like-count {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.project-meta-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--text2);
}

.meta-author {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.author-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--primary-bg);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.meta-stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-icon {
  font-size: 16px;
}

.meta-time {
  color: var(--text3);
}

.remix-source {
  font-size: 14px;
  color: var(--text2);
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--primary-bg);
  border-radius: 10px;
  display: inline-block;
}

.remix-link {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
}

.remix-link:hover {
  text-decoration: underline;
}

.project-desc {
  font-size: 15px;
  color: var(--text2);
  line-height: 1.8;
  white-space: pre-wrap;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--bg);
  border-radius: 12px;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.action-btn {
  font-weight: 600;
  border-radius: 12px;
  min-height: 44px;
  padding: 10px 20px;
  font-size: 15px;
}

.remix-action {
  background: linear-gradient(135deg, var(--primary), var(--accent-purple));
  border: none;
}

.ai-action {
  background: linear-gradient(135deg, var(--accent-green), var(--accent-cyan));
  border: none;
  color: #fff;
}

/* 评论区 */
.comment-box { border-top: 1px solid var(--border); padding: 16px 0; }
.comment-box:first-child { border-top: none; }
.comment-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-bg), #F3E8FF);
  display: flex; align-items: center;
  justify-content: center; font-size: 14px; font-weight: 700; color: var(--primary);
}
.like-btn { transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); }

/* AI 点评流式 */
.streaming-card {
  border: 2px dashed var(--primary-light);
  background: var(--primary-bg);
  padding: 20px;
  border-radius: 16px;
}
.streaming-text {
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  color: var(--text);
}
.streaming-dot {
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--primary);
  animation: pulse-dot 1s ease-in-out infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.8); }
}
.tag-role {
  background: var(--primary-bg); color: var(--primary);
  font-size: 12px; padding: 3px 10px; border-radius: 8px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .project-title { font-size: 20px; }
  .project-meta-bar { gap: 10px; }
  .action-buttons { flex-direction: column; }
  .action-btn { width: 100%; }
}
</style>
