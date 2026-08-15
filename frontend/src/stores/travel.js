import { computed, reactive, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { createTrip, fetchCurrentTrip } from '@/api/travel'

const countrySeed = [
  { code: 'FR', name: '프랑스', city: '파리', flag: '🇫🇷', accent: '#0066ff', securedBudget: 1_900_000 },
  { code: 'CH', name: '스위스', city: '인터라켄', flag: '🇨🇭', accent: '#c8173c', securedBudget: 3_100_000 },
  { code: 'DE', name: '독일', city: '베를린', flag: '🇩🇪', accent: '#151515', securedBudget: 1_500_000 },
  { code: 'JP', name: '일본', city: '도쿄', flag: '🇯🇵', accent: '#ef4b91', securedBudget: 1_200_000 },
  { code: 'HK', name: '홍콩', city: '홍콩', flag: '🇭🇰', accent: '#b8202e', securedBudget: 900_000 },
  { code: 'AE', name: '아랍에미리트', city: '두바이', flag: '🇦🇪', accent: '#0f766e', securedBudget: 1_700_000 },
  { code: 'AU', name: '호주', city: '시드니', flag: '🇦🇺', accent: '#1565c0', securedBudget: 1_800_000 },
  { code: 'BH', name: '바레인', city: '마나마', flag: '🇧🇭', accent: '#c62828', securedBudget: 1_600_000 },
  { code: 'BN', name: '브루나이', city: '반다르스리브가완', flag: '🇧🇳', accent: '#f2b705', securedBudget: 1_300_000 },
  { code: 'CA', name: '캐나다', city: '밴쿠버', flag: '🇨🇦', accent: '#e53935', securedBudget: 1_600_000 },
  { code: 'CN', name: '중국', city: '상하이', flag: '🇨🇳', accent: '#d32f2f', securedBudget: 1_100_000 },
  { code: 'DK', name: '덴마크', city: '코펜하겐', flag: '🇩🇰', accent: '#b71c1c', securedBudget: 1_700_000 },
  { code: 'GB', name: '영국', city: '런던', flag: '🇬🇧', accent: '#1e3a8a', securedBudget: 2_000_000 },
  { code: 'ID', name: '인도네시아', city: '발리', flag: '🇮🇩', accent: '#c62828', securedBudget: 950_000 },
  { code: 'KW', name: '쿠웨이트', city: '쿠웨이트시티', flag: '🇰🇼', accent: '#00897b', securedBudget: 1_600_000 },
  { code: 'MY', name: '말레이시아', city: '쿠알라룸푸르', flag: '🇲🇾', accent: '#1565c0', securedBudget: 950_000 },
  { code: 'NO', name: '노르웨이', city: '오슬로', flag: '🇳🇴', accent: '#c62828', securedBudget: 1_900_000 },
  { code: 'NZ', name: '뉴질랜드', city: '오클랜드', flag: '🇳🇿', accent: '#1565c0', securedBudget: 1_700_000 },
  { code: 'SA', name: '사우디아라비아', city: '리야드', flag: '🇸🇦', accent: '#15803d', securedBudget: 1_450_000 },
  { code: 'SE', name: '스웨덴', city: '스톡홀름', flag: '🇸🇪', accent: '#1e40af', securedBudget: 1_800_000 },
  { code: 'SG', name: '싱가포르', city: '싱가포르', flag: '🇸🇬', accent: '#dc2626', securedBudget: 1_450_000 },
  { code: 'TH', name: '태국', city: '방콕', flag: '🇹🇭', accent: '#2563eb', securedBudget: 900_000 },
  { code: 'US', name: '미국', city: '뉴욕', flag: '🇺🇸', accent: '#1d4ed8', securedBudget: 1_900_000 },
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
  // 여행 등록 API(POST /trips) 응답으로 받은 실제 tripId와, 국가 코드별 tripCountryId.
  // 여행 일정(schedule) API 호출 시 이 값들이 필요하다.
  const tripId = ref(savedGoal?.tripId ?? null)
  const tripCountryIdByCode = reactive(savedGoal?.tripCountryIdByCode ?? {})
  const errorMessage = ref('')

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

  // TRIP 월렛은 서비스가 제공하는 가상 지갑이다. 외부 계좌 배분이 아니라
  // 여행 일정·국가별 목표 예산이 확정되면 저축 계획을 시작할 수 있다.
  const canCompleteGoal = computed(() => canReviewPlan.value && totalTargetAmount.value > 0)

  watch(
    () => ({
      hasTravelGoal: hasTravelGoal.value,
      tripName: tripName.value,
      selectedCountryCodes: [...selectedCountryCodes.value],
      plans: { ...plans },
      allocations: { ...allocations },
      tripId: tripId.value,
      tripCountryIdByCode: { ...tripCountryIdByCode },
    }),
    (value) => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)),
    { deep: true },
  )

  async function completeGoal() {
    if (!canCompleteGoal.value) return false

    errorMessage.value = ''

    try {
      const response = await createTrip({
        tripName: tripName.value,
        countries: selectedPlans.value.map((plan) => ({
          countryName: plan.name,
          startDate: plan.startDate,
          endDate: plan.endDate,
          targetBudget: plan.targetBudget,
        })),
      })

      const tripCountryIdByName = Object.fromEntries(
        (response.countries || []).map((item) => [item.countryName, item.tripCountryId]),
      )

      Object.keys(tripCountryIdByCode).forEach((key) => delete tripCountryIdByCode[key])
      selectedPlans.value.forEach((plan) => {
        tripCountryIdByCode[plan.code] = tripCountryIdByName[plan.name] ?? null
      })

      tripId.value = response.tripId
      hasTravelGoal.value = true
      return true
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '여행 등록에 실패했어요.'
      return false
    }
  }

  // 서버에 이미 등록된 여행을 부팅 시점에 알아내 로컬 상태를 채운다.
  // localStorage 캐시가 다른 기기·세션에서 만든 여행이나 오래된 tripId를 들고 있을 수 있어,
  // 항상 서버 응답을 최신 진실로 삼아 덮어쓴다(등록된 여행이 없으면 기존 로컬 상태를 건드리지 않는다).
  async function loadCurrentTrip() {
    try {
      const trip = await fetchCurrentTrip()
      if (!trip) return false

      tripId.value = trip.tripId
      tripName.value = trip.tripName
      selectedCountryCodes.value = []
      Object.keys(plans).forEach((key) => delete plans[key])
      Object.keys(tripCountryIdByCode).forEach((key) => delete tripCountryIdByCode[key])

      ;(trip.countries || []).forEach((countryDetail) => {
        const meta = countries.find((item) => item.name === countryDetail.countryName)
        if (!meta) return

        selectedCountryCodes.value.push(meta.code)
        plans[meta.code] = {
          ...meta,
          startDate: countryDetail.arrivalDate,
          endDate: countryDetail.departureDate,
          targetBudget: Number(countryDetail.targetBudget),
        }
        tripCountryIdByCode[meta.code] = countryDetail.tripCountryId
      })

      hasTravelGoal.value = true
      return true
    } catch {
      return false
    }
  }

  function resetGoal() {
    hasTravelGoal.value = false
    tripName.value = ''
    selectedCountryCodes.value = []
    tripId.value = null
    Object.keys(plans).forEach((key) => delete plans[key])
    Object.keys(allocations).forEach((key) => delete allocations[key])
    Object.keys(tripCountryIdByCode).forEach((key) => delete tripCountryIdByCode[key])
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
    tripId,
    tripCountryIdByCode,
    errorMessage,
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
    loadCurrentTrip,
    resetGoal,
    enterTravelMode,
    exitTravelMode,
  }
})
