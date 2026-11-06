import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const TOKEN_KEY = 'govai_token'

// 统一 axios 实例：走 Vite proxy，baseURL 指向后端 /api/v1
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

// 请求拦截：自动附带 Bearer token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：解 Result 包装 + 401 跳登录
request.interceptors.response.use(
  (res) => {
    const body = res.data
    // 后端统一 Result：{ code, message, data }
    if (body && body.code === 200) {
      return body.data
    }
    // 非 200（如 400/403/500）统一提示后 reject
    const msg = body?.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(body)
  },
  (err) => {
    const status = err.response?.status
    if (status === 401) {
      ElMessage.error('登录已失效，请重新登录')
      localStorage.removeItem(TOKEN_KEY)
      router.push('/login')
    } else {
      ElMessage.error(err.response?.data?.message || err.message || '网络错误')
    }
    return Promise.reject(err)
  }
)

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export default request