<script setup>
import { computed } from 'vue';
import foodIcon from '@/assets/icons/food.svg';
import cafeIcon from '@/assets/icons/cafe.svg';
import shoppingIcon from '@/assets/icons/shopping-cart.svg';
import taxiIcon from '@/assets/icons/taxi.svg';
import leisureIcon from '@/assets/icons/hobby_drink.svg';
import livingIcon from '@/assets/icons/home-dollar.svg';
import foodIconRaw from '@/assets/icons/food.svg?raw';
import cafeIconRaw from '@/assets/icons/cafe.svg?raw';
import shoppingIconRaw from '@/assets/icons/shopping-cart.svg?raw';
import taxiIconRaw from '@/assets/icons/taxi.svg?raw';
import leisureIconRaw from '@/assets/icons/hobby_drink.svg?raw';
import livingIconRaw from '@/assets/icons/home-dollar.svg?raw';

const props = defineProps({
  report: { type: Object, required: true },
});

defineEmits(['open']);

const categoryMeta = {
  FOOD: { icon: '🍽', iconSrc: foodIcon, color: '#2457aa' },
  CAFE: { icon: '☕', iconSrc: cafeIcon, color: '#3b82f6' },
  LIVING: { icon: '🏠', iconSrc: livingIcon, color: '#13a184' },
  SHOPPING: { icon: '🛍', iconSrc: shoppingIcon, color: '#f59e0b' },
  LEISURE: { icon: '🎮', iconSrc: leisureIcon, color: '#8b5cf6' },
  TRANSPORT: { icon: '🚌', iconSrc: taxiIcon, color: '#0ea5e9' },
  OTHER: { icon: '•••', color: '#64748b' },
};

const categoryIconRaw = {
  FOOD: foodIconRaw,
  CAFE: cafeIconRaw,
  LIVING: livingIconRaw,
  SHOPPING: shoppingIconRaw,
  LEISURE: leisureIconRaw,
  TRANSPORT: taxiIconRaw,
};

const analysisMonthLabel = computed(() => {
  const month = Number(props.report.analysisYearMonth?.split('-')[1]);
  return Number.isFinite(month) ? `${month}월` : '지난달';
});
const topCategories = computed(() =>
  [...(props.report.spendingCategories || [])]
    .sort((a, b) => a.rank - b.rank)
    .slice(0, 3),
);
const savingAvailable = computed(
  () => props.report.savingResult?.status === 'AVAILABLE',
);
const savingDifference = computed(() =>
  Number(props.report.savingResult?.differenceAmount || 0),
);
const savingState = computed(() => {
  if (!savingAvailable.value) return { label: '집계 전', tone: 'neutral' };
  if (savingDifference.value >= 0) return { label: '달성', tone: 'success' };
  return { label: '부족', tone: 'danger' };
});

function formatCurrency(value) {
  return `${Math.abs(Number(value) || 0).toLocaleString('ko-KR')}원`;
}

function metaOf(categoryCode) {
  return categoryMeta[categoryCode] || categoryMeta.OTHER;
}

function darken(hex, amount = 0.3) {
  const value = Number.parseInt(String(hex || '').replace('#', ''), 16);
  if (Number.isNaN(value)) return hex;
  const channel = (shift) => Math.max(0, Math.round(((value >> shift) & 255) * (1 - amount)));
  return `rgb(${channel(16)}, ${channel(8)}, ${channel(0)})`;
}

function coloredCategoryIcon(categoryCode) {
  const raw = categoryIconRaw[categoryCode];
  return raw?.replaceAll('black', darken(metaOf(categoryCode).color)) || '';
}
</script>

