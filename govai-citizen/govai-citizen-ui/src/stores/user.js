import { defineStore } from 'pinia'
import { post, get, put } from '../utils/request.js'
import {
  setToken, clearToken, setStoredUser, clearStoredUser,
  getStoredUser, getToken
} from '../utils/config.js'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: getStoredUser()
  }),
  getters: {
    isLogin: (state) => !!state.token,
    displayName: (state) => (state.userInfo && (state.userInfo.userName || state.userInfo.phone)) || ''
  },
  actions: {
    async login(phone, code) {
      const data = await post('/api/v1/auth/login', { phone, code })
      this.token = data.token
      this.userInfo = data
      setToken(data.token)
      setStoredUser(data)
      return data
    },
    async wxLogin(openid, nickname, avatarUrl) {
      const data = await post('/api/v1/auth/wx-login', { openid, nickname, avatarUrl })
      this.token = data.token
      this.userInfo = data
      setToken(data.token)
      setStoredUser(data)
      return data
    },
    async fetchProfile() {
      const profile = await get('/api/v1/users/profile')
      this.userInfo = Object.assign({}, this.userInfo, profile)
      setStoredUser(this.userInfo)
      return profile
    },
    async updateProfile(payload) {
      const profile = await put('/api/v1/users/profile', payload)
      this.userInfo = Object.assign({}, this.userInfo, profile)
      setStoredUser(this.userInfo)
      return profile
    },
    logout() {
      this.token = ''
      this.userInfo = null
      clearToken()
      clearStoredUser()
    }
  }
})
