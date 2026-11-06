<template>
  <el-container class="layout">
    <!-- 顶部导航 -->
    <el-header class="header" height="64px">
      <div class="header-inner">
        <!-- Logo 区 -->
        <div class="logo">
          <div class="logo-icon">
            <el-icon :size="24"><OfficeBuilding /></el-icon>
          </div>
          <div class="logo-text">
            <span class="logo-title">企业服务门户</span>
            <span class="logo-sub">GovAI One · 一网通办</span>
          </div>
        </div>

        <!-- 横向导航 -->
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          router
          popper-class="nav-popper"
          background-color="transparent"
          text-color="#ffffff"
          active-text-color="#ffffff"
          class="nav-menu"
          :ellipsis="false"
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>工作台</span>
          </el-menu-item>

          <el-sub-menu index="service">
            <template #title>
              <el-icon><Connection /></el-icon>
              <span>办事服务</span>
            </template>
            <el-menu-item index="/matters">
              <el-icon><Search /></el-icon><span>办事大厅</span>
            </el-menu-item>
            <el-menu-item index="/matters/plan">
              <el-icon><Compass /></el-icon><span>智能办事规划</span>
            </el-menu-item>
            <el-menu-item index="/packages">
              <el-icon><Box /></el-icon><span>一件事套餐</span>
            </el-menu-item>
            <el-menu-item index="/guides">
              <el-icon><Compass /></el-icon><span>一件事导引</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="record">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>办事记录</span>
            </template>
            <el-menu-item index="/applications">
              <el-icon><List /></el-icon><span>我的申报</span>
            </el-menu-item>
            <el-menu-item index="/drafts">
              <el-icon><EditPen /></el-icon><span>草稿箱</span>
            </el-menu-item>
            <el-menu-item index="/evaluations">
              <el-icon><Star /></el-icon><span>我的评价</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="enterprise">
            <template #title>
              <el-icon><OfficeBuilding /></el-icon>
              <span>企业中心</span>
            </template>
            <el-menu-item index="/licenses">
              <el-icon><Stamp /></el-icon><span>企业证照</span>
            </el-menu-item>
            <el-menu-item index="/credit">
              <el-icon><TrendCharts /></el-icon><span>信用监管</span>
            </el-menu-item>
            <el-menu-item v-if="isAdmin" index="/enterprise">
              <el-icon><InfoFilled /></el-icon><span>企业信息</span>
            </el-menu-item>
            <el-menu-item v-if="isAdmin" index="/operators">
              <el-icon><UserFilled /></el-icon><span>经办人管理</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="support">
            <template #title>
              <el-icon><ChatDotRound /></el-icon>
              <span>政策与咨询</span>
            </template>
            <el-menu-item index="/policies">
              <el-icon><Promotion /></el-icon><span>政策服务</span>
            </el-menu-item>
            <el-menu-item index="/qa">
              <el-icon><QuestionFilled /></el-icon><span>智能问答</span>
            </el-menu-item>
            <el-menu-item index="/messages">
              <el-icon><ChatDotRound /></el-icon><span>消息中心</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>

        <!-- 右侧用户区 -->
        <div class="header-right">
          <el-tag type="success" effect="dark" size="small" class="ent-tag">
            <el-icon><OfficeBuilding /></el-icon>
            {{ auth.enterpriseName }}
          </el-tag>
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-avatar">
              <div class="avatar-circle">{{ avatarText }}</div>
              <div class="user-meta">
                <span class="user-name">{{ auth.user?.userName || auth.user?.account }}</span>
                <span class="user-role">{{ auth.user?.role === 'ADMIN' ? '管理员' : '经办人' }}</span>
              </div>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon> {{ auth.user?.userName || auth.user?.account }}
                </el-dropdown-item>
                <el-dropdown-item disabled>
                  <el-icon><Stamp /></el-icon> {{ auth.enterpriseName }}
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 面包屑 -->
    <div class="breadcrumb-bar">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="breadcrumbGroup">{{ breadcrumbGroup }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 主内容 -->
    <el-main class="main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElNotification } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { entUnreadCount, listEntMessages } from '@/api'
import {
  OfficeBuilding, DataBoard, Search, Compass, Box, Connection,
  Document, List, EditPen, Stamp, InfoFilled, UserFilled,
  Promotion, ChatDotRound, QuestionFilled, User, ArrowDown, SwitchButton, Star
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 面包屑一级分组(按路径前缀归类,呼应顶部导航子能力)
const breadcrumbGroup = computed(() => {
  const p = route.path
  if (p.startsWith('/matters') || p.startsWith('/packages') || p.startsWith('/guides')) return '办事服务'
  if (p.startsWith('/applications') || p.startsWith('/drafts') || p.startsWith('/evaluations')) return '办事记录'
  if (p.startsWith('/enterprise') || p.startsWith('/operators') || p.startsWith('/licenses') || p.startsWith('/credit')) return '企业中心'
  if (p.startsWith('/policies') || p.startsWith('/qa') || p.startsWith('/messages')) return '政策与咨询'
  return ''
})

const activeMenu = computed(() => route.path)
const isAdmin = computed(() => auth.role === 'ADMIN')
const avatarText = computed(() => {
  const name = auth.user?.userName || auth.user?.account || 'U'
  return name.charAt(0).toUpperCase()
})

// ===== 办件结果实时提醒 =====
// 审批端办结/驳回/退回后会回调企业端写入站内信；这里轮询未读数，
// 一旦"未读增加"就弹通知，让企业不用手动进消息中心也能第一时间看到结果。
const POLL_MS = 20000
const lastUnread = ref<number | null>(null)
let notifyTimer: number | undefined

async function pollUnread() {
  try {
    const count = Number(await entUnreadCount()) || 0
    // 首次进入只记录基线（不把历史未读一次性弹出来）
    if (lastUnread.value !== null && count > lastUnread.value) {
      const list = await listEntMessages()
      const latest = Array.isArray(list) && list.length ? list[0] : null
      if (latest) {
        ElNotification({
          title: latest.title || '您有新的办件通知',
          message: latest.content || '',
          type: latest.title?.includes('驳回') ? 'warning' : 'success',
          duration: 8000,
          position: 'bottom-right',
          onClick: () => router.push('/messages')
        })
      }
    }
    lastUnread.value = count
  } catch {
    // 轮询失败静默忽略，不影响主流程
  }
}

onMounted(() => {
  pollUnread()
  notifyTimer = window.setInterval(pollUnread, POLL_MS)
})
onUnmounted(() => {
  if (notifyTimer) window.clearInterval(notifyTimer)
})

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗?', '提示', { type: 'warning' })
      .then(() => {
        auth.logout()
        router.push('/login')
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ===== 顶部导航栏 ===== */
.header {
  padding: 0;
  background: linear-gradient(90deg, #1a3a8f 0%, #2f6bff 100%);
  box-shadow: 0 2px 12px rgba(26, 58, 143, 0.35);
  position: relative;
  z-index: 10;
}
.header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
}
.header-inner {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 32px;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.logo-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.logo-title {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}
.logo-sub {
  color: rgba(255, 255, 255, 0.5);
  font-size: 10px;
  letter-spacing: 1.5px;
  margin-top: 2px;
}

/* 横向导航 */
.nav-menu {
  flex: 1;
  border-bottom: none !important;
  height: 64px;
}
.nav-menu :deep(.el-menu-item),
.nav-menu :deep(.el-sub-menu__title) {
  height: 64px;
  line-height: 64px;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
  padding: 0 18px !important;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff !important;
}
.nav-menu :deep(.el-menu-item .el-icon),
.nav-menu :deep(.el-sub-menu__title .el-icon) {
  color: #e2e8f0 !important;
}
.nav-menu :deep(.el-menu-item:hover),
.nav-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(47, 107, 255, 0.18) !important;
  color: #fff !important;
}
.nav-menu :deep(.el-menu-item:hover .el-icon),
.nav-menu :deep(.el-sub-menu__title:hover .el-icon) {
  color: #2f6bff !important;
}
.nav-menu :deep(.el-menu-item.is-active) {
  background: rgba(47, 107, 255, 0.15) !important;
  border-bottom-color: #2f6bff;
  color: #fff !important;
}
.nav-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #2f6bff !important;
}
/* 子菜单下拉样式 */
.nav-menu :deep(.el-menu--popup) {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 6px;
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.15);
  margin-top: 4px;
}
.nav-menu :deep(.el-menu--popup .el-menu-item) {
  height: 40px;
  line-height: 40px;
  border-radius: 6px;
  color: #1e293b !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
}
.nav-menu :deep(.el-menu--popup .el-menu-item .el-icon) {
  color: #475569 !important;
}
.nav-menu :deep(.el-menu--popup .el-menu-item:hover) {
  background: #f1f5f9 !important;
  color: #0f172a !important;
}
.nav-menu :deep(.el-menu--popup .el-menu-item:hover .el-icon) {
  color: #2f6bff !important;
}
.nav-menu :deep(.el-menu--popup .el-menu-item.is-active) {
  background: #eff6ff !important;
  color: #2f6bff !important;
}
.nav-menu :deep(.el-menu--popup .el-menu-item.is-active .el-icon) {
  color: #2f6bff !important;
}

/* 右侧用户区 */
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.ent-tag {
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
  padding: 4px 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.ent-tag .el-icon {
  color: #94a3b8;
}
.user-avatar {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 24px;
  transition: all 0.3s;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.user-avatar:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}
.avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #2f6bff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}
.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.user-name {
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 500;
}
.user-role {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
}
.arrow {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

/* ===== 面包屑 ===== */
.breadcrumb-bar {
  background: #fff;
  padding: 12px 24px;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

/* ===== 主内容 ===== */
.main {
  flex: 1;
  background: #f5f7fa;
  overflow-y: auto;
  padding: 20px 24px;
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>

<!-- 顶栏下拉子菜单弹层配色:Element Plus 将弹层渲染到 body,需非 scoped 全局样式命中 -->
<style>
.nav-popper {
  background: #ffffff !important;
  border-radius: 8px;
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.12) !important;
}
.nav-popper .el-menu-item {
  color: #303133 !important;
}
.nav-popper .el-menu-item .el-icon {
  color: #86909c !important;
}
.nav-popper .el-menu-item:hover {
  background: #eaf0ff !important;
  color: #2f6bff !important;
}
.nav-popper .el-menu-item:hover .el-icon {
  color: #2f6bff !important;
}
.nav-popper .el-menu-item.is-active {
  color: #2f6bff !important;
  background: #eaf0ff !important;
  font-weight: 600;
}
</style>
