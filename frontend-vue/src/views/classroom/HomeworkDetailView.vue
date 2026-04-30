<template>
  <div class="homework-detail-page">
    <div class="back-link" @click="router.push('/homework')">← {{ t('common.back') }}</div>

    <LoadingSkeleton v-if="loading" variant="detail" />
    <template v-else-if="homework">
      <!-- 作业头部 -->
      <div class="hero-card page-card">
        <div class="hero-top">
          <div>
            <h1 class="hw-title">{{ homework.title }}</h1>
            <div class="hw-tags">
              <el-tag :type="statusType(homework.status)" size="small" effect="dark">
                {{ statusLabel[homework.status] }}
              </el-tag>
              <span class="hw-score">满分 {{ homework.totalScore }} 分</span>
            </div>
          </div>
          <div class="hero-actions" v-if="isTeacher">
            <el-button v-if="homework.status === 'draft'" type="success" @click="publishHw" :loading="actionLoading">
              📢 发布作业
            </el-button>
            <el-button v-if="homework.status === 'published'" type="warning" @click="closeHw" :loading="actionLoading">
              ⏹ 关闭作业
            </el-button>
          </div>
        </div>

        <p v-if="homework.description" class="hw-desc">{{ homework.description }}</p>

        <!-- 统计信息 -->
        <div class="hw-stats">
          <div class="stat-chip">
            <span class="chip-icon">📝</span>
            <span class="chip-text">{{ homework.submitCount }} 人提交</span>
          </div>
          <div class="stat-chip">
            <span class="chip-icon">✅</span>
            <span class="chip-text">{{ homework.gradedCount }} 人已批改</span>
          </div>
          <div class="stat-chip" v-if="homework.deadline">
            <span class="chip-icon">⏰</span>
            <span class="chip-text" :class="{ 'text-red': isOverdue }">
              截止：{{ new Date(homework.deadline).toLocaleString('zh-CN') }}
            </span>
          </div>
          <div class="stat-chip" v-if="homework.createdAt">
            <span class="chip-icon">📅</span>
            <span class="chip-text">发布于 {{ new Date(homework.createdAt).toLocaleString('zh-CN') }}</span>
          </div>
        </div>

        <!-- 提交进度条 -->
        <div class="progress-section" v-if="isTeacher && homework.submitCount > 0">
          <div class="progress-header">
            <span>提交进度</span>
            <span class="progress-num">{{ homework.submitCount }} 已提交 / {{ homework.gradedCount }} 已批改</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: submitRate + '%' }" />
          </div>
        </div>
      </div>

      <!-- 学生提交区域 -->
      <template v-if="isTeacher">
        <div class="page-card">
          <div class="section-title">📋 提交列表</div>
          <div v-if="submissionsLoading" class="empty-state">加载中...</div>
          <div v-else-if="submissions.length === 0" class="empty-state">
            <div class="empty-icon">📭</div>
            <div>暂无提交</div>
          </div>
          <el-table v-else :data="submissions" stripe size="small">
            <el-table-column label="学生" min-width="120">
              <template #default="{ row }">
                <div class="student-cell">
                  <span class="student-name">{{ row.nickname || row.username || `学生#${row.studentId}` }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'graded' ? 'success' : row.status === 'returned' ? 'warning' : 'info'" size="small">
                  {{ row.status === 'graded' ? '已批改' : row.status === 'returned' ? '已退回' : '待批改' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="分数" width="80">
              <template #default="{ row }">
                <span :class="{ 'score-high': (row.score || 0) >= 80, 'score-mid': (row.score || 0) >= 60 && (row.score || 0) < 80, 'score-low': (row.score || 0) < 60 && row.score !== undefined }">
                  {{ row.score !== undefined && row.score !== null ? row.score : '-' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="160">
              <template #default="{ row }">
                {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button v-if="row.status !== 'graded'" type="primary" size="small" text @click="openGrade(row)">批改</el-button>
                <el-button v-if="row.projectId" type="success" size="small" text @click="previewProject(row.projectId)">预览</el-button>
                <el-button v-if="row.projectId" type="info" size="small" text @click="router.push(`/project/${row.projectId}`)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <!-- 学生视角：提交作业 -->
      <template v-else>
        <div class="page-card" v-if="homework.status === 'published'">
          <div class="section-title">📤 提交作业</div>
          <div class="submit-section">
            <el-form inline>
              <el-form-item label="选择项目">
                <el-select v-model="submitProjectId" placeholder="选择你的 Scratch 项目" style="width: 300px">
                  <el-option v-for="p in myProjects" :key="p.id" :label="p.title" :value="p.id" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitHw" :loading="actionLoading" :disabled="!submitProjectId">提交作业</el-button>
              </el-form-item>
            </el-form>
            <div class="create-hint">
              还没有作品？
              <el-button type="primary" text @click="router.push('/editor')">✏️ 去创建 Scratch 作品</el-button>
            </div>
          </div>
        </div>

        <!-- 我的提交状态 -->
        <div class="page-card" v-if="mySubmission">
          <div class="section-title">📊 我的提交</div>
          <div class="my-submission-card">
            <div class="submission-status">
              <el-tag :type="mySubmission.status === 'graded' ? 'success' : 'info'" size="large" effect="dark">
                {{ mySubmission.status === 'graded' ? '已批改' : '待批改' }}
              </el-tag>
            </div>
            <div v-if="mySubmission.score !== undefined && mySubmission.score !== null" class="submission-score">
              <span class="score-big">{{ mySubmission.score }}</span>
              <span class="score-unit">/ {{ homework.totalScore }}</span>
            </div>
            <div v-if="mySubmission.comment" class="submission-comment">
              <div class="comment-label">教师评语：</div>
              <div class="comment-text">{{ mySubmission.comment }}</div>
            </div>
          </div>
        </div>
      </template>

      <!-- 批改弹窗 -->
      <el-dialog v-model="showGradeDialog" title="批改作业" width="500px">
        <el-form :model="gradeForm" label-width="60px">
          <el-form-item label="分数">
            <el-input-number v-model="gradeForm.score" :min="0" :max="homework.totalScore" />
            <span style="margin-left: 8px; color: var(--text2)">/ {{ homework.totalScore }}</span>
          </el-form-item>
          <el-form-item label="评语">
            <el-input v-model="gradeForm.comment" type="textarea" :rows="3" placeholder="输入评语（可选）" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showGradeDialog = false">取消</el-button>
          <el-button type="primary" @click="doGrade" :loading="actionLoading">确认批改</el-button>
        </template>
      </el-dialog>
    </template>

    <!-- Scratch 预览弹窗 -->
    <ScratchPreviewDialog
      v-model="showPreview"
      :project-id="previewProjectId"
      title="学生作品预览"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'HomeworkDetailView' })
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { homeworkApi, projectApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { Homework, HomeworkSubmission, Project } from '@/types'
import { ElMessage } from 'element-plus'
import { useI18n } from '@/composables/useI18n'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import ScratchPreview from '@/components/ScratchPreview.vue'
import ScratchPreviewDialog from '@/components/ScratchPreviewDialog.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const actionLoading = ref(false)
const submissionsLoading = ref(false)
const homework = ref<Homework | null>(null)
const submissions = ref<HomeworkSubmission[]>([])
const myProjects = ref<Project[]>([])
const submitProjectId = ref<number | null>(null)
const mySubmission = ref<HomeworkSubmission | null>(null)
const showGradeDialog = ref(false)
const gradeForm = ref({ submissionId: 0, score: 0, comment: '' })
const showPreview = ref(false)
const previewProjectId = ref<number>(0)

const isTeacher = computed(() => userStore.user?.role === 'TEACHER' || userStore.user?.role === 'ADMIN')
const hwId = Number(route.params.id)

const statusLabel: Record<string, string> = { draft: '草稿', published: '进行中', closed: '已结束' }
function statusType(s: string) {
  return ({ published: 'success', closed: 'info' } as Record<string, string>)[s] || 'warning'
}

const isOverdue = computed(() => {
  if (!homework.value?.deadline) return false
  return new Date(homework.value.deadline).getTime() < Date.now()
})

const submitRate = computed(() => {
  if (!homework.value || !homework.value.submitCount) return 0
  return Math.round((homework.value.gradedCount / homework.value.submitCount) * 100)
})

async function loadHomework() {
  try {
    const res = await homeworkApi.getDetail(hwId)
    if (res.code === 0) homework.value = res.data || null
    else ElMessage.error(res.msg || '加载失败')
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function loadSubmissions() {
  if (!isTeacher.value) return
  submissionsLoading.value = true
  try {
    const res = await homeworkApi.getSubmissions(hwId)
    if (res.code === 0) submissions.value = res.data?.records || []
  } catch { /* ignore */ }
  finally { submissionsLoading.value = false }
}

async function loadMyProjects() {
  if (isTeacher.value) return
  try {
    // Load user's own projects for submission
    const res = await projectApi.getDetail(0) // This won't work, but we need a list API
  } catch { /* ignore */ }
}

function openGrade(sub: HomeworkSubmission) {
  gradeForm.value = { submissionId: sub.id, score: sub.score || 0, comment: sub.comment || '' }
  showGradeDialog.value = true
}

function previewProject(projectId: number) {
  previewProjectId.value = projectId
  showPreview.value = true
}

async function doGrade() {
  actionLoading.value = true
  try {
    const res = await homeworkApi.grade(gradeForm.value.submissionId, gradeForm.value.score, gradeForm.value.comment)
    if (res.code === 0) {
      ElMessage.success('批改完成')
      showGradeDialog.value = false
      loadSubmissions()
      loadHomework()
    } else { ElMessage.error(res.msg) }
  } catch { ElMessage.error('批改失败') }
  finally { actionLoading.value = false }
}

async function publishHw() {
  actionLoading.value = true
  try {
    const res = await fetch(`/api/v1/homework/${hwId}/publish`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    const data = await res.json()
    if (data.code === 0) { ElMessage.success('已发布'); loadHomework() }
    else ElMessage.error(data.msg)
  } catch { ElMessage.error('操作失败') }
  finally { actionLoading.value = false }
}

async function closeHw() {
  actionLoading.value = true
  try {
    const res = await fetch(`/api/v1/homework/${hwId}/close`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    const data = await res.json()
    if (data.code === 0) { ElMessage.success('已关闭'); loadHomework() }
    else ElMessage.error(data.msg)
  } catch { ElMessage.error('操作失败') }
  finally { actionLoading.value = false }
}

async function submitHw() {
  if (!submitProjectId.value) return
  actionLoading.value = true
  try {
    const res = await homeworkApi.submit(hwId, submitProjectId.value)
    if (res.code === 0) { ElMessage.success('提交成功！'); loadHomework() }
    else ElMessage.error(res.msg)
  } catch { ElMessage.error('提交失败') }
  finally { actionLoading.value = false }
}

onMounted(async () => {
  await loadHomework()
  if (isTeacher.value) {
    loadSubmissions()
  }
})
</script>

<style scoped>
.homework-detail-page {
  max-width: 960px;
  margin: 0 auto;
}

.hero-card {
  position: relative;
  overflow: hidden;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.hw-title {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 10px;
  color: var(--text, #1a1a2e);
}

.hw-tags {
  display: flex;
  gap: 10px;
  align-items: center;
}

.hw-score {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary, #3B82F6);
}

.hw-desc {
  font-size: 14px;
  color: var(--text2, #64748b);
  line-height: 1.8;
  margin: 16px 0;
  padding: 14px;
  background: var(--bg, #f5f5f5);
  border-radius: 12px;
}

.hw-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 16px 0;
}

.stat-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: var(--bg, #f5f5f5);
  border-radius: 20px;
  font-size: 13px;
}

.chip-icon {
  font-size: 14px;
}

.text-red {
  color: #EF4444;
  font-weight: 600;
}

.progress-section {
  margin-top: 16px;
  padding: 16px;
  background: var(--bg, #f5f5f5);
  border-radius: 12px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 8px;
}

.progress-num {
  font-weight: 600;
  color: var(--primary, #3B82F6);
}

.progress-track {
  height: 8px;
  background: var(--border, #e5e7eb);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10B981, #3B82F6);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.section-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 16px;
}

.student-name {
  font-weight: 600;
}

.score-high { color: #10B981; font-weight: 700; }
.score-mid { color: #F59E0B; font-weight: 700; }
.score-low { color: #EF4444; font-weight: 700; }

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

/* My Submission Card */
.my-submission-card {
  text-align: center;
  padding: 20px;
}

.submission-status {
  margin-bottom: 16px;
}

.submission-score {
  margin-bottom: 16px;
}

.score-big {
  font-size: 48px;
  font-weight: 800;
  color: var(--primary, #3B82F6);
}

.score-unit {
  font-size: 20px;
  color: var(--text2, #64748b);
}

.submission-comment {
  padding: 16px;
  background: var(--bg, #f5f5f5);
  border-radius: 12px;
  text-align: left;
}

.comment-label {
  font-size: 13px;
  color: var(--text2, #64748b);
  margin-bottom: 6px;
}

.comment-text {
  font-size: 15px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .hw-title {
    font-size: 20px;
  }
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table .cell) { padding: 6px 4px; }
}

.submit-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.create-hint {
  font-size: 14px;
  color: var(--text2, #64748b);
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
