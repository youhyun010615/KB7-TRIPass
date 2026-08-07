<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'

const router = useRouter()

const step = ref(1)
const loading = ref(false)

// Step 1
const name = ref('')
const userId = ref('')
const password = ref('')
const passwordConfirm = ref('')
const showPassword = ref(false)
const showPasswordConfirm = ref(false)
const idCheckStatus = ref('idle')
const idCheckMessage = ref('')

const USER_ID_PATTERN = /^[A-Za-z0-9]{6,20}$/
const isUserIdValid = computed(() => USER_ID_PATTERN.test(userId.value))
const canProceedStep1 = computed(() => (
  name.value.trim()
  && isUserIdValid.value
  && idCheckStatus.value === 'available'
  && password.value.length >= 8
  && password.value === passwordConfirm.value
))

// Step 2
const phone = ref('')
const verifyCode = ref('')
const codeSent = ref(false)

const errors = ref({})

watch(userId, () => {
  idCheckStatus.value = 'idle'
  idCheckMessage.value = ''
  delete errors.value.userId
})

async function checkUserId() {
  if (!isUserIdValid.value || idCheckStatus.value === 'checking') return

  idCheckStatus.value = 'checking'
  idCheckMessage.value = '아이디를 확인하고 있어요.'
  delete errors.value.userId

  try {
    const response = await api.get('/auth/check-id', {
      params: { loginId: userId.value },
    })
    const available = response.data?.data?.available === true
    idCheckStatus.value = available ? 'available' : 'duplicate'
    idCheckMessage.value = available
      ? '사용 가능한 아이디예요.'
      : '이미 사용 중인 아이디예요.'
  } catch (error) {
    idCheckStatus.value = 'error'
    idCheckMessage.value = error.response?.data?.message || '중복 확인 중 오류가 발생했어요. 다시 시도해 주세요.'
  }
}

function validateStep1() {
  errors.value = {}
  if (!name.value.trim()) errors.value.name = '이름을 입력해 주세요.'
  if (!userId.value) errors.value.userId = '아이디를 입력해 주세요.'
  else if (!isUserIdValid.value) errors.value.userId = '6~20자 영문·숫자로 입력해 주세요.'
  else if (idCheckStatus.value !== 'available') errors.value.userId = '아이디 중복 확인을 완료해 주세요.'
  if (!password.value) errors.value.password = '비밀번호를 입력해 주세요.'
  else if (password.value.length < 8) errors.value.password = '8자 이상 입력해 주세요.'
  if (password.value !== passwordConfirm.value) errors.value.passwordConfirm = '비밀번호가 일치하지 않아요.'
  return Object.keys(errors.value).length === 0
}

function nextStep() {
  if (!validateStep1()) return
  step.value = 2
}

function sendCode() {
  if (!phone.value) return
  codeSent.value = true
}

