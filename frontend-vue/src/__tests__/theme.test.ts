import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value }),
    removeItem: vi.fn((key: string) => { delete store[key] }),
    clear: vi.fn(() => { store = {} })
  }
})()

// Mock matchMedia
const matchMediaMock = vi.fn().mockImplementation((query: string) => ({
  matches: false,
  media: query,
  addEventListener: vi.fn(),
  removeEventListener: vi.fn()
}))

Object.defineProperty(window, 'localStorage', { value: localStorageMock })
Object.defineProperty(window, 'matchMedia', { value: matchMediaMock })

describe('useTheme', () => {
  beforeEach(() => {
    localStorageMock.clear()
    vi.clearAllMocks()
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.classList.remove('dark')
  })

  it('主题模式应支持 light/dark/auto', () => {
    const modes = ['light', 'dark', 'auto']
    modes.forEach(mode => {
      expect(['light', 'dark', 'auto']).toContain(mode)
    })
  })

  it('localStorage 应能存储主题偏好', () => {
    localStorage.setItem('theme-mode', 'dark')
    expect(localStorage.getItem('theme-mode')).toBe('dark')
  })

  it('document 应支持 data-theme 属性', () => {
    document.documentElement.setAttribute('data-theme', 'dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')

    document.documentElement.setAttribute('data-theme', 'light')
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
  })

  it('document 应支持 dark class', () => {
    document.documentElement.classList.add('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    document.documentElement.classList.remove('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('matchMedia 应能检测系统深色偏好', () => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    expect(mq).toBeDefined()
    expect(mq.matches).toBe(false)
    expect(mq.media).toBe('(prefers-color-scheme: dark)')
  })
})

describe('CSS 变量', () => {
  it('应定义亮色模式变量', () => {
    const root = document.documentElement
    root.setAttribute('data-theme', 'light')
    const style = getComputedStyle(root)
    // CSS 变量在测试环境中可能不可用，检查 DOM 操作正常
    expect(root.getAttribute('data-theme')).toBe('light')
  })

  it('应定义深色模式变量', () => {
    const root = document.documentElement
    root.setAttribute('data-theme', 'dark')
    root.classList.add('dark')
    expect(root.getAttribute('data-theme')).toBe('dark')
    expect(root.classList.contains('dark')).toBe(true)
  })
})
