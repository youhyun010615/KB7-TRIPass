import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { fetchSavingReadiness } from '@/api/savingReadiness';

function errorMessageOf(error, fallback) {
  return error.response?.data?.message || fallback;
}

export const useSavingReadinessStore = defineStore('savingReadiness', () => {
  const status = ref('');
  const loading = ref(false);
  const errorMessage = ref('');
  const loaded = ref(false);

  const isReady = computed(() => status.value === 'READY');
  const needsTravelGoal = computed(
    () => status.value === 'TRAVEL_GOAL_REQUIRED',
  );
  const needsFinancialAsset = computed(
    () => status.value === 'FINANCIAL_ASSET_REQUIRED',
  );
  const needsTravelGoalAndFinancialAsset = computed(
    () => status.value === 'TRAVEL_GOAL_AND_FINANCIAL_ASSET_REQUIRED',
  );
  const needsSetup = computed(
    () => loaded.value && !isReady.value && !errorMessage.value,
  );

  async function load({ force = false } = {}) {
    if (!force && loaded.value) return status.value;

    loading.value = true;
    errorMessage.value = '';
    try {
      const data = await fetchSavingReadiness();
      status.value = data?.status || '';
      loaded.value = true;
      return status.value;
    } catch (error) {
      status.value = '';
      errorMessage.value = errorMessageOf(
        error,
        '저축 미션 준비 상태를 확인하지 못했어요.',
      );
      return null;
    } finally {
      loading.value = false;
    }
  }

  return {
    status,
    loading,
    errorMessage,
    loaded,
    isReady,
    needsTravelGoal,
    needsFinancialAsset,
    needsTravelGoalAndFinancialAsset,
    needsSetup,
    load,
  };
});
