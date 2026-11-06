<template>
  <view class="page">
    <view class="topbar">
      <view class="seg">
        <view v-for="t in tabs" :key="t.value" class="seg-item" :class="{ 'seg-on': cur === t.value }"
              hover-class="u-hover" @click="switchTab(t.value)">{{ t.label }}</view>
      </view>
    </view>

    <view class="list">
      <view v-if="firstLoading" class="rows card">
        <view v-for="i in 3" :key="i" class="sk-row">
          <view class="sk-line" style="width: 55%; height: 34rpx;"></view>
          <view class="sk-line" style="width: 80%; height: 24rpx; margin-top: 14rpx;"></view>
        </view>
      </view>
      <template v-else>
        <view v-if="list.length" class="rows card">
          <view v-for="r in list" :key="r.id" class="row-item" hover-class="u-hover" @click="goDetail(r.id)">
            <view class="row-main">
              <view class="row-top">
                <text class="name">{{ r.matterName }}</text>
                <text class="status-pill" :class="sp(r.status)">{{ r.statusText }}</text>
              </view>
              <view class="row-sub">
                <text class="code">{{ r.recordCode }}</text>
                <text class="time">{{ fmtTime(r.submitTime) }}</text>
              </view>
            </view>
            <text class="chev">›</text>
          </view>
        </view>
        <view v-else class="empty-box">
          <text class="eb-text">暂无申报记录</text>
          <view class="eb-btn" hover-class="u-hover" @click="goHall">去办事大厅</view>
        </view>
        <view v-if="finished && list.length" class="nomore">— 没有更多了 —</view>
      </template>
    </view>
  </view>
</template>

<script>
import { fetchRecords } from '../../api/index.js'
import { statusText, formatTime } from '../../utils/format.js'

export default {
  data() {
    return {
      tabs: [
        { label: '全部', value: '' },
        { label: '审批中', value: 'APPROVING' },
        { label: '已办结', value: 'APPROVED' },
        { label: '已驳回', value: 'REJECTED' }
      ],
      cur: '',
      list: [],
      page: 1,
      size: 10,
      total: 0,
      loading: false,
      firstLoading: true,
      finished: false
    }
  },
  onShow() {
    this.reload()
  },
  onPullDownRefresh() {
    this.reload().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    this.load()
  },
  methods: {
    switchTab(v) {
      this.cur = v
      this.reload()
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
        const data = await fetchRecords({ page: this.page, size: this.size, status: this.cur })
        const rows = (data.list || []).map((r) => Object.assign({}, r, { statusText: statusText(r.status) }))
        this.list = this.page === 1 ? rows : this.list.concat(rows)
        this.total = data.total || 0
        if (this.list.length >= this.total) this.finished = true
        else this.page++
      } catch (e) {
        /* ignore */
      } finally {
        this.loading = false
        this.firstLoading = false
      }
    },
    sp(status) {
      if (status === 'APPROVED') return 'sp-ok'
      if (status === 'REJECTED') return 'sp-danger'
      return 'sp-warn'
    },
    goHall() {
      uni.switchTab({ url: '/pages/hall/hall' })
    },
    fmtTime(t) {
      return formatTime(t)
    },
    goDetail(id) {
      uni.navigateTo({ url: '/pages/record/detail?id=' + id })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 60rpx;
}
.topbar {
  padding: 20rpx 20rpx 0;
}
.list {
  padding: 20rpx;
}
.rows {
  padding: 4rpx 24rpx;
}
.row-item {
  display: flex;
  align-items: center;
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
  justify-content: space-between;
  align-items: center;
}
.name {
  flex: 1;
  margin-right: 16rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-sub {
  display: flex;
  align-items: center;
  margin-top: 12rpx;
}
.code {
  font-size: 24rpx;
  color: var(--t3);
  margin-right: 16rpx;
}
.time {
  font-size: 24rpx;
  color: var(--t4);
}
.chev {
  flex-shrink: 0;
  margin-left: 10rpx;
  color: var(--t4);
  font-size: 32rpx;
  font-weight: 300;
}
.sk-row {
  padding: 26rpx 0;
  border-bottom: 1rpx solid var(--c-div);
}
.sk-row:last-child {
  border-bottom: none;
}
</style>
