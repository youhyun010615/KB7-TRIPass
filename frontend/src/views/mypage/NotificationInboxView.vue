<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import { useMypageStore } from '@/stores/mypage';
import { resetAccount as resetAccountApi, setOverrideDate, clearOverrideDate } from '@/api/mypage';
import { effectiveDate, initDevDate, isOverridden, realDate } from '@/utils/devDate';
import alertIcon from '@/assets/icons/alert.svg';
import notificationIcon from '@/assets/icons/mingcute_notification-fill.svg';

const router = useRouter(),
  store = useMypageStore();

const overrideDate = ref('');
const isSettingDate = ref(false);
const isResetting = ref(false);

const meta = {
  schedule: ['▣', '#e7f1ff'],
  finance: ['₩', '#e8f8f2'],
  saving: ['✈', '#fff3e7'],
  exchange: ['↕', '#f0eaff'],
  checklist: ['✓', '#e8f5ff'],
  test: ['🧪', '#f1f5f9'],
  report: ['📊', '#fff0f0'],
};

onMounted(async () => {
  store.fetchNotifications();
  await initDevDate();
  if (isOverridden.value) overrideDate.value = effectiveDate.value;
});

function open(item) {
  store.markRead(item.id);
  if (item.url) {
    router.push(item.url);
  }
}

async function applyOverrideDate() {
  if (!overrideDate.value || isSettingDate.value) return;
  isSettingDate.value = true;
  try {
    await setOverrideDate(overrideDate.value);
    await initDevDate();
    window.alert(`가상 날짜가 ${overrideDate.value}로 설정되었습니다.`);
  } catch (e) {
    window.alert('설정 실패: ' + (e.response?.data?.message || e.message));
  } finally { isSettingDate.value = false; }
}

async function removeOverrideDate() {
  isSettingDate.value = true;
  try {
    await clearOverrideDate();
    overrideDate.value = '';
    await initDevDate();
    window.alert('가상 날짜가 해제되었습니다.');
  } catch (e) {
    window.alert('해제 실패: ' + (e.response?.data?.message || e.message));
  } finally { isSettingDate.value = false; }
}

async function resetAccount() {
  if (isResetting.value) return;
  const confirmed = window.confirm('계정의 모든 데이터(여행, 계좌, 카드, 거래내역, 미션 등)가 삭제됩니다.\n\n정말 초기화할까요?');
  if (!confirmed) return;
  isResetting.value = true;
  try {
    await resetAccountApi();
    window.alert('계정 데이터가 초기화되었습니다.');
    window.location.reload();
  } catch (error) {
    window.alert('초기화에 실패했습니다: ' + (error.response?.data?.message || error.message));
  } finally {
    isResetting.value = false;
  }
}
</script>
<template>
  <main class="page">
    <header>
      <button @click="router.back()">‹</button>
      <h1>알림</h1>
      <button @click="store.markAllRead">전체 읽음</button>
    </header>
    <div class="title">
      <h2><img :src="notificationIcon" alt="" />최근 알림 내역</h2>
    </div>
    <section class="list">
      <div
        v-for="item in store.notifications"
        :key="item.id"
        class="item"
        :class="{ read: item.read }"
        @click="store.markRead(item.id)"
      >
        <i :style="`background:${meta[item.type]?.[1]}`">{{
          meta[item.type]?.[0]
        }}</i
        >
        <span class="content">
          <small>{{ item.time }}</small>
          <b>{{ item.title }}</b>
          <em>{{ item.message }}</em>
        </span>
        <div class="actions">
          <strong v-if="!item.read" class="dot" />
          <button
            v-if="item.url"
            class="go-btn"
            aria-label="알림 상세로 이동"
            @click.stop="open(item)"
          >
            ›
          </button>
        </div>
      </div>
      <div v-if="!store.notifications.length" class="empty">
        <div class="alert-visual" aria-hidden="true">
          <span class="pulse-ring pulse-ring-one"></span>
          <span class="pulse-ring pulse-ring-two"></span>
          <span class="icon-bubble">
            <img :src="alertIcon" alt="" />
            <i></i>
          </span>
        </div>
        <strong>새로운 알림이 없어요</strong>
        <p>새로운 소식이 도착하면<br />이곳에서 바로 알려드릴게요.</p>
      </div>
    </section>

    <section class="dev-section">
      <div class="dev-date-section">
        <h3>가상 날짜 설정 <span class="dev-badge">DEV</span></h3>
        <p class="dev-date-desc">비즈니스 로직에만 적용됩니다 (환율·Codef 등 외부 API 무관)</p>
        <div v-if="isOverridden" class="dev-date-active">
          현재 적용: <strong>{{ effectiveDate }}</strong>
          <small>(실제: {{ realDate }})</small>
        </div>
        <div class="dev-date-controls">
          <input type="date" v-model="overrideDate" class="dev-date-input" />
          <button type="button" class="dev-date-btn apply" :disabled="!overrideDate || isSettingDate" @click="applyOverrideDate">적용</button>
          <button type="button" class="dev-date-btn clear" :disabled="isSettingDate || !isOverridden" @click="removeOverrideDate">해제</button>
        </div>
      </div>
      <button type="button" class="reset-button" :disabled="isResetting" @click="resetAccount">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 12a8 8 0 0 1 14.25-5M20 12a8 8 0 0 1-14.25 5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M20 3v4h-4M4 21v-4h4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
        {{ isResetting ? '초기화 중...' : '계정 데이터 초기화' }}
      </button>
    </section>

    <BottomNav />
  </main>