<template>
  <section
    class="analysis-summary-card"
    :class="{ pending: report.reportStatus === 'PENDING' }"
  >
    <div class="summary-heading">
      <div>
        <span v-if="report.reportStatus === 'PENDING'" class="new-report-badge"
          >NEW · AI REPORT</span
        >
        <small v-else>MONTHLY AI REPORT</small>
        <h2>{{ analysisMonthLabel }} AI 소비 분석</h2>
        <p>지난달 소비와 저축 결과를 한눈에 확인해 보세요.</p>
      </div>
      <button type="button" @click="$emit('open')">상세 보기 <b>›</b></button>
    </div>

    <div class="saving-result" :class="savingState.tone">
      <div>
        <small>{{ analysisMonthLabel }} 저축 결과</small>
        <strong v-if="savingAvailable">
          목표보다
          {{ formatCurrency(savingDifference) }}
          {{ savingDifference >= 0 ? '더 저축했어요' : '부족했어요' }}
        </strong>
        <strong v-else>{{ report.savingResult?.resultMessage }}</strong>
      </div>
      <span>{{ savingState.label }}</span>
      <dl v-if="savingAvailable">
        <div>
          <dt>목표</dt>
          <dd>{{ formatCurrency(report.savingResult.targetAmount) }}</dd>
        </div>
        <div>
          <dt>저축</dt>
          <dd>{{ formatCurrency(report.savingResult.actualAmount) }}</dd>
        </div>
        <div>
          <dt>{{ savingDifference >= 0 ? '초과' : '부족' }}</dt>
          <dd>{{ formatCurrency(savingDifference) }}</dd>
        </div>
      </dl>
    </div>

    <div class="spending-title">
      <div>
        <small>{{ analysisMonthLabel }} 총지출</small>
        <strong>{{ formatCurrency(report.totalSpending) }}</strong>
      </div>
      <span>지출 TOP 3</span>
    </div>

    <div class="top-categories">
      <div
        v-for="category in topCategories"
        :key="category.categoryCode"
        class="category-row"
      >
        <b>{{ category.rank }}</b>
        <i :style="{ background: `${metaOf(category.categoryCode).color}18` }">
          <span
            v-if="categoryIconRaw[category.categoryCode]"
            class="category-icon-glyph"
            v-html="coloredCategoryIcon(category.categoryCode)"
          ></span>
          <template v-else>{{ metaOf(category.categoryCode).icon }}</template>
        </i>
        <div>
          <strong>{{ category.categoryName }}</strong>
          <small>{{ formatCurrency(category.amount) }} · {{ category.ratio }}%</small>
        </div>
        <span>{{ category.transactionCount }}건</span>
      </div>
    </div>

    <button type="button" class="analysis-open-button" @click="$emit('open')">
      <span>✦</span>
      <div>
        <small>TRIPASS AI COACH</small>
        <b>내 소비에 맞는 절약 포인트 확인하기</b>
      </div>
      <i>›</i>
    </button>
  </section>
</template>

