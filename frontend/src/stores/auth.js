import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || null)
  const user = ref(null)
  const isProfileComplete = ref(localStorage.getItem('isProfileComplete') === 'true')

  const isLoggedIn = computed(() => !!accessToken.value)

  function setToken(token) {
    accessToken.value = token
    localStorage.setItem('accessToken', token)
  }

  function setUser(userInfo) {
    user.value = userInfo
  }

  function completeProfile() {
    isProfileComplete.value = true
    localStorage.setItem('isProfileComplete', 'true')
  }

  function logout() {
    accessToken.value = null
    user.value = null
    isProfileComplete.value = false
    localStorage.removeItem('accessToken')
    localStorage.removeItem('isProfileComplete')
  }

  return { accessToken, user, isLoggedIn, isProfileComplete, setToken, setUser, completeProfile, logout }
})
