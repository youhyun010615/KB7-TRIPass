<script setup>
import { computed, nextTick, onActivated, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import NotificationBell from '@/components/common/NotificationBell.vue';
import ScheduleCard from '@/components/schedule/ScheduleCard.vue';
import TravelModeMeta from '@/components/travel/TravelModeMeta.vue';
import { useTravelScheduleStore } from '@/stores/travelSchedule';
import { useTravelStore } from '@/stores/travel';
import { useTravelReportStore } from '@/stores/travelReport';
import { flagIconClass } from '@/stores/travel';
import TravelArchiveSummaryCard from '@/components/mypage/TravelArchiveSummaryCard.vue';
import { now as currentDateTime } from '@/utils/devDate';

const props = defineProps({
  listMode: {
    type: Boolean,
    default: false,
  },
});

const router = useRouter();
const route = useRoute();
const store = useTravelScheduleStore();
const travel = useTravelStore();
const reportStore = useTravelReportStore();
const timelineList = ref(null);
const calendarStrip = ref(null);
const currentTimestamp = ref(currentDateTime().getTime());
const selectedCalendarDate = ref('');
let scheduleClockTimer = null;

onMounted(async () => {
  scheduleClockTimer = window.setInterval(() => {
    currentTimestamp.value = currentDateTime().getTime();
  }, 60_000);
  if (props.listMode) {
    const id = Number(route.params.id || route.query.tripId);
    if (Number.isFinite(id) && id > 0) {
      if (!travel.countries.length) await travel.loadCountries().catch(() => {});
      await Promise.allSettled([
        reportStore.loadPreTripReport(id),
        reportStore.loadTripBasic(id),
      ]);
      await store.loadSchedules(id).catch(() => {});
    }
  } else {
    await store.loadSchedules().catch(() => {});
  }
  await nextTick();
  await positionTimelineAtNext();
  positionCalendarAtToday();
});

onBeforeUnmount(() => window.clearInterval(scheduleClockTimer));

onActivated(async () => {
  await positionTimelineAtNext();
  positionCalendarAtToday();
});

function scheduleTimestamp(item) {
  const timestamp = new Date(`${item.date}T${item.time || '00:00'}:00`).getTime();
  return Number.isNaN(timestamp) ? Number.POSITIVE_INFINITY : timestamp;
}

function isScheduleCompleted(item) {
  return Boolean(
    item.completed ||
      item.scheduleStatus === 'DONE' ||
      scheduleTimestamp(item) < currentTimestamp.value,
  );
}

const nextSchedule = computed(() =>
  store.sortedSchedules.find((item) => !isScheduleCompleted(item)),
);
const completedScheduleCount = computed(() =>
  store.sortedSchedules.filter(isScheduleCompleted).length,
);
watch(
  () => nextSchedule.value?.id,
  async () => {
    await nextTick();
    await positionTimelineAtNext();
  },
);
const timelineGroups = computed(() => {
  const groups = new Map();
  store.sortedSchedules.forEach((item) => {
    if (!groups.has(item.date)) groups.set(item.date, []);
    groups.get(item.date).push(item);
  });
  return [...groups.entries()].map(([date, items]) => ({
    date,
    items,
    isCompleted: items.every(isScheduleCompleted),
  }));
});
const featuredSchedule = computed(() => nextSchedule.value || null);
const upcomingTimelineGroups = computed(() =>
  timelineGroups.value
    .sort((a, b) => a.date.localeCompare(b.date)),
);
const visibleTimelineGroups = computed(() => {
  if (!props.listMode || !selectedCalendarDate.value) return upcomingTimelineGroups.value;
  return upcomingTimelineGroups.value.filter(
    (group) => group.date === selectedCalendarDate.value,
  );
});
const visibleScheduleCount = computed(() =>
  visibleTimelineGroups.value.reduce((total, group) => total + group.items.length, 0),
);
const scheduleCountByDate = computed(() => {
  const counts = new Map();
  store.sortedSchedules.forEach((item) => {
    counts.set(item.date, (counts.get(item.date) || 0) + 1);
  });
  return counts;
});
const selectedTripPeriods = computed(() => {
  if (!props.listMode) return store.configuredPeriods;
  return (reportStore.tripBasic?.countries || [])
    .map((item) => {
      const catalog = travel.countries.find(
        (country) => Number(country.countryId) === Number(item.countryId),
      );
      const name = item.countryName || item.name || catalog?.name || '';
      return {
        code: catalog?.code || travel.countryFlagMap?.[name]?.code || '',
        name,
        startDate: item.arrivalDate || item.startDate || '',
        endDate: item.departureDate || item.endDate || '',
      };
    })
    .filter((item) => item.code && item.startDate && item.endDate)
    .sort((a, b) => a.startDate.localeCompare(b.startDate));
});
const selectedTravelStart = computed(() => {
  if (selectedTripPeriods.value.length) return selectedTripPeriods.value[0].startDate;
  return props.listMode ? reportStore.preTripReport?.startDate : store.travelStart;
});
const selectedTravelEnd = computed(() => {
  if (selectedTripPeriods.value.length) return selectedTripPeriods.value.at(-1).endDate;
  return props.listMode ? reportStore.preTripReport?.endDate : store.travelEnd;
});
const periodForDate = (date) => selectedTripPeriods.value.find(
  (period) => date >= period.startDate && date <= period.endDate,
);

const travelDates = computed(() => {
  if (!selectedTravelStart.value || !selectedTravelEnd.value) return [];
  const cursor = new Date(`${selectedTravelStart.value}T00:00:00`);
  const end = new Date(`${selectedTravelEnd.value}T00:00:00`);
  const dates = [];
  let previousPeriodCode = '';

  while (cursor <= end && dates.length < 45) {
    const date = [
      cursor.getFullYear(),
      String(cursor.getMonth() + 1).padStart(2, '0'),
      String(cursor.getDate()).padStart(2, '0'),
    ].join('-');
    const period = periodForDate(date);

    // 다음 날짜의 국가 정보를 미리 조회하여 국가 마지막 일자 여부(countryEnd) 확인
    const nextCursor = new Date(cursor);
    nextCursor.setDate(nextCursor.getDate() + 1);
    const nextDate = [
      nextCursor.getFullYear(),
      String(nextCursor.getMonth() + 1).padStart(2, '0'),
      String(nextCursor.getDate()).padStart(2, '0'),
    ].join('-');
    const nextPeriod = periodForDate(nextDate);

    dates.push({
      date,
      day: cursor.getDate(),
      weekday: new Intl.DateTimeFormat('ko-KR', { weekday: 'short' }).format(cursor),
      count: scheduleCountByDate.value.get(date) || 0,
      completed:
        (scheduleCountByDate.value.get(date) || 0) > 0 &&
        store.sortedSchedules
          .filter((item) => item.date === date)
          .every(isScheduleCompleted),
      countryCode: period?.code || '',
      countryName: period?.name || '',
      countryStart: Boolean(period?.code && period.code !== previousPeriodCode),
      countryEnd: Boolean(period?.code && (!nextPeriod?.code || period.code !== nextPeriod.code)),
    });
    previousPeriodCode = period?.code || '';
    cursor.setDate(cursor.getDate() + 1);
  }
  return dates;
});

const dateLabel = (date) =>
  new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'short',
  }).format(new Date(`${date}T00:00:00`));

