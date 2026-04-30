import { ref, watch, onMounted } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'auto'

const STORAGE_KEY = 'theme-mode'

/** 当前主题模式 */
export const themeMode = ref<ThemeMode>('light')

/** 是否深色模式 */
export const isDark = ref(false)

/** 系统是否偏好深色 */
const systemDark = ref(false)

/**
 * 主题管理 composable
 */
export function useTheme() {
  /** 应用主题到 DOM */
  function applyTheme(dark: boolean) {
    isDark.value = dark
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
    document.documentElement.classList.toggle('dark', dark)
  }

  /** 根据模式计算是否深色 */
  function resolveDark(mode: ThemeMode): boolean {
    if (mode === 'auto') return systemDark.value
    return mode === 'dark'
  }

  /** 切换主题 */
  function toggleTheme() {
    const next: ThemeMode = themeMode.value === 'dark' ? 'light' : 'dark'
    setTheme(next)
  }

  /** 设置主题模式 */
  function setTheme(mode: ThemeMode) {
    themeMode.value = mode
    localStorage.setItem(STORAGE_KEY, mode)
    applyTheme(resolveDark(mode))
  }

  /** 初始化 */
  function init() {
    // 读取存储的偏好
    const stored = localStorage.getItem(STORAGE_KEY) as ThemeMode | null
    if (stored && ['light', 'dark', 'auto'].includes(stored)) {
      themeMode.value = stored
    }

    // 监听系统主题变化
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    systemDark.value = mq.matches
    mq.addEventListener('change', (e) => {
      systemDark.value = e.matches
      if (themeMode.value === 'auto') {
        applyTheme(e.matches)
      }
    })

    // 应用初始主题
    applyTheme(resolveDark(themeMode.value))
  }

  onMounted(init)

  // 监听模式变化
  watch(themeMode, (mode) => {
    applyTheme(resolveDark(mode))
  })

  return {
    themeMode,
    isDark,
    toggleTheme,
    setTheme
  }
}
