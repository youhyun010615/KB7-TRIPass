<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'

const authStore = useAuthStore()
const router = useRouter()
</script>

<template>
  <div class="min-h-screen pb-20 flex flex-col items-center justify-center gap-4" style="background: #F7F4EE">
    <p class="text-lg font-semibold text-gray-700">홈 화면 (준비 중)</p>
    <p class="text-sm text-gray-400">{{ authStore.user?.name }}님 환영합니다</p>
    <button
      class="px-4 py-2 text-sm text-red-500 border border-red-200 rounded-xl"
      @click="authStore.logout(); router.push('/login')"
    >
      로그아웃
    </button>
    <BottomNav />
  </div>
</template>
