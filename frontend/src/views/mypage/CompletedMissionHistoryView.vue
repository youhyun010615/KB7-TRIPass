<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { fetchSavingMissions } from '@/api/savingMissions'
import { fetchMyTrips } from '@/api/travel'
import { useAuthStore } from '@/stores/auth'
import pigMoneyIcon from '@/assets/icons/pig-money.svg'
import foodIcon from '@/assets/icons/food.svg'
import cafeIcon from '@/assets/icons/cafe.svg'
import taxiIcon from '@/assets/icons/taxi.svg'
import shoppingIcon from '@/assets/icons/shopping-cart.svg'
import hobbyIcon from '@/assets/icons/hobby_drink.svg'
import homeIcon from '@/assets/icons/home-dollar.svg'
import { useRoute } from 'vue-router'
import { today as currentDate } from '@/utils/devDate'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const tripId = computed(() => Number(route.query.tripId) || null)
const loading = ref(true)
const errorMessage = ref('')
const monthlyRecords = ref([])
const expandedMonth = ref('')

const DEMO_MISSION_ACCOUNTS = new Set([
  'lay1217',
  'ahyoung021217@gmail.com',
])
const DEMO_TRIP = {
  name: '유럽 3개국 여행',
  startDate: '2027-04-04',
  endDate: '2027-04-18',
}

