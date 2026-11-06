<template>
  <div class="g-page">
    <!-- ① 欢迎带 + 主 CTA -->
    <section
      class="g-card p-6 md:p-7 flex items-center justify-between flex-wrap gap-4"
      style="background: radial-gradient(560px 220px at 100% -20%, rgba(47, 107, 255, 0.08), transparent 60%), #fff"
    >
      <div class="min-w-0">
        <div class="text-caption text-ink-3 tracking-wide mb-1.5">GovAI One · 企业服务门户</div>
        <div class="text-[24px] md:text-display font-bold text-ink truncate">您好，{{ enterpriseName }}</div>
        <div class="mt-1.5 text-body text-ink-2">一站式法人办事 · 材料一次通过 · 进度全程可查</div>
      </div>
      <div class="flex gap-2.5 flex-1 sm:flex-none">
        <el-button type="primary" size="large" class="!flex-1 sm:!flex-none" @click="router.push('/matters')">
          <el-icon class="mr-1.5"><Plus /></el-icon>我要办事
        </el-button>
        <el-button size="large" @click="router.push('/applications')">我的申报</el-button>
      </div>
    </section>

    <!-- ② 快捷办事入口 -->
    <section class="g-card p-5">
      <div class="flex items-center justify-between mb-3.5">
        <h3 class="text-subtitle text-ink m-0">快捷办事</h3>
        <el-button link type="primary" @click="router.push('/matters')">查看全部事项</el-button>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
        <button
          v-for="q in quickItems"
          :key="q.key"
          class="group flex items-center gap-3 p-4 bg-surface-muted border border-line-soft rounded-md text-left cursor-pointer transition duration-200 hover:border-line-brand hover:shadow-card-hover hover:-translate-y-px"
          @click="router.push('/matters')"
        >
          <span class="g-icon-tile w-[42px] h-[42px]" :style="{ background: q.bg, color: q.color }">
            <el-icon><component :is="q.icon" /></el-icon>
          </span>
          <span class="flex flex-col gap-0.5 min-w-0">
            <span class="text-[15px] font-semibold text-ink">{{ q.name }}</span>
            <span class="text-caption text-ink-3">{{ q.desc }}</span>
          </span>
        </button>
      </div>
    </section>

    <!-- ③ 唯一业务提醒带 -->
    <section v-if="todos.length" class="g-card p-5 bg-[#FFFDF6] border-[#FDE8CD]">
      <div class="flex items-center justify-between mb-1.5">
        <div class="flex items-center gap-2 text-[15px] font-semibold text-[#92400E]">
          <el-icon :size="18" color="#D97706"><Bell /></el-icon>
          有 {{ todos.length }} 件事待您处理
        </div>
        <el-button v-if="todos[0]" link type="primary" @click="router.push(todos[0].to)">去处理</el-button>
      </div>
      <div class="grid gap-0.5">
        <button
          v-for="t in todos"
          :key="t.key"
          class="group flex items-center gap-3 px-2 py-2.5 rounded-sm text-left cursor-pointer transition hover:bg-[#FFF7E6]"
          @click="router.push(t.to)"
        >
          <el-icon :size="20" :color="t.color"><component :is="t.icon" /></el-icon>
          <span class="flex-1 min-w-0 flex flex-col gap-px">
            <span class="text-body font-medium text-ink">{{ t.title }}</span>
            <span class="text-caption text-ink-3">{{ t.desc }}</span>
          </span>
          <el-icon class="text-[#D1D5DB] group-hover:text-brand transition"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>

    <!-- ④ 状态速览（点击直达清单） -->
    <section class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
      <button
        v-for="c in statCards"
        :key="c.key"
        class="g-card g-card-hover group relative flex items-center gap-3.5 p-5 text-left cursor-pointer"
        @click="router.push(c.to)"
      >
        <span class="g-icon-tile w-[46px] h-[46px]" :style="{ background: c.bg, color: c.color }">
          <el-icon :size="22"><component :is="c.icon" /></el-icon>
        </span>
        <span class="min-w-0 flex flex-col gap-px">
          <span class="text-[26px] leading-tight font-bold text-ink tabular-nums">{{ c.value }}</span>
          <span class="text-body text-ink-2">{{ c.label }}</span>
          <span class="text-caption text-ink-3">{{ c.sub }}</span>
        </span>
        <el-icon class="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#C7CBD1] opacity-0 group-hover:opacity-100 transition">
          <ArrowRight />
        </el-icon>
      </button>
    </section>

    <!-- ⑤ 最近申报 + 政策推荐 -->
    <section class="grid grid-cols-1 xl:grid-cols-[1.3fr_1fr] gap-4 items-start">
      <div class="g-card p-5">
        <div class="flex items-center justify-between mb-3.5">
          <h3 class="text-subtitle text-ink m-0">最近申报</h3>
          <el-button link type="primary" @click="router.push('/applications')">查看全部</el-button>
        </div>
        <div v-if="recentApplies.length" class="grid gap-0.5">
          <button
            v-for="r in recentApplies"
            :key="r.id"
            class="flex items-center gap-3 px-2 py-2.5 rounded-sm text-left cursor-pointer transition hover:bg-[#F6F8FB]"
            @click="router.push(`/applications/${r.id}`)"
          >
            <span class="flex-1 min-w-0 flex flex-col gap-0.5">
              <span class="text-body font-medium text-ink truncate">{{ r.matterName }}</span>
              <span class="text-caption text-ink-3 tabular-nums">{{ r.recordCode }}</span>
            </span>
            <el-tag size="small" :type="statusTag(r.status)">{{ statusText(r.status) }}</el-tag>
            <span class="flex-none text-caption text-ink-3">{{ shortTime(r.createTime) }}</span>
          </button>
        </div>
        <div v-else class="py-7 text-center text-ink-3">
          <p class="text-body m-0 mb-3">暂无申报记录</p>
          <el-button type="primary" plain @click="router.push('/matters')">去办理第一件事</el-button>
        </div>
      </div>

      <div class="g-card p-5">
        <div class="flex items-center justify-between mb-3.5">
          <h3 class="text-subtitle text-ink m-0">政策推荐</h3>
          <el-button link type="primary" @click="router.push('/policies')">更多政策</el-button>
        </div>
        <div v-if="pendingPolicies.length" class="grid gap-2.5">
          <div v-for="p in pendingPolicies" :key="p.id" class="border border-line-soft rounded-[10px] p-3.5 bg-surface-muted">
            <div class="flex items-center justify-between gap-2 mb-1">
              <span class="text-body font-semibold text-ink truncate" :title="p.policyTitle">{{ p.policyTitle }}</span>
              <el-tag size="small" type="warning">匹配 {{ p.matchScore }}%</el-tag>
            </div>
            <div class="text-[13px] leading-relaxed text-ink-2 line-clamp-2">{{ p.policyContent }}</div>
            <div class="flex items-center justify-between mt-2">
              <span class="text-caption text-ink-3">免申即享 · 待确认</span>
              <el-button size="small" type="primary" @click="confirmPolicy(p.id)">确认享受</el-button>
            </div>
          </div>
        </div>
        <div v-else class="py-7 text-center text-ink-3">
          <p class="text-body m-0 mb-3">暂无待确认政策</p>
          <el-button plain @click="router.push('/policies')">去看看政策</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { listMyApplies, listPolicies, confirmPolicy as apiConfirm, listLicenses, listExpiringLicenses } from '@/api'
