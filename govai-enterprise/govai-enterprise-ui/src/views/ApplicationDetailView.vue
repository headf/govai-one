<template>
  <div v-if="detail" class="ad" v-loading="loading">
    <!-- 顶栏 -->
    <section class="card ad-head">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="ad-name">{{ detail.record.matterName }}</span>
          <el-tag class="ml-8" :type="statusTag(detail.record.status)" size="small">{{ statusText(detail.record.status) }}</el-tag>
          <span v-if="detail.record.recordCode" class="text-muted ad-code">申报号 {{ detail.record.recordCode }}</span>
        </template>
      </el-page-header>
    </section>

    <div class="ad-grid">
      <!-- 左列：表单 + 材料 -->
      <div class="ad-left">
        <section class="card">
          <div class="blk-title">申报表单</div>
          <el-descriptions v-if="formEntries.length" :column="2" border>
            <el-descriptions-item v-for="(e, i) in formEntries" :key="i" :label="fieldLabel(e.key)">
              {{ displayVal(e.value) }}
            </el-descriptions-item>
          </el-descriptions>
          <p v-else class="blk-body text-muted">无表单内容</p>
        </section>

        <!-- 待补正 -->
        <el-alert
          v-if="detail.record.status === 'REPAIR'"
          type="warning"
          :closable="false"
          show-icon
          class="repair-alert"
          :title="'该申报已被退回补正：' + (detail.record.remark || '请补充材料后重新提交')"
        >
          <template #default>
            <el-button type="primary" size="small" @click="gotoRepair">补充材料并重新提交</el-button>
          </template>
        </el-alert>

        <section class="card">
          <div class="blk-title">申报材料</div>
          <p v-if="!detail.materials.length" class="blk-body text-muted">无已上传材料（可能已由电子证照自动核验）</p>
          <div v-else class="mat-list">
            <div v-for="m in detail.materials" :key="m.id" class="mat-row">
              <el-icon class="doc-ic"><Document /></el-icon>
              <span class="mat-name">{{ m.materialName }}</span>
              <el-button link type="primary" @click="openFile(m.fileUrl)">查看文件</el-button>
            </div>
          </div>
        </section>
      </div>

      <!-- 右列：进度 / 到账 / 预检 / 评价 -->
      <div class="ad-right">
        <section class="card">
          <div class="blk-title">审批进度</div>
          <el-alert v-if="currentNode" type="warning" :closable="false" show-icon class="flow-alert">
            <template #title>当前环节：{{ currentNode.name }}</template>
            <template #default>
              <span v-if="currentNode.assignee" class="text-muted">办理人：{{ currentNode.assignee }}</span>
              <span v-if="nextNode" class="text-muted"> · 下一环节：{{ nextNode.name }}</span>
              <div v-if="!nextNode" class="text-muted" style="font-size: 12px; margin-top: 2px">本环节完成后即办结</div>
            </template>
          </el-alert>
          <el-alert v-else-if="detail.record.status === 'PENDING'" type="info" :closable="false" show-icon class="flow-alert"
            title="等待受理" description="申报已提交，等待审批部门受理，请耐心等待" />
          <p v-if="!detail.progress" class="blk-body text-muted">审批进度尚未生成</p>
          <el-timeline v-else class="flow-timeline">
            <el-timeline-item
              v-for="(n, i) in detail.progress.nodes"
              :key="i"
              :type="nodeType(n.status)"
              :hollow="n.status !== 'DONE'"
              :timestamp="n.handleTime || ''"
            >
              <div style="line-height: 1.7">
                <b>{{ n.name }}</b>
                <el-tag v-if="n.status === 'CURRENT'" size="small" type="warning" class="ml-8">当前环节</el-tag>
                <el-tag v-else-if="n.status === 'DONE'" size="small" type="success" class="ml-8">已完成</el-tag>
                <div v-if="n.assigneeRole" class="text-muted" style="font-size: 12px">办理岗位：{{ roleName(n.assigneeRole) }}</div>
                <div v-if="n.assignee" class="text-muted" style="font-size: 12px">办理人：{{ n.assignee }}</div>
                <div v-if="n.status === 'DONE' && n.approveResult" style="font-size: 12px">
                  结果：<el-tag :type="n.approveResult === 'PASS' ? 'success' : 'danger'" size="small">{{ resultText(n.approveResult) }}</el-tag>
                </div>
                <div v-if="n.status === 'DONE' && n.approveOpinion" class="text-muted" style="font-size: 12px">
                  意见：{{ n.approveOpinion }}
                </div>
                <template v-if="n.handleDetail && (parseHandleDetail(n.handleDetail).licenseNo || parseHandleDetail(n.handleDetail).deliveryType)">
                  <div v-if="parseHandleDetail(n.handleDetail).licenseNo" style="font-size: 12px">
                    证照号：<el-tag size="small" type="success">{{ parseHandleDetail(n.handleDetail).licenseNo }}</el-tag>
                  </div>
                  <div v-if="parseHandleDetail(n.handleDetail).deliveryType" class="text-muted" style="font-size: 12px">
                    送达方式：{{ parseHandleDetail(n.handleDetail).deliveryType }}
                  </div>
                </template>
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>

        <section v-if="detail.subsidies?.length" class="card">
          <div class="blk-title">补贴 / 减免到账</div>
          <div v-for="s in detail.subsidies" :key="s.id" class="subsidy-row">
            <el-icon color="#16a34a"><CircleCheck /></el-icon>
            <div class="subsidy-main">
              <div class="subsidy-name">{{ s.subsidyType }}</div>
              <div class="text-muted" style="font-size: 12px">{{ fmt(s.payTime) }} · {{ s.bankName || '' }}{{ s.bankAccount ? ' ' + s.bankAccount : '' }}</div>
            </div>
            <b class="subsidy-amount">¥{{ Number(s.amount || 0).toLocaleString() }}</b>
          </div>
        </section>

        <section v-if="detail.checks?.length" class="card">
          <div class="blk-title">合规预检</div>
          <div v-for="c in detail.checks" :key="c.id" class="check-row">
            <el-icon :color="c.checkResult ? '#16a34a' : '#dc2626'">
              <CircleCheck v-if="c.checkResult" /><CircleClose v-else />
            </el-icon>
            <span class="check-name">{{ c.checkItem }}</span>
            <el-tag :type="c.checkResult ? 'success' : 'danger'" size="small">{{ c.checkResult ? '通过' : '不通过' }}</el-tag>
          </div>
        </section>

        <!-- 好差评 -->
        <section class="card">
          <div class="blk-title">好差评</div>
          <div v-if="myEval" class="eval-done">
            <el-rate :model-value="myEval.starRating" disabled show-score score-template="{value} 星" />
            <span class="text-muted ml-8">{{ starText(myEval.starRating) }}</span>
            <div v-if="myEval.tags?.length" class="mt-8">
              <el-tag v-for="t in myEval.tags" :key="t" size="small" class="mr-8">{{ t }}</el-tag>
            </div>
            <p v-if="myEval.content" class="text-muted mt-8">{{ myEval.content }}</p>
            <div class="text-muted mt-8" style="font-size: 12px">评价时间：{{ fmt(myEval.createTime) }}</div>
          </div>
          <div v-else-if="canEvaluate">
            <p class="text-muted">欢迎对本次办事体验进行评价，帮助我们持续改进服务。</p>
            <el-button type="primary" :icon="Star" @click="evalDialogVisible = true">评价本次服务</el-button>
          </div>
          <p v-else class="blk-body text-muted">申报办理完成后即可评价</p>
        </section>
      </div>
    </div>

    <!-- 评价弹窗 -->
    <el-dialog v-model="evalDialogVisible" title="好差评" width="480px">
      <div class="rate-box">
        <el-rate v-model="evalForm.starRating" show-text :texts="['非常差', '差', '一般', '满意', '非常满意']" />
      </div>
      <div class="mt-8 label">快捷标签</div>
      <el-checkbox-group v-model="evalForm.tags" class="mt-8 tag-box">
        <el-checkbox v-for="t in tagOptions" :key="t" :value="t">{{ t }}</el-checkbox>
      </el-checkbox-group>
      <el-input
        v-model="evalForm.content"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="说说您的体验或建议（选填）"
        class="mt-8"
      />
      <template #footer>
        <el-button @click="evalDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="evalSubmitting" @click="submitEval">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose, Star, Document } from '@element-plus/icons-vue'
