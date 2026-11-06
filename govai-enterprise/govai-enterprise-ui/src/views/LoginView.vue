<template>
  <div class="login-page">
    <div class="login-shell">
      <aside class="brand-panel">
        <div class="brand-top">
          <div class="brand-mark"><el-icon :size="22"><OfficeBuilding /></el-icon></div>
          <div>
            <div class="brand-name">GovAI One</div>
            <div class="brand-slogan">企业服务门户</div>
          </div>
        </div>
        <div class="brand-body">
          <div class="brand-title">法人办事<br />一站通办 · AI 赋能</div>
          <ul class="brand-feats">
            <li><i class="dot" />电子证照全程网办，智能填表免重复录入</li>
            <li><i class="dot" />申报前 AI 合规预检，材料一次通过</li>
            <li><i class="dot" />进度全程可查，办结证照即时入账</li>
          </ul>
        </div>
        <div class="brand-foot">GovAI One 智慧政务 · 企业办事大厅</div>
      </aside>

      <section class="form-card">
        <div class="form-head">
          <h2>欢迎登录</h2>
          <p>请使用已授权的法人 / 经办人账号登录</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="登录账号">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="onSubmit">登 录</el-button>
          </el-form-item>
        </el-form>
        <el-alert type="info" :closable="false" class="hint">
          <template #title>
            演示账号：ent_admin / 123456（管理员） · ent_operator / 123456（经办人）
          </template>
        </el-alert>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await auth.login(form.username, form.password)
      ElMessage.success('登录成功')
      router.push((route.query.redirect as string) || '/dashboard')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: radial-gradient(1100px 560px at 88% -12%, rgba(47, 107, 255, 0.12), transparent 60%), #f0f2f5;
}
.login-shell {
  width: 920px;
  max-width: 100%;
  height: min(540px, 90vh);
  display: flex;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid rgba(16, 24, 40, 0.06);
  box-shadow: 0 24px 80px rgba(16, 24, 40, 0.16);
}
/* 左侧品牌面板 */
.brand-panel {
  flex: 0 0 46%;
  position: relative;
  overflow: hidden;
  color: #fff;
  padding: 40px 38px 30px;
  background: linear-gradient(150deg, #0d1b4c 0%, #123078 46%, #2f6bff 135%);
  display: flex;
  flex-direction: column;
}
.brand-panel::after {
  content: '';
  position: absolute;
  right: -90px;
  bottom: -100px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.16), transparent 66%);
}
.brand-top {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  z-index: 1;
}
.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-name {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.4px;
}
.brand-slogan {
  margin-top: 2px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}
.brand-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 22px;
  position: relative;
  z-index: 1;
}
.brand-title {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.5;
  letter-spacing: 0.5px;
}
.brand-feats {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 14px;
}
.brand-feats li {
  display: flex;
  gap: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.7;
}
.brand-feats li .dot {
  flex: none;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 8px;
  background: #6d97ff;
}
.brand-foot {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  position: relative;
  z-index: 1;
}
/* 右侧表单 */
.form-card {
  flex: 1;
  padding: 40px 48px 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.form-head {
  margin-bottom: 22px;
}
.form-head h2 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #1f2d3d;
  letter-spacing: 0.3px;
}
.form-head p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
.login-btn {
  width: 100%;
}
.hint {
  margin-top: 8px;
}
/* 大屏放大比例 */
.login-page {
  padding: 40px 32px;
}
.login-shell {
  width: 1120px;
  height: min(660px, 92vh);
}
.brand-panel {
  padding: 56px 52px;
}
.brand-name {
  font-size: 20px;
}
.brand-slogan {
  font-size: 13px;
}
.brand-title {
  font-size: 32px;
}
.brand-feats {
  gap: 18px;
}
.brand-feats li {
  font-size: 14px;
}
.form-card {
  padding: 48px 64px 40px;
}
.form-head h2 {
  font-size: 28px;
}
.form-head p {
  font-size: 14px;
}
@media (max-width: 860px) {
  .brand-panel {
    display: none;
  }
  .login-shell {
    width: 100%;
  }
}
</style>
