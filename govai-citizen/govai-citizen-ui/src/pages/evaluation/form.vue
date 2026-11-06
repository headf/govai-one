<template>
  <view class="page">
    <view class="card center">
      <text class="q">请为本次办件服务打分</text>
      <view class="stars">
        <text v-for="n in 5" :key="n" class="star" :class="{ on: n <= star }" @click="star = n">★</text>
      </view>      <text class="star-text">{{ starText }}</text>
    </view>

    <view class="section-title">快捷标签</view>
    <view class="tags card">
      <view v-for="(t, i) in tagList" :key="t" class="tag" :class="{ on: selectedTags.includes(t) }"
        hover-class="u-hover" @click="toggleTag(t)">{{ t }}</view>
    </view>

    <view class="section-title">评价内容</view>
    <view class="card">
      <textarea class="textarea" v-model="content" placeholder="说说您的办事体验(选填)" maxlength="500" />
      <text class="count">{{ content.length }}/500</text>
    </view>

    <view class="submit-btn" :class="{ disabled: submitting }" hover-class="u-hover" @click="submit">
      {{ submitting ? '提交中…' : '提交评价' }}</view>
  </view>
</template>

<script>
import { submitEvaluation } from '../../api/index.js'
import { starText } from '../../utils/format.js'

const ALL_TAGS = ['效率高', '服务好', '流程清晰', '材料简单', '等候久', '沟通不畅', '跑动多', '体验一般']

export default {
  data() {
    return { recordId: 0, star: 5, content: '', tagList: ALL_TAGS, selectedTags: [], submitting: false }
  },
  onLoad(options) {
    this.recordId = Number(options.recordId)
  },
  computed: {
    starText() {
      return starText(this.star)
    }
  },
  methods: {
    toggleTag(t) {
      const i = this.selectedTags.indexOf(t)
      if (i >= 0) this.selectedTags.splice(i, 1)
      else if (this.selectedTags.length < 3) this.selectedTags.push(t)
      else uni.showToast({ title: '最多选3个', icon: 'none' })
    },
    async submit() {
      if (!this.recordId) {
        uni.showToast({ title: '参数错误', icon: 'none' })
        return
      }
      if (this.submitting) return
      this.submitting = true
      uni.showLoading({ title: '提交中' })
      try {
        await submitEvaluation({
          recordId: this.recordId,
          starRating: this.star,
          content: this.content,
          tags: this.selectedTags
        })
        uni.showToast({ title: '评价成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 800)
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
.center {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 56rpx 20rpx;
}
.q {
  font-size: 32rpx;
  color: var(--t2);
}
.stars {
  display: flex;
  margin-top: 32rpx;
}
.star {
  font-size: 76rpx;
  color: #E3E6EC;
  margin: 0 12rpx;
  line-height: 1;
  transition: transform 0.16s ease, color 0.16s ease;
}
.star.on {
  color: var(--c-gold);
  transform: scale(1.08);
}
.star-text {
  font-size: 28rpx;
  color: var(--c-gold);
  margin-top: 24rpx;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  padding: 16rpx 12rpx;
}
.tag {
  font-size: 24rpx;
  color: var(--t2);
  background: var(--c-bg);
  padding: 14rpx 30rpx;
  border-radius: var(--r-pill);
  margin: 10rpx;
}
.tag.on {
  background: var(--c-soft);
  color: var(--c-primary);
  font-weight: 600;
  box-shadow: inset 0 0 0 1rpx var(--c-line);
}
.textarea {
  width: 100%;
  height: 240rpx;
  font-size: 28rpx;
  color: var(--t1);
  line-height: 1.6;
}
.count {
  display: block;
  text-align: right;
  font-size: 24rpx;
  color: var(--t4);
}
.submit-btn {
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
.submit-btn.disabled {
  opacity: 0.6;
}
</style>