import { getApplyDetail, getEvaluation, submitEvaluation, getMatter } from '@/api'
import type { ApplyDetailVO, Evaluation, FormField } from '@/types'

const route = useRoute()
const router = useRouter()
const detail = ref<ApplyDetailVO>()
const loading = ref(false)
const matterFields = ref<FormField[]>([])

const myEval = ref<Evaluation | null>(null)
const evalDialogVisible = ref(false)
const evalSubmitting = ref(false)
const evalForm = ref<{ starRating: number; content: string; tags: string[] }>({
  starRating: 5,
  content: '',
  tags: []
})
const tagOptions = ['办事效率高', '服务态度好', '材料简单', '流程清晰', '网上办事方便', '有待改进']

const canEvaluate = computed(() => {
  const s = detail.value?.record.status
  return !!s && ['PASSED', 'APPROVED', 'FINISHED', 'REJECTED'].includes(s)
})

const formEntries = computed<{ key: string; value: any }[]>(() =>
  detail.value?.record.formData
    ? Object.entries(detail.value.record.formData).map(([key, value]) => ({ key, value }))
    : []
)
const currentNode = computed(() => detail.value?.progress?.nodes?.find((n) => n.status === 'CURRENT'))
const nextNode = computed(() => {
  const nodes = detail.value?.progress?.nodes || []
  const idx = nodes.findIndex((n) => n.status === 'CURRENT')
  return idx >= 0 && idx + 1 < nodes.length ? nodes[idx + 1] : null
})

