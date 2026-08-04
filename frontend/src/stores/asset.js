import { computed, reactive, ref, watch } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY = 'tripass-asset-management'

export const PREPAID_SCOPE_META = {
  ALL: { name: '전체', flag: '🌍' },
  COMMON: { name: '공통', flag: '🌐', description: '모든 국가에 공통으로 적용되는 항목' },
  FR: { name: '프랑스', flag: '🇫🇷', description: '프랑스 여행 관련 사전 지출 항목' },
  CH: { name: '스위스', flag: '🇨🇭', description: '스위스 여행 관련 사전 지출 항목' },
  DE: { name: '독일', flag: '🇩🇪', description: '독일 여행 관련 사전 지출 항목' },
  JP: { name: '일본', flag: '🇯🇵', description: '일본 여행 관련 사전 지출 항목' },
  VN: { name: '베트남', flag: '🇻🇳', description: '베트남 여행 관련 사전 지출 항목' },
}

const accountSeed = [
  { id: 1, bank: 'KB국민은행', name: 'KB국민은행 여행통장', number: '****4821', balance: 5_200_000, type: '급여계좌', symbol: '국', tone: '#fff5d8', accent: '#a56b00', primary: true },
  { id: 2, bank: '신한은행', name: '신한은행 통장', number: '****5678', balance: 3_000_000, type: '적금', symbol: '신', tone: '#e3f7f2', accent: '#10a88d' },
  { id: 3, bank: '카카오뱅크', name: '카카오뱅크 입출금통장', number: '****9012', balance: 4_250_000, type: '입출금', symbol: '카', tone: '#fff5d8', accent: '#7c5a00' },
]

const transactionSeed = [
  { id: 1, accountId: 1, date: '2024-07-18', dateLabel: '2024.07.18 (목)', time: '14:22', merchant: '비엣포', category: '식비', amount: -85_000, method: 'KB국민은행 여행통장', user: '비엣포', balanceAfter: 4_965_000, memo: '여행 준비 식사' },
  { id: 2, accountId: 2, date: '2024-07-18', dateLabel: '2024.07.18 (목)', time: '18:40', merchant: '신한은행 정기적금', category: '자동이체', amount: -300_000, method: '신한은행 통장', user: '신한은행', balanceAfter: 3_000_000, memo: '7월 정기 적금' },
  { id: 3, accountId: 1, date: '2024-07-19', dateLabel: '2024.07.19 (금)', time: '14:22', merchant: '스타벅스 강남점', category: '카페', amount: -10_000, method: 'KB국민은행 여행통장', user: '스타벅스', balanceAfter: 4_955_000, memo: '' },
  { id: 4, accountId: 1, date: '2024-07-19', dateLabel: '2024.07.19 (금)', time: '15:22', merchant: '지에스리테일(GS25)', category: '생활비', amount: -4_500, method: 'KB국민은행 여행통장', user: 'GS25', balanceAfter: 4_950_500, memo: '' },
  { id: 5, accountId: 1, date: '2024-07-19', dateLabel: '2024.07.19 (금)', time: '17:13', merchant: '쿠팡이츠', category: '식비', amount: -5_000, method: 'KB국민은행 여행통장', user: '쿠팡이츠', balanceAfter: 4_945_500, memo: '' },
  { id: 6, accountId: 1, date: '2024-07-19', dateLabel: '2024.07.19 (금)', time: '20:30', merchant: '7월 급여', category: '급여', amount: 2_500_000, method: 'KB국민은행 여행통장', user: 'TRIPASS', balanceAfter: 5_200_000, memo: '7월 급여 입금' },
  { id: 7, accountId: 3, date: '2024-07-17', dateLabel: '2024.07.17 (수)', time: '12:30', merchant: '교통카드 충전', category: '교통비', amount: -50_000, method: '카카오뱅크 입출금통장', user: '카카오페이', balanceAfter: 4_250_000, memo: '' },
]

