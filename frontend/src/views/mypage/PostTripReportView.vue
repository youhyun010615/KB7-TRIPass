<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChartNoAxesColumnIncreasing, Wallet } from '@lucide/vue'
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
function closeReport() { router.back() }
function handleKeydown(event) { if (event.key === 'Escape') closeReport() }
onMounted(() => {
  loadReport(tripId.value)
  window.addEventListener('keydown', handleKeydown)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown))
watch(tripId, loadReport)

function money(value) { return `${Math.round(Number(value || 0)).toLocaleString('ko-KR')}원` }
function percent(value, total) { return total > 0 ? Math.round((Number(value) / Number(total)) * 1000) / 10 : 0 }

const budgetUsage = computed(() => percent(r.value?.spent, r.value?.targetBudget))
const categoryTotal = computed(() => (r.value?.categories || []).reduce((sum, item) => sum + Number(item.amount), 0))
const categoryRows = computed(() => [...(r.value?.categories || [])]
  .sort((a, b) => Number(b.amount) - Number(a.amount))
  .map((item, index) => ({ ...item, rank: index + 1, percent: percent(item.amount, categoryTotal.value) })))
const topCategory = computed(() => categoryRows.value[0] || null)
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
const dailyCountrySlots = computed(() => daily.value.map((day, index, days) => {
  const period = (r.value?.countryPeriods || []).find(country => day.rawDate >= country.startDate && day.rawDate <= country.endDate)
  const previous = index > 0
    ? (r.value?.countryPeriods || []).find(country => days[index - 1].rawDate >= country.startDate && days[index - 1].rawDate <= country.endDate)
    : null
  const next = index < days.length - 1
    ? (r.value?.countryPeriods || []).find(country => days[index + 1].rawDate >= country.startDate && days[index + 1].rawDate <= country.endDate)
    : null
  return {
    ...period,
    start: Boolean(period && period.name !== previous?.name),
    end: Boolean(period && period.name !== next?.name),
  }
}))
const nextBudgetPlans = computed(() => (r.value?.countrySpend || [])
  .filter(country => Number(country.amount) > 0)
  .map(country => {
    const spent = Number(country.amount)
    const currentBudget = Number(country.budget)
    const suggestedBudget = Math.ceil((spent * 1.1) / 10000) * 10000
    const difference = suggestedBudget - currentBudget
    const top = (r.value?.countryTopCategories || []).find(item => item.name === country.name)
    return {
      ...country,
      spent,
      currentBudget,
      suggestedBudget,
      difference,
      usage: percent(spent, currentBudget),
      action: difference < 0 ? 'reduce' : difference > 0 ? 'increase' : 'keep',
      topCategory: top?.category,
    }
  }))