function statusText(s: string) {
  return { DRAFT: '草稿', PENDING: '待受理', APPROVING: '审批中', APPROVED: '已通过', PASSED: '已通过', REJECTED: '已驳回', REPAIR: '待补正', FINISHED: '已办结' }[s] || s
}
function statusTag(s: string) {
  if (['PENDING', 'APPROVING'].includes(s)) return 'warning'
  if (['APPROVED', 'PASSED', 'FINISHED'].includes(s)) return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}
function nodeType(s: string) {
  if (s === 'DONE') return 'success'
  if (s === 'CURRENT') return 'warning'
  return 'info'
}
function roleName(code?: string) {
  return { APPROVER: '受理初审', DECIDE: '终审', ISSUE: '办结出证' }[code || ''] || code || ''
}
function resultText(r?: string) {
  return { PASS: '通过', REJECT: '驳回', RETURN: '退回' }[r || ''] || r || ''
}
function parseHandleDetail(raw?: string) {
  if (!raw) return {}
  try {
    const obj = JSON.parse(raw)
    return { licenseNo: obj?.licenseNo || '', deliveryType: obj?.deliveryType || '' }
  } catch {
    return {}
  }
}
function fieldLabel(key: string) {
  const f = matterFields.value.find((x) => x.key === key)
  return f ? f.label : key
}
function displayVal(v: any) {
  if (typeof v === 'boolean') return v ? '是' : '否'
  return v === undefined || v === null || v === '' ? '-' : String(v)
}
function openFile(url: string) {
  window.open(`/files${url.startsWith('/files') ? url.slice(6) : url}`, '_blank')
}
function gotoRepair() {
  if (!detail.value) return
  router.push(`/matters/${detail.value.record.matterId}/apply?draft=${detail.value.record.id}&repair=1`)
}
function starText(n: number) {
  return { 1: '非常差', 2: '差', 3: '一般', 4: '满意', 5: '非常满意' }[n] || ''
}
function fmt(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}
async function submitEval() {
  if (!detail.value) return
  if (!evalForm.value.starRating) {
    ElMessage.warning('请选择星级')
    return
  }
  evalSubmitting.value = true
  try {
    await submitEvaluation({
      recordId: detail.value.record.id,
      starRating: evalForm.value.starRating,
      content: evalForm.value.content || undefined,
      tags: evalForm.value.tags
    })
    ElMessage.success('感谢您的评价')
    evalDialogVisible.value = false
    myEval.value = await getEvaluation(detail.value.record.id)
  } finally {
    evalSubmitting.value = false
  }
}
onMounted(async () => {
  loading.value = true
  try {
    detail.value = await getApplyDetail(Number(route.params.id))
    const m = await getMatter(detail.value.record.matterId)
    matterFields.value = m.formConfig || []
    if (detail.value.record.status !== 'DRAFT') {
      myEval.value = await getEvaluation(detail.value.record.id)
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.ad {
  display: grid;
  gap: 16px;
  padding: 4px 0 32px;
}
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 20px 22px;
}
.ad-head {
  padding: 14px 22px;
}
.ad-name {
  font-size: 16px;
  color: #1f2937;
}
.ad-code {
  margin-left: 12px;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.blk-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 4px solid #2f6bff;
}
.blk-body {
  margin: 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.8;
}
.ad-grid {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  gap: 16px;
  align-items: start;
}
.ad-left,
.ad-right {
  display: grid;
  gap: 16px;
}
.repair-alert {
  margin: 0;
}
.mat-list {
  display: grid;
  gap: 4px;
}
.mat-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 6px;
  border-radius: 8px;
}
.mat-row:hover {
  background: #f6f8fb;
}
.doc-ic {
  color: #2f6bff;
}
.mat-name {
  flex: 1;
  font-size: 14px;
  color: #1f2937;
}
.flow-alert {
  margin-bottom: 14px;
}
.flow-timeline {
  padding-left: 2px;
}
.subsidy-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}
.subsidy-main {
  flex: 1;
  min-width: 0;
}
.subsidy-name {
  font-weight: 600;
  color: #303133;
}
.subsidy-amount {
  color: #16a34a;
  font-variant-numeric: tabular-nums;
}
.check-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}
.check-name {
  flex: 1;
  font-size: 13px;
  color: #374151;
}
.eval-done {
  padding: 4px 0;
}
.label {
  color: #606266;
  font-weight: 600;
}
.rate-box {
  text-align: center;
}
.tag-box {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin-bottom: 8px;
}
.ml-8 {
  margin-left: 8px;
}
.mr-8 {
  margin-right: 8px;
}
.mt-8 {
  margin-top: 8px;
}
@media (max-width: 1000px) {
  .ad-grid {
    grid-template-columns: 1fr;
  }
}
</style>
