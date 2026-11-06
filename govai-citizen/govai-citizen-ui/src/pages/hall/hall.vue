<template>
  <view class="page">
    <!-- 顶部渐变 hero + 搜索 -->
    <view class="hero">
      <view class="hero-row">
        <text class="hero-title">办事大厅</text>
        <text class="hero-sub">按需选择事项 · 在线办理</text>
      </view>
      <view class="search-pill">
        <view class="s-ico"></view>
        <input class="search-input" v-model="keyword" placeholder="搜索您要办理的事项" confirm-type="search"
               placeholder-class="ph" @confirm="doSearch" />
        <view class="search-btn" hover-class="u-hover" @click="doSearch">搜索</view>
      </view>
    </view>

    <!-- 事项分类（网格卡片） -->
    <view class="section-title">事项分类</view>
    <view class="cat-card card">
      <view class="cat-grid">
        <view v-for="(c, i) in categories" :key="c.id" class="cat-tile" hover-class="u-hover"
          @click="goCategory(c.id, c.categoryName)">
          <view class="cat-badge" :class="'c' + (i % 6)">{{ firstChar(c.categoryName) }}</view>
          <text class="cat-name">{{ c.categoryName }}</text>
        </view>
        <view v-if="!categories.length" class="empty-box"><text class="eb-text">暂无分类</text></view>
      </view>
    </view>

    <!-- 办事指引（卡片组） -->
    <view class="section-title">办事指引</view>
    <view class="guide-grid">
      <view v-for="g in guide" :key="g.text" class="guide-tile card" hover-class="u-hover" @click="go(g.url)">
        <view class="guide-badge" :class="g.cls">{{ g.ch }}</view>
        <text class="g-t">{{ g.text }}</text>
        <text class="g-go">去办理 ›</text>
      </view>
    </view>

    <!-- #ifdef H5 -->
    <view class="tab-space"></view>
    <app-tabbar active="hall" />
    <!-- #endif -->
  </view>
</template>

<script>
import { fetchCategories } from '../../api/index.js'

export default {
  data() {
    return {
      keyword: '',
      categories: [],
      guide: [
        { text: 'AI 办事助手 · 不知道怎么办？问问它', ch: '问', url: '/pages/ai/chat', cls: 'c0' },
        { text: '我的申报 · 查看办理进度与结果', ch: '办', url: '/pages/record/list', cls: 'c1' },
        { text: '政策推荐 · 专属政策精准匹配', ch: '策', url: '/pages/policy/list', cls: 'c2' }
      ]
    }
  },
  onShow() {
    // #ifdef H5
    uni.hideTabBar({})
    // #endif
    this.load()
  },
  methods: {
    async load() {
      try {
        this.categories = await fetchCategories()
      } catch (e) { /* ignore */ }
    },
    firstChar(name) {
      return name ? name.charAt(0) : '类'
    },
    goCategory(id, name) {
      uni.navigateTo({ url: '/pages/matter/list?categoryId=' + id })
    },
    doSearch() {
      const k = this.keyword.trim()
      uni.navigateTo({ url: '/pages/matter/list?categoryId=0&keyword=' + encodeURIComponent(k) })
    },
    go(url) {
      uni.navigateTo({ url })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 120rpx;
}

/* ===== 顶部渐变 hero ===== */
.hero {
  background: var(--grad-primary);
  padding: 44rpx 32rpx 96rpx;
  border-radius: 0 0 var(--r-lg) var(--r-lg);
  box-shadow: 0 10rpx 30rpx rgba(47, 107, 255, 0.18);
}
.hero-row {
  margin-bottom: 28rpx;
}
.hero-title {
  display: block;
  font-size: var(--fs-title);
  font-weight: 700;
  color: #fff;
  letter-spacing: 1rpx;
}
.hero-sub {
  display: block;
  margin-top: 8rpx;
  font-size: var(--fs-caption);
  color: rgba(255, 255, 255, 0.82);
}
.search-pill {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: var(--r-pill);
  padding: 12rpx 14rpx 12rpx 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 40, 100, 0.14);
}
.s-ico {
  position: relative;
  width: 20rpx;
  height: 20rpx;
  border: 3rpx solid var(--c-primary);
  border-radius: 50%;
  margin-right: 16rpx;
  flex-shrink: 0;
}
.s-ico::after {
  content: '';
  position: absolute;
  right: -11rpx;
  bottom: -5rpx;
  width: 11rpx;
  height: 4rpx;
  background: var(--c-primary);
  border-radius: 2rpx;
  transform: rotate(45deg);
}
.search-input {
  flex: 1;
  font-size: var(--fs-body);
  color: var(--t1);
}
.ph { color: var(--t3); }
.search-btn {
  background: var(--grad-primary);
  color: #fff;
  font-size: var(--fs-caption);
  padding: 14rpx 34rpx;
  border-radius: var(--r-pill);
  margin-left: 12rpx;
}

/* ===== 分类网格 ===== */
.cat-card {
  margin: 0 20rpx;
  padding: 16rpx 12rpx 8rpx;
}
.cat-grid {
  display: flex;
  flex-wrap: wrap;
}
.cat-tile {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 4rpx 22rpx;
  box-sizing: border-box;
}
.cat-badge {
  width: 84rpx;
  height: 84rpx;
  border-radius: var(--r-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-subtitle);
  font-weight: 600;
  box-shadow: 0 6rpx 16rpx rgba(15, 40, 100, 0.08);
}
.cat-name {
  margin-top: 14rpx;
  font-size: var(--fs-caption);
  color: var(--t2);
  text-align: center;
  line-height: 1.35;
  padding: 0 4rpx;
}
.empty-box {
  width: 100%;
  text-align: center;
  padding: 60rpx 0;
}
.eb-text {
  color: var(--t3);
  font-size: var(--fs-body);
}

/* ===== 办事指引卡片组 ===== */
.guide-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin: 0 20rpx;
}
.guide-tile {
  width: calc(50% - 8rpx);
  box-sizing: border-box;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 24rpx 22rpx;
}
.guide-tile:first-child {
  width: 100%;
}
.guide-badge {
  width: 64rpx;
  height: 64rpx;
  border-radius: var(--r-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-body);
  font-weight: 600;
  margin-bottom: 16rpx;
  box-shadow: 0 6rpx 16rpx rgba(15, 40, 100, 0.08);
}
.g-t {
  font-size: var(--fs-body);
  color: var(--t1);
  line-height: 1.5;
}
.g-go {
  margin-top: 12rpx;
  font-size: var(--fs-caption);
  color: var(--c-primary);
}

/* 分类软色块循环 6 色 */
.c0 { background: #EAF0FF; color: #2F6BFF; }
.c1 { background: #E8F7EF; color: var(--c-ok); }
.c2 { background: #E7EDFF; color: #1A3A8F; }
.c3 { background: #E6F6F8; color: #0FA5B0; }
.c4 { background: #FFF3E2; color: #E1831F; }
.c5 { background: #FDEBF1; color: #E0457B; }
</style>
