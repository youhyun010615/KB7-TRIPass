<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelReportStore } from '@/stores/travelReport'
import { exportElementToPdf } from '@/utils/pdf'

const route = useRoute()
const router = useRouter()
const store = useTravelReportStore()
const tripId = computed(() => Number(route.query.tripId || 1))
const r = computed(() => store.postTripView)
const spent = computed(() => r.value?.spent ?? 0)
const remaining = computed(() => r.value?.remaining ?? 0)
const receipts = computed(() => r.value?.receiptCount ?? 0)
const downloading = ref(false)
const reportContent = ref(null)

onMounted(() => store.loadPostTripReport(tripId.value))
watch(tripId, id => store.loadPostTripReport(id))

function money(v) {
  return Number(v).toLocaleString() + '원'
}
async function download() {
  if (downloading.value) return
  downloading.value = true
  try {
    await exportElementToPdf(reportContent.value, `여행후리포트_${r.value.trip.title}.pdf`)
  } catch (error) {
    console.error('PDF 저장 실패:', error)
  } finally {
    downloading.value = false
  }
}

// 가공 지표 (화면에 표기하는 라벨은 "지출 분석 요약", 값은 백엔드에서 계산되어 내려온다)
const dailyAverage = computed(() => r.value?.dailyAverage ?? 0)
const savingsRate = computed(() => r.value?.savingsRate ?? 0)
const topCategory = computed(() => r.value?.categories?.[0])
const bottomCategory = computed(() => { const c = r.value?.categories || []; return c[c.length - 1] })
const categoryRatio = computed(() => (bottomCategory.value?.amount ? Math.round((topCategory.value.amount / bottomCategory.value.amount) * 10) / 10 : 0))
const nextTripMonths = computed(() => r.value?.nextTripMonths ?? 6)
const nextTripMonthly = computed(() => r.value?.nextTripMonthly ?? 0)

const dailyAmounts = computed(() => (r.value?.daily || []).map(d => d.amount))
const maxDaily = computed(() => Math.max(...dailyAmounts.value, 1))
const minDaily = computed(() => Math.min(...dailyAmounts.value, maxDaily.value))

function anchorFor(x) {
  if (x < 40) return 'start'
  if (x > 260) return 'end'
  return 'middle'
}

const chartPoints = computed(() => {
  const daily = r.value?.daily || []
  const n = daily.length
  if (!n) return []
  return daily.map((d, i) => ({
    x: 10 + (i * 280) / Math.max(n - 1, 1),
    y: 88 - (d.amount / maxDaily.value) * 65,
    amount: d.amount,
    date: d.date,
  }))
})
const dailyPoints = computed(() => chartPoints.value.map(p => `${p.x},${p.y}`).join(' '))
const peakDailyIndex = computed(() => dailyAmounts.value.indexOf(maxDaily.value))
const troughDailyIndex = computed(() => dailyAmounts.value.indexOf(minDaily.value))
const peakPoint = computed(() => chartPoints.value[peakDailyIndex.value])
const troughPoint = computed(() =>
  troughDailyIndex.value !== peakDailyIndex.value ? chartPoints.value[troughDailyIndex.value] : null
)

// 카테고리별 지출 도넛 세그먼트
const CIRCUMFERENCE = 251.2
const totalCategory = computed(() => (r.value?.categories || []).reduce((s, c) => s + c.amount, 0) || 1)
const categorySegments = computed(() => {
  let offset = 0
  return (r.value?.categories || []).map(c => {
    const len = (c.amount / totalCategory.value) * CIRCUMFERENCE
    const seg = { color: c.color, len, gap: CIRCUMFERENCE - len, offset: -offset, percent: Math.round((c.amount / totalCategory.value) * 100) }
    offset += len
    return seg
  })
})
</script>

