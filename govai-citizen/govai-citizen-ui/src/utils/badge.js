import { reactive } from 'vue'

export const badgeState = reactive({ count: 0 })

export function setBadge(n) {
  badgeState.count = Math.max(0, Number(n) || 0)
}
