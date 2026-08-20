<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useTravelModeStore } from '@/stores/travelMode';
import {
  useTravelStore,
  countryPresentation as globalCountryPresentation,
  getTravelCountryColors,
} from '@/stores/travel';
import { useExchangeStore } from '@/stores/exchange';
import NotificationBell from '@/components/common/NotificationBell.vue';
import foodIcon from '@/assets/icons/food.svg';
import foodIconRaw from '@/assets/icons/food.svg?raw';
import cafeIcon from '@/assets/icons/cafe.svg';
import cafeIconRaw from '@/assets/icons/cafe.svg?raw';
import shoppingIcon from '@/assets/icons/shopping-cart.svg';
import shoppingIconRaw from '@/assets/icons/shopping-cart.svg?raw';
import taxiIcon from '@/assets/icons/taxi.svg';
import taxiIconRaw from '@/assets/icons/taxi.svg?raw';

const props = defineProps({
  userName: { type: String, default: '권유현' },
  onSwitchMode: { type: Function, default: null },
});

const router = useRouter();
const travelMode = useTravelModeStore();
const travelStore = useTravelStore();
const exchangeStore = useExchangeStore();

// 앱 전체를 감싸는 프레임(App.vue)에 overflow:hidden이 걸려 있어
// position:sticky가 동작하지 않는다. 대신 position:fixed로 고정하고,
// 실제 렌더 높이를 측정해 뒤에 그만큼의 여백을 확보한다. (저축모드 홈과 동일한 방식)
const travelHeaderEl = ref(null);
const travelHeaderHeight = ref(0);
let travelHeaderResizeObserver = null;

function syncTravelHeaderHeight() {
  if (travelHeaderEl.value) {
    travelHeaderHeight.value = travelHeaderEl.value.offsetHeight;
  }
}

watch(travelHeaderEl, (el) => {
  travelHeaderResizeObserver?.disconnect();
  travelHeaderResizeObserver = null;
  if (!el) return;

  syncTravelHeaderHeight();
  if (window.ResizeObserver) {
    travelHeaderResizeObserver = new ResizeObserver(syncTravelHeaderHeight);
    travelHeaderResizeObserver.observe(el);
  }
});

// 데이터 바인딩을 위한 계산 속성 추가
const tripId = computed(() => travelStore.tripId);
const tripStatus = computed(() => travelStore.tripStatus);
const tripInfo = computed(() => tripStatus.value?.tripInfo);
const countries = computed(() => tripStatus.value?.countries || []);

// 국가 목록 캐싱 (필터링되지 않은 전체 목록)
const persistentCountries = ref([]);
// 외화 계산기에서 선택된 국가 코드
const calculatorCountryCode = ref(null);
// 최초 진입 시 여행 정보를 불러오는 동안 "등록된 여행이 없어요" 빈 화면이
// 잠깐 깜빡이며 보이지 않도록 로딩이 끝날 때까지는 아무 것도 그리지 않는다.
const isInitialLoading = ref(true);

// ISO 2자리 국가코드를 유니코드 국기 이모지로 변환 (텍스트로 그대로 표시 가능)
function flagEmoji(iso2) {
  return iso2
    .toUpperCase()
    .replace(/./g, (ch) => String.fromCodePoint(0x1f1e6 + ch.charCodeAt(0) - 65));
}

// 국가 선택 목록 (API 연동) — 캐러셀 슬라이드마다 독립적으로 렌더링할 수 있도록
// 국가별 목표/지출 금액도 함께 들고 있는다.
// 두 'YYYY-MM-DD' 문자열 사이의 일수 차이
function daysBetween(startStr, endStr) {
  if (!startStr || !endStr) return 0;
  const [sy, sm, sd] = startStr.split('-').map(Number);
  const [ey, em, ed] = endStr.split('-').map(Number);
  const start = new Date(sy, sm - 1, sd);
  const end = new Date(ey, em - 1, ed);
  return Math.round((end - start) / (1000 * 60 * 60 * 24));
}

const destinations = computed(() => {
  const totalTargetBudget = persistentCountries.value.reduce((sum, c) => sum + c.targetBudget, 0);
  const totalSpentAmount = persistentCountries.value.reduce((sum, c) => sum + c.spentAmount, 0);

  // 여행 전체 시작일(가장 이른 입국일)을 기준으로 국가별 일차 구간(예: 스위스 7~10일차)을 구한다.
  const datedCountries = persistentCountries.value.filter((c) => c.arrivalDate && c.departureDate);
  const overallStart = datedCountries.reduce(
    (min, c) => (min === null || c.arrivalDate < min ? c.arrivalDate : min),
    null,
  );
  const overallEnd = datedCountries.reduce(
    (max, c) => (max === null || c.departureDate > max ? c.departureDate : max),
    null,
  );
  const overallTotalDays = overallStart && overallEnd ? daysBetween(overallStart, overallEnd) + 1 : 1;

  const all = {
    code: 'all',
    name: '전체',
    flag: '🌍',
    theme: '#17485b',
    progressBg: getTravelCountryColors().progressBg,
    barColor: getTravelCountryColors().barColor,
    targetBudget: totalTargetBudget,
    spentAmount: totalSpentAmount,
    arrivalDate: overallStart,
    departureDate: overallEnd,
    dayRangeStart: 1,
    dayRangeEnd: overallTotalDays,
  };
  const apiCountries = persistentCountries.value.map((c) => {
    const presentation = getCountryPresentation(c.countryName);
    // 외화 계산기용 환율 — 같은 국가명을 쓰는 환율 스토어 항목에서 그대로 가져온다.
    const currencyInfo = exchangeStore.currencies.find(
      (item) => item.countryName === c.countryName,
    );
    return {
      code: c.tripCountryId.toString(),
      name: c.countryName,
      flag: countryFlagMap[c.countryName]
        ? flagEmoji(countryFlagMap[c.countryName])
        : '🌍',
      theme: presentation.headerBg,
      progressBg: presentation.progressBg,
      barColor: presentation.barColor,
      image: overallAssets.find((a) => a.country === c.countryName)?.image
        || globalCountryPresentation[c.countryName]?.image
        || '',
      targetBudget: c.targetBudget,
      spentAmount: c.spentAmount,
      arrivalDate: c.arrivalDate,
      departureDate: c.departureDate,
      dayRangeStart: overallStart ? daysBetween(overallStart, c.arrivalDate) + 1 : 1,
      dayRangeEnd: overallStart ? daysBetween(overallStart, c.departureDate) + 1 : 1,
      currency: currencyInfo?.code || '',
      rate: currencyInfo?.rate || 0,
    };
  });
  // 캐러셀 순서: 국가별 카드 먼저, "전체" 보딩패스는 맨 뒤로
  return [...apiCountries, all];
});

// 날짜 계산
const today = computed(() => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
});

const startDate = computed(() => {
  if (!tripInfo.value?.startDate) return null;
  const [year, month, day] = tripInfo.value.startDate.split('-').map(Number);
  return new Date(year, month - 1, day);
});

const endDate = computed(() => {
  if (!tripInfo.value?.endDate) return null;
  const [year, month, day] = tripInfo.value.endDate.split('-').map(Number);
  return new Date(year, month - 1, day);
});

const currentDay = computed(() => {
  if (!startDate.value) return 0;

  // 오늘 날짜가 시작일보다 이전이면 0일차
  if (today.value < startDate.value) return 0;

  const diff = today.value - startDate.value;
  return Math.floor(diff / (1000 * 60 * 60 * 24)) + 1;
});

const totalTripDays = computed(() => {
  if (!startDate.value || !endDate.value) return 1;
  const diff = endDate.value - startDate.value;
  return Math.floor(diff / (1000 * 60 * 60 * 24)) + 1;
});

// 여행 기간이 이미 끝난 국가는 currentDay가 totalTripDays를 넘어서면서
// 진행률 바/핀이 100%를 넘어 튀어나가지 않도록 0~100 사이로 고정한다.
const tripProgressPercent = computed(() =>
  Math.min(100, Math.max(0, (currentDay.value / totalTripDays.value) * 100)),
);

