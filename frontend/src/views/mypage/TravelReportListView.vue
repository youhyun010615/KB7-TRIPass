<script setup>
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()

const reports = [
  {
    id: 1,
    travelTitle: '총 2개국 배낭여행',
    countries: '프랑스 · 이탈리아',
    dateRange: '2025.07.10 ~ 07.24',
    totalSpent: 2430000,
    categories: [
      { name: '항공', percent: 38, color: '#3B5BDB' },
      { name: '숙박', percent: 28, color: '#60A5FA' },
      { name: '식비', percent: 20, color: '#A5B4FC' },
      { name: '기타', percent: 14, color: '#E0E7FF' },
    ],
  },
  {
    id: 3,
    travelTitle: '일본 오사카 여행',
    countries: '일본',
    dateRange: '2024.12.20 ~ 12.25',
    totalSpent: 980000,
    categories: [
      { name: '항공', percent: 42, color: '#3B5BDB' },
      { name: '숙박', percent: 30, color: '#60A5FA' },
      { name: '식비', percent: 18, color: '#A5B4FC' },
      { name: '기타', percent: 10, color: '#E0E7FF' },
    ],
  },
]

function formatCurrency(n) {
  return n.toLocaleString('ko-KR') + '원'
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
      <h1 class="text-base font-bold text-gray-900">여행 리포트</h1>
      <div class="w-8" />
    </div>

    <!-- 리포트 카드 목록 -->
    <div class="px-4 flex flex-col gap-3">
      <div
        v-for="report in reports"
        :key="report.id"
        class="bg-white rounded-2xl px-5 py-5"
      >
        <!-- 여행 정보 -->
        <p class="text-sm font-bold text-gray-900">{{ report.travelTitle }}</p>
        <p class="text-xs text-gray-400 mt-0.5 mb-4">{{ report.countries }} · {{ report.dateRange }}</p>

        <!-- 총 지출 -->
        <div class="mb-4">
          <p class="text-xs text-gray-400 mb-1">총 지출</p>
          <p class="text-xl font-bold" style="color: #1A337A">{{ formatCurrency(report.totalSpent) }}</p>
        </div>

        <!-- 카테고리 바 -->
        <div class="mb-3">
          <div class="flex rounded-full overflow-hidden h-2.5">
            <div
              v-for="cat in report.categories"
              :key="cat.name"
              :style="{ width: cat.percent + '%', background: cat.color }"
            />
          </div>
        </div>

        <!-- 범례 -->
        <div class="flex flex-wrap gap-x-4 gap-y-1.5">
          <div v-for="cat in report.categories" :key="cat.name" class="flex items-center gap-1.5">
            <div class="w-2.5 h-2.5 rounded-full flex-shrink-0" :style="{ background: cat.color }" />
            <span class="text-[11px] text-gray-500">{{ cat.name }} {{ cat.percent }}%</span>
          </div>
        </div>
      </div>
    </div>

    <BottomNav />
  </div>
</template>
