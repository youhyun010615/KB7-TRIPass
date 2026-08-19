<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import NotificationBell from '@/components/common/NotificationBell.vue';
import CurrencyChart from '@/components/exchange/CurrencyChart.vue';
import ExchangeCalculator from '@/components/exchange/ExchangeCalculator.vue';
import NearbyBanks from '@/components/exchange/NearbyBanks.vue';
import { useExchangeStore } from '@/stores/exchange';
import { useTravelStore } from '@/stores/travel';

const router = useRouter();
const exchange = useExchangeStore();
const travel = useTravelStore();
const currencySearch = ref('');
const countryDropdownOpen = ref(false);

const currencyCountryNames = {
  AED: '아랍에미리트', AUD: '호주', BHD: '바레인', BND: '브루나이',
  CAD: '캐나다', CHF: '스위스', CNH: '중국', DKK: '덴마크',
  EUR: '유럽연합', GBP: '영국', HKD: '홍콩', IDR: '인도네시아',
  JPY: '일본', KWD: '쿠웨이트', MYR: '말레이시아', NOK: '노르웨이',
  NZD: '뉴질랜드', SAR: '사우디아라비아', SEK: '스웨덴',
  SGD: '싱가포르', THB: '태국', USD: '미국',
};

// 앱 프레임(App.vue)의 overflow:hidden 때문에 sticky 대신 fixed로 헤더를 고정한다.
const exchangeHeaderEl = ref(null);
const exchangeHeaderHeight = ref(0);
let exchangeHeaderResizeObserver = null;

function syncExchangeHeaderHeight() {
  if (exchangeHeaderEl.value) {
    exchangeHeaderHeight.value = exchangeHeaderEl.value.offsetHeight;
  }
}

watch(exchangeHeaderEl, (el) => {
  exchangeHeaderResizeObserver?.disconnect();
  exchangeHeaderResizeObserver = null;
  if (!el) return;

  syncExchangeHeaderHeight();
  if (window.ResizeObserver) {
    exchangeHeaderResizeObserver = new ResizeObserver(syncExchangeHeaderHeight);
    exchangeHeaderResizeObserver.observe(el);
  }
});

onBeforeUnmount(() => {
  exchangeHeaderResizeObserver?.disconnect();
});

const currentTab = computed({
  get: () => exchange.currentTab,
  set: (val) => (exchange.currentTab = val),
});

onMounted(() => {
  exchange.updateExchangeRates();
});

// 여행 미등록 시 전체 통화, 등록 시 여행지 국가의 통화만 표시한다.
const displayCurrencies = computed(() => {
  if (travel.selectedPlans.length === 0) {
    return exchange.currencies;
  }

  const codes = new Set(
    travel.selectedPlans
      .map((plan) => plan.currencyCode)
      .filter(Boolean),
  );

  const filtered = exchange.currencies.filter((c) => codes.has(c.code));
  // 여행지 통화가 환율 데이터에 하나도 없으면 전체 통화로 대체 표시한다.
  return filtered.length > 0 ? filtered : exchange.currencies;
});

const filteredCurrencies = computed(() => {
  const keyword = currencySearch.value.trim().toLocaleLowerCase('ko-KR');
  return displayCurrencies.value
    .map((currency) => ({
      ...currency,
      countryName: currencyCountryNames[currency.code] || currency.name || currency.code,
    }))
    .filter((currency) =>
      !keyword || [currency.countryName, currency.code, currency.name, currency.symbol]
        .filter(Boolean)
        .some((value) => String(value).toLocaleLowerCase('ko-KR').includes(keyword)),
    )
    .sort((a, b) => a.countryName.localeCompare(b.countryName, 'ko-KR'));
});

watch(
  displayCurrencies,
  (newList) => {
    if (newList && newList.length > 0) {
      const isSelectedValid = newList.some(
        (c) => c.code === exchange.selectedCode,
      );
      if (!isSelectedValid) {
        exchange.selectedCode = newList[0].code;
      }
    }
  },
  { immediate: true },
);

</script>

