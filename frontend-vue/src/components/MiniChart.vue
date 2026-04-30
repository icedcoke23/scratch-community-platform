<template>
  <div class="mini-chart">
    <div class="chart-title" v-if="title">{{ title }}</div>
    <div class="chart-container">
      <!-- 柱状图 -->
      <div v-if="type === 'bar'" class="bar-chart">
        <div
          v-for="(item, index) in data"
          :key="index"
          class="bar-item"
          :title="`${item.label}: ${item.value}`"
        >
          <div class="bar-value">{{ item.value }}</div>
          <div
            class="bar-fill"
            :style="{
              height: getBarHeight(item.value) + '%',
              background: color || 'var(--primary)'
            }"
          />
          <div class="bar-label">{{ item.label }}</div>
        </div>
      </div>

      <!-- 环形图 -->
      <div v-else-if="type === 'donut'" class="donut-chart">
        <svg viewBox="0 0 100 100" class="donut-svg">
          <circle
            cx="50" cy="50" r="40"
            fill="none"
            :stroke="bgColor || 'var(--border)'"
            stroke-width="12"
          />
          <circle
            cx="50" cy="50" r="40"
            fill="none"
            :stroke="color || 'var(--primary)'"
            stroke-width="12"
            stroke-linecap="round"
            :stroke-dasharray="donutDash"
            :stroke-dashoffset="donutOffset"
            transform="rotate(-90 50 50)"
          />
        </svg>
        <div class="donut-center">
          <div class="donut-value">{{ percentage }}%</div>
        </div>
      </div>

      <!-- 折线图 (简化版) -->
      <div v-else-if="type === 'line'" class="line-chart">
        <svg :viewBox="`0 0 ${lineWidth} ${lineHeight}`" class="line-svg">
          <polyline
            :points="linePoints"
            fill="none"
            :stroke="color || 'var(--primary)'"
            stroke-width="2"
            stroke-linejoin="round"
          />
          <polyline
            :points="lineArea"
            :fill="color || 'var(--primary)'"
            fill-opacity="0.1"
            stroke="none"
          />
        </svg>
        <div class="line-labels">
          <span v-for="(item, index) in data" :key="index" class="line-label">
            {{ item.label }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface ChartDataItem {
  label: string
  value: number
}

const props = withDefaults(defineProps<{
  type: 'bar' | 'donut' | 'line'
  data: ChartDataItem[]
  title?: string
  color?: string
  bgColor?: string
  maxValue?: number
}>(), {
  maxValue: 0
})

// 柱状图高度计算
const maxVal = computed(() => {
  if (props.maxValue > 0) return props.maxValue
  return Math.max(...props.data.map(d => d.value), 1)
})

function getBarHeight(value: number): number {
  return Math.round((value / maxVal.value) * 100)
}

// 环形图计算
const total = computed(() => props.data.reduce((sum, d) => sum + d.value, 0))
const percentage = computed(() => {
  if (total.value === 0) return 0
  const first = props.data[0]?.value || 0
  return Math.round((first / total.value) * 100)
})
const circumference = 2 * Math.PI * 40
const donutDash = computed(() => `${(percentage.value / 100) * circumference} ${circumference}`)
const donutOffset = computed(() => '0')

// 折线图计算
const lineWidth = 200
const lineHeight = 60
const linePoints = computed(() => {
  if (props.data.length === 0) return ''
  const step = lineWidth / Math.max(props.data.length - 1, 1)
  return props.data.map((d, i) => {
    const x = i * step
    const y = lineHeight - (d.value / maxVal.value) * (lineHeight - 10)
    return `${x},${y}`
  }).join(' ')
})
const lineArea = computed(() => {
  if (props.data.length === 0) return ''
  const step = lineWidth / Math.max(props.data.length - 1, 1)
  const points = props.data.map((d, i) => {
    const x = i * step
    const y = lineHeight - (d.value / maxVal.value) * (lineHeight - 10)
    return `${x},${y}`
  })
  return `0,${lineHeight} ${points.join(' ')} ${lineWidth},${lineHeight}`
})
</script>

<style scoped>
.mini-chart {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chart-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.chart-container {
  position: relative;
}

/* 柱状图 */
.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 100px;
  padding-bottom: 24px;
  position: relative;
}

.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  position: relative;
  height: 100%;
  justify-content: flex-end;
}

.bar-value {
  font-size: 10px;
  font-weight: 600;
  color: var(--text2);
}

.bar-fill {
  width: 100%;
  max-width: 32px;
  min-height: 4px;
  border-radius: 3px 3px 0 0;
  transition: height 0.5s ease;
}

.bar-label {
  font-size: 10px;
  color: var(--text2);
  position: absolute;
  bottom: -20px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* 环形图 */
.donut-chart {
  position: relative;
  width: 100px;
  height: 100px;
  margin: 0 auto;
}

.donut-svg {
  width: 100%;
  height: 100%;
}

.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.donut-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}

/* 折线图 */
.line-chart {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.line-svg {
  width: 100%;
  height: 60px;
}

.line-labels {
  display: flex;
  justify-content: space-between;
}

.line-label {
  font-size: 10px;
  color: var(--text2);
}

@media (max-width: 480px) {
  .bar-chart {
    height: 80px;
  }
  .donut-chart {
    width: 80px;
    height: 80px;
  }
  .donut-value {
    font-size: 14px;
  }
}
</style>