<style scoped>
.analysis-summary-card {
  position: relative;
  overflow: hidden;
  padding: 19px;
  border: 1px solid #bfd5ff;
  border-radius: 24px;
  background: linear-gradient(145deg, #f4f8ff 0%, #eaf2ff 100%);
  box-shadow: 0 13px 30px rgba(26, 72, 154, 0.12);
}
.analysis-summary-card::after {
  position: absolute;
  top: -58px;
  right: -45px;
  width: 145px;
  height: 145px;
  border-radius: 50%;
  content: '';
  background: radial-gradient(circle, #4f86ef22 0 45%, transparent 46%);
  pointer-events: none;
}
.analysis-summary-card.pending {
  animation: analysis-card-enter 0.62s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.summary-heading {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.summary-heading small,
.new-report-badge {
  color: #f06a2a;
  font-size: 9px;
  font-weight: 950;
  letter-spacing: 0.12em;
}
.new-report-badge {
  display: inline-flex;
  padding: 5px 8px;
  border-radius: 999px;
  background: #fff0e7;
}
.summary-heading h2 {
  margin-top: 6px;
  color: #173f8d;
  font-size: 19px;
  font-weight: 950;
  letter-spacing: -0.05em;
}
.summary-heading p {
  margin-top: 4px;
  color: #7186aa;
  font-size: 10px;
}
.summary-heading button {
  flex: none;
  margin-top: 2px;
  color: #286ce0;
  font-size: 10px;
  font-weight: 900;
}
.summary-heading button b {
  font-size: 15px;
}
.saving-result {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  margin-top: 15px;
  padding: 14px;
  border: 1px solid #c9dbff;
  border-radius: 16px;
  background: #fff;
}
.saving-result > div > small {
  color: #7186aa;
  font-size: 9px;
  font-weight: 800;
}
.saving-result > div > strong {
  display: block;
  margin-top: 4px;
  color: #173f8d;
  font-size: 12px;
}
.saving-result > span {
  align-self: start;
  padding: 5px 8px;
  border-radius: 999px;
  background: #e2f8f1;
  color: #079179;
  font-size: 9px;
  font-weight: 900;
}
.saving-result.danger > span {
  background: #fff0f0;
  color: #ef5050;
}
.saving-result.neutral > span {
  background: #edf2f8;
  color: #64748b;
}
.saving-result dl {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding-top: 11px;
  border-top: 1px dashed #dbe5f5;
}
.saving-result dl div {
  text-align: center;
}
.saving-result dl div + div {
  border-left: 1px solid #e3eaf5;
}
.saving-result dt {
  color: #8a9bb6;
  font-size: 8px;
}
.saving-result dd {
  margin-top: 3px;
  color: #173f8d;
  font-size: 11px;
  font-weight: 900;
}
.spending-title {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 16px 2px 8px;
}
.spending-title small {
  display: block;
  color: #7186aa;
  font-size: 9px;
}
.spending-title strong {
  display: block;
  margin-top: 3px;
  color: #17213a;
  font-size: 18px;
  font-weight: 950;
}
.spending-title > span {
  color: #286ce0;
  font-size: 9px;
  font-weight: 900;
}
.top-categories {
  display: grid;
  gap: 7px;
}
.category-row {
  display: grid;
  grid-template-columns: 22px 32px 1fr auto;
  align-items: center;
  gap: 7px;
  padding: 9px 10px;
  border-radius: 13px;
  background: #fff;
}
.category-row > b {
  display: grid;
  width: 21px;
  height: 21px;
  place-items: center;
  border-radius: 50%;
  background: #dce9ff;
  color: #286ce0;
  font-size: 9px;
}
.category-row > i {
  display: grid;
  width: 29px;
  height: 29px;
  place-items: center;
  border-radius: 10px;
  font-size: 14px;
  font-style: normal;
}
.category-icon-glyph,
.category-icon-glyph :deep(svg) {
  display: block;
  width: 16px;
  height: 16px;
}
.category-row div strong {
  display: block;
  color: #26334d;
  font-size: 11px;
}
.category-row div small,
.category-row > span {
  color: #8494ad;
  font-size: 8px;
}
.analysis-open-button {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 9px;
  margin-top: 13px;
  padding: 12px;
  border-radius: 15px;
  background: linear-gradient(100deg, #173f8d, #286ce0);
  color: #fff;
  text-align: left;
}
.analysis-open-button > span {
  display: grid;
  width: 31px;
  height: 31px;
  place-items: center;
  border-radius: 11px;
  background: #ffffff18;
  color: #ffd35e;
}
.analysis-open-button div {
  flex: 1;
}
.analysis-open-button small {
  display: block;
  color: #b9d1ff;
  font-size: 7px;
  font-weight: 800;
  letter-spacing: 0.12em;
}
.analysis-open-button b {
  display: block;
  margin-top: 3px;
  font-size: 11px;
}
.analysis-open-button > i {
  font-size: 22px;
  font-style: normal;
}
@keyframes analysis-card-enter {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
@media (prefers-reduced-motion: reduce) {
  .analysis-summary-card.pending {
    animation: none;
  }
}
</style>
