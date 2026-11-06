<template>
  <div class="dashboard">
    <div class="dash-header">
      <div class="dash-title">GovAI 综合监管数据大屏</div>
      <div class="dash-meta">
        <span>生成时间：{{ overview.generateTime || '—' }}</span>
        <span class="live-dot" :class="{ on: wsOnline }">实时推送</span>
        <el-button size="small" round @click="goAdmin">后台管理</el-button>
      </div>
    </div>

    <!-- KPI 卡片 -->
    <div class="kpi-row">
      <div class="kpi-card" v-for="k in kpis" :key="k.key" :style="{ '--accent': k.accent }">
        <div class="kpi-icon">
          <el-icon :size="24"><component :is="k.icon" /></el-icon>
        </div>
        <div class="kpi-mid">
          <div class="kpi-value">{{ k.fmt(overview[k.key]) }}</div>
          <div class="kpi-label">{{ k.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <div class="chart-box chart-large">
        <div class="chart-title">近 7 天办件趋势</div>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">日均办理时长(h)</div>
        <div ref="satRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">办结率</div>
        <div ref="rateRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">信用等级分布（监管对象 {{ overview.monitorEnterpriseCount || 0 }} 家）</div>
        <div ref="creditRef" class="chart"></div>
      </div>
    </div>

    <div class="rank-row">
      <div class="rank-box">
        <div class="chart-title">热门事项 Top</div>
        <div class="rank-list">
          <div v-for="(h, i) in hList" :key="i" class="rank-item">
            <span class="rk" :class="{ top: i < 3 }">{{ i + 1 }}</span>
            <span class="rn">{{ h.matterName }}</span>
            <span class="rc">{{ h.count }} 件</span>
          </div>
          <div v-if="!hList.length" class="rank-empty">暂无办件数据</div>
        </div>
      </div>
      <div class="rank-box">
        <div class="chart-title">办结耗时最长 Top（h）</div>
        <div class="rank-list">
          <div v-for="(d, i) in dList" :key="i" class="rank-item">
            <span class="rk" :class="{ top: i < 3 }">{{ i + 1 }}</span>
            <span class="rn">{{ d.matterName }}</span>
            <span class="rc">{{ d.processDurationHours }}h</span>
          </div>
          <div v-if="!dList.length" class="rank-empty">暂无办结数据</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Tickets, Timer, CircleCheck, CircleClose, TrendCharts, Histogram, Bell, Warning,
} from '@element-plus/icons-vue'
import { statsApi } from '../api/modules'
import { wsConnect, wsSubscribe } from '../utils/webSocket'

const overview = reactive({
  totalApplications: 0, pendingCount: 0, approvedCount: 0, rejectedCount: 0,
  avgProcessHours: 0, doneRate: 0, alertTotal: 0, alertPending: 0, generateDate: '',
  monitorEnterpriseCount: 0, creditLevelDist: {},
})
const daily = ref([])
const wsOnline = ref(false)
const hList = ref([])
const dList = ref([])

const kpis = [
  { key: 'totalApplications', label: '办件量', icon: markRaw(Tickets), accent: '#5aa8ff', fmt: (v) => (v ?? 0).toLocaleString() },
  { key: 'pendingCount', label: '在办', icon: markRaw(Timer), accent: '#ffb64d', fmt: (v) => (v ?? 0).toLocaleString() },
  { key: 'approvedCount', label: '办结', icon: markRaw(CircleCheck), accent: '#2dd4bf', fmt: (v) => (v ?? 0).toLocaleString() },
  { key: 'rejectedCount', label: '驳回', icon: markRaw(CircleClose), accent: '#ff7a7a', fmt: (v) => (v ?? 0).toLocaleString() },
  { key: 'avgProcessHours', label: '平均时长(h)', icon: markRaw(TrendCharts), accent: '#5aa8ff', fmt: (v) => Number(v ?? 0).toFixed(1) },
  { key: 'doneRate', label: '办结率(%)', icon: markRaw(Histogram), accent: '#2dd4bf', fmt: (v) => Number(v ?? 0).toFixed(1) },
  { key: 'alertTotal', label: '预警总数', icon: markRaw(Bell), accent: '#ffb64d', fmt: (v) => (v ?? 0).toLocaleString() },
  { key: 'alertPending', label: '未处理预警', icon: markRaw(Warning), accent: '#ff7a7a', fmt: (v) => (v ?? 0).toLocaleString() },
]

const trendRef = ref()
const satRef = ref()
const rateRef = ref()
const creditRef = ref()
const router = useRouter()
let trendChart = null
let satChart = null
let rateChart = null
let creditChart = null
let pollTimer = null
let unsubscribe = null

async function fetchOverview() {
  try {
    const data = await statsApi.overview()
    Object.assign(overview, data)
    overview.generateDate = data.generateTime || ''
  } catch (e) { /* 后端未启动等，交由拦截器提示 */ }
}

async function fetchDaily() {
  try {
    daily.value = await statsApi.daily({})
  } catch (e) { /* ignore */ }
}

async function fetchRanks() {
  try {
    hList.value = await statsApi.hotMatters()
    dList.value = await statsApi.topDuration(6)
  } catch (e) { /* ignore */ }
}

// ===== ECharts 渲染 =====
const axisTextColor = '#8a94a6'
const splitColor = 'rgba(255,255,255,0.10)'

function renderTrend() {
  const rows = daily.value || []
  const dates = rows.map((r) => r.statDate)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['办件量', '办结', '驳回', '超时'], textStyle: { color: '#cfd5e6' } },
    grid: { left: 50, right: 20, top: 40, bottom: 25 },
    xAxis: { type: 'category', data: dates, axisLabel: { color: axisTextColor }, axisTick: { show: false } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: splitColor } }, axisLabel: { color: axisTextColor } },
    series: [
      { name: '办件量', type: 'line', smooth: true, symbol: 'none', data: rows.map((r) => r.totalApplications), lineStyle: { width: 2 }, itemStyle: { color: '#5aa8ff' }, areaStyle: { color: 'rgba(90,168,255,0.12)' } },
      { name: '办结', type: 'line', smooth: true, symbol: 'none', data: rows.map((r) => r.approvedCount), lineStyle: { width: 2 }, itemStyle: { color: '#2dd4bf' } },
      { name: '驳回', type: 'line', smooth: true, symbol: 'none', data: rows.map((r) => r.rejectedCount), lineStyle: { width: 2 }, itemStyle: { color: '#ff7a7a' } },
      { name: '超时', type: 'line', smooth: true, symbol: 'none', data: rows.map((r) => r.timeoutCount), lineStyle: { width: 2 }, itemStyle: { color: '#ffb64d' } },
    ],
  })
}

