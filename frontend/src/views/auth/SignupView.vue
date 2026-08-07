<script setup>
import {computed, onBeforeUnmount, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {
  checkLoginId as checkLoginIdApi,
  sendPhoneCode as sendPhoneCodeApi,
  signup as signupApi,
  verifyPhoneCode as verifyPhoneCodeApi,
} from '@/api/auth'

const router = useRouter()

// 회원가입 단계
const step = ref(1)

// 기본 정보
const name = ref('')
const userId = ref('')
const password = ref('')
const passwordConfirm = ref('')
const showPassword = ref(false)
const showPasswordConfirm = ref(false)

// 아이디 중복 확인 상태
const idCheckStatus = ref('idle')
const idCheckMessage = ref('')

// 휴대전화 인증 상태
const phone = ref('')
const verifyCode = ref('')
const codeSent = ref(false)
const phoneVerified = ref(false)
const phoneMessage = ref('')
const phoneMessageType = ref('info')
const phoneVerificationRequestId = ref(null)

// 인증번호 만료 및 재전송 시간
const codeExpiresIn = ref(0)
const resendSeconds = ref(0)
let verificationTimer = null

// API 요청 처리 상태
const phoneSending = ref(false)
const phoneVerifying = ref(false)
const signupLoading = ref(false)

const errors = ref({})

// 백엔드와 동일한 입력 형식
const USER_ID_PATTERN = /^[A-Za-z0-9]{6,20}$/
const PASSWORD_PATTERN =
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d\s])\S{8,64}$/
const PHONE_PATTERN = /^010\d{8}$/
const VERIFICATION_CODE_PATTERN = /^\d{6}$/

const normalizedPhone = computed(() =>
    phone.value.replace(/[^0-9]/g, ''),
)

const isUserIdValid = computed(() =>
    USER_ID_PATTERN.test(userId.value.trim()),
)

const isPasswordValid = computed(() =>
    PASSWORD_PATTERN.test(password.value),
)

const isPhoneValid = computed(() =>
    PHONE_PATTERN.test(normalizedPhone.value),
)

const isVerificationCodeValid = computed(() =>
    VERIFICATION_CODE_PATTERN.test(verifyCode.value),
)

const canProceedStep1 = computed(
    () =>
        name.value.trim().length >= 2 &&
        isUserIdValid.value &&
        idCheckStatus.value === 'available' &&
        isPasswordValid.value &&
        password.value === passwordConfirm.value,
)

watch(userId, () => {
  idCheckStatus.value = 'idle'
  idCheckMessage.value = ''
  delete errors.value.userId
})

// 휴대전화번호가 변경되면 기존 인증 결과를 무효화한다.
watch(phone, () => {
  if (
      codeSent.value ||
      phoneVerified.value ||
      phoneVerificationRequestId.value
  ) {
    resetPhoneVerification()
  }

  delete errors.value.phone
})

// 인증번호는 숫자 6자리까지만 입력한다.
watch(verifyCode, (value) => {
  verifyCode.value = value.replace(/[^0-9]/g, '').slice(0, 6)
})

// 아이디 중복 확인
async function checkUserId() {
  if (!isUserIdValid.value || idCheckStatus.value === 'checking') {
    return
  }

  idCheckStatus.value = 'checking'
  idCheckMessage.value = '아이디를 확인하고 있습니다.'
  delete errors.value.userId

  try {
    const response = await checkLoginIdApi(userId.value.trim())
    const available = response.data?.data?.available === true

    idCheckStatus.value = available ? 'available' : 'duplicate'
    idCheckMessage.value = available
        ? '사용 가능한 아이디입니다.'
        : '이미 사용 중인 아이디입니다.'
  } catch (error) {
    idCheckStatus.value = 'error'
    idCheckMessage.value =
        error.response?.data?.message ||
        '아이디 중복 확인에 실패했습니다.'
  }
}

// 회원가입 기본 정보 검사
function validateStep1() {
  errors.value = {}

  const normalizedName = name.value.trim()

  if (!normalizedName) {
    errors.value.name = '이름을 입력해 주세요.'
  } else if (
      normalizedName.length < 2 ||
      normalizedName.length > 100
  ) {
    errors.value.name = '이름은 2~100자로 입력해 주세요.'
  }

  if (!userId.value.trim()) {
    errors.value.userId = '아이디를 입력해 주세요.'
  } else if (!isUserIdValid.value) {
    errors.value.userId =
        '아이디는 영문과 숫자로 6~20자로 입력해 주세요.'
  } else if (idCheckStatus.value !== 'available') {
    errors.value.userId = '아이디 중복 확인을 완료해 주세요.'
  }

  if (!password.value) {
    errors.value.password = '비밀번호를 입력해 주세요.'
  } else if (!isPasswordValid.value) {
    errors.value.password =
        '비밀번호는 영문, 숫자, 특수문자를 포함해 8~64자로 입력해 주세요.'
  }

  if (!passwordConfirm.value) {
    errors.value.passwordConfirm =
        '비밀번호 확인을 입력해 주세요.'
  } else if (password.value !== passwordConfirm.value) {
    errors.value.passwordConfirm =
        '비밀번호가 일치하지 않습니다.'
  }

  return Object.keys(errors.value).length === 0
}

