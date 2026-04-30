import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { timeAgo, formatDate, difficultyType, typeLabel } from '@/utils'

describe('timeAgo', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-04-24T12:00:00'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('null/undefined 应返回空字符串', () => {
    expect(timeAgo(null)).toBe('')
    expect(timeAgo(undefined)).toBe('')
  })

  it('刚刚（<1分钟）', () => {
    expect(timeAgo('2026-04-24T11:59:30')).toBe('刚刚')
  })

  it('N分钟前', () => {
    expect(timeAgo('2026-04-24T11:50:00')).toBe('10分钟前')
  })

  it('N小时前', () => {
    expect(timeAgo('2026-04-24T09:00:00')).toBe('3小时前')
  })

  it('N天前', () => {
    expect(timeAgo('2026-04-22T12:00:00')).toBe('2天前')
  })

  it('超过30天显示日期', () => {
    const result = timeAgo('2026-03-01T12:00:00')
    expect(result).toContain('2026')
  })

  it('Date 对象也应工作', () => {
    expect(timeAgo(new Date('2026-04-24T11:55:00'))).toBe('5分钟前')
  })
})

describe('formatDate', () => {
  it('null 应返回空字符串', () => {
    expect(formatDate(null)).toBe('')
  })

  it('应返回日期字符串', () => {
    const result = formatDate('2026-04-24T12:00:00')
    expect(result).toBeTruthy()
    expect(typeof result).toBe('string')
  })
})

describe('difficultyType', () => {
  it('easy 应返回 success', () => {
    expect(difficultyType('easy')).toBe('success')
  })

  it('medium 应返回 warning', () => {
    expect(difficultyType('medium')).toBe('warning')
  })

  it('hard 应返回 danger', () => {
    expect(difficultyType('hard')).toBe('danger')
  })

  it('未知难度应返回 info', () => {
    expect(difficultyType('unknown')).toBe('info')
  })
})

describe('typeLabel', () => {
  it('choice 应返回 选择题', () => {
    expect(typeLabel('choice')).toBe('选择题')
  })

  it('true_false 应返回 判断题', () => {
    expect(typeLabel('true_false')).toBe('判断题')
  })

  it('scratch_algo 应返回 编程题', () => {
    expect(typeLabel('scratch_algo')).toBe('编程题')
  })

  it('未知类型应返回原值', () => {
    expect(typeLabel('custom')).toBe('custom')
  })
})
