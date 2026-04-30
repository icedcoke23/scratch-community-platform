import { post, get, del } from './request'

export interface OAuthLoginParams {
  provider: string
  code: string
  encryptedData?: string
  iv?: string
}

export interface OAuthCallbackResult {
  token: string
  newUser: boolean
  userInfo: {
    id: number
    username: string
    nickname: string
    avatarUrl?: string
    role: string
  }
}

export interface OAuthBinding {
  provider: string
  nickname?: string
  avatarUrl?: string
  createdAt: string
}

/**
 * 第三方登录
 */
export const oauthLogin = (params: OAuthLoginParams) =>
  post<OAuthCallbackResult>('/oauth/login', params)

/**
 * 绑定第三方账号
 */
export const bindOAuth = (params: OAuthLoginParams) =>
  post('/oauth/bind', params)

/**
 * 解绑第三方账号
 */
export const unbindOAuth = (provider: string) =>
  del(`/oauth/bind/${provider}`)

/**
 * 获取绑定列表
 */
export const getOAuthBindings = () =>
  get<OAuthBinding[]>('/oauth/bindings')
