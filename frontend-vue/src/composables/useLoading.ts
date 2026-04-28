import { ref, computed } from 'vue'
import { getErrorMessage } from '@/utils/error'

/**
 * 全局 Loading 管理 Composable
 *
 * 用法:
 * const { loading, error, withLoading } = useLoading()
 *
 * await withLoading(async () => {
 *   const res = await api.getSomething()
 *   // 处理数据
 * })
 */
export function useLoading(initialState = false) {
  const loading = ref(initialState)
  const error = ref<string | null>(null)

  const isLoading = computed(() => loading.value)
  const hasError = computed(() => error.value !== null)

  /**
   * 包装异步操作，自动管理 loading 和 error 状态
   */
  async function withLoading<T>(fn: () => Promise<T>): Promise<T | null> {
    loading.value = true
    error.value = null
    try {
      return await fn()
    } catch (e: unknown) {
      error.value = getErrorMessage(e) || '操作失败'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 手动设置 loading 状态
   */
  function setLoading(state: boolean) {
    loading.value = state
  }

  /**
   * 手动设置错误
   */
  function setError(msg: string | null) {
    error.value = msg
  }

  /**
   * 清除错误
   */
  function clearError() {
    error.value = null
  }

  /**
   * 重置所有状态
   */
  function reset() {
    loading.value = false
    error.value = null
  }

  return {
    loading,
    error,
    isLoading,
    hasError,
    withLoading,
    setLoading,
    setError,
    clearError,
    reset
  }
}
