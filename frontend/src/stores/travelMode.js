import { defineStore, storeToRefs } from 'pinia';
import { ref, computed } from 'vue';
import { switchTravelMode } from '@/api/travel';
import { useTravelStore } from './travel';

export const useTravelModeStore = defineStore('travelMode', () => {
  // 'savings' | 'travel'
  const mode = ref(localStorage.getItem('travelMode') ?? 'savings');
  const selectedDestination = ref(
    localStorage.getItem('travelModeDestination') ?? 'all',
  );
  const calculatorOpen = ref(false);
  const calculatorCurrency = ref(
    localStorage.getItem('travelModeCurrency') ?? 'EUR',
  );
  const calculatorAmount = ref(100);
  const needsAutoSelect = ref(true);
  const lifecycleChecked = ref(false);

  // 개발·시연 환경에서는 여행 준비/여행 모드를 자유롭게 오가며 화면을 검증한다.
  // 운영 전환 시 false로 변경하면 등록한 여행 기간에만 진입한다.
  const demoMode = ref(false);
  const travelStartDate = ref('2026-08-15');
  const travelEndDate = ref('2026-08-29');

  const isTravelMode = computed(() => mode.value === 'travel');
  const isSavingsMode = computed(() => mode.value === 'savings');
  const isWithinTravelPeriod = computed(() => {
    const travelStore = useTravelStore();
    const today = new Date();
    const startValue = travelStore.activeTrip?.startDate || travelStartDate.value;
    const endValue = travelStore.activeTrip?.endDate || travelEndDate.value;
    const start = new Date(`${startValue}T00:00:00`);
    const end = new Date(`${endValue}T23:59:59`);
    return today >= start && today <= end;
  });
  const canEnterTravelMode = computed(
    () => demoMode.value || isWithinTravelPeriod.value,
  );

  function setMode(newMode) {
    if (newMode === 'travel' && !canEnterTravelMode.value) return false;
    if (newMode === 'travel' && mode.value !== 'travel') {
      needsAutoSelect.value = true;
    }
    mode.value = newMode;
    localStorage.setItem('travelMode', newMode);
    return true;
  }

  function toggleMode() {
    setMode(mode.value === 'savings' ? 'travel' : 'savings');
  }

  function selectDestination(code) {
    selectedDestination.value = code;
    localStorage.setItem('travelModeDestination', code);
  }

  function openCalculator(currency) {
    if (currency) setCalculatorCurrency(currency);
    calculatorOpen.value = true;
  }

  function closeCalculator() {
    calculatorOpen.value = false;
  }

  function setCalculatorCurrency(currency) {
    calculatorCurrency.value = currency;
    localStorage.setItem('travelModeCurrency', currency);
  }

  async function toggleTravelMode(targetTravelMode) {
    const travelStore = useTravelStore();
    const { tripId } = storeToRefs(travelStore);
    if (!tripId.value) {
      console.error('진행 중인 여행 정보가 없습니다.');
      return false;
    }
    try {
      const result = await switchTravelMode(tripId.value, targetTravelMode);
      setMode(result.isTravelMode ? 'travel' : 'savings');
      return true;
    } catch (error) {
      console.error(error);
      return false;
    }
  }

  function consumeAutoSelect() {
    if (!needsAutoSelect.value) return false;
    needsAutoSelect.value = false;
    return true;
  }

  function resetForNewSession() {
    mode.value = 'savings';
    selectedDestination.value = 'all';
    calculatorOpen.value = false;
    calculatorCurrency.value = 'EUR';
    calculatorAmount.value = 100;
    needsAutoSelect.value = true;
    lifecycleChecked.value = false;
  }

  return {
    mode,
    selectedDestination,
    calculatorOpen,
    calculatorCurrency,
    calculatorAmount,
    needsAutoSelect,
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
    toggleTravelMode,
    consumeAutoSelect,
    lifecycleChecked,
    resetForNewSession,
  };
});
