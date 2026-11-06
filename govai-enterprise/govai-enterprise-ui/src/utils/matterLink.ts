import { getMatterByCode } from '@/api'
import type { RouteLocationRaw } from 'vue-router'

/**
 * 按事项编码解析真实 id 并返回跳转路由(申报页)。
 * 用于兑现/延续/变更等跨页跳转,避免硬编码事项 id(种子库自增顺序变化会失效)。
 */
export async function resolveApplyRoute(code: string, query: Record<string, any> = {}): Promise<RouteLocationRaw | null> {
  try {
    const matter = await getMatterByCode(code)
    if (!matter?.id) return null
    return { path: `/matters/${matter.id}/apply`, query }
  } catch (e) {
    return null
  }
}
