<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const step = ref(0) // 0: 인트로, 1: 계좌연결, 2: 급여정보, 3: 고정비, 4: 카테고리목표

// ── 급여 정보 ─────────────────────────────────────────────
const salary = ref('3500000')

// ── 고정비 ────────────────────────────────────────────────
const fixedExpenses = ref([
  { id: 1, name: '월세', amount: '800000', icon: '🏠' },
  { id: 2, name: '통신비', amount: '89000', icon: '📱' },
  { id: 3, name: '보험료', amount: '150000', icon: '🛡' },
  { id: 4, name: '구독 서비스', amount: '30000', icon: '💳' },
])

// ── 카테고리 목표 ──────────────────────────────────────────
const categories = ref([
  { id: 1, name: '식비', icon: '🍽', amount: '450000', color: '#3B5BDB' },
  { id: 2, name: '카페', icon: '☕', amount: '100000', color: '#60A5FA' },
  { id: 3, name: '생활비', icon: '🏠', amount: '300000', color: '#F59E0B' },
  { id: 4, name: '쇼핑', icon: '🛍', amount: '150000', color: '#EC4899' },
  { id: 5, name: '취·여가', icon: '🎨', amount: '100000', color: '#8B5CF6' },
  { id: 6, name: '기타', icon: '💬', amount: '100000', color: '#9CA3AF' },
])

const categoryTotal = computed(() =>
  categories.value.reduce((sum, c) => sum + (parseInt(c.amount) || 0), 0)
)

// 계좌 선택 모의 상태
const accountConnected = ref(false)

function formatNumber(val) {
  const n = parseInt(val.replace(/,/g, '')) || 0
  return n.toLocaleString('ko-KR')
}

