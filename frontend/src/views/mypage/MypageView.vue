<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {logout as logoutApi} from "@/api/auth"
import { useAuthStore } from '@/stores/auth'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()
const authStore = useAuthStore()

const isLoggingOut = ref(false)
const memberIdentity = computed(() => {
  const provider = authStore.user?.loginProvider ?? 'LOCAL'
  if (provider !== 'LOCAL') return authStore.user?.email ?? '이메일 미등록'
  return authStore.user?.loginId ?? authStore.user?.id ?? 'tripass'
})

async function logout() {
  if (isLoggingOut.value) return

  const confirmed = window.confirm('로그아웃할까요?')
  if (!confirmed) return

  isLoggingOut.value = true

  try {
    // 서버의 Refresh Token을 폐기하고 HttpOnly 쿠키를 삭제한다.
    await logoutApi()
  } catch (error) {
    // 서버 요청이 실패하더라도 현재 브라우저의 로그인 상태는 제거한다.
  } finally {
    // Access Token과 사용자 정보를 프론트에서 제거한다.
    authStore.logout()

    // 뒤로 가기로 보호 화면에 돌아가지 않도록 replace를 사용한다.
    await router.replace('/login')

    isLoggingOut.value = false
  }
}

const myManageItems = [
  {
    label: '회원정보',
    sub: '연락처와 비밀번호 관리',
    path: '/mypage/profile',
    icon: 'user',
  },
  {
    label: '금융 프로필',
    sub: '월 수입과 자산 정보',
    path: '/mypage/financial-profile',
    icon: 'financial',
  },
  {
    label: '여행 관리',
    sub: '등록한 여행과 관련 기록',
    path: '/mypage/travel',
    icon: 'travel',
  },
]

const serviceItems = [
  {
    label: '알림 설정',
    sub: '일정·환율·리포트 알림',
    path: '/mypage/notification',
    icon: 'bell',
  },
  {
    label: '고객지원',
    sub: '공지사항과 문의',
    path: '/mypage/support',
    icon: 'help',
  },
]
</script>

<template>
  <div class="min-h-screen pb-20 flex flex-col" style="background: #F7F4EE">

    <!-- 헤더 -->
    <div class="flex items-center px-5 pt-14 pb-3">
      <h1 class="text-2xl font-bold text-gray-900">마이페이지</h1>
    </div>

    <!-- 멤버 패스 카드 -->
    <div class="mx-4 mt-1 rounded-2xl overflow-hidden" style="background: linear-gradient(135deg, #2A4DB0 0%, #1A337A 100%)">
      <div class="flex items-center justify-between px-4 pt-3 pb-2">
        <span class="text-white/60 text-[10px] font-semibold tracking-widest">TRIPASS MEMBER PASS</span>
        <span class="text-white/60 text-[10px]">NO. TP-260715</span>
      </div>
      <div class="flex items-center gap-4 px-4 pb-5">
        <div class="w-14 h-14 rounded-full bg-white flex items-center justify-center text-xl font-bold flex-shrink-0" style="color: #1A337A">
          {{ authStore.user?.name?.[0] ?? '유' }}
        </div>
        <div>
          <p class="text-white font-bold text-xl leading-tight">{{ authStore.user?.name ?? '권유현' }}</p>
          <p class="text-white/60 text-sm mt-0.5">{{ memberIdentity }}</p>
        </div>
      </div>
    </div>

    <!-- 나의 관리 -->
    <div class="px-4 mt-5">
      <h2 class="text-base font-bold text-gray-900 mb-3">나의 관리</h2>
      <div class="bg-white rounded-2xl overflow-hidden">
        <button
          v-for="(item, i) in myManageItems"
          :key="item.label"
          class="w-full flex items-center gap-4 px-4 py-4 active:bg-gray-50"
          :class="i < myManageItems.length - 1 ? 'border-b border-gray-100' : ''"
          @click="router.push(item.path)"
        >
          <!-- 아이콘 -->
          <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0" style="background: #EEF2FF">
            <!-- user -->
            <svg v-if="item.icon === 'user'" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="8" r="4" stroke="#3B5BDB" stroke-width="2"/>
              <path d="M4 20C4 17.24 7.58 15 12 15C16.42 15 20 17.24 20 20" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <!-- financial -->
            <svg v-if="item.icon === 'financial'" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <rect x="3" y="4" width="18" height="4" rx="1" stroke="#3B5BDB" stroke-width="2"/>
              <rect x="3" y="10" width="18" height="4" rx="1" stroke="#3B5BDB" stroke-width="2"/>
              <rect x="3" y="16" width="18" height="4" rx="1" stroke="#3B5BDB" stroke-width="2"/>
            </svg>
            <!-- travel -->
            <svg v-if="item.icon === 'travel'" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M21 3L3 10.5L10 13.5M21 3L13.5 21L10 13.5M21 3L10 13.5" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <!-- 텍스트 -->
          <div class="flex-1 text-left">
            <p class="text-sm font-semibold text-gray-900">{{ item.label }}</p>
            <p class="text-xs text-gray-400 mt-0.5">{{ item.sub }}</p>
          </div>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 서비스 설정 -->
    <div class="px-4 mt-5">
      <h2 class="text-base font-bold text-gray-900 mb-3">서비스 설정</h2>
      <div class="bg-white rounded-2xl overflow-hidden">
        <button
          v-for="(item, i) in serviceItems"
          :key="item.label"
          class="w-full flex items-center gap-4 px-4 py-4 active:bg-gray-50"
          :class="i < serviceItems.length - 1 ? 'border-b border-gray-100' : ''"
          @click="router.push(item.path)"
        >
          <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0" style="background: #EEF2FF">
            <!-- bell -->
            <svg v-if="item.icon === 'bell'" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M18 8C18 6.4 17.37 4.84 16.24 3.76C15.12 2.63 13.59 2 12 2C10.41 2 8.88 2.63 7.76 3.76C6.63 4.84 6 6.4 6 8C6 15 3 17 3 17H21C21 17 18 15 18 8Z" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M13.73 21C13.55 21.3 13.3 21.55 12.99 21.73C12.68 21.91 12.34 22 12 22C11.66 22 11.32 21.91 11.01 21.73C10.7 21.55 10.45 21.3 10.27 21" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- help -->
            <svg v-if="item.icon === 'help'" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="#3B5BDB" stroke-width="2"/>
              <path d="M9.09 9C9.33 8.34 9.77 7.77 10.37 7.37C10.97 6.97 11.67 6.74 12.4 6.72C13.84 6.69 15.1 7.63 15.5 9C15.91 10.37 15.24 11.85 14 12.5C13.37 12.84 12.96 13.5 12.96 14.22V15" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
              <circle cx="12" cy="18" r="1" fill="#3B5BDB"/>
            </svg>
          </div>
          <div class="flex-1 text-left">
            <p class="text-sm font-semibold text-gray-900">{{ item.label }}</p>
            <p class="text-xs text-gray-400 mt-0.5">{{ item.sub }}</p>
          </div>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </div>

    <div class="px-4 mt-5 mb-4">
      <button
          type="button"
          class="w-full py-3.5 rounded-2xl border border-gray-200 bg-white text-sm font-semibold text-gray-500 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="isLoggingOut"
          @click="logout"
      >
        {{ isLoggingOut ? '로그아웃 중...' : '로그아웃' }}
      </button>
    </div>

    <BottomNav />
  </div>
</template>
