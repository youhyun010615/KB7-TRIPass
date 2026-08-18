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

  // 저축 미션은 여행 목표와 독립적으로 동작한다. 금융 자산만 연결되어 있다면
  // 서버가 TRAVEL_GOAL_REQUIRED를 반환해도 미션 화면을 정상 노출한다.
  const isReady = computed(
    () => status.value === 'READY' || status.value === 'TRAVEL_GOAL_REQUIRED',
  );
  const needsFinancialAsset = computed(
    () =>
      status.value === 'FINANCIAL_ASSET_REQUIRED' ||
      status.value === 'TRAVEL_GOAL_AND_FINANCIAL_ASSET_REQUIRED',
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
    needsFinancialAsset,
    needsSetup,
    load,
  };
});
