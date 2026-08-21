<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTravelStore } from '@/stores/travel'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const router = useRouter()
const travelStore = useTravelStore()
const loading = ref(true)
const errorMessage = ref('')

const hasTrip = computed(() => Boolean(travelStore.lifecycle?.hasTrip))
const hasLinkedAccount = computed(() => Boolean(travelStore.lifecycle?.hasLinkedAccount))
const hasLinkedCard = computed(() => Boolean(travelStore.lifecycle?.hasLinkedCard))
const totalSteps = 3
const completedCount = computed(() => Number(hasTrip.value) + Number(hasLinkedAccount.value) + Number(hasLinkedCard.value))

async function loadOnboarding() {
  loading.value = true
  errorMessage.value = ''
  try {
    const lifecycle = await travelStore.loadLifecycle()
    if (lifecycle?.onboardingPending) await travelStore.acknowledgeOnboarding()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '여행 준비 정보를 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

function openTripRegistration() {
  if (hasTrip.value) return
  router.push({ name: 'TravelRegister', query: { onboarding: '1' } })
}

function openAccountRegistration() {
  if (hasLinkedAccount.value) return
  router.push({ name: 'FinancialProfile', query: { onboarding: '1', step: '2' } })
}

function openCardRegistration() {
  if (hasLinkedCard.value) return
  router.push({ name: 'FinancialProfile', query: { onboarding: '1', step: '9' } })
}

function finishOnboarding() {
  router.replace({ name: 'Home' })
}

onMounted(loadOnboarding)
</script>

<template>
  <main class="onboarding-page">
    <section v-if="loading" class="onboarding-loader">
      <LoadingSpinner />
    </section>

    <section v-else-if="errorMessage" class="onboarding-error">
      <strong>여행 준비 화면을 열지 못했어요</strong>
      <p>{{ errorMessage }}</p>
      <button type="button" @click="loadOnboarding">다시 시도</button>
    </section>

    <template v-else>
      <header class="setup-hero">
        <span class="hero-orbit" aria-hidden="true" />
        <p class="brand-mark">
          <span>TRIPASS</span>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M21.7 11.2 14 7.1V3.6a2 2 0 0 0-4 0v3.5l-7.7 4.1a1.5 1.5 0 0 0-.8 1.3v1.2l8.5-2.2v4.2l-2.3 1.8v1l4.3-1 4.3 1v-1L14 15.7v-4.2l8.5 2.2v-1.2a1.5 1.5 0 0 0-.8-1.3Z" /></svg>
        </p>
        <h1>여행 준비를<br>차근차근 시작해 볼까요?</h1>
        <p class="hero-copy">여행 계획, 계좌, 카드를 등록하면<br>맞춤 저축 여정이 시작돼요.</p>
      </header>

      <section class="setup-content">
        <article class="setup-ticket">
          <span class="ticket-notch left" aria-hidden="true" />
          <span class="ticket-notch right" aria-hidden="true" />

          <div class="ticket-band">
            <span>TRIP SETUP BOARDING PASS</span>
            <b>{{ completedCount }}/{{ totalSteps }}</b>
          </div>

          <div class="progress-copy">
            <div>
              <small>여행 준비 진행률</small>
              <strong>{{ completedCount === totalSteps ? '준비가 완료됐어요' : `${totalSteps - completedCount}단계가 남았어요` }}</strong>
            </div>
            <em>{{ Math.round(completedCount / totalSteps * 100) }}%</em>
          </div>
          <div class="progress-track"><i :style="{ width: `${Math.round(completedCount / totalSteps * 100)}%` }" /></div>

          <div class="setup-list">
            <button type="button" class="setup-step" :class="{ complete: hasTrip }" :disabled="hasTrip" @click="openTripRegistration">
              <span class="step-number">{{ hasTrip ? '✓' : '1' }}</span>
              <span class="step-icon plane-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M21.7 11.2 14 7.1V3.6a2 2 0 0 0-4 0v3.5l-7.7 4.1a1.5 1.5 0 0 0-.8 1.3v1.2l8.5-2.2v4.2l-2.3 1.8v1l4.3-1 4.3 1v-1L14 15.7v-4.2l8.5 2.2v-1.2a1.5 1.5 0 0 0-.8-1.3Z" /></svg>
              </span>
              <span class="step-copy"><strong>여행 계획 등록하기</strong><small>{{ hasTrip ? '여행 계획 등록을 완료했어요' : '여행지와 일정, 목표 예산을 정해요' }}</small></span>
              <span class="step-status">{{ hasTrip ? '등록 완료' : '등록하기' }}</span>
            </button>

            <div class="step-connector" :class="{ active: hasTrip }"><i /></div>

            <button type="button" class="setup-step" :class="{ complete: hasLinkedAccount }" :disabled="hasLinkedAccount" @click="openAccountRegistration">
              <span class="step-number">{{ hasLinkedAccount ? '✓' : '2' }}</span>
              <span class="step-icon account-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M3.5 9h17M5 9V19m4-10v10m6-10v10m4-10v10M3 19h18M12 3l9 4H3l9-4Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" /></svg>
              </span>
              <span class="step-copy"><strong>계좌 등록하기</strong><small>{{ hasLinkedAccount ? '주거래 계좌 등록을 완료했어요' : '소비 분석에 사용할 계좌를 연결해요' }}</small></span>
              <span class="step-status">{{ hasLinkedAccount ? '등록 완료' : '등록하기' }}</span>
            </button>

            <div class="step-connector" :class="{ active: hasLinkedAccount }"><i /></div>

            <button type="button" class="setup-step" :class="{ complete: hasLinkedCard }" :disabled="hasLinkedCard" @click="openCardRegistration">
              <span class="step-number">{{ hasLinkedCard ? '✓' : '3' }}</span>
              <span class="step-icon card-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="2" y="5" width="20" height="14" rx="2.5" stroke="currentColor" stroke-width="1.8" /><path d="M2 10h20" stroke="currentColor" stroke-width="1.8" /><path d="M6 15h4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" /></svg>
              </span>
              <span class="step-copy"><strong>카드 등록하기</strong><small>{{ hasLinkedCard ? '카드 등록을 완료했어요' : '소비 분석에 사용할 카드를 연결해요' }}</small></span>
              <span class="step-status">{{ hasLinkedCard ? '등록 완료' : '등록하기' }}</span>
            </button>
          </div>

          <p class="security-copy">계좌·카드 인증 정보는 연결 과정에서만 안전하게 사용돼요.</p>
        </article>

        <button type="button" class="start-button" @click="finishOnboarding">{{ completedCount === totalSteps ? 'TRIPass 시작하기' : '일단 둘러볼게요' }}</button>
      </section>
    </template>
  </main>
</template>

<style scoped>
*{box-sizing:border-box}.onboarding-page{width:min(100%,390px);min-height:100dvh;margin:0 auto;color:#111827;background:#f3f6fb}.onboarding-loader,.onboarding-error{display:flex;min-height:100dvh;align-items:center;justify-content:center;flex-direction:column;padding:28px;text-align:center}.onboarding-error strong{font-size:18px}.onboarding-error p{margin-top:8px;color:#718096;font-size:13px}.onboarding-error button{margin-top:18px;padding:12px 22px;border:0;border-radius:12px;background:#174b9c;color:#fff;font-weight:800}.setup-hero{position:relative;overflow:hidden;padding:58px 24px 76px;color:#fff;background:linear-gradient(155deg,#0b2a6b 0%,#123c94 60%,#17459f 100%)}.hero-orbit{position:absolute;top:-52px;right:-48px;width:180px;height:180px;border-radius:50%;background:rgba(255,255,255,.06)}.brand-mark{position:relative;display:flex;align-items:center;gap:7px;margin:0;color:#fff;font-size:13px;font-weight:900;letter-spacing:.04em}.brand-mark svg{color:#ffd45e;transform:rotate(45deg)}.setup-hero h1{position:relative;margin:19px 0 0;font-size:25px;line-height:1.35;letter-spacing:-.045em}.hero-copy{position:relative;margin:9px 0 0;color:rgba(255,255,255,.67);font-size:12px;font-weight:600;line-height:1.55}.setup-content{margin-top:-50px;padding:0 22px 34px}.setup-ticket{position:relative;overflow:hidden;border-radius:20px;background:#fff;box-shadow:0 15px 34px rgba(11,42,107,.17)}.ticket-notch{position:absolute;top:55px;z-index:2;width:18px;height:18px;border-radius:50%;background:#123c94}.ticket-notch.left{left:-9px}.ticket-notch.right{right:-9px}.ticket-band{display:flex;align-items:center;justify-content:space-between;padding:17px 22px;color:#ffd466;background:#0b2a6b;font-size:10px;font-weight:900;letter-spacing:.09em}.ticket-band b{color:rgba(255,255,255,.72);font-family:ui-monospace,SFMono-Regular,Menlo,monospace}.progress-copy{display:flex;align-items:flex-end;justify-content:space-between;padding:22px 22px 10px}.progress-copy div{display:flex;flex-direction:column;gap:4px}.progress-copy small{color:#94a3b8;font-size:10px;font-weight:700}.progress-copy strong{font-size:16px}.progress-copy em{color:#2469e8;font-size:22px;font-style:normal;font-weight:900}.progress-track{height:6px;margin:0 22px 21px;overflow:hidden;border-radius:999px;background:#e8eef8}.progress-track i{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#ffd45e,#2f6fed);transition:width .45s ease}.setup-list{padding:0 16px 19px}.setup-step{display:grid;width:100%;grid-template-columns:24px 44px minmax(0,1fr) auto;align-items:center;gap:10px;padding:14px 12px;border:1px solid #e6ebf3;border-radius:15px;background:#fff;text-align:left;transition:transform .18s ease,border-color .18s ease,box-shadow .18s ease}.setup-step:not(:disabled):active{transform:scale(.985)}.setup-step:not(:disabled){box-shadow:0 5px 14px rgba(20,45,91,.06)}.setup-step.complete{border-color:#bce7d8;background:#f3fcf8}.step-number{display:grid;width:22px;height:22px;place-items:center;border-radius:50%;color:#fff;background:#2f6fed;font-size:10px;font-weight:900}.complete .step-number{background:#10a77f}.step-icon{display:grid;width:42px;height:42px;place-items:center;border-radius:12px}.plane-icon{color:#2f6fed;background:#eaf1ff}.plane-icon svg{transform:rotate(45deg)}.account-icon{color:#8b5cf6;background:#f3f0fc}.card-icon{color:#e67e22;background:#fef5ec}.step-copy{display:flex;min-width:0;flex-direction:column;gap:4px}.step-copy strong{font-size:13px}.step-copy small{overflow:hidden;color:#98a2b3;font-size:9.5px;text-overflow:ellipsis;white-space:nowrap}.step-status{color:#2469e8;font-size:9px;font-weight:900}.complete .step-status{color:#0b8f6e}.step-connector{height:18px;margin-left:26px;border-left:2px dashed #dbe3ef}.step-connector.active{border-color:#8bd3bc}.security-copy{margin:0 22px 22px;padding:11px 12px;border-radius:11px;color:#6d7f9c;background:#f2f6fc;font-size:9px;text-align:center}.start-button{width:100%;height:54px;margin-top:18px;border:0;border-radius:15px;color:#fff;background:#173f8d;font-size:14px;font-weight:900;box-shadow:0 9px 20px rgba(23,63,141,.2)}
.setup-hero{animation:onboarding-hero-in .55s cubic-bezier(.22,1,.36,1) both}.setup-content{animation:onboarding-content-in .65s cubic-bezier(.22,1,.36,1) .12s both}.setup-step{animation:onboarding-step-in .48s cubic-bezier(.22,1,.36,1) .28s both}.setup-step:nth-of-type(2){animation-delay:.38s}.setup-step:nth-of-type(3){animation-delay:.48s}.start-button{animation:onboarding-button-in .42s ease .48s both}@keyframes onboarding-hero-in{from{opacity:0;transform:translateY(-12px)}to{opacity:1;transform:translateY(0)}}@keyframes onboarding-content-in{from{opacity:0;transform:translateY(30px) scale(.98)}to{opacity:1;transform:translateY(0) scale(1)}}@keyframes onboarding-step-in{from{opacity:0;transform:translateX(-18px)}to{opacity:1;transform:translateX(0)}}@keyframes onboarding-button-in{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}@media(prefers-reduced-motion:reduce){.setup-hero,.setup-content,.setup-step,.start-button{animation:none}}
</style>
