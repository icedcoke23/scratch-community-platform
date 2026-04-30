<template>
  <div class="admin-users-page">
    <h1 class="page-title">
      <span class="title-emoji">👥</span>
      用户管理
    </h1>

    <!-- 搜索与筛选 -->
    <div class="page-card filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名或昵称..."
            clearable
            prefix-icon="Search"
            style="width: 260px"
            @keyup.enter="loadUsers(1)"
            @clear="loadUsers(1)"
          />
          <el-select v-model="roleFilter" placeholder="角色筛选" clearable style="width: 140px" @change="loadUsers(1)">
            <el-option label="全部角色" value="" />
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="loadUsers(1)">
            <el-option label="全部状态" value="" />
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button type="primary" plain @click="loadUsers(1)" :loading="loading">
            🔄 刷新
          </el-button>
          <el-button type="success" plain @click="exportUsers">
            📥 导出
          </el-button>
        </div>
      </div>

      <!-- 统计条 -->
      <div class="stats-bar">
        <span class="stat-item">共 <strong>{{ total }}</strong> 个用户</span>
        <span class="stat-item">正常 <strong class="text-green">{{ activeCount }}</strong></span>
        <span class="stat-item">禁用 <strong class="text-red">{{ disabledCount }}</strong></span>
      </div>
    </div>

    <!-- 用户表格 -->
    <div class="page-card">
      <el-table
        :data="users"
        stripe
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ fontWeight: 700 }"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="70" sortable />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar-sm" :style="{ background: avatarColor(row.role) }">
                {{ (row.nickname || row.username)[0].toUpperCase() }}
              </div>
              <div class="user-info">
                <div class="user-name-text">{{ row.nickname || row.username }}</div>
                <div class="user-username">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small" effect="dark">
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="积分/等级" width="110">
          <template #default="{ row }">
            <div class="points-cell">
              <span>⭐ {{ row.points || 0 }}</span>
              <span class="level-badge">Lv.{{ row.level || 1 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170" sortable prop="createdAt">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" size="small" text @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button
                v-if="row.status === 1"
                type="warning"
                size="small"
                text
                @click="handleDisable(row)"
                :disabled="row.id === currentUserId"
              >
                禁用
              </el-button>
              <el-button v-else type="success" size="small" text @click="handleEnable(row)">
                启用
              </el-button>
              <el-button type="info" size="small" text @click="viewUser(row)">
                详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 批量操作 -->
      <div v-if="selectedUsers.length > 0" class="batch-bar">
        <span>已选 {{ selectedUsers.length }} 项</span>
        <el-button type="warning" size="small" @click="batchDisable">批量禁用</el-button>
        <el-button type="success" size="small" @click="batchEnable">批量启用</el-button>
        <el-button size="small" @click="clearSelection">取消选择</el-button>
      </div>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadUsers"
          @size-change="loadUsers(1)"
        />
      </div>
    </div>

    <!-- 编辑用户弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="480px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="80px" label-position="left">
        <el-form-item label="用户名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="用户昵称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="用户详情" width="520px">
      <div v-if="detailUser" class="user-detail">
        <div class="detail-header">
          <div class="detail-avatar" :style="{ background: avatarColor(detailUser.role) }">
            {{ (detailUser.nickname || detailUser.username)[0].toUpperCase() }}
          </div>
          <div class="detail-info">
            <h3>{{ detailUser.nickname || detailUser.username }}</h3>
            <p>@{{ detailUser.username }}</p>
            <el-tag :type="roleTagType(detailUser.role)" size="small" effect="dark">
              {{ roleLabel(detailUser.role) }}
            </el-tag>
          </div>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="ID">{{ detailUser.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailUser.status === 1 ? 'success' : 'danger'" size="small">
              {{ detailUser.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="积分">⭐ {{ detailUser.points || 0 }}</el-descriptions-item>
          <el-descriptions-item label="等级">Lv.{{ detailUser.level || 1 }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detailUser.email || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">{{ detailUser.bio || '这个人很懒，什么都没写...' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间" :span="2">{{ formatDate(detailUser.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminUsersView' })

import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api'
import type { User } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getErrorMessage } from '@/utils/error'
import { formatDate } from '@/utils'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.user?.id)

const loading = ref(false)
const saving = ref(false)
const users = ref<User[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const roleFilter = ref('')
const statusFilter = ref<number | string>('')
const selectedUsers = ref<User[]>([])

// 编辑弹窗
const editDialogVisible = ref(false)
const editForm = ref({ id: 0, username: '', nickname: '', role: 'STUDENT', status: 1 })

// 详情弹窗
const detailDialogVisible = ref(false)
const detailUser = ref<User | null>(null)

// 统计
const activeCount = computed(() => users.value.filter(u => u.status === 1).length)
const disabledCount = computed(() => users.value.filter(u => u.status !== 1).length)

function avatarColor(role: string) {
  return { ADMIN: '#EF4444', TEACHER: '#F59E0B' }[role] || '#3B82F6'
}

function roleLabel(role: string) {
  return { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[role] || '学生'
}

function roleTagType(role: string) {
  return ({ ADMIN: 'danger', TEACHER: 'warning' }[role] || 'info') as 'danger' | 'warning' | 'info'
}

async function loadUsers(page?: number) {
  if (page) currentPage.value = page
  loading.value = true
  try {
    const res = await adminApi.listUsers(currentPage.value, pageSize.value)
    if (res.code === 0) {
      users.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e))
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(val: User[]) {
  selectedUsers.value = val
}

function clearSelection() {
  selectedUsers.value = []
}

function openEditDialog(user: User) {
  editForm.value = {
    id: user.id,
    username: user.username,
    nickname: user.nickname,
    role: user.role,
    status: user.status
  }
  editDialogVisible.value = true
}

async function saveUser() {
  saving.value = true
  try {
    const res = await adminApi.updateUser(editForm.value.id, {
      role: editForm.value.role,
      status: String(editForm.value.status)
    })
    if (res.code === 0) {
      ElMessage.success('保存成功')
      editDialogVisible.value = false
      loadUsers()
    } else {
      ElMessage.error(res.msg)
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e))
  } finally {
    saving.value = false
  }
}

async function handleDisable(user: User) {
  try {
    await ElMessageBox.confirm(`确定要禁用用户 "${user.nickname || user.username}" 吗？`, '禁用用户', {
      type: 'warning',
      confirmButtonText: '确定禁用',
      cancelButtonText: '取消'
    })
    const res = await adminApi.disableUser(user.id)
    if (res.code === 0) {
      ElMessage.success('已禁用')
      loadUsers()
    } else {
      ElMessage.error(res.msg)
    }
  } catch { /* 取消 */ }
}

async function handleEnable(user: User) {
  const res = await adminApi.enableUser(user.id)
  if (res.code === 0) {
    ElMessage.success('已启用')
    loadUsers()
  } else {
    ElMessage.error(res.msg)
  }
}

async function batchDisable() {
  try {
    await ElMessageBox.confirm(`确定要禁用选中的 ${selectedUsers.value.length} 个用户吗？`, '批量禁用', { type: 'warning' })
    for (const user of selectedUsers.value) {
      if (user.id !== currentUserId.value) {
        await adminApi.disableUser(user.id)
      }
    }
    ElMessage.success('批量禁用完成')
    selectedUsers.value = []
    loadUsers()
  } catch { /* 取消 */ }
}

async function batchEnable() {
  for (const user of selectedUsers.value) {
    await adminApi.enableUser(user.id)
  }
  ElMessage.success('批量启用完成')
  selectedUsers.value = []
  loadUsers()
}

function viewUser(user: User) {
  detailUser.value = user
  detailDialogVisible.value = true
}

function exportUsers() {
  const headers = ['ID', '用户名', '昵称', '角色', '积分', '等级', '状态', '注册时间']
  const rows = users.value.map(u => [
    u.id, u.username, u.nickname, roleLabel(u.role),
    u.points || 0, u.level || 1,
    u.status === 1 ? '正常' : '禁用',
    formatDate(u.createdAt)
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `users_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

onMounted(() => loadUsers())
</script>

<style scoped>
.admin-users-page {
  max-width: 1200px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-right {
  display: flex;
  gap: 8px;
}

.stats-bar {
  display: flex;
  gap: 20px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border, #e5e7eb);
  font-size: 13px;
  color: var(--text2, #64748b);
}

.stat-item strong {
  font-weight: 700;
}

.text-green { color: #22C55E; }
.text-red { color: #EF4444; }

/* User Cell */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar-sm {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-info {
  min-width: 0;
}

.user-name-text {
  font-weight: 600;
  font-size: 14px;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-username {
  font-size: 12px;
  color: var(--text3, #94a3b8);
}

.points-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;
}

.level-badge {
  font-size: 11px;
  color: var(--text3, #94a3b8);
}

.time-text {
  font-size: 13px;
  color: var(--text2, #64748b);
}

.action-btns {
  display: flex;
  gap: 4px;
}

/* Batch Bar */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-top: 12px;
  background: var(--primary-bg, #eff6ff);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary, #3b82f6);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* Detail Dialog */
.user-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-header {
  display: flex;
  gap: 16px;
  align-items: center;
}

.detail-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  font-weight: 800;
  flex-shrink: 0;
}

.detail-info h3 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
}

.detail-info p {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--text2, #64748b);
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .filter-left {
    flex-direction: column;
  }
  .filter-left .el-input,
  .filter-left .el-select {
    width: 100% !important;
  }
}
</style>
