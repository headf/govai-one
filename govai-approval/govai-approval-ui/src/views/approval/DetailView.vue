<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { QuestionFilled, CircleCheck, Document } from '@element-plus/icons-vue'
import { approvalApi, aiApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { roleActions, roleName, ISSUE_DELIVERY_OPTIONS, matterSettleKind, ISSUE_PASS_LABEL, type SettleKind } from '@/utils/roleMeta'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const instance = ref<any>(null)
const simMatter = ref('')
const simTotal = ref(0)
const simPassed = ref(0)
const simRejected = ref(0)
const simReturned = ref(0)
const simList = ref<any[]>([])
const ocrLoading = ref(false)
const ocrErr = ref('')
const ocrFields = ref<Record<string, string>>({})
const progress = ref<any>(null)
const taskId = ref<number | null>(null)
const aiOpinion = ref('')
const aiLoading = ref(false)
const aiGenTime = ref('')
const ruleResult = ref<any>(null)
const ruleLoading = ref(false)

// 申报材料（申报人上传，审批端复核用）与"已核对"确认
const materials = ref<any[]>([])
const materialChecked = ref(false)

// ===== AI 意见结构化解析:骨架(申报事项/申请人/规则校验结论/建议决定/AI核查要点)是后端固定拼装的,逐段抽出再分段渲染,避免一整段堆叠 =====
const opinionSections = computed(() => {
  const t = aiOpinion.value || ''
  if (!t) return null
  const labels = ['申报事项：', '申请人：', '规则校验结论：', '建议决定：', 'AI 核查要点：']
  const sec = (label: string) => {
    const i = t.indexOf(label)
    if (i < 0) return ''
    let end = -1
    for (const l of labels) {
      const j = t.indexOf(l, i + label.length)
      if (j >= 0 && (end < 0 || j < end)) end = j
    }
    return (end < 0 ? t.slice(i + label.length) : t.slice(i + label.length, end)).trim()
  }
  const conclusion = sec('规则校验结论：')
  return {
    matterName: sec('申报事项：'),
    applicantName: sec('申请人：'),
    conclusion,
    decision: sec('建议决定：'),
    // 无骨架的旧/mock意见整段作正文兜底展示
    risk: sec('AI 核查要点：') || (!conclusion ? t : ''),
  }
})

// 规则结论主词(通过/未通过/转人工),去掉括号内解释后做徽标与配色
function conclusionWord(conclusion: string): string {
  const w = (conclusion || '').split('（')[0].replace(/^[:：]/, '').trim()
  return w || '未执行'
}
function conclusionTag(w: string): string {
  if (w.includes('未通过')) return 'danger'
  if (w.includes('转人工')) return 'warning'
  if (w.includes('通过')) return 'success'
  return 'info'
}
// AI 核查要点正文:按"风险提示"拆成小段,并清掉与骨架重复的"核查要点:/风险提示:"行首小标签
const riskParagraphs = computed(() => {
  const r = opinionSections.value?.risk || ''
  if (!r) return []
  // 模型正文逐行输出,前端按行拆成独立条目;识别"风险提示"小标签给红字
  const items: { risk: boolean; text: string }[] = []
  let risk = false
  for (const raw of r.split(/\n+/)) {
    const seg = raw.trim()
    if (!seg) continue
    const m = seg.match(/^(风险提示|核查要点)[:：]/)
    if (m) {
      risk = m[1] === '风险提示'
      const t = seg.replace(/^(风险提示|核查要点)[:：]/, '').trim()
      if (t) items.push({ risk, text: t })
    } else {
      items.push({ risk, text: seg })
    }
  }
  return items
})

// 意见正文折叠:默认只展示"建议决定 + 首条核查要点 + 全部风险提示",其余要点点开才显示,压缩纵向便于快速决策
const showOpinionFull = ref(false)
function toggleOpinionFull() {
  showOpinionFull.value = !showOpinionFull.value
}
const opinionViewItems = computed(() => {
  const all = riskParagraphs.value
  if (showOpinionFull.value || all.length <= 1) return all
  return all.filter((p, i) => i === 0 || p.risk)
})
const opinionHiddenCount = computed(() => {
  if (showOpinionFull.value) return 0
  return Math.max(0, riskParagraphs.value.length - opinionViewItems.value.length)
})

// 规则引擎自动审核横幅:结果码 -> el-alert 类型/标题/说明
function ruleAlertType(result: string): string {
  if (result === 'PASS') return 'success'
  if (result === 'TO_MANUAL') return 'warning'
  if (result === 'FAIL') return 'error'
  return 'info'
}
function ruleAlertTitle(result: string): string {
  if (result === 'PASS') return '自动审核通过'
  if (result === 'TO_MANUAL') return '转人工审核'
  if (result === 'FAIL') return '自动审核未通过'
  return result || '审核结果未知'
}
function ruleAlertDesc(result: string): string {
  if (result === 'PASS') return '规则项全部通过，建议由办结岗复核后办结'
  if (result === 'TO_MANUAL') return '未配置规则集或需人工判定，转受理初审岗人工审核'
  if (result === 'FAIL') return '存在未通过规则项，转受理初审岗复核后决定'
  return ''
}

// 审批操作弹窗
const handleFormRef = ref<FormInstance>()
const handleDialog = reactive({
  visible: false,
  result: 'PASS' as string,
  opinion: '',
  adoptAiAdvice: false,
  // 办结出证附加信息(证照号由企业端自动签发后回填,办结岗只需选送达方式)
  deliveryType: '窗口自取',
})

// 审批表单验证规则：拒绝/退回时意见必填
const handleRules = computed<FormRules>(() => ({
  opinion:
    handleDialog.result !== 'PASS'
      ? [{ required: true, message: '拒绝或退回时请填写审批意见', trigger: 'blur' }]
      : [],
}))

const instanceStatusTag: Record<string, string> = {
  PENDING: 'warning',
  APPROVING: 'primary',
  APPROVED: 'success',
  REJECTED: 'danger',
  RETURNED: 'info',
}

const instanceStatusLabel: Record<string, string> = {
  PENDING: '待受理',
  APPROVING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  RETURNED: '已退回',
}

// 企业信用等级 → el-tag 颜色(守信绿/失信红,未知灰)
function creditTagType(level: string): string {
  if (level === 'A' || level === 'B') return 'success'
  if (level === 'C') return 'warning'
  if (level === 'D') return 'danger'
  return 'info'
}
function creditText(level: string): string {
  return level || '信用未知'
}

// 审批结果中英文映射
const approveResultLabel: Record<string, string> = {
  PASS: '通过',
  REJECT: '拒绝',
  RETURN: '退回补正',
}

// 节点状态对应的 el-step 状态
const nodeStepStatus = (status: string) => {
  if (status === 'DONE') return 'success'
  if (status === 'CURRENT') return 'process'
  if (status === 'CANCEL') return 'error'
  return 'wait'
}

// 规则类型与操作符中英文映射
const ruleTypeLabel: Record<string, string> = {
  NUMBER: '数值',
  TEXT: '文本',
  DATE: '日期',
  LICENSE: '证照',
}

// el-steps 的 active 索引：取最后一个 DONE 或 CURRENT 节点的索引
const activeStepIndex = computed(() => {
  const nodes = progress.value?.nodes || []
  let idx = -1
  for (let i = 0; i < nodes.length; i++) {
    if (nodes[i].status === 'DONE' || nodes[i].status === 'CURRENT') idx = i
  }
  return idx
})

// 审批记录：仅展示有实际办理记录（办理人或办理时间或审批结果）的环节
const recordNodes = computed(() => {
  return (progress.value?.nodes || []).filter(
    (n: any) => n.assignee || n.handleTime || n.approveResult,
  )
})

// 当前登录人可操作的环节节点（progress 接口对企业端匿名可查、无登录上下文，故前端按"节点岗位==登录人岗位"自行判定）
const operableNode = computed(() => {
  const myRole = authStore.role
  return (progress.value?.nodes || []).find(
    (n: any) =>
      n.taskId &&
      (n.status === 'CURRENT' || n.status === 'PENDING') &&
      n.assigneeRole === myRole,
  )
})
// 当前操作环节所属岗位码
const operableRole = computed(() => operableNode.value?.assigneeRole || authStore.role)
// 该岗位在当前环节可执行的动作按钮组
const actionButtons = computed(() => roleActions(operableRole.value))
// 事项办结形态(按申报事项名识别: 发证/注销/无送达),决定办结岗按钮与送达方式
const settleKind = computed<SettleKind>(() => matterSettleKind(instance.value?.matterName))
// 是否办结岗 PASS(出证/注销/确认办结)动作
const isIssuePass = computed(() => operableRole.value === 'ISSUE' && handleDialog.result === 'PASS')
// 办结是否需要记录送达方式: 发证类可选(窗口/邮寄/电子)、注销类固定电子送达、无送达类不记录
const settleNeedDelivery = computed(() => isIssuePass.value && settleKind.value !== 'NO_DELIVERY')
// 是否即办件·系统自动办结(无人工待办,只读展示)
const isAutoSettled = computed(
  () => progress.value?.status === 'APPROVED' && progress.value?.settleMode === 'AUTO',
)
// 是否"规则引擎全通过 -> 快进办结出证岗人工复核"的申报(settleMode=ISSUE_REVIEW)
const isIssueReview = computed(() => progress.value?.settleMode === 'ISSUE_REVIEW')
// 当前登录岗位是否为办结出证岗(决定是否显示"请复核规则后出证"提示)
const isIssueRole = computed(() => authStore.role === 'ISSUE')
// 办结岗对"AI自动审核通过"单做最终办结确认时,弹窗标题/提示语
const isIssueReviewFinalConfirm = computed(
  () => isIssueReview.value && isIssueRole.value && operableRole.value === 'ISSUE',
)
const handleDialogTitle = computed(() =>
  isIssueReviewFinalConfirm.value ? '办结确认（AI自动审核已通过）' : '审批操作',
)
// 办结岗 PASS 按钮文案: AI自动审核通过的单强调"复核后办结"; 其余按事项形态
function issuePassActionLabel() {
  const base = ISSUE_PASS_LABEL[settleKind.value]
  return isIssueReview.value ? '复核后' + base : base
}

// 解析办理附加信息(handleDetail JSON)，返回 { licenseNo?, deliveryType? }
function parseHandleDetail(raw?: string): { licenseNo?: string; deliveryType?: string } {
  if (!raw) return {}
  try {
    const obj = JSON.parse(raw)
    return {
      licenseNo: obj?.licenseNo || '',
      deliveryType: obj?.deliveryType || '',
    }
  } catch {
    return {}
  }
}

const formDataRows = computed(() => {
  if (!instance.value?.formData) return []
  let data: Record<string, any> = {}
  try {
    data = JSON.parse(instance.value.formData)
  } catch {
    return []
  }
  return Object.entries(data).map(([key, value]) => ({
    key,
    value: typeof value === 'object' ? JSON.stringify(value) : String(value ?? ''),
  }))
})

// 规则表的"申报值"列:按校验字段名从表单原文取实际填写值,通过行据此逐项对照,不再显示未过才成立的错误提示
function actualValueOf(field: string): string {
  if (!instance.value?.formData) return ''
  let data: Record<string, any> = {}
  try {
    data = JSON.parse(instance.value.formData)
  } catch {
    return ''
  }
  if (!(field in data)) return '—'
  const v = data[field]
  if (typeof v === 'boolean') return v ? '是' : '否'
  return v === null || v === undefined || v === '' ? '—' : String(v)
}

async function loadInstance() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await approvalApi.getInstance(id)
    instance.value = res.data
    // 加载进度
    const progRes = await approvalApi.getProgress(id)
    progress.value = progRes.data
    // 若 URL 未带待办任务,从进度推断当前登录人可办理的任务(progress 对企业端匿名,按"节点岗位==登录岗位"自判)
    if (!taskId.value) {
      const myRole = authStore.role
      const currentTask = (progRes.data?.nodes || []).find(
        (n: any) =>
          n.taskId &&
          (n.status === 'CURRENT' || n.status === 'PENDING') &&
          n.assigneeRole === myRole,
      )
      if (currentTask) taskId.value = currentTask.taskId
    }
    // 自动运行规则引擎审核
    runRuleCheck()
    // 读取后台预生成的AI意见(申报提交时已异步生成,此处直接显示)
    loadLatestOpinion()
    // 同类办件参考(同事项历史终态, 供结论一致性参考)
    loadSimilar()
    // 申报材料(申报人上传, 审批端复核用)
    loadMaterials()
  } finally {
    loading.value = false
  }
}

