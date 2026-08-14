import axios from 'axios'
import {useAuthStore} from '@/stores/auth'

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
    timeout: 10000,
    // 로그인 시 받은 Refresh Token HttpOnly 쿠키를
    // 이후 재발급과 로그아웃 요청에 자동으로 포함한다.
    withCredentials: true,
})
// 동시에 여러 요청에서 401이 발생해도 재발급은 한 번만 요청한다.
let refreshPromise = null

api.interceptors.request.use((config) => {
    const authStore = useAuthStore()
    if (authStore.accessToken) {
        config.headers.Authorization = `Bearer ${authStore.accessToken}`
    }
    return config
})

// 토큰 없이 사용할 수 있는 인증 API
const publicAuthUrls = [
    '/auth/check-id',
    '/auth/login',
    '/auth/signup',
    '/auth/phone/send',
    '/auth/phone/verify',
    '/auth/find-id',
    '/auth/reset-password',
    '/auth/refresh',
    '/auth/logout',
    '/auth/social/kakao',
]

function isPublicAuthRequest(url) {
    return publicAuthUrls.some((publicUrl) =>
        url?.startsWith(publicUrl),
    )
}

api.interceptors.response.use(
    (response) => response,

    async (error) => {
        const authStore = useAuthStore()
        const originalRequest = error.config
        const status = error.response?.status
        const requestUrl = originalRequest?.url ?? ''

        // 401이 아니거나 공개 인증 API에서 발생한 오류라면
        // 호출한 화면에서 직접 처리한다.
        if (
            status !== 401 ||
            isPublicAuthRequest(requestUrl)
        ) {
            return Promise.reject(error)
        }

        // 재발급 요청 자체가 실패했다면 로그인 상태를 제거한다.
        if (requestUrl.startsWith('/auth/refresh')) {
            authStore.logout()

            if (window.location.pathname !== '/login') {
                window.location.href = '/login'
            }

            return Promise.reject(error)
        }

        // 같은 요청을 두 번 이상 재시도하지 않는다.
        if (originalRequest._retry) {
            authStore.logout()

            if (window.location.pathname !== '/login') {
                window.location.href = '/login'
            }

            return Promise.reject(error)
        }

        originalRequest._retry = true

        try {
            // 여러 API가 동시에 실패해도 재발급은 한 번만 요청한다.
            if (!refreshPromise) {
                refreshPromise = api
                    .post('/auth/refresh')
                    .then((response) => {
                        const refreshData = response.data?.data

                        if (!refreshData?.accessToken) {
                            throw new Error(
                                '토큰 재발급 응답이 올바르지 않습니다.',
                            )
                        }

                        authStore.setToken(refreshData.accessToken)

                        return refreshData.accessToken
                    })
                    .finally(() => {
                        refreshPromise = null
                    })
            }

            const newAccessToken = await refreshPromise

            // 실패했던 요청에 새 토큰을 넣고 다시 요청한다.
            originalRequest.headers =
                originalRequest.headers ?? {}

            originalRequest.headers.Authorization =
                `Bearer ${newAccessToken}`

            return api(originalRequest)
        } catch (refreshError) {
            authStore.logout()

            if (window.location.pathname !== '/login') {
                window.location.href = '/login'
            }

            return Promise.reject(refreshError)
        }
    },
)

export default api
