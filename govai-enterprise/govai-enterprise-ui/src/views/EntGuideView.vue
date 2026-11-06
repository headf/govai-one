<template>
  <div class="guide-page">
    <el-alert
      title="一件事 · 跨部门并联办理导引:按经营生命周期(开办→变更→注销)推荐组合办理路径,系统将串联市场监管局、税务、人社等多部门环节。"
      type="info"
      :closable="false"
      show-icon
    />
    <div class="scene-grid">
      <el-card v-for="s in scenes" :key="s.key" class="scene-card" :style="{ borderTop: `4px solid ${s.color}` }">
        <div class="scene-head">
          <div class="scene-badge" :style="{ background: s.color }">{{ s.icon }}</div>
          <div>
            <h3 style="margin: 0">{{ s.title }}</h3>
            <div class="scene-desc">{{ s.desc }}</div>
          </div>
        </div>
        <el-divider />
        <div class="step-list">
          <div v-for="(st, i) in s.steps" :key="i" class="step">
            <span class="step-no">{{ i + 1 }}</span>
            <span class="step-txt">{{ st }}</span>
          </div>
        </div>
        <div class="scene-acts">
          <el-button type="primary" plain size="small" @click="go(s.primary)">{{ s.primaryText || '开始办理' }}</el-button>
          <el-button size="small" @click="go('/matters/plan')">智能办事规划</el-button>
          <el-button size="small" @click="go('/matters')">办事大厅</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const scenes = [
  {
    key: 'open',
    title: '企业开办',
    icon: '开',
    color: '#2f6bff',
    desc: '营业执照申领 + 刻章备案 + 税务登记 + 社保开户',
    steps: ['名称/住所预审', '经营范围与登记信息填报', '市场监管局核准并签发营业执照', '自动并联税务登记/刻章/社保(套餐办理)'],
    primary: '/packages',
    primaryText: '去一件事套餐并联办理'
  },
  {
    key: 'change',
    title: '企业变更',
    icon: '变',
    color: '#e6a23c',
    desc: '住所/经营范围/股东股权变更等',
    steps: ['选择变更类型(住所/经营范围/股权等)', '按证照类型匹配对应变更事项', '材料与证照信息复用', '审批通过后同步更新证照'],
    primary: '/matters',
    primaryText: '去办事大厅选变更事项'
  },
  {
    key: 'close',
    title: '企业注销',
    icon: '销',
    color: '#f56c6c',
    desc: '税务注销 → 社保注销 → 营业执照注销',
    steps: ['税务清算并取得注销证明', '社保账户减员/注销', '营业执照缴销与注销登记', '证照状态置为已注销(保留历史)'],
    primary: '/matters',
    primaryText: '去办事大厅办注销'
  }
]

function go(path: string) {
  router.push(path)
}
</script>

<style scoped>
.guide-page {
  display: grid;
  gap: 16px;
}
.scene-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
@media (max-width: 1100px) {
  .scene-grid { grid-template-columns: 1fr; }
}
.scene-card {
  transition: box-shadow 0.2s;
}
.scene-head {
  display: flex;
  gap: 12px;
  align-items: center;
}
.scene-badge {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.scene-desc {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.step-list {
  display: grid;
  gap: 10px;
}
.step {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 13px;
}
.step-no {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #eef2ff;
  color: #2f6bff;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.step-txt {
  line-height: 1.5;
}
.scene-acts {
  margin-top: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
