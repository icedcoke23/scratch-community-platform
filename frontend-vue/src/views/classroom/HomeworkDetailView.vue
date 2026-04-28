<template>
  <div>
    <div class="back-link" @click="router.push('/homework')">← {{ t('common.back') }}</div>
    <LoadingSkeleton v-if="loading" variant="detail" />
    <template v-else-if="homework">
      <div class="page-card">
        <div style="font-size: 20px; font-weight: 700; margin-bottom: 8px">{{ homework.title }}</div>
        <div class="card-meta" style="margin-bottom: 10px">
          <el-tag :type="homework.status === 'published' ? 'success' : 'info'" size="small">{{ homework.status === 'published' ? t('homework.pending') : t('competition.status.draft') }}</el-tag>
          <span>{{ t('competition.totalScore', { score: homework.totalScore }) }}</span>
          <span>{{ t('homework.submitted') }} {{ homework.submitCount }} / {{ t('homework.graded') }} {{ homework.gradedCount }}</span>
          <span v-if="homework.deadline">⏰ {{ t('homework.deadline') }} {{ new Date(homework.deadline).toLocaleString() }}</span>
        </div>
        <p v-if="homework.description" style="font-size: 14px; color: var(--text2)">{{ homework.description }}</p>
      </div>
      <div v-if="userStore.user?.role === 'STUDENT'" class="page-card">
        <div style="font-weight: 600; margin-bottom: 10px">📝 {{ t('homework.submit') }}</div>
        <el-input v-model="projectId" placeholder="Project ID" type="number" style="margin-bottom: 10px" />
        <el-button type="primary" :loading="submitLoading" @click="doSubmit">{{ t('homework.submit') }}</el-button>
      </div>
      <div v-if="userStore.isTeacher" class="page-card">
        <div style="font-weight: 600; margin-bottom: 10px">📋 {{ t('homework.submitted') }}</div>
        <el-table :data="submissions" stripe size="small" v-if="submissions.length > 0">
          <el-table-column label="学生"><template #default="{ row }">{{ row.nickname || row.username || row.studentId }}</template></el-table-column>
          <el-table-column prop="projectId" label="项目 ID" width="80" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }"><el-tag :type="row.status === 'graded' ? 'success' : 'info'" size="small">{{ row.status === 'graded' ? t('homework.graded') : t('homework.pending') }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="score" label="分数" width="60" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }"><el-button type="primary" size="small" @click="showGradeDialog(row)">{{ t('homework.score') }}</el-button></template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state" style="padding: 20px">{{ t('common.empty') }}</div>
      </div>
    </template>
    <el-dialog v-model="showGrade" :title="t('homework.score')" width="400px">
      <el-form label-width="60px">
        <el-form-item :label="t('homework.score')"><el-input-number v-model="gradeForm.score" :min="0" :max="100" /></el-form-item>
        <el-form-item :label="t('homework.comment')"><el-input v-model="gradeForm.comment" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" :loading="gradeLoading" @click="doGrade" style="width: 100%">{{ t('common.submit') }}</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'HomeworkDetailView' })
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { homeworkApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/utils/error'
import { useI18n } from '@/composables/useI18n'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import type { HomeworkSubmission } from '@/types'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const homework = ref<any>(null)
const submissions = ref<any[]>([])
const projectId = ref('')
const submitLoading = ref(false)
const showGrade = ref(false)
const gradeLoading = ref(false)
const gradeForm = reactive({ submissionId: 0, score: 0, comment: '' })
const hwId = Number(route.params.id)

async function doSubmit() {
  const pid = parseInt(projectId.value)
  if (isNaN(pid) || pid <= 0) { ElMessage.warning('请输入有效项目 ID'); return }
  submitLoading.value = true
  try {
    const res = await homeworkApi.submit(hwId, pid)
    if (res.code === 0) { ElMessage.success('提交成功'); projectId.value = '' }
    else ElMessage.error(res.msg)
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
  finally { submitLoading.value = false }
}

function showGradeDialog(row: HomeworkSubmission) {
  gradeForm.submissionId = row.id; gradeForm.score = row.score || 0; gradeForm.comment = row.comment || ''
  showGrade.value = true
}

async function doGrade() {
  gradeLoading.value = true
  try {
    const res = await homeworkApi.grade(gradeForm.submissionId, gradeForm.score, gradeForm.comment)
    if (res.code === 0) { showGrade.value = false; ElMessage.success('批改成功'); await loadSubmissions() }
    else ElMessage.error(res.msg)
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
  finally { gradeLoading.value = false }
}

async function loadSubmissions() {
  try { const res = await homeworkApi.getSubmissions(hwId); if (res.code === 0) submissions.value = res.data?.records || [] }
  catch { /* 忽略 */ }
}

onMounted(async () => {
  try {
    const res = await homeworkApi.getDetail(hwId)
    if (res.code === 0) { homework.value = res.data; if (userStore.isTeacher) await loadSubmissions() }
    else { ElMessage.error(res.msg); router.push('/homework') }
  } catch { /* 忽略 */ }
  finally { loading.value = false }
})
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table .cell) { padding: 6px 4px; }
  :deep(.el-dialog) { width: 90% !important; margin: 0 auto; }
}

@media (max-width: 480px) {
  :deep(.el-form-item__label) { font-size: 12px; }
}
</style>
