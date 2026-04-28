import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import { get } from '@/api/request'

export const useUserStore = defineStore('user', () => {
  // 使用 sessionStorage 替代 localStorage：
  // - 关闭标签页自动清除 Token，降低 XSS 窃取风险
  // - 页面刷新时仍可从 sessionStorage 恢复
  const token = ref<string | null>(sessionStorage.getItem('token'))
  const refreshToken = ref<string | null>(sessionStorage.getItem('refreshToken'))
  let parsedUser: User | null = null
  try {
    parsedUser = JSON.parse(sessionStorage.getItem('user') || 'null')
  } catch {
    // sessionStorage 被污染时静默降级
    sessionStorage.removeItem('user')
  }
  const user = ref<User | null>(parsedUser)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isTeacher = computed(() => user.value?.role === 'TEACHER' || user.value?.role === 'ADMIN')

  function setAuth(newToken: string, newUser: User, newRefreshToken?: string) {
    token.value = newToken
    user.value = newUser
    try {
      sessionStorage.setItem('token', newToken)
      sessionStorage.setItem('user', JSON.stringify(newUser))
      if (newRefreshToken) {
        refreshToken.value = newRefreshToken
        sessionStorage.setItem('refreshToken', newRefreshToken)
      }
    } catch {
      // sessionStorage 满或不可用时静默降级
    }
  }

  function setToken(newToken: string, newRefreshToken?: string) {
    token.value = newToken
    try {
      sessionStorage.setItem('token', newToken)
      if (newRefreshToken) {
        refreshToken.value = newRefreshToken
        sessionStorage.setItem('refreshToken', newRefreshToken)
      }
    } catch {
      // sessionStorage 满或不可用时静默降级
    }
  }

  function logout() {
    token.value = null
    refreshToken.value = null
    user.value = null
    try {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('refreshToken')
      sessionStorage.removeItem('user')
    } catch {
      // sessionStorage 不可用时静默降级
    }
  }

  /**
   * 验证已存储的 Token 是否有效
   * 在 store 初始化时调用，如果 token 无效则清除
   */
  async function validateToken(): Promise<boolean> {
    if (!token.value) {
      return false
    }
    try {
      const res = await get<User>('/user/me')
      if (res.code === 0 && res.data) {
        user.value = res.data
        try {
          sessionStorage.setItem('user', JSON.stringify(res.data))
        } catch {
          // sessionStorage 满或不可用时静默降级
        }
        return true
      }
    } catch {
      // Token 无效
    }
    // 验证失败，清除所有认证信息
    logout()
    return false
  }

  return {
    token, refreshToken, user,
    isLoggedIn, isAdmin, isTeacher,
    setAuth, setToken, logout, validateToken
  }
})
