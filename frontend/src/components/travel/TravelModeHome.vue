<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useTravelModeStore } from '@/stores/travelMode';
import {
  useTravelStore,
  countryPresentation as globalCountryPresentation,
  getTravelCountryColors,
  flagIconClass,
} from '@/stores/travel';
import { useExchangeStore } from '@/stores/exchange';
import { useTripWalletStore } from '@/stores/tripWallet';
import NotificationBell from '@/components/common/NotificationBell.vue';
import foodIcon from '@/assets/icons/food.svg';
import foodIconRaw from '@/assets/icons/food.svg?raw';
import cafeIcon from '@/assets/icons/cafe.svg';
import cafeIconRaw from '@/assets/icons/cafe.svg?raw';
import shoppingIcon from '@/assets/icons/shopping-cart.svg';
import shoppingIconRaw from '@/assets/icons/shopping-cart.svg?raw';
import taxiIcon from '@/assets/icons/taxi.svg';
import taxiIconRaw from '@/assets/icons/taxi.svg?raw';
import leisureIcon from '@/assets/icons/hobby_drink.svg';
import leisureIconRaw from '@/assets/icons/hobby_drink.svg?raw';
import livingIcon from '@/assets/icons/home-dollar.svg';
import livingIconRaw from '@/assets/icons/home-dollar.svg?raw';
import calculatorIcon from '@/assets/icons/calculator.svg';
import checklistIcon from '@/assets/icons/checklist.svg';
import tripassTransparentSymbol from '@/assets/brand/tripass-symbol-transparent-v2.png';
import tosimiTravelCard from '@/assets/cards/kb-travelers-tosimi.png';
import ScheduleCard from '@/components/schedule/ScheduleCard.vue';
import { useTravelScheduleStore } from '@/stores/travelSchedule';
import { now as currentDateTime, today as currentDate, todayIso } from '@/utils/devDate';

const props = defineProps({
  userName: { type: String, default: '권유현' },
  onSwitchMode: { type: Function, default: null },
  cardOnly: { type: Boolean, default: false },
});
const emit = defineEmits(['ready']);

const router = useRouter();
const travelMode = useTravelModeStore();
const travelStore = useTravelStore();
const exchangeStore = useExchangeStore();
const tripWalletStore = useTripWalletStore();
const scheduleStore = useTravelScheduleStore();

function goToReturnChecklist() {
  const id = travelStore.tripId || travelStore.homeDashboard?.tripId;
  if (id) router.push(`/mypage/checklists/return?tripId=${id}`);
}

// 앱 전체를 감싸는 프레임(App.vue)에 overflow:hidden이 걸려 있어
// position:sticky가 동작하지 않는다. 대신 position:fixed로 고정하고,
// 실제 렌더 높이를 측정해 뒤에 그만큼의 여백을 확보한다. (저축모드 홈과 동일한 방식)
const travelHeaderEl = ref(null);
const travelHeaderHeight = ref(0);
const scheduleNow = ref(currentDateTime().getTime());
let travelHeaderResizeObserver = null;
let lowerCardRevealObserver = null;
let scheduleClockTimer = null;

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
const showStartReportModal = ref(false);
const amountDisplayCurrency = ref('foreign');
const exchangeRateCaption = computed(() => exchangeStore.lastUpdateDate
  ? `${exchangeStore.lastUpdateDate} 환율 기준`
  : '최신 환율 기준');

function setAmountDisplayCurrency(currency) {
  amountDisplayCurrency.value = currency;
}

function startReportSeenKey(id) {
  return `tripStartReportSeen:${id}`;
}

async function closeStartReportModal() {
  if (tripId.value) localStorage.setItem(startReportSeenKey(tripId.value), 'true');
  showStartReportModal.value = false;
  await travelStore.acknowledgeStartReport().catch(() => {});
}

async function openStartSavingReport() {
  await closeStartReportModal();
  router.push(`/mypage/reports/pre-trip?tripId=${tripId.value}`);
}

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
    theme: '#101a39',
    progressBg: 'rgba(5, 18, 55, 0.82)',
    barColor: '#ffd466',
    targetBudget: totalTargetBudget,
    spentAmount: totalSpentAmount,
    arrivalDate: overallStart || tripInfo.value?.startDate,
    departureDate: overallEnd || tripInfo.value?.endDate,
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
      currency: c.currencyCode || currencyInfo?.code || '',
      rate: currencyInfo?.rate || 0,
      unit: currencyInfo?.unit || 1,
    };
  });
  // 캐러셀 순서: 국가별 카드 먼저, "전체" 보딩패스는 맨 뒤로
  return [...apiCountries, all];
});

const overallCurrencyBreakdown = computed(() => {
  const grouped = new Map();

  destinations.value
    .filter((item) => item.code !== 'all' && item.currency && item.rate > 0)
    .forEach((item) => {
      const code = String(item.currency).toUpperCase();
      const unit = Number(item.unit || 1);
      const rate = Number(item.rate || 0);
      const current = grouped.get(code) || {
        code,
        spent: 0,
        budget: 0,
        spentKrw: 0,
        budgetKrw: 0,
      };
      current.spent += (Number(item.spentAmount || 0) / rate) * unit;
      current.budget += (Number(item.targetBudget || 0) / rate) * unit;
      current.spentKrw += Number(item.spentAmount || 0);
      current.budgetKrw += Number(item.targetBudget || 0);
      grouped.set(code, current);
    });

  return [...grouped.values()];
});

