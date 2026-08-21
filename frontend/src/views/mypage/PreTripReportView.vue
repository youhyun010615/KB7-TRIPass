<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelReportStore } from '@/stores/travelReport'
import { exportElementToPdf } from '@/utils/pdf'
import { flagIconClass } from '@/stores/travel'

const route = useRoute()
const router = useRouter()
const store = useTravelReportStore()
const tripId = computed(() => {
  const parsed = Number(route.query.tripId)
  return route.query.tripId && Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const r = computed(() => store.preTripView)
const savingPercent = computed(() => r.value?.savingsPercent ?? 0)
const downloading = ref(false)
const reportContent = ref(null)

onMounted(() => {
  if (tripId.value) store.loadPreTripReport(tripId.value)
})
watch(tripId, id => {
  if (id) store.loadPreTripReport(id)
})

function money(v) {
  return Number(v).toLocaleString() + '원'
}
async function download() {
  if (downloading.value) return
  downloading.value = true
  try {
    await exportElementToPdf(reportContent.value, `여행대비리포트_${r.value.trip.title}.pdf`)
  } catch (error) {
    console.error('PDF 저장 실패:', error)
  } finally {
    downloading.value = false
  }
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
    <header><button type="button" @click="router.back()">‹</button><h1>여행 저축 리포트</h1><span /></header>

    <p v-if="!tripId" class="loading error">여행 정보를 찾을 수 없어요.</p>
    <p v-else-if="!r && store.errorMessage" class="loading error">{{ store.errorMessage }}</p>
    <p v-else-if="!r" class="loading">불러오는 중...</p>

    <template v-else>
    <div ref="reportContent" class="pdf-content">
    <section class="summary report-ticket">
      <div class="summary-kicker"><small>TRIP REPORT ARCHIVE</small><b>여행 저축</b></div>
      <div class="summary-title">
        <h2>{{ r.trip.title }}</h2>
        <span class="summary-flags"><i v-for="code in r.trip.countryCodes" :key="code" :class="flagIconClass(code)" class="fi-inline" /></span>
      </div>
      <p>{{ r.trip.dateRange }}</p>
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
        <div v-for="(h, i) in r.savingHistory" :key="`${h.date}-${i}`" class="saving-bar">
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
    </div>

    <button class="pdf" type="button" @click="download" :disabled="downloading">{{ downloading ? 'PDF 생성 중...' : 'PDF 저장하기' }}</button>
    </template>

    <BottomNav />
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 0 16px 96px; background: #f8f6f1; color: #111a2d; }
.page > header { display: grid; height: 63px; grid-template-columns: 35px 1fr 35px; align-items: end; padding-bottom: 16px; }
.page > header button { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 12px; background: #fff; color: #193d82; font-size: 24px; font-weight: 700; box-shadow: 0 5px 16px rgba(36, 72, 117, 0.07); }
.page > header h1 { text-align: center; font-size: 15px; font-weight: 900; }
.loading { padding: 40px 0; color: #8290a3; font-size: 11px; text-align: center; }
.loading.error { color: #e5484d; }
.summary { padding: 17px; border-radius: 14px; background: linear-gradient(135deg, #122f6d, #0d214f); color: #fff; }
.summary small { color: #a8c1e8; font-size: 8px; font-weight: 800; }
.summary h2 { margin-top: 10px; font-size: 15px; }
.summary p { margin-top: 5px; color: #b9c9e1; font-size: 8px; }
.card { margin-top: 11px; padding: 15px; border: 1px solid #e2e7ef; border-radius: 15px; background: #fff; }
.card h3 { font-size: 15px; font-weight: 900; }
.card.highlight { background: #eaf3ff; border-color: #cfe3ff; }
.card.highlight .label { color: #3970ad; font-size: 15px; font-weight: 900; }
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
.pdf:disabled { opacity: 0.6; }
</style>
<style scoped>
.page{background:#f3f6fc}.summary{border-radius:20px;background:linear-gradient(145deg,#2662ea,#173f8d);box-shadow:0 14px 30px rgba(23,63,141,.18)}.card{border-color:#dfe7f4;border-radius:18px;box-shadow:0 7px 20px rgba(23,63,141,.05)}.card.highlight{background:linear-gradient(145deg,#edf3ff,#f8faff);border-color:#c9d9fa}.card.highlight .bar span{background:linear-gradient(90deg,#ffd45e,#ffbe3d)}.final{background:#fff4d7;color:#795300}.pdf{background:#2662ea;box-shadow:0 10px 22px rgba(38,98,234,.2)}
.summary{padding:15px;border-radius:18px;box-shadow:0 9px 22px rgba(23,63,141,.14)}.summary h2{margin-top:8px}.card{margin-top:9px;padding:13px;border-radius:16px;box-shadow:0 5px 15px rgba(23,63,141,.045)}.saving-chart{height:100px;margin-top:11px}.donut-row{gap:14px;margin-top:11px}.donut{width:90px;height:90px}.stat-table{margin-top:9px}.final{margin-top:9px;padding:12px;border-radius:12px}.pdf{margin-top:10px;padding:12px;border-radius:10px}
.report-ticket{background:linear-gradient(145deg,#1f5ab9 0%,#14357f 62%,#102d6d 100%)}.summary-kicker{display:flex;align-items:center;justify-content:space-between}.summary-kicker small{color:#ffd466;font-family:'Space Mono',monospace;letter-spacing:.12em}.summary-kicker b{padding:5px 9px;border:1px solid rgba(255,212,94,.62);border-radius:999px;background:rgba(255,212,94,.13);color:#ffd466;font-size:9px}.summary-title{display:flex;align-items:center;gap:7px;margin-top:13px}.summary-title h2{min-width:0;margin:0;overflow:hidden;font-size:17px;text-overflow:ellipsis;white-space:nowrap}.summary-flags{display:flex;gap:3px}.summary-flags i{width:13px;height:9px;border-radius:2px;background-size:cover;box-shadow:0 1px 3px rgba(0,0,0,.18)}.report-ticket>p{margin-top:7px;color:rgba(255,255,255,.72);font-family:'Space Mono',monospace;font-size:9px;font-weight:700}
</style>