const countriesForDate = (date) => {
  const archivedCountries = props.listMode
    ? (reportStore.tripBasic?.countries || []).filter((item) => {
        const start = item.arrivalDate || item.startDate || '';
        const end = item.departureDate || item.endDate || '';
        return start && end && date >= start && date <= end;
      }).map((item) => {
        const catalog = travel.countries.find(
          (country) => Number(country.countryId) === Number(item.countryId),
        );
        const name = item.countryName || catalog?.name || '';
        return {
          code: catalog?.code || travel.countryFlagMap?.[name]?.code || '',
          name,
        };
      })
    : [];

  if (props.listMode) return archivedCountries;
  const codes = store.configuredPeriods
    .filter((period) => date >= period.startDate && date <= period.endDate)
    .map((period) => period.code);
  const scheduleCodes = store.sortedSchedules
    .filter((item) => item.date === date)
    .map((item) => item.countryCode);
  return [...new Set([...codes, ...scheduleCodes])]
    .map((code) => store.countries.find((country) => country.code === code))
    .filter(Boolean);
};

const shortDateLabel = (date) => {
  if (!date) return '';
  const value = new Date(`${date}T00:00:00`);
  return `${String(value.getMonth() + 1).padStart(2, '0')}.${String(value.getDate()).padStart(2, '0')} (${new Intl.DateTimeFormat('ko-KR', { weekday: 'short' }).format(value)})`;
};

