import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getCards, getCardTransactions, fetchCardTransactions as apiFetchCardTransactions } from '@/api/card'

export const useCardStore = defineStore('card', () => {
  const cards = ref([])
  const cardTransactions = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function loadCards() {
    loading.value = true
    error.value = null
    try {
      const res = await getCards()
      cards.value = res.data?.data ?? []
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }

  async function loadCardTransactions(cardId, startDate, endDate) {
    cardTransactions.value = []
    loading.value = true
    error.value = null
    try {
      const res = await getCardTransactions(cardId, startDate, endDate)
      cardTransactions.value = res.data?.data ?? []
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }

  async function fetchAndSaveCardTransactions(cardId, startDate, endDate) {
    cardTransactions.value = []
    loading.value = true
    error.value = null
    try {
      const res = await apiFetchCardTransactions(cardId, startDate, endDate)
      cardTransactions.value = res.data?.data ?? []
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }

  return {
    cards,
    cardTransactions,
    loading,
    error,
    loadCards,
    loadCardTransactions,
    fetchAndSaveCardTransactions,
  }
})
