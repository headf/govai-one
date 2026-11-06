import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/approval/todo',
      children: [
        {
          path: 'approval/todo',
          name: 'approval-todo',
          component: () => import('@/views/approval/TodoView.vue'),
          meta: { title: '待办列表', icon: 'List' },
        },
        {
          path: 'approval/detail/:id',
          name: 'approval-detail',
          component: () => import('@/views/approval/DetailView.vue'),
          meta: { title: '审批详情', hidden: true },
        },
        {
          path: 'approval/history',
          name: 'approval-history',
          component: () => import('@/views/approval/HistoryView.vue'),
          meta: { title: '审批历史', icon: 'Clock' },
        },
        {
          path: 'process/list',
          name: 'process-list',
          component: () => import('@/views/process/ProcessListView.vue'),
          meta: { title: '流程模板', icon: 'Files' },
        },
        {
          path: 'process/designer/:id?',
          name: 'process-designer',
          component: () => import('@/views/process/ProcessDesignerView.vue'),
          meta: { title: '流程设计器', hidden: true },
        },
        {
          path: 'rule/list',
          name: 'rule-list',
          component: () => import('@/views/rule/RuleListView.vue'),
          meta: { title: '规则集管理', icon: 'SetUp' },
        },
        {
          path: 'rule/editor/:id?',
          name: 'rule-editor',
          component: () => import('@/views/rule/RuleEditorView.vue'),
          meta: { title: '规则编辑器', hidden: true },
        },
        {
          path: 'ai/prompts',
          name: 'ai-prompt-list',
          component: () => import('@/views/ai/PromptListView.vue'),
          meta: { title: 'Prompt模板', icon: 'ChatDotRound' },
        },
        {
          path: 'audit/logs',
          name: 'audit-logs',
          component: () => import('@/views/audit/AuditLogView.vue'),
          meta: { title: '审计日志', icon: 'Document' },
        },
      ],
    },
  ],
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
