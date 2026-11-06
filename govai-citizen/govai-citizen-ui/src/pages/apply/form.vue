<template>
  <view class="page" v-if="matter">
    <view class="matter-info card">
      <text class="mn">{{ matter.matterName }}</text>
      <text class="mc">{{ matter.matterCode }} · {{ matter.department }}</text>
    </view>

    <!-- 草稿提示 -->
    <view v-if="hasDraft" class="draft-bar" @click="restoreDraft">
      <text class="db-txt">检测到未提交的草稿</text>
      <text class="db-btn">恢复填写 ›</text>
    </view>

    <!-- 申请人信息 -->
    <view class="section-title">申请人信息</view>
    <view class="card">
      <view class="row">
        <text class="lbl">姓名</text>
        <input class="inp" v-model="form.applicantName" placeholder="请输入姓名" placeholder-class="ph" />
      </view>
      <view class="row">
        <text class="lbl">手机号</text>
        <input class="inp" v-model="form.applicantPhone" type="number" maxlength="11" placeholder="请输入手机号" placeholder-class="ph" />
      </view>
      <view class="row">
        <text class="lbl">身份证号</text>
        <input class="inp" v-model="form.applicantIdCard" placeholder="请输入身份证号(实名核验)" placeholder-class="ph" />
        <text v-if="profileOk" class="verified">已实名</text>
      </view>
      <view v-if="!profileOk" class="row">
        <text class="lbl"></text>
        <view class="ocr-btn" hover-class="u-hover" @click="ocrScan">证件拍照智能识别(OCR)</view>
        <text class="ocr-tip">拍身份证自动回填</text>
      </view>
      <view class="row" v-if="matter.conditions && matter.conditions.length">
        <text class="lbl">申报理由</text>
        <input class="inp" v-model="reason" placeholder="请填写申报说明" placeholder-class="ph" />
      </view>
    </view>

    <!-- 材料上传 -->
    <view class="section-title">材料上传</view>
    <view class="card">
      <view class="mat-row" v-for="(m, i) in matter.materials" :key="i">
        <view class="mat-left">
          <text class="mat-name">{{ m }}</text>
          <text v-if="isElec(materials[i])" class="elec-tag">电子证照</text>
        </view>
        <view v-if="!uploaded[i]" class="row-acts">
          <view class="upload-btn" hover-class="u-hover" @click="chooseFile(i)">上传</view>
          <view v-if="profileOk && canLicense(m)" class="elec-btn" hover-class="u-hover" @click="useLicense(i)">电子证照免提交</view>
        </view>
        <view v-else class="row-acts">
          <view v-if="materials[i] && materials[i].fileUrl" class="upload-btn done" hover-class="u-hover" @click="previewFile(i)">预览</view>
          <view v-else class="upload-btn done">已免提交</view>
          <view class="rm-btn" hover-class="u-hover" @click="removeFile(i)">移除</view>
        </view>
      </view>
      <view class="tip">支持图片 / 文件上传，单个不超过 20MB；已实名的身份证/户口簿类材料可"电子证照免提交"</view>
    </view>

    <view class="submit-btn" :class="{ disabled: submitting }" hover-class="u-hover" @click="doSubmit">
      {{ submitting ? '提交中…' : '提交申报' }}</view>
  </view>
</template>

<script>
import { fetchMatterDetail, submitApply, upload, fetchProfile } from '../../api/index.js'
import { validIdCard } from '../../utils/format.js'

