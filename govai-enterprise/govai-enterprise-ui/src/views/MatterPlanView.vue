<template>
  <div class="plan-page">
    <el-card class="head-card">
      <template #header>
        <div class="head-bar">
          <p class="head-sub">
            描述您想做的业务,系统自动匹配需要办理的证照/许可事项,按先后顺序推荐办理路径。
          </p>
        </div>
      </template>

      <div class="query-area">
        <el-input
          v-model="description"
          type="textarea"
          :rows="3"
          placeholder="例如:我想开一家食品销售便利店,卖点零食饮料和烟草"
          resize="none"
          maxlength="200"
          show-word-limit
          class="query-input"
        />
        <div class="quick-scenes">
          <span class="quick-label">常见场景:</span>
          <el-tag
            v-for="s in scenes"
            :key="s.text"
            class="scene-tag"
            :effect="description === s.text ? 'dark' : 'plain'"
            :type="description === s.text ? 'primary' : 'info'"
            @click="description = s.text"
          >{{ s.label }}</el-tag>
        </div>
        <div class="query-opts">
          <el-checkbox
            v-model="hasLicense"
            :true-value="true"
            :false-value="false"
            label="已取得营业执照(跳过企业开办)"
          />
          <el-button
            type="primary"
            size="large"
            :icon="Promotion"
            :loading="loading"
            @click="generate"
          >生成办事清单</el-button>
          <el-button size="large" :icon="Clock" plain @click="openHistory">历史规划</el-button>
        </div>
      </div>
    </el-card>

    <!-- 生成中可视化进度条:LLM 耗时较长,用动画+轮播提示缓解等待焦虑 -->
    <div v-if="loading" class="plan-loading">
      <div class="loading-orbit">
        <el-icon :size="52" color="#2f6bff" class="loading-spin"><Compass /></el-icon>
        <span class="orbit-ring"></span>
      </div>
      <div class="loading-title">智能规划中</div>
      <el-progress
        :percentage="100"
        :show-text="false"
        :stroke-width="8"
        status="success"
        :indeterminate="true"
        :duration="3"
        striped
        striped-flow
        class="loading-bar"
      />
      <div class="loading-tip">{{ loadingTip }}</div>
      <div class="loading-sub">LLM 正在检索事项库与政策库,预计 10-30 秒,请勿离开</div>
    </div>

    <template v-if="result">
      <!-- 全部事项完成确认条:所有非跳过事项标记完成后显示 -->
      <el-alert
        v-if="allDone && !result.completed"
        type="success"
        show-icon
        :closable="false"
        class="mb-16"
        title="所有事项已办理完成"
        description="确认完成本规划后,可在历史规划中归档,并前往工作台查看办理成果"
      >
        <template #default>
          <div class="complete-bar">
            <span>所有事项已办理完成,是否确认完成本规划?</span>
            <el-button type="success" size="small" :icon="CircleCheck" :loading="completing" @click="confirmComplete">确认完成</el-button>
          </div>
        </template>
      </el-alert>
      <el-alert
        v-if="result.completed"
        type="info"
        show-icon
        :closable="false"
        class="mb-16"
        title="本规划已完成确认"
        description="如需重新规划,可修改需求后再次生成"
      />

      <el-alert
        v-if="result.summary"
        :title="result.title"
        :description="result.summary"
        type="info"
        :closable="false"
        show-icon
        class="mb-16"
      />

      <el-timeline class="plan-timeline">
        <el-timeline-item
          v-for="it in result.items"
          :key="it.matterId"
          :timestamp="formatOrder(it)"
          :type="timelineType(it)"
          :hollow="it.status === 'SKIPPED'"
          size="large"
        >
          <el-card shadow="hover" class="item-card" :class="{ 'card-optional': it.status === 'OPTIONAL', 'card-skipped': it.status === 'SKIPPED' }">
            <div class="item-head">
              <div class="item-meta">
                <span class="item-name">{{ it.matterName }}</span>
                <el-tag v-if="it.prerequisite" type="danger" size="small">前置必需</el-tag>
                <el-tag v-else-if="it.status === 'OPTIONAL'" type="info" size="small">可选</el-tag>
                <el-tag v-else-if="it.status === 'SKIPPED'" type="success" size="small">可跳过</el-tag>
                <el-tag v-else type="success" size="small">推荐办理</el-tag>
                <el-tag v-if="it.deadlineDays" type="warning" size="small" effect="plain">
                  {{ it.deadlineDays }} 个工作日
                </el-tag>
                <span v-if="it.department" class="item-dept">· {{ it.department }}</span>
              </div>
              <div v-if="it.reason" class="item-reason">
                <el-icon><InfoFilled /></el-icon>
                <span>{{ it.reason }}</span>
              </div>
            </div>

            <el-collapse class="item-collapse">
              <el-collapse-item title="办理详情">
                <div class="detail-grid">
                  <div v-if="it.guide" class="detail-block">
                    <div class="detail-title">办理指南</div>
                    <div class="detail-text">{{ it.guide }}</div>
                  </div>
                  <div v-if="it.materials && it.materials.length" class="detail-block">
                    <div class="detail-title">所需材料 ({{ it.materials.length }})</div>
                    <ul class="material-list">
                      <li v-for="(m, idx) in it.materials" :key="idx">{{ m }}</li>
                    </ul>
                  </div>
                  <div v-if="it.process && it.process.length" class="detail-block">
                    <div class="detail-title">办理流程</div>
                    <div class="process-steps">
                      <span v-for="(p, idx) in it.process" :key="idx" class="step">{{ p }}</span>
                    </div>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>

            <div class="item-actions">
              <el-button type="primary" size="small" :icon="Document" @click="goDetail(it)">查看详情</el-button>
              <el-button
                v-if="it.status !== 'SKIPPED' && !isDone(it.matterId)"
                type="success"
                size="small"
                :icon="Promotion"
                @click="goApply(it)"
              >立即办理 →</el-button>
              <el-button
                v-if="it.status !== 'SKIPPED'"
                size="small"
                :type="isDone(it.matterId) ? 'success' : 'info'"
                :icon="CircleCheck"
                plain
                @click="toggleDone(it.matterId)"
              >{{ isDone(it.matterId) ? '已完成' : '标记完成' }}</el-button>
              <el-button size="small" :icon="ChatDotRound" plain @click="askAbout(it)">咨询</el-button>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <el-card v-if="result.tips && result.tips.length" class="tips-card">
        <template #header>
          <span class="tips-title">💡 温馨提示</span>
        </template>
        <ul class="tips-list">
          <li v-for="(t, idx) in result.tips" :key="idx">{{ t }}</li>
        </ul>
      </el-card>
    </template>

    <el-empty v-else description="输入您的业务需求,点击「生成办事清单」开始规划" />

    <!-- 历史规划抽屉:记忆功能,可恢复历史规划 -->
    <el-drawer v-model="historyVisible" title="历史规划" size="420px" direction="rtl">
      <div v-if="historyLoading" class="hist-loading">加载中...</div>
      <el-empty v-else-if="!historyList.length" description="暂无历史规划" />
      <div v-else class="hist-list">
        <div
          v-for="h in historyList"
          :key="h.id"
          class="hist-item"
          :class="{ active: h.id === result?.historyId }"
          @click="restoreHistory(h.id)"
        >
          <div class="hist-title">{{ h.title }}</div>
          <div class="text-muted hist-desc">{{ h.description }}</div>
          <div class="text-muted hist-time">{{ formatHistoryTime(h.createTime) }}</div>
          <el-button class="hist-del" text type="danger" size="small" :icon="Delete" @click.stop="removeHistory(h.id)" />
        </div>
      </div>
      <div class="hist-foot">
        <el-button text type="info" @click="historyVisible = false">关闭</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Compass, Promotion, InfoFilled, Document, ChatDotRound, CircleCheck, Clock, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { planMatters, listPlanHistory, getPlanHistory, completePlan, deletePlan } from '@/api'
