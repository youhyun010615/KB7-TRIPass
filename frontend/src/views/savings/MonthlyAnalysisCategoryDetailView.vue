<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchCategoryAnalysis } from '@/api/monthlyAnalysis';
import foodIcon from '@/assets/icons/food.svg';
import cafeIcon from '@/assets/icons/cafe.svg';
import shoppingIcon from '@/assets/icons/shopping-cart.svg';
import taxiIcon from '@/assets/icons/taxi.svg';
import leisureIcon from '@/assets/icons/hobby_drink.svg';
import foodIconRaw from '@/assets/icons/food.svg?raw';
import cafeIconRaw from '@/assets/icons/cafe.svg?raw';
import shoppingIconRaw from '@/assets/icons/shopping-cart.svg?raw';
import taxiIconRaw from '@/assets/icons/taxi.svg?raw';
import leisureIconRaw from '@/assets/icons/hobby_drink.svg?raw';

const route = useRoute();
const router = useRouter();

const yearMonth = computed(() => String(route.params.yearMonth || ''));
const categoryCode = computed(() => String(route.params.categoryCode || ''));

const loading = ref(false);
const notFound = ref(false);
const errorMessage = ref('');
const data = ref(null);

const categoryMeta = {
  FOOD: { icon: '🍽', iconSrc: foodIcon, iconRaw: foodIconRaw, color: '#2457aa' },
  CAFE: { icon: '☕', iconSrc: cafeIcon, iconRaw: cafeIconRaw, color: '#3b82f6' },
  LIVING: { icon: '🧺', color: '#13a184' },
  SHOPPING: { icon: '🛍', iconSrc: shoppingIcon, iconRaw: shoppingIconRaw, color: '#f59e0b' },
  LEISURE: { icon: '🎮', iconSrc: leisureIcon, iconRaw: leisureIconRaw, color: '#8b5cf6' },
  TRANSPORT: { icon: '🚌', iconSrc: taxiIcon, iconRaw: taxiIconRaw, color: '#0ea5e9' },
  OTHER: { icon: '•••', color: '#64748b' },
};

const meta = computed(() => categoryMeta[categoryCode.value] || categoryMeta.OTHER);

function darken(hex, amount = 0.3) {
  const clean = (hex || '').replace('#', '');
  const num = parseInt(clean, 16);
  if (Number.isNaN(num)) return hex;
  const r = Math.max(0, Math.round(((num >> 16) & 255) * (1 - amount)));
  const g = Math.max(0, Math.round(((num >> 8) & 255) * (1 - amount)));
  const b = Math.max(0, Math.round((num & 255) * (1 - amount)));
  return `rgb(${r}, ${g}, ${b})`;
}

const coloredIcon = computed(() => {
  if (!meta.value.iconRaw) return '';
  return meta.value.iconRaw.replaceAll('black', darken(meta.value.color));
});

function formatCurrency(value, absolute = false) {
  const number = Number(value) || 0;
  return `${(absolute ? Math.abs(number) : number).toLocaleString('ko-KR')}원`;
}

function monthLabel(value) {
  const month = Number(String(value || '').split('-')[1]);
  return Number.isFinite(month) ? `${month}월` : '';
}

const categoryName = computed(() => data.value?.categoryName || '');
const displayYearMonth = computed(() => data.value?.analysisYearMonth || yearMonth.value);

function changeOf(rate) {
  return rate === null || rate === undefined ? null : Number(rate);
}

const changeRate = computed(() => changeOf(data.value?.previousMonthChange));
const threeMonthChangeRate = computed(() => changeOf(data.value?.threeMonthAverageChange));
const isAboveThreeMonthAverage = computed(
  () => threeMonthChangeRate.value !== null && threeMonthChangeRate.value >= 20,
);

const insights = computed(() => {
  const list = data.value?.analysisInsights || [];
  return list.map((text) => ({ text, icon: insightIcon(text) }));
});

function insightIcon(text) {
  if (text.includes('지난달')) return '📊';
  if (text.includes('3개월')) return '📈';
  if (text.includes('요일')) return '📅';
  if (text.includes('주차')) return '📅';
  if (text.includes("'")) return '🏪';
  return '💡';
}

