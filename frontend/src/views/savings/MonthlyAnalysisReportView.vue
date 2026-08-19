<script setup>
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMonthlyAnalysisStore } from '@/stores/monthlyAnalysis';
import foodIcon from '@/assets/icons/food.svg';
import cafeIcon from '@/assets/icons/cafe.svg';
import shoppingIcon from '@/assets/icons/shopping-cart.svg';
import taxiIcon from '@/assets/icons/taxi.svg';
import leisureIcon from '@/assets/icons/hobby_drink.svg';
import aiIcon from '@/assets/icons/ai.svg';
import aiReportIcon from '@/assets/icons/ai_report.svg';
import foodIconRaw from '@/assets/icons/food.svg?raw';
import cafeIconRaw from '@/assets/icons/cafe.svg?raw';
import shoppingIconRaw from '@/assets/icons/shopping-cart.svg?raw';
import taxiIconRaw from '@/assets/icons/taxi.svg?raw';
import leisureIconRaw from '@/assets/icons/hobby_drink.svg?raw';

const route = useRoute();
const router = useRouter();
const analysisStore = useMonthlyAnalysisStore();

const yearMonth = computed(() => String(route.params.yearMonth || ''));
const report = computed(() => analysisStore.report);
const categoryMeta = {
  FOOD: { icon: '🍽', iconSrc: foodIcon, iconRaw: foodIconRaw, color: '#2457aa' },
  CAFE: { icon: '☕', iconSrc: cafeIcon, iconRaw: cafeIconRaw, color: '#3b82f6' },
  LIVING: { icon: '🧺', color: '#13a184' },
  SHOPPING: { icon: '🛍', iconSrc: shoppingIcon, iconRaw: shoppingIconRaw, color: '#f59e0b' },
  LEISURE: { icon: '🎮', iconSrc: leisureIcon, iconRaw: leisureIconRaw, color: '#8b5cf6' },
  TRANSPORT: { icon: '🚌', iconSrc: taxiIcon, iconRaw: taxiIconRaw, color: '#0ea5e9' },
  OTHER: { icon: '•••', color: '#64748b' },
};

