<template>
  <div class="packages">
    <el-card>
      <template #header>
        <div class="list-head">
          <div class="list-sub">将多个关联事项打包,一次申报、一表申请、一套材料、并联办理;提交后自动拆分为多个事项独立审批</div>
        </div>
      </template>
      <el-row :gutter="16" v-loading="loading">
      <el-col v-for="p in list" :key="p.id" :span="12">
        <el-card shadow="hover" class="pkg-card">
          <div class="pkg-head">
            <span class="pkg-name">{{ p.packageName }}</span>
            <el-tag size="small" type="warning">一件事</el-tag>
          </div>
          <div class="text-muted pkg-desc">{{ p.description }}</div>
          <div class="pkg-items">
            <span class="text-muted">包含事项:</span>
            <el-tag v-for="it in p.items" :key="it.matterId" size="small" effect="plain" class="mr-8">
              {{ it.matterName }}
            </el-tag>
          </div>
          <div class="text-muted mt-8">合计承诺时限:{{ p.totalDeadlineDays }} 个工作日</div>
          <div class="pkg-foot">
            <el-button type="primary" @click="router.push(`/packages/${p.id}/apply`)">在线办理(一表申报)</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!list.length" description="暂无套餐" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listPackages } from '@/api'
import type { PackageVO } from '@/types'

const router = useRouter()
const list = ref<PackageVO[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    list.value = await listPackages()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.packages {
  display: grid;
  gap: 16px;
  padding: 4px 0 24px;
}
.list-head {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.list-sub {
  font-size: 12px;
  color: #9ca3af;
}
.mt-8 {
  margin-top: 8px;
}
.mr-8 {
  margin-right: 8px;
}
.pkg-card {
  margin-bottom: 16px;
  min-height: 220px;
  display: flex;
  flex-direction: column;
}
.pkg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.pkg-name {
  font-size: 17px;
  font-weight: 600;
}
.pkg-desc {
  min-height: 40px;
  margin-bottom: 12px;
}
.pkg-items {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.pkg-foot {
  margin-top: auto;
  padding-top: 14px;
}
</style>
