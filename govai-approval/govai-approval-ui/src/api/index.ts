import request from '@/utils/request'

// ============ 认证 ============
export const authApi = {
  login: (data: { username: string; password: string }) =>
    request.post('/auth/login', data),
}

// ============ 审批管理 ============
export const approvalApi = {
  /** 提交审批 */
  submit: (data: any) => request.post('/approval/submit', data),

  /** 查询审批进度 */
  getProgress: (instanceId: number) => request.get(`/approval/progress/${instanceId}`),

  /** 处理审批任务 */
  handleTask: (data: any) => request.post('/approval/task/handle', data),

  /** 我的待办任务 */
  myTodo: (params: any) => request.get('/approval/task/todo', { params }),

  /** 待办统计：当前岗位各环节待处理数 */
  myTodoStats: () => request.get('/approval/task/todo-stats'),

  /** 审批实例分页查询 */
  listInstances: (params: any) => request.get('/approval/instance/list', { params }),

  /** 审批实例详情 */
  getInstance: (id: number) => request.get(`/approval/instance/${id}`),

  /** 同类办件参考(同事项历史终态) */
  similar: (id: number) => request.get(`/approval/instance/${id}/similar`),

  /** 申报材料清单（审批端复核材料用，内部调企业端） */
  instanceMaterials: (id: number) => request.get(`/approval/instance/${id}/materials`),

  /** OCR 材料识别(经企业端 PaddleOCR) */
  ocr: (body: { imageBase64: string; type?: string }) =>
    request.post('/approval/ocr/recognize', body, { timeout: 90000 }),
}

// ============ 流程模板 ============
export const processApi = {
  page: (params: any) => request.get('/process-template/list', { params }),
  detail: (id: number) => request.get(`/process-template/${id}`),
  save: (data: any) => request.post('/process-template', data),
  switchStatus: (id: number, status: number) =>
    request.post(`/process-template/${id}/status/${status}`),
}

// ============ AI能力 ============
export const aiApi = {
  generateOpinion: (instanceId: number) => request.post(`/ai/opinion/${instanceId}`, {}, { timeout: 120000 }),
  latestOpinion: (instanceId: number) => request.get(`/ai/opinion/latest/${instanceId}`),
  pageTemplates: (params: any) => request.get('/ai/prompt/list', { params }),
  saveTemplate: (data: any) => request.post('/ai/prompt', data),
  deleteTemplate: (id: number) => request.delete(`/ai/prompt/${id}`),
  ruleCheckByInstance: (instanceId: number) =>
    request.post(`/ai/rule/check-by-instance/${instanceId}`),
}

// ============ 规则引擎 ============
export const ruleApi = {
  pageSets: (params: any) => request.get('/rule/set/list', { params }),
  detail: (id: number) => request.get(`/rule/set/${id}`),
  saveSet: (data: any) => request.post('/rule/set', data),
  deleteSet: (id: number) => request.delete(`/rule/set/${id}`),
  switchStatus: (id: number, status: number) => request.post(`/rule/set/${id}/status/${status}`),
  testRule: (id: number, formData: any) => request.post(`/rule/set/${id}/test`, formData),
}

// ============ 审计日志 ============
export const auditApi = {
  page: (params: any) => request.get('/audit/list', { params }),
}
