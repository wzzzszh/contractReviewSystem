import { defineStore } from 'pinia'
import { login as loginApi } from '../api/auth'
import { storage } from '../api/request'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: storage.getUser()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.user?.accessToken || state.user?.token),
    displayName: (state) => state.user?.nickname || state.user?.username || '用户'
  },
  actions: {
    async login(form) {
      const data = await loginApi(form)
      storage.setSession(data)
      this.user = data
      return data
    },
    logout() {
      storage.clearSession()
      this.user = null
    }
  }
})
