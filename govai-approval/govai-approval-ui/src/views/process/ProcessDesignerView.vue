<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { processApi } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const templateFormRef = ref<FormInstance>()

const template = reactive({
  id: undefined as number | undefined,
  templateCode: '',
  templateName: '',
  version: '1.0',
  matterType: '',
  status: 1,
  description: '',
})

const templateRules: FormRules = {
  templateCode: [
    { required: true, message: '请填写模板编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '编码须以大写字母开头，仅含大写字母、数字、下划线', trigger: 'blur' },
  ],
  templateName: [{ required: true, message: '请填写模板名称', trigger: 'blur' }],
  matterType: [{ required: true, message: '请选择事项类型', trigger: 'change' }],
}

// 事项类型选项（与规则集的 matterType 对应）
const matterTypeOptions = [
  { label: '新办企业登记', value: 'ENTERPRISE_REGISTER' },
  { label: '食品经营许可', value: 'FOOD_PERMIT' },
  { label: '建筑工程施工许可', value: 'CONSTRUCTION_PERMIT' },
  { label: '个体工商户登记', value: 'INDIVIDUAL_REGISTER' },
  { label: '其他', value: 'OTHER' },
]

const nodes = ref<any[]>([])

// 节点类型选项
const nodeTypeOptions = [
  { label: '开始', value: 'START' },
  { label: '审批', value: 'APPROVAL' },
  { label: '规则检查', value: 'RULE_CHECK' },
  { label: '分支', value: 'BRANCH' },
  { label: '结束', value: 'END' },
]

const roleOptions = [
  { label: '受理初审员', value: 'APPROVER' },
  { label: '终审负责人', value: 'DECIDE' },
  { label: '办结出证员', value: 'ISSUE' },
]

const nodeTypeLabel: Record<string, string> = {
  START: '开始',
  APPROVAL: '审批',
  RULE_CHECK: '规则检查',
  BRANCH: '分支',
  END: '结束',
}

const nodeTypeTag: Record<string, string> = {
  START: 'success',
  APPROVAL: 'primary',
  RULE_CHECK: 'warning',
  BRANCH: 'info',
  END: 'danger',
}

const roleLabel: Record<string, string> = {
  APPROVER: '受理初审员',
  DECIDE: '终审负责人',
  ISSUE: '办结出证员',
}

function addNode() {
  nodes.value.push({
    id: undefined,
    nodeName: '',
    nodeType: 'APPROVAL',
    assigneeRole: 'APPROVER',
    sortOrder: nodes.value.length + 1,
    nextNodeId: null,
    branchConfig: '',
  })
}

function removeNode(index: number) {
  nodes.value.splice(index, 1)
  // 重新排序
  nodes.value.forEach((n, i) => (n.sortOrder = i + 1))
}

function moveUp(index: number) {
  if (index === 0) return
  const temp = nodes.value[index]
  nodes.value[index] = nodes.value[index - 1]
  nodes.value[index - 1] = temp
  nodes.value.forEach((n, i) => (n.sortOrder = i + 1))
}

function moveDown(index: number) {
  if (index === nodes.value.length - 1) return
  const temp = nodes.value[index]
  nodes.value[index] = nodes.value[index + 1]
  nodes.value[index + 1] = temp
  nodes.value.forEach((n, i) => (n.sortOrder = i + 1))
}

async function loadDetail(id: number) {
  loading.value = true
  try {
    const res = await processApi.detail(id)
    if (res.data?.template) {
      Object.assign(template, res.data.template)
    }
    if (res.data?.nodes) {
      nodes.value = res.data.nodes
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  // 表单校验
  const valid = await templateFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (nodes.value.length === 0) {
    ElMessage.warning('请至少添加一个环节')
    return
  }
  if (nodes.value.some((n) => !n.nodeName?.trim())) {
    ElMessage.warning('请填写所有环节的名称')
    return
  }
  saving.value = true
  try {
    await processApi.save({
      template: { ...template },
      nodes: nodes.value,
    })
    ElMessage.success('保存成功')
    router.push('/process/list')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  const id = route.params.id as string
  if (id) {
    loadDetail(Number(id))
  } else {
    // 默认添加开始和结束节点
    nodes.value = [
      { nodeName: '受理', nodeType: 'START', assigneeRole: 'APPROVER', sortOrder: 1 },
      { nodeName: '办结', nodeType: 'END', assigneeRole: 'APPROVER', sortOrder: 2 },
    ]
  }
})
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- 模板基本信息 -->
    <el-card class="section-card">
      <template #header><span class="section-title">模板信息</span></template>
      <el-form ref="templateFormRef" :model="template" :rules="templateRules" label-width="120px" inline>
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="template.templateCode" placeholder="如 ENTERPRISE_REGISTER" maxlength="50" style="width: 240px" />
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="template.templateName" placeholder="如 新办企业审批流程" maxlength="50" style="width: 240px" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="template.version" maxlength="10" style="width: 100px" />
        </el-form-item>
        <el-form-item label="事项类型" prop="matterType">
          <el-select v-model="template.matterType" placeholder="请选择事项类型" style="width: 180px">
            <el-option v-for="opt in matterTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="template.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="template.description" placeholder="模板描述" maxlength="200" style="width: 300px" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 环节配置 -->
    <el-card class="section-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span class="section-title">环节配置（上下移动排序）</span>
          <el-button type="primary" size="small" @click="addNode">+ 添加环节</el-button>
        </div>
      </template>

      <el-table :data="nodes" border style="width: 100%">
        <el-table-column label="排序" width="80" type="index" />
        <el-table-column label="环节名称" width="200">
          <template #default="{ row }">
            <el-input v-model="row.nodeName" placeholder="如 受理" size="small" maxlength="20" />
          </template>
        </el-table-column>
        <el-table-column label="环节类型" width="140">
          <template #default="{ row }">
            <el-select v-model="row.nodeType" size="small">
              <el-option v-for="opt in nodeTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="办理角色" width="160">
          <template #default="{ row }">
            <el-select v-model="row.assigneeRole" size="small" :disabled="row.nodeType === 'START' || row.nodeType === 'END'">
              <el-option v-for="opt in roleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="类型标签" width="100">
          <template #default="{ row }">
            <el-tag :type="nodeTypeTag[row.nodeType] || 'info'" size="small">
              {{ nodeTypeLabel[row.nodeType] || row.nodeType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ $index }">
            <el-button size="small" link :disabled="$index === 0" @click="moveUp($index)">上移</el-button>
            <el-button size="small" link :disabled="$index === nodes.length - 1" @click="moveDown($index)">下移</el-button>
            <el-button size="small" link type="danger" @click="removeNode($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 流程预览 -->
      <div style="margin-top: 20px">
        <div style="font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 12px">流程预览</div>
        <div class="flow-preview">
          <template v-for="(node, idx) in nodes" :key="idx">
            <div class="flow-node" :class="node.nodeType.toLowerCase()">
              <el-tag :type="nodeTypeTag[node.nodeType] || 'info'" size="default">
                {{ node.nodeName || '(未命名)' }}
              </el-tag>
              <div class="flow-role">{{ roleLabel[node.assigneeRole] || '' }}</div>
            </div>
            <div v-if="idx < nodes.length - 1" class="flow-arrow">→</div>
          </template>
        </div>
      </div>
    </el-card>

    <!-- 底部操作 -->
    <div style="text-align: center; padding: 20px">
      <el-button @click="router.push('/process/list')">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>
  </div>
</template>

<style scoped>
.flow-preview {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.flow-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.flow-role {
  font-size: 12px;
  color: #909399;
}

.flow-arrow {
  color: #c0c4cc;
  font-size: 20px;
  margin: 0 4px;
}
</style>
