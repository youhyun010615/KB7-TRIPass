<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTravelReportStore } from '@/stores/travelReport'
import { exportElementToPdf } from '@/utils/pdf'
import { flagIconClass } from '@/stores/travel'
import { TrendingUp, Star, Trophy, Plane, PiggyBank, ClipboardCheck, Flame } from '@lucide/vue'

const route = useRoute()
const router = useRouter()
const store = useTravelReportStore()
const tripId = computed(() => {
  const parsed = Number(route.query.tripId)
  return route.query.tripId && Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const r = computed(() => store.preTripView)
const savingPercent = computed(() => Math.min(100, r.value?.savingsPercent ?? 0))
const goalFilledAmount = computed(() => Math.min(r.value?.securedFund ?? 0, r.value?.targetBudget ?? 0))
const downloading = ref(false)
const reportContent = ref(null)

onMounted(() => {
  if (tripId.value) store.loadPreTripReport(tripId.value)
})
watch(tripId, id => {
  if (id) store.loadPreTripReport(id)
})

function money(v) {
  return Number(v || 0).toLocaleString() + '원'
}
function foreignMoney(v, currencyCode) {
  return Number(v || 0).toLocaleString(undefined, { maximumFractionDigits: 2 }) + ' ' + currencyCode
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

function goChecklist(stage) {
  router.push(`/mypage/checklists/preparation?tripId=${tripId.value}&stage=${stage.toLowerCase()}`)
}

// 분석/인사이트 아이콘 매핑
const insightIconMap = { trend: TrendingUp, star: Star, trophy: Trophy, streak: Flame }
function insightIcon(icon) {
  return insightIconMap[icon] || TrendingUp
}
const mainInsights = computed(() => (r.value?.insights || []).filter(i => i.icon !== 'journey'))
const summaryInsight = computed(() => (r.value?.insights || []).find(i => i.icon === 'journey'))

// 월렛 저축 내역 — 저축 시작 달부터 여행 전달까지 월별로 얼마씩 저축했는지 SVG 좌표 계산
const CHART_WIDTH = 300
const CHART_HEIGHT = 84
const trendChart = computed(() => {
  const trend = r.value?.savingsTrend || []
  if (!trend.length) return null
  const max = Math.max(...trend.map(m => m.savedAmount), 1)
  const stepX = trend.length > 1 ? CHART_WIDTH / (trend.length - 1) : 0
  const coords = trend.map((m, i) => ({
    x: trend.length > 1 ? i * stepX : CHART_WIDTH / 2,
    y: CHART_HEIGHT - Math.max(0, (m.savedAmount / max) * CHART_HEIGHT),
    ...m,
  }))
  const points = coords.map(c => `${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(' ')
  const areaPath = `M0,${CHART_HEIGHT} ${coords.map(c => `L${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(' ')} L${CHART_WIDTH},${CHART_HEIGHT} Z`

  // 월별 저축액 기준 최고/최저 달 표시
  let bestIdx = 0
  let worstIdx = 0
  coords.forEach((c, i) => {
    if (c.savedAmount > coords[bestIdx].savedAmount) bestIdx = i
    if (c.savedAmount < coords[worstIdx].savedAmount) worstIdx = i
  })
  const best = coords[bestIdx]
  const worst = coords.length > 1 && worstIdx !== bestIdx ? coords[worstIdx] : null

  return { coords, points, areaPath, max, best, worst }
})

// 국가별 목표 예산 도넛 세그먼트 계산 (목표 예산 비중 기준)
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
      <Plane :size="34" class="summary-illustration" />
    </section>

    <section class="card highlight anim-in">
      <p class="label"><PiggyBank :size="14" />여행 저축 목표</p>
      <p class="big">{{ money(goalFilledAmount) }} <small>/ {{ money(r.targetBudget) }}</small></p>
      <div class="bar"><span :style="{ width: `${Math.min(100, savingPercent)}%` }" /></div>
      <p class="sub">달성률 {{ savingPercent }}%</p>
      <div v-if="r.emergencyFund > 0" class="emergency-block">
        <p class="emergency-amount"><Trophy :size="14" />비상금 <b>{{ money(r.emergencyFund) }}</b></p>
        <p class="emergency-note">목표보다 더 모은 금액은 비상금으로 자유롭게 쓸 수 있어요.</p>
      </div>
    </section>

    <section class="card anim-in delay-1">
      <h3>국가별 목표 예산</h3>
      <p class="chart-sub">나라별 목표 예산을 각 나라 통화로 환산하면 이만큼이에요.</p>
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
            <div class="legend-head">
              <i :style="{ background: c.color }" />{{ c.flag }} {{ c.name }} <em>{{ c.currencyCode }}</em>
              <b>{{ budgetSegments[i]?.percent ?? 0 }}%</b>
            </div>
            <p class="legend-target">{{ money(c.budget) }} <template v-if="c.foreignAmount != null">= {{ foreignMoney(c.foreignAmount, c.currencyCode) }}</template></p>
          </li>
        </ul>
      </div>
    </section>

    <section class="card anim-in delay-2">
      <h3>월렛 저축 내역</h3>
      <p class="chart-sub">저축 시작부터 여행 전달까지 매달 얼마씩 저축했는지 보여드려요.</p>
      <div v-if="trendChart" class="trend-chart-wrap">
        <svg viewBox="0 0 300 84" class="trend-chart" preserveAspectRatio="none">
          <path :d="trendChart.areaPath" class="trend-area" />
          <polyline :points="trendChart.points" class="trend-line" />
          <circle v-if="trendChart.worst" :cx="trendChart.worst.x" :cy="trendChart.worst.y" r="3" class="trend-marker worst" />
          <circle :cx="trendChart.best.x" :cy="trendChart.best.y" r="3.5" class="trend-marker best" />
        </svg>
        <div class="trend-labels">
          <span v-for="m in r.savingsTrend" :key="m.month">{{ m.monthLabel }}</span>
        </div>
        <div class="trend-minmax">
          <span v-if="trendChart.worst" class="worst"><i />최저 {{ trendChart.worst.monthLabel }} {{ money(trendChart.worst.savedAmount) }}</span>
          <span class="best"><i />최고 {{ trendChart.best.monthLabel }} {{ money(trendChart.best.savedAmount) }}</span>
        </div>
      </div>
      <p v-else class="chart-empty">아직 저축 내역이 없어요.</p>
    </section>

    <section class="card anim-in delay-3">
      <h3>분석 및 인사이트</h3>
      <ul v-if="mainInsights.length" class="insight-list">
        <li v-for="(insight, i) in mainInsights" :key="i">
          <span class="insight-icon"><component :is="insightIcon(insight.icon)" :size="16" /></span>
          <div>
            <b>{{ insight.title }}</b>
            <p>{{ insight.message }}</p>
          </div>
        </li>
      </ul>
      <p v-else-if="!summaryInsight" class="chart-empty">아직 분석할 데이터가 부족해요.</p>
      <div v-if="summaryInsight" class="insight-summary">
        <b>{{ summaryInsight.title }}</b>
        <p>{{ summaryInsight.message }}</p>
      </div>
    </section>

    <section class="card anim-in delay-4">
      <div class="prep-head">
        <h3><ClipboardCheck :size="15" />여행 준비 체크리스트</h3>
      </div>
      <div class="stage-grid">
        <div v-for="stage in r.checklistStages" :key="stage.stage" class="stage-result">
          <div class="stage-result-top">
            <button type="button" class="stage-label-btn" @click="goChecklist(stage.stage)">{{ stage.stageLabel }} ›</button>
            <b class="stage-percent">{{ stage.percent }}%</b>
            <span>{{ stage.completed }}/{{ stage.total }}</span>
          </div>
          <div class="stage-bar"><i :style="{ width: stage.percent + '%' }" /></div>
          <p class="stage-message">{{ stage.message }}</p>
        </div>
      </div>
    </section>

    </div>

    <button class="pdf" type="button" @click="download" :disabled="downloading">{{ downloading ? 'PDF 생성 중...' : 'PDF 저장하기' }}</button>
    </template>
  </main>
</template>

<style scoped>
:root { }
.page {
  --fs-caption: 10.5px;
  --fs-body: 12.5px;
  --fs-h3: 15px;
  --fs-h2: 17px;
  --fs-hero: 22px;
  --w-regular: 500;
  --w-bold: 700;
  --w-black: 900;
  min-height: 100vh;
  padding: 0 16px 32px;
  background: #f3f6fc;
  color: #111a2d;
}
.page > header { display: grid; height: 63px; grid-template-columns: 35px 1fr 35px; align-items: end; padding-bottom: 16px; }
.page > header button { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 12px; background: #fff; color: #193d82; font-size: 24px; font-weight: var(--w-bold); box-shadow: 0 5px 16px rgba(36, 72, 117, 0.07); }
.page > header h1 { text-align: center; font-size: var(--fs-h3); font-weight: var(--w-black); }
.loading { padding: 40px 0; color: #8290a3; font-size: var(--fs-body); text-align: center; }
.loading.error { color: #e5484d; }

.summary {
  position: relative;
  overflow: hidden;
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(145deg, #2662ea, #173f8d);
  box-shadow: 0 14px 30px rgba(23, 63, 141, .18);
  color: #fff;
}
.summary-kicker { display: flex; align-items: center; justify-content: space-between; }
.summary-kicker small { color: #ffd466; font-family: 'Space Mono', monospace; font-size: var(--fs-caption); font-weight: var(--w-bold); letter-spacing: .12em; }
.summary-kicker b { padding: 5px 9px; border: 1px solid rgba(255, 212, 94, .62); border-radius: 999px; background: rgba(255, 212, 94, .13); color: #ffd466; font-size: var(--fs-caption); }
.summary-title { display: flex; align-items: center; gap: 7px; margin-top: 12px; }
.summary-title h2 { min-width: 0; margin: 0; overflow: hidden; font-size: var(--fs-h2); font-weight: var(--w-black); text-overflow: ellipsis; white-space: nowrap; }
.summary-flags { display: flex; gap: 3px; }
.summary-flags i { width: 13px; height: 9px; border-radius: 2px; background-size: cover; box-shadow: 0 1px 3px rgba(0, 0, 0, .18); }
.summary > p { margin-top: 6px; color: rgba(255, 255, 255, .72); font-family: 'Space Mono', monospace; font-size: var(--fs-caption); font-weight: var(--w-bold); }
.summary-illustration { position: absolute; right: -6px; bottom: -8px; color: rgba(255, 255, 255, .16); transform: rotate(24deg); }

.card { margin-top: 11px; padding: 15px; border: 1px solid #dfe7f4; border-radius: 18px; background: #fff; box-shadow: 0 7px 20px rgba(23, 63, 141, .05); }
.card h3 { display: flex; align-items: center; gap: 5px; font-size: var(--fs-h3); font-weight: var(--w-black); }
.card.highlight { background: linear-gradient(145deg, #edf3ff, #f8faff); border-color: #c9d9fa; }
.card.highlight .label { display: flex; align-items: center; gap: 5px; color: #3970ad; font-size: var(--fs-h3); font-weight: var(--w-black); }
.card.highlight .big { margin-top: 8px; color: #123a82; font-size: var(--fs-hero); font-weight: var(--w-black); }
.card.highlight .big small { color: #5f7fae; font-size: var(--fs-body); font-weight: var(--w-bold); }
.card.highlight .bar { height: 7px; margin-top: 11px; border-radius: 4px; background: #d7e6fb; overflow: hidden; }
.card.highlight .bar span { display: block; height: 100%; border-radius: 4px; background: linear-gradient(90deg, #ffd45e, #ffbe3d); animation: bar-fill .8s ease-out; }
.card.highlight .sub { margin-top: 7px; color: #3970ad; font-size: var(--fs-caption); font-weight: var(--w-bold); }
.emergency-block { margin-top: 12px; padding: 10px 12px; border-radius: 13px; background: #fff4d7; }
.emergency-amount { display: flex; align-items: center; gap: 5px; color: #795300; font-size: var(--fs-body); font-weight: var(--w-bold); }
.emergency-amount b { margin-left: 2px; font-size: var(--fs-h3); font-weight: var(--w-black); }
.emergency-note { margin-top: 4px; color: #9a7a2e; font-size: var(--fs-caption); line-height: 1.4; }

.chart-sub { margin-top: 4px; color: #8290a3; font-size: var(--fs-caption); }
.chart-empty { margin-top: 12px; color: #97a6bc; font-size: var(--fs-caption); text-align: center; }

.donut-row { display: flex; align-items: center; gap: 16px; margin-top: 14px; }
.donut { width: 96px; height: 96px; flex-shrink: 0; }
.donut-total { font-size: 15px; font-weight: var(--w-black); fill: #123a82; }
.donut-unit { font-size: 8px; fill: #8290a3; }
.legend { display: flex; flex-direction: column; gap: 12px; flex: 1; min-width: 0; }
.legend-head { display: flex; align-items: center; gap: 5px; font-size: var(--fs-body); font-weight: var(--w-bold); }
.legend-head i { width: 8px; height: 8px; border-radius: 3px; flex-shrink: 0; }
.legend-head em { margin-left: 1px; color: #97a6bc; font-size: var(--fs-caption); font-style: normal; }
.legend-head b { margin-left: auto; color: #176be0; font-size: var(--fs-body); font-weight: var(--w-black); }
.legend-target { margin-top: 4px; color: #55708f; font-size: var(--fs-caption); font-weight: var(--w-bold); }

.trend-chart-wrap { margin-top: 12px; }
.trend-chart { width: 100%; height: 84px; overflow: visible; }
.trend-area { fill: rgba(23, 107, 224, .12); stroke: none; }
.trend-line { fill: none; stroke: #176be0; stroke-width: 2; stroke-linejoin: round; stroke-linecap: round; }
.trend-marker.best { fill: #18a77d; stroke: #fff; stroke-width: 1.2; }
.trend-marker.worst { fill: #e5484d; stroke: #fff; stroke-width: 1.2; }
.trend-labels { display: flex; justify-content: space-between; margin-top: 6px; color: #97a6bc; font-size: 8.5px; }
.trend-minmax { display: flex; gap: 14px; margin-top: 10px; font-size: var(--fs-caption); font-weight: var(--w-bold); }
.trend-minmax span { display: flex; align-items: center; gap: 4px; }
.trend-minmax i { width: 6px; height: 6px; border-radius: 50%; }
.trend-minmax .best { margin-left: auto; color: #18a77d; }
.trend-minmax .best i { background: #18a77d; }
.trend-minmax .worst { color: #e5484d; }
.trend-minmax .worst i { background: #e5484d; }

.insight-list { display: flex; flex-direction: column; gap: 11px; margin-top: 12px; }
.insight-list li { display: flex; align-items: flex-start; gap: 9px; }
.insight-icon { display: grid; flex-shrink: 0; width: 28px; height: 28px; place-items: center; border-radius: 10px; background: #eaf1ff; color: #176be0; line-height: 0; }
.insight-list b { font-size: var(--fs-body); font-weight: var(--w-bold); }
.insight-list p { margin-top: 2px; color: #66748d; font-size: var(--fs-caption); line-height: 1.5; }

.insight-summary { margin-top: 14px; padding: 12px 13px; border: 1px solid #c9d9fa; border-radius: 14px; background: linear-gradient(145deg, #edf3ff, #f8faff); }
.insight-summary b { color: #123a82; font-size: var(--fs-body); font-weight: var(--w-black); }
.insight-summary p { margin-top: 2px; color: #3970ad; font-size: var(--fs-caption); font-weight: var(--w-bold); line-height: 1.5; }

.prep-head { display: flex; align-items: center; justify-content: space-between; }
.stage-grid { display: flex; flex-direction: column; gap: 7px; margin-top: 10px; }
.stage-result { padding: 9px 11px; border-radius: 12px; background: #f7f9fd; }
.stage-result-top { display: flex; align-items: center; gap: 6px; font-size: var(--fs-caption); font-weight: var(--w-bold); color: #111a2d; }
.stage-label-btn { color: #176be0; font-size: var(--fs-caption); font-weight: var(--w-black); }
.stage-percent { color: #176be0; font-size: var(--fs-caption); font-weight: var(--w-black); }
.stage-result-top span { margin-left: auto; color: #8290a3; font-size: 10px; font-weight: var(--w-regular); }
.stage-bar { height: 4px; margin-top: 5px; border-radius: 2px; background: #e4e9f3; overflow: hidden; }
.stage-bar i { display: block; height: 100%; border-radius: 2px; background: #176be0; animation: bar-fill .8s ease-out; }
.stage-message { margin-top: 5px; color: #55708f; font-size: 10px; font-weight: var(--w-regular); line-height: 1.4; }

.pdf { width: 100%; margin-top: 14px; padding: 14px; border-radius: 13px; background: #2662ea; color: #fff; font-size: var(--fs-body); font-weight: var(--w-black); box-shadow: 0 10px 22px rgba(38, 98, 234, .2); }
.pdf:disabled { opacity: .6; }

.anim-in { animation: card-in .45s ease-out backwards; }
.anim-in.delay-1 { animation-delay: .05s; }
.anim-in.delay-2 { animation-delay: .1s; }
.anim-in.delay-3 { animation-delay: .15s; }
.anim-in.delay-4 { animation-delay: .2s; }

@keyframes card-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
@keyframes bar-fill { from { width: 0; } }

@media (prefers-reduced-motion: reduce) {
  .anim-in, .card.highlight .bar span, .country-bar i, .stage-bar i { animation: none; }
}
</style>
