<template>
  <view class="page">
    <view v-if="loading" class="sk-pane">
      <view class="sk-line" style="width: 60%; height: 46rpx;"></view>
      <view class="sk-line" style="height: 260rpx; margin-top: 32rpx;"></view>
      <view class="sk-line" style="width: 80%; height: 32rpx; margin-top: 24rpx;"></view>
    </view>
    <view v-else-if="error" class="empty-box">
      <view class="eb-ic">!</view>
      <text class="eb-text">政策加载失败</text>
      <view class="eb-btn" @click="load">重新加载</view>
    </view>
    <template v-else-if="policy">
      <view class="head card">
        <text class="title">{{ policy.policyTitle }}</text>
        <text class="time">{{ short(policy.createTime) }}</text>
      </view>

      <view class="reason card">
        <text class="rl">匹配原因</text>
        <text class="rt">{{ policy.matchReason }}</text>
      </view>

      <view class="section-title">政策内容</view>
      <view class="content card">{{ policy.policyContent }}</view>

      <view class="bottom-btn" hover-class="u-hover" @click="back">返回政策列表</view>
    </template>
  </view>
</template>

<script>
import { fetchPolicy, markPolicyRead } from '../../api/index.js'

export default {
  data() {
    return { id: 0, policy: null, loading: true, error: false }
  },
  onLoad(options) {
    this.id = Number(options.id)
    // 阅读即已读
    markPolicyRead(this.id).catch(() => {})
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      this.error = false
      try {
        this.policy = await fetchPolicy(this.id)
      } catch (e) {
        this.error = true
      } finally {
        this.loading = false
      }
    },
    short(t) {
      return t ? String(t).substring(0, 10) : ''
    },
    back() {
      uni.navigateBack()
    }
  }
}
</script>

<style scoped>
.page {
  padding: 20rpx;
  padding-bottom: 60rpx;
}
.sk-pane {
  padding: 40rpx 24rpx;
}
.head {
  display: flex;
  flex-direction: column;
  padding: 40rpx 32rpx;
  border-left: 8rpx solid var(--c-primary);
}
.title {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--t1);
  line-height: 1.4;
}
.time {
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 18rpx;
}
.reason {
  display: flex;
  align-items: flex-start;
  background: var(--c-soft2);
  box-shadow: none;
}
.rl {
  color: var(--c-primary);
  background: var(--c-soft);
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  margin-right: 16rpx;
  align-self: flex-start;
}
.rt {
  font-size: 24rpx;
  color: var(--t2);
  line-height: 1.6;
  flex: 1;
}
.content {
  font-size: 32rpx;
  color: var(--t1);
  line-height: 1.9;
  white-space: pre-wrap;
}
.bottom-btn {
  background: var(--grad-primary);
  color: #fff;
  text-align: center;
  padding: 26rpx;
  border-radius: var(--r-pill);
  font-size: 32rpx;
  font-weight: 600;
  margin-top: 40rpx;
  box-shadow: var(--shadow-btn);
}
</style>
