<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TravelManagementMenu from '@/components/mypage/TravelManagementMenu.vue'
import TravelArchiveSummaryCard from '@/components/mypage/TravelArchiveSummaryCard.vue'
import { deleteTrip, fetchMyTrips } from '@/api/travel'
import { fetchPreTripReport, fetchPostTripReport } from '@/api/report'
import { getReceipts } from '@/api/receipt'
import { countryPresentation, flagIconClass, useTravelStore } from '@/stores/travel'
import { daysUntilTrip, tripPhase } from '@/utils/tripLifecycle'

const router = useRouter()
const route = useRoute()
const travelStore = useTravelStore()

const loading = ref(true)
const trip = ref(null)
const report = ref(null)
const receipts = ref([])
const showDeleteConfirm = ref(false)
const deleting = ref(false)

function splitCountryNames(joined) {
  return (joined || '').split(' · ').map((name) => name.trim()).filter(Boolean)
}

function countryCodeOf(countryName) {
  return countryPresentation[countryName]?.code || ''
}

const isEnded = computed(() => tripPhase(trip.value) === 'ENDED')
const countryCodes = computed(() => splitCountryNames(trip.value?.countryNames).map(countryCodeOf).filter(Boolean))
const countryLabel = computed(() => splitCountryNames(trip.value?.countryNames).join(' · '))
const daysUntilStart = computed(() => {
  if (report.value?.daysUntilTrip !== null && report.value?.daysUntilTrip !== undefined) {
    const reportDays = Number(report.value.daysUntilTrip)
    if (Number.isFinite(reportDays)) return reportDays
  }
  return daysUntilTrip(trip.value)
})
const isTraveling = computed(() => tripPhase(trip.value) === 'TRAVELING')
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
      { label: '영수증 보관함', desc: 'OCR 영수증과 지출 기록', icon: 'receipt', path: `/mypage/travel/${tripId}/receipts` },
      { label: '완료 미션', desc: '매달 진행했던 미션 기록', icon: 'mission', path: `/mypage/missions?tripId=${tripId}` },
    ]
  }
  return [
    { label: '여행 리포트', desc: '저축 기록과 여행 후 지출 분석', icon: 'report', path: `/mypage/reports?tripId=${tripId}`, badge: isTraveling.value ? '열람 가능' : '준비 중' },
    { label: '체크리스트', desc: '여행 전 · 귀국 준비', icon: 'checklist', path: `/mypage/checklists?tripId=${tripId}`, badge: `${report.value?.checklistCompleted ?? 0}/${report.value?.checklistTotal ?? 0}` },
    { label: '여행 일정', desc: '등록한 일정 확인', icon: 'schedule', path: `/mypage/travel/${tripId}/schedules?tripId=${tripId}`, badge: `${report.value?.scheduleCount ?? 0}개` },
    { label: '영수증 보관함', desc: 'OCR 영수증과 지출 기록', icon: 'receipt', path: `/mypage/travel/${tripId}/receipts`, badge: `${receipts.value.length}장` },
    { label: '완료 미션', desc: '매달 진행했던 미션 기록', icon: 'mission', path: `/mypage/missions?tripId=${tripId}`, badge: '-' },
  ]
})

function goEdit() {
  router.push({ name: 'TravelRegister', query: { mode: 'edit' } })
}

async function confirmDeleteTrip() {
  if (!trip.value?.tripId || deleting.value) return
  deleting.value = true
  try {
    await deleteTrip(trip.value.tripId)
    travelStore.resetGoal()
    showDeleteConfirm.value = false
    await router.replace({ name: 'MypageTravel' })
  } catch (error) {
    console.error('여행 삭제 실패:', error)
    window.alert(error.response?.data?.message || '여행을 삭제하지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    deleting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const tripId = route.params.id
    const trips = (await fetchMyTrips()) || []
    trip.value = trips.find((t) => String(t.tripId) === String(tripId)) || null

    if (trip.value) {
      if (tripPhase(trip.value) === 'ENDED') {
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
  <div class="min-h-screen pb-8 flex flex-col" style="background: #F4F5F9">

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

      <TravelArchiveSummaryCard :trip-id="trip.tripId" />

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

        <TravelManagementMenu :items="menuItems" @select="router.push($event.path)" />

        <button
          type="button"
          class="trip-delete-button"
          @click="showDeleteConfirm = true"
        >
          여행 삭제하기
        </button>
      </div>
    </div>

    <div v-if="showDeleteConfirm" class="delete-modal-backdrop" @click.self="showDeleteConfirm = false">
      <section class="delete-modal" role="dialog" aria-modal="true" aria-labelledby="delete-trip-title">
        <div class="delete-modal-icon" aria-hidden="true">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M4 7h16M9 7V4h6v3m-8 0 1 13h8l1-13M10 11v5m4-5v5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h2 id="delete-trip-title">이 여행을 삭제할까요?</h2>
        <p>삭제한 여행은 목록과 홈에서 더 이상 표시되지 않아요.</p>
        <div class="delete-modal-actions">
          <button type="button" :disabled="deleting" @click="showDeleteConfirm = false">취소</button>
          <button type="button" class="danger" :disabled="deleting" @click="confirmDeleteTrip">
            {{ deleting ? '삭제 중...' : '삭제하기' }}
          </button>
        </div>
      </section>
    </div>
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
.trip-delete-button{width:100%;height:52px;border:1px solid #ffd9dc;border-radius:16px;background:#fff;color:#e34d59;font-size:14px;font-weight:800;transition:background .15s ease,transform .15s ease}
.trip-delete-button:active{background:#fff3f4;transform:scale(.99)}
.delete-modal-backdrop{position:fixed;inset:0;z-index:80;display:flex;align-items:center;justify-content:center;padding:24px;background:rgba(10,24,48,.48);backdrop-filter:blur(3px)}
.delete-modal{width:100%;max-width:340px;border-radius:24px;background:#fff;padding:28px 22px 20px;text-align:center;box-shadow:0 24px 64px rgba(12,31,68,.25)}
.delete-modal-icon{display:flex;width:52px;height:52px;margin:0 auto 16px;align-items:center;justify-content:center;border-radius:17px;background:#fff0f1;color:#e34d59}
.delete-modal h2{font-size:19px;font-weight:900;color:#101a31}
.delete-modal p{margin-top:8px;font-size:13px;line-height:1.6;color:#8290a9}
.delete-modal-actions{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:24px}
.delete-modal-actions button{height:48px;border-radius:14px;background:#f0f3f8;color:#53627b;font-size:14px;font-weight:800}
.delete-modal-actions button.danger{background:#e84e5a;color:#fff}
.delete-modal-actions button:disabled{opacity:.55}
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
