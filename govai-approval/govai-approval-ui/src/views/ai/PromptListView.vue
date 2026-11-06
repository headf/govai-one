<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { aiApi } from '@/api'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const promptFormRef = ref<FormInstance>()

const query = reactive({
  sceneType: '',
  page: 1,
  size: 20,
})

const sceneOptions = [
  { label: '全部', value: '' },
  { label: '审批意见', value: 'OPINION' },
  { label: '政策摘要', value: 'POLICY_SUMMARY' },
  { label: '风险提示', value: 'RISK' },
]

const sceneLabel: Record<string, string> = {
  OPINION: '审批意见',
  POLICY_SUMMARY: '政策摘要',
  RISK: '风险提示',
}

const sceneTagType: Record<string, string> = {
  OPINION: 'primary',
  POLICY_SUMMARY: 'success',
  RISK: 'danger',
}

// 编辑弹窗
const editDialog = reactive({
  visible: false,
  form: {
    id: undefined as number | undefined,
    templateName: '',
    sceneType: 'OPINION',
    systemPrompt: '',
    userPromptTemplate: '',
    variables: '',
    status: 1,
  },
})

const promptRules: FormRules = {
  templateName: [{ required: true, message: '请填写模板名称', trigger: 'blur' }],
  sceneType: [{ required: true, message: '请选择场景类型', trigger: 'change' }],
  systemPrompt: [{ required: true, message: '请填写系统提示词', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await aiApi.pageTemplates(query)
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

function openCreate() {
  editDialog.visible = true
  editDialog.form = {
    id: undefined,
    templateName: '',
    sceneType: 'OPINION',
    systemPrompt: '',
    userPromptTemplate: '',
    variables: '',
    status: 1,
  }
  setTimeout(() => promptFormRef.value?.clearValidate(), 0)
}

function openEdit(row: any) {
  editDialog.visible = true
  editDialog.form = { ...row }
  setTimeout(() => promptFormRef.value?.clearValidate(), 0)
}

async function handleSave() {
  const valid = await promptFormRef.value?.validate().catch(() => false)
  if (!valid) return
  await aiApi.saveTemplate(editDialog.form)
  ElMessage.success('保存成功')
  editDialog.visible = false
  loadData()
}

function fmtTime(t?: string) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除模板"${row.templateName}"？`, '提示', { type: 'warning' })
    await aiApi.deleteTemplate(row.id)
    ElMessage.success('删除成功')
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
          <span style="font-size: 13px; color: #909399">共 {{ total }} 条模板</span>
          <div class="filter-bar" style="margin-bottom: 0">
            <span>场景类型：</span>
            <el-select v-model="query.sceneType" placeholder="全部" style="width: 140px" clearable>
              <el-option v-for="opt in sceneOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button type="success" @click="openCreate">新增模板</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
        <el-table-column prop="templateName" label="模板名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sceneType" label="场景类型" width="120">
          <template #default="{ row }">
            <el-tag :type="sceneTagType[row.sceneType] || 'info'" size="small">
              {{ sceneLabel[row.sceneType] || row.sceneType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="systemPrompt" label="系统提示词" min-width="220" show-overflow-tooltip />
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
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="(p: number) => { query.page = p; loadData() }"
      />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialog.visible" title="Prompt模板" width="700px" @closed="promptFormRef?.clearValidate">
      <el-form ref="promptFormRef" :model="editDialog.form" :rules="promptRules" label-width="120px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="editDialog.form.templateName" placeholder="如 审批意见生成模板" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="场景类型" prop="sceneType">
          <el-select v-model="editDialog.form.sceneType">
            <el-option label="审批意见" value="OPINION" />
            <el-option label="政策摘要" value="POLICY_SUMMARY" />
            <el-option label="风险提示" value="RISK" />
          </el-select>
        </el-form-item>
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input v-model="editDialog.form.systemPrompt" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="System Prompt" />
        </el-form-item>
        <el-form-item label="用户提示词模板">
          <el-input v-model="editDialog.form.userPromptTemplate" type="textarea" :rows="5" maxlength="2000" show-word-limit placeholder="使用 {变量名} 占位，如 {matterName}" />
        </el-form-item>
        <el-form-item label="变量列表">
          <el-input v-model="editDialog.form.variables" placeholder='如 ["matterName","applicantName"]' maxlength="200" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editDialog.form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