// 휴대전화 인증 단계로 이동
function nextStep() {
  if (!validateStep1()) {
    return
  }

  step.value = 2
}

// 인증번호 발송
async function sendCode() {
  phoneMessage.value = ''
  phoneMessageType.value = 'info'
  delete errors.value.phone

  if (!isPhoneValid.value) {
    errors.value.phone =
        '휴대전화번호는 010으로 시작하는 11자리로 입력해 주세요.'
    return
  }

  if (phoneSending.value || resendSeconds.value > 0) {
    return
  }

  phoneSending.value = true

  try {
    const response = await sendPhoneCodeApi(normalizedPhone.value)
    const sendResult = response.data?.data

    if (!sendResult?.requestId) {
      throw new Error('인증번호 발송 응답이 올바르지 않습니다.')
    }

    phoneVerificationRequestId.value = sendResult.requestId
    codeSent.value = true
    phoneVerified.value = false
    verifyCode.value = ''
    phoneMessage.value = '인증번호를 발송했습니다.'
    phoneMessageType.value = 'info'

    startVerificationTimer(sendResult.expireInSeconds || 180)
  } catch (error) {
    phoneMessage.value =
        error.response?.data?.message ||
        '인증번호 발송에 실패했습니다.'
  } finally {
    phoneSending.value = false
  }
}

// 인증번호 확인
async function verifyPhone() {
  phoneMessage.value = ''
  phoneMessageType.value = 'info'

  if (!phoneVerificationRequestId.value) {
    phoneMessage.value = '먼저 인증번호를 요청해 주세요.'
    phoneMessageType.value = 'error'
    return
  }

  if (!isVerificationCodeValid.value) {
    phoneMessage.value = '인증번호 숫자 6자리를 입력해 주세요.'
    phoneMessageType.value = 'error'
    return
  }

  if (codeExpiresIn.value <= 0) {
    phoneMessage.value =
        '인증번호가 만료되었습니다. 다시 요청해 주세요.'
    phoneMessageType.value = 'error'
    return
  }

  if (phoneVerifying.value) {
    return
  }

  phoneVerifying.value = true

  try {
    await verifyPhoneCodeApi({
      requestId: phoneVerificationRequestId.value,
      phoneNumber: normalizedPhone.value,
      code: verifyCode.value,
    })

    phoneVerified.value = true
    phoneMessage.value = '휴대전화 인증이 완료되었습니다.'
    phoneMessageType.value = 'success'
    stopVerificationTimer()
  } catch (error) {
    phoneVerified.value = false
    phoneMessage.value =
        error.response?.data?.message ||
        '인증번호가 올바르지 않습니다.'
    phoneMessageType.value = 'error'
  } finally {
    phoneVerifying.value = false
  }
}

// 일반 회원가입
async function signup() {
  errors.value.signup = ''

  if (!phoneVerified.value) {
    errors.value.signup = '휴대전화 인증을 완료해 주세요.'
    return
  }

  if (!phoneVerificationRequestId.value) {
    errors.value.signup = '휴대전화 인증 정보가 없습니다.'
    return
  }

  if (signupLoading.value) {
    return
  }

  signupLoading.value = true

  try {
    await signupApi({
      name: name.value.trim(),
      loginId: userId.value.trim(),
      password: password.value,
      phoneNumber: normalizedPhone.value,
      phoneVerificationRequestId:
      phoneVerificationRequestId.value,
    })

    window.alert('회원가입이 완료되었습니다.')
    await router.replace('/login')
  } catch (error) {
    errors.value.signup =
        error.response?.data?.message ||
        '회원가입에 실패했습니다.'
  } finally {
    signupLoading.value = false
  }
}

// 인증번호 만료시간과 재전송 대기시간을 시작한다.
function startVerificationTimer(expireInSeconds) {
  stopVerificationTimer()

  codeExpiresIn.value = expireInSeconds
  resendSeconds.value = 60

  verificationTimer = window.setInterval(() => {
    if (codeExpiresIn.value > 0) {
      codeExpiresIn.value -= 1
      // 인증 완료 전에 만료시간이 끝나면 즉시 안내한다.
      if (
          codeExpiresIn.value === 0 &&
          !phoneVerified.value
      ) {
        phoneMessage.value =
            '인증번호가 만료되었습니다. 다시 요청해 주세요.'
        phoneMessageType.value = 'error'
      }
    }

    if (resendSeconds.value > 0) {
      resendSeconds.value -= 1
    }

    if (
        codeExpiresIn.value <= 0 &&
        resendSeconds.value <= 0
    ) {
      stopVerificationTimer()
    }
  }, 1000)
}

