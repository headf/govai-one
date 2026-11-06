<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ total }} 条预警</span>
        <div class="filter-inline">
          <el-select v-model="query.level" placeholder="级别" clearable style="width: 120px" @change="load">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
          <el-select v-model="query.handleStatus" placeholder="处置状态" clearable style="width: 140px" @change="load">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="HANDLED" />
          </el-select>
          <el-select v-model="query.targetType" placeholder="对象类型" clearable style="width: 140px" @change="load">
            <el-option label="企业" value="ENTERPRISE" />
            <el-option label="个人" value="PERSONAL" />
            <el-option label="审批实例" value="INSTANCE" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="alertNo" label="预警编号" min-width="170" show-overflow-tooltip />
      <el-table-column prop="targetType" label="对象类型" width="110" />
      <el-table-column label="对象" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.targetType === 'ENTERPRISE' ? (row.enterpriseName || '—') : '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="alertLevel" label="级别" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="levelMap[row.alertLevel]?.color">{{ levelMap[row.alertLevel]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="alertContent" label="预警内容" min-width="220" show-overflow-tooltip />
      <el-table-column prop="alertTime" label="预警时间" width="170" />
      <el-table-column prop="handleStatus" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.handleStatus === 'HANDLED' ? 'info' : 'danger'" size="small">{{ row.handleStatus === 'HANDLED' ? '已处理' : '待处理' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handleOpinion" label="处置意见" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.handleStatus === 'PENDING' && hasPerm('alert:handle')">
            <el-button link type="primary" @click="openHandle(row)">处置</el-button>
            <el-button link type="warning" @click="dispatchAlert(row)">转抽查</el-button>
          </template>
          <span v-else>—</span>
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

    <!-- 处置对话框 -->
    <el-dialog v-model="dialog.visible" title="处置预警" width="480px">
      <el-form label-width="90px">
        <el-form-item label="预警内容">
          <span>{{ dialog.alertContent }}</span>
        </el-form-item>
        <el-form-item label="风险级别">
          <el-tag size="small" :type="levelMap[dialog.alertLevel]?.color">{{ levelMap[dialog.alertLevel]?.label }}</el-tag>
        </el-form-item>
        <el-form-item label="处置方式">
          <el-select v-model="dialog.opinion" placeholder="选择处置方式 / 填写意见" style="width: 100%" allow-create filterable default-first-option>
            <el-option label="专项检查" value="已安排专项检查" />
            <el-option label="约谈" value="已约谈监管对象" />
            <el-option label="加强巡查" value="已加强日常巡查" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置意见">
          <el-input v-model="dialog.detailOpinion" type="textarea" :rows="2" placeholder="补充具体说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onConfirmHandle">确认处置</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { riskAlertApi } from '../../api/modules'
import { hasPerm } from '../../utils/perm'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ page: 1, size: 10, level: '', handleStatus: '', targetType: '' })
const dialog = reactive({ visible: false, id: null, alertContent: '', alertLevel: '', opinion: '', detailOpinion: '' })

const levelMap = {
  LOW: { label: '低', color: 'info' },
  MEDIUM: { label: '中', color: 'warning' },
  HIGH: { label: '高', color: 'danger' },
}

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size }
  if (query.level) params.level = query.level
  if (query.handleStatus) params.handleStatus = query.handleStatus
  if (query.targetType) params.targetType = query.targetType
  try {
    const data = await riskAlertApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function openHandle(row) {
  dialog.id = row.id
  dialog.alertContent = row.alertContent
  dialog.alertLevel = row.alertLevel
  dialog.opinion = ''
  dialog.detailOpinion = ''
  dialog.visible = true
}

async function onConfirmHandle() {
  const full = [dialog.opinion, dialog.detailOpinion].filter(Boolean).join('；')
  if (!full) { ElMessage.warning('请填写处置意见'); return }
  saving.value = true
  try {
    await riskAlertApi.handle(dialog.id, full)
    ElMessage.success('已处置')
    dialog.visible = false
    load()
  } finally {
    saving.value = false
  }
}

// 无感监管:预警一键转定向抽查(自动分派检查员并处置)
async function dispatchAlert(row) {
  try {
    await ElMessageBox.confirm(`将预警「${row.alertContent}」自动转为一次定向抽查并记录处置吗?`, '预警转抽查', {
      type: 'warning',
      confirmButtonText: '转抽查',
      cancelButtonText: '取消'
    })
  } catch (e) { return }
  await riskAlertApi.dispatch(row.id)
  ElMessage.success('已生成定向抽查并处置该预警')
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
