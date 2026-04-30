/**
 * TurboWarp 配置文件
 * 集中管理 TurboWarp 相关的 URL 和配置
 */

export interface TurboWarpConfig {
  mode: 'local' | 'remote'
  editorUrl: string
  playerUrl: string
  cdnUrl: string
}

const getConfig = (): TurboWarpConfig => {
  const isProduction = import.meta.env.PROD
  const localBase = '/turbowarp'

  if (isProduction) {
    return {
      mode: 'local',
      editorUrl: `${localBase}/editor-iframe.html`,
      playerUrl: `${localBase}/player-iframe.html`,
      cdnUrl: localBase
    }
  }

  return {
    mode: 'remote',
    editorUrl: 'https://turbowarp.org/editor',
    playerUrl: 'https://turbowarp.org/',
    cdnUrl: 'https://turbowarp.org'
  }
}

export const twConfig = getConfig()

export const TURBOWARP_SETTINGS = {
  fps: 30,
  turbo: false,
  hqpen: false,
  autoplay: false,
  settingsButton: false
} as const

export function buildEditorUrl(projectUrl?: string): string {
  const base = twConfig.editorUrl
  if (!projectUrl) return base

  const url = new URL(base, window.location.origin)
  url.searchParams.set('project_url', projectUrl)
  return url.toString()
}

export function buildPlayerUrl(projectUrl: string, autoplay = false): string {
  const base = twConfig.playerUrl
  const url = new URL(base, window.location.origin)
  url.searchParams.set('project_url', projectUrl)
  if (autoplay) {
    url.searchParams.set('autoplay', 'true')
  }
  return url.toString()
}

export function getDownloadUrl(projectId: number): string {
  return `${window.location.origin}/api/v1/project/${projectId}/sb3/download`
}
