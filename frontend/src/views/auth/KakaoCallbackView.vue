<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  loginWithKakao,
  logout as logoutApi,
} from '@/api/auth'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const errorMessage = ref('')
const processing = ref(false)

function getQueryValue(value) {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }

  return typeof value === 'string'
      ? value
      : ''
}

async function clearFailedLogin() {
  try {
    await logoutApi()
  } catch (error) {
    console.error(
        '카카오 로그인 상태 정리 실패',
        error,
    )
  } finally {
    authStore.logout()
  }
}

async function checkLinkedAccounts() {
  authStore.resetProfileCompletion()

  const accountResponse =
      await api.get('/accounts')

  const linkedAccounts =
      accountResponse.data?.data ?? []

  if (linkedAccounts.length > 0) {
    authStore.completeProfile()
  }
}

async function handleKakaoCallback() {
  if (processing.value) {
    return
  }

  processing.value = true
  loading.value = true
  errorMessage.value = ''

  const oauthError =
      getQueryValue(route.query.error)

  const oauthErrorDescription =
      getQueryValue(
          route.query.error_description,
      )

  if (oauthError) {
    errorMessage.value =
        oauthErrorDescription ||
        '카카오 로그인이 취소되었습니다.'

    loading.value = false
    return
  }

  const code =
      getQueryValue(route.query.code)

  const state =
      getQueryValue(route.query.state)

  if (!code || !state) {
    errorMessage.value =
        '카카오 로그인 정보를 확인할 수 없습니다. 다시 로그인해 주세요.'

    loading.value = false
    return
  }

  // 주소창에 카카오 인가 코드와 state가 계속 노출되지 않도록 제거한다.
  window.history.replaceState(
      {},
      document.title,
      route.path,
  )

  try {
    const response =
        await loginWithKakao({
          code,
          state,
        })

    const loginData =
        response.data?.data

    if (
        !loginData?.accessToken ||
        !loginData?.user
    ) {
      throw new Error(
          '카카오 로그인 응답을 처리할 수 없습니다.',
      )
    }

    authStore.setToken(
        loginData.accessToken,
    )

    authStore.setUser(
        loginData.user,
    )

    try {
      await checkLinkedAccounts()
    } catch (accountError) {
      console.error(
          '연동 계좌 확인 실패',
          accountError,
      )

      if (
          accountError.response?.status === 401
      ) {
        await clearFailedLogin()

        errorMessage.value =
            '로그인 정보가 유효하지 않습니다. 다시 로그인해 주세요.'

        loading.value = false
        return
      }

      errorMessage.value =
          '연동 계좌 정보를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.'

      loading.value = false
      return
    }

    await router.replace('/')
  } catch (error) {
    authStore.logout()

    errorMessage.value =
        error.response?.data?.message ||
        error.message ||
        '카카오 로그인에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

function returnToLogin() {
  router.replace('/login')
}

onMounted(() => {
  handleKakaoCallback()
})
</script>

<template>
  <main
      class="min-h-screen flex items-center justify-center px-6"
      style="background: linear-gradient(to bottom, #263F8C 0%, #172F6B 100%)"
  >
    <section
        class="w-full max-w-sm rounded-3xl bg-white px-6 py-10 text-center"
    >
      <div
          v-if="loading"
          class="flex flex-col items-center"
      >
        <div
            class="h-10 w-10 animate-spin rounded-full border-4 border-gray-200 border-t-[#3B5BDB]"
        ></div>

        <h1
            class="mt-5 text-xl font-bold text-gray-900"
        >
          카카오 로그인 중
        </h1>

        <p
            class="mt-2 text-sm text-gray-400"
        >
          로그인 정보를 확인하고 있어요.
        </p>
      </div>

      <div v-else-if="errorMessage">
        <div class="text-4xl">
          ⚠️
        </div>

        <h1
            class="mt-4 text-xl font-bold text-gray-900"
        >
          카카오 로그인 실패
        </h1>

        <p
            class="mt-3 break-keep text-sm leading-relaxed text-red-500"
        >
          {{ errorMessage }}
        </p>

        <button
            type="button"
            class="mt-7 h-12 w-full rounded-xl bg-[#3B5BDB] font-bold text-white"
            @click="returnToLogin"
        >
          로그인 화면으로 돌아가기
        </button>
      </div>
    </section>
  </main>
</template>