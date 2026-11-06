<template>
  <view class="page" v-if="msg">
    <view class="head card">
      <text class="tag">{{ typeText }}</text>
      <text class="title">{{ msg.title }}</text>
      <text class="meta">{{ full(msg.createTime) }}</text>
    </view>
    <view class="body card">{{ msg.content }}</view>
    <view v-if="msg.recordId" class="go card" @click="goRecord">
      <text>查看关联办件</text>
      <text class="arrow">›</text>
    </view>
  </view>
</template>

<script>
import { fetchMessageDetail } from '../../api/index.js'
import { formatTime } from '../../utils/format.js'

export default {
  data() {
    return { id: 0, msg: null }
  },
  computed: {
    typeText() {
      if (!this.msg) return ''
      return this.msg.messageType === 'APPROVAL' ? '办件通知'
        : this.msg.messageType === 'POLICY' ? '政策推送'
        : '系统消息'
    }
  },
  onLoad(options) {
    this.id = Number(options.id)
    this.load()
  },
  methods: {
    async load() {
      try {
        this.msg = await fetchMessageDetail(this.id)
      } catch (e) { /* ignore */ }
    },
    full(t) {
      return formatTime(t)
    },
    goRecord() {
      uni.navigateTo({ url: '/pages/record/detail?id=' + this.msg.recordId })
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
  display: flex;
  flex-direction: column;
}
.tag {
  align-self: flex-start;
  font-size: 24rpx;
  color: var(--c-primary);
  background: var(--c-soft);
  padding: 6rpx 18rpx;
  border-radius: 12rpx;
}
.title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--t1);
  margin-top: 20rpx;
  line-height: 1.4;
}
.meta {
  font-size: 24rpx;
  color: var(--t4);
  margin-top: 16rpx;
}
.body {
  font-size: 28rpx;
  color: var(--t2);
  line-height: 1.9;
}
.go {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-primary);
  font-size: 28rpx;
  padding: 26rpx;
  background: var(--c-soft2);
  border: 1rpx solid var(--c-line);
  box-shadow: none;
}
.go .arrow {
  font-size: 32rpx;
  margin-left: 8rpx;
  font-weight: 300;
}
</style>
