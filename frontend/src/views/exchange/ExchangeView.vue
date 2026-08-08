<script setup>
import { computed, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import ExchangeTicket from '@/components/exchange/ExchangeTicket.vue';
import CurrencyTabNav from '@/components/exchange/CurrencyTabNav.vue';
import CurrencyChart from '@/components/exchange/CurrencyChart.vue';
import ExchangeCalculator from '@/components/exchange/ExchangeCalculator.vue';
import NearbyBanks from '@/components/exchange/NearbyBanks.vue';
import { useExchangeStore } from '@/stores/exchange';

const router = useRouter();
const exchange = useExchangeStore();
const currentTab = computed({
  get: () => exchange.currentTab,
  set: (val) => exchange.currentTab = val
});

onMounted(() => {
  exchange.updateExchangeRates();
});

const format = (v, d = 2) =>
  Number(v || 0).toLocaleString('ko-KR', { maximumFractionDigits: d });

const currentAlert = computed(() =>
  exchange.alerts.find((a) => a.code === exchange.selectedCode)
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
      <header>
        <h1>환율·환전</h1>
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

      <!-- 환율 탭 -->
      <section v-if="currentTab === 'rate'">
        <CurrencyTabNav
          v-model="exchange.selectedCode"
          :currencies="exchange.currencies"
        />

        <ExchangeTicket
          v-if="exchange.selectedCurrency"
          :name="exchange.selectedCurrency.name"
          :rate="exchange.selectedCurrency.rate"
          :unit="exchange.selectedCurrency.unit"
          :subtitle="`어제보다 ${exchange.selectedCurrency.change > 0 ? '▲' : '▼'} ${format(Math.abs(exchange.selectedCurrency.change))}원`"
          :selectedCode="exchange.selectedCode"
          :flagClass="exchange.selectedCurrency.flagClass"
          :alertButtonLabel="currentAlert ? '알림 설정 중' : '+ 환율 알림 추가'"
          @add-alert="handleAlertAction"
        />

        <CurrencyChart :currency="exchange.selectedCurrency" />

        <ExchangeCalculator />
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
  background: #e7ecf4;
  color: #10192d;
}
.shell {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: auto;
  padding: 52px 18px 100px;
  background: #f7f5ef;
}
header h1 {
  text-align: center;
  font-size: 18px;
  font-weight: 900;
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
.map-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e2e8f0;
  border-radius: 15px;
  margin-top: 10px;
}
</style>