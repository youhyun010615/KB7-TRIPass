import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  // Access Token은 브라우저 메모리에서만 관리한다.
  const accessToken = ref(null)
  // 사용자 공개 정보는 새로고침 후에도 화면에 표시하기 위해 저장한다.
  const user = ref(JSON.parse(localStorage.getItem('tripass-user') || 'null',),)
  const isProfileComplete = ref(localStorage.getItem('isProfileComplete') === 'true',)

  const isLoggedIn = computed(
      () => Boolean(accessToken.value),)

  // 새 Access Token을 메모리에 저장한다.
  function setToken(token) {
    accessToken.value = token
    // 기존 버전에서 저장했던 Access Token을 제거한다.
    localStorage.removeItem('accessToken')
  }

  // 로그인한 회원의 공개 정보를 저장한다.
  function setUser(userInfo) {
    user.value = userInfo
    localStorage.setItem(
        'tripass-user',
        JSON.stringify(userInfo),
    )
  }

  // 기존 회원 정보의 일부를 변경한다.
  function updateUser(userInfo) {
    setUser({...user.value, ...userInfo,})
  }

  function completeProfile() {
    isProfileComplete.value = true
    localStorage.setItem('isProfileComplete', 'true')
  }

  function resetProfileCompletion() {
    isProfileComplete.value = false
    localStorage.removeItem('isProfileComplete')
  }

  // 프론트에 저장된 로그인 정보를 제거한다.
  function logout() {
    accessToken.value = null
    user.value = null
    isProfileComplete.value = false
    localStorage.removeItem('accessToken')
    localStorage.removeItem('tripass-user')
    localStorage.removeItem('isProfileComplete')
  }

  return { accessToken, user, isLoggedIn, isProfileComplete, setToken, setUser, updateUser, completeProfile, resetProfileCompletion, logout }
})
