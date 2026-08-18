<script setup>
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMonthlyAnalysisStore } from '@/stores/monthlyAnalysis';
import foodIcon from '@/assets/icons/food.svg';
import cafeIcon from '@/assets/icons/cafe.svg';
import shoppingIcon from '@/assets/icons/shopping-cart.svg';
import taxiIcon from '@/assets/icons/taxi.svg';
import leisureIcon from '@/assets/icons/hobby_drink.svg';

const route = useRoute();
const router = useRouter();
const analysisStore = useMonthlyAnalysisStore();

const yearMonth = computed(() => String(route.params.yearMonth || ''));
const report = computed(() => analysisStore.report);
const categoryMeta = {
  FOOD: { icon: '🍽', iconSrc: foodIcon, color: '#2457aa' },
  CAFE: { icon: '☕', iconSrc: cafeIcon, color: '#3b82f6' },
  LIVING: { icon: '🧺', color: '#13a184' },
  SHOPPING: { icon: '🛍', iconSrc: shoppingIcon, color: '#f59e0b' },
  HOBBY: { icon: '🎮', iconSrc: leisureIcon, color: '#8b5cf6' },
  TRANSPORT: { icon: '🚌', iconSrc: taxiIcon, color: '#0ea5e9' },
  OTHER: { icon: '•••', color: '#64748b' },
};

const analysisMonthLabel = computed(() => {
  const month = Number(report.value?.analysisYearMonth?.split('-')[1]);
  return Number.isFinite(month) ? `${month}월` : '지난달';
});
const targetMonthLabel = computed(() => {
  const month = Number(report.value?.targetYearMonth?.split('-')[1]);
  return Number.isFinite(month) ? `${month}월` : '이번 달';
});
const categories = computed(() =>
  [...(report.value?.spendingCategories || [])].sort(
    (a, b) => a.rank - b.rank,
  ),
);
const recommendations = computed(() =>
  [...(report.value?.recommendedCategories || [])].sort(
    (a, b) => a.rank - b.rank,
  ),
);
const savingResult = computed(() => report.value?.savingResult || null);
const savingAvailable = computed(
  () => savingResult.value?.status === 'AVAILABLE',
);
const savingDifference = computed(() =>
  Number(savingResult.value?.differenceAmount || 0),
);

async function loadReport() {
  const loadedReport = await analysisStore.loadAnalysis(yearMonth.value, {
    force: true,
  });
  if (loadedReport?.reportStatus === 'PENDING') {
    try {
      await analysisStore.markViewed();
    } catch {
      // 상세 내용은 정상 조회됐으므로 상태 저장 오류만 화면 안내로 남긴다.
    }
  }
}

onMounted(loadReport);

function formatCurrency(value, absolute = false) {
  const number = Number(value) || 0;
  return `${(absolute ? Math.abs(number) : number).toLocaleString('ko-KR')}원`;
}

function metaOf(categoryCode) {
  return categoryMeta[categoryCode] || categoryMeta.OTHER;
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push('/');
}

async function closeReport() {
  try {
    await analysisStore.closeReport();
    await router.push('/');
  } catch {
    // 스토어의 오류 메시지를 현재 화면에서 보여준다.
  }
}

function goMissions() {
  router.push({
    name: 'SavingsMissions',
    query: { from: 'analysis', yearMonth: yearMonth.value },
  });
}
</script>

