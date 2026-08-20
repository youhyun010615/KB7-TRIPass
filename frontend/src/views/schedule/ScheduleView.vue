<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import ScheduleCard from '@/components/schedule/ScheduleCard.vue';
import { useTravelScheduleStore } from '@/stores/travelSchedule';

const router = useRouter();
const store = useTravelScheduleStore();
const timelineList = ref(null);
const currentTimestamp = ref(Date.now());
let scheduleClockTimer = null;

onMounted(async () => {
  scheduleClockTimer = window.setInterval(() => {
    currentTimestamp.value = Date.now();
  }, 60_000);
  await store.loadSchedules().catch(() => {});
  await nextTick();
  positionTimelineAtNext();
});

onBeforeUnmount(() => window.clearInterval(scheduleClockTimer));

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
    positionTimelineAtNext();
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
const dateLabel = (date) =>
  new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'short',
  }).format(new Date(`${date}T00:00:00`));
const travelDays = computed(() =>
  Math.max(
    1,
    Math.floor(
      (new Date(`${store.travelEnd}T00:00:00`) -
        new Date(`${store.travelStart}T00:00:00`)) /
        86_400_000,
    ) + 1,
  ),
);
const openDetail = (id) => router.push(`/schedule/${id}`);

function positionTimelineAtNext() {
  const list = timelineList.value;
  if (!list || !completedScheduleCount.value) return;

  const nextAnchor = list.querySelector('[data-next-anchor="true"]');
  list.scrollTop = nextAnchor
    ? Math.max(0, nextAnchor.offsetTop - 10)
    : list.scrollHeight;
}

function showPastSchedules() {
  timelineList.value?.scrollTo({ top: 0, behavior: 'smooth' });
}
</script>

<template>
  <main class="schedule-page">
    <header class="page-header">
      <button type="button" @click="router.back()">‹</button>
      <h1>여행 일정 목록</h1>
      <span />
    </header>
    <section class="period-card">
      <span class="period-icon">🗓️</span
      ><b>{{ store.travelStart }} ~ {{ store.travelEnd }}</b
      ><em>{{ travelDays }}일</em>
    </section>
    <p v-if="store.errorMessage" class="empty">{{ store.errorMessage }}</p>

    <section
      v-if="nextSchedule"
      class="today-ticket"
    >
      <div class="ticket-head">
        <div>
          <span class="today-eyebrow"><i /> NEXT SCHEDULE</span>
          <b>다음 일정은 이 일정이에요</b>
        </div>
        <time>{{ dateLabel(nextSchedule.date) }}</time>
      </div>
      <div class="cut"><i /><span /><i /></div>
      <div class="today-summary">
        <strong>현재 시각을 기준으로 가장 가까운 일정입니다</strong>
        <small>일정을 선택하면 장소와 결제 정보를 확인할 수 있어요.</small>
      </div>
      <div class="today-preview-list">
        <ScheduleCard
          :schedule="nextSchedule"
          compact
          @detail="openDetail"
        />
      </div>
    </section>
    <section v-else class="empty-ticket">
      <span class="empty-ticket-badge">
        <span class="empty-ticket-pulse" aria-hidden="true" />
        <span aria-hidden="true">✈️</span>
      </span>
      <b>남아 있는 여행 일정이 없어요</b>
      <small>아래에서 완료된 일정을 다시 확인할 수 있어요.</small>
    </section>

    <section class="upcoming-card">
      <div class="section-title">
        <div>
          <h2>여행 일정</h2>
          <p>다음 일정부터 보이며 위로 스크롤하면 완료 일정도 볼 수 있어요</p>
        </div>
        <button
          v-if="completedScheduleCount"
          type="button"
          @click="showPastSchedules"
        >
          완료 {{ completedScheduleCount }}건 <span aria-hidden="true">↑</span>
        </button>
        <span v-else>총 {{ store.sortedSchedules.length }}건</span>
      </div>
      <div ref="timelineList" class="upcoming-list">
        <div
          v-for="group in timelineGroups"
          :key="group.date"
          class="date-group"
          :class="{ completed: group.isCompleted }"
        >
          <h3>
            {{ dateLabel(group.date) }}
            <span v-if="group.isCompleted">완료된 일정</span>
          </h3>
          <ScheduleCard
            v-for="item in group.items"
            :key="item.id"
            :schedule="item"
            :completed="isScheduleCompleted(item)"
            :data-next-anchor="item.id === nextSchedule?.id ? 'true' : null"
            @detail="openDetail"
          />
        </div>
        <div v-if="!timelineGroups.length" class="empty-state">
          <span aria-hidden="true">🧭</span>
          <b>등록된 여행 일정이 없어요</b>
          <small>새 일정을 추가하면 이곳에 표시돼요.</small>
        </div>
      </div>
    </section>

    <button
      class="add-button"
      type="button"
      @click="
        router.push({
          path: '/schedule/new',
          query: router.currentRoute.value.query,
        })
      "
    >
      <span class="add-button-icon" aria-hidden="true">+</span>
      <span class="add-button-label">새 여행 일정 추가하기</span>
    </button>
    <BottomNav />
  </main>
