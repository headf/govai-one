<template>
  <div class="operators">
    <el-card v-loading="loading">
      <template #header>
        <div class="list-head">
          <div>
            <div class="list-sub">法人(管理员)可添加本企业经办人,经办人只能办理业务,不可修改法人信息</div>
          </div>
          <el-button type="primary" :icon="Plus" @click="openAdd">添加经办人</el-button>
        </div>
      </template>
      <el-table :data="list" stripe>
        <el-table-column label="账号" prop="username" width="140" />
        <el-table-column label="姓名" prop="realName" width="120" />
        <el-table-column label="手机号" prop="phone" width="140" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'ADMIN'" type="danger" size="small">法人</el-tag>
            <el-tag v-else type="primary" size="small">经办人</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">正常</el-tag>
            <el-tag v-else type="info" size="small">已禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.role === 'OPERATOR' && row.status === 1"
              type="danger"
              size="small"
              plain
              @click="onDisable(row)"
            >禁用</el-button>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="添加经办人" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="经办人真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" placeholder="默认 123456" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">确定添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { addOperator, disableOperator, listOperators, type EnterpriseUser } from '@/api'

const loading = ref(false)
const saving = ref(false)
const list = ref<EnterpriseUser[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  password: '123456'
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

async function load() {
  loading.value = true
  try {
    list.value = await listOperators()
  } finally {
    loading.value = false
  }
}

function openAdd() {
  form.username = ''
  form.realName = ''
  form.phone = ''
  form.password = '123456'
  dialogVisible.value = true
}

async function onSubmit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    await addOperator({
      username: form.username.trim(),
      password: form.password || '123456',
      realName: form.realName.trim(),
      phone: form.phone.trim()
    })
    ElMessage.success('经办人已添加,默认密码 123456')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onDisable(row: EnterpriseUser) {
  await ElMessageBox.confirm(`确定禁用经办人「${row.realName}(${row.username})」吗?禁用后该账号无法登录`, '提示', { type: 'warning' })
  await disableOperator(row.id)
  ElMessage.success('已禁用')
  await load()
}

onMounted(load)
</script>

<style scoped>
.operators {
  display: grid;
  gap: 16px;
  padding: 4px 0 24px;
}
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  width: 100%;
}
.list-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #9ca3af;
}
.text-muted {
  color: #909399;
}
</style>
