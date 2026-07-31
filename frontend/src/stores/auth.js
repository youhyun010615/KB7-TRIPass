import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || null)
  const user = ref(null)

  const isLoggedIn = computed(() => !!accessToken.value)

  function setToken(token) {
    accessToken.value = token
    localStorage.setItem('accessToken', token)
  }

  function setUser(userInfo) {
    user.value = userInfo
  }

  function logout() {
    accessToken.value = null
    user.value = null
    localStorage.removeItem('accessToken')
  }

  return { accessToken, user, isLoggedIn, setToken, setUser, logout }
})
