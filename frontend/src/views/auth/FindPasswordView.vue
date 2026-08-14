<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  resetPassword as resetPasswordApi,
  sendPhoneCode as sendPhoneCodeApi,
  verifyPhoneCode as verifyPhoneCodeApi,
} from '@/api/auth'

import {
  PASSWORD_PATTERN,
  PHONE_NUMBER_PATTERN,
  VERIFICATION_CODE_PATTERN,
} from '@/constants/authValidation'

const router = useRouter()

const loginId = ref('')
const phone = ref('')
const verificationCode = ref('')
const verificationRequestId = ref(null)
const newPassword = ref('')
const newPasswordConfirm = ref('')

const step = ref('request')
const loading = ref(false)
const errorMessage = ref('')

const normalizedPhone = computed(() =>
    phone.value.replace(/\D/g, ''),
)

watch(verificationCode, (value) => {
  verificationCode.value =
      value.replace(/\D/g, '').slice(0, 6)
})

const passwordsMatch = computed(() =>
    newPassword.value.length > 0
    && newPassword.value === newPasswordConfirm.value,
)

function resetVerification() {
  verificationCode.value = ''
  verificationRequestId.value = null
  newPassword.value = ''
  newPasswordConfirm.value = ''
  step.value = 'request'
  errorMessage.value = ''
}

