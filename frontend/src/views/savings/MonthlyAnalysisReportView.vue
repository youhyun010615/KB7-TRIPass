<script setup>
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMonthlyAnalysisStore } from '@/stores/monthlyAnalysis';
import foodIcon from '@/assets/icons/food.svg';
import cafeIcon from '@/assets/icons/cafe.svg';
import shoppingIcon from '@/assets/icons/shopping-cart.svg';
import taxiIcon from '@/assets/icons/taxi.svg';
import leisureIcon from '@/assets/icons/hobby_drink.svg';
import livingIcon from '@/assets/icons/home-dollar.svg';
import graphIcon from '@/assets/icons/graph-bar.svg';
import foodIconRaw from '@/assets/icons/food.svg?raw';
import cafeIconRaw from '@/assets/icons/cafe.svg?raw';
import shoppingIconRaw from '@/assets/icons/shopping-cart.svg?raw';
import taxiIconRaw from '@/assets/icons/taxi.svg?raw';
import leisureIconRaw from '@/assets/icons/hobby_drink.svg?raw';
import livingIconRaw from '@/assets/icons/home-dollar.svg?raw';

const route = useRoute();
const router = useRouter();
const analysisStore = useMonthlyAnalysisStore();

const yearMonth = computed(() => String(route.params.yearMonth || ''));
const report = computed(() => analysisStore.report);
const categoryMeta = {
  FOOD: { icon: '🍽', iconSrc: foodIcon, iconRaw: foodIconRaw, color: '#2457aa' },
  CAFE: { icon: '☕', iconSrc: cafeIcon, iconRaw: cafeIconRaw, color: '#3b82f6' },
  LIVING: { icon: '🏠', iconSrc: livingIcon, iconRaw: livingIconRaw, color: '#13a184' },
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

function goCategoryDetail(categoryCode) {
  router.push({
    name: 'MonthlyAnalysisCategoryDetail',
    params: { yearMonth: yearMonth.value, categoryCode },
  });
}
</script>

<template>
  <main class="analysis-report-view">
    <div class="report-backdrop" @click="closeReport"></div>
    <div class="report-modal">
      <header class="report-header">
        <span class="drag-handle" aria-hidden="true"></span>
        <div class="report-header-row">
          <div>
            <small>TRIPASS AI REPORT</small>
            <h1>{{ analysisMonthLabel }} AI 분석 리포트</h1>
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
        </div>
      </header>

      <div class="report-modal-body">
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
        <div v-else class="saving-empty">
          <span aria-hidden="true"><img :src="graphIcon" alt="" /></span>
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
        <div class="spending-title">
          <div class="spending-title-top">
            <small>SPENDING INSIGHT</small>
            <small>총지출</small>
          </div>
          <div class="spending-title-bottom">
            <h3>{{ analysisMonthLabel }} 소비</h3>
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
        <p class="coaching-hint">각 카테고리를 클릭하면 AI가 분석한 상세 내용을 확인할 수 있어요.</p>

        <ol class="recommendation-list">
          <li
            v-for="recommendation in recommendations"
            :key="recommendation.categoryCode"
            role="button"
            tabindex="0"
            @click="goCategoryDetail(recommendation.categoryCode)"
            @keydown.enter="goCategoryDetail(recommendation.categoryCode)"
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
            <span class="detail-chevron" aria-hidden="true">›</span>
          </li>
        </ol>

        <p class="mission-guide">
          <span class="sparkle" aria-hidden="true">✦</span>
          카테고리별 절감률을 선택해 나만의 미션을 시작해요.
        </p>
        <button type="button" class="mission-button" @click="goMissions">
          추천 미션 보러 가기 <span>›</span>
        </button>
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
  display: flex;
  flex-direction: column;
  padding: 10px 20px 18px;
  border-bottom: 1px solid #e0e8f5;
  border-radius: 24px 24px 0 0;
  background: #f3f6ff;
}
.drag-handle {
  align-self: center;
  width: 36px;
  height: 4px;
  margin-bottom: 16px;
  border-radius: 999px;
  background: #d7deea;
}
.report-header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.report-header small {
  color: #f06a2a;
  font-size: 10px;
  font-weight: 950;
  letter-spacing: 0.14em;
}
.report-header h1 {
  margin-top: 4px;
  color: #17213a;
  font-size: 20px;
  font-weight: 900;
}
.report-header .close-icon {
  display: grid;
  flex: none;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  background: #eef1f6;
  color: #1a2338;
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
}
.report-paper {
  overflow: hidden;
  margin: 14px;
  border-radius: 25px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(23, 63, 141, 0.16);
  animation: report-enter 0.48s ease both;
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
.section-title small,
.spending-title small {
  color: #f06a2a;
  font-size: 9px;
  font-weight: 950;
  letter-spacing: 0.12em;
}
.section-title h3,
.spending-title h3 {
  margin-top: 3px;
  color: #173f8d;
  font-size: 15px;
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
.saving-message {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}
.saving-message > span {
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
.saving-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding: 22px 16px;
  border: 1px dashed #d7deea;
  border-radius: 14px;
  text-align: center;
}
.saving-empty span {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  font-size: 20px;
}
.saving-empty span img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}
.saving-empty b {
  color: #7186aa;
  font-size: 11.5px;
  font-weight: 700;
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
.spending-title-top,
.spending-title-bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}
.spending-title-bottom {
  margin-top: 3px;
}
.spending-title-bottom h3 {
  margin-top: 0;
}
.spending-title-bottom b {
  color: #17213a;
  font-size: 16px;
  font-weight: 900;
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
  flex-wrap: nowrap;
  align-items: center;
  gap: 5px;
  overflow: hidden;
}
.category-detail b {
  overflow: hidden;
  min-width: 0;
  color: #26334d;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.category-detail small {
  flex: none;
  color: #9aa8bb;
  font-size: 9.5px;
  white-space: nowrap;
}
.category-detail strong {
  flex: none;
  margin-left: auto;
  color: #536887;
  font-size: 11px;
  white-space: nowrap;
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
  grid-template-columns: 24px 38px 1fr 12px;
  align-items: center;
  gap: 9px;
  padding: 12px;
  border: 1px solid #e3eaf5;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.15s ease;
}
.recommendation-list li:active {
  background: #f2f6ff;
}
.recommendation-list .detail-chevron {
  display: block;
  width: auto;
  height: auto;
  border-radius: 0;
  background: none;
  color: #b7c3dc;
  font-size: 15px;
  font-weight: 700;
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
.coaching-hint {
  margin-top: 4px;
  color: #9aa8bb;
  font-size: 9.5px;
  line-height: 1.5;
  word-break: keep-all;
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
