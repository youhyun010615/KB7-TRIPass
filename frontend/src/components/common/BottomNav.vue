<script setup>
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const navItems = [
  { name: '홈', path: '/', icon: 'home' },
  { name: '금융상품', path: '/financial', icon: 'financial' },
  { name: '자산관리', path: '/asset', icon: 'asset' },
  { name: '영수증', path: '/receipt', icon: 'receipt' },
  { name: '마이페이지', path: '/mypage', icon: 'mypage' },
]

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>

<template>
  <nav class="fixed bottom-0 left-0 right-0 h-16 bg-white border-t border-gray-100 flex items-center">
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
        <circle cx="12" cy="12" r="9" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8"/>
        <path d="M12 3V12L16.5 7.5" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
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
