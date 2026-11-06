<template>
  <div class="page-card" v-loading="loading">
    <el-page-header @back="router.back()">
      <template #content>
        <span class="text-large font-bold">{{ pkg?.packageName }}</span>
        <span class="text-muted ml-8">一表申报</span>
      </template>
    </el-page-header>

    <div class="text-muted mt-8">{{ pkg?.description }}</div>
    <!-- 套餐包含事项与流程 -->
    <div class="mt-16 pkg-overview">
      <div class="sub-title">本次联办事项</div>
      <div class="pkg-items">
        <el-tag v-for="it in pkg?.items || []" :key="it.matterId" effect="plain" class="pkg-tag">
          {{ it.matterName }}<span class="text-muted"> · {{ it.department }}</span>
        </el-tag>
      </div>
    </div>

    <el-steps :active="step" finish-status="success" class="steps">
      <el-step title="填写一表" />
      <el-step title="上传材料(一套)" />
      <el-step title="提交完成" />
    </el-steps>

    <!-- Step 0: 一表(合并表单) -->
    <div v-if="step === 0" class="mt-16">
      <el-alert type="info" :closable="false" show-icon class="mb-16"
        title="第 1 步 · 填写一表申请"
        description="共用字段只需填写一次,提交时将自动拆分为多个事项独立申报。企业基础信息已自动带出,带 * 为必填" />
      <!-- OCR 智能填表 -->
      <el-alert type="info" :closable="false" show-icon class="ocr-bar" v-if="!hasBusinessLicense">
        <template #title><span>上传营业执照照片,AI 自动识别并回填共用字段(智能填表)</span></template>
        <template #default>
          <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="handleOcrUpload">
            <el-button type="primary" plain :loading="ocrLoading">
              <el-icon><Camera /></el-icon>&nbsp;拍照/上传证照识别
            </el-button>
          </el-upload>
          <span v-if="ocrDone" class="text-success ml-8">
            <el-icon><CircleCheck /></el-icon> 已识别回填(置信度 {{ ocrConfidence }}%)
          </span>
        </template>
      </el-alert>
      <el-alert v-else type="success" :closable="false" show-icon class="ocr-bar">
        <template #title><span>已从营业执照电子证照自动带出企业信息(免证办),无需重复提交</span></template>
        <template #default>
          <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="handleOcrUpload">
            <el-button type="primary" plain size="small" :loading="ocrLoading">
              <el-icon><Camera /></el-icon>&nbsp;拍照识别覆盖
            </el-button>
          </el-upload>
          <span v-if="ocrDone" class="text-success ml-8">
            <el-icon><CircleCheck /></el-icon> 已识别回填(置信度 {{ ocrConfidence }}%)
          </span>
        </template>
      </el-alert>

      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="160px">
        <el-row :gutter="24">
          <el-col v-for="f in pkg?.mergedForm || []" :key="f.key" :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item :label="f.label" :prop="f.key">
              <el-input v-if="f.type === 'text'" v-model="formData[f.key]" placeholder="请输入" />
              <el-input-number v-else-if="f.type === 'number'" v-model="formData[f.key]" :min="0" :precision="0" style="width: 100%" />
              <el-input v-else-if="f.type === 'textarea'" v-model="formData[f.key]" type="textarea" :rows="3" />
              <el-select v-else-if="f.type === 'select'" v-model="formData[f.key]" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in f.options" :key="o" :label="o" :value="o" />
              </el-select>
              <el-checkbox v-else-if="f.type === 'checkbox'" v-model="formData[f.key]">是</el-checkbox>
              <el-date-picker v-else-if="f.type === 'date'" v-model="formData[f.key]" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="step-foot">
          <el-button type="primary" @click="goMaterials">下一步:上传材料</el-button>
        </div>
      </el-form>
    </div>

    <!-- Step 1: 一套材料 -->
    <div v-if="step === 1" class="mt-16">
      <el-alert type="info" :closable="false" show-icon class="mb-16"
        title="第 2 步 · 上传材料(一套)"
        description="多事项的重复材料已合并,只需上传一份。电子证照能自动核验的(免证办)无需上传;其余请上传或从已有复用。提交时系统会按材料自动匹配到对应事项" />

      <!-- 免证办提示 -->
      <el-alert
        v-if="licenseAutoNames.length"
        title="以下材料已从电子证照库自动核验,无需重复上传"
        type="success"
        :closable="false"
        class="mb-16"
      >
        <template #default>
          <el-tag v-for="name in licenseAutoNames" :key="name" effect="plain" class="license-tag">{{ name }}</el-tag>
        </template>
      </el-alert>

      <el-form label-position="top">
        <div v-for="(mat, idx) in materialRows" :key="mat.name" class="mat-item">
          <div class="mat-label">
            <span class="mat-idx">{{ idx + 1 }}</span>
            <span>{{ materialLabel(mat.name) }}</span>
            <el-tag v-if="mat.autoFill" type="success" size="small">电子证照自动带出</el-tag>
          </div>
          <!-- 需上传材料 -->
          <div v-if="!mat.autoFill" class="upload-row">
            <el-upload
              :auto-upload="false"
              :limit="1"
              :show-file-list="false"
              accept="image/*,.pdf"
              :on-change="(file: any) => handleFile(mat, file)"
            >
              <el-button type="primary" plain :icon="Upload" size="small">上传文件</el-button>
            </el-upload>
            <el-button type="success" plain :icon="FolderOpened" size="small" @click="openReuse(mat)">从已有选择</el-button>
            <span v-if="mat.fileUrl" class="text-success mat-tip">
              <el-icon><CircleCheck /></el-icon> 已选:{{ mat.fileName || mat.name }}
            </span>
            <el-button v-if="mat.fileUrl" type="danger" link size="small" @click="clearMat(mat)">移除</el-button>
          </div>
          <!-- 免证办材料:无需操作 -->
          <div v-else class="text-muted auto-tip">已由电子证照库自动核验,提交时免证办带出</div>
        </div>
        <el-empty v-if="!materialRows.length" description="该套餐无需额外材料" />
      </el-form>

      <!-- 复用已有证照/材料弹窗 -->
      <el-dialog v-model="reuseDialogVisible" title="从已有证照 / 材料中选择" width="680px" top="7vh">
        <el-input v-model="reuseKeyword" placeholder="搜索证照名称 / 编号 / 材料名" clearable class="mb-16">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <div class="reuse-scroll">
          <template v-if="filteredLicenses.length">
            <div class="reuse-sec">电子证照（{{ filteredLicenses.length }}）</div>
            <div class="reuse-rows">
              <div
                v-for="l in filteredLicenses"
                :key="'lic-' + l.id"
                class="reuse-row"
                :class="{ on: reuseSelected === l.fileUrl, off: !l.fileUrl }"
                @click="l.fileUrl && (reuseSelected = l.fileUrl)"
              >
                <el-icon class="rr-icon" color="#2f6bff"><Stamp /></el-icon>
                <div class="rr-main">
                  <div class="rr-title">
                    <span>{{ l.licenseType }}</span>
                    <el-tag size="small" effect="plain" :type="licTagType(l)">{{ licStatusText(l) }}</el-tag>
                  </div>
                  <div class="rr-sub">
                    编号 {{ l.licenseNo || '-' }} · {{ l.expiryDate ? '有效期至 ' + l.expiryDate : '长期有效' }}
                  </div>
                </div>
                <el-icon v-if="reuseSelected === l.fileUrl" class="rr-check" color="#2f6bff"><CircleCheck /></el-icon>
              </div>
            </div>
          </template>

          <template v-if="filteredHistory.length">
            <div class="reuse-sec">历史已上传材料（{{ filteredHistory.length }}）</div>
            <div class="reuse-rows">
              <div
                v-for="h in filteredHistory"
                :key="'his-' + h.materialId"
                class="reuse-row"
                :class="{ on: reuseSelected === h.fileUrl }"
                @click="reuseSelected = h.fileUrl"
              >
                <el-icon class="rr-icon" color="#67c23a"><Document /></el-icon>
                <div class="rr-main">
                  <div class="rr-title"><span>{{ h.materialName }}</span></div>
                  <div class="rr-sub">上传于 {{ fmtTime(h.uploadTime) }}</div>
                </div>
                <el-icon v-if="reuseSelected === h.fileUrl" class="rr-check" color="#2f6bff"><CircleCheck /></el-icon>
              </div>
            </div>
          </template>
        </div>

        <el-empty
          v-if="!filteredLicenses.length && !filteredHistory.length"
          :description="reuseKeyword ? '没有匹配的证照或材料' : '暂无可复用的证照或历史材料'"
          :image-size="60"
        />

        <template #footer>
          <el-button @click="reuseDialogVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!reuseSelected" @click="confirmReuse">使用选中材料</el-button>
        </template>
      </el-dialog>

      <div class="step-foot">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">提交申报(自动拆分多事项)</el-button>
      </div>
    </div>

    <!-- Step 2: 提交成功 -->
    <div v-if="step === 2" class="mt-16">
      <el-result icon="success" title="一表申报提交成功">
        <template #sub-title>
          <p class="text-muted">已按套餐自动拆分为多个事项独立申报,分别进入审批流程</p>
        </template>
        <template #extra>
          <el-table v-if="result?.records?.length" :data="result.records" border class="mb-16">
            <el-table-column prop="recordCode" label="申报编号" width="180" />
            <el-table-column prop="matterName" label="办理事项" />
            <el-table-column label="状态" width="110">
              <template #default>
                <el-tag type="warning" size="small">待提交</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="foot">
            <el-button @click="router.push('/matters')">继续办理其他事项</el-button>
            <el-button type="primary" @click="router.push('/applications')">查看我的申报</el-button>
          </div>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { Upload, CircleCheck, Camera, FolderOpened, Document, Stamp, Search } from '@element-plus/icons-vue'
