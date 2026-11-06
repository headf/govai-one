<template>
  <view class="page">
    <view v-if="loading" class="sk-pane">
      <view class="sk-line" style="width: 66%; height: 44rpx;"></view>
      <view class="sk-line" style="width: 36%; height: 24rpx; margin-top: 20rpx;"></view>
      <view class="sk-line" style="height: 200rpx; margin-top: 32rpx;"></view>
      <view class="sk-line" style="width: 80%; height: 32rpx; margin-top: 20rpx;"></view>
    </view>
    <view v-else-if="error" class="empty-box">
      <view class="eb-ic">!</view>
      <text class="eb-text">事项加载失败，请检查网络后重试</text>
      <view class="eb-btn" @click="load">重新加载</view>
    </view>
    <template v-else-if="matter">
      <view class="head card">
        <text class="name">{{ matter.matterName }}</text>
        <text class="code">{{ matter.matterCode }}</text>
        <view class="tags">
          <text class="tag">{{ matter.categoryName }}</text>
          <text class="tag">{{ matter.department }}</text>
          <text class="tag">法定办结 {{ matter.deadlineDays }} 天</text>
        </view>
        <view class="apply-btn" hover-class="u-hover" @click="apply">立即申报</view>
      </view>

      <view class="section-title">办理指南</view>
      <view class="desc card">{{ matter.guideContent || matter.description || '暂无指南' }}</view>

      <view class="section-title">申报条件</view>
      <view class="cond card">
        <view v-if="matter.conditions && matter.conditions.length" class="cond-row" v-for="(c, i) in matter.conditions" :key="i">
          <view class="dot">{{ i + 1 }}</view>
          <text class="txt">{{ c }}</text>
        </view>
        <text v-else class="no">无特别限制条件</text>
      </view>

      <view class="section-title">所需材料</view>
      <view class="cond card">
        <view v-if="matter.materials && matter.materials.length" class="cond-row" v-for="(m, i) in matter.materials" :key="i">
          <view class="dot mat">{{ i + 1 }}</view>
          <text class="txt">{{ m }}</text>
        </view>
        <text v-else class="no">无需额外材料</text>
      </view>

      <view class="section-title">办理流程</view>
      <view class="cond card">
        <view v-if="matter.processSteps && matter.processSteps.length" class="cond-row" v-for="(p, i) in matter.processSteps" :key="i">
          <view class="dot">{{ i + 1 }}</view>
          <text class="txt">{{ p }}</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script>
import { fetchMatterDetail } from '../../api/index.js'
import { isLoggedIn, toLogin } from '../../utils/config.js'

export default {
  data() {
    return { id: 0, matter: null, loading: true, error: false }
  },
  onLoad(options) {
    this.id = Number(options.id)
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      this.error = false
      try {
        this.matter = await fetchMatterDetail(this.id)
      } catch (e) {
        this.error = true
      } finally {
        this.loading = false
      }
    },
    apply() {
      if (!isLoggedIn()) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        setTimeout(() => toLogin(), 600)
        return
      }
      uni.navigateTo({ url: '/pages/apply/form?matterId=' + this.id })
    }
  }
}
</script>

<style scoped>
.page {
  padding: 20rpx;
  padding-bottom: 60rpx;
}
.head {
  display: flex;
  flex-direction: column;
}
.name {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--t1);
}
.code {
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 8rpx;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: 18rpx;
}
.tag {
  font-size: 24rpx;
  color: var(--c-primary);
  background: var(--c-soft);
  padding: 6rpx 18rpx;
  border-radius: 12rpx;
  margin-right: 12rpx;
  margin-bottom: 6rpx;
}
.apply-btn {
  background: var(--grad-primary);
  color: #fff;
  text-align: center;
  padding: 24rpx;
  border-radius: var(--r-pill);
  font-size: 32rpx;
  font-weight: 600;
  margin-top: 28rpx;
  box-shadow: var(--shadow-btn);
}
.desc {
  font-size: 28rpx;
  color: var(--t2);
  line-height: 1.7;
}
.cond-row {
  display: flex;
  align-items: center;
  padding: 14rpx 0;
}
.dot {
  width: 40rpx;
  height: 40rpx;
  border-radius: 12rpx;
  background: var(--c-soft);
  color: var(--c-primary);
  text-align: center;
  line-height: 40rpx;
  font-size: 24rpx;
  font-weight: 600;
  margin-right: 18rpx;
  flex-shrink: 0;
}
.dot.mat {
  background: var(--c-soft2);
  color: var(--t3);
}
.txt {
  font-size: 28rpx;
  color: var(--t1);
  line-height: 1.6;
}
.no {
  font-size: 24rpx;
  color: var(--t3);
}

.sk-pane {
  padding: 40rpx 24rpx;
}

</style>
