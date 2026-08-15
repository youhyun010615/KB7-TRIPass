<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import tripassWordmark from '@/assets/brand/tripass-wordmark.png'

const router = useRouter()

const slides = [
  {
    key: 'journey',
    eyebrow: 'FLIGHT MODE · MONEY',
    title: ['모으고, 쓰고,', '돌아보는 여행의 모든 순간'],
    description:
      '여행 전 목표와 자금부터 여행 중 지출 관리,\n여행 후 소비까지 함께하는 여행 자금관리 서비스',
  },
  {
    key: 'saving',
    eyebrow: 'BEFORE THE TRIP',
    title: ['목표를 정하면,', 'AI가 저축 계획을 제안해요'],
    description:
      '여행 예산과 월 저축 목표를 계산하고\n실제 소비 패턴에서 아낄 항목을 찾아드려요.',
  },
  {
    key: 'spending',
    eyebrow: 'ON THE TRIP · AFTER',
    title: ['여행 자금을 계획대로 쓰고,', '소비를 돌아봐요'],
    description:
      '여행 중 지출을 간편하게 관리하고\n여행 후 리포트로 다음 계획까지 연결해요.',
  },
  {
    key: 'ready',
    eyebrow: 'READY FOR TAKEOFF',
    title: ['여행 자금을 똑똑하게 관리해', '더 자주 떠나보세요'],
    description:
      '목표를 정하고, 필요한 금액을 모으고,\n여행에서 사용한 뒤 다음 여행을 준비해요.',
  },
]

const current = ref(0)
const touchStartX = ref(0)
const touchDeltaX = ref(0)
const isDragging = ref(false)
const isLast = computed(() => current.value === slides.length - 1)
const routeProgress = computed(() => `${(current.value / (slides.length - 1)) * 100}%`)
const trackTransform = computed(() => {
  const dragOffset = isDragging.value ? touchDeltaX.value : 0
  return `translateX(calc(-${current.value * 100}% + ${dragOffset}px))`
})

function rememberOnboarding() {
  localStorage.setItem('tripass-onboarding-complete', 'true')
}

function goToLogin() {
  rememberOnboarding()
  router.replace('/login')
}

function goToSignup() {
  rememberOnboarding()
  router.replace('/signup')
}

function next() {
  if (isLast.value) {
    goToSignup()
    return
  }
  current.value += 1
}

function goTo(index) {
  current.value = index
}

function onTouchStart(event) {
  touchStartX.value = event.touches[0].clientX
  touchDeltaX.value = 0
  isDragging.value = true
}

function onTouchMove(event) {
  const rawDelta = event.touches[0].clientX - touchStartX.value
  const isPullingPastStart = current.value === 0 && rawDelta > 0
  const isPullingPastEnd = isLast.value && rawDelta < 0

  // 처음과 마지막에서는 살짝 저항을 주고, 나머지는 손가락을 그대로 따라간다.
  touchDeltaX.value = isPullingPastStart || isPullingPastEnd
    ? rawDelta * 0.22
    : rawDelta
}

function onTouchEnd() {
  const swipeThreshold = 45

  if (touchDeltaX.value < -swipeThreshold && current.value < slides.length - 1) {
    current.value += 1
  } else if (touchDeltaX.value > swipeThreshold && current.value > 0) {
    current.value -= 1
  }

  isDragging.value = false
  touchDeltaX.value = 0
}
</script>

