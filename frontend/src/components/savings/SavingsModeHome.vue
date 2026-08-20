<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useExchangeStore } from '@/stores/exchange';
import { useMonthlyAnalysisStore } from '@/stores/monthlyAnalysis';
import { useSavingMissionsStore } from '@/stores/savingMissions';
import { useSavingReadinessStore } from '@/stores/savingReadiness';
import { useTravelStore, countryPresentation as globalCountryPresentation, flagIconClass } from '@/stores/travel';
import { useTravelModeStore } from '@/stores/travelMode';
import { getAccounts } from '@/api/asset';
import { getCards } from '@/api/card';
import NotificationBell from '@/components/common/NotificationBell.vue';
import MonthlyAnalysisSummaryCard from '@/components/savings/MonthlyAnalysisSummaryCard.vue';
import HomeSavingMissionCard from '@/components/savings/HomeSavingMissionCard.vue';
import aiIcon from '@/assets/icons/ai.svg';

const props = defineProps({
  onSwitchMode: { type: Function, default: null },
});

const exchangeStore = useExchangeStore();
const monthlyAnalysisStore = useMonthlyAnalysisStore();
const savingMissionsStore = useSavingMissionsStore();
const savingReadinessStore = useSavingReadinessStore();
const travelStore = useTravelStore();
const travelModeStore = useTravelModeStore();
const router = useRouter();

// 앱 전체를 감싸는 프레임(App.vue)에 overflow:hidden이 걸려 있어
// position:sticky가 동작하지 않는다. 대신 position:fixed로 고정하고,
// 실제 렌더 높이를 측정해 뒤에 그만큼의 여백을 확보한다.
const savingsHeaderEl = ref(null);
const savingsHeaderHeight = ref(0);
let savingsHeaderResizeObserver = null;

function syncSavingsHeaderHeight() {
  if (savingsHeaderEl.value) {
    savingsHeaderHeight.value = savingsHeaderEl.value.offsetHeight;
  }
}

// 헤더는 v-else 분기에서만 렌더링되므로, 마운트 시점에 아직 없을 수 있다.
// homeDashboard가 늦게 도착해 분기가 바뀌는 경우까지 대응하기 위해
// ref 자체를 감시해서 등장할 때마다 옵저버를 다시 붙인다.
watch(savingsHeaderEl, (el) => {
  savingsHeaderResizeObserver?.disconnect();
  savingsHeaderResizeObserver = null;
  if (!el) return;

  syncSavingsHeaderHeight();
  if (window.ResizeObserver) {
    savingsHeaderResizeObserver = new ResizeObserver(syncSavingsHeaderHeight);
    savingsHeaderResizeObserver.observe(el);
  }
});
const linkedAccountCount = ref(0);
const linkedCardCount = ref(0);
const financialSourcesLoading = ref(false);
const financialSourcesError = ref('');
const hasLinkedFinancialSources = computed(
  () => linkedAccountCount.value + linkedCardCount.value > 0,
);

async function loadFinancialSources() {
  financialSourcesLoading.value = true;
  financialSourcesError.value = '';
  const [accountResult, cardResult] = await Promise.allSettled([
    getAccounts(),
    getCards(),
  ]);

  linkedAccountCount.value =
    accountResult.status === 'fulfilled'
      ? accountResult.value.data?.data?.length ?? 0
      : 0;
  linkedCardCount.value =
    cardResult.status === 'fulfilled'
      ? cardResult.value.data?.data?.length ?? 0
      : 0;

  if (accountResult.status === 'rejected' && cardResult.status === 'rejected') {
    financialSourcesError.value = '금융 데이터 연결 상태를 확인하지 못했어요.';
  }
  financialSourcesLoading.value = false;
}

async function loadHomeInsights({ force = false } = {}) {
  await Promise.all([
    loadFinancialSources(),
    savingMissionsStore.loadMissionStatus(),
  ]);

  if (financialSourcesError.value || !hasLinkedFinancialSources.value) {
    monthlyAnalysisStore.resetAnalysis();
    return;
  }

  await monthlyAnalysisStore.loadCurrentAnalysis({ force });
}

onMounted(async () => {
  await nextTick();
  restoreCountryPosition();
  await Promise.all([
    travelStore.loadHomeDashboard({ force: true }),
    exchangeStore.updateExchangeRates(),
    savingReadinessStore.load({ force: true }),
    loadHomeInsights({ force: true }),
  ]);
  await nextTick();
  restoreCountryPosition();
});

onBeforeUnmount(() => {
  savingsHeaderResizeObserver?.disconnect();
});

const countryPresentation = {
  프랑스: {
    flag: '🇫🇷',
    code: 'FR',
    image: '/images/france.png',
    headerBg: '#1a2d6e',
    progressBg: 'rgba(0,35,149,0.80)',
    barColor: 'linear-gradient(90deg,#002395 0%,#EDEDED 50%,#ED2939 100%)',
    desc: '프랑스에서의 여행을 준비하고 있어요.',
  },
  스위스: {
    flag: '🇨🇭',
    code: 'CH',
    image: '/images/switzerland.webp',
    headerBg: '#7a0d1e',
    progressBg: 'rgba(122,13,30,0.82)',
    barColor: 'linear-gradient(90deg,#FF0000 0%,#FFFFFF 60%,#FF0000 100%)',
    desc: '스위스에서의 여행을 준비하고 있어요.',
  },
  독일: {
    flag: '🇩🇪',
    code: 'DE',
    image: '/images/germany.png',
    headerBg: '#111111',
    progressBg: 'rgba(17,17,17,0.85)',
    barColor: 'linear-gradient(90deg,#000000 0%,#DD0000 50%,#FFCE00 100%)',
    desc: '독일에서의 여행을 준비하고 있어요.',
  },
  일본: {
    flag: '🇯🇵',
    code: 'JP',
    image: '/images/japan.webp',
    headerBg: '#c2185b',
    progressBg: 'rgba(194,24,91,0.82)',
    barColor:
      'linear-gradient(90deg,#FFFFFF 0%,#BC002D 35%,#BC002D 65%,#FFFFFF 100%)',
    desc: '일본에서의 여행을 준비하고 있어요.',
  },
  홍콩: {
    flag: '🇭🇰',
    code: 'HK',
    image: '/images/Hong%20Kong.png',
    headerBg: '#b8202e',
    progressBg: 'rgba(184,32,46,0.84)',
    barColor: 'linear-gradient(90deg,#DE2910 0%,#FFDE00 100%)',
    desc: '홍콩에서의 여행을 준비하고 있어요.',
  },
};

const defaultPresentation = {
  flag: '✈️',
  code: 'TRIP',
  image: '',
  headerBg: '#173f8d',
  progressBg: 'rgba(23,63,141,0.84)',
  barColor: 'linear-gradient(90deg,#64d8cb,#fff0b3)',
  displayOrder: '-',
  countryName: '여행지',
  name: '여행지',
  currency: '-',
};

const homeDashboard = computed(() => travelStore.homeDashboard);
const isHomePending = computed(
  () =>
    travelStore.homeLoading ||
    (!homeDashboard.value &&
      !travelStore.homeError &&
      (!travelStore.initialized || travelStore.hasTravelGoal)),
);
// 위 countryPresentation에 없는 국가는 stores/travel.js의 공용 국가 정보(accent, image)로 대체한다.
function hexToRgba(hex, alpha) {
  const clean = (hex || '').replace('#', '');
  const num = parseInt(clean, 16);
  if (Number.isNaN(num)) return `rgba(23,63,141,${alpha})`;
  const r = (num >> 16) & 255;
  const g = (num >> 8) & 255;
  const b = num & 255;
  return `rgba(${r},${g},${b},${alpha})`;
}
function fallbackPresentation(country) {
  const shared = globalCountryPresentation[country.countryName];
  if (!shared) return defaultPresentation;
  const accent = shared.accent || defaultPresentation.headerBg;
  return {
    flag: shared.flag,
    code: shared.code,
    image: shared.image || '',
    headerBg: accent,
    progressBg: hexToRgba(accent, 0.84),
    barColor: `linear-gradient(90deg, ${accent} 0%, ${accent}99 100%)`,
    desc: `${country.countryName}에서의 여행을 준비하고 있어요.`,
  };
}

