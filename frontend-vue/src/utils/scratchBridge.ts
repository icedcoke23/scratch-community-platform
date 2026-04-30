/**
 * Scratch VM postMessage 通信协议
 *
 * 消息类型：
 * - 宿主 → Scratch: load-project / exportProject / enter-editor / enter-player
 *                    / green-flag / stop-all / set-project-name / set-fullscreen
 * - Scratch → 宿主: editor-ready / player-ready / vm-initialized / project-loaded
 *                    / project-changed / project-save / project-start / project-stop
 *                    / error
 */

export interface ScratchMessage {
  type: string
  data?: unknown
  [key: string]: unknown
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
  onVmReady?: (vm: unknown) => void
  onReady?: () => void
  onProjectLoaded?: () => void
}

/**
 * Scratch VM 通信管理器
 */
export class ScratchBridge {
  private iframe: HTMLIFrameElement | null
  private options: ScratchHostOptions
  private messageHandler: (event: MessageEvent) => void
  private ready = false
  private vmReady = false

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
  }

  /**
   * 向 Scratch iframe 发送消息
   *
   * 安全策略：使用 '*' 作为 targetOrigin。
   * 原因：iframe 和父窗口同源（同域部署），'*' 不会引入额外安全风险。
   * 使用具体 origin 反而容易因 URL 变化（端口/协议）导致消息丢失。
   */
  private postMessage(message: ScratchMessage) {
    if (!this.iframe?.contentWindow) {
      console.warn('[ScratchBridge] iframe 不可用，消息丢弃:', message.type)
      return
    }
    try {
      this.iframe.contentWindow.postMessage(message, '*')
    } catch (e) {
      console.error('[ScratchBridge] postMessage 失败:', e)
    }
  }

  /**
   * 处理来自 Scratch iframe 的消息
   *
   * 过滤策略：只处理来自 iframe 的消息。
   * 通过 event.source 判断消息来源，防止处理其他窗口的消息。
   */
  private handleMessage(event: MessageEvent) {
    const data = event.data as ScratchMessage
    if (!data?.type) return

    // 安全：只处理来自 iframe 的消息（如果 iframe 已加载）
    // event.source 可能为 null（某些浏览器/情况），此时放行
    if (this.iframe?.contentWindow && event.source) {
      if (event.source !== this.iframe.contentWindow) return
    }

    switch (data.type) {
      case 'editor-ready':
      case 'player-ready':
        this.ready = true
        console.log('[ScratchBridge] 编辑器就绪:', data.type)
        this.options.onReady?.()
        break

      case 'vm-initialized':
        this.vmReady = true
        console.log('[ScratchBridge] VM 初始化完成')
        this.options.onVmReady?.((data as Record<string, unknown>).vm)
        break

      case 'project-loaded':
        console.log('[ScratchBridge] 项目加载完成')
        this.options.onProjectLoaded?.()
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

      case 'project-start':
        // 项目开始运行
        break

      case 'project-stop':
        // 项目停止
        break

      case 'fullscreen': {
        const isFull = !!(data as Record<string, unknown>).fullscreen
        this.options.onFullscreen?.(isFull)
        break
      }

      case 'error': {
        const errData = data as ScratchProjectData
        console.warn('[ScratchBridge] Scratch 错误:', errData.error)
        this.options.onError?.(errData.error || '未知错误')
        break
      }

      default:
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

  /** 导出为 SB3 文件 */
  exportAsFile(_filename = 'project.sb3') {
    this.exportProject()
  }

  /** 是否已就绪 */
  isReady(): boolean {
    return this.ready
  }

  /** VM 是否已初始化 */
  isVmReady(): boolean {
    return this.vmReady
  }

  /** 销毁 */
  destroy() {
    this.stop()
    this.iframe = null
    this.ready = false
    this.vmReady = false
  }
}

/**
 * 创建 Scratch 桥接实例
 */
export function createScratchBridge(options: ScratchHostOptions): ScratchBridge {
  return new ScratchBridge(options)
}