function handleComplete() {
  authStore.completeProfile()
  router.replace('/')
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background: #F7F4EE">

    <!-- 진행 바 (인트로 제외) -->
    <div v-if="step > 0" class="fixed top-0 left-0 right-0 h-1 bg-gray-200 z-50">
      <div
        class="h-full transition-all duration-300"
        style="background: #3B5BDB"
        :style="{ width: `${(step / 4) * 100}%` }"
      />
    </div>

    <!-- ── STEP 0: 인트로 ──────────────────────────────── -->
    <template v-if="step === 0">
      <div class="flex flex-col min-h-screen">
        <!-- 상단 그라디언트 영역 -->
        <div class="px-6 pt-20 pb-10 flex flex-col" style="background: linear-gradient(160deg, #1A337A 0%, #2A4DB0 100%)">
          <p class="text-white/60 text-xs tracking-widest mb-6">TRIPASS FINANCE</p>
          <h1 class="text-white text-2xl font-bold leading-snug mb-3">
            {{ authStore.user?.name ?? '권유현' }}님,<br>
            여행 자금 계획을 위해<br>
            금융 프로필을 등록해요
          </h1>
          <p class="text-white/60 text-sm">정확한 여행 예산 관리를 위해<br>아래 정보를 입력해 주세요</p>

          <!-- 소요 시간 배지 -->
          <div class="mt-6 inline-flex items-center gap-2 bg-white/15 rounded-full px-4 py-2 self-start">
            <span class="text-white font-bold text-sm">3분</span>
            <span class="text-white/70 text-xs">여행 자금 계획 관리 완성</span>
          </div>
        </div>

        <!-- 혜택 목록 -->
        <div class="px-6 py-8 flex-1 flex flex-col gap-4">
          <div v-for="item in [
            { icon: '💰', title: '수입 및 고정지출', desc: '월 수입과 고정 지출을 등록해요' },
            { icon: '🏦', title: '금융자산 등록', desc: '연결된 계좌와 자산을 확인해요' },
            { icon: '🎯', title: '카테고리별 지출 목표 설정', desc: '항목별 예산을 직접 설정해요' },
          ]" :key="item.title" class="flex items-start gap-4">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 bg-blue-50 text-lg">
              {{ item.icon }}
            </div>
            <div>
              <p class="text-sm font-semibold text-gray-900">{{ item.title }}</p>
              <p class="text-xs text-gray-400 mt-0.5">{{ item.desc }}</p>
            </div>
          </div>
        </div>

        <!-- 버튼 -->
        <div class="px-6 pb-12">
          <button
            class="w-full h-14 rounded-2xl text-white font-bold text-base"
            style="background: #1A337A"
            @click="step = 1"
          >
            금융 프로필 등록하기
          </button>
        </div>
      </div>
    </template>

    <!-- ── STEP 1: 계좌 연결 ───────────────────────────── -->
    <template v-else-if="step === 1">
      <div class="flex flex-col min-h-screen pt-8">
        <!-- 헤더 -->
        <div class="px-6 pt-8">
          <button @click="step = 0" class="mb-6">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M15 18L9 12L15 6" stroke="#1A1A1A" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <p class="text-xs text-gray-400 mb-1">은행 계좌 등록하기</p>
          <h2 class="text-xl font-bold text-gray-900 leading-snug">
            {{ authStore.user?.name ?? '권유현' }}님이 쓰는<br>
            은행 계좌 정보를 불러올게요
          </h2>
        </div>

        <div class="px-6 mt-8 flex-1">
          <template v-if="!accountConnected">
            <!-- 은행 목록 -->
            <p class="text-xs text-gray-400 mb-3">주거래 은행 선택</p>
            <div class="grid grid-cols-3 gap-3">
              <button
                v-for="bank in ['KB국민', '신한', '우리', '하나', '카카오', '토스']"
                :key="bank"
                class="bg-white rounded-2xl py-4 text-sm font-medium text-gray-700 text-center active:bg-blue-50 active:text-blue-600 border border-transparent active:border-blue-200"
                @click="accountConnected = true"
              >
                {{ bank }}
              </button>
            </div>
          </template>

          <!-- 연결 완료 -->
          <template v-else>
            <div class="flex flex-col items-center justify-center py-12">
              <div class="w-16 h-16 rounded-full flex items-center justify-center mb-4" style="background: #EEF2FF">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
                  <path d="M20 6L9 17L4 12" stroke="#3B5BDB" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <p class="text-lg font-bold text-gray-900 mb-1">계좌 연결이 완료됐어요</p>
              <p class="text-sm text-gray-400">6개 계좌 · 11개 자산 연결됨</p>

              <!-- 연결된 계좌 목록 미리보기 -->
              <div class="mt-6 w-full bg-white rounded-2xl px-5 py-4 flex flex-col gap-3">
                <div v-for="acc in [
                  { bank: 'KB국민은행', type: '입출금', last: '1234' },
                  { bank: 'KB국민은행', type: '적금', last: '5678' },
                  { bank: '카카오뱅크', type: '입출금', last: '9012' },
                ]" :key="acc.last" class="flex items-center justify-between">
                  <div>
                    <p class="text-sm font-semibold text-gray-900">{{ acc.bank }}</p>
                    <p class="text-xs text-gray-400">{{ acc.type }} **** {{ acc.last }}</p>
                  </div>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <path d="M20 6L9 17L4 12" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </div>
              </div>
            </div>
          </template>
        </div>

        <div class="px-6 pb-12">
          <button
            v-if="accountConnected"
            class="w-full h-14 rounded-2xl text-white font-bold text-base"
            style="background: #1A337A"
            @click="step = 2"
          >
            다음
          </button>
          <p v-else class="text-center text-xs text-gray-400 mt-4">은행을 선택하면 자동으로 계좌가 연결돼요</p>
        </div>
      </div>
    </template>

    <!-- ── STEP 2: 급여 정보 ───────────────────────────── -->
    <template v-else-if="step === 2">
      <div class="flex flex-col min-h-screen">
        <div class="px-6 pt-16">
          <button @click="step = 1" class="mb-6">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M15 18L9 12L15 6" stroke="#1A1A1A" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <p class="text-xs text-gray-400 mb-1">기간 정보 입력</p>
          <h2 class="text-xl font-bold text-gray-900 mb-8">급여 정보</h2>

          <div class="bg-white rounded-2xl px-5 py-5">
            <p class="text-xs text-gray-400 mb-2">월 급여액</p>
            <div class="flex items-center gap-2">
              <input
                v-model="salary"
                type="number"
                class="flex-1 text-2xl font-bold text-gray-900 bg-transparent outline-none"
                placeholder="0"
              />
              <span class="text-lg font-bold text-gray-400">원</span>
            </div>
            <div class="mt-4 pt-4 border-t border-gray-100">
              <p class="text-xs text-gray-400 mb-1">급여 입금일</p>
              <p class="text-sm font-semibold text-gray-900">매월 25일</p>
            </div>
          </div>

          <p class="text-xs text-gray-400 mt-4 text-center">
            연결된 계좌의 급여 입금 내역을 기반으로 자동 분석됐어요
          </p>
        </div>

        <div class="px-6 pb-12 mt-auto">
          <button
            class="w-full h-14 rounded-2xl text-white font-bold text-base"
            style="background: #1A337A"
            @click="step = 3"
          >
            다음
          </button>
        </div>
      </div>
    </template>

    <!-- ── STEP 3: 고정비 등록 ────────────────────────── -->
    <template v-else-if="step === 3">
      <div class="flex flex-col min-h-screen">
        <div class="px-6 pt-16">
          <button @click="step = 2" class="mb-6">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M15 18L9 12L15 6" stroke="#1A1A1A" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <p class="text-xs text-gray-400 mb-1">고정비 분석</p>
          <h2 class="text-xl font-bold text-gray-900 mb-2">
            매달 나가는 고정비<br>확인해볼까요?
          </h2>
          <p class="text-sm text-gray-400 mb-6">연결된 계좌를 분석해 고정 지출을 찾았어요</p>

          <div class="bg-white rounded-2xl px-5 py-4 mb-3">
            <p class="text-xs text-gray-400 mb-1">총 고정비</p>
            <p class="text-2xl font-bold mb-4" style="color: #1A337A">
              {{ formatNumber(String(fixedExpenses.reduce((s, e) => s + (parseInt(e.amount) || 0), 0))) }}원
            </p>
            <div class="flex flex-col gap-3">
              <div v-for="exp in fixedExpenses" :key="exp.id" class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <span class="text-lg">{{ exp.icon }}</span>
                  <p class="text-sm text-gray-900">{{ exp.name }}</p>
                </div>
                <div class="flex items-center gap-1">
                  <input
                    v-model="exp.amount"
                    type="number"
                    class="text-sm font-semibold text-gray-900 text-right w-24 bg-transparent outline-none"
                  />
                  <span class="text-sm text-gray-400">원</span>
                </div>
              </div>
            </div>
          </div>

          <button class="text-sm font-medium w-full text-center py-3" style="color: #3B5BDB">
            + 고정비 직접 추가하기
          </button>
        </div>

        <div class="px-6 pb-12 mt-auto">
          <button
            class="w-full h-14 rounded-2xl text-white font-bold text-base"
            style="background: #1A337A"
            @click="step = 4"
          >
            다음
          </button>
        </div>
      </div>
    </template>

    <!-- ── STEP 4: 카테고리 목표 설정 ────────────────── -->
    <template v-else-if="step === 4">
      <div class="flex flex-col min-h-screen">
        <div class="px-6 pt-16">
          <button @click="step = 3" class="mb-6">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M15 18L9 12L15 6" stroke="#1A1A1A" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <p class="text-xs text-gray-400 mb-1">카테고리별 목표 금액 설정</p>
          <h2 class="text-xl font-bold text-gray-900 mb-6">카테고리별 목표 금액 입력</h2>

          <div class="bg-white rounded-2xl px-5 py-4 mb-4">
            <div class="flex flex-col gap-4">
              <div v-for="cat in categories" :key="cat.id" class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <div
                    class="w-8 h-8 rounded-lg flex items-center justify-center text-sm flex-shrink-0"
                    :style="`background: ${cat.color}20`"
                  >
                    {{ cat.icon }}
                  </div>
                  <p class="text-sm text-gray-900">{{ cat.name }}</p>
                </div>
                <div class="flex items-center gap-1">
                  <input
                    v-model="cat.amount"
                    type="number"
                    class="text-sm font-semibold text-gray-900 text-right w-24 bg-transparent outline-none"
                  />
                  <span class="text-sm text-gray-400">원</span>
                </div>
              </div>

              <!-- 합계 -->
              <div class="pt-3 border-t border-gray-100 flex justify-between items-center">
                <p class="text-sm font-bold text-gray-900">합계</p>
                <p class="text-base font-bold" style="color: #1A337A">
                  {{ categoryTotal.toLocaleString('ko-KR') }}원
                </p>
              </div>
            </div>
          </div>
        </div>

        <div class="px-6 pb-12 mt-auto">
          <button
            class="w-full h-14 rounded-2xl text-white font-bold text-base"
            style="background: #1A337A"
            @click="handleComplete"
          >
            설정 완료
          </button>
        </div>
      </div>
    </template>

  </div>
</template>
