<template>
  <div class="drafts">
    <el-card v-loading="loading">
      <template #header>
        <div class="list-head">
          <div>
            <div class="list-sub">共 {{ total }} 条 · 保存后未提交的申报草稿</div>
          </div>
          <div class="search-group">
            <el-input
              v-model="keyword"
              placeholder="搜索草稿编号"
              clearable
              style="width: 220px"
              @keyup.enter="query.page = 1; load()"
            />
            <el-button type="primary" @click="query.page = 1; load()">查询</el-button>
            <el-button type="primary" :icon="Plus" @click="router.push('/matters')">新申报</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list">
        <el-table-column prop="recordCode" label="草稿编号" width="170" />
        <el-table-column prop="matterName" label="办理事项" />
        <el-table-column label="状态" width="110">
          <template #default>
            <el-tag type="info" size="small">草稿</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="保存时间" width="180">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="continueEdit(row)">继续编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length" description="暂无草稿" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listMyDrafts, deleteDraft } from '@/api'
import type { ApplyRecordVO } from '@/types'

const router = useRouter()
const list = ref<ApplyRecordVO[]>([])
const total = ref(0)
const keyword = ref('')
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

function fmt(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}
function continueEdit(row: ApplyRecordVO) {
  router.push(`/matters/${row.matterId}/apply?draft=${row.id}`)
}
async function remove(row: ApplyRecordVO) {
  await ElMessageBox.confirm(`确定删除草稿「${row.recordCode}」吗?删除后不可恢复。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteDraft(row.id)
  ElMessage.success('草稿已删除')
  load()
}
async function load() {
  loading.value = true
  try {
    const res = await listMyDrafts({ ...query, keyword: keyword.value || undefined })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.drafts {
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
.search-group {
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
