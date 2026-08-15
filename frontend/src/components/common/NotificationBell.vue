<script setup>
import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useMypageStore } from '@/stores/mypage';

const router = useRouter();
const store = useMypageStore();

let timer = null;

onMounted(() => {
  store.fetchNotifications();
  // 30초마다 알림 갱신 (실시간성 강화)
  timer = setInterval(() => {
    store.fetchNotifications();
  }, 30000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<template>
  <button
    class="bell"
    type="button"
    aria-label="알림함"
    @click="router.push('/notifications')"
  >
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
      <path
        d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M10 21h4"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
      />
    </svg>
    <Transition name="pulse">
      <i v-if="store.unreadCount" :key="store.unreadCount">
        {{ store.unreadCount > 99 ? '99+' : store.unreadCount }}
      </i>
    </Transition>
  </button>
</template>

<style scoped>
.bell {
  position: relative;
  display: grid;
  width: 40px;
  height: 40px;
  flex: none;
  place-items: center;
  border: 1.5px solid #f1f5f9;
  border-radius: 12px;
  background: #fff;
  color: #1e293b;
  transition: all 0.2s ease;
}

.bell:active {
  transform: scale(0.95);
  background: #f8fafc;
}

.bell i {
  position: absolute;
  top: -5px;
  right: -5px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border: 2px solid #fff;
  border-radius: 10px;
  background: #ef4444; /* 조금 더 선명한 빨간색 */
  color: #fff;
  font-size: 10px;
  font-style: normal;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3);
}

/* 펄스 애니메이션 */
.pulse-enter-active {
  animation: pulse-in 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes pulse-in {
  0% {
    transform: scale(0.5);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
