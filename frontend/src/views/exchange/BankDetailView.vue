<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ExchangeTicket from '@/components/exchange/ExchangeTicket.vue';
import { useExchangeStore } from '@/stores/exchange';
import { fetchBankDetail } from '@/api/exchange';

const route = useRoute();
const router = useRouter();
const exchange = useExchangeStore();
const bank = ref(null);
const distanceInfo = ref(null);
const currency = computed(() => exchange.selectedCurrency);

onMounted(async () => {
  try {
    // NearbyBanksView에서 전달된 state 가져오기
    distanceInfo.value = window.history.state;
    bank.value = await fetchBankDetail(route.params.bankId);
  } catch (error) {
    console.error('은행 상세 정보를 가져오는데 실패했습니다:', error);
  }
});

// 거리(정수) 및 도보 시간(1m/초 = 1분당 60m 이동 기준) 계산
const displayDistance = computed(() =>
  Math.round(distanceInfo.value?.distance || 0),
);
const displayWalkTime = computed(
  () => Math.round((distanceInfo.value?.distance || 0) / 60) || 1,
);

const receive = computed(() =>
  exchange.expectedForeign(
    100000,
    bank.value?.preferentialRate,
    currency.value?.unit || 1,
  ),
);
const format = (v) =>
  Number(v || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 });
const notice = (text) =>
  window.alert(`${text} 기능은 실제 연동 전 Mock 화면입니다.`);
</script>

<template>
  <main class="page">
    <div class="shell">
      <header>
        <button @click="router.back()">‹</button>
        <h1>은행 상세</h1>
      </header>
      <template v-if="bank"
        ><ExchangeTicket
          :title="bank.name"
          :subtitle="
            distanceInfo
              ? `현재 위치에서 ${displayDistance}m · 도보 ${displayWalkTime}분`
              : ''
          "
        />
        <section class="estimate">
          <small>100,000원 환전 시</small
          ><strong>약 €{{ format(receive) }}</strong>
          <dl>
            <div>
              <dt>현재 환율</dt>
              <dd>{{ format(currency.rate) }}원</dd>
            </div>
            <div>
              <dt>은행 적용 환율</dt>
              <dd>{{ format(bank.preferentialRate) }}원</dd>
            </div>
            <div>
              <dt>기준 환율</dt>
              <dd>{{ format(currency.rate - 12.4) }}원</dd>
            </div>
          </dl>
          <p>↗ 환율 우대 1.75%</p>
        </section>
        <section class="info">
          <h2>영업 정보</h2>
          <div>
            <span>오늘 영업시간</span><b>{{ bank.businessHours }}</b>
          </div>
          <div>
            <span>주소</span><b>{{ bank.address }}</b>
          </div>
          <div>
            <span>전화</span><b>{{ bank.telephone }}</b>
          </div>
        </section>
        <aside>
          <b>방문 전 확인</b>
          <p>외화 재고와 우대 가능 여부는 지점 상황에 따라 달라질 수 있어요.</p>
        </aside>
        <button class="primary" @click="notice('길찾기')">길찾기</button
        ><button class="secondary" @click="notice('전화 문의')">
          전화 문의
        </button></template
      >
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
  margin-bottom: 16px;
}
header button {
  width: 28px;
  font-size: 25px;
}
header h1 {
  flex: 1;
  padding-right: 28px;
  text-align: center;
  font-size: 18px;
  font-weight: 900;
}
.estimate,
.info,
aside {
  margin-top: 12px;
  padding: 15px;
  border: 1px solid #e1e6ed;
  border-radius: 14px;
  background: #fff;
}
.estimate small,
.estimate strong {
  display: block;
}
.estimate small {
  color: #8c98a8;
  font-size: 8px;
}
.estimate strong {
  margin: 7px 0;
  color: #174494;
  font-size: 24px;
}
.estimate dl {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 12px;
}
.estimate dl > div {
  text-align: center;
}
.estimate dl > div + div {
  border-left: 1px solid #e7ebf0;
}
.estimate dt {
  color: #8c97a7;
  font-size: 7px;
}
.estimate dd {
  margin-top: 5px;
  font-size: 8px;
  font-weight: 900;
}
.estimate p {
  margin-top: 11px;
  text-align: center;
  color: #1472ee;
  font-size: 8px;
}
.info h2 {
  font-size: 11px;
}
.info div {
  display: flex;
  justify-content: space-between;
  gap: 15px;
  padding: 11px 0;
  border-bottom: 1px solid #eef1f5;
  font-size: 8px;
}
.info span {
  color: #8995a5;
}
.info b {
  text-align: right;
}
aside {
  background: #fff8e7;
  color: #8b6413;
}
aside b {
  font-size: 9px;
}
aside p {
  margin-top: 6px;
  font-size: 8px;
  line-height: 1.5;
}
.primary,
.secondary {
  width: 100%;
  margin-top: 12px;
  padding: 13px;
  border-radius: 10px;
  background: #173f8d;
  color: #fff;
  font-weight: 900;
}
.secondary {
  margin-top: 7px;
  border: 1px solid #173f8d;
  background: #fff;
  color: #173f8d;
}
</style>
