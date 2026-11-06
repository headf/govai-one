<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ruleApi } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const ruleSetFormRef = ref<FormInstance>()

const ruleSet = reactive({
  id: undefined as number | undefined,
  ruleSetName: '',
  matterType: '',
  status: 1,
  logicType: 'AND',
  description: '',
})

const ruleSetRules: FormRules = {
  ruleSetName: [{ required: true, message: '请填写规则集名称', trigger: 'blur' }],
  matterType: [{ required: true, message: '请选择事项类型', trigger: 'change' }],
}

// 事项类型选项（与流程模板的 matterType 对应）
const matterTypeOptions = [
  { label: '新办企业登记', value: 'ENTERPRISE_REGISTER' },
  { label: '食品经营许可', value: 'FOOD_PERMIT' },
  { label: '建筑工程施工许可', value: 'CONSTRUCTION_PERMIT' },
  { label: '个体工商户登记', value: 'INDIVIDUAL_REGISTER' },
  { label: '其他', value: 'OTHER' },
]

const items = ref<any[]>([])

const ruleTypeOptions = [
  { label: '数值比较', value: 'NUMBER' },
  { label: '文本匹配', value: 'TEXT' },
  { label: '日期判断', value: 'DATE' },
  { label: '证照查询', value: 'LICENSE' },
]

const operatorOptions: Record<string, string[]> = {
  NUMBER: ['GT', 'LT', 'EQ', 'BETWEEN'],
  TEXT: ['EQ', 'CONTAINS'],
  DATE: ['GT', 'LT', 'EQ', 'BETWEEN'],
  LICENSE: ['EQ'],
}

const operatorLabel: Record<string, string> = {
  GT: '大于',
  LT: '小于',
  EQ: '等于',
  CONTAINS: '包含',
  BETWEEN: '区间',
}

// 测试相关
const testDialog = reactive({
  visible: false,
  formData: '',
  result: null as any,
  testing: false,
})

function addItem() {
  items.value.push({
    id: undefined,
    ruleSetId: undefined,
    ruleType: 'NUMBER',
    fieldName: '',
    operator: 'GT',
    compareValue: '',
    logicOperator: 'AND',
    sortOrder: items.value.length + 1,
    errorMessage: '',
  })
}

function removeItem(index: number) {
  items.value.splice(index, 1)
  items.value.forEach((item, i) => (item.sortOrder = i + 1))
}

function onRuleTypeChange(row: any) {
  const ops = operatorOptions[row.ruleType] || []
  if (!ops.includes(row.operator)) {
    row.operator = ops[0] || ''
  }
}

