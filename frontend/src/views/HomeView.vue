<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTravelModeStore } from '@/stores/travelMode'
import { useTravelStore } from '@/stores/travel'
import { useSavingsPlanStore } from '@/stores/savingsPlan'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import TravelTicket from '@/components/savings/TravelTicket.vue'
import TravelModeHome from '@/components/travel/TravelModeHome.vue'

const authStore = useAuthStore()
const travelModeStore = useTravelModeStore()
const travelStore = useTravelStore()
const plan = useSavingsPlanStore()
const router = useRouter()
const isModeSwitching = ref(false)
const nextMode = ref('travel')

function switchMode(mode) {
  if (mode === travelModeStore.mode || isModeSwitching.value) return
  nextMode.value = mode
  isModeSwitching.value = true
  window.setTimeout(() => {
    travelModeStore.setMode(mode)
    isModeSwitching.value = false
  }, 1000)
}

const userName = computed(() => authStore.user?.name ?? '권유현')

// ── 여행 저축 모드 데이터 ──────────────────────────────────
const countries = [
  {
    id: 1, name: '파리', flag: '🇫🇷', code: 'PAR',
    image: '/images/france.png',
    headerBg: '#1a2d6e',
    progressBg: 'rgba(0,35,149,0.80)',
    barColor: 'linear-gradient(90deg,#002395 0%,#EDEDED 50%,#ED2939 100%)',
    dday: 230, currency: 'EUR', rate: 1548,
    desc: '로맨틱한 파리의 밤 · 파리에서의 하루를 기대하며',
  },
  {
    id: 2, name: '인터라켄', flag: '🇨🇭', code: 'INT',
    image: '/images/switzerland.webp',
    headerBg: '#7a0d1e',
    progressBg: 'rgba(122,13,30,0.82)',
    barColor: 'linear-gradient(90deg,#FF0000 0%,#FFFFFF 60%,#FF0000 100%)',
    dday: 230, currency: 'CHF', rate: 1620,
    desc: '알프스의 맑은 공기 · 인터라켄에서 시작되는 설레는 하루',
  },
  {
    id: 3, name: '베를린', flag: '🇩🇪', code: 'BER',
    image: '/images/germany.png',
    headerBg: '#111111',
    progressBg: 'rgba(17,17,17,0.85)',
    barColor: 'linear-gradient(90deg,#000000 0%,#DD0000 50%,#FFCE00 100%)',
    dday: 230, currency: 'EUR', rate: 1548,
    desc: '클래식과 트렌드가 만나는 도시 · 베를린의 하루를 기대하며',
  },
  {
    id: 4, name: '도쿄', flag: '🇯🇵', code: 'TYO',
    image: '/images/japan.webp',
    headerBg: '#c2185b',
    progressBg: 'rgba(194,24,91,0.82)',
    barColor: 'linear-gradient(90deg,#FFFFFF 0%,#BC002D 35%,#BC002D 65%,#FFFFFF 100%)',
    dday: 230, currency: 'JPY', rate: 9,
    desc: '익숙함 속 새로운 발견 · 도쿄에서의 하루를 기대하며',
  },
  {
    id: 5, name: '다낭', flag: '🇻🇳', code: 'DAD',
    image: '/images/vietnam.png',
    headerBg: '#b8860b',
    progressBg: 'rgba(184,134,11,0.82)',
    barColor: 'linear-gradient(90deg,#DA251D 0%,#FFCD00 50%,#DA251D 100%)',
    dday: 230, currency: 'VND', rate: 0.06,
    desc: '바다와 햇살이 머무는 곳 · 다낭에서의 여유로운 하루',
  },
]
const selectedCountry = ref(countries[0])
const showCountryDropdown = ref(false)

// 탑승권 저축 상태: unset(미설정) / low(부족) / ok(정상)
const savingsCardState = computed(() => {
  if (!plan.savingMethod || !plan.monthlySavings) return 'unset'
  if (plan.status === 'warning' || plan.status === 'error') return 'low'
  return 'ok'
})

