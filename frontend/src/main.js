import './assets/main.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { refreshAccessToken } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

async function initializeApp() {
    const app = createApp(App)
    const pinia = createPinia()

// authStore를 사용하기 전에 Pinia를 Vue 앱에 등록한다.
    app.use(pinia)

    const authStore = useAuthStore(pinia)

    // 기존 로그인 사용자라면 HttpOnly Refresh Token 쿠키를 이용해
    // 새 Access Token을 발급받아 로그인 상태를 복구한다.
    if (authStore.user) {
        try {
            const response = await refreshAccessToken()
            const refreshData = response.data?.data

            if (!refreshData?.accessToken) {
                throw new Error('토큰 재발급 응답이 올바르지 않습니다.')
            }

            authStore.setToken(refreshData.accessToken)
        } catch (error) {
            // Refresh Token이 없거나 만료·폐기된 경우 로그인 정보를 제거한다.
            authStore.logout()
        }
    }
    // 로그인 복구가 끝난 다음 라우터를 등록하고 화면을 실행한다.
    app.use(router)
    app.mount('#app')
}
initializeApp()
