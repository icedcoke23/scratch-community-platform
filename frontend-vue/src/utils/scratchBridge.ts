/**
 * Scratch VM postMessage 通信协议
 *
 * 支持本地 Scratch 编辑器和 TurboWarp 的消息类型：
 * - 宿主 → Scratch: enter-editor / enter-player / exportProject / load-project / green-flag / stop-all
 * - Scratch → 宿主: project-changed / project-save / editor-ready / player-ready / vm-initialized / project-loaded
 */

export interface ScratchMessage {
  type: string
  data?: unknown
  [key: string]: unknown
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
  /** 编辑器/播放器就绪回调 */
  onReady?: () => void
  /** 项目加载完成回调 */
  onProjectLoaded?: () => void
}

/**
 * Scratch VM 通信管理器
 * 同时支持本地 Scratch 编辑器和 TurboWarp 的 postMessage 协议
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

  /** 向 Scratch 发送消息 */
  private postMessage(message: ScratchMessage) {
    // 安全：使用 iframe 的 src origin，而非通配符 '*'
    // 本地编辑器使用 window.location.origin
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
      // === 通用事件 ===
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

      // === 本地 Scratch 编辑器事件 ===
      case 'vm-initialized':
        this.options.onVmReady?.((data as Record<string, unknown>).vm)
        break

      case 'editor-ready':
      case 'player-ready':
        this.options.onReady?.()
        break

      case 'project-loaded':
        this.options.onProjectLoaded?.()
        break

      case 'project-start':
        // 项目开始运行（绿旗）
        break

      case 'project-stop':
        // 项目停止
        break

      // === TurboWarp 兼容事件 ===
      // TurboWarp 使用的事件名可能略有不同
      default:
        // 静默忽略未知事件
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
    this.postMessage({ type: 'load-project', url })
  }

  /** 运行项目（绿旗） */
  greenFlag() {
    this.postMessage({ type: 'green-flag' })
  }

  /** 停止项目 */
  stopAll() {
    this.postMessage({ type: 'stop-all' })
  }

  /** 设置项目名称 */
  setProjectName(name: string) {
    this.postMessage({ type: 'set-project-name', name })
  }

  /** 获取项目名称 */
  getProjectName() {
    this.postMessage({ type: 'get-project-name' })
  }

  /** 请求全屏 */
  requestFullscreen() {
    this.postMessage({ type: 'set-fullscreen', fullscreen: true })
  }

  /** 退出全屏 */
  exitFullscreen() {
    this.postMessage({ type: 'set-fullscreen', fullscreen: false })
  }

  /** 导出为 SB3 文件（触发下载） */
  exportAsFile(_filename = 'project.sb3') {
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
