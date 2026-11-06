<template>
  <view class="page">
    <view class="avatar-row card" @click="chooseAvatar">
      <view class="avatar">{{ avatarChar }}</view>
      <text class="change">点击更换头像</text>
    </view>

    <view class="card form">
      <view class="row">
        <text class="k">手机号</text>
        <text class="v muted">{{ store.userInfo && store.userInfo.phone }} (登录账号不可修改)</text>
      </view>
      <view class="row">
        <text class="k">姓名</text>
        <input class="inp" v-model="realName" placeholder="请输入姓名" placeholder-class="ph" />
      </view>
      <view class="row">
        <text class="k">身份证号</text>
        <input class="inp" v-model="idCard" maxlength="18" placeholder="实名认证, 选填" placeholder-class="ph" />
      </view>
    </view>

    <view class="save-btn" @click="save">保存</view>
    <text class="tip">实名认证后可使用人脸核身等高级服务并享受更精准的政策推荐</text>
  </view>
</template>

<script>
import { useUserStore } from '../../stores/user.js'
import { upload } from '../../api/index.js'
import { validIdCard } from '../../utils/format.js'

export default {
  data() {
    return {
      store: useUserStore(),
      realName: '',
      idCard: '',
      avatarUrl: ''
    }
  },
  computed: {
    avatarChar() {
      const name = this.realName || this.store.displayName || ''
      return name ? name.charAt(0) : '客'
    }
  },
  onLoad() {
    this.load()
  },
  methods: {
    load() {
      const info = this.store.userInfo || {}
      this.realName = info.realName || ''
      this.idCard = info.idCard || ''
      this.avatarUrl = info.avatarUrl || ''
    },
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        success: async (res) => {
          uni.showLoading({ title: '上传中' })
          try {
            const data = await upload(res.tempFilePaths[0])
            this.avatarUrl = data.fileUrl
            uni.hideLoading()
            uni.showToast({ title: '头像已选择, 保存后生效', icon: 'none' })
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    },
    async save() {
      if (this.idCard && !validIdCard(this.idCard)) {
        uni.showToast({ title: '身份证号格式不正确', icon: 'none' })
        return
      }
      uni.showLoading({ title: '保存中' })
      try {
        await this.store.updateProfile({
          realName: this.realName,
          idCard: this.idCard || undefined,
          avatarUrl: this.avatarUrl || undefined
        })
        uni.hideLoading()
        uni.showToast({ title: '保存成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 700)
      } catch (e) {
        uni.hideLoading()
      }
    }
  }
}
</script>

<style scoped>
.page {
  padding: 20rpx;
  padding-bottom: 60rpx;
}
.avatar-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx;
}
.avatar {
  width: 148rpx;
  height: 148rpx;
  border-radius: 50%;
  background: var(--c-soft);
  color: var(--c-primary);
  font-size: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  overflow: hidden;
}
.change {
  font-size: 24rpx;
  color: var(--c-primary);
  margin-top: 20rpx;
}
.form {
  padding: 8rpx 24rpx;
}
.row {
  display: flex;
  align-items: center;
  padding: 26rpx 4rpx;
  border-bottom: 1rpx solid var(--c-div);
}
.row:last-child {
  border-bottom: none;
}
.k {
  width: 170rpx;
  font-size: 28rpx;
  color: var(--t2);
  flex-shrink: 0;
}
.v {
  flex: 1;
  font-size: 28rpx;
  color: var(--t1);
}
.v.muted {
  color: var(--t3);
  font-size: 24rpx;
}
.inp {
  flex: 1;
  font-size: 28rpx;
  color: var(--t1);
}
.ph {
  color: var(--t4);
}
.save-btn {
  background: var(--grad-primary);
  color: #fff;
  text-align: center;
  padding: 26rpx;
  border-radius: var(--r-pill);
  font-size: 32rpx;
  font-weight: 600;
  margin-top: 44rpx;
  box-shadow: var(--shadow-btn);
}
.tip {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 24rpx;
  line-height: 1.6;
}
</style>
