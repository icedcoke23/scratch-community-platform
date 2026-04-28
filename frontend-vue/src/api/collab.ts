import { get, post } from './request'
import type { ApiResponse } from '@/types'

export interface CollabSession {
  id: number
  projectId: number
  ownerId: number
  status: string
  maxEditors: number
  createdAt: string
}

export interface CollabParticipant {
  userId: number
  nickname: string
  avatarUrl?: string
  role: 'editor' | 'viewer'
  cursorX: number
  cursorY: number
}

export interface CollabSessionState {
  sessionId: number
  projectId: number
  ownerId: number
  status: string
  version: number
  participants: CollabParticipant[]
  recentOperations: EditOperation[]
}

export interface EditOperation {
  type: string
  targetId: string
  data: Record<string, unknown>
  version: number
  timestamp: number
}

export interface CollabEvent {
  type: string
  sessionId: number
  userId: number
  nickname: string
  payload: Record<string, unknown>
  timestamp: number
}

/** 创建协作会话 */
export const createCollabSession = (projectId: number) =>
  post<CollabSession>('/collab/session', undefined, { params: { projectId } })

/** 获取会话状态 */
export const getCollabSessionState = (sessionId: number) =>
  get<CollabSessionState>(`/collab/session/${sessionId}`)

/** 获取项目活跃会话 */
export const getActiveCollabSession = (projectId: number) =>
  get<CollabSession>(`/collab/project/${projectId}`)

/** 关闭协作会话 */
export const closeCollabSession = (sessionId: number) =>
  post(`/collab/session/${sessionId}/close`)
