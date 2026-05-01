import { ref, onBeforeUnmount, type Ref } from 'vue'

export interface DebouncedFunction<T extends (...args: any[]) => any> {
  (...args: Parameters<T>): void
  cancel: () => void
}

/**
 * Debounce composable
 * 
 * @param fn 要防抖的函数
 * @param delay 延迟毫秒数 (默认 300)
 */
export function useDebounce<T extends (...args: any[]) => any>(
  fn: T,
  delay = 300
): DebouncedFunction<T> {
  let timer: ReturnType<typeof setTimeout> | null = null

  function debounced(...args: Parameters<T>) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn(...args)
      timer = null
    }, delay)
  }

  debounced.cancel = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  return debounced as DebouncedFunction<T>
}

/**
 * Throttle composable
 * 
 * @param fn 要节流的函数
 * @param interval 间隔毫秒数 (默认 300)
 */
export function useThrottle<T extends (...args: any[]) => any>(
  fn: T,
  interval = 300
): (...args: Parameters<T>) => void {
  let lastTime = 0

  return function throttled(...args: Parameters<T>) {
    const now = Date.now()
    if (now - lastTime >= interval) {
      lastTime = now
      fn(...args)
    }
  }
}

/**
 * 防抖 ref
 * 当 ref 值变化时，延迟触发回调
 */
export function useDebouncedRef<T>(
  initialValue: T,
  delay = 300
): {
  value: Ref<T>
  flush: () => void
  cancel: () => void
} {
  const value = ref(initialValue) as Ref<T>
  let timer: ReturnType<typeof setTimeout> | null = null
  let pendingValue: T = initialValue

  const originalSet = value.value

  // 使用 watch 代替 proxy
  const flush = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    value.value = pendingValue
  }

  const cancel = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  return { value, flush, cancel }
}

/**
 * 搜索防抖 composable
 * 专门用于搜索输入框
 */
export function useSearchDebounce(
  searchFn: (query: string) => void,
  delay = 500
): {
  searchQuery: Ref<string>
  isSearching: Ref<boolean>
  flush: () => void
  cancel: () => void
  onInput: (query: string) => void
  clear: () => void
} {
  const searchQuery = ref('')
  const isSearching = ref(false)

  const debouncedSearch = useDebounce((query: string) => {
    isSearching.value = true
    try {
      searchFn(query)
    } finally {
      isSearching.value = false
    }
  }, delay)

  function onInput(query: string) {
    searchQuery.value = query
    if (query.trim()) {
      debouncedSearch(query)
    } else {
      debouncedSearch.cancel()
      isSearching.value = false
    }
  }

  function clear() {
    searchQuery.value = ''
    debouncedSearch.cancel()
    isSearching.value = false
  }

  return {
    searchQuery,
    isSearching,
    onInput,
    clear,
    flush: () => debouncedSearch.cancel(),
    cancel: () => debouncedSearch.cancel()
  }
}
