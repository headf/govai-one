<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ list.length }} 条规则</span>
        <div class="filter-inline">
          <el-select v-model="query.ruleType" placeholder="规则类型" clearable style="width: 150px" @change="load">
            <el-option label="超时" value="TIMEOUT" />
            <el-option label="阈值" value="THRESHOLD" />
            <el-option label="异常" value="ANOMALY" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openForm()">新增规则</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="ruleName" label="规则名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="ruleType" label="类型" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="ruleTypeMap[row.ruleType]?.color">{{ ruleTypeMap[row.ruleType]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="thresholdValue" label="阈值" min-width="100" />
      <el-table-column prop="alertLevel" label="预警级别" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="levelMap[row.alertLevel]?.color">{{ levelMap[row.alertLevel]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="notifyType" label="通知方式" width="120" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-popconfirm title="确认删除该规则？" @confirm="onRemove(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑规则' : '新增规则'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="规则名称">
          <el-input v-model="form.ruleName" />
        </el-form-item>
        <el-form-item label="规则类型">
          <el-select v-model="form.ruleType" style="width: 100%">
            <el-option label="超时" value="TIMEOUT" />
            <el-option label="阈值" value="THRESHOLD" />
            <el-option label="异常" value="ANOMALY" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值">
          <el-input v-model="form.thresholdValue" placeholder="如 24（小时）" />
        </el-form-item>
        <el-form-item label="预警级别">
          <el-select v-model="form.alertLevel" style="width: 100%">
            <el-option v-for="l in ['LOW', 'MEDIUM', 'HIGH']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知方式">
          <el-select v-model="form.notifyType" style="width: 100%">
            <el-option label="站内" value="SITE" />
            <el-option label="短信" value="SMS" />
            <el-option label="邮件" value="EMAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { riskRuleApi } from '../../api/modules'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const query = reactive({ ruleType: '', status: '' })
const dialog = reactive({ visible: false, isEdit: false })
const form = reactive({ id: null, ruleName: '', ruleType: 'TIMEOUT', thresholdValue: '', alertLevel: 'MEDIUM', notifyType: 'SITE', status: 1 })

const ruleTypeMap = {
  TIMEOUT: { label: '超时', color: 'warning' },
  THRESHOLD: { label: '阈值', color: 'primary' },
  ANOMALY: { label: '异常', color: 'danger' },
}
const levelMap = {
  LOW: { label: '低', color: 'info' },
  MEDIUM: { label: '中', color: 'warning' },
  HIGH: { label: '高', color: 'danger' },
}

async function load() {
  loading.value = true
  const params = {}
  if (query.ruleType) params.ruleType = query.ruleType
  if (query.status !== '') params.status = query.status
  try {
    list.value = (await riskRuleApi.list(params)) || []
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  dialog.isEdit = !!row
  Object.assign(form, row
    ? { id: row.id, ruleName: row.ruleName, ruleType: row.ruleType, thresholdValue: row.thresholdValue, alertLevel: row.alertLevel, notifyType: row.notifyType, status: row.status }
    : { id: null, ruleName: '', ruleType: 'TIMEOUT', thresholdValue: '', alertLevel: 'MEDIUM', notifyType: 'SITE', status: 1 })
  dialog.visible = true
}

async function onSave() {
  if (!form.ruleName) { ElMessage.warning('请输入规则名称'); return }
  saving.value = true
  try {
    if (dialog.isEdit) {
      await riskRuleApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await riskRuleApi.add(form)
      ElMessage.success('新增成功')
    }
    dialog.visible = false
    load()
  } finally {
    saving.value = false
  }
}

async function onRemove(row) {
  await riskRuleApi.remove(row.id)
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
</style>