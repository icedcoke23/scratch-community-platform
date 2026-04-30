<template>
  <div class="admin-statistics-page">
    <h1 class="page-title">
      <span class="title-emoji">📈</span>
      数据统计
    </h1>

    <!-- 核心指标 -->
    <div class="stat-grid">
      <div v-for="stat in statCards" :key="stat.label" class="stat-card" :style="{ '--accent': stat.color }">
        <div class="stat-icon">{{ stat.icon }}</div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 用户角色分布 -->
      <div class="page-card chart-card">
        <div class="chart-title">👥 用户角色分布</div>
        <div class="chart-container">
          <div class="donut-chart">
            <div v-for="(item, i) in roleData" :key="item.label" class="donut-segment" :style="donutStyle(i)">
              <div class="donut-label">{{ item.label }}</div>
              <div class="donut-value">{{ item.value }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 项目状态分布 -->
      <div class="page-card chart-card">
        <div class="chart-title">🎮 项目状态分布</div>
        <div class="chart-container">
          <div class="bar-chart">
            <div v-for="(item, i) in projectStatusData" :key="item.label" class="bar-item">
              <div class="bar-label">{{ item.label }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: barWidth(item.value) + '%', background: barColors[i] }"></div>
              </div>
              <div class="bar-value">{{ item.value }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 判题统计 -->
      <div class="page-card chart-card">
        <div class="chart-title">🧩 判题通过率</div>
        <div class="chart-container">
          <div class="bar-chart">
            <div v-for="(item, i) in judgeData" :key="item.label" class="bar-item">
              <div class="bar-label">{{ item.label }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: barWidth(item.value) + '%', background: judgeColors[i] }"></div>
              </div>
              <div class="bar-value">{{ item.value }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 关键比率 -->
      <div class="page-card chart-card">
        <div class="chart-title">📊 关键比率</div>
        <div class="ratio-list">
          <div class="ratio-item">
            <div class="ratio-header">
              <span>项目发布率</span>
              <span class="ratio-value">{{ publishRate }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: publishRate + '%', background: '#4F46E5' }"></div>
            </div>
          </div>
          <div class="ratio-item">
            <div class="ratio-header">
              <span>判题通过率</span>
              <span class="ratio-value">{{ acRate }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: acRate + '%', background: '#10B981' }"></div>
            </div>
          </div>
          <div class="ratio-item">
            <div class="ratio-header">
              <span>今日用户增长</span>
              <span class="ratio-value">{{ growthRate }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: Math.min(Number(growthRate) * 5, 100) + '%', background: '#F59E0B' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminStatisticsView' })

import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api'
import type { DashboardData } from '@/types'

const dashboard = ref<DashboardData | null>(null)

const barColors = ['#4F46E5', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6']
const judgeColors = ['#10B981', '#EF4444', '#F59E0B', '#3B82F6', '#6B7280']

const statCards = computed(() => {
  if (!dashboard.value) return []
  const d = dashboard.value
  return [
    { icon: '👥', label: '总用户', value: d.totalUsers, color: '#4F46E5' },
    { icon: '🎮', label: '总项目', value: d.totalProjects, color: '#7C3AED' },
    { icon: '🧩', label: '总题目', value: d.totalProblems, color: '#2563EB' },
    { icon: '📝', label: '总提交', value: d.totalSubmissions, color: '#0891B2' },
    { icon: '📚', label: '班级', value: d.totalClasses, color: '#059669' },
    { icon: '📋', label: '作业', value: d.totalHomework, color: '#D97706' },
  ]
})

const roleData = computed(() => {
  // 模拟数据，实际应从后端获取
  return [
    { label: '学生', value: Math.max(0, (dashboard.value?.totalUsers || 0) - 5) },
    { label: '教师', value: 3 },
    { label: '管理员', value: 2 },
  ]
})

const projectStatusData = computed(() => {
  if (!dashboard.value) return []
  const d = dashboard.value
  return [
    { label: '已发布', value: d.publishedProjects },
    { label: '草稿', value: d.totalProjects - d.publishedProjects },
  ]
})

const judgeData = computed(() => {
  if (!dashboard.value) return []
  const d = dashboard.value
  return [
    { label: 'AC (通过)', value: d.acSubmissions },
    { label: '其他', value: d.totalSubmissions - d.acSubmissions },
  ]
})

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

function donutStyle(index: number) {
  const colors = ['#3B82F6', '#F59E0B', '#EF4444']
  return { background: colors[index % colors.length] }
}

function barWidth(value: number) {
  if (!dashboard.value) return 0
  const maxVal = Math.max(dashboard.value.totalProjects, dashboard.value.totalSubmissions, dashboard.value.totalUsers, 1)
  return Math.min((value / maxVal) * 100, 100)
}

onMounted(async () => {
  try {
    const res = await adminApi.getDashboard()
    if (res.code === 0) dashboard.value = res.data || null
  } catch { /* ignore */ }
})
</script>

<style scoped>
.admin-statistics-page { max-width: 1200px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
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

.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }
.stat-icon { font-size: 28px; }
.stat-value { font-size: 22px; font-weight: 800; color: var(--text); }
.stat-label { font-size: 13px; color: var(--text2); font-weight: 500; }

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 16px;
}

.chart-card { padding: 20px; }

.chart-title {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 16px;
  color: var(--text);
}

.chart-container { min-height: 160px; }

/* Donut */
.donut-chart {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.donut-segment {
  padding: 16px 20px;
  border-radius: 12px;
  color: #fff;
  min-width: 100px;
  text-align: center;
}

.donut-label { font-size: 13px; opacity: 0.9; margin-bottom: 4px; }
.donut-value { font-size: 24px; font-weight: 800; }

/* Bar */
.bar-chart { display: flex; flex-direction: column; gap: 14px; }

.bar-item { display: flex; align-items: center; gap: 12px; }
.bar-label { width: 80px; font-size: 13px; font-weight: 600; color: var(--text); text-align: right; }
.bar-track { flex: 1; height: 24px; background: var(--bg, #f0f0f0); border-radius: 12px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 12px; transition: width 0.6s ease; min-width: 4px; }
.bar-value { width: 40px; font-size: 14px; font-weight: 700; color: var(--text); }

/* Ratio */
.ratio-list { display: flex; flex-direction: column; gap: 16px; }
.ratio-item { display: flex; flex-direction: column; gap: 8px; }
.ratio-header { display: flex; justify-content: space-between; font-size: 14px; font-weight: 500; }
.ratio-value { font-weight: 700; color: var(--primary, #3B82F6); }
.progress-bar { height: 10px; background: var(--bg, #f0f0f0); border-radius: 5px; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 5px; transition: width 0.6s ease; }

@media (max-width: 768px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .charts-grid { grid-template-columns: 1fr; }
}
</style>
