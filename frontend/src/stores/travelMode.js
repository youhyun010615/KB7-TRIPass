import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useTravelModeStore = defineStore('travelMode', () => {
  // 'savings' | 'travel'
  const mode = ref(localStorage.getItem('travelMode') ?? 'savings')
  const selectedDestination = ref(localStorage.getItem('travelModeDestination') ?? 'all')
  const calculatorOpen = ref(false)
  const calculatorCurrency = ref(localStorage.getItem('travelModeCurrency') ?? 'EUR')
  const calculatorAmount = ref(100)

  // 개발·시연 환경에서는 여행 준비/여행 모드를 자유롭게 오가며 화면을 검증한다.
  // 운영 전환 시 false로 변경하면 등록한 여행 기간에만 진입한다.
  const demoMode = ref(true)
  const travelStartDate = ref('2026-08-15')
  const travelEndDate = ref('2026-08-29')

  const isTravelMode = computed(() => mode.value === 'travel')
  const isSavingsMode = computed(() => mode.value === 'savings')
  const isWithinTravelPeriod = computed(() => {
    const today = new Date()
    const start = new Date(`${travelStartDate.value}T00:00:00`)
    const end = new Date(`${travelEndDate.value}T23:59:59`)
    return today >= start && today <= end
  })
  const canEnterTravelMode = computed(() => demoMode.value || isWithinTravelPeriod.value)

  function setMode(newMode) {
    if (newMode === 'travel' && !canEnterTravelMode.value) return false
    mode.value = newMode
    localStorage.setItem('travelMode', newMode)
    return true
  }

  function toggleMode() {
    setMode(mode.value === 'savings' ? 'travel' : 'savings')
  }

  function selectDestination(code) {
    selectedDestination.value = code
    localStorage.setItem('travelModeDestination', code)
  }

  function openCalculator(currency) {
    if (currency) setCalculatorCurrency(currency)
    calculatorOpen.value = true
  }

  function closeCalculator() {
    calculatorOpen.value = false
  }

  function setCalculatorCurrency(currency) {
    calculatorCurrency.value = currency
    localStorage.setItem('travelModeCurrency', currency)
  }

  return {
    mode,
    selectedDestination,
    calculatorOpen,
    calculatorCurrency,
    calculatorAmount,
    demoMode,
    travelStartDate,
    travelEndDate,
    isTravelMode,
    isSavingsMode,
    isWithinTravelPeriod,
    canEnterTravelMode,
    setMode,
    toggleMode,
    selectDestination,
    openCalculator,
    closeCalculator,
    setCalculatorCurrency,
  }
})