</template>
<style scoped>
.page {
  min-height: 100vh;
  padding: 0 20px 105px;
  background: #eef2f8;
  color: #10192d;
}
.page > header {
  display: grid;
  height: 66px;
  grid-template-columns: 36px 1fr auto;
  align-items: end;
  gap: 8px;
  padding-bottom: 16px;
}
.page > header button:first-child {
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
.page > header button:last-child {
  color: #286ce0;
  font-size: 10px;
  font-weight: 800;
  text-align: right;
  white-space: nowrap;
}
.page h1 {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  text-align: center;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: -0.03em;
}
.title {
  display: flex;
  justify-content: space-between;
  margin: 20px 2px 10px;
}
.title h2 {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 16px;
  font-weight: 900;
}
.title h2 img {
  width: 19px;
  height: 19px;
  object-fit: contain;
  filter: invert(23%) sepia(77%) saturate(1471%) hue-rotate(196deg)
    brightness(86%) contrast(91%);
}
.list {
  overflow: hidden;
  border: 1px solid #e7edf9;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(16, 25, 43, 0.05);
}
.item {
  position: relative;
  display: grid;
  width: 100%;
  grid-template-columns: 42px 1fr auto;
  gap: 11px;
  padding: 15px;
  border-bottom: 1px solid #eef1f6;
  background: #f4f8ff;
  cursor: pointer;
  transition: background 0.2s;
}
.item.read {
  background: #fff;
}
.item i {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 13px;
  color: #173f8d;
  font-style: normal;
  font-weight: 900;
}
.content > * {
  display: block;
}
.content small {
  color: #94a3b8;
  font-size: 8px;
}
.content b {
  margin-top: 5px;
  color: #10192d;
  font-size: 12px;
}
.content em {
  margin-top: 5px;
  color: #73829d;
  font-size: 9px;
  font-style: normal;
  line-height: 1.45;
}
.actions {
  display: flex;
  align-items: center;
  justify-content: center;
}
.dot {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ff6b35;
}
.go-btn {
  display: grid;
  width: 28px;
  height: 38px;
  place-items: center;
  color: #173f8d;
  font-size: 27px;
  font-weight: 500;
  line-height: 1;
}
.go-btn:active {
  opacity: 0.6;
}
.empty {
  padding: 42px 22px 38px;
  text-align: center;
}
.empty strong {
  display: block;
  margin-top: 24px;
  color: #122342;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.03em;
}
.empty p {
  margin-top: 10px;
  color: #7788a4;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.7;
  letter-spacing: -0.025em;
}
.alert-visual {
  position: relative;
  display: grid;
  width: 94px;
  height: 94px;
  margin: 0 auto;
  place-items: center;
}
.icon-bubble {
  position: relative;
  z-index: 2;
  display: grid;
  width: 66px;
  height: 66px;
  place-items: center;
  animation: bell-float 2.4s ease-in-out infinite;
}
.icon-bubble img {
  width: 30px;
  height: 30px;
  filter: invert(23%) sepia(77%) saturate(1471%) hue-rotate(196deg)
    brightness(86%) contrast(91%);
}
.icon-bubble i {
  position: absolute;
  top: 11px;
  right: 11px;
  width: 8px;
  height: 8px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #ffd45f;
}
.pulse-ring {
  position: absolute;
  inset: 5px;
  border: 1px solid rgba(49, 104, 203, 0.28);
  border-radius: 50%;
  animation: alert-pulse 2.4s ease-out infinite;
}
.pulse-ring-two {
  animation-delay: 1.2s;
}
@keyframes bell-float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-5px) rotate(3deg); }
}
@keyframes alert-pulse {
  0% { opacity: 0; transform: scale(0.72); }
  25% { opacity: 0.8; }
  100% { opacity: 0; transform: scale(1.12); }
}
@media (prefers-reduced-motion: reduce) {
  .icon-bubble,
  .pulse-ring { animation: none; }
}

/* Dev controls */
.dev-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.dev-date-section {
  background: #fff;
  border: 2px dashed #f59e0b;
  border-radius: 16px;
  padding: 16px;
}
.dev-date-section h3 {
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 6px;
}
.dev-badge {
  font-size: 10px;
  font-weight: 700;
  background: #f59e0b;
  color: #fff;
  padding: 1px 6px;
  border-radius: 6px;
}
.dev-date-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}
.dev-date-active {
  margin-top: 10px;
  padding: 8px 12px;
  background: #fef3c7;
  border-radius: 10px;
  font-size: 13px;
  color: #92400e;
}
.dev-date-active strong { font-weight: 800; }
.dev-date-active small { display: block; font-size: 11px; color: #b45309; margin-top: 2px; }
.dev-date-controls {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  align-items: center;
}
.dev-date-input {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 13px;
  color: #1e293b;
  background: #f8fafc;
}
.dev-date-btn {
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  border: none;
  cursor: pointer;
}
.dev-date-btn.apply { background: #2563eb; color: #fff; }
.dev-date-btn.apply:disabled { background: #94a3b8; }
.dev-date-btn.clear { background: #fee2e2; color: #dc2626; }
.dev-date-btn.clear:disabled { background: #f1f5f9; color: #cbd5e1; }
.reset-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  min-height: 48px;
  border: 1px solid #dde3ed;
  border-radius: 16px;
  background: #fff;
  color: #6b7a90;
  font-size: 12px;
  font-weight: 800;
  box-shadow: 0 5px 14px rgba(16,25,43,.04);
}
.reset-button:active { background: #f5f7fb; }
.reset-button:disabled { opacity: .55; }
.reset-button svg { width: 17px; height: 17px; }
</style>
