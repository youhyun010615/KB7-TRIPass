<script setup>
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const navItems = [
  { name: '홈', path: '/', icon: 'home' },
  { name: '여행', path: '/travel', icon: 'travel' },
  { name: '자산', path: '/asset', icon: 'asset' },
  { name: '마이', path: '/mypage', icon: 'mypage' },
]

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>

<template>
  <nav class="fixed bottom-0 left-0 right-0 h-16 bg-white border-t border-gray-100 flex items-center safe-area-pb">
    <button
      v-for="item in navItems"
      :key="item.path"
      class="flex-1 flex flex-col items-center justify-center gap-1 py-2"
      :class="isActive(item.path) ? 'text-blue-600' : 'text-gray-400'"
      @click="router.push(item.path)"
    >
      <!-- home -->
      <svg v-if="item.icon === 'home'" width="22" height="22" viewBox="0 0 24 24" fill="none">
        <path d="M3 9L12 3L21 9V20C21 20.55 20.55 21 20 21H15V15H9V21H4C3.45 21 3 20.55 3 20V9Z"
          :stroke="isActive(item.path) ? '#2563eb' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <!-- travel -->
      <svg v-if="item.icon === 'travel'" width="22" height="22" viewBox="0 0 24 24" fill="none">
        <path d="M12 2C8.13 2 5 5.13 5 9C5 14.25 12 22 12 22C12 22 19 14.25 19 9C19 5.13 15.87 2 12 2ZM12 11.5C10.62 11.5 9.5 10.38 9.5 9C9.5 7.62 10.62 6.5 12 6.5C13.38 6.5 14.5 7.62 14.5 9C14.5 10.38 13.38 11.5 12 11.5Z"
          :fill="isActive(item.path) ? '#2563eb' : '#9CA3AF'"/>
      </svg>
      <!-- asset -->
      <svg v-if="item.icon === 'asset'" width="22" height="22" viewBox="0 0 24 24" fill="none">
        <rect x="3" y="6" width="18" height="13" rx="2" :stroke="isActive(item.path) ? '#2563eb' : '#9CA3AF'" stroke-width="1.8"/>
        <path d="M3 10H21" :stroke="isActive(item.path) ? '#2563eb' : '#9CA3AF'" stroke-width="1.8"/>
        <path d="M7 15H10" :stroke="isActive(item.path) ? '#2563eb' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
      <!-- mypage -->
      <svg v-if="item.icon === 'mypage'" width="22" height="22" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="8" r="4" :stroke="isActive(item.path) ? '#2563eb' : '#9CA3AF'" stroke-width="1.8"/>
        <path d="M4 20C4 17.24 7.58 15 12 15C16.42 15 20 17.24 20 20"
          :stroke="isActive(item.path) ? '#2563eb' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
      <span class="text-xs font-medium">{{ item.name }}</span>
    </button>
  </nav>
</template>