const travelDays = computed(() =>
  Math.max(
    1,
    Math.floor(
      (new Date(`${selectedTravelEnd.value}T00:00:00`) -
        new Date(`${selectedTravelStart.value}T00:00:00`)) /
        86_400_000,
    ) + 1,
  ),
);

const tripCountries = computed(() => {
  const codes = selectedTripPeriods.value.map((period) => period.code);
  return codes.map((code) => store.countries.find((country) => country.code === code)).filter(Boolean);
});

const currentCountry = computed(() => {
  const todaySchedules = store.sortedSchedules.filter((item) => item.date === store.today);
  const currentTime = new Intl.DateTimeFormat('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(currentDateTime());
  const startedSchedule = [...todaySchedules]
    .filter((item) => (item.time || '00:00') <= currentTime)
    .at(-1);
  const activeSchedule = startedSchedule || todaySchedules[0];
  const scheduleCountry = store.countries.find(
    (country) => country.code === activeSchedule?.countryCode,
  );
  return scheduleCountry || store.countryForDate(store.today) || tripCountries.value[0] || null;
});

const currentTravelDay = computed(() => {
  const start = new Date(`${store.travelStart}T00:00:00`).getTime();
  const today = new Date(`${store.today}T00:00:00`).getTime();
  if (![start, today].every(Number.isFinite)) return 0;
  if (today < start) return 0;
  return Math.min(travelDays.value, Math.floor((today - start) / 86_400_000) + 1);
});

const openDetail = (id) => {
  if (props.listMode) {
    const tripId = route.params.id || route.query.tripId;
    router.push(`/mypage/travel/${tripId}/schedules/${id}`);
    return;
  }
  router.push(`/schedule/${id}`);
};
const archiveTrip = computed(() => reportStore.tripSummary);
const archiveStatus = computed(() =>
  archiveTrip.value?.status === '여행 중' ? '여행중' : archiveTrip.value?.status,
);

function openScheduleForm() {
  const query = { ...route.query };
  if (selectedCalendarDate.value) query.date = selectedCalendarDate.value;
  if (props.listMode) {
    const tripId = route.params.id || route.query.tripId;
    router.push({
      path: `/mypage/travel/${tripId}/schedules/new`,
      query: { ...query, tripId },
    });
    return;
  }
  router.push({ path: '/schedule/new', query });
}

async function positionTimelineAtNext() {
  const list = timelineList.value;
  const todayExists = travelDates.value.some((date) => date.date === store.today);
  selectedCalendarDate.value = todayExists ? store.today : (nextSchedule.value?.date || '');
  if (!list) return;

  await nextTick();
  await new Promise((resolve) => window.requestAnimationFrame(resolve));
  const nextAnchor = list.querySelector('[data-next-anchor="true"]');
  const nextGroup = list.querySelector('[data-next-group="true"]');
  const target = nextAnchor || nextGroup;
  if (!target) {
    list.scrollTop = 0;
    return;
  }

  const targetTop =
    target.getBoundingClientRect().top - list.getBoundingClientRect().top + list.scrollTop;
  list.scrollTop = Math.max(0, targetTop - (nextAnchor ? 48 : 2));
}

function groupHasNextSchedule(group) {
  return group.items.some((item) => item.id === nextSchedule.value?.id);
}

function positionCalendarAtToday() {
  const list = calendarStrip.value;
  if (!list) return;
  const targetDate = travelDates.value.some((date) => date.date === store.today)
    ? store.today
    : selectedCalendarDate.value;
  const target = list.querySelector(`[data-calendar-date="${targetDate}"]`);
  if (!target) return;
  list.scrollLeft = Math.max(0, target.offsetLeft - list.offsetLeft - 2);
}

async function focusTimelineDate(date) {
  selectedCalendarDate.value = date;
  await nextTick();
  await new Promise((resolve) => window.requestAnimationFrame(resolve));
  const list = timelineList.value;
  const target = list?.querySelector(`[data-date="${date}"]`);
  if (!target || !list) return;

  list.style.setProperty('--focus-tail-space', '0px');
  const targetTop = target.offsetTop;
  const requiredTail = Math.max(
    0,
    targetTop - (list.scrollHeight - list.clientHeight) + 8,
  );
  list.style.setProperty('--focus-tail-space', `${requiredTail}px`);
  await nextTick();
  await new Promise((resolve) => window.requestAnimationFrame(resolve));
  list.scrollTo({
    top: Math.max(0, targetTop - 2),
    behavior: 'smooth',
  });
}

function showPastSchedules() {
  timelineList.value?.scrollTo({ top: 0, behavior: 'smooth' });
}
</script>

<template>
  <main class="schedule-page" :class="{ 'schedule-list-mode': listMode }">
    <div v-if="!listMode" class="schedule-header-fixed">
      <header class="schedule-header">
        <div>
          <img src="@/assets/brand/tripass-text.png" class="header-wordmark" alt="TRIPASS" />
          <h1>SCHEDULE</h1>
        </div>
        <NotificationBell />
      </header>

      <TravelModeMeta
        :trip-name="travel.tripName"
        :date-range="`${store.travelStart} — ${store.travelEnd}`"
        :day="currentTravelDay"
        :country-name="currentCountry?.name"
        :country-code="currentCountry?.code"
        :country-codes="tripCountries.map(country => country.code)"
      />
    </div>
    <div v-if="!listMode" class="schedule-header-spacer" aria-hidden="true" />
    <header v-else class="schedule-list-header">
      <button type="button" aria-label="이전 화면으로 이동" @click="router.back()">‹</button>
      <h1>여행 일정 목록</h1>
      <span aria-hidden="true" />
    </header>

    <TravelArchiveSummaryCard
      v-if="listMode && (route.params.id || route.query.tripId)"
      :trip-id="route.params.id || route.query.tripId"
    />

    <section class="calendar-card">
      <div class="calendar-heading">
        <div>
          <small>TRIP CALENDAR</small>
          <b>{{ selectedTravelStart }} — {{ selectedTravelEnd }}</b>
        </div>
        <em>{{ travelDays }}일</em>
      </div>
      <div ref="calendarStrip" class="calendar-strip" aria-label="여행 날짜별 일정">
        <button
          v-for="date in travelDates"
          :key="date.date"
          type="button"
          :class="{
            active: selectedCalendarDate === date.date,
            today: store.today === date.date,
            completed: date.completed,
            empty: !date.count,
          }"
          :data-calendar-date="date.date"
          @click="focusTimelineDate(date.date)"
        >
          <span
            v-if="date.countryCode"
            class="country-period-line"
            :class="{ start: date.countryStart, end: date.countryEnd }"
          >
            <em v-if="date.countryStart" :class="flagIconClass(date.countryCode)" />
            <b v-if="date.countryStart">{{ date.countryName }}</b>
          </span>
          <small>{{ date.weekday }}</small>
          <strong>{{ date.day }}</strong>
          <span :aria-label="`${date.count}개 일정`">
            <i v-for="index in Math.min(date.count, 3)" :key="index" />
          </span>
        </button>
      </div>
    </section>
    <p v-if="store.errorMessage" class="empty">{{ store.errorMessage }}</p>

    <section v-if="featuredSchedule && !listMode" class="today-schedule-section">
      <div class="list-heading">
        <h2>다가오는 일정</h2>
        <em>{{ shortDateLabel(featuredSchedule.date) }}</em>
      </div>
      <div class="today-schedule-list">
        <ScheduleCard
          :key="featuredSchedule.id"
          :schedule="featuredSchedule"
          :completed="false"
          today
          @detail="openDetail"
        />
      </div>
    </section>

    <section class="upcoming-card" :class="{ 'is-empty': !visibleTimelineGroups.length }">
      <div class="section-title">
        <div>
          <h2>{{ listMode ? '여행 일정' : '전체 일정' }}</h2>
        </div>
        <span>총 {{ listMode ? visibleScheduleCount : store.sortedSchedules.length }}건</span>
      </div>
      <div ref="timelineList" class="upcoming-list">
        <div
          v-for="group in visibleTimelineGroups"
          :key="group.date"
          class="date-group"
          :class="{ completed: group.isCompleted }"
          :data-date="group.date"
          :data-next-group="groupHasNextSchedule(group) ? 'true' : null"
        >
          <p
            v-if="!listMode && completedScheduleCount && groupHasNextSchedule(group)"
            class="completed-count-note"
          >
            완료된 일정 {{ completedScheduleCount }}건
          </p>
          <h3>
            <span>{{ dateLabel(group.date) }}</span>
            <span v-if="countriesForDate(group.date).length" class="date-countries">
              <span
                v-for="item in countriesForDate(group.date)"
                :key="item.code || item.name"
                class="date-country"
              >
                <i :class="flagIconClass(item.code)" />
                {{ item.name }}
              </span>
            </span>
          </h3>
          <ScheduleCard
            v-for="item in group.items"
            :key="item.id"
            :schedule="item"
            :country-code="countriesForDate(group.date)[0]?.code || item.countryCode"
            :completed="isScheduleCompleted(item)"
            :data-next-anchor="item.id === nextSchedule?.id ? 'true' : null"
            @detail="openDetail"
          />
        </div>
        <div v-if="!visibleTimelineGroups.length" class="empty-state">
          <span class="empty-calendar-icon" aria-hidden="true">＋</span>
          <b>{{ listMode ? '선택한 날짜에 등록된 일정이 없어요' : '아직 등록된 일정이 없어요' }}</b>
          <small>{{ listMode ? '아래 버튼을 눌러 이 날짜에 일정을 추가해 보세요.' : '첫 일정을 등록하면 타임라인에 차곡차곡 채워져요.' }}</small>
        </div>
      </div>
    </section>

    <button
      class="add-button"
      type="button"
      @click="openScheduleForm"
    >
      <span class="add-button-icon" aria-hidden="true">+</span>
      <span class="add-button-label">여행 일정 추가</span>
    </button>
    <BottomNav v-if="!listMode" />
  </main>
