<template>
  <div class="admin-competitions-page">
    <h1 class="page-title">
      <span class="title-emoji">🏆</span>
      竞赛管理
    </h1>

    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索竞赛名称..."
          clearable
          style="width: 280px"
          @keyup.enter="loadData"
          @clear="loadData"
        />
        <el-button type="primary" plain @click="loadData" :loading="loading">🔄 刷新</el-button>
      </div>
    </div>

    <div class="page-card">
      <el-table :data="competitions" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="竞赛名称" min-width="200" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 'TIMED' ? 'primary' : 'warning'" size="small">
              {{ row.type === 'TIMED' ? '限时' : '评级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="compStatusType(row.status)" size="small" effect="dark">
              {{ compStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题目数" width="80" prop="problemCount" />
        <el-table-column label="参与人数" width="90" prop="participantCount" />
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="goToComp(row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="page"
          :page-size="20"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminCompetitionsView' })

import { ref, onMounted } from 'vue'
import { competitionApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const competitions = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')

function compStatusLabel(s: string) {
  return ({ DRAFT: '草稿', PUBLISHED: '已发布', RUNNING: '进行中', ENDED: '已结束' } as Record<string, string>)[s] || s
}
function compStatusType(s: string) {
  return ({ DRAFT: 'info', PUBLISHED: 'primary', RUNNING: 'success', ENDED: 'danger' } as Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'>)[s] || 'info'
}
function formatDateTime(dt: string) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN')
}

async function loadData() {
  loading.value = true
  try {
    const res = await competitionApi.list(page.value, 20)
    if (res.code === 0) {
      competitions.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function goToComp(id: number) {
  window.open(`/competition/${id}`, '_blank')
}

onMounted(loadData)
</script>

<style scoped>
.admin-competitions-page { max-width: 1200px; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
