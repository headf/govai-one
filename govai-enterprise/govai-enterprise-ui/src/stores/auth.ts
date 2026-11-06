import { defineStore } from 'pinia'
import { login as apiLogin, getMe } from '@/api'
import type { LoginResp } from '@/types'

/** 企业端登录态 store(持久化到 localStorage) */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('ent_token') || '',
    user: (JSON.parse(localStorage.getItem('ent_user') || 'null') as LoginResp | null) || null
  }),

  getters: {
    isLogin: (state) => !!state.token,
    role: (state) => state.user?.role || '',
    enterpriseName: (state) => state.user?.enterpriseName || ''
  },

  actions: {
    async login(username: string, password: string) {
      const user = await apiLogin({ username, password })
      this.token = user.token
      this.user = user
      localStorage.setItem('ent_token', user.token)
      localStorage.setItem('ent_user', JSON.stringify(user))
    },

    async fetchMe() {
      try {
        const user = await getMe()
        this.user = user
        localStorage.setItem('ent_user', JSON.stringify(user))
      } catch {
        /* 忽略,由路由守卫处理 */
      }
    },

    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('ent_token')
      localStorage.removeItem('ent_user')
    }
  }
})
