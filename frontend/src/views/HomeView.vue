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
import airplaneIcon from '@/assets/icons/yellow_airplane.png'

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
  }, 1650)
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
      <div
        v-if="isModeSwitching"
        class="mode-transition-loader"
        :class="`to-${nextMode}`"
        role="status"
        aria-live="polite"
      >
        <div class="transition-atmosphere" aria-hidden="true">
          <i class="transition-glow" />
          <i v-for="index in 5" :key="index" class="transition-star" :class="`star-${index}`" />
          <span class="transition-cloud cloud-one" />
          <span class="transition-cloud cloud-two" />
        </div>

        <div v-if="nextMode === 'travel'" class="travel-transition-art" aria-hidden="true">
          <div class="travel-route">
            <i class="route-start" />
            <i class="route-line" />
            <i class="plane-trail" />
            <img :src="airplaneIcon" alt="" class="travel-plane" />
          </div>
        </div>

        <div v-else class="saving-transition-art" aria-hidden="true">
          <div class="saving-route">
            <i class="route-start" />
            <i class="route-line" />
            <span v-for="index in 3" :key="index" class="saving-coin" :class="`coin-${index}`">₩</span>
            <span class="saving-amount">+1,000원</span>
            <div class="saving-vault">
              <i class="vault-glow" />
              <i class="vault-ring" />
              <div class="vault-body">
                <span class="vault-slot" />
                <span class="vault-stack stack-one" />
                <span class="vault-stack stack-two" />
                <b>₩</b>
              </div>
              <i class="vault-sparkle sparkle-one">✦</i>
              <i class="vault-sparkle sparkle-two">✦</i>
            </div>
          </div>
        </div>

        <div class="transition-copy">
          <strong>{{ nextMode === 'travel' ? '여행 모드로 이동 중' : '저축 모드로 이동 중' }}</strong>
          <small>{{ nextMode === 'travel' ? 'TRIPASS가 새로운 여정을 준비하고 있어요' : 'TRIPASS가 저축 목표를 불러오고 있어요' }}</small>
        </div>
      </div>
    </Transition>

    <BottomNav />

  </main>
</template>

