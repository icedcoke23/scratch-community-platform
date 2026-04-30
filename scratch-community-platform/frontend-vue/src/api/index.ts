import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    ElMessage.error(error.message || '请求失败')
    return Promise.reject(error)
  }
)

export interface ApiResponse<T = any> {
  code: number
  data: T
  msg: string
}

export interface ProjectDetail {
  id: number
  title: string
  description?: string
  tags?: string
  status: 'draft' | 'published'
  blockCount?: number
  complexityScore?: number
  createdAt?: string
  sb3Url?: string
}

export const projectApi = {
  getDetail(id: number): Promise<ApiResponse<ProjectDetail>> {
    return request.get(`/v1/project/${id}`)
  },
  create(data: Partial<ProjectDetail>): Promise<ApiResponse<any>> {
    return request.post('/v1/project', data)
  },
  update(id: number, data: Partial<ProjectDetail>): Promise<ApiResponse<any>> {
    return request.put(`/v1/project/${id}`, data)
  },
  publish(id: number): Promise<ApiResponse<any>> {
    return request.post(`/v1/project/${id}/publish`)
  },
  uploadSb3(id: number, file: File): Promise<ApiResponse<any>> {
    const formData = new FormData()
    formData.append('file', file)
    return request.post(`/v1/project/${id}/sb3/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
