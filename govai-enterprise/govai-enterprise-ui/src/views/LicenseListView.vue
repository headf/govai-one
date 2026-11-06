<template>
  <div>
    <section class="hero-lic card">
      <div class="hero-lic-top">
        <div>
          <div class="hero-lic-title">企业证照空间</div>
          <div class="hero-lic-sub">申报时自动核验有效证照（免证办）；点击证照可在线预览 / 下载电子副本</div>
        </div>
        <div class="hero-lic-tools">
          <el-tag v-if="activeEnterpriseLicenses.length" type="success" effect="plain">有效 {{ activeEnterpriseLicenses.length }} 张</el-tag>
          <el-tag v-if="invalidLicenses.length" type="danger" effect="plain">已注销 {{ invalidLicenses.length }} 张</el-tag>
          <el-tag v-if="expiringLicenses.length" type="warning" effect="plain">临期 {{ expiringLicenses.length }} 张</el-tag>
          <el-button type="primary" plain :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </div>
    </section>

    <!-- 已注销证照可见性:引导查看下方垃圾桶 -->
    <el-alert
      v-if="invalidLicenses.length"
      type="info"
      :closable="false"
      show-icon
      class="mb-16"
      title="名下已注销证照已移入下方『已注销证照(垃圾桶)』专区"
      :description="`当前共 ${invalidLicenses.length} 张(自行注销/企业注销)。已从上方有效列表移除,可展开下方专区查看,确保证照状态一目了然`"
    />

    <!-- 重复证照提醒:同类型有效证照 > 1 -->
    <el-alert
      v-if="duplicateGroups.length"
      type="warning"
      :closable="false"
      show-icon
      class="mb-16 dup-banner"
    >
      <template #title>
        <span class="dup-title">重复证照提醒:以下同类型<b>有效</b>证照存在多张,疑似重复办理,同一主体同一事项通常只应保留 1 张</span>
      </template>
      <div v-for="g in duplicateGroups" :key="g.licenseType" class="dup-group">
        <div class="dup-group-head">
          <el-icon :size="16" color="#e6a23c"><Stamp /></el-icon>
          <span class="dup-type">{{ g.licenseType }}</span>
          <el-tag size="small" type="warning" effect="plain">{{ g.count }} 张</el-tag>
          <span class="text-muted dup-hint">多余证照建议办理注销,避免重复持证</span>
          <el-button v-if="cancelCodeOf(g.licenseType)" size="small" type="danger" plain @click="goVoidLicense(g.licenseType)">去注销该类型多余证</el-button>
          <span v-else class="expiring-hint">暂无在线注销事项,请咨询发证部门</span>
        </div>
        <div class="dup-list">
          <div v-for="l in g.licenses" :key="l.id" class="dup-item">
            <el-icon :size="14" color="#e6a23c"><Stamp /></el-icon>
            <span class="dup-name">{{ l.licenseName }}</span>
            <span class="text-muted">编号:{{ l.licenseNo }}</span>
            <span class="text-muted">有效期至 {{ l.expiryDate || '-' }}</span>
            <el-button link type="primary" size="small" @click="preview(l)">预览</el-button>
          </div>
        </div>
      </div>
    </el-alert>

    <el-alert
      v-if="expiringLicenses.length"
      type="warning"
      :closable="false"
      show-icon
      class="mb-16 expiring-banner"
    >
      <template #title>
        <span class="expiring-title">您有 {{ expiringLicenses.length }} 个证照即将到期或已过期,请及时办理延续/重新办理</span>
      </template>
      <div class="expiring-list">
        <div v-for="l in expiringLicenses" :key="l.id" class="expiring-item">
          <el-icon :size="18" :color="isExpired(l) ? '#f56c6c' : '#e6a23c'"><Stamp /></el-icon>
          <span class="expiring-name">{{ l.licenseName || l.licenseType }}</span>
          <el-tag size="small" :type="isExpired(l) ? 'danger' : 'warning'">
            {{ isExpired(l) ? '已过期' : daysLeft(l) + ' 天后到期' }}
          </el-tag>
          <span class="expiring-date">到期日:{{ l.expiryDate }}</span>
          <el-button size="small" type="info" plain @click="preview(l)">去查看</el-button>
          <el-button v-if="renewMatterCode(l)" size="small" type="warning" plain @click="goRenew(l)">{{ isExpired(l) ? '去重新办理' : '去办理延期' }}</el-button>
          <span v-else class="expiring-hint">暂无在线延续/重办事项,请联系发证部门</span>
        </div>
      </div>
    </el-alert>

    <!-- 企业证照(营业执照/许可证/资质证书,主体为统一社会信用代码) -->
    <div class="lic-section">
      <div class="lic-section-title">
        <el-icon :size="18" color="#2f6bff"><OfficeBuilding /></el-icon>
        <span>企业证照</span>
        <el-tag size="small" type="info" effect="plain">{{ enterpriseLicenses.length }} 张</el-tag>
      </div>
      <p class="text-muted lic-section-sub">以统一社会信用代码为主体,申报时自动核验(免证办)</p>
      <el-row :gutter="16">
        <el-col v-for="l in enterpriseLicenses" :key="l.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="license-card">
          <div class="lic-head">
            <el-icon :size="32" :color="iconColor(l)"><Stamp /></el-icon>
            <div class="lic-type">{{ l.licenseType }}</div>
            <el-tag v-if="isExpired(l)" type="danger" size="small">已过期</el-tag>
            <el-tag v-else-if="isExpiring(l)" type="warning" size="small">即将到期</el-tag>
            <el-tag v-else-if="l.status === 0" type="danger" size="small">{{ voidLabel(l) }}</el-tag>
            <el-tag v-else type="success" size="small">有效</el-tag>
            <!-- 同类型多张有效证照时明确提示,避免"重复持证"看不出来 -->
            <el-tag
              v-if="sameTypeActiveCount(l.licenseType) > 1"
              type="warning"
              size="small"
              effect="dark"
            >同类型共 {{ sameTypeActiveCount(l.licenseType) }} 张</el-tag>
          </div>
          <div class="lic-name">{{ l.licenseName }}</div>
          <div class="text-muted lic-no">编号:{{ l.licenseNo }}</div>
          <div class="text-muted">有效期:{{ l.issueDate }} 至 {{ l.expiryDate }}</div>

          <el-image
            v-if="l.fileUrl"
            :src="fileSrc(l)"
            :preview-src-list="[fileSrc(l)]"
            fit="contain"
            class="lic-thumb"
            preview-teleported
          >
            <template #error>
              <div class="thumb-placeholder">预览加载失败</div>
            </template>
          </el-image>

          <div class="lic-actions">
            <el-button v-if="canReapply(l)" size="small" type="warning" plain @click="goRenew(l)">过期重办</el-button>
            <el-button v-else-if="canRenew(l)" size="small" type="warning" plain @click="goRenew(l)">立即延续</el-button>
            <el-button v-if="l.fileUrl" size="small" type="primary" plain @click="preview(l)">预览</el-button>
            <el-button v-if="l.fileUrl" size="small" type="success" plain>
              <a class="dl" :href="fileSrc(l)" :download="downloadName(l)">下载</a>
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    </div>

    <!-- 个人证照(法人/经办人身份证等,与个人身份绑定) -->
    <div class="lic-section">
      <div class="lic-section-title">
        <el-icon :size="18" color="#67c23a"><User /></el-icon>
        <span>个人证照</span>
        <el-tag size="small" type="info" effect="plain">{{ personalLicenses.length }} 张</el-tag>
      </div>
      <p class="text-muted lic-section-sub">法人/经办人个人证件,与企业证照分开管理</p>
      <el-empty v-if="!personalLicenses.length" description="暂无个人证照" :image-size="60" />
      <el-row :gutter="16" v-else>
        <el-col v-for="l in personalLicenses" :key="l.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="license-card personal">
          <div class="lic-head">
            <el-icon :size="32" :color="iconColor(l)"><Stamp /></el-icon>
            <div class="lic-type">{{ l.licenseType }}</div>
            <el-tag v-if="l.status === 0" type="danger" size="small">{{ voidLabel(l) }}</el-tag>
            <el-tag v-else type="success" size="small">有效</el-tag>
          </div>
          <div class="lic-name">{{ l.licenseName }}</div>
          <div class="text-muted lic-no">编号:{{ l.licenseNo }}</div>
          <div class="text-muted">有效期:{{ l.issueDate }} 至 {{ l.expiryDate }}</div>
          <el-image
            v-if="l.fileUrl"
            :src="fileSrc(l)"
            :preview-src-list="[fileSrc(l)]"
            fit="contain"
            class="lic-thumb"
            preview-teleported
          >
            <template #error>
              <div class="thumb-placeholder">预览加载失败</div>
            </template>
          </el-image>
          <div class="lic-actions">
            <el-button v-if="l.fileUrl" size="small" type="primary" plain @click="preview(l)">预览</el-button>
            <el-button v-if="l.fileUrl" size="small" type="success" plain>
              <a class="dl" :href="fileSrc(l)" :download="downloadName(l)">下载</a>
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    </div>

    <!-- 证照历史: 延续/变更换发后的旧证(仅换发历史,非注销,不进垃圾桶) -->
    <div v-if="historyLicenses.length" class="lic-section">
      <el-collapse v-model="historyOpen">
        <el-collapse-item name="history">
          <template #title>
            <div class="lic-section-title trash-title">
              <el-icon :size="18" color="#909399"><Stamp /></el-icon>
              <span>证照历史(换发)</span>
              <el-tag size="small" type="info" effect="plain">{{ historyLicenses.length }} 张</el-tag>
              <span class="text-muted trash-hint">延续/变更换发后的旧证,仅存档备查</span>
            </div>
          </template>
          <div v-for="l in historyLicenses" :key="l.id" class="trash-row">
            <el-icon :size="20" color="#c0c4cc"><Stamp /></el-icon>
            <div class="trash-main">
              <div class="trash-name">{{ l.licenseName || l.licenseType }}</div>
              <div class="text-muted trash-sub">{{ l.licenseType }} · 编号:{{ l.licenseNo }} · 有效期至 {{ l.expiryDate || '-' }}</div>
            </div>
            <el-tag type="info" size="small">{{ voidLabel(l) }}</el-tag>
            <el-button v-if="l.fileUrl" size="small" type="info" plain @click="preview(l)">预览</el-button>
            <el-button v-if="l.fileUrl" size="small" type="info" plain>
              <a class="dl" :href="fileSrc(l)" :download="downloadName(l)">下载</a>
            </el-button>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- 已注销证照垃圾桶: 仅主动注销(自行注销/企业注销)的证照;过期未延续不自动注销,仍在"企业证照"区提示重办 -->
    <div v-if="invalidLicenses.length" class="lic-section">
      <el-collapse v-model="trashOpen">
        <el-collapse-item name="trash">
          <template #title>
            <div class="lic-section-title trash-title">
              <el-icon :size="18" color="#f56c6c"><Delete /></el-icon>
              <span>已注销证照(垃圾桶)</span>
              <el-tag size="small" type="info" effect="plain">{{ invalidLicenses.length }} 张</el-tag>
              <span class="text-muted trash-hint">仅主动注销的证照;过期未延续的证照仍在上方列表并提示延续/重办</span>
            </div>
          </template>
          <div v-for="l in invalidLicenses" :key="l.id" class="trash-row">
            <el-icon :size="20" color="#c0c4cc"><Stamp /></el-icon>
            <div class="trash-main">
              <div class="trash-name">{{ l.licenseName || l.licenseType }}</div>
              <div class="text-muted trash-sub">{{ l.licenseType }} · 编号:{{ l.licenseNo }} · 有效期至 {{ l.expiryDate || '-' }}</div>
            </div>
            <el-tag type="danger" size="small">{{ voidLabel(l) }}</el-tag>
            <el-button v-if="l.fileUrl" size="small" type="info" plain @click="preview(l)">预览</el-button>
            <el-button v-if="l.fileUrl" size="small" type="info" plain>
              <a class="dl" :href="fileSrc(l)" :download="downloadName(l)">下载</a>
            </el-button>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <el-empty v-if="!list.length" description="暂无证照数据" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Stamp, OfficeBuilding, User, Delete } from '@element-plus/icons-vue'
