<script setup>
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import { useExchangeStore } from '@/stores/exchange';

const router = useRouter();
const exchange = useExchangeStore();
const format = (v) =>
  Number(v || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 });

onMounted(async () => {
  if (exchange.currencies.length === 0) {
    await exchange.updateExchangeRates();
  }
});

// 도달한 알림 계산
const reachedAlerts = computed(() => {
  return exchange.alerts.filter((alert) => {
    if (!alert.enabled) return false;
    const currentRate = exchange.getCurrency(alert.currencyCode)?.rate || 0;
    return currentRate > 0 && currentRate <= alert.targetRate;
  });
});
</script>

<template>
  <main class="page">
    <div class="shell">
      <header>
        <button @click="router.back()">‹</button>
        <h1>내 알림</h1>
      </header>
      <section class="list">
        <button
          v-for="alert in exchange.alerts"
          :key="alert.id"
          @click="router.push(`/exchange/alerts/${alert.id}`)"
          class="alert-item"
        >
          <!-- {{ alert }} -->
          <span
            :class="exchange.getCurrency(alert.currencyCode)?.flagClass"
            class="flag"
          ></span>

          <div class="info">
            <b>{{ alert.currencyCode }}</b>
            <small>{{ exchange.getCurrency(alert.currencyCode)?.name }}</small>
          </div>

          <div class="rates">
            <strong>
              <small>목표 {{ format(alert.targetRate) }}원</small>
            </strong>
            <b
              >현재
              {{ format(exchange.getCurrency(alert.currencyCode)?.rate) }}원</b
            >
          </div>

          <div class="status">
            <em :class="{ off: !alert.enabled }">{{
              alert.enabled ? '활성' : '꺼짐'
            }}</em>
          </div>

          <i class="arrow">›</i>
        </button>
        <div v-if="!exchange.alerts.length" class="empty">
          등록한 환율 알림이 없어요.
        </div>
      </section>

      <aside v-if="reachedAlerts.length > 0">
        <b>환율이 도달했어요!</b>
        <p v-for="alert in reachedAlerts" :key="alert.id">
          {{ alert.currencyCode }}가 목표 환율 {{ format(alert.targetRate) }}원 이하에
          도달했어요.
        </p>
      </aside>

      <button class="cta" @click="router.push('/exchange/alerts/new')">
        ＋ 환율 알림 추가</button
      ><BottomNav />
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
  padding: 52px 18px 30px;
  background: #f7f5ef;
}
header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
header button {
  width: 32px;
  height: 32px;
  font-size: 28px;
  font-weight: 300;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #10192d;
  transition: opacity 0.2s;
}
header button:active {
  opacity: 0.5;
}
header h1 {
  flex: 1;
  padding-right: 32px; /* Offset the 32px back button to center perfectly */
  text-align: center;
  font-size: 18px;
  font-weight: 900;
}
.alert-item {
  display: grid;
  width: 100%;
  grid-template-columns: 28px 1fr auto 60px 16px;
  align-items: center; /* 세로 중앙 정렬 */
  justify-items: center; /* 그리드 내부 요소들 중앙 정렬 */
  gap: 10px;
  margin-top: 10px;
  padding: 16px 14px;
  border: 1px solid #e1e6ed;
  border-radius: 14px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}
.alert-item:active {
  transform: scale(0.99);
  background: #f8fafc;
}
.flag {
  display: inline-block;
  width: 28px;
  height: 19px;
  background-size: cover;
  border-radius: 3px;
  vertical-align: middle;
}
.info {
  justify-self: start;
  display: flex;
  flex-direction: column;
}
.info b {
  font-size: 14px;
  font-weight: 700;
  color: #10192d;
}
.info small {
  font-size: 11px;
  color: #7d899a;
  margin-top: 2px;
}
.rates {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
}
.rates small {
  font-size: 11px;
  color: #94a3b8;
}
.rates b {
  font-size: 13px;
  color: #174494;
  font-weight: 700;
}
.status em {
  font-size: 12px;
  font-style: normal;
  color: #0a9f73;
  font-weight: 600;
}
.status em.off {
  color: #9da7b5;
}
.arrow {
  color: #a2acba;
  font-size: 20px;
  font-style: normal;
}
.empty {
  padding: 50px;
  text-align: center;
  color: #94a3b8;
  font-size: 9px;
}
aside {
  margin-top: 15px;
  padding: 14px;
  border: 1px solid #e1e6ed;
  border-radius: 13px;
  background: #fff;
}
aside b {
  font-size: 10px;
}
aside p {
  margin-top: 7px;
  color: #1472ee;
  font-size: 9px;
}
.cta {
  width: 100%;
  margin-top: 14px;
  padding: 14px;
  border-radius: 11px;
  background: #173f8d;
  color: #fff;
  font-weight: 900;
}
</style>
