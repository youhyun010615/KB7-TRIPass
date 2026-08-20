import { computed, reactive, ref } from 'vue';
import { defineStore } from 'pinia';
import {
  confirmTripBudget,
  createTripGoal,
  fetchActiveTripHome,
  fetchActiveTripGoal,
  fetchTripBudget,
  fetchTripCountries,
  fetchTripStatus,
  generateTripBudget,
  updateTripGoal,
  fetchBudgetCheck,
  fetchCurrentTripLifecycle,
  archiveTrip as archiveTripApi,
  acknowledgeTripStartReport,
} from '@/api/travel';

import imageAE from '@/assets/countries/AE.webp';
import imageAT from '@/assets/countries/AT.jpeg';
import imageAU from '@/assets/countries/AU.jpeg';
import imageBH from '@/assets/countries/BH.jpeg';
import imageBN from '@/assets/countries/BN.jpeg';
import imageCA from '@/assets/countries/CA.avif';
import imageCN from '@/assets/countries/CN.avif';
import imageDK from '@/assets/countries/DK.jpeg';
import imageES from '@/assets/countries/ES.jpeg';
import imageFI from '@/assets/countries/FI.jpeg';
import imageGB from '@/assets/countries/GB.jpeg';
import imageGR from '@/assets/countries/GR.jpeg';
import imageGU from '@/assets/countries/GU.webp';
import imageID from '@/assets/countries/ID.webp';
import imageIE from '@/assets/countries/IE.jpeg';
import imageIT from '@/assets/countries/IT.jpeg';
import imageKW from '@/assets/countries/KW.avif';
import imageMY from '@/assets/countries/MY.jpeg';
import imageNL from '@/assets/countries/NL.jpeg';
import imageNO from '@/assets/countries/NO.jpeg';
import imageNZ from '@/assets/countries/NZ.jpeg';
import imagePT from '@/assets/countries/PT.jpg';
import imageSA from '@/assets/countries/SA.jpeg';
import imageSE from '@/assets/countries/SE.jpeg';
import imageSG from '@/assets/countries/SG.jpeg';
import imageTH from '@/assets/countries/TH.avif';
import imageUS from '@/assets/countries/US.jpeg';