import { rememberPlan, readLastPlan, markPlanLeft, consumePlanLeft } from '@/utils/planReturn'
import type { MatterPlanResp, MatterPlanItem, PlanHistory } from '@/types'

const route = useRoute()
const router = useRouter()

const scenes = [
  { label: '食品销售/便利店', text: '我想开一家食品销售便利店,卖点零食饮料和烟草' },
  { label: '餐饮/餐厅', text: '我想开一家餐厅,做热食炒菜,可能卖点烟和饮料' },
  { label: '咖啡馆/轻食', text: '我想开一家咖啡馆兼卖轻食甜品' },
  { label: '酒吧/娱乐场所', text: '我想开一家酒吧,卖烟酒和简单餐食' },
  { label: '科技公司', text: '我想注册一家软件科技公司,申请高新技术企业认定' }
]

const description = ref(scenes[0].text)
const hasLicense = ref(false)
const loading = ref(false)
const result = ref<MatterPlanResp | null>(null)

// 生成中轮播提示:缓解 LLM 等待焦虑
const loadingTips = [
  '正在解析您的业务需求描述...',
  '匹配事项库中符合条件的办理事项...',
  '关联政策库检索适用补贴与优惠政策...',
  '推算各事项的前后置依赖关系...',
  '生成最优办理路径与温馨提示...'
]
const tipIndex = ref(0)
const loadingTip = computed(() => loadingTips[tipIndex.value])
let tipTimer: number | null = null

