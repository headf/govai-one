<template>
  <div class="page-card" v-loading="loading">
    <el-page-header @back="handleHeaderBack">
      <template #content>
        <div class="apply-head-content">
          <div>
            <span class="text-large font-bold">{{ matter?.matterName }}</span>
            <span class="text-muted ml-8">在线申报</span>
          </div>
          <el-button v-if="fromPlan" type="warning" plain size="small" class="back-plan-btn" @click="backToPlan">
            <el-icon><Back /></el-icon>&nbsp;返回智能办事规划
          </el-button>
        </div>
      </template>
    </el-page-header>

    <el-steps :active="step" finish-status="success" class="steps">
      <el-step title="填写表单" />
      <el-step title="上传材料" />
      <el-step title="提交完成" />
    </el-steps>

    <el-alert
      v-if="isDraft"
      type="warning"
      :closable="false"
      show-icon
      class="mt-8"
      :title="isRepair ? '补正退回·请补充材料后重新提交' : '正在续填草稿'"
      :description="isRepair ? '该申报被审批端退回,已恢复此前填写内容。请按退回意见补齐材料后点击「提交申报」重新提交' : '您上次有未提交的草稿,已恢复填写内容,修改后请点击「保存草稿」或「下一步」继续'"
    />

    <!-- Step 0: 动态表单 -->
    <div v-if="step === 0">
      <!-- 重复发证提醒:新办/补办且已持有同类型有效证 -->
      <el-alert
        v-if="dupNewWarn"
        type="warning"
        :closable="false"
        show-icon
        class="mt-8 dup-warn"
      >
        <template #title>
          <span class="dup-warn-title">重复办理提醒:您已持有同类型<b>有效</b>证照,本次办结将再签发一张</span>
        </template>
        <div class="dup-warn-body">
          <div class="text-muted">您名下现有 {{ dupNewWarn.list.length }} 张有效「{{ dupNewWarn.licenseType }}」:</div>
          <div v-for="l in dupNewWarn.list" :key="l.id" class="dup-warn-lic">
            <el-icon :size="14" color="#e6a23c"><Stamp /></el-icon>
            <span class="dup-warn-name">{{ l.licenseName }}</span>
            <span class="text-muted">编号:{{ l.licenseNo }} · 有效期至 {{ l.expiryDate || '-' }}</span>
          </div>
          <div class="text-muted mt-8">若为重复办理,请先到证照空间注销多余证照,避免重复持证。</div>
          <el-button size="small" type="primary" plain class="mt-8" @click="goLicenseSpace">去证照空间查看/注销</el-button>
        </div>
      </el-alert>

      <!-- 延续/变更/注销: 原证确认(唯一有效证自动带出并展示;多张时选择本次作用于哪一张,避免注销/延续错证) -->
      <el-alert
        v-if="multiTargetLicenses.length > 0"
        type="info"
        :closable="false"
        show-icon
        class="mt-8 pick-bar"
      >
        <template #title>
          <span>{{ multiTargetLicenses.length > 1
            ? `检测到 ${multiTargetLicenses.length} 张同类型证照,请确认本次「${matter?.matterName}」作用于哪一张`
            : `本次「${matter?.matterName}」将作用于名下这张证照,请确认无误` }}</span>
        </template>
        <div class="pick-row">
          <el-select v-model="pickedLicenseNo" style="width: 460px" placeholder="选择证照" @change="onPickLicense">
            <el-option
              v-for="l in multiTargetLicenses"
              :key="l.id"
              :value="l.licenseNo"
              :label="`${l.licenseType} · ${l.licenseNo} · 有效期至 ${l.expiryDate || '-'}`"
            />
          </el-select>
          <span class="text-muted pick-current">当前:<b>{{ pickedLabel || pickedLicenseNo || '-' }}</b></span>
          <el-button size="small" type="primary" plain @click="goLicenseSpace">去证照空间核实</el-button>
        </div>
      </el-alert>

      <el-alert v-if="!isDraft" type="info" :closable="false" show-icon class="mt-8 step-tip"
        title="第 1 步 · 填写表单"
        description="请如实填写申请信息(带 * 为必填),企业基础信息已自动带出。完成后点击下方「下一步:上传材料」" />
      <!-- 已有营业执照电子证照:企业信息已自动回填,无需重复提交 -->
      <el-alert v-if="hasBusinessLicense" type="success" :closable="false" class="ocr-bar" show-icon>
        <template #title>
          <span>已从营业执照电子证照自动带出企业信息(免证办),无需重复提交</span>
        </template>
        <template #default>
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="handleOcrUpload"
          >
            <el-button type="primary" plain :loading="ocrLoading" size="small">
              <el-icon><Camera /></el-icon>&nbsp;拍照识别覆盖
            </el-button>
          </el-upload>
          <span v-if="ocrDone" class="text-success ml-8">
            <el-icon><CircleCheck /></el-icon> 已识别回填(置信度 {{ ocrConfidence }}%)
          </span>
        </template>
      </el-alert>
      <!-- 无营业执照电子证照:提供拍照 OCR 智能填表 -->
      <el-alert v-else type="info" :closable="false" class="ocr-bar" show-icon>
        <template #title>
          <span>上传营业执照照片,AI 自动识别并回填企业信息(智能填表)</span>
        </template>
        <template #default>
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="handleOcrUpload"
          >
            <el-button type="primary" plain :loading="ocrLoading">
              <el-icon><Camera /></el-icon>&nbsp;拍照/上传证照识别
            </el-button>
          </el-upload>
          <span v-if="ocrDone" class="text-success ml-8">
            <el-icon><CircleCheck /></el-icon> 已识别回填(置信度 {{ ocrConfidence }}%)
          </span>
        </template>
      </el-alert>

      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="160px" class="mt-16">
        <el-row :gutter="24">
          <el-col v-for="f in matter?.formConfig || []" :key="f.key" :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item :label="f.label" :prop="f.key">
              <!-- text -->
              <el-input v-if="f.type === 'text'" v-model="formData[f.key]" placeholder="请输入" />
              <!-- number -->
              <el-input-number v-else-if="f.type === 'number'" v-model="formData[f.key]" :min="0" :precision="0" style="width: 100%" />
              <!-- textarea -->
              <el-input v-else-if="f.type === 'textarea'" v-model="formData[f.key]" type="textarea" :rows="3" />
              <!-- select -->
              <el-select v-else-if="f.type === 'select'" v-model="formData[f.key]" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in f.options" :key="o" :label="o" :value="o" />
              </el-select>
              <!-- checkbox -->
              <el-checkbox v-else-if="f.type === 'checkbox'" v-model="formData[f.key]">是</el-checkbox>
              <!-- date -->
              <el-date-picker v-else-if="f.type === 'date'" v-model="formData[f.key]" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="step-foot">
          <el-button @click="onSaveDraft" :loading="loading">保存草稿</el-button>
          <el-button type="primary" @click="goUpload">下一步:上传材料</el-button>
        </div>
      </el-form>
    </div>

    <!-- Step 1: 上传材料 -->
    <div v-if="step === 1" class="mt-16">
      <el-alert type="info" :closable="false" show-icon class="mb-16 step-tip"
        title="第 2 步 · 上传材料"
        description="请按要求上传所需材料(免证办证照无需重复上传),也可「从已有选择」复用历史材料。完成后点击「提交申报」" />
      <!-- 免证办:自动带证提示 -->
      <el-alert
        v-if="matchedLicenses.length"
        title="已从电子证照库自动匹配到以下证照,提交时自动核验,无需重复上传"
        type="success"
        :closable="false"
        class="mb-16"
      >
        <template #default>
          <el-tag v-for="l in matchedLicenses" :key="l.id" class="license-tag" effect="plain">{{ l.licenseType }} · {{ l.licenseName }}</el-tag>
        </template>
      </el-alert>

      <el-form label-position="top">
        <div v-for="(mat, idx) in uploadMaterials" :key="mat.name" class="mat-item">
          <div class="mat-label">
            <span class="mat-idx">{{ idx + 1 }}</span>
            <span>{{ materialLabel(mat.name) }}</span>
          </div>
          <div class="upload-row">
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept="image/*,.pdf"
              :on-change="(file: any) => handleFile(mat, file)"
              :on-remove="() => (mat.file = undefined)"
            >
              <el-button type="primary" plain :icon="Upload" size="small">上传文件</el-button>
            </el-upload>
            <el-button type="success" plain :icon="FolderOpened" size="small" @click="openReuse(mat)">从已有选择</el-button>
            <span v-if="mat.file" class="text-success mat-tip">
              <el-icon><CircleCheck /></el-icon> 已选:{{ mat.file.name }}
            </span>
            <span v-else-if="mat.reused" class="text-success mat-tip">
              <el-icon><CircleCheck /></el-icon> 已选用:{{ mat.reused.label }}
            </span>
          </div>
        </div>
        <el-empty v-if="!uploadMaterials.length" description="当前变更事项无需上传材料(证照已免证办自动带出)" />
      </el-form>

      <!-- 复用已有证照/材料弹窗 -->
      <el-dialog v-model="reuseDialogVisible" title="从已有证照 / 材料中选择" width="680px" top="7vh">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="mb-16"
          title="选择您名下已有的电子证照或历史上传材料直接复用，无需重复上传"
        />

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

      <!-- 已上传列表 -->
      <el-table v-if="uploadedList.length" :data="uploadedList" border class="mb-16">
        <el-table-column prop="materialName" label="材料名称" />
        <el-table-column label="状态" width="120">
          <template #default>
            <el-tag type="success" size="small">已上传</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="row.fileUrl" link type="primary" size="small" @click="openPreview(row.fileUrl)">查看</el-button>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="step-foot">
        <el-button @click="step = 0">上一步</el-button>
        <el-button @click="onSaveDraft" :loading="loading">保存草稿</el-button>
        <el-button type="primary" :loading="uploading" @click="submitAll">提交申报</el-button>
      </div>
    </div>

    <!-- Step 2: 提交成功 -->
    <el-result v-if="step === 2" icon="success" title="申报提交成功">
      <template #sub-title>
        <p>申报编号:<b>{{ result?.recordCode }}</b></p>
        <p class="text-muted">已进入审批流程,可在"我的申报"中查看进度</p>
      </template>
      <template #extra>
        <el-button type="primary" @click="router.push('/applications')">查看我的申报</el-button>
        <el-button @click="router.push('/matters')">继续办理其他事项</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { Upload, CircleCheck, Camera, FolderOpened, Document, Stamp, Back, Search } from '@element-plus/icons-vue'
