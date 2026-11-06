<template>
  <el-card shadow="never">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 内部用户 -->
      <el-tab-pane label="内部用户" name="internal">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="用户名/姓名" clearable style="width: 200px" @clear="load" @keyup.enter="load" />
          <el-select v-model="query.roleId" placeholder="角色" clearable style="width: 160px" @change="load">
            <el-option v-for="r in internalRoles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button v-if="hasPerm('user:add')" type="success" @click="openForm()">新增用户</el-button>
        </div>

        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="realName" label="姓名" min-width="100" />
          <el-table-column prop="roleName" label="角色" min-width="110" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column prop="email" label="邮箱" width="170" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <template v-if="hasPerm('user:edit')">
                <el-button link type="primary" @click="openForm(row)">编辑</el-button>
              </template>
              <el-button v-if="hasPerm('user:reset-pwd')" link type="warning" @click="openReset(row)">重置密码</el-button>
              <el-popconfirm v-if="hasPerm('user:delete')" title="确认删除该用户？" @confirm="onRemove(row)">
                <template #reference><el-button link type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="pager"
          layout="total, prev, pager, next, sizes"
          :total="total"
          :page-size="query.size"
          :current-page="query.page"
          :page-sizes="[10, 20, 50]"
          @size-change="(s) => { query.size = s; load() }"
          @current-change="(p) => { query.page = p; load() }"
        />

        <!-- 新增/编辑 -->
        <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑用户' : '新增用户'" width="480px">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :disabled="dialog.isEdit" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password :placeholder="dialog.isEdit ? '留空则不修改' : '请输入初始密码'" />
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" />
            </el-form-item>
            <el-form-item label="角色" prop="roleId">
              <el-select v-model="form.roleId" placeholder="选择角色" style="width: 100%">
                <el-option v-for="r in internalRoles" :key="r.id" :label="r.roleName" :value="r.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item label="部门" prop="department">
              <el-input v-model="form.department" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="dialog.visible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
          </template>
        </el-dialog>

        <!-- 重置密码 -->
        <el-dialog v-model="resetDialog.visible" title="重置密码" width="420px">
          <el-form label-width="80px">
            <el-form-item label="用户">
              <span>{{ resetDialog.username }}</span>
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="resetDialog.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="resetDialog.visible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="onReset">确认重置</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 企业用户 -->
      <el-tab-pane label="企业用户" name="enterprise">
        <el-alert
          type="info" :closable="false" class="ent-tip"
          title="企业用户由企业端法人自行维护。监管端仅展示；经办人可由监管端禁用(监管处置)，新增/改名请由企业法人登录企业端操作。"
        />
        <div class="toolbar">
          <el-input v-model="entQuery.enterpriseName" placeholder="企业名称" clearable style="width: 180px" @clear="entResetPage" @keyup.enter="entResetPage" />
          <el-select v-model="entQuery.role" placeholder="身份" clearable style="width: 120px" @change="entResetPage">
            <el-option label="法人" value="ADMIN" />
            <el-option label="经办人" value="OPERATOR" />
          </el-select>
          <el-select v-model="entQuery.status" placeholder="状态" clearable style="width: 120px" @change="entResetPage">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" @click="entResetPage">查询</el-button>
          <el-button @click="loadEnterpriseUsers">刷新</el-button>
        </div>

        <el-table :data="entPageList" v-loading="entLoading" stripe>
          <el-table-column prop="enterpriseName" label="所属企业" min-width="200">
            <template #default="{ row }">{{ row.enterpriseName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="username" label="登录账号" min-width="120" />
          <el-table-column prop="realName" label="姓名" min-width="100" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column prop="role" label="身份" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.role === 'ADMIN' ? 'warning' : 'primary'">{{ row.role === 'ADMIN' ? '法人' : '经办人' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-popconfirm
                v-if="row.role === 'OPERATOR' && row.status === 1 && hasPerm('user:edit')"
                title="禁用后该经办人将无法登录企业端，确认？"
                @confirm="onDisableEntUser(row)"
              >
                <template #reference><el-button link type="danger">禁用</el-button></template>
              </el-popconfirm>
              <el-popconfirm
                v-else-if="row.role === 'OPERATOR' && row.status === 0 && hasPerm('user:edit')"
                title="确认恢复该经办人的企业端登录？"
                @confirm="onEnableEntUser(row)"
              >
                <template #reference><el-button link type="success">启用</el-button></template>
              </el-popconfirm>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="pager"
          layout="total, prev, pager, next"
          :total="entFiltered.length"
          :page-size="entQuery.size"
          :current-page="entQuery.page"
          @current-change="(p) => { entQuery.page = p }"
        />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi, roleApi, creditEnterpriseApi } from '../../api/modules'