const analysisMonthLabel = computed(() => {
  const month = Number(report.value?.analysisYearMonth?.split('-')[1]);
  return Number.isFinite(month) ? `${month}월` : '지난달';
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
    generateIfMissing: true,
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

// 카테고리 아이콘 색을 해당 카테고리 배경색보다 조금 더 진하게 만든다.
function darken(hex, amount = 0.3) {
  const clean = (hex || '').replace('#', '');
  const num = parseInt(clean, 16);
  if (Number.isNaN(num)) return hex;

  const r = Math.max(0, Math.round(((num >> 16) & 255) * (1 - amount)));
  const g = Math.max(0, Math.round(((num >> 8) & 255) * (1 - amount)));
  const b = Math.max(0, Math.round((num & 255) * (1 - amount)));

  return `rgb(${r}, ${g}, ${b})`;
}

// 카테고리 SVG 아이콘의 black 채우기/선 색을 카테고리 색보다 진하게 바꿔 넣는다.
function coloredCategoryIcon(categoryCode) {
  const meta = metaOf(categoryCode);
  if (!meta.iconRaw) return '';
  return meta.iconRaw.replaceAll('black', darken(meta.color));
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
        <h1>
          {{ analysisMonthLabel }} AI 분석 리포트
          <img class="header-ai-icon" :src="aiReportIcon" alt="" aria-hidden="true" />
        </h1>
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
      <p v-if="analysisStore.errorMessage" class="status-warning">
        {{ analysisStore.errorMessage }}
      </p>

      <article class="report-paper">
        <div class="paper-letterhead">
          <div class="letterhead-left">
            <strong class="letterhead-title">{{ analysisMonthLabel }} AI 분석 리포트</strong>
            <img class="letterhead-report-icon" :src="aiReportIcon" alt="" aria-hidden="true" />
          </div>
          <div class="letterhead-right">
            <span class="ai-label"><img :src="aiIcon" alt="" /></span>
          </div>
        </div>

      <section class="paper-section saving-section">
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
          <span>i</span>
          <b>{{ savingResult?.resultMessage || '이번 달 저축 목표가 설정되지 않아 결과를 계산할 수 없어요.' }}</b>
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

      <section class="paper-section spending-section">
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
              <span
                v-if="metaOf(category.categoryCode).iconSrc"
                class="category-icon-glyph"
                v-html="coloredCategoryIcon(category.categoryCode)"
              ></span>
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
                  }"
                />
              </i>
            </div>
            <b>{{ category.ratio }}%</b>
          </div>
        </div>
      </section>

      <section class="paper-section coaching-section">
        <div class="section-title">
          <div>
            <small>AI COACHING</small>
            <h3>이번 달 절약 포인트</h3>
          </div>
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
              <span
                v-if="metaOf(recommendation.categoryCode).iconSrc"
                class="category-icon-glyph"
                v-html="coloredCategoryIcon(recommendation.categoryCode)"
              ></span>
              <template v-else>{{ metaOf(recommendation.categoryCode).icon }}</template>
            </span>
            <div>
              <strong>{{ recommendation.categoryName }}</strong>
              <p>{{ recommendation.recommendationReason }}</p>
            </div>
          </li>
        </ol>

        <p class="mission-guide">
          <span class="sparkle" aria-hidden="true">✦</span>
          카테고리별 절감률을 선택해 나만의 미션을 시작할 수 있어요.
        </p>
        <button type="button" class="mission-button" @click="goMissions">
          추천 미션 보러 가기 <span>›</span>
        </button>
      </section>
      </article>
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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: 2px;
  color: #173f8d;
  font-size: 15px;
  font-weight: 950;
}
.header-ai-icon {
  width: 14px;
  height: 14px;
  object-fit: contain;
  filter: invert(34%) sepia(94%) saturate(1272%) hue-rotate(199deg) brightness(91%);
}
.report-header .close-icon {
  font-size: 23px;
}
.report-paper {
  overflow: hidden;
  margin: 14px;
  border-radius: 25px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(23, 63, 141, 0.16);
  animation: report-enter 0.48s ease both;
}
.paper-letterhead {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #173f8d 0%, #286ce0 100%);
  color: #fff;
}
.paper-letterhead::after {
  position: absolute;
  right: -52px;
  bottom: -65px;
  width: 170px;
  height: 170px;
  border: 32px solid #ffffff0d;
  border-radius: 50%;
  content: '';
  pointer-events: none;
}
.letterhead-left {
  position: relative;
  z-index: 1;
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}
.letterhead-title {
  overflow: hidden;
  color: #fff;
  font-size: 12.5px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
  letter-spacing: -0.02em;
}
.letterhead-report-icon {
  width: 12px;
  height: 12px;
  flex: none;
  object-fit: contain;
  filter: invert(76%) sepia(59%) saturate(551%) hue-rotate(357deg) brightness(103%) contrast(101%);
}
.letterhead-right {
  position: relative;
  z-index: 1;
  display: flex;
  flex: none;
  align-items: center;
  gap: 8px;
}
.letterhead-right .ai-label {
  position: relative;
  z-index: 1;
  flex: none;
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.16) !important;
  animation: ai-label-pulse-light 2s ease-in-out infinite;
}
.letterhead-right .ai-label img {
  width: 18px;
  height: 18px;
  filter: invert(76%) sepia(59%) saturate(551%) hue-rotate(357deg) brightness(103%) contrast(101%);
}
.paper-section {
  padding: 20px;
}
.paper-section + .paper-section {
  border-top: 1px solid #eef1f8;
}
.section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.section-title small {
  color: #f06a2a;
  font-size: 9px;
  font-weight: 950;
  letter-spacing: 0.12em;
}
.section-title h3 {
  margin-top: 3px;
  color: #173f8d;
  font-size: 18px;
  font-weight: 950;
}
.section-title > span {
  padding: 5px 8px;
  border-radius: 999px;
  font-size: 9px;
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
  font-size: 13px;
}
.saving-message small {
  margin-top: 4px;
  color: #7186aa;
  font-size: 10.5px;
}
.saving-unavailable b {
  color: #173f8d;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.5;
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
  font-size: 9px;
}
.saving-metrics dd {
  margin-top: 4px;
  color: #173f8d;
  font-size: 13px;
  font-weight: 900;
}
.total-spending {
  display: flex;
  align-items: baseline;
  gap: 5px;
}
.total-spending small {
  color: #8a9bb6;
  font-size: 9.5px;
}
.total-spending b {
  color: #17213a;
  font-size: 16px;
}
.category-list {
  display: grid;
  gap: 12px;
  margin-top: 17px;
}
.category-item {
  display: grid;
  grid-template-columns: 38px 1fr 42px;
  align-items: center;
  gap: 10px;
}
.category-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 12px;
  font-size: 16px;
}
.category-icon-glyph {
  display: block;
  width: 18px;
  height: 18px;
}
.category-icon-glyph :deep(svg) {
  display: block;
  width: 100%;
  height: 100%;
}
.category-detail > div {
  display: flex;
  align-items: center;
  gap: 5px;
}
.category-detail b {
  color: #26334d;
  font-size: 13px;
  font-weight: 800;
}
.category-detail small {
  color: #9aa8bb;
  font-size: 9.5px;
}
.category-detail strong {
  margin-left: auto;
  color: #536887;
  font-size: 11px;
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
  background: linear-gradient(90deg, #0b2a6b, #2f70d9);
  transition: width 0.7s ease;
}
.category-item > b {
  color: #173f8d;
  font-size: 12px;
  text-align: right;
}
.ai-label {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 10px;
  background: #173f8d !important;
  color: #fff;
  box-shadow: 0 0 0 0 rgba(23, 63, 141, 0.35);
  animation: ai-label-pulse 2s ease-in-out infinite;
}
.ai-label img {
  width: 15px;
  height: 15px;
  object-fit: contain;
  filter: brightness(0) invert(1);
}
.recommendation-list {
  display: grid;
  gap: 9px;
  margin-top: 13px;
}
.recommendation-list li {
  display: grid;
  grid-template-columns: 24px 38px 1fr;
  align-items: center;
  gap: 9px;
  padding: 12px;
  border: 1px solid #e3eaf5;
  border-radius: 14px;
}
.recommendation-list li > b {
  display: grid;
  width: 23px;
  height: 23px;
  place-items: center;
  border-radius: 50%;
  background: #dce9ff;
  color: #286ce0;
  font-size: 10px;
}
.recommendation-list li > span {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 12px;
}
.recommendation-list strong {
  color: #26334d;
  font-size: 13px;
}
.recommendation-list p {
  margin-top: 3px;
  color: #7186aa;
  font-size: 9.5px;
  line-height: 1.45;
}
.mission-guide {
  margin-top: 14px;
  color: #78869f;
  font-size: 11.5px;
  line-height: 1.6;
  word-break: keep-all;
}
.mission-guide .sparkle {
  margin-right: 3px;
  color: #f0a93c;
  font-size: 16px;
  animation: sparkle-twinkle 1.8s ease-in-out infinite;
}
.mission-button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 11px;
  padding: 12px 16px;
  border-radius: 12px;
  background: #173f8d;
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  animation: mission-button-pulse 2.4s ease-out infinite;
}
.mission-button span {
  font-size: 15px;
}
@keyframes mission-button-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(23, 63, 141, 0.35);
  }
  70%,
  100% {
    box-shadow: 0 0 0 9px rgba(23, 63, 141, 0);
  }
}
@media (prefers-reduced-motion: reduce) {
  .mission-button {
    animation: none;
  }
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
  font-size: 16px;
}
.report-state small {
  margin-top: 7px;
  color: #7b8da9;
  font-size: 11.5px;
  line-height: 1.5;
}
.report-state button {
  margin-top: 16px;
  padding: 11px 16px;
  border-radius: 12px;
  background: #173f8d;
  color: #fff;
  font-size: 12px;
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
  font-size: 10.5px;
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
@keyframes ai-label-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(23, 63, 141, 0.35);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(23, 63, 141, 0);
  }
}
@keyframes ai-label-pulse-light {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(255, 255, 255, 0.4);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(255, 255, 255, 0);
  }
}
@keyframes sparkle-twinkle {
  0%,
  100% {
    opacity: 0.6;
    transform: scale(0.85) rotate(0deg);
  }
  50% {
    opacity: 1;
    transform: scale(1.2) rotate(90deg);
  }
}
@media (prefers-reduced-motion: reduce) {
  .report-paper,
  .ai-label,
  .sparkle {
    animation: none;
  }
}
</style>