import { getMatter, getApplyDetail, createDraft, uploadMaterial, matchLicenses, prefillEnterprise, submitApply, preCheck, ocrRecognize, listReusableMaterials, reuseMaterial, listLicenses } from '@/api'
import { planReturnQuery } from '@/utils/planReturn'
import type { MatterVO, License, FormField, ReusableMaterialVO } from '@/types'

const route = useRoute()
const router = useRouter()
const matter = ref<MatterVO>()
const loading = ref(false)
const uploading = ref(false)
const step = ref(0)
const formRef = ref<FormInstance>()

// 从智能办事规划跳入办理:显示"返回规划"入口
const fromPlan = computed(() => route.query.from === 'plan')
function backToPlan() {
  // 优先携带本页自带的 planId(即使 sessionStorage 失效也能精确恢复同一份清单)
  const pid = route.query.planId
  router.push(pid
    ? { path: '/matters/plan', query: { from: 'plan', planId: String(pid) } }
    : planReturnQuery())
}
// 顶部返回:来自规划时回规划页(保留清单),否则按浏览器历史后退
function handleHeaderBack() {
  if (fromPlan.value) {
    backToPlan()
  } else {
    router.back()
  }
}

const formData = reactive<Record<string, any>>({})
const formRules = reactive<FormRules>({})