<template>
  <main class="analysis-report-view">
    <header class="report-header">
      <button type="button" aria-label="뒤로 가기" @click="goBack">‹</button>
      <div>
        <small>TRIPASS AI REPORT</small>
        <h1>월간 소비 분석</h1>
      </div>
      <button
        type="button"
        class="close-icon"
        aria-label="리포트 닫기"
        :disabled="analysisStore.updatingStatus"
        @click="closeReport"
      >
        ×
      </button>
    </header>

    <section v-if="analysisStore.loading" class="report-state" role="status">
      <span class="report-spinner" />
      <b>AI 분석 리포트를 불러오고 있어요</b>
      <small>지난달 소비와 저축 결과를 확인하고 있습니다.</small>
    </section>

    <section v-else-if="analysisStore.notFound" class="report-state">
      <span class="state-icon">AI</span>
      <b>아직 생성된 리포트가 없어요</b>
      <small>거래내역 분석이 완료되면 월간 리포트가 제공됩니다.</small>
      <button type="button" @click="router.push('/')">홈으로 돌아가기</button>
    </section>

    <section v-else-if="!report" class="report-state error">
      <span class="state-icon">!</span>
      <b>리포트를 불러오지 못했어요</b>
      <small>{{ analysisStore.errorMessage }}</small>
      <button
        type="button"
        @click="loadReport"
      >
        다시 시도
      </button>
    </section>

    <template v-else>
      <section class="report-hero">
        <div class="hero-route"><i /><span>✈</span><i /></div>
        <small>{{ report.analysisYearMonth }}</small>
        <h2>{{ analysisMonthLabel }} AI 분석 리포트</h2>
        <p>여행을 더 자주 떠날 수 있도록 지난달 금융 습관을 정리했어요.</p>
        <span class="viewed-badge">AI 분석 완료</span>
      </section>

      <p v-if="analysisStore.errorMessage" class="status-warning">
        {{ analysisStore.errorMessage }}
      </p>

      <section class="report-section saving-section">
        <div class="section-title">
          <div>
            <small>SAVING RESULT</small>
            <h3>{{ analysisMonthLabel }} 저축 결과</h3>
          </div>
          <span
            v-if="savingAvailable"
            :class="savingDifference >= 0 ? 'success' : 'danger'"
          >
            {{ savingDifference >= 0 ? '목표 달성' : '목표 부족' }}
          </span>
        </div>

        <div v-if="savingAvailable" class="saving-message">
          <span>{{ savingDifference >= 0 ? '✓' : '!' }}</span>
          <div>
            <b>
              목표보다 {{ formatCurrency(savingDifference, true) }}
              {{ savingDifference >= 0 ? '더 저축했어요' : '부족했어요' }}
            </b>
            <small>{{ savingResult.resultMessage }}</small>
          </div>
        </div>
        <div v-else class="saving-unavailable">
          <span>i</span>{{ savingResult?.resultMessage }}
        </div>

        <dl v-if="savingAvailable" class="saving-metrics">
          <div>
            <dt>목표</dt>
            <dd>{{ formatCurrency(savingResult.targetAmount) }}</dd>
          </div>
          <div>
            <dt>실제 저축</dt>
            <dd>{{ formatCurrency(savingResult.actualAmount) }}</dd>
          </div>
          <div>
            <dt>{{ savingDifference >= 0 ? '초과' : '부족' }}</dt>
            <dd>{{ formatCurrency(savingDifference, true) }}</dd>
          </div>
        </dl>
      </section>

      <section class="report-section spending-section">
        <div class="section-title">
          <div>
            <small>SPENDING INSIGHT</small>
            <h3>{{ analysisMonthLabel }} 소비</h3>
          </div>
          <div class="total-spending">
            <small>총지출</small>
            <b>{{ formatCurrency(report.totalSpending) }}</b>
          </div>
        </div>

        <div class="category-list">
          <div
            v-for="category in categories"
            :key="category.categoryCode"
            class="category-item"
          >
            <span
              class="category-icon"
              :style="{
                background: `${metaOf(category.categoryCode).color}18`,
                color: metaOf(category.categoryCode).color,
              }"
            >
              <img v-if="metaOf(category.categoryCode).iconSrc" :src="metaOf(category.categoryCode).iconSrc" alt="" />
              <template v-else>{{ metaOf(category.categoryCode).icon }}</template>
            </span>
            <div class="category-detail">
              <div>
                <b>{{ category.categoryName }}</b>
                <small>{{ category.transactionCount }}건</small>
                <strong>{{ formatCurrency(category.amount) }}</strong>
              </div>
              <i>
                <span
                  :style="{
                    width: `${Math.min(100, Number(category.ratio) || 0)}%`,
                    background: metaOf(category.categoryCode).color,
                  }"
                />
              </i>
            </div>
            <b>{{ category.ratio }}%</b>
          </div>
        </div>
      </section>

      <section class="report-section coaching-section">
        <div class="section-title">
          <div>
            <small>AI COACHING</small>
            <h3>이번 달 절약 포인트</h3>
          </div>
          <span class="ai-label">AI</span>
        </div>

        <div class="coaching-summary">
          <span>✦</span>
          <p>{{ report.coachingSummary }}</p>
        </div>

        <ol class="recommendation-list">
          <li
            v-for="recommendation in recommendations"
            :key="recommendation.categoryCode"
          >
            <b>{{ recommendation.rank }}</b>
            <span
              :style="{
                background: `${metaOf(recommendation.categoryCode).color}18`,
              }"
            >
              <img v-if="metaOf(recommendation.categoryCode).iconSrc" :src="metaOf(recommendation.categoryCode).iconSrc" alt="" />
              <template v-else>{{ metaOf(recommendation.categoryCode).icon }}</template>
            </span>
            <div>
              <strong>{{ recommendation.categoryName }}</strong>
              <p>{{ recommendation.recommendationReason }}</p>
            </div>
          </li>
        </ol>

        <p class="mission-guide">
          {{ targetMonthLabel }}에는 카테고리별 절감률을 선택해 나만의 미션을
          시작할 수 있어요.
        </p>
        <button type="button" class="mission-button" @click="goMissions">
          추천 미션 보러 가기 <span>›</span>
        </button>
      </section>

      <button
        type="button"
        class="close-report-button"
        :disabled="analysisStore.updatingStatus"
        @click="closeReport"
      >
        {{ analysisStore.updatingStatus ? '처리 중...' : '이번 리포트 닫기' }}
      </button>
    </template>
  </main>
