import { createRouter, createWebHistory } from 'vue-router'
import { hasPerm } from '../utils/perm'

// 后台各管理页（懒加载）。meta.perm 为该页所需权限码；admin 内置全权限由 hasPerm 兜底
const adminRoutes = [
  { path: 'users', component: () => import('../views/system/UserMgmt.vue'), meta: { title: '用户管理', icon: 'User', perm: 'user:list' } },
  { path: 'roles', component: () => import('../views/system/RoleMgmt.vue'), meta: { title: '角色管理', icon: 'Avatar', perm: 'role:list' } },
  { path: 'permissions', component: () => import('../views/system/PermissionMgmt.vue'), meta: { title: '权限管理', icon: 'Key', perm: 'perm:list' } },
  { path: 'dicts', component: () => import('../views/system/DictMgmt.vue'), meta: { title: '数据字典', icon: 'Collection', perm: 'dict:list' } },
  { path: 'logs', component: () => import('../views/system/LogMgmt.vue'), meta: { title: '操作日志', icon: 'Document', perm: 'log:list' } },
  { path: 'credit/enterprise', component: () => import('../views/credit/EnterpriseCredit.vue'), meta: { title: '企业信用', icon: 'OfficeBuilding', perm: 'credit:list' } },
  { path: 'credit/personal', component: () => import('../views/credit/PersonalCredit.vue'), meta: { title: '个人信用', icon: 'UserFilled', perm: 'credit:list' } },
  { path: 'credit/repair', component: () => import('../views/credit/RepairMgmt.vue'), meta: { title: '信用修复', icon: 'Refresh', perm: 'repair:audit' } },
  { path: 'risk/rules', component: () => import('../views/risk/RiskRuleMgmt.vue'), meta: { title: '预警规则', icon: 'Operation', perm: 'risk:list' } },
  { path: 'risk/alerts', component: () => import('../views/risk/RiskAlertMgmt.vue'), meta: { title: '预警处置', icon: 'Warning', perm: 'risk:list' } },
  { path: 'supervise/samples', component: () => import('../views/supervise/SampleMgmt.vue'), meta: { title: '监督检查', icon: 'Finished', perm: 'sample:list' } },
  { path: 'supervise/review-orders', component: () => import('../views/supervise/ReviewOrderMgmt.vue'), meta: { title: '差评整改', icon: 'ChatDotSquare' } },
  { path: 'supervise/audit', component: () => import('../views/supervise/AuditTraceMgmt.vue'), meta: { title: '审计追溯', icon: 'Search', perm: 'audit:list' } },
  { path: 'supervise/approval-data', component: () => import('../views/supervise/ApprovalDataMgmt.vue'), meta: { title: '审批数据', icon: 'DataLine', perm: 'approval:list' } },
  { path: 'setting/profile', component: () => import('../views/setting/Profile.vue'), meta: { title: '个人中心', icon: 'Setting' } },
]

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/403', component: () => import('../views/Forbidden.vue'), meta: { title: '无权限' } },
  {
    path: '/dashboard',
    component: () => import('../views/DashboardView.vue'),
    meta: { title: '数据大屏', icon: 'DataAnalysis' },
  },
  {
    path: '/admin',
    component: () => import('../layout/Layout.vue'),
    children: [...adminRoutes],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 登录守卫 + 权限守卫：无 token 跳登录；访问无权限页跳 403
router.beforeEach((to) => {
  const token = localStorage.getItem('govai_token')
  if (to.path !== '/login' && !token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && token) {
    return { path: '/dashboard' }
  }
  if (to.meta?.perm && !hasPerm(to.meta.perm)) {
    return { path: '/403' }
  }
})

export default router
