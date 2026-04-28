<template>
  <view class="detail-page">
    <!-- 项目预览 -->
    <view class="preview-area">
      <image
        :src="project.coverUrl || '/static/default-cover.png'"
        mode="aspectFill"
        class="preview-img"
      />
      <view class="play-btn" @click="playProject">
        <text class="play-icon">▶️</text>
      </view>
    </view>

    <!-- 项目信息 -->
    <view class="project-info card">
      <text class="project-title">{{ project.title || '未命名项目' }}</text>
      <text class="project-desc">{{ project.description || '暂无描述' }}</text>

      <!-- 作者 -->
      <view class="author-row" @click="goUser(project.authorId)">
        <image
          :src="project.authorAvatar || '/static/default-avatar.png'"
          class="author-avatar"
        />
        <text class="author-name">{{ project.authorNickname || '匿名用户' }}</text>
      </view>

      <!-- 互动区 -->
      <view class="action-bar">
        <view class="action-item" @click="toggleLike">
          <text :class="{ liked: project.isLiked }">{{ project.isLiked ? '❤️' : '🤍' }}</text>
          <text class="action-count">{{ project.likeCount || 0 }}</text>
        </view>
        <view class="action-item">
          <text>💬</text>
          <text class="action-count">{{ project.commentCount || 0 }}</text>
        </view>
        <view class="action-item" @click="shareProject">
          <text>📤</text>
          <text>分享</text>
        </view>
      </view>
    </view>

    <!-- 评论区 -->
    <view class="comments-section card">
      <text class="section-title">评论 ({{ comments.length }})</text>

      <view
        v-for="comment in comments"
        :key="comment.id"
        class="comment-item"
      >
        <image
          :src="comment.userAvatar || '/static/default-avatar.png'"
          class="comment-avatar"
        />
        <view class="comment-body">
          <text class="comment-author">{{ comment.userNickname }}</text>
          <text class="comment-text">{{ comment.content }}</text>
          <text class="comment-time">{{ formatTime(comment.createdAt) }}</text>
        </view>
      </view>

      <view v-if="comments.length === 0" class="empty-state">
        <text>暂无评论，快来抢沙发~</text>
      </view>
    </view>

    <!-- 评论输入 -->
    <view class="comment-input-bar">
      <input
        v-model="commentText"
        placeholder="写评论..."
        class="comment-input"
        @confirm="submitComment"
      />
      <button
        class="send-btn"
        :disabled="!commentText.trim()"
        @click="submitComment"
      >发送</button>
    </view>
  </view>
</template>

<script>
import { getProject, likeProject, unlikeProject, getComments, postComment } from '@/api/index'

export default {
  data() {
    return {
      projectId: null,
      project: {},
      comments: [],
      commentText: '',
    }
  },
  onLoad(options) {
    this.projectId = options.id
    this.loadProject()
    this.loadComments()
  },
  methods: {
    async loadProject() {
      try {
        const res = await getProject(this.projectId)
        this.project = res.data || {}
        uni.setNavigationBarTitle({ title: this.project.title || '作品详情' })
      } catch (err) {
        console.error('加载项目失败:', err)
      }
    },
    async loadComments() {
      try {
        const res = await getComments(this.projectId)
        this.comments = res.data?.records || res.data || []
      } catch (err) {
        console.error('加载评论失败:', err)
      }
    },
    async toggleLike() {
      try {
        if (this.project.isLiked) {
          await unlikeProject(this.projectId)
          this.project.isLiked = false
          this.project.likeCount = Math.max(0, (this.project.likeCount || 1) - 1)
        } else {
          await likeProject(this.projectId)
          this.project.isLiked = true
          this.project.likeCount = (this.project.likeCount || 0) + 1
        }
      } catch (err) {
        console.error('点赞失败:', err)
      }
    },
    async submitComment() {
      if (!this.commentText.trim()) return
      try {
        await postComment(this.projectId, this.commentText.trim())
        this.commentText = ''
        this.loadComments()
        uni.showToast({ title: '评论成功', icon: 'success' })
      } catch (err) {
        console.error('评论失败:', err)
      }
    },
    playProject() {
      // TODO: 打开 Scratch 预览
      uni.showToast({ title: '预览功能开发中', icon: 'none' })
    },
    shareProject() {
      uni.share({
        provider: 'weixin',
        scene: 'WXSceneSession',
        type: 0,
        title: this.project.title,
        imageUrl: this.project.coverUrl,
      })
    },
    goUser(id) {
      uni.navigateTo({ url: `/pages/user/profile?id=${id}` })
    },
    formatTime(str) {
      if (!str) return ''
      const d = new Date(str)
      return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${d.getMinutes().toString().padStart(2, '0')}`
    },
  },
}
</script>

<style scoped>
.detail-page {
  padding-bottom: 120rpx;
}

.preview-area {
  position: relative;
  width: 100%;
  height: 500rpx;
  background: #000;
}

.preview-img {
  width: 100%;
  height: 100%;
}

.play-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-icon {
  font-size: 48rpx;
}

.project-info {
  margin: -40rpx 24rpx 0;
  position: relative;
  z-index: 1;
}

.project-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 12rpx;
}

.project-desc {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 20rpx;
  line-height: 1.5;
}

.author-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.author-avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
}

.author-name {
  font-size: 28rpx;
  color: #333;
}

.action-bar {
  display: flex;
  justify-content: space-around;
  border-top: 1rpx solid #eee;
  padding-top: 20rpx;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 26rpx;
  color: #666;
}

.action-item .liked {
  color: #f56c6c;
}

.action-count {
  font-size: 24rpx;
}

.comments-section {
  margin: 20rpx 24rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
}

.comment-item {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.comment-avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
}

.comment-author {
  font-size: 24rpx;
  color: #4F46E5;
  margin-bottom: 6rpx;
}

.comment-text {
  font-size: 26rpx;
  color: #333;
  line-height: 1.5;
}

.comment-time {
  font-size: 20rpx;
  color: #999;
  margin-top: 6rpx;
}

.comment-input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #eee;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}

.comment-input {
  flex: 1;
  padding: 16rpx 24rpx;
  background: #f5f5f5;
  border-radius: 40rpx;
  font-size: 26rpx;
}

.send-btn {
  padding: 16rpx 32rpx;
  background: #4F46E5;
  color: #fff;
  border-radius: 40rpx;
  font-size: 26rpx;
  border: none;
}

.send-btn[disabled] {
  opacity: 0.5;
}
</style>