const DEMO_MISSION_HISTORY = [
  {
    key: '2026-08', year: 2026, month: 8, completedCount: 1, failedCount: 0,
    activeWeeks: 1, savedAmount: 70922,
    weeks: [{ weekNumber: 4, periodStartDate: '2026-08-24', periodEndDate: '2026-08-28', savedAmount: 70922,
      missions: [{ id: 'demo-2026-08-food', title: '식비 지출 30% 줄이기', categoryCode: 'FOOD', savedAmount: 70922 }] }],
  },
  {
    key: '2026-09', year: 2026, month: 9, completedCount: 2, failedCount: 6,
    activeWeeks: 4, savedAmount: 12563,
    weeks: [
      { weekNumber: 2, periodStartDate: '2026-09-07', periodEndDate: '2026-09-13', savedAmount: 6000,
        missions: [{ id: 'demo-2026-09-cafe', title: '카페 지출 10% 줄이기', categoryCode: 'CAFE', savedAmount: 6000 }] },
      { weekNumber: 4, periodStartDate: '2026-09-21', periodEndDate: '2026-09-27', savedAmount: 6563,
        missions: [{ id: 'demo-2026-09-living', title: '생활비 지출 10% 줄이기', categoryCode: 'LIVING', savedAmount: 6563 }] },
    ],
  },
  {
    key: '2026-10', year: 2026, month: 10, completedCount: 5, failedCount: 3,
    activeWeeks: 4, savedAmount: 209991,
    weeks: [
      { weekNumber: 1, periodStartDate: '2026-10-01', periodEndDate: '2026-10-04', savedAmount: 83000,
        missions: [
          { id: 'demo-2026-10-food', title: '식비 지출 30% 줄이기', categoryCode: 'FOOD', savedAmount: 45000 },
          { id: 'demo-2026-10-shopping', title: '쇼핑 지출 30% 줄이기', categoryCode: 'SHOPPING', savedAmount: 38000 },
        ] },
      { weekNumber: 2, periodStartDate: '2026-10-05', periodEndDate: '2026-10-11', savedAmount: 52000,
        missions: [{ id: 'demo-2026-10-transport', title: '교통 지출 30% 줄이기', categoryCode: 'TRANSPORT', savedAmount: 52000 }] },
      { weekNumber: 3, periodStartDate: '2026-10-12', periodEndDate: '2026-10-18', savedAmount: 35000,
        missions: [{ id: 'demo-2026-10-hobby', title: '취미여가 지출 30% 줄이기', categoryCode: 'HOBBY', savedAmount: 35000 }] },
      { weekNumber: 4, periodStartDate: '2026-10-19', periodEndDate: '2026-10-25', savedAmount: 39991,
        missions: [{ id: 'demo-2026-10-cafe', title: '카페 지출 30% 줄이기', categoryCode: 'CAFE', savedAmount: 39991 }] },
    ],
  },
  {
    key: '2026-11', year: 2026, month: 11, completedCount: 4, failedCount: 4,
    activeWeeks: 4, savedAmount: 34222,
    weeks: [
      { weekNumber: 1, periodStartDate: '2026-11-01', periodEndDate: '2026-11-07', savedAmount: 8000,
        missions: [{ id: 'demo-2026-11-cafe', title: '카페 지출 10% 줄이기', categoryCode: 'CAFE', savedAmount: 8000 }] },
      { weekNumber: 2, periodStartDate: '2026-11-08', periodEndDate: '2026-11-14', savedAmount: 9222,
        missions: [{ id: 'demo-2026-11-food', title: '식비 지출 10% 줄이기', categoryCode: 'FOOD', savedAmount: 9222 }] },
      { weekNumber: 3, periodStartDate: '2026-11-15', periodEndDate: '2026-11-21', savedAmount: 7000,
        missions: [{ id: 'demo-2026-11-living', title: '생활비 지출 10% 줄이기', categoryCode: 'LIVING', savedAmount: 7000 }] },
      { weekNumber: 4, periodStartDate: '2026-11-22', periodEndDate: '2026-11-28', savedAmount: 10000,
        missions: [{ id: 'demo-2026-11-shopping', title: '쇼핑 지출 10% 줄이기', categoryCode: 'SHOPPING', savedAmount: 10000 }] },
    ],
  },
  {
    key: '2026-12', year: 2026, month: 12, completedCount: 1, failedCount: 7,
    activeWeeks: 4, savedAmount: 2278,
    weeks: [{ weekNumber: 3, periodStartDate: '2026-12-14', periodEndDate: '2026-12-20', savedAmount: 2278,
      missions: [{ id: 'demo-2026-12-cafe', title: '카페 지출 10% 줄이기', categoryCode: 'CAFE', savedAmount: 2278 }] }],
  },
  {
    key: '2027-01', year: 2027, month: 1, completedCount: 5, failedCount: 3,
    activeWeeks: 4, savedAmount: 287068,
    weeks: [
      { weekNumber: 1, periodStartDate: '2027-01-01', periodEndDate: '2027-01-07', savedAmount: 145000,
        missions: [
          { id: 'demo-2027-01-food', title: '식비 지출 50% 줄이기', categoryCode: 'FOOD', savedAmount: 80000 },
          { id: 'demo-2027-01-shopping', title: '쇼핑 지출 50% 줄이기', categoryCode: 'SHOPPING', savedAmount: 65000 },
        ] },
      { weekNumber: 2, periodStartDate: '2027-01-08', periodEndDate: '2027-01-14', savedAmount: 52000,
        missions: [{ id: 'demo-2027-01-transport', title: '교통 지출 30% 줄이기', categoryCode: 'TRANSPORT', savedAmount: 52000 }] },
      { weekNumber: 3, periodStartDate: '2027-01-15', periodEndDate: '2027-01-21', savedAmount: 45000,
        missions: [{ id: 'demo-2027-01-hobby', title: '취미여가 지출 30% 줄이기', categoryCode: 'HOBBY', savedAmount: 45000 }] },
      { weekNumber: 4, periodStartDate: '2027-01-22', periodEndDate: '2027-01-28', savedAmount: 45068,
        missions: [{ id: 'demo-2027-01-living', title: '생활비 지출 30% 줄이기', categoryCode: 'LIVING', savedAmount: 45068 }] },
    ],
  },
  {
    key: '2027-02', year: 2027, month: 2, completedCount: 4, failedCount: 4,
    activeWeeks: 4, savedAmount: 190101,
    weeks: [
      { weekNumber: 1, periodStartDate: '2027-02-01', periodEndDate: '2027-02-07', savedAmount: 55000,
        missions: [{ id: 'demo-2027-02-food', title: '식비 지출 30% 줄이기', categoryCode: 'FOOD', savedAmount: 55000 }] },
      { weekNumber: 2, periodStartDate: '2027-02-08', periodEndDate: '2027-02-14', savedAmount: 48000,
        missions: [{ id: 'demo-2027-02-shopping', title: '쇼핑 지출 30% 줄이기', categoryCode: 'SHOPPING', savedAmount: 48000 }] },
      { weekNumber: 3, periodStartDate: '2027-02-15', periodEndDate: '2027-02-21', savedAmount: 42101,
        missions: [{ id: 'demo-2027-02-transport', title: '교통 지출 30% 줄이기', categoryCode: 'TRANSPORT', savedAmount: 42101 }] },
      { weekNumber: 4, periodStartDate: '2027-02-22', periodEndDate: '2027-02-28', savedAmount: 45000,
        missions: [{ id: 'demo-2027-02-hobby', title: '취미여가 지출 30% 줄이기', categoryCode: 'HOBBY', savedAmount: 45000 }] },
    ],
  },
  {
    key: '2027-03', year: 2027, month: 3, completedCount: 3, failedCount: 5,
    activeWeeks: 4, savedAmount: 46105,
    weeks: [
      { weekNumber: 1, periodStartDate: '2027-03-01', periodEndDate: '2027-03-07', savedAmount: 16000,
        missions: [{ id: 'demo-2027-03-cafe', title: '카페 지출 10% 줄이기', categoryCode: 'CAFE', savedAmount: 16000 }] },
      { weekNumber: 2, periodStartDate: '2027-03-08', periodEndDate: '2027-03-14', savedAmount: 14105,
        missions: [{ id: 'demo-2027-03-living', title: '생활비 지출 10% 줄이기', categoryCode: 'LIVING', savedAmount: 14105 }] },
      { weekNumber: 4, periodStartDate: '2027-03-22', periodEndDate: '2027-03-28', savedAmount: 16000,
        missions: [{ id: 'demo-2027-03-food', title: '식비 지출 10% 줄이기', categoryCode: 'FOOD', savedAmount: 16000 }] },
    ],
  },
]

