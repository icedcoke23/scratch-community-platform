/**
 * API 请求封装
 * 复用 web 端的接口路径和数据结构
 */

const BASE_URL = process.env.NODE_ENV === 'development'
  ? 'http://localhost:8080/api/v1'
  : '/api/v1'

/**
 * 封装 uni.request
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header,
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const data = res.data
          if (data.code === 0) {
            resolve(data)
          } else if (data.code === 9997) {
            // Token 过期
            uni.removeStorageSync('token')
            uni.removeStorageSync('user')
            uni.navigateTo({ url: '/pages/user/profile' })
            reject(new Error('登录已过期'))
          } else {
            uni.showToast({ title: data.msg || '请求失败', icon: 'none' })
            reject(new Error(data.msg))
          }
        } else if (res.statusCode === 429) {
          uni.showToast({ title: '请求过于频繁', icon: 'none' })
          reject(new Error('rate limit'))
        } else {
          reject(new Error(`HTTP ${res.statusCode}`))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      },
    })
  })
}

// ==================== 社区 API ====================

/** 获取 Feed 列表 */
export const getFeed = (page = 1, size = 20) =>
  request({ url: '/social/feed', data: { page, size } })

/** 获取排行榜 */
export const getRank = (type = 'points', page = 1, size = 20) =>
  request({ url: '/social/rank', data: { type, page, size } })

/** 获取项目详情 */
export const getProject = (id) =>
  request({ url: `/project/${id}` })

/** 点赞项目 */
export const likeProject = (id) =>
  request({ url: `/social/like/${id}`, method: 'POST' })

/** 取消点赞 */
export const unlikeProject = (id) =>
  request({ url: `/social/like/${id}`, method: 'DELETE' })

/** 获取评论列表 */
export const getComments = (targetId, page = 1, size = 20) =>
  request({ url: '/social/comment', data: { targetId, page, size } })

/** 发表评论 */
export const postComment = (targetId, content) =>
  request({ url: '/social/comment', method: 'POST', data: { targetId, content } })

// ==================== 用户 API ====================

/** 用户登录 */
export const login = (username, password) =>
  request({ url: '/user/login', method: 'POST', data: { username, password } })

/** 用户注册 */
export const register = (data) =>
  request({ url: '/user/register', method: 'POST', data })

/** 获取当前用户信息 */
export const getMyInfo = () =>
  request({ url: '/user/me' })

/** 获取用户主页 */
export const getUserProfile = (id) =>
  request({ url: `/user/${id}/profile` })

// ==================== 搜索 API ====================

/** 搜索项目 */
export const searchProjects = (keyword, page = 1, size = 20) =>
  request({ url: '/social/search', data: { keyword, page, size } })

/** 搜索用户 */
export const searchUsers = (q, page = 1, size = 20) =>
  request({ url: '/user/search', data: { q, page, size } })

// ==================== 平台统计 ====================

/** 获取平台统计 */
export const getStats = () =>
  request({ url: '/stats' })

/** 健康检查 */
export const healthCheck = () =>
  request({ url: '/health' })
