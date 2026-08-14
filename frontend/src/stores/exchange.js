import { computed, ref, watch } from 'vue';
import { defineStore } from 'pinia';
import {
  fetchExchangeRates,
  getExchangeAlerts,
  registerExchangeAlert,
  updateExchangeAlert,
  deleteExchangeAlert,
} from '@/api/exchange';
import currencyUnits from '@/assets/currencyUnits.json';
import currencySymbols from '@/assets/currencySymbols.json';

const STORAGE_KEY = 'tripass-exchange';
const currencies = ref([]); // 빈 배열로 초기화

// flag-icons 클래스 매핑 객체 (스토어 외부 export)
export const flagClassMap = {
  AED: 'fi fi-ae', // 아랍에미리트 디르함
  AUD: 'fi fi-au', // 호주 달러
  BHD: 'fi fi-bh', // 바레인 디나르
  BND: 'fi fi-bn', // 브루나이 달러
  CAD: 'fi fi-ca', // 캐나다 달러
  CHF: 'fi fi-ch', // 스위스 프랑
  CNH: 'fi fi-cn', // 위안화 (CNH 포함)
  DKK: 'fi fi-dk', // 덴마크 크로네
  EUR: 'fi fi-eu', // 유로
  GBP: 'fi fi-gb', // 영국 파운드
  HKD: 'fi fi-hk', // 홍콩 달러
  IDR: 'fi fi-id', // 인도네시아 루피아
  JPY: 'fi fi-jp', // 일본 엔
  KWD: 'fi fi-kw', // 쿠웨이트 디나르
  MYR: 'fi fi-my', // 말레이시아 링기트
  NOK: 'fi fi-no', // 노르웨이 크로네
  NZD: 'fi fi-nz', // 뉴질랜드 달러
  SAR: 'fi fi-sa', // 사우디 리얄
  SEK: 'fi fi-se', // 스웨덴 크로나
  SGD: 'fi fi-sg', // 싱가포르 달러
  THB: 'fi fi-th', // 태국 바트
  USD: 'fi fi-us', // 미국 달러
};

// 국가 코드에 따른 통화 코드 매핑 (공통 계약)
export const countryToCurrency = {
  FR: 'EUR',
  CH: 'CHF',
  DE: 'EUR',
  JP: 'JPY',
  HK: 'HKD',
};

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
  const currentTab = ref(saved?.currentTab || 'rate');
  const krwAmount = ref(saved?.krwAmount || 100_000);
  const interestedCurrencyCodes = ref(
    saved?.interestedCurrencyCodes || ['EUR', 'JPY', 'USD'],
  );
  const alerts = ref([]); // Fetch from API instead of localStorage
  const lastUpdateDate = ref('');

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

  // 알림 관련

  async function fetchAlerts() {
    try {
      const data = await getExchangeAlerts();
      alerts.value = data.map((item) => ({
        ...item,
      }));
    } catch (e) {
      console.error('Failed to fetch exchange alerts', e);
    }
  }

  async function saveAlert(payload) {
    try {
      if (payload.id) {
        // PUT: ExchangeRateAlertUpdateRequestDto
        const updatePayload = {
          targetRate: payload.targetRate,
        };
        await updateExchangeAlert(payload.id, updatePayload);
      } else {
        const createPayload = {
          currencyCode: payload.currencyCode,
          targetRate: payload.targetRate,
        };
        await registerExchangeAlert(createPayload);
      }
      await fetchAlerts();
    } catch (e) {
      console.error('Failed to save exchange alert', e);
    }
  }

  async function removeAlert(id) {
    try {
      await deleteExchangeAlert(id);
      await fetchAlerts();
    } catch (e) {
      console.error('Failed to delete exchange alert', e);
    }
  }

  function toggleInterest(code) {
    if (interestedCurrencyCodes.value.includes(code)) {
      interestedCurrencyCodes.value = interestedCurrencyCodes.value.filter(
        (c) => c !== code,
      );
    } else {
      interestedCurrencyCodes.value.push(code);
    }
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
      let maxDate = '';

      // API 응답 데이터를 스토어의 currencies 구조에 맞게 매핑하고 flagClass 추가
      currencies.value = data.map((item) => {
        let cleanCode = (item.currencyCode || '')
          .replace(/\(100\)/g, '')
          .trim();

        const unit = currencyUnits[cleanCode] || 1;

        if (item.rateDate && item.rateDate > maxDate) {
          maxDate = item.rateDate;
        }

        return {
          code: cleanCode,
          name: item.currencyName,
          rate: item.dealBaseRate * unit,
          change: (item.changeAmount || 0) * unit,
          unit: unit,
          flagClass: flagClassMap[cleanCode] || 'fi fi-un',
          symbol: currencySymbols[cleanCode] || cleanCode,
        };
      });

      if (maxDate) {
        const dateObj = new Date(maxDate);
        const yyyy = dateObj.getFullYear();
        const mm = String(dateObj.getMonth() + 1).padStart(2, '0');
        const dd = String(dateObj.getDate()).padStart(2, '0');
        lastUpdateDate.value = `${yyyy}.${mm}.${dd} 11:00`;
      }
    } catch (error) {
      console.error('Failed to update exchange rates', error);
    }
  }

  watch(
    [
      selectedCode,
      period,
      currentTab,
      krwAmount,
      selectedBankId,
      interestedCurrencyCodes,
    ],
    () =>
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          selectedCode: selectedCode.value,
          period: period.value,
          currentTab: currentTab.value,
          krwAmount: krwAmount.value,
          selectedBankId: selectedBankId.value,
          interestedCurrencyCodes: interestedCurrencyCodes.value,
        }),
      ),
    { deep: true },
  );

  return {
    currencies,
    selectedCode,
    period,
    currentTab,
    krwAmount,
    alerts,
    lastUpdateDate,
    selectedBankId,
    interestedCurrencyCodes,
    selectedCurrency,
    selectedBank,
    foreignAmount,
    flagClassMap,
    convertForeign,
    expectedForeign,
    fetchAlerts,
    saveAlert,
    removeAlert,
    toggleInterest,
    getCurrency,
    getBank,
    updateExchangeRates,
  };
});