const matchedLicenses = ref<License[]>([])
// 是否已有营业执照电子证照(决定 OCR 栏展示策略)
const hasBusinessLicense = computed(() =>
  matchedLicenses.value.some((l) => /营业执照/.test(l.licenseType || ''))
)

// ---- 重复证照/目标证照相关: 申报提醒 + 多张同类型证时让用户选一张 ----
// 名下全部证照(含失效/过期,便于判断"已持有同类型有效证")
const allLicenses = ref<License[]>([])
// 事项名称 → 将签发的证照类型(与后端 ApplicationService.mapLicenseType 同口径)
function licenseTypeOfMatter(name?: string): string | null {
  if (!name) return null
  if (name.includes('食品经营')) return '食品经营许可证'
  if (name.includes('卫生')) return '卫生许可证'
  if (name.includes('烟草')) return '烟草专卖零售许可证'
  if (name.includes('药品')) return '药品经营许可证'
  if (name.includes('排污')) return '排污许可证'
  if (name.includes('开办')) return '营业执照'
  return null
}
// "会额外再发一张证、但不作废旧证"的申报: 新办/开办/补办(可能造成同类型堆叠)
const stackingMatter = computed(() => {
  const n = matter.value?.matterName || ''
  return n.includes('新办') || n.includes('开办') || n.includes('补办')
})
const targetLicenseType = computed(() => licenseTypeOfMatter(matter.value?.matterName))
// 名下同类型且有效(未注销未过期)的证照
const heldSameTypeActive = computed(() => {
  const t = targetLicenseType.value
  if (!t) return []
  return allLicenses.value.filter((l) => (l.licenseType || '') === t && l.status === 1 &&
    (!l.expiryDate || new Date(l.expiryDate).getTime() >= Date.now()))
})
// 新办/补办重复发证提醒(仅提醒,不阻断)
const dupNewWarn = computed(() =>
  stackingMatter.value && heldSameTypeActive.value.length ? {
    licenseType: targetLicenseType.value as string,
    list: heldSameTypeActive.value,
  } : null)
