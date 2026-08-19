<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import ScheduleCard from '@/components/schedule/ScheduleCard.vue';
import { useTravelScheduleStore } from '@/stores/travelSchedule';

const router = useRouter();
const store = useTravelScheduleStore();
const todayModalOpen = ref(false);

onMounted(() => {
  store.loadSchedules().catch(() => {});
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
const upcomingGroups = computed(() => {
  const groups = new Map();
  upcomingSchedules.value.forEach((item) => {
    if (!groups.has(item.date)) groups.set(item.date, []);
    groups.get(item.date).push(item);
  });
  return [...groups.entries()].map(([date, items]) => ({ date, items }));
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
        <b>오늘 일정</b><small>{{ store.today.replaceAll('-', '.') }}</small>
      </div>
      <div class="cut"><i /><span /><i /></div>
      <p>오늘 일정이 {{ todaySchedules.length }}건 있어요.</p>
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
      <div class="cut bottom"><i /><span /><i /></div>
      <div class="barcode">||||||||||||||||||||</div>
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
        <h2>다가오는 여행 일정</h2>
        <span>총 {{ upcomingSchedules.length }}건</span>
      </div>
      <div class="upcoming-list">
        <div
          v-for="group in upcomingGroups"
          :key="group.date"
          class="date-group"
        >
          <h3>{{ dateLabel(group.date) }}</h3>
          <ScheduleCard
            v-for="item in group.items"
            :key="item.id"
            :schedule="item"
            @detail="openDetail"
          />
        </div>
        <div v-if="!upcomingGroups.length" class="empty-state">
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
  display: block;
  width: 100%;
  margin-top: 18px;
  overflow: hidden;
  border-radius: 22px;
  background: linear-gradient(135deg, #103779, #1553a2);
  color: #fff;
  box-shadow: 0 10px 22px rgba(16, 41, 92, 0.22);
  text-align: left;
}
.ticket-head {
  display: flex;
  justify-content: space-between;
  padding: 18px 22px 15px;
}
.ticket-head b {
  font-size: 19px;
  font-weight: 700;
}
.ticket-head small {
  color: #ffffff99;
  font-size: 13px;
  font-weight: 500;
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
.today-ticket > p {
  padding: 16px 22px 10px;
  color: #dbe8ff;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
}
.today-ticket :deep(.schedule-card) {
  width: calc(100% - 36px);
  margin: 10px 18px;
}
.all-link {
  padding: 14px 22px 4px;
  text-align: right;
  color: #e1ecff;
  font-size: 13px;
  font-weight: 700;
}
.all-link span {
  font-size: 18px;
}
.cut.bottom {
  margin-top: 12px;
}
.barcode {
  height: 34px;
  padding: 9px 22px;
  text-align: right;
  letter-spacing: -1px;
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
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(16, 25, 43, 0.06);
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.section-title h2 {
  font-size: 20px;
  font-weight: 700;
}
.section-title span {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
}
.upcoming-list {
  max-height: 560px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: #c7d3e4 transparent;
}
.date-group h3 {
  margin: 26px 4px 12px;
  color: #245ec4;
  font-size: 15px;
  font-weight: 700;
}
.date-group:first-child h3 {
  margin-top: 18px;
}
.date-group :deep(.schedule-card) {
  margin-top: 10px;
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