import type { ApplyRecordVO, PolicyMatch, License } from '@/types'
import { Bell, Document, Edit, Warning, Stamp, Promotion, RefreshRight, Switch, Plus, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()
const applies = ref<ApplyRecordVO[]>([])
const policies = ref<PolicyMatch[]>([])
const licenses = ref<License[]>([])
const expiringLicenses = ref<License[]>([])

const enterpriseName = computed(() => auth.user?.enterpriseName || '贵企业')

/* ---- 速览口径（首页面向普通用户，草稿单独归提醒，不并入"在办"） ---- */
const inProgress = computed(() => applies.value.filter((a) => ['PENDING', 'APPROVING'].includes(a.status)))
const drafts = computed(() => applies.value.filter((a) => a.status === 'DRAFT'))
const repairs = computed(() => applies.value.filter((a) => a.status === 'REPAIR'))
const validLicenses = computed(() =>
  licenses.value.filter((l) => l.status === 1 && (!l.expiryDate || new Date(l.expiryDate).getTime() >= Date.now()))
)
const pendingPolicies = computed(() => policies.value.filter((p) => p.isApplied === 0))
const recentApplies = computed(() => applies.value.slice(0, 5))

/* 快捷办事（样卡均导向办事大厅；后续可按分类深链） */
const quickItems = [
  { key: 'new', name: '新办业务', desc: '许可/开办/认定', icon: Document, bg: '#EAF0FF', color: '#2F6BFF' },
  { key: 'renew', name: '到期延续', desc: '续期换证', icon: RefreshRight, bg: '#EFF8FF', color: '#0284C7' },
  { key: 'change', name: '变更登记', desc: '变更/延期', icon: Switch, bg: '#FFF4E5', color: '#D97706' },
  { key: 'subsidy', name: '补贴政策', desc: '免申即享', icon: Promotion, bg: '#ECFDF3', color: '#16A34A' },
]

/* 状态速览（数字可直接 tabular 对齐，点击直达清单） */
const statCards = computed(() => [
  { key: 'apply', label: '在办申报', value: inProgress.value.length, sub: `${drafts.value.length} 草稿 · ${repairs.value.length} 待补正`, to: '/applications', icon: Document, bg: '#EAF0FF', color: '#2F6BFF' },
  { key: 'license', label: '有效证照', value: validLicenses.value.length, sub: '电子证照即时可用', to: '/licenses', icon: Stamp, bg: '#ECFDF3', color: '#16A34A' },
  { key: 'repair', label: '待补正', value: repairs.value.length, sub: '按退回意见补交', to: '/applications', icon: Warning, bg: '#FFF4E5', color: '#D97706' },
  { key: 'policy', label: '可享政策', value: pendingPolicies.value.length, sub: '免申即享待确认', to: '/policies', icon: Promotion, bg: '#FEF2F2', color: '#DC2626' },
])

/* 提醒带：草稿 / 补正 / 证照到期 / 政策待确认 聚合为唯一待办 */
const todos = computed(() => {
  const list: { key: string; title: string; desc: string; to: string; icon: any; color: string }[] = []
  if (drafts.value.length) {
    list.push({ key: 'draft', title: `${drafts.value.length} 个申报草稿未提交`, desc: '继续填写并提交即可完成办事', to: '/applications', icon: Edit, color: '#6B7280' })
  }
  if (repairs.value.length) {
    const r = repairs.value[0]
    list.push({ key: 'repair', title: `${repairs.value.length} 个申报需补正材料`, desc: '按补正要求补齐后重新提交', to: r ? `/applications/${r.id}` : '/applications', icon: Warning, color: '#DC2626' })
  }
  if (expiringLicenses.value.length) {
    list.push({ key: 'license', title: `${expiringLicenses.value.length} 个证照即将到期`, desc: '到期前办理延续，避免影响经营', to: '/licenses', icon: Stamp, color: '#D97706' })
  }
  if (pendingPolicies.value.length) {
    list.push({ key: 'policy', title: `${pendingPolicies.value.length} 项政策可享`, desc: '系统已匹配，确认后权益生效', to: '/policies', icon: Promotion, color: '#2F6BFF' })
  }
  return list
})

function statusText(s: string) {
  return { DRAFT: '草稿', PENDING: '待受理', APPROVING: '审批中', APPROVED: '已通过', PASSED: '已通过', REJECTED: '已驳回', REPAIR: '待补正', FINISHED: '已办结' }[s] || s
}
function statusTag(s: string) {
  if (s === 'DRAFT') return 'info'
  if (['PENDING', 'APPROVING'].includes(s)) return 'warning'
  if (['APPROVED', 'PASSED', 'FINISHED'].includes(s)) return 'success'
  return 'danger'
}
function shortTime(t?: string) {
  if (!t) return '-'
  const d = new Date(t)
  if (isNaN(d.getTime())) return t.replace('T', ' ').slice(5, 16)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
async function confirmPolicy(id: number) {
  try {
    await ElMessageBox.confirm('确认后即视为贵企业已享受该政策权益，确认享受吗?', '确认享受政策', {
      confirmButtonText: '确认享受',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch {
    return
  }
  await apiConfirm(id)
  ElMessage.success('已确认，政策权益将生效')
  policies.value = await listPolicies()
}

onMounted(async () => {
  const [app, pol, lic, exp] = await Promise.all([
    listMyApplies({ page: 1, size: 20 }),
    listPolicies(),
    listLicenses(),
    listExpiringLicenses(30)
  ])
  applies.value = app.list
  policies.value = pol
  licenses.value = lic
  expiringLicenses.value = exp
})
</script>
