<template>
  <div
    v-if="visible"
    class="route-loading-bar"
    :class="{ active: loading, done: !loading }"
  >
    <div class="bar-inner" />
  </div>
</template>

<script setup lang="ts">
import { useRouteLoading } from '@/composables/useRouteLoading'

const { loading, visible } = useRouteLoading()
</script>

<style scoped>
.route-loading-bar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  z-index: 9999;
  pointer-events: none;
}

.bar-inner {
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 0 2px 2px 0;
  transition: width 0.3s ease;
}

.route-loading-bar.active .bar-inner {
  width: 80%;
  transition: width 2s ease;
}

.route-loading-bar.done .bar-inner {
  width: 100%;
  transition: width 0.3s ease;
}
</style>
