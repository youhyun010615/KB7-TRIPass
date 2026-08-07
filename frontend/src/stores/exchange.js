import { computed, ref, watch } from 'vue';
import { defineStore } from 'pinia';
import { fetchExchangeRates } from '@/api/exchange';

const STORAGE_KEY = 'tripass-exchange';
const currencies = ref([]); // 빈 배열로 초기화

// 국가 코드에 따른 국기 이모지 매핑 (KRW 제외)
const flagMap = {
  AED: '🇦🇪', // 아랍에미리트
  AUD: '🇦🇺', // 호주
  BDT: '🇧🇩', // 방글라데시
  BHD: '🇧🇭', // 바레인
  BND: '🇧🇳', // 브루나이
  BRL: '🇧🇷', // 브라질
  CAD: '🇨🇦', // 캐나다
  CHF: '🇨🇭', // 스위스
  CLP: '🇨🇱', // 칠레
  CNY: '🇨🇳', // 중국
  CZK: '🇨🇿', // 체코
  DKK: '🇩🇰', // 덴마크
  EGP: '🇪🇬', // 이집트
  EUR: '🇪🇺', // 유럽연합
  GBP: '🇬🇧', // 영국
  HKD: '🇭🇰', // 홍콩
  HUF: '🇭🇺', // 헝가리
  IDR: '🇮🇩', // 인도네시아
  ILS: '🇮🇱', // 이스라엘
  INR: '🇮🇳', // 인도
  JOD: '🇯🇴', // 요르단
  JPY: '🇯🇵', // 일본
  KWD: '🇰🇼', // 쿠웨이트
  KZT: '🇰🇿', // 카자흐스탄
  MXN: '🇲🇽', // 멕시코
  MYR: '🇲🇾', // 말레이시아
  NOK: '🇳🇴', // 노르웨이
  NZD: '🇳🇿', // 뉴질랜드
  OMR: '🇴🇲', // 오만
  PHP: '🇵🇭', // 필리핀
  PKR: '🇵🇰', // 파키스탄
  PLN: '🇵🇱', // 폴란드
  RUB: '🇷🇺', // 러시아
  SAR: '🇸🇦', // 사우디아라비아
  SEK: '🇸🇪', // 스웨덴
  SGD: '🇸🇬', // 싱가포르
  THB: '🇹🇭', // 태국
  TRY: '🇹🇷', // 튀르키예
  TWD: '🇹🇼', // 대만
  USD: '🇺🇸', // 미국
  VND: '🇻🇳', // 베트남
  ZAR: '🇿🇦', // 남아프리카공화국
};

const banks = [
  {
    id: 'kb-gangnam',
    name: 'KB국민은행 강남역지점',
    distance: 350,
    walk: 5,
    address: '서울 강남구 강남대로 396',
    phone: '02-0000-0000',
    hours: '09:00 - 16:00',
    lat: 44,
    top: 43,
    preferentialRate: 1480.1,
  },
  {
    id: 'kb-seolleung',
    name: 'KB국민은행 선릉지점',
    distance: 620,
    walk: 8,
    address: '서울 강남구 테헤란로 412',
    phone: '02-1111-1111',
    hours: '09:00 - 16:00',
    lat: 70,
    top: 28,
    preferentialRate: 1482.3,
  },
  {
    id: 'shinhan-gangnam',
    name: '신한은행 강남중앙지점',
    distance: 780,
    walk: 11,
    address: '서울 강남구 역삼로 152',
    phone: '02-2222-2222',
    hours: '09:00 - 16:00',
    lat: 28,
    top: 68,
    preferentialRate: 1484.5,
  },
];

function loadState() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
  } catch {
    return null;
  }
}

export const useExchangeStore = defineStore('exchange', () => {
  const saved = loadState();
  const selectedCode = ref(saved?.selectedCode || 'EUR');
  const period = ref(saved?.period || '1w');
  const krwAmount = ref(saved?.krwAmount || 100_000);
  const alerts = ref(
    saved?.alerts || [
      { id: 1, code: 'EUR', target: 1480, amount: 100_000, enabled: true },
      { id: 2, code: 'CHF', target: 1700, amount: 150_000, enabled: true },
    ],
  );
  const selectedBankId = ref(saved?.selectedBankId || 'kb-gangnam');
  const selectedCurrency = computed(
    () =>
      currencies.value.find((item) => item.code === selectedCode.value) ||
      currencies.value[0] ||
      {},
  );
  const selectedBank = computed(
    () => banks.find((item) => item.id === selectedBankId.value) || banks[0],
  );
  const foreignAmount = computed(
    () =>
      (krwAmount.value / (selectedCurrency.value.rate || 1)) *
      (selectedCurrency.value.unit || 1),
  );

  function convertForeign(value, currency = selectedCurrency.value) {
    return (Number(value || 0) / (currency.unit || 1)) * (currency.rate || 0);
  }

  function expectedForeign(amount, rate, unit = 1) {
    return (Number(amount || 0) / Number(rate || 1)) * unit;
  }

  function saveAlert(payload) {
    if (payload.id) {
      const index = alerts.value.findIndex((item) => item.id === payload.id);
      if (index >= 0) alerts.value[index] = { ...payload };
    } else alerts.value.push({ ...payload, id: Date.now(), enabled: true });
  }

  function removeAlert(id) {
    alerts.value = alerts.value.filter((item) => item.id !== id);
  }

  function getCurrency(code) {
    return currencies.value.find((item) => item.code === code);
  }
  function getBank(id) {
    return banks.find((item) => item.id === id);
  }

  async function updateExchangeRates() {
    try {
      const data = await fetchExchangeRates();
      // API 응답 데이터를 스토어의 currencies 구조에 맞게 매핑하고 flag 추가
      currencies.value = data.map((item) => ({
        code: item.currencyCode,
        name: item.currencyName,
        rate: item.dealBaseRate,
        change: item.changeAmount,
        unit: 1,
        flag: flagMap[item.currencyCode] || '🏳️', // 매핑된 국기 또는 기본값
      }));
    } catch (error) {
      console.error('Failed to update exchange rates', error);
    }
  }

  watch(
    [selectedCode, period, krwAmount, alerts, selectedBankId],
    () =>
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          selectedCode: selectedCode.value,
          period: period.value,
          krwAmount: krwAmount.value,
          alerts: alerts.value,
          selectedBankId: selectedBankId.value,
        }),
      ),
    { deep: true },
  );
  return {
    currencies,
    banks,
    selectedCode,
    period,
    krwAmount,
    alerts,
    selectedBankId,
    selectedCurrency,
    selectedBank,
    foreignAmount,
    convertForeign,
    expectedForeign,
    saveAlert,
    removeAlert,
    getCurrency,
    getBank,
    updateExchangeRates,
  };
});