async function sendCode() {
  errorMessage.value = ''

  if (!loginId.value.trim()) {
    errorMessage.value = '아이디를 입력해 주세요.'
    return
  }

  if (!PHONE_NUMBER_PATTERN.test(normalizedPhone.value)) {
    errorMessage.value = '휴대전화번호를 정확히 입력해 주세요.'
    return
  }

  loading.value = true

  try {
    const response = await sendPhoneCodeApi({phoneNumber: normalizedPhone.value, purpose: 'RESET_PASSWORD',})

    verificationRequestId.value =
        response.data?.data?.requestId

    if (!verificationRequestId.value) {
      throw new Error('인증 요청 ID가 없습니다.')
    }

    verificationCode.value = ''
    step.value = 'verify'
  } catch (error) {
    errorMessage.value =
        error.response?.data?.message
        ?? '인증번호를 발송하지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function verifyCode() {
  errorMessage.value = ''

  if (!verificationRequestId.value) {
    errorMessage.value = '인증번호를 다시 요청해 주세요.'
    return
  }

  if (!VERIFICATION_CODE_PATTERN.test(verificationCode.value)) {
    errorMessage.value = '6자리 인증번호를 입력해 주세요.'
    return
  }

  loading.value = true

  try {
    await verifyPhoneCodeApi({
      requestId: verificationRequestId.value,
      phoneNumber: normalizedPhone.value,
      code: verificationCode.value,
    })

    step.value = 'reset'
  } catch (error) {
    errorMessage.value =
        error.response?.data?.message
        ?? '인증번호 확인에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

async function submitNewPassword() {
  errorMessage.value = ''


  if (!PASSWORD_PATTERN.test(newPassword.value)) {
    errorMessage.value =
        '비밀번호는 영문, 숫자, 특수문자를 포함해 8~64자로 입력해 주세요.'
    return
  }

  if (!passwordsMatch.value) {
    errorMessage.value =
        '새 비밀번호와 비밀번호 확인이 일치하지 않습니다.'
    return
  }

  loading.value = true

  try {
    await resetPasswordApi({
      loginId: loginId.value.trim(),
      phoneNumber: normalizedPhone.value,
      phoneVerificationRequestId:
      verificationRequestId.value,
      newPassword: newPassword.value,
    })

    step.value = 'done'
  } catch (error) {
    errorMessage.value =
        error.response?.data?.message
        ?? '비밀번호를 재설정하지 못했습니다.'
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
    <div
        class="absolute top-0 right-0 w-72 h-72 rounded-full pointer-events-none"
        style="background: rgba(255,255,255,0.06); transform: translate(35%, -20%)"
    />

    <div
        class="absolute top-20 right-6 w-56 h-56 rounded-full pointer-events-none"
        style="background: rgba(255,255,255,0.04)"
    />

    <div class="relative z-10 flex items-center justify-between px-5 pt-12">
      <span class="text-white font-bold text-xs tracking-[0.2em]">
        TRIPASS
      </span>
      <span class="text-lg">✈️</span>
    </div>

    <div class="relative z-10 px-5 pt-5 pb-8">
      <h1 class="text-white text-[26px] font-bold leading-snug">
        비밀번호를 안전하게<br>
        다시 설정하세요
      </h1>

      <p class="text-blue-200 text-sm mt-3 leading-relaxed">
        아이디와 휴대폰 번호로 본인 확인을 진행해요.
      </p>
    </div>

    <div class="relative z-10 mx-4 bg-white rounded-3xl px-6 pt-7 pb-7">
      <h2 class="text-[22px] font-bold text-gray-900">
        비밀번호 찾기
      </h2>

      <p class="text-sm text-gray-400 mt-1">
        본인 인증 후 새로운 비밀번호를 설정할 수 있어요.
      </p>

      <div
          v-if="step === 'request' || step === 'verify'"
          class="mt-6 flex flex-col gap-4"
      >
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">
            아이디
          </label>

          <input
              v-model="loginId"
              type="text"
              :disabled="step !== 'request'"
              placeholder="아이디를 입력해 주세요"
              class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white disabled:bg-gray-100 disabled:text-gray-500"
          >
        </div>

        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">
            휴대폰 번호
          </label>

          <input
              v-model="phone"
              type="tel"
              :disabled="step !== 'request'"
              placeholder="숫자만 입력해 주세요"
              class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white disabled:bg-gray-100 disabled:text-gray-500"
          >
        </div>

        <div v-if="step === 'verify'">
          <label class="text-sm font-medium text-gray-700 block mb-1.5">
            인증번호
          </label>

          <input
              v-model="verificationCode"
              type="text"
              inputmode="numeric"
              maxlength="6"
              placeholder="6자리 입력"
              class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
          >

          <p class="text-xs mt-1" style="color: #3B5BDB">
            인증번호가 발송되었습니다.
          </p>
          <button
              type="button"
              class="mt-2 text-xs text-gray-500 underline disabled:opacity-50"
              :disabled="loading"
              @click="resetVerification"
          >
            입력 정보 수정하기
          </button>
        </div>

        <button
            v-if="step === 'request'"
            type="button"
            :disabled="loading"
            class="w-full h-14 rounded-2xl text-white font-bold text-base disabled:opacity-70"
            style="background: #3B5BDB"
            @click="sendCode"
        >
          {{ loading ? '발송 중...' : '인증번호 받기' }}
        </button>

        <button
            v-else
            type="button"
            :disabled="loading"
            class="w-full h-14 rounded-2xl text-white font-bold text-base disabled:opacity-70"
            style="background: #3B5BDB"
            @click="verifyCode"
        >
          {{ loading ? '확인 중...' : '인증번호 확인' }}
        </button>
      </div>

      <div
          v-else-if="step === 'reset'"
          class="mt-6 flex flex-col gap-4"
      >
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">
            새 비밀번호
          </label>

          <input
              v-model="newPassword"
              type="password"
              autocomplete="new-password"
              placeholder="새 비밀번호를 입력해 주세요"
              class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
          >

          <p class="text-xs text-gray-400 mt-1">
            영문, 숫자, 특수문자를 포함해 8~64자로 입력해 주세요.
          </p>
        </div>

        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">
            새 비밀번호 확인
          </label>

          <input
              v-model="newPasswordConfirm"
              type="password"
              autocomplete="new-password"
              placeholder="새 비밀번호를 다시 입력해 주세요"
              class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
          >

          <p
              v-if="newPasswordConfirm && !passwordsMatch"
              class="text-xs text-red-500 mt-1"
          >
            비밀번호가 일치하지 않습니다.
          </p>
        </div>

        <button
            type="button"
            :disabled="loading"
            class="w-full h-14 rounded-2xl text-white font-bold text-base disabled:opacity-70"
            style="background: #3B5BDB"
            @click="submitNewPassword"
        >
          {{ loading ? '변경 중...' : '비밀번호 재설정' }}
        </button>
      </div>

      <div
          v-else
          class="mt-6 flex flex-col gap-4"
      >
        <div class="bg-blue-50 rounded-2xl p-5 text-center">
          <p class="text-2xl mb-2">✅</p>
          <p class="font-semibold text-gray-800">
            비밀번호가 재설정되었습니다.
          </p>
          <p class="text-sm text-gray-500 mt-1">
            새로운 비밀번호로 로그인해 주세요.
          </p>
        </div>

        <button
            type="button"
            class="w-full h-14 rounded-2xl text-white font-bold text-base"
            style="background: #3B5BDB"
            @click="router.push('/login')"
        >
          로그인하러 가기
        </button>
      </div>

      <p
          v-if="errorMessage"
          class="text-xs text-red-500 mt-4"
      >
        {{ errorMessage }}
      </p>
    </div>

    <div class="relative z-10 py-5 text-center">
      <button
          class="text-white/60 text-sm"
          @click="router.push('/login')"
      >
        로그인으로 돌아가기
      </button>
    </div>
  </div>
</template>
