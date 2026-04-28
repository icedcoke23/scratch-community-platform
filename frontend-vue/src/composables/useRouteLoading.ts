import { ref } from 'vue'

/**
 * 路由加载进度条状态管理
 *
 * 供 router guard 和 RouteLoadingBar.vue 组件共享状态。
 * 替代原来使用 document.getElementById 的 DOM 操作方式。
 */
const loading = ref(false)
const visible = ref(false)

let hideTimer: ReturnType<typeof setTimeout> | null = null

export function useRouteLoading() {
  function start() {
    if (hideTimer) {
      clearTimeout(hideTimer)
      hideTimer = null
    }
    visible.value = true
    loading.value = true
  }

  function finish() {
    loading.value = false
    hideTimer = setTimeout(() => {
      visible.value = false
      hideTimer = null
    }, 300)
  }

  return { loading, visible, start, finish }
}
