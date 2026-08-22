<script setup>
import {
  computed,
  onMounted,
  ref,
} from 'vue'
import { useRouter } from 'vue-router'
import { changePassword as changePasswordApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { PASSWORD_PATTERN } from '@/constants/authValidation'

const router = useRouter()
const authStore = useAuthStore()

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

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
    return '#DCE3EF'
  }

  return isMatch.value
      ? '#10B981'
      : '#EF4444'
})

onMounted(() => {
  // 소셜 회원은 TRIPass 비밀번호가 없으므로 직접 접근을 막는다.
  const provider = authStore.user?.loginProvider

  if (provider && provider !== 'LOCAL') {
    router.replace('/mypage/profile')
  }
})

async function submitPasswordChange() {
  errorMessage.value = ''

  if (!currentPassword.value) {
    errorMessage.value =
        '현재 비밀번호를 입력해 주세요.'
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

    // 백엔드에서 모든 Refresh Token을 폐기하므로
    // 프론트에 저장된 로그인 정보도 제거한다.
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
  <div
      class="flex min-h-screen flex-col"
      style="background: #F4F6FB"
  >
    <!-- 헤더 -->
    <header
        class="flex items-center border-b border-[#E2E7F0] bg-white px-5 pb-4 pt-3.5"
    >
      <button
          type="button"
          class="flex h-9 w-9 items-center justify-center rounded-xl bg-white shadow-[0_5px_16px_rgba(36,72,117,0.07)]"
          aria-label="뒤로 가기"
          @click="router.back()"
      >
        <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
        >
          <path
              d="M15 18L9 12L15 6"
              stroke="#193d82"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
          />
        </svg>
      </button>

      <h1
          class="flex-1 pr-9 text-center text-xl font-bold text-[#172033]"
      >
        비밀번호 변경
      </h1>
    </header>

    <main class="flex-1 px-4 pb-28 pt-6">
      <p
          class="mb-7 text-sm leading-5 text-[#718096]"
      >
        안전한 계정 사용을 위해<br>
        새 비밀번호를 입력해 주세요.
      </p>

      <div class="flex flex-col gap-5">
        <!-- 현재 비밀번호 -->
        <section
            class="rounded-2xl border border-[#DCE3EF] bg-white px-5 py-4 shadow-sm"
        >
          <label
              for="current-password"
              class="text-xs font-medium text-[#718096]"
          >
            현재 비밀번호
          </label>

          <div class="mt-3 flex items-center gap-3">
            <input
                id="current-password"
                v-model="currentPassword"
                :type="showCurrentPassword ? 'text' : 'password'"
                autocomplete="current-password"
                class="min-w-0 flex-1 bg-transparent text-sm font-semibold text-[#172033] outline-none"
                placeholder="현재 비밀번호를 입력해 주세요."
                :disabled="loading"
            >

            <button
                type="button"
                class="flex h-8 w-8 items-center justify-center text-[#718096]"
                :aria-label="
                showCurrentPassword
                  ? '현재 비밀번호 숨기기'
                  : '현재 비밀번호 표시'
              "
                @click="showCurrentPassword = !showCurrentPassword"
            >
              <svg
                  v-if="showCurrentPassword"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
              >
                <path
                    d="M3 3L21 21"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                />
                <path
                    d="M10.6 10.7A2 2 0 0013.3 13.4"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                />
                <path
                    d="M9.9 5.2A10.7 10.7 0 0112 5C17 5 20.2 9 21 12a10.6 10.6 0 01-2.1 3.8M6.2 6.2C4.5 7.5 3.4 9.5 3 12c.8 3 4 7 9 7 1.2 0 2.3-.2 3.3-.6"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                />
              </svg>

              <svg
                  v-else
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
              >
                <path
                    d="M3 12S6 5 12 5s9 7 9 7-3 7-9 7-9-7-9-7Z"
                    stroke="currentColor"
                    stroke-width="1.8"
                />
                <circle
                    cx="12"
                    cy="12"
                    r="2.5"
                    stroke="currentColor"
                    stroke-width="1.8"
                />
              </svg>
            </button>
          </div>
        </section>

        <!-- 새 비밀번호 -->
        <section
            class="rounded-2xl border border-[#DCE3EF] bg-white px-5 py-4 shadow-sm"
        >
          <label
              for="new-password"
              class="text-xs font-medium text-[#718096]"
          >
            새 비밀번호
          </label>

          <div class="mt-3 flex items-center gap-3">
            <input
                id="new-password"
                v-model="newPassword"
                :type="showNewPassword ? 'text' : 'password'"
                autocomplete="new-password"
                class="min-w-0 flex-1 bg-transparent text-sm font-semibold text-[#172033] outline-none"
                placeholder="새 비밀번호를 입력해 주세요."
                maxlength="64"
                :disabled="loading"
            >

            <button
                type="button"
                class="flex h-8 w-8 items-center justify-center text-[#718096]"
                :aria-label="
                showNewPassword
                  ? '새 비밀번호 숨기기'
                  : '새 비밀번호 표시'
              "
                @click="showNewPassword = !showNewPassword"
            >
              <svg
                  v-if="showNewPassword"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
              >
                <path
                    d="M3 3L21 21"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                />
                <path
                    d="M10.6 10.7A2 2 0 0013.3 13.4"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                />
                <path
                    d="M9.9 5.2A10.7 10.7 0 0112 5C17 5 20.2 9 21 12a10.6 10.6 0 01-2.1 3.8M6.2 6.2C4.5 7.5 3.4 9.5 3 12c.8 3 4 7 9 7 1.2 0 2.3-.2 3.3-.6"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                />
              </svg>

              <svg
                  v-else
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
              >
                <path
                    d="M3 12S6 5 12 5s9 7 9 7-3 7-9 7-9-7-9-7Z"
                    stroke="currentColor"
                    stroke-width="1.8"
                />
                <circle
                    cx="12"
                    cy="12"
                    r="2.5"
                    stroke="currentColor"
                    stroke-width="1.8"
                />
              </svg>
            </button>
          </div>
        </section>

        <!-- 새 비밀번호 확인 -->
        <section
            class="rounded-2xl bg-white px-5 py-4 shadow-sm"
            :style="{
            border: `1.5px solid ${confirmBorderColor}`
          }"
        >
          <label
              for="confirm-password"
              class="text-xs font-medium text-[#718096]"
          >
            새 비밀번호 확인
          </label>

          <div class="mt-3 flex items-center gap-3">
            <input
                id="confirm-password"
                v-model="confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                autocomplete="new-password"
                class="min-w-0 flex-1 bg-transparent text-sm font-semibold text-[#172033] outline-none"
                placeholder="새 비밀번호를 다시 입력해 주세요."
                maxlength="64"
                :disabled="loading"
            >

            <button
                type="button"
                class="flex h-8 w-8 items-center justify-center text-[#718096]"
                :aria-label="
                showConfirmPassword
                  ? '비밀번호 확인값 숨기기'
                  : '비밀번호 확인값 표시'
              "
                @click="showConfirmPassword = !showConfirmPassword"
            >
              <svg
                  v-if="showConfirmPassword"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
              >
                <path
                    d="M3 3L21 21"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                />
                <path
                    d="M10.6 10.7A2 2 0 0013.3 13.4"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                />
                <path
                    d="M9.9 5.2A10.7 10.7 0 0112 5C17 5 20.2 9 21 12a10.6 10.6 0 01-2.1 3.8M6.2 6.2C4.5 7.5 3.4 9.5 3 12c.8 3 4 7 9 7 1.2 0 2.3-.2 3.3-.6"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                />
              </svg>

              <svg
                  v-else
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
              >
                <path
                    d="M3 12S6 5 12 5s9 7 9 7-3 7-9 7-9-7-9-7Z"
                    stroke="currentColor"
                    stroke-width="1.8"
                />
                <circle
                    cx="12"
                    cy="12"
                    r="2.5"
                    stroke="currentColor"
                    stroke-width="1.8"
                />
              </svg>
            </button>
          </div>
        </section>

        <!-- 비밀번호 확인 상태 -->
        <div
            v-if="isMismatch"
            class="rounded-2xl bg-[#FFF0F0] px-5 py-4"
            role="alert"
        >
          <div class="flex items-start gap-3">
            <span
                class="mt-0.5 text-lg font-bold text-[#EF4444]"
                aria-hidden="true"
            >
              △
            </span>

            <div>
              <p class="text-sm font-bold text-[#EF4444]">
                비밀번호가 서로 일치하지 않아요.
              </p>

              <p class="mt-1 text-xs text-[#8A5A5A]">
                입력한 비밀번호를 다시 확인해 주세요.
              </p>
            </div>
          </div>
        </div>

        <div
            v-else-if="isMatch"
            class="rounded-2xl bg-[#E9FAF3] px-5 py-4"
            role="status"
        >
          <div class="flex items-start gap-3">
            <span
                class="mt-0.5 text-lg font-bold text-[#10B981]"
                aria-hidden="true"
            >
              ✓
            </span>

            <div>
              <p class="text-sm font-bold text-[#059669]">
                비밀번호가 일치해요.
              </p>

              <p class="mt-1 text-xs text-[#5D7D70]">
                새 비밀번호로 변경할 수 있어요.
              </p>
            </div>
          </div>
        </div>

        <!-- API 오류 -->
        <div
            v-if="errorMessage"
            class="rounded-2xl bg-[#FFF0F0] px-5 py-4"
            role="alert"
        >
          <p class="text-sm font-semibold text-[#EF4444]">
            {{ errorMessage }}
          </p>
        </div>
      </div>
    </main>

    <!-- 하단 변경 버튼 -->
    <footer
        class="fixed bottom-0 left-1/2 z-20 w-full max-w-[390px] -translate-x-1/2 border-t border-[#E2E7F0] bg-white px-4 py-4"
    >
      <button
          type="button"
          class="h-14 w-full rounded-2xl bg-[#173E8F] text-base font-bold text-white transition-opacity disabled:bg-[#C8CDD7]"
          :disabled="!canSubmit"
          @click="submitPasswordChange"
      >
        {{ loading ? '변경 중...' : '변경 완료' }}
      </button>
    </footer>
  </div>
</template>