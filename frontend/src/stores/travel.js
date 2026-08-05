import { computed, reactive, ref, watch } from 'vue'
import { defineStore } from 'pinia'

const countrySeed = [
  { code: 'FR', name: '프랑스', city: '파리', flag: '🇫🇷', accent: '#0066ff', securedBudget: 1_900_000 },
  { code: 'CH', name: '스위스', city: '인터라켄', flag: '🇨🇭', accent: '#c8173c', securedBudget: 3_100_000 },
  { code: 'DE', name: '독일', city: '베를린', flag: '🇩🇪', accent: '#151515', securedBudget: 1_500_000 },
  { code: 'JP', name: '일본', city: '도쿄', flag: '🇯🇵', accent: '#ef4b91', securedBudget: 1_200_000 },
  { code: 'HK', name: '홍콩', city: '홍콩', flag: '🇭🇰', accent: '#b8202e', securedBudget: 900_000 },
]

const accountSeed = [
  { id: 1, bank: 'KB국민은행', name: 'KB국민은행 여행통장', number: '**** 5320', balance: 1_500_000 },
  { id: 2, bank: '신한은행', name: '신한은행 통장', number: '**** 8421', balance: 3_000_000 },
  { id: 3, bank: '카카오뱅크', name: '카카오뱅크 입출금통장', number: '**** 1108', balance: 2_450_000 },
]

const createPlan = (country) => ({
  ...country,
  startDate: '',
  endDate: '',
  targetBudget: country.securedBudget,
})

const STORAGE_KEY = 'tripass-travel-goal'

function loadSavedGoal() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch {
    return null
  }
}

export const useTravelStore = defineStore('travel', () => {
  const savedGoal = loadSavedGoal()
  const isTravelMode = ref(false)
  const selectedCountry = ref(null)
  const travelBudget = ref(null)
  const hasTravelGoal = ref(savedGoal?.hasTravelGoal ?? false)
  const tripName = ref(savedGoal?.tripName ?? '')
  const selectedCountryCodes = ref(savedGoal?.selectedCountryCodes ?? [])
  const plans = reactive(savedGoal?.plans ?? {})
  const allocations = reactive(savedGoal?.allocations ?? {})

  const countries = countrySeed
  const accounts = accountSeed
  const selectedPlans = computed(() => selectedCountryCodes.value.map((code) => plans[code]))
  const totalTargetAmount = computed(() => selectedPlans.value.reduce((sum, plan) => sum + Number(plan.targetBudget || 0), 0))
  const totalAllocatedAmount = computed(() => Object.values(allocations).reduce((sum, amount) => sum + Number(amount || 0), 0))
  const selectedAccountCount = computed(() => Object.values(allocations).filter((amount) => Number(amount) > 0).length)

  function toggleCountry(code) {
    const index = selectedCountryCodes.value.indexOf(code)
    if (index >= 0) {
      selectedCountryCodes.value.splice(index, 1)
      delete plans[code]
      return
    }
    const country = countries.find((item) => item.code === code)
    if (!country) return
    selectedCountryCodes.value.push(code)
    plans[code] = createPlan(country)
  }

  function updatePlan(code, patch) {
    if (!plans[code]) return
    Object.assign(plans[code], patch)
  }

  function setAllocation(accountId, value) {
    const account = accounts.find((item) => item.id === accountId)
    if (!account) return
    const amount = Math.max(0, Math.min(Number(value) || 0, account.balance))
    allocations[accountId] = amount
  }

  function planError(plan) {
    if (!plan.startDate || !plan.endDate) return '여행 날짜를 선택해 주세요.'
    if (new Date(plan.endDate) < new Date(plan.startDate)) return '도착일은 출국일보다 빠를 수 없어요.'
    if (Number(plan.targetBudget) < Number(plan.securedBudget)) return '목표 예산은 사전 확보 예산보다 커야 해요.'
    return ''
  }

  const hasDateCollision = computed(() => {
    const dated = selectedPlans.value.filter((plan) => plan.startDate && plan.endDate)
    return dated.some((plan, index) => dated.slice(index + 1).some((other) => (
      new Date(plan.startDate) <= new Date(other.endDate)
      && new Date(other.startDate) <= new Date(plan.endDate)
    )))
  })

  const canReviewPlan = computed(() => (
    tripName.value.trim().length > 0
    && selectedPlans.value.length > 0
    && selectedPlans.value.every((plan) => !planError(plan))
    && !hasDateCollision.value
  ))

  const canCompleteGoal = computed(() => totalTargetAmount.value > 0 && totalAllocatedAmount.value > 0)

  watch(
    () => ({
      hasTravelGoal: hasTravelGoal.value,
      tripName: tripName.value,
      selectedCountryCodes: [...selectedCountryCodes.value],
      plans: { ...plans },
      allocations: { ...allocations },
    }),
    (value) => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)),
    { deep: true },
  )

  function completeGoal() {
    if (!canCompleteGoal.value) return false
    hasTravelGoal.value = true
    return true
  }

  function resetGoal() {
    hasTravelGoal.value = false
    tripName.value = ''
    selectedCountryCodes.value = []
    Object.keys(plans).forEach((key) => delete plans[key])
    Object.keys(allocations).forEach((key) => delete allocations[key])
    localStorage.removeItem(STORAGE_KEY)
  }

  function enterTravelMode(country, budget) {
    isTravelMode.value = true
    selectedCountry.value = country
    travelBudget.value = budget
  }

  function exitTravelMode() {
    isTravelMode.value = false
    selectedCountry.value = null
    travelBudget.value = null
  }

  return {
    isTravelMode,
    selectedCountry,
    travelBudget,
    hasTravelGoal,
    tripName,
    countries,
    accounts,
    selectedCountryCodes,
    selectedPlans,
    allocations,
    totalTargetAmount,
    totalAllocatedAmount,
    selectedAccountCount,
    hasDateCollision,
    canReviewPlan,
    canCompleteGoal,
    toggleCountry,
    updatePlan,
    setAllocation,
    planError,
    completeGoal,
    resetGoal,
    enterTravelMode,
    exitTravelMode,
  }
})