function renderSat() {
  const rows = daily.value || []
  satChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v) => (v == null ? '-' : `${Number(v).toFixed(1)} h`) },
    grid: { left: 45, right: 20, top: 25, bottom: 25 },
    xAxis: { type: 'category', data: rows.map((r) => r.statDate), axisLabel: { color: axisTextColor }, axisTick: { show: false } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: splitColor } }, axisLabel: { color: axisTextColor } },
    series: {
      name: '日均时长', type: 'bar', barMaxWidth: 30,
      data: rows.map((r) => r.avgProcessHours),
      itemStyle: { color: '#2dd4bf', borderRadius: [4, 4, 0, 0] },
    },
  })
}

function renderRate() {
  const val = Number(overview.doneRate ?? 0)
  rateChart.setOption({
    tooltip: { formatter: () => `办结率：${val.toFixed(1)}%` },
    series: [{
      type: 'gauge',
      startAngle: 210, endAngle: -30,
      min: 0, max: 100,
      radius: '95%', center: ['50%', '62%'],
      progress: { show: true, width: 14, itemStyle: { color: '#5aa8ff' } },
      axisLine: { lineStyle: { width: 14, color: [[1, 'rgba(255,255,255,0.12)']] } },
      pointer: { show: false }, axisTick: { show: false }, splitLine: { show: false },
      detail: { valueAnimation: true, fontSize: 26, color: '#fff', offsetCenter: [0, '78%'], formatter: (p) => p + '%' },
      data: [{ value: val }],
    }],
  })
}

function renderCharts() {
  renderTrend()
  renderSat()
  renderRate()
  renderCreditDist()
}

