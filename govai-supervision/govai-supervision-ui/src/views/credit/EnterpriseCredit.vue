<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <span style="font-size: 13px; color: #909399">共 {{ total }} 家企业</span>
        <div class="filter-inline">
          <el-input v-model="query.enterpriseId" placeholder="企业ID" clearable style="width: 150px" @keyup.enter="load" />
          <el-select v-model="query.level" placeholder="信用等级" clearable style="width: 130px" @change="load">
            <el-option v-for="l in ['A', 'B', 'C', 'D']" :key="l" :label="l" :value="l" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openScore()">企业评分</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="enterpriseName" label="企业名称" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.enterpriseName || '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="creditScore" label="信用评分" min-width="120" />
      <el-table-column prop="creditLevel" label="等级" width="90">
        <template #default="{ row }">
          <el-tag :type="levelMap[row.creditLevel]" size="small">{{ row.creditLevel }}级</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="dimensionScores" label="维度得分" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="dims">{{ row.dimensionScores || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ (row.updateTime || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openMembers(row)">成员</el-button>
          <el-button link type="primary" @click="openScore(row)">评分</el-button>
          <el-button link type="warning" @click="openAdjust(row)">调整</el-button>
          <el-popconfirm title="确认删除该企业评分？" @confirm="onRemove(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
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

    <!-- 企业成员(法人+经办人) -->
    <el-dialog v-model="member.visible" :title="member.enterpriseName ? `企业成员：${member.enterpriseName}` : '企业成员'" width="620px">
      <template v-if="member.data">
        <div class="legal-box">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="工商法人">
              {{ member.data.legalPerson || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="法人身份证号">
              {{ member.data.legalPersonIdCard || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <el-table :data="member.data.members || []" v-loading="member.loading" stripe size="small">
          <el-table-column prop="username" label="登录账号" min-width="120" />
          <el-table-column prop="realName" label="姓名" min-width="100" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column prop="role" label="身份" width="100">
            <template #default="{ row: m }">
              <el-tag size="small" :type="m.role === 'ADMIN' ? 'warning' : 'primary'">{{ m.role === 'ADMIN' ? '法人' : '经办人' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row: m }">
              <el-tag size="small" :type="m.status === 1 ? 'success' : 'info'">{{ m.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else-if="!member.loading" description="暂无成员信息(企业端无此企业档案?)" :image-size="60" />
    </el-dialog>

    <!-- 评分 -->
    <el-dialog v-model="score.visible" :title="score.isAdjust ? '调整评分' : '企业评分'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="企业ID">
          <el-input v-model.number="score.enterpriseId" :disabled="score.isAdjust" placeholder="enterpriseId" />
        </el-form-item>
        <el-form-item label="信用评分">
          <el-input-number v-model="score.creditScore" :min="0" :max="100" :precision="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="维度得分" v-if="!score.isAdjust">
          <el-input v-model="score.dimensionScores" type="textarea" :rows="2" placeholder='{"规范经营":20,"履约情况":30}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="score.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onScoreSubmit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { creditEnterpriseApi } from '../../api/modules'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ page: 1, size: 10, enterpriseId: '', level: '' })
const levelMap = { A: 'success', B: 'primary', C: 'warning', D: 'danger' }

const score = reactive({ visible: false, isAdjust: false, enterpriseId: null, creditScore: 0, dimensionScores: '' })

// 企业成员弹窗
const member = reactive({ visible: false, enterpriseId: null, enterpriseName: '', data: null, loading: false })

async function openMembers(row) {
  member.enterpriseId = row.enterpriseId
  member.enterpriseName = row.enterpriseName || ''
  member.data = null
  member.visible = true
  member.loading = true
  try {
    member.data = await creditEnterpriseApi.members(row.enterpriseId)
  } catch (e) {
    member.data = null
  } finally {
    member.loading = false
  }
}

async function load() {
  loading.value = true
  const params = { page: query.page, size: query.size }
  if (query.enterpriseId) params.enterpriseId = query.enterpriseId
  if (query.level) params.level = query.level
  try {
    const data = await creditEnterpriseApi.list(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function openScore(row) {
  score.isAdjust = false
  score.enterpriseId = row?.enterpriseId || ''
  score.creditScore = row?.creditScore || 0
  score.dimensionScores = row?.dimensionScores || ''
  score.visible = true
}

function openAdjust(row) {
  score.isAdjust = true
  score.enterpriseId = row.enterpriseId
  score.creditScore = row.creditScore
  score.visible = true
}

async function onScoreSubmit() {
  if (!score.enterpriseId) { ElMessage.warning('请输入企业ID'); return }
  saving.value = true
  try {
    if (score.isAdjust) {
      await creditEnterpriseApi.adjust({ enterpriseId: score.enterpriseId, score: score.creditScore })
      ElMessage.success('调整成功')
    } else {
      await creditEnterpriseApi.score({ targetId: score.enterpriseId, creditScore: score.creditScore, dimensionScores: score.dimensionScores })
      ElMessage.success('评分成功')
    }
    score.visible = false
    load()
  } finally {
    saving.value = false
  }
}

async function onRemove(row) {
  await creditEnterpriseApi.remove(row.enterpriseId)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.section-title {
  font-size: 16px;
  font-weight: 600;
  padding-left: 8px;
  border-left: 4px solid var(--govai-brand, #2f6bff);
  margin-bottom: 0;
}
.filter-inline { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.pager { margin-top: 16px; justify-content: flex-end; }
.dims { color: #666; font-size: 12px; }
</style>