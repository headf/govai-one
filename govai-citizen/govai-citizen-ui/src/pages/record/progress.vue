<template>
  <view class="page">
    <view v-if="prog" class="head card">
      <text class="cur" :class="prog.status">{{ prog.currentNode }}</text>
      <text class="st">{{ statusText(prog.status) }}</text>
      <text v-if="prog.submitTime" class="sub">提交于 {{ fmt(prog.submitTime) }}</text>
      <view class="head-actions">
        <view class="ref-btn" hover-class="u-hover" @click="load">刷新进度</view>
        <view v-if="prog.status === 'APPROVED'" class="eval-btn" hover-class="u-hover" @click="goEval">去评价办件</view>
      </view>
    </view>

    <view class="section-title">办理进度</view>
    <view v-if="prog && prog.nodes" class="timeline card">
      <view v-for="(n, i) in prog.nodes" :key="i" class="tl-item" :class="n.status">
        <view class="tl-left">
          <view class="dot" :class="n.status"></view>
          <view v-if="i < prog.nodes.length - 1" class="line" :class="{ ok: n.status === 'DONE' }"></view>
        </view>
        <view class="tl-body">
          <view class="tl-name-row">
            <text class="tl-name">{{ n.name }}</text>
            <text class="tl-status" :class="n.status">{{ statusMap(n.status) }}</text>
          </view>
          <text v-if="n.assignee" class="tl-assignee">承办：{{ n.assignee }}</text>
          <text v-if="n.handleTime" class="tl-time">{{ fmt(n.handleTime) }}</text>
        </view>
      </view>
    </view>
    <view v-else class="empty-box"><text class="eb-text">暂无进度数据</text></view>
  </view>
</template>

<script>
import { fetchProgress } from '../../api/index.js'
import { statusText, formatTime } from '../../utils/format.js'

export default {
  data() {
    return { id: 0, prog: null, timer: null }
  },
  onLoad(options) {
    this.id = Number(options.id)
    this.load()
    // 每 15 秒自动刷新一次进度(办结后状态会回流)
    this.timer = setInterval(() => this.load(), 15000)
  },
  onShow() {
    this.load()
  },
  onUnload() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    async load() {
      try {
        this.prog = await fetchProgress(this.id)
      } catch (e) { /* ignore */ }
    },
    fmt(t) {
      return formatTime(t)
    },
    statusMap(s) {
      return { DONE: '已完成', CURRENT: '进行中', PENDING: '待处理' }[s] || s
    },
    goEval() {
      uni.navigateTo({ url: '/pages/evaluation/form?recordId=' + this.id })
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
  align-items: center;
  padding: 44rpx 30rpx;
  background: var(--c-soft2);
  border: 1rpx solid var(--c-line);
  box-shadow: none;
}
.cur {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--c-primary);
  text-align: center;
}
.cur.APPROVED { color: #2FA566; }
.cur.REJECTED { color: var(--c-danger); }
.st {
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 12rpx;
}
.sub {
  font-size: 24rpx;
  color: var(--t4);
  margin-top: 8rpx;
}
.head-actions {
  display: flex;
  margin-top: 24rpx;
}
.ref-btn,
.eval-btn {
  padding: 12rpx 34rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  margin: 0 10rpx;
}
.ref-btn {
  background: #fff;
  color: var(--c-primary);
  border: 1rpx solid var(--c-line);
}
.eval-btn {
  background: var(--grad-primary);
  color: #fff;
  box-shadow: var(--shadow-btn);
}
.tl-item {
  display: flex;
}
.tl-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 56rpx;
  flex-shrink: 0;
}
.dot {
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  background: var(--c-div);
  margin-top: 8rpx;
  box-sizing: border-box;
  flex-shrink: 0;
}
.dot.DONE { background: var(--c-ok); }
.dot.CURRENT {
  background: var(--c-primary);
  box-shadow: 0 0 0 12rpx var(--ring-primary);
}
.dot.PENDING { background: var(--c-div); border: 6rpx solid #fff; box-shadow: 0 0 0 1rpx var(--c-div); }
.line {
  width: 4rpx;
  flex: 1;
  background: var(--c-div);
  margin: 6rpx 0;
  border-radius: 2rpx;
  min-height: 24rpx;
}
.line.ok {
  background: var(--c-ok);
}
.tl-body {
  flex: 1;
  padding: 4rpx 0 34rpx 16rpx;
}
.tl-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.tl-name {
  font-size: 32rpx;
  font-weight: 500;
  color: var(--t1);
}
.tl-status {
  font-size: 24rpx;
  padding: 4rpx 14rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}
.tl-status.DONE { color: var(--c-ok); background: #E8F7EF; }
.tl-status.CURRENT { color: var(--c-primary); background: var(--c-soft); }
.tl-status.PENDING { color: var(--t3); background: var(--c-soft2); }
.tl-assignee {
  display: block;
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 8rpx;
}
.tl-time {
  display: block;
  font-size: 24rpx;
  color: var(--t4);
  margin-top: 6rpx;
}
</style>
