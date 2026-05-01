<template>
  <view class="search-page">
    <!-- 搜索栏 -->
    <view class="search-header">
      <view class="search-bar">
        <text class="search-icon">🔍</text>
        <input
          v-model="searchKeyword"
          class="search-input"
          placeholder="搜索作品或用户"
          confirm-type="search"
          @confirm="handleSearch"
          focus
        />
        <text v-if="searchKeyword" class="clear-icon" @click="clearSearch">✕</text>
      </view>
      <text class="cancel-btn" @click="goBack">取消</text>
    </view>

    <!-- 搜索建议/历史 -->
    <view v-if="!hasSearched" class="search-section">
      <!-- 搜索历史 -->
      <view v-if="searchHistory.length > 0" class="history-section">
        <view class="section-header">
          <text class="section-title">搜索历史</text>
          <text class="clear-history" @click="clearHistory">清空</text>
        </view>
        <view class="history-tags">
          <view
            v-for="(keyword, index) in searchHistory"
            :key="index"
            class="history-tag"
            @click="useHistory(keyword)"
          >
            {{ keyword }}
          </view>
        </view>
      </view>

      <!-- 热门搜索 -->
      <view class="hot-section">
        <view class="section-header">
          <text class="section-title">🔥 热门搜索</text>
        </view>
        <view class="hot-list">
          <view
            v-for="(item, index) in hotSearchList"
            :key="index"
            class="hot-item"
            @click="useHistory(item.keyword)"
          >
            <text class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</text>
            <text class="hot-keyword">{{ item.keyword }}</text>
            <text v-if="item.hot" class="hot-tag">热</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 搜索结果 -->
    <view v-else class="search-results">
      <!-- 加载状态 -->
      <view v-if="loading" class="loading-state">
        <text>搜索中...</text>
      </view>

      <!-- 无结果 -->
      <view v-else-if="searchResults.length === 0" class="empty-state">
        <text class="empty-icon">🔍</text>
        <text class="empty-text">未找到相关作品或用户</text>
        <text class="empty-hint">换个关键词试试吧</text>
      </view>

      <!-- 结果列表 -->
      <view v-else class="results-list">
        <!-- 结果统计 -->
        <view class="results-header">
          <text class="results-count">找到 {{ searchResults.length }} 个结果</text>
        </view>

        <!-- 项目列表 -->
        <view
          v-for="item in searchResults"
          :key="item.id"
          class="result-card"
          @click="goDetail(item.id)"
        >
          <image
            :src="item.coverUrl || '/static/default-cover.png'"
            class="result-cover"
            mode="aspectFill"
          />
          <view class="result-info">
            <text class="result-title">{{ item.title || '未命名项目' }}</text>
            <view class="result-meta">
              <view class="author" @click.stop="goUser(item.authorId)">
                <image
                  :src="item.authorAvatar || '/static/default-avatar.png'"
                  class="author-avatar"
                />
                <text class="author-name">{{ item.authorNickname || '匿名用户' }}</text>
              </view>
              <view class="stats">
                <text class="stat-item">❤️ {{ item.likeCount || 0 }}</text>
                <text class="stat-item">💬 {{ item.commentCount || 0 }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { searchProjects } from '@/api/index'

export default {
  data() {
    return {
      searchKeyword: '',
      searchHistory: [],
      hotSearchList: [
        { keyword: 'Scratch游戏', hot: true },
        { keyword: '动画制作', hot: true },
        { keyword: '编程入门', hot: false },
        { keyword: '角色设计', hot: false },
        { keyword: '音乐创作', hot: false },
        { keyword: '互动故事', hot: false },
      ],
      searchResults: [],
      loading: false,
      hasSearched: false,
    }
  },
  onLoad() {
    this.loadHistory()
  },
  methods: {
    handleSearch() {
      if (!this.searchKeyword.trim()) {
        uni.showToast({
          title: '请输入搜索关键词',
          icon: 'none'
        })
        return
      }
      this.performSearch(this.searchKeyword.trim())
    },
    async performSearch(keyword) {
      this.loading = true
      this.hasSearched = true

      try {
        const res = await searchProjects(keyword)
        this.searchResults = res.data?.records || res.data || []

        // 保存搜索历史
        this.saveToHistory(keyword)
      } catch (err) {
        console.error('搜索失败:', err)
        uni.showToast({
          title: '搜索失败，请重试',
          icon: 'none'
        })
        this.searchResults = []
      } finally {
        this.loading = false
      }
    },
    saveToHistory(keyword) {
      let history = uni.getStorageSync('searchHistory') || []
      history = history.filter(item => item !== keyword)
      history.unshift(keyword)
      history = history.slice(0, 10) // 最多保留10条
      uni.setStorageSync('searchHistory', history)
      this.searchHistory = history
    },
    loadHistory() {
      this.searchHistory = uni.getStorageSync('searchHistory') || []
    },
    clearHistory() {
      uni.showModal({
        title: '提示',
        content: '确定清空搜索历史？',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('searchHistory')
            this.searchHistory = []
            uni.showToast({
              title: '已清空',
              icon: 'success'
            })
          }
        }
      })
    },
    useHistory(keyword) {
      this.searchKeyword = keyword
      this.performSearch(keyword)
    },
    clearSearch() {
      this.searchKeyword = ''
      this.searchResults = []
      this.hasSearched = false
    },
    goBack() {
      uni.navigateBack()
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/project/detail?id=${id}` })
    },
    goUser(id) {
      uni.navigateTo({ url: `/pages/user/profile?id=${id}` })
    }
  }
}
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.search-header {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #fff;
  gap: 16rpx;
}

.search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #f5f5f5;
  border-radius: 40rpx;
  gap: 12rpx;
}

.search-icon {
  font-size: 28rpx;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  height: 40rpx;
}

.clear-icon {
  font-size: 24rpx;
  color: #999;
  padding: 8rpx;
  flex-shrink: 0;
}

.cancel-btn {
  font-size: 28rpx;
  color: #4F46E5;
  flex-shrink: 0;
}

.search-section {
  padding: 24rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.clear-history {
  font-size: 24rpx;
  color: #999;
}

.history-section {
  margin-bottom: 40rpx;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.history-tag {
  padding: 12rpx 24rpx;
  background: #fff;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.hot-item {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #fff;
  border-radius: 12rpx;
  gap: 16rpx;
}

.hot-rank {
  width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
  color: #999;
  background: #f5f5f5;
  border-radius: 8rpx;
  flex-shrink: 0;
}

.hot-rank.top {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
  color: #fff;
}

.hot-keyword {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.hot-tag {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
  color: #fff;
  border-radius: 8rpx;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
  text-align: center;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 32rpx;
}

.empty-text {
  font-size: 32rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.empty-hint {
  font-size: 26rpx;
  color: #999;
}

.results-list {
  padding: 24rpx;
}

.results-header {
  margin-bottom: 20rpx;
}

.results-count {
  font-size: 26rpx;
  color: #666;
}

.result-card {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.result-cover {
  width: 200rpx;
  height: 200rpx;
  flex-shrink: 0;
}

.result-info {
  flex: 1;
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.result-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.author {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.author-avatar {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
}

.author-name {
  font-size: 24rpx;
  color: #666;
}

.stats {
  display: flex;
  gap: 20rpx;
}

.stat-item {
  font-size: 22rpx;
  color: #999;
}
</style>