export const countryPresentation = {
  아랍에미리트: { code: 'AE', city: '두바이', flag: '🇦🇪', accent: '#00732f', image: imageAE },
  오스트리아: { code: 'AT', city: '빈', flag: '🇦🇹', accent: '#ed2939', image: imageAT },
  호주: { code: 'AU', city: '시드니', flag: '🇦🇺', accent: '#00008b', image: imageAU },
  벨기에: { code: 'BE', city: '브뤼셀', flag: '🇧🇪', accent: '#2d2926' },
  바레인: { code: 'BH', city: '마나마', flag: '🇧🇭', accent: '#ce1126', image: imageBH },
  브루나이: {
    code: 'BN',
    city: '반다르스리브가완',
    flag: '🇧🇳',
    accent: '#f7e017',
    image: imageBN,
  },
  캐나다: { code: 'CA', city: '토론토', flag: '🇨🇦', accent: '#ff0000', image: imageCA },
  스위스: { code: 'CH', city: '인터라켄', flag: '🇨🇭', accent: '#c8173c' },
  중국: { code: 'CN', city: '베이징', flag: '🇨🇳', accent: '#de2910', image: imageCN },
  덴마크: { code: 'DK', city: '코펜하겐', flag: '🇩🇰', accent: '#c8102e', image: imageDK },
  스페인: { code: 'ES', city: '마드리드', flag: '🇪🇸', accent: '#c60b1e', image: imageES },
  핀란드: { code: 'FI', city: '헬싱키', flag: '🇫🇮', accent: '#003580', image: imageFI },
  프랑스: { code: 'FR', city: '파리', flag: '🇫🇷', accent: '#0066ff' },
  독일: { code: 'DE', city: '베를린', flag: '🇩🇪', accent: '#151515' },
  영국: { code: 'GB', city: '런던', flag: '🇬🇧', accent: '#1e3a8a', image: imageGB },
  그리스: { code: 'GR', city: '아테네', flag: '🇬🇷', accent: '#0d5eaf', image: imageGR },
  괌: { code: 'GU', city: '괌', flag: '🇬🇺', accent: '#003f87', image: imageGU },
  홍콩: { code: 'HK', city: '홍콩', flag: '🇭🇰', accent: '#b8202e' },
  인도네시아: { code: 'ID', city: '발리', flag: '🇮🇩', accent: '#c62828', image: imageID },
  아일랜드: { code: 'IE', city: '더블린', flag: '🇮🇪', accent: '#169b62', image: imageIE },
  이탈리아: { code: 'IT', city: '로마', flag: '🇮🇹', accent: '#008c45', image: imageIT },
  일본: { code: 'JP', city: '도쿄', flag: '🇯🇵', accent: '#ef4b91' },
  쿠웨이트: { code: 'KW', city: '쿠웨이트시티', flag: '🇰🇼', accent: '#00897b', image: imageKW },
  말레이시아: {
    code: 'MY',
    city: '쿠알라룸푸르',
    flag: '🇲🇾',
    accent: '#1565c0',
    image: imageMY,
  },
  네덜란드: { code: 'NL', city: '암스테르담', flag: '🇳🇱', accent: '#ae1c28', image: imageNL },
  노르웨이: { code: 'NO', city: '오슬로', flag: '🇳🇴', accent: '#c62828', image: imageNO },
  뉴질랜드: { code: 'NZ', city: '오클랜드', flag: '🇳🇿', accent: '#1565c0', image: imageNZ },
  포르투갈: { code: 'PT', city: '리스본', flag: '🇵🇹', accent: '#006600', image: imagePT },
  사우디아라비아: { code: 'SA', city: '리야드', flag: '🇸🇦', accent: '#15803d', image: imageSA },
  스웨덴: { code: 'SE', city: '스톡홀름', flag: '🇸🇪', accent: '#1e40af', image: imageSE },
  싱가포르: { code: 'SG', city: '싱가포르', flag: '🇸🇬', accent: '#dc2626', image: imageSG },
  태국: { code: 'TH', city: '방콕', flag: '🇹🇭', accent: '#2563eb', image: imageTH },
  미국: { code: 'US', city: '뉴욕', flag: '🇺🇸', accent: '#1d4ed8', image: imageUS },
};

// 유니코드 국기 이모지는 OS/브라우저에 따라 깨져 보이는 경우가 있어(윈도우 등),
// 환율 탭 '모든 국가보기'에서 쓰는 flag-icons 라이브러리 클래스로 통일한다.
// <span :class="flagIconClass(code)" class="fi-inline" /> 형태로 사용.
export function flagIconClass(code) {
  return code ? `fi fi-${String(code).toLowerCase()}` : 'fi fi-un';
}

// 저축모드 홈(SavingsModeHome.vue)의 countryPresentation과 동일한 값 — 여행모드
// 화면들(홈 보딩패스, 여행 자금 체크 등)이 전부 이 하나의 함수를 통해 색을 가져오게 해서
// 화면마다 색이 달라지는 걸 방지한다. (저축모드 파일 자체는 건드리지 않는다)
const curatedTravelCountryColors = {
  프랑스: {
    headerBg: '#1a2d6e',
    progressBg: 'rgba(0,35,149,0.80)',
    barColor: 'linear-gradient(90deg,#002395 0%,#EDEDED 50%,#ED2939 100%)',
  },
  스위스: {
    headerBg: '#7a0d1e',
    progressBg: 'rgba(122,13,30,0.82)',
    barColor: 'linear-gradient(90deg,#FF0000 0%,#FFFFFF 60%,#FF0000 100%)',
  },
  독일: {
    headerBg: '#111111',
    progressBg: 'rgba(17,17,17,0.85)',
    barColor: 'linear-gradient(90deg,#000000 0%,#DD0000 50%,#FFCE00 100%)',
  },
  일본: {
    headerBg: '#c2185b',
    progressBg: 'rgba(194,24,91,0.82)',
    barColor:
      'linear-gradient(90deg,#FFFFFF 0%,#BC002D 35%,#BC002D 65%,#FFFFFF 100%)',
  },
  홍콩: {
    headerBg: '#b8202e',
    progressBg: 'rgba(184,32,46,0.84)',
    barColor: 'linear-gradient(90deg,#DE2910 0%,#FFDE00 100%)',
  },
};
const defaultTravelCountryColor = {
  headerBg: '#173f8d',
  progressBg: 'rgba(23,63,141,0.84)',
  barColor: 'linear-gradient(90deg,#64d8cb,#fff0b3)',
};

