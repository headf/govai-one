import request from './request'

// 集中所有接口函数。request 已解包 Result（返回 data）。
// 分页参数统一用 page/size；后端返回分页信封 { list, total, page, size }

// ===== 认证 =====
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  verify: () => request.get('/auth/verify'),
}

// ===== 统计大屏 =====
export const statsApi = {
  overview: () => request.get('/stats/overview'),
  daily: (params) => request.get('/stats/daily', { params }),
  hotMatters: () => request.get('/stats/hot-matters'),
  topDuration: (size = 8) => request.get('/stats/top-duration', { params: { size } }),
}

// ===== 用户 =====
export const userApi = {
  list: (params) => request.get('/users/list', { params }),
  detail: (id) => request.get(`/users/${id}`),
  add: (data) => request.post('/users', data),
  update: (id, data) => request.put(`/users/${id}`, data),
  remove: (id) => request.delete(`/users/${id}`),
  resetPassword: (id, newPassword) => request.post(`/users/${id}/reset-password`, null, { params: { newPassword } }),
}

// ===== 角色 =====
export const roleApi = {
  list: (params) => request.get('/roles/list', { params }),
  detail: (id) => request.get(`/roles/${id}`),
  add: (data) => request.post('/roles', data),
  update: (id, data) => request.put(`/roles/${id}`, data),
  remove: (id) => request.delete(`/roles/${id}`),
}

// ===== 权限 =====
export const permissionApi = {
  tree: () => request.get('/permissions/tree'),
  byRole: (roleId) => request.get(`/permissions/role/${roleId}`),
  grant: (data) => request.post('/permissions/grant', data),
  add: (data) => request.post('/permissions', data),
  update: (id, data) => request.put(`/permissions/${id}`, data),
  remove: (id) => request.delete(`/permissions/${id}`),
}

// ===== 信用 =====
export const creditEnterpriseApi = {
  list: (params) => request.get('/credit/enterprise/list', { params }),
  score: (data) => request.post('/credit/enterprise/score', data),
  adjust: (params) => request.put('/credit/enterprise/adjust', null, { params }),
  remove: (enterpriseId) => request.delete(`/credit/enterprise/${enterpriseId}`, { params: { enterpriseId } }),
  members: (enterpriseId) => request.get('/credit/enterprise/members', { params: { enterpriseId } }),
  allEnterpriseUsers: () => request.get('/credit/enterprise/users/all-enterprise'),
  disableEnterpriseUser: (id) => request.post(`/credit/enterprise/users/${id}/disable`),
  enableEnterpriseUser: (id) => request.post(`/credit/enterprise/users/${id}/enable`),
}
export const creditPersonalApi = {
  list: (params) => request.get('/credit/personal/list', { params }),
  score: (data) => request.post('/credit/personal/score', data),
  remove: (userId) => request.delete(`/credit/personal/${userId}`, { params: { userId } }),
}
export const creditRepairApi = {
  list: (params) => request.get('/credit/repair/list', { params }),
  apply: (data) => request.post('/credit/repair/apply', data),
  audit: (data) => request.post('/credit/repair/audit', data),
}

// ===== 风险 =====
export const riskRuleApi = {
  list: (params) => request.get('/risk/rules/list', { params }),
  add: (data) => request.post('/risk/rules', data),
  update: (id, data) => request.put(`/risk/rules/${id}`, data),
  remove: (id) => request.delete(`/risk/rules/${id}`),
}
export const riskAlertApi = {
  list: (params) => request.get('/risk/alerts/list', { params }),
  stats: () => request.get('/risk/alerts/stats'),
  handle: (id, opinion) => request.post(`/risk/alerts/${id}/handle`, null, { params: { opinion } }),
  dispatch: (id) => request.post(`/risk/alerts/${id}/dispatch`),
}

// ===== 差评整改工单（好差评闭环：群众端/企业端上报 → 监管端处置） =====
export const reviewOrderApi = {
  list: (params) => request.get('/review-orders/list', { params }),
  pendingCount: () => request.get('/review-orders/pending-count'),
  handle: (id, opinion) => request.post(`/review-orders/${id}/handle`, null, { params: { opinion } }),
}

// ===== 审计 / 抽样 =====
export const sampleApi = {
  list: (params) => request.get('/samples/list', { params }),
  random: (count, creditWeighted) => request.post('/samples/random', null, { params: { count, creditWeighted: !!creditWeighted } }),
  targeted: (data) => request.post('/samples/targeted', data),
}
export const auditApi = {
  list: (params) => request.get('/audit/traces/list', { params }),
  chain: (instanceId) => request.get(`/audit/traces/chain/${instanceId}`),
}

// ===== 数据字典 =====
export const dictApi = {
  group: () => request.get('/dicts/group'),
  list: (params) => request.get('/dicts/list', { params }),
  byType: (dictType) => request.get(`/dicts/type/${dictType}`),
  add: (data) => request.post('/dicts', data),
  update: (id, data) => request.put(`/dicts/${id}`, data),
  remove: (id) => request.delete(`/dicts/${id}`),
}

// ===== 操作日志 =====
export const logApi = {
  list: (params) => request.get('/logs/list', { params }),
  clear: () => request.delete('/logs'),
}

// ===== 个人中心 =====
export const profileApi = {
  get: () => request.get('/users/profile'),
  update: (data) => request.put('/users/profile', data),
  changePassword: (oldPassword, newPassword) =>
    request.put('/users/profile/password', null, { params: { oldPassword, newPassword } }),
}

// ===== 审批数据（真实流程实时推送，只读） =====
export const approvalCacheApi = {
  list: (params) => request.get('/approval-cache/list', { params }),
}