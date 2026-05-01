<template>
  <div class="problem-manage-page">
    <h1 class="page-title">
      <span class="title-emoji">🧩</span>
      题目管理
    </h1>

    <!-- 创建题目 -->
    <div class="page-card">
      <div class="section-title">➕ {{ showForm ? '创建题目' : '快速操作' }}</div>
      <el-button v-if="!showForm" type="primary" @click="showForm = true">创建新题目</el-button>
      <template v-else>
        <el-form :model="form" label-width="80px" class="problem-form">
          <el-form-item label="题目标题" required>
            <el-input v-model="form.title" placeholder="如：小猫走路" maxlength="100" />
          </el-form-item>
          <el-form-item label="题目类型" required>
            <el-radio-group v-model="form.type">
              <el-radio label="choice">选择题</el-radio>
              <el-radio label="true_false">判断题</el-radio>
              <el-radio label="scratch_algo">Scratch编程题</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="难度" required>
            <el-radio-group v-model="form.difficulty">
              <el-radio label="easy">简单</el-radio>
              <el-radio label="medium">中等</el-radio>
              <el-radio label="hard">困难</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="分值">
            <el-input-number v-model="form.score" :min="1" :max="100" />
          </el-form-item>
          <el-form-item label="题目描述">
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述题目要求..." />
          </el-form-item>
          <!-- 选择题选项 -->
          <template v-if="form.type === 'choice'">
            <el-form-item label="选项">
              <div v-for="(opt, idx) in form.options" :key="idx" class="option-row">
                <el-input v-model="opt.text" :placeholder="'选项 ' + opt.key" style="flex:1" />
                <el-button v-if="form.options.length > 2" type="danger" text @click="removeOption(idx)">删除</el-button>
              </div>
              <el-button type="primary" text @click="addOption" style="margin-top:8px">+ 添加选项</el-button>
            </el-form-item>
            <el-form-item label="正确答案">
              <el-select v-model="form.answer" placeholder="选择正确答案">
                <el-option v-for="opt in form.options" :key="opt.key" :label="opt.key + '. ' + opt.text" :value="opt.key" />
              </el-select>
            </el-form-item>
          </template>
          <!-- 判断题答案 -->
          <template v-if="form.type === 'true_false'">
            <el-form-item label="正确答案">
              <el-radio-group v-model="form.answer">
                <el-radio label="true">正确 ✓</el-radio>
                <el-radio label="false">错误 ✗</el-radio>
              </el-radio-group>
            </el-form-item>
          </template>
          <!-- Scratch编程题 -->
          <template v-if="form.type === 'scratch_algo'">
            <el-form-item label="参考答案">
              <el-input v-model="form.answer" type="textarea" :rows="3" placeholder="参考答案或评判标准" />
            </el-form-item>
          </template>
          <el-form-item>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">提交创建</el-button>
            <el-button @click="resetForm">重置</el-button>
            <el-button @click="showForm = false">取消</el-button>
          </el-form-item>
        </el-form>
      </template>
    </div>

    <!-- 题目列表 -->
    <div class="page-card">
      <div class="section-title">📋 现有题目</div>
      <div v-if="loading" class="empty-state">加载中...</div>
      <el-table v-else :data="problems" stripe size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'choice' ? 'primary' : row.type === 'true_false' ? 'success' : 'warning'">
              {{ row.type === 'choice' ? '选择题' : row.type === 'true_false' ? '判断题' : '编程题' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.difficulty === 'easy' ? 'success' : row.difficulty === 'medium' ? 'warning' : 'danger'">
              {{ row.difficulty === 'easy' ? '简单' : row.difficulty === 'medium' ? '中等' : '困难' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="70" />
        <el-table-column prop="submitCount" label="提交" width="70" />
        <el-table-column prop="acceptCount" label="通过" width="70" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handlePublish(row)" v-if="row.status !== 'PUBLISHED'">发布</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadProblems" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProblemManageView' })
import { ref, onMounted } from 'vue'
import { problemApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { Problem } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const showForm = ref(false)
const problems = ref<Problem[]>([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const defaultForm = () => ({
  title: '',
  type: 'choice',
  difficulty: 'easy',
  score: 10,
  description: '',
  options: [
    { key: 'A', text: '' },
    { key: 'B', text: '' },
    { key: 'C', text: '' },
    { key: 'D', text: '' }
  ],
  answer: ''
})

const form = ref(defaultForm())

function getHeaders() {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${userStore.token}`
  }
}

async function loadProblems() {
  loading.value = true
  try {
    const res = await problemApi.list(currentPage.value, pageSize)
    if (res.code === 0) {
      problems.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function addOption() {
  const nextKey = String.fromCharCode(65 + form.value.options.length)
  form.value.options.push({ key: nextKey, text: '' })
}

function removeOption(idx: number) {
  form.value.options.splice(idx, 1)
  // Re-key
  form.value.options.forEach((opt, i) => { opt.key = String.fromCharCode(65 + i) })
}

function resetForm() {
  form.value = defaultForm()
}

async function handleSubmit() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入题目标题')
    return
  }
  submitting.value = true
  try {
    const body: Record<string, unknown> = {
      title: form.value.title,
      type: form.value.type,
      difficulty: form.value.difficulty,
      score: form.value.score,
      description: form.value.description
    }
    if (form.value.type === 'choice') {
      body.options = form.value.options
      body.answer = form.value.answer
    } else if (form.value.type === 'true_false') {
      body.answer = form.value.answer
    } else {
      body.answer = form.value.answer
    }

    const res = await fetch('/api/v1/problem', {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(body)
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('题目创建成功！')
      resetForm()
      showForm.value = false
      loadProblems()
    } else {
      ElMessage.error(data.msg || '创建失败')
    }
  } catch { ElMessage.error('创建失败') }
  finally { submitting.value = false }
}

async function handlePublish(problem: Problem) {
  try {
    const res = await fetch(`/api/v1/problem/${problem.id}/publish`, {
      method: 'POST',
      headers: getHeaders()
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('已发布')
      loadProblems()
    } else {
      ElMessage.error(data.msg)
    }
  } catch { ElMessage.error('操作失败') }
}

async function handleDelete(problem: Problem) {
  try {
    await ElMessageBox.confirm(`确定删除题目「${problem.title}」？`, '提示', { type: 'warning' })
    const res = await fetch(`/api/v1/problem/${problem.id}`, {
      method: 'DELETE',
      headers: getHeaders()
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('已删除')
      loadProblems()
    } else {
      ElMessage.error(data.msg)
    }
  } catch { /* cancel */ }
}

onMounted(loadProblems)
</script>

<style scoped>
.problem-manage-page {
  max-width: 1024px;
  margin: 0 auto;
}

.problem-form {
  max-width: 600px;
}

.option-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.pagination-wrap {
  margin-top: 20px;
  text-align: center;
}

.section-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 16px;
}
</style>