// ===== 申报材料：审批端复核用（内部调企业端；企业端不在线时降级为空） =====
async function loadMaterials() {
  try {
    const res = await approvalApi.instanceMaterials(Number(route.params.id))
    materials.value = res.data || []
  } catch {
    materials.value = []
  }
}

/** 同名材料合并为一行并计数（企业端可能对同一份材料重复上传） */
const materialRows = computed(() => {
  const map = new Map<string, any>()
  for (const m of materials.value) {
    const key = String(m.materialName || '').replace(/\([^)]*\)\s*$/, '').trim() || '未命名材料'
    const hit = map.get(key)
    if (hit) hit.count++
    else map.set(key, { name: key, count: 1, fileUrl: m.fileUrl, fileType: m.fileType, uploadTime: m.uploadTime })
  }
  return [...map.values()]
})

/** 预览材料原件（企业端通过 /files/** 暴露上传目录） */
function previewMaterial(m: any) {
  if (!m?.fileUrl) return
  window.open(`http://localhost:8084/files${m.fileUrl}`, '_blank')
}

// 同类办件参考: 同一事项历史终态办件聚合与最近结论
async function loadSimilar() {
  try {
    const id = Number(route.params.id)
    const res = await approvalApi.similar(id)
    const d = res.data || {}
    simMatter.value = d.matterName || ''
    simTotal.value = d.total || 0
    simPassed.value = d.passed || 0
    simRejected.value = d.rejected || 0
    simReturned.value = d.returned || 0
    simList.value = d.latest || []
  } catch (e) { /* 同类参考不可用不阻塞详情 */ }
}

