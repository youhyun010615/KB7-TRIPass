<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const userId = ref('')
const phone = ref('')
const loading = ref(false)
const done = ref(false)

function verify() {
  if (!userId.value || !phone.value) return
  loading.value = true
  setTimeout(() => {
    loading.value = false
    done.value = true
  }, 800)
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
        비밀번호를 안전하게<br>다시 설정하세요
      </h1>
      <p class="text-blue-200 text-sm mt-3 leading-relaxed">
        아이디와 휴대폰 번호로 본인 확인을 진행해요.
      </p>
    </div>

    <!-- 흰색 카드 -->
    <div class="relative z-10 mx-4 bg-white rounded-3xl px-6 pt-7 pb-7">
      <h2 class="text-[22px] font-bold text-gray-900">비밀번호 찾기</h2>
      <p class="text-sm text-gray-400 mt-1">인증이 완료되면 새 비밀번호를 설정할 수 있어요.</p>

      <div v-if="!done" class="mt-6 flex flex-col gap-4">
        <!-- 아이디 -->
        <div>
          <label class="text-sm font-medium text-gray-700 block mb-1.5">아이디</label>
          <input
            v-model="userId"
            type="text"
            placeholder="아이디를 입력해 주세요"
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

        <!-- 본인 인증 버튼 -->
        <button
          @click="verify"
          :disabled="loading"
          class="w-full h-14 rounded-2xl text-white font-bold text-base mt-2 disabled:opacity-70"
          style="background: #3B5BDB"
        >
          {{ loading ? '확인 중...' : '본인 인증하기' }}
        </button>
      </div>

      <!-- 인증 완료 상태 -->
      <div v-else class="mt-6 flex flex-col gap-4">
        <div class="bg-blue-50 rounded-2xl p-5 text-center">
          <p class="text-2xl mb-2">✅</p>
          <p class="font-semibold text-gray-800">본인 인증이 완료됐어요</p>
          <p class="text-sm text-gray-500 mt-1">임시 비밀번호가 휴대폰으로 발송됐어요.</p>
        </div>
        <button
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
