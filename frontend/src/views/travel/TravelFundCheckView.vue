<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import { useTravelModeStore } from '@/stores/travelMode';
import { useTravelStore } from '@/stores/travel';

const router = useRouter();
const travelMode = useTravelModeStore();
const travel = useTravelStore();

const categoryConfig = {
  식비: { key: 'food', icon: '🍴', color: '#2378ea' },
  교통: { key: 'transport', icon: '🚆', color: '#ea5455' },
  숙박: { key: 'lodging', icon: '🏨', color: '#ffb400' },
  카페: { key: 'cafe', icon: '☕', color: '#7248df' },
  생활비: { key: 'living', icon: '📦', color: '#22ad6f' },
  쇼핑: { key: 'shopping', icon: '🛍️', color: '#ef3b86' },
  취미여가: { key: 'hobby', icon: '🎨', color: '#ff912f' },
  기타: { key: 'other', icon: '•••', color: '#98a7ba' },
};

const defaultCategories = [
  { key: 'food', name: '식비', icon: '🍴', color: '#2378ea' },
  { key: 'transport', name: '교통', icon: '🚆', color: '#ea5455' },
  { key: 'lodging', name: '숙박', icon: '🏨', color: '#ffb400' },
  { key: 'cafe', name: '카페', icon: '☕', color: '#7248df' },
  { key: 'living', name: '생활비', icon: '📦', color: '#22ad6f' },
  { key: 'shopping', name: '쇼핑', icon: '🛍️', color: '#ef3b86' },
  { key: 'hobby', name: '취미·여가', icon: '🎨', color: '#ff912f' },
  { key: 'other', name: '기타', icon: '•••', color: '#98a7ba' },
];

const countryImageMap = {
  '프랑스': '/images/france.png',
  '독일': '/images/germany.png',
  '스위스': '/images/switzerland.webp',
  '일본': '/images/japan.webp',
  '홍콩': '/images/Hong Kong.png',
};

const loadAllData = async () => {
  if (!travel.tripId) await travel.loadActiveGoal();
  if (!travel.tripId) return;
  await travel.loadBudgetCheck(travel.tripId);
};

onMounted(async () => {
  await loadAllData();
});

const activeCountries = computed(() => {
  if (!travel.activeTrip || !travel.activeTrip.countries) return [];
  return travel.activeTrip.countries.map((c) => {
    const found = travel.countries.find(
      (tc) => tc.countryId === c.countryId,
    ) || {
      name: c.countryName,
      code: c.currencyCode,
      city: c.countryName,
      flag: '✈️',
      accent: '#174b5d',
    };
    return {
      ...c,
      code: found.code,
      name: found.name,
      city: found.city,
      flag: found.flag,
      flagClass: travel.countryFlagMap[found.name]?.class || '',
      image: countryImageMap[found.name] || '',
      theme: found.accent,
    };
  });
});

const options = computed(() => [
  {
    code: 'all',
    name: '전체',
    city: '전체 여행',
    flag: '🌍',
    flagClass: '',
    image: '',
    theme: '#174b5d',
  },
  ...activeCountries.value,
]);
const selected = computed(
  () =>
    options.value.find(
      (item) => item.code === travelMode.selectedDestination,
    ) ?? options.value[0],
);

const aggregatedBudget = computed(() => travel.aggregatedBudget);

const selectedBudget = computed(() => {
  if (selected.value.code === 'all') return aggregatedBudget.value;
  return (
    travel.budgetCheckData?.find(
      (b) => b.tripCountryId === selected.value.tripCountryId,
    ) || null
  );
});

const usable = computed(
  () =>
    (selectedBudget.value?.targetBudget || 0) -
    (selectedBudget.value?.preExpenseTotal || 0),
);
const spent = computed(() => selectedBudget.value?.travelExpenseTotal || 0);
const balance = computed(() => selectedBudget.value?.remainingFund || 0);

const countryRows = computed(() => {
  if (!travel.budgetCheckData || !Array.isArray(travel.budgetCheckData))
    return [];
  if (selected.value.code === 'all') return travel.budgetCheckData;
  return travel.budgetCheckData.filter(
    (b) => b.tripCountryId === selected.value.tripCountryId,
  );
});