<template>
  <main
    class="onboarding"
    @touchstart.passive="onTouchStart"
    @touchmove.passive="onTouchMove"
    @touchend="onTouchEnd"
  >
    <div class="ambient ambient-top"></div>
    <div class="ambient ambient-bottom"></div>

    <header class="onboarding-header">
      <span class="step-label">STEP {{ String(current + 1).padStart(2, '0') }} / 04</span>
      <button type="button" class="skip-button" @click="goToLogin">건너뛰기</button>
    </header>

    <section class="flight-route" aria-label="온보딩 진행 상태">
      <span class="route-point route-origin" :class="{ active: current >= 0 }"></span>
      <span class="route-line">
        <span class="route-line-progress" :style="{ width: routeProgress }"></span>
      </span>
      <span class="route-plane" :style="{ left: routeProgress }" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M21.7 11.2 14 7.1V3.6a2 2 0 0 0-4 0v3.5l-7.7 4.1a1.5 1.5 0 0 0-.8 1.3v1.2l8.5-2.2v4.2l-2.3 1.8v1l4.3-1 4.3 1v-1L14 15.7v-4.2l8.5 2.2v-1.2a1.5 1.5 0 0 0-.8-1.3Z" />
        </svg>
      </span>
      <span class="route-point route-destination" :class="{ active: isLast }"></span>
      <span class="route-caption route-caption-left">START</span>
      <span class="route-caption route-caption-right">{{ isLast ? 'READY' : 'GOAL' }}</span>
    </section>

    <section class="slides-window">
      <div
        class="slides-track"
        :class="{ dragging: isDragging }"
        :style="{ transform: trackTransform }"
      >
        <article
          v-for="(slide, index) in slides"
          :key="slide.key"
          class="slide"
          :class="{ 'is-active': current === index }"
          :aria-hidden="current !== index"
        >
          <div class="visual-stage">
              <div class="visual-content">
                <div v-if="slide.key === 'journey'" class="brand-visual">
                  <div class="brand-logo-stage">
                    <span class="brand-logo-halo" aria-hidden="true"></span>
                    <img class="brand-logo" :src="tripassWordmark" alt="TRIPASS 여행을 준비하는 가장 똑똑한 금융 습관" />
                    <div class="brand-flight-path" aria-hidden="true">
                      <span></span>
                      <b>✈</b>
                    </div>
                  </div>
                </div>

                <div v-else-if="slide.key === 'saving'" class="saving-visual">
                  <div class="deposit-stream" aria-hidden="true">
                    <span>₩</span>
                    <span>₩</span>
                    <span>₩</span>
                  </div>
                  <div class="achievement-pill">
                    <span>✓</span>
                    <strong>65%</strong>
                    <small>AI 저축 미션 달성</small>
                  </div>
                  <div class="saving-card">
                    <div class="saving-card-head">
                      <span class="coin-icon">₩</span>
                      <span>이번 달 여행 저축</span>
                    </div>
                    <strong class="saving-amount">300,000원</strong>
                    <div class="saving-divider"></div>
                    <div class="saving-fill" aria-hidden="true"><span></span></div>
                    <div class="saving-row">
                      <span>✓ 월 저축 목표</span>
                      <strong>+200,000원</strong>
                    </div>
                    <div class="saving-row">
                      <span>✓ AI 절약 미션</span>
                      <strong>+100,000원</strong>
                    </div>
                  </div>
                </div>

                <div v-else-if="slide.key === 'spending'" class="spending-visual">
                  <div class="receipt-card">
                    <span class="receipt-title">TRIPASS · 여행 중 지출</span>
                    <span class="receipt-line line-long"></span>
                    <span class="receipt-line"></span>
                    <span class="receipt-line line-short"></span>
                    <strong>CHF 280.00 사용</strong>
                  </div>
                  <div class="receipt-arrow">→</div>
                  <div class="rollover-pill">
                    여행 후 남은 자금 <strong>240,000원</strong> → 다음 여행
                  </div>
                  <div class="rollover-flight" aria-hidden="true">
                    <span></span><b>✈</b><span></span>
                  </div>
                </div>

                <div v-else class="ready-visual">
                  <div class="cruise-wind" aria-hidden="true">
                    <span></span><span></span><span></span>
                    <span></span><span></span><span></span>
                  </div>
                  <div class="takeoff-trails" aria-hidden="true">
                    <span></span><span></span><span></span>
                  </div>
                  <div class="ready-plane-ring">
                    <svg viewBox="0 0 24 24" fill="currentColor">
                      <path d="M21.7 11.2 14 7.1V3.6a2 2 0 0 0-4 0v3.5l-7.7 4.1a1.5 1.5 0 0 0-.8 1.3v1.2l8.5-2.2v4.2l-2.3 1.8v1l4.3-1 4.3 1v-1L14 15.7v-4.2l8.5 2.2v-1.2a1.5 1.5 0 0 0-.8-1.3Z" />
                    </svg>
                  </div>
                </div>
              </div>
          </div>

          <div class="copy-block">
            <p v-if="slide.key !== 'journey'" class="eyebrow">{{ slide.eyebrow }}</p>
            <h1>
              <template v-for="(line, lineIndex) in slide.title" :key="line">
                {{ line }}<br v-if="lineIndex < slide.title.length - 1" />
              </template>
            </h1>
            <p class="description">{{ slide.description }}</p>
          </div>
        </article>
      </div>
    </section>

    <footer class="onboarding-footer">
      <div class="page-indicator" aria-label="온보딩 페이지 선택">
        <button
          v-for="(slide, index) in slides"
          :key="slide.key"
          type="button"
          :class="{ active: current === index }"
          :aria-label="`${index + 1}번째 화면`"
          @click="goTo(index)"
        ></button>
      </div>

      <button v-if="!isLast" type="button" class="primary-button" @click="next">
        다음
      </button>
      <div v-else class="final-actions">
        <button type="button" class="primary-button" @click="goToSignup">
          TRIPASS 시작하기
        </button>
        <button type="button" class="login-button" @click="goToLogin">
          이미 계정이 있어요 · 로그인
        </button>
      </div>
    </footer>
  </main>
