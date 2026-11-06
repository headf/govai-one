<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">按 菜单 / 按钮 / 接口 维护系统权限</span>
        <div class="filter-inline">
          <el-button type="primary" @click="load">刷新</el-button>
          <el-button type="success" @click="openForm()">新增根权限</el-button>
        </div>
      </div>
    </template>

    <el-table :data="tree" v-loading="loading" row-key="id" :default-expand-all="true" stripe>
      <el-table-column prop="permissionName" label="权限名称" min-width="160" />
      <el-table-column prop="permissionCode" label="权限编码" min-width="160" />
      <el-table-column prop="type" label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="typeMap[row.type]?.color" size="small">{{ typeMap[row.type]?.label || row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column prop="path" label="路径" min-width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="success" @click="addChild(row)">新增子项</el-button>
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-popconfirm title="确认删除该权限？" @confirm="onRemove(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑权限' : '新增权限'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="权限名称">
          <el-input v-model="form.permissionName" />
        </el-form-item>
        <el-form-item label="权限编码">
          <el-input v-model="form.permissionCode" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="菜单" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
            <el-option label="接口" value="API" />
          </el-select>
        </el-form-item>
        <el-form-item label="父节点">
          <span>{{ form.parentId || '（无，作为根节点）' }}</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="form.path" placeholder="/admin/users" />
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
import { permissionApi } from '../../api/modules'

const tree = ref([])
const loading = ref(false)
const saving = ref(false)
const dialog = reactive({ visible: false, isEdit: false })
const form = reactive({ id: null, permissionName: '', permissionCode: '', type: 'MENU', parentId: null, sortOrder: 0, path: '' })

const typeMap = {
  MENU: { label: '菜单', color: 'primary' },
  BUTTON: { label: '按钮', color: 'success' },
  API: { label: '接口', color: 'info' },
}

async function load() {
  loading.value = true
  try {
    tree.value = (await permissionApi.tree()) || []
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  dialog.isEdit = !!row
  Object.assign(form, row
    ? { id: row.id, permissionName: row.permissionName, permissionCode: row.permissionCode, type: row.type, parentId: row.parentId, sortOrder: row.sortOrder, path: row.path }
    : { id: null, permissionName: '', permissionCode: '', type: 'MENU', parentId: null, sortOrder: 0, path: '' })
  dialog.visible = true
}

function addChild(row) {
  Object.assign(form, { id: null, permissionName: '', permissionCode: '', type: 'BUTTON', parentId: row.id, sortOrder: 0, path: '' })
  dialog.visible = true
}

async function onSave() {
  if (!form.permissionName) { ElMessage.warning('请输入权限名称'); return }
  saving.value = true
  try {
    if (dialog.isEdit) {
      await permissionApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await permissionApi.add(form)
      ElMessage.success('新增成功')
    }
    dialog.visible = false
    load()
  } finally {
    saving.value = false
  }
}

async function onRemove(row) {
  await permissionApi.remove(row.id)
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