// 점이 노선 양 끝(국가명/D+N 텍스트)에 바짝 붙지 않도록 실제 표시 위치는
// 4~90% 범위 안쪽으로만 이동시킨다 (선 자체의 길이/위치는 그대로 둔다).
const routePinPosition = computed(() => 4 + (tripProgressPercent.value / 100) * 86);

// 카테고리 아이콘 매핑
const categoryIcons = {
  식비: '🍴',
  교통: '🚆',
  숙박: '🏨',
  쇼핑: '🛍️',
  관광: '🎨',
  기타: '💬',
  카페: '☕',
  생활비: '📦',
};
const categoryIconImages = {
  식비: foodIcon,
  카페: cafeIcon,
  쇼핑: shoppingIcon,
  교통: taxiIcon,
};
const categoryIconRawImages = {
  식비: foodIconRaw,
  카페: cafeIconRaw,
  쇼핑: shoppingIconRaw,
  교통: taxiIconRaw,
};
const categoryPresentation = {
  식비: { color: '#e0613d', soft: '#fff0ec' },
  교통: { color: '#3478e5', soft: '#edf4ff' },
  쇼핑: { color: '#7449ad', soft: '#f3effd' },
  카페: { color: '#a66c12', soft: '#fff5e8' },
  관광: { color: '#e25283', soft: '#ffedf3' },
  숙박: { color: '#19a88b', soft: '#e7f6f5' },
  생활비: { color: '#19a88b', soft: '#e7f6f5' },
  기타: { color: '#718096', soft: '#f0f3f8' },
};

function getCategoryIcon(name) {
  const presentation = categoryPresentation[name] || categoryPresentation.기타;
  return {
    iconSrc: categoryIconImages[name] || null,
    iconRaw: categoryIconRawImages[name] || null,
    icon: categoryIcons[name] || '📁',
    ...presentation,
  };
}

// 국가별 색상 — stores/travel.js의 getTravelCountryColors()가 유일한 소스다.
// (여행모드의 다른 화면들과 색이 어긋나지 않도록 이 화면만의 사본을 두지 않는다)
const getCountryPresentation = getTravelCountryColors;

function coloredCategoryIcon(icon) {
  return icon.iconRaw?.replaceAll('black', icon.color) || '';
}

// 국가별 색상 매핑 헬퍼 (저축모드와 동일한 headerBg 사용)
function getCountryColor(countryName) {
  return getCountryPresentation(countryName).headerBg;
}

// 여행자금 체크를 위한 데이터 가공
const categorySummary = computed(() => tripStatus.value?.categorySummary || []);

// 거래가 없는 국가/기간이라도 카테고리 표 자체는 항상 노출한다 — 응답에 없는
// 카테고리는 0원으로 채운다.
const CATEGORY_ORDER = ['식비', '교통', '쇼핑', '카페', '관광', '숙박', '기타'];

const totalCategorySpending = computed(() =>
  categorySummary.value.reduce(
    (sum, category) => sum + Number(category.totalAmount || 0),
    0,
  ),
);

const categoryList = computed(() => {
  const byName = new Map(categorySummary.value.map((cat) => [cat.categoryName, cat]));

  return CATEGORY_ORDER.map((name) => {
    const cat = byName.get(name);
    const total = Number(cat?.totalAmount || 0);
    const ratio = totalCategorySpending.value > 0
      ? (total / totalCategorySpending.value) * 100
      : 0;

    const details = (cat?.countryDetails || []).map((d) => ({
      ...d,
      // 세그먼트 폭은 해당 카테고리 전체 폭 내에서의 비율
      percent: total > 0 ? (d.amount / total) * 100 : 0,
    }));

    return {
      name,
      total,
      barWidth: ratio,
      ratio: Math.round(ratio),
      details,
      icon: getCategoryIcon(name),
    };
  });
});

const isReturnPeriod = computed(() => {
  if (!travelStore.homeDashboard?.endDate) return false;

  const today = new Date();
  today.setHours(0, 0, 0, 0); // 시간을 00:00:00으로 초기화

  // 실제 API에서 가져온 endDate 사용 (배열 또는 문자열 형태 처리)
  const endDateArray = travelStore.homeDashboard.endDate;
  const endDate = Array.isArray(endDateArray)
    ? new Date(endDateArray[0], endDateArray[1] - 1, endDateArray[2])
    : new Date(endDateArray);

  const startCheckDate = new Date(endDate);
  startCheckDate.setDate(startCheckDate.getDate() - 1);
  startCheckDate.setHours(0, 0, 0, 0);

  const endCheckDate = new Date(endDate);
  endCheckDate.setHours(23, 59, 59, 999);

  return today >= startCheckDate && today <= endCheckDate;
});

// 오늘 날짜(YYYY-MM-DD)가 arrivalDate~departureDate 범위에 포함되는 국가를 찾는다.
// 해당하는 국가가 없으면(모두 지났거나 아직 시작 전) 첫 번째 국가로 대체한다.
function todayDateString() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function tripDayDisplay(item) {
  const arrivalDate = item.arrivalDate;
  const departureDate = item.departureDate;

  if (!arrivalDate || !departureDate) {
    return {
      label: 'TRIP DAY',
      text: `D+${Math.max(1, currentDay.value)}`,
      state: 'ongoing',
    };
  }

  const todayStr = todayDateString();

  if (todayStr > departureDate) {
    return { label: 'TRIP STATUS', text: '여행 종료', state: 'ended' };
  }

  if (todayStr < arrivalDate) {
    return {
      label: 'START IN',
      text: `D-${Math.max(1, daysBetween(todayStr, arrivalDate))}`,
      state: 'upcoming',
    };
  }

  return {
    label: 'TRIP DAY',
    text: `D+${daysBetween(arrivalDate, todayStr) + 1}`,
    state: 'ongoing',
  };
}

function findTodayCountryCode(countries) {
  if (!countries.length) return 'all';
  const todayStr = todayDateString();
  const match = countries.find(
    (c) => c.arrivalDate && c.departureDate && c.arrivalDate <= todayStr && todayStr <= c.departureDate,
  );
  return (match || countries[0]).tripCountryId.toString();
}

onMounted(async () => {
  try {
    await nextTick();
    restoreCountryPosition();
    if (!exchangeStore.currencies.length) {
      exchangeStore.updateExchangeRates().catch(() => {});
    }
    await travelStore.loadActiveGoal();
    if (tripId.value) {
      // 초기 로딩 시 필터링 없이 전체 데이터를 가져와 캐싱
      const status = await travelStore.loadTripStatus(tripId.value, null);
      persistentCountries.value = status?.countries || [];
      selectedCountryId.value = findTodayCountryCode(persistentCountries.value);

      if (selectedCountryId.value !== 'all') {
        await loadData();
      }
    }
  } finally {
    isInitialLoading.value = false;
    await nextTick();
    restoreCountryPosition();
  }
});

onBeforeUnmount(() => {
  travelHeaderResizeObserver?.disconnect();
});

const selectedCountryId = computed({
  get: () => travelMode.selectedDestination,
  set: (val) => travelMode.selectDestination(val),
});
const assetsCarousel = ref(null);
const countryCarousel = ref(null);
// 스와이프로 국가가 바뀔 때마다 값을 올려서 활성 티켓의 :key를 바꾼다 — 저축모드 홈과
// 동일하게, DOM을 다시 그리게 만들어 진행률 박스의 진입 애니메이션을 매번 재생시킨다.
const countryAnimationKey = ref(0);

// restoreCountryPosition()이 코드로 scrollLeft를 바꾸는 동안에는 그 이동을
// "사용자가 스와이프했다"고 오인해 selectedCountryId를 되돌리지 않도록 막는다.
// (새로고침 직후 국가가 아니라 "전체" 카드가 보이던 문제의 원인)
let suppressScrollHandling = false;