<template>
  <main class="page">
    <header><button type="button" @click="router.back()">‹</button><h1>여행 후 리포트</h1><span /></header>

    <p v-if="!r && store.errorMessage" class="loading error">{{ store.errorMessage }}</p>
    <p v-else-if="!r" class="loading">불러오는 중...</p>

    <template v-else>
    <div ref="reportContent" class="pdf-content">
    <section class="summary">
      <small>TRIP RESULT REPORT</small>
      <div><h2>{{ r.trip.flags }} {{ r.trip.title }}</h2><em>여행 완료</em></div>
      <p>{{ r.trip.dateRange }} · {{ r.trip.days }}일</p>
    </section>

    <section class="card">
      <h3>예산 소비 요약</h3>
      <div class="summary-row">
        <div><small>여행 목표 예산</small><b>{{ money(r.targetBudget) }}</b></div>
        <div><small>지출 금액</small><b>{{ money(spent) }}</b></div>
        <div class="green"><small>남은 잔액</small><b>{{ money(remaining) }}</b></div>
      </div>
    </section>

    <section class="card">
      <h3>지출 분석 요약</h3>
      <div class="insight-grid">
        <div><small>일 평균 지출</small><b>{{ money(dailyAverage) }}</b></div>
        <div><small>예산 절감률</small><b class="green">{{ savingsRate }}%</b></div>
      </div>
    </section>

    <section class="card">
      <h3>일별 지출 추이</h3>
      <svg viewBox="0 0 300 100" class="line-chart">
        <polyline :points="dailyPoints" fill="none" stroke="#176be0" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round" />
        <line x1="10" y1="90" x2="290" y2="90" stroke="#e2e7ef" />
        <template v-if="troughPoint">
          <text :x="troughPoint.x" :y="troughPoint.y - 6" :text-anchor="anchorFor(troughPoint.x)" class="chart-label low">{{ money(troughPoint.amount) }}</text>
          <circle :cx="troughPoint.x" :cy="troughPoint.y" r="3" fill="#18a77d" stroke="#fff" stroke-width="1" />
        </template>
        <template v-if="peakPoint">
          <text :x="peakPoint.x" :y="peakPoint.y - 6" :text-anchor="anchorFor(peakPoint.x)" class="chart-label high">{{ money(peakPoint.amount) }}</text>
          <circle :cx="peakPoint.x" :cy="peakPoint.y" r="3" fill="#e5484d" stroke="#fff" stroke-width="1" />
        </template>
      </svg>
      <div class="daily-labels" v-if="r.daily.length">
        <span v-for="d in r.daily" :key="d.date">{{ d.date }}</span>
      </div>
      <p class="chart-note" v-if="r.daily.length">평균 {{ money(dailyAverage) }}</p>
    </section>

    <section class="card">
      <h3>카테고리별 지출</h3>
      <div class="donut-row">
        <svg viewBox="0 0 100 100" class="donut">
          <g transform="rotate(-90 50 50)">
            <circle v-for="(seg, i) in categorySegments" :key="i" cx="50" cy="50" r="40" fill="none" stroke-width="14"
              :stroke="seg.color" :stroke-dasharray="`${seg.len} ${seg.gap}`" :stroke-dashoffset="seg.offset" />
          </g>
        </svg>
        <ul class="legend">
          <li v-for="(c, i) in r.categories" :key="c.name">
            <i :style="{ background: c.color }" />{{ c.name }}
            <b>{{ categorySegments[i].percent }}%</b>
          </li>
        </ul>
      </div>
      <p class="chart-note" v-if="topCategory && bottomCategory">가장 많은 지출: {{ topCategory.name }} {{ money(topCategory.amount) }} — {{ bottomCategory.name }}의 {{ categoryRatio }}배예요.</p>
    </section>

    <section class="card">
      <h3>국가별 지출 현황</h3>
      <div class="country-list">
        <div v-for="c in r.countrySpend" :key="c.name" class="country-row">
          <span>{{ c.flag }} {{ c.name }}</span>
          <div class="bar-track"><span class="bar-fill" :style="{ width: `${Math.min((c.amount / c.budget) * 100, 100)}%`, background: c.amount > c.budget ? '#e5484d' : c.color }" /></div>
          <small>{{ money(c.amount) }} / {{ money(c.budget) }}</small>
        </div>
      </div>
    </section>

    <section class="card">
      <table class="stat-table">
        <tr><td>등록된 영수증</td><td>{{ receipts }}건</td></tr>
      </table>
    </section>

    <section class="card next-trip">
      <h3>다음 여행 준비 제안</h3>
      <p>이번 여행 지출({{ money(spent) }}) 기준, 다음 여행은 월 {{ money(nextTripMonthly) }}씩 {{ nextTripMonths }}개월 저축을 추천해요.</p>
    </section>
    </div>

    <button class="pdf" type="button" @click="download" :disabled="downloading">{{ downloading ? 'PDF 생성 중...' : '▣ PDF 저장하기' }}</button>
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
.summary > div { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
.summary h2 { font-size: 15px; }
.summary em { padding: 5px 8px; border: 1px solid #ffffff50; border-radius: 10px; font-size: 7px; font-style: normal; }
.summary p { margin-top: 5px; color: #b9c9e1; font-size: 8px; }
.card { margin-top: 11px; padding: 15px; border: 1px solid #e2e7ef; border-radius: 15px; background: #fff; }
.card h3 { font-size: 12px; font-weight: 900; }
.summary-row { display: grid; grid-template-columns: 1fr 1fr 1fr; align-items: center; gap: 6px; margin-top: 12px; padding: 10px; border-radius: 10px; background: #f4f7fb; }
.summary-row > div { text-align: center; }
.summary-row small { display: block; color: #8290a3; font-size: 8px; }
.summary-row b { display: block; margin-top: 4px; font-size: 11px; font-weight: 900; }
.summary-row .green { border-radius: 8px; background: #e8f8f3; padding: 4px 0; }
.summary-row .green b { color: #18a77d; }
.insight-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 12px; }
.insight-grid div { padding: 11px; border-radius: 10px; background: #f4f7fb; text-align: center; }
.insight-grid small { display: block; color: #8290a3; font-size: 8px; }
.insight-grid b { display: block; margin-top: 6px; font-size: 13px; font-weight: 900; color: #19499a; }
.insight-grid b.green { color: #18a77d; }
.line-chart { width: 100%; height: auto; margin-top: 10px; overflow: visible; }
.chart-label { font-size: 7px; font-weight: 800; }
.chart-label.high { fill: #e5484d; }
.chart-label.low { fill: #18a77d; }
.daily-labels { display: flex; justify-content: space-between; padding: 0 2px; }
.daily-labels span { flex: 1; overflow: visible; color: #8290a3; font-size: 6px; text-align: center; white-space: nowrap; transform: rotate(-40deg); transform-origin: center; }
.chart-note { margin-top: 8px; color: #8290a3; font-size: 8px; }
.chart-note + .chart-note { margin-top: 3px; }
.donut-row { display: flex; align-items: center; gap: 18px; margin-top: 12px; }
.donut { width: 90px; height: 90px; flex-shrink: 0; }
.legend { display: flex; flex-direction: column; gap: 7px; flex: 1; }
.legend li { display: flex; align-items: center; gap: 6px; font-size: 10px; }
.legend li i { width: 9px; height: 9px; border-radius: 3px; flex-shrink: 0; }
.legend li b { margin-left: auto; color: #8290a3; font-size: 10px; }
.country-list { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; }
.country-row span { font-size: 10px; }
.bar-track { height: 6px; margin-top: 5px; border-radius: 4px; background: #edf1f6; overflow: hidden; }
.bar-fill { display: block; height: 100%; border-radius: 4px; }
.country-row small { display: block; margin-top: 4px; color: #8290a3; font-size: 8px; text-align: right; }
.stat-table { width: 100%; font-size: 10px; }
.stat-table td { padding: 2px 0; color: #55708f; }
.stat-table td:last-child { text-align: right; font-weight: 800; color: #111a2d; }
.next-trip p { margin-top: 10px; color: #55708f; font-size: 9px; line-height: 1.6; }
.pdf { width: 100%; margin-top: 12px; padding: 14px; border-radius: 11px; background: #174695; color: #fff; font-size: 11px; font-weight: 900; }
.pdf:disabled { opacity: 0.6; }
</style>
