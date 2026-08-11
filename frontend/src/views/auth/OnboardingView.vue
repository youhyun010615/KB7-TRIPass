<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const slides = [
  {
    key: 'goal',
    label: 'MY TRAVEL GOAL',
    caption: '여행 목표 설정',
    title: ['여행을 위한 돈을', '함께 모아볼까요?'],
    desc: '여행 날짜와 현지 사용 예산을 정하면\n매달 필요한 저축 금액을 알려드려요.',
    gradient: 'linear-gradient(150deg, #5B7CFA 0%, #3B5BDB 100%)',
    cta: '다음',
  },
  {
    key: 'insight',
    label: 'SPENDING INSIGHT',
    caption: '소비 분석 코칭',
    title: ['내 소비에서', '여행비를 찾아드려요'],
    desc: '연결한 계좌의 소비 패턴을 분석해\n줄이면 좋은 지출을 알려드려요.',
    gradient: 'linear-gradient(150deg, #8B6EF5 0%, #6C4CE0 100%)',
    cta: '다음',
  },
  {
    key: 'wallet',
    label: 'TRAVEL WALLET',
    caption: '여행 월렛 저축',
    title: ['여행 월렛에 모으고', '다음 여행까지 이어가요'],
    desc: '여행 월렛 입금 내역으로 저축을 확인하고\n남은 돈은 다음 여행에 이월할 수 있어요.',
    gradient: 'linear-gradient(150deg, #F0A15A 0%, #E27A2E 100%)',
    cta: 'TRIPASS 시작하기',
  },
]

const current = ref(0)
const isLast = computed(() => current.value === slides.length - 1)

function goToLogin() {
  router.push('/login')
}

function next() {
  if (isLast.value) {
    goToLogin()
    return
  }
  current.value += 1
}

function goTo(index) {
  current.value = index
}

// 스와이프 제스처
const touchStartX = ref(0)
const touchDeltaX = ref(0)

function onTouchStart(e) {
  touchStartX.value = e.touches[0].clientX
  touchDeltaX.value = 0
}
function onTouchMove(e) {
  touchDeltaX.value = e.touches[0].clientX - touchStartX.value
}
function onTouchEnd() {
  const threshold = 50
  if (touchDeltaX.value < -threshold && current.value < slides.length - 1) {
    current.value += 1
  } else if (touchDeltaX.value > threshold && current.value > 0) {
    current.value -= 1
  }
  touchDeltaX.value = 0
}
</script>

<template>
  <div
    class="min-h-screen flex flex-col relative overflow-hidden"
    style="background: linear-gradient(to bottom, #263F8C 0%, #172F6B 100%)"
  >
    <!-- 데코 원형 배경 -->
    <div
      class="absolute top-0 right-0 w-72 h-72 rounded-full pointer-events-none"
      style="background: rgba(255,255,255,0.06); transform: translate(35%, -20%)"
    ></div>
    <div
      class="absolute top-20 right-6 w-56 h-56 rounded-full pointer-events-none"
      style="background: rgba(255,255,255,0.04)"
    ></div>

    <!-- 로고 바 -->
    <div class="relative z-10 flex items-center justify-between px-5 pt-12">
      <span class="text-white font-bold text-xs tracking-[0.2em]">TRIPASS</span>
      <button class="flex items-center gap-1.5 text-white/60 text-xs font-medium" @click="goToLogin">
        <span class="text-sm">✈️</span>
        건너뛰기
      </button>
    </div>

    <!-- 슬라이드 캐러셀 -->
    <div
      class="relative z-10 mt-6 overflow-hidden"
      @touchstart="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
    >
      <div
        class="flex transition-transform duration-500 ease-out"
        :style="`transform: translateX(-${current * 100}%)`"
      >
        <div v-for="slide in slides" :key="slide.key" class="w-full flex-none px-5">
          <!-- 컬러 카드 -->
          <div
            class="relative overflow-hidden rounded-[28px] aspect-[4/3] flex flex-col items-center justify-center px-6"
            :style="`background:${slide.gradient}`"
          >
            <div
              class="absolute w-40 h-40 rounded-full pointer-events-none"
              style="background: rgba(255,255,255,0.10); right:-30px; top:-30px"
            ></div>
            <span class="absolute top-5 left-6 text-white/70 text-[11px] font-bold tracking-[0.15em]">
              {{ slide.label }}
            </span>

            <div class="relative z-10 flex flex-col items-center gap-6">
              <div v-if="slide.key === 'goal'" class="flex items-center gap-2 text-white text-2xl">
                <span>🛫</span>
                <span class="tracking-[6px] text-white/70 text-lg">••••••</span>
                <span class="w-6 h-6 rounded-full border-[3px] border-white bg-white/20"></span>
              </div>
              <div v-else-if="slide.key === 'insight'" class="flex items-center gap-3 text-white text-2xl font-semibold">
                <span>₩</span>
                <span class="text-white/70">+</span>
                <span>🛫</span>
                <span class="text-white/70">=</span>
                <span>♡</span>
              </div>
              <div v-else class="flex items-center gap-3 text-white text-2xl">
                <span>☰</span>
                <span class="text-white/50">·</span>
                <span>₩</span>
                <span class="text-white/50">·</span>
                <span>🛫</span>
              </div>

              <span class="text-white/70 text-xs font-medium tracking-wide">{{ slide.caption }}</span>
            </div>
          </div>

          <!-- 타이틀 & 설명 -->
          <div class="mt-8 text-center">
            <h1 class="text-white text-[24px] font-bold leading-snug">
              <template v-for="(line, i) in slide.title" :key="i">
                {{ line }}<br v-if="i < slide.title.length - 1" />
              </template>
            </h1>
            <p class="text-blue-200/80 text-[13px] mt-3 leading-relaxed whitespace-pre-line">
              {{ slide.desc }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 하단 고정 영역 -->
    <div class="relative z-10 mt-auto px-5 pb-10 pt-6">
      <!-- 페이지 인디케이터 -->
      <div class="flex items-center justify-center gap-2 mb-6">
        <button
          v-for="(slide, i) in slides"
          :key="slide.key"
          class="h-1.5 rounded-full transition-all duration-300"
          :class="i === current ? 'w-6 bg-white' : 'w-1.5 bg-white/30'"
          @click="goTo(i)"
        ></button>
      </div>

      <!-- CTA 버튼 -->
      <button
        type="button"
        class="w-full h-14 rounded-2xl text-[#172F6B] font-bold text-base bg-white transition-transform active:scale-[0.98]"
        @click="next"
      >
        {{ slides[current].cta }}
      </button>
    </div>
  </div>
</template>
