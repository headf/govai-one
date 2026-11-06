/** 后端统一分页结构 */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 登录响应 */
export interface LoginResp {
  token: string
  userId: number
  userName: string
  account: string
  role: string
  enterpriseId: number
  enterpriseName: string
  expireIn: number
}

/** 动态表单字段 */
export interface FormField {
  key: string
  label: string
  type: string
  required?: boolean
  options?: string[]
  ocr?: string
}

/** 法人事项 */
export interface MatterVO {
  id: number
  matterCode: string
  matterName: string
  categoryId?: number
  categoryName?: string
  department?: string
  description?: string
  guideContent?: string
  conditions?: string[]
  materials?: string[]
  processSteps?: string[]
  deadlineDays?: number
  formConfig?: FormField[]
}

/** 事项分类 */
export interface Category {
  id: number
  categoryName: string
  parentId: number
  sortOrder: number
}

/** 企业电子证照 */
export interface License {
  id: number
  enterpriseId: number
  licenseType: string
  /** 证照类别 ENTERPRISE企业/PERSONAL个人 */
  category?: string
  licenseNo: string
  licenseName: string
  issueDate?: string
  expiryDate?: string
  fileUrl?: string
  status: number
  /** 作废原因 RENEW_EXTEND延续换发/RENEW_CHANGE变更换发/CANCEL_SELF自行注销/CANCEL_ENT企业注销 */
  voidReason?: string
}

/** 申报记录 */
export interface ApplyRecordVO {
  id: number
  recordCode: string
  matterId: number
  matterName: string
  matterCode: string
  status: string
  approvalInstanceId?: number
  formData?: Record<string, any>
  /** 审批端退回/补正意见(REPAIR/REJECTED 时有值) */
  remark?: string
  submitTime?: string
  finishTime?: string
  createTime?: string
}

/** 申报详情 */
export interface ApplyDetailVO {
  record: ApplyRecordVO
  materials: Material[]
  checks: ComplianceCheck[]
  progress?: ApprovalProgress
  subsidies?: SubsidyPayment[]
}

/** 补贴/减免发放记录(补贴类事项办结到账) */
export interface SubsidyPayment {
  id: number
  subsidyType: string
  amount: number | string
  payTime?: string
  status?: string
  bankName?: string
  bankAccount?: string
}

/** 申报材料 */
export interface Material {
  id: number
  recordId: number
  materialName: string
  fileUrl: string
  fileType?: string
  ocrStatus?: number
  uploadTime?: string
}

/** 合规预检项 */
export interface ComplianceCheck {
  id: number
  recordId: number
  checkItem: string
  checkResult: number
  checkDetail?: string
  checkTime?: string
}

/** 审批进度 */
export interface ApprovalProgress {
  instanceId: number
  status: string
  settleMode?: string
  currentNode?: string
  submitTime?: string
  nodes: ProgressNode[]
}

/** 进度环节(与审批端 ProgressVO.Node 对齐) */
export interface ProgressNode {
  name: string
  status: string
  /** 环节岗位码 APPROVER/DECIDE/ISSUE */
  assigneeRole?: string
  assignee?: string
  handleTime?: string
  /** 审批结果 PASS/REJECT/RETURN */
  approveResult?: string
  /** 审批意见 */
  approveOpinion?: string
  /** 办理附加信息(JSON):办结出证含证照号 licenseNo/送达方式 */
  handleDetail?: string
}

/** 政策匹配 */
export interface PolicyMatch {
  id: number
  enterpriseId: number
  policyTitle: string
  policyContent: string
  matchScore?: number
  isApplied: number
  applyTime?: string
  createTime?: string
}

/** 合规预检报告 */
export interface PreCheckResp {
  passed: boolean
  passCount: number
  failCount: number
  items: PreCheckItem[]
}

/** 预检项 */
export interface PreCheckItem {
  checkItem: string
  result: boolean
  detail: string
}

/** OCR 识别响应 */
export interface OcrRecognizeResp {
  type: string
  fields: Record<string, any>
  confidence: number
  lines: { text: string; confidence: number }[]
}