// 延续/变更/注销类:匹配到的候选证照(可能多张,须让用户选作用于哪一张)
const targetMatter = computed(() => {
  const n = matter.value?.matterName || ''
  return n.includes('延续') || n.includes('变更') || n.includes('注销')
})
const multiTargetLicenses = computed(() => (targetMatter.value ? matchedLicenses.value : []))
const pickedLicenseNo = ref('')
const pickedLabel = ref('')
function onPickLicense(no: string) {
  const lic = multiTargetLicenses.value.find((l) => l.licenseNo === no)
  pickedLabel.value = lic ? `${lic.licenseName}(${lic.licenseNo})` : ''
  if (lic) {
    if (formData.licenseNo !== undefined) formData.licenseNo = lic.licenseNo || ''
    if (formData.originalExpiryDate !== undefined) formData.originalExpiryDate = lic.expiryDate || ''
  }
}
function goLicenseSpace() {
  router.push('/licenses')
}
const recordId = ref<number>()
// 续填草稿:路由 query.draft 传入草稿记录ID
const draftId = ref<number | undefined>(route.query.draft ? Number(route.query.draft) : undefined)
// 补正退回续填:路由 query.repair=1 表示待补正记录,重新提交
const isRepair = computed(() => route.query.repair === '1')
const isDraft = computed(() => !!draftId.value)
const result = ref<{ recordCode: string; status: string }>()
const uploadedList = ref<any[]>([])
interface MatItem {
  name: string
  file?: File
  reused?: { fileUrl: string; label: string }
}
const uploadMaterials = ref<MatItem[]>([])
const ocrLoading = ref(false)
const ocrDone = ref(false)
const ocrConfidence = ref(0)

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
/** 证照状态文案 */
function licStatusText(l: License) {
  if (l.status === 0) return '已注销'
  if (l.expiryDate && new Date(l.expiryDate).getTime() < Date.now()) return '已过期'
  return '有效'
}
function licTagType(l: License) {
  const t = licStatusText(l)
  return t === '有效' ? 'success' : t === '已过期' ? 'warning' : 'info'
}

/** 弹窗内证照列表：支持搜索；有效证照排前面，已注销/过期沉底但仍可见 */
const filteredLicenses = computed(() => {
  const kw = reuseKeyword.value.trim().toLowerCase()
  const list = (reusable.value?.licenses || []).filter((l) => {
    if (!kw) return true
    return (l.licenseType || '').toLowerCase().includes(kw) || (l.licenseNo || '').toLowerCase().includes(kw)
  })
  return [...list].sort((a, b) => (isLicValid(b) ? 1 : 0) - (isLicValid(a) ? 1 : 0))
})
/** 弹窗内历史材料：支持搜索 */
const filteredHistory = computed(() => {
  const kw = reuseKeyword.value.trim().toLowerCase()
  const list = reusable.value?.history || []
  if (!kw) return list
  return list.filter((h) => (h.materialName || '').toLowerCase().includes(kw))
})

