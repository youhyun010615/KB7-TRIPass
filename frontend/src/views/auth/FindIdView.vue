<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  findId as findIdApi,
  sendPhoneCode as sendPhoneCodeApi,
  verifyPhoneCode as verifyPhoneCodeApi,
} from '@/api/auth'

import {
  PHONE_NUMBER_PATTERN,
  VERIFICATION_CODE_PATTERN,
} from '@/constants/authValidation'

const router = useRouter()

const name = ref('')
const phone = ref('')
const verifyCode = ref('')
const verificationRequestId = ref(null)

const codeSent = ref(false)
const results = ref([])

const loading = ref(false)
const errorMessage = ref('')

const normalizedPhone = computed(() =>
    phone.value.replace(/\D/g, ''),
)

watch(verifyCode, (value) => {
  verifyCode.value =
      value.replace(/\D/g, '').slice(0, 6)
})

function resetVerification() {
  verifyCode.value = ''
  verificationRequestId.value = null
  codeSent.value = false
  results.value = []
  errorMessage.value = ''
}

async function sendCode() {
  errorMessage.value = ''

  if (!name.value.trim()) {
    errorMessage.value = '이름을 입력해 주세요.'
    return
  }

  if (!PHONE_NUMBER_PATTERN.test(normalizedPhone.value)) {
    errorMessage.value = '휴대전화번호를 정확히 입력해 주세요.'
    return
  }

  loading.value = true

  try {
    const response = await sendPhoneCodeApi({phoneNumber: normalizedPhone.value, purpose: 'FIND_ID',})

    verificationRequestId.value =
        response.data?.data?.requestId

    if (!verificationRequestId.value) {
      throw new Error('인증 요청 ID가 없습니다.')
    }

    verifyCode.value = ''
    codeSent.value = true
    results.value = []
  } catch (error) {
    errorMessage.value =
        error.response?.data?.message
        ?? '인증번호를 발송하지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function confirm() {
  errorMessage.value = ''

  if (!verificationRequestId.value) {
    errorMessage.value = '인증번호를 다시 요청해 주세요.'
    return
  }

  if (!VERIFICATION_CODE_PATTERN.test(verifyCode.value)) {
    errorMessage.value = '6자리 인증번호를 입력해 주세요.'
    return
  }

  loading.value = true

  try {
    await verifyPhoneCodeApi({
      requestId: verificationRequestId.value,
      phoneNumber: normalizedPhone.value,
      code: verifyCode.value,
    })


    const response = await findIdApi({
      name: name.value.trim(),
      phoneNumber: normalizedPhone.value,
      phoneVerificationRequestId:
      verificationRequestId.value,
    })

    results.value =
        response.data?.data?.maskedLoginIds ?? []

    if (results.value.length === 0) {
      errorMessage.value =
          '가입된 아이디를 찾을 수 없습니다.'
    }
  } catch (error) {
    errorMessage.value =
        error.response?.data?.message
        ?? '아이디 찾기에 실패했습니다.'
  } finally {
    loading.value = false
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
        계정을 다시<br>찾아드릴게요
      </h1>
      <p class="text-blue-200 text-sm mt-3 leading-relaxed">
        가입할 때 사용한 휴대폰 번호를 확인해 주세요.
      </p>
    </div>

    <!-- 흰색 카드 -->
    <div class="relative z-10 mx-4 bg-white rounded-3xl px-6 pt-7 pb-7">
      <h2 class="text-[22px] font-bold text-gray-900">아이디 찾기</h2>
      <p class="text-sm text-gray-400 mt-1">본인 인증 후 가입한 아이디를 안내해 드려요.</p>

      <div class="mt-6 flex flex-col gap-4">
        <!-- 이름 -->
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">이름</label>
          <input
            v-model="name"
            type="text"
            placeholder="이름을 입력해 주세요"
            :disabled="codeSent"
            class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
          />
        </div>

        <!-- 휴대폰 번호 -->
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">휴대폰 번호</label>
          <input
            v-model="phone"
            type="tel"
            placeholder="숫자만 입력해 주세요"
            :disabled="codeSent"
            class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
          />
        </div>

        <!-- 인증번호 (발송 후 표시) -->
        <div v-if="codeSent">
          <label class="text-sm font-medium text-gray-700 block mb-1.5">인증번호</label>
          <input
            v-model="verifyCode"
            type="text"
            inputmode="numeric"
            placeholder="6자리 입력"
            maxlength="6"
            class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
          />
          <p class="text-xs mt-1" style="color: #3B5BDB">인증번호가 발송되었습니다.</p>
          <button
              type="button"
              class="mt-2 text-xs text-gray-500 underline disabled:opacity-50"
              :disabled="loading"
              @click="resetVerification"
          >
            입력 정보 수정하기
          </button>
        </div>

        <!-- 결과 -->
        <div
            v-if="results.length > 0"
            class="bg-blue-50 rounded-2xl p-4 text-center"
        >
          <p class="text-xs text-gray-500 mb-2">
            가입된 아이디
          </p>

          <p
              v-for="maskedLoginId in results"
              :key="maskedLoginId"
              class="font-bold text-lg"
              style="color: #3B5BDB"
          >
            {{ maskedLoginId }}
          </p>
        </div>

        <p
            v-if="errorMessage"
            class="text-xs text-red-500"
        >
          {{ errorMessage }}
        </p>

        <!-- 버튼 -->
        <button
          v-if="!codeSent"
          @click="sendCode"
          :disabled="loading"
          class="w-full h-14 rounded-2xl text-white font-bold text-base disabled:opacity-70"
          style="background: #3B5BDB"
        >
          {{ loading ? '발송 중...' : '인증번호 받기' }}
        </button>
        <button
            v-else-if="results.length === 0"
            @click="confirm"
            :disabled="loading"
            class="w-full h-14 rounded-2xl text-white font-bold text-base disabled:opacity-70"
          style="background: #3B5BDB"
        >
          {{ loading ? '확인 중...' : '확인하기' }}
        </button>
        <button
          v-else
          @click="router.push('/login')"
          class="w-full h-14 rounded-2xl text-white font-bold text-base"
          style="background: #3B5BDB"
        >
          로그인하러 가기
        </button>
      </div>
    </div>

    <!-- 로그인으로 돌아가기 -->
    <div class="relative z-10 py-5 text-center">
      <button class="text-white/60 text-sm" @click="router.push('/login')">로그인으로 돌아가기</button>
    </div>
  </div>
</template>
