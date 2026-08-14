<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCardStore } from '@/stores/cardStore'

const route = useRoute()
const router = useRouter()
const cardStore = useCardStore()

const cardId = Number(route.params.cardId)
const card = computed(() => cardStore.cards.find((c) => c.id === cardId))

function toLocalDateStr(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const today = new Date()
const endDate = toLocalDateStr(today)
const startDate = toLocalDateStr(new Date(today.getFullYear(), today.getMonth(), 1))

const isFetching = ref(false)
const fetchMessage = ref('')

onMounted(async () => {
  if (cardStore.cards.length === 0) await cardStore.loadCards()
  await cardStore.loadCardTransactions(cardId, startDate, endDate)
})

async function syncTransactions() {
  isFetching.value = true
  fetchMessage.value = ''
  try {
    await cardStore.fetchAndSaveCardTransactions(cardId, startDate, endDate)
    fetchMessage.value = '최신 거래내역을 불러왔습니다.'
  } catch {
    fetchMessage.value = '거래내역 동기화에 실패했습니다.'
  } finally {
    isFetching.value = false
  }
}

function formatWon(amount) {
  return Number(amount).toLocaleString('ko-KR') + '원'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}.${d.getDate()}`
}

const grouped = computed(() => {
  const groups = {}
  for (const tx of cardStore.cardTransactions) {
    const key = tx.transactionDate
    if (!groups[key]) groups[key] = []
    groups[key].push(tx)
  }
  return Object.entries(groups).sort(([a], [b]) => b.localeCompare(a))
})
</script>

<template>
  <div class="min-h-screen pb-20 flex flex-col" style="background: #F7F4EE">
    <!-- 헤더 -->
    <div class="flex items-center gap-2 px-4 pt-14 pb-3">
      <button class="p-1" @click="router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <path d="M15 18L9 12L15 6" stroke="#111" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <div class="flex-1">
        <h1 class="text-lg font-bold text-gray-900">{{ card?.cardName ?? '카드 거래내역' }}</h1>
        <p class="text-xs text-gray-400">{{ card?.maskedCardNumber }}</p>
      </div>
      <!-- 동기화 버튼 -->
      <button
        class="flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-semibold active:opacity-70"
        style="background: #2A4DB0; color: white"
        :disabled="isFetching"
        @click="syncTransactions"
      >
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
          <path d="M1 4V10H7" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M3.51 15C4.15 16.8 5.44 18.3 7.11 19.24C8.78 20.18 10.73 20.47 12.61 20.06C14.49 19.64 16.18 18.54 17.36 16.97C18.54 15.4 19.12 13.46 18.99 11.5C18.86 9.54 18.03 7.69 16.65 6.3C15.27 4.91 13.44 4.07 11.5 3.94C9.56 3.82 7.63 4.42 6.08 5.62L1 10" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        {{ isFetching ? '동기화 중' : '동기화' }}
      </button>
    </div>

    <!-- 안내 메시지 -->
    <p v-if="fetchMessage" class="text-xs text-center py-2" :class="fetchMessage.includes('실패') ? 'text-red-400' : 'text-blue-500'">
      {{ fetchMessage }}
    </p>

    <!-- 로딩 -->
    <div v-if="cardStore.loading && !isFetching" class="flex justify-center items-center flex-1">
      <p class="text-sm text-gray-400">불러오는 중...</p>
    </div>

    <!-- 거래내역 없음 -->
    <div v-else-if="grouped.length === 0" class="flex flex-col items-center justify-center flex-1 gap-2">
      <p class="text-sm text-gray-400">거래내역이 없습니다.</p>
      <p class="text-xs text-gray-300">동기화 버튼을 눌러 최신 내역을 가져오세요.</p>
    </div>

    <!-- 거래내역 목록 -->
    <div v-else class="px-4 flex flex-col gap-4">
      <div v-for="([date, items]) in grouped" :key="date">
        <p class="text-xs font-semibold text-gray-400 mb-2">{{ date }}</p>
        <div class="bg-white rounded-2xl overflow-hidden">
          <div
            v-for="(tx, i) in items"
            :key="tx.id"
            class="flex items-center gap-3 px-4 py-3"
            :class="i < items.length - 1 ? 'border-b border-gray-100' : ''"
          >
            <div class="flex-1">
              <p class="text-sm font-semibold text-gray-900">{{ tx.merchantName ?? '거래처 없음' }}</p>
              <p class="text-xs text-gray-400 mt-0.5">{{ tx.transactionTime ?? '' }}</p>
            </div>
            <p
              class="text-sm font-bold"
              :class="tx.transactionType === 'DEPOSIT' ? 'text-blue-600' : 'text-gray-900'"
            >
              {{ tx.transactionType === 'DEPOSIT' ? '+' : '-' }}{{ formatWon(Math.abs(tx.amount)) }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