</template>

<style scoped>
.schedule-page {
  min-height: 100vh;
  padding: 0 20px 150px;
  background: #f4f5f9;
  color: #10192d;
}
.schedule-header-fixed {
  position: fixed;
  top: 0;
  left: 50%;
  z-index: 60;
  width: 100%;
  max-width: 390px;
  padding: 14px 18px 10px;
  background: #f4f5f9;
  transform: translateX(-50%);
}
.schedule-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.header-wordmark {
  display: block;
  width: 88px;
  height: auto;
  object-fit: contain;
}
.schedule-header h1 {
  margin-top: 6px;
  color: #29466f;
  font-size: 17px;
  font-weight: 400;
  letter-spacing: normal;
}
.schedule-header-spacer {
  height: 142px;
}
.schedule-list-mode {
  padding-bottom: 48px;
}
.schedule-list-header {
  display: grid;
  height: 64px;
  align-items: center;
  grid-template-columns: 40px 1fr 40px;
  margin-bottom: 12px;
  padding-top: 4px;
}
.schedule-list-header button {
  display: grid;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #10192d;
  font-size: 36px;
  font-weight: 400;
  line-height: 1;
  place-items: center;
}
.schedule-list-header h1 {
  margin: 0;
  text-align: center;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -.04em;
}
.calendar-card {
  overflow: hidden;
  padding: 1px 0 4px;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}