// 좌우 스와이프로 국가 전환 (저축모드 홈과 동일한 방식)
function handleCountryScroll(event) {
  if (suppressScrollHandling) return;
  const carousel = event.currentTarget;
  if (!carousel?.clientWidth) return;

  const idx = Math.max(
    0,
    Math.min(
      destinations.value.length - 1,
      Math.round(carousel.scrollLeft / carousel.clientWidth),
    ),
  );
  const nextCode = destinations.value[idx]?.code;
  if (!nextCode || nextCode === selectedCountryId.value) return;
  selectedCountryId.value = nextCode;
  countryAnimationKey.value += 1;
  loadData();
}

function restoreCountryPosition() {
  const carousel = countryCarousel.value;
  if (!carousel?.clientWidth || !destinations.value.length) return;

  const savedIndex = destinations.value.findIndex((item) => item.code === selectedCountryId.value);
  const idx = savedIndex >= 0 ? savedIndex : 0;
  suppressScrollHandling = true;
  carousel.scrollLeft = idx * carousel.clientWidth;
  // 스크롤 스냅이 완전히 안정된 뒤에(2프레임 정도) 핸들러를 다시 켠다.
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      suppressScrollHandling = false;
    });
  });
}

// 툴팁 상태 관리
const tooltip = ref({
  show: false,
  text: '',
  x: 0,
  y: 0,
});

function showTooltip(e, text) {
  tooltip.value = {
    show: true,
    text,
    x: e.clientX,
    y: e.clientY,
  };
}

const overallAssets = [
  {
    code: 'FR',
    flag: '🇫🇷',
    country: '프랑스',
    amount: 590000,
    local: '약 €128.10',
    image: '/images/france.png',
    theme: '#124c9f',
  },
  {
    code: 'CH',
    flag: '🇨🇭',
    country: '스위스',
    amount: 1000000,
    local: '약 CHF 586.65',
    image: '/images/switzerland.webp',
    theme: '#a81436',
  },
  {
    code: 'DE',
    flag: '🇩🇪',
    country: '독일',
    amount: 720000,
    local: '약 EUR 484.46',
    image: '/images/germany.png',
    theme: '#202020',
  },
  {
    code: 'JP',
    flag: '🇯🇵',
    country: '일본',
    amount: 650000,
    local: '약 JPY 70,422',
    image: '/images/japan.webp',
    theme: '#c82770',
  },
  {
    code: 'HK',
    flag: '🇭🇰',
    country: '홍콩',
    amount: 480000,
    local: '약 HKD 2,606.95',
    image: '/images/Hong%20Kong.png',
    theme: '#b8202e',
  },
];

const selected = computed(
  () =>
    destinations.value.find((item) => item.code === selectedCountryId.value) ??
    destinations.value[0],
);
// 국가별 flag-icons 클래스 매핑
const countryFlagMap = {
  프랑스: 'fr',
  스위스: 'ch',
  독일: 'de',
  일본: 'jp',
  홍콩: 'hk',
  이탈리아: 'it',
  스페인: 'es',
  네덜란드: 'nl',
  벨기에: 'be',
  오스트리아: 'at',
  포르투갈: 'pt',
  그리스: 'gr',
  아일랜드: 'ie',
  핀란드: 'fi',
  중국: 'cn',
  괌: 'gu',
};

const selectedSchedules = computed(() => {
  const apiSchedules = tripStatus.value?.upcomingSchedules || [];
  return apiSchedules.map((s) => {
    const d = new Date(s.dateTime);
    // location 또는 title에서 국가명 추출 (예시)
    const countryName = Object.keys(countryFlagMap).find(
      (name) => s.location.includes(name) || s.title.includes(name),
    );

    return {
      title: s.title,
      date: `${d.getMonth() + 1}.${d.getDate()} (${['일', '월', '화', '수', '목', '금', '토'][d.getDay()]})`,
      time: `${d.getHours()}:${d.getMinutes().toString().padStart(2, '0')}`,
      flagClass: countryFlagMap[countryName]
        ? `fi fi-${countryFlagMap[countryName]}`
        : 'fi fi-xx',
      status: s.location,
      warning: false,
    };
  });
});

// 최근 지출 내역 (API 연동)
const recentTransactions = computed(
  () => tripStatus.value?.recentTransactionsByCountry || {},
);

const selectedRecent = computed(() => {
  let transactions = [];

  if (selected.value.code === 'all') {
    // 모든 나라의 거래 내역을 합침
    transactions = Object.values(recentTransactions.value).flat();
  } else {
    // 선택된 나라의 거래 내역만 가져옴
    transactions = recentTransactions.value[selected.value.name] || [];
  }

  // 날짜 기준 내림차순 정렬 및 상위 5개 추출
  return transactions
    .sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate))
    .slice(0, 5)
    .map((t) => {
      return {
        icon: getCategoryIcon(t.category), // 카테고리별 아이콘 재활용
        place: `${t.description}`, // 예: "일본 택시"
        meta: `${t.category} · ${new Date(t.transactionDate).toLocaleDateString()}`,
        amount: `-${formatWon(t.amount)}(${t.originalAmount.toLocaleString()}${t.currency})`,
      };
    });
});
const calculatorDestination = computed(() =>
  selected.value.code === 'all'
    ? (destinations.value.find((item) => item.code === calculatorCountryCode.value) ??
      destinations.value[0])
    : selected.value,
);
const convertedAmount = computed(() =>
  Math.round(
    (Number(travelMode.calculatorAmount) || 0) *
      calculatorDestination.value.rate,
  ),
);

function formatWon(value) {
  return `${Number(value || 0).toLocaleString('ko-KR')}원`;
}

function fundPercent(item) {
  return item.targetBudget > 0
    ? Math.round((item.spentAmount / item.targetBudget) * 100)
    : 0;
}

// 데이터 로드
const loadData = async () => {
  console.log('선택된 국가 ID:', selectedCountryId.value);
  if (travelStore.tripId) {
    // API 호출하여 데이터만 갱신 (persistentCountries는 건드리지 않음)
    await travelStore.loadTripStatus(
      travelStore.tripId,
      selectedCountryId.value === 'all' ? null : selectedCountryId.value,
    );
  }
  console.log('필터링된 데이터 로드 완료:', tripStatus.value);
};

function openCalculator() {
  if (selected.value.code !== 'all')
    calculatorCountryCode.value = selected.value.code;
  travelMode.openCalculator(
    selected.value.code === 'all'
      ? travelMode.calculatorCurrency
      : selected.value.currency,
  );
}

function selectCalculatorDestination(item) {
  calculatorCountryCode.value = item.code;
  travelMode.setCalculatorCurrency(item.currency);
}
async function switchMode(mode) {
  const isTravelMode = mode === 'travel' ? true : false;
  const success = travelMode.toggleTravelMode(isTravelMode);

  if (success) {
    if (props.onSwitchMode) props.onSwitchMode(mode);
  } else {
    console.error('모드 전환 실패');
  }
}
</script>

