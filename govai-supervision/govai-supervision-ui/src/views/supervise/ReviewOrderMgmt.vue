<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ total }} 条差评工单 · 来自群众端 / 企业端的好差评上报</span>
        <div class="filter-inline">
          <el-select v-model="query.handleStatus" placeholder="处置状态" clearable style="width: 130px" @change="load">
            <el-option label="待处置" value="PENDING" />
            <el-option label="已处置" value="HANDLED" />
          </el-select>
          <el-select v-model="query.source" placeholder="来源" clearable style="width: 120px" @change="load">
            <el-option label="群众端" value="CITIZEN" />
            <el-option label="企业端" value="ENTERPRISE" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
        </div>
      </div>
    </template>

    <!-- 列精简：评价星级/文字/标签合并为一列，整改意见移入弹窗，时间用短格式 —— 保证一屏不横滚 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="reviewNo" label="工单号" width="150" show-overflow-tooltip />
      <el-table-column label="来源" width="76">
        <template #default="{ row }">
          <el-tag size="small" effect="plain" :type="row.source === 'ENTERPRISE' ? 'primary' : 'success'">
            {{ row.source === 'ENTERPRISE' ? '企业' : '群众' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applicantName" label="评价人" width="88" show-overflow-tooltip />
      <el-table-column prop="matterName" label="申报事项" min-width="150" show-overflow-tooltip />
      <el-table-column label="差评内容" min-width="230">
        <template #default="{ row }">
          <div class="cell-review">
            <el-rate :model-value="row.starRating" disabled size="small" />
            <span class="rv-text">{{ row.content || '（未填写文字）' }}</span>
            <span v-if="row.tags" class="rv-tags">{{ row.tags }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="上报时间" width="100">
        <template #default="{ row }">{{ shortTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="78">
        <template #default="{ row }">
          <el-tag :type="row.handleStatus === 'HANDLED' ? 'info' : 'danger'" size="small">
            {{ row.handleStatus === 'HANDLED' ? '已处置' : '待处置' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="76" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.handleStatus === 'PENDING'" link type="primary" @click="openHandle(row)">处置</el-button>
          <el-button v-else link type="info" @click="openView(row)">查看</el-button>
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

    <!-- 处置 / 查看 共用弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'handle' ? '处置差评工单' : '差评工单详情'"
      width="520px"
    >
      <el-form label-width="90px">
        <el-form-item label="工单号">
          <span>{{ dialog.row.reviewNo }}</span>
        </el-form-item>
        <el-form-item label="来源 / 评价人">
          <span>{{ dialog.row.source === 'ENTERPRISE' ? '企业端' : '群众端' }} · {{ dialog.row.applicantName }}</span>
        </el-form-item>
        <el-form-item label="申报事项">
          <span>{{ dialog.row.matterName }}</span>
        </el-form-item>
        <el-form-item label="差评内容">
          <div class="review-quote">
            <el-rate :model-value="dialog.row.starRating" disabled size="small" />
            <div>{{ dialog.row.content || '（未填写文字）' }}</div>
            <div v-if="dialog.row.tags" class="text-muted">{{ dialog.row.tags }}</div>
          </div>
        </el-form-item>

        <!-- 处置模式：填整改措施 -->
        <template v-if="dialog.mode === 'handle'">
          <el-form-item label="整改措施">
            <el-select
              v-model="dialog.opinion"
              placeholder="选择整改措施 / 填写意见"
              style="width: 100%"
              allow-create
              filterable
              default-first-option
            >
              <el-option label="已约谈经办人员并完成整改" value="已约谈经办人员并完成整改" />
              <el-option label="已优化办事流程，压缩办理时限" value="已优化办事流程，压缩办理时限" />
              <el-option label="已加强窗口人员业务培训" value="已加强窗口人员业务培训" />
              <el-option label="已电话回访评价人并说明情况" value="已电话回访评价人并说明情况" />
            </el-select>
          </el-form-item>
          <el-form-item label="补充说明">
            <el-input v-model="dialog.detail" type="textarea" :rows="2" placeholder="补充具体说明（可选）" />
          </el-form-item>
        </template>

        <!-- 查看模式：展示整改结果 -->
        <template v-else>
          <el-form-item label="整改意见">
            <span>{{ dialog.row.handleOpinion || '-' }}</span>
          </el-form-item>
          <el-form-item label="处置人">
            <span>{{ dialog.row.handlerName || '-' }}</span>
          </el-form-item>
          <el-form-item label="处置时间">
            <span>{{ fmtTime(dialog.row.handleTime) }}</span>
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">{{ dialog.mode === 'handle' ? '取消' : '关闭' }}</el-button>
        <el-button v-if="dialog.mode === 'handle'" type="primary" :loading="saving" @click="onConfirm">确认处置</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { reviewOrderApi } from '../../api/modules'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ page: 1, size: 10, handleStatus: '', source: '' })
const dialog = reactive({
  visible: false,
  mode: 'handle',     // handle 处置 / view 查看
  row: {},
  opinion: '',
  detail: ''
})

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size }
  if (query.handleStatus) params.handleStatus = query.handleStatus
  if (query.source) params.source = query.source
  try {
    const data = await reviewOrderApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function openHandle(row) {
  dialog.mode = 'handle'
  dialog.row = row
  dialog.opinion = ''
  dialog.detail = ''
  dialog.visible = true
}

function openView(row) {
  dialog.mode = 'view'
  dialog.row = row
  dialog.visible = true
}

async function onConfirm() {
  const full = [dialog.opinion, dialog.detail].filter(Boolean).join('；')
  if (!full) {
    ElMessage.warning('请填写整改措施')
    return
  }
  saving.value = true
  try {
    await reviewOrderApi.handle(dialog.row.id, full)
    ElMessage.success('已完成整改处置')
    dialog.visible = false
    load()
  } finally {
    saving.value = false
  }
}

/** 列表时间短格式：MM-DD HH:mm */
function shortTime(t) {
  if (!t) return '-'
  const s = String(t).replace('T', ' ')
  return s.length >= 16 ? s.slice(5, 16) : s
}

/** 弹窗内完整时间：YYYY-MM-DD HH:mm */
function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
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
/* 评价列：星级 / 文字 / 标签 竖向紧凑排列 */
.cell-review {
  line-height: 1.4;
}
.rv-text {
  display: block;
  color: #303133;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rv-tags {
  display: block;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.review-quote {
  width: 100%;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  padding: 10px 12px;
  line-height: 1.7;
}
.text-muted {
  color: #909399;
  font-size: 12px;
}
</style>
