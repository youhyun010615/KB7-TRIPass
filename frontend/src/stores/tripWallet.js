import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY = 'tripass-trip-wallet'

const initialTransactions = [
  { id: 1, date: '2026.08.02', type: '입금', title: '8월 여행 저축', note: 'KB국민은행 여행통장', amount: 700000 },
  { id: 2, date: '2026.07.28', type: '입금', title: '7월 저축 미션 달성', note: '식비 미션 · 여행 저축', amount: 120000 },
  { id: 3, date: '2026.07.01', type: '입금', title: '7월 여행 저축', note: '신한은행 통장', amount: 650000 },
]

function savedData() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') } catch { return null }
}

export const useTripWalletStore = defineStore('tripWallet', () => {
  const saved = savedData()
  const balance = ref(Number(saved?.balance ?? 2_000_000))
  const monthlyDeposits = ref(saved?.monthlyDeposits ?? [420000, 520000, 650000, 700000])
  const transactions = ref(saved?.transactions ?? initialTransactions)

  const monthDeposit = computed(() => transactions.value
    .filter(item => item.date.startsWith('2026.08') && item.type === '입금')
    .reduce((sum, item) => sum + item.amount, 0))

  function deposit(amount, title = '여행 저축', note = '연결 계좌에서 송금') {
    const value = Number(String(amount).replace(/[^0-9]/g, '')) || 0
    if (value <= 0) return false
    balance.value += value
    transactions.value.unshift({
      id: Date.now(), date: '2026.08.13', type: '입금', title, note, amount: value,
    })
    monthlyDeposits.value = [...monthlyDeposits.value.slice(-3), monthDeposit.value + value]
    return true
  }

  function withdraw(amount, title = '여행 자금 출금') {
    const value = Number(String(amount).replace(/[^0-9]/g, '')) || 0
    if (value <= 0 || value > balance.value) return false
    balance.value -= value
    transactions.value.unshift({
      id: Date.now(), date: '2026.08.13', type: '출금', title, note: 'TRIP 월렛', amount: -value,
    })
    return true
  }

  watch(() => ({ balance: balance.value, monthlyDeposits: monthlyDeposits.value, transactions: transactions.value }),
    value => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)), { deep: true })

  return { balance, monthlyDeposits, transactions, monthDeposit, deposit, withdraw }
})