function formatForeignBreakdown(code, amount) {
  return `${code} ${Number(amount || 0).toLocaleString('ko-KR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

// 날짜 계산
const today = computed(() => {
  return currentDate();
});

const startDate = computed(() => {
  const overallStart = persistentCountries.value
    .map(country => country.arrivalDate || country.startDate)
    .filter(Boolean)
    .sort()[0] || tripInfo.value?.startDate;
  if (!overallStart) return null;
  const [year, month, day] = overallStart.split('-').map(Number);
  return new Date(year, month - 1, day);
});

const currentDay = computed(() => {
  if (!startDate.value) return 0;

  // 오늘 날짜가 시작일보다 이전이면 0일차
  if (today.value < startDate.value) return 0;

  const diff = today.value - startDate.value;
  return Math.floor(diff / (1000 * 60 * 60 * 24)) + 1;
});

// 카테고리 아이콘 매핑
const categoryIcons = {
  식비: '🍴',
  교통: '🚆',
  쇼핑: '🛍️',
  기타: '•••',
  카페: '☕',
  생활비: '🏠',
  '취미·여가': '🎮',
};
const categoryIconImages = {
  식비: foodIcon,
  카페: cafeIcon,
  쇼핑: shoppingIcon,
  교통: taxiIcon,
  생활비: livingIcon,
  '취미·여가': leisureIcon,
};
const categoryIconRawImages = {
  식비: foodIconRaw,
  카페: cafeIconRaw,
  쇼핑: shoppingIconRaw,
  교통: taxiIconRaw,
  생활비: livingIconRaw,
  '취미·여가': leisureIconRaw,
};
const categoryPresentation = {
  식비: { color: '#e0613d', soft: '#fff0ec' },
  교통: { color: '#3478e5', soft: '#edf4ff' },
  쇼핑: { color: '#7449ad', soft: '#f3effd' },
  카페: { color: '#a66c12', soft: '#fff5e8' },
  생활비: { color: '#19a88b', soft: '#e7f6f5' },
  '취미·여가': { color: '#8b5cf6', soft: '#f3effd' },
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
const CATEGORY_ORDER = ['식비', '교통', '쇼핑', '카페', '생활비', '취미·여가', '기타'];
const CATEGORY_IDS = {
  식비: 1,
  교통: 2,
  쇼핑: 4,
  관광: 5,
  기타: 6,
  카페: 7,
  생활비: 8,
  '취미·여가': 9,
};

function openCategoryDetail(category) {
  const destination = destinations.value.find((item) => item.code === selectedCountryId.value);
  router.push({
    name: 'TravelFundCategoryDetail',
    params: { categoryId: CATEGORY_IDS[category.name] || 6 },
    query: {
      categoryName: category.name,
      ...(destination?.code !== 'all' ? { tripCountryId: destination.code } : {}),
      countryName: destination?.code === 'all' ? '전체 여행' : destination?.name,
      startDate: destination?.arrivalDate,
      endDate: destination?.departureDate,
    },
  });
}

const totalCategorySpending = computed(() =>
  categorySummary.value.reduce(
    (sum, category) => sum + Number(category.totalAmount || 0),
    0,
  ),
);

const categoryList = computed(() => {
  const byName = new Map(categorySummary.value.map((cat) => [
    cat.categoryName === '취미여가' ? '취미·여가' : cat.categoryName,
    cat,
  ]));

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
  }).sort((a, b) => b.total - a.total || CATEGORY_ORDER.indexOf(a.name) - CATEGORY_ORDER.indexOf(b.name));
});

const isReturnPeriod = computed(() => {
  if (!travelStore.homeDashboard?.endDate) return false;

  const today = currentDate();

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
  return todayIso();
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
  scheduleClockTimer = window.setInterval(() => {
    scheduleNow.value = currentDateTime().getTime();
  }, 60_000);

  try {
    await nextTick();
    restoreCountryPosition();
    if (!exchangeStore.currencies.length) {
      exchangeStore.updateExchangeRates().catch(() => {});
    }
    tripWalletStore.loadForeignBalances().catch(() => {});
    await travelStore.loadActiveGoal();
    if (tripId.value) {
      // 초기 로딩 시 필터링 없이 전체 데이터를 가져와 캐싱
      const status = await travelStore.loadTripStatus(tripId.value, null);
      persistentCountries.value = status?.countries || [];
      await scheduleStore.loadSchedules().catch(() => {});

      if (travelMode.consumeAutoSelect()) {
        selectedCountryId.value = findTodayCountryCode(persistentCountries.value);
      }

      if (selectedCountryId.value !== 'all') {
        await loadData();
      }

      const lifecycle = travelStore.lifecycle || await travelStore.loadLifecycle();
      showStartReportModal.value = lifecycle?.startReportAvailable === true
        && lifecycle?.startReportAcknowledged !== true
        && localStorage.getItem(startReportSeenKey(tripId.value)) !== 'true';
    }
  } finally {
    isInitialLoading.value = false;
    await nextTick();
    restoreCountryPosition();
    emit('ready');
  }
});

onBeforeUnmount(() => {
  travelHeaderResizeObserver?.disconnect();
  lowerCardRevealObserver?.disconnect();
  window.clearInterval(scheduleClockTimer);
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

function getLowerCardRevealObserver() {
  if (lowerCardRevealObserver || !('IntersectionObserver' in window)) {
    return lowerCardRevealObserver;
  }

  lowerCardRevealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        entry.target.classList.add('is-visible');
        lowerCardRevealObserver?.unobserve(entry.target);
      });
    },
    { threshold: 0.12, rootMargin: '0px 0px -36px' },
  );

  return lowerCardRevealObserver;
}

const vReveal = {
  mounted(el) {
    const observer = getLowerCardRevealObserver();
    if (observer) {
      observer.observe(el);
    } else {
      el.classList.add('is-visible');
    }
  },
  unmounted(el) {
    lowerCardRevealObserver?.unobserve(el);
  },
};

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
// 위 목록은 16개국만 커버해 나머지 국가(태국·미국·영국 등)의 일정을 등록하면
// 국기를 못 찾는다. globalCountryPresentation(전체 국가)로 빠진 나라를 보강한다.
Object.entries(globalCountryPresentation).forEach(([name, info]) => {
  if (info.code && !(name in countryFlagMap)) countryFlagMap[name] = info.code.toLowerCase()
})

function dottedDate(value) {
  return value ? String(value).replaceAll('-', '.') : '';
}

function countryDateRange(item) {
  if (!item?.arrivalDate || !item?.departureDate) return '';
  return `${dottedDate(item.arrivalDate)} — ${dottedDate(item.departureDate)}`;
}

function isCountryComplete(item) {
  return item?.code !== 'all'
    && Boolean(item?.departureDate)
    && todayDateString() > item.departureDate;
}

const travelMetaCountryCodes = computed(() => persistentCountries.value
  .map(country => countryFlagMap[country.countryName])
  .filter(Boolean));

const currentTravelCountry = computed(() => {
  const todayStr = todayDateString();
  return persistentCountries.value.find(country =>
    country.arrivalDate && country.departureDate
      && country.arrivalDate <= todayStr && todayStr <= country.departureDate,
  ) || persistentCountries.value[0] || null;
});

function travelCardCurrencyCode(item) {
  const currentCountry = currentTravelCountry.value;
  return item?.code === 'all'
    ? (currentCountry?.currencyCode || exchangeStore.currencies.find(
      currency => currency.countryName === currentCountry?.countryName,
    )?.code)
    : item?.currency;
}

function travelCardBalanceText(item) {
  const currencyCode = travelCardCurrencyCode(item);
  const balance = tripWalletStore.foreignBalances.find(
    entry => String(entry.currencyCode || entry.code).toUpperCase() === String(currencyCode || '').toUpperCase(),
  );
  const amount = Number(balance?.balanceAmount ?? balance?.amount ?? 0);
  return `${currencyCode || ''} ${amount.toLocaleString('ko-KR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`.trim();
}

function travelCardBalanceKrwText(item) {
  const currencyCode = travelCardCurrencyCode(item);
  const countryName = item?.code === 'all' ? currentTravelCountry.value?.countryName : item?.name;
  const balance = tripWalletStore.foreignBalances.find(
    entry => String(entry.currencyCode || entry.code).toUpperCase() === String(currencyCode || '').toUpperCase(),
  );
  const currency = exchangeStore.currencies.find(
    entry => entry.countryName === countryName,
  ) || exchangeStore.currencies.find(
    entry => String(entry.code).toUpperCase() === String(currencyCode || '').toUpperCase(),
  );
  const amount = Number(balance?.balanceAmount ?? balance?.amount ?? 0);
  const krwAmount = Math.round((amount / Number(currency?.unit || 1)) * Number(currency?.rate || item?.rate || 0));
  return `약 ${krwAmount.toLocaleString('ko-KR')}원`;
}

function foreignBudgetText(item, krwAmount) {
  const code = String(item?.currency || '').toUpperCase();
  const rate = Number(item?.rate || 0);
  const unit = Number(item?.unit || 1);
  if (!code || rate <= 0) return formatWon(krwAmount);

  const foreignAmount = (Number(krwAmount || 0) / rate) * unit;
  return `${code} ${foreignAmount.toLocaleString('ko-KR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function destinationForCountry(countryName) {
  return destinations.value.find(item => item.name === countryName);
}

function displayAmount(krwAmount, countryDetails = []) {
  if (amountDisplayCurrency.value === 'krw') return formatWon(krwAmount);
  if (selected.value.code !== 'all') return foreignBudgetText(selected.value, krwAmount);

  const grouped = new Map();
  (countryDetails || []).forEach(detail => {
    const destination = destinationForCountry(detail.countryName);
    if (!destination?.currency || !destination.rate) return;
    const code = destination.currency;
    const amount = (Number(detail.amount || 0) / destination.rate) * Number(destination.unit || 1);
    grouped.set(code, (grouped.get(code) || 0) + amount);
  });
  if (!grouped.size) return formatWon(krwAmount);
  return [...grouped].map(([code, amount]) => formatForeignBreakdown(code, amount)).join(' · ');
}

function displayCountryAmount(countryName, amount) {
  if (amountDisplayCurrency.value === 'krw') return formatWon(amount);
  return foreignBudgetText(destinationForCountry(countryName), amount);
}

const allTravelCardBalances = computed(() => tripWalletStore.foreignBalances.map((balance) => {
  const currencyCode = String(balance.currencyCode || balance.code || '').toUpperCase();
  const amount = Number(balance.balanceAmount ?? balance.amount ?? 0);
  const currency = exchangeStore.currencies.find(
    entry => String(entry.code || '').toUpperCase() === currencyCode,
  );
  const krwAmount = Math.round(
    (amount / Number(currency?.unit || 1)) * Number(currency?.rate || 0),
  );
  return {
    currencyCode,
    foreignText: `${currencyCode} ${amount.toLocaleString('ko-KR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`,
    krwText: `약 ${krwAmount.toLocaleString('ko-KR')}원`,
  };
}).filter(balance => balance.currencyCode));

function parseScheduleDateTime(dateTime) {
  if (Array.isArray(dateTime)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = dateTime;
    return new Date(year, month - 1, day, hour, minute, second);
  }

  return new Date(dateTime);
}

const selectedSchedules = computed(() => {
  const apiSchedules = tripStatus.value?.upcomingSchedules || [];
  const now = new Date(scheduleNow.value);
  const tomorrowStart = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate() + 1,
  ).getTime();

  const schedules = apiSchedules
    .map((summary) => {
      const scheduleId = summary.id ?? summary.scheduleId;
      const detailed = scheduleStore.schedules.find((item) => Number(item.id) === Number(scheduleId));
      const schedule = detailed || summary;
      const dateTime = detailed?.date
        ? `${detailed.date}T${detailed.time || '00:00'}`
        : summary.dateTime;

      return {
        schedule,
        summary,
        date: parseScheduleDateTime(dateTime),
      };
    })
    .filter(({ date }) => !Number.isNaN(date.getTime()) && date.getTime() >= now.getTime())
    .sort((a, b) => a.date.getTime() - b.date.getTime())
    .map((item) => ({
      ...item,
      isToday: item.date.getTime() < tomorrowStart,
    }));

  return [
    ...schedules.filter((item) => item.isToday),
    ...schedules.filter((item) => !item.isToday),
  ]
    .slice(0, 3)
    .map(({ schedule, summary, date, isToday }, index) => {
      const title = schedule.title || summary.title || '';
      const location = schedule.placeName || schedule.location || summary.location || '';
      const countryName = schedule.countryName || summary.countryName || Object.keys(countryFlagMap).find(
        (name) => location.includes(name) || title.includes(name),
      );
      const countryCode = schedule.countryCode || countryFlagMap[countryName]?.toUpperCase() || '';

      return {
        id: schedule.id ?? summary.scheduleId ?? `${summary.dateTime}-${title}-${location}`,
        title,
        date: isToday
          ? '오늘'
          : `${date.getFullYear()}. ${String(date.getMonth() + 1).padStart(2, '0')}. ${String(date.getDate()).padStart(2, '0')}. (${['일', '월', '화', '수', '목', '금', '토'][date.getDay()]})`,
        time: `${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`,
        flagClass: countryFlagMap[countryName]
          ? `fi fi-${countryFlagMap[countryName]}`
          : 'fi fi-xx',
        status: location,
        warning: false,
        isToday,
        isNext: index === 0,
        schedule: {
          ...schedule,
          id: schedule.id ?? summary.scheduleId,
          title,
          date: [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-'),
          time: `${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`,
          countryCode,
          currency: schedule.currencyCode || schedule.currency || '',
          amount: Number(schedule.amount || 0),
          paymentStatus: String(schedule.paymentStatus || 'undecided').toLowerCase(),
          placeName: location,
        },
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
  <section class="travel-home" :class="{ 'card-only': cardOnly }">
    <Transition name="start-report-fade">
      <div v-if="showStartReportModal" class="start-report-backdrop" role="dialog" aria-modal="true">
        <article class="start-report-modal">
          <span class="start-report-label">YOUR SAVING JOURNEY</span>
          <div class="start-report-plane" aria-hidden="true">✈</div>
          <h2>기다리던 여행이 시작되었어요!</h2>
          <p>여행을 위해 차곡차곡 저축한 기록을<br>확인해 보세요.</p>
          <button type="button" class="start-report-primary" @click="openStartSavingReport">여행 저축 리포트 보기</button>
          <button type="button" class="start-report-secondary" @click="closeStartReportModal">여행 시작하기</button>
          <small>마이페이지 → 여행 관리 → 해당 여행 → 여행 저축 리포트에서 다시 볼 수 있어요.</small>
        </article>
      </div>
    </Transition>
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

    <div v-if="tripId && !isInitialLoading" class="currency-toolbar">
      <span class="currency-toolbar-label">금액 표시</span>
      <span class="amount-currency-toggle header-currency-toggle" aria-label="예산 금액 표시 통화">
        <button type="button" :class="{ active: amountDisplayCurrency === 'foreign' }" :aria-pressed="amountDisplayCurrency === 'foreign'" @click="setAmountDisplayCurrency('foreign')">외화</button>
        <button type="button" :class="{ active: amountDisplayCurrency === 'krw' }" :aria-pressed="amountDisplayCurrency === 'krw'" @click="setAmountDisplayCurrency('krw')">원화</button>
      </span>
    </div>

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
            <div class="ticket-meta-copy">
              <div class="ticket-meta-title">
                <strong>{{ tripInfo?.tripName || '나의 여행' }}</strong>
                <span class="ticket-meta-flags" aria-label="여행 국가">
                  <i v-for="code in travelMetaCountryCodes" :key="code" :class="flagIconClass(code)" />
                </span>
              </div>
            </div>
            <span class="ticket-meta-divider" aria-hidden="true" />
            <span class="ticket-meta-now">
              <b>NOW</b>
              <span>{{ currentTravelCountry?.countryName }}</span>
              <i v-if="currentTravelCountry" :class="flagIconClass(countryFlagMap[currentTravelCountry.countryName])" />
            </span>
            <span class="ticket-meta-divider" aria-hidden="true" />
            <span class="ticket-meta-day">DAY {{ currentDay }}</span>
          </div>
          <div class="perforation"><i /><span /><i /></div>
          <div class="ticket-main">
            <div class="trip-line">
              <div class="trip-destination">
                <p class="trip-country-name">
                  <span>{{ item.code === 'all' ? '전체 국가' : item.name }}</span>
                  <em v-if="isCountryComplete(item)" class="trip-complete-badge">여행 완료</em>
                </p>
                <p
                  v-if="item.arrivalDate && item.departureDate"
                  class="trip-country-dates"
                  :class="{ 'country-date-range': item.code !== 'all' }"
                >{{ countryDateRange(item) }}</p>
              </div>
              <button
                v-if="isReturnPeriod && item.code !== 'all' && !isCountryComplete(item)"
                type="button"
                class="return-checklist-icon"
                aria-label="귀국 체크리스트 확인하기"
                @click="goToReturnChecklist"
              >
                <img :src="checklistIcon" alt="" aria-hidden="true">
                <span>CHECKLIST</span>
              </button>
            </div>
            <div class="ticket-photo-space" />

            <div class="travel-summary-content">
              <section v-if="isCountryComplete(item)" class="completed-country-panel">
                <h3>{{ item.name }} 여행 종료</h3>
                <p>{{ countryDateRange(item) }}</p>
                <div>
                  <span>{{ item.name }} 총 지출</span>
                  <strong>{{ amountDisplayCurrency === 'foreign' ? foreignBudgetText(item, item.spentAmount) : formatWon(item.spentAmount) }}</strong>
                </div>
              </section>
              <template v-else>
              <div class="travel-card-balance-row">
                <img class="travel-card-icon-image" :src="tosimiTravelCard" alt="토심이 트래블카드">
                <div class="travel-card-balance">
                  <small>트래블카드 잔액</small>
                  <div v-if="item.code === 'all'" class="all-travel-card-balances">
                    <div v-for="balance in allTravelCardBalances" :key="balance.currencyCode" class="travel-card-balance-values">
                      <strong>{{ balance.foreignText }}</strong>
                      <em>{{ balance.krwText }}</em>
                    </div>
                    <div v-if="!allTravelCardBalances.length" class="travel-card-balance-values">
                      <strong>0.00</strong>
                      <em>약 0원</em>
                    </div>
                  </div>
                  <div v-else class="travel-card-balance-values">
                    <strong>{{ travelCardBalanceText(item) }}</strong>
                    <em>{{ travelCardBalanceKrwText(item) }}</em>
                  </div>
                  <span class="travel-card-rate-caption">{{ exchangeRateCaption }}</span>
                </div>
              </div>
              <div v-if="item.code === 'all'" class="fund-progress-box overall-fund-progress-box">
                <div class="fund-progress-head">
                  <span class="fund-progress-title">
                    전체 예산 사용률
                  </span><strong>{{ fundPercent(item) }}%</strong>
                </div>
                <div class="fund-progress-track">
                  <i :style="{ width: `${fundPercent(item)}%` }" />
                </div>
                <div class="fund-progress-meta">
                  <div class="overall-fund-stat">
                    <span v-if="amountDisplayCurrency === 'foreign'" class="overall-currency-breakdown">
                      <span
                        v-for="currency in overallCurrencyBreakdown"
                        :key="`spent-${currency.code}`"
                        class="overall-currency-row"
                      >
                        <em>{{ formatForeignBreakdown(currency.code, currency.spent) }}</em>
                      </span>
                    </span>
                    <span v-else class="overall-single-amount">{{ formatWon(item.spentAmount) }}</span>
                    <small>SPENT</small>
                  </div>
                  <div class="overall-fund-stat align-right">
                    <span v-if="amountDisplayCurrency === 'foreign'" class="overall-currency-breakdown align-right">
                      <span
                        v-for="currency in overallCurrencyBreakdown"
                        :key="`budget-${currency.code}`"
                        class="overall-currency-row align-right"
                      >
                        <em>{{ formatForeignBreakdown(currency.code, currency.budget) }}</em>
                      </span>
                    </span>
                    <span v-else class="overall-single-amount">{{ formatWon(item.targetBudget) }}</span>
                    <small>BUDGET</small>
                  </div>
                </div>
              </div>
              <div v-else class="fund-progress-box">
                <div class="fund-progress-head">
                  <span class="fund-progress-title">
                    {{ item.name }} 예산 사용률
                  </span><strong>{{ fundPercent(item) }}%</strong>
                </div>
                <div class="fund-progress-track">
                  <i :style="{ width: `${fundPercent(item)}%` }" />
                </div>
                <div class="fund-progress-meta">
                  <div>
                    <span class="fund-amount-line">
                      <b>{{ amountDisplayCurrency === 'foreign' ? foreignBudgetText(item, item.spentAmount) : formatWon(item.spentAmount) }}</b>
                    </span>
                    <small>SPENT</small>
                  </div>
                  <div class="align-right">
                    <span class="fund-amount-line align-right">
                      <b>{{ amountDisplayCurrency === 'foreign' ? foreignBudgetText(item, item.targetBudget) : formatWon(item.targetBudget) }}</b>
                    </span>
                    <small>BUDGET</small>
                  </div>
                </div>
              </div>
              </template>
            </div>
          </div>
          <div class="perforation lower"><i /><span /><i /></div>
          <div class="ticket-stub" aria-label="TRIPASS 여행 보딩패스">
            <span><img :src="tripassTransparentSymbol" alt="" aria-hidden="true"> TRIPASS</span>
            <b>JOURNEY BOARDING PASS</b>
          </div>
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

    <div
      :key="`travel-dashboard-${countryAnimationKey}`"
      class="travel-dashboard"
    >
    <article
      v-reveal
      class="card budget-card reveal-card"
      style="--card-delay: 0ms"
    >
      <div
        class="card-title budget-card-title"
        :class="{ 'overall-budget-title': selected.code === 'all' }"
      >
        <h2>여행자금 체크</h2>
        <div class="legend" v-if="countries.length > 0">
          <span
            v-for="c in countries"
            :key="c.countryName"
            class="country-badge"
          >
            <i :class="flagIconClass(countryFlagMap[c.countryName])" />
            <b>{{ c.countryName }}</b>
          </span
          >
        </div>
      </div>
      <div class="budget-total-block">
        <span>총 지출</span>
        <strong>{{ displayAmount(totalCategorySpending, categorySummary.flatMap(category => category.countryDetails || [])) }}</strong>
      </div>
      <div class="budget-divider"></div>
      <div class="budget-heading">
        <span>카테고리별 지출</span>
      </div>
      <button
        v-for="(cat, index) in categoryList"
        :key="cat.name"
        type="button"
        class="budget-row"
        :style="{ '--row-delay': `${index * 48}ms` }"
        :aria-label="`${cat.name} 지출 상세보기`"
        @click="openCategoryDetail(cat)"
      >
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
                showTooltip($event, `${d.countryName}: ${displayCountryAmount(d.countryName, d.amount)}`)
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
        <b class="budget-amount">{{ displayAmount(cat.total, cat.details) }}</b>
        <b class="budget-ratio">{{ cat.ratio }}%</b>
        <span class="budget-chevron" aria-hidden="true">›</span>
      </button>
    </article>
    <article
      v-reveal
      class="card schedule-section-card reveal-card"
      style="--card-delay: 70ms"
    >
      <div class="card-title schedule-card-title">
        <div>
          <h2>다가오는 여행 일정</h2>
        </div>
        <button type="button" @click="router.push('/schedule')">
          전체 일정 <span aria-hidden="true">›</span>
        </button>
      </div>
      <div v-if="selectedSchedules.length === 0" class="empty-msg">
        예정된 여행 일정이 없어요.
      </div>
      <div
        v-else
        v-for="(item, index) in selectedSchedules"
        :key="item.id"
        class="home-schedule-row"
        :class="{ 'is-next': item.isNext }"
        :style="{ '--row-delay': `${index * 48}ms` }"
      >
        <small class="home-schedule-date">{{ item.date }}</small>
        <ScheduleCard
          :schedule="item.schedule"
          :today="item.isToday"
          @detail="router.push('/schedule')"
        />
      </div>
    </article>
    </div>

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
          <span :class="flagIconClass(item.code)" class="fi-inline" style="font-size: 13px" /> {{ item.currency }}
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
      <img :src="calculatorIcon" alt="" aria-hidden="true" />
    </button>
    </template>
  </section>
</template>

<style scoped>
.travel-home.card-only > :not(.country-carousel):not(.country-carousel-meta) { display: none; }
.travel-home.card-only {
  min-height: 0;
  margin: 0;
  padding: 0;
}
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
@keyframes home-row-reveal {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
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
  .schedule-card-title h2,
  .schedule-card-title h2::after,
  .card,
  .reveal-card.is-visible .budget-row,
  .reveal-card.is-visible .schedule-row { animation: none; }
  .reveal-card,
  .reveal-card.is-visible { opacity: 1; transform: none; }
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
.currency-toolbar{display:flex;align-items:center;justify-content:flex-end;gap:8px;margin:7px 20px 8px}.currency-toolbar-label{color:#536681;font-size:11px;font-weight:850}.header-currency-toggle{padding:3px;border-color:#b9c9e1;background:#fff;box-shadow:0 5px 14px rgba(22,58,114,.12)}.header-currency-toggle button{min-width:48px;padding:7px 12px;color:#536681;font-size:11px}.header-currency-toggle button.active{background:#1767dc;color:#fff}
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
}
.reveal-card {
  opacity: 0;
  transform: translateY(22px) scale(0.985);
}
.reveal-card.is-visible {
  animation: home-card-reveal 0.56s var(--card-delay, 0ms)
    cubic-bezier(0.22, 1, 0.36, 1) both;
}
.reveal-card.is-visible .budget-row,
.reveal-card.is-visible .schedule-row {
  animation: home-row-reveal 0.42s
    calc(var(--card-delay, 0ms) + 160ms + var(--row-delay, 0ms))
    cubic-bezier(0.22, 1, 0.36, 1) both;
}
@media (prefers-reduced-motion: reduce) {
  .reveal-card,
  .reveal-card.is-visible {
    opacity: 1;
    transform: none;
    animation: none;
  }
  .reveal-card.is-visible .budget-row,
  .reveal-card.is-visible .schedule-row {
    animation: none;
  }
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
  gap: 6px;
}
.legend .country-badge {
  display: inline-flex;
  min-height: 30px;
  align-items: center;
  gap: 7px;
  padding: 5px 10px;
  border-radius: 999px;
  background: #eef5ff;
  color: #315b91;
  white-space: nowrap;
}
.legend .country-badge i {
  display: block;
  width: 28px;
  height: 18px;
  flex: none;
  border-radius: 3px;
  background-size: cover;
  box-shadow: 0 1px 2px rgba(16, 25, 43, 0.12);
}
.legend .country-badge b {
  font-size: 11px;
  font-weight: 900;
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
.budget-card-title.overall-budget-title {
  align-items: center;
  gap: 8px;
}
.overall-budget-title h2 {
  flex: none;
  font-size: 15px;
  white-space: nowrap;
}
.overall-budget-title .legend {
  min-width: 0;
  flex-wrap: nowrap;
  gap: 3px;
}
.overall-budget-title .country-badge {
  min-height: 24px;
  gap: 4px;
  padding: 3px 6px;
}
.overall-budget-title .country-badge i {
  width: 20px;
  height: 13px;
  border-radius: 2px;
}
.overall-budget-title .country-badge b {
  font-size: 9px;
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
  grid-template-columns: 94px minmax(44px, 1fr) auto 30px 12px;
  align-items: center;
  gap: 9px;
  width: 100%;
  min-height: 45px;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.budget-row:active {
  opacity: 0.68;
}
.budget-chevron {
  color: #9aa8bd;
  font-size: 21px;
  font-weight: 800;
  line-height: 1;
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
.schedule-section-card {
  margin-bottom: 12px;
  padding: 20px;
  border: 1px solid #e7edf9;
  border-radius: 20px;
  background: linear-gradient(165deg, #fff 0%, #f8faff 100%);
  box-shadow: 0 8px 22px rgba(16, 25, 43, 0.07);
}
.schedule-card-title {
  align-items: flex-start;
  margin-bottom: 16px;
}
.schedule-card-title h2 {
  position: relative;
  display: inline-block;
  color: #173f8d;
  font-size: 17px;
  font-weight: 900;
  letter-spacing: -0.03em;
  animation: schedule-title-arrive 0.65s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.schedule-card-title h2::after {
  position: absolute;
  right: 0;
  bottom: -5px;
  left: 0;
  height: 3px;
  border-radius: 99px;
  background: linear-gradient(90deg, #ffd45e, #2f70e9);
  content: '';
  transform-origin: left;
  animation: schedule-title-line 2.8s ease-in-out infinite;
}
.schedule-card-title button {
  display: flex;
  align-items: center;
  gap: 3px;
  min-height: 28px;
  padding: 0 9px;
  border-radius: 9px;
  background: #edf4ff;
  color: #2868cf;
  font-size: 9px;
  font-weight: 800;
  white-space: nowrap;
}
.schedule-card-title button span {
  font-size: 14px;
  line-height: 1;
}
@keyframes schedule-title-arrive {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}
@keyframes schedule-title-line {
  0%, 100% { opacity: .45; transform: scaleX(.28); }
  48%, 62% { opacity: 1; transform: scaleX(1); }
}
.home-schedule-row {
  display: grid;
  gap: 6px;
  margin-top: 9px;
}
.home-schedule-row:first-of-type { margin-top: 0; }
.home-schedule-date {
  color: #2f6fed;
  font-family: 'Space Mono', ui-monospace, monospace;
  font-size: 9px;
  font-weight: 800;
}
.schedule-row {
  display: grid;
  width: 100%;
  grid-template-columns: 38px minmax(0, 1fr) 18px;
  align-items: center;
  gap: 11px;
  min-height: 70px;
  margin-top: 9px;
  padding: 11px 12px;
  border: 1px solid #edf1f8;
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 3px 10px rgba(26, 52, 96, 0.04);
  text-align: left;
  transition: transform 0.2s ease, border-color 0.2s ease,
    box-shadow 0.2s ease;
}
.schedule-row:first-of-type {
  margin-top: 0;
}
.schedule-row:active {
  transform: scale(0.985);
  border-color: #cfddf6;
  box-shadow: 0 2px 7px rgba(26, 52, 96, 0.06);
}
.schedule-row.is-next {
  border-color: #cbdcff;
  background: linear-gradient(135deg, #f4f8ff 0%, #fff 72%);
  box-shadow: 0 5px 14px rgba(40, 104, 207, 0.09);
}
.schedule-row.is-next .schedule-marker {
  background: linear-gradient(145deg, #173f8d, #2868cf);
}
.schedule-row.is-next .schedule-marker::after {
  background: #ffd45f;
}
.schedule-row.is-next .schedule-meta b {
  padding: 2px 7px;
  border-radius: 6px;
  background: #173f8d;
  color: #fff;
}
.schedule-marker {
  position: relative;
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 13px;
  background: linear-gradient(145deg, #e8f1ff, #f3f7ff);
  box-shadow: inset 0 0 0 1px rgba(50, 104, 199, 0.05);
}
.schedule-marker::after {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 8px;
  height: 8px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #3975d8;
  content: '';
}
.schedule-marker i {
  font-size: 18px;
  border-radius: 2px;
  box-shadow: 0 2px 5px rgba(17, 38, 74, 0.12);
}
.schedule-content {
  min-width: 0;
}
.schedule-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 4px;
}
.schedule-meta b {
  color: #2868cf;
  font-size: 9.5px;
  font-weight: 900;
}
.schedule-meta time {
  padding: 2px 6px;
  border-radius: 6px;
  background: #eef3fa;
  color: #6f7d92;
  font-family: 'Space Mono', ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 8px;
  font-weight: 700;
}
.schedule-content > strong {
  display: block;
  overflow: hidden;
  color: #111b2f;
  font-size: 11.5px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.schedule-content > small {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-top: 5px;
  overflow: hidden;
  color: #8793a6;
  font-size: 8.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.schedule-content > small span {
  color: #5b82c5;
  font-size: 10px;
}
.schedule-arrow {
  color: #9ba8ba;
  font-size: 20px;
  font-weight: 300;
  text-align: right;
}
.quick-calculator {
  position: fixed;
  right: max(calc((100vw - 390px) / 2 + 18px), 12px);
  bottom: 136px;
  z-index: 45;
  width: min(354px, calc(100vw - 24px));
  padding: 18px;
  border: 1px solid #bfd3f2;
  border-radius: 18px;
  background: linear-gradient(135deg, #dce9fb 0%, #c8daf6 100%);
  box-shadow: 0 12px 30px rgba(23, 63, 141, .2);
}
.calculator-head {
  display: flex;
  justify-content: space-between;
  font-size: 15px;
}
.calculator-head button {
  font-size: 21px;
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
  background: rgba(255, 255, 255, .68);
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
  margin-top: 12px;
}
.calculator-fields label,
.calculator-fields output {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid rgba(23, 63, 141, .1);
  background: rgba(255, 255, 255, .78);
  font-size: 14px;
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
  font-size: 9px;
}
.calculator-fab {
  position: fixed;
  right: max(calc((100vw - 390px) / 2 + 18px), 18px);
  bottom: 74px;
  z-index: 44;
  width: 48px;
  height: 48px;
  border: 5px solid #fff;
  border-radius: 50%;
  background: linear-gradient(135deg, #dce9fb 0%, #c8daf6 100%);
  box-shadow: 0 8px 18px rgba(23, 63, 141, .25);
}
.calculator-fab img {
  display: block;
  width: 23px;
  height: 23px;
  margin: auto;
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
  display: inline-block;
  width: 28px;
  height: 19px;
  margin-right: 6px;
  border-radius: 3px;
  background-size: cover;
  box-shadow: 0 2px 5px rgba(0, 0, 0, .18);
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
.start-report-backdrop{position:fixed;inset:0;z-index:300;display:grid;place-items:center;padding:24px;background:#07183fb8;backdrop-filter:blur(7px)}
.start-report-modal{width:min(100%,342px);padding:30px 22px 22px;border-radius:26px;background:linear-gradient(160deg,#fff 0%,#edf4ff 100%);text-align:center;box-shadow:0 28px 70px #06153680}
.start-report-label{font-size:9px;font-weight:900;letter-spacing:.16em;color:#2f6fed}
.start-report-plane{width:68px;height:68px;margin:17px auto 13px;display:grid;place-items:center;border-radius:50%;background:#17499c;color:#ffd466;font-size:29px;box-shadow:0 12px 24px #17499c3d}
.start-report-modal h2{font-size:20px;font-weight:950;color:#10192b}
.start-report-modal p{margin:9px 0 20px;font-size:13px;line-height:1.7;color:#637087}
.start-report-modal button{width:100%;height:49px;border-radius:15px;font-size:13px;font-weight:900}
.start-report-primary{border:0;background:#17499c;color:#fff}
.start-report-secondary{margin-top:8px;border:1px solid #ccd8ea;background:#fff;color:#24426f}
.start-report-modal small{display:block;margin-top:14px;font-size:9px;line-height:1.5;color:#8995a8}
.start-report-fade-enter-active,.start-report-fade-leave-active{transition:opacity .2s ease}.start-report-fade-enter-from,.start-report-fade-leave-to{opacity:0}
.ticket-top{display:flex;height:44px;min-height:44px;box-sizing:border-box;align-items:center;gap:9px;padding:6px 12px;color:#fff;letter-spacing:normal}.ticket-meta-copy{display:flex;min-width:0;flex:1;flex-direction:column;gap:3px}.ticket-meta-title{display:flex;min-width:0;align-items:center;gap:6px}.ticket-meta-title>strong{overflow:hidden;font-size:12px;font-weight:900;text-overflow:ellipsis;white-space:nowrap}.ticket-meta-flags{display:flex;flex:none;gap:2px}.ticket-meta-flags i,.ticket-meta-now i{display:block;width:19px;height:13px;border-radius:2px;background-size:cover;box-shadow:0 1px 3px rgba(0,0,0,.18)}.ticket-meta-copy>small{overflow:hidden;color:rgba(255,255,255,.68);font-family:'Space Mono',ui-monospace,monospace;font-size:8px;font-weight:700;text-overflow:ellipsis;white-space:nowrap}.ticket-meta-divider{width:1px;height:20px;flex:none;background:rgba(255,255,255,.24)}.ticket-meta-day{flex:none;padding:5px 9px;border-radius:999px;color:#173f8d;background:#ffd45e;font-family:'Space Mono',ui-monospace,monospace;font-size:9px;font-weight:950}.ticket-meta-now{display:flex;flex:none;align-items:center;gap:4px;padding:5px 7px;border:1px solid rgba(255,255,255,.25);border-radius:999px;background:rgba(255,255,255,.12);font-size:9px;font-weight:800}.ticket-meta-now b{color:#ffd45e;font-family:'Space Mono',ui-monospace,monospace;font-size:8px}.ticket-meta-now i{width:20px;height:14px}.perforation:not(.lower){position:relative;top:auto}.trip-country-dates{margin-top:5px;color:rgba(255,255,255,.74);font-family:'Space Mono',ui-monospace,monospace;font-size:9px;font-weight:700}.trip-destination{min-width:0}.trip-country-name{margin-top:0}.summary-title-spacer{height:45px}
.country-date-range{display:block}
.ticket{--ticket-edge-height:45px}.ticket-top,.ticket-stub{height:var(--ticket-edge-height);min-height:var(--ticket-edge-height);box-sizing:border-box}.ticket-stub{display:flex;align-items:center;justify-content:space-between;padding:0 14px;color:rgba(255,255,255,.72);font-family:'Space Mono',ui-monospace,monospace;font-size:8px;font-weight:800;letter-spacing:.08em}.ticket-stub>span{color:#ffd45e}.ticket-stub>b{color:#fff;font-size:8px;letter-spacing:.05em}
.ticket{--ticket-edge-height:60px;--ticket-perforation-height:15px}.ticket-top{height:calc(var(--ticket-edge-height) - var(--ticket-perforation-height));min-height:calc(var(--ticket-edge-height) - var(--ticket-perforation-height))}.perforation:not(.lower){height:var(--ticket-perforation-height);background:var(--theme)}.ticket-stub{height:var(--ticket-edge-height);min-height:var(--ticket-edge-height);padding:0 16px;font-size:10px}.ticket-stub>span{display:flex;align-items:center;gap:6px;font-size:11px}.ticket-stub>span img{width:21px;height:21px;object-fit:contain}.ticket-stub>b{font-size:10px;letter-spacing:.06em}
.ticket{--ticket-edge-height:45px}.ticket-top,.ticket-stub{height:var(--ticket-edge-height);min-height:var(--ticket-edge-height)}.perforation:not(.lower){position:absolute;top:var(--ticket-edge-height);height:0;background:transparent}.perforation.lower{bottom:var(--ticket-edge-height)}.ticket-stub{padding:0 14px;font-size:9px}.ticket-stub>span{font-size:10px}.ticket-stub>span img{width:18px;height:18px}.ticket-stub>b{font-size:9px}
.perforation:not(.lower),.perforation.lower{transform:translateY(-11px)}
.travel-card-balance{display:flex;flex-direction:column;align-items:flex-start;text-align:left}.travel-card-balance small{color:rgba(255,255,255,.72);font-size:8px;font-weight:700}.travel-card-balance strong{margin-top:2px;color:#fff;font-family:inherit;font-size:11px;font-weight:900}
.fund-progress-title{display:inline-flex;align-items:center;gap:7px}.amount-currency-toggle{display:inline-flex;padding:2px;border:1px solid rgba(255,255,255,.3);border-radius:999px;background:rgba(4,17,48,.45)}.amount-currency-toggle button{min-width:29px;padding:3px 6px;border:0;border-radius:999px;background:transparent;color:rgba(255,255,255,.66);font-size:7px;font-weight:900;line-height:1}.amount-currency-toggle button.active{background:#fff;color:#17499c;box-shadow:0 2px 6px rgba(0,0,0,.18)}
.ticket:not(.combined) .ticket-main{display:flex;flex-direction:column}.ticket:not(.combined) .ticket-photo-space{min-height:34px;height:auto;flex:1}.ticket:not(.combined) .travel-summary-content{margin-top:auto}.ticket:not(.combined) .summary-title-spacer{display:none}
.return-checklist-icon{display:inline-flex;flex:none;align-items:center;flex-direction:column;gap:4px;padding:7px 9px 6px;border:1px solid rgba(255,255,255,.38);border-radius:12px;background:rgba(7,22,55,.76);color:#ffd466;font-family:'Space Mono',ui-monospace,monospace;font-size:7px;font-weight:900;letter-spacing:.04em;box-shadow:0 7px 16px rgba(3,17,45,.24);backdrop-filter:blur(7px);animation:return-checklist-float 2.4s ease-in-out infinite}.return-checklist-icon img{width:27px;height:27px;padding:5px;border-radius:8px;background:#fff;object-fit:contain}.return-checklist-icon:active{transform:scale(.96)}@keyframes return-checklist-float{0%,100%{transform:translateY(0)}50%{transform:translateY(-3px)}}
.travel-card-balance-values{display:flex;align-items:baseline;gap:8px}.travel-card-balance-values em{color:#8cebbf;font-size:11px;font-style:normal;font-weight:800;white-space:nowrap}
.travel-card-rate-caption{display:block;margin-top:3px;color:rgba(255,255,255,.68);font-size:8px;font-weight:700;line-height:1.25;white-space:nowrap}
.all-travel-card-balances{display:flex;flex-wrap:wrap;gap:3px 12px}.all-travel-card-balances .travel-card-balance-values{flex:0 0 auto}
.combined .ticket-main{display:flex;flex-direction:column;background:linear-gradient(145deg,#0b1635 0%,#152b62 58%,#10224d 100%)}.combined .ticket-photo-space{min-height:24px;height:auto;flex:1}.combined .travel-summary-content{margin-top:auto}.overall-fund-progress-box{margin-top:0;background:rgba(4,14,44,.8);box-shadow:0 12px 28px rgba(0,0,0,.2)}
.travel-card-balance-row{display:flex;width:fit-content;max-width:100%;align-items:center;gap:13px;margin-bottom:12px;perspective:180px}.travel-card-balance-row .travel-card-icon-image{width:40px;height:56px;flex:none;border:1px solid rgba(255,255,255,.58);border-radius:6px;object-fit:cover;box-shadow:0 6px 14px rgba(3,16,43,.34);transform-origin:center;animation:travel-card-flip 4s ease-in-out infinite}.travel-card-balance-row .travel-card-balance small{color:#ffd466;font-size:15px;font-weight:950;letter-spacing:-.02em}.travel-card-balance-row .travel-card-balance strong{color:#fff;font-size:20px;line-height:1.2;text-shadow:0 2px 8px rgba(0,0,0,.28)}
@keyframes travel-card-flip{0%,35%{transform:rotateY(0)}50%{transform:rotateY(180deg)}65%,100%{transform:rotateY(360deg)}}
@media (prefers-reduced-motion:reduce){.travel-card-balance-row .travel-card-icon-image,.return-checklist-icon{animation:none}}
.fund-progress-meta>div:first-child small{color:#ff9b9b}.fund-progress-meta>div:last-child small{color:#ffd466}.fund-progress-meta b{color:#fff;font-weight:900}.fund-progress-meta small{font-weight:900;opacity:1}
.ticket-meta-day{padding:0;border-radius:0;color:#ffd45e;background:transparent;font-size:17px;line-height:1;white-space:nowrap}
.trip-complete-badge{display:inline-flex;align-items:center;margin-left:8px;padding:5px 9px;border-radius:999px;background:#cce7ff;color:#173f75;font-size:9px;font-style:normal;font-weight:900;vertical-align:middle}
.completed-country-panel{padding:22px 18px 16px;border:1px solid rgba(151,190,255,.72);border-radius:18px;background:rgba(8,35,91,.86);box-shadow:0 12px 28px rgba(0,0,0,.25);text-align:center;backdrop-filter:blur(3px)}
.completed-country-panel h3{color:#fff;font-size:27px;font-weight:950;letter-spacing:-.04em;line-height:1.15;text-shadow:0 2px 8px rgba(0,0,0,.25)}
.completed-country-panel>p{margin-top:9px;color:#9dc8f4;font-family:'Space Mono',ui-monospace,monospace;font-size:10px;font-weight:800}
.completed-country-panel>div{display:flex;align-items:center;justify-content:space-between;margin-top:18px;padding-top:14px;border-top:1px dashed rgba(255,255,255,.48);text-align:left}
.completed-country-panel span{font-size:12px;font-weight:850}
.completed-country-panel strong{font-size:22px;font-weight:950}
.fund-amount-line{display:flex;align-items:baseline;gap:5px;white-space:nowrap}
.fund-amount-line.align-right{justify-content:flex-end}
.fund-amount-line em{color:#8cebbf;font-size:8px;font-style:normal;font-weight:850}
.overall-fund-stat{width:50%;min-width:0}
.overall-currency-breakdown{display:flex;flex-direction:column;align-items:flex-start;gap:6px;white-space:nowrap}
.overall-currency-breakdown.align-right{align-items:flex-end}
.overall-currency-breakdown em{color:#fff;font-size:12px;font-style:normal;font-weight:900}
.overall-currency-row{display:flex;align-items:baseline;gap:5px}
.overall-currency-row.align-right{justify-content:flex-end}
.overall-currency-row i{color:#8cebbf;font-size:8px;font-style:normal;font-weight:850}
.overall-fund-stat>small{margin-top:7px}
.overall-single-amount{display:block;color:#fff;font-size:14px;font-weight:950;white-space:nowrap}
</style>
