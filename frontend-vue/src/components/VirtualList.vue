<template>
  <div
    ref="containerRef"
    class="virtual-list"
    :style="{ height: height + 'px', overflow: 'auto' }"
    @scroll="onScroll"
  >
    <div class="virtual-list-phantom" :style="{ height: totalHeight + 'px' }" />
    <div class="virtual-list-content" :style="{ transform: `translateY(${offsetY}px)` }">
      <div
        v-for="item in visibleItems"
        :key="item.index"
        class="virtual-list-item"
        :style="{ height: itemHeight + 'px' }"
      >
        <slot :item="item.data" :index="item.index" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'

const props = withDefaults(defineProps<{
  items: T[]
  itemHeight?: number
  height?: number
  buffer?: number
}>(), {
  itemHeight: 60,
  height: 400,
  buffer: 5
})

const containerRef = ref<HTMLDivElement>()
const scrollTop = ref(0)

const totalHeight = computed(() => props.items.length * props.itemHeight)

const visibleCount = computed(() => Math.ceil(props.height / props.itemHeight) + props.buffer * 2)

const startIndex = computed(() => {
  const start = Math.floor(scrollTop.value / props.itemHeight) - props.buffer
  return Math.max(0, start)
})

const endIndex = computed(() => {
  const end = startIndex.value + visibleCount.value
  return Math.min(props.items.length, end)
})

const offsetY = computed(() => startIndex.value * props.itemHeight)

const visibleItems = computed(() => {
  return props.items.slice(startIndex.value, endIndex.value).map((data, i) => ({
    data,
    index: startIndex.value + i
  }))
})

function onScroll() {
  if (containerRef.value) {
    scrollTop.value = containerRef.value.scrollTop
  }
}

// 滚动到指定索引
function scrollToIndex(index: number) {
  if (containerRef.value) {
    containerRef.value.scrollTop = index * props.itemHeight
  }
}

// 滚动到顶部
function scrollToTop() {
  if (containerRef.value) {
    containerRef.value.scrollTop = 0
  }
}

defineExpose({ scrollToIndex, scrollToTop })
</script>

<style scoped>
.virtual-list {
  position: relative;
}

.virtual-list-phantom {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  z-index: -1;
}

.virtual-list-content {
  position: relative;
}

.virtual-list-item {
  overflow: hidden;
}
</style>
