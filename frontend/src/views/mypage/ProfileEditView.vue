<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()
const authStore = useAuthStore()

const name = ref(authStore.user?.name ?? '권유현')
const email = ref(authStore.user?.email ?? 'youhyun@email.com')
const phone = ref('010-1234-5678')
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
      <h1 class="text-base font-bold text-gray-900">회원정보 수정</h1>
      <div class="w-8" />
    </div>

    <!-- 수정 필드 -->
    <div class="px-4 mt-2 flex flex-col gap-3">
      <!-- 이름 -->
      <div>
        <p class="text-xs text-gray-400 mb-1.5">이름</p>
        <div class="bg-white rounded-2xl px-5 py-4 flex items-center justify-between">
          <input
            v-model="name"
            class="flex-1 text-sm text-gray-900 bg-transparent outline-none"
            placeholder="이름 입력"
          />
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M11 4H4C3.46957 4 2.96086 4.21071 2.58579 4.58579C2.21071 4.96086 2 5.46957 2 6V20C2 20.5304 2.21071 21.0391 2.58579 21.4142C2.96086 21.7893 3.46957 22 4 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V13" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M18.5 2.5C18.8978 2.10217 19.4374 1.87868 20 1.87868C20.5626 1.87868 21.1022 2.10217 21.5 2.5C21.8978 2.89782 22.1213 3.43739 22.1213 4C22.1213 4.56261 21.8978 5.10218 21.5 5.5L12 15L8 16L9 12L18.5 2.5Z" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      </div>

      <!-- 이메일 -->
      <div>
        <p class="text-xs text-gray-400 mb-1.5">이메일</p>
        <div class="bg-white rounded-2xl px-5 py-4 flex items-center justify-between">
          <input
            v-model="email"
            type="email"
            class="flex-1 text-sm text-gray-900 bg-transparent outline-none"
            placeholder="이메일 입력"
          />
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M18 13V6A2 2 0 0016 4H4a2 2 0 00-2 2v7" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M22 16C22 17.1046 21.1046 18 20 18H4C2.89543 18 2 17.1046 2 16V13H22V16Z" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M15 21H9" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round"/>
            <path d="M12 18V21" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
      </div>

      <!-- 휴대폰 번호 -->
      <div>
        <p class="text-xs text-gray-400 mb-1.5">휴대폰 번호</p>
        <div class="bg-white rounded-2xl px-5 py-4 flex items-center justify-between">
          <input
            v-model="phone"
            type="tel"
            class="flex-1 text-sm text-gray-900 bg-transparent outline-none"
            placeholder="휴대폰 번호 입력"
          />
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M11 4H4C3.46957 4 2.96086 4.21071 2.58579 4.58579C2.21071 4.96086 2 5.46957 2 6V20C2 20.5304 2.21071 21.0391 2.58579 21.4142C2.96086 21.7893 3.46957 22 4 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V13" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M18.5 2.5C18.8978 2.10217 19.4374 1.87868 20 1.87868C20.5626 1.87868 21.1022 2.10217 21.5 2.5C21.8978 2.89782 22.1213 3.43739 22.1213 4C22.1213 4.56261 21.8978 5.10218 21.5 5.5L12 15L8 16L9 12L18.5 2.5Z" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- 하단 버튼 -->
    <div class="px-4 mt-auto pt-6 flex flex-col gap-3">
      <button
        class="w-full h-14 rounded-2xl text-white font-bold text-base"
        style="background: #1A337A"
      >
        수정 내용 저장
      </button>
      <button
        class="w-full h-14 rounded-2xl font-bold text-sm border"
        style="color: #1A337A; border-color: #1A337A; background: transparent"
        @click="router.push('/mypage/password')"
      >
        비밀번호 변경
      </button>
    </div>

    <BottomNav />
  </div>
</template>