function hexToRgba(hex, alpha) {
  const clean = (hex || '').replace('#', '');
  const num = parseInt(clean, 16);
  if (Number.isNaN(num)) return `rgba(23,63,141,${alpha})`;
  const r = (num >> 16) & 255;
  const g = (num >> 8) & 255;
  const b = num & 255;
  return `rgba(${r},${g},${b},${alpha})`;
}

// 국가 이름으로 { headerBg, progressBg, barColor } 를 반환한다 — 여행모드의
// 모든 화면이 국가 색을 정할 때는 이 함수 하나만 쓴다.
export function getTravelCountryColors(countryName) {
  if (curatedTravelCountryColors[countryName]) {
    return curatedTravelCountryColors[countryName];
  }
  const accent = countryPresentation[countryName]?.accent;
  if (!accent) return defaultTravelCountryColor;
  return {
    headerBg: accent,
    progressBg: hexToRgba(accent, 0.84),
    barColor: `linear-gradient(90deg, ${accent} 0%, ${accent}99 100%)`,
  };
}

const accountSeed = [
  {
    id: 1,
    bank: 'KB국민은행',
    name: 'KB국민은행 여행통장',
    number: '**** 5320',
    balance: 1_500_000,
  },
  {
    id: 2,
    bank: '신한은행',
    name: '신한은행 통장',
    number: '**** 8421',
    balance: 3_000_000,
  },
  {
    id: 3,
    bank: '카카오뱅크',
    name: '카카오뱅크 입출금통장',
    number: '**** 1108',
    balance: 2_450_000,
  },
];

const budgetFields = [
  'airfareAmount',
  'lodgingAmount',
  'activityAmount',
  'transportAmount',
  'foodAmount',
  'otherAmount',
];

const localBudgetFields = [
  'activityAmount',
  'transportAmount',
  'foodAmount',
  'otherAmount',
];

function apiErrorMessage(error, fallback) {
  return error.response?.data?.message || fallback;
}

function isNotFound(error, code) {
  return (
    error.response?.status === 404 || error.response?.data?.code === code
  );
}

function decorateCountry(country) {
  const presentation = countryPresentation[country.countryName] || {};
  return {
    id: country.countryId,
    countryId: country.countryId,
    name: country.countryName,
    currencyCode: country.currencyCode,
    currencyName: country.currencyName,
    flagUrl: country.flagUrl,
    code: presentation.code || country.currencyCode,
    city: presentation.city || country.countryName,
    flag: presentation.flag || '✈️',
    accent: presentation.accent || '#2469e8',
    image: presentation.image || null,
    selectable: country.selectable !== false,
  };
}

function normalizeApiDate(value) {
  if (Array.isArray(value)) {
    const [year, month, day] = value;
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  }
  return value || '';
}

function createPlan(country, initial = {}) {
  return {
    ...country,
    startDate: normalizeApiDate(initial.arrivalDate || initial.startDate),
    endDate: normalizeApiDate(initial.departureDate || initial.endDate),
    tripCountryId: initial.tripCountryId || null,
    targetBudget: Number(initial.targetBudget || 0),
    budget: {
      airfareAmount: 0,
      lodgingAmount: 0,
      activityAmount: 0,
      transportAmount: 0,
      foodAmount: 0,
      otherAmount: 0,
    },
    recommendedBudget: {
      airfareAmount: 0,
      lodgingAmount: 0,
      activityAmount: 0,
      transportAmount: 0,
      foodAmount: 0,
      otherAmount: 0,
    },
    aiReason: '',
    isConfirmed: false,
  };
}

