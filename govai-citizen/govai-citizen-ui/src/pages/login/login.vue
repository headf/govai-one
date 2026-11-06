<template>
  <view class="page">
    <view class="hero">
      <view class="logo-badge"><text class="logo-char">政</text></view>
      <text class="app-name">智慧政务</text>
      <text class="app-sub">群众端 · 让数据多跑路 群众少跑腿</text>
    </view>

    <view class="form card">
      <view class="field">
        <text class="label">手机号</text>
        <input class="input" v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" placeholder-class="ph" />
      </view>
      <view class="field">
        <text class="label">短信验证</text>
        <view class="code-wrap">
          <input class="input" v-model="code" type="number" maxlength="6" placeholder="验证码(演示可任意填)" placeholder-class="ph" />
          <view class="get-code" hover-class="u-hover" @click="countDown">{{ sendText }}</view>
        </view>
      </view>
      <view class="field">
        <text class="label">姓名</text>
        <input class="input" v-model="realName" placeholder="选填,实名后享更多服务" placeholder-class="ph" />
      </view>

      <view class="login-btn" hover-class="u-hover" @click="doLogin">登录 / 注册</view>
      <view class="hint">未注册手机号将自动创建账号</view>
    </view>

    <view class="wx-login" hover-class="u-hover" @click="wxQuickLogin">微信一键登录(演示)</view>
  </view>
</template>

<script>
import { useUserStore } from '../../stores/user.js'

export default {
  data() {
    return {
      phone: '',
      code: '',
      realName: '',
      sendText: '获取验证码',
      counting: false
    }
  },
  methods: {
    countDown() {
      if (!/^1[3-9]\d{9}$/.test(this.phone)) {
        uni.showToast({ title: '请输入正确手机号', icon: 'none' })
        return
      }
      if (this.counting) return
      this.counting = true
      let n = 60
      this.sendText = n + 's后重发'
      const timer = setInterval(() => {
        n--
        this.sendText = n + 's后重发'
        if (n <= 0) {
          clearInterval(timer)
          this.counting = false
          this.sendText = '获取验证码'
        }
      }, 1000)
      uni.showToast({ title: '验证码已发送(演示)', icon: 'none' })
    },
    async doLogin() {
      if (!/^1[3-9]\d{9}$/.test(this.phone)) {
        uni.showToast({ title: '请输入正确手机号', icon: 'none' })
        return
      }
      const store = useUserStore()
      uni.showLoading({ title: '登录中' })
      try {
        await store.login(this.phone, this.code || '000000')
        uni.hideLoading()
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          const pages = getCurrentPages()
          if (pages.length > 1) {
            uni.navigateBack()
          } else {
            uni.switchTab({ url: '/pages/index/index' })
          }
        }, 600)
      } catch (e) {
        uni.hideLoading()
      }
    },
    async wxQuickLogin() {
      const store = useUserStore()
      uni.showLoading({ title: '登录中' })
      try {
        // H5/演示: 固定 openid; 小程序端可替换为 uni.login 换取 code 后调后端换 openid
        await store.wxLogin('demo_openid_' + Date.now(), '微信用户', '')
        uni.hideLoading()
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          const pages = getCurrentPages()
          if (pages.length > 1) {
            uni.navigateBack()
          } else {
            uni.switchTab({ url: '/pages/index/index' })
          }
        }, 600)
      } catch (e) {
        uni.hideLoading()
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--c-bg);
}
.hero {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: var(--grad-primary);
  padding: 110rpx 0 140rpx;
}
.hero::after {
  content: '';
  position: absolute;
  right: -100rpx;
  top: -120rpx;
  width: 360rpx;
  height: 360rpx;
  border-radius: 50%;
  background: radial-gradient(rgba(255, 255, 255, 0.16), transparent 66%);
  pointer-events: none;
}
.logo-badge {
  width: 148rpx;
  height: 148rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 12rpx 40rpx rgba(15, 40, 100, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-char {
  font-size: 76rpx;
  font-weight: 700;
  color: var(--c-primary);
}
.app-name {
  color: #fff;
  font-size: 40rpx;
  font-weight: 700;
  margin-top: 28rpx;
}
.app-sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: 24rpx;
  margin-top: 12rpx;
}

.form {
  margin: -76rpx 40rpx 0;
  position: relative;
  z-index: 1;
  padding: 28rpx 32rpx;
}
.field {
  display: flex;
  align-items: center;
  padding: 26rpx 4rpx;
  border-bottom: 1rpx solid var(--c-div);
}
.label {
  width: 150rpx;
  font-size: 28rpx;
  color: var(--t2);
}
.input {
  flex: 1;
  font-size: 28rpx;
  color: var(--t1);
}
.ph {
  color: var(--t4);
}
.code-wrap {
  flex: 1;
  display: flex;
  align-items: center;
}
.code-wrap .input {
  flex: 1;
}
.get-code {
  font-size: 24rpx;
  color: var(--c-primary);
  padding: 8rpx 0 8rpx 16rpx;
  flex-shrink: 0;
}
.login-btn {
  background: var(--grad-primary);
  color: #fff;
  text-align: center;
  padding: 26rpx;
  border-radius: var(--r-pill);
  font-size: 32rpx;
  font-weight: 600;
  margin-top: 56rpx;
  box-shadow: var(--shadow-btn);
}
.hint {
  text-align: center;
  color: var(--t3);
  font-size: 24rpx;
  margin-top: 24rpx;
}
.wx-login {
  text-align: center;
  color: var(--c-wechat);
  font-size: 28rpx;
  margin: 40rpx 0 60rpx;
}
</style>
