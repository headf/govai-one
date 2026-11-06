<template>
  <div class="credit-page">
    <!-- 信用概览 -->
    <el-card class="block">
      <template #header><div class="card-title">信用概览</div></template>
      <div v-if="credit" class="credit-banner">
        <div class="score-block">
          <div class="score-label">信用分</div>
          <div class="score-value">{{ credit.creditScore ?? '—' }}</div>
        </div>
        <div class="level-block">
          <div class="score-label">信用等级</div>
          <el-tag :type="levelTag" size="large" effect="dark">{{ credit.creditLevel || '—' }}</el-tag>
        </div>
        <div class="dim-block" v-if="credit.dimensionScores">
          <div class="score-label">维度得分</div>
          <div class="dims">{{ credit.dimensionScores }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无信用档案,评分后即可查看" :image-size="70" />
    </el-card>

    <!-- 风险预警 -->
    <el-card class="block">
      <template #header>
        <div class="card-title">
          风险预警
          <el-radio-group v-model="alertFilter" size="small" class="filter" @change="loadAlerts">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="PENDING">待处理</el-radio-button>
            <el-radio-button label="HANDLED">已处理</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="alerts" v-loading="loadingAlert" stripe>
        <el-table-column prop="alertLevel" label="级别" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="levelType[row.alertLevel] || 'info'">{{ levelText[row.alertLevel] || row.alertLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertContent" label="预警内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ (row.alertTime || '').replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column prop="handleStatus" label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.handleStatus === 'HANDLED' ? 'info' : 'danger'">{{ row.handleStatus === 'HANDLED' ? '已处理' : '待处理' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleOpinion" label="处置意见" min-width="150" show-overflow-tooltip />
      </el-table>
      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="alertTotal"
        :page-size="alertQuery.size"
        :current-page="alertQuery.page"
        @current-change="onAlertPage"
      />
    </el-card>

    <!-- 信用修复记录 -->
    <el-card class="block">
      <template #header><div class="card-title">信用修复记录</div></template>
      <el-table :data="repairs" v-loading="loadingRepair" stripe>
        <el-table-column prop="repairReason" label="修复原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">{{ (row.applyTime || '').replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column prop="auditStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="repairStatus[row.auditStatus]?.type || 'info'">{{ repairStatus[row.auditStatus]?.label || row.auditStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditOpinion" label="审核意见" min-width="170" show-overflow-tooltip />
      </el-table>
      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="repairTotal"
        :page-size="repairQuery.size"
        :current-page="repairQuery.page"
        @current-change="onRepairPage"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getMyCredit, getMyAlerts, getMyRepairs } from '@/api'
import type { CreditProfile, CreditAlert, CreditRepairRecord } from '@/types'

const auth = useAuthStore()
const enterpriseId = auth.user?.enterpriseId

const credit = ref<CreditProfile | null>(null)
const levelTag = computed(() => (['A', 'B', 'C', 'D'].includes(credit.value?.creditLevel || '') ? credit.value!.creditLevel : 'info'))

const levelType: Record<string, string> = { LOW: 'info', MEDIUM: 'warning', HIGH: 'danger' }
const levelText: Record<string, string> = { LOW: '低', MEDIUM: '中', HIGH: '高' }
const repairStatus: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待审', type: 'warning' },
  PASS: { label: '通过', type: 'success' },
  REJECT: { label: '驳回', type: 'danger' },
}

// 预警
const alertFilter = ref('')
const alerts = ref<CreditAlert[]>([])
const alertTotal = ref(0)
const loadingAlert = ref(false)
const alertQuery = reactive({ page: 1, size: 10 })

// 修复记录
const repairs = ref<CreditRepairRecord[]>([])
const repairTotal = ref(0)
const loadingRepair = ref(false)
const repairQuery = reactive({ page: 1, size: 10 })

async function loadCredit() {
  if (!enterpriseId) return
  credit.value = await getMyCredit(enterpriseId)
}

async function loadAlerts() {
  if (!enterpriseId) return
  loadingAlert.value = true
  try {
    const params: { page: number; size: number; handleStatus?: string } = { page: alertQuery.page, size: alertQuery.size }
    if (alertFilter.value) params.handleStatus = alertFilter.value
    const data = await getMyAlerts(enterpriseId, params)
    alerts.value = data?.list || []
    alertTotal.value = data?.total || 0
  } finally {
    loadingAlert.value = false
  }
}

async function loadRepairs() {
  if (!enterpriseId) return
  loadingRepair.value = true
  try {
    const data = await getMyRepairs(enterpriseId, { page: repairQuery.page, size: repairQuery.size })
    repairs.value = data?.list || []
    repairTotal.value = data?.total || 0
  } finally {
    loadingRepair.value = false
  }
}

function onAlertPage(p: number) {
  alertQuery.page = p
  loadAlerts()
}

function onRepairPage(p: number) {
  repairQuery.page = p
  loadRepairs()
}

onMounted(() => {
  loadCredit()
  loadAlerts()
  loadRepairs()
})
</script>

<style scoped>
.credit-page { display: grid; gap: 16px; padding: 4px 0 24px; }
.block { width: 100%; }
.card-title { display: flex; align-items: center; justify-content: space-between; }
.filter { margin-left: 12px; }
.credit-banner { display: flex; gap: 48px; align-items: center; padding: 10px 6px; }
.score-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.score-value { font-size: 44px; font-weight: 700; color: #1d3557; line-height: 1; }
.level-block .el-tag { font-size: 18px; height: 44px; }
.dims { color: #606266; font-size: 13px; white-space: pre-wrap; max-width: 480px; }
.pager { margin-top: 14px; justify-content: flex-end; }
</style>
