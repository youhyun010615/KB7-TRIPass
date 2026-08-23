<script setup>
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import { useExchangeStore } from '@/stores/exchange';
import alertIcon from '@/assets/icons/alert.svg';

const router = useRouter();
const exchange = useExchangeStore();
const format = (v) =>
  Number(v || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 });

onMounted(async () => {
  if (exchange.currencies.length === 0) {
    await exchange.updateExchangeRates();
  }
  await exchange.fetchAlerts();
});

// 도달한 알림 계산
const reachedAlerts = computed(() => {
  return exchange.alerts.filter((alert) => {
    if (!alert.enabled) return false;
    // countryId를 사용하여 정확한 국가의 환율 조회
    const currentRate = exchange.getCurrencyByCountryId(alert.countryId)?.rate || 0;
    return currentRate > 0 && currentRate <= alert.targetRate;
  });
});
</script>

<template>
  <main class="page">
    <div class="shell">
      <header>
        <button @click="router.back()">‹</button>
        <h1>목표 환율 알림</h1>
        <span aria-hidden="true"></span>
      </header>
      <section class="list">
        <button
          v-for="alert in exchange.alerts"
          :key="alert.id"
          @click="router.push(`/exchange/alerts/${alert.id}`)"
          class="alert-item"
        >
          <span
            :class="exchange.getCurrencyByCountryId(alert.countryId)?.flagClass"
            class="flag"
          ></span>

          <div class="info">
            <b>{{ alert.countryName }} ({{
                exchange.getCurrencyByCountryId(alert.countryId)?.symbol || ''
              }})</b>
            <small>{{ alert.currencyCode }}</small>
          </div>

          <div class="rates">
            <strong>
              <small>목표 {{ format(alert.targetRate) }}원</small>
            </strong>
            <b
              >현재
              {{ format(exchange.getCurrencyByCountryId(alert.countryId)?.rate) }}원</b
            >
          </div>
          <i class="arrow">›</i>
        </button>
        <div v-if="!exchange.alerts.length" class="empty">
          <div class="alert-visual" aria-hidden="true">
            <span class="pulse-ring pulse-ring-one"></span>
            <span class="pulse-ring pulse-ring-two"></span>
            <span class="icon-bubble">
              <img :src="alertIcon" alt="" />
              <i></i>
            </span>
          </div>
          <strong>등록된 환율 알림이 없어요</strong>
          <p>
            목표 환율을 등록해 목표 환율에 도달하면<br />
            알림을 받아 미리 환전해 두세요.
          </p>
        </div>
      </section>

      <aside v-if="reachedAlerts.length > 0">
        <b>환율이 도달했어요!</b>
        <p v-for="alert in reachedAlerts" :key="alert.id">
          {{
            exchange.getCurrency(alert.currencyCode)?.symbol ||
            alert.currencyCode
          }}가 목표 환율 {{ format(alert.targetRate) }}원 이하에 도달했어요.
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
  background: #e8edf6;
  color: #10192d;
}
.shell {
  position: relative;
  width: min(100%, 390px);
  min-height: 100vh;
  margin: auto;
  padding: 14px 18px 108px;
  overflow: hidden;
  background:
    radial-gradient(circle at 100% 5%, rgba(54, 105, 201, 0.13), transparent 25%),
    linear-gradient(180deg, #f4f7fd 0%, #eef3fb 48%, #f6f8fc 100%);
}
.shell::before {
  position: absolute;
  top: 88px;
  left: -72px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: rgba(37, 93, 184, 0.035);
  content: '';
  pointer-events: none;
}
header {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: center;
  margin-bottom: 16px;
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
  transition: opacity 0.2s;
}
header button:active {
  opacity: 0.5;
}
header h1 {
  text-align: center;
  font-size: 18px;
  font-weight: 900;
}
.alert-item {
  position: relative;
  display: grid;
  width: 100%;
  grid-template-columns: auto 1fr auto auto;
  align-items: center; /* 세로 중앙 정렬 */
  justify-items: center; /* 그리드 내부 요소들 중앙 정렬 */
  gap: 10px;
  margin-top: 10px;
  padding: 16px 14px;
  border: 1px solid #dbe5f5;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(22, 55, 112, 0.07);
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

.arrow {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  color: #a2acba;
  font-size: 20px;
  font-style: normal;
  margin-left: 4px; /* 환율 금액과의 최소 간격 */
}
.empty {
  position: relative;
  z-index: 1;
  margin-top: 28px;
  padding: 42px 22px 38px;
  border: 1px solid #d8e4f7;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 38px rgba(20, 57, 119, 0.09);
  text-align: center;
}
.empty strong {
  display: block;
  margin-top: 24px;
  color: #122342;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.03em;
}
.empty p {
  margin-top: 10px;
  color: #7788a4;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.7;
  letter-spacing: -0.025em;
}
.alert-visual {
  position: relative;
  display: grid;
  width: 94px;
  height: 94px;
  margin: 0 auto;
  place-items: center;
}
.icon-bubble {
  position: relative;
  z-index: 2;
  display: grid;
  width: 66px;
  height: 66px;
  place-items: center;
  animation: bell-float 2.4s ease-in-out infinite;
}
.icon-bubble img {
  width: 30px;
  height: 30px;
  filter: invert(23%) sepia(77%) saturate(1471%) hue-rotate(196deg)
    brightness(86%) contrast(91%);
}
.icon-bubble i {
  position: absolute;
  top: 11px;
  right: 11px;
  width: 8px;
  height: 8px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #ffd45f;
}
.pulse-ring {
  position: absolute;
  inset: 5px;
  border: 1px solid rgba(49, 104, 203, 0.28);
  border-radius: 50%;
  animation: alert-pulse 2.4s ease-out infinite;
}
.pulse-ring-two {
  animation-delay: 1.2s;
}
@keyframes bell-float {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-5px) rotate(3deg);
  }
}
@keyframes alert-pulse {
  0% {
    opacity: 0;
    transform: scale(0.72);
  }
  25% {
    opacity: 0.8;
  }
  100% {
    opacity: 0;
    transform: scale(1.12);
  }
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
  position: relative;
  z-index: 1;
  width: 100%;
  margin-top: 18px;
  padding: 15px;
  border-radius: 14px;
  background: linear-gradient(135deg, #173f8d, #245cbc);
  color: #fff;
  font-weight: 900;
  box-shadow: 0 10px 22px rgba(23, 63, 141, 0.2);
}
@media (prefers-reduced-motion: reduce) {
  .icon-bubble,
  .pulse-ring {
    animation: none;
  }
}
</style>