function renderCreditDist() {
  const dist = overview.creditLevelDist || {}
  const levelMeta = { A: 'A级(85-100)', B: 'B级(70-84)', C: 'C级(55-69)', D: 'D级(40-54)' }
  const colors = { A: '#2dd4bf', B: '#5aa8ff', C: '#ffb64d', D: '#ff7a7a' }
  const data = ['A', 'B', 'C', 'D'].filter((lv) => (dist[lv] ?? 0) > 0).map((lv) => ({
    name: levelMeta[lv], value: dist[lv] ?? 0, itemStyle: { color: colors[lv] },
  }))
  creditChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 家 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#b7c0d6' }, itemWidth: 12, itemHeight: 12 },
    series: [{
      type: 'pie', radius: ['52%', '74%'], center: ['50%', '44%'],
      label: { show: true, color: '#cfd5e6', fontSize: 12 },
      data,
    }],
  })
}

async function refreshData() {
  await Promise.all([fetchOverview(), fetchDaily(), fetchRanks()])
  renderCharts()
}

function goAdmin() {
  router.push('/admin/users')
}

onMounted(async () => {
  await Promise.all([fetchOverview(), fetchDaily(), fetchRanks()])
  await nextTick()
  trendChart = echarts.init(trendRef.value)
  satChart = echarts.init(satRef.value)
  rateChart = echarts.init(rateRef.value)
  creditChart = echarts.init(creditRef.value)
  renderCharts()

  // 30s 定时轮询兜底
  pollTimer = setInterval(refreshData, 30000)

  // WebSocket 实时推送
  wsConnect()
  unsubscribe = wsSubscribe((payload) => {
    if (payload && payload.event === 'approval.updated') {
      wsOnline.value = true
      refreshData()
    }
  })
})

onUnmounted(() => {
  clearInterval(pollTimer)
  if (unsubscribe) unsubscribe()
  trendChart?.dispose()
  satChart?.dispose()
  rateChart?.dispose()
  creditChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  min-height: 100%;
  padding: 20px;
  background: linear-gradient(165deg, #0a1749 0%, #0d2260 55%, #0a1749 100%);
  color: #fff;
}
.dash-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.dash-title { font-size: 26px; font-weight: 700; letter-spacing: 4px; }
.dash-meta { display: flex; gap: 16px; align-items: center; color: #aab2c8; font-size: 13px; }
.live-dot { display: inline-flex; align-items: center; gap: 6px; color: #757f9a; }
.live-dot::before { content: ''; width: 8px; height: 8px; border-radius: 50%; background: currentColor; }
.live-dot.on { color: #13c2c2; }
.live-dot.on::before { box-shadow: 0 0 8px currentColor; }

.kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.kpi-card {
  position: relative;
  display: flex; align-items: center; gap: 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px; padding: 16px 18px 16px 22px;
  overflow: hidden;
}
.kpi-card::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px;
  background: var(--accent);
}
.kpi-icon {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  color: var(--accent);
}
.kpi-value { font-size: 26px; font-weight: 700; line-height: 1.1; color: #fff; }
.kpi-label { font-size: 13px; color: #9aa3bd; margin-top: 4px; }

.chart-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.chart-box {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px; padding: 12px;
}
.chart-large { grid-column: span 2; }
.chart-grid .chart-box:nth-child(4) { grid-column: span 4; }
.chart-grid .chart-box:nth-child(4) .chart { height: 220px; }
.chart-title { font-size: 14px; color: #b7c0d6; margin-bottom: 8px; padding-left: 4px; }
.chart { height: 320px; }
@media (max-width: 1400px) {
  .chart-grid { grid-template-columns: 1fr 1fr; }
  .chart-large { grid-column: span 2; }
}

.rank-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 14px; }
.rank-box {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px; padding: 12px;
}
.rank-list { display: flex; flex-direction: column; gap: 6px; max-height: 300px; overflow: auto; }
.rank-item {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 8px; border-radius: 6px; background: rgba(255, 255, 255, 0.03);
  font-size: 13px; color: #cfd5e6;
}
.rk { width: 22px; height: 22px; border-radius: 5px; background: rgba(255,255,255,.08);
  display: inline-flex; align-items: center; justify-content: center; font-size: 12px; color: #9aa3bd; flex-shrink: 0; }
.rk.top { background: #2f6bff; color: #fff; }
.rn { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rc { color: #5aa8ff; flex-shrink: 0; }
.rank-empty { color: #9aa3bd; font-size: 13px; padding: 12px 8px; }
@media (max-width: 900px) {
  .rank-row { grid-template-columns: 1fr; }
}
</style>