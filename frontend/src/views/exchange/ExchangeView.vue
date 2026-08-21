<script setup>
import { computed, ref, onMounted, watch } from 'vue';
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
const countryDropdownOpen = ref(false);
const travelGoalLoading = ref(true);

const currentTab = computed({
  get: () => exchange.currentTab,
  set: (val) => (exchange.currentTab = val),
});

onMounted(async () => {
  // 여행 계획이 아직 로드되지 않은 상태로 환율 탭에 바로 들어오면
  // travel.selectedPlans가 비어 있어 등록 국가 필터링이 되지 않는 문제를 방지한다.
  // 다른 화면이 activeTrip만 채워 initialized=true로 만든 경우도 있으므로,
  // 국가 계획이 비어 있다면 force로 국가까지 다시 hydrate한다.
  await Promise.all([
    exchange.updateExchangeRates(),
    travel.loadActiveGoal({ force: true }),
  ]);
  travelGoalLoading.value = false;
});

// 여행 미등록 시 전체 국가, 등록 시 여행에 등록된 국가만 표시한다.
// /exchange-rates/countries는 항상 전체 국가를 내려주므로 필터링은 FE 책임이다.
// currencyCode가 아닌 countryId로 걸러야 같은 EUR이라도 등록한 국가(예: 프랑스)만
// 보이고, 국가를 추가/삭제하면 그대로 반영된다.
const displayCurrencies = computed(() => {
  if (travelGoalLoading.value) return [];

  const hasActiveTravelGoal = Boolean(travel.tripId || travel.activeTrip?.tripId);
  if (!hasActiveTravelGoal) {
    return exchange.currencies;
  }

  // 백엔드 응답에서 countryId가 문자열/숫자로 달라도 매칭되게 정규화하고,
  // 이전 데이터처럼 ID가 맞지 않는 경우에는 국가명으로 한 번 더 매칭한다.
  // 여행 목표가 존재할 때는 매칭 실패 시에도 전체 국가로 돌아가지 않는다.
  return travel.selectedPlans
    .map((plan) => exchange.currencies.find((currency) => (
      (plan.countryId != null
        && currency.countryId != null
        && String(currency.countryId) === String(plan.countryId))
      || (plan.name && currency.countryName === plan.name)
    )))
    .filter(Boolean);
});

const filteredCurrencies = computed(() => {
  return displayCurrencies.value
    .map((currency) => ({
      ...currency,
      // /exchange-rates/countries가 국가 단위로 내려주므로 countryName을 그대로 쓴다.
      countryName: currency.countryName || currency.name || currency.code,
    }))
    .sort((a, b) => a.countryName.localeCompare(b.countryName, 'ko-KR'));
});

watch(
  filteredCurrencies,
  (newList) => {
    if (newList && newList.length > 0) {
      const isSelectedValid = newList.some((c) =>
        exchange.selectedCountryId != null
          ? String(c.countryId) === String(exchange.selectedCountryId)
          : c.code === exchange.selectedCode,
      );
      if (!isSelectedValid) {
        // 여행 국가만 가나다순으로 정렬한 목록의 첫 국가를 기본값으로 사용한다.
        // 이전에 저장한 동일 통화 국가(예: 그리스 EUR)가 여행 목록에 없으면
        // 통화 코드만으로 되살리지 않고 현재 여행 국가로 교체한다.
        exchange.selectedCode = newList[0].code;
        exchange.selectedCountryId = newList[0].countryId ?? null;
      }
    }
  },
  { immediate: true },
);

</script>

