<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import NotificationBell from '@/components/common/NotificationBell.vue';
import CurrencyTabNav from '@/components/exchange/CurrencyTabNav.vue';
import CurrencyChart from '@/components/exchange/CurrencyChart.vue';
import ExchangeCalculator from '@/components/exchange/ExchangeCalculator.vue';
import NearbyBanks from '@/components/exchange/NearbyBanks.vue';
import { useExchangeStore, countryToCurrency } from '@/stores/exchange';
import { useTravelStore } from '@/stores/travel';

const router = useRouter();
const exchange = useExchangeStore();
const travel = useTravelStore();

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

// 개인화된 통화 목록 계산
const displayCurrencies = computed(() => {
  if (travel.selectedPlans.length === 0) {
    return exchange.currencies;
  }

  const codes = new Set([
    ...exchange.interestedCurrencyCodes,
    ...exchange.alerts.map((a) => a.currencyCode),
    ...travel.selectedPlans.map((p) => countryToCurrency[p.code] || 'USD'),
  ]);
  return exchange.currencies.filter((c) => codes.has(c.code));
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
        <template v-if="displayCurrencies.length > 0">
          <CurrencyTabNav
            v-model="exchange.selectedCode"
            :currencies="displayCurrencies"
          />

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
  padding: 42px 20px 12px;
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
  gap: 12px;
  margin: 12px 0 14px;
}
.action-card-btn {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  min-height: 68px;
  padding: 11px 12px;
  border: 0;
  border-radius: 16px;
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
