<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()

const sections = ref([
  {
    title: '일정 알림',
    items: [
      { id: 'schedule_departure', label: '출발 D-7 알림', sub: '출발 7일 전 여행 준비 알림', enabled: true },
      { id: 'schedule_daily', label: '당일 일정 알림', sub: '여행 당일 일정 요약', enabled: true },
    ],
  },
  {
    title: '환율 알림',
    items: [
      { id: 'exchange_target', label: '목표 환율 도달', sub: '설정한 환율에 도달하면 알림', enabled: true },
      { id: 'exchange_daily', label: '일일 환율 요약', sub: '매일 오전 9시 환율 정보', enabled: false },
    ],
  },
  {
    title: '리포트 알림',
    items: [
      { id: 'report_weekly', label: '주간 지출 리포트', sub: '매주 월요일 지출 요약', enabled: true },
      { id: 'report_travel', label: '여행 종료 리포트', sub: '여행 완료 후 최종 리포트', enabled: true },
    ],
  },
  {
    title: '마케팅 알림',
    items: [
      { id: 'marketing_promo', label: '프로모션·이벤트', sub: '할인 혜택과 이벤트 소식', enabled: false },
    ],
  },
])

function toggle(item) {
  item.enabled = !item.enabled
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
      <h1 class="text-base font-bold text-gray-900">알림 설정</h1>
      <div class="w-8" />
    </div>

    <!-- 섹션별 토글 목록 -->
    <div class="px-4 flex flex-col gap-5">
      <div v-for="section in sections" :key="section.title">
        <h2 class="text-xs font-semibold text-gray-400 mb-2 px-1">{{ section.title }}</h2>
        <div class="bg-white rounded-2xl overflow-hidden">
          <div
            v-for="(item, i) in section.items"
            :key="item.id"
            class="flex items-center gap-4 px-4 py-4"
            :class="i < section.items.length - 1 ? 'border-b border-gray-100' : ''"
          >
            <div class="flex-1">
              <p class="text-sm font-semibold text-gray-900">{{ item.label }}</p>
              <p class="text-xs text-gray-400 mt-0.5">{{ item.sub }}</p>
            </div>

            <!-- 토글 스위치 -->
            <button
              class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors flex-shrink-0"
              :style="item.enabled ? 'background: #3B5BDB' : 'background: #D1D5DB'"
              @click="toggle(item)"
            >
              <span
                class="inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform"
                :style="item.enabled ? 'transform: translateX(24px)' : 'transform: translateX(4px)'"
              />
            </button>
          </div>
        </div>
      </div>
    </div>

    <BottomNav />
  </div>
</template>