import { listLicenses } from '@/api'
import { resolveApplyRoute } from '@/utils/matterLink'
import type { License } from '@/types'

const router = useRouter()
const list = ref<License[]>([])
// 注销证照垃圾桶默认展开,让"已注销"证照直接可见
const trashOpen = ref<string[]>(['trash'])
// 证照历史(换发旧证)默认折叠
const historyOpen = ref<string[]>([])
// 到期提醒窗口:未来 30 天内到期(含已过期)的企业证照
const RENEW_WINDOW_DAYS = 30
// 即将到期(30天内,含已过期未注销)的企业证照,触发到期提醒横幅
const expiringLicenses = computed(() => enterpriseLicenses.value.filter(l => isExpiring(l) || isExpired(l)))
// 有效(未注销且未过期)的企业证照:用于检测"同类型多张有效"的重复证照
const activeEnterpriseLicenses = computed(() =>
  list.value.filter(l => isEnterpriseLicense(l) && l.status === 1 && !isExpired(l)))
// 按证照类型分组,返回有效证照数 > 1 的类型(疑似重复办理)
const duplicateGroups = computed(() => {
  const byType = new Map<string, License[]>()
  for (const l of activeEnterpriseLicenses.value) {
    const t = l.licenseType || '其他证照'
    const arr = byType.get(t) || []
    arr.push(l)
    byType.set(t, arr)
  }
  return [...byType.entries()]
    .filter(([, ls]) => ls.length > 1)
    .map(([licenseType, licenses]) => ({ licenseType, count: licenses.length, licenses }))
})
// 同类型证照的"在线注销事项"码(暂仅食品经营许可提供在线注销申报)
const CANCEL_MATTER_BY_TYPE: Record<string, string> = { 食品经营许可证: 'SP-JY-004' }
function cancelCodeOf(licenseType: string): string | null {
  return CANCEL_MATTER_BY_TYPE[licenseType] || null
}
// 引导办理注销申报,清理重复/冗余证照
async function goVoidLicense(licenseType: string) {
  const code = cancelCodeOf(licenseType)
  if (!code) {
    router.push('/matters')
    return
  }
  try {
    const route = await resolveApplyRoute(code)
    if (route) router.push(route)
    else router.push('/matters')
  } catch (e) {
    router.push('/matters')
  }
}
function isEnterpriseLicense(l: License): boolean {
  if (l.category) return l.category === 'ENTERPRISE'
  const t = l.licenseType || ''
  return !(t.includes('身份证') || t.includes('社保') || t.includes('个人'))
}
function isPersonalLicense(l: License): boolean {
  if (l.category) return l.category === 'PERSONAL'
  return !isEnterpriseLicense(l)
}
// 企业证照(营业执照/许可证/资质证书,不含失效的)
const enterpriseLicenses = computed(() => list.value.filter(l => isEnterpriseLicense(l) && l.status !== 0))
// 个人证照(法人/经办人身份证等,不含失效的)
const personalLicenses = computed(() => list.value.filter(l => isPersonalLicense(l) && l.status !== 0))
// 失效/注销证照的标签文案:按作废原因区分(企业注销/自行注销/延续换发/变更换发)
function voidLabel(l: License): string {
  const r = l.voidReason
  if (r === 'CANCEL_ENT') return '已注销(企业注销)'
  if (r === 'CANCEL_SELF') return '已注销'
  if (r === 'RENEW_EXTEND') return '已换发(延续)'
  if (r === 'RENEW_CHANGE') return '已换发(变更)'
  return '已失效'
}
// 是否"换发历史"旧证(延续/变更换发产生): 归证照历史区,不算注销
function isRenewHistory(l: License): boolean {
  return l.voidReason === 'RENEW_EXTEND' || l.voidReason === 'RENEW_CHANGE'
}
// 垃圾桶: 仅主动注销(CANCEL_*),不含换发旧证(过期不自动注销,过期证留在上方列表)
const invalidLicenses = computed(() => list.value.filter(l => l.status === 0 && !isRenewHistory(l)))
// 证照历史: 延续/变更换发的旧证(RENEW_*),作换发历史留存
const historyLicenses = computed(() => list.value.filter(l => l.status === 0 && isRenewHistory(l)))

