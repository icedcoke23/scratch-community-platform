<template>
  <div class="audit-page">
    <h1 class="page-title">
      <span class="title-emoji">🔍</span>
      内容审核
    </h1>

    <!-- 审核统计 -->
    <div class="stat-grid">
      <div class="stat-card" style="--accent: #F59E0B">
        <div class="stat-icon">⏳</div>
        <div class="stat-content">
          <div class="stat-value">{{ pendingCount }}</div>
          <div class="stat-label">待审核</div>
        </div>
      </div>
      <div class="stat-card" style="--accent: #10B981">
        <div class="stat-icon">✅</div>
        <div class="stat-content">
          <div class="stat-value">{{ approvedCount }}</div>
          <div class="stat-label">已通过</div>
        </div>
      </div>
      <div class="stat-card" style="--accent: #EF4444">
        <div class="stat-icon">❌</div>
        <div class="stat-content">
          <div class="stat-value">{{ rejectedCount }}</div>
          <div class="stat-label">已拒绝</div>
        </div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="page-card">
      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" @change="loadAudits">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="PENDING">待审核</el-radio-button>
          <el-radio-button label="APPROVED">已通过</el-radio-button>
          <el-radio-button label="REJECTED">已拒绝</el-radio-button>
        </el-radio-group>
        <el-button type="primary" plain @click="loadAudits" :loading="loading">刷新</el-button>
      </div>
    </div>

    <!-- 审核列表 -->
    <div class="page-card">
      <div v-if="loading" class="empty-state">加载中...</div>
      <div v-else-if="audits.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <div>暂无审核记录</div>
      </div>
      <div v-else class="audit-list">
        <div v-for="audit in audits" :key="audit.id" class="audit-item" :class="'status-' + (audit.status || 'pending').toLowerCase()">
          <div class="audit-header">
            <div class="audit-type">
              <el-tag :type="getTypeTag(audit.contentType)" size="small">{{ getTypeName(audit.contentType) }}</el-tag>
              <span class="audit-id">#{{ audit.id }}</span>
            </div>
            <el-tag :type="getStatusTag(audit.status)" size="small">{{ getStatusName(audit.status) }}</el-tag>
          </div>
          <div class="audit-content">
            <div class="content-preview" v-if="audit.content">
              {{ audit.content.substring(0, 200) }}{{ audit.content.length > 200 ? '...' : '' }}
            </div>
            <div class="content-meta">
              <span v-if="audit.userId">用户ID: {{ audit.userId }}</span>
              <span v-if="audit.createdAt">{{ new Date(audit.createdAt).toLocaleString('zh-CN') }}</span>
            </div>
            <div class="sensitive-words" v-if="audit.sensitiveWords">
              <span class="sw-label">敏感词：</span>
              <el-tag v-for="word in audit.sensitiveWords.split(',')" :key="word" size="small" type="danger" class="sw-tag">{{ word }}</el-tag>
            </div>
          </div>
          <div class="audit-actions" v-if="audit.status === 'PENDING' || audit.status === 'pending'">
            <el-button type="success" size="small" @click="handleAudit(audit.id, 'APPROVED')" :loading="auditingId === audit.id">✅ 通过</el-button>
            <el-button type="danger" size="small" @click="handleAudit(audit.id, 'REJECTED')" :loading="auditingId === audit.id">❌ 拒绝</el-button>
          </div>
        </div>
      </div>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadAudits" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AuditView' })
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

interface AuditItem {
  id: number
  contentType?: string
  content?: string
  userId?: number
  status: string
  sensitiveWords?: string
  createdAt?: string
}

const userStore = useUserStore()
const loading = ref(false)
const auditingId = ref<number | null>(null)
const audits = ref<AuditItem[]>([])
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const pendingCount = computed(() => audits.value.filter(a => a.status === 'PENDING' || a.status === 'pending').length)
const approvedCount = computed(() => audits.value.filter(a => a.status === 'APPROVED' || a.status === 'approved').length)
const rejectedCount = computed(() => audits.value.filter(a => a.status === 'REJECTED' || a.status === 'rejected').length)

function getTypeTag(type?: string) {
  const map: Record<string, string> = { comment: 'info', project: 'warning', user: 'danger' }
  return (map[type || ''] || 'info') as 'info' | 'warning' | 'danger' | 'success'
}

function getTypeName(type?: string) {
  const map: Record<string, string> = { comment: '评论', project: '项目', user: '用户资料' }
  return map[type || ''] || type || '未知'
}

function getStatusTag(status: string) {
  const s = status.toUpperCase()
  if (s === 'PENDING') return 'warning' as const
  if (s === 'APPROVED') return 'success' as const
  return 'danger' as const
}

function getStatusName(status: string) {
  const s = status.toUpperCase()
  if (s === 'PENDING') return '待审核'
  if (s === 'APPROVED') return '已通过'
  return '已拒绝'
}

async function loadAudits() {
  loading.value = true
  try {
    const token = userStore.token
    const params = new URLSearchParams({
      page: String(currentPage.value),
      size: String(pageSize)
    })
    if (statusFilter.value) params.set('status', statusFilter.value)

    const res = await fetch(`/api/v1/admin/audit?${params}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await res.json()
    if (data.code === 0) {
      audits.value = data.data?.records || []
      total.value = data.data?.total || 0
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleAudit(id: number, status: string) {
  auditingId.value = id
  try {
    const token = userStore.token
    const res = await fetch(`/api/v1/admin/audit/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ status })
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success(status === 'APPROVED' ? '已通过' : '已拒绝')
      loadAudits()
    } else {
      ElMessage.error(data.msg || '操作失败')
    }
  } catch { ElMessage.error('操作失败') }
  finally { auditingId.value = null }
}

onMounted(loadAudits)
</script>

<style scoped>
.audit-page {
  max-width: 1024px;
  margin: 0 auto;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--card, #fff);
  border-radius: 16px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  border-left: 4px solid var(--accent);
}

.stat-icon { font-size: 32px; }
.stat-value { font-size: 24px; font-weight: 800; }
.stat-label { font-size: 13px; color: var(--text2, #64748b); }

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.audit-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.audit-item {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border, #e5e7eb);
  transition: all 0.2s ease;
}

.audit-item:hover {
  box-shadow: var(--shadow);
}

.audit-item.status-pending {
  border-left: 4px solid #F59E0B;
}

.audit-item.status-approved {
  border-left: 4px solid #10B981;
}

.audit-item.status-rejected {
  border-left: 4px solid #EF4444;
}

.audit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.audit-type {
  display: flex;
  align-items: center;
  gap: 8px;
}

.audit-id {
  font-size: 12px;
  color: var(--text2, #64748b);
}

.content-preview {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text, #1a1a2e);
  margin-bottom: 8px;
  padding: 10px;
  background: var(--bg, #f5f5f5);
  border-radius: 8px;
}

.content-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text2, #64748b);
  margin-bottom: 8px;
}

.sensitive-words {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.sw-label {
  font-size: 12px;
  color: var(--text2, #64748b);
}

.sw-tag {
  font-size: 11px;
}

.audit-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.section-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 16px;
}

.pagination-wrap {
  margin-top: 20px;
  text-align: center;
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
