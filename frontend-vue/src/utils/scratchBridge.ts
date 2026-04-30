/**
 * Scratch VM (TurboWarp) postMessage 通信协议
 *
 * 支持的消息类型：
 * - 宿主 → Scratch: enter-editor / enter-player / exportProject / load-project
 * - Scratch → 宿主: project-changed / project-save / fullscreen / error
 */

export interface ScratchMessage {
  type: string
  data?: unknown
}

export interface ScratchProjectData {
  /** base64 编码的 sb3 数据 */
  data?: string
  /** 项目 JSON */
  projectJson?: string
  /** 错误信息 */
  error?: string
}

export interface ScratchHostOptions {
  /** 编辑器 iframe 引用 */
  iframe: HTMLIFrameElement | null
  /** 项目变更回调 */
  onProjectChanged?: () => void
  /** 项目保存回调（收到 sb3 数据） */
  onProjectSave?: (base64Data: string) => void
  /** 全屏事件回调 */
  onFullscreen?: (isFullscreen: boolean) => void
  /** 错误回调 */
  onError?: (error: string) => void
  /** VM 初始化完成回调 */
  onVmReady?: (vm: unknown) => void
}

/**
 * Scratch VM 通信管理器
 */
export class ScratchBridge {
  private iframe: HTMLIFrameElement | null
  private options: ScratchHostOptions
  private messageHandler: (event: MessageEvent) => void

  constructor(options: ScratchHostOptions) {
    this.iframe = options.iframe
    this.options = options
    this.messageHandler = this.handleMessage.bind(this)
  }

  /** 开始监听消息 */
  start() {
    window.addEventListener('message', this.messageHandler)
  }

  /** 停止监听消息 */
  stop() {
    window.removeEventListener('message', this.messageHandler)
  }

  /** 更新 iframe 引用 */
  setIframe(iframe: HTMLIFrameElement | null) {
    this.iframe = iframe
  }

  /** 向 Scratch 发送消息（使用 origin 限制，避免 postMessage 泄露到恶意页面） */
  private postMessage(message: ScratchMessage) {
    // 使用 iframe 的 src origin，而非通配符 '*'
    // 安全改进: 防止恶意页面通过 window.opener 或嵌入的 iframe 接收消息
    const targetOrigin = this.iframe?.src
      ? new URL(this.iframe.src).origin
      : window.location.origin
    this.iframe?.contentWindow?.postMessage(message, targetOrigin)
  }

  /** 处理来自 Scratch 的消息 */
  private handleMessage(event: MessageEvent) {
    const data = event.data as ScratchMessage
    if (!data?.type) return

    switch (data.type) {
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

      case 'vm-initialized':
        this.options.onVmReady?.((data as Record<string, unknown>).vm)
        break
    }
  }

  /** 请求导出项目（base64 sb3） */
  exportProject() {
    this.postMessage({ type: 'exportProject' })
  }

  /** 切换到编辑模式 */
  enterEditor() {
    this.postMessage({ type: 'enter-editor' })
  }

  /** 切换到播放模式 */
  enterPlayer() {
    this.postMessage({ type: 'enter-player' })
  }

  /** 加载项目（通过 URL） */
  loadProject(url: string) {
    this.postMessage({ type: 'load-project', data: { url } })
  }

  /** 请求全屏 */
  requestFullscreen() {
    this.postMessage({ type: 'set-fullscreen', data: { fullscreen: true } })
  }

  /** 退出全屏 */
  exitFullscreen() {
    this.postMessage({ type: 'set-fullscreen', data: { fullscreen: false } })
  }

  /** 导出为 SB3 文件（触发下载） */
  exportAsFile(filename = 'project.sb3') {
    this.exportProject()
    // 文件下载由 onProjectSave 回调处理
  }

  /** 销毁 */
  destroy() {
    this.stop()
    this.iframe = null
  }
}

/**
 * 创建 Scratch 桥接实例
 */
export function createScratchBridge(options: ScratchHostOptions): ScratchBridge {
  return new ScratchBridge(options)
}
