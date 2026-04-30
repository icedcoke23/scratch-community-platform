<template>
  <div class="judge-detail">
    <div v-if="!details || details.length === 0" class="no-detail">
      <el-empty description="暂无判题详情" :image-size="80" />
    </div>

    <div v-else>
      <!-- 总览 -->
      <div class="summary">
        <el-tag :type="verdictType" size="large" effect="dark">
          {{ verdictLabel }}
        </el-tag>
        <span v-if="runtimeMs" class="runtime">
          <el-icon><Timer /></el-icon>
          {{ runtimeMs }}ms
        </span>
        <span v-if="score !== undefined" class="score">
          得分: <strong>{{ score }}</strong>
        </span>
      </div>

      <!-- 测试用例列表 -->
      <el-table :data="details" stripe size="small" class="detail-table">
        <el-table-column label="#" width="50" align="center">
          <template #default="{ $index }">
            {{ $index + 1 }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.passed ? '✓ 通过' : '✗ 失败' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="输入" min-width="120">
          <template #default="{ row }">
            <code class="io-text">{{ row.input || '-' }}</code>
          </template>
        </el-table-column>

        <el-table-column label="期望输出" min-width="120">
          <template #default="{ row }">
            <code class="io-text expected">{{ row.expected || '-' }}</code>
          </template>
        </el-table-column>

        <el-table-column label="实际输出" min-width="120">
          <template #default="{ row }">
            <code :class="['io-text', row.passed ? 'passed' : 'failed']">
              {{ row.actual ?? (row.error || '-') }}
            </code>
          </template>
        </el-table-column>

        <el-table-column label="用时" width="80" align="right">
          <template #default="{ row }">
            <span v-if="row.timeUsed">{{ row.timeUsed }}ms</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 执行日志（可折叠） -->
      <el-collapse v-if="executionLog" class="log-collapse">
        <el-collapse-item title="执行日志" name="log">
          <pre class="execution-log">{{ executionLog }}</pre>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Timer } from '@element-plus/icons-vue'

interface TestCaseResult {
  testCaseId: number
  input: string
  expected: string
  actual: string | null
  passed: boolean
  timeUsed?: number
  error?: string
}

interface Props {
  verdict?: string
  details?: TestCaseResult[]
  runtimeMs?: number
  score?: number
  executionLog?: string
}

const props = withDefaults(defineProps<Props>(), {
  verdict: 'PENDING',
  details: () => [],
  runtimeMs: undefined,
  score: undefined,
  executionLog: undefined
})

const verdictType = computed(() => {
  const map: Record<string, string> = {
    AC: 'success',
    WA: 'danger',
    TLE: 'warning',
    RE: 'danger',
    PENDING: 'info'
  }
  return map[props.verdict] || 'info'
})

const verdictLabel = computed(() => {
  const map: Record<string, string> = {
    AC: '✓ 答案正确 (AC)',
    WA: '✗ 答案错误 (WA)',
    TLE: '⏱ 超时 (TLE)',
    RE: '💥 运行错误 (RE)',
    PENDING: '⏳ 等待中'
  }
  return map[props.verdict] || props.verdict
})
</script>

<style scoped>
.judge-detail {
  padding: 12px 0;
}

.summary {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.runtime, .score {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.score strong {
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.detail-table {
  margin-bottom: 12px;
}

.io-text {
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 3px;
  background: var(--el-fill-color-light);
  word-break: break-all;
}

.io-text.expected {
  background: var(--el-color-info-light-9);
}

.io-text.passed {
  color: var(--el-color-success);
}

.io-text.failed {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.text-muted {
  color: var(--el-text-color-placeholder);
}

.log-collapse {
  margin-top: 8px;
}

.execution-log {
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  line-height: 1.6;
  padding: 12px;
  background: var(--el-fill-color-dark);
  color: var(--el-text-color-primary);
  border-radius: 6px;
  overflow-x: auto;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}

.no-detail {
  padding: 24px;
  text-align: center;
}
</style>
