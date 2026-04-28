import { get, post } from './request'
import type { AiReview, PageResult } from '@/types'

export const aiReviewApi = {
  generate: (projectId: number) =>
    post<AiReview>(`/ai-review/project/${projectId}`),
  getLatest: (projectId: number) =>
    get<AiReview>(`/ai-review/project/${projectId}`),
  getHistory: (projectId: number, page = 1, size = 10) =>
    get<PageResult<AiReview>>(`/ai-review/project/${projectId}/history`, { page, size }),
  getSseToken: () =>
    get<string>('/ai-review/sse-token'),
  stream: async (projectId: number, callbacks: {
    onToken?: (token: string) => void
    onComplete?: (review: AiReview) => void
    onError?: (error: string) => void
  }): Promise<EventSource> => {
    let sseToken: string
    try {
      const res = await get<string>('/ai-review/sse-token')
      if (res.code !== 0 || !res.data) {
        callbacks.onError?.('获取 SSE Token 失败')
        throw new Error('获取 SSE Token 失败')
      }
      sseToken = res.data
    } catch {
      callbacks.onError?.('获取 SSE Token 失败')
      throw new Error('获取 SSE Token 失败')
    }

    const url = `/api/v1/ai-review/project/${projectId}/stream?sse_token=${encodeURIComponent(sseToken)}`
    const es = new EventSource(url)

    es.addEventListener('token', (e) => {
      callbacks.onToken?.(e.data)
    })

    es.addEventListener('error', (e) => {
      callbacks.onError?.((e as MessageEvent).data || '服务端错误')
      es.close()
    })

    es.addEventListener('complete', (e) => {
      try {
        const review = JSON.parse(e.data) as AiReview
        callbacks.onComplete?.(review)
      } catch {
        callbacks.onError?.('解析点评结果失败')
      }
      es.close()
    })

    es.onerror = () => {
      callbacks.onError?.('连接中断')
      es.close()
    }

    return es
  }
}
