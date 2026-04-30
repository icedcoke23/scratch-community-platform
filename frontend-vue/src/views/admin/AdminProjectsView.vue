<template>
  <div class="admin-projects-page">
    <h1 class="page-title">
      <span class="title-emoji">🎮</span>
      作品管理
    </h1>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div v-for="stat in statCards" :key="stat.label" class="stat-card" :style="{ '--accent': stat.color }">
        <div class="stat-icon">{{ stat.icon }}</div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <div class="page-card filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="keyword"
            placeholder="搜索作品标题、作者..."
            clearable
            prefix-icon="Search"
            style="width: 280px"
            @keyup.enter="loadProjects(1)"
            @clear="loadProjects(1)"
          />
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="loadProjects(1)">
            <el-option label="全部状态" value="" />
            <el-option label="已发布" value="published" />
            <el-option label="草稿" value="draft" />
            <el-option label="审核中" value="reviewing" />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button type="primary" plain @click="loadProjects(1)" :loading="loading">🔄 刷新</el-button>
        </div>
      </div>
    </div>

    <!-- 作品表格 -->
    <div class="page-card">
      <el-table
        :data="projects"
        stripe
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ fontWeight: 700 }"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="作品" min-width="220">
          <template #default="{ row }">
            <div class="project-cell">
              <div class="project-cover" :style="coverStyle(row.cover_url)">
                <span v-if="!row.cover_url" class="cover-placeholder">🎨</span>
              </div>
              <div class="project-info">
                <div class="project-title">{{ row.title }}</div>
                <div class="project-desc">{{ row.description || '暂无描述' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="130">
          <template #default="{ row }">
            <div class="author-cell">
              <span class="author-name">{{ row.nickname || row.username }}</span>
              <span class="author-id">@{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="dark">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数据" width="150">
          <template #default="{ row }">
            <div class="data-cell">
              <span title="点赞">❤️ {{ row.like_count || 0 }}</span>
              <span title="评论">💬 {{ row.comment_count || 0 }}</span>
              <span title="浏览">👁️ {{ row.view_count || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="积木" width="70" prop="block_count" />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDateTime(row.created_at) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" size="small" text @click="previewProject(row)">
                预览
              </el-button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleStatusChange(row, cmd)">
                <el-button type="warning" size="small" text>
                  状态 ▾
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="published" :disabled="row.status === 'published'">发布</el-dropdown-item>
                    <el-dropdown-item command="draft" :disabled="row.status === 'draft'">下线</el-dropdown-item>
                    <el-dropdown-item command="reviewing" :disabled="row.status === 'reviewing'">设为审核中</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button type="danger" size="small" text @click="handleDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadProjects"
          @size-change="loadProjects(1)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminProjectsView' })

import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getErrorMessage } from '@/utils/error'

interface ProjectRecord {
  id: number
  user_id: number
  username: string
  nickname: string
  title: string
  description: string
  cover_url: string
  status: string
  block_count: number
  complexity_score: number
  like_count: number
  comment_count: number
  view_count: number
  tags: string
  created_at: string
}

const loading = ref(false)
const projects = ref<ProjectRecord[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const statusFilter = ref('')

const stats = ref({ total: 0, published: 0, drafts: 0, reviewing: 0 })

const statCards = computed(() => [
  { icon: '🎮', label: '总作品', value: stats.value.total, color: '#4F46E5' },
  { icon: '✅', label: '已发布', value: stats.value.published, color: '#10B981' },
  { icon: '📝', label: '草稿', value: stats.value.drafts, color: '#F59E0B' },
  { icon: '⏳', label: '审核中', value: stats.value.reviewing, color: '#EF4444' },
])

function coverStyle(url: string) {
  return url ? { backgroundImage: `url(${url})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}
}

function statusLabel(status: string) {
  return ({ published: '已发布', draft: '草稿', reviewing: '审核中' } as Record<string, string>)[status] || status
}

function statusTagType(status: string) {
  return ({ published: 'success', draft: 'info', reviewing: 'warning' } as Record<string, string>)[status] || 'info' as const
}

function formatDateTime(dt: string) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function loadStats() {
  try {
    const res = await adminApi.getProjectStats()
    if (res.code === 0 && res.data) {
      stats.value = res.data as typeof stats.value
    }
  } catch { /* ignore */ }
}

async function loadProjects(page?: number) {
  if (page) currentPage.value = page
  loading.value = true
  try {
    const res = await adminApi.listProjects({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: currentPage.value,
      size: pageSize.value
    })
    if (res.code === 0 && res.data) {
      const data = res.data as { total: number; records: ProjectRecord[] }
      projects.value = data.records || []
      total.value = data.total || 0
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e))
  } finally {
    loading.value = false
  }
}

async function handleStatusChange(project: ProjectRecord, status: string) {
  try {
    const res = await adminApi.updateProjectStatus(project.id, status)
    if (res.code === 0) {
      ElMessage.success(`已${status === 'published' ? '发布' : status === 'draft' ? '下线' : '设为审核中'}`)
      loadProjects()
      loadStats()
    } else {
      ElMessage.error(res.msg)
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e))
  }
}

async function handleDelete(project: ProjectRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要删除作品 "${project.title}" 吗？此操作不可撤销。`,
      '删除作品',
      { type: 'error', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    const res = await adminApi.deleteProject(project.id)
    if (res.code === 0) {
      ElMessage.success('已删除')
      loadProjects()
      loadStats()
    } else {
      ElMessage.error(res.msg)
    }
  } catch { /* 取消 */ }
}

function previewProject(project: ProjectRecord) {
  window.open(`/project/${project.id}`, '_blank')
}

onMounted(() => {
  loadProjects()
  loadStats()
})
</script>

<style scoped>
.admin-projects-page {
  max-width: 1200px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: var(--card, #fff);
  border-radius: 14px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 4px solid var(--accent);
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.stat-icon { font-size: 28px; }
.stat-value { font-size: 22px; font-weight: 800; color: var(--text); }
.stat-label { font-size: 13px; color: var(--text2); font-weight: 500; }

.filter-card { margin-bottom: 16px; }

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left { display: flex; gap: 10px; flex-wrap: wrap; }
.filter-right { display: flex; gap: 8px; }

/* Project Cell */
.project-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.project-cover {
  width: 48px;
  height: 36px;
  border-radius: 6px;
  background: var(--bg, #f0f0f0);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.cover-placeholder { font-size: 18px; }

.project-info { min-width: 0; }

.project-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-desc {
  font-size: 12px;
  color: var(--text3, #94a3b8);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-cell { display: flex; flex-direction: column; }
.author-name { font-weight: 600; font-size: 13px; color: var(--text); }
.author-id { font-size: 11px; color: var(--text3, #94a3b8); }

.data-cell {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: var(--text2, #64748b);
}

.time-text { font-size: 13px; color: var(--text2, #64748b); }

.action-btns { display: flex; gap: 4px; }

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .filter-left { flex-direction: column; }
  .filter-left .el-input, .filter-left .el-select { width: 100% !important; }
}
</style>