import { getPackage, submitPackage, uploadPackageMaterial, ocrRecognize, prefillEnterprise, listReusableMaterials } from '@/api'
import type { PackageVO, FormField, PackageSubmitResp, ReusableMaterialVO, License } from '@/types'

const route = useRoute()
const router = useRouter()
const pkg = ref<PackageVO>()
const loading = ref(false)
const submitting = ref(false)
const step = ref(0)
const formRef = ref<FormInstance>()

const formData = reactive<Record<string, any>>({})
const formRules = reactive<FormRules>({})

const ocrLoading = ref(false)
const ocrDone = ref(false)
const ocrConfidence = ref(0)

const result = ref<PackageSubmitResp>()

// 本企业全部有效证照(用于判定哪些材料可免证办自动带出,与后端同口径)
const licenses = ref<License[]>([])
const hasBusinessLicense = computed(() =>
  licenses.value.some((l) => /营业执照/.test(l.licenseType || ''))
)

interface MatItem {
  name: string
  fileUrl?: string
  fileName?: string
  autoFill: boolean
}
const materialRows = ref<MatItem[]>([])

// 免证办自动带出的材料名(供顶部提示展示)
const licenseAutoNames = computed(() => materialRows.value.filter((m) => m.autoFill).map((m) => materialLabel(m.name)))

