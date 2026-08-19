<script setup>
import { computed, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import CurrencyChart from '@/components/exchange/CurrencyChart.vue';
import { useExchangeStore } from '@/stores/exchange';

const router = useRouter();
const exchange = useExchangeStore();
const query = ref('');
const openedCurrencyCode = ref(null);

onMounted(() => {
  if (exchange.currencies.length === 0) {
    exchange.updateExchangeRates();
  }
});

const format = (value) =>
  Number(value || 0).toLocaleString('ko-KR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

// 이 화면은 여행 등록 여부와 무관하게 항상 전체 국가의 통화를 보여준다.
const filteredCurrencies = computed(() => {
  const keyword = query.value.trim().toLocaleLowerCase('ko-KR');
  if (!keyword) return exchange.currencies;

  return exchange.currencies.filter((item) => {
    // 각 필드를 소문자로 변환하여 검색어와 매칭 (OR 조건)
    const code = (item.code || '').toLocaleLowerCase('ko-KR');
    const name = (item.name || '').toLocaleLowerCase('ko-KR');
    const country = (item.countryName || '').toLocaleLowerCase('ko-KR');

    return (
      code.includes(keyword) ||
      name.includes(keyword) ||
      country.includes(keyword)
    );
  });
});

function toggleCurrency(item) {
  const key = item.countryId ?? item.code;
  if (openedCurrencyCode.value === key) {
    openedCurrencyCode.value = null;
  } else {
    exchange.selectedCode = item.code;
    exchange.selectedCountryId = item.countryId ?? null;
    openedCurrencyCode.value = key;
  }
}
</script>

<template>
  <main class="page">
    <div class="shell">
      <header>
        <button type="button" aria-label="이전 화면" @click="router.back()">
          ‹
        </button>
        <div>
          <h1>모든 국가 통화</h1>
          <small v-if="exchange.lastUpdateDate" class="update-info">
            {{ exchange.lastUpdateDate }} 고시 기준
          </small>
        </div>
        <span aria-hidden="true"></span>
      </header>

      <label class="search">
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          aria-hidden="true"
        >
          <circle
            cx="11"
            cy="11"
            r="7"
            stroke="currentColor"
            stroke-width="1.8"
          />
          <path
            d="M16 16L21 21"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
          />
        </svg>
        <input
          v-model="query"
          placeholder="국가명 또는 통화 코드를 검색해 주세요"
          aria-label="통화 검색"
        />
        <button
          v-if="query"
          type="button"
          aria-label="검색어 지우기"
          @click="query = ''"
        >
          ×
        </button>
      </label>

      <p class="result-count">
        {{ query ? `'${query}' 검색 결과` : '전체 통화' }} ·
        {{ filteredCurrencies.length }}개
      </p>

      <section v-if="filteredCurrencies.length" class="currency-list">
        <div v-for="item in filteredCurrencies" :key="item.countryId ?? item.code" class="currency-item-wrapper">
          <button
            type="button"
            class="currency-btn"
            @click="toggleCurrency(item)"
          >
            <span :class="item.flagClass" class="list-flag"></span>
            <span class="identity">
              <b>{{ item.countryName || item.name }}</b>
              <em>{{ item.code }}<small v-if="item.unit > 1"> {{ item.unit }}</small></em>
            </span>
            <span class="rate">
              <strong>{{ format(item.rate) }}원</strong>
              <small :class="{ up: item.change > 0 }">
                {{ item.change > 0 ? '▲' : '▼' }}
                {{ format(Math.abs(item.change)) }}
              </small>
            </span>
            <i :class="{ rotated: openedCurrencyCode === (item.countryId ?? item.code) }">›</i>
          </button>
          
          <div v-if="openedCurrencyCode === (item.countryId ?? item.code)" class="currency-chart-wrapper">
            <CurrencyChart :currency="item" />
          </div>
        </div>
      </section>

      <section v-else class="empty">
        <b>검색 결과가 없어요.</b>
        <p>국가명, 통화명 또는 통화 코드를 다시 확인해 주세요.</p>
        <button type="button" @click="query = ''">전체 통화 보기</button>
      </section>

      <BottomNav />
    </div>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #eef2f8;
  color: #10192d;
}

.shell {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: auto;
  padding: 52px 18px 100px;
  background: #eef2f8;
}

header {
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: center;
}

header button {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: #fff;
  color: #193d82;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 5px 16px rgba(36, 72, 117, 0.07);
}

header h1 {
  text-align: center;
  font-size: 17px;
  font-weight: 900;
  letter-spacing: -0.03em;
}

.update-info {
  display: block;
  margin-top: 2px;
  text-align: center;
  font-size: 9px;
  color: #7186aa;
  font-weight: 700;
}

.search {
  display: grid;
  grid-template-columns: 20px 1fr 24px;
  align-items: center;
  gap: 7px;
  height: 46px;
  margin-top: 20px;
  padding: 0 14px;
  border: 1px solid #d7e1f0;
  border-radius: 13px;
  background: #fff;
  color: #7186aa;
}

.search input {
  min-width: 0;
  background: transparent;
  color: #10192d;
  font-size: 11px;
  outline: none;
}

.search button {
  font-size: 17px;
  color: #94a3b8;
}

.result-count {
  margin: 16px 3px 9px;
  color: #7186aa;
  font-size: 9.5px;
  font-weight: 800;
}

.currency-item-wrapper {
  margin-bottom: 9px;
}

.currency-list .currency-btn {
  display: grid;
  width: 100%;
  grid-template-columns: 32px 1fr auto 10px;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border: 1px solid #e7edf9;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(16, 25, 43, 0.05);
  text-align: left;
}

.currency-chart-wrapper {
  margin-top: 8px;
  padding: 0 10px;
}

.list-flag {
  display: inline-block;
  width: 1.33em;
  height: 1em;
  background-size: cover;
  border-radius: 2px;
  vertical-align: middle;
  font-size: 19px;
}

.identity b,
.identity em,
.rate strong,
.rate small {
  display: block;
}

.identity b {
  color: #10192d;
  font-size: 11.5px;
  font-weight: 800;
}

.identity em {
  margin-top: 3px;
  color: #173f8d;
  font-size: 9px;
  font-weight: 800;
  font-style: normal;
}

.identity em small {
  margin-left: 2px;
  color: #7186aa;
  font-size: 8px;
  font-weight: 700;
}

.rate {
  text-align: right;
}

.rate strong {
  font-size: 11.5px;
  font-weight: 800;
}

.rate small {
  margin-top: 4px;
  color: #3972d8;
  font-size: 8px;
  font-weight: 700;
}

.rate small.up {
  color: #ed5555;
}

.currency-list i {
  color: #9aa7b7;
  font-size: 18px;
  font-style: normal;
  transition: transform 0.2s;
}
.currency-list i.rotated {
  transform: rotate(90deg);
}

.empty {
  margin-top: 28px;
  padding: 34px 24px;
  border: 1px solid #d5e2f8;
  border-radius: 24px;
  background: #fff;
  text-align: center;
  box-shadow: 0 10px 30px rgba(30, 64, 112, 0.04);
}

.empty b {
  font-size: 13px;
  font-weight: 900;
}

.empty p {
  margin-top: 7px;
  color: #73829d;
  font-size: 10px;
  line-height: 1.6;
}

.empty button {
  margin-top: 16px;
  padding: 10px 16px;
  border-radius: 12px;
  background: #245ec4;
  color: #fff;
  font-size: 11px;
  font-weight: 900;
}
</style>
