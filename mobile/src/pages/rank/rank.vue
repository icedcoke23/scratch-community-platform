<template>
  <view class="rank-page">
    <!-- 排行榜类型切换 -->
    <view class="rank-tabs">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- 排行列表 -->
    <scroll-view
      class="rank-list"
      scroll-y
      @scrolltolower="loadMore"
    >
      <!-- 前三名特殊展示 -->
      <view class="top3" v-if="rankList.length >= 3">
        <view class="top-item second" @click="goUser(rankList[1].userId)">
          <image :src="rankList[1].avatarUrl || '/static/default-avatar.png'" class="top-avatar" />
          <text class="top-rank">🥈</text>
          <text class="top-name">{{ rankList[1].nickname }}</text>
          <text class="top-score">{{ rankList[1].points || 0 }} 分</text>
        </view>
        <view class="top-item first" @click="goUser(rankList[0].userId)">
          <image :src="rankList[0].avatarUrl || '/static/default-avatar.png'" class="top-avatar" />
          <text class="top-rank">🥇</text>
          <text class="top-name">{{ rankList[0].nickname }}</text>
          <text class="top-score">{{ rankList[0].points || 0 }} 分</text>
        </view>
        <view class="top-item third" @click="goUser(rankList[2].userId)">
          <image :src="rankList[2].avatarUrl || '/static/default-avatar.png'" class="top-avatar" />
          <text class="top-rank">🥉</text>
          <text class="top-name">{{ rankList[2].nickname }}</text>
          <text class="top-score">{{ rankList[2].points || 0 }} 分</text>
        </view>
      </view>

      <!-- 其他排名 -->
      <view
        v-for="(item, index) in rankList.slice(3)"
        :key="item.userId"
        class="rank-item"
        @click="goUser(item.userId)"
      >
        <text class="rank-num">{{ index + 4 }}</text>
        <image :src="item.avatarUrl || '/static/default-avatar.png'" class="rank-avatar" />
        <view class="rank-info">
          <text class="rank-name">{{ item.nickname }}</text>
          <text class="rank-level">Lv.{{ item.level || 1 }}</text>
        </view>
        <text class="rank-score">{{ item.points || 0 }} 分</text>
      </view>

      <view v-if="loading" class="loading"><text>加载中...</text></view>
      <view v-if="!loading && rankList.length === 0" class="empty-state">
        <text class="icon">🏆</text>
        <text>暂无排行数据</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getRank } from '@/api/index'

export default {
  data() {
    return {
      tabs: [
        { label: '积分榜', value: 'points' },
        { label: '作品榜', value: 'projects' },
        { label: '点赞榜', value: 'likes' },
      ],
      currentTab: 'points',
      rankList: [],
      page: 1,
      loading: false,
      noMore: false,
    }
  },
  onShow() {
    this.loadRank()
  },
  methods: {
    switchTab(tab) {
      this.currentTab = tab
      this.page = 1
      this.rankList = []
      this.noMore = false
      this.loadRank()
    },
    async loadRank() {
      if (this.loading) return
      this.loading = true
      try {
        const res = await getRank(this.currentTab, this.page, 50)
        const list = res.data?.records || res.data || []
        if (this.page === 1) {
          this.rankList = list
        } else {
          this.rankList = [...this.rankList, ...list]
        }
        this.noMore = list.length < 50
      } catch (err) {
        console.error('加载排行榜失败:', err)
      } finally {
        this.loading = false
      }
    },
    loadMore() {
      if (this.noMore || this.loading) return
      this.page++
      this.loadRank()
    },
    goUser(id) {
      uni.navigateTo({ url: `/pages/user/profile?id=${id}` })
    },
  },
}
</script>

<style scoped>
.rank-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.rank-tabs {
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

.rank-list {
  flex: 1;
  padding: 24rpx;
}

.top3 {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 24rpx;
  padding: 40rpx 0;
  margin-bottom: 20rpx;
}

.top-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 180rpx;
}

.top-item.first {
  order: 1;
}

.top-item.second {
  order: 0;
}

.top-item.third {
  order: 2;
}

.top-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-bottom: 8rpx;
}

.top-item.first .top-avatar {
  width: 120rpx;
  height: 120rpx;
  border: 4rpx solid #ffd700;
}

.top-rank {
  font-size: 36rpx;
  margin-bottom: 4rpx;
}

.top-name {
  font-size: 24rpx;
  color: #333;
  margin-bottom: 4rpx;
  max-width: 160rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-score {
  font-size: 22rpx;
  color: #999;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  border-radius: 12rpx;
  margin-bottom: 12rpx;
}

.rank-num {
  width: 48rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  color: #999;
}

.rank-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
}

.rank-info {
  flex: 1;
}

.rank-name {
  font-size: 28rpx;
  color: #333;
}

.rank-level {
  font-size: 20rpx;
  color: #4F46E5;
  margin-left: 8rpx;
}

.rank-score {
  font-size: 28rpx;
  font-weight: 600;
  color: #f5a623;
}

.loading,
.empty-state {
  text-align: center;
  padding: 40rpx;
  color: #999;
  font-size: 24rpx;
}
</style>