// 复用已有证照/材料
const reusable = ref<ReusableMaterialVO>()
const reuseDialogVisible = ref(false)
const reuseTarget = ref<MatItem | null>(null)
const reuseSelected = ref('')
const reuseKeyword = ref('')

/** 证照有效性：未注销且未过期 */
function isLicValid(l: License) {
  return l.status === 1 && (!l.expiryDate || new Date(l.expiryDate).getTime() >= Date.now())
}
function licStatusText(l: License) {
  if (l.status === 0) return '已注销'
  if (l.expiryDate && new Date(l.expiryDate).getTime() < Date.now()) return '已过期'
  return '有效'
}
function licTagType(l: License) {
  const t = licStatusText(l)
  return t === '有效' ? 'success' : t === '已过期' ? 'warning' : 'info'
}
/** 弹窗内证照：支持搜索；有效排前，已注销/过期沉底 */
const filteredLicenses = computed(() => {
  const kw = reuseKeyword.value.trim().toLowerCase()
  const list = (reusable.value?.licenses || []).filter((l) => {
    if (!kw) return true
    return (l.licenseType || '').toLowerCase().includes(kw) || (l.licenseNo || '').toLowerCase().includes(kw)
  })
  return [...list].sort((a, b) => (isLicValid(b) ? 1 : 0) - (isLicValid(a) ? 1 : 0))
})
const filteredHistory = computed(() => {
  const kw = reuseKeyword.value.trim().toLowerCase()
  const list = reusable.value?.history || []
  if (!kw) return list
  return list.filter((h) => (h.materialName || '').toLowerCase().includes(kw))
})

