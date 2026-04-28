<template>
  <div class="comment-section">
    <div class="comment-header">
      <h3>💬 评论 ({{ total }})</h3>
    </div>

    <!-- 发表评论 -->
    <div v-if="isLoggedIn" class="comment-input">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="2"
        placeholder="写下你的评论..."
        maxlength="500"
        show-word-limit
      />
      <el-button
        type="primary"
        size="small"
        :loading="submitting"
        :disabled="!newComment.trim()"
        @click="submitComment"
      >
        发表
      </el-button>
    </div>
    <div v-else class="login-hint">
      <el-button type="primary" link @click="$emit('login')">登录后评论</el-button>
    </div>

    <!-- 评论列表 -->
    <div v-if="loading" class="empty-state">加载中...</div>
    <div v-else-if="comments.length === 0" class="empty-state">暂无评论，快来抢沙发！</div>
    <div v-else class="comment-list">
      <div v-for="c in comments" :key="c.id" class="comment-item">
        <div class="comment-avatar">{{ (c.nickname || c.username || '?')[0] }}</div>
        <div class="comment-body">
          <div class="comment-meta">
            <span class="comment-author">{{ c.nickname || c.username || '匿名' }}</span>
            <span class="comment-time">{{ timeAgo(c.createdAt) }}</span>
            <el-button
              v-if="canDelete(c)"
              type="danger"
              link
              size="small"
              @click="deleteComment(c.id)"
            >
              删除
            </el-button>
          </div>
          <div class="comment-content">{{ c.content }}</div>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore" class="load-more">
      <el-button link @click="loadMore">加载更多评论</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { socialApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { Comment } from '@/types'
import { timeAgo } from '@/utils'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  projectId: number
}>()

defineEmits<{
  login: []
}>()

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

const comments = ref<Comment[]>([])
const loading = ref(true)
const submitting = ref(false)
const newComment = ref('')
const total = ref(0)
const page = ref(1)
const pageSize = 20
const hasMore = computed(() => comments.value.length < total.value)

const canDelete = (c: Comment) => {
  return userStore.user?.id === c.userId || userStore.user?.role === 'ADMIN'
}

async function loadComments(reset = false) {
  if (reset) {
    page.value = 1
    comments.value = []
  }
  loading.value = true
  try {
    const res = await socialApi.getComments(props.projectId, page.value, pageSize)
    if (res.code === 0 && res.data) {
      if (reset) {
        comments.value = res.data.records || []
      } else {
        comments.value.push(...(res.data.records || []))
      }
      total.value = res.data.total || 0
    }
  } catch { /* 忽略 */ }
  finally { loading.value = false }
}

async function submitComment() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    const res = await socialApi.addComment(props.projectId, newComment.value.trim())
    if (res.code === 0) {
      newComment.value = ''
      ElMessage.success('评论成功')
      loadComments(true)
    }
  } catch { ElMessage.error('评论失败') }
  finally { submitting.value = false }
}

async function deleteComment(commentId: number) {
  try {
    await socialApi.deleteComment(commentId)
    ElMessage.success('已删除')
    loadComments(true)
  } catch { ElMessage.error('删除失败') }
}

function loadMore() {
  page.value++
  loadComments()
}

onMounted(() => loadComments())
</script>

<style scoped>
.comment-section { margin-top: 24px; }
.comment-header h3 { font-size: 16px; margin-bottom: 16px; }
.comment-input { display: flex; gap: 10px; margin-bottom: 20px; align-items: flex-start; }
.comment-input .el-input { flex: 1; }
.login-hint { text-align: center; padding: 16px; color: var(--text2, #6b7280); }
.comment-list { display: flex; flex-direction: column; gap: 16px; }
.comment-item { display: flex; gap: 12px; }
.comment-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: var(--primary, #6366f1); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; flex-shrink: 0;
}
.comment-body { flex: 1; min-width: 0; }
.comment-meta {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; margin-bottom: 4px;
}
.comment-author { font-weight: 600; color: var(--text, #1f2937); }
.comment-time { color: var(--text3, #9ca3af); }
.comment-content { font-size: 14px; line-height: 1.6; color: var(--text, #1f2937); }
.load-more { text-align: center; padding: 12px; }
.empty-state { text-align: center; padding: 24px; color: var(--text2, #6b7280); }
</style>
