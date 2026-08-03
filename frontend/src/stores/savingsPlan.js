import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useTravelStore } from '@/stores/travel'

const STORAGE_KEY = 'tripass-savings-plan'
const AVAILABLE_FUNDS = 1_000_000
const TARGET_MONTHS = 6
const BASE_YEAR = 2026
const BASE_MONTH_INDEX = 6

function loadSavedPlan() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch {
    return null
  }
}

function addMonths(months) {
  const date = new Date(BASE_YEAR, BASE_MONTH_INDEX + months, 1)
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}`
}

export const useSavingsPlanStore = defineStore('savingsPlan', () => {
  const travelStore = useTravelStore()
  const saved = loadSavedPlan()
  const savingMethod = ref(saved?.savingMethod ?? null)
  const monthlySavings = ref(Number(saved?.monthlySavings ?? 0))

  const totalTargetAmount = computed(() => travelStore.totalTargetAmount || 5_000_000)
  const securedAmount = computed(() => travelStore.totalAllocatedAmount || 2_000_000)
  const shortageAmount = computed(() => Math.max(0, totalTargetAmount.value - securedAmount.value))
  const securedPercent = computed(() => totalTargetAmount.value
    ? Math.min(100, Math.round((securedAmount.value / totalTargetAmount.value) * 100))
    : 0)
  const availableFunds = computed(() => AVAILABLE_FUNDS)
  const remainingAvailableFunds = computed(() => availableFunds.value - monthlySavings.value)
  const insufficientFunds = computed(() => Math.max(0, monthlySavings.value - availableFunds.value))
  const recommendedMonthlySavings = computed(() => {
    if (!shortageAmount.value) return 0
    return Math.ceil(shortageAmount.value / TARGET_MONTHS / 10_000) * 10_000
  })
  const additionalRecommendedAmount = computed(() => Math.max(0, recommendedMonthlySavings.value - monthlySavings.value))
  const expectedMonths = computed(() => monthlySavings.value > 0
    ? Math.ceil(shortageAmount.value / monthlySavings.value)
    : 0)
  const expectedDeparture = computed(() => expectedMonths.value ? addMonths(expectedMonths.value) : null)
  const delayMonths = computed(() => Math.max(0, expectedMonths.value - TARGET_MONTHS))
  const status = computed(() => {
    if (monthlySavings.value > availableFunds.value) return 'error'
    if (!monthlySavings.value) return 'unset'
    if (monthlySavings.value >= recommendedMonthlySavings.value) return 'success'
    return 'warning'
  })
  const canSave = computed(() => savingMethod.value === 'product'
    || (savingMethod.value === 'cash' && monthlySavings.value > 0 && !insufficientFunds.value))

  function selectMethod(method) {
    savingMethod.value = method
    if (method === 'cash' && !monthlySavings.value) monthlySavings.value = recommendedMonthlySavings.value
    if (method === 'product') monthlySavings.value = 0
  }

  function setMonthlySavings(value) {
    monthlySavings.value = Math.max(0, Number(String(value).replace(/[^0-9]/g, '')) || 0)
  }

  function resetSavingsPlan() {
    savingMethod.value = null
    monthlySavings.value = 0
    localStorage.removeItem(STORAGE_KEY)
  }

  watch(
    () => ({ savingMethod: savingMethod.value, monthlySavings: monthlySavings.value }),
    (value) => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)),
    { deep: true },
  )

  return {
    savingMethod,
    monthlySavings,
    totalTargetAmount,
    securedAmount,
    shortageAmount,
    securedPercent,
    availableFunds,
    remainingAvailableFunds,
    insufficientFunds,
    recommendedMonthlySavings,
    additionalRecommendedAmount,
    expectedMonths,
    expectedDeparture,
    delayMonths,
    status,
    canSave,
    selectMethod,
    setMonthlySavings,
    resetSavingsPlan,
  }
})
