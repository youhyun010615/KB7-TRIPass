<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTravelModeStore } from '@/stores/travelMode'
import BottomNav from '@/components/common/BottomNav.vue'
import SavingsModeHome from '@/components/savings/SavingsModeHome.vue'
import TravelModeHome from '@/components/travel/TravelModeHome.vue'
import TravelEndingView from '@/views/travel/TravelEndingView.vue'
import { useTravelStore } from '@/stores/travel'

const authStore = useAuthStore()
const travelModeStore = useTravelModeStore()
const travelStore = useTravelStore()
const router = useRouter()
const lifecycleReady = ref(false)
const isModeSwitching = ref(false)
const nextMode = ref('travel')
const userName = computed(() => authStore.user?.name ?? '권유현')
function switchMode(mode) {
  if (mode === travelModeStore.mode || isModeSwitching.value) return
  if (mode === 'travel' && !travelModeStore.demoMode && !travelModeStore.canEnterTravelMode) {
    window.alert('여행 모드는 등록한 여행 기간에만 이용할 수 있어요.')
    return
  }

  nextMode.value = mode
  isModeSwitching.value = true
  window.setTimeout(() => {
    travelModeStore.setMode(mode)
    isModeSwitching.value = false
  }, 1000)
}

onMounted(async () => {
  const lifecycle = await travelStore.loadLifecycle()
  if (lifecycle?.onboardingPending) {
    await router.replace({ name: 'TripOnboarding' })
    return
  }
  if (lifecycle?.needsWalletReflectPrompt) {
    await router.replace({ name: 'TravelRegister', query: { mode: 'edit', walletStep: '1' } })
    return
  }
  if (!travelModeStore.lifecycleChecked) {
    travelModeStore.lifecycleChecked = true
    if (travelStore.lifecycle?.lifecycle === 'TRAVELING') travelModeStore.setMode('travel')
    if (travelStore.lifecycle?.lifecycle === 'REVIEW') travelModeStore.setMode('savings')
  }
  lifecycleReady.value = true
})
</script>

<template>
  <TravelEndingView v-if="lifecycleReady && travelStore.lifecycle?.endingReviewRequired" />
  <main v-else class="app-home-shell tab-scroll-surface pb-20" data-tab-scroll>
    <SavingsModeHome
      v-if="travelModeStore.isSavingsMode"
      :on-switch-mode="switchMode"
    />
    <TravelModeHome
      v-else
      :user-name="userName"
      :on-switch-mode="switchMode"
    />

    <Transition name="flight-fade">
      <div v-if="isModeSwitching" class="mode-flight-loader" role="status" aria-live="polite">
        <div class="flight-path"><span>✈</span></div>
        <strong>{{ nextMode === 'travel' ? '여행 모드로 이동 중' : '저축 모드로 이동 중' }}</strong>
        <small>TRIPass가 새로운 여정을 준비하고 있어요</small>
      </div>
    </Transition>

    <BottomNav />

  </main>
</template>

<style scoped>
.app-home-shell { position: relative; width: min(100%, 390px); height: 100vh; height: 100dvh; min-height: 0; margin: 0 auto; overflow-x: hidden; overflow-y: auto; overscroll-behavior-y: contain; touch-action: pan-y; -webkit-overflow-scrolling: touch; scrollbar-width: none; background: #eef2f8; }
.app-home-shell::-webkit-scrollbar { display: none; }
.mode-flight-loader { position: fixed; top: 0; bottom: 0; left: 50%; width: min(100vw,390px); z-index: 200; display: flex; flex-direction: column; align-items: center; justify-content: center; transform: translateX(-50%); background: linear-gradient(180deg,#173f8d 0%,#285eb7 70%,#dbeafe 100%); color: #fff; }
.mode-flight-loader strong { margin-top: 22px; font-size: 18px; }
.mode-flight-loader small { margin-top: 7px; color: #dbeafe; font-size: 11px; }
.flight-path { position: relative; width: 230px; border-top: 2px dashed #ffffff7a; }
.flight-path::before,.flight-path::after { position: absolute; top: -6px; width: 10px; height: 10px; border-radius: 50%; background: #fff; content: ''; }
.flight-path::before { left: 0; }
.flight-path::after { right: 0; }
.flight-path span { position: absolute; top: -22px; left: 0; font-size: 32px; filter: drop-shadow(0 5px 8px #0c255b66); animation: fly-across 1s ease-in-out forwards; }
.flight-fade-enter-active,.flight-fade-leave-active { transition: opacity .18s ease; }
.flight-fade-enter-from,.flight-fade-leave-to { opacity: 0; }
@keyframes fly-across { from { transform: translateX(0) rotate(5deg); } to { transform: translateX(202px) rotate(5deg); } }
</style>
