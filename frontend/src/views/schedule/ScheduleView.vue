<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import ScheduleCard from '@/components/schedule/ScheduleCard.vue';
import { useTravelScheduleStore } from '@/stores/travelSchedule';

const router = useRouter();
const store = useTravelScheduleStore();
const todayModalOpen = ref(false);
const timelineList = ref(null);

onMounted(async () => {
  await store.loadSchedules().catch(() => {});
  await nextTick();
  positionTimelineAtUpcoming();
});
const todaySchedules = computed(() =>
  store.sortedSchedules.filter(
    (item) => item.date === store.todayFor(item.timeZone),
  ),
);
const upcomingSchedules = computed(() =>
  store.sortedSchedules.filter(
    (item) => item.date > store.todayFor(item.timeZone),
  ),
);
const pastSchedules = computed(() =>
  store.sortedSchedules.filter(
    (item) => item.date < store.todayFor(item.timeZone),
  ),
);
const timelineGroups = computed(() => {
  const groups = new Map();
  [...pastSchedules.value, ...upcomingSchedules.value].forEach((item) => {
    if (!groups.has(item.date)) groups.set(item.date, []);
    groups.get(item.date).push(item);
  });
  return [...groups.entries()].map(([date, items]) => ({
    date,
    items,
    isPast: items.every(
      (item) => item.date < store.todayFor(item.timeZone),
    ),
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

function positionTimelineAtUpcoming() {
  const list = timelineList.value;
  if (!list || !pastSchedules.value.length) return;

  const upcomingAnchor = list.querySelector('[data-upcoming-anchor="true"]');
  list.scrollTop = upcomingAnchor
    ? Math.max(0, upcomingAnchor.offsetTop - 10)
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
      v-if="todaySchedules.length"
      class="today-ticket"
      role="button"
      tabindex="0"
      @click="todayModalOpen = true"
      @keydown.enter="todayModalOpen = true"
    >
      <div class="ticket-head">
        <div>
          <span class="today-eyebrow"><i /> TODAY</span>
          <b>오늘의 여행 일정</b>
        </div>
        <time>{{ store.today.replaceAll('-', '.') }}</time>
      </div>
      <div class="cut"><i /><span /><i /></div>
      <div class="today-summary">
        <strong>{{ todaySchedules.length }}개의 일정이 기다리고 있어요</strong>
        <small>가장 가까운 일정부터 확인해 보세요.</small>
      </div>
      <div class="today-preview-list">
        <ScheduleCard
          v-for="item in todaySchedules"
          :key="item.id"
          :schedule="item"
          compact
          @detail="openDetail"
        />
      </div>
      <div class="all-link">오늘 일정 전체 보기 <span>›</span></div>
    </section>
    <section v-else class="empty-ticket">
      <span class="empty-ticket-badge">
        <span class="empty-ticket-pulse" aria-hidden="true" />
        <span aria-hidden="true">✈️</span>
      </span>
      <b>등록된 오늘 일정이 없어요</b>
      <small>새로운 여행 일정을 추가해 보세요.</small>
    </section>

    <section class="upcoming-card">
      <div class="section-title">
        <div>
          <h2>다가오는 여행 일정</h2>
          <p v-if="pastSchedules.length">위로 스크롤하면 지난 일정도 볼 수 있어요</p>
        </div>
        <button
          v-if="pastSchedules.length"
          type="button"
          @click="showPastSchedules"
        >
          지난 {{ pastSchedules.length }}건 <span aria-hidden="true">↑</span>
        </button>
        <span v-else>총 {{ upcomingSchedules.length }}건</span>
      </div>
      <div ref="timelineList" class="upcoming-list">
        <div
          v-for="group in timelineGroups"
          :key="group.date"
          class="date-group"
          :class="{ past: group.isPast }"
          :data-upcoming-anchor="group.isPast ? null : 'true'"
        >
          <h3>
            {{ dateLabel(group.date) }}
            <span v-if="group.isPast">지난 일정</span>
          </h3>
          <ScheduleCard
            v-for="item in group.items"
            :key="item.id"
            :schedule="item"
            @detail="openDetail"
          />
        </div>
        <div v-if="!timelineGroups.length" class="empty-state">
          <span aria-hidden="true">🧭</span>
          <b>다가오는 여행 일정이 없어요</b>
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

    <Teleport to="body"
      ><Transition name="modal"
        ><div
          v-if="todayModalOpen"
          class="modal-wrap"
          role="dialog"
          aria-modal="true"
          aria-label="오늘 일정 전체 보기"
        >
          <button
            class="modal-backdrop"
            aria-label="닫기"
            @click="todayModalOpen = false"
          />
          <section class="today-modal">
            <header>
              <div>
                <small>{{ store.today.replaceAll('-', '.') }}</small>
                <h2>오늘 일정 전체</h2>
              </div>
              <button
                type="button"
                aria-label="닫기"
                @click="todayModalOpen = false"
              >
                ×
              </button>
            </header>
            <div class="modal-list">
              <ScheduleCard
                v-for="item in todaySchedules"
                :key="item.id"
                :schedule="item"
                @detail="openDetail"
              />
            </div>
          </section></div></Transition
    ></Teleport>
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
.all-link {
  padding: 11px 22px 18px;
  text-align: right;
  color: #dce9ff;
  font-size: 11px;
  font-weight: 800;
}
.all-link span {
  color: #ffd761;
  font-size: 17px;
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
  overscroll-behavior: contain;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: #c7d3e4 transparent;
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
.date-group.past h3 {
  color: #8b97a9;
}
.date-group.past :deep(.schedule-card) {
  border: 1px solid #edf0f4;
  background: #f6f7f9;
  box-shadow: none;
  opacity: 0.82;
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
.modal-wrap {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.modal-backdrop {
  position: absolute;
  inset: 0;
  background: #10182780;
}
.today-modal {
  position: relative;
  width: min(100%, 430px);
  max-height: 82vh;
  padding: 20px;
  border-radius: 24px 24px 0 0;
  background: #f4f5f9;
  box-shadow: 0 -10px 35px #1018272e;
}
.today-modal > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 2px 16px;
}
.today-modal small {
  color: #276ed6;
  font-size: 12px;
  font-weight: 700;
}
.today-modal h2 {
  margin-top: 4px;
  font-size: 21px;
  font-weight: 700;
}
.today-modal header > button {
  font-size: 28px;
  color: #657184;
}
.modal-list {
  display: grid;
  gap: 10px;
  max-height: 64vh;
  overflow: auto;
  padding-bottom: 20px;
}
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.today-preview-list {
  max-height: 224px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: #ffffff70 transparent;
}
.today-preview-list :deep(.schedule-card) {
  width: calc(100% - 36px);
  margin: 10px 18px;
}
.modal-list {
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: #9eabc0 transparent;
}
</style>