const countries = computed(() =>
  [...(homeDashboard.value?.countries || [])]
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map((country) => {
      const presentation =
        countryPresentation[country.countryName] || fallbackPresentation(country);
      return {
        ...country,
        ...presentation,
        id: country.tripCountryId,
        name: country.countryName,
        currency: country.currencyCode,
        desc:
          presentation.desc ||
          `${country.countryName}에서의 여행을 준비하고 있어요.`,
      };
    }),
);
const countryCarousel = ref(null);
const countryAnimationKey = ref(0);
const selectedCountryId = computed({
  get: () => travelStore.homeSelectedCountryId,
  set: (countryId) => travelStore.setHomeSelectedCountry(countryId),
});
const selectedCountry = computed(
  () =>
    countries.value.find((country) => country.id === selectedCountryId.value) ||
    countries.value[0] ||
    defaultPresentation,
);

const homeGoalAmount = computed(() =>
  Number(homeDashboard.value?.totalTargetAmount || 0),
);
const homeSavedAmount = computed(() =>
  Number(homeDashboard.value?.walletBalance || 0),
);
const homeRemainingAmount = computed(() =>
  Number(homeDashboard.value?.remainingTargetAmount || 0),
);
const homeSavingsPercent = computed(() =>
  Number(homeDashboard.value?.savingProgressPercent || 0),
);
const monthlyTarget = computed(() =>
  Number(homeDashboard.value?.monthlySavingTarget || 0),
);
const daysUntilDeparture = computed(() =>
  Number(homeDashboard.value?.daysUntilDeparture ?? 0),
);

const checklistInfo = computed(() => {
  const days = daysUntilDeparture.value;
  if (days >= 8 && days <= 30) return { label: 'D-30 체크리스트' };
  if (days >= 2 && days <= 7) return { label: 'D-7 체크리스트' };
  if (days >= 0 && days <= 1) return { label: 'D-1 체크리스트' };
  return null;
});

const currentMonthLabel = computed(() => `${new Date().getMonth() + 1}월`);
const monthlySavedAmount = computed(() =>
  Number(homeDashboard.value?.currentMonthSaving || 0),
);
const monthlyRemainingAmount = computed(() =>
  Math.max(0, monthlyTarget.value - monthlySavedAmount.value),
);
const monthlySavingPercent = computed(() =>
  monthlyTarget.value > 0
    ? Math.min(
        100,
        Math.round((monthlySavedAmount.value / monthlyTarget.value) * 100),
      )
    : 0,
);
const homeInsightLoading = computed(
  () =>
    savingReadinessStore.loading ||
    savingMissionsStore.missionStatusLoading ||
    (!savingMissionsStore.hasStartedMissions && monthlyAnalysisStore.loading) ||
    (!savingMissionsStore.hasStartedMissions &&
      !monthlyAnalysisStore.hasVisibleReport &&
      financialSourcesLoading.value),
);
const selectedExchangeRate = computed(() =>
  exchangeStore.getCurrency(selectedCountry.value.currency),
);
const exchangeUnitLabel = computed(() => {
  const rate = selectedExchangeRate.value;
  if (!rate) return selectedCountry.value.currency || '-';
  return Number(rate.unit || 1) > 1 ? `${rate.unit}${rate.code}` : rate.code;
});
const exchangeChangePercent = computed(() => {
  const rate = selectedExchangeRate.value;
  if (!rate?.rate || !rate.change) return 0;
  const previousRate = Number(rate.rate) - Number(rate.change);
  return previousRate ? (Number(rate.change) / previousRate) * 100 : 0;
});
const exchangeCardStyle = computed(() => ({
  '--exchange-primary': selectedCountry.value.headerBg || '#173f8d',
  '--exchange-accent':
    selectedCountry.value.code === 'DE' ? '#ffce00' : '#ffffff',
  '--exchange-glow':
    selectedCountry.value.code === 'CH'
      ? 'rgba(255,255,255,.16)'
      : 'rgba(94,160,255,.22)',
  '--exchange-photo': selectedCountry.value.image
    ? `url(${selectedCountry.value.image})`
    : 'none',
}));

const ticketSavingCopy = computed(() => {
  if (!homeGoalAmount.value) {
    return {
      title: '여행 예산을 확정해 주세요',
      amountLabel: '목표 예산 미설정',
    };
  }
  if (!homeRemainingAmount.value) {
    return {
      title: '여행 저축 목표를 달성했어요',
      amountLabel: 'GOAL COMPLETE',
    };
  }
  return {
    title: '여행 저축 목표',
    amountLabel: 'SAVED',
  };
});

function formatCurrency(value) {
  return Math.max(0, Number(value) || 0).toLocaleString('ko-KR') + '원';
}

function formatDate(value) {
  if (Array.isArray(value)) {
    const [year, month, day] = value;
    return `${year}.${String(month).padStart(2, '0')}.${String(day).padStart(2, '0')}`;
  }
  if (typeof value === 'string') return value.replaceAll('-', '.');
  return '-';
}

