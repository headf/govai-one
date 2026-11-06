<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { roleName } from '@/utils/roleMeta'
import {
  Menu as MenuIcon,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 侧边栏菜单（与路由 children 对应，hidden 的不显示）
const menuItems = computed(() => {
  const mainRoute = router.options.routes.find((r) => r.path === '/')
  if (!mainRoute || !mainRoute.children) return []
  return mainRoute.children.filter((r: any) => !r.meta?.hidden)
})

const activeMenu = computed(() => route.path)

const handleSelect = (index: string) => {
  router.push(index)
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon :size="24"><MenuIcon /></el-icon>
        <span>GovAI One 审批端</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        background-color="#1a3a8f"
        text-color="#cfd8dc"
        active-text-color="#ffffff"
        @select="handleSelect"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="(item as any).path"
          :index="'/' + (item as any).path"
        >
          <el-icon><component :is="(item as any).meta?.icon" /></el-icon>
          <span>{{ (item as any).meta?.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ route.meta?.title || '审批工作台' }}</span>
        </div>
        <div class="header-right">
          <span class="user-name">{{ authStore.userName || '审批员' }}（{{ roleName(authStore.role) || '审批岗位' }}）</span>
          <el-button text @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #1a3a8f;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-menu {
  border-right: none;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: #606266;
}

.main-content {
  background: #f0f2f5;
  overflow-y: auto;
}
</style>