const monthlyTrend = computed(() => data.value?.monthlyTrend || []);
const monthlyTrendMax = computed(() =>
  Math.max(1, ...monthlyTrend.value.map((row) => Number(row.spending) || 0)),
);
const monthlyTrendAverage = computed(() => {
  if (!monthlyTrend.value.length) return 0;
  const sum = monthlyTrend.value.reduce((acc, row) => acc + (Number(row.spending) || 0), 0);
  return sum / monthlyTrend.value.length;
});
const monthlyTrendAveragePosition = computed(() => {
  if (!monthlyTrendMax.value) return 0;
  return Math.min(100, (monthlyTrendAverage.value / monthlyTrendMax.value) * 100);
});

const weeklyBreakdown = computed(() => data.value?.weeklyBreakdown || []);
const weeklyBreakdownMax = computed(() =>
  Math.max(1, ...weeklyBreakdown.value.map((row) => Number(row.spending) || 0)),
);

const topMerchants = computed(() => data.value?.topMerchants || []);

function formatPeakDay(value) {
  if (!value) return '';
  return /^\d{4}-\d{2}-\d{2}$/.test(value) ? value.replaceAll('-', '.') : value;
}

async function load() {
  loading.value = true;
  notFound.value = false;
  errorMessage.value = '';
  try {
    data.value = await fetchCategoryAnalysis(yearMonth.value, categoryCode.value);
  } catch (error) {
    if (error.response?.status === 404) {
      notFound.value = true;
    } else {
      errorMessage.value =
        error.response?.data?.message || '카테고리 분석 데이터를 불러오지 못했어요.';
    }
  } finally {
    loading.value = false;
  }
}

onMounted(load);

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push({ name: 'MonthlyAnalysisReport', params: { yearMonth: yearMonth.value } });
}
</script>

