<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useTravelModeStore } from '@/stores/travelMode';
import { useTravelStore } from '@/stores/travel';
import NotificationBell from '@/components/common/NotificationBell.vue';

const props = defineProps({
  userName: { type: String, default: '권유현' },
  onSwitchMode: { type: Function, default: null },
});

const router = useRouter();
const travelMode = useTravelModeStore();
const travelStore = useTravelStore();

// 데이터 바인딩을 위한 계산 속성 추가
const tripId = computed(() => travelStore.tripId);
const passNumber = computed(() => {
  const d = new Date();
  const yy = String(d.getFullYear()).slice(-2);
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yy}${mm}${dd}`;
});
const tripStatus = computed(() => travelStore.tripStatus);
const tripInfo = computed(() => tripStatus.value?.tripInfo);
const countries = computed(() => tripStatus.value?.countries || []);

// 국가 목록 캐싱 (필터링되지 않은 전체 목록)
const persistentCountries = ref([]);

// 국가 선택 목록 (API 연동)
const destinations = computed(() => {
  const all = { code: 'all', name: '전체', flag: '🌍', theme: '#17485b' };
  const apiCountries = persistentCountries.value.map((c) => ({
    code: c.tripCountryId.toString(),
    name: c.countryName,
    flag: countryFlagMap[c.countryName]
      ? `fi fi-${countryFlagMap[c.countryName]}`
      : '🌍',
    theme: getCountryColor(c.countryName),
  }));
  return [all, ...apiCountries];
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

const dday = computed(() => {
  if (!endDate.value) return 0;
  const diff = endDate.value - today.value;
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
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

// 자산 및 진행률 계산
const totalRemainingFund = computed(
  () => tripStatus.value?.totalRemainingFund || 0,
);

const totalBudget = computed(() =>
  countries.value.reduce((sum, c) => sum + c.targetBudget, 0),
);
const totalSpent = computed(() =>
  countries.value.reduce((sum, c) => sum + c.spentAmount, 0),
);
const overallProgress = computed(() =>
  totalBudget.value > 0
    ? Math.round((totalSpent.value / totalBudget.value) * 100)
    : 0,
);

// 국가별 자산 (Carousel)
const tripAssets = computed(() => {
  return countries.value.map((c) => {
    const assetTemplate = overallAssets.find(
      (a) => a.country === c.countryName,
    );
    return {
      ...assetTemplate,
      amount: c.targetBudget - c.spentAmount,
      local: `${c.countryName} 남은 금액`,
    };
  });
});

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

function getCategoryIcon(name) {
  return categoryIcons[name] || '📁';
}

// 국가별 색상 매핑 헬퍼
function getCountryColor(countryName) {
  if (countryName === '홍콩') return '#ffb800'; // 요청하신 노란색
  const asset = overallAssets.find((a) => a.country === countryName);
  return asset ? asset.theme : '#9aa4b3'; // 기본색
}

// 여행자금 체크를 위한 데이터 가공
const categorySummary = computed(() => tripStatus.value?.categorySummary || []);

const maxCategoryTotal = computed(() => {
  return Math.max(...categorySummary.value.map((c) => c.totalAmount), 1);
});

const categoryList = computed(() => {
  return categorySummary.value.map((cat) => {
    const total = cat.totalAmount;
    // 이제 바의 전체 길이를 maxCategoryTotal 대비로 설정 (비례적 표현)
    const barWidthPercent = (total / maxCategoryTotal.value) * 100;

    const details = cat.countryDetails.map((d) => ({
      ...d,
      // 세그먼트 폭은 해당 카테고리 전체 폭 내에서의 비율
      percent: total > 0 ? (d.amount / total) * 100 : 0,
    }));

    return {
      name: cat.categoryName,
      total: cat.totalAmount,
      barWidth: barWidthPercent,
      details: details,
      icon: getCategoryIcon(cat.categoryName),
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

onMounted(async () => {
  await travelStore.loadActiveGoal();
  if (tripId.value) {
    // 초기 로딩 시 필터링 없이 전체 데이터를 가져와 캐싱
    const status = await travelStore.loadTripStatus(tripId.value, null);
    persistentCountries.value = status?.countries || [];

    if (selectedCountryId.value !== 'all') {
      await loadData();
    }
  }
});

const countryMenuOpen = ref(false);
const selectedCountryId = computed({
  get: () => travelMode.selectedDestination,
  set: (val) => travelMode.selectDestination(val),
});
const assetsCarousel = ref(null);

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

function selectDestination(item) {
  selectedCountryId.value = item.code;
  countryMenuOpen.value = false;
  loadData();
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
    ? (destinations.find((item) => item.code === calculatorCountryCode.value) ??
      destinations[1])
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
    <header class="travel-header">
      <div class="header-controls">
        <div
          class="mode-toggle travel-selected"
          aria-label="서비스 모드 전환"
          @click="switchMode('savings')"
        >
          <span class="mode-thumb" />
          <button class="active" type="button">여행</button>
          <button type="button">저축</button>
        </div>
        <div class="country-select">
          <button
            type="button"
            :aria-expanded="countryMenuOpen"
            @click="countryMenuOpen = !countryMenuOpen"
          >
            <span></span>{{ selected.name }}<i>⌄</i>
          </button>
          <div v-if="countryMenuOpen" class="country-menu">
            <button
              v-for="item in destinations"
              :key="item.code"
              type="button"
              :class="{ active: item.code === selected.code }"
              @click="selectDestination(item)"
            >
              {{ item.name }}
            </button>
          </div>
        </div>
      </div>
      <h1>안녕하세요, {{ userName }}님</h1>
      <NotificationBell />
    </header>

    <template v-if="!tripId">
      <article class="empty-trip-ticket">
        <span class="empty-trip-orbit" aria-hidden="true"></span>
        <div class="empty-trip-band">
          <span>TRIPASS · START JOURNEY</span>
          <span class="empty-trip-goal">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M2 16l20-7-7 20-3-8-8-3-2-2z" fill="#FFD466"/></svg>
            GOAL
          </span>
        </div>
        <div class="empty-trip-body">
          <span class="empty-trip-badge">
            <svg width="21" height="21" viewBox="0 0 24 24" fill="none"><path d="M2 16l20-7-7 20-3-8-8-3-2-2z" fill="#FFD466"/></svg>
          </span>
          <div>
            <strong>아직 등록된 여행이 없어요</strong>
            <p>여행명·국가·일정을 등록하면<br>AI가 목표 예산과 월 저축액을 제안해요</p>
          </div>
          <button type="button" class="empty-trip-cta" @click="router.push({ name: 'TravelRegister' })">여행 계획 등록하기</button>
        </div>
        <div class="empty-trip-tear" aria-hidden="true">
          <span class="empty-trip-notch left"></span>
          <span class="empty-trip-notch right"></span>
          <span class="empty-trip-dash"></span>
        </div>
        <div class="empty-trip-footer">
          <span>PASS NO. TRP-{{ passNumber }}</span>
          <span class="empty-trip-barcode" aria-hidden="true"></span>
        </div>
      </article>

      <div class="empty-trip-guide">
        <span class="guide-label">TRIPASS GUIDE</span>
        <strong>목표 설정부터 월렛 저축까지</strong>
        <p>여행 예산은 AI가 제안하고, 실제 저축은 TRIP 월렛에서 관리해요</p>
      </div>
    </template>
    <template v-else>
    <article
      class="ticket"
      :class="[
        { combined: selected.code === 'all' },
        `country-${selected.code}`,
      ]"
      :style="{
        '--theme': selected.theme,
        '--photo': `url(${selected.image})`,
      }"
    >
      <div class="ticket-top">
        <span>BOARDING PASS</span><span>TRIPASS AIR</span
        ><span
          >NO. {{ selected.code === 'all' ? 'EUR' : selected.code }}-230</span
        >
      </div>
      <div class="perforation"><i /><span /><i /></div>
      <div class="ticket-main">
        <div class="trip-line">
          <b>{{ tripInfo?.tripName || '여행' }}</b
          ><strong>D-{{ dday }}</strong>
        </div>
        <div class="trip-progress">
          <small>{{ currentDay }}일차</small>
          <div>
            <i
              :style="{
                width: `${(currentDay / totalTripDays) * 100}%`,
              }"
            />
          </div>
          <small>{{ totalTripDays }}일차</small>
        </div>
        <p class="trip-description">여행 남은 자산을 한눈에 확인해요 ✨</p>
        <div class="ticket-photo-space" />

        <div class="travel-summary-content">
          <div class="summary-title-wrapper">
            <div class="summary-title">
              <span
                >{{
                  selected.code === 'all'
                    ? '전체 남은 여행 자산'
                    : `${selected.name}에서 남은 여행 자산`
                }}
                (합산)</span
              ><strong>{{ formatWon(totalRemainingFund) }}</strong>
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
            class="country-assets"
            :style="{
              'grid-template-columns':
                selected.code === 'all' ? '1fr 1fr' : '1fr',
            }"
            ref="assetsCarousel"
            aria-label="국가별 남은 여행 자산"
          >
            <div
              v-for="asset in selected.code === 'all'
                ? tripAssets
                : tripAssets.filter((a) => a.code === selected.code)"
              :key="asset.code"
              class="country-asset-card"
              :style="{
                '--asset-image': `url(${asset.image})`,
                '--asset-theme': asset.theme,
              }"
            >
              <span>{{ asset.flag }} {{ asset.country }} 남은 여행 자산</span
              ><b>{{ formatWon(asset.amount) }}</b
              ><small>({{ asset.local }})</small>
            </div>
          </div>
          <div class="fund-label">
            <span>여행 자금 진행률</span><b>{{ overallProgress }}%</b>
          </div>
          <div class="fund-track">
            <i :style="{ width: `${overallProgress}%` }" />
          </div>
          <div class="fund-meta">
            <span>목표 {{ formatWon(totalBudget) }}</span
            ><span>현재 지출 {{ formatWon(totalSpent) }}</span>
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
    </article>

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
      <div class="card-title">
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
      <div v-if="categoryList.length === 0" class="empty-msg">
        여행 자금 체크 내역이 없어요.
      </div>
      <div v-else v-for="cat in categoryList" :key="cat.name" class="budget-row">
        <span class="category"
          ><i>{{ cat.icon }}</i
          >{{ cat.name }}</span
        >
        <div class="split-bar" :style="{ width: `${cat.barWidth}%` }">
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
        </div>
        <b>{{ formatWon(cat.total) }}</b>
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
        <i>{{ item.icon }}</i
        ><span
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
          v-for="item in destinations.slice(1)"
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
  margin: auto;
  padding-bottom: 94px;
  background: #f8f6f1;
  color: #10192d;
}
.empty-trip-ticket {
  position: relative;
  margin: 18px 16px 0;
  border-radius: 20px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(155deg, #0b2a6b 0%, #123c94 60%, #17459f 100%);
  box-shadow: 0 12px 26px rgba(11, 42, 107, 0.24);
}
.empty-trip-orbit {
  position: absolute;
  top: -50px;
  right: -40px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
}
.empty-trip-band {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.14em;
  color: rgba(255, 255, 255, 0.55);
}
.empty-trip-goal {
  display: flex;
  align-items: center;
  gap: 4px;
  letter-spacing: 0.1em;
  color: #ffd466;
}
.empty-trip-body {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 26px 22px 22px;
  text-align: center;
}
.empty-trip-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}
.empty-trip-body strong {
  font-size: 16.5px;
  font-weight: 800;
}
.empty-trip-body p {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.65);
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
  background: #fff;
  font-size: 14px;
  font-weight: 800;
}
.empty-trip-tear { position: relative; height: 18px; }
.empty-trip-notch {
  position: absolute;
  top: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f8f6f1;
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
  margin: 0 16px;
  overflow: hidden;
  border-radius: 18px;
  background: var(--theme);
  color: #fff;
  box-shadow: 0 8px 18px #2037652b;
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
  background: #f8f6f1;
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
    linear-gradient(180deg, #091b4270, #071733b8),
    var(--photo) center/cover;
}
.combined .ticket-main {
  background: linear-gradient(135deg, #103779, #1553a2);
}
.trip-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.trip-line b {
  font-size: 14px;
}
.trip-line strong {
  font-size: 24px;
}
.trip-progress {
  display: grid;
  grid-template-columns: 42px 1fr 42px;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  color: #d8e5ff;
  font-size: 10px;
}
.trip-progress small:last-child {
  text-align: right;
}
.trip-progress div {
  height: 3px;
  background: #ffffff80;
}
.trip-progress i {
  display: block;
  height: 4px;
  background: #ffb800;
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
  background: linear-gradient(90deg, #73c8e7, #fff1cc 55%, #ef5b54);
}
.fund-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  color: #d6e1f2;
  font-size: 9px;
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
  border: 1px solid #dfe5ee;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 12px #1425480d;
  text-align: left;
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
  gap: 8px;
}
.legend span:first-child {
  color: #276ce0;
}
.legend span:last-child {
  color: #c12b40;
}
.budget-row {
  display: grid;
  grid-template-columns: 92px 1fr 76px;
  align-items: center;
  gap: 7px;
  height: 43px;
}
.category {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  font-weight: 700;
}
.category i {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: #f3f5f8;
  font-style: normal;
}
.single-bar,
.split-bar {
  display: flex;
  height: 7px;
  overflow: hidden;
  border-radius: 99px;
  background: #e8ebf2;
}
.single-bar i,
.split-bar i,
.split-bar em {
  display: block;
  height: 100%;
}
.split-bar i {
  background: #0b3c90;
}
.split-bar em {
  background: #a51530;
}
.budget-row > b {
  text-align: right;
  font-size: 10px;
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
  font-style: normal;
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
  height: 335px;
  min-height: 335px;
  background:
    linear-gradient(180deg, #091b4260, #071733bf),
    var(--photo) center/cover;
}
.trip-line {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: end;
  gap: 8px;
}
.trip-line > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.trip-line small {
  color: #ffffff80;
  font-size: 7px;
  letter-spacing: 0.12em;
}
.trip-line b {
  font-size: 15px;
}
.departure {
  text-align: right;
}
.flight-route {
  display: flex;
  align-items: center;
  color: #ffd829;
  font-size: 16px;
}
.flight-route i {
  width: 100%;
  border-top: 1px dashed #ffffff80;
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
  height: 320px;
  min-height: 320px;
  padding: 20px;
  overflow: hidden;
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
.summary-title {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
  margin-top: 9px;
}
.summary-title > span {
  color: #fff;
  font-size: 10px;
  font-weight: 800;
}
.summary-title > strong {
  color: #fff;
  font-size: 18px;
  white-space: nowrap;
}
.summary-title > strong small {
  color: #8cebbf;
  font-size: 8px;
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
}
.trip-line b {
  font-size: 14px;
}
.trip-line strong {
  font-size: 22px;
}
.trip-progress {
  grid-template-columns: 42px 1fr 42px;
  margin-top: 7px;
  font-size: 9px;
}
.trip-description {
  margin-top: 7px;
  font-size: 9px;
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
.country-FR .fund-track i {
  background: linear-gradient(90deg, #002395 0%, #f4f4f4 52%, #ed2939 100%);
}
.country-CH .fund-track i {
  background: linear-gradient(90deg, #ff0000 0%, #fff 58%, #ff0000 100%);
}
.country-DE .fund-track i {
  background: linear-gradient(90deg, #111 0%, #dd0000 52%, #ffce00 100%);
}
.country-JP .fund-track i {
  background: linear-gradient(90deg, #fff 0%, #bc002d 48%, #fff 100%);
}
.country-HK .fund-track i {
  background: linear-gradient(90deg, #de2910 0%, #ffde00 100%);
}
.country-all .fund-track i {
  background: linear-gradient(
    90deg,
    #002395 0%,
    #f4f4f4 30%,
    #ed2939 48%,
    #ff0000 66%,
    #fff 82%,
    #ff0000 100%
  );
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
.country-all .fund-track i {
  background: linear-gradient(90deg, #79d3d8 0%, #f8d56b 55%, #f29a55 100%);
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
