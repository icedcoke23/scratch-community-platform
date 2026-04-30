<template>
  <div class="dashboard-page">
    <h1 class="page-title">⚙️ {{ t('admin.title') }}</h1>
    <div v-if="loading" class="empty-state">加载中...</div>
    <template v-else-if="dashboard">
      <!-- 核心指标卡片 -->
      <div class="stat-grid">
        <div v-for="stat in statCards" :key="stat.label" class="stat-card" :style="{ '--accent': stat.color }">
          <div class="stat-icon">{{ stat.icon }}</div>
          <div class="stat-content">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
          <div v-if="stat.sub !== undefined" class="stat-sub">
            <span :class="stat.subType === 'good' ? 'text-green' : stat.subType === 'warn' ? 'text-orange' : 'text-gray'">
              {{ stat.sub }}
            </span>
          </div>
        </div>
      </div>

      <!-- 关键比率 -->
      <div class="page-card">
        <div class="section-title">📊 关键指标</div>
        <div class="ratio-grid">
          <div class="ratio-item">
            <div class="ratio-header">
              <span>项目发布率</span>
              <span class="ratio-value">{{ publishRate }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: publishRate + '%', background: '#4F46E5' }" />
            </div>
          </div>
          <div class="ratio-item">
            <div class="ratio-header">
              <span>判题通过率</span>
              <span class="ratio-value">{{ acRate }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: acRate + '%', background: '#10B981' }" />
            </div>
          </div>
          <div class="ratio-item">
            <div class="ratio-header">
              <span>今日用户增长</span>
              <span class="ratio-value">{{ growthRate }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: Math.min(Number(growthRate) * 5, 100) + '%', background: '#F59E0B' }" />
            </div>
          </div>
        </div>
      </div>

      <!-- 快速导航 -->
      <div class="page-card">
        <div class="section-title">🚀 快速导航</div>
        <div class="quick-nav">
          <router-link to="/feed" class="nav-item">
            <span class="nav-icon">🏠</span>
            <span>社区首页</span>
          </router-link>
          <router-link to="/rank" class="nav-item">
            <span class="nav-icon">🏆</span>
            <span>排行榜</span>
          </router-link>
          <router-link to="/admin/problems" class="nav-item">
            <span class="nav-icon">🧩</span>
            <span>题目管理</span>
          </router-link>
          <router-link to="/class" class="nav-item">
            <span class="nav-icon">📚</span>
            <span>班级管理</span>
          </router-link>
          <router-link to="/competition" class="nav-item">
            <span class="nav-icon">🎯</span>
            <span>竞赛管理</span>
          </router-link>
          <router-link to="/editor" class="nav-item">
            <span class="nav-icon">✏️</span>
            <span>新建项目</span>
          </router-link>
          <router-link to="/admin/audit" class="nav-item">
            <span class="nav-icon">🔍</span>
            <span>内容审核</span>
          </router-link>
          <router-link to="/admin/config" class="nav-item">
            <span class="nav-icon">⚙️</span>
            <span>系统配置</span>
          </router-link>
          <router-link to="/homework/create" class="nav-item">
            <span class="nav-icon">📝</span>
            <span>布置作业</span>
          </router-link>
          <router-link to="/analytics" class="nav-item">
            <span class="nav-icon">📊</span>
            <span>学情分析</span>
          </router-link>
          <router-link to="/notifications" class="nav-item">
            <span class="nav-icon">🔔</span>
            <span>通知中心</span>
          </router-link>
          <router-link to="/settings" class="nav-item">
            <span class="nav-icon">👤</span>
            <span>个人设置</span>
          </router-link>
        </div>
      </div>

      <!-- 数据可视化 -->
      <div class="page-card">
        <div class="section-title">📈 数据概览</div>
        <div class="chart-grid">
          <MiniChart
            type="bar"
            :data="submissionChartData"
            title="提交分布"
            color="#4F46E5"
          />
          <MiniChart
            type="donut"
            :data="userRoleData"
            title="用户角色"
            color="#4F46E5"
            bg-color="#E5E7EB"
          />
          <MiniChart
            type="line"
            :data="projectTrendData"
            title="项目趋势"
            color="#10B981"
          />
        </div>
      </div>

      <!-- 活动时间线 -->
      <div class="page-card">
        <ActivityTimeline
          title="📋 最近活动"
          :items="recentActivities"
          :show-refresh="true"
          @refresh="loadDashboard"
        />
      </div>

      <!-- 用户管理 -->
      <div class="page-card">
        <div class="section-title">👥 {{ t('admin.userManage') }}</div>
        <el-table :data="users" stripe size="small">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column label="角色" width="80">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'TEACHER' ? 'warning' : 'info'" size="small">
                {{ row.role === 'ADMIN' ? '管理员' : row.role === 'TEACHER' ? '教师' : '学生' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" type="danger" size="small" @click="disableUser(row.id)">禁用</el-button>
              <el-button v-else type="success" size="small" @click="enableUser(row.id)">启用</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="userTotal > 20" style="margin-top: 12px; text-align: center">
          <el-pagination v-model:current-page="userPage" :page-size="20" :total="userTotal" layout="prev, pager, next" @current-change="loadUsers" />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminView' })
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api'
import type { DashboardData, User } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getErrorMessage } from '@/utils/error'
import { useI18n } from '@/composables/useI18n'
import MiniChart from '@/components/MiniChart.vue'
import ActivityTimeline from '@/components/ActivityTimeline.vue'
import type { ActivityItem } from '@/components/ActivityTimeline.vue'

const { t } = useI18n()

const loading = ref(true)
const dashboard = ref<DashboardData | null>(null)
const users = ref<User[]>([])
const userPage = ref(1)
const userTotal = ref(0)

const publishRate = computed(() => {
  if (!dashboard.value || dashboard.value.totalProjects === 0) return '0'
  return ((dashboard.value.publishedProjects / dashboard.value.totalProjects) * 100).toFixed(1)
})

const acRate = computed(() => {
  if (!dashboard.value || dashboard.value.totalSubmissions === 0) return '0'
  return ((dashboard.value.acSubmissions / dashboard.value.totalSubmissions) * 100).toFixed(1)
})

const growthRate = computed(() => {
  if (!dashboard.value || dashboard.value.totalUsers === 0) return '0'
  return ((dashboard.value.todayNewUsers / dashboard.value.totalUsers) * 100).toFixed(2)
})

const statCards = computed(() => {
  if (!dashboard.value) return []
  const d = dashboard.value
  return [
    { icon: '👥', label: t('admin.totalUsers'), value: d.totalUsers, color: '#4F46E5', sub: `${t('admin.todayNew')} +${d.todayNewUsers}`, subType: 'good' },
    { icon: '🎮', label: t('admin.totalProjects'), value: d.totalProjects, color: '#7C3AED', sub: `${t('project.published')} ${d.publishedProjects}`, subType: 'good' },
    { icon: '🧩', label: t('admin.totalProblems'), value: d.totalProblems, color: '#2563EB' },
    { icon: '📝', label: t('admin.totalSubmissions'), value: d.totalSubmissions, color: '#0891B2', sub: `AC ${d.acSubmissions}`, subType: 'good' },
    { icon: '📚', label: '班级', value: d.totalClasses, color: '#059669' },
    { icon: '📋', label: '作业', value: d.totalHomework, color: '#D97706' },
    { icon: '⏳', label: '待审核', value: d.pendingAudits, color: d.pendingAudits > 0 ? '#DC2626' : '#6B7280', sub: d.pendingAudits > 0 ? '需要处理' : '无待处理', subType: d.pendingAudits > 0 ? 'warn' : 'good' },
  ]
})

// 图表数据
const submissionChartData = computed(() => {
  if (!dashboard.value) return []
  const d = dashboard.value
  return [
    { label: '总提交', value: d.totalSubmissions },
    { label: 'AC', value: d.acSubmissions },
    { label: '总项目', value: d.totalProjects },
    { label: '已发布', value: d.publishedProjects },
    { label: '总题目', value: d.totalProblems },
  ]
})

const userRoleData = computed(() => {
  if (!dashboard.value) return []
  // 使用用户列表统计角色分布
  const roles: Record<string, number> = {}
  users.value.forEach(u => {
    roles[u.role] = (roles[u.role] || 0) + 1
  })
  return Object.entries(roles).map(([label, value]) => ({ label, value }))
})

const projectTrendData = computed(() => {
  if (!dashboard.value) return []
  // 模拟趋势数据（实际应从后端获取）
  const d = dashboard.value
  return [
    { label: '项目', value: d.totalProjects },
    { label: '已发布', value: d.publishedProjects },
    { label: '草稿', value: d.totalProjects - d.publishedProjects },
  ]
})

const recentActivities = computed<ActivityItem[]>(() => {
  if (!dashboard.value) return []
  const d = dashboard.value
  const items: ActivityItem[] = []
  if (d.todayNewUsers > 0) {
    items.push({ text: `${d.todayNewUsers} 个新用户注册`, time: '今日', icon: '👤', color: '#4F46E5' })
  }
  if (d.totalProjects > 0) {
    items.push({ text: `平台共有 ${d.totalProjects} 个项目`, time: '总计', icon: '🎮', color: '#7C3AED' })
  }
  if (d.acSubmissions > 0) {
    items.push({ text: `${d.acSubmissions} 次判题通过`, time: '总计', icon: '✅', color: '#10B981' })
  }
  if (d.pendingAudits > 0) {
    items.push({ text: `${d.pendingAudits} 条内容待审核`, time: '待处理', icon: '⏳', color: '#F59E0B' })
  }
  if (d.totalClasses > 0) {
    items.push({ text: `${d.totalClasses} 个班级活跃中`, time: '总计', icon: '📚', color: '#059669' })
  }
  return items
})

async function loadUsers(page: number) {
  try {
    const res = await adminApi.listUsers(page, 20)
    if (res.code === 0) { users.value = res.data?.records || []; userTotal.value = res.data?.total || 0 }
  } catch { /* 忽略 */ }
}

async function disableUser(id: number) {
  try {
    await ElMessageBox.confirm('确定禁用此用户？', '提示', { type: 'warning' })
    const res = await adminApi.disableUser(id)
    if (res.code === 0) { ElMessage.success('已禁用'); loadUsers(userPage.value) }
    else ElMessage.error(res.msg)
  } catch { /* 取消 */ }
}

async function enableUser(id: number) {
  try {
    const res = await adminApi.enableUser(id)
    if (res.code === 0) { ElMessage.success('已启用'); loadUsers(userPage.value) }
    else ElMessage.error(res.msg)
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
}

async function loadDashboard() {
  loading.value = true
  try {
    const res = await adminApi.getDashboard()
    if (res.code === 0) { dashboard.value = res.data || null; await loadUsers(1) }
  } catch { /* 忽略 */ }
  finally { loading.value = false }
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-page {
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
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.stat-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--text, #1a1a2e);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text2, #64748b);
  margin-top: 2px;
  font-weight: 500;
}

.stat-sub {
  font-size: 12px;
  font-weight: 600;
}

.text-green { color: #22C55E; }
.text-orange { color: #F59E0B; }
.text-gray { color: #6B7280; }

.section-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ratio-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.ratio-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ratio-header {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 500;
}

.ratio-value {
  font-weight: 700;
  color: var(--primary, #3B82F6);
}

.progress-bar {
  height: 10px;
  background: var(--bg, #f0f0f0);
  border-radius: 5px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.6s ease;
}

.quick-nav {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--bg, #f5f5f5);
  border-radius: 12px;
  text-decoration: none;
  color: var(--text, #1a1a2e);
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.nav-item:hover {
  background: var(--primary, #3B82F6);
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.nav-icon {
  font-size: 20px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .stat-card {
    padding: 14px;
  }
  .stat-value {
    font-size: 20px;
  }
  .stat-icon {
    font-size: 24px;
  }
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table .cell) { padding: 6px 4px; }
}

@media (max-width: 480px) {
  .stat-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
  .quick-nav {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
