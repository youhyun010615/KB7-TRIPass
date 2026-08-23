<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TravelArchiveSummaryCard from '@/components/mypage/TravelArchiveSummaryCard.vue'
import { useTravelReportStore } from '@/stores/travelReport'
import { exportElementToPdf } from '@/utils/pdf'

const route = useRoute()
const router = useRouter()
const store = useTravelReportStore()
const tripId = computed(() => {
  const parsed = Number(route.query.tripId)
  return route.query.tripId && Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const r = computed(() => store.postTripView)
const downloading = ref(false)
const reportContent = ref(null)

function loadReport(id) { if (id) store.loadPostTripReport(id) }
onMounted(() => loadReport(tripId.value))
watch(tripId, loadReport)

function money(value) { return `${Math.round(Number(value || 0)).toLocaleString('ko-KR')}원` }
function percent(value, total) { return total > 0 ? Math.round((Number(value) / Number(total)) * 1000) / 10 : 0 }

const budgetUsage = computed(() => percent(r.value?.spent, r.value?.targetBudget))
const topCategory = computed(() => r.value?.categories?.[0] || null)
const categoryTotal = computed(() => (r.value?.categories || []).reduce((sum, item) => sum + Number(item.amount), 0))
const categoryRows = computed(() => (r.value?.categories || []).map((item, index) => ({ ...item, rank: index + 1, percent: percent(item.amount, categoryTotal.value) })))
const daily = computed(() => r.value?.daily || [])
const dailyMax = computed(() => Math.max(...daily.value.map(item => Number(item.amount)), 1))
const peak = computed(() => daily.value.reduce((best, item) => !best || item.amount > best.amount ? item : best, null))
const low = computed(() => daily.value.reduce((best, item) => !best || item.amount < best.amount ? item : best, null))
const zeroDays = computed(() => daily.value.filter(item => Number(item.amount) === 0).length)
const chartPoints = computed(() => daily.value.map((item, index) => ({ x: 10 + (index * 280) / Math.max(daily.value.length - 1, 1), y: 86 - (Number(item.amount) / dailyMax.value) * 62, ...item })))
const linePoints = computed(() => chartPoints.value.map(point => `${point.x},${point.y}`).join(' '))
const peakPoint = computed(() => chartPoints.value.find(point => point.date === peak.value?.date))
const lowPoint = computed(() => chartPoints.value.find(point => point.date === low.value?.date))
const averageY = computed(() => 86 - (Number(r.value?.dailyAverage || 0) / dailyMax.value) * 62)
const highestCountryUsage = computed(() => (r.value?.countrySpend || []).reduce((best, item) => {
  const usage = percent(item.amount, item.budget)
  return !best || usage > best.usage ? { ...item, usage } : best
}, null))

async function download() {
  if (downloading.value) return
  downloading.value = true
  try { await exportElementToPdf(reportContent.value, `여행후리포트_${r.value.trip.title}.pdf`) }
  catch (error) { console.error('PDF 저장 실패:', error) }
  finally { downloading.value = false }
}
</script>

<template>
  <main class="page">
    <header class="page-header"><button type="button" aria-label="뒤로 가기" @click="router.back()">‹</button><h1>여행 후 리포트</h1><span /></header>
    <p v-if="!tripId" class="loading error">여행 정보를 찾을 수 없어요.</p>
    <p v-else-if="!r && store.errorMessage" class="loading error">{{ store.errorMessage }}</p>
    <p v-else-if="!r" class="loading">불러오는 중...</p>

    <template v-else>
      <div ref="reportContent" class="pdf-content">
        <TravelArchiveSummaryCard :trip-id="tripId" />

        <section class="card overview">
          <h2>이번 여행 한눈에 보기</h2>
          <div class="overview-head"><div><strong>{{ money(r.spent) }}</strong><span>지출</span></div><em>{{ r.remaining >= 0 ? '예산 안에서 여행을 마쳤어요' : '예산을 초과했어요' }}</em></div>
          <div class="usage-label"><span>예산 사용률</span><b>{{ budgetUsage }}%</b></div>
          <div class="bar-track"><span class="bar-fill blue" :style="{ width: `${Math.min(budgetUsage, 100)}%` }" /></div>
          <div class="metric-grid"><div><small>목표 예산</small><b>{{ money(r.targetBudget) }}</b></div><div><small>남은 예산</small><b class="green">{{ money(r.remaining) }}</b></div><div><small>일 평균</small><b>{{ money(r.dailyAverage) }}</b></div></div>
        </section>

        <section class="card">
          <h2>국가별 예산 결과</h2>
          <div class="country-list">
            <div v-for="country in r.countrySpend" :key="country.name" class="country-row">
              <div class="country-head"><b>{{ country.flag }} {{ country.name }}</b><strong :class="{ danger: country.amount > country.budget }">{{ percent(country.amount, country.budget) }}%</strong></div>
              <div class="bar-track"><span class="bar-fill" :style="{ width: `${Math.min(percent(country.amount, country.budget), 100)}%`, background: country.amount > country.budget ? '#e5484d' : country.color }" /></div>
              <div class="country-money"><span>{{ money(country.amount) }} / {{ money(country.budget) }}</span><em>잔액 {{ money(country.budget - country.amount) }}</em></div>
            </div>
          </div>
        </section>

        <section class="card">
          <h2>어디에 많이 썼나요?</h2>
          <div class="category-list">
            <div v-for="item in categoryRows" :key="item.name" class="category-row"><i :style="{ background: item.color }">{{ item.rank }}</i><b>{{ item.name }}</b><div class="bar-track"><span class="bar-fill" :style="{ width: `${item.percent}%`, background: item.color }" /></div><strong>{{ money(item.amount) }} · {{ item.percent }}%</strong></div>
          </div>
          <p v-if="topCategory" class="insight">{{ topCategory.name }} 지출이 {{ money(topCategory.amount) }}으로 가장 많았어요.</p>
        </section>

        <section v-if="r.countryTopCategories.length" class="card">
          <h2>나라별 지출 특징</h2>
          <div class="feature-grid"><article v-for="item in r.countryTopCategories" :key="item.name"><b>{{ item.flag }} {{ item.name }}</b><small>가장 많이 쓴 항목</small><strong :style="{ color: item.color }">{{ item.category }}</strong><span>{{ item.percent }}% · {{ money(item.amount) }}</span></article></div>
        </section>

        <section class="card">
          <h2>일별 지출 추이</h2>
          <svg v-if="daily.length" viewBox="0 0 300 108" class="line-chart">
            <line x1="10" :y1="averageY" x2="290" :y2="averageY" class="average-line" /><text x="12" :y="averageY - 4" class="average-label">일 평균 {{ money(r.dailyAverage) }}</text>
            <polyline :points="linePoints" fill="none" stroke="#2f6fed" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round" />
            <template v-if="peakPoint"><circle :cx="peakPoint.x" :cy="peakPoint.y" r="3.5" fill="#e5484d" stroke="#fff" stroke-width="1.5" /><text :x="peakPoint.x" :y="peakPoint.y - 7" text-anchor="middle" class="peak-label">최고 {{ money(peakPoint.amount) }}</text></template>
            <template v-if="lowPoint && lowPoint.date !== peakPoint?.date"><circle :cx="lowPoint.x" :cy="lowPoint.y" r="3.5" fill="#18a77d" stroke="#fff" stroke-width="1.5" /></template><line x1="10" y1="88" x2="290" y2="88" stroke="#dfe7f4" />
          </svg>
          <div class="daily-labels"><span v-for="item in daily" :key="item.date">{{ item.date }}</span></div>
          <div class="trend-facts"><span>지출 없는 날 <b>{{ zeroDays }}일</b></span><span>최고 지출일 <b>{{ peak?.date }}</b></span></div>
        </section>

        <section class="card next-trip">
          <h2>다음 여행 준비 제안</h2>
          <p v-if="highestCountryUsage"><i>↗</i><span><b>{{ highestCountryUsage.name }}</b>에서 예산의 {{ highestCountryUsage.usage }}%를 사용했어요. 다음 여행에서는 이 국가의 여유 예산을 더 확보해 보세요.</span></p>
          <p v-if="topCategory"><i>★</i><span>가장 큰 <b>{{ topCategory.name }}</b> 예산 {{ money(topCategory.amount) }}을 다음 여행 계획에서 먼저 확보해 보세요.</span></p>
        </section>
      </div>
      <button class="pdf" type="button" :disabled="downloading" @click="download">{{ downloading ? 'PDF 생성 중...' : 'PDF로 저장하기' }}</button>
    </template>
  </main>
</template>

<style scoped>
.page{min-height:100vh;padding:0 16px 32px;background:#f3f6fc;color:#111a2d}.page-header{display:grid;height:64px;grid-template-columns:36px 1fr 36px;align-items:end;padding-bottom:14px}.page-header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:25px;font-weight:800;box-shadow:0 5px 16px rgba(36,72,117,.07)}.page-header h1{text-align:center;font-size:16px;font-weight:900}.loading{padding:40px 0;color:#8290a3;font-size:12px;text-align:center}.loading.error{color:#e5484d}.pdf-content{display:flex;flex-direction:column;gap:10px}.card{padding:15px;border:1px solid #dfe7f4;border-radius:17px;background:#fff;box-shadow:0 6px 18px rgba(23,63,141,.05)}.card h2{font-size:13px;font-weight:950}.overview-head{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-top:12px}.overview-head strong{color:#2f6fed;font-size:22px;font-weight:950;letter-spacing:-.04em}.overview-head span{margin-left:4px;color:#2f6fed;font-size:12px;font-weight:900}.overview-head em{max-width:120px;padding:8px 10px;border-radius:12px;background:#e8f8f3;color:#087f61;font-size:9px;font-style:normal;font-weight:800;line-height:1.4}.usage-label{display:flex;align-items:center;gap:8px;margin-top:11px;font-size:9px}.usage-label b{font-size:11px}.bar-track{height:7px;border-radius:99px;background:#e9eef6;overflow:hidden}.usage-label+.bar-track{margin-top:6px}.bar-fill{display:block;height:100%;border-radius:99px}.bar-fill.blue{background:#2f6fed}.metric-grid{display:grid;grid-template-columns:repeat(3,1fr);margin-top:13px;padding:11px 4px;border-radius:12px;background:#f5f8fd}.metric-grid>div{text-align:center}.metric-grid>div+div{border-left:1px solid #dfe7f4}.metric-grid small{display:block;color:#8290a3;font-size:8px}.metric-grid b{display:block;margin-top:5px;font-size:10px}.green{color:#18a77d}.country-list{display:flex;flex-direction:column;gap:14px;margin-top:13px}.country-head,.country-money{display:flex;align-items:center;justify-content:space-between}.country-head b{font-size:10px}.country-head strong{color:#2f6fed;font-size:10px}.country-head strong.danger{color:#e5484d}.country-row .bar-track{margin-top:6px}.country-money{margin-top:5px;color:#71819a;font-size:8px}.country-money em{color:#18a77d;font-style:normal;font-weight:800}.category-list{display:flex;flex-direction:column;gap:9px;margin-top:13px}.category-row{display:grid;grid-template-columns:18px 45px minmax(50px,1fr) 105px;align-items:center;gap:7px}.category-row i{display:grid;width:17px;height:17px;place-items:center;border-radius:50%;color:#fff;font-size:8px;font-style:normal;font-weight:900}.category-row b{font-size:9px}.category-row .bar-track{height:6px}.category-row strong{font-size:8px;text-align:right}.insight{margin-top:12px;padding:8px 10px;border-radius:10px;background:#eef5ff;color:#2f6fed;font-size:9px;font-weight:800}.feature-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:7px;margin-top:12px}.feature-grid article{display:flex;min-width:0;flex-direction:column;align-items:center;padding:10px 5px;border:1px solid #e4eaf3;border-radius:12px;text-align:center}.feature-grid article b{font-size:9px}.feature-grid article small{margin-top:8px;color:#8290a3;font-size:7px}.feature-grid article strong{margin-top:3px;font-size:12px}.feature-grid article span{margin-top:4px;font-size:7px;font-weight:800}.line-chart{width:100%;margin-top:10px;overflow:visible}.average-line{stroke:#79a6f5;stroke-width:1;stroke-dasharray:3 3}.average-label{fill:#2f6fed;font-size:6px;font-weight:700}.peak-label{fill:#e5484d;font-size:6px;font-weight:800}.daily-labels{display:flex;justify-content:space-between;margin-top:-6px}.daily-labels span{flex:1;color:#8290a3;font-size:5.5px;text-align:center;transform:rotate(-35deg)}.trend-facts{display:grid;grid-template-columns:1fr 1fr;margin-top:12px;padding:10px;border-radius:11px;background:#f5f8fd;color:#71819a;font-size:8px;text-align:center}.trend-facts span+span{border-left:1px solid #dfe7f4}.trend-facts b{margin-left:4px;color:#173f8d;font-size:9px}.next-trip{display:flex;flex-direction:column;gap:8px}.next-trip p{display:flex;align-items:flex-start;gap:9px;padding:10px;border:1px solid #e4eaf3;border-radius:12px;color:#55708f;font-size:9px;line-height:1.5}.next-trip i{display:grid;flex:0 0 25px;width:25px;height:25px;place-items:center;border-radius:8px;background:#eaf1ff;color:#2f6fed;font-style:normal;font-weight:900}.next-trip b{color:#173f8d}.pdf{width:100%;margin-top:12px;padding:14px;border-radius:12px;background:#2f6fed;color:#fff;font-size:12px;font-weight:900;box-shadow:0 10px 22px rgba(47,111,237,.2)}.pdf:disabled{opacity:.6}
</style>
