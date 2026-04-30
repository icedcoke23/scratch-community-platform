export interface TurboWarpConfig {
  mode: 'local' | 'remote'
  editorUrl: string
  playerUrl: string
}

const getConfig = (): TurboWarpConfig => {
  const isProduction = import.meta.env?.PROD ?? false
  const localBase = '/turbowarp'

  return {
    mode: 'local',
    editorUrl: `${localBase}/editor.html`,
    playerUrl: `${localBase}/player.html`
  }
}

export const twConfig = getConfig()

export function buildEditorUrl(projectUrl?: string): string {
  const base = twConfig.editorUrl
  if (!projectUrl) return base

  const url = new URL(base, window.location.origin)
  url.searchParams.set('project_url', projectUrl)
  return url.toString()
}

export function buildPlayerUrl(projectUrl: string): string {
  const base = twConfig.playerUrl
  const url = new URL(base, window.location.origin)
  url.searchParams.set('project_url', projectUrl)
  return url.toString()
}

export function getDownloadUrl(projectId: number): string {
  return `${window.location.origin}/api/v1/project/${projectId}/sb3/download`
}
