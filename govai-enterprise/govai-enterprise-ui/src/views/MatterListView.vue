<template>
  <div class="hall">
    <el-card class="filter-card">
      <template #header>
        <div class="hall-head">
          <div>
            <div class="hall-sub">共 {{ total }} 条可办事项 · 按需选择，在线办理</div>
          </div>
          <div class="search-row">
            <el-input
              v-model="query.keyword"
              placeholder="搜索事项名称 / 事项编码，如：食品经营许可"
              clearable
              style="width: 300px"
              @keyup.enter="load"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="load">搜索</el-button>
          </div>
        </div>
      </template>
      <el-radio-group v-model="query.categoryId" @change="load">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button v-for="c in categories" :key="c.id" :value="c.id">{{ c.categoryName }}</el-radio-button>
      </el-radio-group>
    </el-card>

    <div class="matter-grid" v-loading="loading">
      <button
        v-for="m in list"
        :key="m.id"
        class="matter-card"
        @click="router.push(`/matters/${m.id}`)"
      >
        <div class="matter-top">
          <span class="matter-name">{{ m.matterName }}</span>
          <el-icon class="matter-go"><ArrowRight /></el-icon>
        </div>
        <el-tag size="small" type="primary" effect="plain" style="margin: 8px 0 6px">{{ m.categoryName }}</el-tag>
        <div class="matter-desc">{{ truncate(m.description || '', 54) }}</div>
        <div class="matter-meta">
          <span class="text-muted">{{ m.department }}</span>
          <span class="text-muted">承诺 {{ m.deadlineDays }} 个工作日</span>
        </div>
      </button>
    </div>

    <el-empty v-if="!list.length && !loading" description="未找到相关事项" />
    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[6, 9, 18]"
        layout="total, sizes, prev, pager, next"
        @change="load"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { listMatters, listCategories } from '@/api'
import type { MatterVO, Category } from '@/types'

const router = useRouter()
const list = ref<MatterVO[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive<{ keyword: string; categoryId?: number; page: number; size: number }>({
  keyword: '',
  categoryId: undefined,
  page: 1,
  size: 9
})

function truncate(s: string, n: number) {
  return s.length > n ? s.slice(0, n) + '...' : s
}

async function load() {
  loading.value = true
  try {
    const res = await listMatters({
      keyword: query.keyword || undefined,
      categoryId: query.categoryId,
      page: query.page,
      size: query.size
    })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  categories.value = await listCategories()
  await load()
})
</script>

<style scoped>
.hall {
  display: grid;
  gap: 16px;
  padding: 4px 0 24px;
}
.filter-card {
  border-radius: 14px;
}
.hall-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  width: 100%;
}
.hall-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #9ca3af;
}
.search-row {
  display: flex;
  gap: 8px;
}
.matter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
.matter-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 18px 20px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
}
.matter-card:hover {
  border-color: #c1d3ff;
  box-shadow: 0 2px 4px rgba(16, 24, 40, 0.04), 0 8px 20px rgba(47, 107, 255, 0.12);
  transform: translateY(-2px);
}
.matter-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.matter-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.45;
}
.matter-go {
  color: #c7cbd1;
  transition: color 0.2s;
}
.matter-card:hover .matter-go {
  color: #2f6bff;
}
.matter-desc {
  flex: 1;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.7;
  min-height: 44px;
}
.matter-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  font-size: 12px;
}
.pager {
  display: flex;
  justify-content: center;
}
</style>