// 事项完成状态(记忆:本地记录已标记完成的 matterId)
const doneSet = ref<Set<number>>(new Set())
const completing = ref(false)

// 历史规划
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyList = ref<PlanHistory[]>([])

// 是否所有非跳过事项都已标记完成
const allDone = computed(() => {
  if (!result.value?.items?.length) return false
  const need = result.value.items.filter((it) => it.status !== 'SKIPPED')
  if (!need.length) return false
  return need.every((it) => doneSet.value.has(it.matterId))
})

function isDone(matterId: number) {
  return doneSet.value.has(matterId)
}
function toggleDone(matterId: number) {
  if (isDone(matterId)) {
    doneSet.value.delete(matterId)
  } else {
    doneSet.value.add(matterId)
  }
  doneSet.value = new Set(doneSet.value)
}

async function generate() {
  if (!description.value.trim()) {
    ElMessage.warning('请先描述一下您的业务需求')
    return
  }
  loading.value = true
  doneSet.value = new Set()
  tipIndex.value = 0
  // 启动轮播提示,每 3 秒切换一条
  tipTimer = window.setInterval(() => {
    tipIndex.value = (tipIndex.value + 1) % loadingTips.length
  }, 3000)
  try {
    result.value = await planMatters({
      description: description.value.trim(),
      hasLicense: hasLicense.value
    })
    // 记录本次规划:供跳去办理后再返回时自动恢复同一份清单,无需重新生成
    if (result.value?.historyId) {
      rememberPlan({ historyId: result.value.historyId, title: result.value.title })
    }
    if (!result.value?.items?.length) {
      ElMessage.warning('暂未匹配到事项,试试换一种说法')
    } else {
      ElMessage.success(`已为您规划 ${result.value.items.length} 项事务`)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '规划失败,请稍后重试')
  } finally {
    loading.value = false
    if (tipTimer) {
      clearInterval(tipTimer)
      tipTimer = null
    }
  }
}

// 确认完成本规划:所有事项办结后调用
async function confirmComplete() {
  if (!result.value?.historyId) {
    ElMessage.warning('规划历史不存在,无法确认')
    return
  }
  await ElMessageBox.confirm('确认本规划所有事项已办理完成?完成后将在历史规划中归档。', '确认完成', { type: 'success' })
  completing.value = true
  try {
    await completePlan(result.value.historyId)
    result.value.completed = true
    ElMessage.success('已确认完成本规划')
    // 完成后跳转工作台查看办理成果
    setTimeout(() => router.push('/dashboard'), 1200)
  } catch (e: any) {
    ElMessage.error(e?.message || '确认失败')
  } finally {
    completing.value = false
  }
}

// 打开历史规划抽屉
async function openHistory() {
  historyVisible.value = true
  historyLoading.value = true
  try {
    historyList.value = await listPlanHistory()
  } catch (e: any) {
    ElMessage.error(e?.message || '加载历史失败')
  } finally {
    historyLoading.value = false
  }
}