async function loadDetail(id: number) {
  loading.value = true
  try {
    const res = await ruleApi.detail(id)
    if (res.data?.ruleSet) {
      Object.assign(ruleSet, res.data.ruleSet)
    }
    if (res.data?.items) {
      items.value = res.data.items
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  // 表单校验
  const valid = await ruleSetFormRef.value?.validate().catch(() => false)
  if (!valid) return
  // 规则项校验
  if (items.value.some((it) => !it.fieldName?.trim())) {
    ElMessage.warning('请填写所有规则项的字段名')
    return
  }
  saving.value = true
  try {
    await ruleApi.saveSet({
      ruleSet: { ...ruleSet },
      items: items.value,
    })
    ElMessage.success('保存成功')
    router.push('/rule/list')
  } finally {
    saving.value = false
  }
}

async function runTest() {
  let formData: any
  try {
    formData = JSON.parse(testDialog.formData)
  } catch {
    ElMessage.warning('请输入有效的JSON格式表单数据')
    return
  }
  testDialog.testing = true
  try {
    const res = await ruleApi.testRule(ruleSet.id!, formData)
    testDialog.result = res.data
  } finally {
    testDialog.testing = false
  }
}

function openTest() {
  if (!ruleSet.id) {
    ElMessage.warning('请先保存规则集')
    return
  }
  testDialog.visible = true
  testDialog.formData = '{}'
  testDialog.result = null
}

onMounted(() => {
  const id = route.params.id as string
  if (id) {
    loadDetail(Number(id))
  }
})
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- 规则集信息 -->
    <el-card class="section-card">
      <template #header><span class="section-title">规则集信息</span></template>
      <el-form ref="ruleSetFormRef" :model="ruleSet" :rules="ruleSetRules" label-width="120px" inline>
        <el-form-item label="规则集名称" prop="ruleSetName">
          <el-input v-model="ruleSet.ruleSetName" placeholder="如 新办企业基本规则" maxlength="50" show-word-limit style="width: 240px" />
        </el-form-item>
        <el-form-item label="事项类型" prop="matterType">
          <el-select v-model="ruleSet.matterType" placeholder="请选择事项类型" style="width: 200px">
            <el-option v-for="opt in matterTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="逻辑类型">
          <el-radio-group v-model="ruleSet.logicType">
            <el-radio value="AND">AND (全部满足)</el-radio>
            <el-radio value="OR">OR (任一满足)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="ruleSet.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="ruleSet.description" placeholder="规则集描述" maxlength="200" style="width: 300px" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 规则项列表 -->
    <el-card class="section-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span class="section-title">规则项列表</span>
          <div>
            <el-button type="warning" size="small" @click="openTest" :disabled="!ruleSet.id">测试规则</el-button>
            <el-button type="primary" size="small" @click="addItem">+ 添加规则</el-button>
          </div>
        </div>
      </template>

      <el-table :data="items" border style="width: 100%">
        <el-table-column label="序号" width="60" type="index" />
        <el-table-column label="规则类型" width="140">
          <template #default="{ row }">
            <el-select v-model="row.ruleType" size="small" @change="onRuleTypeChange(row)">
              <el-option v-for="opt in ruleTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="字段名" width="160">
          <template #default="{ row }">
            <el-input v-model="row.fieldName" placeholder="如 capital" size="small" maxlength="30" />
          </template>
        </el-table-column>
        <el-table-column label="操作符" width="120">
          <template #default="{ row }">
            <el-select v-model="row.operator" size="small">
              <el-option v-for="op in (operatorOptions[row.ruleType] || [])" :key="op" :label="operatorLabel[op] || op" :value="op" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="比较值" width="180">
          <template #default="{ row }">
            <el-input v-model="row.compareValue" placeholder="如 50万 或 1,100" size="small" maxlength="50" />
          </template>
        </el-table-column>
        <el-table-column label="错误提示" width="200">
          <template #default="{ row }">
            <el-input v-model="row.errorMessage" placeholder="不通过时提示" size="small" maxlength="100" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button size="small" link type="danger" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 底部操作 -->
    <div style="text-align: center; padding: 20px">
      <el-button @click="router.push('/rule/list')">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>

    <!-- 测试弹窗 -->
    <el-dialog v-model="testDialog.visible" title="规则测试" width="700px">
      <el-form label-width="100px">
        <el-form-item label="表单数据JSON">
          <el-input v-model="testDialog.formData" type="textarea" :rows="6" placeholder='{"capital":"50万","businessType":"餐饮"}' />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="testDialog.testing" @click="runTest">执行测试</el-button>
        </el-form-item>
      </el-form>

      <div v-if="testDialog.result" style="margin-top: 16px">
        <el-divider>测试结果</el-divider>
        <el-result
          :icon="testDialog.result.result === 'PASS' ? 'success' : 'warning'"
          :title="testDialog.result.result === 'PASS' ? '规则通过' : '规则未通过'"
        />
        <el-table :data="testDialog.result.items" border size="small">
          <el-table-column prop="fieldName" label="字段" />
          <el-table-column prop="ruleType" label="类型" width="100">
            <template #default="{ row }">
              {{ ruleTypeOptions.find((o) => o.value === row.ruleType)?.label || row.ruleType }}
            </template>
          </el-table-column>
          <el-table-column prop="operator" label="操作符" width="80">
            <template #default="{ row }">
              {{ operatorLabel[row.operator] || row.operator }}
            </template>
          </el-table-column>
          <el-table-column label="结果" width="80">
            <template #default="{ row }">
              <el-tag :type="row.passed ? 'success' : 'danger'" size="small">
                {{ row.passed ? '通过' : '未过' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="errorMessage" label="错误信息" />
        </el-table>
        <div v-if="testDialog.result.errors?.length" style="margin-top: 12px">
          <el-alert v-for="(err, i) in testDialog.result.errors" :key="i" :title="err" type="error" :closable="false" style="margin-bottom: 4px" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>