<template>
  <main class="page">
    <div class="shell tab-scroll-surface" data-tab-scroll>
      <div class="exchange-header">
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
              <strong>전체 통화 환율</strong>
              <p>국가별 환율 확인</p>
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
              <strong>환율 알림 목록</strong>
              <p>환율 지정 알림</p>
            </div>
          </button>
        </div>
        <div class="currency-section-heading">
          <strong>여행 국가 환율 바로보기</strong>
          <p>국가를 선택해 현재 환율과 추이를 확인하세요.</p>
        </div>

        <div v-if="travelGoalLoading" class="empty-state exchange-loading-state">
          <div class="exchange-loading-spinner" aria-hidden="true"></div>
          <p>등록한 여행 국가의 환율을 불러오고 있어요.</p>
        </div>
        <template v-else-if="filteredCurrencies.length > 0">
          <div class="currency-select" :class="{ open: countryDropdownOpen }">
            <button
              type="button"
              class="currency-select-trigger"
              :aria-expanded="countryDropdownOpen"
              @click="countryDropdownOpen = !countryDropdownOpen"
            >
              <img
                v-if="exchange.selectedCurrency?.flagUrl"
                class="currency-country-flag-img"
                :src="exchange.selectedCurrency.flagUrl"
                alt=""
              />
              <span v-else class="fi currency-country-flag" :class="exchange.selectedCurrency?.flagClass" aria-hidden="true"></span>
              <span class="currency-select-info">
                <span class="currency-country-name">{{ exchange.selectedCurrency?.countryName || exchange.selectedCurrency?.name }}</span>
                <span class="currency-country-unit">{{ exchange.selectedCurrency?.name }} · {{ exchange.selectedCode }}</span>
              </span>
              <svg class="currency-select-chevron" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="m7 10 5 5 5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>

            <div v-if="countryDropdownOpen" class="currency-country-list" aria-label="지원 국가 목록">
              <button
                v-for="currency in filteredCurrencies"
                :key="currency.countryId ?? currency.code"
                type="button"
                :class="{ active: exchange.selectedCurrency?.countryId != null ? exchange.selectedCurrency.countryId === currency.countryId : exchange.selectedCode === currency.code }"
                @click="exchange.selectedCode = currency.code; exchange.selectedCountryId = currency.countryId ?? null; countryDropdownOpen = false"
              >
                <img v-if="currency.flagUrl" class="currency-country-flag-img" :src="currency.flagUrl" alt="" />
                <span v-else class="fi currency-country-flag" :class="currency.flagClass" aria-hidden="true"></span>
                <span class="currency-country-name">{{ currency.countryName }}</span>
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
        <template v-else>
          <div class="empty-state">
            <div class="empty-icon">💸</div>
            <p>
              <template v-if="travel.tripId || travel.activeTrip?.tripId">
                등록한 여행 국가의 환율 정보가 아직 없어요.<br />잠시 후 다시 확인해 주세요.
              </template>
              <template v-else>
                아직 관심 있는 환율이 없어요!<br />여행을 계획하거나 직접 통화를 추가해보세요.
              </template>
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
.exchange-loading-state {
  min-height: 180px;
}
.exchange-loading-spinner {
  width: 30px;
  height: 30px;
  margin: 0 auto 14px;
  border: 3px solid #dbe7fb;
  border-top-color: #2469e8;
  border-radius: 50%;
  animation: exchange-spin .75s linear infinite;
}
@keyframes exchange-spin {
  to { transform: rotate(360deg); }
}
.page {
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  overscroll-behavior: none;
  background: #eef2f8;
  color: #10192d;
}
.shell {
  width: min(100%, 390px);
  height: 100%;
  margin: auto;
  padding: 78px 20px 100px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior-y: contain;
  touch-action: pan-y;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  background: #eef2f8;
  box-sizing: border-box;
}
.shell::-webkit-scrollbar {
  display: none;
}
.exchange-header {
  position: fixed;
  top: 0;
  left: 50%;
  z-index: 60;
  width: min(100%, 390px);
  padding: 14px 20px 8px;
  background: #eef2f8;
  box-sizing: border-box;
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
  border: 1px solid #bcd2ff;
  border-radius: 14px;
  background: linear-gradient(150deg, #eff5ff 0%, #e6f0ff 100%);
  box-shadow: 0 8px 22px rgba(23, 43, 77, .055);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}
.action-card-btn:active {
  transform: scale(0.98);
  background: #e3eeff;
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
  background: #d8e7ff;
  color: #173f8d;
}
.icon-circle.secondary {
  background: #d8e7ff;
  color: #173f8d;
}
.btn-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.btn-text strong {
  font-size: 12px;
  font-weight: 800;
  color: #173f8d;
  white-space: nowrap;
}
.btn-text p {
  margin-top: 2px;
  font-size: 9.5px;
  color: #96a1b5;
  white-space: nowrap;
}
.currency-section-heading {
  margin-top: 18px;
}
.currency-section-heading strong {
  color: #173f8d;
  font-size: 15px;
  font-weight: 800;
}
.currency-section-heading p {
  margin-top: 4px;
  color: #8d99ad;
  font-size: 10px;
  font-weight: 500;
}
.currency-select {
  margin: 10px 0 12px;
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
  grid-template-columns: 22px minmax(66px, auto) minmax(0, 1fr) 16px;
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
.currency-country-flag-img {
  width: 19px;
  height: 14px;
  border-radius: 2px;
  object-fit: cover;
  flex: none;
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
