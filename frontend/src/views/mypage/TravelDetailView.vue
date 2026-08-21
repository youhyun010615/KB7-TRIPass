<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { fetchMyTrips } from '@/api/travel'
import { fetchPreTripReport, fetchPostTripReport } from '@/api/report'
import { getReceipts } from '@/api/receipt'
import { countryPresentation, flagIconClass } from '@/stores/travel'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const trip = ref(null)
const report = ref(null)
const receipts = ref([])

function splitCountryNames(joined) {
  return (joined || '').split(' · ').map((name) => name.trim()).filter(Boolean)
}

function countryCodeOf(countryName) {
  return countryPresentation[countryName]?.code || ''
}

const isEnded = computed(() => trip.value?.status === 'ENDED')
const countryCodes = computed(() => splitCountryNames(trip.value?.countryNames).map(countryCodeOf).filter(Boolean))
const countryLabel = computed(() => splitCountryNames(trip.value?.countryNames).join(' · '))
const daysUntilStart = computed(() => {
  if (report.value?.daysUntilTrip !== null && report.value?.daysUntilTrip !== undefined) {
    const reportDays = Number(report.value.daysUntilTrip)
    if (Number.isFinite(reportDays)) return reportDays
  }
  if (!trip.value?.startDate) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const startDate = new Date(`${trip.value.startDate}T00:00:00`)
  return Math.ceil((startDate - today) / 86400000)
})
const isTraveling = computed(() =>
  trip.value?.status === 'TRAVELING' || (!isEnded.value && daysUntilStart.value !== null && daysUntilStart.value <= 0),
)
const tripStatusLabel = computed(() => {
  if (isEnded.value) return '여행 완료'
  return isTraveling.value ? '여행 중' : '준비 중'
})
const dDayLabel = computed(() => {
  if (daysUntilStart.value === null) return 'D-'
  return daysUntilStart.value <= 0 ? 'D-0' : `D-${daysUntilStart.value}`
})

function formatDateRange(startDate, endDate) {
  const fmt = (d) => (d ? d.replaceAll('-', '.') : '')
  return `${fmt(startDate)} - ${fmt(endDate)}`
}

function formatWon(amount) {
  return `${Number(amount || 0).toLocaleString('ko-KR')}원`
}

const savingsPercent = computed(() =>
  Math.min(100, Math.max(0, Number(report.value?.savingsPercent ?? 0))),
)

// 접수된 영수증 중 원화(KRW) 항목만 합산한다. 외화 영수증은 환율 변환 API가
// 없어 그대로 합산하면 총액이 왜곡되므로 제외한다.
const krwReceiptTotal = computed(() =>
  receipts.value
    .filter((r) => r.currencyCode === 'KRW')
    .reduce((sum, r) => sum + Number(r.totalAmount || 0), 0),
)

const budgetDiff = computed(() => Number(report.value?.targetBudget || 0) - Number(report.value?.spent || 0))

const menuItems = computed(() => {
  const tripId = trip.value?.tripId
  if (isEnded.value) {
    return [
      { label: '여행 리포트', desc: '저축 기록과 여행 후 지출 분석', icon: 'report', path: `/mypage/reports?tripId=${tripId}` },
      { label: '체크리스트', desc: '여행 전 · 귀국 준비', icon: 'checklist', path: `/mypage/checklists?tripId=${tripId}` },
      { label: '여행 일정', desc: '등록한 일정 확인', icon: 'schedule', path: `/mypage/travel/${tripId}/schedules?tripId=${tripId}` },
      { label: '영수증 보관함', desc: 'OCR 영수증과 지출 기록', icon: 'receipt', path: `/trips/${tripId}/receipts` },
      { label: '완료 미션', desc: '매달 진행했던 미션 기록', icon: 'mission', path: '/missions' },
    ]
  }
  return [
    { label: '여행 리포트', desc: '저축 기록과 여행 후 지출 분석', icon: 'report', path: `/mypage/reports?tripId=${tripId}`, badge: isTraveling.value ? '열람 가능' : '준비 중' },
    { label: '체크리스트', desc: '여행 전 · 귀국 준비', icon: 'checklist', path: `/mypage/checklists?tripId=${tripId}`, badge: `${report.value?.checklistCompleted ?? 0}/${report.value?.checklistTotal ?? 0}` },
    { label: '여행 일정', desc: '등록한 일정 확인', icon: 'schedule', path: `/mypage/travel/${tripId}/schedules?tripId=${tripId}`, badge: `${report.value?.scheduleCount ?? 0}개` },
    { label: '영수증 보관함', desc: 'OCR 영수증과 지출 기록', icon: 'receipt', path: `/trips/${tripId}/receipts`, badge: `${receipts.value.length}장` },
    { label: '완료 미션', desc: '매달 진행했던 미션 기록', icon: 'mission', path: '/missions', badge: '-' },
  ]
})

