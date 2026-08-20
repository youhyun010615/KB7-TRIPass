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
const reflectError = ref('')
const showWalletReflectPrompt = computed(
  () => lifecycleReady.value && Boolean(travelStore.lifecycle?.needsWalletReflectPrompt),
)
const reflectAmount = computed(() =>
  Number(travelStore.lifecycle?.walletReflectAmount || 0).toLocaleString('ko-KR'),
)

async function resolveWalletReflect(reflect) {
  if (travelStore.walletReflectSubmitting) return
  reflectError.value = ''
  if (!reflect) {
    const confirmed = window.confirm(
      `${reflectAmount.value}원은 연동 계좌로 송금되고 월렛은 0원부터 시작해요. 포함하지 않을까요?`,
    )
    if (!confirmed) return
  }
  try {
    await travelStore.resolveWalletBalanceReflect(reflect)
    await travelStore.loadHomeDashboard({ force: true })
  } catch (error) {
    reflectError.value = error.response?.data?.message || '선택을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.'
  }
}

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

    <div v-if="showWalletReflectPrompt" class="reflect-backdrop" role="dialog" aria-modal="true">
      <section class="reflect-sheet">
        <i class="reflect-handle" />
        <span class="reflect-icon">₩</span>
        <small>TRAVEL SAVINGS</small>
        <h2>미리 모아둔 월렛 잔액이 있어요</h2>
        <p>
          여행 등록 전 월렛에 모아둔 <strong>{{ reflectAmount }}원</strong>을<br>
          이번 여행 저축 금액에 포함할까요?
        </p>
        <p class="reflect-note">포함하지 않으면 전액이 연동 계좌로 송금돼요.</p>
        <p v-if="reflectError" class="reflect-error">{{ reflectError }}</p>
        <div class="reflect-actions">
          <button type="button" class="secondary" :disabled="travelStore.walletReflectSubmitting" @click="resolveWalletReflect(false)">포함 안 할게요</button>
          <button type="button" :disabled="travelStore.walletReflectSubmitting" @click="resolveWalletReflect(true)">
            {{ travelStore.walletReflectSubmitting ? '처리 중...' : '포함할게요' }}
          </button>
        </div>
      </section>
    </div>
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
.reflect-backdrop{position:fixed;inset:0;z-index:220;display:flex;align-items:flex-end;justify-content:center;background:rgba(9,22,49,.52)}.reflect-sheet{width:min(100%,390px);padding:12px 24px calc(24px + env(safe-area-inset-bottom));border-radius:28px 28px 0 0;background:#fff;text-align:center;box-shadow:0 -16px 40px rgba(12,35,78,.2);animation:reflect-rise .28s cubic-bezier(.22,1,.36,1)}.reflect-handle{display:block;width:42px;height:4px;margin:0 auto 18px;border-radius:99px;background:#d8e0ed}.reflect-icon{display:grid;width:52px;height:52px;margin:0 auto 12px;place-items:center;border-radius:17px;background:#e8f0ff;color:#174b9c;font-size:23px;font-weight:900}.reflect-sheet>small{color:#2f70e9;font-size:10px;font-weight:900;letter-spacing:.15em}.reflect-sheet h2{margin-top:7px;font-size:20px;font-weight:900}.reflect-sheet>p{margin-top:10px;color:#66748c;font-size:13px;line-height:1.6}.reflect-sheet>p strong{color:#174b9c;font-weight:900}.reflect-note{padding:10px 12px;border-radius:11px;background:#fff8e5;color:#9a6a00!important;font-size:11px!important}.reflect-error{color:#e04b4b!important}.reflect-actions{display:grid;grid-template-columns:1fr 1.2fr;gap:9px;margin-top:18px}.reflect-actions button{height:49px;border-radius:14px;background:#174b9c;color:#fff;font-size:14px;font-weight:900}.reflect-actions button.secondary{background:#edf2f8;color:#53627a}.reflect-actions button:disabled{opacity:.55}@keyframes reflect-rise{from{transform:translateY(40px);opacity:0}to{transform:translateY(0);opacity:1}}
</style>
