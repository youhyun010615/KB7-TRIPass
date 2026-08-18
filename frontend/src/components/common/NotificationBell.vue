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
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
      <path
        d="M9.99936 2.5C8.50751 2.5 7.07677 3.09263 6.02188 4.14752C4.96699 5.20242 4.37436 6.63316 4.37436 8.125V11.1263L3.16936 14.1437C3.13161 14.2385 3.11764 14.3411 3.12865 14.4425C3.13967 14.5439 3.17533 14.6411 3.23254 14.7255C3.28975 14.81 3.36675 14.8791 3.45684 14.927C3.54692 14.9748 3.64735 14.9999 3.74936 15H7.49936C7.49936 16.385 8.61436 17.5 9.99936 17.5C11.3844 17.5 12.4994 16.385 12.4994 15H16.2494C16.3514 14.9999 16.4518 14.9748 16.5419 14.927C16.632 14.8791 16.709 14.81 16.7662 14.7255C16.8234 14.6411 16.859 14.5439 16.8701 14.4425C16.8811 14.3411 16.8671 14.2385 16.8294 14.1437L15.6244 11.125V8.125C15.6244 6.63316 15.0317 5.20242 13.9768 4.14752C12.9219 3.09263 11.4912 2.5 9.99936 2.5ZM11.2494 15C11.2494 15.695 10.6944 16.25 9.99936 16.25C9.30436 16.25 8.74936 15.695 8.74936 15H11.2494ZM5.62436 8.125C5.62436 6.96468 6.08529 5.85188 6.90576 5.03141C7.72623 4.21094 8.83903 3.75 9.99936 3.75C11.1597 3.75 12.2725 4.21094 13.0929 5.03141C13.9134 5.85188 14.3744 6.96468 14.3744 8.125V11.2475C14.3746 11.3267 14.3898 11.4052 14.4194 11.4787L15.3269 13.75H4.67186L5.57936 11.4787C5.60888 11.4052 5.62415 11.3267 5.62436 11.2475V8.125Z"
        fill="currentColor"
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
