import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * axios 实例:baseURL 为 /api(vite dev 代理到 8084)
 * 统一处理:注入 token / 401 跳登录 / 错误提示
 */
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('ent_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截:统一解包 + 处理错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一 Result 结构 {code, message, data}
    if (res && res.code !== undefined) {
      if (res.code === 200) {
        return res.data
      }
      if (res.code === 401) {
        ElMessage.error('登录已过期,请重新登录')
        localStorage.removeItem('ent_token')
        router.push('/login')
        return Promise.reject(new Error(res.message || '未认证'))
      }
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    return res
  },
  (error) => {
    ElMessage.error(error.message || '网络错误,请稍后重试')
    return Promise.reject(error)
  }
)

/**
 * http 封装:类型标注为 Promise<T>
 * (axios 拦截器已把后端 Result.data 解包返回,故泛型第二参数为 T)
 */
const http = {
  get<T>(url: string, config?: object): Promise<T> {
    return request.get(url, config) as unknown as Promise<T>
  },
  post<T>(url: string, data?: unknown, config?: object): Promise<T> {
    return request.post(url, data, config) as unknown as Promise<T>
  },
  put<T>(url: string, data?: unknown, config?: object): Promise<T> {
    return request.put(url, data, config) as unknown as Promise<T>
  },
  delete<T>(url: string, config?: object): Promise<T> {
    return request.delete(url, config) as unknown as Promise<T>
  }
}

export default http
