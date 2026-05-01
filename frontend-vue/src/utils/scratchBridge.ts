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
  private destroyed = false
  private messageQueue: ScratchMessage[] = []

  constructor(options: ScratchHostOptions) {
    this.iframe = options.iframe
    this.options = options
    this.messageHandler = this.handleMessage.bind(this)
  }

  start() {
    if (this.destroyed) return
    window.addEventListener('message', this.messageHandler)
  }

  stop() {
    window.removeEventListener('message', this.messageHandler)
  }

  setIframe(iframe: HTMLIFrameElement | null) {
    this.iframe = iframe
    // 如果有排队的消息，发送它们
    if (this.ready && this.messageQueue.length > 0) {
      this.flushQueue()
    }
  }

  /**
   * 向 Scratch iframe 发送消息
   *
   * 安全策略：优先使用精确的 origin，降级到 '*'
   * 原因：iframe 和父窗口同源（同域部署），精确 origin 更安全
   * 使用消息队列：防止在 iframe 未就绪时丢失消息
   */
  private postMessage(message: ScratchMessage) {
    if (this.destroyed) {
      console.warn('[ScratchBridge] 已销毁，消息丢弃:', message.type)
      return
    }

    if (!this.iframe?.contentWindow) {
      // 加入消息队列，等 iframe 就绪后发送
      this.messageQueue.push(message)
      console.log('[ScratchBridge] 消息入队（iframe 未就绪）:', message.type)
      return
    }

    // 安全策略：优先使用精确的 origin，降级到 '*'
    let targetOrigin = '*'
    try {
      if (this.iframe.src) {
        targetOrigin = new URL(this.iframe.src).origin
      } else if (window.location.origin && window.location.origin !== 'null') {
        targetOrigin = window.location.origin
      }
    } catch (e) {
      // URL 解析失败，使用 '*'
    }

    try {
      this.iframe.contentWindow.postMessage(message, targetOrigin)
    } catch (e) {
      console.error('[ScratchBridge] postMessage 失败:', e)
      try {
        this.iframe?.contentWindow?.postMessage(message, '*')
      } catch (e2) {
        console.error('[ScratchBridge] 降级发送也失败:', e2)
      }
    }
  }

  /**
   * 刷新消息队列，发送所有排队的消息
   */
  private flushQueue() {
    while (this.messageQueue.length > 0) {
      const message = this.messageQueue.shift()
      if (message) {
        this.postMessage(message)
      }
    }
  }

  /**
   * 处理来自 Scratch iframe 的消息
   *
   * 安全策略：
   * 1. 只处理来自 iframe 的消息
   * 2. 验证消息来源（event.source）
   * 3. 验证消息格式（防止 XSS）
   * 4. 消息内容白名单验证
   */
  private handleMessage(event: MessageEvent) {
    if (this.destroyed) return

    const data = event.data as ScratchMessage
    if (!data?.type) return

    // 安全：只处理来自 iframe 的消息（如果 iframe 已加载）
    // event.source 可能为 null（某些浏览器/情况），此时放行
    if (this.iframe?.contentWindow && event.source) {
      if (event.source !== this.iframe.contentWindow) {
        console.warn('[ScratchBridge] 忽略非 iframe 来源的消息')
        return
      }
    }

    // 安全：验证消息 origin（防止 XSS）
    const allowedOrigins = [
      window.location.origin,
      // 如果 iframe 是其他源，在这里添加白名单
    ]
    if (event.origin && !allowedOrigins.includes(event.origin)) {
      console.warn('[ScratchBridge] 忽略非白名单来源的消息:', event.origin)
      return
    }

    // 安全：验证消息类型（白名单）
    const allowedTypes = [
      'editor-ready', 'player-ready', 'vm-initialized', 'project-loaded',
      'project-changed', 'project-save', 'exportProject', 'project-start',
      'project-stop', 'fullscreen', 'error'
    ]
    if (!allowedTypes.includes(data.type)) {
      console.warn('[ScratchBridge] 忽略未知消息类型:', data.type)
      return
    }

    switch (data.type) {
      case 'editor-ready':
      case 'player-ready':
        this.ready = true
        console.log('[ScratchBridge] 编辑器就绪:', data.type)
        this.options.onReady?.()
        // 发送排队的消息
        this.flushQueue()
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
          // 安全：验证 base64 格式，防止注入
          if (typeof projectData.data === 'string' && 
              projectData.data.startsWith('data:application/x.scratch.sb3;base64,')) {
            this.options.onProjectSave?.(projectData.data)
          } else {
            console.warn('[ScratchBridge] 忽略无效的 sb3 数据格式')
          }
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
        const isFull = !!((data as Record<string, unknown>).fullscreen)
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
    // 安全：验证 URL 格式
    if (typeof url !== 'string' || !url) {
      console.warn('[ScratchBridge] 无效的项目 URL')
      return
    }
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
    // 安全：限制名称长度，防止注入
    const safeName = (name || '').substring(0, 100)
    this.postMessage({ type: 'set-project-name', name: safeName })
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

  /** 是否已销毁 */
  isDestroyed(): boolean {
    return this.destroyed
  }

  /** 销毁 */
  destroy() {
    this.destroyed = true
    this.stop()
    this.iframe = null
    this.ready = false
    this.vmReady = false
    this.messageQueue = []
  }
}

/**
 * 创建 Scratch 桥接实例
 */
export function createScratchBridge(options: ScratchHostOptions): ScratchBridge {
  return new ScratchBridge(options)
}