import { hasPerm } from '../../utils/perm'

// ===== 内部用户 =====
const activeTab = ref('internal')
const list = ref([])
const roles = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', roleId: '', status: '' })

const dialog = reactive({ visible: false, isEdit: false })
const formRef = ref()
const form = reactive({ id: null, username: '', password: '', realName: '', roleId: '', phone: '', email: '', department: '', status: 1 })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const resetDialog = reactive({ visible: false, id: null, username: '', newPassword: '' })

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size, onlyInternal: true }
  if (query.keyword) params.keyword = query.keyword
  if (query.roleId !== '') params.roleId = query.roleId
  if (query.status !== '') params.status = query.status
  try {
    const data = await userApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  const data = await roleApi.list()
  roles.value = data || []
}

// 内部用户 Tab 只展示/选择非企业角色（企业账号统一在企业用户 Tab 看）
const internalRoles = computed(() =>
  (roles.value || []).filter((r) => r.roleCode !== 'ENTERPRISE')
)

function openForm(row) {
  dialog.isEdit = !!row
  if (row) {
    Object.assign(form, {
      id: row.id, username: row.username, password: '', realName: row.realName,
      roleId: row.roleId, phone: row.phone, email: row.email, department: row.department, status: row.status,
    })
  } else {
    Object.assign(form, { id: null, username: '', password: '', realName: '', roleId: '', phone: '', email: '', department: '', status: 1 })
  }
  dialog.visible = true
}

async function onSave() {
  await formRef.value.validate().catch(() => { throw new Error('') })
  saving.value = true
  try {
    if (dialog.isEdit) {
      await userApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await userApi.add(form)
      ElMessage.success('新增成功')
    }
    dialog.visible = false
    load()
  } finally {
    saving.value = false
  }
}

function openReset(row) {
  resetDialog.id = row.id
  resetDialog.username = row.username
  resetDialog.newPassword = ''
  resetDialog.visible = true
}

async function onReset() {
  if (!resetDialog.newPassword) { ElMessage.warning('请输入新密码'); return }
  saving.value = true
  try {
    await userApi.resetPassword(resetDialog.id, resetDialog.newPassword)
    ElMessage.success('密码已重置')
    resetDialog.visible = false
  } finally {
    saving.value = false
  }
}

async function onRemove(row) {
  await userApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

// ===== 企业用户 =====
const entUsers = ref([])
const entLoading = ref(false)
const entQuery = reactive({ page: 1, size: 10, enterpriseName: '', role: '', status: '' })

const entFiltered = computed(() => {
  return entUsers.value.filter((u) => {
    if (entQuery.enterpriseName) {
      const name = (u.enterpriseName || '').toLowerCase()
      if (!name.includes(entQuery.enterpriseName.toLowerCase())) return false
    }
    if (entQuery.role !== '' && u.role !== entQuery.role) return false
    if (entQuery.status !== '' && u.status !== entQuery.status) return false
    return true
  })
})

const entPageList = computed(() => {
  const start = (entQuery.page - 1) * entQuery.size
  return entFiltered.value.slice(start, start + entQuery.size)
})

async function loadEnterpriseUsers() {
  entLoading.value = true
  try {
    entUsers.value = await creditEnterpriseApi.allEnterpriseUsers() || []
    entQuery.page = 1
  } finally {
    entLoading.value = false
  }
}

function entResetPage() {
  entQuery.page = 1
}

async function onDisableEntUser(row) {
  await creditEnterpriseApi.disableEnterpriseUser(row.id)
  ElMessage.success(`已禁用经办人 ${row.username || row.realName || ''}`)
  loadEnterpriseUsers()
}

async function onEnableEntUser(row) {
  await creditEnterpriseApi.enableEnterpriseUser(row.id)
  ElMessage.success(`已恢复经办人 ${row.username || row.realName || ''} 的登录`)
  loadEnterpriseUsers()
}

function onTabChange(name) {
  if (name === 'enterprise' && entUsers.value.length === 0 && !entLoading.value) {
    loadEnterpriseUsers()
  }
}

onMounted(() => { load(); loadRoles() })
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.pager { margin-top: 16px; justify-content: flex-end; }
.ent-tip { margin-bottom: 14px; }
.muted { color: #a8abb2; font-size: 13px; }
</style>
