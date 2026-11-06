<template>
  <div class="login-page">
    <div class="login-shell">
      <aside class="brand-panel">
        <div class="brand-top">
          <div class="brand-mark"><el-icon :size="22"><Monitor /></el-icon></div>
          <div>
            <div class="brand-name">GovAI One</div>
            <div class="brand-slogan">综合监管平台</div>
          </div>
        </div>
        <div class="brand-body">
          <div class="brand-title">监管数字中枢<br />数据实时汇聚 · 风险闭环处置</div>
          <ul class="brand-feats">
            <li><i class="dot" />审批办理数据实时汇聚，大屏一屏总览</li>
            <li><i class="dot" />双随机抽查、监测预警、信用风险全程留痕</li>
            <li><i class="dot" />多端统一账号，监管 / 审批 / 企业权责分明</li>
          </ul>
        </div>
        <div class="brand-foot">GovAI One 智慧政务 · 综合监管平台</div>
      </aside>

      <section class="form-card">
        <div class="form-head">
          <h2>欢迎登录</h2>
          <p>请输入账号密码（系统自动识别所属端）</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="onSubmit">登 录</el-button>
          </el-form-item>
        </el-form>
        <p class="login-hint">预置账号：admin / approver1 / supervisor1（密码均为 123456）</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Monitor } from '@element-plus/icons-vue'
import { authApi } from '../api/modules'
import { setToken, clearToken } from '../api/request'
import { setPermissions } from '../utils/perm'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  await formRef.value.validate().catch(() => { throw new Error('') })
  loading.value = true
  try {
    const loginParam = { username: form.username, password: form.password }
    const data = await authApi.login(loginParam)
    clearToken()
    setToken(data.token)
    localStorage.setItem('govai_userName', data.userName || form.username)
    localStorage.setItem('govai_role', data.role || '')
    setPermissions(data.permissions || [])
    ElMessage.success(`欢迎，${data.userName || form.username}`)
    router.push('/dashboard')
  } catch (e) {
    // 错误已由 request 拦截器提示
  } finally {
    loading.value = false
  }
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
.login-hint {
  margin: 12px 0 0;
  text-align: center;
  font-size: 12px;
  color: #a8abb2;
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
