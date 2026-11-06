// 审批岗位元数据：岗位码 -> 展示名/可做动作/按钮文案
// 与后端 Constants 审批岗位码对齐(APPROVER 受理初审 / DECIDE 终审 / ISSUE 办结出证)
export type RoleCode = 'APPROVER' | 'DECIDE' | 'ISSUE'

export interface RoleMeta {
  code: string
  name: string
  // 该岗位在流程环节上的操作按钮配置
  actions: Array<{
    result: string // PASS / REJECT / RETURN（对应后端 approveResult）
    label: string
    type: 'success' | 'danger' | 'warning' | 'primary'
    // 该动作是否需要填写附加办理信息(handleDetail)，如办结出证填证照/送达方式
    needHandleDetail?: boolean
  }>
}

// 办结出证岗的办理表单字段（送达方式等），新增可在此扩展
export const ISSUE_DELIVERY_OPTIONS = ['窗口自取', '邮寄送达', '电子证照推送']

// 事项办结形态(审批端按事项名识别,决定办结岗按钮/送达方式):
//  LICENSE      发证/许可类(新办/延续/认定/补办/企业开办) —— 办结出证,可选送达方式(窗口/邮寄/电子)
//  CANCEL       注销类(食品/企业注销) —— 出具注销证明,只能电子送达,不能邮寄证照
//  NO_DELIVERY  无证照送达类(补贴/减免/变更/延期) —— 不出实体证,无送达方式
export type SettleKind = 'LICENSE' | 'CANCEL' | 'NO_DELIVERY'

export function matterSettleKind(matterName?: string): SettleKind {
  const n = matterName || ''
  if (n.includes('注销')) return 'CANCEL'
  if (n.includes('补贴') || n.includes('减免') || n.includes('延期') || n.includes('变更') || n.includes('股权') || n.includes('稳岗')) {
    return 'NO_DELIVERY'
  }
  return 'LICENSE'
}

// 办结岗 PASS 按钮文案,按事项形态区分
export const ISSUE_PASS_LABEL: Record<SettleKind, string> = {
  LICENSE: '出证办结',
  CANCEL: '注销办结',
  NO_DELIVERY: '确认办结',
}

export const ROLE_META: Record<string, RoleMeta> = {
  APPROVER: {
    code: 'APPROVER',
    name: '受理初审员',
    actions: [
      { result: 'PASS', label: '审核通过', type: 'success' },
      { result: 'RETURN', label: '退回补正', type: 'warning' },
      { result: 'REJECT', label: '不予受理', type: 'danger' },
    ],
  },
  DECIDE: {
    code: 'DECIDE',
    name: '终审负责人',
    actions: [
      { result: 'PASS', label: '准予决定', type: 'success' },
      { result: 'RETURN', label: '退回复审', type: 'warning' },
      { result: 'REJECT', label: '不予批准', type: 'danger' },
    ],
  },
  ISSUE: {
    code: 'ISSUE',
    name: '办结出证员',
    actions: [
      { result: 'PASS', label: '出证办结', type: 'success', needHandleDetail: true },
      { result: 'RETURN', label: '退回终审复核', type: 'warning' },
    ],
  },
}

export function roleName(code?: string): string {
  return (code && ROLE_META[code]?.name) || code || ''
}

export function roleActions(code?: string): RoleMeta['actions'] {
  return (code && ROLE_META[code]?.actions) || []
}