.calendar-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  display:none;
}
.calendar-heading > div {
  display: grid;
  gap: 3px;
}
.calendar-heading small {
  color: #2f6fed;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: .15em;
}
.calendar-heading b {
  font-size: 14px;
  font-weight: 850;
  letter-spacing: -.02em;
}
.calendar-heading em {
  padding: 7px 13px;
  border-radius: 99px;
  background: #eef2ff;
  color: #173f8d;
  font-size: 12px;
  font-weight: 700;
  font-style: normal;
}
.calendar-strip {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 39px 2px 3px;
  overflow-x: auto;
  scroll-snap-type: x proximity;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}
.calendar-strip::-webkit-scrollbar {
  display: none;
}
.calendar-strip button {
  position: relative;
  display: grid;
  flex: 0 0 40px;
  width: 40px;
  height: 52px;
  min-height: 0;
  padding: 6px 0 5px;
  grid-template-rows: 12px 19px 3px;
  place-items: center;
  align-content: center;
  gap: 0;
  border: 1px solid #edf1f7;
  border-radius: 13px;
  background: #fff;
  color: #17233b;
  scroll-snap-align: center;
  transition: .2s ease;
}

/* 국가별 날짜 범위 전체에 진한 파란색 라인 적용 */
.calendar-strip .country-period-line {
  position: absolute;
  top: -15px;
  left: -4px;
  display: block;
  width: 47px;
  height: 13px;
  border-top: 2px solid #2662ea;
  color: #526f9e;
  z-index: 1;
}