const iconByCategory = {
  FOOD: foodIcon,
  CAFE: cafeIcon,
  TRANSPORT: taxiIcon,
  TRANSPORTATION: taxiIcon,
  SHOPPING: shoppingIcon,
  HOBBY: hobbyIcon,
  LEISURE: hobbyIcon,
  LIVING: homeIcon,
  LIFE: homeIcon,
}

function recentYearMonths(count = 12) {
  const now = currentDate()
  return Array.from({ length: count }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - index, 1)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  })
}

function amount(value) {
  return Number(value || 0).toLocaleString('ko-KR')
}

function shortDate(value) {
  if (!value) return ''
  const [, month, day] = String(value).split('-')
  return `${Number(month)}.${Number(day)}`
}

function missionIcon(code) {
  return iconByCategory[String(code || '').toUpperCase()] || pigMoneyIcon
}

function normalizeMonth(yearMonth, response) {
  const weeks = new Map()

  for (const mission of response?.missions || []) {
    for (const weekly of mission.weeklyMissions || []) {
      if (weekly.status !== 'SUCCESS') continue
      const weekNumber = Number(weekly.weekNumber || 0)
      if (!weeks.has(weekNumber)) {
        weeks.set(weekNumber, {
          weekNumber,
          periodStartDate: weekly.periodStartDate,
          periodEndDate: weekly.periodEndDate,
          missions: [],
        })
      }
      weeks.get(weekNumber).missions.push({
        id: weekly.id,
        title: weekly.missionMessage || `${mission.categoryName || '저축'} 미션 완료`,
        categoryCode: mission.categoryCode,
        savedAmount: Number(weekly.rewardAmount ?? weekly.actualSaving ?? 0),
      })
    }
  }

  const weekList = [...weeks.values()]
    .sort((a, b) => a.weekNumber - b.weekNumber)
    .map((week) => ({
      ...week,
      savedAmount: week.missions.reduce((sum, mission) => sum + mission.savedAmount, 0),
    }))
  if (!weekList.length) return null

  const [year, month] = yearMonth.split('-').map(Number)
  return {
    key: yearMonth,
    year,
    month,
    weeks: weekList,
    completedCount: weekList.reduce((sum, week) => sum + week.missions.length, 0),
    failedCount: 0,
    activeWeeks: weekList.length,
    savedAmount: weekList.reduce((sum, week) => sum + week.savedAmount, 0),
  }
}