function goEdit() {
  router.push({ name: 'TravelRegister', query: { mode: 'edit' } })
}

onMounted(async () => {
  loading.value = true
  try {
    const tripId = route.params.id
    const trips = (await fetchMyTrips()) || []
    trip.value = trips.find((t) => String(t.tripId) === String(tripId)) || null

    if (trip.value) {
      if (trip.value.status === 'ENDED') {
        report.value = await fetchPostTripReport(tripId)
      } else {
        report.value = await fetchPreTripReport(tripId)
        const receiptRes = await getReceipts(tripId)
        receipts.value = receiptRes.data?.data ?? []
      }
    }
  } catch (error) {
    console.error('여행 상세 조회 실패:', error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="min-h-screen pb-20 flex flex-col" style="background: #F4F5F9">

    <!-- 헤더 -->
    <div class="flex items-center gap-3 px-5 pt-3.5 pb-3">
      <button type="button" class="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl bg-white shadow-[0_5px_16px_rgba(36,72,117,0.07)]" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M15 6l-6 6 6 6" stroke="#193d82" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <h1 class="flex-1 text-center text-[18px] font-black text-gray-900 truncate">여행 관리</h1>
      <button v-if="!isEnded" type="button" class="text-[13px] font-bold flex-shrink-0" style="color:#2F6FED" @click="goEdit">편집</button>
      <span v-else class="text-[13px] font-bold flex-shrink-0" style="color:#2F6FED">공유</span>
    </div>

    <p v-if="loading" class="text-center text-xs text-gray-400 py-10">불러오는 중...</p>
    <p v-else-if="!trip" class="text-center text-xs text-gray-400 py-10">여행 정보를 찾을 수 없어요.</p>

    <div v-else class="px-4 flex flex-col gap-6">

      <!-- 여행 요약 카드 -->
      <div class="trip-summary-card relative rounded-[20px] p-5 overflow-hidden">
        <div class="absolute rounded-full" style="top:-54px; right:-38px; width:146px; height:146px; background: rgba(255,212,102,0.1)"></div>

        <div class="relative flex items-start justify-between gap-3">
          <div class="min-w-0 flex-1">
            <div class="flex min-w-0 items-center gap-2">
              <p class="truncate text-[17px] font-black">{{ trip.tripName }}</p>
              <div class="trip-detail-flags flex-shrink-0">
                <span v-for="(code, i) in countryCodes" :key="`${code}-${i}`" :class="flagIconClass(code)" class="fi-inline trip-detail-flag"></span>
              </div>
            </div>
            <p class="trip-summary-date font-mono text-[11.5px] font-bold mt-2">
              {{ formatDateRange(trip.startDate, trip.endDate) }} · {{ trip.totalDays }}일
            </p>
          </div>
          <span
            class="flex-shrink-0 rounded-full px-2.5 py-[5px] text-[10.5px] font-bold"
            :style="isEnded ? 'background:#fff3c7;color:#8c6500' : isTraveling ? 'background:#dff6ef;color:#07826e' : 'background:rgba(255,255,255,.72);color:#173f8d'"
          >{{ isEnded ? tripStatusLabel : isTraveling ? '여행 중' : `출국까지 ${dDayLabel}` }}</span>
        </div>

        <template v-if="!isEnded">
          <div class="trip-goal-section relative mt-4 pt-4">
            <div class="flex items-baseline justify-between text-[11px] font-bold">
              <span>여행 목표 자금</span>
              <span class="trip-goal-percent">{{ savingsPercent }}%</span>
            </div>
            <div class="trip-goal-bar h-1.5 rounded-full mt-2 overflow-hidden">
              <div class="h-full rounded-full" :style="{ width: `${savingsPercent}%` }"></div>
            </div>
            <p class="trip-goal-amount mt-2 text-right font-mono text-[11px] font-bold">{{ formatWon(report?.targetBudget) }}</p>
          </div>
        </template>

        <template v-else>
          <div class="relative mt-5 pt-4" style="border-top: 1px solid rgba(255,255,255,0.16)">
            <div class="flex items-end justify-between">
              <div>
                <p class="text-[11px] font-bold" style="color: rgba(255,255,255,0.6)">총 지출</p>
                <p class="font-mono text-[26px] font-bold mt-1">{{ formatWon(report?.spent) }}</p>
              </div>
              <div class="text-right">
                <p class="text-[11px] font-bold" style="color: rgba(255,255,255,0.6)">예산 대비</p>
                <p class="text-[13px] font-bold mt-1" style="color:#FFD466">
                  {{ budgetDiff >= 0 ? '-' : '+' }}{{ formatWon(Math.abs(budgetDiff)) }} {{ budgetDiff >= 0 ? '절약' : '초과' }}
                </p>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 통계 (예정된 여행에서만 노출) -->
      <div v-if="!isEnded" class="grid grid-cols-3 gap-3">
        <div class="bg-white rounded-2xl py-4 text-center" style="box-shadow: 0 4px 14px rgba(16,25,43,0.07)">
          <p class="text-[19px] font-black text-gray-900">{{ report?.scheduleCount ?? 0 }}</p>
          <p class="text-[11px] font-bold text-gray-400 mt-0.5">등록 일정</p>
        </div>
        <div class="bg-white rounded-2xl py-4 text-center" style="box-shadow: 0 4px 14px rgba(16,25,43,0.07)">
          <p class="text-[19px] font-black text-gray-900">{{ receipts.length }}</p>
          <p class="text-[11px] font-bold text-gray-400 mt-0.5">영수증</p>
        </div>
        <div class="bg-white rounded-2xl py-4 text-center">
          <p class="text-[17px] font-black text-gray-900 font-mono">{{ krwReceiptTotal.toLocaleString('ko-KR') }}</p>
          <p class="text-[11px] font-bold text-gray-400 mt-0.5">누적 지출(원)</p>
        </div>
      </div>

      <!-- 메뉴 -->
      <div class="flex flex-col gap-3">
        <h2 class="text-lg font-black text-gray-900 px-0.5">{{ isEnded ? '여행 기록' : '여행 관리 메뉴' }}</h2>

        <div class="grid grid-cols-2 gap-3">
          <button
            v-for="(item, i) in menuItems"
            :key="item.label"
            type="button"
            class="menu-tile text-left active:scale-[0.97]"
            :class="{ 'col-span-2': i === menuItems.length - 1 && menuItems.length % 2 === 1 }"
            :style="{ animationDelay: `${i * 70}ms` }"
            @click="router.push(item.path)"
          >
            <div class="flex items-start justify-between">
              <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0" style="background: #EEF2FF">
                <svg v-if="item.icon === 'report'" width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M14 2V8H20" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <line x1="8" y1="13" x2="16" y2="13" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
                  <line x1="8" y1="17" x2="12" y2="17" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
                </svg>
                <svg v-else-if="item.icon === 'checklist'" width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M9 11L12 14L22 4" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M21 12V19C21 19.5304 20.7893 20.0391 20.4142 20.4142C20.0391 20.7893 19.5304 21 19 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H16" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <svg v-else-if="item.icon === 'schedule'" width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <rect x="3" y="4" width="18" height="18" rx="2" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M16 2V6M8 2V6M3 10H21" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <svg v-else-if="item.icon === 'receipt'" width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M5 3H19V21L16.5 19.5L14 21L11.5 19.5L9 21L5 19V3Z" stroke="#3B5BDB" stroke-width="2" stroke-linejoin="round"/>
                  <path d="M9 8H15M9 12H15M9 16H13" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
                </svg>
                <svg v-else-if="item.icon === 'mission'" width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="9" stroke="#3B5BDB" stroke-width="2"/>
                  <path d="M9 12L11 14L15.5 9.5" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <span
                v-if="item.badge"
                class="flex-shrink-0 text-[10.5px] font-bold px-2 py-[3px] rounded-full"
                :style="item.badge === '준비 중' ? 'background:#EAF1FF; color:#0B2A6B' : 'background:#F4F5F9; color:#111827'"
              >{{ item.badge }}</span>
            </div>
            <p class="text-sm font-semibold text-gray-900 mt-3">{{ item.label }}</p>
            <p class="text-xs text-gray-400 mt-0.5">{{ item.desc }}</p>
          </button>
        </div>
      </div>
    </div>

    <BottomNav />
  </div>
</template>

<style scoped>
.trip-detail-flags{display:flex;align-items:center;gap:3px}
.trip-detail-flag{width:14px;height:9px;border-radius:2px;background-size:cover;box-shadow:0 1px 3px rgba(0,0,0,.18)}
.trip-summary-card{background:linear-gradient(135deg,#dce9fb 0%,#c8daf6 100%);color:#10234a;box-shadow:0 10px 24px rgba(35,73,136,.13)}
.trip-summary-date{color:#6680a8}
.trip-goal-section{border-top:1px solid rgba(23,63,141,.16);color:#365b96}
.trip-goal-percent{color:#173f8d}
.trip-goal-bar{background:rgba(255,255,255,.7)}
.trip-goal-bar>div{background:#2662ea}
.trip-goal-amount{color:#5e78a2}
</style>

<style scoped>
.menu-tile {
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(16, 25, 43, 0.07);
  opacity: 0;
  transform: translateY(18px) scale(0.9);
  animation: tile-pop-in 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
  transition: transform 0.15s ease;
}
@keyframes tile-pop-in {
  0% {
    opacity: 0;
    transform: translateY(18px) scale(0.9);
  }
  60% {
    opacity: 1;
    transform: translateY(-2px) scale(1.02);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
@media (prefers-reduced-motion: reduce) {
  .menu-tile {
    animation: none;
    opacity: 1;
    transform: none;
  }
}
</style>
