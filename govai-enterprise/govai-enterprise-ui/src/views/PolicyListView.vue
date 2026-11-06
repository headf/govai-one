<template>
  <div class="policy-page">
    <el-alert
      title="政策找人 · 免申即享:系统根据企业画像自动匹配您可能符合的惠企政策,一键确认即可享受,无需提交申请。"
      type="info"
      :closable="false"
      show-icon
    />

    <div v-loading="loading" class="policy-list">
      <el-card v-for="p in list" :key="p.id" class="policy-card">
      <div class="policy-head">
        <div>
          <h3 style="margin: 0">{{ p.policyTitle }}</h3>
          <div class="mt-8">
            <el-tag size="small" type="warning">匹配度 {{ p.matchScore }}%</el-tag>
            <el-tag size="small" :type="p.isApplied ? 'success' : 'info'" class="ml-8">
              {{ p.isApplied ? '已确认享受' : '待确认' }}
            </el-tag>
          </div>
        </div>
        <el-button v-if="!p.isApplied" type="primary" @click="confirm(p)">
          确认享受(免申即享)
        </el-button>
        <div v-else class="claim-row">
          <el-tag type="success" effect="plain">已于 {{ p.applyTime }} 确认</el-tag>
          <el-button v-if="isClaimable(p)" type="warning" plain size="small" @click="goClaim(p)">
            去申报兑现
          </el-button>
        </div>
      </div>
      <el-divider />
      <div class="policy-content">{{ p.policyContent }}</div>
      <div class="text-muted policy-foot">数据来源:政策知识库 · 系统智能匹配</div>
    </el-card>
    <el-empty v-if="!list.length" description="暂无可享政策" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPolicies, confirmPolicy } from '@/api'
import { resolveApplyRoute } from '@/utils/matterLink'
import type { PolicyMatch } from '@/types'

const router = useRouter()
const list = ref<PolicyMatch[]>([])
const loading = ref(false)

// 免申即享确认后,仍需"申报兑现"的补贴类政策(标题含稳岗/补贴)
function isClaimable(p: PolicyMatch) {
  return p.isApplied && /稳岗|补贴|返还/.test(p.policyTitle)
}
// 跳转到对应补贴申报事项:稳岗返还 → SP-BZ-002(按编码动态解析,避免硬编码 id)
async function goClaim(p: PolicyMatch) {
  const code = /稳岗/.test(p.policyTitle) ? 'SP-BZ-002' : 'SP-BZ-002'
  const route = await resolveApplyRoute(code)
  if (route) router.push(route)
  else ElMessage.warning('暂未开通该项补贴的在线申报,可前往法人办事大厅选择')
}

async function confirm(p: PolicyMatch) {
  await ElMessageBox.confirm(`确认您的企业符合"${p.policyTitle}"条件并同意享受该政策权益吗?`, '免申即享确认', {
    type: 'success',
    confirmButtonText: '确认享受',
    cancelButtonText: '再想想'
  })
  const updated = await confirmPolicy(p.id)
  ElMessage.success('已确认,政策权益将同步生效')
  p.isApplied = updated.isApplied
  p.applyTime = updated.applyTime
}

async function load() {
  loading.value = true
  try {
    list.value = await listPolicies()
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.policy-page {
  display: grid;
  gap: 16px;
  padding: 4px 0 24px;
}
.policy-list {
  display: grid;
  gap: 16px;
}
.mt-8 {
  margin-top: 8px;
}
.ml-8 {
  margin-left: 8px;
}
.policy-card {
  border-left: 4px solid #2f6bff;
}
.policy-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.policy-content {
  color: #606266;
  line-height: 1.8;
}
.policy-foot {
  margin-top: 8px;
  font-size: 12px;
}
.claim-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
