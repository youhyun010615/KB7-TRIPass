<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useTravelFundStore } from '@/stores/travelFund';
import { useTravelStore } from '@/stores/travel';
import { fetchTripTransactions } from '@/api/travel';
import foodIconRaw from '@/assets/icons/food.svg?raw';
import cafeIconRaw from '@/assets/icons/cafe.svg?raw';
import shoppingIconRaw from '@/assets/icons/shopping-cart.svg?raw';
import taxiIconRaw from '@/assets/icons/taxi.svg?raw';
import leisureIconRaw from '@/assets/icons/hobby_drink.svg?raw';
import livingIconRaw from '@/assets/icons/home-dollar.svg?raw';

const route = useRoute();
const router = useRouter();
const fund = useTravelFundStore();
const travel = useTravelStore();

const transactions = ref([]);
const isLoading = ref(true);
const errorMessage = ref('');

const category = computed(() => {
  const stored = fund.getCategory(Number(route.params.categoryId));
  if (stored) return stored;
  return { name: String(route.query.categoryName || '기타'), icon: '💳', color: '#2f6fed' };
});

const requestedCategoryName = computed(() =>
  String(route.query.categoryName || category.value.name).replace('취미·여가', '취미여가'),
);
const displayCategoryName = computed(() =>
  String(route.query.categoryName || category.value.name).replace('취미여가', '취미·여가'),
);
const categoryPresentations = {
  식비: { color: '#e0613d', soft: '#fff0ec', iconRaw: foodIconRaw },
  교통: { color: '#3478e5', soft: '#edf4ff', iconRaw: taxiIconRaw },
  쇼핑: { color: '#7449ad', soft: '#f3effd', iconRaw: shoppingIconRaw },
  카페: { color: '#a66c12', soft: '#fff5e8', iconRaw: cafeIconRaw },
  생활비: { color: '#19a88b', soft: '#e7f6f5', iconRaw: livingIconRaw },
  '취미·여가': { color: '#8b5cf6', soft: '#f3effd', iconRaw: leisureIconRaw },
  기타: { color: '#718096', soft: '#f0f3f8', iconRaw: null },
};
const categoryPresentation = computed(() =>
  categoryPresentations[displayCategoryName.value] || categoryPresentations.기타,
);
const coloredCategoryIcon = computed(() =>
  categoryPresentation.value.iconRaw?.replaceAll('black', categoryPresentation.value.color) || '',
);

// 데이터 가져오는 함수
const loadTransactions = async () => {
  const tripId = travel.tripId;
  if (!tripId) return;

  isLoading.value = true;
  errorMessage.value = '';
  try {
    const result = await fetchTripTransactions(
      tripId,
      route.query.tripCountryId || null,
      requestedCategoryName.value,
    );
    transactions.value = (Array.isArray(result) ? result : []).sort((a, b) => {
      const dateCompare = String(b.transactionDate || '').localeCompare(String(a.transactionDate || ''));
      if (dateCompare !== 0) return dateCompare;
      return String(b.transactionTime || '').localeCompare(String(a.transactionTime || ''));
    });
  } catch (error) {
    console.error('거래 내역 조회 실패:', error);
    transactions.value = [];
    errorMessage.value = '거래 내역을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.';
  } finally {
    isLoading.value = false;
  }
};

onMounted(async () => {
  if (!travel.tripId) await travel.loadActiveGoal();
  await loadTransactions();
});
watch(
  [() => route.params.categoryId, () => route.query.tripCountryId, requestedCategoryName],
  loadTransactions,
);

const total = computed(() =>
  transactions.value.reduce((sum, item) => sum + Number(item.amount || 0), 0),
);
const foreignTotalText = computed(() => {
  const totals = new Map();
  transactions.value.forEach(item => {
    const code = item.currencySymbol || 'EUR';
    const amount = Number(item.originalAmount ?? (
      Number(item.amount || 0) / Number(item.appliedExchangeRate || 1)
    ));
    totals.set(code, (totals.get(code) || 0) + amount);
  });
  return [...totals]
    .map(([code, amount]) => `${code} ${amount.toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`)
    .join(' · ');
});
const countryName = computed(() => String(route.query.countryName || '전체 여행'));
const dateRange = computed(() => {
  const start = route.query.startDate;
  const end = route.query.endDate;
  return start && end ? `${start} ~ ${end}` : '여행 기간 전체';
});
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`;
const foreignMoney = (item) => {
  const code = item.currencySymbol || 'EUR';
  const amount = Number(item.originalAmount ?? (
    Number(item.amount || 0) / Number(item.appliedExchangeRate || 1)
  ));
  return `${code} ${amount.toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
};
const dateLabel = (value) =>
  value ? new Intl.DateTimeFormat('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    weekday: 'short',
  }).format(new Date(`${value}T00:00:00`)) : '-';
</script>