function signup() {
  if (!phone.value || !verifyCode.value) return
  loading.value = true
  setTimeout(() => {
    loading.value = false
    router.push('/login')
  }, 1000)
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
        여행을 위한 첫 패스,<br>지금 만들어 보세요
      </h1>
      <p class="text-blue-200 text-sm mt-3 leading-relaxed">
        필요한 정보만 간단히 입력하면 준비가 끝나요.
      </p>
    </div>

    <!-- 흰색 카드 -->
    <div class="relative z-10 mx-4 bg-white rounded-3xl px-6 pt-7 pb-7">
      <h2 class="text-[22px] font-bold text-gray-900">회원가입</h2>
      <p class="text-sm font-semibold mt-1" style="color: #3B5BDB">
        {{ step }} / 2 기본 정보
      </p>

      <!-- Step 1: 기본 정보 -->
      <div v-if="step === 1" class="mt-5 flex flex-col gap-4">
        <!-- 이름 -->
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">이름</label>
          <input
            v-model="name"
            type="text"
            placeholder="이름을 입력해 주세요"
            class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
            :class="errors.name ? 'border-red-400' : ''"
          />
          <p v-if="errors.name" class="text-xs text-red-500 mt-1">{{ errors.name }}</p>
        </div>

        <!-- 아이디 -->
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">아이디</label>
          <div class="flex gap-2">
            <input
              v-model="userId"
              type="text"
              inputmode="text"
              autocomplete="username"
              maxlength="20"
              placeholder="영문·숫자 6~20자"
              class="min-w-0 flex-1 h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
              :class="errors.userId || idCheckStatus === 'duplicate' || idCheckStatus === 'error' ? 'border-red-400' : idCheckStatus === 'available' ? 'border-blue-500' : ''"
              @keyup.enter="checkUserId"
            />
            <button
              type="button"
              :disabled="!isUserIdValid || idCheckStatus === 'checking'"
              class="h-12 shrink-0 px-3.5 rounded-xl text-sm font-semibold whitespace-nowrap border transition-colors disabled:text-gray-400 disabled:border-gray-200 disabled:bg-gray-100"
              :class="isUserIdValid && idCheckStatus !== 'checking' ? 'text-[#3B5BDB] border-[#3B5BDB] bg-white' : ''"
              @click="checkUserId"
            >
              {{ idCheckStatus === 'checking' ? '확인 중' : '중복 확인' }}
            </button>
          </div>
          <p v-if="errors.userId" class="text-xs text-red-500 mt-1">{{ errors.userId }}</p>
          <p
            v-else-if="idCheckMessage"
            role="status"
            aria-live="polite"
            class="text-xs mt-1"
            :class="idCheckStatus === 'available' ? 'text-blue-600' : idCheckStatus === 'checking' ? 'text-gray-500' : 'text-red-500'"
          >
            {{ idCheckMessage }}
          </p>
        </div>

        <!-- 비밀번호 -->
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">비밀번호</label>
          <div class="relative">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="영문·숫자·특수문자 8자 이상"
              class="w-full h-12 px-4 pr-12 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
              :class="errors.password ? 'border-red-400' : ''"
            />
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-300" @click="showPassword = !showPassword">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path v-if="showPassword" d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24M1 1l22 22" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <template v-else>
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2"/>
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                </template>
              </svg>
            </button>
          </div>
          <p v-if="errors.password" class="text-xs text-red-500 mt-1">{{ errors.password }}</p>
        </div>

        <!-- 비밀번호 확인 -->
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">비밀번호 확인</label>
          <div class="relative">
            <input
              v-model="passwordConfirm"
              :type="showPasswordConfirm ? 'text' : 'password'"
              placeholder="한 번 더 입력해 주세요"
              class="w-full h-12 px-4 pr-12 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
              :class="errors.passwordConfirm ? 'border-red-400' : ''"
            />
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-300" @click="showPasswordConfirm = !showPasswordConfirm">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path v-if="showPasswordConfirm" d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24M1 1l22 22" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <template v-else>
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2"/>
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                </template>
              </svg>
            </button>
          </div>
          <p v-if="errors.passwordConfirm" class="text-xs text-red-500 mt-1">{{ errors.passwordConfirm }}</p>
        </div>

        <!-- 다음 버튼 -->
        <button
          type="button"
          @click="nextStep"
          :disabled="!canProceedStep1"
          class="w-full h-14 rounded-2xl text-white font-bold text-base mt-2 transition-opacity disabled:opacity-40 disabled:cursor-not-allowed"
          style="background: #3B5BDB"
        >
          다음
        </button>
      </div>

      <!-- Step 2: 휴대폰 인증 -->
      <div v-else class="mt-5 flex flex-col gap-4">
        <!-- 휴대폰 번호 -->
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">휴대폰 번호</label>
          <div class="flex gap-2">
            <input
              v-model="phone"
              type="tel"
              placeholder="010-0000-0000"
              class="flex-1 h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
            />
            <button
              class="h-12 px-4 rounded-xl text-sm font-semibold whitespace-nowrap border"
              :class="codeSent ? 'text-gray-400 border-gray-200' : 'text-[#3B5BDB] border-[#3B5BDB]'"
              @click="sendCode"
            >
              인증요청
            </button>
          </div>
        </div>

        <!-- 인증번호 -->
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">인증번호</label>
          <input
            v-model="verifyCode"
            type="text"
            placeholder="6자리 입력"
            maxlength="6"
            class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
          />
          <p v-if="codeSent" class="text-xs mt-1" style="color: #3B5BDB">인증번호가 발송되었습니다.</p>
        </div>

        <!-- 완료 버튼 -->
        <button
          @click="signup"
          :disabled="loading"
          class="w-full h-14 rounded-2xl text-white font-bold text-base mt-2 disabled:opacity-70"
          style="background: #3B5BDB"
        >
          {{ loading ? '처리 중...' : '완료' }}
        </button>
      </div>
    </div>

    <!-- 로그인 링크 -->
    <div class="relative z-10 py-5 text-center">
      <span class="text-white/60 text-sm">이미 계정이 있나요? </span>
      <button class="text-white font-semibold text-sm" @click="router.push('/login')">로그인</button>
    </div>
  </div>
</template>