</template>

<style scoped>
.onboarding {
  --navy: #0d2e72;
  --navy-light: #285ec5;
  --yellow: #ffd45e;
  position: relative;
  display: flex;
  min-height: 100dvh;
  flex-direction: column;
  overflow: hidden;
  color: white;
  background:
    radial-gradient(circle at 96% 6%, rgba(108, 159, 255, 0.38) 0 84px, transparent 85px),
    linear-gradient(160deg, #2861c9 0%, #153f91 42%, #0b2865 100%);
  isolation: isolate;
  touch-action: pan-y;
}

.onboarding::after {
  position: absolute;
  top: 19%;
  left: -35%;
  z-index: -1;
  width: 34%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.42), transparent);
  content: '';
  animation: sky-streak 7s ease-in-out infinite;
  pointer-events: none;
}

.ambient {
  position: absolute;
  z-index: -1;
  border-radius: 999px;
  pointer-events: none;
}

.ambient-top {
  top: -62px;
  right: -58px;
  width: 210px;
  height: 210px;
  background: rgba(255, 255, 255, 0.06);
  animation: ambient-drift 8s ease-in-out infinite alternate;
}

.ambient-bottom {
  bottom: -110px;
  left: -82px;
  width: 250px;
  height: 250px;
  background: rgba(42, 91, 181, 0.34);
  animation: ambient-drift 10s ease-in-out 1s infinite alternate-reverse;
}

.onboarding-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(42px, env(safe-area-inset-top)) 22px 0;
}

.step-label,
.eyebrow,
.brand-subtitle {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.24em;
}

.step-label {
  color: rgba(214, 227, 255, 0.72);
}

.skip-button {
  padding: 6px 0 6px 12px;
  border: 0;
  color: rgba(255, 255, 255, 0.72);
  background: transparent;
  font-size: 13px;
  font-weight: 600;
}

.flight-route {
  position: relative;
  height: 56px;
  margin: 30px 28px 0;
}

.route-line {
  position: absolute;
  top: 13px;
  right: 6px;
  left: 6px;
  height: 2px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.3);
}

.route-line-progress {
  display: block;
  height: 100%;
  background: var(--yellow);
  box-shadow: 0 0 10px rgba(255, 212, 94, 0.7);
  transition: width 520ms cubic-bezier(0.22, 1, 0.36, 1);
}

.route-point {
  position: absolute;
  top: 9px;
  z-index: 2;
  width: 10px;
  height: 10px;
  border: 2px solid rgba(255, 255, 255, 0.52);
  border-radius: 50%;
  background: #2456b4;
  transition: 300ms ease;
}

.route-point.active {
  border-color: var(--yellow);
  background: var(--yellow);
  box-shadow: 0 0 0 4px rgba(255, 212, 94, 0.1);
}

.route-origin { left: 0; }
.route-destination { right: 0; }

.route-plane {
  position: absolute;
  top: 1px;
  z-index: 3;
  width: 25px;
  height: 25px;
  color: var(--yellow);
  transform: translateX(-50%) rotate(90deg);
  transition: left 520ms cubic-bezier(0.22, 1, 0.36, 1);
  filter: drop-shadow(0 4px 8px rgba(3, 21, 61, 0.28));
}