const homeGoalAmount = computed(() => plan.totalTargetAmount)
const homeSavedAmount = computed(() => plan.securedAmount)
const homeSavingsPercent = computed(() => plan.securedPercent)
const ticketSavingCopy = computed(() => {
  if (savingsCardState.value === 'unset') {
    return {
      title: '월 저축 계획이 필요해요',
      action: '월 저축 계획 설정',
      amountLabel: `추천 월 ${formatCurrency(plan.recommendedMonthlySavings)}`,
    }
  }
  if (savingsCardState.value === 'low') {
    return {
      title: '목표 일정까지 저축액이 부족해요',
      action: '월 저축 계획 조정',
      amountLabel: `추가 월 ${formatCurrency(plan.additionalRecommendedAmount)}`,
    }
  }
  return {
    title: '여행 저축 목표',
    action: '여행 목표 자금 관리',
    amountLabel: '',
  }
})

const savingsData = {
  balance: 12500000,
  accounts: 2,
  saved: 2000000,
  goal: 5000000,
  monthly: {
    available: 500000,
    total: 3500000,
    details: '급여일 3,500,000원 - 고정지출 1,800,000원 - 카테고리 목표 1,200,000원',
  },
  categories: [
    { icon: '🍴', name: '식비', spent: 180000, budget: 450000, percent: 72, color: '#173b86' },
    { icon: '☕', name: '카페', spent: 42000, budget: 100000, percent: 42, color: '#315ca8' },
    { icon: '🧴', name: '생활비', spent: 126000, budget: 300000, percent: 70, color: '#25ad79' },
    { icon: '🛍', name: '쇼핑', spent: 48000, budget: 150000, percent: 60, color: '#f0a000' },
    { icon: '🎮', name: '취미', spent: 35000, budget: 100000, percent: 50, color: '#173b86' },
  ],
  schedule: [
    { date: '7/25', label: '급여일', desc: '2,600,000원 입금 예정', type: '입금' },
    { date: '7/28', label: '월세', desc: '350,000원 납부 예정', type: '지출' },
    { date: '7/31', label: '통신비', desc: '61,000원 납부 예정', type: '지출' },
  ],
}

// ── 여행 모드 데이터 ──────────────────────────────────────
const travelData = {
  totalNights: 2,
  totalDays: 15,
  dday: 13,
  remainTotal: 1590000,
  countries: [
    { name: '파리', flag: '🇫🇷', remain: 590000, currency: 'EUR', rate: 128.1 },
    { name: '스위스', flag: '🇨🇭', remain: 1000000, currency: 'CHF', rate: 129.1 },
  ],
  goalAmount: 5000000,
  prepaidAmount: 1800000,
  fundPercent: 32,
  categories: [
    { icon: '🍽', name: '식비', amount: 600000, color: '#3B5BDB' },
    { icon: '☕', name: '카페', amount: 320000, color: '#60A5FA' },
    { icon: '🏠', name: '생활비', amount: 430000, color: '#F59E0B' },
    { icon: '🛍', name: '쇼핑', amount: 130000, color: '#EC4899' },
    { icon: '🎨', name: '취미·여가', amount: 110000, color: '#8B5CF6' },
    { icon: '💬', name: '기타', amount: 110000, color: '#9CA3AF' },
  ],
  schedule: [
    { date: '2026.08.15 (수)', icon: '🏛', label: '루브르 박물관 가이드 투어', status: '사전결제 완료' },
    { date: '2026.08.21 (금)', icon: '🏨', label: '쉐마트 마티로큰 살레 숙소', status: '현장결제 필요', urgent: true },
  ],
  recent: [
    { icon: '🍽', place: 'Café de Flore (프랑스)', category: '식비', time: '오늘 12:30', amount: -18360 },
    { icon: '🏪', place: 'Swiss Fondue House (스위스)', category: '식비', time: '어제 12:30', amount: -68400 },
  ],
}

const travelSelectedCountry = ref(travelData.countries[0])

function formatCurrency(n) {
  return Math.abs(n).toLocaleString('ko-KR') + '원'
}
</script>