function simStatusLabel(st: string) {
  return ({ APPROVED: '办结', REJECTED: '驳回', RETURNED: '退回' } as Record<string, string>)[st] || st
}
function simStatusTag(st: string) {
  return st === 'APPROVED' ? 'success' : st === 'RETURNED' ? 'warning' : 'danger'
}

// OCR 材料识别: 经审批端代理调用企业端 PaddleOCR, 提取材料字段供复核参考
async function onOcrPick(file: any) {
  const raw = file?.raw || file
  if (!raw) return
  ocrLoading.value = true
  ocrErr.value = ''
  ocrFields.value = {}
  try {
    const b64 = await fileToBase64(raw)
    const res = await approvalApi.ocr({ imageBase64: b64, type: 'GENERAL' })
    ocrFields.value = (res.data && res.data.fields) || {}
  } catch (e: any) {
    ocrErr.value = e?.message || 'OCR 识别失败, 请确认 ocr-service(8090) 已启动'
  } finally {
    ocrLoading.value = false
  }
}
function fileToBase64(raw: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const s = String(reader.result)
      resolve(s.includes('base64,') ? s.split('base64,')[1] : s)
    }
    reader.onerror = reject
    reader.readAsDataURL(raw)
  })
}

// 加载后台预生成的 AI 意见(最近一次); 无则保持空,保留手动生成按钮
async function loadLatestOpinion() {
  const id = Number(route.params.id)
  const res = await aiApi.latestOpinion(id).catch(() => null)
  const rec = res?.data
  if (rec?.aiResponse) {
    aiOpinion.value = rec.aiResponse
    aiGenTime.value = rec.createTime || ''
  }
}

async function runRuleCheck() {
  ruleLoading.value = true
  ruleResult.value = null
  try {
    const res = await aiApi.ruleCheckByInstance(Number(route.params.id))
    ruleResult.value = res.data
  } finally {
    ruleLoading.value = false
  }
}