<template>
  <section class="travel-home">
    <div ref="travelHeaderEl" class="savings-home-header">
      <div class="savings-header-row">
        <div class="mode-switch-control">
          <span class="mode-switch-thumb" />
          <button type="button" class="selected">여행</button>
          <button type="button" @click="switchMode('savings')">저축</button>
        </div>
        <NotificationBell />
      </div>
      <h1 class="home-header-title">
        <img src="@/assets/brand/tripass-text.png" class="home-wordmark" alt="TRIPASS" />
      </h1>
    </div>
    <div :style="{ height: travelHeaderHeight + 'px' }" aria-hidden="true" />

    <template v-if="isInitialLoading">
      <div class="ticket-skeleton" aria-hidden="true" />
    </template>
    <template v-else-if="!tripId">
      <article class="empty-trip-ticket">
        <span class="empty-trip-orbit" aria-hidden="true"></span>
        <div class="empty-trip-band">
          <span>TRIPASS · START JOURNEY</span>
        </div>
        <div class="empty-trip-body">
          <span class="empty-trip-badge empty-map-badge" aria-hidden="true">
            <svg class="empty-map-pin" viewBox="0 0 32 32" fill="none">
              <path d="M16 28s8-7.4 8-15a8 8 0 1 0-16 0c0 7.6 8 15 8 15Z" fill="#FFD466" />
              <circle cx="16" cy="13" r="3.25" fill="#123C94" />
            </svg>
          </span>
          <div>
            <strong>아직 등록된 여행이 없어요</strong>
            <p>여행명·국가·일정을 등록하면<br>AI가 목표 예산과 월 저축액을 제안해요</p>
          </div>
          <button type="button" class="empty-trip-cta" @click="router.push({ name: 'TravelRegister' })">여행 계획 등록하기</button>
        </div>
      </article>

      <div class="empty-trip-guide">
        <span class="guide-label">TRIPASS GUIDE</span>
        <strong>목표 설정부터 월렛 저축까지</strong>
        <p>여행 예산은 AI가 제안하고, 실제 저축은 TRIP 월렛에서 관리해요</p>
      </div>
    </template>
    <template v-else>
    <!-- BOARDING PASS 카드: 좌우 스와이프로 국가 전환 (저축모드 홈과 동일한 방식) -->
    <div
      ref="countryCarousel"
      class="country-carousel"
      @scroll.passive="handleCountryScroll"
    >
      <article
        v-for="item in destinations"
        :key="item.code"
        class="country-slide"
        :class="{ active: selected.code === item.code }"
      >
        <div
          :key="`${item.code}-${item.code === selected.code ? countryAnimationKey : 0}`"
          class="ticket"
          :class="[
            { combined: item.code === 'all' },
            `country-${item.code}`,
          ]"
          :style="{
            '--theme': item.theme,
            '--progress-bg': item.progressBg,
            '--bar': item.barColor,
            '--photo': `url(${item.image})`,
          }"
        >
          <div class="ticket-top">
            <span>BOARDING PASS</span><span>TRIPASS AIR</span
            ><span
              >NO. {{ item.code === 'all' ? 'EUR' : item.code }}-230</span
            >
          </div>
          <div class="perforation"><i /><span /><i /></div>
          <div class="ticket-main">
            <div class="trip-line">
              <div class="trip-destination">
                <p class="trip-label">DESTINATION</p>
                <p class="trip-country-name">
                  <span v-if="item.code !== 'all'" class="trip-country-flag">{{ item.flag }}</span
                  ><span>{{ item.code === 'all' ? (tripInfo?.tripName || '여행') : item.name }}</span>
                </p>
              </div>
              <div class="destination-route" aria-hidden="true">
                <i class="route-line"></i>
                <span
                  class="route-pin"
                  :style="{ left: `${routePinPosition}%` }"
                >
                  <i class="route-pin-pulse"></i>
                  <i class="route-pin-dot"></i>
                </span>
              </div>
              <div class="trip-departure">
                <p class="trip-label">{{ tripDayDisplay(item).label }}</p>
                <p
                  class="trip-day-count"
                  :class="`is-${tripDayDisplay(item).state}`"
                >
                  {{ tripDayDisplay(item).text }}
                </p>
              </div>
            </div>
            <p class="trip-description">여행 남은 자산을 한눈에 확인해요 ✨</p>
            <p class="trip-day-range">{{ item.dayRangeStart }}일차 ~ {{ item.dayRangeEnd }}일차</p>
            <div class="ticket-photo-space" />

            <div class="travel-summary-content">
              <div class="summary-title-wrapper">
                <div class="summary-title">
                  <span
                    >{{
                      item.code === 'all'
                        ? '전체 남은 여행 자산'
                        : `${item.name}에서 남은 여행 자산`
                    }}
                    (합산)</span
                  ><strong>{{ formatWon(item.targetBudget - item.spentAmount) }}</strong>
                </div>
                <button
                  v-if="isReturnPeriod"
                  class="return-checklist-button"
                  @click="
                    () => {
                      const id =
                        travelStore.tripId || travelStore.homeDashboard?.tripId;
                      if (id) {
                        router.push(`/mypage/checklists/return?tripId=${id}`);
                      } else {
                        console.error('tripId를 찾을 수 없습니다.');
                      }
                    }
                  "
                >
                  귀국 체크리스트 확인하기 ›
                </button>
              </div>
              <div
                v-if="item.code === 'all'"
                class="country-assets"
                style="grid-template-columns: 1fr 1fr"
                aria-label="국가별 남은 여행 자산"
              >
                <div
                  v-for="asset in destinations.slice(0, -1)"
                  :key="asset.code"
                  class="country-asset-card"
                  :style="{
                    '--asset-image': `url(${asset.image})`,
                    '--asset-theme': asset.theme,
                  }"
                >
                  <span>{{ asset.flag }} {{ asset.name }} 남은 여행 자산</span
                  ><b>{{ formatWon(asset.targetBudget - asset.spentAmount) }}</b>
                </div>
              </div>
              <template v-if="item.code === 'all'">
                <div class="fund-label">
                  <span>예산 사용률</span><b>{{ fundPercent(item) }}%</b>
                </div>
                <div class="fund-track">
                  <i :style="{ width: `${fundPercent(item)}%` }" />
                </div>
                <div class="fund-meta">
                  <span>BUDGET {{ formatWon(item.targetBudget) }}</span
                  ><span>SPENT {{ formatWon(item.spentAmount) }}</span>
                </div>
              </template>
              <div v-else class="fund-progress-box">
                <div class="fund-progress-head">
                  <span>예산 사용률</span><strong>{{ fundPercent(item) }}%</strong>
                </div>
                <div class="fund-progress-track">
                  <i :style="{ width: `${fundPercent(item)}%` }" />
                </div>
                <div class="fund-progress-meta">
                  <div>
                    <b>{{ formatWon(item.spentAmount) }}</b>
                    <small>SPENT</small>
                  </div>
                  <div class="align-right">
                    <b>{{ formatWon(item.targetBudget) }}</b>
                    <small>BUDGET</small>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="perforation lower"><i /><span /><i /></div>
          <button
            class="ticket-stub"
            type="button"
            @click="router.push('/travel/funds')"
          >
            <span>여행 목표 자금 관리</span>
          </button>
        </div>
      </article>
    </div>
    <div v-if="destinations.length > 1" class="country-carousel-meta">
      <span>옆으로 넘겨 방문 국가를 확인하세요</span>
      <div class="country-carousel-dots" aria-hidden="true">
        <i
          v-for="item in destinations"
          :key="item.code"
          :class="{ active: selected.code === item.code }"
        />
      </div>
    </div>

    <!-- 툴팁 컴포넌트 -->
    <div
      v-if="tooltip.show"
      class="custom-tooltip"
      :style="{ top: `${tooltip.y + 10}px`, left: `${tooltip.x + 10}px` }"
    >
      {{ tooltip.text }}
    </div>

    <article
      class="card budget-card"
      role="button"
      tabindex="0"
      aria-label="여행자금 체크 상세 보기"
      @click="router.push('/travel/funds')"
      @keydown.enter="router.push('/travel/funds')"
    >
      <div class="card-title budget-card-title">
        <h2>여행자금 체크</h2>
        <div class="legend" v-if="countries.length > 0">
          <span
            v-for="c in countries"
            :key="c.countryName"
            :style="{ color: getCountryColor(c.countryName) }"
          >● {{ c.countryName }}</span
          >
        </div>
      </div>
      <div class="budget-total-block">
        <span>총 지출</span>
        <strong>{{ formatWon(totalCategorySpending) }}</strong>
      </div>
      <div class="budget-divider"></div>
      <div class="budget-heading">
        <span>카테고리별 지출</span>
        <span>여행 기간</span>
      </div>
      <div v-for="cat in categoryList" :key="cat.name" class="budget-row">
        <span class="category">
          <i :style="{ background: cat.icon.soft }">
            <span
              v-if="cat.icon.iconRaw"
              class="budget-category-icon"
              v-html="coloredCategoryIcon(cat.icon)"
            ></span>
            <template v-else>{{ cat.icon.icon }}</template>
          </i>
          {{ cat.name }}
        </span>
        <div class="split-bar">
          <div class="budget-bar-fill" :style="{ width: `${cat.barWidth}%` }">
            <i
              v-for="d in cat.details"
              :key="d.countryName"
              @mouseover="
                showTooltip($event, `${d.countryName}: ${formatWon(d.amount)}`)
              "
              @mouseleave="hideTooltip"
              :style="{
                width: `${d.percent}%`,
                background: getCountryColor(d.countryName),
              }"
            />
            <i
              v-if="cat.total > 0 && cat.details.length === 0"
              :style="{ width: '100%', background: cat.icon.color }"
            />
          </div>
        </div>
        <b class="budget-amount">{{ formatWon(cat.total) }}</b>
        <b class="budget-ratio">{{ cat.ratio }}%</b>
      </div>
    </article>
    <article class="card">
      <div class="card-title">
        <h2>다가오는 여행 일정</h2>
        <button type="button" @click="router.push('/schedule')">
          전체 보기
        </button>
      </div>
      <div v-if="selectedSchedules.length === 0" class="empty-msg">
        다가오는 여행 일정이 없어요.
      </div>
      <button
        v-else
        v-for="item in selectedSchedules"
        :key="item.title"
        class="schedule-row"
        type="button"
        @click="router.push('/schedule')"
      >
        <span
          ><b v-if="item.date">{{ item.date }}</b
          ><strong
            ><i :class="item.flagClass" style="margin-right: 5px"></i
            >{{ item.title }}</strong
          ><small>{{ item.time }}</small></span
        >
        <em :class="{ warning: item.warning }">{{ item.status }}</em>
      </button>
    </article>

    <article class="card recent-card">
      <div class="card-title">
        <h2>최근 지출 내역</h2>
        <button type="button" @click="router.push('/asset/transactions')">
          전체 거래내역
        </button>
      </div>
      <div v-if="selectedRecent.length === 0" class="empty-msg">
        최근 지출 내역이 없어요.
      </div>
      <button
        v-else
        v-for="item in selectedRecent"
        :key="item.place"
        class="recent-row"
        type="button"
        @click="router.push('/asset/transactions')"
      >
        <i>
          <img v-if="item.icon.iconSrc" :src="item.icon.iconSrc" alt="" />
          <template v-else>{{ item.icon.icon }}</template>
        </i>
        <span
          ><b>{{ item.place }}</b
          ><small>{{ item.meta }}</small></span
        ><strong> {{ item.amount }}</strong>
      </button>
    </article>

    <section
      v-if="travelMode.calculatorOpen"
      class="quick-calculator"
      aria-label="외화 계산기"
    >
      <div class="calculator-head">
        <b>외화 계산기</b
        ><button
          type="button"
          aria-label="닫기"
          @click="travelMode.closeCalculator"
        >
          ×
        </button>
      </div>
      <div v-if="selected.code === 'all'" class="calculator-currencies">
        <button
          v-for="item in destinations.slice(0, -1)"
          :key="item.code"
          type="button"
          :class="{ active: calculatorDestination.code === item.code }"
          @click="selectCalculatorDestination(item)"
        >
          {{ item.flag }} {{ item.currency }}
        </button>
      </div>
      <div class="calculator-fields">
        <label
          ><input
            v-model.number="travelMode.calculatorAmount"
            type="number"
            min="0"
          /><span>{{ calculatorDestination.currency }}</span></label
        ><b>↔</b
        ><output
          >{{ convertedAmount.toLocaleString('ko-KR') }}
          <small>KRW</small></output
        >
      </div>
    </section>
    <button
      class="calculator-fab"
      type="button"
      aria-label="외화 계산기 열기"
      @click="openCalculator"
    >
      ▦
    </button>
    </template>
  </section>
