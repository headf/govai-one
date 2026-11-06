<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ list.length }} 个角色</span>
        <div class="filter-inline">
          <el-input v-model="query.keyword" placeholder="角色名" clearable style="width: 200px" @keyup.enter="load" />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openForm()">新增角色</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="roleName" label="角色名" min-width="130" show-overflow-tooltip />
      <el-table-column prop="roleCode" label="角色编码" min-width="110" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="success" @click="openGrant(row)">授权</el-button>
          <el-popconfirm title="确认删除该角色？" @confirm="onRemove(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑角色' : '新增角色'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="dialog.isEdit" placeholder="ADMIN/ APPROVER/ SUPERVISOR..." />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 授权 -->
    <el-dialog v-model="grant.visible" :title="`为【${grant.roleName}】分配权限`" width="460px">
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="{ label: 'permissionName', children: 'children' }"
        style="border: 1px solid #eee; padding: 10px; max-height: 400px; overflow: auto;"
      />
      <template #footer>
        <el-button @click="grant.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onGrant">保存授权</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { roleApi, permissionApi } from '../../api/modules'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ keyword: '' })

const dialog = reactive({ visible: false, isEdit: false })
const formRef = ref()
const form = reactive({ id: null, roleName: '', roleCode: '', description: '', status: 1 })
const rules = {
  roleName: [{ required: true, message: '请输入角色名', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

const grant = reactive({ visible: false, roleId: null, roleName: '', checked: [] })
const permissionTree = ref([])
const treeRef = ref()

async function load() {
  loading.value = true
  const params = {}
  if (query.keyword) params.keyword = query.keyword
  try {
    const data = await roleApi.list(params)
    list.value = data || []
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  dialog.isEdit = !!row
  if (row) {
    Object.assign(form, { id: row.id, roleName: row.roleName, roleCode: row.roleCode, description: row.description, status: row.status })
  } else {
    Object.assign(form, { id: null, roleName: '', roleCode: '', description: '', status: 1 })
  }
  dialog.visible = true
}

async function onSave() {
  await formRef.value.validate().catch(() => { throw new Error('') })
  saving.value = true
  try {
    if (dialog.isEdit) {
      await roleApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await roleApi.add(form)
      ElMessage.success('新增成功')
    }
    dialog.visible = false
    load()
  } finally {
    saving.value = false
  }
}

async function onRemove(row) {
  try {
    await roleApi.remove(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    // 后端会提示：该角色被引用不可删除
  }
}

async function openGrant(row) {
  grant.roleId = row.id
  grant.roleName = row.roleName
  grant.visible = true
  const [tree, ids] = await Promise.all([permissionApi.tree(), permissionApi.byRole(row.id)])
  permissionTree.value = tree || []
  grant.checked = (ids || []).map(String)
  // 等待 DOM 渲染后回填勾选
  nextTick(() => {
    treeRef.value?.setCheckedKeys(ids || [])
  })
}

async function onGrant() {
  saving.value = true
  try {
    const ids = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys()).map((n) => Number(n))
    await permissionApi.grant({ roleId: grant.roleId, permissionIds: ids })
    ElMessage.success('授权成功')
    grant.visible = false
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
</style>