// 初始化规则:必填校验
function initRules(fields: FormField[]) {
  for (const f of fields) {
    if (f.required) {
      formRules[f.key] = [{ required: true, message: `请填写${f.label}`, trigger: 'blur' }]
    }
    // 初始化空值
    if (formData[f.key] === undefined) {
      formData[f.key] = f.type === 'number' ? undefined : f.type === 'checkbox' ? false : ''
    }
  }
}

// 该材料能否由企业名下电子证照自动核验(免证办)。与后端 ComplianceService 同口径:
// 材料名含"营业执照/许可/资质"关键词 且 企业确实持有匹配证照 才免传;否则必须上传,避免静默漏交
function isLicenseAutoFill(name: string, licenses: License[]) {
  const has = (kw: string) => licenses.some((l) => (l.licenseType || '').includes(kw))
  if (name.includes('营业执照')) return has('营业执照')
  if (name.includes('许可')) return has('许可')
  if (name.includes('资质')) return has('资质')
  return false
}

// 需要用户上传的材料清单:排除「企业持有匹配证照、可自动带出」的(免证办)
function isAutoFill(name: string) {
  return isLicenseAutoFill(name, matchedLicenses.value)
}

// 根据表单选择(如"变更事项")动态决定材料是否需要上传
// 材料名条件标注格式:"变更后经营场所使用证明(变更地址时)" → 选"经营地址变更"时显示
// 提取括号内"变更X时"的关键词 X,匹配 formData.changeType
function shouldShowMaterial(name: string): boolean {
  const m = name.match(/\(([^)]+)\)\s*$/)
  if (!m) return true
  const cond = m[1]
  // 仅"变更X时"视为动态条件,其他括号标注(如"交回""免证办""OCR")按总显示
  const kwm = cond.match(/变更(.+?)时/)
  if (kwm) {
    const core = kwm[1]
    const ct = String(formData.changeType || '')
    return ct.includes(core)
  }
  return true
}

// 材料显示名:去掉括号内条件标注(如"变更后经营场所使用证明(变更地址时)"→"变更后经营场所使用证明")
function materialLabel(name: string): string {
  return name.replace(/\(([^)]+)\)\s*$/, '').trim()
}

// 重新计算待上传材料清单(按当前 changeType 过滤,保留已选文件/复用状态)
function recomputeUploadMaterials() {
  const prev = new Map(uploadMaterials.value.map((m) => [m.name, m]))
  uploadMaterials.value = (matter.value?.materials || [])
    .filter((name) => !isAutoFill(name))
    .filter((name) => shouldShowMaterial(name))
    .map((name) => {
      const old = prev.get(name)
      return old ? { ...old } : { name }
    })
}

// 变更事项变化时,动态刷新待上传材料清单
watch(() => formData.changeType, () => recomputeUploadMaterials())

