<template>
  <div class="evals">
    <el-alert
      title="我的好差评:办结后提交的满意度评价记录,可随时回顾。若为差评,系统已同步至整改反馈。"
      type="info"
      :closable="false"
      show-icon
    />
    <el-card>
      <div v-for="e in list" :key="e.id" class="eval-card">
        <div class="eval-head">
          <div>
            <span class="eval-matter">{{ e.matterName || '-' }}</span>
            <span class="text-muted ml-8">{{ e.recordCode }}</span>
          </div>
          <span class="text-muted">{{ fmt(e.createTime) }}</span>
        </div>
        <div class="eval-body">
          <el-rate :model-value="e.starRating" disabled show-score score-template="{value} 星" />
          <el-tag :type="e.starRating <= 2 ? 'danger' : e.starRating === 3 ? 'warning' : 'success'" size="small" class="ml-8">
            {{ starText(e.starRating) }}
          </el-tag>
        </div>
        <div v-if="e.tags?.length" class="eval-tags">
          <el-tag v-for="t in e.tags" :key="t" size="small" effect="plain" class="mr-8">{{ t }}</el-tag>
        </div>
        <div v-if="e.content" class="text-muted eval-content">{{ e.content }}</div>
      </div>
      <el-empty v-if="!list.length" description="您还没有评价记录,办结申报后即可评价" />
      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { listMyEvaluations } from '@/api'
import type { Evaluation } from '@/types'

const list = ref<Evaluation[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

function starText(n: number) {
  return { 1: '非常差', 2: '差', 3: '一般', 4: '满意', 5: '非常满意' }[n] || ''
}
function fmt(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}
async function load() {
  const res = await listMyEvaluations({ ...query })
  list.value = res.list
  total.value = res.total
}
onMounted(load)
</script>

<style scoped>
.evals {
  display: grid;
  gap: 16px;
  padding: 4px 0 24px;
}
.eval-card {
  border-bottom: 1px dashed #ebeef5;
  padding: 14px 4px;
}
.eval-card:last-child {
  border-bottom: none;
}
.eval-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.eval-matter {
  font-weight: 600;
  color: #303133;
}
.eval-body {
  display: flex;
  align-items: center;
  margin-top: 10px;
}
.eval-tags {
  margin-top: 10px;
}
.eval-content {
  margin-top: 8px;
  font-size: 13px;
}
.ml-8 {
  margin-left: 8px;
}
.mr-8 {
  margin-right: 8px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
