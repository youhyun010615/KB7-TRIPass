<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { fetchMyTrips } from '@/api/travel'
import { fetchPreTripReport, fetchPostTripReport } from '@/api/report'
import { countryPresentation, flagIconClass, useTravelStore } from '@/stores/travel'

const router = useRouter()
const travelStore = useTravelStore()

function startNewTrip() {
  // 스토어에 이전에 조회했던 여행 정보가 남아있을 수 있어서, 새 여행
  // 등록 폼이 빈 상태로 시작하도록 먼저 초기화한다.
  travelStore.resetGoal()
  router.push({ name: 'TravelRegister' })
}

const loading = ref(true)
const upcomingTrips = ref([])
const pastTrips = ref([])
const visitedCountryCodes = ref([])
const visitedCountryCount = ref(0)

function splitCountryNames(joined) {
  return (joined || '').split(' · ').map((name) => name.trim()).filter(Boolean)
}

function countryCodeOf(countryName) {
  return countryPresentation[countryName]?.code || ''
}

function daysUntilStart(trip) {
  if (trip.report?.daysUntilTrip !== null && trip.report?.daysUntilTrip !== undefined) {
    const reportDays = Number(trip.report.daysUntilTrip)
    if (Number.isFinite(reportDays)) return reportDays
  }
  if (!trip.startDate) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const startDate = new Date(`${trip.startDate}T00:00:00`)
  return Math.ceil((startDate - today) / 86400000)
}

function isTraveling(trip) {
  return trip.status === 'TRAVELING' || (trip.status !== 'ENDED' && daysUntilStart(trip) !== null && daysUntilStart(trip) <= 0)
}

function statusLabel(trip) {
  return isTraveling(trip) ? '여행 중' : '준비 중'
}

function formatDateRange(startDate, endDate) {
  const fmt = (d) => (d ? d.replaceAll('-', '.') : '')
  return `${fmt(startDate)} - ${fmt(endDate)}`
}

function formatWon(amount) {
  return `${Number(amount || 0).toLocaleString('ko-KR')}원`
}

function checklistLabel(trip) {
  const total = trip.report?.checklistTotal ?? 0
  const completed = trip.report?.checklistCompleted ?? 0
  return `체크리스트 ${completed}/${total}`
}

function savingsPercent(trip) {
  return Math.min(100, Math.max(0, Number(trip.report?.savingsPercent ?? 0)))
}

