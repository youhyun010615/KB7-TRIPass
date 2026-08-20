<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CalendarDays,
  House,
  PiggyBank,
  ReceiptText,
  UserRound,
  WalletCards,
} from '@lucide/vue'

import ExchangeRateIcon from '@/components/common/ExchangeRateIcon.vue'
import { useTravelModeStore } from '@/stores/travelMode'
import { getTravelCountryColors, useTravelStore } from '@/stores/travel'

const router = useRouter()
const route = useRoute()
const travelModeStore = useTravelModeStore()
const travelStore = useTravelStore()
const indicatorStorageKey = 'tripass-bottom-nav-index'
const initialIndicatorIndex = Number(window.sessionStorage.getItem(indicatorStorageKey))
const indicatorPosition = ref(
  Number.isInteger(initialIndicatorIndex)
    ? Math.min(4, Math.max(0, initialIndicatorIndex))
    : 0,
)
const indicatorIconIndex = ref(Math.round(indicatorPosition.value))
const pressAmount = ref(0)
const orbOpacity = ref(1)
const orbScale = ref(1)
const orbOffsetY = ref(0)
const indicatorMoving = ref(false)
let indicatorAnimationFrame = null
let firstFrame = null
let secondFrame = null

const savingsNavItems = [
  { name: '홈', path: '/', icon: House },
  { name: '저축 미션', path: '/missions', icon: PiggyBank },
  { name: '월렛', path: '/wallet', icon: WalletCards },
  { name: '환율', path: '/exchange', icon: ExchangeRateIcon },
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

const tripCountries = computed(() => {
  const dashboardCountries = travelStore.homeDashboard?.countries || []
  if (dashboardCountries.length) return dashboardCountries
  return travelStore.activeTrip?.countries || []
})

const selectedTripCountry = computed(() => {
  const selectedId = travelModeStore.isTravelMode
    ? travelModeStore.selectedDestination
    : travelStore.homeSelectedCountryId

  if (selectedId == null || selectedId === 'all') return null
  return tripCountries.value.find(
    country => String(country.tripCountryId) === String(selectedId),
  ) ?? null
})

function shadeHex(hex, amount) {
  const clean = String(hex || '').replace('#', '')
  if (!/^[0-9a-f]{6}$/i.test(clean)) return '#174b9c'
  const target = amount < 0 ? 0 : 255
  const ratio = Math.abs(amount)
  const channels = [0, 2, 4].map((offset) => {
    const value = Number.parseInt(clean.slice(offset, offset + 2), 16)
    return Math.round(value + ((target - value) * ratio))
      .toString(16)
      .padStart(2, '0')
  })
  return `#${channels.join('')}`
}

function colorLuminance(hex) {
  const clean = String(hex || '').replace('#', '')
  if (!/^[0-9a-f]{6}$/i.test(clean)) return 0
  const [red, green, blue] = [0, 2, 4].map(
    offset => Number.parseInt(clean.slice(offset, offset + 2), 16) / 255,
  )
  return (red * 0.2126) + (green * 0.7152) + (blue * 0.0722)
}

const navCountryName = computed(() => selectedTripCountry.value?.countryName || '')
const navTheme = computed(() => {
  const base = getTravelCountryColors(navCountryName.value).headerBg || '#174b9c'
  const isLight = colorLuminance(base) > 0.62
  return {
    start: shadeHex(base, -0.28),
    middle: shadeHex(base, -0.08),
    end: shadeHex(base, 0.14),
    icon: isLight ? '#17315f' : 'rgba(255,255,255,.76)',
    activeIcon: isLight ? '#17315f' : '#ffd45c',
  }
})
const navThemeKey = computed(() =>
  `${travelModeStore.mode}-${navCountryName.value || 'default'}`,
)
const navThemeStyle = computed(() => ({
  '--nav-icon-color': navTheme.value.icon,
  '--nav-active-icon-color': navTheme.value.activeIcon,
  '--nav-orb-color': navTheme.value.end,
}))

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const activeIndex = computed(() => {
  const index = navItems.value.findIndex(item => isActive(item.path))
  return index >= 0 ? index : 0
})
const indicatorItem = computed(() =>
  navItems.value[indicatorIconIndex.value] ?? navItems.value[activeIndex.value],
)
const indicatorStyle = computed(() => ({
  left: `${((indicatorPosition.value + 0.5) / navItems.value.length) * 100}%`,
  '--orb-opacity': orbOpacity.value,
  '--orb-scale': orbScale.value,
  '--orb-y': `${orbOffsetY.value}px`,
}))

function navIconStyle(index) {
  const distance = Math.abs(index - indicatorPosition.value)
  const influence = Math.max(0, 1 - (distance / 0.82))
  const opacity = Math.max(0, 1 - (influence * 1.35))

  return {
    '--icon-opacity': opacity,
    '--icon-scale': 1 - (influence * 0.24),
    '--icon-press-y': `${influence * 15}px`,
  }
}

const navSurfacePath = computed(() => {
  const center = ((indicatorPosition.value + 0.5) / navItems.value.length) * 390
  const depth = 29 + pressAmount.value * 6
  const left = center - 36
  const right = center + 36
  return [
    'M 0 22',
    'Q 0 0 24 0',
    `H ${left.toFixed(2)}`,
    `C ${(center - 25).toFixed(2)} 0 ${(center - 31).toFixed(2)} ${depth.toFixed(2)} ${center.toFixed(2)} ${depth.toFixed(2)}`,
    `C ${(center + 31).toFixed(2)} ${depth.toFixed(2)} ${(center + 25).toFixed(2)} 0 ${right.toFixed(2)} 0`,
    'H 366',
    'Q 390 0 390 22',
    'V 56',
    'Q 390 78 368 78',
    'H 22',
    'Q 0 78 0 56',
    'Z',
  ].join(' ')
})

function moveIndicator(index) {
  window.cancelAnimationFrame(indicatorAnimationFrame)
  const from = indicatorPosition.value
  const distance = index - from
  if (Math.abs(distance) < 0.001) {
    indicatorPosition.value = index
    indicatorIconIndex.value = index
    pressAmount.value = 0
    orbOpacity.value = 1
    orbScale.value = 1
    orbOffsetY.value = 0
    indicatorMoving.value = false
    return
  }

  const startedAt = window.performance.now()
  const duration = 680
  let iconChanged = false
  indicatorMoving.value = true

  const animate = (now) => {
    const progress = Math.min(1, (now - startedAt) / duration)
    const travelProgress = Math.min(1, Math.max(0, (progress - 0.24) / 0.44))
    const travelEase = travelProgress * travelProgress * (3 - 2 * travelProgress)

    indicatorPosition.value = from + distance * travelEase
    pressAmount.value = Math.sin(Math.PI * travelProgress)

    if (progress < 0.24) {
      const sinkProgress = progress / 0.24
      const sinkEase = sinkProgress * sinkProgress
      orbOpacity.value = 1 - sinkEase
      orbScale.value = 1 - (0.28 * sinkEase)
      orbOffsetY.value = 34 * sinkEase
    } else if (progress < 0.68) {
      orbOpacity.value = 0
      orbScale.value = 0.72
      orbOffsetY.value = 34
      if (!iconChanged && progress >= 0.46) {
        indicatorIconIndex.value = index
        iconChanged = true
      }
    } else {
      if (!iconChanged) {
        indicatorIconIndex.value = index
        iconChanged = true
      }
      const riseProgress = (progress - 0.68) / 0.32
      const riseEase = 1 - Math.pow(1 - riseProgress, 3)
      orbOpacity.value = riseProgress
      orbScale.value = 0.72 + (0.28 * riseEase)
      orbOffsetY.value = 34 * (1 - riseEase)
    }

    if (progress < 1) {
      indicatorAnimationFrame = window.requestAnimationFrame(animate)
      return
    }

    indicatorPosition.value = index
    indicatorIconIndex.value = index
    pressAmount.value = 0
    orbOpacity.value = 1
    orbScale.value = 1
    orbOffsetY.value = 0
    indicatorMoving.value = false
    window.sessionStorage.setItem(indicatorStorageKey, String(index))
  }

  indicatorAnimationFrame = window.requestAnimationFrame(animate)
}

async function openReceipt() {
  await travelStore.loadActiveGoal({ force: true })
  if (!travelStore.tripId) {
    window.alert('진행 중인 여행이 없어 영수증 보관함을 열 수 없습니다.')
    return
  }
  await router.push({ name: 'Receipt', params: { tripId: travelStore.tripId } })
}

async function handleNavigation(item) {
  window.sessionStorage.setItem(indicatorStorageKey, String(activeIndex.value))
  if (item.action === 'receipt') {
    await openReceipt()
    return
  }
  await router.push(item.path)
}

watch(activeIndex, index => moveIndicator(index))

onMounted(() => {
  firstFrame = window.requestAnimationFrame(() => {
    secondFrame = window.requestAnimationFrame(() => moveIndicator(activeIndex.value))
  })
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(indicatorAnimationFrame)
  window.cancelAnimationFrame(firstFrame)
  window.cancelAnimationFrame(secondFrame)
})
</script>

<template>
  <nav class="bottom-nav" :style="navThemeStyle" aria-label="주요 메뉴">
    <div class="nav-shell">
      <svg
          class="nav-background"
          viewBox="0 0 390 78"
          preserveAspectRatio="none"
          aria-hidden="true"
      >
        <defs>
          <linearGradient id="tripass-nav-blue" x1="0" y1="0" x2="1" y2="0">
            <stop class="nav-color-stop" offset="0" :stop-color="navTheme.start" />
            <stop class="nav-color-stop" offset="0.55" :stop-color="navTheme.middle" />
            <stop class="nav-color-stop" offset="1" :stop-color="navTheme.end" />
          </linearGradient>
        </defs>
        <path :d="navSurfacePath" fill="url(#tripass-nav-blue)" />
        <path
            :key="navThemeKey"
            class="theme-bloom"
            :d="navSurfacePath"
            fill="#ffffff"
        />
      </svg>

      <span
          class="moving-notch"
          :class="{ moving: indicatorMoving }"
          :style="indicatorStyle"
          aria-hidden="true"
      >
        <span class="active-orb">
          <Transition name="icon-swap" mode="out-in">
            <component
                :is="indicatorItem.icon"
                :key="indicatorItem.name"
                :size="30"
                :stroke-width="2.45"
            />
          </Transition>
        </span>
      </span>

      <button
          v-for="(item, index) in navItems"
          :key="item.name"
          type="button"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          :aria-current="isActive(item.path) ? 'page' : undefined"
          @click="handleNavigation(item)"
      >
        <span class="nav-icon" :style="navIconStyle(index)">
          <component :is="item.icon" :size="26" :stroke-width="2" />
        </span>
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
  height: 104px;
  transform: translateX(-50%);
  pointer-events: none;
}
.nav-shell {
  position: absolute;
  right: 0;
  bottom: 6px;
  left: 0;
  display: grid;
  height: 64px;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: center;
  padding: 0 6px;
  pointer-events: auto;
}
.nav-background {
  position: absolute;
  z-index: 0;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
  filter: drop-shadow(0 -8px 14px rgba(12, 47, 115, .2));
}
.nav-color-stop {
  transition: stop-color .72s cubic-bezier(.22, .82, .2, 1);
}
.theme-bloom {
  opacity: 0;
  pointer-events: none;
  animation: theme-bloom .72s ease-out both;
}
.moving-notch {
  position: absolute;
  top: -24px;
  z-index: 2;
  width: 62px;
  height: 62px;
  transform: translateX(-50%);
  pointer-events: none;
}
.active-orb {
  position: absolute;
  top: 0;
  left: 50%;
  z-index: 2;
  display: grid;
  width: 62px;
  height: 62px;
  place-items: center;
  box-sizing: border-box;
  opacity: var(--orb-opacity);
  transform: translateX(-50%) translateY(var(--orb-y)) scale(var(--orb-scale));
  border: 6px solid #fff;
  border-radius: 50%;
  background-color: var(--nav-orb-color);
  color: var(--nav-active-icon-color);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, .14),
    0 8px 17px rgba(8, 34, 84, .3);
  will-change: opacity, transform;
  transition: background-color .72s cubic-bezier(.22, .82, .2, 1), color .5s ease;
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
  color: var(--nav-icon-color);
  transition: color .5s ease;
  -webkit-tap-highlight-color: transparent;
}
.nav-icon {
  display: grid;
  width: 32px;
  height: 29px;
  place-items: center;
  opacity: var(--icon-opacity);
  transform: translateY(var(--icon-press-y)) scale(var(--icon-scale));
  will-change: opacity, transform;
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
@keyframes theme-bloom {
  0% { opacity: .2; }
  100% { opacity: 0; }
}
@media (prefers-reduced-motion: reduce) {
  .nav-icon,
  .icon-swap-enter-active,
  .icon-swap-leave-active {
    transition: none;
  }
  .nav-color-stop,
  .active-orb,
  .theme-bloom {
    transition: none;
    animation: none;
  }
}
</style>
