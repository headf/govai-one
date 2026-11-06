import http from '@/utils/request'
import type {
  LoginResp,
  MatterVO,
  Category,
  License,
  PageResult,
  ApplyRecordVO,
  ApplyDetailVO,
  Material,
  PolicyMatch,
  PreCheckResp,
  OcrRecognizeResp,
  Evaluation,
  PackageVO,
  PackageSubmitResp,
  QaAskResp,
  QaSession,
  QaSessionMessage,
  ReusableMaterialVO,
  EntMessage,
  MatterPlanResp,
  MatterPlanReq,
  PlanHistory,
  CreditProfile,
  CreditAlert,
  CreditRepairRecord
} from '@/types'

/** 登录 */
export function login(data: { username: string; password: string }) {
  return http.post<LoginResp>('/ent-auth/login', data)
}

/** 当前用户 */
export function getMe() {
  return http.get<LoginResp>('/sys/me')
}

/** 事项分页检索 */
export function listMatters(params: { keyword?: string; categoryId?: number; page?: number; size?: number }) {
  return http.get<PageResult<MatterVO>>('/ent-matters/list', { params })
}

/** 事项详情 */
export function getMatter(id: number) {
  return http.get<MatterVO>(`/ent-matters/${id}`)
}

/** 按事项编码查事项(用于兑现/延续/变更等跨页跳转,避免硬编码 id) */
export function getMatterByCode(code: string) {
  return http.get<MatterVO>('/ent-matters/by-code', { params: { code } })
}

/** 事项分类 */
export function listCategories() {
  return http.get<Category[]>('/ent-matters/categories')
}

/** 创建申报草稿(传 recordId 则更新既有草稿) */
export function createDraft(data: { matterId: number; formData: Record<string, any>; recordId?: number }) {
  return http.post<{ recordId: number; recordCode: string; status: string }>('/ent-apply/draft', data)
}

