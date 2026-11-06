<template>
  <view class="page">
    <view v-if="stats" class="stats card">
      <view class="avg">
        <text class="num">{{ stats.avgStar || '0.0' }}</text>
        <view class="stars">
          <text v-for="n in 5" :key="n" class="sm" :class="{ on: n <= Math.round(stats.avgStar || 0) }">★</text>
        </view>
        <text class="cnt">共 {{ stats.total }} 条评价</text>
      </view>
      <view class="bars">
        <view class="bar-row" v-for="lv in levels" :key="lv.label">
          <text class="bl">{{ lv.label }}</text>
          <view class="track"><view class="fill" :style="{ width: percent(stats[lv.key], stats.total) }"></view></view>
          <text class="bc">{{ stats[lv.key] || 0 }}</text>
        </view>
      </view>
      <view class="rate">好评率 {{ stats.positiveRate || 0 }}%</view>
    </view>

    <view class="section-title">我的评价</view>
    <view v-if="list.length" class="rows card">
      <view v-for="e in list" :key="e.id" class="row-item">
        <view class="row-main">
          <view class="top">
            <text class="mn">{{ e.matterName }}</text>
            <view class="stars2"><text v-for="n in 5" :key="n" class="sm" :class="{ on: n <= e.starRating }">★</text></view>
          </view>
          <text class="content">{{ e.content || '该用户未填写文字评价' }}</text>
          <view v-if="e.tags && e.tags.length" class="tags">
            <text v-for="t in e.tags" :key="t" class="tag">{{ t }}</text>
          </view>
          <text class="time">{{ full(e.createTime) }}</text>
        </view>
      </view>
    </view>
    <view v-else class="empty-box">
      <view class="eb-ic">★</view>
      <text class="eb-text">还没有评价记录，办结后可在这里评价</text>
      <view class="eb-btn" hover-class="u-hover" @click="toHall">去办事大厅</view>
    </view>
  </view>
</template>

<script>
import { fetchMyEvaluations, fetchEvaluationStats } from '../../api/index.js'
import { formatTime } from '../../utils/format.js'

export default {
  data() {
    return { list: [], stats: null }
  },
  computed: {
    levels() {
      return [
        { label: '5星', key: 'fiveStar' },
        { label: '4星', key: 'fourStar' },
        { label: '3星', key: 'threeStar' },
        { label: '2星', key: 'twoStar' },
        { label: '1星', key: 'oneStar' }
      ]
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    toHall() {
      uni.switchTab({ url: '/pages/hall/hall' })
    },
    async load() {
      try {
        this.list = await fetchMyEvaluations()
        this.stats = await fetchEvaluationStats()
      } catch (e) { /* ignore */ }
    },
    percent(n, total) {
      return total ? (n / total) * 100 + '%' : '0%'
    },
    full(t) {
      return formatTime(t)
    }
  }
}
</script>

<style scoped>
.page {
  padding: 20rpx;
  padding-bottom: 60rpx;
}
.stats {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 34rpx;
}
.avg {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.num {
  font-size: 84rpx;
  font-weight: 700;
  color: var(--c-gold);
  line-height: 1;
}
.stars, .stars2 {
  display: flex;
  margin-top: 12rpx;
}
.stars2 {
  margin-top: 0;
}
.sm {
  font-size: 28rpx;
  color: #E3E6EC;
}
.sm.on {
  color: var(--c-gold);
}
.cnt {
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 10rpx;
}
.bars {
  width: 92%;
  margin-top: 28rpx;
}
.bar-row {
  display: flex;
  align-items: center;
  margin: 8rpx 0;
}
.bl {
  width: 60rpx;
  font-size: 24rpx;
  color: var(--t3);
}
.track {
  flex: 1;
  height: 14rpx;
  background: var(--c-bg);
  border-radius: 12rpx;
  margin: 0 12rpx;
  overflow: hidden;
}
.fill {
  height: 100%;
  background: var(--c-gold);
  border-radius: 12rpx;
}
.bc {
  font-size: 24rpx;
  color: var(--t3);
  width: 40rpx;
  text-align: right;
}
.rate {
  font-size: 24rpx;
  color: #2FA566;
  margin-top: 18rpx;
}
.eval {
  margin-bottom: 20rpx;
}
.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mn {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--t1);
  flex: 1;
  margin-right: 16rpx;
}
.content {
  display: block;
  font-size: 24rpx;
  color: var(--t2);
  margin-top: 16rpx;
  line-height: 1.7;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: 14rpx;
}
.tag {
  font-size: 24rpx;
  color: var(--c-primary);
  background: var(--c-soft);
  padding: 6rpx 18rpx;
  border-radius: 12rpx;
  margin: 6rpx 12rpx 0 0;
}
.rows {
  padding: 4rpx 24rpx;
}
.row-item {
  padding: 28rpx 0;
  border-bottom: 1rpx solid var(--c-div);
}
.row-item:last-child {
  border-bottom: none;
}
.row-main {
  min-width: 0;
}
.time {
  display: block;
  font-size: 24rpx;
  color: var(--t4);
  margin-top: 16rpx;
}
</style>
