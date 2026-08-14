<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAccounts } from '@/api/asset'
import { useCardStore } from '@/stores/cardStore'

const router = useRouter()
const cardStore = useCardStore()
const accounts = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getAccounts()
    accounts.value = res.data?.data ?? []
  } catch {
    accounts.value = []
  }
  await cardStore.loadCards()
  loading.value = false
})

function formatWon(amount) {
  if (amount == null) return '-'
  return Number(amount).toLocaleString('ko-KR') + '원'
}

function cardTypeLabel(type) {
  return type === 'CHECK' ? '체크카드' : '신용카드'
}
</script>

<template>
  <div class="min-h-screen pb-20 flex flex-col" style="background: #F7F4EE">
    <!-- 헤더 -->
    <div class="flex items-center gap-2 px-4 pt-14 pb-4">
      <button class="p-1" @click="router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <path d="M15 18L9 12L15 6" stroke="#111" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <h1 class="text-lg font-bold text-gray-900">계좌·카드 연동</h1>
    </div>

    <div v-if="loading" class="flex justify-center items-center flex-1">
      <p class="text-sm text-gray-400">불러오는 중...</p>
    </div>

    <div v-else class="px-4 flex flex-col gap-5">
      <!-- 계좌 목록 -->
      <section>
        <h2 class="text-sm font-bold text-gray-500 mb-2">통장 {{ accounts.length }}개</h2>
        <div v-if="accounts.length === 0" class="bg-white rounded-2xl px-4 py-5 text-sm text-gray-400 text-center">
          연동된 계좌가 없습니다.
        </div>
        <div v-else class="bg-white rounded-2xl overflow-hidden">
          <button
            v-for="(account, i) in accounts"
            :key="account.id"
            class="w-full flex items-center gap-3 px-4 py-4 active:bg-gray-50"
            :class="i < accounts.length - 1 ? 'border-b border-gray-100' : ''"
            @click="router.push(`/asset/accounts/${account.id}?isReal=true`)"
          >
            <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 text-white text-sm font-bold" style="background: #2A4DB0">
              통
            </div>
            <div class="flex-1 text-left">
              <p class="text-sm font-semibold text-gray-900">{{ account.accountName }}</p>
              <p class="text-xs text-gray-400 mt-0.5">{{ account.accountType }}</p>
            </div>
            <div class="text-right">
              <p class="text-sm font-semibold text-gray-900">{{ formatWon(account.balance) }}</p>
            </div>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
      </section>

      <!-- 카드 목록 -->
      <section>
        <h2 class="text-sm font-bold text-gray-500 mb-2">카드 {{ cardStore.cards.length }}장</h2>
        <div v-if="cardStore.cards.length === 0" class="bg-white rounded-2xl px-4 py-5 text-sm text-gray-400 text-center">
          연동된 카드가 없습니다.
        </div>
        <div v-else class="bg-white rounded-2xl overflow-hidden">
          <button
            v-for="(card, i) in cardStore.cards"
            :key="card.id"
            class="w-full flex items-center gap-3 px-4 py-4 active:bg-gray-50"
            :class="i < cardStore.cards.length - 1 ? 'border-b border-gray-100' : ''"
            @click="router.push(`/mypage/cards/${card.id}/transactions`)"
          >
            <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 text-white text-sm font-bold" style="background: #1A337A">
              카
            </div>
            <div class="flex-1 text-left">
              <p class="text-sm font-semibold text-gray-900">{{ card.cardName }}</p>
              <p class="text-xs text-gray-400 mt-0.5">{{ cardTypeLabel(card.cardType) }} · {{ card.maskedCardNumber }}</p>
            </div>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M9 18L15 12L9 6" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>