const fixedExpenseSeed = [
  { id: 1, name: '월세', day: 28, amount: 800_000, accountId: 1, alert: '3일 전', memo: '매월 월세', icon: '⌂', color: '#ff6973', active: true },
  { id: 2, name: '통신비', day: 31, amount: 50_000, accountId: 1, alert: '당일', memo: '', icon: '⌕', color: '#4d79ff', active: true },
  { id: 3, name: '공과금', day: 25, amount: 50_000, accountId: 2, alert: '1일 전', memo: '전기·수도·가스', icon: '●', color: '#f3a21a', active: true },
  { id: 4, name: '교통비', day: 1, amount: 80_000, accountId: 3, alert: '당일', memo: '', icon: '▣', color: '#14af70', active: true },
  { id: 5, name: '보험료', day: 1, amount: 120_000, accountId: 1, alert: '1일 전', memo: '', icon: '▤', color: '#124be1', active: true },
  { id: 6, name: '구독료', day: 1, amount: 30_000, accountId: 3, alert: '당일', memo: '', icon: '▱', color: '#32c788', active: true },
  { id: 7, name: '여행 저축 금액', day: 1, amount: 500_000, accountId: 1, alert: '당일', memo: '여행 목표 저축', icon: '▰', color: '#21a66f', active: true, travelSaving: true },
]

const prepaidSeed = [
  { id: 1, scope: 'COMMON', name: '항공권', date: '2025.06.20', amount: 900_000, icon: '✈️' },
  { id: 2, scope: 'COMMON', name: '여행자 보험', date: '2025.06.20', amount: 80_000, icon: '◇' },
  { id: 3, scope: 'CH', name: '호텔(숙박비)', date: '2025.06.20', amount: 350_000, icon: '▦' },
  { id: 4, scope: 'CH', name: '투어/액티비티', date: '2025.06.20', amount: 100_000, icon: '●' },
  { id: 5, scope: 'CH', name: '교통비', date: '2025.06.20', amount: 50_000, icon: '▣' },
  { id: 6, scope: 'FR', name: '호텔(숙박비)', date: '2025.06.20', amount: 220_000, icon: '▦' },
  { id: 7, scope: 'FR', name: '투어/액티비티', date: '2025.06.20', amount: 70_000, icon: '●' },
  { id: 8, scope: 'FR', name: '공항 픽업/교통', date: '2025.06.20', amount: 30_000, icon: '◆' },
]