.route-plane svg {
  animation: plane-cruise 1.8s ease-in-out infinite;
}

.route-caption {
  position: absolute;
  top: 28px;
  color: rgba(255, 255, 255, 0.56);
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.route-caption-left { left: -3px; }
.route-caption-right { right: -5px; }

.slides-window {
  flex: 1;
  overflow: hidden;
}

.slides-track {
  display: flex;
  height: 100%;
  transition: transform 520ms cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.slides-track.dragging {
  transition: none;
}

.slide {
  display: flex;
  width: 100%;
  flex: 0 0 100%;
  flex-direction: column;
  align-items: center;
  padding: 5px 26px 0;
  opacity: 0.46;
  transform: scale(0.965);
  transition: opacity 260ms ease, transform 420ms cubic-bezier(0.22, 1, 0.36, 1);
}

.slide.is-active {
  opacity: 1;
  transform: scale(1);
}

.slide.is-active .visual-content {
  animation: visual-arrive 520ms cubic-bezier(0.22, 1, 0.36, 1) both;
}

.slide.is-active .copy-block > * {
  animation: copy-arrive 460ms cubic-bezier(0.22, 1, 0.36, 1) both;
}

.slide.is-active .copy-block > :nth-child(1) { animation-delay: 100ms; }
.slide.is-active .copy-block > :nth-child(2) { animation-delay: 170ms; }
.slide.is-active .copy-block > :nth-child(3) { animation-delay: 240ms; }

.visual-stage {
  display: flex;
  width: 100%;
  min-height: clamp(205px, 29vh, 270px);
  align-items: center;
  justify-content: center;
}

.visual-content {
  width: 100%;
}

.brand-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.brand-logo-stage {
  position: relative;
  display: grid;
  width: min(100%, 330px);
  height: 122px;
  place-items: center;
}

.brand-logo-halo {
  position: absolute;
  inset: 8px 18px;
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(86, 150, 255, 0.28), transparent 70%);
  filter: blur(12px);
}

.brand-logo {
  position: relative;
  z-index: 2;
  display: block;
  width: calc(100% - 30px);
  filter:
    brightness(2.35)
    saturate(0.9)
    drop-shadow(0 2px 1px rgba(255, 255, 255, 0.15))
    drop-shadow(0 14px 24px rgba(2, 16, 52, 0.32));
}

.brand-flight-path {
  position: absolute;
  right: 10px;
  bottom: 3px;
  left: 58%;
  z-index: 4;
  height: 34px;
  overflow: hidden;
}

.brand-flight-path span {
  position: absolute;
  right: 22px;
  bottom: 5px;
  left: 0;
  height: 34px;
  border-top: 2px dashed rgba(223, 164, 21, 0.72);
  border-radius: 50% 50% 0 0;
  transform: rotate(-8deg) scaleX(0);
  transform-origin: left bottom;
}

.brand-flight-path b {
  position: absolute;
  right: 2px;
  bottom: 13px;
  color: #e1a51a;
  font-size: 17px;
  font-weight: 400;
  transform: rotate(-8deg);
}

.slide.is-active .brand-logo {
  animation:
    brand-logo-board 680ms cubic-bezier(0.16, 1, 0.3, 1) 100ms both,
    brand-logo-float 3.8s ease-in-out 900ms infinite;
}

.slide.is-active .brand-logo-halo {
  animation: brand-halo 2.8s ease-out 520ms infinite;
}

.slide.is-active .brand-flight-path span {
  animation: brand-route-draw 780ms cubic-bezier(0.22, 1, 0.36, 1) 420ms both;
}

.slide.is-active .brand-flight-path b {
  animation: brand-plane-arrive 780ms cubic-bezier(0.22, 1, 0.36, 1) 420ms both;
}

.brand-subtitle,
.eyebrow {
  color: var(--yellow);
}

.saving-visual {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.deposit-stream {
  position: absolute;
  top: 26px;
  left: 50%;
  z-index: 3;
  width: 36px;
  height: 126px;
  transform: translateX(-50%);
  pointer-events: none;
}

.deposit-stream span {
  position: absolute;
  top: 0;
  left: 50%;
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  border: 1px solid rgba(255, 226, 127, 0.75);
  border-radius: 50%;
  opacity: 0;
  color: #173d88;
  background: var(--yellow);
  box-shadow: 0 5px 14px rgba(255, 212, 94, 0.32);
  font-size: 10px;
  font-weight: 900;
}

.slide.is-active .deposit-stream span {
  animation: deposit-coin 1.15s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.slide.is-active .deposit-stream span:nth-child(1) { animation-delay: 330ms; }
.slide.is-active .deposit-stream span:nth-child(2) { animation-delay: 590ms; }
.slide.is-active .deposit-stream span:nth-child(3) { animation-delay: 850ms; }

.achievement-pill,
.rollover-pill {
  display: flex;
  align-items: center;
  gap: 7px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.78);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
}

.achievement-pill {
  padding: 8px 14px;
}

.achievement-pill span,
.achievement-pill strong,
.rollover-pill strong {
  color: var(--yellow);
}

.achievement-pill small { font-size: 10px; }

.saving-card {
  width: min(100%, 286px);
  padding: 18px 20px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 18px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.06));
  box-shadow: 0 18px 34px rgba(5, 25, 70, 0.24);
  backdrop-filter: blur(10px);
}

