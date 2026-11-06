// 平台差异化配置: H5 走 vite proxy(相对路径), 小程序直连后端
// #ifdef H5
export const BASE_URL = ''
// #endif
// #ifndef H5
// 真机/小程序不能访问 localhost, 用电脑局域网 IP(ipconfig 查看, 随网络变化)
// 当前: 10.25.135.241 (2026-09-21)
export const BASE_URL = 'http://10.25.135.241:8083'
// #endif

const TOKEN_KEY = 'govai_token'
const USER_KEY = 'govai_user'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function clearToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

export function getStoredUser() {
  return uni.getStorageSync(USER_KEY) || null
}

export function setStoredUser(user) {
  uni.setStorageSync(USER_KEY, user)
}

export function clearStoredUser() {
  uni.removeStorageSync(USER_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}

// 统一跳登录(带当前页回跳)
export function toLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}
