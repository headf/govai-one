<template>
  <view class="page">
    <!-- 未登录 -->
    <view v-if="!store.isLogin" class="hero not-login">
      <text class="welcome-title">欢迎使用智慧政务</text>
      <text class="welcome-sub">登录后可在线申报、跟踪进度</text>
      <view class="login-btn" @click="login">登录 / 注册</view>
    </view>

    <!-- 已登录 -->
    <view v-else class="hero">
      <view class="avatar">{{ avatarChar }}</view>
      <view class="uinfo">
        <text class="uname">{{ store.displayName }}</text>
        <text class="uphone">{{ store.userInfo && store.userInfo.phone }}</text>
      </view>
      <view class="vip" v-if="store.userInfo && store.userInfo.hasIdCard">已实名</view>
    </view>

    <view class="menu card">
      <view v-for="m in menus" :key="m.text" class="m-item" @click="go(m.url)">
        <view class="m-chip">{{ m.ch }}</view>
        <text class="n">{{ m.text }}</text>
        <text class="a">›</text>
      </view>
    </view>

    <view class="credit-card" v-if="store.isLogin && store.userInfo && store.userInfo.creditLevel">
      <view class="credit-head">
        <text class="credit-title">个人信用</text>
        <text class="credit-no" v-if="store.userInfo.creditNo">{{ store.userInfo.creditNo }}</text>
      </view>
      <view class="credit-body">
        <text class="credit-level">{{ store.userInfo.creditLevel }} 级</text>
        <text class="credit-score">{{ store.userInfo.creditScore }} 分</text>
      </view>
    </view>

    <view v-if="store.isLogin" class="logout-btn" @click="logout">退出登录</view>

    <!-- #ifdef H5 -->
    <view class="tab-space"></view>
    <app-tabbar active="mine" />
    <!-- #endif -->
  </view>
</template>

<script>
import { useUserStore } from '../../stores/user.js'

export default {
  data() {
    return {
      store: useUserStore(),
      menus: [
        { text: '我的申报', ch: '申', url: '/pages/record/list' },
        { text: '我的评价', ch: '评', url: '/pages/evaluation/my' },
        { text: '政策推荐', ch: '策', url: '/pages/policy/list' },
        { text: '个人资料', ch: '资', url: '/pages/mine/profile' },
        { text: 'AI 助手', ch: '问', url: '/pages/ai/chat' },
        { text: '关于系统', ch: '关', url: '' }
      ]
    }
  },
  computed: {
    avatarChar() {
      const name = this.store.displayName || ''
      return name ? name.charAt(0) : '客'
    }
  },
  onShow() {
    // #ifdef H5
    uni.hideTabBar({})
    // #endif
    if (this.store.isLogin) {
      this.store.fetchProfile().catch(() => {})
    }
  },
  methods: {
    login() {
      uni.navigateTo({ url: '/pages/login/login' })
    },
    go(url) {
      if (!url) {
        this.about()
        return
      }
      if (!this.store.isLogin && url.indexOf('login') < 0) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        setTimeout(() => uni.navigateTo({ url: '/pages/login/login' }), 500)
        return
      }
      uni.navigateTo({ url })
    },
    logout() {
      uni.showModal({
        title: '提示',
        content: '确定退出登录吗?',
        success: (res) => {
          if (res.confirm) {
            this.store.logout()
            uni.showToast({ title: '已退出', icon: 'none' })
          }
        }
      })
    },
    about() {
      uni.showModal({
        title: 'GovAI One 智慧政务',
        content: '群众端 v1.0.0\n让数据多跑路 群众少跑腿\n\n责任人: 张梦凡',
        showCancel: false
      })
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
  display: flex;
  align-items: center;
  padding: 56rpx 40rpx 96rpx;
  background: var(--grad-primary);
}
.hero::after {
  content: '';
  position: absolute;
  right: -80rpx;
  top: -100rpx;
  width: 340rpx;
  height: 340rpx;
  border-radius: 50%;
  background: radial-gradient(rgba(255, 255, 255, 0.16), transparent 66%);
  pointer-events: none;
}
.not-login {
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: #fff;
  color: var(--c-primary);
  font-size: 56rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 8rpx 24rpx rgba(15, 40, 100, 0.15);
}
.uinfo {
  margin-left: 28rpx;
  display: flex;
  flex-direction: column;
}
.uname {
  color: #fff;
  font-size: 40rpx;
  font-weight: 600;
}
.uphone {
  color: rgba(255, 255, 255, 0.82);
  font-size: 24rpx;
  margin-top: 10rpx;
}
.vip {
  margin-left: auto;
  background: rgba(255, 255, 255, 0.24);
  border: 1rpx solid rgba(255, 255, 255, 0.5);
  color: #fff;
  font-size: 24rpx;
  padding: 8rpx 22rpx;
  border-radius: var(--r-pill);
}
.welcome-title {
  color: #fff;
  font-size: 40rpx;
  font-weight: 700;
}
.welcome-sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: 24rpx;
  margin-top: 16rpx;
}
.login-btn {
  margin-top: 40rpx;
  background: #fff;
  color: var(--c-primary);
  font-size: 32rpx;
  font-weight: 600;
  padding: 20rpx 96rpx;
  border-radius: var(--r-pill);
  box-shadow: 0 8rpx 24rpx rgba(15, 40, 100, 0.15);
}

.menu {
  margin: -56rpx 20rpx 0;
  padding: 8rpx 24rpx;
  position: relative;
  z-index: 1;
}
.m-item {
  display: flex;
  align-items: center;
  padding: 26rpx 0;
  border-bottom: 1rpx solid var(--c-div);
}
.m-item:last-child {
  border-bottom: none;
}
.m-chip {
  width: 60rpx;
  height: 60rpx;
  border-radius: 24rpx;
  background: var(--c-soft);
  color: var(--c-primary);
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.n {
  flex: 1;
  font-size: 28rpx;
  color: var(--t1);
}
.a {
  color: var(--t4);
  font-size: 32rpx;
  font-weight: 300;
}

.logout-btn {
  background: #fff;
  color: var(--c-danger);
  border: 1rpx solid #FFD2D0;
  text-align: center;
  padding: 24rpx;
  font-size: 32rpx;
  margin: 24rpx 20rpx 0;
  border-radius: var(--r-pill);
}

.credit-card {
  margin: 24rpx 20rpx 0;
  padding: 28rpx 30rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #4a7dff, #2f6bff);
  color: #fff;
  box-shadow: var(--shadow-btn);
}
.credit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.credit-title {
  font-size: 24rpx;
  opacity: 0.85;
}
.credit-no {
  font-size: 24rpx;
  opacity: 0.75;
  letter-spacing: 1rpx;
}
.credit-body {
  display: flex;
  align-items: baseline;
  margin-top: 10rpx;
}
.credit-level {
  font-size: 56rpx;
  font-weight: 700;
  line-height: 1;
}
.credit-score {
  font-size: 24rpx;
  margin-left: 14rpx;
  opacity: 0.9;
}
</style>