function selectedAmount(country, field) {
  const confirmedField = field
    .replace('Amount', '')
    .replace(/^./, (value) => value.toUpperCase());
  const confirmed = country[`confirmed${confirmedField}Amount`];
  const recommended = country[`recommended${confirmedField}Amount`];
  return Number(confirmed ?? recommended ?? 0);
}

export const useTravelStore = defineStore('travel', () => {
  const isTravelMode = ref(false);
  const selectedCountry = ref(null);
  const travelBudget = ref(null);
  const hasTravelGoal = ref(false);
  const initialized = ref(false);
  const tripId = ref(null);
  const activeTrip = ref(null);
  const homeDashboard = ref(null);
  const homeSelectedCountryId = ref(null);
  const homeLoading = ref(false);
  const homeError = ref('');
  const recommendation = ref(null);
  const completion = ref(null);
  const tripName = ref('');
  const countries = ref([]);
  const selectedCountryCodes = ref([]);
  const plans = reactive({});
  const allocations = reactive({});
  const loading = ref(false);
  const countryLoading = ref(false);
  const errorMessage = ref('');
  const tripStatus = ref(null);
  const statusLoading = ref(false);
  const budgetCheckData = ref(null);
  const budgetLoading = ref(false);
  const lifecycle = ref(null);
  const lifecycleLoading = ref(false);

  const countryFlagMap = {
    프랑스: { code: 'fr', class: 'fi fi-fr', emoji: '🇫🇷' },
    독일: { code: 'de', class: 'fi fi-de', emoji: '🇩🇪' },
    스위스: { code: 'ch', class: 'fi fi-ch', emoji: '🇨🇭' },
    일본: { code: 'jp', class: 'fi fi-jp', emoji: '🇯🇵' },
    홍콩: { code: 'hk', class: 'fi fi-hk', emoji: '🇭🇰' },
    이탈리아: { code: 'it', class: 'fi fi-it', emoji: '🇮🇹' },
    스페인: { code: 'es', class: 'fi fi-es', emoji: '🇪🇸' },
    네덜란드: { code: 'nl', class: 'fi fi-nl', emoji: '🇳🇱' },
    벨기에: { code: 'be', class: 'fi fi-be', emoji: '🇧🇪' },
    오스트리아: { code: 'at', class: 'fi fi-at', emoji: '🇦🇹' },
    포르투갈: { code: 'pt', class: 'fi fi-pt', emoji: '🇵🇹' },
    그리스: { code: 'gr', class: 'fi fi-gr', emoji: '🇬🇷' },
    아일랜드: { code: 'ie', class: 'fi fi-ie', emoji: '🇮🇪' },
    핀란드: { code: 'fi', class: 'fi fi-fi', emoji: '🇫🇮' },
    중국: { code: 'cn', class: 'fi fi-cn', emoji: '🇨🇳' },
    괌: { code: 'gu', class: 'fi fi-gu', emoji: '🇬🇺' },
  };

  const aggregatedBudget = computed(() => {
    if (!budgetCheckData.value || !Array.isArray(budgetCheckData.value))
      return null;
    const data = budgetCheckData.value;
    const targetBudget = data.reduce((s, b) => s + (b.targetBudget || 0), 0);
    const preExpenseTotal = data.reduce(
      (s, b) => s + (b.preExpenseTotal || 0),
      0,
    );
    const travelExpenseTotal = data.reduce(
      (s, b) => s + (b.travelExpenseTotal || 0),
      0,
    );
    const remainingFund = data.reduce((s, b) => s + (b.remainingFund || 0), 0);

    const mergedCategories = {};
    data.forEach((b) => {
      b.categoryBreakdown.forEach((c) => {
        mergedCategories[c.categoryName] =
          (mergedCategories[c.categoryName] || 0) + c.amount;
      });
    });

    return {
      targetBudget,
      preExpenseTotal,
      travelExpenseTotal,
      remainingFund,
      categoryBreakdown: Object.entries(mergedCategories).map(
        ([name, amount]) => ({ categoryName: name, amount }),
      ),
    };
  });

  const accounts = accountSeed;
  const selectedPlans = computed(() =>
    selectedCountryCodes.value.map((key) => plans[key]).filter(Boolean),
  );
  const totalTargetAmount = computed(() => {
    if (completion.value)
      return Number(completion.value.localTravelTargetTotal || 0);
    return selectedPlans.value.reduce(
      (sum, plan) =>
        sum +
        localBudgetFields.reduce(
          (subtotal, field) => subtotal + Number(plan.budget[field] || 0),
          0,
        ),
      0,
    );
  });
  const prepaidExpenseTotal = computed(() => {
    if (completion.value)
      return Number(completion.value.prepaidExpenseTotal || 0);
    return selectedPlans.value.reduce(
      (sum, plan) =>
        sum +
        Number(plan.budget.airfareAmount || 0) +
        Number(plan.budget.lodgingAmount || 0),
      0,
    );
  });
  const monthlySavingTarget = computed(() =>
    Number(completion.value?.monthlySavingTarget || 0),
  );
  const currentWalletBalance = computed(() =>
    Number(completion.value?.currentWalletBalance || 0),
  );
  const remainingMonths = computed(() =>
    Number(completion.value?.remainingMonths || 0),
  );
  const totalAllocatedAmount = computed(() =>
    Object.values(allocations).reduce(
      (sum, amount) => sum + Number(amount || 0),
      0,
    ),
  );
  const selectedAccountCount = computed(
    () =>
      Object.values(allocations).filter((amount) => Number(amount) > 0).length,
  );

  function clearError() {
    errorMessage.value = '';
  }

  function clearPlans() {
    selectedCountryCodes.value = [];
    Object.keys(plans).forEach((key) => delete plans[key]);
  }

  async function loadCountries(keyword = '') {
    countryLoading.value = true;
    try {
      const result = await fetchTripCountries(keyword);
      // 선택 가능한 국가를 먼저, 그 안에서는 가나다순으로 정렬한다.
      countries.value = (result || [])
        .map(decorateCountry)
        .sort((a, b) => {
          if (a.selectable !== b.selectable) return a.selectable ? -1 : 1;
          return (a.name || '').localeCompare(b.name || '', 'ko-KR');
        });
      return countries.value;
    } catch (error) {
      errorMessage.value = apiErrorMessage(
        error,
        '국가 목록을 불러오지 못했습니다.',
      );
      return [];
    } finally {
      countryLoading.value = false;
    }
  }

  function toggleCountry(countryId) {
    const key = String(countryId);
    const index = selectedCountryCodes.value.indexOf(key);
    if (index >= 0) {
      selectedCountryCodes.value.splice(index, 1);
      delete plans[key];
      return true;
    }
    if (selectedCountryCodes.value.length >= 5) {
      errorMessage.value = '여행 국가는 최대 5개까지 선택할 수 있어요.';
      return false;
    }
    const country = countries.value.find(
      (item) => String(item.countryId) === key,
    );
    if (!country) return false;
    clearError();
    selectedCountryCodes.value.push(key);
    plans[key] = createPlan(country);
    return true;
  }

  function updatePlan(countryId, patch) {
    const plan = plans[String(countryId)];
    if (!plan) return;
    Object.assign(plan, patch);
  }

  function reorderCountries(fromIndex, toIndex) {
    if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0) return;
    if (
      fromIndex >= selectedCountryCodes.value.length ||
      toIndex >= selectedCountryCodes.value.length
    )
      return;
    const nextOrder = [...selectedCountryCodes.value];
    const [movedCountry] = nextOrder.splice(fromIndex, 1);
    nextOrder.splice(toIndex, 0, movedCountry);
    selectedCountryCodes.value = nextOrder;
  }

  function updateBudget(countryId, field, value) {
    const plan = plans[String(countryId)];
    if (!plan || !budgetFields.includes(field)) return;
    plan.budget[field] = Math.max(0, Number(value) || 0);
    plan.targetBudget = localBudgetFields.reduce(
      (sum, key) => sum + Number(plan.budget[key] || 0),
      0,
    );
  }

  function resetBudgetToRecommendation(countryId) {
    const plan = plans[String(countryId)];
    if (!plan) return;
    for (const field of budgetFields) {
      plan.budget[field] = Number(plan.recommendedBudget[field] || 0);
    }
    plan.targetBudget = localBudgetFields.reduce(
      (sum, key) => sum + Number(plan.budget[key] || 0),
      0,
    );
  }

  function setAllocation(accountId, value) {
    const account = accounts.find((item) => item.id === accountId);
    if (!account) return;
    allocations[accountId] = Math.max(
      0,
      Math.min(Number(value) || 0, account.balance),
    );
  }

  function planError(plan) {
    if (!plan.startDate || !plan.endDate) return '여행 날짜를 선택해 주세요.';
    if (new Date(plan.endDate) <= new Date(plan.startDate))
      return '출발일은 도착일보다 늦어야 해요.';
    return '';
  }

  const hasDateCollision = computed(() =>
    selectedPlans.value.some((plan, index, list) => {
      if (index === 0 || !plan.startDate || !list[index - 1]?.endDate)
        return false;
      return new Date(plan.startDate) < new Date(list[index - 1].endDate);
    }),
  );

  const canReviewPlan = computed(
    () =>
      tripName.value.trim().length > 0 &&
      selectedPlans.value.length > 0 &&
      selectedPlans.value.every((plan) => !planError(plan)) &&
      !hasDateCollision.value,
  );

  const canCompleteGoal = computed(
    () =>
      Boolean(recommendation.value) &&
      selectedPlans.value.length > 0 &&
      selectedPlans.value.every((plan) =>
        budgetFields.every(
          (field) =>
            Number.isFinite(Number(plan.budget[field])) &&
            Number(plan.budget[field]) >= 0,
        ),
      ) &&
      totalTargetAmount.value > 0,
  );

  function goalPayload() {
    return {
      tripName: tripName.value.trim(),
      countries: selectedPlans.value.map((plan, index) => ({
        countryId: plan.countryId,
        arrivalDate: plan.startDate,
        departureDate: plan.endDate,
        displayOrder: index + 1,
      })),
    };
  }

  function applyRecommendation(result) {
    recommendation.value = result;
    for (const country of result?.countries || []) {
      const plan = selectedPlans.value.find(
        (item) => item.name === country.countryName,
      );
      if (!plan) continue;
      plan.tripCountryId = country.tripCountryId;
      plan.aiReason = country.aiReason || '';
      plan.isConfirmed = Boolean(country.isConfirmed);
      for (const field of budgetFields) {
        const recommendedField = field
          .replace('Amount', '')
          .replace(/^./, (value) => value.toUpperCase());
        plan.recommendedBudget[field] = Number(
          country[`recommended${recommendedField}Amount`] ?? 0,
        );
        plan.budget[field] = selectedAmount(country, field);
      }
      plan.targetBudget = localBudgetFields.reduce(
        (sum, field) => sum + Number(plan.budget[field] || 0),
        0,
      );
    }
  }

  async function savePlanAndRecommend() {
    if (!canReviewPlan.value) return false;
    loading.value = true;
    clearError();
    try {
      if (tripId.value) {
        await updateTripGoal(tripId.value, goalPayload());
      } else {
        const created = await createTripGoal(goalPayload());
        if (!created?.tripId) {
          throw new Error('여행 목표 생성 응답에 여행 ID가 없습니다.');
        }
        tripId.value = created.tripId;
      }
      applyRecommendation(await generateTripBudget(tripId.value));
      return true;
    } catch (error) {
      errorMessage.value = apiErrorMessage(
        error,
        '여행 예산을 추천하지 못했습니다.',
      );
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function completeGoal() {
    if (!canCompleteGoal.value || !tripId.value) return false;
    loading.value = true;
    clearError();
    try {
      completion.value = await confirmTripBudget(tripId.value, {
        countries: selectedPlans.value.map((plan) => ({
          tripCountryId: plan.tripCountryId,
          ...Object.fromEntries(
            budgetFields.map((field) => [
              field,
              Number(plan.budget[field] || 0),
            ]),
          ),
        })),
      });
      applyRecommendation({ ...completion.value, tripId: tripId.value });
      hasTravelGoal.value = true;
      activeTrip.value = {
        tripId: tripId.value,
        tripName: tripName.value,
        totalTargetAmount: completion.value.localTravelTargetTotal,
      };
      return true;
    } catch (error) {
      errorMessage.value = apiErrorMessage(
        error,
        '여행 목표 예산을 확정하지 못했습니다.',
      );
      return false;
    } finally {
      loading.value = false;
    }
  }

  function hydrateActiveTrip(result) {
    activeTrip.value = result;
    tripId.value = result.tripId;
    tripName.value = result.tripName || '';
    completion.value = null;
    hasTravelGoal.value = Number(result.totalTargetAmount || 0) > 0;
    clearPlans();
    for (const item of result.countries || []) {
      const country =
        countries.value.find((entry) => entry.countryId === item.countryId) ||
        decorateCountry({
          countryId: item.countryId,
          countryName: item.countryName,
          currencyCode: item.currencyCode,
          currencyName: item.currencyCode,
          flagUrl: null,
        });
      const key = String(item.countryId);
      selectedCountryCodes.value.push(key);
      plans[key] = createPlan(country, item);
    }
  }

  async function loadActiveGoal({ force = false } = {}) {
    if (initialized.value && !force) return activeTrip.value;
    loading.value = true;
    try {
      if (!countries.value.length) await loadCountries();
      const result = await fetchActiveTripGoal();
      hydrateActiveTrip(result);
      try {
        const savedRecommendation = await fetchTripBudget(result.tripId);
        if (
          savedRecommendation?.countries?.some(
            (country) => country.recommendedAirfareAmount != null,
          )
        ) {
          applyRecommendation(savedRecommendation);
        }
      } catch {
        recommendation.value = null;
      }
      return result;
    } catch (error) {
      if (!isNotFound(error, 'TRIP_NOT_FOUND')) {
        errorMessage.value = apiErrorMessage(
          error,
          '진행 중인 여행 목표를 불러오지 못했습니다.',
        );
      }
      hasTravelGoal.value = false;
      tripId.value = null;
      activeTrip.value = null;
      recommendation.value = null;
      completion.value = null;
      tripName.value = '';
      clearPlans();
      return null;
    } finally {
      initialized.value = true;
      loading.value = false;
    }
  }

  async function loadLifecycle() {
    lifecycleLoading.value = true;
    try {
      lifecycle.value = await fetchCurrentTripLifecycle();
      return lifecycle.value;
    } catch {
      lifecycle.value = { lifecycle: 'NONE' };
      return lifecycle.value;
    } finally {
      lifecycleLoading.value = false;
    }
  }

  async function archiveCurrentTrip() {
    if (!lifecycle.value?.tripId) return false;
    await archiveTripApi(lifecycle.value.tripId);
    initialized.value = false;
    activeTrip.value = null;
    tripId.value = null;
    homeDashboard.value = null;
    lifecycle.value = { lifecycle: 'ARCHIVED' };
    return true;
  }

  async function acknowledgeStartReport() {
    if (!lifecycle.value?.tripId || lifecycle.value.startReportAcknowledged) return;
    await acknowledgeTripStartReport(lifecycle.value.tripId);
    lifecycle.value = { ...lifecycle.value, startReportAcknowledged: true };
  }

  async function loadHomeDashboard({ force = false } = {}) {
    if (homeDashboard.value && !force) return homeDashboard.value;
    homeLoading.value = true;
    homeError.value = '';
    try {
      const result = await fetchActiveTripHome();
      homeDashboard.value = result;
      activeTrip.value = result;
      tripId.value = result.tripId;
      tripName.value = result.tripName || '';
      hasTravelGoal.value = Boolean(result.tripId);
      return result;
    } catch (error) {
      if (isNotFound(error, 'TRIP_NOT_FOUND')) {
        homeDashboard.value = null;
        hasTravelGoal.value = false;
        tripId.value = null;
        activeTrip.value = null;
        tripName.value = '';
        return null;
      }
      homeError.value = apiErrorMessage(
        error,
        '여행 저축 홈을 불러오지 못했습니다.',
      );
      return null;
    } finally {
      initialized.value = true;
      homeLoading.value = false;
    }
  }

  async function loadTripStatus(tripId, countryId = null) {
    statusLoading.value = true;
    try {
      tripStatus.value = await fetchTripStatus(tripId, countryId);
      return tripStatus.value;
    } catch (error) {
      errorMessage.value = apiErrorMessage(
        error,
        '여행 상태를 불러오지 못했습니다.',
      );
      return null;
    } finally {
      statusLoading.value = false;
    }
  }

  async function loadBudgetCheck(tripId) {
    budgetLoading.value = true;
    try {
      budgetCheckData.value = await fetchBudgetCheck(tripId);
      return budgetCheckData.value;
    } catch (error) {
      errorMessage.value = apiErrorMessage(
        error,
        '여행 자금 데이터를 불러오지 못했습니다.',
      );
      return null;
    } finally {
      budgetLoading.value = false;
    }
  }

  function resetGoal() {
    hasTravelGoal.value = false;
    initialized.value = false;
    tripId.value = null;
    activeTrip.value = null;
    homeDashboard.value = null;
    homeSelectedCountryId.value = null;
    homeError.value = '';
    recommendation.value = null;
    completion.value = null;
    tripName.value = '';
    clearPlans();
    Object.keys(allocations).forEach((key) => delete allocations[key]);
    clearError();
  }

  function enterTravelMode(country, budget) {
    isTravelMode.value = true;
    selectedCountry.value = country;
    travelBudget.value = budget;
  }

  function exitTravelMode() {
    isTravelMode.value = false;
    selectedCountry.value = null;
    travelBudget.value = null;
  }

  function setHomeSelectedCountry(countryId) {
    homeSelectedCountryId.value = countryId == null ? null : Number(countryId);
  }

  return {
    isTravelMode,
    selectedCountry,
    travelBudget,
    hasTravelGoal,
    initialized,
    tripId,
    activeTrip,
    homeDashboard,
    homeSelectedCountryId,
    homeLoading,
    homeError,
    recommendation,
    completion,
    tripName,
    countries,
    accounts,
    selectedCountryCodes,
    selectedPlans,
    allocations,
    loading,
    countryLoading,
    errorMessage,
    tripStatus,
    statusLoading,
    lifecycle,
    lifecycleLoading,
    totalTargetAmount,
    prepaidExpenseTotal,
    monthlySavingTarget,
    currentWalletBalance,
    remainingMonths,
    totalAllocatedAmount,
    selectedAccountCount,
    hasDateCollision,
    canReviewPlan,
    canCompleteGoal,
    budgetCheckData,
    aggregatedBudget,
    countryFlagMap,
    loadCountries,
    loadActiveGoal,
    loadLifecycle,
    archiveCurrentTrip,
    acknowledgeStartReport,
    loadHomeDashboard,
    loadTripStatus,
    loadBudgetCheck,
    setHomeSelectedCountry,
    toggleCountry,
    reorderCountries,
    updatePlan,
    updateBudget,
    resetBudgetToRecommendation,
    setAllocation,
    planError,
    savePlanAndRecommend,
    completeGoal,
    resetGoal,
    clearError,
    enterTravelMode,
    exitTravelMode,
  };
});
