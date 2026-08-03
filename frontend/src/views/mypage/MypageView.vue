<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/common/AppHeader.vue'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()
const authStore = useAuthStore()

const menuItems = [
  { label: '프로필 수정', icon: '✏️' },
  { label: '알림 설정', icon: '🔔' },
  { label: '공지사항', icon: '📢' },
  { label: '고객센터', icon: '💬' },
  { label: '서비스 이용약관', icon: '📄' },
]

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex flex-col pb-16">
    <AppHeader title="마이페이지" :showBack="false" />

    <!-- 프로필 카드 -->
    <div class="bg-white mx-4 mt-4 rounded-2xl p-5 flex items-center gap-4 shadow-sm">
      <div class="w-16 h-16 rounded-full bg-[#263F8C] flex items-center justify-center text-white text-2xl font-bold flex-shrink-0">
        {{ authStore.user?.name?.[0] ?? 'T' }}
      </div>
      <div>
        <p class="font-bold text-gray-900 text-lg">{{ authStore.user?.name ?? '사용자' }}</p>
        <p class="text-sm text-gray-400 mt-0.5">{{ authStore.user?.id ?? 'tripass' }}</p>
      </div>
    </div>

    <!-- 메뉴 리스트 -->
    <div class="bg-white mx-4 mt-3 rounded-2xl overflow-hidden shadow-sm">
      <button
        v-for="item in menuItems"
        :key="item.label"
        class="w-full flex items-center justify-between px-5 py-4 border-b border-gray-50 last:border-0 active:bg-gray-50"
      >
        <div class="flex items-center gap-3">
          <span class="text-lg">{{ item.icon }}</span>
          <span class="text-sm font-medium text-gray-800">{{ item.label }}</span>
        </div>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </div>

    <!-- 앱 버전 -->
    <p class="text-center text-xs text-gray-300 mt-4">TRIPass v1.0.0</p>

    <!-- 로그아웃 -->
    <button
      @click="logout"
      class="mx-4 mt-3 w-[calc(100%-2rem)] py-3.5 rounded-2xl border border-red-200 text-red-500 text-sm font-semibold"
    >
      로그아웃
    </button>

    <BottomNav />
  </div>
</template>
