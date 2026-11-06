<template>
  <div class="applies">
    <el-card>
      <template #header>
        <div class="list-head">
          <div>
            <div class="list-sub">共 {{ total }} 条 · 状态与进度一目了然</div>
          </div>
          <div class="search-row">
            <el-input
              v-model="keyword"
              placeholder="搜索申报编号"
              clearable
              style="width: 220px"
              @keyup.enter="query.page = 1; load()"
            />
            <el-button type="primary" @click="query.page = 1; load()">查询</el-button>
            <el-button type="primary" plain :icon="Plus" @click="router.push('/matters')">新申报</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" @row-click="goDetail" style="cursor: pointer">
        <el-table-column prop="recordCode" label="申报编号" width="170" />
        <el-table-column prop="matterName" label="办理事项" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ fmt(row.submitTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length && !loading" description="暂无申报记录" />

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { listMyApplies } from '@/api'
import type { ApplyRecordVO } from '@/types'

const router = useRouter()
const list = ref<ApplyRecordVO[]>([])
const total = ref(0)
const keyword = ref('')
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

function statusText(s: string) {
  return { DRAFT: '草稿', PENDING: '待受理', APPROVING: '审批中', APPROVED: '已通过', PASSED: '已通过', REJECTED: '已驳回', REPAIR: '待补正', FINISHED: '已办结' }[s] || s
}
function statusTag(s: string) {
  if (['PENDING', 'APPROVING'].includes(s)) return 'warning'
  if (['APPROVED', 'PASSED', 'FINISHED'].includes(s)) return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}
function fmt(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}
function goDetail(row: ApplyRecordVO) {
  router.push(`/applications/${row.id}`)
}

async function load() {
  loading.value = true
  try {
    const res = await listMyApplies({ ...query, keyword: keyword.value || undefined })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.applies {
  display: grid;
  gap: 16px;
  padding: 4px 0 24px;
}
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  width: 100%;
}
.list-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #9ca3af;
}
.search-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
