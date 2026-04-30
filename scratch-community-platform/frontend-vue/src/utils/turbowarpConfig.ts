/**
 * TurboWarp 配置模块
 * 
 * 负责管理 TurboWarp 编辑器和播放器的 URL 构建与配置
 */

export interface TurboWarpConfig {
  mode: 'local' | 'remote'
  editorUrl: string
  playerUrl: string
  baseUrl: string
}

export interface UrlBuildOptions {
  projectUrl?: string
  projectId?: number | string
  autoplay?: boolean
  embed?: boolean
  enableCompiler?: boolean
  fps?: number
  theme?: 'light' | 'dark'
}

const DEFAULT_OPTIONS: UrlBuildOptions = {
  autoplay: false,
  embed: true,
  enableCompiler: true,
  fps: 60,
  theme: 'light'
}

function getConfig(): TurboWarpConfig {
  const isProduction = (import.meta as unknown as { env?: { PROD?: boolean } }).env?.PROD ?? false
  const localBase = '/turbowarp'
  const remoteBase = 'https://turbowarp.org'

  void isProduction

  return {
    mode: 'local',
    editorUrl: `${localBase}/editor.html`,
    playerUrl: `${localBase}/player.html`,
    baseUrl: remoteBase
  }
}

export const twConfig = getConfig()

function normalizeProjectUrl(url?: string): string | undefined {
  if (!url) return undefined
  
  try {
    const parsed = new URL(url)
    return parsed.href
  } catch {
    console.warn('[turbowarpConfig] Invalid project URL:', url)
    return undefined
  }
}

export function buildEditorUrl(projectUrl?: string, _options?: Partial<UrlBuildOptions>): string {
  const base = twConfig.editorUrl
  const normalizedUrl = normalizeProjectUrl(projectUrl)

  if (!normalizedUrl) return base

  const url = new URL(base, window.location.origin)
  url.searchParams.set('project_url', normalizedUrl)

  return url.toString()
}

export function buildPlayerUrl(projectUrl: string, options?: Partial<UrlBuildOptions>): string {
  const base = twConfig.playerUrl
  const mergedOptions = { ...DEFAULT_OPTIONS, autoplay: true, ...options }
  const normalizedUrl = normalizeProjectUrl(projectUrl)

  if (!normalizedUrl) {
    console.warn('[turbowarpConfig] Player URL requires a valid projectUrl')
    return base
  }

  const url = new URL(base, window.location.origin)
  url.searchParams.set('project_url', normalizedUrl)

  if (mergedOptions.autoplay) {
    url.searchParams.set('autoplay', 'true')
  }

  return url.toString()
}

export function buildDirectEditorUrl(options?: UrlBuildOptions): string {
  const mergedOptions = { ...DEFAULT_OPTIONS, ...options }
  const baseUrl = twConfig.baseUrl

  const url = new URL(`${baseUrl}/editor`)

  if (mergedOptions.projectUrl) {
    const normalized = normalizeProjectUrl(mergedOptions.projectUrl)
    if (normalized) {
      url.searchParams.set('project_url', normalized)
    }
  }

  if (mergedOptions.enableCompiler) {
    url.searchParams.set('compiler', 'true')
  }

  if (mergedOptions.fps && mergedOptions.fps > 0) {
    url.searchParams.set('fps', String(mergedOptions.fps))
  }

  if (mergedOptions.embed) {
    url.searchParams.set('embed', 'true')
  }

  if (mergedOptions.theme) {
    url.searchParams.set('ui', mergedOptions.theme === 'dark' ? 'dark' : 'light')
  }

  return url.toString()
}

export function buildDirectPlayerUrl(options?: UrlBuildOptions): string {
  const mergedOptions = { ...DEFAULT_OPTIONS, autoplay: true, ...options }
  const baseUrl = twConfig.baseUrl

  const url = new URL(baseUrl)

  if (mergedOptions.projectUrl) {
    const normalized = normalizeProjectUrl(mergedOptions.projectUrl)
    if (normalized) {
      url.searchParams.set('project_url', normalized)
    }
  }

  if (mergedOptions.enableCompiler) {
    url.searchParams.set('compiler', 'true')
  }

  if (mergedOptions.autoplay) {
    url.searchParams.set('autoplay', 'true')
  }

  if (mergedOptions.fps && mergedOptions.fps > 0) {
    url.searchParams.set('fps', String(mergedOptions.fps))
  }

  if (mergedOptions.embed) {
    url.searchParams.set('embed', 'true')
  }

  return url.toString()
}

export function getDownloadUrl(projectId: number | string): string {
  const id = typeof projectId === 'string' ? parseInt(projectId, 10) : projectId

  if (isNaN(id) || id <= 0) {
    console.warn('[turbowarpConfig] Invalid project ID for download:', projectId)
    return ''
  }

  return `${window.location.origin}/api/v1/project/${id}/sb3/download`
}

export function parseTurboWarpUrl(url: string): {
  mode: 'editor' | 'player' | 'unknown'
  projectUrl: string | null
  params: Record<string, string>
} | null {
  try {
    const parsed = new URL(url)
    const pathParts = parsed.pathname.split('/').filter(Boolean)

    let mode: 'editor' | 'player' | 'unknown' = 'unknown'
    if (pathParts.includes('editor')) {
      mode = 'editor'
    } else if (pathParts.length === 0 || !pathParts.includes('editor')) {
      mode = 'player'
    }

    const projectUrl = parsed.searchParams.get('project_url') || null

    const params: Record<string, string> = {}
    parsed.searchParams.forEach((value, key) => {
      params[key] = value
    })

    return { mode, projectUrl, params }
  } catch {
    return null
  }
}

export function isTurboWarpUrl(url: string): boolean {
  try {
    const parsed = new URL(url)
    return parsed.hostname === 'turbowarp.org' || 
           parsed.hostname.endsWith('.turbowarp.org')
  } catch {
    return false
  }
}