async function loadMatter() {
  const id = Number(route.params.id)
  matter.value = await getMatter(id)
  initRules(matter.value?.formConfig || [])
  // 免证办匹配
  matchedLicenses.value = await matchLicenses(id)
  // 名下全量证照(用于"已持有同类型有效证"的重复办理提醒)
  try {
    allLicenses.value = await listLicenses()
  } catch (e) { /* 拉取失败仅降级为不提示,不影响申报 */ }
  // 续填草稿:回填表单与已上传材料
  if (draftId.value) {
    await loadDraft(draftId.value)
  } else {
    // 新建申报:已有营业执照等证照时,从企业信息库自动回填基础字段,无需重复填表/拍照
    try {
      const prefill = await prefillEnterprise()
      for (const f of matter.value?.formConfig || []) {
        const v = prefill[f.key]
        if (v !== undefined && v !== null && v !== '' &&
            (formData[f.key] === '' || formData[f.key] === undefined)) {
          formData[f.key] = f.type === 'number' ? Number(v) : v
        }
      }
    } catch (e) {
      // 预填失败不影响申报,用户可手填或拍照识别
    }
    // 证照类(注销/延续/变更)预填:目标证始终取最新一张有效证;表单证号缺失或已失效(历史草稿/克隆旧单)时覆盖为新证,避免沿用已注销旧证号
    if (targetMatter.value && matchedLicenses.value.length) {
      const lic = matchedLicenses.value[0]
      const current = String(formData.licenseNo ?? '')
      const keep = !!current && matchedLicenses.value.some((l) => l.licenseNo === current)
      if (!keep) {
        formData.licenseNo = lic.licenseNo || ''
        if (formData.originalExpiryDate !== undefined) formData.originalExpiryDate = lic.expiryDate || ''
        if (current) pickedLabel.value = ''
      }
      pickedLicenseNo.value = lic.licenseNo || ''
      if (!pickedLabel.value) pickedLabel.value = lic.licenseName || ''
    }
    // 路由预设:从证照到期/企业信息页跳转带 changeType 时自动选中变更事项
    const qt = route.query.changeType
    if (qt && formData.changeType !== undefined) {
      formData.changeType = String(qt)
    }
    // 按当前表单(含预填/选择)计算待上传材料清单
    recomputeUploadMaterials()
  }
}

async function loadDraft(id: number) {
  const d = await getApplyDetail(id)
  recordId.value = d.record.id
  const cfg = matter.value?.formConfig || []
  const data = d.record.formData || {}
  for (const f of cfg) {
    const v = data[f.key]
    if (v !== undefined && v !== null) {
      formData[f.key] = f.type === 'number' ? Number(v) : v
    }
  }
  uploadedList.value = (d.materials || []).map((m) => ({ materialName: m.materialName, fileUrl: m.fileUrl }))
  // 按回填的变更事项重新计算待上传材料,再排除已上传的
  recomputeUploadMaterials()
  const uploadedNames = new Set((d.materials || []).map((m) => m.materialName))
  uploadMaterials.value = uploadMaterials.value.filter((m) => !uploadedNames.has(m.name))
  // 注销/延续/变更类:草稿里指定的原证若已失效(先存草稿、后换发/注销成新证),自动换成最新一张有效证,避免提交被后端"证照已注销"校验拦下
  if (targetMatter.value && formData.licenseNo !== undefined && matchedLicenses.value.length) {
    const valid = matchedLicenses.value.some((l) => l.licenseNo === formData.licenseNo)
    if (!valid) {
      const latest = matchedLicenses.value[0]
      formData.licenseNo = latest.licenseNo || ''
      if (formData.originalExpiryDate !== undefined) formData.originalExpiryDate = latest.expiryDate || ''
      pickedLicenseNo.value = latest.licenseNo || ''
      ElMessage.warning(`此前草稿指定的证照已注销/失效,已自动切换为当前有效证照(${latest.licenseNo}),提交前请核对`)
    }
  }
}

function handleFile(mat: MatItem, file: UploadFile) {
  mat.file = file.raw
  mat.reused = undefined
}

async function openReuse(mat: MatItem) {
  reuseTarget.value = mat
  reuseSelected.value = mat.reused?.fileUrl || ''
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
  reuseTarget.value.reused = { fileUrl: sel, label }
  reuseTarget.value.file = undefined
  reuseDialogVisible.value = false
}

function openPreview(url: string) {
  window.open(`/files${url.startsWith('/files') ? url.slice(6) : url}`, '_blank')
}