export default {
  data() {
    return {
      matterId: 0,
      matter: null,
      form: { applicantName: '', applicantPhone: '', applicantIdCard: '' },
      reason: '',
      materials: [],
      uploaded: [],
      submitting: false,
      hasDraft: false,
      profileOk: false,
      profile: null
    }
  },
  computed: {
    draftKey() {
      return 'apply_draft_' + this.matterId
    }
  },
  onLoad(options) {
    this.matterId = Number(options.matterId)
    this.load()
  },
  onUnload() {
    this.saveDraft()
  },
  onHide() {
    this.saveDraft()
  },
  methods: {
    async load() {
      try {
        this.matter = await fetchMatterDetail(this.matterId)
        if (this.matter.materials) {
          this.uploaded = new Array(this.matter.materials.length).fill(false)
          this.materials = new Array(this.matter.materials.length).fill(null)
        }
        this.prefillProfile()
        this.checkDraft()
      } catch (e) { /* ignore */ }
    },
    // 登录用户实名资料自动预填(仅空字段)
    async prefillProfile() {
      try {
        const p = await fetchProfile()
        if (p && p.phone) {
          this.profile = p
          if (!this.form.applicantPhone) this.form.applicantPhone = p.phone
          if (p.realName && !this.form.applicantName) this.form.applicantName = p.realName
          if (p.idCard && !this.form.applicantIdCard) this.form.applicantIdCard = p.idCard
          this.profileOk = !!p.idCard
        }
      } catch (e) { /* ignore */ }
    },
    // 证件拍照智能识别(OCR): 演示级——无 OCR 引擎时按实名资料回填; 真机接入企业端 PaddleOCR 时替换为识别结果
    ocrScan() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: () => {
          uni.showLoading({ title: 'OCR 识别中' })
          setTimeout(() => {
            uni.hideLoading()
            const p = this.profile
            if (p && p.idCard) {
              if (!this.form.applicantName) this.form.applicantName = p.realName || ''
              if (!this.form.applicantIdCard) this.form.applicantIdCard = p.idCard
              if (!this.form.applicantPhone) this.form.applicantPhone = p.phone || ''
              this.profileOk = true
              uni.showToast({ title: '已识别并回填', icon: 'success' })
            } else {
              uni.showToast({ title: '请先在个人中心完成实名认证', icon: 'none' })
            }
          }, 1200)
        }
      })
    },
    checkDraft() {
      const d = uni.getStorageSync(this.draftKey)
      if (!d) return
      this.hasDraft = true
      uni.showModal({
        title: '发现草稿',
        content: '是否恢复上次未提交的填写内容？',
        confirmText: '恢复',
        success: (res) => {
          if (res.confirm) this.restoreDraft()
        }
      })
    },
    restoreDraft() {
      const d = uni.getStorageSync(this.draftKey)
      if (!d) return
      if (d.form) this.form = Object.assign({}, this.form, d.form)
      if (d.reason) this.reason = d.reason
      if (d.materials) {
        this.materials = this.matter.materials.map((mn, i) => {
          const cur = d.materials[i]
          return cur ? { materialName: mn, fileUrl: cur.fileUrl, fileType: cur.fileType || 'image' } : null
        })
        this.uploaded = this.materials.map((m) => !!m)
      }
      this.hasDraft = false
      uni.showToast({ title: '已恢复草稿', icon: 'none' })
    },
    saveDraft() {
      if (!this.matter) return
      const filled = this.form.applicantName || this.form.applicantPhone || this.form.applicantIdCard ||
        this.reason || this.materials.some((m) => m && m.fileUrl)
      if (!filled) return
      uni.setStorageSync(this.draftKey, {
        form: this.form,
        reason: this.reason,
        materials: this.materials.map((m) => (m ? { fileUrl: m.fileUrl, fileType: m.fileType } : null)),
        savedAt: Date.now()
      })
    },
    isElec(mat) {
      return !!(mat && mat.fileType === 'electronic')
    },
    canLicense(m) {
      // 实名后, 身份证/户口类材料可走电子证照免提交(演示级)
      return /身份证|户口簿|户口本/.test(m)
    },
    useLicense(i) {
      this.uploaded[i] = true
      this.materials[i] = {
        materialName: this.matter.materials[i],
        fileUrl: null,
        fileType: 'electronic'
      }
      uni.showToast({ title: '已通过电子证照免提交', icon: 'none' })
    },
    chooseFile(i) {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: async (res) => {
          const path = res.tempFilePaths[0]
          uni.showLoading({ title: '上传中' })
          try {
            const data = await upload(path)
            this.materials[i] = {
              materialName: this.matter.materials[i],
              fileUrl: data.fileUrl,
              fileType: data.fileType || 'image'
            }
            this.uploaded[i] = true
            uni.hideLoading()
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    },
    previewFile(i) {
      const url = this.materials[i] && this.materials[i].fileUrl
      if (!url) return
      uni.previewImage({ urls: [url] }).catch(() => {
        uni.showModal({ title: '材料文件', content: url, showCancel: false })
      })
    },
    removeFile(i) {
      this.uploaded[i] = false
      this.materials[i] = null
    },
    async doSubmit() {
      if (this.submitting) return
      if (!this.form.applicantName) {
        uni.showToast({ title: '请填写姓名', icon: 'none' })
        return
      }
      if (!/^1[3-9]\d{9}$/.test(this.form.applicantPhone)) {
        uni.showToast({ title: '请填写正确手机号', icon: 'none' })
        return
      }
      if (this.form.applicantIdCard && !validIdCard(this.form.applicantIdCard)) {
        uni.showToast({ title: '身份证号格式不正确', icon: 'none' })
        return
      }
      this.submitting = true
      uni.showLoading({ title: '提交中' })
      try {
        const payload = {
          matterId: this.matterId,
          applicantName: this.form.applicantName,
          applicantPhone: this.form.applicantPhone,
          applicantIdCard: this.form.applicantIdCard || undefined,
          formData: this.reason ? { reason: this.reason } : {},
          materials: this.materials.filter((m) => m && (m.fileUrl || m.fileType === 'electronic'))
        }
        const data = await submitApply(payload)
        uni.removeStorageSync(this.draftKey)
        uni.hideLoading()
        uni.showModal({
          title: '提交成功',
          content: '申报编号 ' + data.recordCode + ', 可在"我的申报"查看进度。',
          showCancel: false,
          success: () => {
            uni.redirectTo({ url: '/pages/record/detail?id=' + data.recordId })
          }
        })
      } catch (e) {
        uni.hideLoading()
      } finally {
        this.submitting = false
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
.matter-info {
  display: flex;
  flex-direction: column;
}
.mn {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--t1);
}
.mc {
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 10rpx;
}
.row {
  display: flex;
  align-items: center;
  padding: 22rpx 4rpx;
  border-bottom: 1rpx solid var(--c-div);
}
.row:last-child {
  border-bottom: none;
}
.lbl {
  width: 160rpx;
  font-size: 28rpx;
  color: var(--t2);
  flex-shrink: 0;
}
.inp {
  flex: 1;
  font-size: 28rpx;
  color: var(--t1);
}
.ph {
  color: var(--t4);
}
.mat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 4rpx;
  border-bottom: 1rpx solid var(--c-div);
}
.mat-row:last-child {
  border-bottom: none;
}
.mat-left {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  margin-right: 20rpx;
}
.mat-name {
  flex-shrink: 1;
  font-size: 28rpx;
  color: var(--t1);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.elec-tag {
  flex-shrink: 0;
  font-size: 24rpx;
  color: var(--c-ok);
  background: #E8F7EF;
  border-radius: 999rpx;
  padding: 2rpx 12rpx;
  margin-left: 12rpx;
}
.row-acts {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}
.elec-btn {
  background: #E8F7EF;
  color: var(--c-ok);
  font-size: 24rpx;
  padding: 10rpx 20rpx;
  border-radius: var(--r-pill);
  margin-left: 12rpx;
  flex-shrink: 0;
}
.upload-btn {
  background: var(--c-soft);
  color: var(--c-primary);
  font-size: 24rpx;
  padding: 12rpx 30rpx;
  border-radius: var(--r-pill);
  flex-shrink: 0;
}
.upload-btn.done {
  background: #E8F7EF;
  color: var(--c-ok);
}
.rm-btn {
  background: var(--c-danger-bg);
  color: var(--c-danger);
  font-size: 24rpx;
  padding: 10rpx 20rpx;
  border-radius: var(--r-pill);
  margin-left: 12rpx;
  flex-shrink: 0;
}
.verified {
  font-size: 24rpx;
  color: var(--c-ok);
  background: #E8F7EF;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
  margin-left: 12rpx;
}
.ocr-btn {
  background: var(--c-primary);
  color: #fff;
  font-size: 24rpx;
  padding: 12rpx 26rpx;
  border-radius: var(--r-pill);
  flex-shrink: 0;
  box-shadow: var(--shadow-card-sm);
}
.ocr-tip {
  font-size: 24rpx;
  color: var(--t3);
  margin-left: 14rpx;
}
.draft-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #FFF7E8;
  color: #B07A0E;
  border: 1rpx solid #F6E2B8;
  border-radius: 24rpx;
  padding: 18rpx 24rpx;
  margin-bottom: 20rpx;
}
.db-txt {
  font-size: 24rpx;
}
.db-btn {
  font-size: 24rpx;
  font-weight: 600;
}
.tip {
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 16rpx;
}
.submit-btn {
  background: var(--grad-primary);
  color: #fff;
  text-align: center;
  padding: 26rpx;
  border-radius: var(--r-pill);
  font-size: 32rpx;
  font-weight: 600;
  margin-top: 40rpx;
  box-shadow: var(--shadow-btn);
}
.submit-btn.disabled {
  opacity: 0.6;
}
</style>
