<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

const isDirty = computed(() => confirmPassword.value.length > 0)
const isMatch = computed(() => newPassword.value === confirmPassword.value && newPassword.value.length > 0)
const isMismatch = computed(() => isDirty.value && !isMatch.value)

const confirmBorderColor = computed(() => {
  if (!isDirty.value) return '#E5E7EB'
  return isMatch.value ? '#3B5BDB' : '#EF4444'
})
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

    <!-- 하단 버튼 -->
    <div class="px-4 mt-auto pt-6">
      <button
        class="w-full h-14 rounded-2xl text-white font-bold text-base transition-opacity"
        style="background: #1A337A"
        :class="{ 'opacity-40': !isMatch }"
        :disabled="!isMatch"
      >
        비밀번호 변경
      </button>
    </div>

    <BottomNav />
  </div>
</template>
