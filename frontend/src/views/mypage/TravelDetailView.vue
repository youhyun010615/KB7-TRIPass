<script setup>
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()
const route = useRoute()

const travel = {
  id: 1,
  title: '총 2개국 배낭여행',
  countries: ['프랑스', '이탈리아'],
  dateRange: '2025.07.10 ~ 2025.07.24',
  days: 14,
  nights: 13,
  totalBudget: 2850000,
  spentBudget: 2430000,
  status: '완료',
}

const menus = [
  {
    label: '여행 리포트',
    desc: '여행 지출 분석과 요약',
    path: '/mypage/reports',
    icon: 'report',
  },
  {
    label: '체크리스트',
    desc: '준비물과 할 일 목록',
    path: `/mypage/checklists?tripId=${route.params.id}`,
    icon: 'checklist',
  },
  {
    label: '영수증 관리',
    desc: '촬영한 영수증과 번역 내역',
    path: `/receipt?tripId=${route.params.id}`,
    icon: 'receipt',
  },
  {
    label: '일정 관리',
    desc: '날짜별 여행 일정',
    path: '#',
    icon: 'schedule',
  },
]

function formatCurrency(n) {
  return n.toLocaleString('ko-KR') + '원'
}
const spentPercent = Math.round((travel.spentBudget / travel.totalBudget) * 100)
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
      <h1 class="text-base font-bold text-gray-900">여행 상세</h1>
      <div class="w-8" />
    </div>

    <!-- 여행 요약 카드 -->
    <div class="mx-4 rounded-2xl overflow-hidden" style="background: linear-gradient(135deg, #2A4DB0 0%, #1A337A 100%)">
      <div class="px-5 pt-5 pb-6">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-white/60 text-[11px]">{{ travel.dateRange }}</span>
          <span class="text-white/40 text-[10px] font-semibold px-2 py-0.5 rounded-full" style="border: 1px solid rgba(255,255,255,0.3)">{{ travel.status }}</span>
        </div>
        <h2 class="text-white text-xl font-bold mb-1">{{ travel.title }}</h2>
        <p class="text-white/70 text-sm">{{ travel.countries.join(' · ') }}</p>

        <!-- 통계 행 -->
        <div class="flex gap-6 mt-5">
          <div>
            <p class="text-white/50 text-[10px] mb-0.5">여행 기간</p>
            <p class="text-white font-bold text-lg">{{ travel.nights }}박 {{ travel.days }}일</p>
          </div>
          <div>
            <p class="text-white/50 text-[10px] mb-0.5">방문 국가</p>
            <p class="text-white font-bold text-lg">{{ travel.countries.length }}개국</p>
          </div>
        </div>

        <!-- 예산 바 -->
        <div class="mt-5">
          <div class="flex justify-between items-center mb-2">
            <p class="text-white/60 text-[11px]">지출 현황</p>
            <p class="text-white text-xs font-semibold">{{ spentPercent }}%</p>
          </div>
          <div class="h-1.5 rounded-full bg-white/20">
            <div class="h-full rounded-full bg-white transition-all" :style="{ width: spentPercent + '%' }" />
          </div>
          <div class="flex justify-between mt-1.5">
            <p class="text-white/60 text-[10px]">{{ formatCurrency(travel.spentBudget) }}</p>
            <p class="text-white/60 text-[10px]">{{ formatCurrency(travel.totalBudget) }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 메뉴 -->
    <div class="px-4 mt-4">
      <div class="bg-white rounded-2xl overflow-hidden">
        <button
          v-for="(item, i) in menus"
          :key="item.label"
          class="w-full flex items-center gap-4 px-4 py-4 active:bg-gray-50"
          :class="i < menus.length - 1 ? 'border-b border-gray-100' : ''"
          @click="router.push(item.path)"
        >
          <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0" style="background: #EEF2FF">
            <!-- report -->
            <svg v-if="item.icon === 'report'" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M14 2V8H20" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="8" y1="13" x2="16" y2="13" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
              <line x1="8" y1="17" x2="12" y2="17" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <!-- checklist -->
            <svg v-if="item.icon === 'checklist'" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M9 11L12 14L22 4" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M21 12V19C21 19.5304 20.7893 20.0391 20.4142 20.4142C20.0391 20.7893 19.5304 21 19 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H16" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- receipt -->
            <svg v-if="item.icon === 'receipt'" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M5 3H19V21L16.5 19.5L14 21L11.5 19.5L9 21L5 19V3Z" stroke="#3B5BDB" stroke-width="2" stroke-linejoin="round"/>
              <path d="M9 8H15M9 12H15M9 16H13" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <!-- schedule -->
            <svg v-if="item.icon === 'schedule'" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <rect x="3" y="4" width="18" height="18" rx="2" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M16 2V6M8 2V6M3 10H21" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="flex-1 text-left">
            <p class="text-sm font-semibold text-gray-900">{{ item.label }}</p>
            <p class="text-xs text-gray-400 mt-0.5">{{ item.desc }}</p>
          </div>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </div>

    <BottomNav />
  </div>
</template>