<template>
  <main class="analysis-report-view">
    <div class="report-backdrop" @click="goBack"></div>
    <div class="report-modal">
      <header v-if="!data" class="report-header">
        <button type="button" aria-label="리포트로 돌아가기" @click="goBack">‹</button>
        <div>
          <small>TRIPASS AI REPORT</small>
          <h1>{{ monthLabel(displayYearMonth) }} {{ categoryName || meta.icon }} 상세 분석</h1>
        </div>
        <span></span>
      </header>
      <header v-else class="hero-header">
        <button type="button" aria-label="리포트로 돌아가기" @click="goBack">‹</button>
        <span
          class="category-icon-glyph"
          :style="{ background: `${meta.color}18` }"
        >
          <span v-if="meta.iconSrc" v-html="coloredIcon"></span>
          <template v-else>{{ meta.icon }}</template>
        </span>
        <div>
          <b>{{ monthLabel(displayYearMonth) }} {{ categoryName }} 상세 분석</b>
          <small>{{ categoryName }} 지출</small>
          <strong>{{ formatCurrency(data.currentMonthSpending) }}</strong>
          <em v-if="data.spendingRank">이번 달 소비 {{ data.spendingRank }}위</em>
        </div>
      </header>

      <div class="report-modal-body">
    <section v-if="loading" class="state-box" role="status">
      <span class="spinner" />
      <b>AI가 소비 내역을 분석하고 있어요</b>
    </section>

    <section v-else-if="notFound" class="state-box">
      <span class="state-icon">AI</span>
      <b>아직 분석 데이터가 없어요</b>
      <small>리포트가 생성되면 카테고리별 상세 분석도 함께 제공돼요.</small>
    </section>

    <section v-else-if="errorMessage" class="state-box error">
      <span class="state-icon">!</span>
      <b>분석 내용을 불러오지 못했어요</b>
      <small>{{ errorMessage }}</small>
      <button type="button" @click="load">다시 시도</button>
    </section>

    <template v-else-if="data">
      <article class="report-paper">
      <section class="paper-section">
        <h3>이번 달 요약</h3>
        <dl class="summary-grid">
          <div>
            <dt>거래 건수</dt>
            <dd>{{ data.transactionCount ?? 0 }}건</dd>
          </div>
          <div v-if="data.spendingRatio != null">
            <dt>전체 지출 비중</dt>
            <dd>{{ Number(data.spendingRatio).toFixed(1) }}%</dd>
          </div>
          <div v-if="data.weeklyAverage != null">
            <dt>주 평균</dt>
            <dd>{{ formatCurrency(data.weeklyAverage) }}</dd>
          </div>
          <div v-if="data.dailyAverage != null">
            <dt>일 평균</dt>
            <dd>{{ formatCurrency(data.dailyAverage) }}</dd>
          </div>
        </dl>
        <p v-if="data.peakSpendingDay" class="peak-day-note">
          가장 많이 지출한 날은 <b>{{ formatPeakDay(data.peakSpendingDay) }}</b>이에요
          <template v-if="data.peakSpendingDayCount">({{ data.peakSpendingDayCount }}건)</template>
        </p>
      </section>

      <section
        v-if="data.previousMonthSpending != null || data.threeMonthAverage != null || data.projectedMonthSpending != null"
        class="paper-section"
      >
        <h3>비교 분석</h3>
        <div v-if="data.previousMonthSpending != null" class="compare-row">
          <div>
            <small>지난달</small>
            <b>{{ formatCurrency(data.previousMonthSpending) }}</b>
          </div>
          <span
            v-if="changeRate !== null"
            :class="['change-badge', changeRate >= 0 ? 'up' : 'down']"
          >
            {{ changeRate >= 0 ? '▲' : '▼' }} {{ Math.abs(changeRate).toFixed(1) }}%
          </span>
        </div>
        <div v-if="data.threeMonthAverage != null" class="compare-row">
          <div>
            <small>최근 3개월 평균</small>
            <b>{{ formatCurrency(data.threeMonthAverage) }}</b>
          </div>
          <span v-if="isAboveThreeMonthAverage" class="change-badge up">평균보다 많이 썼어요</span>
        </div>
        <div v-if="data.projectedMonthSpending != null" class="compare-row">
          <div>
            <small>이번 달 예상 지출</small>
            <b>{{ formatCurrency(data.projectedMonthSpending) }}</b>
          </div>
        </div>
      </section>

      <section v-if="data.recommendationReason || insights.length" class="paper-section">
        <h3>AI 인사이트</h3>
        <p v-if="data.recommendationReason" class="recommendation-callout">
          <span aria-hidden="true">✦</span>{{ data.recommendationReason }}
        </p>
        <ul v-if="insights.length" class="insight-list">
          <li v-for="(insight, index) in insights" :key="index">
            <span>{{ insight.icon }}</span>
            <p>{{ insight.text }}</p>
          </li>
        </ul>
      </section>

      <section v-if="monthlyTrend.length" class="paper-section">
        <h3>월별 지출 추이</h3>
        <div class="trend-chart" :class="{ single: monthlyTrend.length === 1 }">
          <div
            v-if="monthlyTrendAverage"
            class="trend-average-line"
            :style="{ bottom: `${monthlyTrendAveragePosition}%` }"
          ></div>
          <div v-for="(row, index) in monthlyTrend" :key="row.yearMonth" class="trend-bar">
            <span class="trend-count">{{ row.transactionCount ?? 0 }}건</span>
            <div
              class="trend-bar-fill"
              :class="{ current: index === monthlyTrend.length - 1 }"
              :style="{ height: `${Math.max(6, ((Number(row.spending) || 0) / monthlyTrendMax) * 100)}%` }"
            ></div>
            <small>{{ monthLabel(row.yearMonth) }}</small>
          </div>
        </div>
      </section>

      <section v-if="weeklyBreakdown.length" class="paper-section">
        <h3>주차별 지출</h3>
        <div class="weekly-list">
          <div v-for="(row, index) in weeklyBreakdown" :key="row.week ?? index" class="weekly-row">
            <small>{{ row.week ?? index + 1 }}주차</small>
            <div class="weekly-bar-track">
              <div
                class="weekly-bar-fill"
                :style="{ width: `${Math.max(6, ((Number(row.spending) || 0) / weeklyBreakdownMax) * 100)}%` }"
              ></div>
            </div>
            <b>{{ formatCurrency(row.spending) }}</b>
          </div>
        </div>
      </section>
      <section v-else class="paper-section">
        <h3>주차별 지출</h3>
        <p class="empty-note">이번 달 거래가 없습니다.</p>
      </section>

      <section class="paper-section">
        <h3>가맹점 TOP5</h3>
        <ol v-if="topMerchants.length" class="merchant-list">
          <li v-for="(merchant, index) in topMerchants" :key="merchant.merchantName">
            <b>{{ index + 1 }}</b>
            <span>{{ merchant.merchantName }}</span>
            <div>
              <strong>{{ formatCurrency(merchant.totalAmount) }}</strong>
              <small>{{ merchant.transactionCount }}건</small>
            </div>
          </li>
        </ol>
        <p v-else class="empty-note">가맹점 정보가 없습니다.</p>
      </section>
      </article>
    </template>
      </div>
    </div>
  </main>