async function generateAiOpinion() {
  aiLoading.value = true
  try {
    const res = await aiApi.generateOpinion(Number(route.params.id))
    aiOpinion.value = res.data.response
    aiGenTime.value = ''
  } catch {
    // 错误已处理
  } finally {
    aiLoading.value = false
  }
}

const pendingActionLabel = ref('')

function openHandleDialog(action: { result: string; label: string }) {
  handleDialog.result = action.result
  // 办结岗 PASS 动作标题: AI自动审核通过的单强调"复核后办结"; 其余按事项形态(出证/注销/确认办结)
  pendingActionLabel.value =
    action.result === 'PASS' && operableRole.value === 'ISSUE' ? issuePassActionLabel() : action.label
  handleDialog.opinion = ''
  handleDialog.adoptAiAdvice = false
  handleDialog.deliveryType = ISSUE_DELIVERY_OPTIONS[0] ?? '窗口自取'
  handleDialog.visible = true
}

// 勾选"采纳AI建议"时，将 AI 意见填入审批意见框
watch(() => handleDialog.adoptAiAdvice, (checked) => {
  if (checked) {
    if (!aiOpinion.value) {
      ElMessage.warning('请先生成AI意见')
      handleDialog.adoptAiAdvice = false
      return
    }
    handleDialog.opinion = aiOpinion.value
  }
})