function loadSavedState() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAssetStore = defineStore('asset', () => {
  const saved = loadSavedState()
  const accounts = reactive(saved?.accounts ?? accountSeed.map((item) => ({ ...item })))
  const transactions = reactive(saved?.transactions ?? transactionSeed.map((item) => ({ ...item })))
  const fixedExpenses = reactive(saved?.fixedExpenses ?? fixedExpenseSeed.map((item) => ({ ...item })))
  const prepaidExpenses = reactive(saved?.prepaidExpenses ?? prepaidSeed.map((item) => ({ ...item })))
  const transactionFilter = ref(saved?.transactionFilter ?? 'all')
  const selectedDate = ref(saved?.selectedDate ?? '2024-07-19')
  const selectedPrepaidScope = ref(saved?.selectedPrepaidScope ?? 'ALL')

  const totalAssets = computed(() => accounts.reduce((sum, item) => sum + Number(item.balance || 0), 0))
  const activeFixedTotal = computed(() => fixedExpenses.filter((item) => item.active).reduce((sum, item) => sum + Number(item.amount || 0), 0))
  const prepaidTotal = computed(() => prepaidExpenses.reduce((sum, item) => sum + Number(item.amount || 0), 0))
  const commonPrepaidTotal = computed(() => prepaidExpenses.filter((item) => item.scope === 'COMMON').reduce((sum, item) => sum + Number(item.amount || 0), 0))
  const prepaidCountryScopes = computed(() => [...new Set(prepaidExpenses.filter((item) => item.scope !== 'COMMON').map((item) => item.scope))])
  const commonAllocation = computed(() => prepaidCountryScopes.value.length ? Math.floor(commonPrepaidTotal.value / prepaidCountryScopes.value.length) : 0)
  const prepaidGroups = computed(() => Object.entries(PREPAID_SCOPE_META)
    .filter(([scope]) => scope !== 'ALL')
    .map(([scope, meta]) => {
      const items = prepaidExpenses.filter((item) => item.scope === scope)
      const directTotal = items.reduce((sum, item) => sum + Number(item.amount || 0), 0)
      return {
        scope,
        ...meta,
        items,
        directTotal,
        allocatedCommon: scope === 'COMMON' ? 0 : commonAllocation.value,
        finalTotal: scope === 'COMMON' ? directTotal : directTotal + commonAllocation.value,
      }
    }))
  const depositCount = computed(() => transactions.filter((item) => item.amount > 0).length)
  const withdrawalCount = computed(() => transactions.filter((item) => item.amount < 0).length)

  const filteredTransactions = computed(() => transactions.filter((item) => {
    if (transactionFilter.value === 'deposit') return item.amount > 0
    if (transactionFilter.value === 'withdrawal') return item.amount < 0
    return true
  }))

  const groupedTransactions = computed(() => filteredTransactions.value.reduce((groups, item) => {
    const group = groups.find((entry) => entry.date === item.date)
    if (group) group.items.push(item)
    else groups.push({ date: item.date, label: item.dateLabel, items: [item] })
    return groups
  }, []))

  function getAccount(id) {
    return accounts.find((item) => item.id === Number(id))
  }

  function getTransaction(id) {
    return transactions.find((item) => item.id === Number(id))
  }

  function getFixedExpense(id) {
    return fixedExpenses.find((item) => item.id === Number(id))
  }

  function transactionsByAccount(accountId) {
    return transactions.filter((item) => item.accountId === Number(accountId))
  }

  function addFixedExpense(payload) {
    const amount = Math.max(0, Number(String(payload.amount).replace(/[^0-9]/g, '')) || 0)
    const day = Number(payload.day)
    if (!payload.name?.trim() || day < 1 || day > 31 || !amount || !getAccount(payload.accountId)) return false
    fixedExpenses.push({
      id: Date.now(), name: payload.name.trim(), day, amount,
      accountId: Number(payload.accountId), alert: payload.alert || '당일', memo: payload.memo?.trim() || '',
      icon: '▣', color: '#3475f4', active: true,
    })
    return true
  }

  function updateFixedExpense(id, patch) {
    const item = getFixedExpense(id)
    if (!item) return false
    Object.assign(item, patch)
    if ('amount' in patch) item.amount = Math.max(0, Number(String(patch.amount).replace(/[^0-9]/g, '')) || 0)
    if ('day' in patch) item.day = Math.min(31, Math.max(1, Number(patch.day) || 1))
    return true
  }

  function removeFixedExpense(id) {
    const index = fixedExpenses.findIndex((item) => item.id === Number(id))
    if (index < 0) return false
    fixedExpenses.splice(index, 1)
    return true
  }

  function addPrepaidExpense(payload) {
    const amount = Math.max(0, Number(String(payload.amount).replace(/[^0-9]/g, '')) || 0)
    const scope = payload.scope || payload.countryCode
    if (!payload.name?.trim() || !PREPAID_SCOPE_META[scope] || scope === 'ALL' || !payload.date || !amount) return false
    prepaidExpenses.unshift({
      id: Date.now(),
      scope,
      name: payload.name.trim(),
      date: payload.date,
      amount,
      memo: payload.memo?.trim() || '',
      icon: payload.icon || '▦',
    })
    return true
  }

  function updatePrepaidExpense(id, patch) {
    const item = prepaidExpenses.find((entry) => entry.id === Number(id))
    if (!item) return false
    Object.assign(item, patch)
    if ('amount' in patch) item.amount = Math.max(0, Number(String(patch.amount).replace(/[^0-9]/g, '')) || 0)
    return true
  }

  function removePrepaidExpense(id) {
    const index = prepaidExpenses.findIndex((item) => item.id === Number(id))
    if (index < 0) return false
    prepaidExpenses.splice(index, 1)
    return true
  }

  watch(
    () => ({
      accounts: accounts.map((item) => ({ ...item })),
      transactions: transactions.map((item) => ({ ...item })),
      fixedExpenses: fixedExpenses.map((item) => ({ ...item })),
      prepaidExpenses: prepaidExpenses.map((item) => ({ ...item })),
      transactionFilter: transactionFilter.value,
      selectedDate: selectedDate.value,
      selectedPrepaidScope: selectedPrepaidScope.value,
    }),
    (value) => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)),
    { deep: true },
  )

  return {
    accounts, transactions, fixedExpenses, prepaidExpenses,
    transactionFilter, selectedDate, selectedPrepaidScope, totalAssets, activeFixedTotal, prepaidTotal,
    commonPrepaidTotal, prepaidCountryScopes, commonAllocation, prepaidGroups,
    depositCount, withdrawalCount, filteredTransactions, groupedTransactions,
    getAccount, getTransaction, getFixedExpense, transactionsByAccount,
    addFixedExpense, updateFixedExpense, removeFixedExpense,
    addPrepaidExpense, updatePrepaidExpense, removePrepaidExpense,
  }
})