.slide.is-active .saving-card {
  animation: card-float 3.4s ease-in-out 600ms infinite;
}

.saving-card-head {
  display: flex;
  align-items: center;
  gap: 9px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 11px;
  font-weight: 700;
}

.coin-icon {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 50%;
  color: var(--yellow);
  background: rgba(255, 212, 94, 0.14);
}

.slide.is-active .coin-icon {
  animation: coin-pulse 2.2s ease-in-out 850ms infinite;
}

.saving-amount {
  display: block;
  margin: 8px 0 12px 39px;
  font-size: 24px;
}

.saving-divider {
  height: 1px;
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.15);
}

.saving-fill {
  height: 4px;
  margin: -2px 0 8px 39px;
  overflow: hidden;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.1);
}

.saving-fill span {
  display: block;
  width: 65%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ffd45e, #fff0a8);
  transform: scaleX(0);
  transform-origin: left;
}

.slide.is-active .saving-fill span {
  animation: saving-fill-up 850ms cubic-bezier(0.22, 1, 0.36, 1) 820ms both;
}

.slide.is-active .saving-amount {
  animation: amount-confirm 480ms ease 980ms both;
}

.saving-row {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 10px;
}

.saving-row strong { color: var(--yellow); }

.spending-visual {
  position: relative;
  display: flex;
  min-height: 220px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.receipt-card {
  display: flex;
  width: 216px;
  min-height: 118px;
  transform: rotate(3deg);
  flex-direction: column;
  padding: 18px;
  border-radius: 14px;
  color: #17254a;
  background: #fff9e8;
  box-shadow: 0 20px 35px rgba(3, 21, 61, 0.28);
}

.slide.is-active .receipt-card {
  animation: receipt-float 3.2s ease-in-out 550ms infinite;
}

.receipt-title {
  margin-bottom: 14px;
  font-size: 10px;
  font-weight: 800;
}

.receipt-line {
  --receipt-line-width: 74%;
  width: 74%;
  height: 5px;
  margin-bottom: 7px;
  border-radius: 10px;
  background: #e8e0c9;
}

.receipt-line.line-long { --receipt-line-width: 88%; width: 88%; }
.receipt-line.line-short { --receipt-line-width: 58%; width: 58%; }

.slide.is-active .receipt-line {
  animation: receipt-write 620ms ease both;
}

.slide.is-active .receipt-line:nth-of-type(2) { animation-delay: 430ms; }
.slide.is-active .receipt-line:nth-of-type(3) { animation-delay: 570ms; }
.slide.is-active .receipt-line:nth-of-type(4) { animation-delay: 710ms; }

.receipt-card strong {
  margin-top: auto;
  color: #d58100;
  font-size: 10px;
  text-align: right;
}

.receipt-arrow {
  position: absolute;
  top: 26px;
  right: 27px;
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border: 1px solid var(--yellow);
  border-radius: 50%;
  color: var(--yellow);
  background: #0c2d70;
  font-size: 24px;
}

.slide.is-active .receipt-arrow {
  animation: arrow-nudge 1.6s ease-in-out 850ms infinite;
}

.rollover-pill {
  margin-top: 19px;
  padding: 9px 14px;
  font-size: 9px;
}

.slide.is-active .rollover-pill strong {
  animation: rollover-highlight 1.8s ease-in-out 1s infinite;
}

.rollover-flight {
  display: flex;
  width: 206px;
  align-items: center;
  gap: 8px;
  margin-top: 13px;
  color: var(--yellow);
}

.rollover-flight span {
  height: 1px;
  flex: 1;
  background: rgba(255, 255, 255, 0.22);
}

.rollover-flight b {
  opacity: 0;
  font-size: 16px;
  font-weight: 400;
}

.slide.is-active .rollover-flight b {
  animation: rollover-plane 1.1s cubic-bezier(0.22, 1, 0.36, 1) 900ms both;
}

.ready-visual {
  position: relative;
  display: flex;
  width: 100%;
  min-height: 112px;
  align-items: center;
  justify-content: center;
}

.cruise-wind {
  position: absolute;
  inset: 0 -28px;
  overflow: hidden;
  pointer-events: none;
}

.cruise-wind span {
  position: absolute;
  left: -42%;
  width: 42%;
  height: 2px;
  border-radius: 99px;
  opacity: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.48), rgba(255, 212, 94, 0.72), transparent);
}

