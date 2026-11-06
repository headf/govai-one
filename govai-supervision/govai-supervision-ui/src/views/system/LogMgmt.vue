<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ total }} 条操作日志</span>
        <div class="filter-inline">
          <el-input v-model="query.keyword" placeholder="操作人/操作/路径" clearable style="width: 220px" @clear="load" @keyup.enter="load" />
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 300px"
            @change="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button v-if="hasPerm('log:clear')" type="danger" plain @click="onClear">清空日志</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="username" label="操作人" width="120" />
      <el-table-column prop="operation" label="操作" min-width="140" show-overflow-tooltip />
      <el-table-column prop="method" label="请求" min-width="200" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column prop="costTime" label="耗时ms" width="90" />
      <el-table-column label="时间" width="160">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.page"
      @current-change="(p) => { query.page = p; load() }"
    />

    <el-dialog v-model="detail.visible" title="日志详情" width="620px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="操作人">{{ detail.row.username }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.row.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求">{{ detail.row.method }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.row.ip }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.row.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.row.createTime }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="params">{{ detail.row.params || '—' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logApi } from '../../api/modules'
import { hasPerm } from '../../utils/perm'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', begin: '', end: '' })
const dateRange = ref(null)
const detail = reactive({ visible: false, row: {} })

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size }
  if (query.keyword) params.keyword = query.keyword
  if (dateRange.value && dateRange.value.length === 2) {
    params.begin = dateRange.value[0]
    params.end = dateRange.value[1]
  }
  try {
    const data = await logApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function openDetail(row) {
  detail.row = row
  detail.visible = true
}

async function onClear() {
  await ElMessageBox.confirm('确认清空全部操作日志？该操作不可恢复。', '清空日志', { type: 'warning' })
    .catch(() => { throw new Error('cancel') })
  await logApi.clear()
  ElMessage.success('日志已清空')
  load()
}

onMounted(load)
</script>

<style scoped>
.section-title {
  font-size: 16px;
  font-weight: 600;
  padding-left: 8px;
  border-left: 4px solid var(--govai-brand, #2f6bff);
  margin-bottom: 0;
}
.filter-inline { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.pager { margin-top: 16px; justify-content: flex-end; }
.params { margin: 0; white-space: pre-wrap; word-break: break-all; font-size: 12px; background: #f5f7fa; padding: 8px; border-radius: 4px; max-height: 240px; overflow: auto; }
</style>
