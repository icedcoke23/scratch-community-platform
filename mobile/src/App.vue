<script>
export default {
  onLaunch() {
    console.log('App Launch')
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  },
}
</script>

<style>
/* 全局样式 */
page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px;
  color: #333;
  background-color: #f5f5f5;
}

.container {
  padding: 24rpx;
}

/* 通用卡片样式 */
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

/* 通用按钮 */
.btn-primary {
  background: #4F46E5;
  color: #fff;
  border-radius: 12rpx;
  padding: 20rpx 40rpx;
  text-align: center;
  font-size: 28rpx;
  border: none;
}

.btn-primary:active {
  opacity: 0.8;
}

/* 头像 */
.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  overflow: hidden;
}

.avatar image {
  width: 100%;
  height: 100%;
}

/* 徽章 */
.badge {
  display: inline-block;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  font-size: 20rpx;
  background: #4F46E5;
  color: #fff;
}

/* 分割线 */
.divider {
  height: 1rpx;
  background: #eee;
  margin: 20rpx 0;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80rpx 40rpx;
  color: #999;
}

.empty-state .icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}
</style>