.cruise-wind span:nth-child(1) { top: 12%; width: 32%; }
.cruise-wind span:nth-child(2) { top: 28%; width: 46%; }
.cruise-wind span:nth-child(3) { top: 44%; width: 25%; }
.cruise-wind span:nth-child(4) { top: 60%; width: 39%; }
.cruise-wind span:nth-child(5) { top: 76%; width: 29%; }
.cruise-wind span:nth-child(6) { top: 90%; width: 50%; }

.slide.is-active .cruise-wind span {
  animation: wind-stream 2.35s linear 900ms infinite;
}

.slide.is-active .cruise-wind span:nth-child(2) { animation-delay: 1.25s; animation-duration: 2.8s; }
.slide.is-active .cruise-wind span:nth-child(3) { animation-delay: 1.75s; animation-duration: 2s; }
.slide.is-active .cruise-wind span:nth-child(4) { animation-delay: 2.2s; animation-duration: 2.6s; }
.slide.is-active .cruise-wind span:nth-child(5) { animation-delay: 1.45s; animation-duration: 2.15s; }
.slide.is-active .cruise-wind span:nth-child(6) { animation-delay: 2.65s; animation-duration: 3s; }

.takeoff-trails {
  position: absolute;
  top: 40px;
  right: calc(50% + 34px);
  display: flex;
  width: 190px;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.takeoff-trails span {
  display: block;
  height: 2px;
  border-radius: 99px;
  opacity: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 212, 94, 0.85));
}

.takeoff-trails span:nth-child(1) { width: 100%; }
.takeoff-trails span:nth-child(2) { width: 72%; }
.takeoff-trails span:nth-child(3) { width: 44%; }

.slide.is-active .takeoff-trails span {
  animation: takeoff-trail 700ms ease-out both;
}

.slide.is-active .takeoff-trails span:nth-child(1) { animation-delay: 130ms; }
.slide.is-active .takeoff-trails span:nth-child(2) { animation-delay: 210ms; }
.slide.is-active .takeoff-trails span:nth-child(3) { animation-delay: 290ms; }

.ready-plane-ring {
  display: grid;
  width: 82px;
  height: 82px;
  place-items: center;
  border: 1px solid var(--yellow);
  border-radius: 50%;
  color: var(--yellow);
  box-shadow: 0 0 0 12px rgba(255, 212, 94, 0.04);
}

.slide.is-active .ready-plane-ring {
  animation:
    takeoff-arrive 720ms cubic-bezier(0.16, 1, 0.3, 1) 80ms both,
    ready-radar 2.2s ease-out 850ms infinite;
}

.ready-plane-ring svg {
  width: 42px;
  height: 42px;
  transform: rotate(45deg);
}

.slide.is-active .ready-plane-ring svg {
  animation: plane-settle 720ms cubic-bezier(0.16, 1, 0.3, 1) 80ms both;
}

.copy-block {
  margin-top: 2px;
  text-align: center;
}

