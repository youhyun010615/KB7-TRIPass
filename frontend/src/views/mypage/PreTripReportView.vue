<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelReportStore } from '@/stores/travelReport'

const route = useRoute()
const router = useRouter()
const store = useTravelReportStore()
const tripId = computed(() => Number(route.query.tripId || 1))
const r = computed(() => store.preTripView)
const savingPercent = computed(() => r.value?.savingsPercent ?? 0)
const downloading = ref(false)

onMounted(() => store.loadPreTripReport(tripId.value))
watch(tripId, id => store.loadPreTripReport(id))

function money(v) {
  return Number(v).toLocaleString() + '원'
}
function download() {
  downloading.value = true
  setTimeout(() => (downloading.value = false), 1100)
}

// 저축 내역 막대그래프 높이 계산
const maxSaving = computed(() => Math.max(...(r.value?.savingHistory || []).map(h => Math.abs(h.amount)), 1))
function barHeight(amount) {
  return Math.round((Math.abs(amount) / maxSaving.value) * 60)
}

// 국가별 목표 예산 도넛 세그먼트 계산
const CIRCUMFERENCE = 251.2
const budgetSegments = computed(() => {
  const countrySpend = r.value?.countrySpend || []
  const total = countrySpend.reduce((s, c) => s + c.budget, 0) || 1
  let offset = 0
  return countrySpend.map(c => {
    const len = (c.budget / total) * CIRCUMFERENCE
    const seg = { color: c.color, len, gap: CIRCUMFERENCE - len, offset: -offset, percent: Math.round((c.budget / total) * 100) }
    offset += len
    return seg
  })
})
</script>

