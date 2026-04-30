<template>
  <div class="admin-comments-page">
    <h1 class="page-title">
      <span class="title-emoji">💬</span>
      评论管理
    </h1>

    <!-- 搜索 -->
    <div class="page-card filter-card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索评论内容..."
          clearable
          prefix-icon="Search"
          style="width: 320px"
          @keyup.enter="loadComments(1)"
          @clear="loadComments(1)"
        />
        <div class="filter-right">
          <el-button type="primary" plain @click="loadComments(1)" :loading="loading">🔄 刷新</el-button>
        </div>
      </div>
      <div class="stats-bar">
        共 <strong>{{ total }}</strong> 条评论
      </div>
    </div>

    <!-- 评论列表 -->
    <div class="page-card">
      <div v-if="loading && comments.length === 0" class="empty-state">加载中...</div>
      <div v-else-if="comments.length === 0" class="empty-state">
        <div class="empty-icon">💬</div>
        <p>暂无评论</p>
      </div>

      <div v-else class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <div class="comment-user">
              <span class="user-avatar-sm">{{ (comment.nickname || comment.username || '?')[0].toUpperCase() }}</span>
              <div>
                <span class="user-name">{{ comment.nickname || comment.username }}</span>
                <span class="comment-time">{{ formatDateTime(comment.created_at) }}</span>
              </div>
            </div>
            <div class="comment-actions">
              <el-button type="primary" size="small" text @click="goToProject(comment.project_id)">
                查看作品
              </el-button>
              <el-button type="danger" size="small" text @click="handleDelete(comment)">
                删除
              </el-button>
            </div>
          </div>
          <div class="comment-body">{{ comment.content }}</div>
          <div class="comment-meta">
            <span>作品: {{ comment.project_title || `#${comment.project_id}` }}</span>
            <span>ID: {{ comment.id }}</span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadComments"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminCommentsView' })

import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getErrorMessage } from '@/utils/error'

interface CommentRecord {
  id: number
  user_id: number
  username: string
  nickname: string
  project_id: number
  project_title: string
  content: string
  created_at: string
}

const loading = ref(false)
const comments = ref<CommentRecord[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')

function formatDateTime(dt: string) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function loadComments(page?: number) {
  if (page) currentPage.value = page
  loading.value = true
  try {
    const res = await adminApi.listComments({
      keyword: keyword.value || undefined,
      page: currentPage.value,
      size: pageSize.value
    })
    if (res.code === 0 && res.data) {
      const data = res.data as { total: number; records: CommentRecord[] }
      comments.value = data.records || []
      total.value = data.total || 0
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e))
  } finally {
    loading.value = false
  }
}

async function handleDelete(comment: CommentRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要删除这条评论吗？\n"${comment.content.slice(0, 50)}${comment.content.length > 50 ? '...' : ''}"`,
      '删除评论',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    const res = await adminApi.deleteComment(comment.id)
    if (res.code === 0) {
      ElMessage.success('已删除')
      loadComments()
    } else {
      ElMessage.error(res.msg)
    }
  } catch { /* 取消 */ }
}

function goToProject(projectId: number) {
  window.open(`/project/${projectId}`, '_blank')
}

onMounted(() => loadComments())
</script>

<style scoped>
.admin-comments-page { max-width: 1000px; }

.filter-card { margin-bottom: 16px; }

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-right { display: flex; gap: 8px; }

.stats-bar {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--border, #e5e7eb);
  font-size: 13px;
  color: var(--text2, #64748b);
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  padding: 16px;
  background: var(--bg, #f9fafb);
  border-radius: 12px;
  border: 1px solid var(--border, #e5e7eb);
  transition: all 0.2s;
}

.comment-item:hover {
  border-color: var(--primary-light, #bfdbfe);
  box-shadow: var(--shadow);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar-sm {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text);
  display: block;
}

.comment-time {
  font-size: 12px;
  color: var(--text3, #94a3b8);
}

.comment-actions { display: flex; gap: 4px; }

.comment-body {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text, #1a1a2e);
  padding: 10px 14px;
  background: var(--card, #fff);
  border-radius: 8px;
  margin-bottom: 8px;
  word-break: break-word;
}

.comment-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text3, #94a3b8);
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--text2, #64748b);
}

.empty-icon { font-size: 48px; margin-bottom: 8px; }

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