function formatRate(value) {
  return Number(value || 0).toLocaleString('ko-KR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function handleCountryScroll(event) {
  const carousel = event.currentTarget;
  if (!carousel?.clientWidth) return;

  const countryIndex = Math.max(
    0,
    Math.min(
      countries.value.length - 1,
      Math.round(carousel.scrollLeft / carousel.clientWidth),
    ),
  );
  const nextCountryId = countries.value[countryIndex]?.id ?? null;
  if (nextCountryId === selectedCountryId.value) return;
  selectedCountryId.value = nextCountryId;
  countryAnimationKey.value += 1;
}

function restoreCountryPosition() {
  const carousel = countryCarousel.value;
  if (!carousel?.clientWidth || !countries.value.length) return;

  const savedIndex = countries.value.findIndex(
    (country) => country.id === selectedCountryId.value,
  );
  const countryIndex = savedIndex >= 0 ? savedIndex : 0;
  selectedCountryId.value = countries.value[countryIndex].id;
  carousel.scrollLeft = countryIndex * carousel.clientWidth;
}

function retryHome() {
  travelStore.loadHomeDashboard({ force: true });
}

function retryMonthlyAnalysis() {
  Promise.all([
    savingReadinessStore.load({ force: true }),
    loadHomeInsights({ force: true }),
  ]);
}

function openFinancialSources() {
  router.push('/profile/financial?step=1');
}

function openMonthlyAnalysis() {
  const yearMonth = monthlyAnalysisStore.report?.analysisYearMonth;
  if (!yearMonth) return;
  router.push({ name: 'MonthlyAnalysisReport', params: { yearMonth } });
}

function openSavingMissions() {
  router.push({ name: 'SavingsMissions' });
}

function openRecommendedMissions() {
  router.push({
    name: 'SavingsMissions',
    query: { view: 'recommendations', from: 'home' },
  });
}

function goWallet() {
  router.push('/wallet');
}

// [ADDED] Checklist Navigation Function
function goToPreparationChecklist() {
  const tripId = travelStore.tripId || travelStore.homeDashboard?.tripId;
  if (tripId) {
    router.push(`/mypage/checklists/preparation?tripId=${tripId}`);
  } else {
    console.error('tripId를 찾을 수 없습니다.');
  }
}
// [END ADDED]

async function switchMode(mode) {
  if (mode === 'travel' && !homeDashboard.value) {
    window.alert('여행 계획을 먼저 등록해 주세요.');
    return;
  }
  const isTravelMode = mode === 'travel' ? true : false;
  const success = travelModeStore.toggleTravelMode(isTravelMode);

  if (success) {
    if (props.onSwitchMode) props.onSwitchMode(mode);
  } else {
    console.error('모드 전환 실패');
  }
}
</script>

<template>
  <section class="savings-mode-home">
    <template v-if="isHomePending">
      <div class="home-state" role="status" aria-live="polite">
        <span class="home-spinner" />
        <b>여행 저축 현황을 불러오고 있어요</b>
        <small>등록한 여행과 월렛 정보를 확인하고 있습니다.</small>
      </div>
    </template>

    <template v-else-if="travelStore.homeError">
      <div class="home-state home-error">
        <span>!</span>
        <b>홈 정보를 불러오지 못했어요</b>
        <small>{{ travelStore.homeError }}</small>
        <button type="button" @click="retryHome">다시 시도</button>
      </div>
    </template>

    <!-- ══ 여행 미등록 홈 ══════════════════════════════════════ -->
    <template v-else-if="!homeDashboard">
      <!-- 헤더: 앱 프레임의 overflow:hidden 때문에 sticky 대신 fixed로 고정 -->
      <div ref="savingsHeaderEl" class="savings-home-header">
        <div class="savings-header-row">
          <div class="mode-switch-control savings-selected">
            <span class="mode-switch-thumb" />
            <button type="button" @click="switchMode('travel')">여행</button>
            <button type="button" class="selected">저축</button>
          </div>
          <NotificationBell />
        </div>
        <h1 class="home-header-title">
          <img src="@/assets/brand/tripass-text.png" class="home-wordmark" alt="TRIPASS" />
        </h1>
      </div>
      <div :style="{ height: savingsHeaderHeight + 'px' }" aria-hidden="true" />

      <div class="empty-trip-guide mx-4 mt-3">
        <span class="guide-label">TRIPASS GUIDE</span>
        <strong>목표 설정부터 월렛 저축까지</strong>
        <p>여행 예산은 AI가 제안하고, 실제 저축은 TRIP 월렛에서 관리해요</p>
      </div>

      <div class="empty-trip-ticket-wrap mx-4 mt-3">
        <span class="empty-trip-backdrop-glow" aria-hidden="true"></span>
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
      </div>

      <HomeSavingMissionCard
        v-if="savingMissionsStore.hasStartedMissions"
        class="mx-4 mt-3"
        :mission-data="savingMissionsStore.missions"
        @open="openSavingMissions"
      />

      <section
        v-else-if="!financialSourcesLoading && !hasLinkedFinancialSources"
        class="analysis-empty-state mx-4 mt-3"
      >
        <small class="analysis-empty-label">AI SAVING MISSION</small>
        <div class="home-mission-setup-body">
          <div class="home-mission-ai-stage" aria-hidden="true">
            <span class="home-mission-ai-orbit"></span>
            <span class="home-mission-ai-spark one">✦</span>
            <span class="home-mission-ai-spark two">✦</span>
            <span class="home-mission-ai-core"><img :src="aiIcon" alt="" /></span>
          </div>
          <b>AI 추천 미션을 받아보세요!</b>
          <small>계좌나 카드를 연결하면 거래내역을 분석해 맞춤 저축 미션을 추천해 드려요.</small>
          <button type="button" @click="openFinancialSources">금융 데이터 연결하기</button>
        </div>
      </section>

      <section
        v-else-if="!financialSourcesLoading && hasLinkedFinancialSources"
        class="analysis-empty-state mx-4 mt-3"
      >
        <small class="analysis-empty-label">AI SAVING MISSION</small>
        <div class="home-mission-setup-body">
          <div class="home-mission-ai-stage" aria-hidden="true">
            <span class="home-mission-ai-orbit"></span>
            <span class="home-mission-ai-spark one">✦</span>
            <span class="home-mission-ai-spark two">✦</span>
            <span class="home-mission-ai-core"><img :src="aiIcon" alt="" /></span>
          </div>
          <b>맞춤 저축 미션을 시작해 보세요!</b>
          <small>연결된 금융 데이터를 분석해 줄이기 좋은 소비와 절약 목표를 추천해 드려요.</small>
          <button type="button" @click="openRecommendedMissions">이달의 리포트 보러가기</button>
        </div>
      </section>
    </template>

    <!-- ══ 여행 저축 모드 ══════════════════════════════════════ -->
    <template v-else>
      <!-- 헤더: 앱 프레임의 overflow:hidden 때문에 sticky 대신 fixed로 고정 -->
      <div ref="savingsHeaderEl" class="savings-home-header">
        <div class="savings-header-row">
          <div class="mode-switch-control savings-selected">
            <span class="mode-switch-thumb" />
            <button type="button" @click="switchMode('travel')">여행</button>
            <button type="button" class="selected">저축</button>
          </div>
          <NotificationBell />
        </div>
        <h1 class="home-header-title">
          <img src="@/assets/brand/tripass-text.png" class="home-wordmark" alt="TRIPASS" />
        </h1>
      </div>
      <div :style="{ height: savingsHeaderHeight + 'px' }" aria-hidden="true" />

      <!-- BOARDING PASS 카드: 좌우 스와이프로 국가 전환 -->
      <div
        ref="countryCarousel"
        class="country-carousel"
        @scroll.passive="handleCountryScroll"
      >
        <article
          v-for="country in countries"
          :key="country.id"
          class="country-slide"
          :class="{ active: selectedCountry.id === country.id }"
        >
          <div
            :key="`${country.id}-${country.id === selectedCountry.id ? countryAnimationKey : 0}`"
            class="country-ticket overflow-hidden"
            :style="`background:${country.headerBg}`"
          >
            <!-- ① 기존 탑승권 헤더 -->
            <div
              class="px-5 pt-4 pb-3 flex items-center justify-between"
              :style="`background:${country.headerBg}`"
            >
              <span
                class="text-[10px] font-bold tracking-widest"
                style="color: #ffd466"
                >TRIPASS AIR</span
              >
              <span class="text-white/70 text-[10px] font-bold tracking-widest"
                >BOARDING PASS</span
              >
              <RouterLink
                class="text-white text-[10px] font-semibold whitespace-nowrap"
                :to="{ name: 'TravelRegister', query: { mode: 'edit' } }"
              >
                여행 계획 수정하기 ›
              </RouterLink>
            </div>

            <!-- 헤더와 사진 섹션 사이 절취선(탑승권 펀칭 구멍) -->
            <div class="ticket-cutline ticket-cutline-top">
              <div class="ticket-notch ticket-notch-left" />
              <div class="ticket-dashed-line" />
              <div class="ticket-notch ticket-notch-right" />
            </div>

            <!-- ② 사진 전체 배경 섹션 (나머지 전부) -->
            <div
              class="relative"
              :style="
                country.image
                  ? `background:url(${country.image}) center/cover no-repeat`
                  : `background:linear-gradient(145deg,${country.headerBg},#3568bd)`
              "
            >
              <!-- 어두운 오버레이 -->
              <div
                class="absolute inset-0 bg-black/30 pointer-events-none z-0"
              />

              <div class="relative z-10 px-5 pt-6">
                <!-- DESTINATION / DEPARTURE / 설명 -->
                <div class="flex items-center gap-2">
                  <div class="flex-none">
                    <p
                      class="text-white/65 text-[10px] uppercase tracking-widest mb-1"
                    >
                      Destination
                    </p>
                    <p
                      class="text-white text-[22px] font-extrabold leading-none flex items-center gap-2"
                    >
                      <span :class="flagIconClass(country.code)" class="fi-inline" style="font-size: 17px" />
                      {{ country.name }}
                    </p>
                  </div>
                  <div class="flex-1 mt-3.5 destination-route" aria-hidden="true">
                    <i></i>
                    <span>✈</span>
                  </div>
                  <div class="text-right flex-none">
                    <p
                      class="text-white/65 text-[10px] uppercase tracking-widest mb-1"
                    >
                      Departure
                    </p>
                    <p
                      class="text-[22px] font-extrabold leading-none"
                      style="color: #ffd466"
                    >
                      D-{{ daysUntilDeparture }}
                    </p>
                  </div>
                </div>
                <p class="ticket-description text-white/90 text-[12px] mt-3">
                  {{ country.desc }}
                </p>

                <!-- 사진이 보이는 여백 및 체크리스트 버튼 -->
                <div class="ticket-photo-space">
                  <button
                    v-if="checklistInfo"
                    class="checklist-btn"
                    @click="goToPreparationChecklist"
                  >
                    {{ checklistInfo.label }} ›
                  </button>
                </div>

                <!-- 진행 박스 (반투명, 사진 위에 떠있음) -->
                <div
                  class="rounded-xl px-4 py-4"
                  :style="`background:${country.progressBg}`"
                >
                  <div class="flex justify-between mb-2">
                    <span class="font-semibold text-[13px] text-white">{{
                      ticketSavingCopy.title
                    }}</span>
                    <span class="font-extrabold text-[14px]" style="color: #ffd466"
                      >{{ homeSavingsPercent }}%</span
                    >
                  </div>
                  <div class="h-2 rounded-full bg-white/25 overflow-hidden">
                    <div
                      class="h-full rounded-full transition-all"
                      :style="`width:${homeSavingsPercent}%;background:${country.barColor}`"
                    />
                  </div>
                  <div class="flex justify-between mt-2.5">
                    <div>
                      <p class="text-white text-[14px] font-bold">
                        {{ formatCurrency(homeSavedAmount) }}
                      </p>
                      <p class="text-white/65 text-[9px] tracking-wider mt-1">
                        {{ ticketSavingCopy.amountLabel || 'SAVED' }}
                      </p>
                    </div>
                    <div class="text-right">
                      <p class="text-white text-[14px] font-bold">
                        {{ formatCurrency(homeGoalAmount) }}
                      </p>
                      <p class="text-white/60 text-[9px] tracking-wider mt-1">
                        GOAL
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 사진의 종료 경계와 정확히 맞닿는 하단 절취선 -->
              <div class="ticket-cutline ticket-cutline-bottom">
                <div class="ticket-notch ticket-notch-left" />
                <div class="ticket-dashed-line" />
                <div class="ticket-notch ticket-notch-right" />
              </div>

              <!-- ④ 국가 컬러 스텁 -->
              <div
                class="relative z-10"
                :style="`background:${country.headerBg}`"
              >
                <button
                  class="ticket-stub w-full px-5 flex items-center justify-start gap-1 active:bg-gray-50"
                  @click="goWallet"
                >
                  <span class="text-[13px] font-bold text-white">송금하기</span>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path
                      d="M9 18L15 12L9 6"
                      stroke="#FFFFFF"
                      stroke-width="2.5"
                      stroke-linecap="round"
                    />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </article>
      </div>
      <div v-if="countries.length > 1" class="country-carousel-meta">
        <span>옆으로 넘겨 방문 국가를 확인하세요</span>
        <div class="country-carousel-dots" aria-hidden="true">
          <i
            v-for="country in countries"
            :key="country.id"
            :class="{ active: selectedCountry.id === country.id }"
          />
        </div>
      </div>

      <section class="month-saving-card mx-4 mt-3">
        <div class="month-saving-heading">
          <h2>{{ currentMonthLabel }} 여행 저축</h2>
        </div>
        <div class="month-saving-values">
          <div>
            <small>이번 달 목표</small><b>{{ formatCurrency(monthlyTarget) }}</b>
          </div>
          <div>
            <small>저축한 금액</small
            ><b>{{ formatCurrency(monthlySavedAmount) }}</b>
          </div>
          <div>
            <small>남은 저축</small
            ><b>{{ formatCurrency(monthlyRemainingAmount) }}</b>
          </div>
        </div>
        <div class="month-progress-label">
          <strong>{{ monthlySavingPercent }}%</strong>
        </div>
        <div class="month-saving-progress">
          <i :style="{ width: `${monthlySavingPercent}%` }" />
        </div>
        <div v-if="monthlySavingPercent >= 100" class="month-saving-success">
          <span>✓</span>
          <div>
            <b>이번 달 목표 달성!</b
            ><small
              >{{ formatCurrency(monthlySavedAmount) }}을 저축했어요.</small
            >
          </div>
        </div>
        <button class="month-wallet-button" @click="goWallet">
          <span><i>＋</i> 여행 월렛에 저축하기</span><b>출발 준비하기 ›</b>
        </button>
      </section>

      <section
        v-if="homeInsightLoading"
        class="analysis-summary-skeleton mx-4 mt-3"
        aria-label="월간 분석 및 미션 정보를 불러오는 중"
      >
        <i /><i /><i /><i />
      </section>

      <section
        v-else-if="financialSourcesError"
        class="analysis-load-error mx-4 mt-3"
      >
        <span>AI</span>
        <div>
          <b>금융 데이터 연결 상태를 확인하지 못했어요</b>
          <small>{{ financialSourcesError }}</small>
        </div>
        <button type="button" @click="retryMonthlyAnalysis">다시 시도</button>
      </section>

      <HomeSavingMissionCard
        v-else-if="savingMissionsStore.hasStartedMissions"
        class="mx-4 mt-3"
        :mission-data="savingMissionsStore.missions"
        @open="openSavingMissions"
      />

      <section
        v-else-if="!financialSourcesLoading && !hasLinkedFinancialSources"
        class="analysis-empty-state mx-4 mt-3"
      >
        <small class="analysis-empty-label">AI SAVING MISSION</small>
        <div class="home-mission-setup-body">
          <div class="home-mission-ai-stage" aria-hidden="true">
            <span class="home-mission-ai-orbit"></span>
            <span class="home-mission-ai-spark one">✦</span>
            <span class="home-mission-ai-spark two">✦</span>
            <span class="home-mission-ai-core"><img :src="aiIcon" alt="" /></span>
          </div>
          <b>AI 추천 미션을 받아보세요!</b>
          <small>계좌나 카드를 연결하면 거래내역을 분석해 맞춤 저축 미션을 추천해 드려요.</small>
          <button type="button" @click="openFinancialSources">금융 데이터 연결하기</button>
        </div>
      </section>

      <section
        v-else-if="!financialSourcesLoading && hasLinkedFinancialSources"
        class="analysis-empty-state mx-4 mt-3"
      >
        <small class="analysis-empty-label">AI SAVING MISSION</small>
        <div class="home-mission-setup-body">
          <div class="home-mission-ai-stage" aria-hidden="true">
            <span class="home-mission-ai-orbit"></span>
            <span class="home-mission-ai-spark one">✦</span>
            <span class="home-mission-ai-spark two">✦</span>
            <span class="home-mission-ai-core"><img :src="aiIcon" alt="" /></span>
          </div>
          <b>맞춤 저축 미션을 시작해 보세요!</b>
          <small>연결된 금융 데이터를 분석해 줄이기 좋은 소비와 절약 목표를 추천해 드려요.</small>
          <button type="button" @click="openRecommendedMissions">이달의 리포트 보러가기</button>
        </div>
      </section>

      <MonthlyAnalysisSummaryCard
        v-else-if="monthlyAnalysisStore.hasVisibleReport"
        class="mx-4 mt-3"
        :report="monthlyAnalysisStore.report"
        @open="openMonthlyAnalysis"
      />

      <section
        v-else-if="monthlyAnalysisStore.errorMessage"
        class="analysis-load-error mx-4 mt-3"
      >
        <span>AI</span>
        <div>
          <b>월간 분석 리포트를 불러오지 못했어요</b>
          <small>{{ monthlyAnalysisStore.errorMessage }}</small>
        </div>
        <button type="button" @click="retryMonthlyAnalysis">다시 시도</button>
      </section>

      <section v-else class="analysis-empty-state mx-4 mt-3">
        <small class="analysis-empty-label">AI SAVING MISSION</small>
        <button type="button" @click="openFinancialSources">
          <span class="analysis-empty-plus" aria-hidden="true">＋</span>
          <b>아직 분석할 지난달 거래가 없어요</b>
          <small>분류된 계좌·카드 지출이 쌓이면 소비 분석과 맞춤 미션을 보여드려요.</small>
          <em>연동 자산 확인하기<i aria-hidden="true">›</i></em>
        </button>
      </section>

      <!-- 오늘의 실시간 환율 -->
      <div class="exchange-live-card mx-4 mt-3 mb-4" :style="exchangeCardStyle">
        <div class="exchange-card-head">
          <div><i /><span>LIVE EXCHANGE</span></div>
          <time>{{ exchangeStore.lastUpdateDate || '최신 고시 기준' }}</time>
        </div>
        <div class="exchange-card-body">
          <div class="exchange-country-mark">
            <i :class="flagIconClass(selectedCountry.code)" class="exchange-flag-icon" aria-hidden="true"></i>
            <div>
              <small>{{ selectedCountry.name }} 여행 환율</small
              ><b>{{ exchangeUnitLabel }} <i>→</i> KRW</b>
            </div>
          </div>
          <div v-if="selectedExchangeRate" class="exchange-rate-value">
            <b>{{ formatRate(selectedExchangeRate.rate) }}<small>원</small></b>
            <span
              :class="
                exchangeChangePercent > 0
                  ? 'up'
                  : exchangeChangePercent < 0
                    ? 'down'
                    : ''
              "
            >
              전일 대비 {{ exchangeChangePercent > 0 ? '+' : ''
              }}{{ exchangeChangePercent.toFixed(2) }}%
              {{
                exchangeChangePercent > 0
                  ? '↑'
                  : exchangeChangePercent < 0
                    ? '↓'
                    : '-'
              }}
            </span>
          </div>
          <span v-else class="exchange-rate-loading"
            >환율 정보를 불러오는 중</span
          >
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.empty-trip-ticket-wrap {
  position: relative;
}
.empty-trip-backdrop-glow {
  position: absolute;
  top: -22px;
  left: 6%;
  right: 6%;
  height: 96px;
  border-radius: 50%;
  background: radial-gradient(ellipse at center, rgba(23, 77, 167, .38) 0%, rgba(23, 77, 167, 0) 72%);
  filter: blur(4px);
  pointer-events: none;
}
.empty-trip-ticket {
  position: relative;
  margin-top: 12px;
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
  animation: empty-plane-beacon 2.4s ease-out infinite;
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
@keyframes empty-plane-beacon {
  0% { box-shadow: 0 0 0 0 rgba(255, 212, 102, .3); }
  70%, 100% { box-shadow: 0 0 0 12px rgba(255, 212, 102, 0); }
}
@media (prefers-reduced-motion: reduce) {
  .empty-trip-badge,
  .empty-map-pin { animation: none; }
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
.savings-mode-home {
  min-height: 100vh;
  background: #f4f5f9;
}
.analysis-summary-skeleton,
.analysis-load-error,
.analysis-empty-state {
  border: 1px solid #d6e3fa;
  border-radius: 22px;
  background: #f8fbff;
}
.analysis-summary-skeleton {
  display: grid;
  gap: 10px;
  padding: 19px;
}
.analysis-summary-skeleton i {
  display: block;
  height: 13px;
  overflow: hidden;
  border-radius: 99px;
  background: linear-gradient(90deg, #e7eef9 25%, #f5f8fd 50%, #e7eef9 75%);
  background-size: 200% 100%;
  animation: analysis-skeleton 1.25s infinite linear;
}
.analysis-summary-skeleton i:nth-child(1) {
  width: 42%;
}
.analysis-summary-skeleton i:nth-child(2) {
  width: 70%;
  height: 40px;
}
.analysis-summary-skeleton i:nth-child(3),
.analysis-summary-skeleton i:nth-child(4) {
  height: 35px;
}
.analysis-load-error,
.analysis-empty-state {
  padding: 14px;
}
.analysis-load-error {
  display: flex;
  align-items: center;
  gap: 10px;
}
.analysis-load-error > span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  background: #e7efff;
  color: #286ce0;
  font-size: 11px;
  font-weight: 950;
}
.analysis-load-error div {
  min-width: 0;
  flex: 1;
}
.analysis-load-error b,
.analysis-load-error small {
  display: block;
}
.analysis-load-error b {
  color: #26334d;
  font-size: 11px;
}
.analysis-load-error small {
  overflow: hidden;
  margin-top: 3px;
  color: #7b8da9;
  font-size: 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.analysis-load-error button {
  flex: none;
  color: #286ce0;
  font-size: 9px;
  font-weight: 900;
  white-space: nowrap;
}
.analysis-empty-state {
  position: relative;
  padding: 18px;
  background: #fff;
  box-shadow: 0 10px 24px rgb(36 80 153 / 7%);
}
.analysis-empty-label {
  display: block;
  margin-bottom: 10px;
  color: #286ce0;
  font-size: 9px;
  font-weight: 950;
  letter-spacing: 0.12em;
}
.analysis-empty-state > button {
  position: relative;
  display: flex;
  width: 100%;
  min-height: 176px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 18px 16px;
  border: 1px dashed #c9d8ef;
  border-radius: 17px;
  background: #f6f8fc;
  text-align: center;
}
.home-mission-setup-body{display:flex;min-height:210px;padding:22px 18px 18px;flex-direction:column;align-items:center;justify-content:center;border:1px dashed #c9d8ef;border-radius:17px;background:linear-gradient(180deg,#f7faff,#f3f7fd);text-align:center}.home-mission-flight{position:relative;width:126px;height:45px;margin-bottom:13px}.home-mission-flight-route{position:absolute;top:22px;left:8px;right:8px;border-top:2px dashed #b9ccef}.home-mission-flight-start,.home-mission-flight-end{position:absolute;top:18px;width:10px;height:10px;border:2px solid #8eafe5;border-radius:50%;background:#f6f9ff}.home-mission-flight-start{left:2px}.home-mission-flight-end{right:2px}.home-mission-flight-end::after{position:absolute;inset:-6px;border:1px solid rgb(40 108 224 / 28%);border-radius:50%;content:'';animation:home-mission-destination-pulse 1.9s ease-out infinite}.home-mission-flight img{position:absolute;z-index:2;top:10px;left:7px;width:25px;height:25px;filter:drop-shadow(0 5px 5px rgb(40 108 224 / 22%));animation:home-mission-plane-travel 2.8s ease-in-out infinite}.home-mission-setup-body b{color:#26334d;font-size:14px;font-weight:900}.home-mission-setup-body>small{max-width:290px;margin-top:7px;color:#8190a9;font-size:10px;line-height:1.55;word-break:keep-all}.home-mission-setup-body>button{margin-top:17px;padding:11px 22px;border-radius:12px;background:#245ec4;color:#fff;font-size:12px;font-weight:900}
.home-mission-ai-stage{position:relative;width:82px;height:72px;margin-bottom:10px}.home-mission-ai-core{position:absolute;top:8px;left:13px;z-index:2;display:grid;width:56px;height:56px;place-items:center;border-radius:20px;background:linear-gradient(145deg,#dbe8ff,#fff);box-shadow:0 10px 24px rgb(40 108 224 / 20%);animation:home-ai-float 2.6s ease-in-out infinite}.home-mission-ai-core img{width:30px;height:30px;filter:invert(34%) sepia(94%) saturate(1272%) hue-rotate(199deg) brightness(91%)}.home-mission-ai-orbit{position:absolute;inset:0;border:1.5px dashed #9fb9e8;border-radius:50%;animation:home-ai-orbit 7s linear infinite}.home-mission-ai-spark{position:absolute;z-index:3;color:#4a82df;font-size:13px;animation:home-ai-spark 1.8s ease-in-out infinite}.home-mission-ai-spark.one{top:0;right:2px}.home-mission-ai-spark.two{bottom:2px;left:0;animation-delay:.8s}
.analysis-empty-plus {
  display: grid;
  width: 48px;
  height: 48px;
  margin-bottom: 13px;
  place-items: center;
  border-radius: 50%;
  background: #e4edff;
  color: #286ce0;
  font-size: 28px;
  font-weight: 400;
}
.analysis-empty-state b {
  color: #26334d;
  font-size: 14px;
}
.analysis-empty-state button > small {
  max-width: 290px;
  margin-top: 7px;
  color: #8190a9;
  font-size: 10px;
  line-height: 1.55;
  word-break: keep-all;
}
.analysis-empty-state em {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 16px;
  color: #286ce0;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
}
.analysis-empty-state em i {
  font-size: 17px;
  font-style: normal;
  line-height: 1;
}
@keyframes analysis-skeleton {
  to {
    background-position: -200% 0;
  }
}
@keyframes home-mission-plane-travel{0%{opacity:.35;transform:translate(0,4px) rotate(-8deg)}18%{opacity:1}50%{transform:translate(47px,-5px) rotate(2deg)}82%{opacity:1}100%{opacity:.35;transform:translate(94px,2px) rotate(9deg)}}
@keyframes home-mission-destination-pulse{0%{opacity:.8;transform:scale(.55)}100%{opacity:0;transform:scale(1.45)}}
@keyframes home-ai-float{0%,100%{transform:translateY(0) rotate(-2deg)}50%{transform:translateY(-5px) rotate(2deg)}}
@keyframes home-ai-orbit{to{transform:rotate(360deg)}}
@keyframes home-ai-spark{0%,100%{opacity:.2;transform:scale(.65) rotate(0)}50%{opacity:1;transform:scale(1.2) rotate(90deg)}}
@media (prefers-reduced-motion: reduce) {
  .home-mission-flight img,
  .home-mission-flight-end::after { animation: none; }
  .home-mission-flight img { left: 50%; transform: translateX(-50%); }
  .home-mission-ai-core,
  .home-mission-ai-orbit,
  .home-mission-ai-spark { animation: none; }
}
.home-state {
  display: flex;
  min-height: 72vh;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px;
  color: #173f8d;
  text-align: center;
}
.home-state > b {
  margin-top: 14px;
  font-size: 15px;
}
.home-state > small {
  margin-top: 6px;
  color: #8190a8;
  font-size: 10px;
}
.home-state > button {
  margin-top: 16px;
  padding: 10px 18px;
  border-radius: 11px;
  background: #173f8d;
  color: #fff;
  font-size: 11px;
  font-weight: 900;
}
.home-state.home-error > span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  background: #fff0f0;
  color: #e5484d;
  font-size: 20px;
  font-weight: 900;
}
.home-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #dce8fb;
  border-top-color: #2469e8;
  border-radius: 50%;
  animation: home-spin 0.7s linear infinite;
}
.country-ticket {
  border-radius: 18px;
  box-shadow: 0 10px 24px rgba(22, 39, 78, 0.15);
}
.ticket-photo-space {
  height: 104px;
}
.ticket-description {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ticket-stub {
  height: 45px;
}
.ticket-cutline {
  position: relative;
  z-index: 20;
  display: flex;
  align-items: center;
  height: 0;
}
.ticket-cutline-bottom {
  margin-top: 14px;
}
.ticket-cutline-top {
  margin-top: 0;
}
.ticket-notch {
  position: absolute;
  top: 50%;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f4f5f9;
  transform: translateY(-50%);
}
.ticket-notch-left {
  left: -12px;
}
.ticket-notch-right {
  right: -12px;
}
.ticket-dashed-line {
  width: calc(100% - 34px);
  margin: 0 auto;
  border-top: 1.5px dashed rgba(255, 255, 255, 0.42);
}
.mode-switch-control {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 84px;
  padding: 2px;
  overflow: hidden;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(16, 25, 43, 0.06);
}
.mode-switch-control button {
  position: relative;
  z-index: 2;
  height: 25px;
  border-radius: 999px;
  color: #6b7688;
  font-size: 10px;
  font-weight: 900;
  transition: color 0.25s ease;
}
.mode-switch-control button.selected {
  color: #fff;
}
.mode-switch-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: calc(50% - 2px);
  height: 25px;
  border-radius: 999px;
  background: #173f8d;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.mode-switch-control.savings-selected .mode-switch-thumb {
  transform: translateX(100%);
}
.savings-home-header {
  padding: 42px 20px 14px;
  background: #f3f6ff;
}
.savings-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.savings-greeting-row {
  margin-top: 10px;
}
.savings-greeting-row h1 {
  color: #111827;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.04em;
}
.savings-action-row {
  margin-top: 9px;
}
.savings-country-button {
  display: flex;
  min-width: 78px;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border: 1px solid #d8dee8;
  border-radius: 10px;
  background: #fff;
  color: #273449;
  font-size: 11px;
  font-weight: 800;
  box-shadow: 0 2px 7px rgba(27, 43, 75, 0.05);
}
.travel-edit-link {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 11px;
  border-radius: 10px;
  background: #fff0e8;
  color: #e45f24;
  font-size: 11px;
  font-weight: 900;
}
.travel-edit-link span {
  font-size: 16px;
  line-height: 10px;
}
.month-saving-card {
  padding: 16px;
  border: 1.5px solid #2bb69f;
  border-radius: 19px;
  background: #fff;
  box-shadow: 0 7px 16px rgba(19, 59, 123, 0.06);
}
.month-saving-heading {
  display: block;
}
.month-saving-title-row {
  display: flex;
  align-items: center;
}
.month-saving-heading p {
  display: inline-block;
  padding: 5px 11px;
  border: 1px solid #20aa94;
  border-radius: 99px;
  color: #089b83;
  font-size: 10px;
  font-weight: 900;
}
.month-saving-heading h2 {
  margin-top: 6px;
  color: #079982;
  font-size: 19px;
  font-weight: 900;
}
.month-saving-heading > span {
  padding: 5px 9px;
  border-radius: 99px;
  background: #e2f8f1;
  color: #08a084;
  font-size: 10px;
  font-weight: 900;
}
.month-saving-values {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 16px;
}
.month-saving-values > div {
  padding: 0 8px;
}
.month-saving-values > div:first-child {
  padding-left: 0;
}
.month-saving-values > div + div {
  border-left: 1px solid #5bcab9;
}
.month-saving-values small {
  display: block;
  color: #079982;
  font-size: 9px;
  font-weight: 700;
}
.month-saving-values b {
  display: block;
  margin-top: 5px;
  color: #079982;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: -0.04em;
}
.month-saving-progress {
  position: relative;
  height: 8px;
  margin-top: 19px;
  border-radius: 99px;
  background: #dff3ef;
}
.month-saving-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #25b59d;
}
.month-saving-progress strong {
  position: absolute;
  right: 0;
  top: -18px;
  color: #08a084;
  font-size: 10px;
}
.month-saving-success {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 15px;
  padding: 10px;
  border-radius: 13px;
  background: #e2f8f1;
}
.month-saving-success > span {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 10px;
  background: #c8f0e6;
  color: #00a489;
  font-size: 17px;
  font-weight: 900;
}
.month-saving-success b,
.month-saving-success small {
  display: block;
}
.month-saving-success b {
  color: #048c77;
  font-size: 11px;
}
.month-saving-success small {
  margin-top: 2px;
  color: #229d89;
  font-size: 9px;
}
.month-wallet-button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  margin-top: 13px;
  padding: 11px 13px;
  border: 1px solid #9adfd1;
  border-radius: 12px;
  color: #049781;
  font-size: 11px;
  font-weight: 900;
}
.month-wallet-button span {
  font-size: 17px;
}
.ai-report-card {
  padding: 17px;
  border: 1px solid #bfd7ff;
  border-radius: 20px;
  background: #eaf3ff;
  box-shadow: 0 8px 18px rgba(27, 64, 129, 0.05);
  cursor: pointer;
}
.ai-report-heading {
  display: flex;
  justify-content: space-between;
  align-items: start;
}
.ai-report-heading p {
  color: #286ce0;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: -0.05em;
}
.ai-report-heading small {
  display: block;
  margin-top: 5px;
  color: #6e88b2;
  font-size: 10px;
}
.ai-report-heading button {
  color: #286ce0;
  font-size: 10px;
  font-weight: 800;
}
.ai-goal-status {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 13px;
  padding: 11px;
  border-radius: 13px;
  background: #fff;
}
.status-icon {
  display: grid;
  width: 31px;
  height: 31px;
  place-items: center;
  border-radius: 11px;
  background: #e1f8f2;
  color: #03a084;
  font-size: 18px;
}
.ai-goal-status strong {
  color: #109482;
  font-size: 12px;
}
.ai-goal-status p {
  margin-top: 3px;
  color: #6681a6;
  font-size: 9px;
}
.ai-top-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}
.ai-top-title span {
  color: #3b68aa;
  font-size: 10px;
  font-weight: 900;
}
.ai-top-title small {
  color: #8aa0bf;
  font-size: 8px;
}
.ai-coaching-row {
  display: grid;
  grid-template-columns: 22px 34px 1fr auto;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  padding: 9px;
  border-radius: 11px;
  background: #fff;
}
.ai-coaching-row > b {
  display: grid;
  width: 19px;
  height: 19px;
  place-items: center;
  border-radius: 50%;
  background: #dce9ff;
  color: #2c68d7;
  font-size: 9px;
}
.ai-coaching-row span {
  font-size: 11px;
  font-weight: 900;
}
.ai-coaching-row small {
  color: #7186a8;
  font-size: 8px;
}
.ai-coaching-row strong {
  padding: 5px 7px;
  border: 1px solid #ffc6c6;
  border-radius: 99px;
  color: #ef5050;
  font-size: 8px;
}
.ai-saving-total {
  margin-top: 12px;
  padding: 13px;
  border-radius: 13px;
  background: #e2f8f1;
}
.ai-saving-total small {
  display: block;
  color: #179980;
  font-size: 9px;
  font-weight: 800;
}
.ai-saving-total b {
  display: block;
  margin-top: 5px;
  color: #173b75;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: -0.05em;
}
.monthly-target-guide {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 14px;
  padding: 10px 11px;
  border-radius: 12px;
  background: #eefaf7;
  color: #078b76;
}
.monthly-target-guide span {
  font-size: 9px;
  font-weight: 800;
}
.monthly-target-guide b {
  margin-left: auto;
  font-size: 13px;
}
.monthly-target-guide small {
  color: #5a9f93;
  font-size: 8px;
}
.trip-summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 9px;
  padding: 10px 11px;
  border-radius: 11px;
  background: #fff;
}
.trip-summary-row span {
  flex: none;
  color: #6e88b2;
  font-size: 9px;
}
.trip-summary-row b {
  overflow: hidden;
  color: #244f91;
  font-size: 10px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.savings-home-header {
  animation: home-fade-down 0.45s ease both;
}
.savings-greeting-row p {
  margin-top: 4px;
  color: #e45f24;
  font-size: 11px;
  font-weight: 800;
}
.country-ticket .rounded-xl {
  animation: home-fade-up 0.45s 0.25s ease both;
}
.country-carousel {
  animation: home-card-reveal 0.56s 0.1s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.country-carousel-meta {
  animation: home-fade-up 0.42s 0.2s ease both;
}
.month-saving-card {
  animation: home-card-reveal 0.56s 0.24s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.ai-report-card {
  cursor: default;
  animation: home-card-reveal 0.56s 0.38s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.ai-report-heading a {
  color: #286ce0;
  font-size: 10px;
  font-weight: 800;
}
.exchange-live-card {
  animation: home-card-reveal 0.56s 0.52s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.month-saving-progress i {
  position: relative;
  overflow: hidden;
  transition: width 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}
.month-saving-progress i::after {
  position: absolute;
  inset: 0;
  content: '';
  background: linear-gradient(90deg, transparent, #ffffff99, transparent);
  transform: translateX(-100%);
  animation: progress-shine 1.8s 0.5s ease-in-out infinite;
}
.travel-edit-link {
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.travel-edit-link:active {
  transform: scale(0.96);
}
.savings-home-header {
  position: relative;
  z-index: 60;
  padding: 42px 20px 18px;
  background:
    radial-gradient(circle at 100% 0, #e7efff 0, transparent 42%), #f3f6ff;
}
.savings-greeting-row {
  margin-top: 13px;
}
.savings-greeting-row h1 {
  font-size: 21px;
  line-height: 1.25;
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
}
.country-carousel::-webkit-scrollbar {
  display: none;
}
.country-slide {
  flex: 0 0 100%;
  min-width: 0;
  opacity: 0.56;
  transform: translateY(5px) scale(0.965);
  transition:
    opacity 0.34s ease,
    transform 0.42s cubic-bezier(0.22, 1, 0.36, 1);
  scroll-snap-align: center;
  scroll-snap-stop: always;
}
.country-slide.active {
  opacity: 1;
  transform: translateY(0) scale(1);
}
.country-ticket {
  position: relative;
  z-index: 1;
  margin: 0;
  border-radius: 21px;
  box-shadow: 0 16px 32px rgba(17, 35, 70, 0.19);
}
.country-slide.active .country-ticket {
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
  transition:
    width 0.22s ease,
    background 0.22s ease;
}
.country-carousel-dots i.active {
  width: 17px;
  background: #2469e8;
}
.ticket-photo-space {
  height: 112px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.checklist-btn {
  padding: 8px 16px;
  border-radius: 12px;
  background: #ffb800;
  color: #173f8d;
  font-size: 11px;
  font-weight: 900;
  box-shadow: 0 4px 12px rgba(255, 184, 0, 0.3);
}
.checklist-btn:active {
  transform: scale(0.96);
}
.ticket-stub {
  height: 49px;
}
.month-saving-card {
  position: relative;
  margin-top: 16px;
  overflow: hidden;
  padding: 21px 18px 18px;
  border: 1px solid #d4e0f5;
  border-radius: 23px;
  background: linear-gradient(145deg, #fffaf1 0%, #f8fbff 48%, #edf4ff 100%);
  box-shadow: 0 13px 30px rgba(23, 63, 141, 0.11);
}
.month-saving-card::before {
  position: absolute;
  top: -43px;
  right: -38px;
  width: 128px;
  height: 128px;
  border: 22px solid rgba(240, 120, 60, 0.08);
  border-radius: 50%;
  content: '';
}
.destination-route {
  position: relative;
  height: 20px;
}
.destination-route i {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  border-top: 1px dashed rgba(255, 255, 255, 0.4);
}
.destination-route span {
  position: absolute;
  top: 50%;
  left: 0;
  color: #fde047;
  font-size: 18px;
  transform: translate(-50%, -50%);
  animation: plane-travel 3.4s ease-in-out infinite;
}
@keyframes plane-travel {
  0% {
    left: 0%;
    opacity: 0;
    transform: translate(-50%, -50%) rotate(-4deg);
  }
  12% {
    opacity: 1;
  }
  50% {
    transform: translate(-50%, -50%) rotate(2deg);
  }
  88% {
    opacity: 1;
  }
  100% {
    left: 100%;
    opacity: 0;
    transform: translate(-50%, -50%) rotate(-4deg);
  }
}
.month-saving-heading {
  position: relative;
  z-index: 1;
}
.month-saving-heading small {
  display: block;
  color: #ef7b3c;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.15em;
}
.month-saving-heading h2 {
  margin-top: 5px;
  color: #173f8d;
  font-size: 21px;
  font-weight: 950;
  letter-spacing: -0.045em;
}
.month-saving-heading p {
  padding: 7px 11px;
  border: 0;
  background: #173f8d;
  color: #fff;
  font-size: 10px;
  box-shadow: 0 5px 12px rgba(23, 63, 141, 0.18);
}
.month-saving-values {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 7px;
  margin-top: 18px;
}
.month-saving-values > div {
  display: flex;
  min-width: 0;
  align-items: center;
  flex-direction: column;
  padding: 11px 7px 10px;
  border: 1px solid rgba(198, 213, 238, 0.8);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.82);
  text-align: center;
  box-shadow: 0 4px 12px rgba(23, 63, 141, 0.05);
}
.month-saving-values > div + div {
  border-left: 1px solid rgba(198, 213, 238, 0.8);
}
.month-saving-values small {
  color: #7183a3;
  font-size: 9px;
  font-weight: 750;
}
.month-saving-values b {
  overflow: hidden;
  margin-top: 5px;
  color: #183b76;
  font-size: 13px;
  font-weight: 950;
  letter-spacing: -0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.month-progress-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
  color: #466189;
  font-size: 10px;
  font-weight: 800;
}
.month-progress-label strong {
  color: #2469e8;
  font-size: 12px;
}
.month-saving-progress {
  height: 9px;
  margin-top: 7px;
  overflow: visible;
  border: 1px solid #d8e3f5;
  background: #e7edf7;
}
.month-saving-progress i {
  position: relative;
  min-width: 5px;
  overflow: visible;
  background: linear-gradient(90deg, #2469e8, #56a3ff);
}
.month-saving-progress i::before {
  position: absolute;
  top: 50%;
  right: -5px;
  width: 11px;
  height: 11px;
  border: 3px solid #fff;
  border-radius: 50%;
  background: #ef7b3c;
  box-shadow: 0 2px 6px rgba(29, 71, 143, 0.25);
  content: '';
  transform: translateY(-50%);
}
.month-saving-success {
  background: #e8f7f2;
}
.month-wallet-button {
  position: relative;
  z-index: 1;
  margin-top: 17px;
  padding: 13px 14px;
  border: 0;
  border-radius: 13px;
  background: linear-gradient(100deg, #173f8d, #2969d2);
  color: #fff;
  box-shadow: 0 8px 18px rgba(23, 63, 141, 0.2);
}
.month-wallet-button > span {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}
.month-wallet-button > span i {
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 14px;
  font-style: normal;
}
.month-wallet-button > b {
  color: #dbe8ff;
  font-size: 9px;
  font-weight: 750;
}
.month-wallet-button:active {
  transform: scale(0.985);
}
.ai-report-card {
  margin-top: 16px;
  padding: 20px 18px;
  border-radius: 22px;
  background: linear-gradient(145deg, #eaf3ff, #f5f8ff);
  box-shadow: 0 10px 25px rgba(36, 105, 232, 0.1);
}
.ai-report-heading p {
  font-size: 21px;
}
.ai-report-heading small {
  font-size: 11px;
}
.ai-report-heading a {
  font-size: 12px;
}
.ai-goal-status {
  gap: 12px;
  margin-top: 16px;
  padding: 14px;
}
.status-icon {
  width: 38px;
  height: 38px;
  font-size: 20px;
}
.ai-goal-status strong {
  font-size: 14px;
}
.ai-goal-status p {
  margin-top: 5px;
  color: #526b93;
  font-size: 11px;
  font-weight: 650;
}
.trip-summary-row {
  margin-top: 10px;
  padding: 13px;
}
.trip-summary-row span {
  font-size: 11px;
  font-weight: 700;
}
.trip-summary-row b {
  font-size: 12px;
}
.exchange-live-card {
  position: relative;
  margin-top: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(10, 20, 45, .32), rgba(10, 20, 45, .58)),
    var(--exchange-photo) center/cover no-repeat,
    linear-gradient(
      135deg,
      var(--exchange-primary),
      color-mix(in srgb, var(--exchange-primary) 76%, #2f72df)
    );
  color: #fff;
  box-shadow: 0 14px 30px
    color-mix(in srgb, var(--exchange-primary) 25%, transparent);
}
.exchange-live-card::before {
  position: absolute;
  top: -58px;
  right: -38px;
  width: 155px;
  height: 155px;
  border-radius: 50%;
  background: var(--exchange-glow);
  content: '';
}
.exchange-live-card::after {
  position: absolute;
  right: 38px;
  bottom: -58px;
  width: 120px;
  height: 120px;
  border: 20px solid rgba(255, 255, 255, 0.045);
  border-radius: 50%;
  content: '';
}
.exchange-card-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 17px 12px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.24);
}
.exchange-card-head > div {
  display: flex;
  align-items: center;
  gap: 6px;
}
.exchange-card-head i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4cd97b;
  box-shadow: 0 0 0 4px rgba(76, 217, 123, 0.18);
  animation: live-pulse 1.8s ease-in-out infinite;
}
@keyframes live-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 4px rgba(76, 217, 123, 0.18);
    opacity: 1;
  }
  50% {
    box-shadow: 0 0 0 7px rgba(76, 217, 123, 0.06);
    opacity: 0.6;
  }
}
.exchange-card-head span {
  color: rgba(255, 255, 255, 0.88);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.14em;
}
.exchange-card-head time {
  color: rgba(255, 255, 255, 0.72);
  font-size: 11px;
  font-weight: 650;
}
.exchange-card-body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 19px 17px;
}
.exchange-country-mark {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}
/* 국기가 다른 .exchange-country-mark > span 규칙들과 섞이지 않도록 완전히 별도 클래스로 관리한다. */
.exchange-country-mark .exchange-flag-icon {
  display: block;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 10px;
  background-color: rgba(255, 255, 255, 0.12);
  background-size: cover;
  background-position: 50%;
}
.exchange-country-mark > span {
  display: grid;
  flex: 0 0 42px;
  height: 42px;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 14px;
  background-color: rgba(255, 255, 255, 0.12);
  font-size: 23px;
  backdrop-filter: blur(8px);
}
.exchange-country-mark small {
  display: block;
  color: rgba(255, 255, 255, 0.72);
  font-size: 10px;
  font-weight: 750;
}
.exchange-country-mark b {
  display: block;
  margin-top: 5px;
  color: #fff;
  font-size: 16px;
  font-weight: 900;
  white-space: nowrap;
}
.exchange-country-mark b i {
  margin: 0 3px;
  color: var(--exchange-accent);
  font-style: normal;
}
.exchange-rate-value {
  flex: none;
  text-align: right;
}
.exchange-rate-value > b {
  display: block;
  color: #fff;
  font-size: 27px;
  font-weight: 950;
  letter-spacing: -0.04em;
}
.exchange-rate-value > b small {
  margin-left: 2px;
  font-size: 14px;
}
.exchange-rate-value > span {
  display: inline-block;
  margin-top: 6px;
  padding: 4px 7px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.76);
  font-size: 10px;
  font-weight: 850;
}
.exchange-rate-value > span.up {
  color: #ffd7d0;
}
.exchange-rate-value > span.down {
  color: #bfe0ff;
}
.exchange-rate-loading {
  color: rgba(255, 255, 255, 0.65);
  font-size: 9px;
}
@keyframes home-fade-down {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes home-fade-up {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes home-card-reveal {
  from {
    opacity: 0;
    transform: translateY(22px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
@keyframes ticket-swap {
  from {
    opacity: 0;
    transform: translateX(14px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}
@keyframes ticket-card-enter {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.97);
    filter: blur(3px);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}
@keyframes progress-shine {
  60%,
  100% {
    transform: translateX(100%);
  }
}
@keyframes home-spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── 여행 등록 후 홈 화면 - 후보 A 사이즈/디자인 반영 ── */
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
  animation: none;
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
  font-size: 19px;
  font-weight: 900;
  color: #10192b;
  letter-spacing: -0.02em;
}
.home-wordmark {
  display: block;
  width: 88px;
  height: auto;
  object-fit: contain;
}
.mode-switch-control {
  flex: none;
  width: 112px;
  padding: 3px;
  background: #edeff3;
  box-shadow: none;
}
.mode-switch-control button {
  height: auto;
  padding: 6px 0;
  font-size: 11px;
  font-weight: 800;
}
.mode-switch-thumb {
  top: 3px;
  bottom: 3px;
  left: 3px;
  height: auto;
  width: calc(50% - 3px);
  background: #0b2a6b;
}


.month-saving-card {
  margin-top: 16px;
  padding: 18px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff8ef 0%, #ffffff 55%);
  box-shadow: 0 4px 14px rgba(16, 25, 43, 0.06);
}
.month-saving-card::before {
  top: -40px;
  right: -40px;
  width: 140px;
  height: 140px;
  border: none;
  background: radial-gradient(
    circle,
    rgba(242, 153, 74, 0.16) 0%,
    rgba(242, 153, 74, 0) 70%
  );
}
.month-saving-heading {
  margin-bottom: 16px;
}
.month-saving-heading h2 {
  margin-top: 0;
  font-size: 17px;
  white-space: nowrap;
}
.month-saving-values {
  gap: 10px;
  margin-top: 0;
}
.month-saving-values > div {
  padding: 15px 10px;
  border: none;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(16, 25, 43, 0.05);
}
.month-saving-values > div + div {
  border-left: none;
}
.month-saving-values small {
  color: #98a2b3;
  font-size: 10.5px;
  font-weight: 700;
}
.month-saving-values b {
  margin-top: 6px;
  color: #10192b;
  font-size: 14px;
}
.month-progress-label {
  justify-content: flex-end;
  margin-top: 16px;
  color: #5a6478;
  font-size: 12.5px;
}
.month-progress-label strong {
  color: #2f6fed;
  font-size: 13.5px;
}
.month-saving-progress {
  height: 6px;
  margin-top: 10px;
  border: none;
  background: #edf0f6;
}
.month-saving-progress i {
  background: #2f6fed;
}
.month-saving-progress i::before {
  width: 12px;
  height: 12px;
  background: #f2994a;
}
.month-wallet-button {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #0b2a6b;
  box-shadow: none;
}
.month-wallet-button > span i {
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.15);
}
.month-wallet-button > b {
  color: #ffd466;
  font-size: 11.5px;
  font-weight: 700;
}

.analysis-load-error {
  padding: 14px 15px;
  border: none;
  border-radius: 14px;
  background: #f6f8fc;
  gap: 12px;
}
.analysis-load-error > span {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: #eaf1ff;
  color: #2f6fed;
  font-size: 10.5px;
  font-weight: 800;
}
.analysis-load-error b {
  color: #10192b;
  font-size: 12.5px;
  font-weight: 800;
}
.analysis-load-error small {
  color: #98a2b3;
  font-size: 10.5px;
  font-weight: 600;
}
.analysis-load-error button {
  color: #2f6fed;
  font-size: 11.5px;
  font-weight: 800;
}

.exchange-live-card {
  margin-top: 16px;
  border-radius: 16px;
}
.exchange-card-head {
  padding: 14px 18px;
}
.exchange-card-head span {
  font-size: 10px;
}
.exchange-card-head time {
  font-size: 9.5px;
}
.exchange-country-mark > span {
  flex: 0 0 32px;
  height: 32px;
  background-color: #fff;
  font-size: 15px;
}
.exchange-country-mark small {
  font-size: 11.5px;
  font-weight: 700;
}
.exchange-country-mark b {
  font-size: 13.5px;
}
.exchange-rate-value > b {
  font-size: 20px;
}
.exchange-rate-value > span {
  background: rgba(76, 217, 123, 0.15);
  color: #4cd97b;
}
.exchange-rate-value > span.up {
  color: #4cd97b;
}
.exchange-rate-value > span.down {
  color: #ff6b6b;
}

@media (prefers-reduced-motion: reduce) {
  .savings-home-header,
  .country-carousel,
  .country-carousel-meta,
  .country-ticket,
  .country-ticket .rounded-xl,
  .month-saving-card,
  .ai-report-card,
  .exchange-live-card,
  .month-saving-progress i::after {
    animation: none;
  }
  .country-slide {
    transition: none;
  }
}
</style>
