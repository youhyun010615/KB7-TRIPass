<script setup>
import BottomNav from '@/components/common/BottomNav.vue'
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="min-h-screen pb-20 flex flex-col items-center justify-center" style="background: #F7F4EE">
    <button @click="router.back()" class="absolute top-14 left-5 p-1">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="M15 18L9 12L15 6" stroke="#1A1A1A" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </button>
    <p class="text-gray-400 text-sm">MYP · 준비 중</p>
    <p class="text-xs text-gray-300 mt-1">담당: 권유현</p>
    <BottomNav />
  </div>
</template>
