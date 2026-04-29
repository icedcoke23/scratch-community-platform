<template>
  <div>
    <h1 class="page-title">
      <span class="title-emoji">📝</span>
      {{ t('problems.title') }}
    </h1>

    <LoadingSkeleton v-if="loading" :count="8" variant="problem" />
    <EmptyState v-else-if="problems.length === 0" icon="📝" :text="t('problems.empty')" />

    <div v-else class="problem-list">
      <div v-for="p in problems" :key="p.id" class="page-card problem-card">
        <div class="problem-header">
          <div class="problem-info">
            <div class="card-title">{{ p.title }}</div>
            <div class="problem-tags">
              <span class="difficulty-badge" :class="`difficulty-${p.difficulty}`">
                {{ difficultyIcon(p.difficulty) }} {{ difficultyLabel(p.difficulty) }}
              </span>
              <span class="type-badge">
                {{ typeIcon(p.type) }} {{ typeLabel(p.type) }}
              </span>
            </div>
          </div>
          <div class="problem-score">
            <span class="score-number">{{ p.score }}</span>
            <span class="score-label">分</span>
          </div>
        </div>
        <div class="problem-stats">
          <div class="stat-item">
            <span class="stat-icon">📊</span>
            <span>{{ t('problems.submissions', { count: p.submitCount || 0 }) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-icon">✅</span>
            <span>{{ t('problems.acceptRate', { rate: p.submitCount ? Math.round((p.acceptCount / p.submitCount) * 100) : 0 }) }}</span>
          </div>
        </div>
        <div class="problem-actions">
          <el-button type="primary" size="large" @click="startSolve(p)" class="solve-btn">
            🚀 {{ t('problems.submit') }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 答题弹窗 -->
    <el-dialog v-model="showSolveDialog" :title="" width="520px" class="solve-dialog">
      <div class="solve-header">
        <h3 class="solve-title">{{ currentProblem?.title }}</h3>
        <span v-if="currentProblem" class="difficulty-badge" :class="`difficulty-${currentProblem.difficulty}`">
          {{ difficultyIcon(currentProblem.difficulty) }} {{ difficultyLabel(currentProblem.difficulty) }}
        </span>
      </div>
      <p v-if="currentProblem?.description" class="solve-desc">
        {{ currentProblem.description }}
      </p>
      <div v-if="currentProblem?.type === 'choice' || currentProblem?.type === 'true_false'" class="options-list">
        <div
          v-for="opt in (currentProblem.options || [])"
          :key="opt.key"
          class="option-item"
          :class="{ selected: selectedAnswer === opt.key }"
          @click="selectedAnswer = opt.key"
        >
          <span class="option-key">{{ opt.key }}</span>
          <span class="option-text">{{ opt.text }}</span>
          <span v-if="selectedAnswer === opt.key" class="option-check">✓</span>
        </div>
      </div>
      <div v-else class="scratch-hint">
        <span class="hint-icon">🧩</span>
        <span>Scratch 编程题需要上传 sb3 文件</span>
      </div>
      <template #footer>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="submitAnswer"
          size="large"
          class="submit-answer-btn"
        >
          📤 {{ t('problems.submit') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 结果弹窗 -->
    <el-dialog v-model="showResultDialog" :title="" width="440px" class="result-dialog">
      <div class="result-content">
        <div class="result-icon" :class="resultData?.verdict === 'AC' ? 'result-success' : 'result-fail'">
          {{ resultData?.verdict === 'AC' ? '🎉' : '😤' }}
        </div>
        <div :class="['verdict-text', resultData?.verdict === 'AC' ? 'verdict-ac' : 'verdict-wa']">
          {{ resultData?.verdict === 'AC' ? t('problems.ac') : t(`problems.${resultData?.verdict?.toLowerCase()}`) }}
        </div>
        <p v-if="resultData?.verdict === 'AC'" class="result-message">
          太棒了！你成功解决了这道题！
        </p>
        <p v-else class="result-message">
          别灰心，再试一次吧！
        </p>
      </div>
      <pre v-if="resultData?.judgeDetail" class="judge-detail">{{ resultData.judgeDetail }}</pre>
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

function difficultyIcon(d: string) {
  return ({ easy: '🌱', medium: '🌿', hard: '🔥' } as Record<string, string>)[d] || '❓'
}

function difficultyLabel(d: string) {
  return ({ easy: '简单', medium: '中等', hard: '困难' } as Record<string, string>)[d] || d
}

function typeIcon(type: string) {
  return ({ choice: '📋', true_false: '✅', scratch_algo: '🧩' } as Record<string, string>)[type] || '❓'
}

function typeLabel(type: string) {
  return ({ choice: t('problems.choice'), true_false: t('problems.trueFalse'), scratch_algo: t('problems.scratch') } as Record<string, string>)[type] || type
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
  if (!selectedAnswer.value) { ElMessage.warning('请选择一个答案'); return }
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
.problem-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.problem-card {
  padding: 20px;
  transition: all 0.2s ease;
}

.problem-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.problem-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.problem-info {
  flex: 1;
  min-width: 0;
}

.problem-tags {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.difficulty-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.difficulty-easy {
  background: #DCFCE7;
  color: #16A34A;
}

.difficulty-medium {
  background: #FEF3C7;
  color: #D97706;
}

.difficulty-hard {
  background: #FEE2E2;
  color: #DC2626;
}

.type-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  background: var(--primary-bg);
  color: var(--primary);
}

.problem-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 16px;
  background: linear-gradient(135deg, var(--primary-bg), #FEF3C7);
  border-radius: 12px;
  min-width: 60px;
}

.score-number {
  font-size: 24px;
  font-weight: 800;
  color: var(--primary);
  line-height: 1;
}

.score-label {
  font-size: 12px;
  color: var(--text2);
  font-weight: 500;
}

.problem-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text2);
}

.stat-icon {
  font-size: 16px;
}

.problem-actions {
  display: flex;
  justify-content: flex-end;
}

.solve-btn {
  min-width: 140px;
  font-weight: 600;
  border-radius: 12px;
}

/* 答题弹窗 */
.solve-dialog :deep(.el-dialog) {
  border-radius: 20px;
}

.solve-dialog :deep(.el-dialog__header) {
  display: none;
}

.solve-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.solve-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
}

.solve-desc {
  font-size: 15px;
  color: var(--text2);
  line-height: 1.7;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: var(--bg);
  border-radius: 12px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border: 2px solid var(--border);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 16px;
}

.option-item:hover {
  border-color: var(--primary-light);
  background: var(--primary-bg);
  transform: translateX(4px);
}

.option-item.selected {
  border-color: var(--primary);
  background: var(--primary-bg);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

.option-key {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary-bg);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
}

.option-item.selected .option-key {
  background: var(--primary);
  color: #fff;
}

.option-text {
  flex: 1;
  color: var(--text);
  font-weight: 500;
}

.option-check {
  color: var(--primary);
  font-size: 20px;
  font-weight: 700;
}

.scratch-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
  background: var(--primary-bg);
  border-radius: 14px;
  font-size: 15px;
  color: var(--text2);
}

.hint-icon {
  font-size: 28px;
}

.submit-answer-btn {
  width: 100%;
  height: 48px;
  font-size: 17px;
  font-weight: 700;
  border-radius: 14px;
}

/* 结果弹窗 */
.result-dialog :deep(.el-dialog) {
  border-radius: 20px;
}

.result-dialog :deep(.el-dialog__header) {
  display: none;
}

.result-content {
  text-align: center;
  padding: 20px 0;
}

.result-icon {
  font-size: 64px;
  margin-bottom: 12px;
}

.result-success {
  animation: success-bounce 0.5s ease;
}

@keyframes success-bounce {
  0% { transform: scale(0); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.verdict-text {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 8px;
}

.verdict-ac { color: var(--success); }
.verdict-wa { color: var(--danger); }

.result-message {
  font-size: 16px;
  color: var(--text2);
  margin: 0;
}

.judge-detail {
  background: var(--bg);
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 13px;
  overflow-x: auto;
  margin-top: 16px;
  color: var(--text2);
}

@media (max-width: 768px) {
  .problem-header { flex-direction: column; gap: 12px; }
  .problem-score { flex-direction: row; gap: 4px; min-width: auto; }
  .score-number { font-size: 20px; }
  .option-item { padding: 12px 14px; font-size: 15px; }
}
</style>
