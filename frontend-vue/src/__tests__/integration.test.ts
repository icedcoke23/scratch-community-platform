import { describe, it, expect, vi } from 'vitest'

// ScratchBridge 集成测试
describe('ScratchBridge 集成', () => {
  it('应能创建实例', async () => {
    const { createScratchBridge, ScratchBridge } = await import('@/utils/scratchBridge')
    const bridge = createScratchBridge({ iframe: null })
    expect(bridge).toBeInstanceOf(ScratchBridge)
    bridge.destroy()
  })

  it('start/stop 应管理事件监听', async () => {
    const { createScratchBridge } = await import('@/utils/scratchBridge')
    const addSpy = vi.spyOn(window, 'addEventListener')
    const removeSpy = vi.spyOn(window, 'removeEventListener')

    const bridge = createScratchBridge({ iframe: null })
    bridge.start()
    expect(addSpy).toHaveBeenCalledWith('message', expect.any(Function))

    bridge.stop()
    expect(removeSpy).toHaveBeenCalledWith('message', expect.any(Function))
    bridge.destroy()
  })

  it('setIframe 应更新 iframe 引用', async () => {
    const { createScratchBridge } = await import('@/utils/scratchBridge')
    const bridge = createScratchBridge({ iframe: null })

    const mockIframe = document.createElement('iframe')
    bridge.setIframe(mockIframe)
    // 内部状态更新，无法直接验证，但不应抛错
    bridge.destroy()
  })

  it('回调应在收到消息时触发', async () => {
    const { createScratchBridge } = await import('@/utils/scratchBridge')
    const onChanged = vi.fn()
    const bridge = createScratchBridge({
      iframe: null,
      onProjectChanged: onChanged
    })

    bridge.start()
    // 模拟 postMessage
    window.postMessage({ type: 'project-changed' }, '*')

    // 等待消息传递
    await new Promise(r => setTimeout(r, 50))
    expect(onChanged).toHaveBeenCalled()

    bridge.destroy()
  })

  it('exportProject 应向 iframe 发送消息', async () => {
    const { createScratchBridge } = await import('@/utils/scratchBridge')
    const mockPostMessage = vi.fn()
    const mockIframe = {
      src: '',
      contentWindow: { postMessage: mockPostMessage }
    } as unknown as HTMLIFrameElement

    const bridge = createScratchBridge({ iframe: mockIframe })
    bridge.exportProject()
    // targetOrigin 使用精确的 origin（从 window.location.origin 获取）或 '*'
    expect(mockPostMessage).toHaveBeenCalledWith({ type: 'exportProject' }, expect.stringMatching(/^(\*|http:\/\/localhost:\d+)$/))
    bridge.destroy()
  })

  it('enterEditor/enterPlayer 应发送正确消息', async () => {
    const { createScratchBridge } = await import('@/utils/scratchBridge')
    const mockPostMessage = vi.fn()
    const mockIframe = {
      src: '',
      contentWindow: { postMessage: mockPostMessage }
    } as unknown as HTMLIFrameElement

    const bridge = createScratchBridge({ iframe: mockIframe })

    bridge.enterEditor()
    expect(mockPostMessage).toHaveBeenCalledWith({ type: 'enter-editor' }, expect.stringMatching(/^(\*|http:\/\/localhost:\d+)$/))

    bridge.enterPlayer()
    expect(mockPostMessage).toHaveBeenCalledWith({ type: 'enter-player' }, expect.stringMatching(/^(\*|http:\/\/localhost:\d+)$/))

    bridge.destroy()
  })
})

// vLazy 指令集成测试
describe('vLazy 指令集成', () => {
  it('应导出 vLazy 指令', async () => {
    const { vLazy } = await import('@/directives/lazy')
    expect(vLazy).toBeDefined()
    expect(vLazy.mounted).toBeInstanceOf(Function)
    expect(vLazy.updated).toBeInstanceOf(Function)
    expect(vLazy.unmounted).toBeInstanceOf(Function)
  })

  it('mounted 应设置占位图', async () => {
    const { vLazy } = await import('@/directives/lazy')
    const img = document.createElement('img')
    const binding = {
      value: 'https://example.com/image.jpg',
      oldValue: undefined,
      modifiers: {},
      dir: {},
      instance: null,
      arg: undefined
    }

    vLazy.mounted(img, binding as any)
    expect(img.src).toContain('data:image')
    expect(img.classList.contains('lazy-img')).toBe(true)
  })
})

// MiniChart 集成测试
describe('MiniChart 集成', () => {
  it('应导出 MiniChart 组件', async () => {
    const mod = await import('@/components/MiniChart.vue')
    expect(mod.default).toBeDefined()
  })
})

// ActivityTimeline 集成测试
describe('ActivityTimeline 集成', () => {
  it('应导出 ActivityTimeline 组件', async () => {
    const mod = await import('@/components/ActivityTimeline.vue')
    expect(mod.default).toBeDefined()
  })
})
