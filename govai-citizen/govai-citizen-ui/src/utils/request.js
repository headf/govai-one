import { BASE_URL, getToken, clearToken, toLogin } from './config.js'

/**
 * 统一请求封装:
 * - 自动拼接 base, 注入 Authorization: Bearer token
 * - 解包后端 Result{code,message,data}
 * - code 401 -> 清 token 跳登录
 */
export function request(options) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header = Object.assign(
      { 'Content-Type': 'application/json' },
      options.header || {}
    )
    if (token) {
      header['Authorization'] = 'Bearer ' + token
    }
    // 文件上传不走 JSON header
    const isUpload = options.isUpload
    if (isUpload) {
      delete header['Content-Type']
    }
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      timeout: options.timeout || 15000,
      success: (res) => {
        if (res.statusCode === 200) {
          const body = res.data
          if (body && typeof body === 'object' && 'code' in body) {
            if (body.code === 200) {
              resolve(body.data)
            } else if (body.code === 401) {
              clearToken()
              uni.showToast({ title: '请先登录', icon: 'none' })
              setTimeout(() => toLogin(), 600)
              reject(new Error(body.message || '未登录'))
            } else {
              uni.showToast({ title: body.message || '请求失败', icon: 'none' })
              reject(new Error(body.message || '请求失败'))
            }
          } else {
            resolve(body)
          }
        } else if (res.statusCode === 401) {
          clearToken()
          uni.showToast({ title: '请先登录', icon: 'none' })
          setTimeout(() => toLogin(), 600)
          reject(new Error('未登录'))
        } else {
          uni.showToast({ title: '网络错误(' + res.statusCode + ')', icon: 'none' })
          reject(new Error('HTTP ' + res.statusCode))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

export const get = (url, data) => request({ url, method: 'GET', data })
export const post = (url, data, extra) => request(Object.assign({ url, method: 'POST', data }, extra || {}))
export const put = (url, data) => request({ url, method: 'PUT', data })
export const del = (url, data) => request({ url, method: 'DELETE', data })

// 文件上传(返回与普通接口一致的 data)
export function uploadFile(filePath, name = 'file') {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: BASE_URL + '/api/v1/upload',
      filePath,
      name,
      header: token ? { Authorization: 'Bearer ' + token } : {},
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          if (body.code === 200) {
            resolve(body.data)
          } else {
            uni.showToast({ title: body.message || '上传失败', icon: 'none' })
            reject(new Error(body.message))
          }
        } catch (e) {
          reject(new Error('上传响应解析失败'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject(err)
      }
    })
  })
}
