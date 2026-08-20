<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTravelModeStore } from '@/stores/travelMode'
import { useTravelStore } from '@/stores/travel'

const router = useRouter()
const route = useRoute()
const travelModeStore = useTravelModeStore()
const travelStore = useTravelStore()

const savingsNavItems = [
  { name: '홈', path: '/', icon: 'home' },
  { name: '저축 미션', path: '/missions', icon: 'mission' },
  { name: '월렛', path: '/wallet', icon: 'wallet' },
  { name: '환율', path: '/exchange', icon: 'exchange' },
  { name: '마이페이지', path: '/mypage', icon: 'mypage' },
]

const travelNavItems = [
  { name: '홈', path: '/', icon: 'home' },
  { name: '여행일정', path: '/schedule', icon: 'schedule' },
  { name: '월렛', path: '/wallet', icon: 'wallet' },
  { name: '영수증', path: '/trips', icon: 'receipt', action: 'receipt' },
  { name: '마이페이지', path: '/mypage', icon: 'mypage' },
]

const navItems = computed(() =>
  travelModeStore.isTravelMode ? travelNavItems : savingsNavItems
)

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
async function openReceipt() {
  await travelStore.loadActiveGoal({ force: true })

  if (!travelStore.tripId) {
    window.alert('진행 중인 여행이 없어 영수증 보관함을 열 수 없습니다.')
    return
  }

  await router.push({
    name: 'Receipt',
    params: {
      tripId: travelStore.tripId,
    },
  })
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
    <div class="nav-surface">
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
      <!-- home -->
      <svg v-if="item.icon === 'home'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="M3 9L12 3L21 9V20C21 20.55 20.55 21 20 21H15V15H9V21H4C3.45 21 3 20.55 3 20V9Z"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <svg v-if="item.icon === 'mission'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="M11.5 3C12.4831 2.99974 13.4393 3.32145 14.2223 3.91593C15.0053 4.51042 15.5721 5.34497 15.836 6.292L15.888 6.497L17.758 6.03C17.8947 5.99598 18.037 5.99122 18.1757 6.01602C18.3143 6.04083 18.4462 6.09465 18.5626 6.17395C18.679 6.25326 18.7773 6.35626 18.8511 6.47621C18.925 6.59616 18.9726 6.73035 18.991 6.87L19 7V8.81C19.564 9.35029 20.0261 9.98781 20.364 10.692L20.502 11H21C21.2449 11 21.4813 11.09 21.6644 11.2527C21.8474 11.4155 21.9643 11.6397 21.993 11.883L22 12V15C22 15.1646 21.9594 15.3266 21.8818 15.4718C21.8042 15.6169 21.6919 15.7407 21.555 15.832L21.447 15.894L20.279 16.479C19.8017 17.4046 19.1089 18.202 18.259 18.804L18 18.978V20C18 20.2449 17.91 20.4813 17.7473 20.6644C17.5845 20.8474 17.3603 20.9643 17.117 20.993L17 21H14C13.7551 21 13.5187 20.91 13.3356 20.7473C13.1526 20.5845 13.0357 20.3603 13.007 20.117L13 20H12C12 20.2449 11.91 20.4813 11.7473 20.6644C11.5845 20.8474 11.3603 20.9643 11.117 20.993L11 21H8C7.75507 21 7.51866 20.91 7.33563 20.7473C7.15259 20.5845 7.03566 20.3603 7.007 20.117L7 20V18.978C6.27729 18.5165 5.65304 17.9165 5.16322 17.2127C4.6734 16.5088 4.32769 15.715 4.146 14.877C3.55618 14.7018 3.03451 14.3494 2.65171 13.8677C2.2689 13.3859 2.04348 12.7982 2.006 12.184L2 12V11.5C2.00028 11.2451 2.09788 11 2.27285 10.8146C2.44782 10.6293 2.68695 10.5178 2.94139 10.5028C3.19584 10.4879 3.44638 10.5707 3.64183 10.7343C3.83729 10.8979 3.9629 11.1299 3.993 11.383L4 11.5V12C4.00133 12.1493 4.03133 12.2877 4.09 12.415C4.24239 11.5146 4.58279 10.6565 5.08901 9.89646C5.59523 9.13644 6.25591 8.49161 7.028 8.004C6.95705 7.37448 7.01981 6.73708 7.21219 6.1335C7.40456 5.52991 7.72221 4.97374 8.14435 4.50138C8.56649 4.02903 9.0836 3.65112 9.66187 3.3924C10.2401 3.13367 10.8665 2.99996 11.5 3ZM17 8.28L14.242 8.97L14.122 8.993L14 9H10.5C9.48887 8.99933 8.50698 9.33922 7.71265 9.96486C6.91832 10.5905 6.35785 11.4654 6.12159 12.4486C5.88534 13.4317 5.98708 14.4658 6.4104 15.384C6.83373 16.3022 7.55397 17.0512 8.455 17.51C8.60017 17.5842 8.725 17.6928 8.81859 17.8263C8.91219 17.9598 8.97172 18.1142 8.992 18.276L9 18.4V19H10C10 18.7551 10.09 18.5187 10.2527 18.3356C10.4155 18.1526 10.6397 18.0357 10.883 18.007L11 18H14C14.2449 18 14.4813 18.09 14.6644 18.2527C14.8474 18.4155 14.9643 18.6397 14.993 18.883L15 19H16V18.4C16.0001 18.2159 16.051 18.0353 16.1472 17.8783C16.2434 17.7212 16.381 17.5938 16.545 17.51C17.4628 17.0404 18.1924 16.2712 18.613 15.33C18.688 15.1617 18.8081 15.0174 18.96 14.913L19.079 14.843L20 14.382V13H19.793C19.5753 12.9999 19.3635 12.9288 19.1899 12.7974C19.0162 12.666 18.8902 12.4815 18.831 12.272C18.5811 11.3922 18.069 10.6094 17.363 10.028C17.1672 9.86621 17.0402 9.63596 17.008 9.384L17 9.257V8.28ZM16 11C16.2652 11 16.5196 11.1054 16.7071 11.2929C16.8946 11.4804 17 11.7348 17 12C17 12.2652 16.8946 12.5196 16.7071 12.7071C16.5196 12.8946 16.2652 13 16 13C15.7348 13 15.4804 12.8946 15.2929 12.7071C15.1054 12.5196 15 12.2652 15 12C15 11.7348 15.1054 11.4804 15.2929 11.2929C15.4804 11.1054 15.7348 11 16 11ZM11.5 5C10.8943 5 10.3092 5.2199 9.85341 5.61884C9.39763 6.01778 9.1022 6.56862 9.022 7.169C9.50658 7.05629 10.0025 6.99958 10.5 7H13.877L13.947 6.983C13.8284 6.42196 13.5206 5.91874 13.075 5.5577C12.6295 5.19666 12.0734 4.99976 11.5 5Z"
          :fill="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'"/>
      </svg>
      <svg v-if="item.icon === 'wallet'" width="20" height="20" viewBox="0 0 25 25" fill="none">
        <rect x="2.25" y="4.75" width="17.5" height="12.5" rx="1.25" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.5"/>
        <rect x="2.5" y="8" width="17" height="2" rx="1" :fill="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'"/>
      </svg>
      <!-- exchange (환율) -->
      <svg v-if="item.icon === 'exchange'" width="20" height="20" viewBox="0 0 26 26" fill="none">
        <path d="M6.27792 15.4996L4.35825 13.5767L2.4375 15.4996M19.7221 10.5391L21.6417 12.4631L23.5625 10.5391"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M21.6409 12.4625C21.6409 10.1658 20.7309 7.96556 19.1103 6.34164C18.3083 5.53809 17.3558 4.90058 16.3072 4.46559C15.2585 4.0306 14.1344 3.80668 12.9992 3.80664C11.8964 3.80577 10.8039 4.01802 9.78167 4.43173C8.64055 4.88925 7.61014 5.58491 6.75921 6.47227C5.90828 7.35963 5.25641 8.41827 4.84709 9.57756M4.35742 13.5751C4.35909 15.5842 5.05872 17.5303 6.33667 19.0806C7.61323 20.629 9.3893 21.6844 11.3596 22.0654C13.3299 22.4463 15.3714 22.1291 17.1332 21.1681C18.8949 20.2052 20.2676 18.6606 21.0169 16.798"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M10.3789 15.1444C10.4353 15.7875 10.7412 16.383 11.2312 16.8033C11.7212 17.2237 12.3564 17.4355 13.0006 17.3934C15.1152 17.3934 15.6222 16.1768 15.6222 15.1444C15.6222 14.112 14.748 13.0178 13.0006 13.0178C11.2532 13.0178 10.3789 12.3266 10.3789 10.9183C10.4032 10.4303 10.5755 9.96123 10.8729 9.57358C11.1704 9.18594 11.5789 8.8981 12.044 8.74838C12.3527 8.64871 12.6777 8.61296 13.0006 8.64329C13.6456 8.61526 14.2769 8.83547 14.7645 9.25863C15.2522 9.6818 15.5591 10.2757 15.6222 10.9183M13.0006 18.7042V17.5656M13.0006 7.32812V8.63896"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <!-- schedule (여행일정) -->
      <svg v-if="item.icon === 'schedule'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <rect x="3" y="4" width="18" height="18" rx="2" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8"/>
        <path d="M16 2V6M8 2V6M3 10H21" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
      <!-- asset (여행자금 체크 - 지출 분포 파이 차트) -->
      <svg v-if="item.icon === 'asset'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="M21.21 15.89A10 10 0 1 1 8 2.83" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M22 12A10 10 0 0 0 12 2V12H22Z" :fill="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'"/>
      </svg>
      <!-- receipt (영수증) -->
      <svg v-if="item.icon === 'receipt'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="M5 2H19C19.55 2 20 2.45 20 3V22L17.5 20.5L15 22L12.5 20.5L10 22L7.5 20.5L5 22V3C5 2.45 5.45 2 6 2Z"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
        <line x1="9" y1="8" x2="15" y2="8" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
        <line x1="9" y1="12" x2="15" y2="12" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
        <line x1="9" y1="16" x2="12" y2="16" :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
      <!-- mypage -->
      <svg v-if="item.icon === 'mypage'" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="8" r="4"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'"
          :fill="isActive(item.path) ? '#3B5BDB' : 'none'"
          stroke-width="1.8"/>
        <path d="M4 20C4 17.24 7.58 15 12 15C16.42 15 20 17.24 20 20"
          :stroke="isActive(item.path) ? '#3B5BDB' : '#9CA3AF'" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
      </span>
      <span class="nav-label">{{ item.name }}</span>
      </button>
    </div>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 50%;
  z-index: 50;
  width: 100%;
  max-width: 390px;
  height: 74px;
  transform: translateX(-50%);
  pointer-events: none;
}
.nav-surface {
  position: absolute;
  inset: 10px 0 0;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: center;
  padding: 0 6px env(safe-area-inset-bottom, 0);
  border-top: 1px solid #e8eef7;
  background: rgba(255, 255, 255, .97);
  box-shadow: 0 -9px 24px rgba(23, 63, 141, .08);
  pointer-events: auto;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}
