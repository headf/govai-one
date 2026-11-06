<template>
  <view class="page">
    <view class="top">
      <view class="seg top-seg">
        <view v-for="t in tabs" :key="t.value" class="seg-item" :class="{ 'seg-on': cur === t.value }"
              hover-class="u-hover" @click="switchTab(t.value)">{{ t.label }}</view>
      </view>
      <text class="read-all" hover-class="u-hover" @click="readAll">全部已读</text>
    </view>

    <view class="list">
      <view v-if="loading && !list.length" class="rows card">
        <view v-for="i in 4" :key="i" class="sk-row">
          <view class="sk-line" style="width: 40%; height: 30rpx;"></view>
          <view class="sk-line" style="width: 85%; height: 24rpx; margin-top: 14rpx;"></view>
        </view>
      </view>
      <view v-else-if="list.length" class="rows card">
        <view v-for="m in list" :key="m.id" class="row-item" hover-class="u-hover" @click="goDetail(m.id)">
          <view class="dot" :class="{ unread: m.isRead === 0 }"></view>
          <view class="row-main">
            <view class="row-top">
              <text class="title" :class="{ bold: m.isRead === 0 }">{{ m.title }}</text>
              <text class="time">{{ short(m.createTime) }}</text>
            </view>
            <view class="content">{{ m.content }}</view>
          </view>
        </view>
      </view>
      <view v-else class="empty-box">
        <text class="eb-text">暂无{{ curLabel }}消息</text>
        <view v-if="cur !== 'SYSTEM'" class="eb-btn" hover-class="u-hover" @click="toDo">去办事 / 看政策</view>
      </view>
      <view v-if="finished && list.length" class="nomore">— 没有更多了 —</view>
    </view>

    <!-- #ifdef H5 -->
    <view class="tab-space"></view>
    <app-tabbar active="message" />
    <!-- #endif -->
  </view>
</template>

<script>
import { fetchMessages, readAllMessages } from '../../api/index.js'
import { formatTime } from '../../utils/format.js'

export default {
  data() {
    return {
      tabs: [
        { label: '全部', value: '' },
        { label: '办件通知', value: 'APPROVAL' },
        { label: '政策推送', value: 'POLICY' },
        { label: '系统', value: 'SYSTEM' }
      ],
      cur: '',
      list: [],
      page: 1,
      size: 10,
      total: 0,
      loading: false,
      finished: false
    }
  },
  onShow() {
    // #ifdef H5
    uni.hideTabBar({})
    // #endif
    this.reload()
    this.loadUnread()
  },
  onPullDownRefresh() {
    this.reload().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    this.load()
  },
  methods: {
    async loadUnread() {
      try {
        const { fetchUnreadCount } = await import('../../api/index.js')
        const n = await fetchUnreadCount()
        // #ifdef H5
        const { setBadge } = await import('../../utils/badge.js')
        setBadge(n)
        // #endif
        uni.setTabBarBadge({ index: 2, text: String(Math.min(n, 99)) }).catch(() => {})
      } catch (e) { /* ignore */ }
    },
    switchTab(v) {
      this.cur = v
      this.reload()
    },
    curLabel() {
      const t = this.tabs.find((x) => x.value === this.cur)
      return t ? t.label.replace('通知', '').replace('推送', '') : '全部'
    },
    toDo() {
      uni.switchTab({ url: '/pages/hall/hall' })
    },
    async reload() {
      this.page = 1
      this.finished = false
      await this.load()
    },
    async load() {
      if (this.loading || this.finished) return
      this.loading = true
      try {
        const data = await fetchMessages({ page: this.page, size: this.size, type: this.cur })
        const rows = data.list || []
        this.list = this.page === 1 ? rows : this.list.concat(rows)
        this.total = data.total || 0
        if (this.list.length >= this.total) this.finished = true
        else this.page++
      } catch (e) { /* ignore */ }
      finally { this.loading = false }
    },
    short(t) {
      return formatTime(t).substring(5, 16)
    },
    async readAll() {
      try {
        await readAllMessages()
        // #ifdef H5
        const { setBadge } = await import('../../utils/badge.js')
        setBadge(0)
        // #endif
        uni.showToast({ title: '已全部标记已读', icon: 'none' })
        this.reload()
      } catch (e) { /* ignore */ }
    },
    goDetail(id) {
      uni.navigateTo({ url: '/pages/message/detail?id=' + id })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
}
.top {
  display: flex;
  align-items: center;
  padding: 20rpx 20rpx 0;
}
.top-seg {
  flex: 1;
}
.read-all {
  font-size: 24rpx;
  color: var(--c-primary);
  padding: 0 8rpx 0 20rpx;
}
.list {
  padding: 20rpx;
}
.rows {
  padding: 4rpx 24rpx;
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
  margin: 10rpx 14rpx 0 0;
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.title.bold {
  color: var(--t1);
  font-weight: 600;
}
.time {
  font-size: 24rpx;
  color: var(--t4);
  margin-left: 12rpx;
  flex-shrink: 0;
}
.content {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: var(--t3);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.sk-row {
  padding: 26rpx 0;
  border-bottom: 1rpx solid var(--c-div);
}
.sk-row:last-child {
  border-bottom: none;
}
</style>
