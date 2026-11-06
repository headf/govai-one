<template>
  <!-- #ifdef H5 -->
  <view class="app-tabbar">
    <view
      v-for="t in tabs"
      :key="t.key"
      class="tb-item"
      :class="{ active: active === t.key }"
      @click="onTap(t.key)"
    >
      <view class="tb-ico-wrap">
        <view class="tb-ico" :class="'i-' + t.key"></view>
        <view v-if="t.key === 'message' && badge > 0" class="tb-badge">{{ badge > 99 ? '99+' : badge }}</view>
      </view>
      <text class="tb-text">{{ t.text }}</text>
    </view>
  </view>
  <!-- #endif -->
</template>

<script>
import { badgeState } from '../../utils/badge.js'

const TABS = [
  { key: 'home', text: '首页', path: '/pages/index/index' },
  { key: 'hall', text: '办事', path: '/pages/hall/hall' },
  { key: 'message', text: '消息', path: '/pages/message/message' },
  { key: 'mine', text: '我的', path: '/pages/mine/mine' }
]

export default {
  name: 'app-tabbar',
  props: {
    active: { type: String, default: 'home' }
  },
  data() {
    return { tabs: TABS }
  },
  computed: {
    badge() {
      return badgeState.count
    }
  },
  methods: {
    onTap(key) {
      if (key === this.active) return
      const t = TABS.find((x) => x.key === key)
      if (t) uni.switchTab({ url: t.path })
    }
  }
}
</script>

<style scoped>
.app-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: stretch;
  background: #fff;
  box-shadow: 0 -4rpx 24rpx rgba(15, 40, 100, 0.08);
  z-index: 999;
  height: calc(112rpx + constant(safe-area-inset-bottom));
  height: calc(112rpx + env(safe-area-inset-bottom));
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}
.tb-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9aa3b2;
}
.tb-ico-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 76rpx;
  height: 56rpx;
  border-radius: 32rpx;
}
.tb-ico {
  width: 44rpx;
  height: 44rpx;
  background-color: currentColor;
  -webkit-mask: no-repeat center / contain;
  mask: no-repeat center / contain;
}
.tb-item.active {
  color: var(--c-primary);
}
.tb-item.active .tb-ico-wrap {
  background: var(--c-soft);
}
.tb-text {
  font-size: 24rpx;
  line-height: 1;
  margin-top: 6rpx;
}
.tb-item.active .tb-text {
  font-weight: 600;
}
.tb-badge {
  position: absolute;
  top: 0;
  right: -2rpx;
  min-width: 30rpx;
  height: 30rpx;
  padding: 0 6rpx;
  border-radius: 24rpx;
  background: #ff3b30;
  color: #fff;
  font-size: 24rpx;
  line-height: 30rpx;
  text-align: center;
  box-sizing: border-box;
}
/* 线性图标（fill 黑底，经 mask + currentColor 染色，仅 H5） */
.i-home {
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%2024%2024'%3E%3Cpath%20fill='%23000'%20d='M12%203L2%2012h3v8h6v-6h2v6h6v-8h3z'/%3E%3C/svg%3E");
  mask-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%2024%2024'%3E%3Cpath%20fill='%23000'%20d='M12%203L2%2012h3v8h6v-6h2v6h6v-8h3z'/%3E%3C/svg%3E");
}
.i-hall {
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%2024%2024'%3E%3Cpath%20fill='%23000'%20d='M3.5%203.5h7v7h-7zM13.5%203.5h7v7h-7zM3.5%2013.5h7v7h-7zM13.5%2013.5h7v7h-7z'/%3E%3C/svg%3E");
  mask-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%2024%2024'%3E%3Cpath%20fill='%23000'%20d='M3.5%203.5h7v7h-7zM13.5%203.5h7v7h-7zM3.5%2013.5h7v7h-7zM13.5%2013.5h7v7h-7z'/%3E%3C/svg%3E");
}
.i-message {
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%2024%2024'%3E%3Cpath%20fill='%23000'%20d='M12%202.5A6.5%206.5%200%200%200%205.5%209v4.8L4%2017h16l-1.5-3.2V9A6.5%206.5%200%200%200%2012%202.5zM9.5%2020.5h5a2.5%202.5%200%200%201-5%200z'/%3E%3C/svg%3E");
  mask-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%2024%2024'%3E%3Cpath%20fill='%23000'%20d='M12%202.5A6.5%206.5%200%200%200%205.5%209v4.8L4%2017h16l-1.5-3.2V9A6.5%206.5%200%200%200%2012%202.5zM9.5%2020.5h5a2.5%202.5%200%200%201-5%200z'/%3E%3C/svg%3E");
}
.i-mine {
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%2024%2024'%3E%3Cpath%20fill='%23000'%20d='M12%2012a4.5%204.5%200%201%200%200-9%204.5%204.5%200%200%200%200%209zm0%202c-4.5%200-8%202-8%204v1.5h16V18c0-2-3.5-4-8-4z'/%3E%3C/svg%3E");
  mask-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%2024%2024'%3E%3Cpath%20fill='%23000'%20d='M12%2012a4.5%204.5%200%201%200%200-9%204.5%204.5%200%200%200%200%209zm0%202c-4.5%200-8%202-8%204v1.5h16V18c0-2-3.5-4-8-4z'/%3E%3C/svg%3E");
}
</style>