// 恢复历史规划:点击某条历史 / 从办理页带 planId 返回,加载完整内容
async function restoreHistory(id: number, silent = false) {
  try {
    const resp = await getPlanHistory(id)
    result.value = resp
    doneSet.value = new Set()
    // 若历史列表未加载(如从办理页直接返回),先拉取以便还原输入框上下文
    if (!historyList.value.length) {
      try {
        historyList.value = await listPlanHistory()
      } catch {
        /* 列表加载失败不阻断规划恢复 */
      }
    }
    const h = historyList.value.find((x) => x.id === id)
    if (h) {
      description.value = h.description
      hasLicense.value = !!h.hasLicense
    }
    // 记录为"最近一次规划",保证再次跳转返回时也能恢复
    if (resp.historyId) {
      rememberPlan({ historyId: resp.historyId, title: resp.title })
    }
    if (!silent) {
      historyVisible.value = false
      ElMessage.success('已恢复历史规划')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '恢复失败')
  }
}

// 删除历史规划
async function removeHistory(id: number) {
  try {
    await ElMessageBox.confirm('确定删除这条历史规划?', '删除', { type: 'warning' })
    await deletePlan(id)
    historyList.value = historyList.value.filter((h) => h.id !== id)
    if (result.value?.historyId === id) {
      result.value = null
    }
    ElMessage.success('已删除')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

function formatHistoryTime(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}

function formatOrder(it: MatterPlanItem) {
  return `第 ${it.order ?? 1} 步`
}
function timelineType(it: MatterPlanItem) {
  if (it.prerequisite) return 'primary'
  if (it.status === 'OPTIONAL') return 'info'
  if (it.status === 'SKIPPED') return 'success'
  return 'success'
}

function goDetail(it: MatterPlanItem) {
  markPlanLeft()
  const q = planIdQuery()
  router.push(q ? { path: `/matters/${it.matterId}`, query: q } : `/matters/${it.matterId}`)
}
function goApply(it: MatterPlanItem) {
  markPlanLeft()
  const q = planIdQuery()
  router.push(q ? { path: `/matters/${it.matterId}/apply`, query: q } : `/matters/${it.matterId}/apply`)
}
// 当前规划的追溯 query(来自规划入口时),供办理页显示"返回规划"并可恢复同一份清单
function planIdQuery(): Record<string, string> | null {
  const id = result.value?.historyId ?? route.query.planId
  return id ? { from: 'plan', planId: String(id) } : null
}
function askAbout(it: MatterPlanItem) {
  router.push({ path: '/qa', query: { q: it.matterName } })
}

// 组件卸载时清理轮播定时器,避免内存泄漏
onUnmounted(() => {
  if (tipTimer) {
    clearInterval(tipTimer)
    tipTimer = null
  }
})

// 挂载时自动恢复规划上下文:
// 1) 从办理页点"返回规划"带 planId → 精确恢复同一份清单(核心返回链路);
// 2) 从办理页浏览器后退回来(无 planId)→ 因 markPlanLeft 留有"去过办理"标记,静默续上上次清单,避免重新生成。
onMounted(async () => {
  if (result.value) return
  const planId = route.query.planId ? Number(route.query.planId) : undefined
  if (planId) {
    await restoreHistory(planId, true)
    return
  }
  if (consumePlanLeft()) {
    const last = readLastPlan()
    if (last) {
      await restoreHistory(last.historyId, true)
    }
  }
})
</script>

<style scoped>
.plan-page { min-height: 100%; padding: 4px 0 24px; }
.mb-16 { margin-bottom: 16px; }

/* 生成中可视化进度条 */
.plan-loading {
  display: flex; flex-direction: column; align-items: center; gap: 14px;
  padding: 48px 24px; margin-bottom: 16px;
  background: linear-gradient(135deg, #f6f9ff 0%, #eef4ff 100%);
  border: 1px solid #d6e4ff; border-radius: 12px;
}
.loading-orbit {
  position: relative; width: 96px; height: 96px;
  display: flex; align-items: center; justify-content: center;
}
.loading-spin { animation: orbit-spin 2s linear infinite; }
.orbit-ring {
  position: absolute; inset: 0; border-radius: 50%;
  border: 3px solid transparent; border-top-color: #2f6bff; border-right-color: #79bbff;
  animation: orbit-ring 1.5s linear infinite;
}
@keyframes orbit-spin {
  to { transform: rotate(360deg); }
}
@keyframes orbit-ring {
  to { transform: rotate(-360deg); }
}
.loading-title {
  font-size: 18px; font-weight: 700; color: #1d2129; letter-spacing: 1px;
}
.loading-bar { width: 60%; max-width: 360px; }
.loading-tip {
  font-size: 14px; color: #2f6bff; font-weight: 500;
  min-height: 20px; transition: opacity 0.3s;
}
.loading-sub {
  font-size: 12px; color: #86909c;
}
.head-card { margin-bottom: 16px; }
.head-bar { display: flex; flex-direction: column; gap: 6px; }
.head-sub { margin: 0; color: #86909c; font-size: 13px; }

.query-area { display: flex; flex-direction: column; gap: 12px; }
.query-input :deep(.el-textarea__inner) { font-size: 14px; }
.quick-scenes { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.quick-label { color: #86909c; font-size: 13px; margin-right: 4px; }
.scene-tag { cursor: pointer; user-select: none; }
.query-opts {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 8px; border-top: 1px dashed #e5e6eb;
}

.plan-timeline { padding: 0 8px; }
.item-card { margin-bottom: 20px; border-radius: 8px; }
.item-card.card-optional { opacity: 0.85; border-style: dashed; }
.item-card.card-skipped { opacity: 0.6; background: #f7f8fa; }

.item-head { margin-bottom: 10px; }
.item-meta {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  margin-bottom: 6px;
}
.item-name { font-size: 16px; font-weight: 600; color: #1d2129; }
.item-dept { color: #86909c; font-size: 13px; }
.item-reason {
  display: flex; align-items: center; gap: 6px;
  color: #2f6bff; font-size: 13px;
  background: #eaf2ff; padding: 6px 10px; border-radius: 4px;
}
.item-collapse { margin-top: 8px; }
.item-collapse :deep(.el-collapse-item__header) {
  font-size: 13px; color: #4e5969; height: 36px; line-height: 36px;
}
.detail-grid { display: flex; flex-direction: column; gap: 14px; }
.detail-block { }
.detail-title {
  font-size: 13px; font-weight: 600; color: #4e5969;
  margin-bottom: 6px;
}
.detail-text { font-size: 13px; color: #86909c; line-height: 1.7; }
.material-list { margin: 0; padding-left: 18px; font-size: 13px; color: #1d2129; line-height: 1.9; }
.process-steps { display: flex; flex-wrap: wrap; gap: 6px 10px; }
.step {
  font-size: 12px; color: #4e5969;
  background: #f2f3f5; padding: 3px 10px; border-radius: 10px;
}
.item-actions {
  display: flex; gap: 8px; flex-wrap: wrap;
  margin-top: 12px; padding-top: 10px; border-top: 1px dashed #e5e6eb;
}

.tips-card { margin-top: 8px; }
.tips-title { font-weight: 600; }
.tips-list { margin: 0; padding-left: 20px; color: #4e5969; line-height: 1.9; font-size: 13px; }

.complete-bar { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.hist-loading { text-align: center; color: #86909c; padding: 40px 0; }
.hist-list { display: flex; flex-direction: column; gap: 10px; padding: 0 4px; }
.hist-item {
  position: relative; padding: 12px 14px; border: 1px solid #e5e6eb; border-radius: 8px;
  cursor: pointer; transition: all 0.2s;
}
.hist-item:hover { border-color: #2f6bff; box-shadow: 0 2px 8px rgba(47,107,255,0.1); }
.hist-item.active { border-color: #2f6bff; background: #eaf2ff; }
.hist-title { font-weight: 600; color: #1d2129; margin-bottom: 4px; padding-right: 30px; }
.hist-desc { font-size: 13px; margin-bottom: 4px; }
.hist-time { font-size: 12px; }
.hist-del { position: absolute; top: 8px; right: 8px; }
.hist-foot { padding-top: 12px; border-top: 1px solid #e5e6eb; text-align: right; }
</style>