<template>
  <main class="page">
    <div class="shell">
      <div ref="exchangeHeaderEl" class="exchange-header-fixed">
        <header>
          <div class="exchange-header-top">
            <div>
              <p class="exchange-header-eyebrow">
                <img src="@/assets/brand/tripass-text.png" class="header-wordmark" alt="TRIPASS" />
              </p>
              <h1>EXCHANGE</h1>
            </div>
            <NotificationBell />
          </div>
        </header>
      </div>
      <div :style="{ height: exchangeHeaderHeight + 'px' }" aria-hidden="true" />

      <nav class="main-tabs">
        <button
          :class="{ active: currentTab === 'rate' }"
          @click="currentTab = 'rate'"
        >
          환율
        </button>
        <button
          :class="{ active: currentTab === 'exchange' }"
          @click="currentTab = 'exchange'"
        >
          환전
        </button>
      </nav>

      <!-- 환율 탭 -->
      <section v-if="currentTab === 'rate'">
        <!-- 프리미엄 핀테크 대시보드 액션 카드 -->
        <div class="header-actions">
          <button
            @click="router.push('/exchange/currencies')"
            class="action-card-btn"
          >
            <span class="icon-circle primary" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="1.8" />
                <path d="M9.4 9.25c.5-.75 1.35-1.15 2.55-1.15 1.45 0 2.45.67 2.45 1.75 0 2.65-5 1.1-5 3.9 0 1.15 1.03 1.95 2.7 1.95 1.25 0 2.18-.42 2.72-1.28M12 6.8v10.4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
              </svg>
            </span>
            <div class="btn-text">
              <strong>환율 더보기</strong>
              <p>모든 통화 환율</p>
            </div>
          </button>
          <button
            @click="router.push('/exchange/alerts')"
            class="action-card-btn"
          >
            <span class="icon-circle secondary" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none">
                <path d="M6.8 16.4h10.4l-1.2-1.8V11a4 4 0 0 0-8 0v3.6l-1.2 1.8Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round" />
                <path d="M10.2 18.2a2 2 0 0 0 3.6 0" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
              </svg>
            </span>
            <div class="btn-text">
              <strong>내 알림 목록</strong>
              <p>환율 지정 알림</p>
            </div>
          </button>
        </div>
        <div class="currency-search">
          <svg aria-hidden="true" viewBox="0 0 24 24" fill="none">
            <circle cx="11" cy="11" r="6.5" stroke="currentColor" stroke-width="1.8" />
            <path d="m16 16 4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          </svg>
          <input
            v-model="currencySearch"
            type="search"
            inputmode="search"
            autocomplete="off"
            aria-label="국가 또는 통화 검색"
            placeholder="국가명 또는 통화코드 검색"
          />
          <button
            v-if="currencySearch"
            type="button"
            aria-label="검색어 지우기"
            @click="currencySearch = ''"
          >
            ×
          </button>
        </div>

        <template v-if="filteredCurrencies.length > 0">
          <div class="currency-select" :class="{ open: countryDropdownOpen || !!currencySearch }">
            <button
              type="button"
              class="currency-select-trigger"
              :aria-expanded="countryDropdownOpen || !!currencySearch"
              @click="countryDropdownOpen = !countryDropdownOpen"
            >
              <span class="fi currency-country-flag" :class="exchange.selectedCurrency?.flagClass" aria-hidden="true"></span>
              <span class="currency-select-info">
                <span class="currency-country-name">{{ currencyCountryNames[exchange.selectedCode] || exchange.selectedCurrency?.name }}</span>
                <span class="currency-country-unit">{{ exchange.selectedCurrency?.name }} · {{ exchange.selectedCode }}</span>
              </span>
              <svg class="currency-select-chevron" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="m7 10 5 5 5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>

            <div v-if="countryDropdownOpen || currencySearch" class="currency-country-list" aria-label="지원 국가 목록">
              <button
                v-for="currency in filteredCurrencies"
                :key="currency.code"
                type="button"
                :class="{ active: exchange.selectedCode === currency.code }"
                @click="exchange.selectedCode = currency.code; countryDropdownOpen = false; currencySearch = ''"
              >
                <span class="fi currency-country-flag" :class="currency.flagClass" aria-hidden="true"></span>
                <span class="currency-country-name">{{ currency.countryName }}</span>
                <span class="currency-country-unit">{{ currency.name }}</span>
                <b>{{ currency.code }}</b>
                <span class="currency-country-check" aria-hidden="true">✓</span>
              </button>
            </div>
          </div>

          <CurrencyChart
            :currency="exchange.selectedCurrency"
            :last-update-date="exchange.lastUpdateDate"
          />
          <ExchangeCalculator />
        </template>
        <div v-else-if="currencySearch" class="currency-search-empty">
          <b>검색 결과가 없어요</b>
          <p>국가명이나 통화코드를 다시 확인해 주세요.</p>
          <button type="button" @click="currencySearch = ''">전체 통화 보기</button>
        </div>
        <template v-else>
          <div class="empty-state">
            <div class="empty-icon">💸</div>
            <p>
              아직 관심 있는 환율이 없어요!<br />여행을 계획하거나 직접 통화를
              추가해보세요.
            </p>
            <div class="empty-actions">
              <button
                @click="router.push('/travel/register')"
                class="btn-primary"
              >
                여행 등록하기
              </button>
              <button
                @click="router.push('/exchange/currencies')"
                class="btn-secondary"
              >
                모든 통화 보기
              </button>
            </div>
          </div>
        </template>
      </section>

      <!-- 환전 탭 (지도/은행) -->
      <section v-else class="exchange-map">
        <NearbyBanks />
      </section>
      <BottomNav />
    </div>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f3f5fa;
  color: #10192d;
}
.shell {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: auto;
  padding: 0 20px 100px;
  background: #f3f5fa;
}
.exchange-header-fixed {
  position: fixed;
  top: 0;
  left: 50%;
  z-index: 60;
  width: 100%;
  max-width: 390px;
  padding: 42px 20px 8px;
  background: #f3f5fa;
  transform: translateX(-50%);
}
.exchange-header-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.exchange-header-eyebrow {
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: 'Space Mono', monospace;
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.15em;
  color: #0b2a6b;
  margin-bottom: 4px;
}
.header-wordmark {
  display: block;
  width: 88px;
  height: auto;
  object-fit: contain;
}
.header-plane {
  width: 12px;
  height: 12px;
  animation: header-plane-fly 2.6s ease-in-out infinite;
}
@keyframes header-plane-fly {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
    filter: brightness(1) drop-shadow(0 0 0 rgba(47, 112, 242, 0));
  }
  25% {
    transform: translateY(-1.5px) rotate(-8deg);
  }
  50% {
    transform: translateY(0) rotate(0deg);
    filter: brightness(1.6) drop-shadow(0 0 3px rgba(47, 112, 242, 0.55));
  }
  75% {
    transform: translateY(1.5px) rotate(6deg);
  }
}
header h1 {
  margin-top: 2px;
  text-align: left;
  font-size: 17px;
  font-weight: 400;
  color: #29466f;
}
.main-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  padding: 5px;
  margin: 8px 0 10px;
  border-radius: 999px;
  background: #e9ecf3;
}
.main-tabs button {
  min-height: 38px;
  padding: 8px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #929caf;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background .2s ease, color .2s ease, box-shadow .2s ease;
}
.main-tabs button.active {
  background: #123478;
  color: #fff;
  box-shadow: 0 5px 12px rgba(18, 52, 120, .18);
}

