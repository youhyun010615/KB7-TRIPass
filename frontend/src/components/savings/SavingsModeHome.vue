<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useExchangeStore } from '@/stores/exchange';
import { useTravelStore } from '@/stores/travel';
import NotificationBell from '@/components/common/NotificationBell.vue';
import TravelTicket from '@/components/savings/TravelTicket.vue';

const props = defineProps({
  onSwitchMode: { type: Function, default: null },
});

const authStore = useAuthStore();
const exchangeStore = useExchangeStore();
const travelStore = useTravelStore();
const router = useRouter();
const userName = computed(() => authStore.user?.name ?? '회원');

onMounted(async () => {
  await nextTick();
  restoreCountryPosition();
  await Promise.all([
    travelStore.loadHomeDashboard({ force: true }),
    exchangeStore.updateExchangeRates(),
  ]);
  await nextTick();
  restoreCountryPosition();
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
const countries = computed(() =>
  [...(homeDashboard.value?.countries || [])]
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map((country) => {
      const presentation =
        countryPresentation[country.countryName] || defaultPresentation;
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
const prepaidExpenseTotal = computed(() =>
  Number(homeDashboard.value?.prepaidExpenseTotal || 0),
);
const daysUntilDeparture = computed(() =>
  Number(homeDashboard.value?.daysUntilDeparture || 0),
);
const currentMonthLabel = computed(() => `${new Date().getMonth() + 1}월`);
const monthlySavedAmount = computed(() =>
  Number(homeDashboard.value?.monthlySavedAmount || 0),
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

function goWallet() {
  router.push('/wallet');
}

function switchMode(mode) {
  if (props.onSwitchMode) props.onSwitchMode(mode);
}
</script>

<template>
  <section class="savings-mode-home">
    <template v-if="travelStore.homeLoading && !homeDashboard">
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
    <template v-else-if="!travelStore.hasTravelGoal">
      <div class="px-5 pt-12 pb-3 bg-white/80">
        <div class="flex items-center justify-between">
          <button
            class="px-3 py-1.5 rounded-full text-xs font-bold"
            style="background: #eef2ff; color: #263f8c"
          >
            여행 저축
          </button>
          <NotificationBell />
        </div>
        <p class="mt-2 text-lg font-extrabold">안녕하세요, {{ userName }}님</p>
        <p class="mt-1 text-[10px] text-slate-500">
          새로운 여행을 함께 준비해 볼까요?
        </p>
      </div>

      <div class="px-4 mt-3">
        <TravelTicket eyebrow="TRIPASS · START JOURNEY">
          <div class="py-1 text-center">
            <div class="mb-2 text-2xl">✈</div>
            <h2 class="text-[16px] font-extrabold">
              아직 등록된 여행이 없어요
            </h2>
            <p class="mt-2 text-[10px] leading-4 text-blue-100">
              여행명·국가·일정을 등록하면<br />AI가 목표 예산과 월 저축액을
              제안해요.
            </p>
            <button
              class="w-full h-11 mt-4 rounded-xl text-[12px] font-extrabold text-white"
              style="background: #ff7a36"
              @click="router.push('/savings')"
            >
              여행 계획 등록하기
            </button>
          </div>
        </TravelTicket>
      </div>

      <section
        class="mx-4 mt-4 rounded-2xl border border-blue-100 bg-blue-50/80 px-4 py-4"
      >
        <p class="text-[10px] font-extrabold" style="color: #2864e8">
          TRIPASS GUIDE
        </p>
        <h2 class="mt-1 text-[15px] font-extrabold">
          목표 설정부터 월렛 저축까지
        </h2>
        <p class="mt-1 text-[10px] leading-4 text-slate-500">
          여행 예산은 AI가 제안하고, 실제 저축은 TRIP 월렛에서 관리해요.
        </p>
      </section>
    </template>

    <!-- ══ 여행 저축 모드 ══════════════════════════════════════ -->
    <template v-else>
      <!-- 헤더 -->
      <div class="savings-home-header">
        <div class="mode-switch-control savings-selected">
          <span class="mode-switch-thumb" />
          <button type="button" @click="switchMode('travel')">여행</button>
          <button type="button" class="selected" @click="switchMode('savings')">
            저축
          </button>
        </div>

        <div class="savings-header-row savings-greeting-row">
          <h1>안녕하세요, {{ userName }}님</h1>
          <NotificationBell />
        </div>

        <section class="active-trip-heading">
          <div class="active-trip-icon">✈</div>
          <div class="active-trip-copy">
            <small>MY NEXT TRIP</small>
            <h2>{{ homeDashboard?.tripName || '여행 정보 불러오는 중...' }}</h2>
            <p v-if="homeDashboard">
              <span>출발</span>{{ formatDate(homeDashboard.startDate) }}
              <i>·</i> D-{{ daysUntilDeparture }}
            </p>
          </div>
          <RouterLink
            class="trip-edit-button"
            :to="{ name: 'TravelRegister', query: { mode: 'edit' } }"
          >
            수정 <span>›</span>
          </RouterLink>
        </section>
      </div>

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
              <span class="text-white/70 text-[10px] font-bold tracking-widest"
                >BOARDING PASS</span
              >
              <span class="text-white/50 text-[10px] tracking-widest"
                >TRIPASS AIR</span
              >
              <span class="text-white/70 text-[10px] font-semibold"
                >NO. {{ country.code }}-{{ country.displayOrder }}</span
              >
            </div>

            <!-- 사진의 시작 경계와 정확히 맞닿는 상단 절취선 -->
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
                      class="text-white text-[26px] font-extrabold leading-none"
                    >
                      {{ country.flag }} {{ country.name }}
                    </p>
                  </div>
                  <div class="flex-1 flex items-center mt-3.5">
                    <div
                      class="flex-1 border-t border-dashed border-white/40"
                    />
                    <span class="mx-2 text-yellow-300 text-lg">✈</span>
                    <div
                      class="flex-1 border-t border-dashed border-white/40"
                    />
                  </div>
                  <div class="text-right flex-none">
                    <p
                      class="text-white/65 text-[10px] uppercase tracking-widest mb-1"
                    >
                      Departure
                    </p>
                    <p
                      class="text-white text-[26px] font-extrabold leading-none"
                    >
                      D-{{ daysUntilDeparture }}
                    </p>
                  </div>
                </div>
                <p class="ticket-description text-white/90 text-[12px] mt-3">
                  {{ country.desc }} ✨
                </p>

                <!-- 사진이 보이는 여백 -->
                <div class="ticket-photo-space" />

                <!-- 진행 박스 (반투명, 사진 위에 떠있음) -->
                <div
                  class="rounded-xl px-4 py-4"
                  :style="`background:${country.progressBg}`"
                >
                  <div class="flex justify-between mb-2">
                    <span class="font-semibold text-[13px] text-white">{{
                      ticketSavingCopy.title
                    }}</span>
                    <span class="text-white font-extrabold text-[14px]"
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
                  class="ticket-stub w-full px-5 flex items-center justify-between active:bg-gray-50"
                  @click="goWallet"
                >
                  <span class="text-[13px] font-bold text-white">송금하기</span>
                  <div class="flex items-center gap-2">
                    <div class="flex gap-[1.5px] items-end h-5">
                      <div
                        v-for="(h, i) in [
                          14, 7, 20, 5, 14, 9, 20, 5, 16, 5, 12, 8, 18, 5, 14,
                        ]"
                        :key="i"
                        class="bg-white/85 rounded-[0.5px]"
                        :style="`height:${h}px;width:${i % 4 === 0 ? '2.5px' : '1.5px'}`"
                      />
                    </div>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <path
                        d="M9 18L15 12L9 6"
                        stroke="#FFFFFF"
                        stroke-width="2.5"
                        stroke-linecap="round"
                      />
                    </svg>
                  </div>
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
        <div class="month-card-route" aria-hidden="true">
          <i /><span>✈</span><i />
        </div>
        <div class="month-saving-heading">
          <div>
            <small>MONTHLY TRAVEL FUND</small>
            <h2>{{ currentMonthLabel }} 여행 저축</h2>
          </div>
          <p>D-{{ daysUntilDeparture }}</p>
        </div>
        <div class="month-saving-values">
          <div>
            <span>◎</span><small>이번 달 목표</small
            ><b>{{ formatCurrency(monthlyTarget) }}</b>
          </div>
          <div>
            <span>✓</span><small>저축한 금액</small
            ><b>{{ formatCurrency(monthlySavedAmount) }}</b>
          </div>
          <div>
            <span>▣</span><small>남은 저축</small
            ><b>{{ formatCurrency(monthlyRemainingAmount) }}</b>
          </div>
        </div>
        <div class="month-progress-label">
          <span>이번 달 저축 여정</span
          ><strong>{{ monthlySavingPercent }}%</strong>
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

      <section class="ai-report-card mx-4 mt-3">
        <div class="ai-report-heading">
          <div>
            <p>{{ homeDashboard.tripName }}</p>
            <small>등록한 여행 준비 요약</small>
          </div>
          <RouterLink :to="{ name: 'TravelRegister', query: { mode: 'edit' } }"
            >수정하기 ›</RouterLink
          >
        </div>
        <div class="ai-goal-status">
          <span class="status-icon">✦</span>
          <div>
            <strong>출발까지 {{ daysUntilDeparture }}일 남았어요</strong>
            <p>
              {{ formatDate(homeDashboard.startDate) }} ~
              {{ formatDate(homeDashboard.endDate) }}
            </p>
          </div>
        </div>
        <div class="trip-summary-row">
          <span>방문 국가</span
          ><b>{{
            countries.map((country) => country.countryName).join(' · ')
          }}</b>
        </div>
        <div class="trip-summary-row">
          <span>항공·숙소 사전 지출</span
          ><b>{{ formatCurrency(prepaidExpenseTotal) }}</b>
        </div>
        <div class="trip-summary-row">
          <span>월 저축 목표</span><b>{{ formatCurrency(monthlyTarget) }}</b>
        </div>
      </section>

      <!-- 오늘의 실시간 환율 -->
      <div class="exchange-live-card mx-4 mt-3 mb-4" :style="exchangeCardStyle">
        <div class="exchange-card-head">
          <div><i /><span>LIVE EXCHANGE</span></div>
          <time>{{ exchangeStore.lastUpdateDate || '최신 고시 기준' }}</time>
        </div>
        <div class="exchange-card-body">
          <div class="exchange-country-mark">
            <span>{{ selectedCountry.flag }}</span>
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
        <div class="exchange-route" aria-hidden="true">
          <i /><span>✈</span><i />
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.savings-mode-home {
  min-height: 100vh;
  background: #f3f6ff;
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
.ticket-cutline-top {
  transform: translateY(0);
}
.ticket-cutline-bottom {
  margin-top: 14px;
}
.ticket-notch {
  position: absolute;
  top: 50%;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f7f4ee;
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
  border: 1px solid #d9dee7;
  border-radius: 999px;
  background: #eceff3;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
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
.active-trip-heading {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #dae6fb;
  border-radius: 17px;
  background: linear-gradient(135deg, #fff 0%, #eef5ff 100%);
  box-shadow: 0 8px 22px rgba(24, 61, 130, 0.09);
}
.active-trip-icon {
  display: grid;
  flex: 0 0 42px;
  height: 42px;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(145deg, #173f8d, #3475e6);
  color: #fff;
  font-size: 20px;
  box-shadow: 0 7px 14px rgba(36, 105, 232, 0.24);
}
.active-trip-copy {
  min-width: 0;
}
.active-trip-copy {
  flex: 1;
}
.active-trip-copy small {
  color: #6d8dc0;
  font-size: 9px;
  font-weight: 900;
  letter-spacing: 0.13em;
}
.active-trip-copy h2 {
  overflow: hidden;
  margin-top: 3px;
  color: #173f8d;
  font-size: 19px;
  font-weight: 950;
  letter-spacing: -0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.active-trip-copy p {
  margin-top: 6px;
  color: #526b93;
  font-size: 12px;
  font-weight: 750;
}
.active-trip-copy p span {
  margin-right: 6px;
  padding: 3px 6px;
  border-radius: 6px;
  background: #dceaff;
  color: #2469e8;
  font-size: 9px;
  font-weight: 900;
}
.active-trip-copy p i {
  margin: 0 4px;
  color: #9cb0cf;
  font-style: normal;
}
.trip-edit-button {
  display: flex;
  flex: none;
  align-items: center;
  gap: 2px;
  padding: 8px 10px;
  border: 1px solid #c9dcfa;
  border-radius: 10px;
  background: #fff;
  color: #2469e8;
  font-size: 11px;
  font-weight: 900;
  transition:
    transform 0.2s ease,
    background 0.2s ease;
}
.trip-edit-button span {
  font-size: 15px;
  line-height: 1;
}
.trip-edit-button:active {
  transform: scale(0.95);
  background: #edf4ff;
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
  padding: 0 1px 4px;
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
.month-card-route {
  position: absolute;
  top: 23px;
  right: 88px;
  display: flex;
  width: 78px;
  align-items: center;
  color: #ef7b3c;
  opacity: 0.75;
}
.month-card-route i {
  flex: 1;
  border-top: 1px dashed #ef9b6f;
}
.month-card-route span {
  margin: 0 5px;
  font-size: 13px;
  transform: rotate(7deg);
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
.month-saving-values > div > span {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  margin-bottom: 8px;
  border-radius: 8px;
  background: #e7effe;
  color: #2469e8;
  font-size: 11px;
  font-weight: 900;
}
.month-saving-values > div:nth-child(2) > span {
  background: #e3f7f1;
  color: #0a9a82;
}
.month-saving-values > div:nth-child(3) > span {
  background: #fff0e7;
  color: #e86e31;
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
  background: linear-gradient(
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
  background: var(--exchange-accent);
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.1);
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
  align-items: flex-end;
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
.exchange-country-mark > span {
  display: grid;
  flex: 0 0 42px;
  height: 42px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.12);
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
.exchange-route {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  padding: 0 17px 14px;
  color: var(--exchange-accent);
  opacity: 0.7;
}
.exchange-route i {
  flex: 1;
  border-top: 1px dashed rgba(255, 255, 255, 0.24);
}
.exchange-route span {
  margin: 0 8px;
  font-size: 12px;
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
