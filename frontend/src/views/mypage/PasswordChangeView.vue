<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { changePassword as changePasswordApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { PASSWORD_PATTERN } from '@/constants/authValidation'

const router = useRouter()
const authStore = useAuthStore()

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

const loading = ref(false)
const errorMessage = ref('')


const isDirty = computed(() =>
    confirmPassword.value.length > 0,
)

const isPasswordValid = computed(() =>
    PASSWORD_PATTERN.test(newPassword.value),
)

const isMatch = computed(() =>
    newPassword.value === confirmPassword.value
    && newPassword.value.length > 0,
)

const isMismatch = computed(() =>
    isDirty.value && !isMatch.value,
)

const canSubmit = computed(() =>
    currentPassword.value.length > 0
    && isPasswordValid.value
    && isMatch.value
    && !loading.value,
)

const confirmBorderColor = computed(() => {
  if (!isDirty.value) {
    return '#E5E7EB'
  }

  return isMatch.value
      ? '#3B5BDB'
      : '#EF4444'
})

async function submitPasswordChange() {
  errorMessage.value = ''

  if (!currentPassword.value) {
    errorMessage.value = '현재 비밀번호를 입력해 주세요.'
    return
  }

  if (!isPasswordValid.value) {
    errorMessage.value =
        '새 비밀번호는 영문, 숫자, 특수문자를 포함해 8~64자로 입력해 주세요.'
    return
  }

  if (!isMatch.value) {
    errorMessage.value =
        '새 비밀번호와 비밀번호 확인이 일치하지 않습니다.'
    return
  }

  loading.value = true

  try {
    await changePasswordApi({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
      newPasswordConfirm: confirmPassword.value,
    })

    // 비밀번호 변경 시 백엔드가 모든 Refresh Token을 폐기하므로
    // 프론트에 보관된 Access Token과 회원 정보도 제거한다.
    authStore.logout()

    window.alert(
        '비밀번호가 변경되었습니다. 다시 로그인해 주세요.',
    )

    await router.replace('/login')
  } catch (error) {
    errorMessage.value =
        error.response?.data?.message
        ?? '비밀번호를 변경하지 못했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen pb-20 flex flex-col" style="background: #F7F4EE">

    <!-- 헤더 -->
    <div class="flex items-center justify-between px-5 pt-14 pb-4">
      <button @click="router.back()" class="p-1">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M15 18L9 12L15 6" stroke="#1A1A1A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <h1 class="text-base font-bold text-gray-900">비밀번호 변경</h1>
      <div class="w-8" />
    </div>

    <!-- 입력 필드 -->
    <div class="px-4 mt-2 flex flex-col gap-3">
      <!-- 현재 비밀번호 -->
      <div>
        <p class="text-xs text-gray-400 mb-1.5">현재 비밀번호</p>
        <div class="bg-white rounded-2xl px-5 py-4" style="border: 1.5px solid #E5E7EB">
          <input
            v-model="currentPassword"
            type="password"
            class="w-full text-sm text-gray-900 bg-transparent outline-none"
            placeholder="현재 비밀번호 입력"
          />
        </div>
      </div>

      <!-- 새 비밀번호 -->
      <div>
        <p class="text-xs text-gray-400 mb-1.5">새 비밀번호</p>
        <div class="bg-white rounded-2xl px-5 py-4" style="border: 1.5px solid #E5E7EB">
          <input
            v-model="newPassword"
            type="password"
            class="w-full text-sm text-gray-900 bg-transparent outline-none"
            placeholder="새 비밀번호 입력"
          />
        </div>
        <p class="text-xs text-gray-400 mt-1.5 ml-1">
          영문, 숫자, 특수문자를 포함해 8~64자로 입력해 주세요.
        </p>
      </div>

      <!-- 새 비밀번호 확인 -->
      <div>
        <p class="text-xs text-gray-400 mb-1.5">새 비밀번호 확인</p>
        <div
          class="bg-white rounded-2xl px-5 py-4"
          :style="{ border: `1.5px solid ${confirmBorderColor}` }"
        >
          <input
            v-model="confirmPassword"
            type="password"
            class="w-full text-sm text-gray-900 bg-transparent outline-none"
            placeholder="새 비밀번호 재입력"
          />
        </div>
        <!-- 에러 메시지 -->
        <p v-if="isMismatch" class="text-xs mt-1.5 ml-1" style="color: #EF4444">
          비밀번호가 일치하지 않습니다.
        </p>
        <!-- 일치 메시지 -->
        <p v-else-if="isMatch" class="text-xs mt-1.5 ml-1" style="color: #3B5BDB">
          비밀번호가 일치합니다.
        </p>
      </div>
    </div>

    <p
        v-if="errorMessage"
        class="px-4 mt-4 text-xs text-red-500"
    >
      {{ errorMessage }}
    </p>
    <!-- 하단 버튼 -->
    <div class="px-4 mt-auto pt-6">
      <button
          type="button"
          class="w-full h-14 rounded-2xl text-white font-bold text-base transition-opacity"
          style="background: #1A337A"
          :class="{ 'opacity-40': !canSubmit }"
          :disabled="!canSubmit"
          @click="submitPasswordChange"
      >
        {{ loading ? '변경 중...' : '비밀번호 변경' }}
      </button>
    </div>

    <BottomNav />
  </div>
</template>
