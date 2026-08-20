<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CalendarDays,
  House,
  PiggyBank,
  ReceiptText,
  RefreshCw,
  UserRound,
  WalletCards,
} from '@lucide/vue'

import { useTravelModeStore } from '@/stores/travelMode'
import { useTravelStore } from '@/stores/travel'

const router = useRouter()
const route = useRoute()
const travelModeStore = useTravelModeStore()
const travelStore = useTravelStore()

const savingsNavItems = [
  { name: '홈', path: '/', icon: House },
  { name: '저축 미션', path: '/missions', icon: PiggyBank },
  { name: '월렛', path: '/wallet', icon: WalletCards },
  { name: '환율', path: '/exchange', icon: RefreshCw },
  { name: '마이페이지', path: '/mypage', icon: UserRound },
]

const travelNavItems = [
  { name: '홈', path: '/', icon: House },
  { name: '여행일정', path: '/schedule', icon: CalendarDays },
  { name: '월렛', path: '/wallet', icon: WalletCards },
  { name: '영수증', path: '/trips', icon: ReceiptText, action: 'receipt' },
  { name: '마이페이지', path: '/mypage', icon: UserRound },
]

const navItems = computed(() =>
  travelModeStore.isTravelMode ? travelNavItems : savingsNavItems,
)

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const activeIndex = computed(() => {
  const index = navItems.value.findIndex(item => isActive(item.path))
  return index >= 0 ? index : 0
})
const activeItem = computed(() => navItems.value[activeIndex.value])
const indicatorStyle = computed(() => ({
  left: `${((activeIndex.value + 0.5) / navItems.value.length) * 100}%`,
}))

async function openReceipt() {
  await travelStore.loadActiveGoal({ force: true })
  if (!travelStore.tripId) {
    window.alert('진행 중인 여행이 없어 영수증 보관함을 열 수 없습니다.')
    return
  }
  await router.push({ name: 'Receipt', params: { tripId: travelStore.tripId } })
}

async function handleNavigation(item) {
  if (item.action === 'receipt') {
    await openReceipt()
    return
  }
  await router.push(item.path)
}
</script>

<template>
  <nav class="bottom-nav" aria-label="주요 메뉴">
    <div class="nav-shell">
      <span class="moving-notch" :style="indicatorStyle" aria-hidden="true">
        <span class="notch-halo" />
        <span class="active-orb">
          <Transition name="icon-swap" mode="out-in">
            <component
                :is="activeItem.icon"
                :key="activeItem.name"
                :size="23"
                :stroke-width="2.35"
            />
          </Transition>
        </span>
      </span>

      <button
          v-for="item in navItems"
          :key="item.name"
          type="button"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          :aria-current="isActive(item.path) ? 'page' : undefined"
          @click="handleNavigation(item)"
      >
        <span class="nav-icon">
          <component :is="item.icon" :size="21" :stroke-width="1.9" />
        </span>
        <span class="nav-label">{{ item.name }}</span>
      </button>
    </div>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  z-index: 50;
  width: 100%;
  max-width: 390px;
  height: 88px;
  transform: translateX(-50%);
  pointer-events: none;
}
.nav-shell {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: grid;
  height: 68px;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: center;
  padding: 5px 7px max(4px, env(safe-area-inset-bottom));
  border-radius: 25px 25px 0 0;
  background: linear-gradient(105deg, #0c2f73 0%, #174b9c 55%, #1e5eb8 100%);
  box-shadow: 0 -8px 24px rgba(12, 47, 115, .22);
  pointer-events: auto;
}
.moving-notch {
  position: absolute;
  top: -27px;
  z-index: 2;
  width: 72px;
  height: 62px;
  transform: translateX(-50%);
  transition: left .46s cubic-bezier(.22, .82, .2, 1);
  pointer-events: none;
}
.notch-halo {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 3px 0 rgba(223, 232, 246, .9);
}
.notch-halo::before,
.notch-halo::after {
  position: absolute;
  top: 27px;
  width: 18px;
  height: 18px;
  content: '';
}
.notch-halo::before {
  left: -13px;
  border-radius: 0 15px 0 0;
  box-shadow: 6px -6px 0 5px #fff;
}
.notch-halo::after {
  right: -13px;
  border-radius: 15px 0 0;
  box-shadow: -6px -6px 0 5px #fff;
}
.active-orb {
  position: absolute;
  top: 6px;
  left: 50%;
  z-index: 2;
  display: grid;
  width: 50px;
  height: 50px;
  place-items: center;
  transform: translateX(-50%);
  border-radius: 50%;
  background: linear-gradient(145deg, #0c2f73, #1e5eb8);
  color: #ffd45c;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, .14),
    0 8px 17px rgba(8, 34, 84, .3);
}
.nav-item {
  position: relative;
  z-index: 3;
  display: flex;
  min-width: 0;
  height: 58px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: rgba(255, 255, 255, .7);
  -webkit-tap-highlight-color: transparent;
}
.nav-icon {
  display: grid;
  width: 27px;
  height: 24px;
  place-items: center;
  transition: opacity .2s ease, transform .25s ease;
}
.nav-label {
  overflow: hidden;
  max-width: 100%;
  color: inherit;
  font-size: 9px;
  font-weight: 700;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color .25s ease, transform .35s cubic-bezier(.22, .82, .2, 1);
}
.nav-item.active .nav-icon {
  opacity: 0;
  transform: translateY(-9px) scale(.72);
}
.nav-item.active .nav-label {
  color: #fff;
  font-weight: 900;
  transform: translateY(8px);
}
.nav-item:active:not(.active) .nav-icon {
  transform: scale(.86);
}
.nav-item:focus-visible {
  outline: none;
}
.nav-item:focus-visible::after {
  position: absolute;
  inset: 5px;
  border: 2px solid rgba(255, 212, 92, .75);
  border-radius: 14px;
  content: '';
}
.icon-swap-enter-active,
.icon-swap-leave-active {
  transition: opacity .13s ease, transform .16s ease;
}
.icon-swap-enter-from { opacity: 0; transform: scale(.55) rotate(-15deg); }
.icon-swap-leave-to { opacity: 0; transform: scale(.55) rotate(15deg); }
@media (prefers-reduced-motion: reduce) {
  .moving-notch,
  .nav-icon,
  .nav-label,
  .icon-swap-enter-active,
  .icon-swap-leave-active {
    transition: none;
  }
}
</style>