.exchange-map {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.add-btn-wrapper {
  text-align: right;
  margin: 10px 0;
}
.header-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin: 8px 0 9px;
}
.action-card-btn {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  min-height: 58px;
  padding: 9px 12px;
  border: 0;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(23, 43, 77, .055);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}
.action-card-btn:active {
  transform: scale(0.98);
  background: #f8fafc;
}
.icon-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
}
.icon-circle svg {
  width: 18px;
  height: 18px;
}
.icon-circle.primary {
  background: #eaf1ff;
  color: #2f6fea;
}
.icon-circle.secondary {
  background: #fff3d9;
  color: #d89000;
}
.btn-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.btn-text strong {
  font-size: 12px;
  font-weight: 800;
  color: #10192d;
  white-space: nowrap;
}
.btn-text p {
  margin-top: 2px;
  font-size: 9.5px;
  color: #96a1b5;
  white-space: nowrap;
}
.currency-search {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) 26px;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 10px 0 13px;
  margin: 2px 0 3px;
  border: 1px solid #dce4f0;
  border-radius: 14px;
  background: #fff;
  color: #7d8ba3;
  box-shadow: 0 5px 16px rgba(23, 43, 77, .04);
}
.currency-search:focus-within {
  border-color: #2f6fea;
  box-shadow: 0 0 0 3px rgba(47, 111, 234, .1);
}
.currency-search svg {
  width: 18px;
  height: 18px;
}
.currency-search input {
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #17233b;
  font-size: 12px;
  font-weight: 600;
}
.currency-search input::placeholder { color: #a0aabd; }
.currency-search input::-webkit-search-cancel-button { display: none; }
.currency-search button {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 50%;
  background: #eef2f8;
  color: #718097;
  font-size: 18px;
  line-height: 1;
}
.currency-select {
  margin: 8px 0 12px;
}
.currency-select-trigger {
  display: grid;
  width: 100%;
  grid-template-columns: 22px 1fr 16px;
  align-items: center;
  gap: 10px;
  min-height: 50px;
  padding: 8px 14px;
  border: 1px solid #e0e7f1;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 7px 20px rgba(23, 43, 77, .045);
  text-align: left;
}
.currency-select.open .currency-select-trigger {
  border-color: #b7cdf6;
  border-radius: 14px 14px 0 0;
  box-shadow: none;
}
.currency-select-info {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}
.currency-select-chevron {
  width: 15px;
  height: 15px;
  color: #8b97a9;
  transition: transform .2s ease;
}
.currency-select.open .currency-select-chevron {
  transform: rotate(180deg);
}
.currency-select .currency-country-list {
  max-height: 220px;
  margin: 0;
  border-top: 0;
  border-radius: 0 0 14px 14px;
}
.currency-country-list {
  display: grid;
  max-height: 172px;
  margin: 8px 0 12px;
  overflow-y: auto;
  border: 1px solid #e0e7f1;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 7px 20px rgba(23, 43, 77, .045);
  scrollbar-width: thin;
  scrollbar-color: #c8d3e3 transparent;
}
.currency-country-list button {
  display: grid;
  grid-template-columns: 22px minmax(66px, auto) minmax(0, 1fr) 34px 16px;
  align-items: center;
  gap: 8px;
  min-height: 46px;
  padding: 8px 12px;
  border-bottom: 1px solid #edf1f6;
  color: #162139;
  text-align: left;
}
.currency-country-list button:last-child { border-bottom: 0; }
.currency-country-list button.active { background: #edf4ff; }
.currency-country-flag {
  width: 19px;
  height: 14px;
  border-radius: 2px;
  background-size: cover;
}
.currency-country-name {
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}
.currency-country-unit {
  overflow: hidden;
  color: #8b97a9;
  font-size: 9.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.currency-country-list b {
  color: #596981;
  font-size: 10px;
  text-align: right;
}
.currency-country-check {
  color: transparent;
  font-size: 12px;
  font-weight: 900;
}
.currency-country-list button.active .currency-country-name,
.currency-country-list button.active b,
.currency-country-list button.active .currency-country-check { color: #2464d8; }
.currency-search-empty {
  margin: 10px 0 16px;
  padding: 28px 18px;
  border: 1px dashed #cad5e5;
  border-radius: 18px;
  background: #fff;
  text-align: center;
}
.currency-search-empty b { font-size: 13px; }
.currency-search-empty p {
  margin-top: 6px;
  color: #8a97aa;
  font-size: 10px;
}
.currency-search-empty button {
  margin-top: 14px;
  padding: 8px 13px;
  border-radius: 10px;
  background: #eaf1ff;
  color: #2464d8;
  font-size: 10px;
  font-weight: 800;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  gap: 24px;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 8px;
}
.empty-state p {
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}
.empty-actions {
  display: flex;
  gap: 12px;
  width: 100%;
  justify-content: center;
}
.btn-primary,
.btn-secondary {
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-primary {
  background: #17387f;
  color: #fff;
  border: none;
}
.btn-primary:hover {
  background: #0f2a63;
}
.btn-secondary {
  background: #fff;
  color: #17387f;
  border: 1px solid #17387f;
}
.btn-secondary:hover {
  background: #f1f5f9;
}
</style>
