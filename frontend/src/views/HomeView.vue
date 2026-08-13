<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTravelModeStore } from '@/stores/travelMode'
import { useTravelStore } from '@/stores/travel'
import { useSavingsPlanStore } from '@/stores/savingsPlan'
import { useTripWalletStore } from '@/stores/tripWallet'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import TravelTicket from '@/components/savings/TravelTicket.vue'
import TravelModeHome from '@/components/travel/TravelModeHome.vue'
import NotificationBell from '@/components/common/NotificationBell.vue'

const authStore = useAuthStore()
const travelModeStore = useTravelModeStore()
const travelStore = useTravelStore()
const plan = useSavingsPlanStore()
const wallet = useTripWalletStore()
const router = useRouter()
const isModeSwitching = ref(false)
const nextMode = ref('travel')

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
    id: 5, name: '홍콩', flag: '🇭🇰', code: 'HKG',
    image: '/images/Hong%20Kong.png',
    headerBg: '#b8202e',
    progressBg: 'rgba(184,32,46,0.84)',
    barColor: 'linear-gradient(90deg,#DE2910 0%,#FFDE00 100%)',
    dday: 230, currency: 'HKD', rate: 184.2,
    desc: '빛나는 야경과 활기찬 거리 · 홍콩에서 시작되는 특별한 하루',
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
const monthlyTarget = computed(() => Math.max(0, Math.ceil((homeGoalAmount.value - wallet.balance.value) / 5 / 10_000) * 10_000))
const monthlySaved = computed(() => wallet.monthDeposit)
const monthlyRemaining = computed(() => Math.max(0, monthlyTarget.value - monthlySaved.value))
const monthlyProgress = computed(() => monthlyTarget.value ? Math.min(100, Math.round(monthlySaved.value / monthlyTarget.value * 100)) : 100)
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

function goWallet() {
  router.push('/wallet')
}
</script>