// 실행 중인 인증 타이머를 정리한다.
function stopVerificationTimer() {
  if (verificationTimer !== null) {
    window.clearInterval(verificationTimer)
    verificationTimer = null
  }
}

// 전화번호가 변경되면 인증 관련 상태를 초기화한다.
function resetPhoneVerification() {
  stopVerificationTimer()

  codeSent.value = false
  phoneVerified.value = false
  verifyCode.value = ''
  phoneVerificationRequestId.value = null
  codeExpiresIn.value = 0
  resendSeconds.value = 0
  phoneMessage.value = ''
  phoneMessageType.value = 'info'
}

// 초를 분:초 형식으로 변환한다.
function formatSeconds(seconds) {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60

  return `${minutes}:${String(remainingSeconds).padStart(2, '0')}`
}

// 화면을 벗어날 때 타이머를 정리한다.
onBeforeUnmount(() => {
  stopVerificationTimer()
})
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
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-300"
                    @click="showPassword = !showPassword">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path v-if="showPassword"
                      d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24M1 1l22 22"
                      stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
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
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-300"
                    @click="showPasswordConfirm = !showPasswordConfirm">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path v-if="showPasswordConfirm"
                      d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24M1 1l22 22"
                      stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
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
          <label
              class="text-sm font-medium text-gray-700 block mb-1.5"
          >
            휴대전화번호
          </label>

          <div class="flex gap-2">
            <input
                v-model="phone"
                type="tel"
                inputmode="numeric"
                autocomplete="tel"
                maxlength="13"
                placeholder="010-0000-0000"
                class="min-w-0 flex-1 h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
                :class="errors.phone ? 'border-red-400' : ''"
            />

            <button
                type="button"
                :disabled="
        !isPhoneValid
        || phoneSending
        || resendSeconds > 0
        || phoneVerified
      "
                class="h-12 shrink-0 px-4 rounded-xl text-sm font-semibold whitespace-nowrap border disabled:text-gray-400 disabled:border-gray-200 disabled:bg-gray-100"
                :class="
        isPhoneValid
        && !phoneSending
        && resendSeconds === 0
        && !phoneVerified
          ? 'text-[#3B5BDB] border-[#3B5BDB]'
          : ''
      "
                @click="sendCode"
            >
              <template v-if="phoneSending">
                발송 중
              </template>

              <template v-else-if="resendSeconds > 0">
                재전송 {{ resendSeconds }}초
              </template>

              <template v-else-if="codeSent">
                재전송
              </template>

              <template v-else>
                인증요청
              </template>
            </button>
          </div>
          <p
              v-if="errors.phone"
              class="text-xs text-red-500 mt-1"
          >
            {{ errors.phone }}
          </p>
        </div>

        <!-- 인증번호 -->
        <div>
          <label
              class="text-sm font-medium text-gray-700 block mb-1.5"
          >
            인증번호
          </label>

          <div class="flex gap-2">
            <div class="relative min-w-0 flex-1">
              <input
                  v-model="verifyCode"
                  type="text"
                  inputmode="numeric"
                  autocomplete="one-time-code"
                  placeholder="6자리 입력"
                  maxlength="6"
                  :disabled="
          !codeSent || phoneVerified
        "
                  class="w-full h-12 px-4 pr-14 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white disabled:bg-gray-100"
                  @keyup.enter="verifyPhone"
              />

              <span
                  v-if="
          codeSent
          && !phoneVerified
          && codeExpiresIn > 0
        "
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-red-500"
              >
        {{ formatSeconds(codeExpiresIn) }}
      </span>
            </div>

            <button
                type="button"
                :disabled="
        !codeSent
        || !isVerificationCodeValid
        || phoneVerifying
        || phoneVerified
        || codeExpiresIn <= 0
      "
                class="h-12 shrink-0 px-4 rounded-xl text-sm font-semibold border text-[#3B5BDB] border-[#3B5BDB] disabled:text-gray-400 disabled:border-gray-200 disabled:bg-gray-100"
                @click="verifyPhone"
            >
              <template v-if="phoneVerifying">
                확인 중
              </template>

              <template v-else-if="phoneVerified">
                인증 완료
              </template>

              <template v-else>
                확인
              </template>
            </button>
          </div>

          <p
              v-if="phoneMessage"
              class="text-xs mt-1"
              :class="{
                'text-blue-600': phoneMessageType === 'success',
                'text-red-500': phoneMessageType === 'error',
                'text-gray-500': phoneMessageType === 'info',
                }"
          >
            {{ phoneMessage }}
          </p>
        </div>

        <!-- 완료 버튼 -->
        <p
            v-if="errors.signup"
            class="text-xs text-red-500"
        >
          {{ errors.signup }}
        </p>

        <button
            type="button"
            :disabled="signupLoading || !phoneVerified"
            class="w-full h-14 rounded-2xl text-white font-bold text-base mt-2 disabled:opacity-40 disabled:cursor-not-allowed"
            style="background: #3B5BDB"
            @click="signup"
        >
          {{ signupLoading ? '처리 중...' : '완료' }}
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
