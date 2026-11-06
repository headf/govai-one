import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '企业登录', public: true }
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '企业工作台' }
        },
        {
          path: 'matters',
          name: 'matters',
          component: () => import('@/views/MatterListView.vue'),
          meta: { title: '法人办事大厅' }
        },
        {
          path: 'matters/plan',
          name: 'matter-plan',
          component: () => import('@/views/MatterPlanView.vue'),
          meta: { title: '智能办事规划' }
        },
        {
          path: 'matters/:id',
          name: 'matter-detail',
          component: () => import('@/views/MatterDetailView.vue'),
          meta: { title: '事项详情' }
        },
        {
          path: 'matters/:id/apply',
          name: 'matter-apply',
          component: () => import('@/views/ApplyView.vue'),
          meta: { title: '在线申报' }
        },
        {
          path: 'packages',
          name: 'packages',
          component: () => import('@/views/PackageListView.vue'),
          meta: { title: '一件事套餐' }
        },
        {
          path: 'guides',
          name: 'guides',
          component: () => import('@/views/EntGuideView.vue'),
          meta: { title: '一件事导引' }
        },
        {
          path: 'packages/:id/apply',
          name: 'package-apply',
          component: () => import('@/views/PackageApplyView.vue'),
          meta: { title: '一表申报' }
        },
        {
          path: 'applications',
          name: 'applications',
          component: () => import('@/views/ApplicationListView.vue'),
          meta: { title: '我的申报' }
        },
        {
          path: 'applications/:id',
          name: 'application-detail',
          component: () => import('@/views/ApplicationDetailView.vue'),
          meta: { title: '申报详情' }
        },
        {
          path: 'drafts',
          name: 'drafts',
          component: () => import('@/views/DraftListView.vue'),
          meta: { title: '草稿箱' }
        },
        {
          path: 'evaluations',
          name: 'evaluations',
          component: () => import('@/views/MyEvaluationsView.vue'),
          meta: { title: '我的评价' }
        },
        {
          path: 'enterprise',
          name: 'enterprise',
          component: () => import('@/views/EnterpriseInfoView.vue'),
          meta: { title: '企业信息', requireAdmin: true }
        },
        {
          path: 'operators',
          name: 'operators',
          component: () => import('@/views/OperatorManageView.vue'),
          meta: { title: '经办人管理', requireAdmin: true }
        },
        {
          path: 'licenses',
          name: 'licenses',
          component: () => import('@/views/LicenseListView.vue'),
          meta: { title: '企业证照' }
        },
        {
          path: 'credit',
          name: 'credit',
          component: () => import('@/views/CreditMonitorView.vue'),
          meta: { title: '信用监管' }
        },
        {
          path: 'policies',
          name: 'policies',
          component: () => import('@/views/PolicyListView.vue'),
          meta: { title: '政策服务' }
        },
        {
          path: 'qa',
          name: 'qa',
          component: () => import('@/views/QaView.vue'),
          meta: { title: '智能问答' }
        },
        {
          path: 'messages',
          name: 'messages',
          component: () => import('@/views/EntMessageView.vue'),
          meta: { title: '消息中心' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

// 路由守卫:需登录页面校验 token
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 仅法人/管理员可访问的页面:经办人重定向到工作台
  if (to.meta.requireAdmin && auth.role !== 'ADMIN') {
    return { path: '/dashboard' }
  }
  if (to.meta.title) {
    document.title = `${to.meta.title} - GovAI企业服务`
  }
  return true
})

export default router
