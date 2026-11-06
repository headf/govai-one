<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { processApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const query = reactive({
  matterType: '',
  status: undefined as number | undefined,
  page: 1,
  size: 20,
})

// 事项类型中英文映射
const matterTypeLabel: Record<string, string> = {
  ENTERPRISE_REGISTER: '新办企业登记',
  FOOD_PERMIT: '食品经营许可',
  CONSTRUCTION_PERMIT: '建筑工程施工许可',
  INDIVIDUAL_REGISTER: '个体工商户登记',
  OTHER: '其他',
}

async function loadData() {
  loading.value = true
  try {
    const res = await processApi.page(query)
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

function fmtTime(t?: string) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

function goDesigner(id?: number) {
  router.push(`/process/designer/${id || ''}`)
}

async function switchStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(`确认${newStatus === 1 ? '启用' : '停用'}模板"${row.templateName}"？`, '提示', { type: 'warning' })
    await processApi.switchStatus(row.id, newStatus)
    ElMessage.success('操作成功')
    loadData()
  } catch {
    // 取消
  }
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
          <span style="font-size: 13px; color: #909399">共 {{ total }} 条流程模板</span>
          <div class="filter-bar" style="margin-bottom: 0">
            <span>事项类型：</span>
            <el-input v-model="query.matterType" placeholder="事项类型" style="width: 180px" clearable />
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button type="success" @click="goDesigner()">新增模板</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
        <el-table-column prop="templateCode" label="模板编码" width="180" />
        <el-table-column prop="templateName" label="模板名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="matterType" label="事项类型" width="150">
          <template #default="{ row }">
            {{ matterTypeLabel[row.matterType] || row.matterType }}
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="goDesigner(row.id)">编辑</el-button>
            <el-button :type="row.status === 1 ? 'warning' : 'success'" size="small" link @click="switchStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
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