</template>

<style scoped>
.analysis-report-view {
  min-height: 100vh;
  padding-bottom: 38px;
  background: #f3f6ff;
  color: #17213a;
}
.report-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: 40px 1fr 40px;
  align-items: center;
  padding: 18px 14px 13px;
  border-bottom: 1px solid #e0e8f5;
  background: #f3f6fff2;
  backdrop-filter: blur(14px);
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
  font-size: 16px;
  font-weight: 950;
}
.report-header .close-icon {
  font-size: 23px;
}
.report-hero {
  position: relative;
  overflow: hidden;
  margin: 14px;
  padding: 26px 20px 22px;
  border-radius: 25px;
  background: linear-gradient(135deg, #173f8d 0%, #286ce0 100%);
  color: #fff;
  box-shadow: 0 15px 30px rgba(23, 63, 141, 0.24);
  animation: report-enter 0.48s ease both;
}
.report-hero::after {
  position: absolute;
  right: -52px;
  bottom: -65px;
  width: 170px;
  height: 170px;
  border: 32px solid #ffffff0d;
  border-radius: 50%;
  content: '';
}
.hero-route {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: #ffd35e;
}
.hero-route i {
  flex: 1;
  border-top: 1px dashed #ffffff55;
}
.report-hero > small {
  color: #b9d1ff;
  font-size: 9px;
  font-weight: 900;
  letter-spacing: 0.13em;
}
.report-hero h2 {
  margin-top: 5px;
  font-size: 24px;
  font-weight: 950;
  letter-spacing: -0.06em;
}
.report-hero p {
  max-width: 270px;
  margin-top: 8px;
  color: #d3e1ff;
  font-size: 11px;
  line-height: 1.55;
}
.viewed-badge {
  display: inline-flex;
  margin-top: 16px;
  padding: 6px 9px;
  border: 1px solid #ffffff25;
  border-radius: 999px;
  background: #ffffff12;
  color: #ffd35e;
  font-size: 9px;
  font-weight: 900;
}
.report-section {
  margin: 12px 14px 0;
  padding: 18px;
  border: 1px solid #dce6f5;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 9px 24px rgba(34, 62, 112, 0.08);
  animation: report-enter 0.48s ease both;
}
.spending-section {
  animation-delay: 0.08s;
}
.coaching-section {
  animation-delay: 0.16s;
}
.section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.section-title small {
  color: #f06a2a;
  font-size: 8px;
  font-weight: 950;
  letter-spacing: 0.12em;
}
.section-title h3 {
  margin-top: 3px;
  color: #173f8d;
  font-size: 17px;
  font-weight: 950;
}
.section-title > span {
  padding: 5px 8px;
  border-radius: 999px;
  font-size: 8px;
  font-weight: 900;
}
.section-title > span.success {
  background: #e2f8f1;
  color: #079179;
}
.section-title > span.danger {
  background: #fff0f0;
  color: #ef5050;
}
.saving-message,
.saving-unavailable {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
  padding: 13px;
  border-radius: 14px;
  background: #eef4ff;
}
.saving-message > span,
.saving-unavailable > span {
  display: grid;
  flex: 0 0 32px;
  height: 32px;
  place-items: center;
  border-radius: 11px;
  background: #dce9ff;
  color: #286ce0;
  font-weight: 950;
}
.saving-message b,
.saving-message small {
  display: block;
}
.saving-message b {
  color: #173f8d;
  font-size: 11px;
}
.saving-message small,
.saving-unavailable {
  margin-top: 4px;
  color: #7186aa;
  font-size: 9px;
}
.saving-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 13px;
  padding: 13px 0;
  border: 1px solid #e3eaf5;
  border-radius: 14px;
}
.saving-metrics div {
  padding: 0 4px;
  text-align: center;
}
.saving-metrics div + div {
  border-left: 1px solid #e3eaf5;
}
.saving-metrics dt {
  color: #8a9bb6;
  font-size: 8px;
}
.saving-metrics dd {
  margin-top: 4px;
  color: #173f8d;
  font-size: 11px;
  font-weight: 900;
}
.total-spending {
  text-align: right;
}
.total-spending small,
.total-spending b {
  display: block;
}
.total-spending small {
  color: #8a9bb6;
  font-size: 8px;
}
.total-spending b {
  margin-top: 3px;
  color: #17213a;
  font-size: 15px;
}
.category-list {
  display: grid;
  gap: 12px;
  margin-top: 17px;
}
.category-item {
  display: grid;
  grid-template-columns: 34px 1fr 38px;
  align-items: center;
  gap: 9px;
}
.category-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 11px;
  font-size: 15px;
}
.category-icon img {
  width: 17px;
  height: 17px;
}
.category-detail > div {
  display: flex;
  align-items: center;
  gap: 5px;
}
.category-detail b {
  color: #26334d;
  font-size: 10px;
}
.category-detail small {
  color: #9aa8bb;
  font-size: 7px;
}
.category-detail strong {
  margin-left: auto;
  color: #536887;
  font-size: 9px;
}
.category-detail > i {
  display: block;
  height: 6px;
  overflow: hidden;
  margin-top: 6px;
  border-radius: 99px;
  background: #e9eef6;
}
.category-detail > i span {
  display: block;
  height: 100%;
  border-radius: inherit;
  transition: width 0.7s ease;
}
.category-item > b {
  color: #173f8d;
  font-size: 10px;
  text-align: right;
}
.ai-label {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  background: #173f8d !important;
  color: #fff;
}
.coaching-summary {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  padding: 14px;
  border-radius: 15px;
  background: linear-gradient(135deg, #173f8d, #286ce0);
  color: #fff;
}
.coaching-summary > span {
  color: #ffd35e;
}
.coaching-summary p {
  color: #eef4ff;
  font-size: 10px;
  line-height: 1.6;
}
.recommendation-list {
  display: grid;
  gap: 9px;
  margin-top: 13px;
}
.recommendation-list li {
  display: grid;
  grid-template-columns: 22px 34px 1fr;
  align-items: center;
  gap: 8px;
  padding: 11px;
  border: 1px solid #e3eaf5;
  border-radius: 14px;
}
.recommendation-list li > b {
  display: grid;
  width: 21px;
  height: 21px;
  place-items: center;
  border-radius: 50%;
  background: #dce9ff;
  color: #286ce0;
  font-size: 9px;
}
.recommendation-list li > span {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 11px;
}
.recommendation-list li > span img {
  width: 16px;
  height: 16px;
}
.recommendation-list strong {
  color: #26334d;
  font-size: 11px;
}
.recommendation-list p {
  margin-top: 3px;
  color: #7186aa;
  font-size: 8px;
  line-height: 1.45;
}
.mission-guide {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff7e7;
  color: #9a6813;
  font-size: 9px;
  line-height: 1.5;
}
.mission-button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 11px;
  padding: 14px;
  border-radius: 14px;
  background: #173f8d;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}
.mission-button span {
  font-size: 18px;
}
.close-report-button {
  display: block;
  margin: 17px auto 0;
  padding: 10px 14px;
  color: #7b8da9;
  font-size: 10px;
  text-decoration: underline;
}
.report-state {
  display: flex;
  min-height: 72vh;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
  text-align: center;
}
.report-state b {
  margin-top: 14px;
  color: #173f8d;
  font-size: 15px;
}
.report-state small {
  margin-top: 7px;
  color: #7b8da9;
  font-size: 10px;
  line-height: 1.5;
}
.report-state button {
  margin-top: 16px;
  padding: 10px 15px;
  border-radius: 12px;
  background: #173f8d;
  color: #fff;
  font-size: 10px;
  font-weight: 900;
}
.state-icon,
.report-spinner {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 18px;
  background: #dce9ff;
  color: #286ce0;
  font-size: 14px;
  font-weight: 950;
}
.report-spinner {
  width: 38px;
  height: 38px;
  border: 4px solid #dce9ff;
  border-top-color: #286ce0;
  border-radius: 50%;
  background: transparent;
  animation: spin 0.8s linear infinite;
}
.status-warning {
  margin: 0 14px 12px;
  padding: 9px 11px;
  border-radius: 11px;
  background: #fff0f0;
  color: #d94b4b;
  font-size: 9px;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@keyframes report-enter {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@media (prefers-reduced-motion: reduce) {
  .report-hero,
  .report-section {
    animation: none;
  }
}
</style>