function fmtTime(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

// OCR 拍照识别并回填表单
async function handleOcrUpload(file: UploadFile) {
  const raw = file.raw
  if (!raw) return
  ocrLoading.value = true
  ocrDone.value = false
  try {
    // 图片转 base64
    const base64 = await fileToBase64(raw)
    const resp = await ocrRecognize({ imageBase64: base64, type: 'BUSINESS_LICENSE' })
    const fields = resp.fields || {}
    // 按表单字段 key 回填(识别字段名与表单 key 一致,或从 rawText 兜底)
    let filled = 0
    for (const f of matter.value?.formConfig || []) {
      const v = fields[f.key]
      if (v !== undefined && v !== null && v !== '') {
        // number 类型字段(如注册资本)需转成数值,否则 el-input-number 无法显示
        formData[f.key] = f.type === 'number' ? Number(v) : v
        filled++
      }
    }
    ocrConfidence.value = Math.round((resp.confidence || 0) * 100)
    ocrDone.value = true
    if (filled > 0) {
      ElMessage.success(`已自动识别并回填 ${filled} 个字段(置信度 ${ocrConfidence.value}%)`)
    } else {
      ElMessage.warning(`已识别文本,但未匹配到可回填字段(置信度 ${ocrConfidence.value}%)`)
    }
  } catch (e: any) {
    ElMessage.error('OCR 识别失败:' + (e.message || '请确认 OCR 服务已启动'))
  } finally {
    ocrLoading.value = false
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

// 构建表单提交载荷
function buildPayload() {
  const payload: Record<string, any> = {}
  for (const f of matter.value?.formConfig || []) {
    payload[f.key] = formData[f.key]
  }
  return payload
}

// 保存草稿(新建或更新,不强制校验必填)
async function saveDraft() {
  const draft = await createDraft({
    matterId: Number(route.params.id),
    formData: buildPayload(),
    recordId: recordId.value
  })
  recordId.value = draft.recordId
  return draft
}

async function onSaveDraft() {
  loading.value = true
  try {
    await saveDraft()
    ElMessage.success('草稿已保存,可在「草稿箱」继续编辑')
  } finally {
    loading.value = false
  }
}

// 进入材料页:先校验并保存草稿拿 recordId
async function goUpload() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await saveDraft()
      step.value = 1
    } finally {
      loading.value = false
    }
  })
}

async function submitAll() {
  if (!recordId.value) return
  uploading.value = true
  try {
    // 1. 逐个上传/复用材料
    for (const mat of uploadMaterials.value) {
      if (mat.reused) {
        await reuseMaterial(recordId.value, mat.name, mat.reused.fileUrl)
        uploadedList.value.push({ materialName: mat.name, fileUrl: mat.reused.fileUrl })
      } else if (mat.file) {
        const up = await uploadMaterial(recordId.value, mat.name, mat.file)
        uploadedList.value.push({ materialName: mat.name, fileUrl: up.fileUrl })
      }
    }
    // 2. 合规预检
    const report = await preCheck(recordId.value)
    if (!report.passed) {
      // 有不通过项:弹窗告知,让用户选择返回补充或仍提交
      const fails = report.items
        .filter((i) => !i.result)
        .map((i) => `· ${i.checkItem}:${i.detail}`)
        .join('\n')
      try {
        await ElMessageBox.confirm(
          `智能预检出 ${report.failCount} 项未通过:\n\n${fails}\n\n您可以选择返回修改,或仍坚持提交(可能影响审批结果)。`,
          '合规预检提示',
          { type: 'warning', confirmButtonText: '仍要提交', cancelButtonText: '返回修改' }
        )
      } catch {
        return // 用户选择返回
      }
    } else {
      ElMessage.success(`合规预检通过(${report.passCount} 项全部通过)`)
    }
    // 3. 提交(后端含信用硬拦等校验,失败时 request 层已 toast 业务消息,此处仅阻止进入成功页)
    try {
      result.value = await submitApply(recordId.value)
    } catch {
      return // 提交被拦(如信用受限/审批端异常),停留在当前步
    }
    step.value = 2
    ElMessage.success('申报已提交')
  } finally {
    uploading.value = false
  }
}

onMounted(loadMatter)
</script>

<style scoped>
.steps {
  max-width: 600px;
  margin: 24px auto 0;
}
.apply-head-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  gap: 12px;
}
.back-plan-btn {
  margin-right: 8px;
}
.ocr-bar {
  margin-bottom: 16px;
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
.dup-warn-body {
  margin-top: 6px;
}
.dup-warn-title {
  font-weight: 600;
}
.dup-warn-lic {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
  padding: 4px 8px;
  margin-top: 6px;
  background: #fffbe6;
  border-radius: 4px;
}
.dup-warn-name {
  font-weight: 600;
  color: #303133;
}
.pick-bar {
  border-radius: 6px;
}
.pick-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 4px;
}
.pick-current {
  font-size: 13px;
}
</style>
