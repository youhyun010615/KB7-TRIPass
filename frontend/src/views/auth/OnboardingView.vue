<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

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
const isLast = computed(() => current.value === slides.length - 1)
const routeProgress = computed(() => `${(current.value / (slides.length - 1)) * 100}%`)

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
}

function onTouchMove(event) {
  touchDeltaX.value = event.touches[0].clientX - touchStartX.value
}

function onTouchEnd() {
  const swipeThreshold = 45

  if (touchDeltaX.value < -swipeThreshold && current.value < slides.length - 1) {
    current.value += 1
  } else if (touchDeltaX.value > swipeThreshold && current.value > 0) {
    current.value -= 1
  }

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
        :style="{ transform: `translateX(-${current * 100}%)` }"
      >
        <article
          v-for="(slide, index) in slides"
          :key="slide.key"
          class="slide"
          :aria-hidden="current !== index"
        >
          <div class="visual-stage">
            <Transition name="visual-pop" mode="out-in">
              <div :key="`${slide.key}-${current}`" class="visual-content">
                <div v-if="slide.key === 'journey'" class="brand-visual">
                  <div class="brand-word">TRIPASS</div>
                  <div class="brand-rule"></div>
                  <div class="brand-subtitle">FLIGHT MODE · MONEY</div>
                </div>

                <div v-else-if="slide.key === 'saving'" class="saving-visual">
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
                </div>

                <div v-else class="ready-visual">
                  <div class="ready-plane-ring">
                    <svg viewBox="0 0 24 24" fill="currentColor">
                      <path d="M21.7 11.2 14 7.1V3.6a2 2 0 0 0-4 0v3.5l-7.7 4.1a1.5 1.5 0 0 0-.8 1.3v1.2l8.5-2.2v4.2l-2.3 1.8v1l4.3-1 4.3 1v-1L14 15.7v-4.2l8.5 2.2v-1.2a1.5 1.5 0 0 0-.8-1.3Z" />
                    </svg>
                  </div>
                </div>
              </div>
            </Transition>
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

      <button type="button" class="primary-button" @click="next">
        {{ isLast ? 'TRIPASS 시작하기' : '다음' }}
      </button>
      <button v-if="isLast" type="button" class="login-button" @click="goToLogin">
        이미 계정이 있어요 · 로그인
      </button>
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
}

.ambient-bottom {
  bottom: -110px;
  left: -82px;
  width: 250px;
  height: 250px;
  background: rgba(42, 91, 181, 0.34);
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

.slide {
  display: flex;
  width: 100%;
  flex: 0 0 100%;
  flex-direction: column;
  align-items: center;
  padding: 5px 26px 0;
}

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

.brand-word {
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 0.05em;
}

.brand-rule {
  width: 42px;
  height: 2px;
  margin: 16px 0 12px;
  background: rgba(255, 255, 255, 0.34);
}

.brand-subtitle,
.eyebrow {
  color: var(--yellow);
}

.saving-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

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

.receipt-title {
  margin-bottom: 14px;
  font-size: 10px;
  font-weight: 800;
}

.receipt-line {
  width: 74%;
  height: 5px;
  margin-bottom: 7px;
  border-radius: 10px;
  background: #e8e0c9;
}

.receipt-line.line-long { width: 88%; }
.receipt-line.line-short { width: 58%; }

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

.rollover-pill {
  margin-top: 19px;
  padding: 9px 14px;
  font-size: 9px;
}

.ready-visual {
  display: flex;
  justify-content: center;
}

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

.ready-plane-ring svg {
  width: 42px;
  height: 42px;
  transform: rotate(45deg);
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
}

.login-button {
  margin-top: 10px;
  border: 1px solid rgba(255, 255, 255, 0.54);
  color: white;
  background: rgba(255, 255, 255, 0.04);
}

.primary-button:active,
.login-button:active {
  transform: scale(0.98);
  filter: brightness(0.96);
}

.visual-pop-enter-active,
.visual-pop-leave-active {
  transition: opacity 260ms ease, transform 360ms cubic-bezier(0.22, 1, 0.36, 1);
}

.visual-pop-enter-from {
  opacity: 0;
  transform: translateY(14px) scale(0.96);
}

.visual-pop-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
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
  .slides-track,
  .route-line-progress,
  .route-plane,
  .visual-pop-enter-active,
  .visual-pop-leave-active {
    transition: none;
  }
}
</style>
