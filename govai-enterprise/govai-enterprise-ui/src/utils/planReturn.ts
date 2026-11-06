/**
 * 智能办事规划 ⇄ 办理事务页 的快捷返回辅助
 *
 * 痛点:从 /matters/plan 生成清单后跳去办理(在线申报/事项详情),再返回时规划页组件重挂载、
 * 内存清单丢失,不得不重新 LLM 生成。规划内容本身后端已自动落历史(planMatters 存 EntMatterPlanHistory),
 * 这里只需记住"最近一次规划",供返回时自动恢复同一份清单。
 *
 * sessionStorage 兜底保证浏览器后退/刷新仍可恢复(路由 query 在后退时不一定保留)。
 * key 带企业 id 隔离,避免同浏览器多企业串号。
 */
import { useAuthStore } from '@/stores/auth'

const KEY = 'govai_last_plan'
const MAX_AGE_MS = 2 * 60 * 60 * 1000 // 2 小时内可恢复,超时视为过期场景

export interface LastPlan {
  historyId: number
  title: string
  time: number
}

function scopeKey(): string {
  const auth = useAuthStore()
  return `${auth.user?.enterpriseId ?? 'anon'}:${KEY}`
}

/** 记录最近一次成功生成的规划 */
export function rememberPlan(plan: { historyId?: number; title?: string }): void {
  if (!plan.historyId) return
  const snap: LastPlan = { historyId: plan.historyId, title: plan.title || '', time: Date.now() }
  try {
    sessionStorage.setItem(scopeKey(), JSON.stringify(snap))
  } catch {
    /* sessionStorage 不可用时静默降级:仅本页内存态,不影响主流程 */
  }
}

/** 读取最近一次规划;未记录或已过期返回 null */
export function readLastPlan(): LastPlan | null {
  try {
    const raw = sessionStorage.getItem(scopeKey())
    if (!raw) return null
    const snap = JSON.parse(raw) as LastPlan
    if (!snap.historyId || Date.now() - snap.time > MAX_AGE_MS) return null
    return snap
  } catch {
    return null
  }
}

/** 返回规划的跳转 query 组装:有历史 id 则携带,供规划页自动恢复 */
export function planReturnQuery(): { path: string; query: { from: string; planId?: string } } {
  const snap = readLastPlan()
  return snap
    ? { path: '/matters/plan', query: { from: 'plan', planId: String(snap.historyId) } }
    : { path: '/matters/plan', query: { from: 'plan' } }
}

const LEFT_KEY = 'govai_plan_left'
/**
 * 标记"从规划页跳去办理":规划页 goDetail/goApply 跳走前调用。
 * 返回规划页时据此判断是"办事后回来"还是"首次进入",前者静默续上清单。
 */
export function markPlanLeft(): void {
  try {
    sessionStorage.setItem(scopeKeyFor(LEFT_KEY), '1')
  } catch {
    /* 忽略 */
  }
}

/** 读取并清除"去过办理"标记(一次性) */
export function consumePlanLeft(): boolean {
  try {
    const k = scopeKeyFor(LEFT_KEY)
    const hit = sessionStorage.getItem(k) === '1'
    sessionStorage.removeItem(k)
    return hit
  } catch {
    return false
  }
}

function scopeKeyFor(tag: string): string {
  const auth = useAuthStore()
  return `${auth.user?.enterpriseId ?? 'anon'}:${tag}`
}