<template>
  <main class="detail-page">
    <header>
      <button type="button" @click="router.back()">‹</button>
      <h1>{{ displayCategoryName }} 상세 거래내역</h1>
      <span aria-hidden="true"></span>
    </header>
    <section
      class="category-summary"
      :style="{
        '--accent': categoryPresentation.color,
        '--accent-soft': categoryPresentation.soft,
      }"
    >
      <div class="title">
        <span class="category-icon">
          <i v-if="coloredCategoryIcon" v-html="coloredCategoryIcon"></i>
          <b v-else aria-hidden="true">•••</b>
        </span>
        <div>
          <b
            >{{ countryName === '전체 여행' ? '🌍 전체 여행' : countryName }}
            · {{ displayCategoryName }}</b
          ><small>{{ dateRange }}</small>
        </div>
      </div>
      <div class="total">
        <span>여행 기간 사용 합산 금액</span><strong>{{ foreignTotalText }}</strong>
      </div>
      <p>등록한 여행 기간에 발생한 거래만 반영했어요.</p>
    </section>
    <div class="list-title">
      <h2>거래 내역</h2>
      <b>{{ transactions.length }}건</b>
    </div>
    <section class="transaction-list">
      <p v-if="isLoading" class="empty">거래 내역을 불러오는 중이에요.</p>
      <button
        v-for="item in isLoading ? [] : transactions"
        :key="item.transactionId"
        type="button"
        @click="router.push(`/travel/funds/transactions/${item.transactionId}`)"
      >
        <span class="merchant-icon" :style="{ background: categoryPresentation.soft }">
          <i v-if="coloredCategoryIcon" v-html="coloredCategoryIcon"></i>
          <b v-else aria-hidden="true">•••</b>
        </span>
        <span class="merchant"
          ><small>{{ dateLabel(item.transactionDate) }}</small
          ><b>{{ item.merchantName }}</b></span
        >
        <span class="amount"
          ><b>-{{ foreignMoney(item) }}</b
          ><small>상세보기 ›</small></span
        >
      </button>
      <p v-if="!isLoading && errorMessage" class="empty error">{{ errorMessage }}</p>
      <p v-else-if="!isLoading && !transactions.length" class="empty">
        선택한 여행 기간의 거래 내역이 없어요.
      </p>
    </section>
  </main>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 14px 16px 30px;
  background: #f3f6fc;
  color: #151f33;
}
header {
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: center;
}
header button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  background: #fff;
  color: #193d82;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 5px 16px rgba(36, 72, 117, 0.07);
}
h1 {
  text-align: center;
  font-size: 19px;
  font-weight: 900;
}
.category-summary {
  margin-top: 17px;
  padding: 17px 15px;
  border: 1px solid color-mix(in srgb, var(--accent) 18%, white);
  border-radius: 17px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(31, 64, 119, 0.08);
}
.title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.category-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 13px;
  background: var(--accent-soft);
}
.category-icon i,
.merchant-icon i {
  display: block;
  width: 20px;
  height: 20px;
}
.category-icon i :deep(svg),
.merchant-icon i :deep(svg) {
  display: block;
  width: 100%;
  height: 100%;
}
.category-icon b,
.merchant-icon b {
  color: var(--accent, #718096);
  font-size: 13px;
  letter-spacing: 1px;
}
.title div > * {
  display: block;
}
.title b {
  font-size: 14px;
}
.title small {
  margin-top: 4px;
  color: #64748b;
  font-size: 9px;
}
.total {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed #cbd5e1;
}
.total span {
  color: #64748b;
  font-size: 10px;
}
.total strong {
  color: var(--accent);
  font-size: 20px;
}
.category-summary p {
  margin-top: 9px;
  color: #64748b;
  font-size: 9px;
}
.list-title {
  display: flex;
  justify-content: space-between;
  margin: 27px 4px 11px;
}
.list-title h2 {
  font-size: 16px;
  font-weight: 900;
}
.list-title b {
  color: #94a3b8;
  font-size: 10px;
}
.transaction-list {
  padding: 12px;
  border: 1px solid #dbe5f4;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(31, 64, 119, 0.06);
}
.transaction-list button {
  display: grid;
  grid-template-columns: 42px 1fr auto;
  align-items: center;
  width: 100%;
  margin-bottom: 9px;
  padding: 12px;
  border: 1px solid #e5eaf2;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 4px 10px #1e34620d;
  text-align: left;
}
.merchant-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 11px;
}
.merchant small,
.merchant b,
.amount b,
.amount small {
  display: block;
}
.merchant small {
  color: #94a3b8;
  font-size: 9px;
}
.merchant b {
  margin-top: 4px;
  font-size: 13px;
}
.amount {
  text-align: right;
}
.amount b {
  font-size: 13px;
}
.amount small {
  margin-top: 5px;
  color: #64748b;
  font-size: 9px;
}
.empty {
  padding: 40px 10px;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
}
</style>