async function submitHandle() {
  if (!taskId.value) {
    ElMessage.warning('未找到待办任务ID')
    return
  }
  // 表单校验
  const valid = await handleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  // 办结前必须核对申报材料：材料内容无法自动判断（尤其 OCR 不可用时），必须由人确认后才能出证
  if (isIssueReviewFinalConfirm.value && !materialChecked.value) {
    ElMessage.warning('请先核对申报材料，并勾选「已核对材料齐全且与申报相符」后再办结')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认执行【${pendingActionLabel.value}】操作？`,
      '提示',
      { type: 'warning' },
    )
    // 办结出证等动作携带办理附加信息(证照号由企业端签发后回填,办结岗只选送达方式)
    const payload: any = {
      taskId: taskId.value,
      approveResult: handleDialog.result,
      approveOpinion: handleDialog.opinion,
      adoptAiAdvice: handleDialog.adoptAiAdvice,
    }
    if (settleNeedDelivery.value) {
      // 注销类固定"电子送达"(不出实体证、不邮寄); 发证类用办结岗所选送达方式
      const dt = settleKind.value === 'CANCEL' ? '电子送达' : handleDialog.deliveryType
      payload.handleDetail = JSON.stringify({ deliveryType: dt })
    }
    await approvalApi.handleTask(payload)
    ElMessage.success('审批操作成功')
    handleDialog.visible = false
    router.push('/approval/todo')
  } catch {
    // 取消确认
  }
}

onMounted(() => {
  taskId.value = route.query.taskId ? Number(route.query.taskId) : null
  loadInstance()
})
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- 1. 顶部概览：事项信息 + 状态/环节 + 当前岗位可执行操作(操作上提,减少滚动) -->
    <el-card class="section-card">
      <div class="detail-head">
        <div class="detail-head-main">
          <div class="detail-title-row">
            <h2 class="detail-matter">{{ instance?.matterName || '—' }}</h2>
            <el-tag :type="instanceStatusTag[progress?.status] || 'info'" size="small">
              {{ instanceStatusLabel[progress?.status] || progress?.status }}
            </el-tag>
            <el-tag v-if="isIssueReview" type="success" size="small" effect="dark">AI自动审核通过</el-tag>
            <el-tag v-if="isAutoSettled" type="success" size="small">即办件·自动办结</el-tag>
          </div>
          <div class="detail-sub-row">
            <span>申请人：{{ instance?.applicantName }}</span>
            <span v-if="instance?.applicantType">
              {{ instance?.applicantType === 'ENTERPRISE' ? '企业' : '个人' }}
            </span>
            <el-tag v-if="instance?.applicantType === 'ENTERPRISE' && instance?.creditLevel"
                    size="small" :type="creditTagType(instance.creditLevel)">
              <el-icon v-if="creditTagType(instance.creditLevel) === 'success'" style="margin-right:2px"><CircleCheck /></el-icon>
              企业信用 {{ creditText(instance.creditLevel) }}{{ instance.creditLevel === 'A' ? '·绿色通道' : '' }}
            </el-tag>
            <span>提交：{{ instance?.submitTime }}</span>
          </div>
        </div>
        <!-- 当前岗位可执行操作区 -->
        <div v-if="operableNode" class="detail-actions">
          <div class="detail-actions-hint">
            当前环节：<b>{{ operableNode.name }}</b>
            <el-tag size="small" type="info" style="margin-left: 6px">{{ roleName(operableRole) }}</el-tag>
          </div>
          <div class="detail-actions-btns">
            <el-button
              v-for="act in actionButtons"
              :key="act.result"
              :type="act.type"
              @click="openHandleDialog(act)"
            >{{ operableRole === 'ISSUE' && act.result === 'PASS' ? issuePassActionLabel() : act.label }}</el-button>
          </div>
        </div>
      </div>

      <!-- AI自动审核全通过 -> 快进办结出证岗人工最终办结: 顶部醒目提示发证岗复核后再办结 -->
      <el-alert
        v-if="isIssueReview"
        type="success"
        :closable="false"
        show-icon
        class="issue-review-alert"
      >
        <template #title>
          <span v-if="isIssueRole">
            <b>AI 自动审核已通过</b>（规则校验全过）——此单已快进至您处，请您人工复核申报材料后确认办结（最终办结权在人工）
          </span>
          <span v-else>
            <b>AI 自动审核已通过</b>（规则校验全过），当前已快进至办结出证岗等待人工复核办结
          </span>
        </template>
      </el-alert>

      <!-- 流程进度：四环节细分 -->
      <el-steps
        v-if="progress"
        :active="activeStepIndex"
        finish-status="success"
        align-center
        class="detail-steps"
      >
        <el-step
          v-for="(node, idx) in progress.nodes"
          :key="idx"
          :title="node.name"
          :status="nodeStepStatus(node.status)"
          :description="node.assigneeRole ? roleName(node.assigneeRole) : '—'"
        />
      </el-steps>
      <div v-if="progress" class="detail-current-line">
        <span>当前环节：<b>{{ isAutoSettled ? '已系统自动办结' : (progress.currentNode || '已办结') }}</b></span>
        <span v-if="progress.status === 'APPROVING' && !operableNode" class="text-muted">
          （该环节待办不归当前岗位办理）
        </span>
      </div>
    </el-card>

    <!-- 1.5 申报材料:申报人上传的材料原件（材料内容无法自动核验，须审批人人工核对） -->
    <el-card class="section-card">
      <template #header>
        <div style="display: flex; align-items: center; gap: 8px">
          <span class="section-title">申报材料</span>
          <el-tag v-if="materialRows.length" type="info" size="small">
            {{ materialRows.length }} 种 / {{ materials.length }} 份
          </el-tag>
          <el-tag v-else type="warning" size="small">无上传材料</el-tag>
          <el-tooltip content="申报人上传的材料原件，请核对是否齐全、是否与申报事项相符（内容无法自动判断，须人工查看）" placement="top">
            <el-icon style="color: #909399; cursor: help"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </template>
      <el-table v-if="materialRows.length" :data="materialRows" size="small" border>
        <el-table-column label="材料名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.name }}
            <el-tag v-if="row.count > 1" size="small" type="warning" effect="plain" style="margin-left: 6px">
              重复 {{ row.count }} 份
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="文件类型" width="150" />
        <el-table-column label="上传时间" width="150">
          <template #default="{ row }">{{ String(row.uploadTime || '').replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.fileUrl" link type="primary" @click="previewMaterial(row)">查看原件</el-button>
            <span v-else class="text-muted">无文件</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="该申报没有上传材料（可能全部由电子证照免证办自动核验）" :image-size="50" />
    </el-card>

    <!-- 2. 申报表单数据(申报无表单字段时整卡自动隐藏,不留空卡) -->
    <el-card v-if="formDataRows.length" class="section-card">
      <template #header>
        <div style="display: flex; align-items: center">
          <span class="section-title">申报表单</span>
          <el-tooltip content="群众/企业提交申报时填写的表单字段，规则引擎据此自动校验" placement="top">
            <el-icon style="margin-left: 6px; color: #909399; cursor: help"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </template>
      <el-table :data="formDataRows" border size="small">
        <el-table-column prop="key" label="字段" width="220" />
        <el-table-column prop="value" label="填写内容" />
      </el-table>
    </el-card>

    <!-- 3. 智能审核区(单卡): ①规则引擎自动审核 → ②AI 审核意见 上下排列,减少卡片碎片 -->
    <el-card class="section-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
          <div style="display: flex; align-items: center; gap: 8px">
            <span class="section-title">AI 智能审核</span>
            <el-tooltip content="规则引擎按申报值自动核验给出结论,再生成简短核查要点,供审批人复核参考" placement="top">
              <el-icon style="color: #909399; cursor: help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
          <div>
            <el-button size="small" :loading="ruleLoading" @click="runRuleCheck">重新审核</el-button>
            <el-button type="primary" size="small" :loading="aiLoading" @click="generateAiOpinion">
              {{ aiLoading ? '研判中…' : aiOpinion ? '重新生成AI意见' : '生成AI意见' }}
            </el-button>
          </div>
        </div>
      </template>

      <!-- 规则引擎自动审核 -->
      <div class="ai-zone-head">
        <span class="ai-zone-title">规则引擎自动审核</span>
      </div>
      <el-alert
        v-if="ruleResult"
        :type="ruleAlertType(ruleResult.result)"
        :title="ruleAlertTitle(ruleResult.result)"
        :description="ruleAlertDesc(ruleResult.result)"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-table v-if="ruleResult?.items?.length" :data="ruleResult.items" border size="small">
        <el-table-column prop="fieldName" label="校验字段" width="150" />
        <el-table-column prop="ruleType" label="类型" width="80">
          <template #default="{ row }">
            {{ ruleTypeLabel[row.ruleType] || row.ruleType }}
          </template>
        </el-table-column>
        <el-table-column label="申报值" width="150">
          <template #default="{ row }">
            {{ actualValueOf(row.fieldName) }}
          </template>
        </el-table-column>
        <el-table-column label="结果" width="70">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : 'danger'" size="small">
              {{ row.passed ? '通过' : '未过' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提示">
          <template #default="{ row }">
            <el-tag v-if="!row.passed" type="danger" size="small">{{ row.errorMessage }}</el-tag>
            <span v-else class="text-muted" style="font-size: 12px">符合规则项要求</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="ruleResult?.errors?.length" style="margin-top: 12px">
        <el-alert
          v-for="(err, i) in ruleResult.errors"
          :key="i"
          :title="err"
          type="error"
          :closable="false"
          style="margin-bottom: 4px"
        />
      </div>
      <el-empty v-if="!ruleResult" description="正在自动校验申报材料是否符合规则集要求..." :image-size="48" />

      <el-divider style="margin: 12px 0 8px" />

      <!-- AI 审核意见(默认只给摘要,可展开全部) -->
      <div class="ai-zone-head">
        <span class="ai-zone-title">AI 审核意见</span>
        <span v-if="aiGenTime" class="text-muted" style="font-size: 12px">
          {{ String(aiGenTime).replace('T', ' ').slice(0, 16) }}
        </span>
      </div>
      <div v-if="aiOpinion">
        <template v-if="opinionSections?.conclusion">
          <!-- 无规则横幅时(接口空/加载中)用意见结论兜底 -->
          <div
            v-if="!ruleResult"
            class="ai-verdict"
            :class="'is-' + conclusionTag(conclusionWord(opinionSections.conclusion))"
            style="margin-bottom: 8px"
          >
            <div class="ai-verdict-word">{{ conclusionWord(opinionSections.conclusion) }}</div>
            <div class="ai-verdict-detail">{{ opinionSections.conclusion }}</div>
          </div>
          <template v-if="riskParagraphs.length">
            <div class="ai-para-title">核查要点</div>
            <div
              v-for="(p, i) in opinionViewItems"
              :key="i"
              class="ai-opinion-line"
              :class="{ 'ai-opinion-risk': p.risk, 'ai-clamp': !showOpinionFull }"
            >{{ p.text }}</div>
            <div v-if="opinionHiddenCount" class="ai-fold">
              <el-button link type="primary" @click="toggleOpinionFull">展开全部 {{ riskParagraphs.length }} 条</el-button>
            </div>
            <div v-else-if="showOpinionFull" class="ai-fold">
              <el-button link type="info" @click="toggleOpinionFull">收起</el-button>
            </div>
          </template>
        </template>
        <!-- 兜底:无固定骨架的旧/mock意见原样展示 -->
        <pre v-else style="white-space: pre-wrap; font-family: inherit; margin: 0; line-height: 1.8">{{ aiOpinion }}</pre>
      </div>
      <el-empty v-else :description="aiLoading ? '大模型分析中…' : '暂无AI预生成意见，可点击右上角生成'" :image-size="44" />
    </el-card>

    <!-- 3.1 同类办件参考(知识关联: 同事项历史终态结论, 辅助结论一致性) -->
    <el-card v-if="simMatter" class="section-card">
      <template #header>
        <div style="display: flex; align-items: center; gap: 8px">
          <span class="section-title">同类办件参考</span>
          <el-tag type="info" size="small">「{{ simMatter }}」历史终态</el-tag>
        </div>
      </template>
      <div class="sim-counts" style="display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 10px">
        <el-tag type="info" effect="plain">历史共 {{ simTotal }} 件</el-tag>
        <el-tag type="success" effect="plain">办结 {{ simPassed }}</el-tag>
        <el-tag type="danger" effect="plain">驳回 {{ simRejected }}</el-tag>
        <el-tag type="warning" effect="plain">退回 {{ simReturned }}</el-tag>
      </div>
      <div v-if="simList.length" style="display: grid; gap: 8px">
        <div
          v-for="(s, i) in simList"
          :key="i"
          style="display: flex; align-items: center; gap: 8px; font-size: 13px; color: #606266; padding: 4px 0; border-bottom: 1px dashed #ebeef5"
        >
          <span style="color: #909399">#{{ s.id }}</span>
          <span style="flex: 1">{{ s.applicantName || '-' }}</span>
          <el-tag size="small" :type="simStatusTag(s.status)">{{ simStatusLabel(s.status) }}</el-tag>
          <span style="color: #b0b5c0; font-size: 12px">{{ s.finishTime || '' }}</span>
        </div>
      </div>
      <el-empty v-else description="暂无历史同类办件" :image-size="50" />
    </el-card>

    <!-- 3.2 OCR 材料识别(辅助复核上传材料) -->
    <el-card class="section-card">
      <template #header>
        <div style="display: flex; align-items: center; gap: 8px">
          <span class="section-title">OCR 材料识别</span>
          <el-tag type="info" size="small">辅助复核上传材料</el-tag>
        </div>
      </template>
      <div style="display: flex; align-items: center; gap: 12px">
        <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onOcrPick">
          <el-button type="primary" plain :loading="ocrLoading">{{ ocrLoading ? '识别中…' : '选择材料图片识别' }}</el-button>
        </el-upload>
        <span style="color: #909399; font-size: 12px">识别结果仅供参考，不作为审批依据</span>
      </div>
      <el-alert v-if="ocrErr" :title="ocrErr" type="warning" :closable="false" show-icon style="margin-top: 10px" />
      <div
        v-if="Object.keys(ocrFields).length"
        style="margin-top: 12px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px"
      >
        <div
          v-for="(v, k) in ocrFields"
          :key="k"
          style="background: #f7f8fa; border-radius: 6px; padding: 8px 10px; font-size: 13px"
        >
          <div style="color: #909399; font-size: 12px">{{ k }}</div>
          <div style="color: #303133">{{ v }}</div>
        </div>
      </div>
      <el-empty
        v-else-if="!ocrLoading && !ocrErr"
        description="上传材料图片，自动提取字段"
        :image-size="44"
        style="padding: 8px 0"
      />
    </el-card>

    <!-- 4. 状态提示(无操作场景的补充说明) -->
    <el-alert
      v-if="isAutoSettled"
      title="该申报为即办件，已由系统自动办结，无需人工审批"
      type="success"
      :closable="false"
      show-icon
      class="section-card"
      style="margin-bottom: 16px"
    />

    <!-- 5. 审批记录(已办理环节明细：岗位/办理人/结果/意见/证照号) -->
    <el-card class="section-card">
      <template #header>
        <div style="display: flex; align-items: center">
          <span class="section-title">审批记录</span>
          <el-tag type="info" size="small" style="margin-left: 8px">已办理环节明细</el-tag>
        </div>
      </template>
      <div v-if="recordNodes.length" class="record-grid">
        <div v-for="(node, idx) in recordNodes" :key="idx" class="record-card" :class="{ done: node.status === 'DONE' }">
          <div class="record-card-head">
            <b>{{ node.name }}</b>
            <el-tag v-if="node.status === 'DONE'" size="small" type="success">已完成</el-tag>
            <el-tag v-else-if="node.status === 'CURRENT'" size="small" type="warning">当前</el-tag>
            <el-tag v-else size="small" type="info">待办</el-tag>
          </div>
          <div v-if="node.assigneeRole" class="record-line">岗位：{{ roleName(node.assigneeRole) }}</div>
          <div v-if="node.assignee" class="record-line">办理人：{{ node.assignee }}</div>
          <div v-if="node.approveResult" class="record-line">
            结果：<el-tag :type="node.approveResult === 'PASS' ? 'success' : node.approveResult === 'RETURN' ? 'warning' : 'danger'" size="small">
              {{ approveResultLabel[node.approveResult] || node.approveResult }}
            </el-tag>
          </div>
          <div v-if="node.approveOpinion" class="record-line record-opinion">意见：{{ node.approveOpinion }}</div>
          <div v-if="parseHandleDetail(node.handleDetail).licenseNo" class="record-line">
            证照号：<el-tag size="small" type="success">{{ parseHandleDetail(node.handleDetail).licenseNo }}</el-tag>
          </div>
          <div v-if="parseHandleDetail(node.handleDetail).deliveryType" class="record-line">
            送达：{{ parseHandleDetail(node.handleDetail).deliveryType }}
          </div>
          <div v-if="node.handleTime" class="record-line record-time">{{ node.handleTime }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无审批记录" :image-size="60" />
    </el-card>

    <!-- 审批操作弹窗 -->
    <el-dialog v-model="handleDialog.visible" :title="handleDialogTitle" width="520px" @closed="handleFormRef?.clearValidate">
      <el-form ref="handleFormRef" :model="handleDialog" :rules="handleRules" label-width="100px">
        <!-- 办结岗对 AI自动审核通过的单做最终办结确认: 明确提示这是 AI结论,由人工做最终把关 -->
        <el-alert
          v-if="isIssueReviewFinalConfirm"
          type="success"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        >
          <template #title>
            AI 自动审核已全部通过，系统将本次办结决定交予您人工复核——请确认申报材料与规则校验结果相符后办结
          </template>
        </el-alert>
        <!-- 办结前材料核对：材料内容无法自动判断，必须人工核对并勾选确认后才能出证 -->
        <el-form-item v-if="isIssueReviewFinalConfirm" label="材料核对" required>
          <div class="mat-check">
            <div v-if="!materialRows.length" class="mat-check-empty">
              该申报没有上传材料（可能全部由电子证照免证办自动核验）
            </div>
            <div v-for="(m, i) in materialRows" :key="i" class="mat-check-row">
              <el-icon color="#2f6bff"><Document /></el-icon>
              <span class="mc-name">{{ m.name }}</span>
              <el-tag v-if="m.count > 1" size="small" type="warning" effect="plain">重复 {{ m.count }} 份</el-tag>
              <el-button v-if="m.fileUrl" link type="primary" @click="previewMaterial(m)">查看</el-button>
            </div>
            <el-checkbox v-model="materialChecked" style="margin-top: 8px">
              我已核对上述材料齐全、且与本次申报事项相符
            </el-checkbox>
          </div>
        </el-form-item>
        <el-form-item label="审批结果">
          <el-tag :type="handleDialog.result === 'PASS' ? 'success' : handleDialog.result === 'REJECT' ? 'danger' : 'warning'">
            {{ pendingActionLabel }}
          </el-tag>
        </el-form-item>
        <!-- 办结附加信息：送达方式(证照号由企业端自动签发回填,办结岗无需填)。
             发证类可选(窗口/邮寄/电子);注销类固定电子送达(不邮寄证照);无送达类不显示 -->
        <template v-if="settleNeedDelivery">
          <el-form-item v-if="settleKind === 'LICENSE'" label="送达方式" prop="deliveryType">
            <el-select v-model="handleDialog.deliveryType" style="width: 100%">
              <el-option v-for="opt in ISSUE_DELIVERY_OPTIONS" :key="opt" :label="opt" :value="opt" />
            </el-select>
          </el-form-item>
          <el-form-item v-else label="送达方式">
            <el-tag type="info">电子送达</el-tag>
            <span style="margin-left: 8px; color: #909399; font-size: 12px">注销结果电子送达，不邮寄证照</span>
          </el-form-item>
        </template>
        <el-form-item label="审批意见" prop="opinion">
          <el-input
            v-model="handleDialog.opinion"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            :placeholder="handleDialog.result === 'PASS' ? '请输入审批意见（选填）' : '请输入审批意见（必填）'"
          />
        </el-form-item>
        <el-form-item label="采纳AI建议" v-if="operableRole === 'DECIDE' || operableRole === 'APPROVER'">
          <el-switch v-model="handleDialog.adoptAiAdvice" />
          <span v-if="!aiOpinion" style="margin-left: 8px; color: #909399; font-size: 12px">需先生成AI意见</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ai-opinion-box {
  background: #f5f7fa;
  padding: 14px;
  border-radius: 8px;
  border-left: 4px solid #2f6bff;
  line-height: 1.8;
}

/* AI 意见结构化卡片:结论徽标 / 建议决定 / 核查要点与风险分段 */
.ai-verdict {
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 10px;
}
.ai-verdict.is-success {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
}
.ai-verdict.is-danger {
  background: #fef0f0;
  border: 1px solid #fde2e2;
}
.ai-verdict.is-warning {
  background: #fdf6ec;
  border: 1px solid #faecd8;
}
.ai-verdict.is-info {
  background: #f4f4f5;
  border: 1px solid #e9e9eb;
}
.ai-verdict-word {
  font-weight: 700;
  font-size: 15px;
}
.is-success .ai-verdict-word { color: #67c23a; }
.is-danger .ai-verdict-word { color: #f56c6c; }
.is-warning .ai-verdict-word { color: #e6a23c; }
.is-info .ai-verdict-word { color: #909399; }
.ai-verdict-detail {
  font-size: 12px;
  color: #606266;
  margin-top: 3px;
}
.ai-decision {
  margin-bottom: 10px;
}
.ai-section-label {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
  margin-bottom: 2px;
}
.ai-check-title {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
  margin-bottom: 6px;
}
.ai-line {
  font-size: 13px;
  color: #303133;
  margin: 0;
}
.ai-para {
  font-size: 13px;
  line-height: 1.9;
  padding: 6px 10px;
  border-radius: 6px;
  margin-bottom: 6px;
}
.ai-para:last-child {
  margin-bottom: 0;
}
.ai-para-check {
  background: #fff;
  border: 1px solid #ebeef5;
  color: #303133;
}
.ai-para-risk {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  color: #d9534f;
}
.ai-opinion-box.legacy {
  background: #fff;
}

/* 顶部概览 */
.detail-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.detail-head-main {
  min-width: 260px;
}
.detail-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.detail-matter {
  margin: 0;
  font-size: 18px;
  color: #1f2d3d;
}
.detail-sub-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}
.detail-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
.detail-actions-hint {
  font-size: 13px;
  color: #606266;
}
.detail-actions-btns {
  display: flex;
  gap: 10px;
}
.detail-steps {
  margin-top: 20px;
}
.detail-current-line {
  margin-top: 14px;
  text-align: center;
  color: #606266;
  font-size: 13px;
}
.text-muted {
  color: #909399;
}

/* 审批记录：横向小卡 */
.record-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 12px;
}
.record-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 14px;
  background: #fafbfc;
  line-height: 1.7;
}
.record-card.done {
  border-left: 3px solid #67c23a;
}
.record-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.record-line {
  font-size: 13px;
  color: #606266;
}
.record-opinion {
  color: #303133;
  word-break: break-all;
}
.record-time {
  margin-top: 4px;
  font-size: 12px;
  color: #a8abb2;
}

/* AI 智能审核单卡:分区标题 / 决策引用块 / 核查要点分点 */
.ai-zone-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.ai-zone-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.ai-decision-block {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  background: #f0f4ff;
  border: 1px solid #dbe6ff;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 10px;
  font-size: 13px;
  line-height: 1.8;
  color: #303133;
}
.ai-decision-tag {
  flex: none;
  font-weight: 600;
  color: #2f6bff;
  white-space: nowrap;
}
.ai-decision-text {
  flex: 1;
  min-width: 0;
}
.ai-para-title {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
  margin-bottom: 6px;
}
.ai-para {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.9;
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 8px;
}
.ai-para:last-child {
  margin-bottom: 0;
}
.ai-para-mark {
  flex: none;
  margin-top: 5px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  text-align: center;
  line-height: 16px;
  font-size: 11px;
  font-weight: 700;
}
.ai-para-text {
  flex: 1;
  min-width: 0;
}
.ai-para-check {
  background: #fff;
  border: 1px solid #ebeef5;
  color: #303133;
}
.ai-para-check .ai-para-mark {
  background: transparent;
  color: #a8abb2;
  font-size: 15px;
  font-weight: 400;
  line-height: 16px;
}
.ai-para-risk {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  color: #d9534f;
}
.ai-para-risk .ai-para-mark {
  background: #f56c6c;
  color: #fff;
}
.ai-fold {
  margin-top: 2px;
}
.ai-opinion-line {
  font-size: 13px;
  line-height: 1.85;
  color: #303133;
  margin-bottom: 6px;
}
.ai-opinion-line:last-child {
  margin-bottom: 0;
}
.ai-opinion-risk {
  color: #d9534f;
}
.ai-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

/* 办结前材料核对清单 */
.mat-check {
  width: 100%;
  max-height: 220px;
  overflow-y: auto;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
}
.mat-check-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  padding: 4px 0;
}
.mc-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
}
.mat-check-empty {
  font-size: 13px;
  color: #909399;
  padding: 4px 0;
}
</style>