/** 好差评 */
export interface Evaluation {
  id: number
  recordId: number
  recordCode?: string
  matterName?: string
  starRating: number
  content?: string
  tags?: string[]
  createTime?: string
}

/** 套餐事项明细 */
export interface PackageItem {
  matterId: number
  matterCode: string
  matterName: string
  department?: string
  deadlineDays?: number
  formConfig?: FormField[]
  materials?: string[]
}

/** 一件事套餐 */
export interface PackageVO {
  id: number
  packageCode: string
  packageName: string
  description?: string
  items?: PackageItem[]
  mergedForm?: FormField[]
  mergedMaterials?: string[]
  processSteps?: string[]
  totalDeadlineDays?: number
}

/** 套餐申报拆分结果单项 */
export interface PackageSubmitItem {
  recordId: number
  recordCode: string
  matterId: number
  matterName: string
  status: string
}

/** 套餐申报结果 */
export interface PackageSubmitResp {
  records: PackageSubmitItem[]
}

/** 问答引用来源 */
export interface QaSource {
  type: string
  title: string
  snippet?: string
}

/** 问答关联事项 */
export interface QaRelatedMatter {
  matterId: number
  matterName: string
  matterCode: string
}

/** 问答响应 */
export interface QaAskResp {
  answer: string
  sources: QaSource[]
  relatedMatters: QaRelatedMatter[]
  suggestedQuestions: string[]
}

/** 问答历史记录 */
export interface QaHistoryItem {
  id: number
  question: string
  answer: string
  createTime?: string
}

/** 问答会话(历史会话列表项) */
export interface QaSession {
  id: number
  title: string
  messageCount: number
  lastTime?: string
  createTime?: string
}

/** 会话消息(含引用来源,用于恢复历史对话窗口) */
export interface QaSessionMessage {
  id: number
  question: string
  answer: string
  sources?: QaSource[]
  createTime?: string
}

/** 历史已上传材料(可复用) */
export interface HistoryMaterial {
  materialId: number
  materialName: string
  fileUrl: string
  uploadTime?: string
}

/** 可复用证照/材料 */
export interface ReusableMaterialVO {
  licenses: License[]
  history: HistoryMaterial[]
}

/** 智能办事规划 - 单个事项 */
export interface MatterPlanItem {
  matterId: number
  matterName: string
  department?: string
  order: number
  reason?: string
  guide?: string
  materials?: string[]
  process?: string[]
  deadlineDays?: number
  prerequisite?: boolean
  status?: 'RECOMMENDED' | 'OPTIONAL' | 'SKIPPED'
}

/** 智能办事规划结果 */
export interface MatterPlanResp {
  title: string
  summary: string
  items: MatterPlanItem[]
  tips: string[]
  historyId?: number
  completed?: boolean
}

/** 规划历史记录 */
export interface PlanHistory {
  id: number
  enterpriseId: number
  userId: number
  description: string
  hasLicense: number
  title: string
  summary: string
  planJson: string
  createTime: string
}

/** 智能办事规划请求 */
export interface MatterPlanReq {
  description?: string
  businessType?: string
  hasLicense?: boolean
}

/* ===== 信用监管(监管端 ent-view 下发) ===== */

/** 本企业信用档案 */
export interface CreditProfile {
  id?: number
  enterpriseId: number
  creditScore: number | null
  creditLevel: string | null
  dimensionScores?: string | null
  updateTime?: string | null
}

/** 风险预警记录 */
export interface CreditAlert {
  id: number
  ruleId?: number | null
  targetType?: string
  targetId?: number
  alertLevel: string
  alertContent: string
  alertTime: string
  handleStatus: string
  handleOpinion?: string | null
  handleTime?: string | null
}

/** 信用修复申请记录 */
export interface CreditRepairRecord {
  id: number
  targetType: string
  targetId: number
  repairReason: string
  applyTime: string
  auditStatus: string
  auditOpinion?: string | null
  auditTime?: string | null
}

/** 企业站内消息 */
export interface EntMessage {
  id: number
  msgType: string
  title: string
  content: string
  isRead: number
  createTime: string
}