const totalSaved = computed(() => monthlyRecords.value.reduce((sum, month) => sum + month.savedAmount, 0))
const totalCompleted = computed(() => monthlyRecords.value.reduce((sum, month) => sum + month.completedCount, 0))

function monthLabel(record) {
  return `${String(record.year).slice(-2)}년 ${record.month}월`
}

function toggleMonth(key) {
  expandedMonth.value = expandedMonth.value === key ? '' : key
}

async function loadHistory() {
  loading.value = true
  errorMessage.value = ''
  const identity = String(authStore.user?.loginId || authStore.user?.email || '').trim().toLowerCase()
  if (DEMO_MISSION_ACCOUNTS.has(identity) && tripId.value) {
    try {
      const trips = await fetchMyTrips()
      const trip = (trips || []).find(item => String(item.tripId) === String(tripId.value))
      const isDemoTrip = trip?.tripName === DEMO_TRIP.name
        && trip?.startDate === DEMO_TRIP.startDate
        && trip?.endDate === DEMO_TRIP.endDate
      const dDayReached = currentDate() >= new Date(`${DEMO_TRIP.startDate}T00:00:00`)
      if (isDemoTrip && dDayReached) {
        monthlyRecords.value = DEMO_MISSION_HISTORY
        expandedMonth.value = DEMO_MISSION_HISTORY[0].key
        loading.value = false
        return
      }
    } catch (error) {
      console.error('시연 여행 확인 실패:', error)
    }
  }
  const targets = recentYearMonths()
  const results = await Promise.allSettled(targets.map((target) => fetchSavingMissions(target)))
  monthlyRecords.value = results.flatMap((result, index) => {
    if (result.status !== 'fulfilled') return []
    const normalized = normalizeMonth(targets[index], result.value)
    return normalized ? [normalized] : []
  })
  const failures = results.filter((result) => result.status === 'rejected')
  if (!monthlyRecords.value.length && failures.length === results.length) {
    const nonEmptyFailure = failures.find(({ reason }) => reason?.response?.status !== 404)
    if (nonEmptyFailure) errorMessage.value = '완료 미션 기록을 불러오지 못했어요.'
  }
  expandedMonth.value = monthlyRecords.value[0]?.key || ''
  loading.value = false
}

onMounted(loadHistory)
</script>

