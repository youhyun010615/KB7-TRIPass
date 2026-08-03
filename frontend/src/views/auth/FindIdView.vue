<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const name = ref('')
const phone = ref('')
const verifyCode = ref('')
const codeSent = ref(false)
const result = ref('')
const loading = ref(false)

function sendCode() {
  if (!name.value || !phone.value) return
  loading.value = true
  setTimeout(() => {
    loading.value = false
    codeSent.value = true
  }, 600)
}

function confirm() {
  if (!verifyCode.value) return
  // 목데이터
  result.value = 'tri***'
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
            class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
          />
        </div>

        <!-- 인증번호 (발송 후 표시) -->
        <div v-if="codeSent">
          <label class="text-sm font-medium text-gray-700 block mb-1.5">인증번호</label>
          <input
            v-model="verifyCode"
            type="text"
            placeholder="6자리 입력"
            maxlength="6"
            class="w-full h-12 px-4 rounded-xl text-sm border border-gray-200 outline-none focus:border-[#3B5BDB] placeholder-gray-300 bg-white"
          />
          <p class="text-xs mt-1" style="color: #3B5BDB">인증번호가 발송되었습니다.</p>
        </div>

        <!-- 결과 -->
        <div v-if="result" class="bg-blue-50 rounded-2xl p-4 text-center">
          <p class="text-xs text-gray-500 mb-1">가입된 아이디</p>
          <p class="font-bold text-lg" style="color: #3B5BDB">{{ result }}</p>
        </div>

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
          v-else-if="!result"
          @click="confirm"
          class="w-full h-14 rounded-2xl text-white font-bold text-base"
          style="background: #3B5BDB"
        >
          확인하기
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
