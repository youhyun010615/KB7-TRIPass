<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ExchangeTicket from '@/components/exchange/ExchangeTicket.vue';
import { useExchangeStore, countryToCurrency } from '@/stores/exchange';
import { useTravelStore } from '@/stores/travel';
import { fetchBankDetail, fetchExchangeEstimate } from '@/api/exchange';

const route = useRoute();
const router = useRouter();
const exchange = useExchangeStore();
const travel = useTravelStore();
const bank = ref(null);
const distanceInfo = ref(null);
const estimate = ref(null);
const inputAmount = ref(exchange.krwAmount || 100000);
const displayAmount = ref('');

// 사용자의 여행 일정 및 관심 통화를 수집하여 보여줄 통화 필터링
const availableCurrencies = computed(() => {
  const codes = new Set([
    ...exchange.interestedCurrencyCodes,
    ...exchange.alerts.map((a) => a.currencyCode),
    ...travel.selectedPlans.map((p) => countryToCurrency[p.code] || 'USD'),
  ]);

  // 만약 등록된 정보가 비어있다면 대중적인 3대 통화 제공
  if (codes.size === 0) {
    codes.add('USD');
    codes.add('JPY');
    codes.add('EUR');
  }

  return exchange.currencies.filter((c) => codes.has(c.code));
});

// 현재 활성화된 통화 코드 (기본값은 스토어에서 선택 중인 통화)
const selectedCurrencyCode = ref(exchange.selectedCode || 'EUR');

// 메모리 캐싱 저장소 (통화 코드별 데이터 캐싱)
const cachedEstimates = ref({});

// 콤마 포맷팅 함수
const formatWithCommas = (num) => {
  if (!num && num !== 0) return '';
  return Number(num).toLocaleString('ko-KR');
};

// 숫자가 아닌 문자 제거 후 파싱
const parseNumber = (str) => {
  return Number(str.replace(/[^0-9]/g, '')) || 0;
};

onMounted(async () => {
  try {
    distanceInfo.value = window.history.state;
    bank.value = await fetchBankDetail(route.params.bankId);
    displayAmount.value = formatWithCommas(inputAmount.value);

    // 환율 정보가 비어있다면 즉시 로드! (다이렉트 진입 및 새로고침 대응)
    if (exchange.currencies.length === 0) {
      await exchange.updateExchangeRates();
    }

    // 스토어 선택 통화가 사용 가능한 통화 목록에 없는 극단적 상황 방어
    if (
      !availableCurrencies.value.some(
        (c) => c.code === selectedCurrencyCode.value,
      )
    ) {
      selectedCurrencyCode.value =
        availableCurrencies.value[0]?.code || 'USD';
    }

    // 최초 통화로 우대 환율 정보 로드 및 캐시 저장
    const initialCode = selectedCurrencyCode.value;
    const data = await fetchExchangeEstimate(inputAmount.value, initialCode);
    estimate.value = data;
    cachedEstimates.value[initialCode] = data;
  } catch (error) {
    console.error('은행 상세 정보를 가져오는데 실패했습니다:', error);
  }
});

// 통화 변경 및 인메모리 캐싱 처리
const selectCurrency = async (code) => {
  selectedCurrencyCode.value = code;
  const requestedCode = code;

  // 1. 이미 캐시된 데이터가 존재하면 즉시 반환 (API 호출 0회, 0초 렉)
  if (cachedEstimates.value[code]) {
    estimate.value = cachedEstimates.value[code];
    return;
  }

  // 2. 캐시가 없으면 서버에 조회 요청 후 캐시에 보관
  try {
    const data = await fetchExchangeEstimate(inputAmount.value, code);
    cachedEstimates.value[code] = data;
    if (selectedCurrencyCode.value === requestedCode) {
      estimate.value = data;
    }
  } catch (error) {
    console.error(`${code} 우대 정보 갱신 실패:`, error);
  }
};

const onAmountInput = (event) => {
  const value = event.target.value;
  const rawNum = parseNumber(value);
  inputAmount.value = rawNum;
  displayAmount.value = formatWithCommas(rawNum);

  // 실시간으로 스토어 금액 동기화
  exchange.krwAmount = rawNum;
};

// 실시간 프론트엔드 계산 (총액 상관없이 환율은 동일하므로 API 재요청 없음!)
const computedEstimatedAmount = computed(() => {
  if (!estimate.value) return 0;
  const buyRate = Number(estimate.value.buyRate || 1);
  const unit = Number(estimate.value.unit || 1);
  return (inputAmount.value / buyRate) * unit;
});

// 거리(정수) 및 도보 시간(1m/초 = 1분당 60m 이동 기준) 계산
const displayDistance = computed(() => {
  const dist = Math.round(distanceInfo.value?.distance || 0);
  if (dist >= 1000) {
    return `${(dist / 1000).toFixed(1)}km`;
  }
  return `${dist}m`;
});
const displayWalkTime = computed(
  () => Math.round((distanceInfo.value?.distance || 0) / 60) || 1,
);

