<script>
export default {
  onLaunch() {
    console.log('GovAI One 群众端 launched')
  }
}
</script>

<style>
/* ============ 设计令牌（清新现代政务蓝） ============ */
page {
  /* 主蓝系 */
  --c-primary: #2F6BFF;
  --c-primary-hi: #6D97FF;
  --c-primary-deep: #2656CC;
  --grad-primary: linear-gradient(135deg, #6D97FF, #2F6BFF);
  --c-soft: #EAF0FF;      /* 浅蓝底 */
  --c-soft2: #F3F7FF;     /* 更浅底 */
  --c-line: #C1D3FF;      /* 主蓝描边 */
  /* 文字层级 */
  --t1: #1F2329;
  --t2: #4E5969;
  --t3: #86909C;
  --t4: #C9CDD4;
  /* 中性 */
  --c-div: #E5E6EB;
  --c-bg: #F4F6FA;
  /* 语义色(全站统一使用,避免跨页漂移) */
  --c-ok: #2FA566;            /* 成功/已办结统一绿 */
  --c-green: #2FA566;         /* 旧引用别名,指向同一绿 */
  --c-warn: #E6A23C;
  --c-gold: #F5A623;
  --c-star: #F5A623;          /* 星级评分 */
  --c-danger: #FF3B30;
  --c-danger-bg: #FFECED;
  --c-info: #4E8CF5;          /* 进行中/信息蓝 */
  --ring-primary: rgba(47, 107, 255, 0.16);   /* 时间线当前环/选中描边 */
  --c-wechat: #07C160;
  --wechat: #07C160;
  /* 质感 */
  --shadow-card: 0 8rpx 32rpx rgba(15, 40, 100, 0.07);
  --shadow-card-sm: 0 4rpx 16rpx rgba(15, 40, 100, 0.05);
  --shadow-btn: 0 8rpx 24rpx rgba(47, 107, 255, 0.25);
  --r-card: 24rpx;
  /* 与 Web 规范对齐的语义刻度(uni-app: 1px=2rpx @750) */
  --fs-caption: 24rpx;
  --fs-body: 28rpx;
  --fs-subtitle: 32rpx;
  --fs-title: 40rpx;
  --fs-display: 56rpx;
  --r-sm: 12rpx;
  --r-md: 24rpx;
  --r-lg: 32rpx;
  --r-pill: 999rpx;

  background-color: var(--c-bg);
  color: var(--t1);
  font-size: 28rpx;
  font-family: -apple-system, "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
}

/* ============ 全局通用类（质感进全局，布局留页面） ============ */
.container {
  padding: 20rpx;
}
.card {
  background: #fff;
  border-radius: var(--r-card);
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: var(--shadow-card);
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}
.card:hover {
  box-shadow: 0 14rpx 40rpx rgba(15, 40, 100, 0.12);
  transform: translateY(-2rpx);
}
.btn-primary,
.btn-soft,
.btn-danger {
  transition: filter 0.15s ease, transform 0.12s ease;
}
.btn-primary:active,
.btn-soft:active,
.btn-danger:active {
  filter: brightness(0.96);
  transform: scale(0.98);
}
.btn-primary {
  background: var(--grad-primary);
  color: #fff;
  border-radius: var(--r-pill);
  box-shadow: var(--shadow-btn);
}
.btn-ghost {
  background: #fff;
  color: var(--c-primary);
  border: 1rpx solid var(--c-line);
  border-radius: var(--r-pill);
}
.section-title {
  display: flex;
  align-items: center;
  font-size: 32rpx;
  font-weight: 600;
  color: var(--t1);
  margin: 28rpx 4rpx 16rpx;
}
.section-title::before {
  content: '';
  width: 8rpx;
  height: 30rpx;
  border-radius: 4rpx;
  background: var(--c-primary);
  margin-right: 14rpx;
}
.chip-soft {
  display: inline-flex;
  align-items: center;
  padding: 6rpx 16rpx;
  background: var(--c-soft);
  color: var(--c-primary);
  font-size: 24rpx;
  border-radius: 12rpx;
}
.empty {
  text-align: center;
  color: var(--t3);
  padding: 110rpx 0;
  font-size: 24rpx;
}
.nomore {
  text-align: center;
  color: var(--t4);
  font-size: 24rpx;
  padding: 24rpx;
}
/* 自定义底部导航避让（H5 四个 tab 页在根内容末尾放的占位） */
.tab-space {
  height: 140rpx;
}

/* ============ 通用交互反馈 ============ */
.u-hover {
  transform: scale(0.97);
  opacity: 0.9;
  transition: transform 0.12s ease, opacity 0.12s ease;
}

/* ============ 分段选择器(.seg) ============ */
.seg {
  display: flex;
  background: var(--c-soft2);
  border-radius: 999rpx;
  padding: 6rpx;
}
.seg-item {
  flex: 1;
  text-align: center;
  font-size: 24rpx;
  color: var(--t2);
  padding: 14rpx 0;
  border-radius: 999rpx;
  transition: all 0.18s ease;
}
.seg-on {
  background: #fff;
  color: var(--c-primary);
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(47, 107, 255, 0.14);
}

/* ============ 按钮变体(主题; 尺寸/占位由页面控制) ============ */
.btn-danger {
  background: var(--c-danger);
  color: #fff;
  border-radius: var(--r-pill);
  box-shadow: 0 8rpx 24rpx rgba(255, 59, 48, 0.22);
}
.btn-soft {
  background: var(--c-soft);
  color: var(--c-primary);
  border-radius: var(--r-pill);
}

/* ============ 状态胶囊 ============ */
.status-pill {
  display: inline-flex;
  align-items: center;
  font-size: 24rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  line-height: 1.5;
}
.sp-ok { background: #E8F7EF; color: var(--c-ok); }
.sp-warn { background: #FDF3E3; color: #B07A0E; }
.sp-danger { background: var(--c-danger-bg); color: var(--c-danger); }
.sp-info { background: var(--c-soft); color: var(--c-primary); }
.sp-hold { background: #F1F2F4; color: var(--t3); }

.tag-pill {
  display: inline-flex;
  align-items: center;
  font-size: 24rpx;
  color: var(--c-primary);
  background: var(--c-soft);
  border-radius: 12rpx;
  padding: 4rpx 12rpx;
  margin-right: 10rpx;
}

/* ============ 表单行 ============ */
.form-row {
  display: flex;
  align-items: center;
  padding: 26rpx 4rpx;
  border-bottom: 1rpx solid var(--c-div);
}
.form-row:last-child { border-bottom: none; }
.form-lbl {
  width: 170rpx;
  font-size: 28rpx;
  color: var(--t2);
  flex-shrink: 0;
}
.form-val { flex: 1; font-size: 28rpx; color: var(--t1); }
.form-inp { flex: 1; font-size: 28rpx; color: var(--t1); }
.ph { color: var(--t4); }

/* ============ 列表项 ============ */
.list-item {
  display: flex;
  align-items: center;
  padding: 26rpx 4rpx;
  border-bottom: 1rpx solid var(--c-div);
}
.list-item:last-child { border-bottom: none; }
.li-main { flex: 1; min-width: 0; }
.li-title {
  font-size: 32rpx;
  color: var(--t1);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.li-sub {
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 6rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.li-right {
  flex-shrink: 0;
  margin-left: 16rpx;
  display: flex;
  align-items: center;
}
.li-arrow { color: var(--t4); font-size: 32rpx; font-weight: 300; margin-left: 8rpx; }

/* ============ 时间线基础(办件进度) ============ */
.tl-dot { width: 18rpx; height: 18rpx; border-radius: 50%; flex-shrink: 0; }
.tl-ok { background: var(--c-ok); }
.tl-cur { background: var(--c-primary); box-shadow: 0 0 0 8rpx var(--ring-primary); }
.tl-wait { background: var(--c-div); }

/* ============ 空态(带图标与引导) ============ */
.empty-box {
  padding: 90rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.eb-ic {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: var(--c-soft2);
  color: var(--c-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56rpx;
  margin-bottom: 24rpx;
}
.eb-text { color: var(--t3); font-size: 24rpx; line-height: 1.6; }
.eb-btn {
  margin-top: 28rpx;
  background: var(--c-soft);
  color: var(--c-primary);
  padding: 14rpx 44rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}

/* ============ 骨架屏 ============ */
.sk-line {
  height: 32rpx;
  border-radius: 12rpx;
  background: linear-gradient(90deg, #eef1f6 25%, #f7f9fc 37%, #eef1f6 63%);
  background-size: 400% 100%;
  animation: sk-shine 1.4s ease infinite;
}
@keyframes sk-shine {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
</style>