const categoryRows = computed(() => {
  if (!selectedBudget.value || !selectedBudget.value.categoryBreakdown)
    return [];

  const result = defaultCategories.map((cat) => ({
    ...cat,
    amount: 0,
    percent: 0,
  }));

  selectedBudget.value.categoryBreakdown.forEach((b) => {
    const conf = categoryConfig[b.categoryName];
    const key = conf ? conf.key : 'other';
    let targetCat = result.find((r) => r.key === key);
    if (!targetCat) targetCat = result.find((r) => r.key === 'other');
    targetCat.amount += b.amount;
  });

  const total = result.reduce((sum, cat) => sum + cat.amount, 0);
  result.forEach(
    (cat) =>
      (cat.percent = total > 0 ? Math.round((cat.amount / total) * 100) : 0),
  );
  return result;
});

const donutStyle = computed(() => {
  let cursor = 0;
  const stops = categoryRows.value.map((item) => {
    const start = cursor;
    cursor += item.percent;
    return `${item.color} ${start}% ${cursor}%`;
  });
  return { background: `conic-gradient(${stops.join(', ')})` };
});

function formatWon(value) {
  return `${Number(value || 0).toLocaleString('ko-KR')}원`;
}
function selectDestination(event) {
  travelMode.selectDestination(event.target.value);
}
function ratio(item) {
  return item.targetBudget > 0
    ? Math.min(
        Math.round((item.travelExpenseTotal / item.targetBudget) * 100),
        100,
      )
    : 0;
}
</script>

