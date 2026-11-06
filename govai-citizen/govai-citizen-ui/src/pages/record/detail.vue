<template>
  <view class="page" v-if="record">
    <view class="status-bar card" :class="record.status">
      <text class="s-text">{{ record.statusText }}</text>
      <text class="s-sub">{{ record.status === 'APPROVED' ? '办件已完成' : '正在为您办理' }}</text>
      <view v-if="record.status === 'APPROVED'" class="cert-link" hover-class="u-hover" @click="showCert = true">
        查看电子证明</view>
    </view>

    <!-- 电子办结证明样张 -->
    <view v-if="showCert" class="cert-mask" @click="showCert = false">
      <view class="cert" @click.stop>
        <text class="c-org">智慧政务一体化平台</text>
        <text class="c-title">办理结果告知单</text>
        <view class="c-row"><text class="c-k">事项名称</text><text class="c-v">{{ record.matterName }}</text></view>
        <view class="c-row"><text class="c-k">申报编号</text><text class="c-v">{{ record.recordCode }}</text></view>
        <view class="c-row"><text class="c-k">申请人</text><text class="c-v">{{ record.applicantName }}</text></view>
        <view class="c-row"><text class="c-k">办理结论</text><text class="c-v ok">予以办结</text></view>
        <view class="c-row"><text class="c-k">办结时间</text><text class="c-v">{{ fmt(record.finishTime) }}</text></view>
        <view class="c-seal">电子办结专用章</view>
        <text class="c-tip">本告知单由系统自动生成，可作办理结果凭证。</text>
        <view class="c-btn" @click="showCert = false">关闭</view>
      </view>
    </view>

    <view class="section-title">事项信息</view>
    <view class="info card">
      <view class="row"><text class="k">事项名称</text><text class="v">{{ record.matterName }}</text></view>
      <view class="row"><text class="k">事项编码</text><text class="v">{{ record.matterCode }}</text></view>
      <view class="row"><text class="k">实施部门</text><text class="v">{{ record.department }}</text></view>
      <view class="row"><text class="k">申报编号</text><text class="v">{{ record.recordCode }}</text></view>
    </view>

    <view class="section-title">申请人信息</view>
    <view class="info card">
      <view class="row"><text class="k">姓名</text><text class="v">{{ record.applicantName }}</text></view>
      <view class="row"><text class="k">手机号</text><text class="v">{{ record.applicantPhone }}</text></view>
      <view class="row"><text class="k">身份证号</text><text class="v">{{ record.applicantIdCard }}</text></view>
    </view>

    <view v-if="formEntries.length" class="section-title">申报信息</view>
    <view v-if="formEntries.length" class="info card">
      <view class="row" v-for="(f, i) in formEntries" :key="i"><text class="k">{{ f.k }}</text><text class="v">{{ f.v }}</text></view>
    </view>

    <view v-if="record.materials && record.materials.length" class="section-title">已提交材料</view>
    <view v-if="record.materials && record.materials.length" class="info card">
      <view class="row mat" v-for="(m, i) in record.materials" :key="i">
        <text class="mk">{{ m.materialName }}</text>
        <text v-if="m.fileUrl" class="v link" @click="preview(m.fileUrl)">查看 ›</text>
        <text v-else class="v elec">电子证照</text>
      </view>
    </view>

    <view class="actions">
      <view v-if="record.status === 'APPROVED'" class="btn" hover-class="u-hover" @click="goEval">评价办件</view>
      <view class="btn ghost" hover-class="u-hover" @click="goProgress">查看进度</view>
      <view v-if="canWithdraw" class="btn ghost danger" hover-class="u-hover" @click="withdraw">撤回申报</view>
    </view>
  </view>
</template>

<script>
import { fetchRecordDetail, withdrawRecord } from '../../api/index.js'
import { statusText, formatTime } from '../../utils/format.js'