const nextBudgetTotal = computed(() => nextBudgetPlans.value.reduce((sum, country) => sum + country.suggestedBudget, 0))
const nextBudgetDifference = computed(() => nextBudgetTotal.value - Number(r.value?.targetBudget || 0))

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
    <button class="modal-backdrop" type="button" aria-label="리포트 닫기" @click="closeReport" />
    <section class="report-modal" role="dialog" aria-modal="true" aria-labelledby="post-trip-report-title">
    <header class="page-header"><span class="document-mark" aria-hidden="true"><i></i><i></i><i></i></span><div><small>TRIPASS REPORT</small><h1 id="post-trip-report-title">여행 후 리포트</h1></div><button type="button" aria-label="닫기" @click="closeReport">×</button></header>
    <p v-if="!tripId" class="loading error">여행 정보를 찾을 수 없어요.</p>
    <p v-else-if="!r && store.errorMessage" class="loading error">{{ store.errorMessage }}</p>
    <p v-else-if="!r" class="loading">불러오는 중...</p>

    <template v-else>
      <div ref="reportContent" class="pdf-content">
        <TravelArchiveSummaryCard :trip-id="tripId" :show-status="false" />

        <section class="card overview">
          <h2>여행 지출 한눈에 보기</h2>
          <div class="overview-head"><div><span>총</span><strong>{{ money(r.spent) }}</strong><span>지출</span></div><em>{{ r.remaining >= 0 ? '예산 안에서 여행을 마쳤어요' : '예산을 초과했어요' }}</em></div>
          <div class="usage-label"><span>예산 사용률</span><b>{{ budgetUsage }}%</b></div>
          <div class="bar-track"><span class="bar-fill blue" :style="{ width: `${Math.min(budgetUsage, 100)}%` }" /></div>
          <div class="budget-scale"><span>0원</span><b>목표 {{ money(r.targetBudget) }}</b></div>
          <div class="metric-grid"><div class="remaining"><i aria-hidden="true"><Wallet /></i><span><small>남은 예산</small><b>{{ money(r.remaining) }}</b></span></div><div class="average"><i aria-hidden="true"><ChartNoAxesColumnIncreasing /></i><span><small>일 평균</small><b>{{ money(r.dailyAverage) }}</b></span></div></div>
        </section>

        <section class="card">
          <h2>카테고리별 지출</h2>
          <div class="category-list">
            <div v-for="item in categoryRows" :key="item.name" class="category-row"><i :style="{ background: item.color }">{{ item.rank }}</i><b>{{ item.name }}</b><div class="bar-track"><span class="bar-fill" :style="{ width: `${item.percent}%`, background: item.color }" /></div><strong>{{ money(item.amount) }} · {{ item.percent }}%</strong></div>
          </div>
          <p v-if="topCategory" class="insight">{{ topCategory.name }} 지출이 {{ money(topCategory.amount) }}으로 가장 많았어요.</p>
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
          <h2>나라별 지출 특징</h2>
          <div v-if="r.countryTopCategories.length" class="feature-grid"><article v-for="item in r.countryTopCategories" :key="item.name"><b>{{ item.flag }} {{ item.name }}</b><small>가장 많이 쓴 항목</small><strong :style="{ color: item.color }">{{ item.category }}</strong><span>{{ item.percent }}% · {{ money(item.amount) }}</span></article></div>
          <p v-else class="empty-state">여행 중 지출 내역이 없어 나라별 지출 특징을 계산할 수 없어요.</p>
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
          <div v-if="dailyCountrySlots.some(slot => slot.name)" class="daily-country-periods" aria-label="날짜별 여행 국가">
            <span v-for="(slot, index) in dailyCountrySlots" :key="`${daily[index]?.rawDate}-${slot.name || 'none'}`" :class="{ start: slot.start, end: slot.end, empty: !slot.name }" :style="{ '--country-color': slot.color }">
              <b v-if="slot.start">{{ slot.flag }} {{ slot.name }}</b>
            </span>
          </div>
          <div class="trend-facts"><span>지출 없는 날 <b>{{ zeroDays }}일</b></span><span>최고 지출일 <b>{{ peak?.date }}</b></span></div>
        </section>

        <section class="card next-trip">
          <h2>다음 여행 예산 제안</h2>
          <div class="budget-basis-guide"><p>이번 여행의 국가별 실제 지출과 예산 사용률을 기준으로, 실제 지출에 10%의 여유 금액을 더해 다음 예산을 제안해요.</p></div>
          <template v-if="nextBudgetPlans.length">
            <div class="recommend-summary">
              <small>비슷한 일정의 다음 추천 예산</small>
              <strong>{{ money(nextBudgetTotal) }}</strong>
              <em v-if="nextBudgetDifference < 0">현재보다 {{ money(Math.abs(nextBudgetDifference)) }} 줄여도 여유 있어요</em>
              <em v-else-if="nextBudgetDifference > 0" class="increase">현재보다 {{ money(nextBudgetDifference) }} 더 준비해 보세요</em>
              <em v-else>현재 예산 수준을 유지해 보세요</em>
              <span>이번 실제 지출에 10% 여유 금액을 더해 계산했어요.</span>
            </div>
            <div class="recommend-list">
              <article v-for="country in nextBudgetPlans" :key="country.name">
                <div class="recommend-country"><b>{{ country.flag }} {{ country.name }}</b><span>예산 사용률 {{ country.usage }}%</span></div>
                <div class="recommend-change"><small>{{ money(country.currentBudget) }}</small><i>→</i><strong>{{ money(country.suggestedBudget) }}</strong></div>
                <p v-if="country.action === 'reduce'"><b>{{ money(Math.abs(country.difference)) }} 줄여도 괜찮아요.</b><span v-if="country.topCategory">가장 큰 지출은 {{ country.topCategory }}였어요.</span></p>
                <p v-else-if="country.action === 'increase'" class="increase"><b>{{ money(country.difference) }} 늘려보세요.</b><span v-if="country.topCategory">가장 큰 지출은 {{ country.topCategory }}였어요.</span></p>
                <p v-else class="keep"><b>현재 예산을 유지해도 좋아요.</b><span v-if="country.topCategory">가장 큰 지출은 {{ country.topCategory }}였어요.</span></p>
              </article>
            </div>
          </template>
          <p v-else class="empty-state">지출 내역이 쌓이면 이번 여행을 바탕으로 다음 국가별 예산을 제안해 드려요.</p>
        </section>
      </div>
      <button class="pdf" type="button" :disabled="downloading" @click="download">{{ downloading ? 'PDF 생성 중...' : 'PDF로 저장하기' }}</button>
    </template>
    </section>
  </main>