<style scoped>
.app-home-shell { position: relative; width: min(100%, 390px); height: 100vh; height: 100dvh; min-height: 0; margin: 0 auto; overflow-x: hidden; overflow-y: auto; overscroll-behavior-y: contain; touch-action: pan-y; -webkit-overflow-scrolling: touch; scrollbar-width: none; background: #eef2f8; }
.app-home-shell::-webkit-scrollbar { display: none; }
.mode-transition-loader { position: fixed; inset: 0 auto 0 50%; z-index: 200; display: flex; width: min(100vw,390px); flex-direction: column; align-items: center; justify-content: center; overflow: hidden; transform: translateX(-50%); background: linear-gradient(180deg,#102e6f 0%,#173f8d 38%,#2855a5 72%,#456db3 100%); color: #fff; }
.transition-atmosphere { position: absolute; inset: 0; overflow: hidden; pointer-events: none; }
.transition-glow { position: absolute; top: 8%; right: -65px; width: 230px; height: 230px; border-radius: 50%; background: radial-gradient(circle,rgba(255,255,255,.22),rgba(255,255,255,0) 70%); animation: sky-glow 2.8s ease-in-out infinite; }
.transition-star { position: absolute; width: 4px; height: 4px; border-radius: 50%; background: #fff; animation: transition-twinkle 1.9s ease-in-out infinite; }
.star-1 { top: 18%; left: 15%; }.star-2 { top: 25%; right: 13%; animation-delay: .35s; }.star-3 { top: 36%; left: 28%; animation-delay: .7s; }.star-4 { right: 25%; bottom: 27%; animation-delay: 1.05s; }.star-5 { bottom: 18%; left: 18%; animation-delay: 1.35s; }
.transition-cloud { position: absolute; width: 92px; height: 25px; border-radius: 999px; background: rgba(255,255,255,.22); filter: blur(.3px); }
.transition-cloud::before,.transition-cloud::after { position: absolute; bottom: 0; border-radius: 50%; background: inherit; content: ''; }.transition-cloud::before { left: 14px; width: 39px; height: 39px; }.transition-cloud::after { right: 13px; width: 48px; height: 48px; }
.cloud-one { top: 20%; left: -35px; animation: cloud-drift 6s ease-in-out infinite alternate; }.cloud-two { right: -30px; bottom: 19%; transform: scale(.75); animation: cloud-drift-reverse 7s ease-in-out infinite alternate; }
.travel-transition-art,.saving-transition-art { position: relative; z-index: 2; width: 100%; }
.travel-route,.saving-route { position: relative; width: 280px; height: 56px; margin: 0 auto; }
.route-line { position: absolute; top: 27px; right: 0; left: 0; height: 2px; background: repeating-linear-gradient(90deg,rgba(255,255,255,.55) 0 10px,transparent 10px 20px); }
.route-start { position: absolute; top: 20px; left: -1px; z-index: 2; width: 14px; height: 14px; border: 3px solid rgba(255,255,255,.45); border-radius: 50%; background: #fff; box-shadow: 0 0 0 5px rgba(255,255,255,.08); animation: route-pulse 1.4s ease-in-out infinite; }
.travel-plane { position: absolute; top: 2px; left: -3px; z-index: 4; width: 48px; height: 48px; object-fit: contain; filter: brightness(0) invert(1) drop-shadow(0 7px 10px rgba(8,31,80,.35)); backface-visibility: hidden; will-change: transform; animation: plane-cross 1.65s cubic-bezier(.22,.61,.36,1) forwards; }
.plane-trail { position: absolute; top: 26px; left: 4px; z-index: 3; width: 62px; height: 3px; border-radius: 99px; background: linear-gradient(90deg,transparent,rgba(255,255,255,.8)); transform-origin: right; animation: plane-trail 1.65s ease-out forwards; }
.saving-coin { position: absolute; top: 16px; left: 0; z-index: 4; display: grid; width: 24px; height: 24px; place-items: center; border: 1px solid #f1b43b; border-radius: 50%; background: #ffd466; color: #815600; font-family:'Space Mono',monospace; font-size: 11px; font-weight: 900; opacity: 0; box-shadow: 0 4px 8px rgba(83,55,0,.18); animation: coin-to-vault 1.35s ease-in infinite; }
.saving-coin.coin-2 { animation-delay: .42s; }.saving-coin.coin-3 { animation-delay: .84s; }
.saving-amount { position: absolute; top: -15px; right: -4px; color: #ffd466; font-family:'Space Mono',monospace; font-size: 11px; font-weight: 900; animation: amount-rise 1.35s ease-in-out infinite .48s; }
.saving-vault { position: absolute; top: -1px; right: -8px; z-index: 5; width: 58px; height: 58px; }
.vault-glow,.vault-ring { position: absolute; top: 50%; left: 50%; border-radius: 50%; transform: translate(-50%,-50%); }.vault-glow { width: 72px; height: 72px; background: radial-gradient(circle,rgba(255,212,102,.54),transparent 70%); animation: vault-glow 1.35s ease-in-out infinite; }.vault-ring { width: 48px; height: 48px; border: 1.5px solid rgba(255,212,102,.7); animation: vault-ring 1.35s ease-out infinite; }
.vault-body { position: absolute; right: 5px; bottom: 3px; width: 45px; height: 43px; overflow: hidden; border: 2px solid rgba(255,255,255,.92); border-radius: 11px 11px 13px 13px; background: rgba(255,255,255,.14); box-shadow: 0 6px 14px rgba(11,42,107,.22); }.vault-body b { position: absolute; top: 9px; left: 50%; color: rgba(255,255,255,.88); font-family:'Space Mono',monospace; font-size: 13px; transform: translateX(-50%); }.vault-slot { position: absolute; top: 4px; left: 50%; width: 20px; height: 3px; border-radius: 9px; background: #fff; transform: translateX(-50%); }.vault-stack { position: absolute; right: 5px; bottom: 5px; left: 5px; height: 6px; border-radius: 3px; background: #ffd466; animation: stack-fill 1.35s ease-in-out infinite; }.stack-two { bottom: 13px; opacity: .72; animation-delay: .15s; }
.vault-sparkle { position: absolute; color: #ffd466; font-size: 15px; font-style: normal; animation: sparkle-pop 1.35s ease-in-out infinite; }.sparkle-one { top: -9px; right: -3px; }.sparkle-two { bottom: -4px; left: -8px; color: #fff; font-size: 10px; animation-delay: .6s; }
.transition-copy { position: relative; z-index: 3; margin-top: 24px; text-align: center; animation: transition-copy-in .4s ease both; }.transition-copy strong { display: block; font-size: 24px; font-weight: 900; letter-spacing: -.025em; }.transition-copy small { display: block; margin-top: 10px; color: rgba(255,255,255,.76); font-size: 13px; font-weight: 650; }
.flight-fade-enter-active,.flight-fade-leave-active { transition: opacity .18s ease; }
.flight-fade-enter-from,.flight-fade-leave-to { opacity: 0; }
@keyframes plane-cross { from { transform: translate3d(0,9px,0) rotate(45deg) scale(.9); } to { transform: translate3d(250px,-20px,0) rotate(45deg) scale(.96); } }
@keyframes plane-trail { 0% { opacity: 0; transform: translateX(0) scaleX(.2); } 30% { opacity: .85; } 100% { opacity: 0; transform: translateX(214px) scaleX(1); } }
@keyframes coin-to-vault { 0% { left: 2%; opacity: 0; transform: translateY(-8px) scale(.55) rotate(0); } 12% { opacity: 1; } 58% { transform: translateY(-14px) scale(1) rotate(180deg); } 88% { left: 88%; opacity: 1; transform: translateY(1px) scale(.9) rotate(330deg); } 100% { left: 94%; opacity: 0; transform: translateY(8px) scale(.45) rotate(360deg); } }
@keyframes amount-rise { 0%,20%,100% { opacity: 0; transform: translateY(6px); } 42%,78% { opacity: 1; transform: translateY(0); } }
@keyframes vault-glow { 0%,100% { opacity: .45; transform: translate(-50%,-50%) scale(.88); } 70% { opacity: 1; transform: translate(-50%,-50%) scale(1.12); } }
@keyframes vault-ring { 0% { opacity: .65; transform: translate(-50%,-50%) scale(.65); } 100% { opacity: 0; transform: translate(-50%,-50%) scale(1.65); } }
@keyframes stack-fill { 0%,100% { opacity: .48; transform: scaleX(.72); } 70% { opacity: 1; transform: scaleX(1); } }
@keyframes sparkle-pop { 0%,100% { opacity: 0; transform: scale(.55) rotate(0); } 65% { opacity: 1; transform: scale(1.1) rotate(25deg); } }
@keyframes route-pulse { 0%,100% { transform: scale(1); opacity: .9; } 50% { transform: scale(1.18); opacity: 1; } }
@keyframes sky-glow { 0%,100% { opacity: .4; } 50% { opacity: .85; } }
@keyframes transition-twinkle { 0%,100% { opacity: .2; transform: scale(.7); } 50% { opacity: .9; transform: scale(1.2); } }
@keyframes cloud-drift { from { transform: translateX(0); } to { transform: translateX(45px); } }
@keyframes cloud-drift-reverse { from { transform: scale(.75) translateX(0); } to { transform: scale(.75) translateX(-45px); } }
@keyframes transition-copy-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
@media (prefers-reduced-motion: reduce) { .mode-transition-loader * { animation-duration: .01ms !important; animation-iteration-count: 1 !important; } }
</style>
