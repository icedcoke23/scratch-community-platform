<template>
  <div class="homework-create-page">
    <h1 class="page-title">
      <span class="title-emoji">📝</span>
      布置作业
    </h1>

    <div class="page-card">
      <el-form :model="form" label-width="100px" class="homework-form">
        <el-form-item label="选择班级" required>
          <el-select v-model="form.classId" placeholder="选择要布置作业的班级" style="width:100%">
            <el-option v-for="cls in classes" :key="cls.id" :label="cls.name" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业标题" required>
          <el-input v-model="form.title" placeholder="如：第3课-循环结构练习" maxlength="100" />
        </el-form-item>
        <el-form-item label="作业描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述作业要求、评分标准等..." />
        </el-form-item>
        <el-form-item label="总分">
          <el-input-number v-model="form.totalScore" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="form.deadline" type="datetime" placeholder="选择截止时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="关联题目">
          <el-select v-model="form.problemIds" multiple placeholder="选择关联的判题题目（可选）" style="width:100%" clearable>
            <el-option v-for="p in problems" :key="p.id" :label="p.title" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">发布作业</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 已布置的作业 -->
    <div class="page-card" v-if="form.classId">
      <div class="section-title">📋 该班级已有作业</div>
      <div v-if="loadingHomework" class="empty-state">加载中...</div>
      <el-table v-else :data="homeworks" stripe size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : row.status === 'closed' ? 'info' : 'warning'" size="small">
              {{ row.status === 'published' ? '进行中' : row.status === 'closed' ? '已结束' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitCount" label="提交" width="70" />
        <el-table-column prop="gradedCount" label="已批" width="70" />
        <el-table-column label="截止时间" width="160">
          <template #default="{ row }">
            {{ row.deadline ? new Date(row.deadline).toLocaleString('zh-CN') : '无' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="publishHomework(row)" v-if="row.status === 'draft'">发布</el-button>
            <el-button type="warning" size="small" text @click="closeHomework(row)" v-if="row.status === 'published'">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'HomeworkCreateView' })
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { homeworkApi, problemApi, userApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { ClassRoom, Homework, Problem } from '@/types'
import { ElMessage } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()
const submitting = ref(false)
const loadingHomework = ref(false)
const classes = ref<ClassRoom[]>([])
const problems = ref<Problem[]>([])
const homeworks = ref<Homework[]>([])

const form = ref({
  classId: null as number | null,
  title: '',
  description: '',
  totalScore: 100,
  deadline: '',
  problemIds: [] as number[]
})

function getHeaders() {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${userStore.token}`
  }
}

async function loadClasses() {
  try {
    const res = await userApi.getMyClasses()
    if (res.code === 0) {
      classes.value = res.data || []
      // Auto-select from query param
      const qid = route.query.classId
      if (qid) {
        form.value.classId = Number(qid)
      }
    }
  } catch { /* ignore */ }
}

async function loadProblems() {
  try {
    const res = await problemApi.list(1, 100)
    if (res.code === 0) {
      problems.value = res.data?.records || []
    }
  } catch { /* ignore */ }
}

async function loadHomeworks() {
  if (!form.value.classId) return
  loadingHomework.value = true
  try {
    const res = await homeworkApi.listByClass(form.value.classId, 1, 50)
    if (res.code === 0) {
      homeworks.value = res.data?.records || []
    }
  } catch { /* ignore */ }
  finally { loadingHomework.value = false }
}

watch(() => form.value.classId, () => {
  if (form.value.classId) loadHomeworks()
})

async function handleSubmit() {
  if (!form.value.classId) { ElMessage.warning('请选择班级'); return }
  if (!form.value.title.trim()) { ElMessage.warning('请输入作业标题'); return }
  submitting.value = true
  try {
    const body = {
      classId: form.value.classId,
      title: form.value.title,
      description: form.value.description,
      totalScore: form.value.totalScore,
      deadline: form.value.deadline || undefined,
      problemIds: form.value.problemIds.length > 0 ? form.value.problemIds : undefined
    }
    const res = await fetch('/api/v1/homework', {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(body)
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('作业创建成功！')
      resetForm()
      loadHomeworks()
    } else {
      ElMessage.error(data.msg || '创建失败')
    }
  } catch { ElMessage.error('创建失败') }
  finally { submitting.value = false }
}

function resetForm() {
  form.value = {
    classId: form.value.classId, // Keep class selected
    title: '',
    description: '',
    totalScore: 100,
    deadline: '',
    problemIds: []
  }
}

async function publishHomework(hw: Homework) {
  try {
    const res = await fetch(`/api/v1/homework/${hw.id}/publish`, {
      method: 'POST',
      headers: getHeaders()
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('已发布')
      loadHomeworks()
    } else { ElMessage.error(data.msg) }
  } catch { ElMessage.error('操作失败') }
}

async function closeHomework(hw: Homework) {
  try {
    const res = await fetch(`/api/v1/homework/${hw.id}/close`, {
      method: 'POST',
      headers: getHeaders()
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('已关闭')
      loadHomeworks()
    } else { ElMessage.error(data.msg) }
  } catch { ElMessage.error('操作失败') }
}

onMounted(() => {
  loadClasses()
  loadProblems()
})
</script>

<style scoped>
.homework-create-page {
  max-width: 800px;
  margin: 0 auto;
}

.homework-form {
  max-width: 600px;
}

.section-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 16px;
}
</style>
