<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">GovAI One 监管端</div>
      <el-menu
        :default-active="activePath"
        router
        background-color="#1a3a8f"
        text-color="#c3cbe0"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon><span>数据大屏</span>
        </el-menu-item>
        <el-sub-menu v-if="sysVisible" index="system">
          <template #title><el-icon><User /></el-icon><span>系统管理</span></template>
          <el-menu-item v-if="hasPerm('user:list')" index="/admin/users">用户管理</el-menu-item>
          <el-menu-item v-if="hasPerm('role:list')" index="/admin/roles">角色管理</el-menu-item>
          <el-menu-item v-if="hasPerm('perm:list')" index="/admin/permissions">权限管理</el-menu-item>
          <el-menu-item v-if="hasPerm('dict:list')" index="/admin/dicts">数据字典</el-menu-item>
          <el-menu-item v-if="hasPerm('log:list')" index="/admin/logs">操作日志</el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="creditVisible" index="credit">
          <template #title><el-icon><OfficeBuilding /></el-icon><span>信用管理</span></template>
          <el-menu-item v-if="hasPerm('credit:list')" index="/admin/credit/enterprise">企业信用</el-menu-item>
          <el-menu-item v-if="hasPerm('credit:list')" index="/admin/credit/personal">个人信用</el-menu-item>
          <el-menu-item v-if="hasPerm('repair:audit')" index="/admin/credit/repair">信用修复</el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="riskVisible" index="risk">
          <template #title><el-icon><Warning /></el-icon><span>风险预警</span></template>
          <el-menu-item v-if="hasPerm('risk:list')" index="/admin/risk/rules">预警规则</el-menu-item>
          <el-menu-item v-if="hasPerm('risk:list')" index="/admin/risk/alerts">预警处置</el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="superviseVisible" index="supervise">
          <template #title><el-icon><Finished /></el-icon><span>监管业务</span></template>
          <el-menu-item v-if="hasPerm('sample:list')" index="/admin/supervise/samples">监督检查</el-menu-item>
          <el-menu-item index="/admin/supervise/review-orders">差评整改</el-menu-item>
          <el-menu-item v-if="hasPerm('audit:list')" index="/admin/supervise/audit">审计追溯</el-menu-item>
          <el-menu-item v-if="hasPerm('approval:list')" index="/admin/supervise/approval-data">审批数据</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/admin/setting/profile">
          <el-icon><Setting /></el-icon><span>个人中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ pageTitle }}</div>
        <div class="header-user">
          <el-tag size="small" effect="plain">{{ roleText }}</el-tag>
          <span class="uname">{{ userName }}</span>
          <el-button link @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearToken } from '../api/request'
import { hasPerm, clearPermissions } from '../utils/perm'

const route = useRoute()
const router = useRouter()

const userName = localStorage.getItem('govai_userName') || ''
const role = localStorage.getItem('govai_role') || ''

const roleMap = { ADMIN: '管理员', SUPERVISOR: '监管员', APPROVER: '审批员', CITIZEN: '群众', ENTERPRISE: '企业' }
const roleText = computed(() => roleMap[role] || role)

// 侧边菜单高亮当前路由
const activePath = computed(() => route.path)

// 从后台路由 meta 取当前页面标题；大屏单独给标题
const pageTitle = computed(() =>
  route.path === '/dashboard' ? '数据大屏' : (route.meta?.title || '后台管理')
)

// 各子菜单是否有至少一项可见权限（否则整组隐藏）
const anyPerm = (...codes) => codes.some((c) => hasPerm(c))
const sysVisible = computed(() => anyPerm('user:list', 'role:list', 'perm:list', 'dict:list', 'log:list'))
const creditVisible = computed(() => anyPerm('credit:list', 'repair:audit'))
const riskVisible = computed(() => hasPerm('risk:list'))
const superviseVisible = computed(() => anyPerm('sample:list', 'audit:list', 'approval:list'))

function onLogout() {
  clearToken()
  localStorage.removeItem('govai_userName')
  localStorage.removeItem('govai_role')
  clearPermissions()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #1a3a8f; }
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
}
.el-menu { border-right: none; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
}
.header-title { font-size: 17px; font-weight: 600; color: #1d3557; }
.header-user { display: flex; align-items: center; gap: 10px; }
.uname { color: #333; font-size: 14px; }
.main { background: #f5f7fa; height: calc(100vh - 60px); overflow: auto; }
</style>