// 材料名关键字 → 证照类型关键字(与后端 ComplianceService.isAutoFillByLicense 同口径)
function autofillKeyword(name: string): string | null {
  if (name.includes('营业执照')) return '营业执照'
  if (name.includes('许可') || name.includes('许可证')) return '许可'
  if (name.includes('资质')) return '资质'
  return null
}

// 判定某份合并材料能否由企业名下电子证照自动核验(免证办,无需上传)
function canLicenseAutofill(name: string): boolean {
  const kw = autofillKeyword(name)
  if (!kw) return false
  return licenses.value.some((l) => (l.licenseType || '').includes(kw))
}

// 材料显示名:去掉括号内条件/标注(如 "变更后经营场所使用证明(变更地址时)" → 去括号部分)
function materialLabel(name: string): string {
  return name.replace(/\(([^)]+)\)\s*$/, '').trim()
}

// 证照类材料若企业没有对应证照则必须上传,与后端预检判定保持一致,避免静默漏交
function buildMaterialRows(names: string[]) {
  materialRows.value = (names || []).map((name) => {
    const autoFill = canLicenseAutofill(name)
    return { name, autoFill }
  })
}

function initRules(fields: FormField[]) {
  for (const f of fields) {
    if (f.required) {
      formRules[f.key] = [{ required: true, message: `请填写${f.label}`, trigger: 'blur' }]
    }
    if (formData[f.key] === undefined) {
      formData[f.key] = f.type === 'number' ? undefined : f.type === 'checkbox' ? false : ''
    }
  }
}

async function loadPackage() {
  loading.value = true
  try {
    pkg.value = await getPackage(Number(route.params.id))
    initRules(pkg.value?.mergedForm || [])
    // 免证办判定需要企业名下有效证照
    try {
      const reusableData = await listReusableMaterials()
      reusable.value = reusableData
      licenses.value = reusableData?.licenses || []
    } catch (e) {
      licenses.value = []
    }
    buildMaterialRows(pkg.value?.mergedMaterials || [])
    // 已有营业执照等证照时,自动回填企业基础字段(免重复填表/拍照)
    try {
      const prefill = await prefillEnterprise()
      for (const f of pkg.value?.mergedForm || []) {
        const v = prefill[f.key]
        if (v !== undefined && v !== null && v !== '' &&
            (formData[f.key] === '' || formData[f.key] === undefined)) {
          formData[f.key] = f.type === 'number' ? Number(v) : v
        }
      }
    } catch (e) {
      // 预填失败不影响,用户可手填或拍照识别
    }
  } finally {
    loading.value = false
  }
}

function goMaterials() {
  if (!formRef.value) return
  formRef.value.validate((valid) => {
    if (valid) step.value = 1
  })
}

async function handleFile(mat: MatItem, file: UploadFile) {
  const raw = file.raw
  if (!raw) return
  try {
    const resp = await uploadPackageMaterial(raw)
    mat.fileUrl = resp.fileUrl
    mat.fileName = resp.materialName
    ElMessage.success(`已选择:${mat.fileName}`)
  } catch (e: any) {
    ElMessage.error('上传失败:' + (e.message || ''))
  }
}

function clearMat(mat: MatItem) {
  mat.fileUrl = undefined
  mat.fileName = undefined
}

async function openReuse(mat: MatItem) {
  reuseTarget.value = mat
  reuseSelected.value = mat.fileUrl || ''
  reuseDialogVisible.value = true
  if (!reusable.value) {
    try {
      reusable.value = await listReusableMaterials()
    } catch (e: any) {
      ElMessage.error('获取可复用材料失败:' + (e.message || ''))
    }
  }
}

