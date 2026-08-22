<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  getGoogleAuthorizationUrl,
  getKakaoAuthorizationUrl,
  login as loginApi,
} from '@/api/auth'
import AuthBoardingPass from '@/components/auth/AuthBoardingPass.vue'

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
  if (loading.value || kakaoLoading.value || googleLoading.value) {
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
    // Access Token과 로그인 회원 정보를 Pinia에 저장한다.
    authStore.handleLoginSuccess(loginData.accessToken, loginData.user)

    // RefreshToken은 HttpOnly 쿠키로 자동 저장되므로
    // 프론트 JavaScript에서 직접 처리하지 않는다.
    await router.replace('/')
  } catch (error) {
    errorMsg.value = error.response?.data?.message || '로그인에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

async function startKakaoLogin() {
  if (loading.value || kakaoLoading.value || googleLoading.value) {
    return
  }

  errorMsg.value = ''
  kakaoLoading.value = true

  try {
    const response = await getKakaoAuthorizationUrl()
    const authorizationUrl = response.data?.data?.authorizationUrl

    if (!authorizationUrl) {
      throw new Error('카카오 로그인 주소를 확인할 수 없습니다.')
    }

    const parsedAuthorizationUrl = new URL(authorizationUrl)
    if (
      parsedAuthorizationUrl.protocol !== 'https:' ||
      parsedAuthorizationUrl.hostname !== 'kauth.kakao.com'
    ) {
      throw new Error('유효하지 않은 카카오 로그인 주소입니다.')
    }

    window.location.assign(parsedAuthorizationUrl.toString())
  } catch (error) {
    errorMsg.value =
      error.response?.data?.message ||
      error.message ||
      '카카오 로그인을 시작할 수 없습니다.'
    kakaoLoading.value = false
  }
}

async function startGoogleLogin() {
  if (loading.value || kakaoLoading.value || googleLoading.value) {
    return
  }

  errorMsg.value = ''
  googleLoading.value = true

  try {
    const response = await getGoogleAuthorizationUrl()
    const authorizationUrl = response.data?.data?.authorizationUrl

    if (!authorizationUrl) {
      throw new Error('Google 로그인 주소를 확인할 수 없습니다.')
    }

    const parsedAuthorizationUrl = new URL(authorizationUrl)
    if (
      parsedAuthorizationUrl.protocol !== 'https:' ||
      parsedAuthorizationUrl.hostname !== 'accounts.google.com'
    ) {
      throw new Error('유효하지 않은 Google 로그인 주소입니다.')
    }

    window.location.assign(parsedAuthorizationUrl.toString())
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
  <AuthBoardingPass
    title="다시 만나서 반가워요"
    description="로그인하고 여행 준비를 이어가세요."
    ticket-code="LOGIN"
  >
    <form class="login-form" @submit.prevent="login">
      <p v-if="errorMsg" class="form-message error" role="alert">{{ errorMsg }}</p>

      <div class="field-group">
        <label for="login-id">아이디</label>
        <input
          id="login-id"
          v-model="userId"
          type="text"
          autocomplete="username"
          placeholder="아이디를 입력해 주세요"
        />
      </div>

      <div class="field-group">
        <label for="login-password">비밀번호</label>
        <div class="password-field">
          <input
            id="login-password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="current-password"
            placeholder="비밀번호를 입력해 주세요"
          />
          <button
            type="button"
            class="visibility-button"
            :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 표시'"
            @click="showPassword = !showPassword"
          >
            <svg v-if="showPassword" viewBox="0 0 24 24" fill="none">
              <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24M1 1l22 22" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2" />
              <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2" />
            </svg>
          </button>
        </div>
      </div>

      <div class="account-links">
        <button type="button" @click="router.push('/find-id')">아이디 찾기</button>
        <span>·</span>
        <button type="button" @click="router.push('/find-password')">비밀번호 찾기</button>
      </div>

      <button
        type="submit"
        class="primary-button"
        :disabled="loading || kakaoLoading || googleLoading"
      >
        {{ loading ? '로그인 중...' : '로그인' }}
      </button>
      <button type="button" class="secondary-button" @click="router.push('/signup')">
        회원가입
      </button>

      <div class="social-divider"><span>간편 로그인</span></div>
      <div class="social-buttons">
        <button
          type="button"
          class="kakao"
          :disabled="loading || kakaoLoading || googleLoading"
          :aria-busy="kakaoLoading"
          aria-label="카카오 로그인"
          @click="startKakaoLogin"
        >
          {{ kakaoLoading ? '…' : 'K' }}
        </button>
        <button
          type="button"
          class="google"
          :disabled="loading || kakaoLoading || googleLoading"
          :aria-busy="googleLoading"
          aria-label="구글 로그인"
          @click="startGoogleLogin"
        >
          {{ googleLoading ? '…' : 'G' }}
        </button>
      </div>
      <p v-if="kakaoLoading || googleLoading" class="social-loading" role="status">
        {{
          kakaoLoading
            ? '카카오 로그인 화면으로 이동하고 있어요.'
            : 'Google 로그인 화면으로 이동하고 있어요.'
        }}
      </p>
    </form>
  </AuthBoardingPass>
</template>

<style scoped>
.login-form {
  display: flex;
  flex-direction: column;
  gap: 17px;
}

.field-group label {
  display: block;
  margin-bottom: 7px;
  color: #3d4860;
  font-size: 12px;
  font-weight: 750;
}

.field-group input {
  width: 100%;
  height: 50px;
  padding: 0 15px;
  border: 1px solid #dbe3f0;
  border-radius: 11px;
  outline: none;
  color: #15213a;
  background: #f7f9fc;
  font-size: 13px;
  transition: border-color 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.field-group input:focus {
  border-color: #2a63c9;
  background: white;
  box-shadow: 0 0 0 3px rgba(42, 99, 201, 0.1);
}

.field-group input::placeholder { color: #b3bdcd; }

.password-field { position: relative; }
.password-field input { padding-right: 47px; }

.visibility-button {
  position: absolute;
  top: 50%;
  right: 13px;
  width: 24px;
  height: 24px;
  padding: 2px;
  border: 0;
  color: #aab4c5;
  background: transparent;
  transform: translateY(-50%);
}

.visibility-button svg { width: 20px; height: 20px; }

.account-links {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: -5px;
  color: #8d98aa;
  font-size: 11px;
}

.account-links button {
  border: 0;
  color: inherit;
  background: transparent;
}

.primary-button,
.secondary-button {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 800;
}

.primary-button {
  margin-top: 3px;
  border: 0;
  color: white;
  background: #0d327e;
  box-shadow: 0 9px 18px rgba(13, 50, 126, 0.16);
}

.primary-button:disabled { opacity: 0.65; }

.secondary-button {
  margin-top: -8px;
  border: 1px solid #cad6e8;
  color: #173c84;
  background: white;
}

.social-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #a3acba;
  font-size: 10px;
}

.social-divider::before,
.social-divider::after {
  height: 1px;
  flex: 1;
  background: #e5e9f0;
  content: '';
}

.social-buttons {
  display: flex;
  justify-content: center;
  gap: 13px;
  margin-top: -6px;
}

.social-buttons button {
  display: grid;
  width: 43px;
  height: 43px;
  place-items: center;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 850;
}

.social-buttons button:disabled { cursor: not-allowed; opacity: 0.6; }

.social-loading {
  margin: -8px 0 0;
  color: #8d98aa;
  font-size: 11px;
  text-align: center;
}

.kakao { border: 0; color: #3c1e1e; background: #fee500; }
.google { border: 1px solid #dbe2ec; color: #3164ca; background: white; }

.form-message {
  margin: -3px 0 0;
  padding: 10px 12px;
  border-radius: 9px;
  font-size: 11px;
}

.form-message.error { color: #b42318; background: #fff1f0; }
</style>