.copy-block h1 {
  margin: 13px 0 0;
  font-size: clamp(22px, 6.5vw, 27px);
  line-height: 1.38;
  font-weight: 850;
  letter-spacing: -0.045em;
}

.description {
  margin-top: 14px;
  color: rgba(210, 225, 255, 0.76);
  font-size: 12px;
  line-height: 1.75;
  white-space: pre-line;
}

.onboarding-footer {
  padding: 12px 20px max(22px, env(safe-area-inset-bottom));
}

.page-indicator {
  display: flex;
  height: 24px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 12px;
}

.page-indicator button {
  width: 5px;
  height: 5px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.32);
  transition: width 260ms ease, background 260ms ease;
}

.page-indicator button.active {
  width: 24px;
  background: var(--yellow);
}

.primary-button,
.login-button {
  width: 100%;
  height: 52px;
  border-radius: 13px;
  font-size: 14px;
  font-weight: 800;
  transition: transform 150ms ease, filter 150ms ease;
}

.primary-button {
  border: 0;
  color: #102e6c;
  background: var(--yellow);
  box-shadow: 0 12px 24px rgba(4, 22, 59, 0.2);
  animation: button-glow 3s ease-in-out infinite;
}

.login-button {
  margin-top: 10px;
  border: 1px solid rgba(255, 255, 255, 0.54);
  color: white;
  background: rgba(255, 255, 255, 0.04);
}

.final-actions {
  opacity: 0;
  transform: translateY(18px);
  animation: final-actions-arrive 480ms cubic-bezier(0.22, 1, 0.36, 1) 820ms both;
}

.primary-button:active,
.login-button:active {
  transform: scale(0.98);
  filter: brightness(0.96);
}

@keyframes ambient-drift {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to { transform: translate3d(12px, 18px, 0) scale(1.08); }
}

@keyframes sky-streak {
  0%, 16% { opacity: 0; transform: translateX(0); }
  28% { opacity: 0.75; }
  52%, 100% { opacity: 0; transform: translateX(510%); }
}

@keyframes plane-cruise {
  0%, 100% { transform: translateY(0) rotate(-2deg); }
  50% { transform: translateY(-3px) rotate(2deg); }
}

@keyframes visual-arrive {
  from { opacity: 0; transform: translateY(16px) scale(0.94); filter: blur(5px); }
  to { opacity: 1; transform: translateY(0) scale(1); filter: blur(0); }
}

@keyframes copy-arrive {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes brand-logo-board {
  from { transform: translateY(12px) scale(0.92); filter: blur(3px) brightness(2.35) saturate(0.9) drop-shadow(0 14px 24px rgba(2, 16, 52, 0.32)); }
  70% { transform: translateY(-2px) scale(1.015); filter: blur(0) brightness(2.5) saturate(0.94) drop-shadow(0 18px 28px rgba(2, 16, 52, 0.38)); }
  to { transform: translateY(0) scale(1); filter: blur(0) brightness(2.35) saturate(0.9) drop-shadow(0 14px 24px rgba(2, 16, 52, 0.32)); }
}

@keyframes brand-logo-float {
  0%, 100% { transform: translateY(0) rotate(0); }
  50% { transform: translateY(-6px) rotate(0.8deg); }
}

@keyframes brand-halo {
  0% { opacity: 0.25; transform: scale(0.88); }
  62% { opacity: 0.72; }
  100% { opacity: 0; transform: scale(1.16); }
}

@keyframes brand-route-draw {
  from { opacity: 0; transform: rotate(-8deg) scaleX(0); }
  to { opacity: 1; transform: rotate(-8deg) scaleX(1); }
}

@keyframes brand-plane-arrive {
  from { opacity: 0; transform: translate(-88px, 28px) rotate(-22deg) scale(0.72); }
  to { opacity: 1; transform: translate(0, 0) rotate(-8deg) scale(1); }
}

@keyframes card-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

@keyframes deposit-coin {
  0% { opacity: 0; transform: translate(-50%, -14px) scale(0.72) rotate(-12deg); }
  18% { opacity: 1; }
  72% { opacity: 1; }
  100% { opacity: 0; transform: translate(-50%, 90px) scale(0.92) rotate(18deg); }
}

@keyframes saving-fill-up {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

@keyframes amount-confirm {
  0% { color: white; transform: scale(1); }
  48% { color: var(--yellow); transform: scale(1.08); }
  100% { color: white; transform: scale(1); }
}

@keyframes coin-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 212, 94, 0); }
  50% { box-shadow: 0 0 0 8px rgba(255, 212, 94, 0.1); }
}