export default {
  data() {
    return { id: 0, record: null, formEntries: [], showCert: false }
  },
  computed: {
    // 仅本地尚未真正提交到审批端的待受理办件可撤回
    canWithdraw() {
      const r = this.record
      return r && r.status === 'PENDING' && !r.approvalInstanceId
    }
  },
  onLoad(options) {
    this.id = Number(options.id)
  },
  onShow() {
    this.load()
  },
  methods: {
    async load() {
      try {
        const r = await fetchRecordDetail(this.id)
        r.statusText = statusText(r.status)
        this.record = r
        this.formEntries = Object.keys(r.formData || {}).map((k) => ({ k, v: r.formData[k] }))
      } catch (e) { /* ignore */ }
    },
    preview(url) {
      if (!url) return
      uni.previewImage({ urls: [url] }).catch(() => {
        uni.showModal({ title: '文件', content: url, showCancel: false })
      })
    },
    fmt(t) {
      return formatTime(t)
    },
    goProgress() {
      uni.navigateTo({ url: '/pages/record/progress?id=' + this.id })
    },
    goEval() {
      uni.navigateTo({ url: '/pages/evaluation/form?recordId=' + this.id })
    },
    withdraw() {
      uni.showModal({
        title: '撤回申报',
        content: '确定撤回该申报吗？撤回后如需办理请重新提交。',
        success: async (res) => {
          if (!res.confirm) return
          uni.showLoading({ title: '撤回中' })
          try {
            await withdrawRecord(this.id)
            uni.hideLoading()
            uni.showToast({ title: '已撤回', icon: 'success' })
            this.load()
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.page {
  padding: 20rpx;
  padding-bottom: 60rpx;
}
.status-bar {
  padding: 40rpx;
  color: #fff;
  display: flex;
  flex-direction: column;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 40, 100, 0.08);
}
.status-bar.APPROVED { background: linear-gradient(135deg, #52C876, #2FA566); }
.status-bar.APPROVING, .status-bar.PENDING { background: linear-gradient(135deg, #F5B13E, #E88B1F); }
.status-bar.REJECTED { background: linear-gradient(135deg, #FF7A72, #E8352B); }
.s-text {
  font-size: 40rpx;
  font-weight: 700;
}
.s-sub {
  font-size: 24rpx;
  opacity: 0.92;
  margin-top: 10rpx;
}
.info {
  padding: 8rpx 24rpx;
}
.row {
  display: flex;
  padding: 20rpx 4rpx;
  border-bottom: 1rpx solid var(--c-div);
}
.row:last-child {
  border-bottom: none;
}
.k {
  width: 190rpx;
  font-size: 24rpx;
  color: var(--t3);
  flex-shrink: 0;
}
.v {
  flex: 1;
  font-size: 24rpx;
  color: var(--t1);
  line-height: 1.5;
}
.row.mat {
  justify-content: space-between;
}
.mk {
  flex: 1;
  font-size: 24rpx;
  color: var(--t1);
  margin-right: 20rpx;
  line-height: 1.5;
}
.link {
  color: var(--c-primary);
  font-size: 24rpx;
  flex-shrink: 0;
}
.elec {
  color: var(--c-ok);
  font-size: 24rpx;
  flex-shrink: 0;
}
.actions {
  margin-top: 40rpx;
  display: flex;
}
.btn {
  flex: 1;
  background: var(--grad-primary);
  color: #fff;
  text-align: center;
  padding: 24rpx;
  border-radius: var(--r-pill);
  font-size: 32rpx;
  font-weight: 600;
  margin: 0 10rpx;
  box-shadow: var(--shadow-btn);
}
.btn.ghost {
  background: #fff;
  color: var(--c-primary);
  border: 1rpx solid var(--c-line);
  box-shadow: none;
  font-weight: 500;
}
.cert-link {
  margin-top: 22rpx;
  align-self: center;
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  border: 1rpx solid rgba(255, 255, 255, 0.5);
  padding: 10rpx 34rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}
.cert-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  z-index: 99;
}
.cert {
  width: 100%;
  background: #fff;
  border-radius: 24rpx;
  padding: 44rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1rpx solid var(--c-div);
  max-height: 88vh;
  overflow-y: auto;
}
.c-org {
  font-size: 24rpx;
  color: var(--t3);
  letter-spacing: 2rpx;
}
.c-title {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--t1);
  margin: 12rpx 0 30rpx;
  letter-spacing: 4rpx;
}
.c-row {
  width: 100%;
  display: flex;
  padding: 16rpx 0;
  border-bottom: 1rpx dashed var(--c-div);
}
.c-k {
  width: 180rpx;
  font-size: 28rpx;
  color: var(--t3);
  flex-shrink: 0;
}
.c-v {
  flex: 1;
  font-size: 28rpx;
  color: var(--t1);
}
.c-v.ok {
  color: var(--c-ok);
  font-weight: 600;
}
.c-seal {
  margin-top: 30rpx;
  width: 240rpx;
  height: 150rpx;
  border: 6rpx double var(--c-danger);
  border-radius: 24rpx;
  color: var(--c-danger);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
  transform: rotate(-8deg);
  opacity: 0.85;
}
.c-tip {
  font-size: 24rpx;
  color: var(--t4);
  margin-top: 24rpx;
}
.c-btn {
  margin-top: 28rpx;
  background: var(--grad-primary);
  color: #fff;
  padding: 20rpx 80rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 600;
}

</style>
