<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { approvalApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const query = reactive({
  status: '',
  applicantName: '',
  page: 1,
  size: 20,
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待受理', value: 'PENDING' },
  { label: '审批中', value: 'APPROVING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已退回', value: 'RETURNED' },
]

const statusTagType: Record<string, string> = {
  PENDING: 'warning',
  APPROVING: 'primary',
  APPROVED: 'success',
  REJECTED: 'danger',
  RETURNED: 'info',
}

const statusLabel: Record<string, string> = {
  PENDING: '待受理',
  APPROVING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  RETURNED: '已退回',
}

async function loadData() {
  loading.value = true
  try {
    const res = await approvalApi.listInstances(query)
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

function goDetail(row: any) {
  router.push(`/approval/detail/${row.id}`)
}

function fmtTime(t?: string) {
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
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
          <span style="font-size: 13px; color: #909399">共 {{ total }} 条审批记录</span>
          <div class="filter-inline">
            <el-select v-model="query.status" placeholder="全部状态" style="width: 140px" clearable>
              <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-input v-model="query.applicantName" placeholder="申请人姓名" style="width: 170px" clearable />
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
        <el-table-column prop="matterName" label="事项名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申请人" min-width="120" />
        <el-table-column prop="applicantType" label="类型" width="80">
          <template #default="{ row }">
            {{ row.applicantType === 'ENTERPRISE' ? '企业' : '个人' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small">
              {{ statusLabel[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="办结时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.finishTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="goDetail(row)">查看</el-button>
          </template>
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
