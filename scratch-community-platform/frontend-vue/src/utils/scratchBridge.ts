/**
 * Scratch VM (TurboWarp) postMessage 双向通信协议
 * 
 * 支持的消息类型：
 * 
 * 【宿主 → TurboWarp】
 * - export-project: 请求导出项目
 * - load-project: 加载项目（通过 URL）
 * - enter-editor: 切换到编辑模式
 * - enter-player: 切换到播放模式
 * - set-fullscreen: 设置全屏状态
 * 
 * 【TurboWarp → 宿主】
 * - vm-initialized: VM 初始化完成
 * - project-changed: 项目已变更
 * - project-save: 项目保存/导出完成
 * - fullscreen: 全屏状态变化
 * - error: 错误信息
 * - editor-mode: 进入编辑模式
 * - player-mode: 进入播放模式
 */

export interface ScratchMessage {
  type: string
  data?: unknown
}

export interface ScratchProjectData {
  data?: string
  projectJson?: string
  error?: string
}

export interface ScratchHostOptions {
  iframe: HTMLIFrameElement | null
  onProjectChanged?: () => void
  onProjectSave?: (base64Data: string) => void
  onFullscreen?: (isFullscreen: boolean) => void
  onError?: (error: string) => void
  onVmReady?: (vmId: string) => void
  onEditorMode?: () => void
  onPlayerMode?: () => void
}

const TURBOWARP_ORIGIN = 'https://turbowarp.org'

export class ScratchBridge {
  private iframe: HTMLIFrameElement | null
  private options: ScratchHostOptions
  private messageHandler: (event: MessageEvent) => void
  private isReady: boolean = false
  private pendingExports: Map<string, (data: string) => void> = new Map()

  constructor(options: ScratchHostOptions) {
    this.iframe = options.iframe
    this.options = options
    this.messageHandler = this.handleMessage.bind(this)
  }

  start() {
    window.addEventListener('message', this.messageHandler)
  }

  stop() {
    window.removeEventListener('message', this.messageHandler)
  }

  setIframe(iframe: HTMLIFrameElement | null) {
    this.iframe = iframe
    this.isReady = false
  }

  private getTargetOrigin(): string {
    if (!this.iframe) return '*'
    const src = this.iframe.src
    if (src.startsWith('http')) {
      try {
        return new URL(src).origin
      } catch {
        return '*'
      }
    }
    if (src.startsWith('/')) {
      return window.location.origin
    }
    return '*'
  }

  private postMessage(message: ScratchMessage, forceOrigin: string | null = null) {
    const origin = forceOrigin || this.getTargetOrigin()
    this.iframe?.contentWindow?.postMessage(message, origin)
  }

  private handleMessage(event: MessageEvent) {
    if (!this.iframe) return
    
    const data = event.data as ScratchMessage
    if (!data?.type) return

    switch (data.type) {
      case 'vm-initialized':
        this.isReady = true
        const vmId = (data as { vmId?: string }).vmId || 'unknown'
        this.options.onVmReady?.(vmId)
        break

      case 'project-changed':
        this.options.onProjectChanged?.()
        break

      case 'project-save':
      case 'exportProject': {
        const projectData = data as ScratchProjectData
        if (projectData.data) {
          this.options.onProjectSave?.(projectData.data)
        }
        break
      }

      case 'fullscreen': {
        const isFull = !!(data as Record<string, unknown>).fullscreen
        this.options.onFullscreen?.(isFull)
        break
      }

      case 'error': {
        const errData = data as ScratchProjectData
        this.options.onError?.(errData.error || '未知错误')
        break
      }

      case 'editor-mode':
        this.options.onEditorMode?.()
        break

      case 'player-mode':
        this.options.onPlayerMode?.()
        break
    }
  }

  exportProject(): Promise<string> {
    return new Promise((resolve, reject) => {
      if (!this.isReady) {
        reject(new Error('VM 未初始化'))
        return
      }

      const requestId = Date.now().toString()
      this.pendingExports.set(requestId, resolve)

      const timeoutId = setTimeout(() => {
        this.pendingExports.delete(requestId)
        reject(new Error('导出超时'))
      }, 30000)

      const originalHandler = this.messageHandler
      const self = this
      window.addEventListener('message', function handler(event: MessageEvent) {
        const data = event.data as ScratchMessage
        if (data?.type === 'project-save' && data?.data) {
          clearTimeout(timeoutId)
          self.pendingExports.delete(requestId)
          window.removeEventListener('message', handler)
          resolve(data.data as string)
        }
      })

      this.postMessage({ type: 'export-project' })
    })
  }

  loadProject(url: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.isReady) {
        reject(new Error('VM 未初始化'))
        return
      }

      const timeoutId = setTimeout(() => {
        reject(new Error('加载超时'))
      }, 30000)

      const originalHandler = this.messageHandler
      const self = this
      window.addEventListener('message', function handler(event: MessageEvent) {
        const data = event.data as ScratchMessage
        if (data?.type === 'project-loaded' || data?.type === 'vm-initialized') {
          clearTimeout(timeoutId)
          window.removeEventListener('message', handler)
          resolve()
        }
      })

      this.postMessage({ type: 'load-project', data: { url } })

      setTimeout(reject, 30000)
    })
  }

  enterEditor() {
    if (!this.isReady) return
    this.postMessage({ type: 'enter-editor' })
  }

  enterPlayer() {
    if (!this.isReady) return
    this.postMessage({ type: 'enter-player' })
  }

  requestFullscreen() {
    this.postMessage({ type: 'set-fullscreen', data: { fullscreen: true } })
  }

  exitFullscreen() {
    this.postMessage({ type: 'set-fullscreen', data: { fullscreen: false } })
  }

  isVMReady(): boolean {
    return this.isReady
  }

  destroy() {
    this.stop()
    this.pendingExports.clear()
    this.iframe = null
  }
}

export function createScratchBridge(options: ScratchHostOptions): ScratchBridge {
  return new ScratchBridge(options)
}