</template>

<style scoped>
.schedule-page {
  min-height: 100vh;
  padding: 0 20px 150px;
  background: #f4f5f9;
  color: #10192d;
}
.page-header {
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: end;
  height: 92px;
  padding-bottom: 17px;
}
.page-header button {
  font-size: 28px;
  text-align: left;
  color: #10192d;
}
.page-header h1 {
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.period-card {
  display: grid;
  grid-template-columns: 22px 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(16, 25, 43, 0.06);
}
.period-icon {
  font-size: 17px;
}
.period-card b {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.period-card em {
  padding: 7px 13px;
  border-radius: 99px;
  background: #eef2ff;
  color: #173f8d;
  font-size: 12px;
  font-weight: 700;
  font-style: normal;
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
  top: 116px;
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
  padding: 22px 22px 18px;
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
  font-size: 20px;
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
  padding: 18px 22px 5px;
}
.today-summary strong,
.today-summary small {
  display: block;
}
.today-summary strong {
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}
.today-summary small {
  margin-top: 5px;
  color: #bcd2f5;
  font-size: 10px;
  font-weight: 600;
}
.today-ticket :deep(.schedule-card) {
  width: calc(100% - 36px);
  margin: 12px 18px 8px;
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
  margin-top: 18px;
  padding: 20px;
  border: 1px solid #e7edf9;
  border-radius: 22px;
  background: linear-gradient(165deg, #fff 0%, #f8faff 100%);
  box-shadow: 0 8px 22px rgba(16, 25, 43, 0.07);
}
.section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.section-title h2 {
  color: #173f8d;
  font-size: 18px;
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
  margin: 16px -5px 0;
  padding: 0 5px 5px;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 24px 4px 11px;
  color: #245ec4;
  font-size: 13px;
  font-weight: 900;
}
.date-group:first-child h3 {
  margin-top: 2px;
}
.date-group h3 span {
  padding: 4px 7px;
  border-radius: 7px;
  background: #eef1f5;
  color: #8b97a9;
  font-size: 8px;
  font-weight: 800;
}
.date-group.completed h3 {
  color: #8b97a9;
}
.date-group :deep(.schedule-card) {
  margin-top: 9px;
}
.empty {
  padding: 40px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 44px 20px;
  text-align: center;
}
.empty-state span {
  font-size: 28px;
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
  right: max(calc((100vw - 390px) / 2 + 20px), 20px);
  bottom: 78px;
  left: max(calc((100vw - 390px) / 2 + 20px), 20px);
  z-index: 40;
  display: flex;
  height: 56px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 28px;
  background: #173f8d;
  color: #fff;
  box-shadow: 0 10px 22px rgba(23, 63, 141, 0.3);
}
.add-button:active {
  transform: scale(0.98);
}
.add-button-icon {
  display: flex;
  width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  font-weight: 600;
  line-height: 1;
}
.add-button-label {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.today-preview-list {
  padding-bottom: 12px;
}
.today-preview-list :deep(.schedule-card) {
  width: calc(100% - 36px);
  margin: 10px 18px;
}
</style>
