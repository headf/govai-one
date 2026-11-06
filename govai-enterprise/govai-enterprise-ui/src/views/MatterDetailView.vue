<template>
  <div v-if="matter" class="md">
    <!-- ① 事项头卡 -->
    <section class="card hero">
      <div class="hero-main">
        <span class="hero-cat">{{ matter.categoryName }}</span>
        <h1 class="hero-name">{{ matter.matterName }}</h1>
        <div class="hero-meta">
          <span>{{ matter.department }}</span>
          <span class="sep">·</span>
          <span>事项编码 {{ matter.matterCode }}</span>
          <span class="sep">·</span>
          <span>承诺办结 {{ matter.deadlineDays }} 个工作日</span>
        </div>
      </div>
      <div class="hero-actions">
        <el-button v-if="fromPlan" plain @click="backToPlan">
          <el-icon><Back /></el-icon>&nbsp;返回智能办事规划
        </el-button>
        <el-button type="primary" size="large" @click="goApply()">在线申报</el-button>
      </div>
    </section>

    <!-- ② 描述 / 指南 双卡 -->
    <section class="grid">
      <div class="card blk">
        <div class="blk-title">事项描述</div>
        <p class="blk-body">{{ matter.description || '暂无' }}</p>
      </div>
      <div class="card blk">
        <div class="blk-title">办理指南</div>
        <p class="blk-body">{{ matter.guideContent || '暂无' }}</p>
      </div>
    </section>

    <!-- ③ 申报条件 -->
    <section class="card blk">
      <div class="blk-title">申报条件</div>
      <ul v-if="matter.conditions?.length" class="cond-list">
        <li v-for="(c, i) in matter.conditions" :key="i">
          <el-icon class="ok"><CircleCheck /></el-icon>{{ c }}
        </li>
      </ul>
      <p v-else class="blk-body text-muted">无特殊申报条件</p>
    </section>

    <!-- ④ 申请材料清单 -->
    <section class="card blk">
      <div class="blk-title">申请材料清单</div>
      <div v-if="materials.length" class="mat-list">
        <div v-for="(m, i) in materials" :key="i" class="mat-row">
          <span class="mat-name">{{ m.name }}</span>
          <el-tag size="small" effect="plain" :type="m.autofill ? 'success' : 'info'">
            {{ m.autofill ? '电子证照自动带出' : '上传 / 拍照' }}
          </el-tag>
        </div>
      </div>
      <p v-else class="blk-body text-muted">本事项无需额外材料</p>
    </section>

    <!-- ⑤ 办理流程 -->
    <section class="card blk">
      <div class="blk-title">办理流程</div>
      <el-steps :active="0" align-center class="flow-steps">
        <el-step v-for="(s, i) in matter.processSteps" :key="i" :title="s" />
      </el-steps>
    </section>

    <!-- 底部 CTA -->
    <div class="cta-row">
      <el-button type="primary" size="large" @click="goApply()">立即申报</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, CircleCheck } from '@element-plus/icons-vue'
import { getMatter } from '@/api'
import { planReturnQuery } from '@/utils/planReturn'
import type { MatterVO } from '@/types'

const route = useRoute()
const router = useRouter()
const matter = ref<MatterVO>()

// 从智能办事规划跳入办理:是否显示"返回规划"快捷入口
const fromPlan = computed(() => route.query.from === 'plan')

function backToPlan() {
  const pid = route.query.planId
  router.push(pid
    ? { path: '/matters/plan', query: { from: 'plan', planId: String(pid) } }
    : planReturnQuery())
}

// 规划场景下,点击"在线申报"把来源一并透传;非规划来源(办事大厅直进)不携带,避免误显"返回规划"
function goApply() {
  if (matter.value) {
    const query: Record<string, string> = {}
    if (fromPlan.value) {
      query.from = 'plan'
      if (route.query.planId) query.planId = String(route.query.planId)
    }
    router.push({ path: `/matters/${matter.value.id}/apply`, query })
  }
}

// 材料列表:名称 + 是否免证办(材料名含"免证办"或匹配电子证照关键词)
const materials = computed(() => {
  if (!matter.value?.materials) return []
  const autoKeywords = ['营业执照', '许可', '资质']
  return matter.value.materials.map((name) => {
    const autofill = autoKeywords.some((k) => name.includes(k))
    return { name, autofill }
  })
})

onMounted(async () => {
  matter.value = await getMatter(Number(route.params.id))
})
</script>

<style scoped>
.md {
  display: grid;
  gap: 16px;
  padding: 4px 0 32px;
}
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 22px 24px;
}

/* ① 头卡 */
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  padding: 26px 28px;
  background:
    radial-gradient(480px 200px at 100% -20%, rgba(47, 107, 255, 0.08), transparent 60%),
    #fff;
}
.hero-cat {
  display: inline-block;
  font-size: 12px;
  color: #2f6bff;
  background: #eaf0ff;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}
.hero-name {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #111827;
}
.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
}
.hero-meta .sep {
  color: #d1d5db;
}
.hero-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

/* ② 双卡 */
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.blk-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 4px solid #2f6bff;
}
.blk-body {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #4b5563;
}

/* ③ 申报条件 */
.cond-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}
.cond-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  color: #374151;
  line-height: 1.7;
}
.cond-list .ok {
  margin-top: 4px;
  color: #16a34a;
  flex: none;
}

/* ④ 材料清单 */
.mat-list {
  display: grid;
  gap: 2px;
}
.mat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 8px;
  border-radius: 8px;
}
.mat-row:hover {
  background: #f6f8fb;
}
.mat-name {
  font-size: 14px;
  color: #1f2937;
}

/* ⑤ 流程 */
.flow-steps {
  padding: 10px 0 4px;
}

/* CTA */
.cta-row {
  text-align: center;
  padding: 4px 0;
}

@media (max-width: 800px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
