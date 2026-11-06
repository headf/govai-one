// 状态文案与时间/字段工具

const STATUS_TEXT = {
  PENDING: '受理中',
  APPROVING: '审批中',
  APPROVED: '已办结',
  REJECTED: '已驳回'
}

export function statusText(status) {
  return STATUS_TEXT[status] || status || ''
}

export function formatTime(time) {
  if (!time) return ''
  const s = String(time)
  return s.length >= 16 ? s.substring(0, 16).replace('T', ' ') : s
}

export function shortTime(time) {
  if (!time) return ''
  const s = String(time)
  const d = s.length >= 10 ? s.substring(0, 10) : s
  return d
}

export function fmtDate(d) {
  if (!d) return ''
  const s = String(d)
  return s.length >= 10 ? s.substring(0, 10) : s
}

// 材料/条件等数组渲染为中文顿号文本
export function joinArr(arr) {
  if (!arr || !Array.isArray(arr) || arr.length === 0) return '无'
  return arr.join('、')
}

// 星级描述
export function starText(n) {
  const map = { 5: '非常满意', 4: '满意', 3: '一般', 2: '不满意', 1: '很不满意' }
  return map[n] || ''
}

// 校验手机号
export function validPhone(p) {
  return /^1[3-9]\d{9}$/.test(p)
}

// 校验身份证
export function validIdCard(c) {
  return /^\d{17}[\dXx]$/.test(c)
}
