<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ total }} 条修复申请</span>
        <div class="filter-inline">
          <el-select v-model="query.status" placeholder="审核状态" clearable style="width: 140px" @change="load">
            <el-option label="待审" value="PENDING" />
            <el-option label="通过" value="PASS" />
            <el-option label="驳回" value="REJECT" />
          </el-select>
          <el-select v-model="query.targetType" placeholder="主体类型" clearable style="width: 140px" @change="load">
            <el-option label="企业" value="ENTERPRISE" />
            <el-option label="个人" value="PERSONAL" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openApply()">发起修复申请</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="repairNo" label="修复单号" min-width="170" show-overflow-tooltip />
      <el-table-column prop="targetType" label="主体类型" width="110">
        <template #default="{ row }">
          <el-tag :type="row.targetType === 'ENTERPRISE' ? 'primary' : 'success'" size="small">{{ row.targetType === 'ENTERPRISE' ? '企业' : row.targetType === 'PERSONAL' ? '个人' : row.targetType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请主体" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.targetName || '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="repairReason" label="修复原因" min-width="200" show-overflow-tooltip />
      <el-table-column label="申请时间" width="160">
        <template #default="{ row }">{{ (row.applyTime || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column prop="auditStatus" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.auditStatus]?.color" size="small">{{ statusMap[row.auditStatus]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditOpinion" label="审核意见" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <template v-if="row.auditStatus === 'PENDING'">
            <el-button link type="success" @click="openAudit(row, 'PASS')">通过</el-button>
            <el-button link type="danger" @click="openAudit(row, 'REJECT')">驳回</el-button>
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

    <!-- 修复申请 -->
    <el-dialog v-model="apply.visible" title="发起信用修复申请" width="460px">
      <el-form label-width="90px">
        <el-form-item label="主体类型">
          <el-select v-model="apply.targetType" style="width: 100%">
            <el-option label="企业" value="ENTERPRISE" />
            <el-option label="个人" value="PERSONAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="主体ID">
          <el-input-number v-model="apply.targetId" :min="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="修复原因">
          <el-input v-model="apply.repairReason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="apply.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 审核 -->
    <el-dialog v-model="audit.visible" :title="`审核：${audit.auditStatus === 'PASS' ? '通过' : '驳回'}`" width="440px">
      <el-form label-width="90px">
        <el-form-item label="申请ID">
          <span>{{ audit.id }}</span>
        </el-form-item>
        <el-form-item label="修复原因">
          <span>{{ audit.repairReason }}</span>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="audit.auditOpinion" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="audit.visible = false">取消</el-button>
        <el-button :type="audit.auditStatus === 'PASS' ? 'success' : 'danger'" :loading="saving" @click="onAudit">确认</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { creditRepairApi } from '../../api/modules'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ page: 1, size: 10, status: '', targetType: '' })
const statusMap = {
  PENDING: { label: '待审', color: 'warning' },
  PASS: { label: '通过', color: 'success' },
  REJECT: { label: '驳回', color: 'danger' },
}

const apply = reactive({ visible: false, targetType: 'ENTERPRISE', targetId: 1, repairReason: '' })
const audit = reactive({ visible: false, id: null, repairReason: '', auditStatus: 'PASS', auditOpinion: '' })

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size }
  if (query.status) params.status = query.status
  if (query.targetType) params.targetType = query.targetType
  try {
    const data = await creditRepairApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function openApply() {
  apply.targetType = 'ENTERPRISE'
  apply.targetId = 1
  apply.repairReason = ''
  apply.visible = true
}

async function onApply() {
  if (!apply.repairReason) { ElMessage.warning('请输入修复原因'); return }
  saving.value = true
  try {
    await creditRepairApi.apply({ targetType: apply.targetType, targetId: apply.targetId, repairReason: apply.repairReason })
    ElMessage.success('申请提交成功')
    apply.visible = false
    load()
  } finally {
    saving.value = false
  }
}

function openAudit(row, status) {
  audit.id = row.id
  audit.repairReason = row.repairReason
  audit.auditStatus = status
  audit.auditOpinion = ''
  audit.visible = true
}

async function onAudit() {
  saving.value = true
  try {
    await creditRepairApi.audit({ id: audit.id, auditStatus: audit.auditStatus, auditOpinion: audit.auditOpinion })
    ElMessage.success('审核完成')
    audit.visible = false
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