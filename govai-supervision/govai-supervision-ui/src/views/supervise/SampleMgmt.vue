<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ total }} 条抽查记录</span>
        <div class="filter-inline">
          <el-select v-model="query.sampleType" placeholder="抽样类型" clearable style="width: 150px" @change="load">
            <el-option label="随机" value="RANDOM" />
            <el-option label="定向" value="TARGETED" />
          </el-select>
          <el-select v-model="query.checkResult" placeholder="检查结果" clearable style="width: 140px" @change="load">
            <el-option label="通过" value="PASS" />
            <el-option label="不合格" value="FAIL" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="warning" @click="random.visible = true">随机抽样</el-button>
          <el-button type="success" @click="target.visible = true">定向抽查</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="sampleNo" label="抽查单号" min-width="170" show-overflow-tooltip />
      <el-table-column prop="instanceId" label="审批实例ID" min-width="120" />
      <el-table-column prop="sampleType" label="类型" width="110">
        <template #default="{ row }">
          <el-tag :type="row.sampleType === 'RANDOM' ? 'primary' : 'success'" size="small">{{ typeText[row.sampleType] || row.sampleType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="checkResult" label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.checkResult === 'PASS' ? 'success' : 'danger'" size="small">{{ resultText[row.checkResult] || row.checkResult }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="checkDetail" label="检查详情" min-width="200" show-overflow-tooltip />
      <el-table-column label="检查时间" width="160">
        <template #default="{ row }">{{ (row.checkTime || '').replace('T', ' ').slice(0, 16) }}</template>
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

    <!-- 随机抽样 -->
    <el-dialog v-model="random.visible" title="随机抽样" width="460px">
      <el-form label-width="110px">
        <el-form-item label="抽样数量">
          <el-input-number v-model="random.count" :min="1" :max="100" style="width: 160px" />
        </el-form-item>
        <el-form-item label="信用差异化">
          <el-checkbox v-model="random.creditWeighted">按信用等级加权抽取(D级优先命中,落实差异化监管)</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="random.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onRandom">开始抽样</el-button>
      </template>
    </el-dialog>

    <!-- 定向抽查 -->
    <el-dialog v-model="target.visible" title="定向抽查" width="460px">
      <el-form label-width="90px">
        <el-form-item label="审批实例ID">
          <el-input-number v-model="target.instanceId" :min="1" style="width: 220px" />
        </el-form-item>
        <el-form-item label="检查结果">
          <el-radio-group v-model="target.checkResult">
            <el-radio value="PASS">通过</el-radio>
            <el-radio value="FAIL">不合格</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="检查详情">
          <el-input v-model="target.checkDetail" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="target.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onTargeted">提交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sampleApi } from '../../api/modules'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ page: 1, size: 10, sampleType: '', checkResult: '' })
const typeText = { RANDOM: '随机抽查', TARGETED: '定向抽查' }
const resultText = { PASS: '通过', FAIL: '不合格' }
const random = reactive({ visible: false, count: 5, creditWeighted: false })
const target = reactive({ visible: false, instanceId: null, checkResult: 'PASS', checkDetail: '' })

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size }
  if (query.sampleType) params.sampleType = query.sampleType
  if (query.checkResult) params.checkResult = query.checkResult
  try {
    const data = await sampleApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function onRandom() {
  if (!random.count || random.count <= 0) { ElMessage.warning('请输入抽样数量'); return }
  saving.value = true
  try {
    await sampleApi.random(random.count, random.creditWeighted)
    ElMessage.success(random.creditWeighted ? '信用差异化随机抽样完成(D级优先)' : '随机抽样完成')
    random.visible = false
    load()
  } finally {
    saving.value = false
  }
}

async function onTargeted() {
  if (!target.instanceId) { ElMessage.warning('请输入审批实例ID'); return }
  saving.value = true
  try {
    await sampleApi.targeted({ instanceId: target.instanceId, checkResult: target.checkResult, checkDetail: target.checkDetail })
    ElMessage.success('定向检查完成')
    target.visible = false
    load()
  } finally {
    saving.value = false
  }
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