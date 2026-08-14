<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {
  login as loginApi,
  logout as logoutApi,
  getKakaoAuthorizationUrl,
  getGoogleAuthorizationUrl,
} from '@/api/auth'

import kakaoLoginButton
  from '@/assets/images/auth/kakao_login_large_wide.png'

import api from '@/api'

const router = useRouter()
const authStore = useAuthStore()

const userId = ref('')
const password = ref('')
const showPassword = ref(false)
const errorMsg = ref('')
const loading = ref(false)
const kakaoLoading = ref(false)
const googleLoading = ref(false)

// 일반 로그인 API를 호출한다.
async function login() {
  errorMsg.value = ''
  const loginId = userId.value.trim()

  if (!loginId || !password.value) {
    errorMsg.value = '아이디와 비밀번호를 입력해 주세요.'
    return
  }
  // 로그인 버튼을 연속으로 누르는 것을 막는다.
  if (
      loading.value ||
      kakaoLoading.value ||
      googleLoading.value
  ) {
    return
  }

  loading.value = true

  try {
    const response = await loginApi({
      loginId,
      password: password.value,
    })
    const loginData = response.data?.data
    if (!loginData?.accessToken || !loginData?.user) {
      errorMsg.value =
          '로그인 응답을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.'
      return
    }
    //Access Token과 로그인 회원 정보를 Pinia에 저장한다.
    authStore.setToken(loginData.accessToken)
    authStore.setUser(loginData.user)

    try {
      // 이전 로그인 사용자의 프로필 완료 상태를 먼저 제거한다.
      authStore.resetProfileCompletion()

      const accountResponse = await api.get('/accounts')
      const linkedAccounts = accountResponse.data?.data ?? []

      if (linkedAccounts.length > 0) {
        authStore.completeProfile()
      }
    } catch (accountError) {
      console.error(
          '연동 계좌 확인 실패',
          accountError,
      )

      const status = accountError.response?.status
      const isAuthenticationError =
          status === 401

      if (isAuthenticationError) {
        // 인증 세션이 유효하지 않을 때만 서버와 프론트 로그인 상태를 정리한다.
        try {
          await logoutApi()
        } catch (logoutError) {
          console.error(
              '로그인 상태 정리 실패',
              logoutError,
          )
        } finally {
          authStore.logout()
        }

        errorMsg.value =
            '로그인 정보가 유효하지 않습니다. 다시 로그인해 주세요.'

        return
      }

      // 네트워크 오류나 서버 오류에서는 로그인 상태를 유지한다.
      errorMsg.value =
          '연동 계좌 정보를 확인하지 못했습니다. 로그인 버튼을 눌러 다시 시도해 주세요.'

      return
    }

    //RefreshToken은 HttpOnly 쿠키로 자동 저장되므로
    //프론트 JavaScript에서 직접 처리하지 않는다.
    await router.replace('/')
  } catch (error) {
    errorMsg.value =
        error.response?.data?.message
        || '로그인에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

async function startKakaoLogin() {
  if (
      loading.value ||
      kakaoLoading.value ||
      googleLoading.value
  ) {
    return
  }

  errorMsg.value = ''
  kakaoLoading.value = true

  try {
    const response =
        await getKakaoAuthorizationUrl()
    const authorizationUrl =
        response.data?.data?.authorizationUrl
    if (!authorizationUrl) {
      throw new Error(
          '카카오 로그인 주소를 확인할 수 없습니다.',
      )
    }
    const parsedAuthorizationUrl =
        new URL(authorizationUrl)
    if (
        parsedAuthorizationUrl.protocol !== 'https:' ||
        parsedAuthorizationUrl.hostname !== 'kauth.kakao.com'
    ) {
      throw new Error(
          '유효하지 않은 카카오 로그인 주소입니다.',
      )
    }

    window.location.assign(
        parsedAuthorizationUrl.toString(),
    )
  } catch (error) {
    errorMsg.value =
        error.response?.data?.message ||
        error.message ||
        '카카오 로그인을 시작할 수 없습니다.'

    kakaoLoading.value = false
  }
}

async function startGoogleLogin() {
  if (
      loading.value ||
      kakaoLoading.value ||
      googleLoading.value
  ) {
    return
  }

  errorMsg.value = ''
  googleLoading.value = true

  try {
    const response =
        await getGoogleAuthorizationUrl()

    const authorizationUrl =
        response.data?.data?.authorizationUrl

    if (!authorizationUrl) {
      throw new Error(
          'Google 로그인 주소를 확인할 수 없습니다.',
      )
    }

    const parsedAuthorizationUrl =
        new URL(authorizationUrl)

    if (
        parsedAuthorizationUrl.protocol !== 'https:' ||
        parsedAuthorizationUrl.hostname !== 'accounts.google.com'
    ) {
      throw new Error(
          '유효하지 않은 Google 로그인 주소입니다.',
      )
    }

    window.location.assign(
        parsedAuthorizationUrl.toString(),
    )
  } catch (error) {
    errorMsg.value =
        error.response?.data?.message ||
        error.message ||
        'Google 로그인을 시작할 수 없습니다.'

    googleLoading.value = false
  }
}

</script>

<template>
  <div
      class="min-h-screen flex flex-col relative overflow-hidden"
      style="background: linear-gradient(to bottom, #263F8C 0%, #172F6B 100%)"
  >
    <!-- 데코 원형 배경 -->
    <div
        class="absolute top-0 right-0 w-72 h-72 rounded-full pointer-events-none"
        style="background: rgba(255,255,255,0.06); transform: translate(35%, -20%)"
    ></div>
    <div
        class="absolute top-20 right-6 w-56 h-56 rounded-full pointer-events-none"
        style="background: rgba(255,255,255,0.04)"
    ></div>

    <!-- 로고 바 -->
    <div class="relative z-10 flex items-center justify-between px-5 pt-12">
      <span class="text-white font-bold text-xs tracking-[0.2em]">TRIPASS</span>
      <span class="text-lg">✈️</span>
    </div>

    <!-- 히어로 텍스트 -->
    <div class="relative z-10 px-5 pt-5 pb-8">
      <h1 class="text-white text-[26px] font-bold leading-snug">
        여행을 준비하는 가장<br>똑똑한 금융 습관
      </h1>
      <p class="text-blue-200 text-sm mt-3 leading-relaxed">
        목표 설정부터 여행 지출까지, TRIPass와 함께하세요.
      </p>
    </div>

    <!-- 흰색 카드 -->
    <div class="relative z-10 mx-4 bg-white rounded-3xl px-6 pt-7 pb-7">
      <h2 class="text-[22px] font-bold text-gray-900">로그인</h2>
      <p class="text-sm text-gray-400 mt-1">다시 만나서 반가워요</p>

      <p v-if="errorMsg" class="mt-3 text-xs text-red-500">{{ errorMsg }}</p>

      <!-- 아이디 -->
      <div class="mt-5">
        <label class="text-sm font-medium text-gray-700 block mb-1.5">아이디</label>
        <input
            v-model="userId"
            type="text"
            placeholder="아이디를 입력해 주세요"
            @keyup.enter="login"
            class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
        />
      </div>

      <!-- 비밀번호 -->
      <div class="mt-4">
        <label class="text-sm font-medium text-gray-700 block mb-1.5">비밀번호</label>
        <div class="relative">
          <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="비밀번호를 입력해 주세요"
              @keyup.enter="login"
              class="w-full h-12 px-4 pr-12 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
          />
          <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-300"
              :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
              @click="showPassword = !showPassword"
          >
            <svg v-if="showPassword" width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path
                  d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24M1 1l22 22"
                  stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2"
                    stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 찾기 링크 (오른쪽 정렬) -->
      <div class="flex justify-end items-center gap-2 mt-3">
        <button class="text-xs text-gray-400" @click="router.push('/find-id')">아이디 찾기</button>
        <span class="text-gray-300 text-xs">·</span>
        <button class="text-xs text-gray-400" @click="router.push('/find-password')">비밀번호 찾기</button>
      </div>

      <!-- 로그인 버튼 -->
      <button
          type="button"
          @click="login"
          :disabled="loading || kakaoLoading || googleLoading"
          class="w-full h-14 rounded-2xl text-white font-bold text-base mt-5 disabled:opacity-70 transition-opacity"
          style="background: #3B5BDB"
      >
        {{ loading ? '로그인 중...' : '로그인' }}
      </button>

      <!-- 소셜 로그인 -->
      <div class="mt-6 flex items-center gap-3">
        <div class="h-px flex-1 bg-gray-200"></div>

        <span class="text-xs text-gray-400">
          간편 로그인
        </span>

        <div class="h-px flex-1 bg-gray-200"></div>
      </div>

      <div class="mt-4 flex flex-col gap-3">
        <button
            type="button"
            class="h-14 w-full overflow-hidden rounded-2xl border-0 bg-[#FEE500] p-0 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="loading || kakaoLoading || googleLoading"
            aria-label="카카오 로그인"
            @click="startKakaoLogin"
        >
          <img
              :src="kakaoLoginButton"
              alt="카카오 로그인"
              class="block h-full w-full object-fill"
          />
        </button>

        <button
            type="button"
            class="flex h-14 w-full items-center justify-center gap-3 rounded-2xl border border-[#747775] bg-white px-4 text-sm font-medium text-[#1F1F1F] transition-shadow hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="loading || kakaoLoading || googleLoading"
            :aria-busy="googleLoading"
            @click="startGoogleLogin">
          <span
              class="flex h-5 w-5 shrink-0 items-center justify-center"
              aria-hidden="true"
          >
            <svg
                viewBox="0 0 48 48"
                xmlns="http://www.w3.org/2000/svg"
                class="h-5 w-5"
            >
              <path
                  fill="#EA4335"
                  d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
              />
              <path
                  fill="#4285F4"
                  d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"
              />
              <path
                  fill="#FBBC05"
                  d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24s.92 7.54 2.56 10.78l7.97-6.19z"
              />
              <path
                  fill="#34A853"
                  d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
              />
            </svg>
          </span>

          <span>
            {{
              googleLoading
                  ? 'Google 로그인 중...'
                  : 'Google 계정으로 로그인'
            }}
          </span>
        </button>
      </div>

      <p
          v-if="kakaoLoading || googleLoading"
          class="mt-3 text-center text-xs text-gray-400"
      >
        {{
          kakaoLoading
              ? '카카오 로그인 화면으로 이동하고 있어요.'
              : 'Google 로그인 화면으로 이동하고 있어요.'
        }}
      </p>
    </div>

    <!-- 회원가입 링크 -->
    <div class="relative z-10 py-5 text-center">
      <span class="text-sm text-white/60">
        아직 계정이 없나요?
      </span>

      <button
          type="button"
          class="ml-1 text-sm font-semibold text-white"
          @click="router.push('/signup')"
      >
        회원가입
      </button>
    </div>
  </div>
</template>
