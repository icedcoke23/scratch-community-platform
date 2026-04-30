import type { ProjectDetail } from '@/api'

export interface ScratchMessage {
  type: string
  data?: unknown
}

export interface ScratchHostOptions {
  iframe: HTMLIFrameElement | null
  onReady?: () => void
  onError?: (error: string) => void
}

export class ScratchBridge {
  private iframe: HTMLIFrameElement | null
  private messageHandler: ((event: MessageEvent) => void) | null = null
  private ready = false

  constructor(options: ScratchHostOptions) {
    this.iframe = options.iframe
    this.messageHandler = (event: MessageEvent) => {
      if (event.data?.type === 'iframe-ready') {
        this.ready = true
        options.onReady?.()
      } else if (event.data?.type === 'error') {
        options.onError?.(event.data.error)
      }
    }
  }

  start() {
    if (this.messageHandler) {
      window.addEventListener('message', this.messageHandler)
    }
  }

  stop() {
    if (this.messageHandler) {
      window.removeEventListener('message', this.messageHandler)
    }
  }

  setIframe(iframe: HTMLIFrameElement | null) {
    this.iframe = iframe
    this.ready = false
  }

  isReady(): boolean {
    return this.ready
  }

  destroy() {
    this.stop()
    this.iframe = null
  }
}

export function createScratchBridge(options: ScratchHostOptions): ScratchBridge {
  return new ScratchBridge(options)
}
