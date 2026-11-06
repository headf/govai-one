// 前端权限工具：读取登录返回的权限码，判断菜单/按钮显隐
const PERM_KEY = 'govai_permissions'

export function setPermissions(list) {
  localStorage.setItem(PERM_KEY, JSON.stringify(list || []))
}

export function clearPermissions() {
  localStorage.removeItem(PERM_KEY)
}

export function getPermissions() {
  try {
    return JSON.parse(localStorage.getItem(PERM_KEY) || '[]')
  } catch (e) {
    return []
  }
}

/** 是否拥有某权限码（ADMIN 兜底为全权限，兼容老 token 无 permissions） */
export function hasPerm(code) {
  const role = localStorage.getItem('govai_role')
  if (role === 'ADMIN') return true
  return getPermissions().includes(code)
}

/** 角色是否超管 */
export function isAdmin() {
  return localStorage.getItem('govai_role') === 'ADMIN'
}
