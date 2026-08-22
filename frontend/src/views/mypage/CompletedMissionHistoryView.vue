<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { fetchSavingMissions } from '@/api/savingMissions'
import pigMoneyIcon from '@/assets/icons/pig-money.svg'
import foodIcon from '@/assets/icons/food.svg'
import cafeIcon from '@/assets/icons/cafe.svg'
import taxiIcon from '@/assets/icons/taxi.svg'
import shoppingIcon from '@/assets/icons/shopping-cart.svg'
import hobbyIcon from '@/assets/icons/hobby_drink.svg'
import homeIcon from '@/assets/icons/home-dollar.svg'
import { useRoute } from 'vue-router'
import TravelArchiveSummaryCard from '@/components/mypage/TravelArchiveSummaryCard.vue'

const router = useRouter()
const route = useRoute()
const tripId = computed(() => Number(route.query.tripId) || null)
const loading = ref(true)
const errorMessage = ref('')
const monthlyRecords = ref([])
const expandedMonth = ref('')

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
  const now = new Date()
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
    savedAmount: weekList.reduce((sum, week) => sum + week.savedAmount, 0),
  }
}

const totalSaved = computed(() => monthlyRecords.value.reduce((sum, month) => sum + month.savedAmount, 0))
const totalCompleted = computed(() => monthlyRecords.value.reduce((sum, month) => sum + month.completedCount, 0))
const displayYear = computed(() => monthlyRecords.value[0]?.year || new Date().getFullYear())

function monthLabel(record) {
  return record.year === displayYear.value ? `${record.month}월` : `${record.year}년 ${record.month}월`
}

function toggleMonth(key) {
  expandedMonth.value = expandedMonth.value === key ? '' : key
}

async function loadHistory() {
  loading.value = true
  errorMessage.value = ''
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

    <TravelArchiveSummaryCard v-if="tripId" class="mission-trip-summary" :trip-id="tripId" />

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
        <p class="total-kicker">TOTAL SAVED</p>
        <div class="total-amount"><strong>{{ amount(totalSaved) }}원</strong><span>미션으로 모은 저축액</span></div>
        <div class="total-divider" />
        <div class="total-footer"><span>수행 완료 미션 {{ totalCompleted }}개</span><b>{{ displayYear }}년</b></div>
      </section>

      <section class="month-list">
        <article v-for="record in monthlyRecords" :key="record.key" class="month-card" :class="{ expanded: expandedMonth === record.key }">
          <button type="button" class="month-summary" :aria-expanded="expandedMonth === record.key" @click="toggleMonth(record.key)">
            <strong>{{ monthLabel(record) }}</strong>
            <span><b>완료 {{ record.completedCount }}개 · {{ record.weeks.length }}주 활동</b><em>+{{ amount(record.savedAmount) }}원</em></span>
            <i aria-hidden="true">⌄</i>
          </button>
          <div v-if="expandedMonth === record.key" class="week-list">
            <section v-for="week in record.weeks" :key="week.weekNumber" class="week-section">
              <div class="week-header">
                <strong>{{ week.weekNumber }}주차 <small>{{ shortDate(week.periodStartDate) }}–{{ shortDate(week.periodEndDate) }}</small></strong>
                <b>+{{ amount(week.savedAmount) }}원</b>
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
</style>