<template>
  <div class="app-home-shell min-h-screen pb-20" style="background: #F7F4EE">

    <!-- ══ 여행 미등록 홈 ══════════════════════════════════════ -->
    <template v-if="travelModeStore.isSavingsMode && !travelStore.hasTravelGoal">
      <div class="px-5 pt-12 pb-3 bg-white/80">
        <div class="flex items-center justify-between">
          <button class="px-3 py-1.5 rounded-full text-xs font-bold" style="background:#eef2ff;color:#263f8c">여행 저축</button>
          <NotificationBell />
        </div>
        <p class="mt-2 text-lg font-extrabold">안녕하세요, {{ userName }}님</p>
        <p class="mt-1 text-[10px] text-slate-500">새로운 여행을 함께 준비해 볼까요?</p>
      </div>

      <div class="px-4 mt-3">
        <TravelTicket eyebrow="TRIPASS · START JOURNEY">
          <div class="py-1 text-center">
            <div class="mb-2 text-2xl">✈</div>
            <h2 class="text-[16px] font-extrabold">아직 등록된 여행이 없어요</h2>
            <p class="mt-2 text-[10px] leading-4 text-blue-100">여행명·국가·일정을 등록하면<br>AI가 목표 예산과 월 저축액을 제안해요.</p>
            <button class="w-full h-11 mt-4 rounded-xl text-[12px] font-extrabold text-white" style="background:#ff7a36" @click="router.push('/savings')">여행 계획 등록하기</button>
          </div>
        </TravelTicket>
      </div>

      <section class="mx-4 mt-4 rounded-2xl border border-blue-100 bg-blue-50/80 px-4 py-4">
        <p class="text-[10px] font-extrabold" style="color:#2864e8">TRIPASS GUIDE</p>
        <h2 class="mt-1 text-[15px] font-extrabold">목표 설정부터 월렛 저축까지</h2>
        <p class="mt-1 text-[10px] leading-4 text-slate-500">여행 예산은 AI가 제안하고, 실제 저축은 TRIP 월렛에서 관리해요.</p>
      </section>
    </template>

    <!-- ══ 여행 저축 모드 ══════════════════════════════════════ -->
    <template v-else-if="travelModeStore.isSavingsMode">

      <!-- 헤더 -->
      <div class="savings-home-header">
        <div class="mode-switch-control savings-selected">
          <span class="mode-switch-thumb" />
          <button type="button" @click="switchMode('travel')">여행</button>
          <button type="button" class="selected" @click="switchMode('savings')">저축</button>
        </div>

        <div class="savings-header-row savings-greeting-row">
          <h1>안녕하세요, {{ userName }}님</h1>
          <NotificationBell />
        </div>

        <div class="savings-header-row savings-action-row">
          <!-- 국가 드롭다운 -->
          <div class="relative">
            <button
              class="savings-country-button"
              @click="showCountryDropdown = !showCountryDropdown"
            >
              <span>{{ selectedCountry.flag }}</span>
              <span>{{ selectedCountry.name }}</span>
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
                <path d="M6 9L12 15L18 9" stroke="#6B7280" stroke-width="2.5" stroke-linecap="round"/>
              </svg>
            </button>
            <div v-if="showCountryDropdown" class="absolute left-0 top-9 bg-white rounded-xl shadow-lg border border-gray-100 py-1 z-20 min-w-[120px]">
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
          <button class="travel-edit-link" type="button" @click="router.push('/savings')">
            여행 계획 수정하기 <span>›</span>
          </button>
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
              @click="goWallet">
              <span class="text-[10px] font-bold text-white">송금하기</span>
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

      <section class="month-saving-card mx-4 mt-3">
        <div class="month-saving-heading"><div><p>8월</p><h2>이번 달 저축</h2></div><span v-if="monthlyProgress >= 100">달성</span></div>
        <div class="month-saving-values"><div><small>목표 금액</small><b>{{ formatCurrency(monthlyTarget) }}</b></div><div><small>저축한 금액</small><b>{{ formatCurrency(monthlySaved) }}</b></div><div><small>남은 저축</small><b>{{ formatCurrency(monthlyRemaining) }}</b></div></div>
        <div class="month-saving-progress"><i :style="{ width: `${monthlyProgress}%` }"/><strong>{{ monthlyProgress }}%</strong></div>
        <div v-if="monthlyProgress >= 100" class="month-saving-success"><span>✓</span><div><b>이번 달 목표 달성!</b><small>{{ formatCurrency(monthlySaved) }} 송금을 완료했어요.</small></div></div>
        <button class="month-wallet-button" @click="goWallet">월렛으로 송금하기 <span>›</span></button>
      </section>

      <section class="ai-report-card mx-4 mt-3" @click="router.push('/missions')">
        <div class="ai-report-heading"><div><p>7월 AI 분석 리포트</p><small>지난달 여행 저축 목표</small></div><button @click.stop="router.push('/missions')">상세 보기 ›</button></div>
        <div class="ai-goal-status"><span class="status-icon">✦</span><div><strong>{{ monthlySaved >= monthlyTarget ? '목표 달성' : '목표 미도달' }}</strong><p v-if="monthlySaved >= monthlyTarget">목표보다 {{ formatCurrency(monthlySaved - monthlyTarget) }} 더 모았어요.</p><p v-else>목표까지 {{ formatCurrency(monthlyTarget - monthlySaved) }}이 부족했어요.</p></div></div>
        <div class="ai-top-title"><span>AI 소비 분석</span><small>최근 거래내역 기준</small></div>
        <div v-for="(item, index) in [{ name:'식비', amount:'120,000원' },{ name:'카페', amount:'60,000원' },{ name:'쇼핑', amount:'40,000원' }]" :key="item.name" class="ai-coaching-row"><b>{{ index + 1 }}</b><span>{{ item.name }}</span><small>{{ index === 0 ? '342,000원 · 38%' : index === 1 ? '171,000원 · 19%' : '126,000원 · 14%' }}</small><strong>{{ item.amount }} 줄이기</strong></div>
        <div class="ai-saving-total"><small>이달의 절약 가능 금액</small><b>월 220,000원 확보 가능</b></div>
      </section>

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
.mode-switch-control { position: relative; display: grid; grid-template-columns: 1fr 1fr; width: 84px; padding: 2px; overflow: hidden; border: 1px solid #d9dee7; border-radius: 999px; background: #eceff3; }
.mode-switch-control button { position: relative; z-index: 2; height: 25px; border-radius: 999px; color: #6b7688; font-size: 10px; font-weight: 900; transition: color .25s ease; }
.mode-switch-control button.selected { color: #fff; }
.mode-switch-thumb { position: absolute; top: 2px; left: 2px; width: calc(50% - 2px); height: 25px; border-radius: 999px; background: #173f8d; transition: transform .3s cubic-bezier(.22,1,.36,1); }
.mode-switch-control.savings-selected .mode-switch-thumb { transform: translateX(100%); }
.savings-home-header { padding: 42px 20px 14px; background: #f7f4ee; }
.savings-header-row { display: flex; align-items: center; justify-content: space-between; }
.savings-greeting-row { margin-top: 10px; }
.savings-greeting-row h1 { color: #111827; font-size: 20px; font-weight: 900; letter-spacing: -.04em; }
.savings-action-row { margin-top: 9px; }
.savings-country-button { display: flex; min-width: 78px; align-items: center; gap: 6px; padding: 7px 10px; border: 1px solid #d8dee8; border-radius: 10px; background: #fff; color: #273449; font-size: 11px; font-weight: 800; box-shadow: 0 2px 7px rgba(27,43,75,.05); }
.travel-edit-link { display: flex; align-items: center; gap: 4px; padding: 8px 11px; border-radius: 10px; background: #fff0e8; color: #e45f24; font-size: 11px; font-weight: 900; }
.travel-edit-link span { font-size: 16px; line-height: 10px; }
.app-home-shell { position: relative; width: min(100%, 390px); margin: 0 auto; overflow-x: hidden; }
.month-saving-card{padding:16px;border:1.5px solid #2bb69f;border-radius:19px;background:#fff;box-shadow:0 7px 16px rgba(19,59,123,.06)}.month-saving-heading{display:flex;align-items:center;justify-content:space-between}.month-saving-heading p{display:inline-block;padding:5px 11px;border:1px solid #20aa94;border-radius:99px;color:#089b83;font-size:10px;font-weight:900}.month-saving-heading h2{margin-top:6px;color:#079982;font-size:19px;font-weight:900}.month-saving-heading>span{padding:5px 9px;border-radius:99px;background:#e2f8f1;color:#08a084;font-size:10px;font-weight:900}.month-saving-values{display:grid;grid-template-columns:repeat(3,1fr);margin-top:16px}.month-saving-values>div{padding:0 8px}.month-saving-values>div:first-child{padding-left:0}.month-saving-values>div+div{border-left:1px solid #5bcab9}.month-saving-values small{display:block;color:#079982;font-size:9px;font-weight:700}.month-saving-values b{display:block;margin-top:5px;color:#079982;font-size:13px;font-weight:900;letter-spacing:-.04em}.month-saving-progress{position:relative;height:8px;margin-top:19px;border-radius:99px;background:#dff3ef}.month-saving-progress i{display:block;height:100%;border-radius:inherit;background:#25b59d}.month-saving-progress strong{position:absolute;right:0;top:-18px;color:#08a084;font-size:10px}.month-saving-success{display:flex;align-items:center;gap:9px;margin-top:15px;padding:10px;border-radius:13px;background:#e2f8f1}.month-saving-success>span{display:grid;width:28px;height:28px;place-items:center;border-radius:10px;background:#c8f0e6;color:#00a489;font-size:17px;font-weight:900}.month-saving-success b,.month-saving-success small{display:block}.month-saving-success b{color:#048c77;font-size:11px}.month-saving-success small{margin-top:2px;color:#229d89;font-size:9px}.month-wallet-button{display:flex;width:100%;align-items:center;justify-content:space-between;margin-top:13px;padding:11px 13px;border:1px solid #9adfd1;border-radius:12px;color:#049781;font-size:11px;font-weight:900}.month-wallet-button span{font-size:17px}.ai-report-card{padding:17px;border:1px solid #bfd7ff;border-radius:20px;background:#eaf3ff;box-shadow:0 8px 18px rgba(27,64,129,.05);cursor:pointer}.ai-report-heading{display:flex;justify-content:space-between;align-items:start}.ai-report-heading p{color:#286ce0;font-size:18px;font-weight:900;letter-spacing:-.05em}.ai-report-heading small{display:block;margin-top:5px;color:#6e88b2;font-size:10px}.ai-report-heading button{color:#286ce0;font-size:10px;font-weight:800}.ai-goal-status{display:flex;align-items:center;gap:9px;margin-top:13px;padding:11px;border-radius:13px;background:#fff}.status-icon{display:grid;width:31px;height:31px;place-items:center;border-radius:11px;background:#e1f8f2;color:#03a084;font-size:18px}.ai-goal-status strong{color:#109482;font-size:12px}.ai-goal-status p{margin-top:3px;color:#6681a6;font-size:9px}.ai-top-title{display:flex;align-items:center;justify-content:space-between;margin-top:16px}.ai-top-title span{color:#3b68aa;font-size:10px;font-weight:900}.ai-top-title small{color:#8aa0bf;font-size:8px}.ai-coaching-row{display:grid;grid-template-columns:22px 34px 1fr auto;align-items:center;gap:5px;margin-top:8px;padding:9px;border-radius:11px;background:#fff}.ai-coaching-row>b{display:grid;width:19px;height:19px;place-items:center;border-radius:50%;background:#dce9ff;color:#2c68d7;font-size:9px}.ai-coaching-row span{font-size:11px;font-weight:900}.ai-coaching-row small{color:#7186a8;font-size:8px}.ai-coaching-row strong{padding:5px 7px;border:1px solid #ffc6c6;border-radius:99px;color:#ef5050;font-size:8px}.ai-saving-total{margin-top:12px;padding:13px;border-radius:13px;background:#e2f8f1}.ai-saving-total small{display:block;color:#179980;font-size:9px;font-weight:800}.ai-saving-total b{display:block;margin-top:5px;color:#173b75;font-size:18px;font-weight:900;letter-spacing:-.05em}
.mode-flight-loader { position: fixed; top: 0; bottom: 0; left: 50%; width: min(100vw,390px); z-index: 200; display: flex; flex-direction: column; align-items: center; justify-content: center; transform: translateX(-50%); background: linear-gradient(180deg,#173f8d 0%,#285eb7 70%,#dbeafe 100%); color: #fff; }
.mode-flight-loader strong { margin-top: 22px; font-size: 18px; }
.mode-flight-loader small { margin-top: 7px; color: #dbeafe; font-size: 11px; }
.flight-path { position: relative; width: 230px; border-top: 2px dashed #ffffff7a; }
.flight-path::before,.flight-path::after { position: absolute; top: -6px; width: 10px; height: 10px; border-radius: 50%; background: #fff; content: ''; }
.flight-path::before { left: 0; }.flight-path::after { right: 0; }
.flight-path span { position: absolute; top: -22px; left: 0; font-size: 32px; filter: drop-shadow(0 5px 8px #0c255b66); animation: fly-across 1s ease-in-out forwards; }
.flight-fade-enter-active,.flight-fade-leave-active { transition: opacity .18s ease; }.flight-fade-enter-from,.flight-fade-leave-to { opacity: 0; }
@keyframes fly-across { from { transform: translateX(0) rotate(5deg); } to { transform: translateX(202px) rotate(5deg); } }
</style>
