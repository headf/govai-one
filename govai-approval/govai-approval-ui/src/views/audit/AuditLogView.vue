<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { auditApi } from '@/api'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const query = reactive({
  instanceId: undefined as number | undefined,
  operatorId: undefined as number | undefined,
  operationType: '',
  from: '',
  to: '',
  page: 1,
  size: 20,
})

// 日期范围
const dateRange = ref<[string, string] | null>(null)

const operationTypeOptions = [
  { label: '全部', value: '' },
  { label: '提交', value: 'SUBMIT' },
  { label: '受理', value: 'RECEIVE' },
  { label: '审批', value: 'APPROVE' },
  { label: '退回', value: 'RETURN' },
  { label: '办结', value: 'FINISH' },
  { label: 'AI建议', value: 'AI_ADVICE' },
]

const operationLabel: Record<string, string> = {
  SUBMIT: '提交',
  RECEIVE: '受理',
  APPROVE: '审批',
  RETURN: '退回',
  FINISH: '办结',
  AI_ADVICE: 'AI建议',
}

const operationTagType: Record<string, string> = {
  SUBMIT: 'info',
  RECEIVE: 'primary',
  APPROVE: 'success',
  RETURN: 'warning',
  FINISH: 'success',
  AI_ADVICE: 'danger',
}

// 实例状态中英文映射
const instanceStatusLabel: Record<string, string> = {
  PENDING: '待受理',
  APPROVING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  RETURNED: '已退回',
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (dateRange.value && dateRange.value.length === 2) {
      params.from = dateRange.value[0]
      params.to = dateRange.value[1]
    } else {
      params.from = ''
      params.to = ''
    }
    const res = await auditApi.page(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handlePageChange(page: number) {
  query.page = page
  loadData()
}

function fmtTime(t?: string | number) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page-container">
    <el-card class="section-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px">
          <span style="font-size: 13px; color: #909399">共 {{ total }} 条日志</span>
          <div class="filter-bar" style="margin-bottom: 0">
            <span>实例ID：</span>
            <el-input-number v-model="query.instanceId" :min="1" style="width: 120px" controls-position="right" />
            <span>操作人ID：</span>
            <el-input-number v-model="query.operatorId" :min="1" style="width: 120px" controls-position="right" />
            <span>操作类型：</span>
            <el-select v-model="query.operationType" placeholder="全部" style="width: 120px" clearable>
              <el-option v-for="opt in operationTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <span>时间范围：</span>
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 380px"
            />
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="operationType" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="operationTagType[row.operationType] || 'info'" size="small">
              {{ operationLabel[row.operationType] || row.operationType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作意见" min-width="220">
          <template #default="{ row }">
            <el-tooltip
              :disabled="!row.operationOpinion || row.operationOpinion.length <= 60"
              placement="top"
              effect="light"
              :show-after="300"
            >
              <template #content>
                <div class="opinion-preview">{{ row.operationOpinion }}</div>
              </template>
              <span class="opinion-cell">{{ row.operationOpinion && row.operationOpinion.length > 60 ? row.operationOpinion.slice(0, 60) + '…' : row.operationOpinion }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="beforeStatus" label="操作前状态" width="100">
          <template #default="{ row }">
            {{ instanceStatusLabel[row.beforeStatus] || row.beforeStatus || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="afterStatus" label="操作后状态" width="100">
          <template #default="{ row }">
            {{ instanceStatusLabel[row.afterStatus] || row.afterStatus || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.operationTime) }}</template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<style>
.opinion-preview {
  max-width: 420px;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.8;
  color: #303133;
  background: #fff;
  padding: 8px 10px;
  border-radius: 6px;
}
.opinion-cell {
  color: #606266;
}
</style>