</template>

<style scoped>
.analysis-report-view {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 16px;
  color: #17213a;
}
.report-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(9, 18, 38, 0.55);
  animation: report-backdrop-enter 0.3s ease both;
}
.report-modal {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 358px;
  max-height: 100%;
  overflow: hidden;
  border-radius: 24px;
  background: #f3f6ff;
  box-shadow: 0 24px 60px rgba(9, 18, 38, 0.35);
  animation: report-modal-enter 0.28s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.report-modal-body {
  overflow-y: auto;
  flex: 1;
  padding-bottom: 30px;
  -webkit-overflow-scrolling: touch;
}
@keyframes report-backdrop-enter {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes report-modal-enter {
  from { transform: scale(0.94) translateY(10px); opacity: 0; }
  to { transform: scale(1) translateY(0); opacity: 1; }
}
.report-header {
  flex: none;
  z-index: 10;
  display: grid;
  grid-template-columns: 40px 1fr 40px;
  align-items: center;
  padding: 18px 14px 13px;
  border-bottom: 1px solid #e0e8f5;
  border-radius: 24px 24px 0 0;
  background: #f3f6ff;
}
.report-header > button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  color: #173f8d;
  font-size: 28px;
  line-height: 1;
}
.report-header > div {
  text-align: center;
}
.report-header small {
  color: #f06a2a;
  font-size: 7px;
  font-weight: 950;
  letter-spacing: 0.16em;
}
.report-header h1 {
  margin-top: 2px;
  overflow: hidden;
  color: #173f8d;
  font-size: 14px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.report-paper {
  overflow: hidden;
  margin: 14px;
  border-radius: 25px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(23, 63, 141, 0.16);
}
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 90px 20px;
  text-align: center;
  color: #64748b;
}
.state-box b {
  color: #17213a;
  font-size: 13px;
}
.state-box small {
  font-size: 11px;
}
.state-box .state-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 50%;
  background: #dce9ff;
  color: #286ce0;
  font-size: 11px;
  font-weight: 900;
}
.state-box.error .state-icon {
  background: #ffe3e3;
  color: #e8484f;
}
.state-box button {
  margin-top: 6px;
  padding: 8px 16px;
  border-radius: 10px;
  background: #173f8d;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}
.spinner {
  width: 26px;
  height: 26px;
  border: 3px solid #dce9ff;
  border-top-color: #173f8d;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.hero-header {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  padding: 14px 16px 22px;
  border-radius: 24px 24px 0 0;
  background: linear-gradient(135deg, #173f8d 0%, #286ce0 100%);
  color: #fff;
}
.hero-header > button {
  display: grid;
  flex: 0 0 100%;
  width: 36px;
  height: 36px;
  margin-bottom: -4px;
  place-items: center;
  border-radius: 12px;
  color: #fff;
  font-size: 28px;
  line-height: 1;
}
.category-icon-glyph {
  display: grid;
  flex: none;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 14px;
}
.category-icon-glyph :deep(svg) {
  width: 22px;
  height: 22px;
}
.hero-header b {
  display: block;
  font-size: 16px;
  font-weight: 900;
}
.hero-header small {
  display: block;
  margin-top: 3px;
  color: #d7e4ff;
  font-size: 10.5px;
  font-weight: 700;
}
.hero-header strong {
  display: block;
  margin-top: 6px;
  font-size: 21px;
  font-weight: 900;
}
.hero-header em {
  display: block;
  margin-top: 4px;
  color: #d7e4ff;
  font-size: 10px;
  font-style: normal;
}
.paper-section {
  padding: 20px;
}
.paper-section + .paper-section {
  border-top: 1px solid #eef1f8;
}
.paper-section h3 {
  margin-bottom: 12px;
  color: #17213a;
  font-size: 12.5px;
  font-weight: 900;
}
.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.summary-grid dt {
  color: #8a9bb6;
  font-size: 9.5px;
}
.summary-grid dd {
  margin-top: 3px;
  color: #17213a;
  font-size: 13px;
  font-weight: 800;
}
.peak-day-note {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eef1f6;
  color: #64748b;
  font-size: 10.5px;
  line-height: 1.5;
}
.peak-day-note b {
  color: #173f8d;
  font-weight: 800;
}
.compare-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}
.compare-row + .compare-row {
  border-top: 1px solid #eef1f6;
}
.compare-row small {
  display: block;
  color: #8a9bb6;
  font-size: 9.5px;
}
.compare-row b {
  display: block;
  margin-top: 2px;
  color: #17213a;
  font-size: 13px;
  font-weight: 800;
}
.change-badge {
  padding: 5px 9px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 900;
  white-space: nowrap;
}
.change-badge.up {
  background: #ffe3e3;
  color: #e8484f;
}
.change-badge.down {
  background: #e3f6ea;
  color: #1c9a67;
}
.recommendation-callout {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #eaf2ff;
  color: #173f8d;
  font-size: 11.5px;
  font-weight: 700;
  line-height: 1.5;
}
.recommendation-callout span {
  flex: none;
  color: #f0a93c;
  font-size: 13px;
}
.insight-list {
  display: grid;
  gap: 10px;
}
.insight-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.insight-list span {
  flex: none;
  font-size: 14px;
}
.insight-list p {
  color: #3a4a68;
  font-size: 11.5px;
  line-height: 1.5;
}
.trend-chart {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  align-items: end;
  gap: 10px;
  height: 130px;
  padding-top: 8px;
}
.trend-chart.single {
  grid-template-columns: 1fr;
}
.trend-average-line {
  position: absolute;
  right: 0;
  left: 0;
  border-top: 1px dashed #b7c3dc;
}
.trend-bar {
  display: flex;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}
.trend-count {
  color: #8a9bb6;
  font-size: 8.5px;
}
.trend-bar-fill {
  width: 26px;
  border-radius: 6px 6px 0 0;
  background: #cbdaf5;
}
.trend-bar-fill.current {
  background: #173f8d;
}
.trend-bar small {
  color: #8a9bb6;
  font-size: 9.5px;
}
.weekly-list {
  display: grid;
  gap: 10px;
}
.weekly-row {
  display: grid;
  grid-template-columns: 40px 1fr auto;
  align-items: center;
  gap: 8px;
}
.weekly-row small {
  color: #8a9bb6;
  font-size: 9.5px;
}
.weekly-bar-track {
  height: 8px;
  border-radius: 999px;
  background: #eef1f6;
}
.weekly-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: #4a7fe0;
}
.weekly-row b {
  color: #17213a;
  font-size: 11px;
  font-weight: 800;
}
.merchant-list {
  display: grid;
  gap: 10px;
}
.merchant-list li {
  display: grid;
  grid-template-columns: 22px 1fr auto;
  align-items: center;
  gap: 8px;
}
.merchant-list li > b {
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border-radius: 50%;
  background: #dce9ff;
  color: #286ce0;
  font-size: 9.5px;
  font-weight: 900;
}
.merchant-list li > span {
  color: #26334d;
  font-size: 12px;
  font-weight: 700;
}
.merchant-list li > div {
  text-align: right;
}
.merchant-list li > div strong {
  display: block;
  color: #17213a;
  font-size: 12px;
  font-weight: 800;
}
.merchant-list li > div small {
  display: block;
  margin-top: 2px;
  color: #8a9bb6;
  font-size: 9px;
}
.empty-note {
  padding: 20px 0;
  text-align: center;
  color: #94a3b8;
  font-size: 11px;
}
</style>
