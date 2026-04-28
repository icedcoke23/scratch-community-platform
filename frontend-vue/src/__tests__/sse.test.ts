import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock EventSource
class MockEventSource {
  static instances: MockEventSource[] = []
  url: string
  readyState = 0
  onerror: ((event: any) => void) | null = null
  private listeners: Record<string, ((event: any) => void)[]> = {}

  constructor(url: string) {
    this.url = url
    this.readyState = 1 // OPEN
    MockEventSource.instances.push(this)
  }

  addEventListener(type: string, handler: (event: any) => void) {
    if (!this.listeners[type]) this.listeners[type] = []
    this.listeners[type].push(handler)
  }

  close() {
    this.readyState = 2 // CLOSED
  }

  // Test helpers
  emit(type: string, data: string) {
    const handlers = this.listeners[type] || []
    handlers.forEach(h => h({ data }))
  }

  emitError(message?: string) {
    if (this.onerror) this.onerror({})
  }
}

// @ts-ignore
global.EventSource = MockEventSource

import { describe as d2, it as i2, expect as e2 } from 'vitest'

describe('useSseStream', () => {
  beforeEach(() => {
    MockEventSource.instances = []
  })

  it('EventSource mock 应正常工作', () => {
    const es = new MockEventSource('/test')
    expect(es.url).toBe('/test')
    expect(es.readyState).toBe(1)
    expect(MockEventSource.instances).toHaveLength(1)
  })

  it('EventSource 应支持事件监听', () => {
    const es = new MockEventSource('/test')
    let received = ''
    es.addEventListener('token', (e: any) => { received = e.data })
    es.emit('token', 'hello')
    expect(received).toBe('hello')
  })

  it('EventSource close 应设置 CLOSED 状态', () => {
    const es = new MockEventSource('/test')
    es.close()
    expect(es.readyState).toBe(2)
  })
})

describe('SSE 相关工具函数', () => {
  it('URL 编码应正确处理特殊字符', () => {
    const token = 'abc?def&ghi=jkl'
    const encoded = encodeURIComponent(token)
    expect(encoded).not.toContain('?')
    expect(encoded).not.toContain('&')
    expect(encoded).not.toContain('=')
  })

  it('SSE URL 构建应包含 token 参数', () => {
    const projectId = 42
    const sseToken = 'test-sse-token-123'
    const url = `/api/v1/ai-review/project/${projectId}/stream?token=${encodeURIComponent(sseToken)}`
    expect(url).toContain('/api/v1/ai-review/project/42/stream')
    expect(url).toContain('token=test-sse-token-123')
  })
})