</template>

<style scoped>
.page{min-height:100vh;padding:0 16px 32px;background:#f3f6fc;color:#111a2d}.page-header{display:grid;height:64px;grid-template-columns:36px 1fr 36px;align-items:end;padding-bottom:14px}.page-header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:25px;font-weight:800;box-shadow:0 5px 16px rgba(36,72,117,.07)}.page-header h1{text-align:center;font-size:16px;font-weight:900}.loading{padding:40px 0;color:#8290a3;font-size:12px;text-align:center}.loading.error{color:#e5484d}.pdf-content{display:flex;flex-direction:column;gap:10px}.card{padding:15px;border:1px solid #dfe7f4;border-radius:17px;background:#fff;box-shadow:0 6px 18px rgba(23,63,141,.05)}.card h2{font-size:13px;font-weight:950}.overview-head{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:12px}.overview-head>div{display:flex;min-width:0;align-items:baseline;white-space:nowrap}.overview-head strong{color:#2f6fed;font-size:22px;font-weight:950;letter-spacing:-.04em}.overview-head span{margin-left:4px;color:#2f6fed;font-size:12px;font-weight:900}.overview-head em{flex:0 0 auto;padding:8px 10px;border-radius:999px;background:#e8f8f3;color:#087f61;font-size:8px;font-style:normal;font-weight:850;line-height:1;white-space:nowrap}.usage-label{display:flex;align-items:center;gap:8px;margin-top:11px;font-size:9px}.usage-label b{font-size:11px}.bar-track{height:7px;border-radius:99px;background:#e9eef6;overflow:hidden}.usage-label+.bar-track{margin-top:6px}.bar-fill{display:block;height:100%;border-radius:99px}.bar-fill.blue{background:#2f6fed}.metric-grid{display:grid;grid-template-columns:repeat(3,1fr);margin-top:13px;padding:11px 4px;border-radius:12px;background:#f5f8fd}.metric-grid>div{text-align:center}.metric-grid>div+div{border-left:1px solid #dfe7f4}.metric-grid small{display:block;color:#8290a3;font-size:8px}.metric-grid b{display:block;margin-top:5px;font-size:10px}.green{color:#18a77d}.country-list{display:flex;flex-direction:column;gap:14px;margin-top:13px}.country-head,.country-money{display:flex;align-items:center;justify-content:space-between}.country-head b{font-size:10px}.country-head strong{color:#2f6fed;font-size:10px}.country-head strong.danger{color:#e5484d}.country-row .bar-track{margin-top:6px}.country-money{margin-top:5px;color:#71819a;font-size:8px}.country-money em{color:#18a77d;font-style:normal;font-weight:800}.category-list{display:flex;flex-direction:column;gap:9px;margin-top:13px}.category-row{display:grid;grid-template-columns:18px 45px minmax(50px,1fr) 105px;align-items:center;gap:7px}.category-row i{display:grid;width:17px;height:17px;place-items:center;border-radius:50%;color:#fff;font-size:8px;font-style:normal;font-weight:900}.category-row b{font-size:9px}.category-row .bar-track{height:6px}.category-row strong{font-size:8px;text-align:right}.insight{margin-top:12px;padding:8px 10px;border-radius:10px;background:#eef5ff;color:#2f6fed;font-size:9px;font-weight:800}.feature-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:7px;margin-top:12px}.feature-grid article{display:flex;min-width:0;flex-direction:column;align-items:center;padding:10px 5px;border:1px solid #e4eaf3;border-radius:12px;text-align:center}.feature-grid article b{font-size:9px}.feature-grid article small{margin-top:8px;color:#8290a3;font-size:7px}.feature-grid article strong{margin-top:3px;font-size:12px}.feature-grid article span{margin-top:4px;font-size:7px;font-weight:800}.empty-state{margin-top:12px;padding:15px;border-radius:12px;background:#f5f8fd;color:#8290a3;font-size:9px;line-height:1.5;text-align:center}.line-chart{width:100%;margin-top:10px;overflow:visible}.average-line{stroke:#79a6f5;stroke-width:1;stroke-dasharray:3 3}.average-label{fill:#2f6fed;font-size:6px;font-weight:700}.peak-label{fill:#e5484d;font-size:6px;font-weight:800}.daily-labels{display:flex;justify-content:space-between;margin-top:-6px}.daily-labels span{flex:1;color:#8290a3;font-size:5.5px;text-align:center;transform:rotate(-35deg)}.trend-facts{display:grid;grid-template-columns:1fr 1fr;margin-top:12px;padding:10px;border-radius:11px;background:#f5f8fd;color:#71819a;font-size:8px;text-align:center}.trend-facts span+span{border-left:1px solid #dfe7f4}.trend-facts b{margin-left:4px;color:#173f8d;font-size:9px}.next-trip{display:flex;flex-direction:column;gap:10px}.ai-budget-guide{display:flex;align-items:center;gap:9px;padding:10px 11px;border-radius:12px;background:#f5f8fd}.ai-budget-guide i{display:grid;flex:0 0 28px;width:28px;height:28px;place-items:center;border-radius:9px;background:linear-gradient(135deg,#2f6fed,#7143e8);color:#fff;font-size:9px;font-style:normal;font-weight:950;box-shadow:0 5px 12px rgba(47,111,237,.18)}.ai-budget-guide p{color:#55708f;font-size:9px;font-weight:750;line-height:1.5}.recommend-summary{display:flex;flex-direction:column;margin-top:0;padding:14px;border-radius:14px;background:#eef5ff}.recommend-summary small{color:#55708f;font-size:8px;font-weight:700}.recommend-summary strong{margin-top:4px;color:#2f6fed;font-size:19px;font-weight:950}.recommend-summary em{margin-top:5px;color:#087f61;font-size:9px;font-style:normal;font-weight:900}.recommend-summary em.increase{color:#e5484d}.recommend-summary span{margin-top:7px;color:#8290a3;font-size:7px}.recommend-list{display:flex;flex-direction:column;gap:8px}.recommend-list article{padding:11px;border:1px solid #e4eaf3;border-radius:13px}.recommend-country,.recommend-change{display:flex;align-items:center;justify-content:space-between}.recommend-country b{font-size:10px}.recommend-country span{color:#8290a3;font-size:8px}.recommend-change{justify-content:flex-start;gap:7px;margin-top:7px}.recommend-change small{color:#8290a3;font-size:9px;text-decoration:line-through}.recommend-change i{color:#9aa8bb;font-size:10px;font-style:normal}.recommend-change strong{color:#2f6fed;font-size:13px}.recommend-list article p{display:flex;justify-content:space-between;gap:8px;margin-top:7px;padding-top:7px;border-top:1px solid #edf1f6;color:#55708f;font-size:8px}.recommend-list article p b{color:#087f61}.recommend-list article p.increase b{color:#e5484d}.recommend-list article p.keep b{color:#2f6fed}.recommend-list article p span{text-align:right}.next-trip p.empty-state{display:block;margin-top:0;text-align:center}.pdf{width:100%;margin-top:12px;padding:14px;border-radius:12px;background:#2f6fed;color:#fff;font-size:12px;font-weight:900;box-shadow:0 10px 22px rgba(47,111,237,.2)}.pdf:disabled{opacity:.6}@media(max-width:380px){.overview-head{align-items:flex-start;flex-direction:column}.overview-head em{align-self:flex-start}}

/* PDF 미리보기처럼 떠오르는 리포트 레이어 */
.page{position:fixed;z-index:1000;inset:0;overflow-y:auto;min-height:0;padding:18px 14px 30px;background:transparent}.modal-backdrop{position:fixed;z-index:-1;inset:0;width:100%;height:100%;border:0;background:rgba(15,30,58,.48);backdrop-filter:blur(5px);animation:report-backdrop-in .28s ease-out both}.report-modal{width:min(100%,430px);min-height:calc(100vh - 48px);margin:0 auto;padding:0 14px 20px;border:1px solid rgba(255,255,255,.88);border-radius:22px;background:#f3f6fc;box-shadow:0 28px 70px rgba(11,27,55,.3);animation:report-paper-in .46s cubic-bezier(.2,.8,.2,1) both;transform-origin:50% 100%}.page-header{position:sticky;z-index:5;top:0;display:grid;height:70px;grid-template-columns:38px 1fr 38px;gap:10px;align-items:center;margin:0 -14px 12px;padding:9px 14px 7px;border-bottom:1px solid #e1e8f3;border-radius:22px 22px 0 0;background:rgba(255,255,255,.94);backdrop-filter:blur(12px)}.page-header>div{min-width:0}.page-header small{display:block;color:#2f6fed;font-size:7px;font-weight:900;letter-spacing:.14em}.page-header h1{margin-top:2px;text-align:left;font-size:15px}.page-header button{width:34px;height:34px;border-radius:50%;background:#edf3fd;font-size:22px;font-weight:500;line-height:1;box-shadow:none}.document-mark{display:flex;width:34px;height:38px;flex-direction:column;gap:4px;padding:10px 8px;border:1.5px solid #2f6fed;border-radius:7px;background:#eef4ff}.document-mark i{display:block;height:2px;border-radius:2px;background:#2f6fed}.document-mark i:last-child{width:65%}@keyframes report-paper-in{from{opacity:0;transform:translateY(42px) scale(.965)}to{opacity:1;transform:translateY(0) scale(1)}}@keyframes report-backdrop-in{from{opacity:0}to{opacity:1}}@media(max-width:380px){.page{padding:8px 6px 18px}.report-modal{min-height:calc(100vh - 16px);padding-right:10px;padding-left:10px;border-radius:18px}.page-header{margin-right:-10px;margin-left:-10px;border-radius:18px 18px 0 0}}@media(prefers-reduced-motion:reduce){.report-modal,.modal-backdrop{animation:none}}
.daily-country-periods{display:flex;height:31px;margin:12px 2px 0}.daily-country-periods>span{position:relative;flex:1;border-top:2px solid var(--country-color,#cbd5e1)}.daily-country-periods>span::after{position:absolute;top:-4px;right:-1px;width:6px;height:6px;border:1.5px solid var(--country-color,#cbd5e1);border-radius:50%;background:#fff;content:''}.daily-country-periods>span.start::before{position:absolute;top:-4px;left:-1px;width:6px;height:6px;border:1.5px solid var(--country-color,#cbd5e1);border-radius:50%;background:#fff;content:''}.daily-country-periods>span.end::after{background:var(--country-color,#cbd5e1)}.daily-country-periods>span.empty{border-color:transparent}.daily-country-periods>span.empty::after{display:none}.daily-country-periods b{position:absolute;top:6px;left:0;z-index:1;color:var(--country-color,#526f9e);font-size:7px;font-weight:900;white-space:nowrap}
.overview-head>div>span:first-child{margin:0 7px 0 0;color:#111a2d;font-size:13px}.overview-head>div>span:last-child{color:#111a2d}.budget-scale{display:flex;align-items:center;justify-content:space-between;margin-top:6px;color:#111a2d;font-size:8px}.budget-scale b{font-size:8px}.overview .metric-grid{grid-template-columns:1fr 1fr;gap:8px;padding:0;background:transparent}.overview .metric-grid>div{display:flex;min-width:0;align-items:center;gap:9px;padding:11px 10px;border:0;border-radius:12px;text-align:left}.overview .metric-grid>div.remaining{background:#edf9f5}.overview .metric-grid>div.average{background:#f1f5ff}.overview .metric-grid i{display:grid;flex:0 0 29px;width:29px;height:29px;place-items:center;border-radius:50%;background:#fff;color:#18a77d;font-size:12px;font-style:normal}.overview .metric-grid .average i{color:#2f6fed}.overview .metric-grid span{min-width:0}.overview .metric-grid small{font-size:8px;font-weight:800}.overview .metric-grid .remaining small,.overview .metric-grid .remaining b{color:#079975}.overview .metric-grid .average small{color:#2f6fed}.overview .metric-grid b{overflow:hidden;margin-top:3px;color:#111a2d;font-size:11px;text-overflow:ellipsis;white-space:nowrap}
.overview .metric-grid i svg{width:15px;height:15px;stroke-width:2.2}
.report-modal .page-header{position:relative;top:auto}
.report-modal .card{padding:17px}.report-modal .card h2{font-size:15px}.report-modal .overview-head strong{font-size:24px}.report-modal .overview-head span{font-size:13px}.report-modal .overview-head em{font-size:10px;line-height:1.25}.report-modal .usage-label{font-size:11px}.report-modal .usage-label b{font-size:12px}.report-modal .budget-scale,.report-modal .budget-scale b{font-size:10px}.report-modal .overview .metric-grid small{font-size:10px}.report-modal .overview .metric-grid b{font-size:13px}.report-modal .country-head b,.report-modal .country-head strong{font-size:12px}.report-modal .country-money{font-size:10px}.report-modal .category-row{grid-template-columns:21px 48px minmax(40px,1fr) 112px;gap:8px}.report-modal .category-row i{width:20px;height:20px;font-size:9px}.report-modal .category-row b{font-size:11px}.report-modal .category-row strong{font-size:10px}.report-modal .insight{font-size:11px;line-height:1.5}.report-modal .feature-grid article b{font-size:11px}.report-modal .feature-grid article small{font-size:9px}.report-modal .feature-grid article strong{font-size:14px}.report-modal .feature-grid article span{font-size:9px}.report-modal .empty-state{font-size:11px}.report-modal .average-label,.report-modal .peak-label{font-size:7px}.report-modal .daily-labels span{font-size:6.5px}.report-modal .daily-country-periods b{font-size:8px}.report-modal .trend-facts{font-size:10px}.report-modal .trend-facts b{font-size:11px}.budget-basis-guide{padding:12px 13px;border:1px solid #dbe6f7;border-radius:12px;background:#f5f8fd}.budget-basis-guide p{color:#55708f;font-size:11px;font-weight:750;line-height:1.65}.report-modal .recommend-summary small{font-size:10px}.report-modal .recommend-summary strong{font-size:21px}.report-modal .recommend-summary em{font-size:11px}.report-modal .recommend-summary span{font-size:9px;line-height:1.5}.report-modal .recommend-country b{font-size:12px}.report-modal .recommend-country span{font-size:10px}.report-modal .recommend-change small{font-size:11px}.report-modal .recommend-change strong{font-size:15px}.report-modal .recommend-list article p{font-size:10px;line-height:1.5}.report-modal .pdf{font-size:13px}
</style>
