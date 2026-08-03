<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTravelModeStore } from '@/stores/travelMode'
import BottomNav from '@/components/common/BottomNav.vue'

const authStore = useAuthStore()
const travelModeStore = useTravelModeStore()

const userName = computed(() => authStore.user?.name ?? '권유현')

// ── 여행 저축 모드 데이터 ──────────────────────────────────
const countries = [
  {
    id: 1, name: '파리', flag: '🇫🇷', code: 'PAR',
    color: '#2563EB', image: 'paris',
    dday: 230, currency: 'EUR', rate: 1548,
  },
  {
    id: 2, name: '인터라켄', flag: '🇨🇭', code: 'INT',
    color: '#DB2777', image: 'interlaken',
    dday: 230, currency: 'CHF', rate: 1620,
  },
  {
    id: 3, name: '베를린', flag: '🇩🇪', code: 'BER',
    color: '#1F2937', image: 'berlin',
    dday: 230, currency: 'EUR', rate: 1548,
  },
  {
    id: 4, name: '도쿄', flag: '🇯🇵', code: 'THO',
    color: '#DC2626', image: 'tokyo',
    dday: 230, currency: 'JPY', rate: 9,
  },
  {
    id: 5, name: '다낭', flag: '🇻🇳', code: 'DAD',
    color: '#D97706', image: 'danang',
    dday: 230, currency: 'VND', rate: 0.06,
  },
]
const selectedCountry = ref(countries[0])

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
    { name: '식비', spent: 180000, budget: 450000, percent: 72, color: '#3B5BDB' },
    { name: '카페', spent: 41000, budget: 100000, percent: 42, color: '#60A5FA' },
    { name: '생활비', spent: 126000, budget: 300000, percent: 70, color: '#F59E0B' },
    { name: '쇼핑', spent: 45000, budget: 150000, percent: 60, color: '#EC4899' },
    { name: '취미', spent: 35000, budget: 100000, percent: 50, color: '#8B5CF6' },
  ],
  schedule: [
    { date: '7/25', label: '금액입금', type: '입금' },
    { date: '7/28', label: '돌산에 낙하예', type: '지출' },
    { date: '7/31', label: '돌산에 낙하예', type: '지출' },
  ],
}

