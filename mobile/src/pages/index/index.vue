<template>
  <view class="feed-page">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input" @click="goSearch">
        <text class="search-icon">🔍</text>
        <text class="search-placeholder">搜索作品或用户</text>
      </view>
    </view>

    <!-- Tab 切换 -->
    <view class="feed-tabs">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="currentTab = tab.value"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- 项目列表 -->
    <scroll-view
      class="feed-list"
      scroll-y
      @scrolltolower="loadMore"
      refresher-enabled
      @refresherrefresh="onRefresh"
      :refresher-triggered="refreshing"
    >
      <view
        v-for="item in feedList"
        :key="item.id"
        class="project-card"
        @click="goDetail(item.id)"
      >
        <!-- 封面图 -->
        <view class="card-cover">
          <image
            :src="item.coverUrl || '/static/default-cover.png'"
            mode="aspectFill"
            class="cover-img"
          />
        </view>

        <!-- 信息区 -->
        <view class="card-info">
          <text class="card-title">{{ item.title || '未命名项目' }}</text>
          <view class="card-meta">
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

      <!-- 加载状态 -->
      <view v-if="loading" class="loading">
        <text>加载中...</text>
      </view>
      <view v-if="noMore && feedList.length > 0" class="no-more">
        <text>没有更多了</text>
      </view>
      <view v-if="!loading && feedList.length === 0" class="empty-state">
        <text class="icon">📭</text>
        <text>暂无作品</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getFeed } from '@/api/index'

export default {
  data() {
    return {
      tabs: [
        { label: '推荐', value: 'recommend' },
        { label: '最新', value: 'latest' },
        { label: '热门', value: 'hot' },
      ],
      currentTab: 'recommend',
      feedList: [],
      page: 1,
      loading: false,
      refreshing: false,
      noMore: false,
    }
  },
  watch: {
    currentTab() {
      this.page = 1
      this.feedList = []
      this.noMore = false
      this.loadFeed()
    },
  },
  onShow() {
    this.loadFeed()
  },
  methods: {
    async loadFeed() {
      if (this.loading) return
      this.loading = true

      try {
        const res = await getFeed(this.page, 20)
        const list = res.data?.records || res.data || []
        if (this.page === 1) {
          this.feedList = list
        } else {
          this.feedList = [...this.feedList, ...list]
        }
        this.noMore = list.length < 20
      } catch (err) {
        console.error('加载 Feed 失败:', err)
      } finally {
        this.loading = false
        this.refreshing = false
      }
    },
    loadMore() {
      if (this.noMore || this.loading) return
      this.page++
      this.loadFeed()
    },
    onRefresh() {
      this.refreshing = true
      this.page = 1
      this.noMore = false
      this.loadFeed()
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/project/detail?id=${id}` })
    },
    goUser(id) {
      uni.navigateTo({ url: `/pages/user/profile?id=${id}` })
    },
    goSearch() {
      uni.navigateTo({ url: '/pages/index/search' })
    },
  },
}
</script>

<style scoped>
.feed-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.search-bar {
  padding: 16rpx 24rpx;
  background: #fff;
}

.search-input {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 24rpx;
  background: #f5f5f5;
  border-radius: 40rpx;
}

.search-icon {
  font-size: 28rpx;
}

.search-placeholder {
  color: #999;
  font-size: 26rpx;
}

.feed-tabs {
  display: flex;
  background: #fff;
  padding: 0 24rpx;
  border-bottom: 1rpx solid #eee;
}

.tab-item {
  padding: 20rpx 32rpx;
  font-size: 28rpx;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #4F46E5;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 4rpx;
  background: #4F46E5;
  border-radius: 2rpx;
}

.feed-list {
  flex: 1;
  padding: 16rpx 24rpx;
}

.project-card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.card-cover {
  width: 100%;
  height: 360rpx;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.card-info {
  padding: 20rpx 24rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 12rpx;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
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

.loading,
.no-more,
.empty-state {
  text-align: center;
  padding: 40rpx;
  color: #999;
  font-size: 24rpx;
}
</style>