<template>
  <main class="mission-history-page">
    <header class="page-header">
      <button type="button" class="back-button" aria-label="뒤로 가기" @click="router.back()">‹</button>
      <div>
        <p>MISSION RECORD</p>
        <h1>수행 완료 미션</h1>
      </div>
    </header>

    <section v-if="loading" class="state-card loading-state" aria-live="polite">
      <span class="loading-ring" />
      <h2>완료 미션을 불러오고 있어요</h2>
    </section>

    <section v-else-if="errorMessage" class="state-card">
      <div class="empty-icon"><img :src="pigMoneyIcon" alt="" /></div>
      <h2>{{ errorMessage }}</h2>
      <p>잠시 후 다시 시도해 주세요.</p>
      <button type="button" class="retry-button" @click="loadHistory">다시 시도</button>
    </section>

    <section v-else-if="!monthlyRecords.length" class="state-card empty-state">
      <div class="empty-icon"><img :src="pigMoneyIcon" alt="" /></div>
      <h2>완료 미션이 없어요</h2>
      <p>미션을 완료하면 월별 저축 기록을<br />이곳에서 확인할 수 있어요.</p>
    </section>

    <template v-else>
      <section class="total-card">
        <p class="total-kicker">미션으로 모은 저축액</p>
        <div class="total-amount"><strong>{{ amount(totalSaved) }}원</strong></div>
        <div class="total-divider" />
        <div class="total-footer"><span>수행 완료 미션 {{ totalCompleted }}개</span></div>
      </section>

      <section class="month-list">
        <article v-for="record in monthlyRecords" :key="record.key" class="month-card" :class="{ expanded: expandedMonth === record.key }">
          <button type="button" class="month-summary" :aria-expanded="expandedMonth === record.key" @click="toggleMonth(record.key)">
            <strong>{{ monthLabel(record) }}</strong>
            <span><b>완료 {{ record.completedCount }}개</b><em>+{{ amount(record.savedAmount) }}원</em></span>
            <i aria-hidden="true">⌄</i>
          </button>
          <div v-if="expandedMonth === record.key" class="week-list">
            <section v-for="week in record.weeks" :key="week.weekNumber" class="week-section">
              <div class="week-header">
                <strong>{{ week.weekNumber }}주차 <small>{{ shortDate(week.periodStartDate) }}–{{ shortDate(week.periodEndDate) }}</small></strong>
                <span><em>성공</em><b>+{{ amount(week.savedAmount) }}원</b></span>
              </div>
              <div class="mission-rows">
                <div v-for="mission in week.missions" :key="mission.id" class="mission-row">
                  <span><img :src="missionIcon(mission.categoryCode)" alt="" /></span>
                  <strong>{{ mission.title }}</strong>
                  <b>{{ amount(mission.savedAmount) }}원</b>
                </div>
              </div>
            </section>
          </div>
        </article>
      </section>
    </template>
    <BottomNav />
  </main>
</template>

