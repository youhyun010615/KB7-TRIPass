<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import NotificationBell from '@/components/common/NotificationBell.vue';
import ExchangeTicket from '@/components/exchange/ExchangeTicket.vue';
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

const format = (v, d = 2) =>
  Number(v || 0).toLocaleString('ko-KR', { maximumFractionDigits: d });

const currentAlert = computed(() =>
  exchange.alerts.find((a) => a.currencyCode === exchange.selectedCode),
);

function handleAlertAction() {
  if (currentAlert.value) {
    router.push(`/exchange/alerts/${currentAlert.value.id}`);
  } else {
    router.push('/exchange/alerts/add');
  }
}
</script>

<template>
  <main class="page">
    <div class="shell">
      <div ref="exchangeHeaderEl" class="exchange-header-fixed">
        <header>
          <div class="exchange-header-top">
            <div>
              <p class="exchange-header-eyebrow">
                <img src="@/assets/icons/blue_airplane.svg" class="header-plane" alt="" />
                TRIPASS
              </p>
              <h1>환율·환전</h1>
            </div>
            <NotificationBell />
          </div>
          <div
            v-if="exchange.lastUpdateDate && currentTab === 'rate'"
            class="update-info"
          >
            {{ exchange.lastUpdateDate }} 고시 기준
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
        </header>
      </div>
      <div :style="{ height: exchangeHeaderHeight + 'px' }" aria-hidden="true" />

      <!-- 환율 탭 -->
      <section v-if="currentTab === 'rate'">
        <!-- 프리미엄 핀테크 대시보드 액션 카드 -->
        <div class="header-actions">
          <button
            @click="router.push('/exchange/currencies')"
            class="action-card-btn"
          >
            <span class="icon-circle primary">💵</span>
            <div class="btn-text">
              <strong>환율 더보기</strong>
              <p>모든 통화 환율</p>
            </div>
          </button>
          <button
            @click="router.push('/exchange/alerts')"
            class="action-card-btn"
          >
            <span class="icon-circle secondary">🔔</span>
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

          <ExchangeTicket
            v-if="exchange.selectedCurrency"
            :name="exchange.selectedCurrency.name"
            :rate="exchange.selectedCurrency.rate"
            :unit="exchange.selectedCurrency.unit"
            :subtitle="`어제보다 ${exchange.selectedCurrency.change > 0 ? '▲' : '▼'} ${format(Math.abs(exchange.selectedCurrency.change))}원`"
            :selectedCode="exchange.selectedCode"
            :flagClass="exchange.selectedCurrency.flagClass"
            :symbol="exchange.selectedCurrency.symbol"
            @add-alert="handleAlertAction"
          />
          <CurrencyChart :currency="exchange.selectedCurrency" />
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
  background: #f4f5f9;
  color: #10192d;
}
.shell {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: auto;
  padding: 0 18px 100px;
  background: #f4f5f9;
}
.exchange-header-fixed {
  position: fixed;
  top: 0;
  left: 50%;
  z-index: 60;
  width: 100%;
  max-width: 390px;
  padding: 42px 18px 1px;
  background: #f4f5f9;
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
  font-size: 19px;
  font-weight: 900;
  color: #10192b;
}
.update-info {
  margin-top: 14px;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 18px;
}
.main-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 20px;
}
.main-tabs button {
  padding: 12px;
  border: none;
  border-radius: 12px;
  background: #e2e8f0;
  color: #64748b;
  font-weight: bold;
  cursor: pointer;
}
.main-tabs button.active {
  background: #17387f;
  color: #fff;
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
  margin-bottom: 18px;
}
.action-card-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e1e6ed;
  border-radius: 14px;
  background: #fff;
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
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-size: 14px;
}
.icon-circle.primary {
  background: #f0f4fc;
  color: #17387f;
}
.icon-circle.secondary {
  background: #fff8e7;
  color: #ff9f0a;
}
.btn-text {
  display: flex;
  flex-direction: column;
}
.btn-text strong {
  font-size: 11px;
  font-weight: 700;
  color: #10192d;
}
.btn-text p {
  font-size: 9px;
  color: #8c98a8;
  margin-top: 1px;
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