onMounted(async () => {
  loading.value = true
  try {
    const trips = (await fetchMyTrips()) || []

    const uniqueCountries = new Set()
    trips.forEach((t) =>
      splitCountryNames(t.countryNames).forEach((name) => uniqueCountries.add(name)),
    )
    visitedCountryCount.value = uniqueCountries.size
    visitedCountryCodes.value = [...uniqueCountries].slice(0, 4).map(countryCodeOf).filter(Boolean)

    const upcomingBase = trips
      .filter((t) => t.status !== 'ENDED')
      .sort((a, b) => {
        if (a.status !== b.status) return a.status === 'TRAVELING' ? -1 : 1
        return (a.startDate || '').localeCompare(b.startDate || '')
      })

    const pastBase = trips
      .filter((t) => t.status === 'ENDED')
      .sort((a, b) => (b.startDate || '').localeCompare(a.startDate || ''))

    const [upcomingDetails, pastDetails] = await Promise.all([
      Promise.all(
        upcomingBase.map(async (t) => {
          try {
            return { ...t, report: await fetchPreTripReport(t.tripId) }
          } catch {
            return { ...t, report: null }
          }
        }),
      ),
      Promise.all(
        pastBase.map(async (t) => {
          try {
            return { ...t, report: await fetchPostTripReport(t.tripId) }
          } catch {
            return { ...t, report: null }
          }
        }),
      ),
    ])

    upcomingTrips.value = upcomingDetails
    pastTrips.value = pastDetails
  } catch (error) {
    console.error('여행 목록 조회 실패:', error)
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
      <h1 class="flex-1 text-center text-[18px] font-black text-gray-900">여행 관리</h1>
      <span class="h-9 w-9 flex-shrink-0" aria-hidden="true"></span>
    </div>

    <p v-if="loading" class="text-center text-xs text-gray-400 py-10">불러오는 중...</p>

    <div v-else class="px-4 flex flex-col gap-6">

      <!-- TRAVEL RECORD -->
      <div class="relative rounded-[20px] text-white p-5 overflow-hidden" style="background: linear-gradient(155deg, #0B2A6B 0%, #123C94 62%, #17459F 100%); box-shadow: 0 10px 24px rgba(11,42,107,0.2)">
        <div class="absolute rounded-full" style="top:-54px; right:-38px; width:146px; height:146px; background: rgba(255,212,102,0.1)"></div>
        <p class="relative text-[11px] font-extrabold tracking-[0.1em]" style="color:#FFD466">TRAVEL RECORD</p>
        <div class="relative flex items-center gap-1 mt-2 text-lg">
          <span v-for="(code, i) in visitedCountryCodes" :key="`${code}-${i}`" :class="flagIconClass(code)" class="fi-inline travel-record-flag"></span>
        </div>
        <div class="relative flex mt-3">
          <div class="flex-1">
            <p class="font-mono text-[26px] font-bold">{{ visitedCountryCount }}</p>
            <p class="text-[11px] font-bold mt-[3px]" style="color: rgba(255,255,255,0.55)">방문 국가</p>
          </div>
          <div class="w-px" style="background: rgba(255,255,255,0.16)"></div>
          <div class="flex-1 pl-4">
            <p class="font-mono text-[26px] font-bold">{{ pastTrips.length }}</p>
            <p class="text-[11px] font-bold mt-[3px]" style="color: rgba(255,255,255,0.55)">완료 여행</p>
          </div>
        </div>
      </div>

      <!-- 다가오는 여행 -->
      <div class="flex flex-col gap-3">
        <div class="flex items-center justify-between px-0.5">
          <h2 class="text-lg font-black text-gray-900">다가오는 여행</h2>
          <span class="text-[12.5px] font-bold text-gray-400">{{ upcomingTrips.length }}개</span>
        </div>

        <p v-if="!upcomingTrips.length" class="text-center text-xs text-gray-400 py-6 bg-white rounded-2xl">
          예정된 여행이 없어요.
        </p>

        <button
          v-for="trip in upcomingTrips"
          :key="trip.tripId"
          type="button"
          class="bg-white rounded-2xl text-left w-full active:bg-gray-50 overflow-hidden"
          style="box-shadow: 0 4px 14px rgba(16,25,43,0.07)"
          @click="router.push(`/mypage/travel/${trip.tripId}`)"
        >
          <div class="px-[18px] pt-[18px] pb-4">
            <div class="flex items-center gap-2.5">
              <span class="travel-list-flags flex-shrink-0 whitespace-nowrap">
                <span v-for="countryName in splitCountryNames(trip.countryNames)" :key="countryName" :class="flagIconClass(countryCodeOf(countryName))" class="fi-inline travel-list-flag"></span>
              </span>
              <div class="flex-1 min-w-0">
                <p class="text-[15.5px] font-black text-gray-900 truncate">{{ trip.tripName }}</p>
                <p class="font-mono text-[11.5px] font-bold text-gray-400 mt-1">{{ formatDateRange(trip.startDate, trip.endDate) }} · {{ trip.totalDays }}일</p>
              </div>
              <span
                class="flex-shrink-0 text-[10.5px] font-bold px-2.5 py-[5px] rounded-full"
                :style="isTraveling(trip) ? 'background:#E5F8F2; color:#0FAE96' : 'background:#EAF1FF; color:#0B2A6B'"
              >{{ statusLabel(trip) }}</span>
            </div>

            <div v-if="trip.report" class="mt-4">
              <div class="flex items-baseline justify-between text-[11px] font-bold">
                <span style="color:#98A2B3">여행 자금 {{ formatWon(trip.report.targetBudget) }} 목표</span>
                <span style="color:#2F6FED">{{ savingsPercent(trip) }}%</span>
              </div>
              <div class="h-1.5 rounded-full mt-2 overflow-hidden" style="background:#EDF0F6">
                <div class="h-full rounded-full" :style="{ width: `${savingsPercent(trip)}%`, background: '#2F6FED' }"></div>
              </div>
            </div>
          </div>

          <div v-if="trip.report" class="flex items-center gap-3 px-[18px] py-[13px]" style="background:#F6F8FC; border-top:1px solid #ECEFF5">
            <span class="flex-1 text-[11.5px] font-bold" style="color:#5A6478">
              출국까지 D-{{ trip.report.daysUntilTrip }} · {{ checklistLabel(trip) }}
            </span>
            <span class="flex-shrink-0 text-[11.5px] font-bold" style="color:#2F6FED" @click.stop="router.push(`/mypage/travel/${trip.tripId}`)">
              일정 보기 ›
            </span>
          </div>
        </button>
      </div>

      <!-- 지난 여행 -->
      <div class="flex flex-col gap-3">
        <div class="flex items-center justify-between px-0.5">
          <div class="flex items-center gap-1.5">
            <h2 class="text-lg font-black text-gray-900">지난 여행</h2>
            <span class="text-[13px] font-bold" style="color:#2F6FED">{{ pastTrips.length }}개</span>
          </div>
          <span class="text-[12.5px] font-bold text-gray-400">전체보기</span>
        </div>

        <p v-if="!pastTrips.length" class="text-center text-xs text-gray-400 py-6 bg-white rounded-2xl">
          완료된 여행이 없어요.
        </p>

        <div v-else class="bg-white rounded-2xl overflow-hidden px-[18px]" style="box-shadow: 0 4px 14px rgba(16,25,43,0.07)">
          <button
            v-for="(trip, i) in pastTrips"
            :key="trip.tripId"
            type="button"
            class="w-full flex items-center gap-3 py-4 text-left active:bg-gray-50"
            :class="i < pastTrips.length - 1 ? 'border-b' : ''"
            style="border-color:#F1F3F8"
            @click="router.push(`/mypage/travel/${trip.tripId}`)"
          >
            <span :class="flagIconClass(countryCodeOf(splitCountryNames(trip.countryNames)[0]))" class="fi-inline travel-list-flag flex-shrink-0"></span>
            <div class="flex-1 min-w-0">
              <p class="text-[14.5px] font-extrabold text-gray-900 truncate">{{ trip.tripName }}</p>
              <p class="font-mono text-[11px] font-bold text-gray-400 mt-1">{{ formatDateRange(trip.startDate, trip.endDate) }} · {{ trip.totalDays }}일</p>
            </div>
            <p class="font-mono text-[12.5px] font-bold text-gray-900 flex-shrink-0">{{ formatWon(trip.report?.spent) }}</p>
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" class="flex-shrink-0"><path d="M9 6l6 6-6 6" stroke="#C7CDD8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
        </div>
      </div>

      <button
        type="button"
        class="w-full py-4 rounded-[18px] flex items-center justify-center gap-2"
        style="border: 1.5px dashed #C9D3E4; color:#2F6FED"
        @click="startNewTrip"
      >
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none"><path d="M12 5v14M5 12h14" stroke="#2F6FED" stroke-width="2.2" stroke-linecap="round"/></svg>
        <span class="text-[13.5px] font-bold">새 여행 등록하기</span>
      </button>
    </div>

    <BottomNav />
  </div>
</template>

<style scoped>
.travel-record-flag{width:25px;height:17px;border-radius:3px;background-size:cover;box-shadow:0 1px 3px rgba(0,0,0,.2)}
.travel-list-flags{display:flex;gap:3px}
.travel-list-flag{width:27px;height:18px;border-radius:3px;background-size:cover;box-shadow:0 1px 3px rgba(15,23,42,.18)}
</style>