@keyframes receipt-float {
  0%, 100% { transform: translateY(0) rotate(3deg); }
  50% { transform: translateY(-5px) rotate(1.5deg); }
}

@keyframes receipt-write {
  from { width: 0; opacity: 0.25; }
  to { width: var(--receipt-line-width); opacity: 1; }
}

@keyframes arrow-nudge {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(4px); }
}

@keyframes rollover-highlight {
  0%, 100% { text-shadow: 0 0 0 rgba(255, 212, 94, 0); }
  50% { text-shadow: 0 0 12px rgba(255, 212, 94, 0.72); }
}

@keyframes rollover-plane {
  from { opacity: 0; transform: translateX(-86px) rotate(-12deg); }
  to { opacity: 1; transform: translateX(0) rotate(0); }
}

@keyframes takeoff-trail {
  from { opacity: 0; transform: scaleX(0.25) translateX(-25px); transform-origin: right; }
  45% { opacity: 1; }
  to { opacity: 0; transform: scaleX(1) translateX(10px); transform-origin: right; }
}

@keyframes wind-stream {
  0% { opacity: 0; transform: translateX(0) scaleX(0.55); }
  14% { opacity: 0.68; }
  72% { opacity: 0.42; }
  100% { opacity: 0; transform: translateX(410%) scaleX(1); }
}

@keyframes takeoff-arrive {
  from { opacity: 0; transform: translateX(-230px) scale(0.52) rotate(-10deg); }
  70% { opacity: 1; transform: translateX(8px) scale(1.05) rotate(2deg); }
  to { opacity: 1; transform: translateX(0) scale(1) rotate(0); }
}

@keyframes plane-settle {
  from { transform: rotate(45deg) scale(0.7); }
  70% { transform: rotate(49deg) scale(1.08); }
  to { transform: rotate(45deg) scale(1); }
}

@keyframes ready-radar {
  0% { box-shadow: 0 0 0 0 rgba(255, 212, 94, 0.2); }
  70%, 100% { box-shadow: 0 0 0 22px rgba(255, 212, 94, 0); }
}

@keyframes button-glow {
  0%, 100% { box-shadow: 0 12px 24px rgba(4, 22, 59, 0.2); }
  50% { box-shadow: 0 12px 30px rgba(255, 212, 94, 0.2); }
}

@keyframes final-actions-arrive {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-height: 760px) {
  .onboarding-header { padding-top: 28px; }
  .flight-route { margin-top: 18px; }
  .visual-stage { min-height: 185px; }
  .copy-block h1 { font-size: 21px; }
  .description { margin-top: 9px; line-height: 1.55; }
  .onboarding-footer { padding-bottom: 15px; }
}

@media (prefers-reduced-motion: reduce) {
  .onboarding::after,
  .ambient,
  .route-plane svg,
  .slide.is-active .brand-logo,
  .slide.is-active .brand-logo-halo,
  .slide.is-active .brand-flight-path span,
  .slide.is-active .brand-flight-path b,
  .slide.is-active .visual-content,
  .slide.is-active .copy-block > *,
  .slide.is-active .saving-card,
  .slide.is-active .coin-icon,
  .slide.is-active .deposit-stream span,
  .slide.is-active .saving-fill span,
  .slide.is-active .saving-amount,
  .slide.is-active .receipt-card,
  .slide.is-active .receipt-line,
  .slide.is-active .receipt-arrow,
  .slide.is-active .rollover-pill strong,
  .slide.is-active .rollover-flight b,
  .slide.is-active .takeoff-trails span,
  .slide.is-active .cruise-wind span,
  .slide.is-active .ready-plane-ring,
  .slide.is-active .ready-plane-ring svg,
  .final-actions,
  .primary-button {
    animation: none;
  }

  .slides-track,
  .slide,
  .route-line-progress,
  .route-plane,
  .final-actions {
    transition: none;
  }

  .final-actions {
    opacity: 1;
    transform: none;
  }
}
</style>
