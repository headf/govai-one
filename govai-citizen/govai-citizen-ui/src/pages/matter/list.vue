<template>
  <view class="page">
    <view class="searchbar">
      <view class="s-inner">
        <view class="s-ico"></view>
        <input class="search-input" v-model="keyword" placeholder="输入事项名称 / 关键词" confirm-type="search"
               placeholder-class="ph" @confirm="onSearch" />
      </view>
      <view class="search-btn" hover-class="u-hover" @click="onSearch">搜索</view>
    </view>

    <!-- 搜索联想 -->
    <view v-if="sugg.length" class="sugg-bar">
      <text class="sg-tip">猜你想搜</text>
      <view class="sugg-row">
        <view class="sg-chip" v-for="(s, i) in sugg" :key="i" hover-class="u-hover" @click="pickSug(s)">{{ s }}</view>
      </view>
    </view>

    <scroll-view class="matters" scroll-y @scrolltolower="loadMore">
      <view v-if="firstLoading" class="rows card">
        <view v-for="i in 4" :key="i" class="sk-row">
          <view class="sk-line" style="width: 60%; height: 34rpx;"></view>
          <view class="sk-line" style="width: 90%; height: 24rpx; margin-top: 16rpx;"></view>
          <view class="sk-line" style="width: 40%; height: 24rpx; margin-top: 12rpx;"></view>
        </view>
      </view>
      <view v-else-if="list.length" class="rows card">
        <view v-for="m in list" :key="m.id" class="row-item" hover-class="u-hover" @click="goDetail(m.id)">
          <view class="row-main">
            <view class="item-top">
              <text class="name">{{ m.matterName }}</text>
              <text class="dept">{{ m.categoryName }}</text>
            </view>
            <view class="item-bottom">
              <text class="desc">{{ m.description || m.matterCode }}</text>
              <text class="days">法定办结 {{ m.deadlineDays }} 天</text>
            </view>
          </view>
          <text class="chev">›</text>
        </view>
      </view>
      <view v-else-if="!loading" class="empty-box">
        <text class="eb-text">没有找到相关事项，换个关键词试试</text>
        <view class="eb-btn" hover-class="u-hover" @click="keyword = ''; onSearch()">查看全部事项</view>
      </view>
      <view v-if="loading && !firstLoading" class="loadmore">加载中…</view>
      <view v-if="finished && list.length" class="nomore">— 没有更多了 —</view>
    </scroll-view>
  </view>
</template>

<script>
import { fetchMatters } from '../../api/index.js'

export default {
  data() {
    return {
      categoryId: 0,
      keyword: '',
      list: [],
      page: 1,
      size: 10,
      total: 0,
      loading: false,
      firstLoading: true,
      finished: false,
      sugg: [],
      suggTimer: null
    }
  },
  watch: {
    keyword(nv) {
      if (this.suggTimer) clearTimeout(this.suggTimer)
      const kw = (nv || '').trim()
      if (!kw) {
        this.sugg = []
        return
      }
      this.suggTimer = setTimeout(() => this.loadSugg(kw), 300)
    }
  },
  onLoad(options) {
    this.categoryId = Number(options.categoryId || 0)
    this.keyword = decodeURIComponent(options.keyword || '')
    this.reload()
  },
  onPullDownRefresh() {
    this.reload().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    this.loadMore()
  },
  methods: {
    async loadSugg(kw) {
      try {
        const data = await fetchMatters({ page: 1, size: 6, keyword: kw })
        const seen = new Set([kw])
        this.sugg = (data.list || [])
          .map((m) => m.matterName)
          .filter((n) => !seen.has(n) && seen.add(n))
      } catch (e) {
        this.sugg = []
      }
    },
    pickSug(s) {
      this.keyword = s
      this.sugg = []
      this.onSearch()
    },
    onSearch() {
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
        const params = { page: this.page, size: this.size }
        if (this.categoryId) params.categoryId = this.categoryId
        if (this.keyword.trim()) params.keyword = this.keyword.trim()
        const data = await fetchMatters(params)
        const rows = data.list || []
        this.list = this.page === 1 ? rows : this.list.concat(rows)
        this.total = data.total || 0
        if (this.list.length >= this.total) {
          this.finished = true
        } else {
          this.page++
        }
      } catch (e) {
        /* ignore */
      } finally {
        this.loading = false
        this.firstLoading = false
      }
    },
    loadMore() {
      this.load()
    },
    goDetail(id) {
      uni.navigateTo({ url: '/pages/matter/detail?id=' + id })
    }
  }
}
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.searchbar {
  display: flex;
  align-items: center;
  padding: 16rpx 20rpx;
  background: #fff;
}
.s-inner {
  flex: 1;
  display: flex;
  align-items: center;
  background: var(--c-bg);
  border-radius: var(--r-pill);
  padding: 16rpx 28rpx;
}
.s-ico {
  position: relative;
  width: 18rpx;
  height: 18rpx;
  border: 3rpx solid var(--t3);
  border-radius: 50%;
  margin-right: 18rpx;
  flex-shrink: 0;
}
.s-ico::after {
  content: '';
  position: absolute;
  right: -10rpx;
  bottom: -4rpx;
  width: 10rpx;
  height: 3rpx;
  background: var(--t3);
  border-radius: 2rpx;
  transform: rotate(45deg);
}
.search-input {
  flex: 1;
  font-size: 28rpx;
  color: var(--t1);
}
.ph {
  color: var(--t3);
}
.search-btn {
  background: var(--grad-primary);
  color: #fff;
  padding: 16rpx 34rpx;
  border-radius: var(--r-pill);
  margin-left: 16rpx;
  font-size: 28rpx;
}
.matters {
  flex: 1;
  padding: 16rpx 20rpx;
}
.item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.name {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--t1);
  flex: 1;
  margin-right: 16rpx;
}
.dept {
  font-size: 24rpx;
  color: var(--c-primary);
  background: var(--c-soft);
  padding: 6rpx 16rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}
.item-bottom {
  display: flex;
  justify-content: space-between;
  margin-top: 14rpx;
}
.desc {
  font-size: 24rpx;
  color: var(--t3);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 16rpx;
}
.days {
  font-size: 24rpx;
  color: var(--t4);
  white-space: nowrap;
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
.loadmore {
  text-align: center;
  color: var(--t4);
  font-size: 24rpx;
  padding: 20rpx;
}
.sugg-bar {
  padding: 0 24rpx 8rpx;
  background: #fff;
  border-bottom: 1rpx solid var(--c-div);
}
.sg-tip {
  font-size: 24rpx;
  color: var(--t4);
}
.sugg-row {
  display: flex;
  flex-wrap: wrap;
  padding: 10rpx 0 14rpx;
}
.sg-chip {
  font-size: 24rpx;
  color: var(--c-primary);
  background: var(--c-soft);
  border-radius: 999rpx;
  padding: 8rpx 22rpx;
  margin: 6rpx 12rpx 0 0;
}
</style>