<template>
  <main class="fund-check-page">
    <header class="page-header">
      <button type="button" aria-label="이전 화면" @click="router.back()">
        ‹
      </button>
      <h1>여행 자금 체크</h1>
      <label>
        <span v-if="selected.flagClass" :class="selected.flagClass"></span>
        <span v-else>{{ selected.flag }}</span>
        <select
          :value="selected.code"
          aria-label="여행 국가 선택"
          @change="selectDestination"
        >
          <option v-for="item in options" :key="item.code" :value="item.code">
            {{ item.name }}
          </option>
        </select>
      </label>
    </header>

    <section
      class="summary-ticket"
      :class="{ overall: selected.code === 'all' }"
      :style="{
        '--theme': selected.theme,
        '--theme-alpha1': selected.theme + 'a8',
        '--theme-alpha2': selected.theme + 'd9',
        '--photo': selected.image ? `url('${selected.image}')` : 'none',
      }"
    >
      <div class="ticket-label">
        <span>
          <i v-if="selected.flagClass" :class="selected.flagClass" style="margin-right: 4px;"></i>
          <template v-else>{{ selected.flag }} </template>
          {{ selected.city }} 예산
        </span>
        <small>TRIPASS BUDGET</small>
      </div>
      <div class="ticket-rule"><i /><span /><i /></div>
      <div class="ticket-values">
        <div>
          <small>여행 목표 예산</small
          ><strong>{{ formatWon(selectedBudget?.targetBudget) }}</strong>
        </div>
        <div>
          <small>사전 지불 금액</small
          ><strong>{{ formatWon(selectedBudget?.preExpenseTotal) }}</strong>
        </div>
      </div>
      <div class="ticket-rule bottom"><i /><span /><i /></div>
      <div class="barcode" aria-hidden="true">
        <i
          v-for="index in 22"
          :key="index"
          :class="{ wide: index % 4 === 0 }"
        />
      </div>
    </section>

    <section class="content-card consumption-card">
      <h2>예산 소비 요약</h2>
      <div class="calculation">
        <div>
          <small>{{
            selected.code === 'all' ? '남은 가능 금액' : '사용 가능 금액'
          }}</small
          ><strong>{{ formatWon(usable) }}</strong
          ><span>여행 목표 예산<br />- 사전 지불 금액</span>
        </div>
        <b>−</b>
        <div>
          <small>지출 금액</small><strong>{{ formatWon(spent) }}</strong
          ><span>현재까지<br />실제 지출 금액</span>
        </div>
        <b>=</b>
        <div class="balance">
          <small>현재 잔액</small><strong>{{ formatWon(balance) }}</strong
          ><span>사용 가능 금액<br />- 사용 금액</span>
        </div>
      </div>
      <p>ⓘ 사용 가능 금액에서 실제 사용 금액을 차감한 현재 잔액이에요.</p>
    </section>

    <section class="content-card country-card">
      <h2>국가별 지출 현황 <small>ⓘ</small></h2>
      <div
        v-for="item in countryRows"
        :key="item.tripCountryId"
        class="country-row"
      >
        <span v-if="travel.countryFlagMap[item.countryName]?.class" :class="['flag', travel.countryFlagMap[item.countryName].class]"></span>
        <span v-else class="flag">✈️</span>
        <div class="country-progress">
          <div>
            <b>{{ item.countryName }}</b
            ><span
              >{{ formatWon(item.travelExpenseTotal) }} /
              {{ formatWon(item.targetBudget) }}</span
            ><strong>{{ ratio(item) }}%</strong>
          </div>
          <div class="progress-track">
            <i
              :style="{ width: `${ratio(item)}%`, background: selected.theme }"
            />
          </div>
        </div>
      </div>
      <div v-if="selected.code === 'all'" class="country-total">
        <span>국가 지출 합계</span><strong>{{ formatWon(spent) }}</strong>
      </div>
    </section>

    <section class="content-card category-card">
      <h2>카테고리별 소비 현황</h2>
      <div class="category-content">
        <div class="donut" :style="donutStyle">
          <div>
            <small>사용 금액</small><strong>{{ formatWon(spent) }}</strong>
          </div>
        </div>
        <ul>
          <li v-for="item in categoryRows" :key="item.key">
            <button
              type="button"
              :aria-label="`${item.name} 상세 보기`"
              @click="router.push(`/travel/funds/categories/${item.key}`)"
            >
              <i :style="{ background: item.color }">{{ item.icon }}</i>
              <span>{{ item.name }}</span>
              <b>{{ formatWon(item.amount) }}</b>
              <small>{{ item.percent }}% ›</small>
            </button>
          </li>
        </ul>
      </div>
      <p>ⓘ 각 비율은 사용 금액 합계 기준입니다.</p>
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.fund-check-page {
  min-height: 100vh;
  padding: 0 14px 88px;
  background: #f8f6f1;
  color: #10192d;
}
.page-header {
  display: grid;
  grid-template-columns: 46px 1fr 92px;
  align-items: center;
  height: 86px;
  padding-top: 26px;
}
.page-header > button {
  font-size: 30px;
  text-align: left;
}
.page-header h1 {
  text-align: center;
  font-size: 18px;
  font-weight: 900;
}
.page-header label {
  display: flex;
  height: 34px;
  align-items: center;
  gap: 3px;
  padding: 0 7px;
  border: 1px solid #dce3ed;
  border-radius: 13px;
  background: #fff;
}
.page-header label span {
  font-size: 13px;
}
.page-header select {
  min-width: 0;
  width: 58px;
  background: transparent;
  font-size: 11px;
  font-weight: 800;
  outline: none;
}
.summary-ticket {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  background:
    linear-gradient(100deg, var(--theme-alpha1, #061a45b8), var(--theme-alpha2, #0d2f68b8)),
    var(--photo) center/cover;
  background-color: var(--theme);
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.6);
  box-shadow: 0 7px 16px #19345b25;
}
.summary-ticket.overall {
  background: linear-gradient(125deg, #123f52, #1d6173);
}
.ticket-label {
  display: flex;
  justify-content: space-between;
  padding: 13px 16px 11px;
  font-size: 11px;
  font-weight: 900;
}
.ticket-label small {
  color: #ffffffa0;
  font-size: 7px;
  letter-spacing: 0.08em;
}
.ticket-rule {
  display: grid;
  grid-template-columns: 15px 1fr 15px;
  align-items: center;
  height: 0;
}
.ticket-rule i {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f8f6f1;
}
.ticket-rule i:first-child {
  transform: translateX(-10px);
}
.ticket-rule i:last-child {
  transform: translateX(5px);
}
.ticket-rule span {
  border-top: 1px dashed #ffffff80;
}
.ticket-values {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 18px 15px 15px;
}
.ticket-values > div + div {
  padding-left: 16px;
  border-left: 1px solid #ffffff40;
}
.ticket-values small,
.ticket-values strong {
  display: block;
}
.ticket-values small {
  color: #dce8fa;
  font-size: 9px;
}
.ticket-values strong {
  margin-top: 7px;
  font-size: 19px;
}
.ticket-rule.bottom {
  position: absolute;
  right: 0;
  bottom: 19px;
  left: 0;
}
.barcode {
  display: flex;
  height: 24px;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
  padding: 8px 17px 7px;
  background: #ffffff0c;
}
.barcode i {
  width: 1px;
  height: 11px;
  background: #fff;
}
.barcode i.wide {
  width: 3px;
}
.content-card {
  margin-top: 12px;
  padding: 15px;
  border: 1px solid #dce4ee;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 12px #1525470d;
}
.content-card h2 {
  font-size: 15px;
  font-weight: 900;
}
.consumption-card {
  padding-bottom: 9px;
}
.calculation {
  display: grid;
  grid-template-columns: 1fr 12px 1fr 12px 1fr;
  align-items: center;
  margin-top: 13px;
}
.calculation > b {
  text-align: center;
  color: #657389;
}
.calculation > div {
  min-width: 0;
  padding: 12px 4px;
  border: 1px solid #dfe6f0;
  border-radius: 11px;
  text-align: center;
}
.calculation small,
.calculation strong,
.calculation span {
  display: block;
}
.calculation small {
  color: #8a98ab;
  font-size: 8px;
}
.calculation strong {
  margin: 8px 0;
  color: #256ee7;
  font-size: 13px;
}
.calculation span {
  color: #9ca7b5;
  font-size: 7px;
  line-height: 1.45;
}
.calculation .balance {
  border-color: #bcebd6;
  background: #eefbf5;
}
.calculation .balance strong {
  color: #13a967;
}
.content-card > p {
  margin: 10px -7px 0;
  padding: 5px 8px;
  border-radius: 6px;
  background: #eef4ff;
  color: #7992b3;
  font-size: 7px;
}
.country-card h2 small {
  color: #7890ae;
}
.country-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #edf0f4;
}
.country-row:last-of-type {
  border-bottom: 0;
}
.flag {
  font-size: 25px;
}
.country-progress {
  flex: 1;
}
.country-progress > div:first-child {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 7px;
}
.country-progress b {
  font-size: 11px;
}
.country-progress span {
  color: #728198;
  font-size: 8px;
}
.country-progress strong {
  color: #65748c;
  font-size: 9px;
}
.progress-track {
  height: 6px;
  margin-top: 7px;
  overflow: hidden;
  border-radius: 99px;
  background: #e9edf3;
}
.progress-track i {
  display: block;
  height: 100%;
  border-radius: 99px;
}
.country-total {
  display: flex;
  justify-content: space-between;
  margin: 4px -15px -15px;
  padding: 11px 15px;
  border-radius: 0 0 15px 15px;
  background: #eef4ff;
  color: #6b7d96;
  font-size: 10px;
}
.country-total strong {
  color: #246ce0;
}
.category-content {
  display: grid;
  grid-template-columns: 145px 1fr;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
}
.donut {
  display: grid;
  width: 140px;
  height: 140px;
  place-items: center;
  border-radius: 50%;
}
.donut > div {
  display: grid;
  width: 82px;
  height: 82px;
  place-items: center;
  border-radius: 50%;
  background: #fff;
  text-align: center;
}
.donut small,
.donut strong {
  display: block;
}
.donut small {
  font-size: 8px;
}
.donut strong {
  font-size: 12px;
}
.category-card ul {
  display: grid;
  gap: 7px;
}
.category-card li {
  display: grid;
  grid-template-columns: 25px 1fr auto 24px;
  align-items: center;
  gap: 5px;
}
.category-card li i {
  display: grid;
  width: 23px;
  height: 23px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  font-size: 9px;
  font-style: normal;
}
.category-card li span {
  font-size: 9px;
  font-weight: 800;
}
.category-card li b {
  font-size: 8px;
}
.category-card li small {
  color: #8794a5;
  font-size: 8px;
  text-align: right;
}
.category-card > p {
  margin-top: 13px;
}
.category-card li {
  display: block;
}
.category-card li button {
  display: grid;
  width: 100%;
  grid-template-columns: 25px 1fr auto 31px;
  align-items: center;
  gap: 5px;
  padding: 3px 0;
  text-align: left;
}
</style>
