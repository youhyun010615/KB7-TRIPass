<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { fetchMyTrips } from '@/api/travel'
import { fetchPreTripReport, fetchPostTripReport } from '@/api/report'
import { countryPresentation, flagIconClass, useTravelStore } from '@/stores/travel'

const router = useRouter()
const travelStore = useTravelStore()
const showActiveTripNotice = ref(false)

function startNewTrip() {
  if (upcomingTrips.value.length > 0) {
    showActiveTripNotice.value = true
    return
  }
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
  if (isTraveling(trip)) return '여행 중'
  const days = daysUntilStart(trip)
  return days === null ? 'D-' : `D-${Math.max(0, days)}`
}

function formatDateRange(startDate, endDate) {
  const fmt = (d) => (d ? d.replaceAll('-', '.') : '')
  return `${fmt(startDate)} - ${fmt(endDate)}`
}

function formatWon(amount) {
  return `${Number(amount || 0).toLocaleString('ko-KR')}원`
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
        <div class="relative flex mt-3">
          <div class="flex-1">
            <div class="flex items-center gap-2">
              <p class="font-mono text-[26px] font-bold">{{ visitedCountryCount }}</p>
              <div class="flex items-center gap-1">
                <span v-for="(code, i) in visitedCountryCodes" :key="`${code}-${i}`" :class="flagIconClass(code)" class="fi-inline travel-record-flag"></span>
              </div>
            </div>
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
          <div class="relative flex min-h-[94px] items-center px-[18px] py-[17px] pr-[48px]">
            <div class="min-w-0 flex-1">
              <div class="flex min-w-0 items-center gap-2">
                <p class="truncate text-[15.5px] font-black text-gray-900">{{ trip.tripName }}</p>
                <span class="travel-list-flags flex-shrink-0 whitespace-nowrap">
                  <span v-for="countryName in splitCountryNames(trip.countryNames)" :key="countryName" :class="flagIconClass(countryCodeOf(countryName))" class="fi-inline travel-list-flag"></span>
                </span>
              </div>
              <p class="font-mono text-[11.5px] font-bold text-gray-400 mt-1.5">{{ formatDateRange(trip.startDate, trip.endDate) }} · {{ trip.totalDays }}일</p>
            </div>
            <div class="absolute right-[14px] top-[14px] flex flex-col items-end gap-3">
              <span
                class="flex-shrink-0 text-[10.5px] font-bold px-2.5 py-[5px] rounded-full"
                :style="isTraveling(trip) ? 'background:#E5F8F2; color:#0FAE96' : 'background:#EAF1FF; color:#0B2A6B'"
              >{{ statusLabel(trip) }}</span>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M9 6l6 6-6 6" stroke="#A7B0C0" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </div>
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

    <div v-if="showActiveTripNotice" class="fixed inset-0 z-[120] grid place-items-center bg-slate-950/45 px-5" @click.self="showActiveTripNotice = false">
      <section class="w-full max-w-sm rounded-[24px] bg-white p-6 text-center shadow-2xl">
        <span class="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-[#EAF1FF] text-2xl">✈</span>
        <h2 class="mt-4 text-lg font-black text-gray-900">진행 중인 여행이 있어요</h2>
        <p class="mt-2 text-xs font-semibold leading-5 text-gray-500">현재 여행을 마친 뒤 새로운 여행 목표를 등록할 수 있어요.<br>먼저 진행 중인 여행을 확인해 주세요.</p>
        <button type="button" class="mt-5 w-full rounded-2xl bg-[#173F8D] py-3.5 text-sm font-black text-white" @click="showActiveTripNotice = false">확인</button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.travel-record-flag{width:13px;height:9px;border-radius:2px;background-size:cover;box-shadow:0 1px 2px rgba(0,0,0,.2)}
.travel-list-flags{display:flex;gap:3px}
.travel-list-flag{width:14px;height:9px;border-radius:2px;background-size:cover;box-shadow:0 1px 2px rgba(15,23,42,.16)}
</style>
