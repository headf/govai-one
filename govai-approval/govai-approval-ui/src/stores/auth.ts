import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || null)
  const userName = ref(localStorage.getItem('userName') || '')
  const role = ref(localStorage.getItem('role') || '')

  function setAuth(data: { token: string; userId: number; userName: string; role: string }) {
    token.value = data.token
    userId.value = data.userId
    userName.value = data.userName
    role.value = data.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('userName', data.userName)
    localStorage.setItem('role', data.role)
  }

  function logout() {
    token.value = ''
    userId.value = null
    userName.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('userName')
    localStorage.removeItem('role')
  }

  return { token, userId, userName, role, setAuth, logout }
})
