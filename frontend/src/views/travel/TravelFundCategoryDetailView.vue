<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useTravelModeStore } from '@/stores/travelMode';
import { useTravelFundStore } from '@/stores/travelFund';
import { useTravelStore } from '@/stores/travel'; // 추가
import { fetchTripTransactions } from '@/api/travel'; // 추가

const route = useRoute();
const router = useRouter();
const travelMode = useTravelModeStore();
const fund = useTravelFundStore();
const travel = useTravelStore(); // 추가

const transactions = ref([]); // ref로 변경

const category = computed(
  () => fund.getCategory(route.params.categoryId) ?? fund.categories[0],
);

// 데이터 가져오는 함수
const loadTransactions = async () => {
  const tripId = travel.tripId; // 활성 여행 ID 사용
  if (!tripId || !travel.activeTrip) return; // 활성 여행 정보가 없으면 대기

  // route.query에서 tripCountryId를 직접 가져옴
  const countryId = route.query.tripCountryId || null;
  const categoryName = category.value.name === '취미·여가' ? '취미여가' : category.value.name;

  

  try {
    transactions.value = await fetchTripTransactions(
      tripId,
      countryId,
      categoryName,
    );
  } catch (error) {
    console.error('거래 내역 조회 실패:', error);
    transactions.value = [];
  }
};

// 마운트 시 및 category/destination 변경 시 데이터 다시 불러오기
onMounted(async () => {
  if (!travel.tripId) await travel.loadActiveGoal(); // tripId가 없으면 로드 시도
  loadTransactions();
});
watch([() => travelMode.selectedDestination, category], loadTransactions);

const total = computed(() =>
  transactions.value.reduce((sum, item) => sum + item.amount, 0),
);
const country = computed(() =>
  travelMode.selectedDestination === 'all'
    ? null
    : fund.getCountry(travelMode.selectedDestination),
);
const periods = computed(() =>
  country.value
    ? [fund.period(country.value.code)]
    : fund.countries.map((item) => fund.period(item.code)),
);
const dateRange = computed(() => {
  const starts = periods.value.map((item) => item.startDate).sort();
  const ends = periods.value.map((item) => item.endDate).sort();
  return `${starts[0]} ~ ${ends.at(-1)}`;
});
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`;
const dateLabel = (value) =>
  new Intl.DateTimeFormat('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    weekday: 'short',
  }).format(new Date(`${value}T00:00:00`));
</script>

<template>
  <main class="detail-page">
    <header>
      <button type="button" @click="router.back()">‹</button>
      <h1>{{ category.name }} 상세</h1>
    </header>
    <section class="category-summary" :style="{ '--accent': category.color }">
      <div class="title">
        <span>{{ category.icon }}</span>
        <div>
          <b
            >{{
              country ? `${country.flag} ${country.name}` : '🌍 전체 여행'
            }}
            · {{ category.name }}</b
          ><small>{{ dateRange }}</small>
        </div>
      </div>
      <div class="total">
        <span>여행 기간 사용 금액</span><strong>{{ money(total) }}</strong>
      </div>
      <p>등록한 여행 기간에 발생한 거래만 반영했어요.</p>
    </section>
    <div class="list-title">
      <h2>거래 내역</h2>
      <b>{{ transactions.length }}건</b>
    </div>
    <section class="transaction-list">
      <button
        v-for="item in transactions"
        :key="item.transactionId"
        type="button"
        @click="router.push(`/travel/funds/transactions/${item.transactionId}`)"
      >
        <span class="merchant-icon">{{ item.categoryName?.[0] || '?' }}</span>
        <span class="merchant"
          ><small>{{ dateLabel(item.transactionDate) }}</small
          ><b>{{ item.merchantName }}</b></span
        >
        <span class="amount"
          ><b>-{{ money(item.amount) }}</b
          ><small>상세보기 ›</small></span
        >
      </button>
      <p v-if="!transactions.length" class="empty">
        선택한 여행 기간의 거래 내역이 없어요.
      </p>
    </section>
  </main>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 14px 16px 30px;
  background: #f8f6f1;
  color: #151f33;
}
header {
  display: grid;
  grid-template-columns: 30px 1fr;
  align-items: center;
}
header button {
  font-size: 28px;
  text-align: left;
}
h1 {
  font-size: 19px;
  font-weight: 900;
}
.category-summary {
  margin-top: 17px;
  padding: 17px 15px;
  border: 1px solid color-mix(in srgb, var(--accent) 24%, white);
  border-radius: 17px;
  background: color-mix(in srgb, var(--accent) 7%, white);
  box-shadow: 0 7px 16px #1e346212;
}
.title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.title > span {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 50%;
  background: #fff;
  font-size: 18px;
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
  font-size: 23px;
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
  border: 1px solid #e5eaf2;
  border-radius: 20px;
  background: #fff;
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
  border-radius: 50%;
  background: #f1eaff;
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