</template>

<style scoped>
.travel-home {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: auto;
  padding-bottom: 94px;
  background: #f4f5f9;
  color: #10192d;
}
.ticket-skeleton {
  margin: 18px 16px 0;
  height: 420px;
  border-radius: 21px;
  background: linear-gradient(90deg, #e4e7ee 25%, #f2f4f8 50%, #e4e7ee 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.6s ease-in-out infinite;
}
@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
@media (prefers-reduced-motion: reduce) {
  .ticket-skeleton { animation: none; }
}
.empty-trip-ticket {
  position: relative;
  margin: 18px 16px 0;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(105, 151, 232, .22);
  color: #fff;
  background: linear-gradient(145deg, #0b2a6b 0%, #123c94 62%, #174da7 100%);
  box-shadow: 0 12px 26px rgba(11, 42, 107, .22);
}
.empty-trip-orbit {
  position: absolute;
  top: -50px;
  right: -40px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .07);
}
.empty-trip-band {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 17px 18px 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.14em;
  color: rgba(255, 255, 255, .6);
}
.empty-trip-body {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 22px 22px;
  text-align: center;
}
.empty-trip-badge {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .12);
  box-shadow: 0 0 0 0 rgba(255, 212, 102, 0.28);
  animation: empty-map-beacon 2.4s ease-out infinite;
}
.empty-map-pin {
  width: 29px;
  height: 29px;
  filter: drop-shadow(0 4px 5px rgba(0, 0, 0, 0.18));
  animation: empty-map-float 2.4s ease-in-out infinite;
}
.empty-trip-body strong {
  font-size: 16.5px;
  font-weight: 800;
}
.empty-trip-body p {
  margin-top: 6px;
  color: rgba(255, 255, 255, .68);
  font-size: 12px;
  line-height: 1.6;
}
.empty-trip-cta {
  width: 100%;
  margin-top: 2px;
  padding: 13px;
  border: 0;
  border-radius: 12px;
  color: #0b2a6b;
  background: #ffd466;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 7px 16px rgba(2, 18, 50, .2);
}
@keyframes empty-map-float {
  0%, 100% { transform: translateY(2px); }
  50% { transform: translateY(-3px); }
}
@keyframes empty-map-beacon {
  0% { box-shadow: 0 0 0 0 rgba(255, 212, 102, .3); }
  70%, 100% { box-shadow: 0 0 0 12px rgba(255, 212, 102, 0); }
}
/* ── 저축모드 홈과 동일한 진입 애니메이션 세트 ── */
@keyframes home-fade-up {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes home-card-reveal {
  from { opacity: 0; transform: translateY(22px) scale(0.985); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes ticket-card-enter {
  from { opacity: 0; transform: translateY(24px) scale(0.97); filter: blur(3px); }
  to { opacity: 1; transform: translateY(0) scale(1); filter: blur(0); }
}
@keyframes progress-shine {
  60%, 100% { transform: translateX(100%); }
}
@media (prefers-reduced-motion: reduce) {
  .empty-trip-badge,
  .empty-map-pin,
  .country-carousel,
  .country-carousel-meta,
  .country-slide.active .ticket,
  .fund-progress-box,
  .fund-progress-track i::after,
  .route-pin-pulse,
  .card { animation: none; }
  .country-slide { transition: none; }
}
.empty-trip-tear { position: relative; height: 18px; }
.empty-trip-notch {
  position: absolute;
  top: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f4f5f9;
}
.empty-trip-notch.left { left: -9px; }
.empty-trip-notch.right { right: -9px; }
.empty-trip-dash {
  position: absolute;
  left: 16px;
  right: 16px;
  top: 9px;
  height: 1px;
  background: repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.4) 0 5px, transparent 5px 10px);
}
.empty-trip-footer {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px 16px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.55);
}
.empty-trip-barcode {
  width: 90px;
  height: 16px;
  opacity: 0.55;
  background: repeating-linear-gradient(90deg, #fff 0 2px, transparent 2px 4px, #fff 4px 5px, transparent 5px 9px, #fff 9px 12px, transparent 12px 14px);
}
.empty-trip-guide {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 14px 16px 0;
  padding: 16px;
  border: 1px solid #d8e5fc;
  border-radius: 16px;
  background: #eaf1ff;
}
.empty-trip-guide .guide-label {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: #2f6fed;
}
.empty-trip-guide strong { font-size: 14.5px; font-weight: 800; color: #10192b; }
.empty-trip-guide p { font-size: 11.5px; color: #5a6478; line-height: 1.6; }
.summary-title-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.return-checklist-button {
  padding: 6px 10px;
  border-radius: 8px;
  background: #ffb800;
  color: #173f8d;
  font-size: 10px;
  font-weight: 900;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}
.return-checklist-button:active {
  transform: scale(0.96);
}
.travel-header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 46px 16px 14px;
  background: #fff;
}
.travel-header h1 {
  font-size: 16px;
  font-weight: 800;
  white-space: nowrap;
}
.mode-toggle {
  display: flex;
  align-items: center;
  padding: 2px;
  border: 2px solid #173f8d;
  border-radius: 999px;
  background: #fff;
}
.mode-toggle button {
  padding: 5px 7px;
  border-radius: 999px;
  color: #173f8d;
  font-size: 10px;
  font-weight: 900;
}
.mode-toggle button.active {
  background: #173f8d;
  color: #fff;
}
.country-select {
  position: relative;
  margin-left: auto;
}
.country-select > button {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 9px;
  border: 1px solid #d8e0eb;
  border-radius: 11px;
  background: #fff;
  font-size: 11px;
  font-weight: 800;
}
.country-select i {
  font-style: normal;
  color: #64748b;
}
.country-menu {
  position: absolute;
  right: 0;
  top: 40px;
  z-index: 80;
  width: 138px;
  padding: 5px;
  border: 1px solid #dce3ed;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 10px 25px #15254724;
}
.country-menu button {
  display: flex;
  width: 100%;
  gap: 7px;
  padding: 10px;
  border-radius: 8px;
  text-align: left;
  font-size: 11px;
}
.country-menu button.active {
  background: #eef4ff;
  color: #173f8d;
  font-weight: 900;
}
.ticket {
  position: relative;
  margin: 0;
  overflow: hidden;
  border-radius: 21px;
  background: var(--theme);
  color: #fff;
  box-shadow: 0 16px 32px rgba(17, 35, 70, 0.19);
}
/* ── 저축모드 홈과 동일한 공통 헤더 + 스와이프 캐러셀 ── */
.savings-home-header {
  position: fixed;
  top: 0;
  left: 50%;
  width: 100%;
  max-width: 390px;
  z-index: 60;
  padding: 14px 20px;
  background: #f4f5f9;
  transform: translateX(-50%);
}
.savings-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.home-header-title {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
}
.home-wordmark {
  display: block;
  width: 88px;
  height: auto;
  object-fit: contain;
}
.mode-switch-control {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  flex: none;
  width: 112px;
  padding: 3px;
  overflow: hidden;
  border-radius: 999px;
  background: #edeff3;
}
.mode-switch-control button {
  position: relative;
  z-index: 2;
  height: auto;
  padding: 6px 0;
  border-radius: 999px;
  color: #6b7688;
  font-size: 11px;
  font-weight: 800;
  transition: color 0.25s ease;
}
.mode-switch-control button.selected {
  color: #fff;
}
.mode-switch-thumb {
  position: absolute;
  top: 3px;
  bottom: 3px;
  left: 3px;
  width: calc(50% - 3px);
  border-radius: 999px;
  background: #173f8d;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.country-carousel {
  display: flex;
  gap: 0;
  margin: 7px 16px 0;
  overflow-x: auto;
  overscroll-behavior-x: contain;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  touch-action: pan-x pan-y;
  animation: home-card-reveal 0.56s 0.1s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.country-carousel::-webkit-scrollbar {
  display: none;
}
.country-slide {
  flex: 0 0 100%;
  min-width: 0;
  padding: 0 1px 4px;
  opacity: 0.56;
  transform: translateY(5px) scale(0.965);
  transition: opacity 0.34s ease, transform 0.42s cubic-bezier(0.22, 1, 0.36, 1);
  scroll-snap-align: center;
  scroll-snap-stop: always;
}
.country-slide.active {
  opacity: 1;
  transform: translateY(0) scale(1);
}
.country-slide.active .ticket {
  animation: ticket-card-enter 0.62s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.country-carousel-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 5px 21px 0;
  color: #71809a;
  font-size: 9px;
  font-weight: 700;
  animation: home-fade-up 0.42s 0.2s ease both;
}
.country-carousel-dots {
  display: flex;
  flex: none;
  align-items: center;
  gap: 5px;
}
.country-carousel-dots i {
  display: block;
  width: 6px;
  height: 6px;
  border-radius: 99px;
  background: #cbd5e4;
  transition: width 0.22s ease, background 0.22s ease;
}
.country-carousel-dots i.active {
  width: 17px;
  background: #173f8d;
}
.ticket-top {
  display: flex;
  justify-content: space-between;
  padding: 15px 18px 14px;
  color: #ffffffa6;
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.06em;
}
.perforation {
  position: relative;
  z-index: 3;
  display: grid;
  grid-template-columns: 20px 1fr 20px;
  align-items: center;
  height: 0;
}
.perforation i {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #f4f5f9;
}
.perforation i:first-child {
  transform: translateX(-11px);
}
.perforation i:last-child {
  transform: translateX(9px);
}
.perforation span {
  border-top: 1px dashed #ffffff70;
}
.ticket-main {
  min-height: 312px;
  padding: 20px;
  background:
    linear-gradient(rgba(0, 0, 0, 0.3), rgba(0, 0, 0, 0.3)),
    var(--photo) center/cover;
}
.combined .ticket-main {
  background: linear-gradient(135deg, #103779, #1553a2);
}
.trip-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ticket-label {
  margin-top: 18px;
  color: #ffbd14;
  font-size: 11px;
  font-weight: 800;
}
.ticket h2 {
  margin-top: 5px;
  font-size: 28px;
}
.ticket h2 small {
  font-size: 10px;
  color: #8cebbf;
}
.country-assets {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-top: 14px;
  overflow: hidden;
  border-radius: 12px;
  background: #ffffff12;
}
.country-assets > div {
  position: relative;
  padding: 14px 12px;
  background-position: center;
  background-size: cover;
}
.country-assets > div::before {
  position: absolute;
  inset: 0;
  content: '';
  background: linear-gradient(135deg, #0c4b9fe8, #163c8adb);
}
.country-assets > div.swiss-asset::before {
  background: linear-gradient(135deg, #8d1639e8, #c91432dc);
}
.country-assets .france-asset {
  background-image: url('/images/france.png');
}
.country-assets .swiss-asset {
  background-image: url('/images/switzerland.webp');
}
.country-assets span,
.country-assets b,
.country-assets small {
  position: relative;
  display: block;
  z-index: 1;
}
.country-assets span {
  font-size: 10px;
  color: #e3ecfa;
}
.country-assets b {
  margin-top: 6px;
  font-size: 18px;
}
.country-assets small {
  text-align: right;
  color: #8cebbf;
  font-size: 8px;
}
.daily-budget {
  margin-top: 20px;
  padding: 13px;
  border-radius: 12px;
  background: #ffffff1a;
}
.daily-budget span,
.daily-budget b {
  display: block;
}
.daily-budget span {
  font-size: 10px;
  color: #dde7f8;
}
.daily-budget b {
  margin-top: 6px;
  font-size: 15px;
}
.daily-budget small {
  font-size: 9px;
  color: #8cebbf;
}
.fund-label {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 10px;
  font-weight: 800;
}
.fund-track {
  height: 7px;
  margin-top: 8px;
  overflow: hidden;
  border-radius: 99px;
  background: #ffffff30;
}
.fund-track i {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--bar);
}
.fund-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  color: #d6e1f2;
  font-size: 9px;
}
.fund-progress-box {
  margin-top: 16px;
  padding: 16px;
  border-radius: 12px;
  background: var(--progress-bg);
  animation: home-fade-up 0.45s 0.25s ease both;
}
.fund-progress-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}
.fund-progress-head strong {
  color: #ffd466;
  font-size: 14px;
  font-weight: 800;
}
.fund-progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.25);
}
.fund-progress-track i {
  position: relative;
  display: block;
  height: 100%;
  overflow: hidden;
  border-radius: 99px;
  background: var(--bar);
  transition: width 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}
.fund-progress-track i::after {
  position: absolute;
  inset: 0;
  content: '';
  background: linear-gradient(90deg, transparent, #ffffff99, transparent);
  transform: translateX(-100%);
  animation: progress-shine 1.8s 0.5s ease-in-out infinite;
}
.fund-progress-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}
.fund-progress-meta b {
  display: block;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}
.fund-progress-meta small {
  display: block;
  margin-top: 1px;
  color: #ffffffa6;
  font-size: 9px;
  letter-spacing: 0.04em;
}
.fund-progress-meta .align-right {
  text-align: right;
}
.ticket-stub {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: #fff;
  color: var(--theme);
  font-size: 12px;
  font-weight: 900;
  transition: transform 0.2s ease;
}
.ticket-stub:active {
  transform: scale(0.98);
}
.ticket-stub b {
  color: #263a5b;
  letter-spacing: -1px;
}
.card {
  display: block;
  width: calc(100% - 32px);
  margin: 12px 16px 0;
  padding: 16px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(16, 25, 43, 0.06);
  text-align: left;
  animation: home-card-reveal 0.56s 0.24s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.card:nth-of-type(2) {
  animation-delay: 0.38s;
}
.card:nth-of-type(3) {
  animation-delay: 0.52s;
}
.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.card-title h2 {
  font-size: 15px;
}
.card-title button,
.legend {
  font-size: 9px;
  color: #4b77ca;
}
.legend {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}
.budget-card {
  padding: 20px;
  border: 1px solid #e7edf9;
  border-radius: 20px;
  background: linear-gradient(165deg, #fff 0%, #f8faff 100%);
  box-shadow: 0 8px 22px rgba(16, 25, 43, 0.07);
}
.budget-card-title {
  align-items: flex-start;
  margin-bottom: 0;
}
.budget-card-title h2 {
  color: #173f8d;
  font-size: 17px;
  font-weight: 900;
  letter-spacing: -0.03em;
}
.budget-total-block {
  display: flex;
  align-items: baseline;
  gap: 7px;
  margin-top: 12px;
}
.budget-total-block span {
  color: #98a2b3;
  font-size: 11.5px;
  font-weight: 700;
}
.budget-total-block strong {
  color: #10192b;
  font-family: 'Space Mono', ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 19px;
  font-weight: 800;
}
.budget-divider {
  height: 1px;
  margin: 18px 0;
  background: #eef1f7;
}
.budget-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.budget-heading span:first-child {
  color: #10192b;
  font-size: 13.5px;
  font-weight: 900;
}
.budget-heading span:last-child {
  color: #98a2b3;
  font-size: 11px;
  font-weight: 700;
}
.budget-row {
  display: grid;
  grid-template-columns: 94px minmax(44px, 1fr) auto 30px;
  align-items: center;
  gap: 9px;
  min-height: 45px;
}
.category {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #10192b;
  font-size: 12.5px;
  font-weight: 700;
}
.category i {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border-radius: 11px;
  font-style: normal;
  font-size: 15px;
}
.budget-category-icon {
  display: block;
  width: 18px;
  height: 18px;
}
.budget-category-icon :deep(svg) {
  display: block;
  width: 100%;
  height: 100%;
}
.single-bar,
.split-bar {
  display: flex;
  height: 6px;
  overflow: hidden;
  border-radius: 99px;
  background: #edf0f6;
}
.budget-bar-fill {
  display: flex;
  height: 100%;
  overflow: hidden;
  border-radius: inherit;
  transform-origin: left;
  animation: budget-bar-grow 0.9s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.single-bar i,
.budget-bar-fill i {
  display: block;
  height: 100%;
}
.budget-amount {
  color: #5a6478;
  font-family: 'Space Mono', ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10px;
  font-weight: 500;
  text-align: right;
  white-space: nowrap;
}
.budget-ratio {
  width: 30px;
  color: #98a2b3;
  font-family: 'Space Mono', ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  font-weight: 800;
  text-align: right;
}
@keyframes budget-bar-grow {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}
.budget-total {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid #26333f;
  font-size: 11px;
}
.budget-total strong {
  color: #2872e5;
}
.schedule-row {
  display: flex;
  width: 100%;
  justify-content: space-between;
  align-items: center;
  padding: 11px 0;
  border-top: 1px solid #edf0f4;
  text-align: left;
}
.schedule-row span > * {
  display: block;
}
.schedule-row b {
  margin-bottom: 5px;
  color: #315fc0;
  font-size: 10px;
}
.schedule-row strong {
  font-size: 11px;
}
.schedule-row small {
  margin-top: 4px;
  color: #8996a7;
  font-size: 9px;
}
.schedule-row em {
  padding: 5px 7px;
  border-radius: 6px;
  background: #eaf3ff;
  color: #2472da;
  font-size: 8px;
  font-style: normal;
}
.schedule-row em.warning {
  background: #fff0ef;
  color: #db6258;
}
.recent-card {
  margin-bottom: 12px;
}
.recent-row {
  display: grid;
  width: 100%;
  grid-template-columns: 30px 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 11px 0;
  border-top: 1px solid #edf0f4;
  text-align: left;
}
.recent-row > i {
  display: grid;
  place-items: center;
  font-style: normal;
}
.recent-row > i img {
  width: 16px;
  height: 16px;
}
.recent-row span > * {
  display: block;
}
.recent-row b,
.recent-row strong {
  font-size: 10px;
}
.recent-row small {
  margin-top: 3px;
  color: #8c98a9;
  font-size: 8px;
}
.quick-calculator {
  position: fixed;
  right: max(calc((100vw - 390px) / 2 + 18px), 18px);
  bottom: 77px;
  z-index: 45;
  width: 320px;
  padding: 14px;
  border: 1px solid #dfe5ee;
  border-radius: 15px;
  background: #fff;
  box-shadow: 0 10px 30px #15254733;
}
.calculator-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
.calculator-head button {
  font-size: 18px;
  color: #7b8798;
}
.calculator-currencies {
  display: flex;
  gap: 5px;
  margin-top: 8px;
  overflow-x: auto;
}
.calculator-currencies button {
  flex: none;
  padding: 6px 8px;
  border-radius: 7px;
  background: #f2f4f7;
  font-size: 9px;
}
.calculator-currencies button.active {
  background: #173f8d;
  color: #fff;
}
.calculator-fields {
  display: grid;
  grid-template-columns: 1fr 20px 1fr;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
}
.calculator-fields label,
.calculator-fields output {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px;
  border-radius: 9px;
  background: #f4f5f7;
  font-size: 12px;
  font-weight: 900;
}
.calculator-fields input {
  width: 70px;
  background: transparent;
  font-weight: 900;
  outline: none;
}
.calculator-fields span,
.calculator-fields small {
  color: #9aa4b3;
  font-size: 8px;
}
.calculator-fab {
  position: fixed;
  right: max(calc((100vw - 390px) / 2 + 18px), 18px);
  bottom: 74px;
  z-index: 44;
  width: 48px;
  height: 48px;
  border: 6px solid #dce5f2;
  border-radius: 50%;
  background: #173f8d;
  color: #fff;
  font-size: 22px;
  box-shadow: 0 8px 18px #173f8d3d;
}
.summary-title-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.return-checklist-button {
  padding: 6px 10px;
  border-radius: 8px;
  background: #ffb800;
  color: #173f8d;
  font-size: 10px;
  font-weight: 900;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}
.return-checklist-button:active {
  transform: scale(0.96);
}
.travel-header {
  padding: 40px 16px 12px;
}
.mode-toggle {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 84px;
  padding: 2px;
  overflow: hidden;
}
.mode-toggle button {
  position: relative;
  z-index: 2;
  height: 25px;
  padding: 0;
  border-radius: 999px;
  transition: color 0.25s ease;
}
.mode-toggle button.active {
  background: transparent;
  color: #fff;
}
.mode-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: calc(50% - 2px);
  height: 25px;
  border-radius: 999px;
  background: #173f8d;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.ticket-main {
  min-height: 335px;
  background:
    linear-gradient(rgba(0, 0, 0, 0.3), rgba(0, 0, 0, 0.3)),
    var(--photo) center/cover;
}
.trip-description {
  height: 16px;
  margin-top: 9px;
  overflow: hidden;
  color: #ffffffe0;
  font-size: 9px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.ticket-main {
  min-height: 320px;
  padding: 20px;
}
.ticket-photo-space {
  height: 36px;
}
.travel-summary-panel {
  padding: 12px 14px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--theme) 78%, transparent);
  box-shadow: inset 0 0 0 1px #ffffff0d;
  backdrop-filter: blur(2px);
}
.travel-summary-panel .trip-progress {
  margin-top: 0;
}
.travel-summary-panel .country-assets {
  margin-top: 8px;
  border-radius: 9px;
}
.travel-summary-panel .country-assets > div {
  padding: 8px 9px;
}
.travel-summary-panel .country-assets span {
  font-size: 8px;
}
.travel-summary-panel .country-assets b {
  margin-top: 3px;
  font-size: 13px;
}
.travel-summary-panel .country-assets small {
  font-size: 7px;
}
.travel-summary-panel .daily-budget {
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 9px;
  background: #ffffff12;
}
.travel-summary-panel .daily-budget span {
  font-size: 8px;
}
.travel-summary-panel .daily-budget b {
  margin-top: 3px;
  font-size: 12px;
}
.travel-summary-panel .fund-label {
  margin-top: 9px;
}
.travel-summary-panel .fund-track {
  height: 6px;
  margin-top: 5px;
}
.travel-summary-panel .fund-meta {
  margin-top: 6px;
}
.ticket-stub {
  height: 45px;
  padding: 0 18px;
  background: var(--theme);
  color: #fff;
}
.ticket-stub b {
  color: #fff;
}
.trip-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.trip-destination {
  flex: none;
  min-width: 0;
}
.trip-departure {
  flex: none;
}
.trip-departure {
  text-align: right;
}
.trip-label {
  margin-bottom: 4px;
  color: rgba(255, 255, 255, 0.65);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.trip-country-name {
  color: #fff;
  font-size: 26px;
  font-weight: 800;
  line-height: 1.15;
}
.trip-country-flag {
  margin-right: 6px;
}
.trip-day-count {
  color: #ffd466;
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}
.trip-day-count.is-ended {
  font-size: 16px;
  letter-spacing: -0.04em;
}
.destination-route {
  position: relative;
  flex: 1;
  height: 20px;
  margin-top: 14px;
}
.route-line {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  border-top: 1px dashed rgba(255, 255, 255, 0.4);
}
.route-pin {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
}
.route-pin-dot {
  position: relative;
  z-index: 1;
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fde047;
  box-shadow: 0 0 0 2px rgba(9, 27, 66, 0.55);
}
.route-pin-pulse {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  background: rgba(253, 224, 71, 0.35);
  animation: route-pin-ping 1.8s ease-out infinite;
}
@keyframes route-pin-ping {
  0% { transform: scale(0.4); opacity: 0.8; }
  100% { transform: scale(1.9); opacity: 0; }
}
.trip-day-range {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 11px;
  font-weight: 600;
}
.trip-description {
  height: auto;
  margin-top: 12px;
  overflow: visible;
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  white-space: normal;
}
.ticket-photo-space {
  height: 34px;
}
.travel-summary-content {
  color: #fff;
}
.summary-title {
  display: block;
  margin-top: 0;
}
.summary-title > span {
  display: block;
  margin-bottom: 3px;
  color: #ffffffd9;
  font-size: 11px;
}
.summary-title > strong {
  display: block;
  font-size: 24px;
  line-height: 1.15;
}
.summary-title > strong small {
  margin-left: 4px;
  color: #8ff0bf;
  font-size: 10px;
}
.asset-title {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: end;
  gap: 10px;
}
.asset-title > span {
  margin: 0;
  font-size: 11px;
  font-weight: 800;
}
.asset-title > strong {
  text-align: right;
  font-size: 23px;
}
.travel-summary-content .daily-budget {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid #ffffff0a;
  border-radius: 11px;
  background: #ffffff17;
  backdrop-filter: blur(3px);
}
.travel-summary-content .daily-budget span {
  font-size: 9px;
}
.travel-summary-content .daily-budget b {
  margin-top: 4px;
  font-size: 15px;
}
.travel-summary-content .daily-budget small {
  font-size: 10px;
}
.travel-summary-content .fund-label {
  margin-top: 11px;
  font-size: 11px;
}
.travel-summary-content .fund-track {
  height: 7px;
  margin-top: 6px;
}
.travel-summary-content .fund-meta {
  margin-top: 7px;
  font-size: 9px;
}
.stub-action {
  display: flex;
  align-items: center;
  gap: 8px;
}
.barcode {
  display: flex;
  height: 24px;
  align-items: flex-end;
  gap: 2px;
}
.barcode i {
  display: block;
  background: #fff;
  border-radius: 1px;
}
.ticket-top {
  height: 44px;
  box-sizing: border-box;
}
.perforation {
  position: absolute;
  left: 0;
  right: 0;
  z-index: 5;
  height: 0;
}
.perforation:not(.lower) {
  top: 33px;
}
.perforation.lower {
  bottom: 56px;
}
.asset-title {
  display: block;
}
.asset-title > span {
  display: block;
  margin-bottom: 5px;
  color: #ffbd14;
  font-size: 12px;
  line-height: 1.2;
}
.asset-title > strong {
  display: block;
  text-align: left;
  font-size: 25px;
}
.combined .ticket-main {
  background: linear-gradient(145deg, #12354c 0%, #17606a 58%, #1b485f 100%);
}
.country-assets {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  overscroll-behavior-x: contain;
  border-radius: 10px;
  background: #ffffff0d;
  scrollbar-width: none;
}
.country-assets::-webkit-scrollbar {
  display: none;
}
.country-assets > div.country-asset-card {
  flex: 0 0 calc(50% - 4px);
  min-width: 0;
  padding: 9px 10px;
  scroll-snap-align: start;
  background-image: var(--asset-image);
  background-position: center;
  background-size: cover;
}
.country-assets > div.country-asset-card::before {
  background: var(--asset-theme);
  opacity: 0.84;
}
.country-assets > div.country-asset-card span {
  font-size: 8px;
  white-space: nowrap;
}
.country-assets > div.country-asset-card b {
  font-size: 13px;
  white-space: nowrap;
}
.country-assets > div.country-asset-card small {
  font-size: 7px;
  white-space: nowrap;
}
.summary-title-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.return-checklist-button {
  padding: 6px 10px;
  border-radius: 8px;
  background: #ffb800;
  color: #173f8d;
  font-size: 10px;
  font-weight: 900;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}
.return-checklist-button:active {
  transform: scale(0.96);
}
.travel-header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) 34px;
  align-items: start;
  gap: 10px;
  padding-bottom: 14px;
}
.travel-header h1 {
  padding-top: 6px;
}
.header-controls {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.header-controls .country-select {
  margin-left: 0;
}
.header-controls .country-select > button {
  padding: 7px 9px;
  font-size: 10px;
}
.header-controls .country-menu {
  right: auto;
  left: 0;
  top: 37px;
}
.custom-tooltip {
  position: fixed;
  z-index: 1000;
  padding: 6px 10px;
  background-color: #333;
  color: #fff;
  border-radius: 6px;
  font-size: 11px;
  pointer-events: none;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}
.empty-msg {
  padding: 24px 0;
  text-align: center;
  color: #8c98a9;
  font-size: 11px;
}
</style>
