<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { fetchMyTrips } from '@/api/travel'

const router = useRouter()

const travels = ref([])
const loading = ref(true)

function formatDateRange(startDate, endDate) {
  const fmt = d => (d ? d.replaceAll('-', '.') : '')
  return `${fmt(startDate)} ~ ${fmt(endDate)}`
}

function statusLabel(status) {
  if (status === 'ENDED') return '완료'
  if (status === 'TRAVELING') return '여행 중'
  return '예정'
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await fetchMyTrips()
    travels.value = (data || []).map(t => ({
      id: t.tripId,
      title: t.tripName,
      subtitle: t.countryNames || '',
      dateRange: formatDateRange(t.startDate, t.endDate),
      days: t.totalDays || 1,
      status: statusLabel(t.status),
    }))
  } catch (error) {
    console.error('여행 목록 조회 실패:', error)
  } finally {
    loading.value = false
  }
})
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
      <h1 class="text-base font-bold text-gray-900">여행 관리</h1>
      <div class="w-8" />
    </div>

    <p v-if="loading" class="text-center text-xs text-gray-400 py-10">불러오는 중...</p>
    <p v-else-if="!travels.length" class="text-center text-xs text-gray-400 py-10">등록된 여행이 없어요.</p>

    <!-- 여행 카드 목록 -->
    <div v-else class="px-4 flex flex-col gap-3">
      <button
        v-for="travel in travels"
        :key="travel.id"
        class="bg-white rounded-2xl px-5 py-5 text-left w-full active:bg-gray-50"
        @click="router.push(`/mypage/travel/${travel.id}`)"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <p class="text-sm font-bold text-gray-900">{{ travel.title }}</p>
              <span
                class="text-[10px] font-semibold px-2 py-0.5 rounded-full"
                :style="travel.status === '완료'
                  ? 'background: #EEF2FF; color: #3B5BDB'
                  : 'background: #FEF3C7; color: #D97706'"
              >{{ travel.status }}</span>
            </div>
            <p class="text-xs text-gray-400 mb-2">{{ travel.subtitle }}</p>
            <p class="text-xs text-gray-500">{{ travel.dateRange }}</p>
          </div>
          <div class="flex flex-col items-end gap-1 ml-3">
            <p class="text-2xl font-bold" style="color: #3B5BDB">{{ travel.days }}</p>
            <p class="text-[10px] text-gray-400">박 {{ travel.days - 1 }}일</p>
          </div>
        </div>

        <!-- 하단 링크 아이콘 행 -->
        <div class="flex gap-3 mt-4 pt-4 border-t border-gray-100">
          <button
            class="flex items-center gap-1.5 text-xs text-gray-500"
            @click.stop="router.push(`/mypage/reports?tripId=${travel.id}`)"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="#6B7280" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M14 2V8H20" stroke="#6B7280" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            여행 리포트
          </button>
          <button
            class="flex items-center gap-1.5 text-xs text-gray-500"
            @click.stop="router.push(`/mypage/checklists?tripId=${travel.id}`)"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M9 11L12 14L22 4" stroke="#6B7280" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M21 12V19C21 19.5304 20.7893 20.0391 20.4142 20.4142C20.0391 20.7893 19.5304 21 19 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H16" stroke="#6B7280" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            체크리스트
          </button>
          <button class="flex items-center gap-1.5 text-xs text-gray-500" @click.stop="router.push(`/receipt?tripId=${travel.id}`)">
            <span>▤</span> 영수증 관리
          </button>
        </div>
      </button>
    </div>

    <BottomNav />
  </div>
</template>
