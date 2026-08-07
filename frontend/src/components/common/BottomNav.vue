<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTravelModeStore } from '@/stores/travelMode'

const router = useRouter()
const route = useRoute()
const travelModeStore = useTravelModeStore()

const savingsNavItems = [
  { name: '홈', path: '/', icon: 'home' },
  { name: '금융상품', path: '/financial', icon: 'financial' },
  { name: '자산관리', path: '/asset', icon: 'asset' },
  { name: '환율', path: '/exchange', icon: 'exchange' },
  { name: '마이페이지', path: '/mypage', icon: 'mypage' },
]

const travelNavItems = [
  { name: '홈', path: '/', icon: 'home' },
  { name: '여행일정', path: '/schedule', icon: 'schedule' },
  { name: '여행자금 체크', path: '/travel/funds', icon: 'asset' },
  { name: '영수증', path: '/receipt', icon: 'receipt' },
  { name: '마이페이지', path: '/mypage', icon: 'mypage' },
]

const navItems = computed(() =>
  travelModeStore.isTravelMode ? travelNavItems : savingsNavItems
)

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>

<template>
  <nav class="fixed bottom-0 left-1/2 -translate-x-1/2 z-50 w-full max-w-[390px] h-16 bg-white border-t border-gray-100 flex items-center">
    <button
      v-for="item in navItems"
      :key="item.path"
      class="flex-1 flex flex-col items-center justify-center gap-0.5 py-2"
      :class="isActive(item.path) ? 'text-[#3B5BDB]' : 'text-gray-400'"
      @click="router.push(item.path)"
    >
      <!-- home -->
      <svg v-if="item.icon === 'home'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="M3 9L12 3L21 9V20C21 20.55 20.55 21 20 21H15V15H9V21H4C3.45 21 3 20.55 3 20V9Z"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <!-- financial (금융상품) -->
      <svg v-if="item.icon === 'financial'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <rect x="3" y="3" width="7" height="7" rx="1.5" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8"/>
        <rect x="14" y="3" width="7" height="7" rx="1.5" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8"/>
        <rect x="3" y="14" width="7" height="7" rx="1.5" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8"/>
        <rect x="14" y="14" width="7" height="7" rx="1.5" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8"/>
      </svg>
      <!-- asset (자산관리) -->
      <svg v-if="item.icon === 'asset'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <rect x="3" y="6" width="18" height="13" rx="2" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8"/>
        <path d="M3 10H21" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8"/>
        <path d="M7 15H10" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
      <!-- exchange (환율) -->
      <svg v-if="item.icon === 'exchange'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2Z"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8"/>
        <path d="M8 12H16M16 12L13 9M16 12L13 15" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <!-- schedule (여행일정) -->
      <svg v-if="item.icon === 'schedule'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <rect x="3" y="4" width="18" height="18" rx="2" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8"/>
        <path d="M16 2V6M8 2V6M3 10H21" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
      <!-- receipt (영수증) -->
      <svg v-if="item.icon === 'receipt'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="M5 2H19C19.55 2 20 2.45 20 3V22L17.5 20.5L15 22L12.5 20.5L10 22L7.5 20.5L5 22V3C5 2.45 5.45 2 6 2Z"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
        <line x1="9" y1="8" x2="15" y2="8" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
        <line x1="9" y1="12" x2="15" y2="12" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
        <line x1="9" y1="16" x2="12" y2="16" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
      <!-- mypage -->
      <svg v-if="item.icon === 'mypage'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="8" r="4"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'"
          :fill="isActive(item.path) ? '#3B5BDB' : 'none'"
          stroke-width="1.8"/>
        <path d="M4 20C4 17.24 7.58 15 12 15C16.42 15 20 17.24 20 20"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
      <span class="text-[10px] font-medium">{{ item.name }}</span>
    </button>
  </nav>
</template>