function fileSrc(l: License) {
  return '/files' + (l.fileUrl || '')
}
// 下载文件名:取 fileUrl 真实后缀,避免硬编码 .svg
function downloadName(l: License) {
  const base = l.licenseName || '证照'
  const m = (l.fileUrl || '').match(/\.(svg|pdf|jpg|jpeg|png)$/i)
  const ext = m ? m[1].toLowerCase() : 'svg'
  return `${base}.${ext}`
}
function isExpired(l: License) {
  return l.status === 1 && !!l.expiryDate && new Date(l.expiryDate).getTime() < Date.now()
}
// 同类型有效证照数量:同类型多张时在卡片上提示"同类型共 N 张"(避免用户察觉不到重复持证)
function sameTypeActiveCount(licenseType?: string) {
  if (!licenseType) return 0
  return list.value.filter((l) => l.licenseType === licenseType && l.status === 1 && !isExpired(l)).length
}
function daysLeft(l: License) {
  return Math.max(0, Math.ceil((new Date(l.expiryDate!).getTime() - Date.now()) / 86400000))
}
function isExpiring(l: License) {
  if (l.status === 0 || !l.expiryDate || isExpired(l)) return false
  const exp = new Date(l.expiryDate).getTime()
  const days = (exp - Date.now()) / 86400000
  return days >= 0 && days <= RENEW_WINDOW_DAYS
}
function iconColor(l: License) {
  if (l.status === 0 || isExpired(l)) return '#f56c6c'
  return isExpiring(l) ? '#e6a23c' : '#2f6bff'
}
function preview(l: License) {
  window.open(fileSrc(l), '_blank')
}
// 到期/过期联动处置:证照类型 → 事项编码。优先延续(即将到期/可续),超期后引导重新办理(新办/补办)
function renewMatterCode(l: License): string | null {
  const t = l.licenseType || ''
  if (/食品经营许可/.test(t)) return isExpired(l) ? 'SP-JY-001' : 'SP-JY-003'
  if (/卫生许可/.test(t)) return isExpired(l) ? 'WS-WS-001' : 'WS-WS-002'
  return null
}
// 卡片上的延续按钮:即将到期且有对应延续事项时显示
function canRenew(l: License) {
  return isExpiring(l) && renewMatterCode(l) !== null
}
// 已过期(未注销)证照:提示延续/重办,仍留在主列表处置
function canReapply(l: License) {
  return isExpired(l)
}
// 选择办理延期 → 跳到对应延续事项申报页
async function goRenew(l: License) {
  const code = renewMatterCode(l)
  if (!code) {
    router.push('/matters')
    return
  }
  const route = await resolveApplyRoute(code)
  if (route) router.push(route)
  else router.push('/matters')
}

