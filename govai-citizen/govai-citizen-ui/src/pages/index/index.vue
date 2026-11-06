<template>
  <view class="page">
    <!-- 顶部渐变 hero -->
    <view class="hero">
      <text class="hero-title">办事少跑腿 · 数据多跑路</text>
      <text class="hero-sub">政务服务一站直达</text>
      <view class="search-pill">
        <view class="s-ico"></view>
        <input class="s-inp" v-model="keyword" confirm-type="search" placeholder="搜索事项 / 办理指南"
          placeholder-class="ph" @confirm="doSearch" />
        <text v-if="keyword" class="s-clear" @click="keyword = ''">×</text>
      </view>
    </view>

    <!-- 关怀版开关 -->
    <view class="care-bar">
      <view class="care-toggle" :class="{ on: careMode }" hover-class="u-hover" @click="toggleCare">
        {{ careMode ? '当前为关怀版(大字)，点击恢复标准版' : '切换关怀版 · 大字模式' }}
      </view>
    </view>

    <!-- 关怀版大按钮入口 -->
    <view v-if="careMode" class="care-grid">
      <view v-for="c in careCards" :key="c.label" class="care-card" hover-class="u-hover" @click="openCare(c)">
        <text class="cc-label">{{ c.label }}</text>
        <text class="cc-sub">{{ c.sub }}</text>
      </view>
      <view class="care-card alt" hover-class="u-hover" @click="go('/pages/mine/profile')">
        <text class="cc-label">实名认证</text>
        <text class="cc-sub">填信息换精准服务</text>
      </view>
    </view>

    <!-- 未读公告 -->
    <view v-if="unread > 0" class="notice card" hover-class="u-hover" @click="switchMsg">
      <view class="n-badge">{{ unread > 99 ? '99+' : unread }}</view>
      <text class="n-txt">您有 {{ unread }} 条未读消息，点击查看</text>
      <text class="li-arrow">›</text>
    </view>

    <!-- 快捷服务 -->
    <view class="quick card">
      <view v-for="q in quickItems" :key="q.text" class="q-tile" :class="q.cls" hover-class="u-hover"
        @click="go(q.url)">
        <view class="q-badge">{{ q.ch }}</view>
        <view class="q-main">
          <text class="q-name">{{ q.text }}</text>
          <text class="q-sub">{{ q.sub }}</text>
        </view>
      </view>
    </view>

    <!-- 事项分类 -->
    <view v-if="categories.length" class="cat-card card">
      <view class="cat-head">
        <text class="ch-t">按分类办事</text>
        <text class="ch-more" @click="go('/pages/matter/list?categoryId=0')">全部事项 ›</text>
      </view>
      <scroll-view scroll-x class="cat-scroll" :show-scrollbar="false">
        <view class="cat-row">
          <view class="cat-chip on" @click="go('/pages/matter/list?categoryId=0')">全部</view>
          <view v-for="c in categories" :key="c.id" class="cat-chip" hover-class="u-hover"
            @click="go('/pages/matter/list?categoryId=' + c.id)">{{ c.categoryName }}</view>
        </view>
      </scroll-view>
    </view>

    <!-- 热门事项 -->
    <view class="section-title">热门事项</view>
    <view v-if="loadingHot" class="hot-list card">
      <view v-for="i in 5" :key="i" class="hot-skel">
        <view class="sk-line" style="width: 44%; height: 34rpx;"></view>
        <view class="sk-line" style="width: 30%; height: 24rpx; margin-top: 12rpx;"></view>
      </view>
    </view>
    <view v-else-if="hotMatters.length" class="hot-list card">
      <view v-for="(m, i) in hotMatters" :key="m.id" class="hot-item" hover-class="u-hover"
        @click="go('/pages/matter/detail?id=' + m.id)">
        <view class="hot-idx" :class="i < 3 ? 'r' + (i + 1) : ''">{{ i + 1 }}</view>
        <view class="hot-main">
          <text class="hot-name">{{ m.matterName }}</text>
          <text class="hot-dept">{{ m.department }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>
    <view v-else class="empty-box">
      <text class="eb-text">暂无热门事项</text>
    </view>

    <!-- #ifdef H5 -->
    <view class="tab-space"></view>
    <app-tabbar active="home" />
    <!-- #endif -->
  </view>
</template>

<script>
import { fetchMatters, fetchCategories, fetchUnreadCount } from '../../api/index.js'

export default {
  data() {
    return {
      hotMatters: [],
      categories: [],
      loadingHot: true,
      keyword: '',
      unread: 0,
      careMode: false,
      careCards: [
        { label: '办事大厅', sub: '查找可办事项', url: '/pages/hall/hall', tab: true },
        { label: '我的申报', sub: '进度与办理结果', url: '/pages/record/list', tab: false },
        { label: '消息中心', sub: '办件通知与提醒', url: '/pages/message/message', tab: true },
        { label: 'AI 助手', sub: '不会办?问一问', url: '/pages/ai/chat', tab: false }
      ],
      quickItems: [
        { text: '全部事项', sub: '按分类查找', ch: '事', url: '/pages/matter/list?categoryId=0', cls: 'q1' },
        { text: '我的申报', sub: '进度实时可查', ch: '申', url: '/pages/record/list', cls: 'q2' },
        { text: 'AI 助手', sub: '不会办 问问它', ch: 'AI', url: '/pages/ai/chat', cls: 'q3' },
        { text: '政策推荐', sub: '专属政策匹配', ch: '策', url: '/pages/policy/list', cls: 'q4' }
      ]
    }
  },
  onShow() {
    // #ifdef H5
    uni.hideTabBar({})
    // #endif
    this.careMode = uni.getStorageSync('govai_care') === '1'
    this.loadCategories()
    this.loadHot()
    this.loadUnread()
  },
  methods: {
    toggleCare() {
      this.careMode = !this.careMode
      uni.setStorageSync('govai_care', this.careMode ? '1' : '')
    },
    openCare(c) {
      if (c.tab) uni.switchTab({ url: c.url })
      else uni.navigateTo({ url: c.url })
    },
    switchMsg() {
      uni.switchTab({ url: '/pages/message/message' })
    },
    doSearch() {
      const kw = (this.keyword || '').trim()
      uni.navigateTo({ url: '/pages/matter/list?categoryId=0&keyword=' + encodeURIComponent(kw) })
    },
    go(url) {
      uni.navigateTo({ url })
    },
    async loadCategories() {
      try {
        this.categories = await fetchCategories()
      } catch (e) {
        /* ignore */
      }
    },
    async loadHot() {
      this.loadingHot = true
      try {
        const page = await fetchMatters({ page: 1, size: 5 })
        this.hotMatters = page.list || []
      } catch (e) {
        this.hotMatters = []
      } finally {
        this.loadingHot = false
      }
    },
    async loadUnread() {
      try {
        const n = await fetchUnreadCount()
        this.unread = n || 0
        // #ifdef H5
        const { setBadge } = await import('../../utils/badge.js')
        setBadge(n)
        // #endif
        uni.setTabBarBadge({
          index: 2,
          text: String(Math.min(n, 99))
        }).catch(() => {})
      } catch (e) {
        /* ignore */
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
}
.hero {
  position: relative;
  overflow: hidden;
  background: var(--grad-primary);
  padding: 48rpx 32rpx 48rpx;
  display: flex;
  flex-direction: column;
}
.hero::after {
  content: '';
  position: absolute;
  right: -80rpx;
  top: -80rpx;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  background: radial-gradient(rgba(255, 255, 255, 0.18), transparent 68%);
  pointer-events: none;
}
.hero::before {
  content: '';
  position: absolute;
  right: 160rpx;
  bottom: -120rpx;
  width: 260rpx;
  height: 260rpx;
  border-radius: 50%;
  background: radial-gradient(rgba(255, 255, 255, 0.12), transparent 66%);
  pointer-events: none;
}
.hero-title {
  color: #fff;
  font-size: 40rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}
.hero-sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: 24rpx;
  margin-top: 12rpx;
}
.search-pill {
  margin-top: 40rpx;
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: var(--r-pill);
  padding: 22rpx 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 40, 100, 0.12);
  transition: box-shadow 0.18s ease;
}
.search-pill:focus-within {
  box-shadow: 0 0 0 6rpx rgba(255, 255, 255, 0.28), 0 10rpx 28rpx rgba(15, 40, 100, 0.18);
}
.s-ico {
  position: relative;
  width: 22rpx;
  height: 22rpx;
  border: 4rpx solid var(--c-primary);
  border-radius: 50%;
  margin-right: 18rpx;
  flex-shrink: 0;
}
.s-ico::after {
  content: '';
  position: absolute;
  right: -12rpx;
  bottom: -6rpx;
  width: 12rpx;
  height: 4rpx;
  background: var(--c-primary);
  border-radius: 2rpx;
  transform: rotate(45deg);
}
.s-inp {
  flex: 1;
  font-size: 28rpx;
  color: var(--t1);
}
.s-clear {
  color: var(--t3);
  font-size: 32rpx;
  padding: 0 4rpx 0 16rpx;
  line-height: 1;
}
.s-ph {
  color: var(--t3);
  font-size: 28rpx;
}

.quick {
  margin: 20rpx 20rpx 0;
  padding: 20rpx;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}
.q-tile {
  width: calc(50% - 8rpx);
  box-sizing: border-box;
  margin-bottom: 12rpx;
  padding: 26rpx 22rpx;
  border-radius: var(--r-md);
  display: flex;
  align-items: center;
  gap: 18rpx;
}
.q-badge {
  width: 68rpx;
  height: 68rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-body);
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(15, 40, 100, 0.06);
}
.q-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.q-tile:hover {
  transform: translateY(-2rpx);
  box-shadow: 0 10rpx 24rpx rgba(15, 40, 100, 0.1);
}
.q-name {
  font-size: var(--fs-subtitle);
  font-weight: 600;
}
.q-sub {
  font-size: var(--fs-caption);
  opacity: 0.72;
  margin-top: 8rpx;
}
.q1 { background: #EAF0FF; color: var(--c-primary); }
.q2 { background: #E8F7EF; color: var(--c-ok); }
.q3 { background: #E7EDFF; color: #1A3A8F; }
.q4 { background: #FDF3E3; color: #B07A0E; }

.section-title {
  margin: 32rpx 24rpx 0;
}
.hot-list {
  margin: 0 20rpx;
}
.hot-item {
  display: flex;
  align-items: center;
  padding: 22rpx 6rpx;
  border-bottom: 1rpx solid var(--c-div);
}
.hot-item:last-child {
  border-bottom: none;
}
.hot-idx {
  width: 44rpx;
  height: 44rpx;
  border-radius: 12rpx;
  background: var(--c-soft2);
  color: var(--t3);
  font-size: 24rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.hot-idx.act {
  background: var(--c-soft);
  color: var(--c-primary);
}
.hot-idx.r1 {
  background: linear-gradient(135deg, #ffd76a, #f5a623);
  color: #7a4b00;
  box-shadow: 0 6rpx 16rpx rgba(245, 166, 35, 0.35);
}
.hot-idx.r2 {
  background: linear-gradient(135deg, #e6eaf1, #c3cad6);
  color: #5b6472;
}
.hot-idx.r3 {
  background: linear-gradient(135deg, #f6d8bd, #e0a878);
  color: #7a4a1e;
}
.hot-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.hot-name {
  font-size: 32rpx;
  font-weight: 500;
  color: var(--t1);
}
.hot-dept {
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 6rpx;
}
.arrow {
  color: var(--t4);
  font-size: 32rpx;
  font-weight: 300;
}

/* 未读公告 */
.notice {
  margin: 20rpx 20rpx 0;
  display: flex;
  align-items: center;
  padding: 22rpx 24rpx;
  border-left: 6rpx solid var(--c-primary);
}
.n-badge {
  min-width: 44rpx;
  height: 34rpx;
  padding: 0 8rpx;
  box-sizing: border-box;
  background: var(--c-danger);
  color: #fff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  margin-right: 16rpx;
}
.n-txt {
  flex: 1;
  font-size: 28rpx;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 分类横滑 */
.cat-card {
  margin: 20rpx;
  padding: 24rpx 24rpx 12rpx;
}
.cat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}
.ch-t {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--t1);
}
.ch-more {
  font-size: 24rpx;
  color: var(--t3);
}
.cat-scroll {
  white-space: nowrap;
  width: 100%;
}
.cat-row {
  display: flex;
  padding-bottom: 16rpx;
}
.cat-chip {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  padding: 12rpx 30rpx;
  border-radius: 999rpx;
  background: var(--c-soft2);
  color: var(--t2);
  font-size: 24rpx;
  margin-right: 16rpx;
}
.cat-chip.on {
  background: var(--c-primary);
  color: #fff;
  font-weight: 600;
}

/* 热门骨架 */
.hot-skel {
  padding: 20rpx 6rpx;
  border-bottom: 1rpx solid var(--c-div);
}
.hot-skel:last-child { border-bottom: none; }

/* 关怀版 */
.care-bar {
  display: flex;
  justify-content: flex-end;
  padding: 14rpx 24rpx 0;
}
.care-toggle {
  font-size: 24rpx;
  color: var(--c-primary);
  background: var(--c-soft);
  border-radius: 999rpx;
  padding: 10rpx 26rpx;
}
.care-toggle.on {
  color: #fff;
  background: var(--c-primary);
}
.care-grid {
  margin: 20rpx;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}
.care-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: var(--shadow-card);
}
.care-card.alt {
  grid-column: 1 / -1;
}
.cc-label {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--t1);
}
.cc-sub {
  font-size: 28rpx;
  color: var(--t3);
  margin-top: 12rpx;
}

</style>