.nav-item {
  position: relative;
  display: flex;
  min-width: 0;
  height: 64px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: #98a2b3;
  -webkit-tap-highlight-color: transparent;
}
.nav-icon {
  position: relative;
  z-index: 1;
  display: grid;
  width: 34px;
  height: 30px;
  place-items: center;
  border-radius: 50%;
  transition:
    width .34s cubic-bezier(.2, .8, .2, 1),
    height .34s cubic-bezier(.2, .8, .2, 1),
    transform .34s cubic-bezier(.2, .8, .2, 1),
    background-color .25s ease,
    box-shadow .34s ease;
}
.nav-icon::before {
  position: absolute;
  z-index: -1;
  inset: -7px;
  border: 1px solid transparent;
  border-radius: 50%;
  content: '';
  transition: .34s cubic-bezier(.2, .8, .2, 1);
}
.nav-label {
  overflow: hidden;
  max-width: 100%;
  color: #98a2b3;
  font-size: 10px;
  font-weight: 650;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color .25s ease, transform .34s cubic-bezier(.2, .8, .2, 1);
}
.nav-item.active .nav-icon {
  width: 46px;
  height: 46px;
  transform: translateY(-17px);
  background: linear-gradient(145deg, #123b86, #2869cf);
  box-shadow: 0 9px 20px rgba(28, 83, 172, .32);
}
.nav-item.active .nav-icon::before {
  inset: -6px;
  border-color: #e1ebfb;
  background: #fff;
  box-shadow: 0 -2px 0 #f3f6fb;
}
.nav-item.active .nav-icon svg {
  width: 22px;
  height: 22px;
}
.nav-item.active .nav-icon :deep([stroke]) {
  stroke: #fff;
}
.nav-item.active .nav-icon :deep([fill]:not([fill="none"])) {
  fill: #fff;
}
.nav-item.active .nav-label {
  color: #17499c;
  font-weight: 850;
  transform: translateY(-7px);
}
.nav-item:active .nav-icon {
  transform: scale(.92);
}
.nav-item.active:active .nav-icon {
  transform: translateY(-17px) scale(.92);
}
.nav-item:focus-visible {
  outline: none;
}
.nav-item:focus-visible .nav-icon {
  box-shadow: 0 0 0 3px #bfd4f7;
}
@media (prefers-reduced-motion: reduce) {
  .nav-icon,
  .nav-icon::before,
  .nav-label {
    transition: none;
  }
}
</style>
