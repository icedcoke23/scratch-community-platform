<template>
  <div class="admin-classes-page">
    <h1 class="page-title">
      <span class="title-emoji">📚</span>
      班级管理
    </h1>

    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索班级名称..."
          clearable
          style="width: 280px"
          @keyup.enter="loadData"
          @clear="loadData"
        />
        <el-button type="primary" plain @click="loadData" :loading="loading">🔄 刷新</el-button>
      </div>
    </div>

    <div class="page-card">
      <el-table :data="classes" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="班级名称" min-width="180" />
        <el-table-column label="教师" width="120">
          <template #default="{ row }">{{ row.teacherName || `ID:${row.teacherId}` }}</template>
        </el-table-column>
        <el-table-column prop="grade" label="年级" width="100" />
        <el-table-column prop="studentCount" label="学生数" width="90" />
        <el-table-column label="邀请码" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.inviteCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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
defineOptions({ name: 'AdminClassesView' })

import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { get } from '@/api/request'

const loading = ref(false)
const classes = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')

function formatDateTime(dt: string) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN')
}

async function loadData() {
  loading.value = true
  try {
    const res = await get<{ records: any[]; total: number }>('/class', { page: page.value, size: 20 })
    if (res.code === 0) {
      classes.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.admin-classes-page { max-width: 1200px; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