/** 上传材料 */
export function uploadMaterial(recordId: number, materialName: string, file: File) {
  const fd = new FormData()
  fd.append('materialName', materialName)
  fd.append('file', file)
  return http.post<Material>(`/ent-apply/${recordId}/material`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 可复用的证照与历史材料 */
export function listReusableMaterials() {
  return http.get<ReusableMaterialVO>('/ent-apply/reusable-materials')
}

/** 复用已有材料(证照/历史材料) */
export function reuseMaterial(recordId: number, materialName: string, fileUrl: string) {
  return http.post<Material>(`/ent-apply/${recordId}/material/reuse`, { materialName, fileUrl })
}

/** 免证办匹配证照 */
export function matchLicenses(matterId: number) {
  return http.get<License[]>('/ent-apply/licenses/match', { params: { matterId } })
}

/** 表单预填:从企业信息库带出基础字段(已有营业执照等证照时免重复填表/拍照) */
export function prefillEnterprise() {
  return http.get<Record<string, string>>('/ent-apply/prefill')
}

/** 企业信息(含法定代表人身份证号) */
export interface EnterpriseInfo {
  id: number
  creditCode: string
  enterpriseName: string
  legalPerson: string
  legalPersonIdCard: string | null
  registerAddress: string
  contactPhone: string
  enterpriseType: string | null
  establishDate: string | null
}

/** 查询我的企业信息 */
export function getEnterpriseInfo() {
  return http.get<EnterpriseInfo>('/ent-enterprise/info')
}

/** 修改法定代表人信息 */
export function updateLegalPerson(data: { legalPerson: string; legalPersonIdCard: string }) {
  return http.put<void>('/ent-enterprise/legal-person', data)
}

/* ===== 信用监管(转发到监管端 8081 ent-view,带企业ID头) ===== */

function creditHeaders(enterpriseId: number) {
  return { headers: { 'X-Enterprise-Id': String(enterpriseId) } }
}

/** 本企业信用档案 */
export function getMyCredit(enterpriseId: number) {
  return http.get<CreditProfile | null>('/ent-view/credit', creditHeaders(enterpriseId))
}

/** 本企业风险预警 */
export function getMyAlerts(enterpriseId: number, params?: { page?: number; size?: number; handleStatus?: string }) {
  return http.get<PageResult<CreditAlert>>('/ent-view/alerts', { ...creditHeaders(enterpriseId), params })
}

/** 本企业信用修复记录 */
export function getMyRepairs(enterpriseId: number, params?: { page?: number; size?: number }) {
  return http.get<PageResult<CreditRepairRecord>>('/ent-view/repairs', { ...creditHeaders(enterpriseId), params })
}

/** 企业用户(法人/经办人) */
export interface EnterpriseUser {
  id: number
  username: string
  realName: string
  phone: string
  role: string
  status: number
  createTime?: string
}

/** 本企业用户列表(法人+经办人) */
export function listOperators() {
  return http.get<EnterpriseUser[]>('/ent-enterprise/users')
}

/** 新增经办人 */
export function addOperator(data: { username: string; password: string; realName: string; phone: string }) {
  return http.post<void>('/ent-enterprise/users', data)
}

/** 禁用经办人 */
export function disableOperator(id: number) {
  return http.put<void>(`/ent-enterprise/users/${id}/disable`)
}

/** 我的企业证照列表 */
export function listLicenses() {
  return http.get<License[]>('/ent-license/list')
}

/** 提交申报 */
export function submitApply(recordId: number) {
  return http.post<{ recordId: number; recordCode: string; status: string; approvalInstanceId: number }>(
    `/ent-apply/${recordId}/submit`
  )
}

/** 合规预检(提交前) */
export function preCheck(recordId: number) {
  return http.post<PreCheckResp>(`/ent-apply/${recordId}/precheck`)
}

/** 我的申报列表 */
export function listMyApplies(params: { page?: number; size?: number; keyword?: string }) {
  return http.get<PageResult<ApplyRecordVO>>('/ent-apply/list', { params })
}

/** 申报详情 */
export function getApplyDetail(recordId: number) {
  return http.get<ApplyDetailVO>(`/ent-apply/${recordId}`)
}

/** 政策匹配列表 */
export function listPolicies() {
  return http.get<PolicyMatch[]>('/ent-policy/list')
}

/** 政策确认(免申即享) */
export function confirmPolicy(id: number) {
  return http.post<PolicyMatch>(`/ent-policy/${id}/confirm`)
}

/** 企业站内消息列表 */
export function listEntMessages(type?: string) {
  return http.get<EntMessage[]>('/ent-message/list', { params: type ? { type } : {} })
}

/** 企业消息未读数 */
export function entUnreadCount() {
  return http.get<number>('/ent-message/unread-count')
}

/** 标记已读 */
export function markEntMessageRead(id: number) {
  return http.post<void>(`/ent-message/${id}/read`)
}

/** 全部已读 */
export function readAllEntMessages() {
  return http.post<void>('/ent-message/read-all')
}

/** OCR 识别(契约接口8,企业端提供) */
export function ocrRecognize(data: { imageBase64: string; type: string }) {
  return http.post<OcrRecognizeResp>('/ocr/recognize', data)
}

/** 证照到期提醒 */
export function listExpiringLicenses(days = 30) {
  return http.get<License[]>('/ent-license/expiring', { params: { days } })
}

/** 提交好差评 */
export function submitEvaluation(data: { recordId: number; starRating: number; content?: string; tags?: string[] }) {
  return http.post<Evaluation>('/ent-eval', data)
}

/** 查询某申报的评价 */
export function getEvaluation(recordId: number) {
  return http.get<Evaluation | null>(`/ent-eval/record/${recordId}`)
}

/** 我的评价列表 */
export function listMyEvaluations(params: { page?: number; size?: number }) {
  return http.get<PageResult<Evaluation>>('/ent-eval/list', { params })
}

/** 草稿箱列表 */
export function listMyDrafts(params: { page?: number; size?: number; keyword?: string }) {
  return http.get<PageResult<ApplyRecordVO>>('/ent-apply/drafts', { params })
}

/** 删除草稿 */
export function deleteDraft(recordId: number) {
  return http.delete<void>(`/ent-apply/draft/${recordId}`)
}

/** 一件事套餐列表 */
export function listPackages() {
  return http.get<PackageVO[]>('/ent-package/list')
}

/** 套餐详情(合并表单/材料) */
export function getPackage(id: number) {
  return http.get<PackageVO>(`/ent-package/${id}`)
}

/** 套餐一表申报提交 */
export function submitPackage(packageId: number, formData: Record<string, any>, materials?: { materialName: string; fileUrl: string }[]) {
  return http.post<PackageSubmitResp>(`/ent-package/${packageId}/submit`, { formData, materials })
}

/** 套餐材料上传:存文件返回 fileUrl,提交时再关联到拆分出的子申报 */
export function uploadPackageMaterial(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return http.post<{ materialName: string; fileUrl: string }>('/ent-package/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 智能问答(带 sessionId 归入会话;不传后端自动新建会话) */
export function qaAsk(question: string, sessionId?: number) {
  return http.post<QaAskResp>('/ent-qa/ask', { question, sessionId })
}

/** 推荐问题 */
export function qaSuggestions() {
  return http.get<string[]>('/ent-qa/suggestions')
}

/** 新建会话(返回会话ID) */
export function createQaSession() {
  return http.post<QaSession>('/ent-qa/session')
}

/** 会话分页列表 */
export function listQaSessions(params: { page?: number; size?: number }) {
  return http.get<PageResult<QaSession>>('/ent-qa/sessions', { params })
}

/** 加载某会话全部消息 */
export function getQaSession(sessionId: number) {
  return http.get<QaSessionMessage[]>(`/ent-qa/session/${sessionId}`)
}

/** 删除某会话 */
export function deleteQaSession(sessionId: number) {
  return http.delete<void>(`/ent-qa/session/${sessionId}`)
}

/** 清空问答历史(清空全部会话与记录) */
export function clearQaHistory() {
  return http.delete<void>('/ent-qa/history')
}

/** 智能办事规划:根据业务描述生成所需事项清单(LLM 耗时较长,单独放宽超时至 2 分钟) */
export function planMatters(req: MatterPlanReq) {
  return http.post<MatterPlanResp>('/ent-matters/plan', req || {}, { timeout: 120000 })
}

// 智能办事规划历史(记忆功能)
export function listPlanHistory() {
  return http.get<PlanHistory[]>('/ent-matters/plan/history')
}
export function getPlanHistory(id: number) {
  return http.get<MatterPlanResp>(`/ent-matters/plan/history/${id}`)
}
export function completePlan(id: number) {
  return http.put<void>(`/ent-matters/plan/history/${id}/complete`)
}
export function deletePlan(id: number) {
  return http.delete<void>(`/ent-matters/plan/history/${id}`)
}
