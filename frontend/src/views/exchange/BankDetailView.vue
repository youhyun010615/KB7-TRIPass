<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useExchangeStore } from '@/stores/exchange';
import { fetchBankDetail, fetchExchangeEstimate } from '@/api/exchange';

const route = useRoute();
const router = useRouter();
const exchange = useExchangeStore();
const bank = ref(null);
const distanceInfo = ref(null);
const estimate = ref(null);
const inputAmount = ref(exchange.krwAmount || 100000);
const displayAmount = ref('');
const countrySearch = ref('');
const countryDropdownOpen = ref(false);
const koreanCountryCollator = new Intl.Collator('ko-KR', {
  usage: 'sort',
  sensitivity: 'base',
});

// 지원되는 전체 국가를 가나다순으로 제공한다.
const availableCurrencies = computed(() => {
  return [...exchange.currencies].sort((a, b) =>
    koreanCountryCollator.compare(
      String(a.countryName || a.name || a.code),
      String(b.countryName || b.name || b.code),
    ),
  );
});

// 현재 활성화된 통화 코드 (기본값은 스토어에서 선택 중인 통화)
const selectedCurrencyCode = ref(exchange.selectedCode || 'EUR');
const selectedCountryId = ref(exchange.selectedCountryId ?? null);
const selectedCurrency = computed(
  () =>
    availableCurrencies.value.find(
      (currency) =>
        selectedCountryId.value != null &&
        currency.countryId === selectedCountryId.value,
    ) ||
    availableCurrencies.value.find(
      (currency) => currency.code === selectedCurrencyCode.value,
    ),
);
const filteredCurrencies = computed(() => {
  const keyword = countrySearch.value.trim().toLocaleLowerCase('ko-KR');
  if (!keyword) return availableCurrencies.value;
  return availableCurrencies.value.filter((currency) =>
    [currency.countryName, currency.name, currency.code]
      .filter(Boolean)
      .some((value) =>
        String(value).toLocaleLowerCase('ko-KR').includes(keyword),
      ),
  );
});

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
        (c) =>
          c.code === selectedCurrencyCode.value &&
          (selectedCountryId.value == null ||
            c.countryId === selectedCountryId.value),
      )
    ) {
      selectedCurrencyCode.value = availableCurrencies.value[0]?.code || 'USD';
      selectedCountryId.value = availableCurrencies.value[0]?.countryId ?? null;
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
const selectCurrency = async (currency) => {
  const code = currency.code;
  selectedCurrencyCode.value = code;
  selectedCountryId.value = currency.countryId ?? null;
  exchange.selectedCode = code;
  exchange.selectedCountryId = currency.countryId ?? null;
  countryDropdownOpen.value = false;
  countrySearch.value = '';
  const requestedCode = code;

  // 1. 이미 캐시된 데이터가 존재하면 즉시 사용
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

  // 💡 [수정] 금액이 변경되었으므로 기존의 캐시 데이터를 초기화합니다.
  cachedEstimates.value = {};

  // 💡 [수정] 백엔드에서 내려준 estimatedAmount가 있다면, 입력한 금액 비율에 맞춰 실시간 갱신해줍니다.
  if (estimate.value) {
    // 100,000원 기준 계산 데이터를 현재 입력금액 비율만큼 재계산
    if (estimate.value.buyRate) {
      const buyRate = Number(estimate.value.buyRate || 1);
      const unit = Number(estimate.value.unit || 1);
      estimate.value = {
        ...estimate.value,
        estimatedAmount: (rawNum / buyRate) * unit,
      };
    }
  }
};

// 실시간 프론트엔드 계산
const computedEstimatedAmount = computed(() => {
  if (!estimate.value) return 0;

  const buyRate = Number(estimate.value.buyRate || 1);
  const unit = Number(estimate.value.unit || 1);

  // buyRate(살 때 환율)와 unit(통화 단위, 예: JPY는 100) 기반으로 실시간 계산
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
          </div>
          <span aria-hidden="true"></span>
        </header>
        <section class="estimate">
          <div class="currency-heading">
            <div>
              <small>EXCHANGE CURRENCY</small>
              <h2>환전할 통화를 선택하세요</h2>
            </div>
            <span>{{ availableCurrencies.length }}개 국가</span>
          </div>
          <div class="country-picker" :class="{ open: countryDropdownOpen }">
            <button
              type="button"
              class="country-trigger"
              :aria-expanded="countryDropdownOpen"
              @click="countryDropdownOpen = !countryDropdownOpen"
            >
              <img v-if="selectedCurrency?.flagUrl" :src="selectedCurrency.flagUrl" alt="" />
              <span v-else class="fi country-flag" :class="selectedCurrency?.flagClass" aria-hidden="true"></span>
              <b>{{ selectedCurrency?.countryName }}</b>
              <span>{{ selectedCurrency?.name }} · {{ selectedCurrencyCode }}</span>
              <i>⌄</i>
            </button>
            <div v-if="countryDropdownOpen" class="country-dropdown">
              <label class="country-search">
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <circle cx="11" cy="11" r="6.5" stroke="currentColor" stroke-width="1.8" />
                  <path d="m16 16 4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                </svg>
                <input v-model="countrySearch" type="search" placeholder="국가명 또는 통화 검색" />
              </label>
              <div class="country-list">
                <button
                  v-for="currency in filteredCurrencies"
                  :key="currency.countryId ?? `${currency.code}-${currency.countryName}`"
                  type="button"
                  :class="{
                    active:
                      selectedCurrency?.countryId != null
                        ? selectedCurrency.countryId === currency.countryId
                        : selectedCurrencyCode === currency.code,
                  }"
                  @click="selectCurrency(currency)"
                >
                  <img v-if="currency.flagUrl" :src="currency.flagUrl" alt="" />
                  <span v-else class="fi country-flag" :class="currency.flagClass" aria-hidden="true"></span>
                  <b>{{ currency.countryName }}</b>
                  <span>{{ currency.name }} <small>{{ currency.code }}</small></span>
                </button>
                <p v-if="!filteredCurrencies.length">검색 결과가 없어요.</p>
              </div>
            </div>
          </div>

          <div class="conversion-line">
            <input
              type="text"
              :value="displayAmount"
              @input="onAmountInput"
              class="amount-input"
              placeholder="0"
            />
            <span class="currency-label">원 환전 시</span>
            <strong
              >약
              {{
                exchange.getCurrency(estimate.currencyCode)?.symbol ||
                estimate.currencyCode
              }}{{ format(computedEstimatedAmount) }}</strong
            >
          </div>
          <div class="conversion-meta">
            <em>↗ 환율 우대 {{ estimate.buyFeeRate }}%</em>
            <b>환율 {{ format(estimate.buyRate) }}원 적용</b>
            <span>({{ exchange.lastUpdateDate }} 11:00 기준)</span>
          </div>
        </section>
        <section class="info">
          <h2>지점 정보</h2>
          <div>
            <span>영업점명</span><b>{{ bank.branchName }}</b>
          </div>
          <div>
            <span>주소</span><b>{{ bank.address }}</b>
          </div>
          <div>
            <span>영업 시간</span><b>{{ bank.businessHours }}</b>
          </div>
          <div>
            <span>거리</span><b>{{ displayDistance }}</b>
          </div>
          <div>
            <span>도보 시간</span><b>약 {{ displayWalkTime }}분</b>
          </div>
          <div>
            <span>전화</span>
            <span class="phone-value">
              <b>{{ bank.telephone }}</b>
              <button
                type="button"
                class="copy-button"
                aria-label="전화번호 복사"
                @click="copyToClipboard(bank.telephone)"
              >
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <rect x="8" y="8" width="10" height="11" rx="2" stroke="currentColor" stroke-width="1.8" />
                  <path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                </svg>
              </button>
            </span>
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
  background: #e8edf6;
  color: #10192d;
}
.shell {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: auto;
  padding: 14px 18px 34px;
  background:
    radial-gradient(circle at 100% 4%, rgba(48, 103, 202, .12), transparent 25%),
    linear-gradient(180deg, #f2f6fc 0%, #eef3fa 100%);
}
header {
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
  cursor: pointer;
}
.header-title {
  display: flex;
  flex-direction: column;
  align-items: center;
}
header h1 {
  color: #152746;
  font-size: 17px;
  font-weight: 900;
  line-height: 1.2;
}
.header-subtitle {
  align-items: center;
  font-size: 11px;
  color: #8c98a8;
  margin-top: 2px;
  font-weight: 500;
}
.estimate,
.info,
aside {
  margin-top: 12px;
  padding: 17px;
  border: 1px solid #dbe5f4;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(24, 56, 111, .07);
}
.estimate {
  background: linear-gradient(155deg, #fff 0%, #f7faff 100%);
}
.estimate small,
.estimate strong {
  display: block;
}
.currency-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}
.currency-heading small {
  color: #2f6fea;
  font-family: 'Space Mono', monospace;
  font-size: 8px;
  font-weight: 800;
  letter-spacing: .12em;
}
.currency-heading h2 {
  margin-top: 4px;
  color: #173f8d;
  font-size: 15px;
  font-weight: 900;
}
.currency-heading > span {
  padding: 5px 8px;
  border-radius: 999px;
  background: #eaf1ff;
  color: #2f67c8;
  font-size: 9px;
  font-weight: 800;
  white-space: nowrap;
}
.country-picker {
  position: relative;
  z-index: 5;
  margin-top: 13px;
}
.country-trigger {
  display: grid;
  width: 100%;
  min-height: 54px;
  grid-template-columns: 24px minmax(64px, auto) 1fr 18px;
  align-items: center;
  gap: 9px;
  padding: 10px 13px;
  border: 1px solid #dce5f2;
  border-radius: 14px;
  background: #fff;
  color: #152746;
  text-align: left;
}
.country-picker.open .country-trigger {
  border-color: #9ebcf0;
  border-radius: 14px 14px 0 0;
}
.country-trigger img,
.country-list img,
.country-flag {
  width: 22px;
  height: 16px;
  border-radius: 3px;
  object-fit: cover;
  background-size: cover;
}
.country-trigger b,
.country-list b {
  font-size: 12px;
  font-weight: 900;
}
.country-trigger > span:not(.country-flag) {
  overflow: hidden;
  color: #8795aa;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.country-trigger i {
  color: #8290a5;
  font-size: 17px;
  font-style: normal;
  transition: transform .2s;
}
.country-picker.open .country-trigger i { transform: rotate(180deg); }
.country-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  left: 0;
  overflow: hidden;
  border: 1px solid #9ebcf0;
  border-top: 0;
  border-radius: 0 0 14px 14px;
  background: #fff;
  box-shadow: 0 16px 28px rgba(22, 58, 121, .15);
}
.country-search {
  display: grid;
  grid-template-columns: 18px 1fr;
  align-items: center;
  gap: 8px;
  margin: 10px;
  padding: 9px 11px;
  border-radius: 10px;
  background: #f1f5fb;
  color: #78879d;
}
.country-search svg { width: 17px; height: 17px; }
.country-search input {
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #17243b;
  font-size: 11px;
  font-weight: 700;
}
.country-list {
  max-height: 230px;
  overflow-y: auto;
  border-top: 1px solid #eef2f7;
}
.country-list button {
  display: grid;
  width: 100%;
  min-height: 44px;
  grid-template-columns: 24px minmax(70px, auto) 1fr;
  align-items: center;
  gap: 9px;
  padding: 9px 13px;
  border-bottom: 1px solid #eff3f8;
  color: #17243b;
  text-align: left;
}
.country-list button.active { background: #edf4ff; color: #245fc6; }
.country-list button > span:not(.country-flag) {
  color: #7f8da2;
  font-size: 10px;
}
.country-list button > span small {
  display: inline;
  margin-left: 3px;
  color: #a1adbd;
  font-size: 8px;
}
.country-list > p {
  padding: 24px;
  color: #8c98aa;
  font-size: 11px;
  text-align: center;
}
.conversion-line {
  display: flex;
  align-items: baseline;
  gap: 5px;
  width: 100%;
  margin: 17px 0 6px;
  padding: 13px 12px;
  border: 1px solid #dfe7f2;
  border-radius: 14px;
  background: #f5f8fc;
  white-space: nowrap;
}
.amount-input {
  width: 88px;
  min-width: 0;
  border: none;
  border-bottom: 1px solid #afbdd0;
  padding: 2px;
  font-size: 13px;
  font-weight: 900;
  color: #10192d;
  background: transparent;
  outline: none;
  text-align: right;
}
.amount-input:focus {
  border-bottom: 2px solid #173f8d;
}
.currency-label {
  flex: none;
  color: #77869b;
  font-size: 10px;
}
.estimate small {
  color: #8c98a8;
  font-size: 12px;
}
.conversion-line > strong {
  min-width: 0;
  margin-left: auto;
  color: #174494;
  font-size: 17px;
  font-weight: 900;
  letter-spacing: -.03em;
  white-space: nowrap;
}
.conversion-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 8px;
  padding: 0 3px;
  letter-spacing: -.015em;
}
.conversion-meta em {
  color: #1472ee;
  font-size: 9px;
  font-weight: 800;
  font-style: normal;
}
.conversion-meta b {
  color: #168065;
  font-size: 10.5px;
  font-weight: 900;
}
.conversion-meta span {
  flex-basis: 100%;
  color: #8795aa;
  font-size: 9px;
  font-weight: 600;
}
.info h2 {
  color: #173f8d;
  font-size: 15px;
  font-weight: 900;
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
.phone-value {
  display: flex;
  align-items: center;
  gap: 7px;
}
.phone-value b {
  color: #10192d;
}
.copy-button {
  display: grid;
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 1px solid #d9e3f2;
  border-radius: 9px;
  background: #edf3ff;
  color: #275fbd;
}
.copy-button svg {
  width: 15px;
  height: 15px;
}
.copy-button:active {
  opacity: .65;
}
aside {
  border-color: #f4c8c8;
  background: linear-gradient(145deg, #fff3f3, #ffe9e9);
  color: #a43b3b;
  box-shadow: 0 10px 24px rgba(182, 55, 55, .08);
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
