<template>
  <div class="ent">
    <el-card>
      <template #header>
        <div class="head">
          <div class="ent-title">企业信息管理</div>
          <el-button type="primary" plain :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </template>
      <p class="text-muted ent-desc">查看企业基本信息与法定代表人信息(只读)。如需变更法定代表人,请办理「企业变更一件事」登记,审批通过后自动同步</p>
    </el-card>

    <el-row :gutter="16" v-loading="loading">
      <el-col :span="14">
        <el-card shadow="hover" class="info-card">
          <template #header>
            <span class="card-title">企业基本信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="企业名称">{{ info?.enterpriseName }}</el-descriptions-item>
            <el-descriptions-item label="统一社会信用代码">{{ info?.creditCode }}</el-descriptions-item>
            <el-descriptions-item label="企业类型">{{ info?.enterpriseType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="成立日期">{{ info?.establishDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册地址">{{ info?.registerAddress }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ info?.contactPhone }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="hover" class="info-card">
          <template #header>
            <div class="legal-head">
              <span class="card-title">法定代表人信息</span>
              <el-tag size="small" type="info">登记数据 · 只读</el-tag>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="法定代表人">{{ info?.legalPerson || '-' }}</el-descriptions-item>
            <el-descriptions-item label="身份证号">{{ maskIdCard(info?.legalPersonIdCard) }}</el-descriptions-item>
          </el-descriptions>
          <el-alert type="warning" :closable="false" show-icon class="mt-12">
            <template #title>法定代表人信息由登记机关核发,企业不得自行修改。如需变更,请办理变更登记。</template>
          </el-alert>
          <div class="legal-actions">
            <el-button type="primary" :icon="Edit" @click="goChange">去办理变更登记</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Edit } from '@element-plus/icons-vue'
import { getEnterpriseInfo, type EnterpriseInfo } from '@/api'
import { resolveApplyRoute } from '@/utils/matterLink'

const router = useRouter()
const loading = ref(false)
const info = ref<EnterpriseInfo | null>(null)

async function load() {
  loading.value = true
  try {
    info.value = await getEnterpriseInfo()
  } finally {
    loading.value = false
  }
}

/** 身份证号脱敏:显示前6后4,中间用 * */
function maskIdCard(card?: string | null): string {
  if (!card || card.length < 10) return card || '-'
  return card.substring(0, 6) + '********' + card.substring(card.length - 4)
}

/** 跳转企业变更一件事(按编码解析),预设法定代表人变更 */
async function goChange() {
  const route = await resolveApplyRoute('QY-KB-002', { changeType: '法定代表人变更' })
  if (route) router.push(route)
  else router.push('/matters')
}

onMounted(load)
</script>

<style scoped>
.ent {
  display: grid;
  gap: 16px;
  padding: 4px 0 24px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.ent-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}
.ent-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
}
.info-card {
  margin-bottom: 16px;
}
.card-title {
  font-weight: 600;
}
.legal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mt-12 {
  margin-top: 12px;
}
.legal-actions {
  margin-top: 16px;
  text-align: center;
}
</style>