const format = (v) =>
  Number(v || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 });

const copyToClipboard = (text) => {
  navigator.clipboard
    .writeText(text)
    .then(() => {
      alert('전화번호가 복사되었습니다.');
    })
    .catch((err) => {
      console.error('복사 실패:', err);
    });
};
</script>

<template>
  <main class="page">
    <div class="shell">
      <template v-if="bank && estimate">
        <header>
          <button @click="router.back()">‹</button>
          <div class="header-title">
            <h1>{{ bank.branchName }}</h1>
            <small v-if="distanceInfo" class="header-subtitle">
              {{ displayDistance }} · {{ displayWalkTime }}분
            </small>
          </div>
        </header>
        <section class="estimate">
          <!-- 개인 맞춤형 미니 통화 셀렉터 -->
          <div class="mini-currency-selector">
            <button
              v-for="curr in availableCurrencies"
              :key="curr.code"
              :class="{ active: selectedCurrencyCode === curr.code }"
              @click="selectCurrency(curr.code)"
            >
              <span class="fi" :class="curr.flagClass"></span>
              {{ curr.code }}
            </button>
          </div>

          <div class="input-container">
            <input
              type="text"
              :value="displayAmount"
              @input="onAmountInput"
              class="amount-input"
              placeholder="0"
            />
            <span class="currency-label">원 환전 시</span>
          </div>
          <strong
            >약 {{ estimate.currencyCode }}
            {{ format(computedEstimatedAmount) }}</strong
          >
          <dl>
            <div>
              <dt>현재 환율</dt>
              <dd>{{ format(estimate.baseRate) }}원</dd>
            </div>
            <div>
              <dt>은행 적용 환율</dt>
              <dd>{{ format(estimate.buyRate) }}원</dd>
            </div>
            <div>
              <dt>기준 환율</dt>
              <dd>{{ format(estimate.baseRate) }}원</dd>
            </div>
          </dl>
          <p>↗ 환율 우대 {{ estimate.buyFeeRate }}%</p>
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
            <span>전화</span>
            <b class="clickable" @click="copyToClipboard(bank.telephone)">
              {{ bank.telephone }} 📋
            </b>
          </div>
        </section>
        <aside>
          <b>방문 전 확인</b>
          <p>외화 재고와 우대 가능 여부는 지점 상황에 따라 달라질 수 있어요.</p>
        </aside>
      </template>
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
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  text-align: left;
}
.header-title {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-right: 28px; /* Offset the back button to center perfectly */
}
header h1 {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
}
.header-subtitle {
  font-size: 11px;
  color: #8c98a8;
  margin-top: 2px;
  font-weight: 500;
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
.mini-currency-selector {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 10px;
  margin-bottom: 14px;
  border-bottom: 1px solid #f0f3f6;
  scrollbar-width: none; /* Firefox */
}
.mini-currency-selector::-webkit-scrollbar {
  display: none; /* Chrome, Safari */
}
.mini-currency-selector button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #e1e6ed;
  border-radius: 15px;
  background: #fff;
  color: #5e6b7d;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}
.mini-currency-selector button span.fi {
  border-radius: 1px;
}
.mini-currency-selector button.active {
  border-color: #173f8d;
  background: #f0f4fc;
  color: #173f8d;
}
.input-container {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.amount-input {
  width: 120px;
  border: none;
  border-bottom: 1px solid #c8cfd9;
  padding: 2px 4px;
  font-size: 14px;
  font-weight: 700;
  color: #10192d;
  background: transparent;
  outline: none;
  text-align: right;
}
.amount-input:focus {
  border-bottom: 2px solid #173f8d;
}
.currency-label {
  color: #8c98a8;
  font-size: 12px;
}
.estimate small {
  color: #8c98a8;
  font-size: 12px;
}
.estimate strong {
  margin: 7px 0;
  color: #174494;
  font-size: 28px;
}
.estimate dl {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 16px;
}
.estimate dl > div {
  text-align: center;
}
.estimate dl > div + div {
  border-left: 1px solid #e7ebf0;
}
.estimate dt {
  color: #8c97a7;
  font-size: 11px;
}
.estimate dd {
  margin-top: 5px;
  font-size: 13px;
  font-weight: 700;
}
.estimate p {
  margin-top: 14px;
  text-align: center;
  color: #1472ee;
  font-size: 11px;
}
.info h2 {
  font-size: 15px;
}
.info div {
  display: flex;
  justify-content: space-between;
  gap: 15px;
  padding: 14px 0;
  border-bottom: 1px solid #eef1f5;
  font-size: 12px;
}
.info span {
  color: #8995a5;
}
.info b {
  text-align: right;
}
.clickable {
  cursor: pointer;
  user-select: all;
  transition: opacity 0.2s;
}
.clickable:hover {
  opacity: 0.7;
}
aside {
  background: #fff8e7;
  color: #8b6413;
}
aside b {
  font-size: 13px;
}
aside p {
  margin-top: 8px;
  font-size: 11px;
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
