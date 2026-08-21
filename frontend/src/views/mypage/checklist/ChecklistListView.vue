<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import { useChecklistStore } from '@/stores/checklist';

const route = useRoute();
const router = useRouter();
const store = useChecklistStore();

// 1. URL Query에서 tripId 추출 (기본값 없음)
const tripId = computed(() =>
  route.query.tripId ? Number(route.query.tripId) : null,
);

// [MODIFIED] ChecklistStore에서 tripDday를 직접 가져오기
const dDayLabel = computed(() => {
  const days = store.tripDday; // 스토어의 반응형 변수 참조

  if (days === undefined || days === null) return '...'; // 로딩 중일 때 표시
  if (days === 0) return 'D-DAY';
  if (days > 0) return `D-${days}`;
  return `D+${Math.abs(days)}`;
});

// 2. 컴포넌트 마운트 시 체크리스트 요약 API 호출
onMounted(async () => {
  if (tripId.value) {
    // 순서대로 호출
    await Promise.all([
      store.loadSummary(tripId.value),
      store.fetchTripDday(tripId.value)
    ]);
  } else {
    console.error('tripId가 없습니다.');
    router.back();
  }
});

// tripId가 변경될 때마다 요약 정보 재조회
watch(tripId, (newTripId) => {
  if (newTripId) {
    store.loadSummary(newTripId);
  }
});

// 3. 백엔드 Summary 데이터 계산 프로퍼티
const summary = computed(() => store.summary);

// 전체 진행률 계산
const totalProgress = computed(() => {
  const total = summary.value.totalItemCount || 0;
  const done = summary.value.totalCompletedCount || 0;
  const percent = total > 0 ? Math.round((done / total) * 100) : 0;
  return { done, total, percent };
});

// 여행 준비 진행률
const prepProgress = computed(() => ({
  done: summary.value.prepCompletedCount || 0,
  total: summary.value.prepItemCount || 0,
}));

// 귀국 진행률
const returnProgress = computed(() => ({
  done: summary.value.returnCompletedCount || 0,
  total: summary.value.returnItemCount || 0,
}));
</script>

<template>
  <main class="checklist-page">
    <header>
      <button type="button" @click="router.back()">‹</button>
      <h1>체크리스트</h1>
      <span />
    </header>

    <!-- 티켓 스타일 요약 카드 -->
    <section class="ticket">
      <small>TRIP CHECKLIST PASS</small>
      <div>
        <h2>내 여행 체크리스트</h2>
        <b>{{ dDayLabel }}</b>
      </div>
      <i />
      <p>전체 완료율</p>
      <div class="total">
        <strong>{{ totalProgress.done }} / {{ totalProgress.total }}</strong>
        <em>{{ totalProgress.percent }}% 완료</em>
      </div>
      <div class="bar">
        <span :style="{ width: `${totalProgress.percent}%` }" />
      </div>
    </section>

    <h3>체크리스트 목록</h3>

    <!-- 1. 여행 준비 체크리스트 버튼 -->
    <button
      class="menu-card"
      @click="router.push(`/mypage/checklists/preparation?tripId=${tripId}`)"
    >
      <span class="menu-icon preparation">✓</span>
      <span class="copy">
        <b>여행 준비 체크리스트</b>
        <small>D-30 · D-7 · D-1 준비 항목</small>
      </span>
      <em>{{ prepProgress.done }}/{{ prepProgress.total }}</em>
      <strong>›</strong>
    </button>

    <!-- 2. 귀국 체크리스트 버튼 -->
    <button
      class="menu-card"
      @click="router.push(`/mypage/checklists/return?tripId=${tripId}`)"
    >
      <span class="menu-icon returning">↩</span>
      <span class="copy">
        <b>귀국 체크리스트</b>
        <small>귀국일 점검 및 정리 항목</small>
      </span>
      <em
        :class="{
          scheduled: returnProgress.total === 0 || returnProgress.done === 0,
        }"
      >
        {{
          returnProgress.total > 0
            ? `${returnProgress.done}/${returnProgress.total}`
            : '예정'
        }}
      </em>
      <strong>›</strong>
    </button>

    <aside>
      <span>✈️</span>
      <span>
        <b>완료하지 못한 준비 항목은 다음 단계로 이월돼요</b>
        <small>체크리스트는 직접 추가할 수도 있어요.</small>
      </span>
    </aside>

    <BottomNav />
  </main>
</template>

