<template>
  <div>
    <h1 class="page-title">📝 {{ t('problems.title') }}</h1>

    <LoadingSkeleton v-if="loading" :count="8" variant="problem" />
    <EmptyState v-else-if="problems.length === 0" icon="📝" :text="t('problems.empty')" />

    <div v-else>
      <div v-for="p in problems" :key="p.id" class="page-card">
        <div style="display: flex; justify-content: space-between; align-items: center">
          <div class="card-title">{{ p.title }}</div>
          <el-tag :type="difficultyType(p.difficulty)" size="small">{{ t(`problems.${p.difficulty}`) }}</el-tag>
        </div>
        <div class="card-meta">
          <span>{{ typeLabel(p.type) }}</span>
          <span>{{ t('competition.totalScore', { score: p.score }) }}</span>
          <span>{{ t('problems.submissions', { count: p.submitCount || 0 }) }} / {{ t('problems.acceptRate', { rate: p.submitCount ? Math.round((p.acceptCount / p.submitCount) * 100) : 0 }) }}</span>
        </div>
        <div style="margin-top: 10px">
          <el-button type="primary" size="small" @click="startSolve(p)">{{ t('problems.submit') }}</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="showSolveDialog" :title="currentProblem?.title || t('problems.submit')" width="500px">
      <template v-if="currentProblem">
        <p v-if="currentProblem.description" style="font-size: 13px; color: var(--text2); margin-bottom: 14px">
          {{ currentProblem.description }}
        </p>
        <div v-if="currentProblem.type === 'choice' || currentProblem.type === 'true_false'">
          <div
            v-for="opt in (currentProblem.options || [])"
            :key="opt.key"
            class="option-item"
            :class="{ selected: selectedAnswer === opt.key }"
            @click="selectedAnswer = opt.key"
          >
            {{ opt.key }}. {{ opt.text }}
          </div>
        </div>
        <div v-else style="font-size: 13px; color: var(--text2)">Scratch 编程题需上传 sb3 文件</div>
      </template>
      <template #footer>
        <el-button type="primary" :loading="submitLoading" @click="submitAnswer" style="width: 100%">
          {{ t('problems.submit') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showResultDialog" :title="t('problems.result')" width="400px">
      <div style="text-align: center; padding: 16px">
        <div :class="['verdict', resultData?.verdict === 'AC' ? 'verdict-ac' : 'verdict-wa']" style="font-size: 24px; font-weight: 700">
          {{ resultData?.verdict === 'AC' ? '✅ ' + t('problems.ac') : '❌ ' + t(`problems.${resultData?.verdict?.toLowerCase()}`) }}
        </div>
        <p style="margin-top: 6px; color: var(--text2)">
          {{ resultData?.verdict === 'AC' ? '🎉' : '' }}
        </p>
      </div>
      <pre v-if="resultData?.judgeDetail" style="background: var(--bg); padding: 10px; border-radius: var(--radius); font-size: 12px; overflow-x: auto">{{ resultData.judgeDetail }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Problems' })

import { ref, onMounted } from 'vue'
import { problemApi } from '@/api'
import type { Problem, Submission } from '@/types'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/utils/error'
import { useI18n } from '@/composables/useI18n'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()

const loading = ref(true)
const problems = ref<Problem[]>([])
const showSolveDialog = ref(false)
const showResultDialog = ref(false)
const currentProblem = ref<Problem | null>(null)
const selectedAnswer = ref('')
const submitLoading = ref(false)
const resultData = ref<Submission | null>(null)

function difficultyType(d: string) {
  return ({ easy: 'success', medium: 'warning', hard: 'danger' } as Record<string, string>)[d] || 'info'
}

function typeLabel(type: string) {
  return ({ choice: '📋 ' + t('problems.choice'), true_false: '✅ ' + t('problems.trueFalse'), scratch_algo: '🧩 ' + t('problems.scratch') } as Record<string, string>)[type] || type
}

async function startSolve(problem: Problem) {
  try {
    const res = await problemApi.getDetail(problem.id)
    if (res.code === 0) {
      currentProblem.value = res.data || null
      selectedAnswer.value = ''
      showSolveDialog.value = true
    } else ElMessage.error(res.msg)
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
}

async function submitAnswer() {
  if (!selectedAnswer.value) { ElMessage.warning(t('problems.submit')); return }
  submitLoading.value = true
  try {
    const res = await problemApi.submit(currentProblem.value!.id, selectedAnswer.value)
    if (res.code === 0) {
      showSolveDialog.value = false
      resultData.value = res.data || null
      showResultDialog.value = true
    } else ElMessage.error(res.msg)
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
  finally { submitLoading.value = false }
}

onMounted(async () => {
  try {
    const res = await problemApi.list()
    if (res.code === 0) problems.value = res.data?.records || []
  } catch { /* 忽略 */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.option-item {
  padding: 10px 14px; border: 1px solid var(--border); border-radius: var(--radius);
  margin-bottom: 6px; cursor: pointer; transition: .15s; font-size: 14px;
}
.option-item:hover { border-color: var(--primary); background: var(--primary-bg); }
.option-item.selected { border-color: var(--primary); background: var(--primary-bg); font-weight: 600; }
.verdict-ac { color: var(--success); }
.verdict-wa { color: var(--danger); }

@media (max-width: 768px) {
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table .cell) { padding: 6px 4px; }
}

@media (max-width: 480px) {
  .option-item { font-size: 13px; padding: 8px; }
}
</style>
