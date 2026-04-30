export type PlaybackState = 'stopped' | 'running' | 'paused'

export type EditorMode = 'editor' | 'player'

export interface BaseMessage {
  type: string
  [key: string]: unknown
}

export interface IframeReadyMessage extends BaseMessage {
  type: 'iframe-ready'
  mode: EditorMode
  state?: PlaybackState
}

export interface ProjectLoadedMessage extends BaseMessage {
  type: 'project-loaded'
  projectId?: number | string
  projectUrl?: string
}

export interface ProjectChangedMessage extends BaseMessage {
  type: 'project-changed'
  projectId?: number | string
  hasUnsavedChanges?: boolean
}

export interface PlaybackStateMessage extends BaseMessage {
  type: 'playback-state'
  state: PlaybackState
}

export interface SaveResultMessage extends BaseMessage {
  type: 'save-result'
  success: boolean
  error?: string
  projectId?: number | string
}

export interface StateResultMessage extends BaseMessage {
  type: 'state-result'
  state: unknown
}

export interface ErrorMessage extends BaseMessage {
  type: 'error'
  error: string
}

export interface GreenFlagMessage extends BaseMessage {
  type: 'green-flag'
}

export interface StopAllMessage extends BaseMessage {
  type: 'stop-all'
}

export type BridgeMessage =
  | IframeReadyMessage
  | ProjectLoadedMessage
  | ProjectChangedMessage
  | PlaybackStateMessage
  | SaveResultMessage
  | StateResultMessage
  | ErrorMessage
  | GreenFlagMessage
  | StopAllMessage
  | BaseMessage

export interface BridgeCallbacks {
  onReady?: (mode: EditorMode, state?: PlaybackState) => void
  onError?: (error: string) => void
  onProjectLoaded?: (projectId?: number | string, projectUrl?: string) => void
  onProjectChanged?: (projectId?: number | string, hasUnsavedChanges?: boolean) => void
  onPlaybackStateChange?: (state: PlaybackState) => void
  onGreenFlag?: () => void
  onStopAll?: () => void
  onSaveResult?: (success: boolean, error?: string, projectId?: number | string) => void
  onStateResult?: (state: unknown) => void
  onMessage?: (message: BridgeMessage) => void
}

export interface BridgeOptions {
  iframe: HTMLIFrameElement | null
  callbacks?: BridgeCallbacks
  debug?: boolean
}

export class ScratchBridge {
  private iframe: HTMLIFrameElement | null
  private callbacks: BridgeCallbacks
  private debug: boolean
  private messageHandler: ((event: MessageEvent) => void) | null = null
  private ready = false
  private mode: EditorMode = 'editor'
  private state: PlaybackState = 'stopped'

  constructor(options: BridgeOptions) {
    this.iframe = options.iframe
    this.callbacks = options.callbacks ?? {}
    this.debug = options.debug ?? false

    this.messageHandler = this.createMessageHandler()
  }

  private createMessageHandler(): (event: MessageEvent) => void {
    return (event: MessageEvent) => {
      const data = event.data

      if (!data || typeof data !== 'object') return
      if (event.source !== (this.iframe?.contentWindow ?? null)) return

      this.debug && console.debug('[ScratchBridge] Received:', data)

      switch (data.type) {
        case 'iframe-ready':
          this.handleReady(data)
          break

        case 'error':
          this.handleError(data)
          break

        case 'project-loaded':
          this.handleProjectLoaded(data)
          break

        case 'project-changed':
          this.handleProjectChanged(data)
          break

        case 'playback-state':
          this.handlePlaybackState(data)
          break

        case 'green-flag':
          this.handleGreenFlag()
          break

        case 'stop-all':
          this.handleStopAll()
          break

        case 'save-result':
          this.handleSaveResult(data)
          break

        case 'state-result':
          this.handleStateResult(data)
          break

        default:
          this.callbacks.onMessage?.(data as BridgeMessage)
      }
    }
  }

  private handleReady(data: IframeReadyMessage) {
    this.ready = true
    this.mode = data.mode ?? 'editor'
    this.state = data.state ?? 'stopped'
    this.callbacks.onReady?.(this.mode, this.state)
  }

  private handleError(data: ErrorMessage) {
    const error = data.error ?? 'Unknown error'
    this.callbacks.onError?.(error)
  }

  private handleProjectLoaded(data: ProjectLoadedMessage) {
    this.callbacks.onProjectLoaded?.(data.projectId, data.projectUrl)
  }

  private handleProjectChanged(data: ProjectChangedMessage) {
    this.callbacks.onProjectChanged?.(data.projectId, data.hasUnsavedChanges)
  }

  private handlePlaybackState(data: PlaybackStateMessage) {
    this.state = data.state
    this.callbacks.onPlaybackStateChange?.(this.state)
  }

  private handleGreenFlag() {
    this.state = 'running'
    this.callbacks.onGreenFlag?.()
  }

  private handleStopAll() {
    this.state = 'stopped'
    this.callbacks.onStopAll?.()
  }

  private handleSaveResult(data: SaveResultMessage) {
    this.callbacks.onSaveResult?.(data.success, data.error, data.projectId)
  }

  private handleStateResult(data: StateResultMessage) {
    this.callbacks.onStateResult?.(data.state)
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
    this.state = 'stopped'
  }

  isReady(): boolean {
    return this.ready
  }

  getMode(): EditorMode {
    return this.mode
  }

  getState(): PlaybackState {
    return this.state
  }

  send(message: BaseMessage) {
    if (!this.iframe?.contentWindow) {
      this.debug && console.warn('[ScratchBridge] iframe not ready')
      return
    }

    this.debug && console.debug('[ScratchBridge] Sending:', message)
    this.iframe.contentWindow.postMessage(message, '*')
  }

  play() {
    this.send({ type: 'play' })
  }

  stopPlayback() {
    this.send({ type: 'stop' })
  }

  pause() {
    this.send({ type: 'pause' })
  }

  reload() {
    this.send({ type: 'reload' })
  }

  saveProject() {
    this.send({ type: 'tw-save-project' })
  }

  loadProject(projectUrl: string) {
    this.send({ type: 'tw-load-project', projectUrl })
  }

  requestState() {
    this.send({ type: 'tw-get-state' })
  }

  updateCallbacks(callbacks: Partial<BridgeCallbacks>) {
    this.callbacks = { ...this.callbacks, ...callbacks }
  }

  destroy() {
    this.stop()
    this.iframe = null
    this.callbacks = {}
  }
}

export function createScratchBridge(options: BridgeOptions): ScratchBridge {
  return new ScratchBridge(options)
}
