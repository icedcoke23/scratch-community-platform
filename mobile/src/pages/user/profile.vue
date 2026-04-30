<template>
  <view class="profile-page">
    <!-- 未登录状态 -->
    <view v-if="!isLoggedIn" class="login-card card">
      <text class="login-title">登录 Scratch 社区</text>
      <text class="login-desc">创作、分享、学习</text>
      <view class="login-form">
        <input v-model="loginForm.username" placeholder="用户名" class="login-input" />
        <input v-model="loginForm.password" placeholder="密码" type="password" class="login-input" />
        <button class="btn-primary" @click="handleLogin">登录</button>
      </view>
    </view>

    <!-- 已登录状态 -->
    <view v-else>
      <!-- 用户卡片 -->
      <view class="user-card card">
        <view class="user-header">
          <image
            :src="user.avatarUrl || '/static/default-avatar.png'"
            class="user-avatar"
          />
          <view class="user-info">
            <text class="user-nickname">{{ user.nickname }}</text>
            <text class="user-username">@{{ user.username }}</text>
            <view class="user-level">
              <text class="badge">Lv.{{ user.level || 1 }}</text>
              <text class="user-points">{{ user.points || 0 }} 积分</text>
            </view>
          </view>
        </view>
        <text v-if="user.bio" class="user-bio">{{ user.bio }}</text>
      </view>

      <!-- 统计 -->
      <view class="stats-row">
        <view class="stat-box card">
          <text class="stat-num">{{ user.projectCount || 0 }}</text>
          <text class="stat-label">作品</text>
        </view>
        <view class="stat-box card">
          <text class="stat-num">{{ user.followerCount || 0 }}</text>
          <text class="stat-label">粉丝</text>
        </view>
        <view class="stat-box card">
          <text class="stat-num">{{ user.followingCount || 0 }}</text>
          <text class="stat-label">关注</text>
        </view>
      </view>

      <!-- 功能列表 -->
      <view class="menu-list card">
        <view class="menu-item" @click="goPage('/pages/user/works')">
          <text>🎨 我的作品</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goPage('/pages/user/likes')">
          <text>❤️ 我的点赞</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goPage('/pages/user/achievements')">
          <text>🏅 成就系统</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="goPage('/pages/user/settings')">
          <text>⚙️ 设置</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<script>
import { login, getMyInfo } from '@/api/index'

export default {
  data() {
    return {
      isLoggedIn: false,
      user: {},
      loginForm: {
        username: '',
        password: '',
      },
    }
  },
  onShow() {
    this.checkLogin()
  },
  methods: {
    checkLogin() {
      const token = uni.getStorageSync('token')
      if (token) {
        this.isLoggedIn = true
        this.loadUserInfo()
      }
    },
    async loadUserInfo() {
      try {
        const res = await getMyInfo()
        this.user = res.data || {}
      } catch (err) {
        console.error('获取用户信息失败:', err)
      }
    },
    async handleLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
        return
      }
      try {
        const res = await login(this.loginForm.username, this.loginForm.password)
        const { token, userInfo } = res.data
        uni.setStorageSync('token', token)
        uni.setStorageSync('user', JSON.stringify(userInfo))
        this.isLoggedIn = true
        this.user = userInfo
        uni.showToast({ title: '登录成功', icon: 'success' })
      } catch (err) {
        console.error('登录失败:', err)
      }
    },
    handleLogout() {
      uni.showModal({
        title: '确认退出',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('token')
            uni.removeStorageSync('user')
            this.isLoggedIn = false
            this.user = {}
            uni.showToast({ title: '已退出', icon: 'success' })
          }
        },
      })
    },
    goPage(url) {
      uni.navigateTo({ url })
    },
  },
}
</script>

<style scoped>
.profile-page {
  padding: 24rpx;
  min-height: 100vh;
}

.login-card {
  text-align: center;
  padding: 60rpx 40rpx;
}

.login-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 12rpx;
}

.login-desc {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 40rpx;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.login-input {
  padding: 24rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.user-card {
  padding: 32rpx;
}

.user-header {
  display: flex;
  gap: 24rpx;
}

.user-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
}

.user-info {
  flex: 1;
}

.user-nickname {
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
}

.user-username {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.user-level {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.user-points {
  font-size: 24rpx;
  color: #f5a623;
}

.user-bio {
  font-size: 26rpx;
  color: #666;
  margin-top: 20rpx;
  line-height: 1.5;
}

.stats-row {
  display: flex;
  gap: 16rpx;
  margin: 20rpx 0;
}

.stat-box {
  flex: 1;
  text-align: center;
  padding: 20rpx;
}

.stat-num {
  font-size: 36rpx;
  font-weight: 700;
  color: #4F46E5;
  display: block;
}

.stat-label {
  font-size: 22rpx;
  color: #999;
}

.menu-list {
  padding: 0;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f5f5f5;
  font-size: 28rpx;
  color: #333;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-arrow {
  color: #ccc;
  font-size: 32rpx;
}

.logout-btn {
  margin-top: 40rpx;
  background: #fff;
  color: #f56c6c;
  border: 1rpx solid #f56c6c33;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 28rpx;
}
</style>
