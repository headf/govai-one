<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ total }} 名个人</span>
        <div class="filter-inline">
          <el-input v-model="query.userId" placeholder="用户ID" clearable style="width: 150px" @keyup.enter="load" />
          <el-select v-model="query.level" placeholder="信用等级" clearable style="width: 130px" @change="load">
            <el-option v-for="l in ['A', 'B', 'C', 'D']" :key="l" :label="l" :value="l" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openScore()">个人评分</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="creditNo" label="信用单号" min-width="170" show-overflow-tooltip />
      <el-table-column label="用户" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.userName || '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="creditScore" label="信用评分" min-width="120" />
      <el-table-column prop="creditLevel" label="等级" width="90">
        <template #default="{ row }">
          <el-tag :type="levelMap[row.creditLevel]" size="small">{{ row.creditLevel }}级</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ (row.updateTime || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openScore(row)">评分</el-button>
          <el-popconfirm title="确认删除该个人评分？" @confirm="onRemove(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
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

    <el-dialog v-model="score.visible" title="个人评分" width="420px">
      <el-form label-width="90px">
        <el-form-item label="用户ID">
          <el-input v-model.number="score.userId" placeholder="userId" />
        </el-form-item>
        <el-form-item label="信用评分">
          <el-input-number v-model="score.creditScore" :min="0" :max="100" :precision="1" style="width: 200px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="score.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { creditPersonalApi } from '../../api/modules'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ page: 1, size: 10, userId: '', level: '' })
const levelMap = { A: 'success', B: 'primary', C: 'warning', D: 'danger' }
const score = reactive({ visible: false, userId: null, creditScore: 0 })

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size }
  if (query.userId) params.userId = query.userId
  if (query.level) params.level = query.level
  try {
    const data = await creditPersonalApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function openScore(row) {
  score.userId = row?.userId || ''
  score.creditScore = row?.creditScore || 0
  score.visible = true
}

async function onSubmit() {
  if (!score.userId) { ElMessage.warning('请输入用户ID'); return }
  saving.value = true
  try {
    await creditPersonalApi.score({ targetId: score.userId, creditScore: score.creditScore })
    ElMessage.success('评分成功')
    score.visible = false
    load()
  } finally {
    saving.value = false
  }
}

async function onRemove(row) {
  await creditPersonalApi.remove(row.userId)
  ElMessage.success('删除成功')
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
</style>