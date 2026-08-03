<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()

const checklists = ref([
  {
    id: 1,
    travelTitle: '총 2개국 배낭여행',
    dateRange: '2025.07.10 ~ 07.24',
    items: [
      { id: 1, text: '여권 유효기간 확인', done: true },
      { id: 2, text: '항공권 예매', done: true },
      { id: 3, text: '숙소 예약', done: true },
      { id: 4, text: '여행자 보험 가입', done: true },
      { id: 5, text: '환전', done: false },
      { id: 6, text: '세계 어댑터 챙기기', done: false },
    ],
  },
  {
    id: 2,
    travelTitle: '동남아 단기 여행',
    dateRange: '2025.09.01 ~ 09.08',
    items: [
      { id: 1, text: '비자 확인', done: false },
      { id: 2, text: '항공권 예매', done: true },
      { id: 3, text: '예방접종 확인', done: false },
    ],
  },
])

function toggle(checklist, item) {
  item.done = !item.done
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
      <h1 class="text-base font-bold text-gray-900">체크리스트</h1>
      <div class="w-8" />
    </div>

    <!-- 체크리스트 카드 목록 -->
    <div class="px-4 flex flex-col gap-3">
      <div
        v-for="checklist in checklists"
        :key="checklist.id"
        class="bg-white rounded-2xl px-5 py-5"
      >
        <!-- 제목 -->
        <p class="text-sm font-bold text-gray-900">{{ checklist.travelTitle }}</p>
        <div class="flex items-center justify-between mb-4 mt-0.5">
          <p class="text-xs text-gray-400">{{ checklist.dateRange }}</p>
          <p class="text-xs font-semibold" style="color: #3B5BDB">
            {{ checklist.items.filter(i => i.done).length }}/{{ checklist.items.length }}
          </p>
        </div>

        <!-- 진행 바 -->
        <div class="h-1.5 rounded-full bg-gray-100 mb-4">
          <div
            class="h-full rounded-full transition-all"
            style="background: #3B5BDB"
            :style="{ width: (checklist.items.filter(i => i.done).length / checklist.items.length * 100) + '%' }"
          />
        </div>

        <!-- 항목 목록 -->
        <div class="flex flex-col gap-3">
          <button
            v-for="item in checklist.items"
            :key="item.id"
            class="flex items-center gap-3 text-left"
            @click="toggle(checklist, item)"
          >
            <div
              class="w-5 h-5 rounded-full flex items-center justify-center flex-shrink-0 transition-colors"
              :style="item.done
                ? 'background: #3B5BDB; border: none'
                : 'background: transparent; border: 1.5px solid #D1D5DB'"
            >
              <svg v-if="item.done" width="11" height="11" viewBox="0 0 24 24" fill="none">
                <path d="M20 6L9 17L4 12" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span
              class="text-sm transition-colors"
              :class="item.done ? 'line-through text-gray-400' : 'text-gray-900'"
            >{{ item.text }}</span>
          </button>
        </div>
      </div>
    </div>

    <BottomNav />
  </div>
</template>
