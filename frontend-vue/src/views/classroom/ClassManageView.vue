<template>
  <div class="class-manage-page">
    <h1 class="page-title">
      <span class="title-emoji">🏫</span>
      班级管理
    </h1>

    <!-- 创建班级卡片 -->
    <div class="create-card page-card" v-if="userStore.user?.role !== 'STUDENT'">
      <div class="section-title">➕ 创建新班级</div>
      <el-form :model="createForm" inline class="create-form">
        <el-form-item label="班级名称">
          <el-input v-model="createForm.name" placeholder="如：Scratch初级班" maxlength="50" />
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="createForm.grade" placeholder="选择年级" clearable>
            <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="createForm.description" placeholder="班级简介（可选）" maxlength="200" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleCreate" :loading="creating">创建班级</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 加入班级 -->
    <div class="join-card page-card">
      <div class="section-title">🔗 加入班级</div>
      <el-form inline>
        <el-form-item label="邀请码">
          <el-input v-model="joinCode" placeholder="输入班级邀请码" maxlength="20" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="handleJoin" :loading="joining" :disabled="!joinCode.trim()">加入班级</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 我的班级列表 -->
    <div class="page-card">
      <div class="section-title">📋 我的班级</div>
      <div v-if="loading" class="empty-state">加载中...</div>
      <div v-else-if="classes.length === 0" class="empty-state">
        <div class="empty-icon">📚</div>
        <div>暂无班级</div>
        <div class="empty-hint">{{ userStore.user?.role === 'STUDENT' ? '使用上方邀请码加入班级' : '创建一个新班级开始吧' }}</div>
      </div>
      <div v-else class="class-grid">
        <div v-for="cls in classes" :key="cls.id" class="class-card" @click="openClass(cls)">
          <div class="class-header">
            <div class="class-name">{{ cls.name }}</div>
            <el-tag v-if="cls.grade" size="small" type="info">{{ cls.grade }}</el-tag>
          </div>
          <div class="class-desc" v-if="cls.description">{{ cls.description }}</div>
          <div class="class-meta">
            <span class="meta-item">👩‍🏫 教师ID: {{ cls.teacherId }}</span>
            <span class="meta-item">👨‍🎓 {{ cls.studentCount }} 名学生</span>
          </div>
          <div class="class-code">
            <span class="code-label">邀请码：</span>
            <span class="code-value">{{ cls.inviteCode }}</span>
            <el-button size="small" text type="primary" @click.stop="copyCode(cls.inviteCode)">复制</el-button>
          </div>
          <div class="class-actions">
            <el-button size="small" type="primary" plain @click.stop="viewMembers(cls)">成员管理</el-button>
            <el-button size="small" type="warning" plain @click.stop="viewHomework(cls)" v-if="userStore.user?.role !== 'STUDENT'">布置作业</el-button>
            <el-button size="small" type="danger" plain @click.stop="leaveClass(cls)" v-if="userStore.user?.role === 'STUDENT'">退出班级</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 成员管理弹窗 -->
    <el-dialog v-model="showMembers" title="班级成员" width="600px">
      <div v-if="membersLoading" class="empty-state">加载中...</div>
      <template v-else>
        <el-table :data="members" stripe size="small">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column label="角色" width="80">
            <template #default="{ row }">
              <el-tag :type="row.role === 'TEACHER' ? 'warning' : 'info'" size="small">
                {{ row.role === 'TEACHER' ? '教师' : '学生' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" v-if="isTeacher">
            <template #default="{ row }">
              <el-button v-if="row.role !== 'TEACHER'" type="danger" size="small" text @click="removeMember(row.id)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ClassManageView' })
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { ClassRoom, User } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getErrorMessage } from '@/utils/error'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const creating = ref(false)
const joining = ref(false)
const membersLoading = ref(false)
const classes = ref<ClassRoom[]>([])
const members = ref<User[]>([])
const showMembers = ref(false)
const currentClass = ref<ClassRoom | null>(null)
const joinCode = ref('')

const grades = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级', '初一', '初二', '初三', '高中']

const createForm = ref({
  name: '',
  description: '',
  grade: ''
})

const isTeacher = computed(() => {
  return userStore.user?.role === 'TEACHER' || userStore.user?.role === 'ADMIN'
})

async function loadClasses() {
  loading.value = true
  try {
    const res = await userApi.getMyClasses()
    if (res.code === 0) {
      classes.value = res.data || []
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleCreate() {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入班级名称')
    return
  }
  creating.value = true
  try {
    const res = await fetch('/api/v1/class', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify(createForm.value)
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('班级创建成功！')
      createForm.value = { name: '', description: '', grade: '' }
      loadClasses()
    } else {
      ElMessage.error(data.msg || '创建失败')
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e))
  } finally { creating.value = false }
}

async function handleJoin() {
  if (!joinCode.value.trim()) return
  joining.value = true
  try {
    const res = await fetch(`/api/v1/class/join?code=${encodeURIComponent(joinCode.value)}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('成功加入班级！')
      joinCode.value = ''
      loadClasses()
    } else {
      ElMessage.error(data.msg || '加入失败')
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e))
  } finally { joining.value = false }
}

async function viewMembers(cls: ClassRoom) {
  currentClass.value = cls
  showMembers.value = true
  membersLoading.value = true
  try {
    const res = await fetch(`/api/v1/class/${cls.id}/members`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    const data = await res.json()
    if (data.code === 0) {
      members.value = data.data || []
    }
  } catch { /* ignore */ }
  finally { membersLoading.value = false }
}

async function removeMember(uid: number) {
  if (!currentClass.value) return
  try {
    await ElMessageBox.confirm('确定移除此成员？', '提示', { type: 'warning' })
    const res = await fetch(`/api/v1/class/${currentClass.value.id}/member/${uid}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('已移除')
      viewMembers(currentClass.value)
    } else {
      ElMessage.error(data.msg)
    }
  } catch { /* cancel */ }
}

async function leaveClass(cls: ClassRoom) {
  try {
    await ElMessageBox.confirm(`确定退出「${cls.name}」？`, '提示', { type: 'warning' })
    // Use remove self
    const res = await fetch(`/api/v1/class/${cls.id}/member/${userStore.user?.id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('已退出班级')
      loadClasses()
    } else {
      ElMessage.error(data.msg)
    }
  } catch { /* cancel */ }
}

function viewHomework(cls: ClassRoom) {
  router.push(`/homework?classId=${cls.id}`)
}

function openClass(cls: ClassRoom) {
  viewMembers(cls)
}

function copyCode(code: string) {
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success('邀请码已复制')
  }).catch(() => {
    ElMessage.info(`邀请码：${code}`)
  })
}

onMounted(loadClasses)
</script>

<style scoped>
.class-manage-page {
  max-width: 1024px;
  margin: 0 auto;
}

.create-card {
  margin-bottom: 20px;
}

.create-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.join-card {
  margin-bottom: 20px;
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.class-card {
  background: var(--card, #fff);
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.class-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary, #3B82F6);
}

.class-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.class-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text, #1a1a2e);
}

.class-desc {
  font-size: 13px;
  color: var(--text2, #64748b);
  margin-bottom: 12px;
  line-height: 1.5;
}

.class-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.meta-item {
  font-size: 13px;
  color: var(--text2, #64748b);
}

.class-code {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg, #f5f5f5);
  border-radius: 8px;
  margin-bottom: 12px;
}

.code-label {
  font-size: 13px;
  color: var(--text2, #64748b);
}

.code-value {
  font-size: 16px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  color: var(--primary, #3B82F6);
  letter-spacing: 2px;
}

.class-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-hint {
  font-size: 13px;
  color: var(--text2, #64748b);
  margin-top: 8px;
}

.section-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 768px) {
  .class-grid {
    grid-template-columns: 1fr;
  }
  .create-form {
    flex-direction: column;
  }
}
</style>
