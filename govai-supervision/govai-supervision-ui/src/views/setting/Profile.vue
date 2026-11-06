<template>
  <div class="profile">
    <el-card shadow="never" class="card">
      <template #header><span class="section-title">个人资料</span></template>
      <el-form v-if="profile" label-width="90px" style="max-width: 520px">
        <el-form-item label="用户名">
          <span>{{ profile.username }}</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-tag size="small">{{ roleText }}</el-tag>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editForm.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="editForm.department" />
        </el-form-item>
        <el-form-item label="创建时间">
          <span>{{ profile.createTime }}</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSaveProfile">保存资料</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="card">
      <template #header><span class="section-title">修改密码</span></template>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px" style="max-width: 520px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="savingPwd" @click="onChangePwd">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { profileApi } from '../../api/modules'
import { clearToken } from '../../api/request'
import { clearPermissions } from '../../utils/perm'

const router = useRouter()
const profile = ref(null)
const saving = ref(false)
const savingPwd = ref(false)
const roleText = computed(() => profile.value?.roleName || localStorage.getItem('govai_role') || '')

const editForm = reactive({ realName: '', phone: '', email: '', department: '' })
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '新密码至少 6 位', trigger: 'blur' }],
  confirmPassword: [{
    validator: (r, v, cb) => (v === pwdForm.newPassword ? cb() : cb(new Error('两次输入的密码不一致'))),
    trigger: 'blur',
  }],
}

async function loadProfile() {
  profile.value = await profileApi.get()
  Object.assign(editForm, {
    realName: profile.value.realName || '',
    phone: profile.value.phone || '',
    email: profile.value.email || '',
    department: profile.value.department || '',
  })
}

async function onSaveProfile() {
  saving.value = true
  try {
    await profileApi.update({ ...editForm })
    ElMessage.success('资料已保存')
    localStorage.setItem('govai_userName', editForm.realName || localStorage.getItem('govai_userName'))
    loadProfile()
  } finally {
    saving.value = false
  }
}

async function onChangePwd() {
  await pwdFormRef.value.validate().catch(() => { throw new Error('') })
  savingPwd.value = true
  try {
    await profileApi.changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码已修改，请重新登录')
    clearToken()
    clearPermissions()
    localStorage.removeItem('govai_userName')
    localStorage.removeItem('govai_role')
    router.push('/login')
  } finally {
    savingPwd.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile { display: flex; flex-direction: column; gap: 16px; max-width: 720px; }
.card { width: 100%; }
.section-title {
  font-size: 16px;
  font-weight: 600;
  padding-left: 8px;
  border-left: 4px solid var(--govai-brand, #2f6bff);
  margin-bottom: 0;
}
</style>