<style scoped>
.mission-history-page{min-height:100vh;padding:0 12px 108px;background:#f3f6fc;color:#101a30}.page-header{display:flex;align-items:center;gap:20px;padding:18px 2px 20px}.back-button{display:grid;width:42px;height:42px;flex:0 0 auto;place-items:center;border:1px solid #dbe3ef;border-radius:50%;background:#fff;color:#607087;font-size:29px;line-height:1;box-shadow:0 5px 15px rgba(26,53,99,.05)}.page-header p{color:#173f8d;font-family:'Space Mono',monospace;font-size:10px;font-weight:900;letter-spacing:.22em}.page-header h1{margin-top:5px;font-size:21px;font-weight:950;letter-spacing:-.04em}.total-card{padding:22px 24px 20px;border-radius:20px;background:linear-gradient(145deg,#173f8d 0%,#1e51ba 100%);color:#fff;box-shadow:0 14px 30px rgba(23,63,141,.18)}.total-kicker{color:#ffd45e;font-family:'Space Mono',monospace;font-size:11px;font-weight:900;letter-spacing:.12em}.total-amount{display:flex;align-items:baseline;gap:12px;margin-top:13px}.total-amount strong{font-family:'Space Mono',monospace;font-size:28px;font-weight:900;letter-spacing:-.07em}.total-amount span{color:#b7c9ec;font-size:11px;font-weight:800}.total-divider{margin:18px 0 14px;border-top:1px solid rgba(255,255,255,.2)}.total-footer{display:flex;align-items:center;justify-content:space-between;color:#c5d4f0;font-size:11px;font-weight:850}.total-footer b{color:#ffd45e}.month-list{display:grid;gap:10px;margin-top:16px}.month-card{overflow:hidden;border:1px solid #e6ebf3;border-radius:18px;background:#fff;box-shadow:0 8px 22px rgba(22,43,80,.06)}.month-summary{display:grid;width:100%;grid-template-columns:72px 1fr 20px;align-items:center;padding:19px 18px;text-align:left}.month-summary>strong{font-size:17px;font-weight:950}.month-summary>span b,.month-summary>span em{display:block}.month-summary>span b{color:#9aa7ba;font-size:11px}.month-summary>span em{margin-top:5px;color:#173f8d;font-family:'Space Mono',monospace;font-size:14px;font-style:normal;font-weight:900}.month-summary>i{color:#aab6c7;font-size:21px;font-style:normal;transition:transform .2s ease}.month-card.expanded .month-summary>i{transform:rotate(180deg)}.week-list{padding:0 18px 17px}.week-section{padding-top:15px;border-top:1px solid #edf0f5}.week-section+.week-section{margin-top:16px}.week-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:9px}.week-header>strong{font-size:12px}.week-header small{margin-left:5px;color:#9aa7ba;font-size:9px}.week-header>b{color:#0fae96;font-family:'Space Mono',monospace;font-size:12px}.mission-rows{padding:4px 12px;border-radius:14px;background:#f7f9fc}.mission-row{display:grid;grid-template-columns:30px 1fr auto;align-items:center;gap:8px;min-height:47px}.mission-row+.mission-row{border-top:1px solid #e9edf4}.mission-row>span{display:grid;width:27px;height:27px;place-items:center;border-radius:50%;background:#eaf1ff}.mission-row img{width:15px;height:15px;object-fit:contain}.mission-row>strong{min-width:0;font-size:11px;font-weight:850}.mission-row>b{color:#173f8d;font-family:'Space Mono',monospace;font-size:11px}.state-card{display:flex;min-height:430px;flex-direction:column;align-items:center;justify-content:center;padding:30px;text-align:center}.empty-icon{display:grid;width:86px;height:86px;place-items:center;border-radius:28px;background:#e8f0ff;animation:empty-float 2.2s ease-in-out infinite}.empty-icon img{width:48px;height:48px}.state-card h2{margin-top:22px;font-size:18px;font-weight:950}.state-card p{margin-top:10px;color:#8b99ad;font-size:11px;font-weight:650;line-height:1.7}.retry-button{margin-top:20px;padding:12px 24px;border-radius:14px;background:#173f8d;color:#fff;font-size:12px;font-weight:900}.loading-ring{width:42px;height:42px;border:4px solid #dce7fa;border-top-color:#2662ea;border-radius:50%;animation:spin .85s linear infinite}.loading-state h2{color:#62738b;font-size:14px}@keyframes spin{to{transform:rotate(360deg)}}@keyframes empty-float{0%,100%{transform:translateY(0)}50%{transform:translateY(-8px)}}@media(max-width:370px){.total-amount{align-items:flex-start;flex-direction:column;gap:5px}.total-amount strong{font-size:25px}.month-summary{grid-template-columns:62px 1fr 18px;padding-inline:14px}}@media(prefers-reduced-motion:reduce){.empty-icon,.loading-ring{animation:none}.month-summary>i{transition:none}}
.total-card{padding:17px 22px 15px;border-radius:18px}.total-kicker{font-size:14px;letter-spacing:-.01em}.total-amount{margin-top:8px}.total-amount strong{font-size:26px}.total-divider{margin:12px 0 10px}.total-footer{justify-content:flex-start}.month-summary{grid-template-columns:82px 1fr 20px;padding-block:17px}.month-summary>strong{font-size:15px}.month-summary>span b{color:#10a57e}.month-summary>i{transition:none}.month-card.expanded .month-summary>i{transform:none}.week-header>span{display:flex;align-items:center;gap:7px}.week-header>span em{padding:3px 7px;border:1px solid #0fae96;border-radius:7px;color:#0b9d87;font-size:9px;font-style:normal;font-weight:900}.week-header>span b{color:#0fae96;font-family:'Space Mono',monospace;font-size:12px}.mission-row img{width:17px;height:17px}
</style>
