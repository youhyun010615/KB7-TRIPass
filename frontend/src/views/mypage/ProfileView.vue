<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()
const authStore = useAuthStore()

const mockUser = {
  name: authStore.user?.name ?? '권유현',
  email: authStore.user?.email ?? 'youhyun@email.com',
  phone: '010-1234-5678',
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
      <h1 class="text-base font-bold text-gray-900">회원정보</h1>
      <button class="text-sm font-medium" style="color: #3B5BDB" @click="router.push('/mypage/profile/edit')">수정</button>
    </div>

    <!-- 프로필 미니 카드 -->
    <div class="mx-4 bg-white rounded-2xl px-5 py-4 flex items-center gap-4">
      <div class="w-12 h-12 rounded-full flex items-center justify-center text-lg font-bold flex-shrink-0" style="background: #EEF2FF; color: #3B5BDB">
        {{ mockUser.name[0] }}
      </div>
      <div>
        <p class="font-bold text-gray-900 text-base">{{ mockUser.name }}</p>
        <p class="text-xs text-gray-400 mt-0.5">TRIPASS MEMBER</p>
      </div>
    </div>

    <!-- 정보 필드 -->
    <div class="px-4 mt-5 flex flex-col gap-3">
      <div>
        <p class="text-xs text-gray-400 mb-1.5">이름</p>
        <div class="bg-white rounded-2xl px-5 py-4">
          <p class="text-sm text-gray-900">{{ mockUser.name }}</p>
        </div>
      </div>
      <div>
        <p class="text-xs text-gray-400 mb-1.5">이메일</p>
        <div class="bg-white rounded-2xl px-5 py-4">
          <p class="text-sm text-gray-900">{{ mockUser.email }}</p>
        </div>
      </div>
      <div>
        <p class="text-xs text-gray-400 mb-1.5">휴대폰 번호</p>
        <div class="bg-white rounded-2xl px-5 py-4">
          <p class="text-sm text-gray-900">{{ mockUser.phone }}</p>
        </div>
      </div>
    </div>

    <!-- 수정 버튼 -->
    <div class="px-4 mt-auto pt-6">
      <button
        @click="router.push('/mypage/profile/edit')"
        class="w-full h-14 rounded-2xl text-white font-bold text-base"
        style="background: #1A337A"
      >
        회원정보 수정
      </button>
    </div>

    <BottomNav />
  </div>
</template>
