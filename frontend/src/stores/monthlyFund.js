import { computed, reactive, ref, watch } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY = 'tripass-monthly-fund'

const categorySeed = [
  { id: 'food', name: '식비', icon: '🍴', description: '외식, 배달, 식료품 구입 등', target: 250_000, color: '#7431ff' },
  { id: 'cafe', name: '카페', icon: '☕', description: '커피, 디저트 등 카페 이용', target: 100_000, color: '#8b6f62' },
  { id: 'living', name: '생활비', icon: '🛒', description: '편의점, 마트, 잡화 등', target: 300_000, color: '#21a66f' },
  { id: 'shopping', name: '쇼핑', icon: '🛍️', description: '의류, 신발, 화장품, 온라인 쇼핑 등', target: 150_000, color: '#ef4b91' },
  { id: 'hobby', name: '취미·여가', icon: '🚲', description: '영화, 공연, 여행, 운동 등', target: 100_000, color: '#d97706' },
  { id: 'etc', name: '기타', icon: '•••', description: '위 카테고리에 포함되지 않는 항목', target: 300_000, color: '#94a3b8' },
]

const transactionSeed = [
  { id: 1, date: '7/19 (토)', merchant: '편의점', amount: 8_500, categoryId: 'food', icon: '▰' },
  { id: 2, date: '7/18 (금)', merchant: '구내식당', amount: 6_000, categoryId: 'food', icon: '🍴' },
  { id: 3, date: '7/17 (목)', merchant: '마트', amount: 42_000, categoryId: 'food', icon: '🛒' },
  { id: 4, date: '7/16 (수)', merchant: '카페', amount: 5_500, categoryId: 'food', icon: '☕' },
  { id: 5, date: '7/15 (화)', merchant: '배달음식', amount: 18_000, categoryId: 'food', icon: '🛵' },
  { id: 6, date: '7/14 (월)', merchant: '커피빈', amount: 34_500, categoryId: 'cafe', icon: '☕' },
  { id: 7, date: '7/12 (토)', merchant: '생활용품점', amount: 9_800, categoryId: 'living', icon: '🧴' },
  { id: 8, date: '7/10 (목)', merchant: '편집숍', amount: 55_000, categoryId: 'shopping', icon: '🛍️' },
  { id: 9, date: '7/08 (화)', merchant: '전시회', amount: 30_000, categoryId: 'hobby', icon: '🎨' },
]

function loadSavedState() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch {
    return null
  }
}

export const useMonthlyFundStore = defineStore('monthlyFund', () => {
  const saved = loadSavedState()
  const monthlyIncome = ref(saved?.monthlyIncome ?? 3_500_000)
  const fixedExpense = ref(saved?.fixedExpense ?? 1_800_000)
  const categories = reactive(saved?.categories ?? categorySeed.map((item) => ({ ...item })))
  const transactions = reactive(saved?.transactions ?? transactionSeed.map((item) => ({ ...item })))
  const prepaidExpenses = reactive(saved?.prepaidExpenses ?? [])

  const categoryTargetTotal = computed(() => categories.reduce((sum, item) => sum + Number(item.target || 0), 0))
  const availableFunds = computed(() => Math.max(0, monthlyIncome.value - fixedExpense.value))
  const freeFunds = computed(() => Math.max(0, availableFunds.value - categoryTargetTotal.value))
  const prepaidTotal = computed(() => prepaidExpenses.reduce((sum, item) => sum + Number(item.amount || 0), 0))

  function getCategory(categoryId) {
    return categories.find((item) => item.id === categoryId)
  }

  function categorySpent(categoryId) {
    return transactions
      .filter((item) => item.categoryId === categoryId)
      .reduce((sum, item) => sum + Number(item.amount || 0), 0)
  }

  function categorySummary(categoryId) {
    const category = getCategory(categoryId)
    if (!category) return null
    const spent = categorySpent(categoryId)
    const target = Number(category.target || 0)
    return {
      ...category,
      spent,
      remaining: Math.max(0, target - spent),
      percent: target > 0 ? Math.min(100, Math.round((spent / target) * 100)) : 0,
    }
  }

  const categorySummaries = computed(() => categories.map((item) => categorySummary(item.id)))

  function updateCategoryTarget(categoryId, value) {
    const category = getCategory(categoryId)
    if (!category) return
    category.target = Math.max(0, Number(String(value).replace(/[^0-9]/g, '')) || 0)
  }

  function updateTransactionCategory(transactionId, categoryId) {
    const transaction = transactions.find((item) => item.id === Number(transactionId))
    if (!transaction || (categoryId !== 'prepaid' && !getCategory(categoryId))) return false
    transaction.categoryId = categoryId
    return true
  }

  function addPrepaidExpense(payload) {
    const amount = Math.max(0, Number(String(payload.amount).replace(/[^0-9]/g, '')) || 0)
    if (!payload.name?.trim() || !payload.countryCode || !payload.date || !amount) return false
    prepaidExpenses.unshift({
      id: Date.now(),
      name: payload.name.trim(),
      countryCode: payload.countryCode,
      date: payload.date,
      amount,
      memo: payload.memo?.trim() ?? '',
    })
    return true
  }

  watch(
    () => ({
      monthlyIncome: monthlyIncome.value,
      fixedExpense: fixedExpense.value,
      categories: categories.map((item) => ({ ...item })),
      transactions: transactions.map((item) => ({ ...item })),
      prepaidExpenses: prepaidExpenses.map((item) => ({ ...item })),
    }),
    (value) => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)),
    { deep: true },
  )

  return {
    monthlyIncome,
    fixedExpense,
    categories,
    transactions,
    prepaidExpenses,
    categoryTargetTotal,
    categorySummaries,
    availableFunds,
    freeFunds,
    prepaidTotal,
    getCategory,
    categorySpent,
    categorySummary,
    updateCategoryTarget,
    updateTransactionCategory,
    addPrepaidExpense,
  }
})
