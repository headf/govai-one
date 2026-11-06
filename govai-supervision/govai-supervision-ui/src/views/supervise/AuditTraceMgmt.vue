<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ total }} 条追溯记录</span>
        <div class="filter-inline">
          <el-input v-model="query.instanceId" placeholder="审批实例ID" clearable style="width: 170px" @keyup.enter="load" />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openChain()">追溯全链路</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="instanceId" label="审批实例号" width="120" />
      <el-table-column label="链路摘要" min-width="300">
        <template #default="{ row }">
          <span class="trace-line">{{ traceSummary(row.traceData) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="记录时间" width="150">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openChain(row.instanceId)">查看链路</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.page"
      @current-change="(p) => { query.page = p; load() }"
    />

    <!-- 全链路追溯：四段结构化视图（审批 / 抽查 / 预警 / 审计） -->
    <el-dialog v-model="chain.visible" title="全链路追溯" width="760px" top="6vh">
      <div class="chain-bar">
        <el-input v-model="chain.queryInstance" placeholder="输入审批实例ID" style="width: 200px" @keyup.enter="runChain()" />
        <el-button type="primary" :loading="chain.loading" @click="runChain()">追溯</el-button>
        <span class="text-muted" style="font-size: 12px">输入实例号，可把该件在审批 / 抽查 / 预警 / 审计四个域的痕迹串成一条链</span>
      </div>

      <template v-if="chain.data">
        <!-- ① 审批记录 -->
        <div class="chain-block">
          <div class="cb-title"><span class="cb-no">①</span>审批记录</div>
          <el-descriptions v-if="chain.data.approvalRecord" :column="2" size="small" border>
            <el-descriptions-item label="申报事项">{{ chain.data.approvalRecord.matterName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="办理状态">
              <el-tag size="small" :type="statusTag(chain.data.approvalRecord.approvalStatus)">
                {{ statusText(chain.data.approvalRecord.approvalStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="申请人">{{ chain.data.approvalRecord.applicantName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申请人类型">{{ chain.data.approvalRecord.applicantType === 'ENTERPRISE' ? '企业' : '个人' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ fmt(chain.data.approvalRecord.submitTime) }}</el-descriptions-item>
            <el-descriptions-item label="办结时间">{{ fmt(chain.data.approvalRecord.finishTime) }}</el-descriptions-item>
            <el-descriptions-item label="办理时长" :span="2">
              {{ chain.data.approvalRecord.processDurationHours != null ? chain.data.approvalRecord.processDurationHours + ' 小时' : '-' }}
            </el-descriptions-item>
          </el-descriptions>
          <div v-else class="cb-empty">未找到该实例的审批记录</div>
        </div>

        <!-- ② 监督检查 -->
        <div class="chain-block">
          <div class="cb-title">
            <span class="cb-no">②</span>监督检查
            <el-tag size="small" type="info" effect="plain">{{ (chain.data.samples || []).length }} 条</el-tag>
          </div>
          <div v-if="(chain.data.samples || []).length" class="cb-list">
            <div v-for="s in chain.data.samples" :key="s.id" class="cb-row">
              <span class="cb-name">{{ s.sampleNo }}</span>
              <el-tag size="small" :type="s.checkResult === 'PASS' ? 'success' : 'warning'">
                {{ s.checkResult === 'PASS' ? '通过' : (s.checkResult || '待检查') }}
              </el-tag>
              <span class="text-muted">{{ fmt(s.checkTime) }}</span>
            </div>
          </div>
          <div v-else class="cb-empty">该件未被抽查</div>
        </div>

        <!-- ③ 风险预警 -->
        <div class="chain-block">
          <div class="cb-title">
            <span class="cb-no">③</span>风险预警
            <el-tag size="small" type="info" effect="plain">{{ (chain.data.riskAlerts || []).length }} 条</el-tag>
          </div>
          <div v-if="(chain.data.riskAlerts || []).length" class="cb-list">
            <div v-for="a in chain.data.riskAlerts" :key="a.id" class="cb-row">
              <span class="cb-name">{{ a.alertNo }}</span>
              <el-tag size="small" :type="a.alertLevel === 'HIGH' ? 'danger' : a.alertLevel === 'MEDIUM' ? 'warning' : 'info'">
                {{ levelText(a.alertLevel) }}
              </el-tag>
              <span class="cb-text">{{ a.alertContent }}</span>
              <span class="text-muted">{{ fmt(a.alertTime) }}</span>
            </div>
          </div>
          <div v-else class="cb-empty">无风险预警</div>
        </div>

        <!-- ④ 审计留痕 -->
        <div class="chain-block">
          <div class="cb-title">
            <span class="cb-no">④</span>审计留痕
            <el-tag size="small" type="info" effect="plain">{{ (chain.data.auditTraces || []).length }} 条</el-tag>
          </div>
          <div v-if="(chain.data.auditTraces || []).length" class="cb-list">
            <div v-for="t in chain.data.auditTraces" :key="t.id" class="cb-row">
              <span class="cb-text">{{ traceSummary(t.traceData) }}</span>
              <span class="text-muted">{{ fmt(t.createTime) }}</span>
            </div>
          </div>
          <div v-else class="cb-empty">无审计留痕</div>
        </div>
      </template>

      <div v-else class="cb-loading">
        {{ chain.loading ? '追溯中…' : '请输入审批实例号后点击「追溯」' }}
      </div>

      <template #footer>
        <el-button @click="chain.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { auditApi } from '../../api/modules'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, instanceId: '' })
const chain = reactive({ visible: false, queryInstance: '', data: null, loading: false })

/** 时间：支持毫秒时间戳与 ISO 字符串，统一输出 YYYY-MM-DD HH:mm */
function fmt(t) {
  if (t === null || t === undefined || t === '') return '-'
  if (typeof t === 'number') {
    const d = new Date(t)
    const p = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

function statusText(s) {
  return { PENDING: '待受理', APPROVING: '审批中', APPROVED: '已通过', REJECTED: '已驳回', RETURNED: '已退回' }[s] || s || '-'
}
function statusTag(s) {
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  if (s === 'RETURNED') return 'warning'
  return 'info'
}
function levelText(l) {
  return { HIGH: '高', MEDIUM: '中', LOW: '低' }[l] || l || '-'
}

/** 链路数据摘要：优先取 stage / operator / detail 三个语义字段 */
function traceSummary(traceData) {
  if (!traceData) return '—'
  try {
    const o = typeof traceData === 'string' ? JSON.parse(traceData) : traceData
    const parts = [o.stage, o.operator, o.detail, o.opinion].filter(Boolean)
    if (parts.length) return parts.join(' · ')
    return JSON.stringify(o).slice(0, 80)
  } catch {
    return String(traceData).slice(0, 80)
  }
}

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size }
  if (query.instanceId) params.instanceId = query.instanceId
  try {
    const data = await auditApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function openChain(instanceId) {
  chain.queryInstance = instanceId ? String(instanceId) : chain.queryInstance
  chain.data = null
  chain.visible = true
  if (instanceId) runChain()
}

async function runChain() {
  const key = chain.queryInstance
  if (!key) {
    ElMessage.warning('请输入审批实例 ID')
    return
  }
  chain.loading = true
  chain.data = null
  try {
    // 后端 data 是 JSON 字符串（聚合审批/抽查/预警/审计），这里二次解析成结构化对象
    const raw = await auditApi.chain(key)
    chain.data = typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (e) {
    ElMessage.error('追溯失败：' + (e?.message || '数据解析异常'))
  } finally {
    chain.loading = false
  }
}

onMounted(load)
</script>

<style scoped>
.filter-inline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.trace-line {
  font-size: 13px;
  color: #303133;
}
.text-muted {
  color: #909399;
}

/* 链路弹窗 */
.chain-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 14px;
}
.chain-block {
  margin-bottom: 16px;
}
.cb-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.cb-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: #eaf0ff;
  color: #2f6bff;
  font-size: 12px;
}
.cb-list {
  display: grid;
  gap: 6px;
}
.cb-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 13px;
}
.cb-name {
  font-weight: 600;
  color: #303133;
}
.cb-text {
  flex: 1;
  min-width: 0;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cb-empty {
  padding: 10px 12px;
  background: #fafbfc;
  border: 1px dashed #e4e7ed;
  border-radius: 6px;
  color: #909399;
  font-size: 13px;
}
.cb-loading {
  padding: 40px 0;
  text-align: center;
  color: #909399;
}
</style>
