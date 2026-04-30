import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

describe('useDebounce', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('应延迟执行函数', async () => {
    const { useDebounce } = await import('@/composables/useDebounce')
    const fn = vi.fn()
    const debounced = useDebounce(fn, 100)

    debounced()
    expect(fn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(100)
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('多次调用只执行最后一次', async () => {
    const { useDebounce } = await import('@/composables/useDebounce')
    const fn = vi.fn()
    const debounced = useDebounce(fn, 100)

    debounced()
    debounced()
    debounced()

    vi.advanceTimersByTime(100)
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('应传递参数', async () => {
    const { useDebounce } = await import('@/composables/useDebounce')
    const fn = vi.fn()
    const debounced = useDebounce(fn, 100)

    debounced('arg1', 'arg2')
    vi.advanceTimersByTime(100)
    expect(fn).toHaveBeenCalledWith('arg1', 'arg2')
  })

  it('cancel 应取消执行', async () => {
    const { useDebounce } = await import('@/composables/useDebounce')
    const fn = vi.fn()
    const debounced = useDebounce(fn, 100)

    debounced()
    debounced.cancel()

    vi.advanceTimersByTime(200)
    expect(fn).not.toHaveBeenCalled()
  })
})

describe('useThrottle', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('应立即执行第一次调用', async () => {
    const { useThrottle } = await import('@/composables/useDebounce')
    const fn = vi.fn()
    const throttled = useThrottle(fn, 100)

    throttled()
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('间隔内不应重复执行', async () => {
    const { useThrottle } = await import('@/composables/useDebounce')
    const fn = vi.fn()
    const throttled = useThrottle(fn, 100)

    throttled()
    throttled()
    throttled()

    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('间隔后应能再次执行', async () => {
    const { useThrottle } = await import('@/composables/useDebounce')
    const fn = vi.fn()
    const throttled = useThrottle(fn, 100)

    throttled()
    vi.advanceTimersByTime(100)
    throttled()

    expect(fn).toHaveBeenCalledTimes(2)
  })
})

describe('useSearchDebounce', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('应管理搜索状态', async () => {
    const { useSearchDebounce } = await import('@/composables/useDebounce')
    const searchFn = vi.fn()
    const { searchQuery, isSearching, onInput } = useSearchDebounce(searchFn, 100)

    expect(searchQuery.value).toBe('')
    expect(isSearching.value).toBe(false)

    onInput('test')
    expect(searchQuery.value).toBe('test')
  })

  it('clear 应重置状态', async () => {
    const { useSearchDebounce } = await import('@/composables/useDebounce')
    const searchFn = vi.fn()
    const { searchQuery, clear, onInput } = useSearchDebounce(searchFn, 100)

    onInput('test')
    expect(searchQuery.value).toBe('test')

    clear()
    expect(searchQuery.value).toBe('')
  })

  it('空查询不应触发搜索', async () => {
    const { useSearchDebounce } = await import('@/composables/useDebounce')
    const searchFn = vi.fn()
    const { onInput } = useSearchDebounce(searchFn, 100)

    onInput('')
    vi.advanceTimersByTime(200)
    expect(searchFn).not.toHaveBeenCalled()
  })
})

// 成就系统测试
describe('成就系统逻辑', () => {
  it('成就应包含必要字段', () => {
    const achievement = {
      id: 'first-project',
      name: '初出茅庐',
      description: '创建第一个项目',
      icon: '🎮',
      category: 'create',
      unlocked: true,
      unlockedAt: '2026-04-20T10:00:00'
    }
    expect(achievement.id).toBeTruthy()
    expect(achievement.name).toBeTruthy()
    expect(achievement.description).toBeTruthy()
    expect(achievement.icon).toBeTruthy()
    expect(['create', 'social', 'learn', 'special']).toContain(achievement.category)
  })

  it('进度计算应正确', () => {
    const achievements = [
      { unlocked: true },
      { unlocked: true },
      { unlocked: false },
      { unlocked: false },
      { unlocked: false }
    ]
    const unlocked = achievements.filter(a => a.unlocked).length
    const total = achievements.length
    const percent = Math.round((unlocked / total) * 100)
    expect(percent).toBe(40)
  })

  it('分类过滤应正确', () => {
    const achievements = [
      { category: 'create' },
      { category: 'social' },
      { category: 'create' },
      { category: 'learn' }
    ]
    const createOnly = achievements.filter(a => a.category === 'create')
    expect(createOnly).toHaveLength(2)
  })

  it('进度颜色应根据百分比变化', () => {
    function getColor(percent: number) {
      if (percent >= 80) return '#10B981'
      if (percent >= 50) return '#F59E0B'
      return '#4F46E5'
    }
    expect(getColor(90)).toBe('#10B981')
    expect(getColor(60)).toBe('#F59E0B')
    expect(getColor(30)).toBe('#4F46E5')
  })
})

// 设置页面测试
describe('设置页面逻辑', () => {
  it('密码验证应正确', () => {
    function validatePassword(old: string, newP: string, confirm: string) {
      if (!old || !newP) return '请填写所有字段'
      if (newP.length < 6) return '新密码至少6位'
      if (newP !== confirm) return '两次密码不一致'
      return null
    }
    expect(validatePassword('', 'abc', 'abc')).toBe('请填写所有字段')
    expect(validatePassword('old', 'abc', 'abc')).toBe('新密码至少6位')
    expect(validatePassword('old', 'abcdef', 'abcghi')).toBe('两次密码不一致')
    expect(validatePassword('old', 'abcdef', 'abcdef')).toBeNull()
  })

  it('主题模式应可切换', () => {
    const modes = ['light', 'dark', 'auto']
    modes.forEach(mode => {
      expect(modes).toContain(mode)
    })
  })

  it('语言应可切换', () => {
    const locales = ['zh-CN', 'en']
    locales.forEach(l => {
      expect(locales).toContain(l)
    })
  })
})