<template>
  <main class="page">
    <header><button type="button" @click="router.back()">‹</button><h1>여행 대비 리포트</h1><span /></header>

    <p v-if="!r && store.errorMessage" class="loading error">{{ store.errorMessage }}</p>
    <p v-else-if="!r" class="loading">불러오는 중...</p>

    <template v-else>
    <section class="summary">
      <small>TRIP SAVING REPORT</small>
      <h2>{{ r.trip.flags }} {{ r.trip.title }}</h2>
      <p>{{ r.trip.dateRange }} · 출국까지 {{ r.trip.dDay }}일</p>
    </section>

    <section class="card highlight">
      <p class="label">여행 저축 목표</p>
      <p class="big">{{ money(r.securedFund) }} <small>/ {{ money(r.targetBudget) }}</small></p>
      <div class="bar"><span :style="{ width: `${savingPercent}%` }" /></div>
      <p class="sub">달성률 {{ savingPercent }}%</p>
    </section>

    <section class="card">
      <h3>월렛 저축 내역</h3>
      <div class="saving-chart">
        <div v-for="h in r.savingHistory" :key="h.date" class="saving-bar">
          <span class="value" :class="{ minus: h.amount < 0 }">{{ h.amount > 0 ? '+' : '' }}{{ h.amount.toLocaleString() }}</span>
          <i :style="{ height: `${barHeight(h.amount)}px`, background: h.amount < 0 ? '#e5484d' : '#176be0' }" />
          <small>{{ h.date }}<br />{{ h.label }}</small>
        </div>
      </div>
    </section>

    <section class="card">
      <h3>국가별 목표 예산</h3>
      <div class="donut-row">
        <svg viewBox="0 0 100 100" class="donut">
          <g transform="rotate(-90 50 50)">
            <circle v-for="(seg, i) in budgetSegments" :key="i" cx="50" cy="50" r="40" fill="none" stroke-width="14"
              :stroke="seg.color" :stroke-dasharray="`${seg.len} ${seg.gap}`" :stroke-dashoffset="seg.offset" />
          </g>
          <text x="50" y="47" text-anchor="middle" class="donut-total">{{ Math.round(r.targetBudget / 10000) }}</text>
          <text x="50" y="60" text-anchor="middle" class="donut-unit">만원</text>
        </svg>
        <ul class="legend">
          <li v-for="(c, i) in r.countrySpend" :key="c.name">
            <i :style="{ background: c.color }" />{{ c.flag }} {{ c.name }}
            <b>{{ budgetSegments[i].percent }}%</b>
          </li>
        </ul>
      </div>
    </section>

    <section class="card">
      <h3>여행 준비 현황</h3>
      <table class="stat-table">
        <tr><td>체크리스트 완료</td><td>{{ r.checklistCompleted }}/{{ r.checklistTotal }}건</td></tr>
        <tr><td>등록된 여행 일정</td><td>{{ r.schedules }}건</td></tr>
        <tr><td>사전 결제 · 현장 결제</td><td>{{ r.paidSchedules }}건 · {{ r.pendingSchedules }}건</td></tr>
      </table>
    </section>

    <section class="final">
      <b>✈️ 여행 전 준비 상태</b>
      <small>여행 날짜가 되면 여행 모드에서 TRIP 월렛과 트래블 카드 잔액을 이어서 관리할 수 있어요.</small>
    </section>

    <button class="pdf" type="button" @click="download">{{ downloading ? 'PDF 생성 중...' : '▣ PDF 저장하기' }}</button>
    </template>

    <BottomNav />
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 0 16px 96px; background: #f8f6f1; color: #111a2d; }
.page > header { display: grid; height: 88px; grid-template-columns: 35px 1fr 35px; align-items: end; padding-bottom: 16px; }
.page > header button { font-size: 29px; text-align: left; }
.page > header h1 { text-align: center; font-size: 15px; font-weight: 900; }
.loading { padding: 40px 0; color: #8290a3; font-size: 11px; text-align: center; }
.loading.error { color: #e5484d; }
.summary { padding: 17px; border-radius: 14px; background: linear-gradient(135deg, #122f6d, #0d214f); color: #fff; }
.summary small { color: #a8c1e8; font-size: 8px; font-weight: 800; }
.summary h2 { margin-top: 10px; font-size: 15px; }
.summary p { margin-top: 5px; color: #b9c9e1; font-size: 8px; }
.card { margin-top: 11px; padding: 15px; border: 1px solid #e2e7ef; border-radius: 15px; background: #fff; }
.card h3 { font-size: 12px; font-weight: 900; }
.card.highlight { background: #eaf3ff; border-color: #cfe3ff; }
.card.highlight .label { color: #3970ad; font-size: 9px; font-weight: 800; }
.card.highlight .big { margin-top: 6px; color: #123a82; font-size: 20px; font-weight: 900; }
.card.highlight .big small { color: #5f7fae; font-size: 10px; font-weight: 700; }
.card.highlight .bar { height: 6px; margin-top: 10px; border-radius: 4px; background: #d7e6fb; overflow: hidden; }
.card.highlight .bar span { display: block; height: 100%; border-radius: 4px; background: #176be0; }
.card.highlight .sub { margin-top: 7px; color: #3970ad; font-size: 9px; }
.saving-chart { display: flex; justify-content: space-around; align-items: end; height: 110px; margin-top: 14px; }
.saving-bar { display: flex; flex-direction: column; align-items: center; justify-content: end; height: 100%; }
.saving-bar .value { margin-bottom: 4px; font-size: 8px; font-weight: 800; color: #176be0; }
.saving-bar .value.minus { color: #e5484d; }
.saving-bar i { display: block; width: 30px; border-radius: 4px 4px 0 0; }
.saving-bar small { margin-top: 6px; color: #8290a3; font-size: 7px; text-align: center; line-height: 1.4; }
.donut-row { display: flex; align-items: center; gap: 18px; margin-top: 14px; }
.donut { width: 100px; height: 100px; flex-shrink: 0; }
.donut-total { font-size: 15px; font-weight: 900; fill: #123a82; }
.donut-unit { font-size: 8px; fill: #8290a3; }
.legend { display: flex; flex-direction: column; gap: 8px; flex: 1; }
.legend li { display: flex; align-items: center; gap: 6px; font-size: 10px; }
.legend li i { width: 9px; height: 9px; border-radius: 3px; flex-shrink: 0; }
.legend li b { margin-left: auto; color: #8290a3; font-size: 10px; }
.stat-table { width: 100%; margin-top: 12px; font-size: 10px; }
.stat-table td { padding: 7px 0; border-bottom: 1px solid #edf0f4; color: #55708f; }
.stat-table td:last-child { text-align: right; font-weight: 800; color: #111a2d; }
.stat-table tr:last-child td { border: 0; }
.final { margin-top: 11px; padding: 14px; border-radius: 13px; background: #e8f8f3; color: #167e66; }
.final b, .final small { display: block; }
.final b { font-size: 10px; }
.final small { margin-top: 5px; font-size: 7px; }
.pdf { width: 100%; margin-top: 12px; padding: 14px; border-radius: 11px; background: #174695; color: #fff; font-size: 11px; font-weight: 900; }
</style>
