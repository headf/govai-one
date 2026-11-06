<template>
  <view class="page">
    <view class="head card">
      <text class="big">政策推荐</text>
      <text class="sub">根据您的信息为您智能匹配相关惠民政策</text>
    </view>

    <view v-if="list.length" class="rows card">
      <view v-for="p in list" :key="p.id" class="row-item" hover-class="u-hover" @click="go(p.id)">
        <view class="dot" :class="{ unread: p.isRead === 0 }"></view>
        <view class="row-main">
          <view class="row-top">
            <text class="title" :class="{ bold: p.isRead === 0 }">{{ p.policyTitle }}</text>
            <text class="time">{{ short(p.createTime) }}</text>
          </view>
          <view class="content">{{ p.policyContent }}</view>
          <view class="reason"><text class="rl">匹配原因</text><text class="rt">{{ p.matchReason }}</text></view>
        </view>
        <text class="chev">›</text>
      </view>
    </view>
    <view v-else class="empty-box">
      <text class="eb-text">暂无推荐政策</text>
      <view class="eb-btn" hover-class="u-hover" @click="goProfile">完善实名信息，获取精准推荐</view>
    </view>
  </view>
</template>

<script>
import { fetchPolicies } from '../../api/index.js'

export default {
  data() {
    return { list: [] }
  },
  onShow() {
    this.load()
  },
  onPullDownRefresh() {
    this.load().finally(() => uni.stopPullDownRefresh())
  },
  methods: {
    async load() {
      try {
        this.list = await fetchPolicies()
      } catch (e) { /* ignore */ }
    },
    short(t) {
      return t ? String(t).substring(0, 10) : ''
    },
    go(id) {
      uni.navigateTo({ url: '/pages/policy/detail?id=' + id })
    },
    goProfile() {
      uni.navigateTo({ url: '/pages/mine/profile' })
    }
  }
}
</script>

<style scoped>
.page {
  padding: 20rpx;
  padding-bottom: 60rpx;
}
.head {
  background: var(--grad-primary);
  display: flex;
  flex-direction: column;
  padding: 44rpx 36rpx;
  box-shadow: none;
  position: relative;
  overflow: hidden;
}
.head::after {
  content: '';
  position: absolute;
  right: -60rpx;
  top: -80rpx;
  width: 300rpx;
  height: 300rpx;
  border-radius: 50%;
  background: radial-gradient(rgba(255, 255, 255, 0.16), transparent 68%);
}
.big {
  color: #fff;
  font-size: 40rpx;
  font-weight: 700;
  position: relative;
}
.sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: 24rpx;
  margin-top: 14rpx;
  position: relative;
}
.rows {
  padding: 4rpx 24rpx;
  margin-top: 20rpx;
}
.row-item {
  display: flex;
  align-items: flex-start;
  padding: 26rpx 0;
  border-bottom: 1rpx solid var(--c-div);
}
.row-item:last-child {
  border-bottom: none;
}
.row-main {
  flex: 1;
  min-width: 0;
}
.row-top {
  display: flex;
  align-items: center;
}
.dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  margin: 10rpx 16rpx 0 0;
  flex-shrink: 0;
  background: transparent;
}
.dot.unread {
  background: var(--c-primary);
}
.title {
  flex: 1;
  font-size: 28rpx;
  color: var(--t2);
  line-height: 1.5;
}
.title.bold {
  font-weight: 600;
  color: var(--t1);
}
.time {
  font-size: 24rpx;
  color: var(--t4);
  margin-left: 12rpx;
  flex-shrink: 0;
}
.reason {
  margin-top: 14rpx;
  display: flex;
  background: var(--c-soft2);
  border-radius: 12rpx;
  padding: 14rpx 18rpx;
}
.rl {
  color: var(--c-primary);
  background: var(--c-soft);
  font-size: 24rpx;
  padding: 4rpx 14rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  margin-right: 14rpx;
  align-self: flex-start;
}
.rt {
  font-size: 24rpx;
  color: var(--t3);
  line-height: 1.6;
}
.content {
  font-size: 24rpx;
  color: var(--t3);
  line-height: 1.6;
  margin-top: 10rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.chev {
  flex-shrink: 0;
  margin-left: 10rpx;
  color: var(--t4);
  font-size: 32rpx;
  font-weight: 300;
}

</style>