function confirmReuse() {
  if (!reuseTarget.value || !reuseSelected.value) return
  const sel = reuseSelected.value
  const label =
    reusable.value?.licenses?.find((l) => l.fileUrl === sel)?.licenseType ||
    reusable.value?.history?.find((h) => h.fileUrl === sel)?.materialName ||
    '已有材料'
  reuseTarget.value.fileUrl = sel
  reuseTarget.value.fileName = label
  reuseDialogVisible.value = false
}

async function doSubmit() {
  const p = pkg.value
  if (!p) return
  // 校验:所有非免证办材料必须有文件,否则阻止提交并提示(与后端预检口径一致)
  const missing = materialRows.value.filter((m) => !m.autoFill && !m.fileUrl)
  if (missing.length) {
    ElMessage.warning('尚有材料未上传:' + missing.map((m) => materialLabel(m.name)).join('、'))
    return
  }
  submitting.value = true
  try {
    const payload: Record<string, any> = {}
    for (const f of p.mergedForm || []) {
      payload[f.key] = formData[f.key]
    }
    const materials = materialRows.value
      .filter((m) => !m.autoFill && m.fileUrl)
      .map((m) => ({ materialName: m.name, fileUrl: m.fileUrl! }))
    result.value = await submitPackage(p.id, payload, materials)
    step.value = 2
    ElMessage.success('申报已提交,已自动拆分为多个事项')
  } catch (e: any) {
    ElMessage.error('提交失败:' + (e.message || ''))
  } finally {
    submitting.value = false
  }
}

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

async function handleOcrUpload(file: UploadFile) {
  const raw = file.raw
  if (!raw) return
  ocrLoading.value = true
  ocrDone.value = false
  try {
    const base64 = await fileToBase64(raw)
    const resp = await ocrRecognize({ imageBase64: base64, type: 'BUSINESS_LICENSE' })
    const fields = resp.fields || {}
    let filled = 0
    for (const f of pkg.value?.mergedForm || []) {
      const v = fields[f.key]
      if (v !== undefined && v !== null && v !== '') {
        formData[f.key] = f.type === 'number' ? Number(v) : v
        filled++
      }
    }
    ocrConfidence.value = Math.round((resp.confidence || 0) * 100)
    ocrDone.value = true
    filled > 0
      ? ElMessage.success(`已自动识别并回填 ${filled} 个字段(置信度 ${ocrConfidence.value}%)`)
      : ElMessage.warning(`已识别文本,但未匹配到可回填字段(置信度 ${ocrConfidence.value}%)`)
  } catch (e: any) {
    ElMessage.error('OCR 识别失败:' + (e.message || '请确认 OCR 服务已启动'))
  } finally {
    ocrLoading.value = false
  }
}

function fmtTime(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

onMounted(loadPackage)
</script>

<style scoped>
.steps {
  max-width: 680px;
  margin: 20px auto 0;
}
.pkg-overview {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 16px;
}
.sub-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  font-size: 14px;
}
.pkg-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pkg-tag {
  font-size: 13px;
}
.ocr-bar {
  margin: 16px 0 8px;
}
.step-foot {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}
.license-tag {
  margin: 4px 8px 0 0;
}
.mat-item {
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #fafbfc;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}
.mat-label {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.mat-idx {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #2f6bff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.upload-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.mat-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  word-break: break-all;
}
.auto-tip {
  font-size: 13px;
}
.reuse-sec {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin: 12px 0 8px;
}
.reuse-scroll {
  max-height: 46vh;
  overflow-y: auto;
  padding-right: 4px;
}
.reuse-rows {
  display: grid;
  gap: 8px;
}
.reuse-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.reuse-row:hover {
  border-color: #c1d3ff;
  background: #f7faff;
}
.reuse-row.on {
  border-color: #2f6bff;
  background: #eaf0ff;
}
.reuse-row.off {
  opacity: 0.55;
  cursor: not-allowed;
}
.rr-icon {
  font-size: 20px;
  flex: none;
}
.rr-main {
  flex: 1;
  min-width: 0;
}
.rr-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.rr-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rr-check {
  flex: none;
  font-size: 18px;
}
.font-bold {
  font-weight: 600;
}
.ml-8 {
  margin-left: 8px;
}
.mb-16 {
  margin-bottom: 16px;
}
.foot {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}
</style>
