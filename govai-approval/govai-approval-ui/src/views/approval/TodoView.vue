<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { approvalApi } from '@/api'
import { roleName } from '@/utils/roleMeta'

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
// 各环节待办统计(后端已按当前岗位过滤)
const stats = ref<{ nodeName: string; count: number }[]>([])

const query = reactive({
  status: 'PENDING',
  nodeName: '' as string, // 空=全部
  page: 1,
  size: 20,
})

const statusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '已处理', value: 'DONE' },
  { label: '全部', value: '' },
]

// 环节 tag 配色
const nodeTagType: Record<string, string> = {
  受理: 'primary',
  初审: 'warning',
  决定: 'danger',
  办结: 'success',
}

// 状态标签映射
const statusTagType: Record<string, string> = {
  PENDING: 'warning',
  DONE: 'success',
  CANCEL: 'info',
}

const statusLabel: Record<string, string> = {
  PENDING: '待处理',
  DONE: '已处理',
  CANCEL: '已取消',
}

// 审批结果中英文映射
const approveResultLabel: Record<string, string> = {
  PASS: '通过',
  REJECT: '拒绝',
  RETURN: '退回补正',
}

const totalPending = () => stats.value.reduce((s, x) => s + Number(x.count || 0), 0)

async function loadStats() {
  const res = await approvalApi.myTodoStats().catch(() => null)
  if (res?.data) stats.value = res.data
}

async function loadData() {
  loading.value = true
  try {
    const res = await approvalApi.myTodo({
      status: query.status,
      nodeName: query.nodeName,
      page: query.page,
      size: query.size,
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function refresh() {
  query.page = 1
  loadData()
  if (query.status === 'PENDING') loadStats()
}

// 点统计卡: 切换环节筛选
function filterByNode(name: string) {
  query.nodeName = query.nodeName === name ? '' : name
  refresh()
}

function handleSearch() {
  refresh()
}

function handlePageChange(page: number) {
  query.page = page
  loadData()
}

function goDetail(task: any) {
  router.push(`/approval/detail/${task.instanceId}?taskId=${task.id}`)
}

function fmtTime(t?: string) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

onMounted(() => {
  loadData()
  loadStats()
})
</script>

<template>
  <div class="page-container">
    <!-- 环节统计卡：实时显示当前岗位各环节待办数(受理/初审/决定/办结)，点击切换 -->
    <el-card class="section-card" v-if="stats.length">
      <div class="stats-row">
        <div
          class="stat-item"
          :class="{ active: query.nodeName === '' && query.status === 'PENDING' }"
          @click="query.status = 'PENDING'; filterByNode('')"
        >
          <div class="stat-num">{{ totalPending() }}</div>
          <div class="stat-label">全部待办</div>
        </div>
        <div
          v-for="s in stats"
          :key="s.nodeName"
          class="stat-item"
          :class="{ active: query.nodeName === s.nodeName }"
          @click="filterByNode(s.nodeName)"
        >
          <div class="stat-num">
            {{ s.count }}
            <el-tag size="small" :type="nodeTagType[s.nodeName] || 'info'" style="vertical-align: middle">
              {{ s.nodeName }}
            </el-tag>
          </div>
          <div class="stat-label">待处理</div>
        </div>
      </div>
    </el-card>

    <!-- 待办列表：筛选并入表头，减少卡片堆叠 -->
    <el-card class="section-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
          <span style="font-size: 13px; color: #909399">共 {{ total }} 条{{ query.status === 'PENDING' ? '待办' : '记录' }}</span>
          <div style="display: flex; align-items: center; gap: 10px">
            <span style="font-size: 13px; color: #909399">任务状态：</span>
            <el-select v-model="query.status" placeholder="选择状态" style="width: 150px" clearable @change="handleSearch">
              <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
        <el-table-column label="环节" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.nodeName" size="small" :type="nodeTagType[row.nodeName] || 'info'">
              {{ row.nodeName }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="办理岗位" width="110">
          <template #default="{ row }">
            {{ roleName(row.processRoleCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="taskStatus" label="任务状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.taskStatus] || 'info'" size="small">
              {{ statusLabel[row.taskStatus] || row.taskStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approveResult" label="审批结果" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.approveResult" :type="row.approveResult === 'PASS' ? 'success' : row.approveResult === 'REJECT' ? 'danger' : 'warning'" size="small">
              {{ approveResultLabel[row.approveResult] || row.approveResult }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="approveOpinion" label="审批意见" show-overflow-tooltip />
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.handleTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="goDetail(row)">
              {{ row.taskStatus === 'PENDING' ? '去审批' : '查看' }}
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

<style scoped>
.stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.stat-item {
  flex: 1;
  min-width: 130px;
  text-align: center;
  padding: 16px 8px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background: #fafbfc;
  cursor: pointer;
  transition: all 0.2s;
}
.stat-item:hover {
  border-color: #2f6bff;
  background: #f0f6ff;
}
.stat-item.active {
  border-color: #2f6bff;
  background: #eaf1ff;
  box-shadow: 0 2px 8px rgba(47, 107, 255, 0.15);
}
.stat-num {
  font-size: 24px;
  font-weight: 600;
  color: #1f2d3d;
  line-height: 1.4;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