<style scoped>
.checklist-page {
  min-height: 100vh;
  padding: 0 18px 96px;
  background: #f8f6f1;
  color: #111a2d;
}
.checklist-page > header {
  display: grid;
  height: 68px;
  grid-template-columns: 40px 1fr 40px;
  align-items: end;
  padding-bottom: 18px;
}
.checklist-page > header button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  background: #fff;
  color: #193d82;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 5px 16px rgba(36, 72, 117, 0.07);
}
.checklist-page > header h1 {
  text-align: center;
  font-size: 20px;
  font-weight: 900;
}
.ticket {
  position: relative;
  padding: 22px 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, #17397f, #102b66);
  color: #fff;
  box-shadow: 0 12px 24px #19386d24;
}
.ticket::before,
.ticket::after {
  position: absolute;
  top: 48%;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f8f6f1;
  content: '';
}
.ticket::before {
  left: -9px;
}
.ticket::after {
  right: -9px;
}
.ticket small {
  color: #c7d7f6;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.08em;
}
.ticket > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ticket h2 {
  margin-top: 18px;
  font-size: 17px;
}
.ticket > div > b {
  margin-top: 18px;
  color: #ffb21c;
  font-size: 13px;
}
.ticket i {
  display: block;
  margin: 18px 0 14px;
  border-top: 1px dashed #8fa9d5;
}
.ticket p {
  color: #b9cae7;
  font-size: 10px;
}
.ticket .total {
  margin-top: 7px;
}
.ticket .total strong {
  font-size: 22px;
}
.ticket .total em {
  color: #58c9ff;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}
.bar {
  height: 5px;
  margin-top: 13px;
  border-radius: 8px;
  background: #ffffff24;
}
.bar span {
  display: block;
  height: 100%;
  border-radius: 8px;
  background: linear-gradient(90deg, #45d7ff, #1e8bff);
  transition: width 0.3s ease;
}
.checklist-page > h3 {
  margin: 25px 2px 14px;
  font-size: 17px;
}
.menu-card {
  display: grid;
  width: 100%;
  grid-template-columns: 50px 1fr auto 12px;
  gap: 13px;
  align-items: center;
  margin-bottom: 12px;
  padding: 20px 16px;
  border: 1px solid #e4e8ef;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 18px #1727490c;
  text-align: left;
}
.menu-icon {
  display: grid;
  width: 48px;
  height: 48px;
  border-radius: 15px;
  place-items: center;
  font-size: 23px;
  font-weight: 900;
}
.preparation {
  background: #eaf3ff;
  color: #0767e9;
}
.returning {
  background: #e9faf4;
  color: #13a17c;
}
.copy b,
.copy small {
  display: block;
}
.copy b {
  font-size: 14px;
}
.copy small {
  margin-top: 6px;
  color: #7f8da2;
  font-size: 10px;
}
.menu-card em {
  padding: 7px 11px;
  border-radius: 15px;
  background: #e9f3ff;
  color: #0869eb;
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
}
.menu-card em.scheduled {
  background: #e8faf4;
  color: #10a17c;
}
.menu-card > strong {
  color: #7d8999;
  font-size: 24px;
}
aside {
  display: flex;
  gap: 13px;
  margin-top: 22px;
  padding: 17px;
  border-radius: 16px;
  background: #e6f1ff;
}
aside b,
aside small {
  display: block;
}
aside b {
  color: #143879;
  font-size: 11px;
}
aside small {
  margin-top: 7px;
  color: #71839f;
  font-size: 9px;
}
</style>
<style scoped>
.checklist-page{background:#f3f6fc}.ticket{background:linear-gradient(145deg,#2662ea 0%,#173f8d 100%);box-shadow:0 16px 34px rgba(23,63,141,.2)}.ticket::before,.ticket::after{background:#f3f6fc}.ticket>div>b,.ticket .total em{color:#ffd45e}.bar span{background:linear-gradient(90deg,#ffd45e,#ffbe3d)}.menu-card{border-color:#dfe7f4;box-shadow:0 8px 22px rgba(23,63,141,.06)}.menu-icon{background:#e9f0ff!important;color:#2662ea!important}aside{background:#eaf1ff;color:#173f8d}
.ticket{padding:18px;border-radius:18px;box-shadow:0 10px 24px rgba(23,63,141,.14)}.ticket h2,.ticket>div>b{margin-top:14px}.ticket i{margin:14px 0 11px}.bar{margin-top:10px}.checklist-page>h3{margin:20px 2px 11px}.menu-card{grid-template-columns:40px 1fr auto 10px;gap:10px;margin-bottom:10px;padding:14px;border-radius:16px;box-shadow:0 6px 16px rgba(23,63,141,.05)}.menu-icon{width:38px;height:38px;border-radius:12px;font-size:18px}.copy small{margin-top:4px}.menu-card em{padding:6px 9px}.menu-card>strong{font-size:20px}aside{gap:9px;margin-top:17px;padding:13px;border:1px solid #d8e4f8;border-radius:14px}
</style>
