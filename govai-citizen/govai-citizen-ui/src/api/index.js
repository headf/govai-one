import { get, post, put, del, uploadFile } from '../utils/request.js'

// 事项
export const fetchCategories = () => get('/api/v1/matters/categories')
export const fetchMatters = (params) => get('/api/v1/matters/list', params)
export const fetchMatterDetail = (id) => get('/api/v1/matters/' + id)

// 认证/用户
export const loginApi = (phone, code) => post('/api/v1/auth/login', { phone, code })
export const wxLoginApi = (openid, nickname, avatarUrl) => post('/api/v1/auth/wx-login', { openid, nickname, avatarUrl })
export const fetchProfile = () => get('/api/v1/users/profile')
export const updateProfile = (data) => put('/api/v1/users/profile', data)

// 申报
export const submitApply = (data) => post('/api/v1/apply', data)
export const fetchRecords = (params) => get('/api/v1/records', params)
export const fetchRecordDetail = (id) => get('/api/v1/records/' + id)
export const fetchProgress = (id) => get('/api/v1/records/' + id + '/progress')
export const mockApprove = (id) => post('/api/v1/records/' + id + '/mock-approve')
export const withdrawRecord = (id) => post('/api/v1/records/' + id + '/withdraw')

// 消息
export const fetchMessages = (params) => get('/api/v1/messages/list', params)
export const fetchUnreadCount = () => get('/api/v1/messages/unread-count')
export const fetchMessageDetail = (id) => get('/api/v1/messages/' + id)
export const readAllMessages = () => post('/api/v1/messages/read-all')

// 评价
export const submitEvaluation = (data) => post('/api/v1/evaluations', data)
export const fetchMyEvaluations = () => get('/api/v1/evaluations/my')
export const fetchEvaluationStats = () => get('/api/v1/evaluations/statistics')

// 政策
export const fetchPolicies = () => get('/api/v1/policies/list')
export const fetchPolicy = (id) => get('/api/v1/policies/' + id)
export const markPolicyRead = (id) => post('/api/v1/policies/' + id + '/read')

// AI (reasoning 模型可能 20s+, 需放宽超时, 否则前端提前超时被当作不可用)
export const aiChat = (message) => post('/api/v1/ai/chat', { message }, { timeout: 60000 })

// 上传
export const upload = (filePath) => uploadFile(filePath, 'file')