async function load() {
  list.value = await listLicenses()
}
onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mb-16 {
  margin-bottom: 16px;
}
.license-card {
  margin-bottom: 16px;
}
.license-card :deep(.el-card__body) {
  padding: 12px 14px;
}
.lic-section {
  margin-top: 20px;
}
.lic-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}
.lic-section-sub {
  margin: 0 0 12px 26px;
  font-size: 13px;
}
.license-card.personal {
  border-left: 3px solid #67c23a;
}
.lic-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.lic-head .el-icon {
  flex-shrink: 0;
}
.lic-type {
  font-size: 14px;
  font-weight: 600;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lic-name {
  font-weight: 600;
  margin: 8px 0 4px;
  min-height: 20px;
}
.lic-no {
  margin-bottom: 4px;
  font-size: 13px;
}
.lic-thumb {
  width: 100%;
  height: 88px;
  margin-top: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}
.thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88px;
  color: #c0c4cc;
  font-size: 12px;
}
.lic-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.lic-actions .el-button {
  margin-left: 0;
}
.dl {
  color: inherit;
  text-decoration: none;
}
.dup-title {
  font-weight: 600;
}
.dup-group {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #f0d080;
}
.dup-group-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.dup-type {
  font-weight: 600;
  color: #303133;
}
.dup-hint {
  font-size: 13px;
}
.dup-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}
.dup-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
  padding: 4px 8px;
  background: #fffbe6;
  border-radius: 4px;
}
.dup-name {
  font-weight: 600;
  color: #303133;
}
.expiring-banner {
  border-radius: 6px;
}
.expiring-banner :deep(.el-alert__content) {
  width: 100%;
}
.expiring-title {
  font-weight: 600;
}
.expiring-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}
.expiring-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 6px 0;
  border-bottom: 1px dashed #f0d080;
}
.expiring-item:last-child {
  border-bottom: none;
}
.expiring-name {
  font-weight: 600;
  color: #303133;
}
.expiring-date {
  color: #e6a23c;
  font-size: 13px;
  margin-right: auto;
}
.expiring-hint {
  color: #909399;
  font-size: 12px;
}
.trash-title {
  margin: 0;
  font-size: 15px;
}
.trash-hint {
  font-weight: 400;
  font-size: 12px;
}
.trash-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-top: 1px solid #f0f0f0;
}
.trash-row .el-button + .el-button {
  margin-left: 0;
}
.trash-main {
  flex: 1;
  min-width: 0;
}
.trash-name {
  font-weight: 600;
  color: #909399;
  text-decoration: line-through;
}
.trash-sub {
  font-size: 12px;
}
/* 门户头卡 + 证照卡 hover */
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 22px 24px;
}
.hero-lic {
  background: radial-gradient(560px 220px at 100% -20%, rgba(47, 107, 255, 0.08), transparent 60%), #fff;
}
.hero-lic-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.hero-lic-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}
.hero-lic-sub {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}
.hero-lic-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.license-card {
  border-radius: 12px;
  transition: box-shadow 0.2s, transform 0.15s;
}
.license-card:hover {
  box-shadow: 0 2px 4px rgba(16, 24, 40, 0.04), 0 8px 20px rgba(47, 107, 255, 0.12);
  transform: translateY(-2px);
}
.license-card:not(.personal):hover {
  border-color: #c1d3ff;
}
</style>
