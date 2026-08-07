<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const userId = ref('')
const password = ref('')
const showPassword = ref(false)
const errorMsg = ref('')
const loading = ref(false)

function login() {
  errorMsg.value = ''
  if (!userId.value || !password.value) return
  loading.value = true
  setTimeout(() => {
    loading.value = false
    if (userId.value === 'tripass' && password.value === '1234') {
      authStore.setToken('mock-token-tripass')
      authStore.setUser({
        id: 1,
        loginId: userId.value,
        name: '권유현',
        phoneNumber: '010-1234-5678',
        loginProvider: 'LOCAL',
      })
      router.push('/')
    } else {
      errorMsg.value = '아이디 또는 비밀번호가 올바르지 않아요.'
    }
  }, 600)
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
          <button class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-300" @click="showPassword = !showPassword">
            <svg v-if="showPassword" width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24M1 1l22 22" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
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
        @click="login"
        :disabled="loading"
        class="w-full h-14 rounded-2xl text-white font-bold text-base mt-5 disabled:opacity-70 transition-opacity"
        style="background: #3B5BDB"
      >
        {{ loading ? '로그인 중...' : '로그인' }}
      </button>

      <!-- 소셜 로그인 -->
      <div class="flex items-center gap-3 mt-6">
        <div class="flex-1 h-px bg-gray-200"></div>
        <span class="text-xs text-gray-400">간편 로그인</span>
        <div class="flex-1 h-px bg-gray-200"></div>
      </div>
      <div class="flex justify-center gap-4 mt-4">
        <button class="w-12 h-12 rounded-full bg-[#FEE500] flex items-center justify-center font-bold text-[#3C1E1E] text-lg">K</button>
        <button class="w-12 h-12 rounded-full bg-white border border-gray-200 flex items-center justify-center font-bold text-[#3B5BDB] text-lg">G</button>
      </div>
    </div>

    <!-- 회원가입 링크 -->
    <div class="relative z-10 py-5 text-center">
      <span class="text-white/60 text-sm">아직 계정이 없나요? </span>
      <button class="text-white font-semibold text-sm" @click="router.push('/signup')">회원가입</button>
    </div>
  </div>
</template>