<template>
  <div class="min-h-screen pb-20" style="background: #F7F4EE">

    <!-- ══ 여행 미등록 홈 ══════════════════════════════════════ -->
    <template v-if="travelModeStore.isSavingsMode && !travelStore.hasTravelGoal">
      <div class="px-5 pt-12 pb-3 bg-white/80">
        <div class="flex items-center justify-between">
          <button class="px-3 py-1.5 rounded-full text-xs font-bold" style="background:#eef2ff;color:#263f8c">여행 저축</button>
          <button class="w-8 h-8 rounded-full text-sm" style="background:#fff1d9;color:#d97706">♧</button>
        </div>
        <p class="mt-2 text-lg font-extrabold">안녕하세요, {{ userName }}님</p>
        <p class="mt-1 text-[10px] text-slate-500">새로운 여행을 함께 준비해 볼까요?</p>
      </div>

      <div class="px-4 mt-3">
        <TravelTicket eyebrow="TRIPASS · START JOURNEY">
          <div class="py-1 text-center">
            <div class="mb-2 text-2xl">✈</div>
            <h2 class="text-[16px] font-extrabold">아직 등록된 여행이 없어요</h2>
            <p class="mt-2 text-[10px] leading-4 text-blue-100">여행을 등록하면 목표 금액을 설정하고<br>저축 계획까지 한 번에 도와드려요.</p>
            <button class="w-full h-11 mt-4 rounded-xl text-[12px] font-extrabold text-white" style="background:#ff7a36" @click="router.push('/savings')">여행 계획 등록하기</button>
            <div class="grid grid-cols-2 gap-3 mt-4 text-left">
              <div><span class="block text-[8px] text-blue-200">보유 총자산</span><b class="text-[14px]">12,500,000원</b></div>
              <div class="border-l border-white/20 pl-3"><span class="block text-[8px] text-blue-200">연결 계좌</span><b class="text-[14px]">2개</b></div>
            </div>
          </div>
        </TravelTicket>
      </div>

      <button class="block w-[calc(100%-2rem)] mx-4 mt-3 p-4 rounded-2xl bg-white shadow-sm text-left" @click="router.push('/savings/monthly')">
        <div class="w-full flex justify-between items-center"><h2 class="text-[13px] font-extrabold">이달의 자금 체크</h2><span>›</span></div>
        <p class="mt-3 text-[9px] text-slate-400">이달의 여유자금</p>
        <p class="text-2xl font-extrabold" style="color:#0066ff">500,000원</p>
        <p class="mt-1 text-[8px] text-slate-400">월급 3,500,000원 · 고정지출 1,800,000원 · 카테고리 목표 1,200,000원</p>
        <div class="mt-5 pt-4 border-t border-gray-100 flex justify-between"><h2 class="text-[13px] font-extrabold">카테고리별 사용 현황</h2><span class="text-[9px] text-slate-400">이번 달</span></div>
        <div v-for="cat in savingsData.categories" :key="cat.name" class="grid grid-cols-[48px_1fr_34px] items-center gap-2 mt-3">
          <span class="text-[9px] font-semibold">{{ cat.name }}</span><div class="h-1.5 rounded bg-slate-100"><i class="block h-full rounded" :style="`width:${cat.percent}%;background:${cat.color}`" /></div><b class="text-right text-[9px]" :style="`color:${cat.color}`">{{ cat.percent }}%</b>
        </div>
      </button>

      <section class="mx-4 mt-3 p-4 rounded-2xl bg-white shadow-sm cursor-pointer" role="button" tabindex="0" @click="router.push('/financial-schedule')" @keydown.enter="router.push('/financial-schedule')">
        <button class="w-full flex justify-between" @click.stop="router.push('/financial-schedule')"><h2 class="text-[13px] font-extrabold">다가오는 금융 일정</h2><span>›</span></button>
        <div v-for="item in savingsData.schedule" :key="item.date" class="grid grid-cols-[38px_1fr_30px] mt-3 text-[9px]"><b style="color:#0066ff">{{ item.date }}</b><span>{{ item.label }}</span><strong :style="item.type==='입금' ? 'color:#16a36a' : 'color:#263f8c'">{{ item.type }}</strong></div>
      </section>
    </template>

    <!-- ══ 여행 저축 모드 ══════════════════════════════════════ -->
    <template v-else-if="travelModeStore.isSavingsMode">

      <!-- 헤더 -->
      <div class="bg-white px-5 pt-10 pb-3 relative">
        <div class="flex items-center justify-between">
          <div>
            <div class="mode-switch-control savings-selected">
              <span class="mode-switch-thumb" />
              <button type="button" @click="switchMode('travel')">여행</button>
              <button type="button" class="selected" @click="switchMode('savings')">저축</button>
            </div>
            <h1 class="text-[20px] font-extrabold text-gray-900 mt-0.5">안녕하세요, {{ userName }}님</h1>
            <button class="text-[11px] text-gray-400 mt-0.5" @click="router.push('/travel/register')">여행 계획 수정하기 ›</button>
          </div>
          <!-- 국가 드롭다운 -->
          <div class="relative flex-none">
            <button
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-full border border-gray-200 text-[12px] font-bold text-gray-800"
              @click="showCountryDropdown = !showCountryDropdown"
            >
              <span>{{ selectedCountry.flag }}</span>
              <span>{{ selectedCountry.name }}</span>
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
                <path d="M6 9L12 15L18 9" stroke="#6B7280" stroke-width="2.5" stroke-linecap="round"/>
              </svg>
            </button>
            <div v-if="showCountryDropdown" class="absolute right-0 top-9 bg-white rounded-xl shadow-lg border border-gray-100 py-1 z-20 min-w-[120px]">
              <button
                v-for="c in countries" :key="c.id"
                class="w-full flex items-center gap-2 px-3 py-2 text-[12px] text-gray-700 hover:bg-gray-50 active:bg-gray-100"
                :class="{ 'font-extrabold': selectedCountry.id === c.id }"
                @click="selectedCountry = c; showCountryDropdown = false"
              >
                <span>{{ c.flag }}</span><span>{{ c.name }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- BOARDING PASS 카드 -->
      <div class="country-ticket mx-4 mt-2 overflow-hidden" :style="`background:${selectedCountry.headerBg}`">

        <!-- ① 헤더 스트립 (나라 컬러, 짧게) -->
        <div class="px-5 pt-4 pb-3 flex items-center justify-between"
             :style="`background:${selectedCountry.headerBg}`">
          <span class="text-white/65 text-[9px] font-bold tracking-widest">BOARDING PASS</span>
          <span class="text-white/40 text-[9px] tracking-widest">TRIPASS AIR</span>
          <span class="text-white/65 text-[9px] font-semibold">NO. {{ selectedCountry.code }}-{{ selectedCountry.dday }}</span>
        </div>

        <!-- 사진의 시작 경계와 정확히 맞닿는 상단 절취선 -->
        <div class="ticket-cutline ticket-cutline-top">
          <div class="ticket-notch ticket-notch-left" />
          <div class="ticket-dashed-line" />
          <div class="ticket-notch ticket-notch-right" />
        </div>

        <!-- ② 사진 전체 배경 섹션 (나머지 전부) -->
        <div class="relative" :style="`background:url(${selectedCountry.image}) center/cover no-repeat`">
          <!-- 어두운 오버레이 -->
          <div class="absolute inset-0 bg-black/30 pointer-events-none z-0" />

          <div class="relative z-10 px-5 pt-6">
            <!-- DESTINATION / DEPARTURE / 설명 -->
            <div class="flex items-center gap-2">
              <div class="flex-none">
                <p class="text-white/50 text-[8px] uppercase tracking-widest mb-0.5">Destination</p>
                <p class="text-white text-[22px] font-extrabold leading-none">{{ selectedCountry.flag }} {{ selectedCountry.name }}</p>
              </div>
              <div class="flex-1 flex items-center mt-3.5">
                <div class="flex-1 border-t border-dashed border-white/40" />
                <span class="mx-2 text-yellow-300 text-lg">✈</span>
                <div class="flex-1 border-t border-dashed border-white/40" />
              </div>
              <div class="text-right flex-none">
                <p class="text-white/50 text-[8px] uppercase tracking-widest mb-0.5">Departure</p>
                <p class="text-white text-[22px] font-extrabold leading-none">D-{{ selectedCountry.dday }}</p>
              </div>
            </div>
            <p class="ticket-description text-white/80 text-[10px] mt-2">{{ selectedCountry.desc }} ✨</p>

            <!-- 사진이 보이는 여백 -->
            <div class="ticket-photo-space" />

            <!-- 진행 박스 (반투명, 사진 위에 떠있음) -->
            <div class="rounded-xl px-4 py-4" :style="`background:${selectedCountry.progressBg}`">
              <div class="flex justify-between mb-2">
                <span class="font-semibold text-[11px]" :class="savingsCardState === 'unset' ? 'text-red-300' : 'text-white'">{{ ticketSavingCopy.title }}</span>
                <span class="text-white font-extrabold text-[12px]">{{ homeSavingsPercent }}%</span>
              </div>
              <div class="h-2 rounded-full bg-white/25 overflow-hidden">
                <div class="h-full rounded-full transition-all" :style="`width:${homeSavingsPercent}%;background:${selectedCountry.barColor}`" />
              </div>
              <div class="flex justify-between mt-2.5">
                <div>
                  <p class="text-white text-[11px] font-bold">{{ formatCurrency(homeSavedAmount) }}</p>
                  <p class="text-white/55 text-[7px] tracking-wider mt-0.5">{{ ticketSavingCopy.amountLabel || 'SAVED' }}</p>
                </div>
                <div class="text-right">
                  <p class="text-white text-[11px] font-bold">{{ formatCurrency(homeGoalAmount) }}</p>
                  <p class="text-white/50 text-[7px] tracking-wider mt-0.5">GOAL</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 사진의 종료 경계와 정확히 맞닿는 하단 절취선 -->
          <div class="ticket-cutline ticket-cutline-bottom">
            <div class="ticket-notch ticket-notch-left" />
            <div class="ticket-dashed-line" />
            <div class="ticket-notch ticket-notch-right" />
          </div>

          <!-- ④ 국가 컬러 스텁 -->
          <div class="relative z-10" :style="`background:${selectedCountry.headerBg}`">
            <button
              class="ticket-stub w-full px-5 flex items-center justify-between active:bg-gray-50"
              @click="router.push(savingsCardState === 'ok' ? '/savings' : '/savings/plan')">
              <span class="text-[10px] font-bold text-white">{{ ticketSavingCopy.action }}</span>
              <div class="flex items-center gap-2">
                <div class="flex gap-[1.5px] items-end h-5">
                  <div v-for="(h,i) in [14,7,20,5,14,9,20,5,16,5,12,8,18,5,14]" :key="i"
                    class="bg-white/85 rounded-[0.5px]"
                    :style="`height:${h}px;width:${i%4===0?'2.5px':'1.5px'}`" />
                </div>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path d="M9 18L15 12L9 6" stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round"/>
                </svg>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- 보유 총자산 / 연동 계좌 -->
      <div class="mx-4 mt-3 bg-white rounded-2xl px-5 py-4 flex items-center shadow-sm">
        <div class="flex-1">
          <p class="text-[10px] text-gray-400 mb-1">보유 총자산</p>
          <p class="text-[16px] font-extrabold text-gray-900">{{ formatCurrency(savingsData.balance) }}</p>
        </div>
        <div class="w-px h-8 bg-gray-100 mx-4" />
        <div class="text-right">
          <p class="text-[10px] text-gray-400 mb-1">연동 계좌</p>
          <p class="text-[16px] font-extrabold text-gray-900">{{ savingsData.accounts }}개</p>
        </div>
        <svg class="ml-2 flex-none" width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </div>

      <!-- 이달의 자금 체크 + 카테고리별 사용 현황 -->
      <button class="block w-[calc(100%-2rem)] mx-4 mt-3 bg-white rounded-2xl px-5 py-4 shadow-sm text-left" @click="router.push('/savings/monthly')">
        <div class="w-full flex items-center justify-between mb-2">
          <p class="text-[14px] font-extrabold text-gray-900">이달의 자금 체크</p>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <p class="text-[11px] text-gray-400 mb-1.5">이달의 여유자금</p>
        <p class="text-[28px] font-extrabold" style="color:#3B5BDB">{{ formatCurrency(savingsData.monthly.available) }}</p>
        <p class="text-[10px] text-gray-400 mt-1 leading-relaxed">{{ savingsData.monthly.details }}</p>
        <div class="mt-5 pt-4 border-t border-gray-100 flex items-center justify-between mb-3">
          <p class="text-[14px] font-extrabold text-gray-900">카테고리별 사용 현황</p>
          <span class="text-[11px] text-gray-400">이번 달</span>
        </div>
        <div class="flex flex-col gap-3">
          <div v-for="cat in savingsData.categories" :key="cat.name" class="flex items-center gap-2">
            <span class="text-[15px] w-5 flex-none">{{ cat.icon }}</span>
            <span class="text-[11px] text-gray-600 w-10 flex-none">{{ cat.name }}</span>
            <div class="flex-1 h-1.5 rounded-full bg-gray-100">
              <div class="h-full rounded-full" :style="`width:${cat.percent}%;background:${cat.color}`" />
            </div>
            <span class="text-[10px] text-gray-400 w-[90px] text-right flex-none">
              {{ cat.spent.toLocaleString('ko-KR') }}/{{ cat.budget.toLocaleString('ko-KR') }}
            </span>
            <span class="text-[11px] font-bold w-8 text-right flex-none" :style="`color:${cat.color}`">{{ cat.percent }}%</span>
          </div>
        </div>
      </button>

      <!-- 다가오는 금융 일정 -->
      <div class="mx-4 mt-3 bg-white rounded-2xl px-5 py-4 shadow-sm cursor-pointer" role="button" tabindex="0" @click="router.push('/financial-schedule')" @keydown.enter="router.push('/financial-schedule')">
        <button class="w-full flex items-center justify-between mb-3" @click.stop="router.push('/financial-schedule')">
          <p class="text-[14px] font-extrabold text-gray-900">다가오는 금융 일정</p>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
        <div class="flex flex-col gap-3">
          <div v-for="item in savingsData.schedule" :key="item.date" class="flex items-center gap-3">
            <p class="text-[13px] font-extrabold w-8 flex-none" style="color:#3B5BDB">{{ item.date }}</p>
            <div class="flex-1 min-w-0">
              <p class="text-[13px] font-semibold text-gray-900">{{ item.label }}</p>
              <p class="text-[10px] text-gray-400 mt-0.5">{{ item.desc }}</p>
            </div>
            <span
              class="text-[10px] font-semibold px-2.5 py-1 rounded-full flex-none"
              :style="item.type === '입금'
                ? 'background:#F0FDF4;color:#16A34A'
                : 'background:#EEF2FF;color:#3B5BDB'"
            >{{ item.type }}</span>
          </div>
        </div>
      </div>

      <!-- 오늘의 환율 -->
      <div class="mx-4 mt-3 mb-4 rounded-2xl overflow-hidden" :style="`background:${selectedCountry.headerBg}`">
        <div class="px-4 py-2.5 border-b border-white/10 flex items-center justify-between">
          <span class="text-white/60 text-[11px] font-semibold">오늘의 환율</span>
          <span class="text-white/40 text-[10px]">{{ new Date().toLocaleDateString('ko-KR') }} 기준</span>
        </div>
        <div class="px-4 py-3 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="text-[14px]">{{ selectedCountry.flag }}</span>
            <span class="text-white font-bold text-[14px]">{{ selectedCountry.currency }}/KRW</span>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-white text-[22px] font-extrabold">{{ selectedCountry.rate.toLocaleString('ko-KR') }}원</span>
            <span class="text-[10px] font-semibold px-1.5 py-0.5 rounded text-blue-300">-0.3% ↓</span>
          </div>
        </div>
      </div>

    </template>

    <!-- ══ 여행 모드 ══════════════════════════════════════════ -->
    <template v-else-if="false">

      <!-- 헤더 -->
      <div class="bg-white px-5 pt-14 pb-3">
        <div class="flex items-center justify-between">
          <!-- 모드 전환 버튼 -->
          <button
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold"
            style="background: #EEF2FF; color: #3B5BDB"
            @click="travelModeStore.setMode('savings')"
          >
            <span>여행 모드</span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
              <path d="M6 9L12 15L18 9" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>

          <!-- 국가 드롭다운 -->
          <div class="flex items-center gap-1.5">
            <span>{{ travelSelectedCountry.flag }}</span>
            <span class="text-sm font-semibold text-gray-900">{{ travelSelectedCountry.name }}</span>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M6 9L12 15L18 9" stroke="#6B7280" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
        </div>
        <p class="text-xl font-bold text-gray-900 mt-3">안녕하세요, {{ userName }}님</p>
      </div>

      <!-- BOARDING PASS (여행 모드) -->
      <div class="mx-4 mt-4 rounded-2xl overflow-hidden" style="background: linear-gradient(135deg, #2A4DB0 0%, #1A337A 100%)">
        <div class="px-4 pt-4 pb-5">
          <div class="flex justify-between text-white/60 text-[10px] mb-3">
            <span class="tracking-widest">BOARDING PASS · TRIPASS AIR</span>
            <span>NO. EUR-230</span>
          </div>

          <!-- 전체 여행 잔여 자산 -->
          <p class="text-white/60 text-xs mb-1">전체 남은 여행 자산 (환산)</p>
          <p class="text-white text-3xl font-bold">{{ formatCurrency(travelData.remainTotal) }}</p>

          <!-- 국가별 잔여 -->
          <div class="flex gap-4 mt-3">
            <div v-for="c in travelData.countries" :key="c.name">
              <p class="text-white/60 text-[10px]">{{ c.flag }} {{ c.name }} 남은 여행 자산</p>
              <p class="text-white font-bold text-sm">{{ formatCurrency(c.remain) }}</p>
              <p class="text-white/50 text-[10px]">(약 {{ c.currency }} {{ c.rate }})</p>
            </div>
          </div>

          <!-- 자금 진행률 -->
          <div class="mt-4">
            <div class="flex justify-between text-xs text-white/70 mb-1.5">
              <p class="text-white/60 text-[11px]">남은 5일 동안 하루에 쓸 수 있는 금액</p>
            </div>
            <div class="h-1.5 rounded-full bg-white/20">
              <div class="h-full rounded-full bg-white" :style="`width: ${travelData.fundPercent}%`"/>
            </div>
            <div class="flex justify-between text-[10px] text-white/60 mt-1">
              <span>목표 {{ formatCurrency(travelData.goalAmount) }}</span>
              <span>사전 지불 {{ formatCurrency(travelData.prepaidAmount) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 여행 자금 체크 -->
      <div class="mx-4 mt-3 bg-white rounded-2xl px-5 py-4">
        <p class="text-sm font-bold text-gray-900 mb-3">여행 자금 체크</p>
        <div class="flex flex-col gap-2.5">
          <div v-for="cat in travelData.categories" :key="cat.name" class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <span class="text-base">{{ cat.icon }}</span>
              <span class="text-sm text-gray-700">{{ cat.name }}</span>
            </div>
            <span class="text-sm font-semibold text-gray-900">{{ formatCurrency(cat.amount) }}</span>
          </div>
          <div class="border-t border-gray-100 pt-2 flex justify-between">
            <span class="text-sm font-bold text-gray-900">지출 총합</span>
            <span class="text-sm font-bold" style="color: #3B5BDB">
              {{ formatCurrency(travelData.categories.reduce((s, c) => s + c.amount, 0)) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 다가오는 여행 일정 -->
      <div class="mx-4 mt-3 bg-white rounded-2xl px-5 py-4">
        <p class="text-sm font-bold text-gray-900 mb-3">다가오는 여행 일정</p>
        <div class="flex flex-col gap-3">
          <div v-for="item in travelData.schedule" :key="item.label">
            <p class="text-xs font-semibold mb-1" style="color: #3B5BDB">{{ item.date }}</p>
            <div class="flex items-center justify-between">
              <p class="text-sm text-gray-900">{{ item.icon }} {{ item.label }}</p>
              <span
                class="text-[11px] font-semibold px-2 py-0.5 rounded-full"
                :class="item.urgent
                  ? 'bg-red-50 text-red-500'
                  : 'bg-blue-50 text-blue-600'"
              >{{ item.status }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 최근 지출 내역 -->
      <div class="mx-4 mt-3 mb-4 bg-white rounded-2xl px-5 py-4">
        <div class="flex items-center justify-between mb-3">
          <p class="text-sm font-bold text-gray-900">최근 지출 내역</p>
          <span class="text-xs text-gray-400">전체 보기</span>
        </div>
        <div class="flex flex-col gap-3">
          <div v-for="item in travelData.recent" :key="item.place" class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <span class="text-xl">{{ item.icon }}</span>
              <div>
                <p class="text-sm text-gray-900">{{ item.place }}</p>
                <p class="text-xs text-gray-400">{{ item.category }} · {{ item.time }}</p>
              </div>
            </div>
            <p class="text-sm font-semibold text-gray-900">- {{ formatCurrency(item.amount) }}</p>
          </div>
        </div>
      </div>

    </template>

    <TravelModeHome v-else :user-name="userName" :on-switch-mode="switchMode" />

    <Transition name="flight-fade">
      <div v-if="isModeSwitching" class="mode-flight-loader" role="status" aria-live="polite">
        <div class="flight-path"><span>✈</span></div>
        <strong>{{ nextMode === 'travel' ? '여행 모드로 이동 중' : '저축 모드로 이동 중' }}</strong>
        <small>TRIPass가 새로운 여정을 준비하고 있어요</small>
      </div>
    </Transition>

    <BottomNav />
  </div>
</template>

<style scoped>
.country-ticket {
  border-radius: 18px;
  box-shadow: 0 10px 24px rgba(22, 39, 78, .15);
}
.ticket-photo-space { height: 104px; }
.ticket-description { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ticket-stub { height: 45px; }
.ticket-cutline { position: relative; z-index: 20; display: flex; align-items: center; height: 0; }
.ticket-cutline-top { transform: translateY(0); }
.ticket-cutline-bottom { margin-top: 14px; }
.ticket-notch { position: absolute; top: 50%; width: 24px; height: 24px; border-radius: 50%; background: #f7f4ee; transform: translateY(-50%); }
.ticket-notch-left { left: -12px; }
.ticket-notch-right { right: -12px; }
.ticket-dashed-line { width: calc(100% - 34px); margin: 0 auto; border-top: 1.5px dashed rgba(255, 255, 255, .42); }
.mode-switch-control { position: relative; display: grid; grid-template-columns: 1fr 1fr; width: 84px; padding: 2px; overflow: hidden; border: 2px solid #173f8d; border-radius: 999px; background: #fff; }
.mode-switch-control button { position: relative; z-index: 2; height: 25px; border-radius: 999px; color: #173f8d; font-size: 10px; font-weight: 900; transition: color .25s ease; }
.mode-switch-control button.selected { color: #fff; }
.mode-switch-thumb { position: absolute; top: 2px; left: 2px; width: calc(50% - 2px); height: 25px; border-radius: 999px; background: #173f8d; transition: transform .3s cubic-bezier(.22,1,.36,1); }
.mode-switch-control.savings-selected .mode-switch-thumb { transform: translateX(100%); }
.mode-flight-loader { position: fixed; inset: 0; z-index: 200; display: flex; flex-direction: column; align-items: center; justify-content: center; background: linear-gradient(180deg,#173f8d 0%,#285eb7 70%,#dbeafe 100%); color: #fff; }
.mode-flight-loader strong { margin-top: 22px; font-size: 18px; }
.mode-flight-loader small { margin-top: 7px; color: #dbeafe; font-size: 11px; }
.flight-path { position: relative; width: 230px; border-top: 2px dashed #ffffff7a; }
.flight-path::before,.flight-path::after { position: absolute; top: -6px; width: 10px; height: 10px; border-radius: 50%; background: #fff; content: ''; }
.flight-path::before { left: 0; }.flight-path::after { right: 0; }
.flight-path span { position: absolute; top: -22px; left: 0; font-size: 32px; filter: drop-shadow(0 5px 8px #0c255b66); animation: fly-across 1s ease-in-out forwards; }
.flight-fade-enter-active,.flight-fade-leave-active { transition: opacity .18s ease; }.flight-fade-enter-from,.flight-fade-leave-to { opacity: 0; }
@keyframes fly-across { from { transform: translateX(0) rotate(5deg); } to { transform: translateX(202px) rotate(5deg); } }
</style>
