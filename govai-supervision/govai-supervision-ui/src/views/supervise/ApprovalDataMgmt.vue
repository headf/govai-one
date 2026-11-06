<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ total }} 条办件</span>
        <div class="filter-inline">
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
            <el-option label="待受理" value="PENDING" />
            <el-option label="审批中" value="APPROVING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已退回" value="RETURNED" />
          </el-select>
          <el-button type="primary" @click="load">刷新列表</el-button>
        </div>
      </div>
    </template>

    <div class="intro">
      以下办件由<b>审批端/企业端真实流程</b>自动推送（企业申报 → 审批办理），监管端实时接收展示，仅可查看。
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="instanceId" label="实例号" width="110" />
      <el-table-column prop="matterName" label="事项" min-width="150" show-overflow-tooltip />
      <el-table-column prop="applicantName" label="申请人" width="120" show-overflow-tooltip />
      <el-table-column prop="applicantType" label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.applicantType === 'ENTERPRISE' ? 'primary' : 'success'">{{ typeText[row.applicantType] || row.applicantType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="approvalStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType[row.approvalStatus] || 'info'">{{ statusText[row.approvalStatus] || row.approvalStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">{{ (row.submitTime || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column prop="processDurationHours" label="时长(h)" width="80" />
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.page"
      @current-change="(p) => { query.page = p; load() }"
    />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { approvalCacheApi } from '../../api/modules'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, status: '' })
const statusType = {
  PENDING: 'warning', APPROVING: 'warning', APPROVED: 'success',
  PASSED: 'success', REJECTED: 'danger', RETURNED: 'info', REPAIR: 'danger',
}
const statusText = {
  PENDING: '待受理', APPROVING: '审批中', APPROVED: '已通过',
  PASSED: '已办结', REJECTED: '已驳回', RETURNED: '已退回', REPAIR: '待补正',
}
const typeText = { ENTERPRISE: '企业', PERSONAL: '个人' }

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size }
  if (query.status) params.status = query.status
  try {
    const data = await approvalCacheApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.intro { margin-bottom: 14px; color: #606266; font-size: 13px; background: #f0f6ff; padding: 10px 14px; border-radius: 6px; }
.section-title {
  font-size: 16px;
  font-weight: 600;
  padding-left: 8px;
  border-left: 4px solid var(--govai-brand, #2f6bff);
  margin-bottom: 0;
}
.filter-inline { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
