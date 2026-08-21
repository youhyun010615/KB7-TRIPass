<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import { useMypageStore } from '@/stores/mypage';
import alertIcon from '@/assets/icons/alert.svg';
import notificationIcon from '@/assets/icons/mingcute_notification-fill.svg';
const router = useRouter(),
  store = useMypageStore();
const meta = {
  schedule: ['▣', '#e7f1ff'],
  finance: ['₩', '#e8f8f2'],
  saving: ['✈', '#fff3e7'],
  exchange: ['↕', '#f0eaff'],
  checklist: ['✓', '#e8f5ff'],
  test: ['🧪', '#f1f5f9'],
  report: ['📊', '#fff0f0'],
};
onMounted(() => {
  store.fetchNotifications();
});
function open(item) {
  store.markRead(item.id);
  if (item.url) {
    router.push(item.url);
  }
}
</script>
<template>
  <main class="page">
    <header>
      <button @click="router.back()">‹</button>
      <h1>알림<img :src="notificationIcon" alt="" /></h1>
      <button @click="store.markAllRead">전체 읽음</button>
    </header>
    <div class="title">
      <h2>최근 알림</h2>
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
            @click.stop="open(item)"
          >
            이동 ›
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
.page h1 img { width:21px;height:21px;object-fit:contain; }
.title {
  display: flex;
  justify-content: space-between;
  margin: 20px 2px 10px;
}
.title h2 {
  font-size: 16px;
  font-weight: 900;
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
  color: #173f8d;
  font-size: 10px;
  font-weight: 800;
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
</style>
