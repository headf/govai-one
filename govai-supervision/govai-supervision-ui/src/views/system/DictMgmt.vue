<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ total }} 条字典项</span>
        <div class="filter-inline">
          <el-select v-model="query.dictType" placeholder="字典类型" clearable style="width: 220px" @change="load">
            <el-option v-for="g in groups" :key="g.dictType" :label="g.dictType" :value="g.dictType" />
          </el-select>
          <el-input v-model="query.keyword" placeholder="类型/标签/值关键字" clearable style="width: 200px" @clear="load" @keyup.enter="load" />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openForm()">新增字典项</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="dictType" label="字典类型" width="160" show-overflow-tooltip />
      <el-table-column prop="dictLabel" label="显示名" min-width="130" />
      <el-table-column prop="dictValue" label="字典值" width="130" />
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-popconfirm title="确认删除该字典项？" @confirm="onRemove(row)">
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

    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑字典项' : '新增字典项'" width="440px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="form.dictType" placeholder="如 credit_level / alert_level" />
        </el-form-item>
        <el-form-item label="显示名" prop="dictLabel">
          <el-input v-model="form.dictLabel" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="form.dictValue" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
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
import { dictApi } from '../../api/modules'

const list = ref([])
const groups = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ page: 1, size: 10, dictType: '', keyword: '' })

const dialog = reactive({ visible: false, isEdit: false })
const formRef = ref()
const form = reactive({ id: null, dictType: '', dictLabel: '', dictValue: '', sortOrder: 0, status: 1 })
const rules = {
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
  dictLabel: [{ required: true, message: '请输入显示名', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }],
}

async function loadGroups() {
  groups.value = await dictApi.group() || []
}

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size }
  if (query.dictType) params.dictType = query.dictType
  if (query.keyword) params.keyword = query.keyword
  try {
    const data = await dictApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  dialog.isEdit = !!row
  if (row) {
    Object.assign(form, { id: row.id, dictType: row.dictType, dictLabel: row.dictLabel, dictValue: row.dictValue, sortOrder: row.sortOrder, status: row.status })
  } else {
    Object.assign(form, { id: null, dictType: query.dictType || '', dictLabel: '', dictValue: '', sortOrder: 0, status: 1 })
  }
  dialog.visible = true
}

async function onSave() {
  await formRef.value.validate().catch(() => { throw new Error('') })
  saving.value = true
  try {
    if (dialog.isEdit) {
      await dictApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await dictApi.add(form)
      ElMessage.success('新增成功')
    }
    dialog.visible = false
    loadGroups()
    load()
  } finally {
    saving.value = false
  }
}

async function onRemove(row) {
  await dictApi.remove(row.id)
  ElMessage.success('删除成功')
  loadGroups()
  load()
}

onMounted(() => { loadGroups(); load() })
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