const savingsPercent = computed(() =>
  Math.round((savingsData.saved / savingsData.goal) * 100)
)

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
  <div class="min-h-screen pb-20" style="background: #F5F5F5">

    <!-- ══ 여행 저축 모드 ══════════════════════════════════════ -->
    <template v-if="travelModeStore.isSavingsMode">

      <!-- 헤더 -->
      <div class="bg-white px-5 pt-14 pb-3">
        <div class="flex items-center justify-between mb-1">
          <!-- 모드 전환 버튼 -->
          <button
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold"
            style="background: #EEF2FF; color: #3B5BDB"
            @click="travelModeStore.setMode('travel')"
          >
            <span>여행 저축</span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
              <path d="M6 9L12 15L18 9" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <button class="text-xs text-gray-400">여행 계획 수정 &gt;</button>
        </div>
        <p class="text-xl font-bold text-gray-900 mt-2">안녕하세요, {{ userName }}님</p>

        <!-- 국가 탭 -->
        <div class="flex gap-2 mt-4 overflow-x-auto pb-1 scrollbar-hide">
          <button
            v-for="c in countries"
            :key="c.id"
            class="flex-shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold transition-colors"
            :style="selectedCountry.id === c.id
              ? `background: ${c.color}; color: white`
              : 'background: #F3F4F6; color: #6B7280'"
            @click="selectedCountry = c"
          >
            <span>{{ c.flag }}</span>
            <span>{{ c.name }}</span>
          </button>
        </div>
      </div>

      <!-- BOARDING PASS 카드 -->
      <div class="mx-4 mt-4 rounded-2xl overflow-hidden" :style="`background: ${selectedCountry.color}`">
        <div class="px-4 pt-4 pb-5">
          <div class="flex items-center justify-between mb-1">
            <div>
              <p class="text-white/60 text-[10px] tracking-widest">BOARDING PASS · TRIPASS AIR</p>
              <p class="text-white/60 text-[10px]">NO. {{ selectedCountry.code }}-230</p>
            </div>
          </div>
          <div class="flex items-center justify-between mt-2">
            <div>
              <p class="text-white/60 text-xs">DESTINATION</p>
              <p class="text-white text-2xl font-bold flex items-center gap-2">
                {{ selectedCountry.flag }} {{ selectedCountry.name }}
              </p>
              <p class="text-white/60 text-xs mt-1">설레는 여행이 기다려요 ›</p>
            </div>
            <div class="text-right">
              <p class="text-white/60 text-[10px]">DEPARTURE</p>
              <p class="text-white font-bold text-2xl">D-{{ selectedCountry.dday }}</p>
            </div>
          </div>

          <!-- 저축 진행바 -->
          <div class="mt-4">
            <div class="flex justify-between text-xs text-white/70 mb-1.5">
              <span>여행 저축 목표</span>
              <span class="font-bold text-white">{{ savingsPercent }}%</span>
            </div>
            <div class="h-2 rounded-full bg-white/20">
              <div class="h-full rounded-full bg-white transition-all" :style="`width: ${savingsPercent}%`"/>
            </div>
            <div class="flex justify-between text-[10px] text-white/60 mt-1">
              <span>{{ formatCurrency(savingsData.saved) }}</span>
              <span>{{ formatCurrency(savingsData.goal) }}</span>
            </div>
          </div>

          <button class="mt-3 w-full py-2 rounded-xl text-xs font-semibold text-center bg-white/15 text-white">
            여행 목표 자금 관리 ›
          </button>
        </div>
      </div>

      <!-- 보유 자금 / 연동 계좌 -->
      <div class="mx-4 mt-3 bg-white rounded-2xl px-5 py-4 flex">
        <div class="flex-1 text-center border-r border-gray-100">
          <p class="text-xs text-gray-400 mb-1">보유 자금</p>
          <p class="text-base font-bold text-gray-900">{{ formatCurrency(savingsData.balance) }}</p>
        </div>
        <div class="flex-1 text-center">
          <p class="text-xs text-gray-400 mb-1">연동 계좌</p>
          <p class="text-base font-bold text-gray-900">{{ savingsData.accounts }}개</p>
        </div>
      </div>

      <!-- 이달의 자금 체크 -->
      <div class="mx-4 mt-3 bg-white rounded-2xl px-5 py-4">
        <div class="flex items-center justify-between mb-1">
          <p class="text-sm font-bold text-gray-900">이달의 자금 체크</p>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <p class="text-xs text-gray-400 mb-2">이달에 여유자금</p>
        <p class="text-2xl font-bold" style="color: #3B5BDB">{{ formatCurrency(savingsData.monthly.available) }}</p>
        <p class="text-[11px] text-gray-400 mt-1">{{ savingsData.monthly.details }}</p>
      </div>

      <!-- 카테고리별 사용 현황 -->
      <div class="mx-4 mt-3 bg-white rounded-2xl px-5 py-4">
        <div class="flex items-center justify-between mb-3">
          <p class="text-sm font-bold text-gray-900">카테고리별 사용 현황</p>
          <span class="text-xs text-gray-400">이번 달</span>
        </div>
        <div class="flex flex-col gap-2.5">
          <div v-for="cat in savingsData.categories" :key="cat.name" class="flex items-center gap-3">
            <span class="text-xs text-gray-500 w-12 flex-shrink-0">{{ cat.name }}</span>
            <div class="flex-1 h-1.5 rounded-full bg-gray-100">
              <div class="h-full rounded-full" :style="`width: ${cat.percent}%; background: ${cat.color}`"/>
            </div>
            <span class="text-xs text-gray-400 w-20 text-right flex-shrink-0">
              {{ formatCurrency(cat.spent) }}/{{ formatCurrency(cat.budget) }}
            </span>
            <span class="text-xs font-semibold w-8 text-right flex-shrink-0" :style="`color: ${cat.color}`">
              {{ cat.percent }}%
            </span>
          </div>
        </div>
      </div>

      <!-- 다가오는 금융 일정 -->
      <div class="mx-4 mt-3 bg-white rounded-2xl px-5 py-4">
        <div class="flex items-center justify-between mb-3">
          <p class="text-sm font-bold text-gray-900">다가오는 금융 일정</p>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="flex flex-col gap-3">
          <div v-for="item in savingsData.schedule" :key="item.date" class="flex items-center justify-between">
            <p class="text-sm text-gray-900 font-medium">{{ item.date }}</p>
            <p class="text-xs text-gray-500 flex-1 mx-3">{{ item.label }}</p>
            <span
              class="text-[11px] font-semibold px-2 py-0.5 rounded-full"
              :style="item.type === '입금'
                ? 'background: #EEF2FF; color: #3B5BDB'
                : 'background: #FEF2F2; color: #EF4444'"
            >{{ item.type }}</span>
          </div>
        </div>
      </div>

      <!-- 오늘의 환율 -->
      <div class="mx-4 mt-3 mb-4 rounded-2xl px-5 py-4 flex items-center justify-between" :style="`background: ${selectedCountry.color}`">
        <div>
          <p class="text-white/60 text-xs mb-0.5">오늘의 환율</p>
          <p class="text-white text-xs">{{ selectedCountry.flag }} {{ selectedCountry.currency }}/KRW</p>
        </div>
        <p class="text-white text-xl font-bold">{{ selectedCountry.rate.toLocaleString('ko-KR') }}원</p>
      </div>

    </template>

    <!-- ══ 여행 모드 ══════════════════════════════════════════ -->
    <template v-else>

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

    <BottomNav />
  </div>
</template>