/* 마디 점 */
.calendar-strip .country-period-line::after {
  position: absolute;
  top: -4px;
  right: 0;
  width: 6px;
  height: 6px;
  border: 2px solid #2662ea;
  border-radius: 50%;
  background: #f4f5f9;
  content: '';
}

/* 국가 시작일 (4일) */
.calendar-strip .country-period-line.start {
  z-index: 2;
}
.calendar-strip .country-period-line.start::before {
  position: absolute;
  top: -4px;
  left: 0;
  width: 6px;
  height: 6px;
  border: 2px solid #2662ea;
  border-radius: 50%;
  background: #fff;
  content: '';
}
.calendar-strip .country-period-line em {
  position: absolute;
  top: -17px;
  left: 0;
  width: 13px;
  height: 9px;
  border-radius: 2px;
  background-size: cover;
  font-style: normal;
}
.calendar-strip .country-period-line b {
  position: absolute;
  top: -19px;
  left: 17px;
  color: #284c87;
  font-size: 8px;
  font-weight: 900;
  white-space: nowrap;
}

/* 국가 마지막 날 (9일) */
.calendar-strip .country-period-line.end::after {
  background: #2662ea;
}

.calendar-strip button small {
  color: #8c98aa;
  font-size: 8px;
  font-weight: 800;
}
.calendar-strip button strong {
  font-family: 'Space Mono', ui-monospace, monospace;
  font-size: 13px;
  font-weight: 900;
}
.calendar-strip button > span:not(.country-period-line) {
  display: flex;
  height: 3px;
  align-items: center;
  gap: 2px;
}
.calendar-strip button i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #2f6fed;
}
.calendar-strip button.completed { opacity: .55; }
.calendar-strip button.empty { color: #98a2b3; }
.calendar-strip button.today {
  opacity: 1;
  border-color: #f2c64d;
  background: #fff8df;
  box-shadow: 0 5px 13px rgba(206, 153, 20, .14);
}
.calendar-strip button.today small { color: #a56d00; }
.calendar-strip button.today i { background: #e9aa12; }
.calendar-strip button.active {
  border-color: #1d58b8;
  background: #0b2a6b;
  color: #fff;
  box-shadow: 0 7px 15px rgba(30, 88, 181, .25);
  transform: translateY(-1px);
}
.calendar-strip button.active small { color: #d7e6ff; }
.calendar-strip button.active i { background: #ffd462; }
.calendar-strip button.today.active {
  border-color:#ffd462;
  background:#0b2a6b;
  box-shadow:0 7px 15px rgba(30,88,181,.25),0 0 0 2px rgba(255,212,98,.42);
}
.today-ticket {
  position: relative;
  display: block;
  width: 100%;
  margin-top: 18px;
  overflow: hidden;
  border-radius: 22px;
  background:
    radial-gradient(circle at 88% 4%, rgba(88, 156, 255, 0.48), transparent 32%),
    linear-gradient(145deg, #0e3479 0%, #1553aa 58%, #1d64c2 100%);
  color: #fff;
  box-shadow: 0 14px 30px rgba(16, 55, 121, 0.26);
  text-align: left;
}
.today-ticket::before,
.today-ticket::after {
  position: absolute;
  top: 94px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f4f5f9;
  content: '';
}
.today-ticket::before { left: -10px; }
.today-ticket::after { right: -10px; }
.ticket-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 19px 20px 16px;
}
.ticket-head > div {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.today-eyebrow {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #ffd761;
  font-family: 'Space Mono', ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.15em;
}
.today-eyebrow i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ffd761;
  box-shadow: 0 0 0 5px rgba(255, 215, 97, 0.14);
}
.ticket-head b {
  font-size: 18px;
  font-weight: 900;
  letter-spacing: -0.035em;
}
.ticket-head time {
  padding: 7px 10px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.1);
  color: #dce9ff;
  font-family: 'Space Mono', ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
}
.cut {
  display: grid;
  grid-template-columns: 15px 1fr 15px;
  align-items: center;
  height: 0;
}
.cut i {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f4f5f9;
}
.cut i:first-child {
  transform: translateX(-10px);
}
.cut i:last-child {
  transform: translateX(5px);
}
.cut span {
  border-top: 1px dashed #ffffff80;
}
.today-summary {
  padding: 15px 20px 3px;
}
.today-summary strong,
.today-summary small {
  display: block;
}
.today-summary strong {
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}
.today-summary small {
  margin-top: 5px;
  color: #bcd2f5;
  font-size: 10px;
  font-weight: 600;
}
.today-ticket :deep(.schedule-card) {
  width: calc(100% - 32px);
  margin: 10px 16px 8px;
}
.empty-ticket {
  display: flex;
  min-height: 150px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-top: 18px;
  border-radius: 22px;
  background: linear-gradient(135deg, #103779, #1553a2);
  color: #fff;
  box-shadow: 0 10px 22px rgba(16, 41, 92, 0.22);
}
.empty-ticket-badge {
  position: relative;
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
  font-size: 19px;
}
.empty-ticket-pulse {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(255, 212, 102, 0.32);
  animation: schedule-beacon 2.4s ease-out infinite;
}
@keyframes schedule-beacon {
  0% { box-shadow: 0 0 0 0 rgba(255, 212, 102, 0.32); }
  70%, 100% { box-shadow: 0 0 0 12px rgba(255, 212, 102, 0); }
}
@media (prefers-reduced-motion: reduce) {
  .empty-ticket-pulse { animation: none; }
}
.empty-ticket b {
  margin-top: 14px;
  font-size: 16px;
  font-weight: 700;
}
.empty-ticket small {
  margin-top: 6px;
  color: #d7e4fb;
  font-size: 13px;
  font-weight: 500;
}
.upcoming-card {
  margin-top: 14px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}
.upcoming-card.is-empty {
  display: flex;
  min-height: calc(100dvh - 300px);
  flex-direction: column;
}
.upcoming-card.is-empty .upcoming-list {
  display: flex;
  max-height: none;
  flex: 1;
  margin-top: 0;
  padding: 0;
  overflow: hidden;
}
.section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.section-title h2 {
  color: #10192d;
  font-size: 17px;
  font-weight: 900;
  letter-spacing: -0.035em;
}
.section-title p {
  margin-top: 5px;
  color: #98a2b3;
  font-size: 9px;
  font-weight: 600;
}
.section-title > span {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
}
.section-title button {
  display: flex;
  min-height: 29px;
  align-items: center;
  gap: 4px;
  padding: 0 9px;
  border-radius: 9px;
  background: #edf4ff;
  color: #2868cf;
  font-size: 9px;
  font-weight: 800;
  white-space: nowrap;
}
.section-title button span {
  color: #2868cf;
  font-size: 11px;
}
.upcoming-list {
  position: relative;
  max-height: 560px;
  margin: 14px 0 0;
  padding: 0 2px calc(5px + var(--focus-tail-space, 0px)) 41px;
  overflow-y: auto;
  overscroll-behavior-y: contain;
  scroll-behavior: smooth;
  touch-action: pan-y;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: #c7d3e4 transparent;
}
.upcoming-list::-webkit-scrollbar {
  width: 6px;
}
.upcoming-list::-webkit-scrollbar-thumb {
  border-radius: 99px;
  background: #c7d3e4;
}
.date-group h3 {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 24px 4px 11px 9px;
  color: #245ec4;
  font-size: 13px;
  font-weight: 900;
}
.date-group:first-child h3 {
  margin-top: 2px;
}
.date-countries {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 5px;
}
.date-country {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 7px;
  border-radius: 999px;
  background: #edf4ff;
  color: #355b91;
  font-size: 9px;
  font-weight: 900;
  white-space: nowrap;
}
.date-country i {
  display: inline-block;
  width: 16px;
  height: 11px;
  border-radius: 2px;
  background-position: center;
  background-size: cover;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .12);
}
.date-group {
  position: relative;
  padding-left: 9px;
}
.date-group::before {
  position: absolute;
  top: 28px;
  bottom: -25px;
  left: -25px;
  width: 2px;
  border-radius: 99px;
  background: #a9c8f5;
  content: '';
}
.date-group:last-child::before { bottom: 28px; }
.date-group h3::before {
  position: absolute;
  left: -43px;
  width: 10px;
  height: 10px;
  border: 3px solid #f8faff;
  border-radius: 50%;
  background: #2662ea;
  box-shadow: 0 0 0 2px #cfe0fb, 0 0 0 4px #f4f5f9;
  content: '';
}
.date-group.completed::before { background: #aeb7c4; }
.date-group.completed h3::before {
  background: #667085;
  box-shadow: 0 0 0 2px #d5d9df, 0 0 0 4px #f4f5f9;
}
.date-group.completed h3 {
  color: #8b97a9;
}
.date-group :deep(.schedule-card) {
  margin-top: 9px;
  border-left: 0;
}
.date-group.completed :deep(.schedule-card) { border-color: #d6dbe3; }
.completed-count-note {
  margin: 2px 4px 8px 9px;
  color: #98a2b3;
  font-size: 10px;
  font-weight: 750;
}
.completed-count-note + h3 { margin-top: 0; }
.today-schedule-section {
  margin-top: 22px;
}
.list-heading {
  display: flex;
  align-items: center;
  gap: 9px;
}
.list-heading h2 {
  font-size: 17px;
  font-weight: 900;
}
.list-heading em {
  padding: 5px 10px;
  border-radius: 999px;
  background: #0b2a6b;
  color: #fff;
  font-family: 'Space Mono', ui-monospace, monospace;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
}
.today-schedule-list {
  display: grid;
  gap: 11px;
  margin-top: 12px;
}
.empty {
  padding: 40px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}
.empty-state {
  display: flex;
  width: 100%;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 20px 20px 76px;
  text-align: center;
}
.empty-state span {
  font-size: 28px;
}
.empty-state .empty-calendar-icon {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 50%;
  background: #eaf1ff;
  color: #0b2a6b;
  font-size: 26px;
  font-weight: 500;
  animation: empty-calendar-float 2.4s ease-in-out infinite;
}
@keyframes empty-calendar-float {
  0%, 100% {
    transform: translateY(0);
    box-shadow: 0 0 0 0 rgba(47, 111, 237, .14);
  }
  50% {
    transform: translateY(-5px);
    box-shadow: 0 0 0 10px rgba(47, 111, 237, 0);
  }
}
.empty-state b {
  margin-top: 4px;
  color: #26334d;
  font-size: 16px;
  font-weight: 700;
}
.empty-state small {
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.4;
}
.add-button {
  position: fixed;
  right: max(calc((100vw - 390px) / 2 + 28px), 28px);
  bottom: 78px;
  left: max(calc((100vw - 390px) / 2 + 28px), 28px);
  z-index: 40;
  display: flex;
  height: 48px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 13px;
  background: #0b2a6b;
  color: #fff;
  box-shadow: 0 10px 22px rgba(23, 63, 141, 0.3);
}
.add-button:active {
  transform: scale(0.98);
}
.add-button-icon {
  display: flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 600;
  line-height: 1;
}
.add-button-label {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
@media (prefers-reduced-motion: reduce){.trip-pass-head em.traveling{animation:none}}
.today-preview-list {
  padding-bottom: 12px;
}
.today-preview-list :deep(.schedule-card) {
  width: calc(100% - 36px);
  margin: 10px 18px;
}
.route-flag{display:inline-block;width:27px;height:18px;border-radius:3px;background-size:cover;box-shadow:0 2px 5px rgba(0,0,0,.22);vertical-align:middle}
</style>
