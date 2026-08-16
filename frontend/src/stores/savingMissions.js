import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import {
  createSavingMissions,
  fetchMissionOptions,
  fetchMissionSelections,
  fetchSavingMissions,
  saveMissionSelections,
} from '@/api/savingMissions';

function previousYearMonth(baseDate = new Date()) {
  const previousMonth = new Date(
    baseDate.getFullYear(),
    baseDate.getMonth() - 1,
    1,
  );
  return `${previousMonth.getFullYear()}-${String(previousMonth.getMonth() + 1).padStart(2, '0')}`;
}

function nextYearMonth(yearMonth) {
  const [year, month] = yearMonth.split('-').map(Number);
  const nextMonth = new Date(year, month, 1);
  return `${nextMonth.getFullYear()}-${String(nextMonth.getMonth() + 1).padStart(2, '0')}`;
}

function errorMessageOf(error, fallback) {
  return error.response?.data?.message || fallback;
}

export const useSavingMissionsStore = defineStore('savingMissions', () => {
  const analysisYearMonth = ref('');
  const targetYearMonth = ref('');
  const options = ref([]);
  const selections = ref(null);
  const missions = ref(null);
  const selectedRates = ref({});
  const loading = ref(false);
  const submitting = ref(false);
  const errorMessage = ref('');

  const hasStartedMissions = computed(
    () => Number(missions.value?.missionCount || 0) > 0,
  );
  const selectedCount = computed(
    () => Object.keys(selectedRates.value).length,
  );
  const selectedOptions = computed(() =>
    options.value.flatMap((category) => {
      const rate = selectedRates.value[String(category.categoryId)];
      if (!rate) return [];
      const selectedOption = category.options?.find(
        (option) => Number(option.reductionRate) === Number(rate),
      );
      return selectedOption
        ? [{ ...category, selectedOption }]
        : [];
    }),
  );
  const expectedSavingAmount = computed(() =>
    selectedOptions.value.reduce(
      (sum, item) =>
        sum + Number(item.selectedOption.monthlyReductionTarget || 0),
      0,
    ),
  );

  function applySelections(savedSelections = []) {
    selectedRates.value = Object.fromEntries(
      savedSelections.map((selection) => [
        String(selection.categoryId),
        Number(selection.reductionRate),
      ]),
    );
  }

  async function load(yearMonth = previousYearMonth()) {
    analysisYearMonth.value = yearMonth;
    targetYearMonth.value = nextYearMonth(yearMonth);
    loading.value = true;
    errorMessage.value = '';

    try {
      const [optionData, selectionData, missionData] = await Promise.all([
        fetchMissionOptions(yearMonth),
        fetchMissionSelections(yearMonth),
        fetchSavingMissions(targetYearMonth.value),
      ]);
      options.value = optionData || [];
      selections.value = selectionData;
      missions.value = missionData;
      applySelections(selectionData?.selections || []);
    } catch (error) {
      options.value = [];
      selections.value = null;
      missions.value = null;
      selectedRates.value = {};
      errorMessage.value = errorMessageOf(
        error,
        '저축 미션 정보를 불러오지 못했어요.',
      );
    } finally {
      loading.value = false;
    }
  }

  function toggleCategory(category) {
    if (hasStartedMissions.value) return;
    const key = String(category.categoryId);
    const next = { ...selectedRates.value };
    if (next[key]) {
      delete next[key];
    } else {
      const defaultOption =
        category.options?.find((option) => option.reductionRate === 30) ||
        category.options?.[0];
      if (defaultOption) next[key] = Number(defaultOption.reductionRate);
    }
    selectedRates.value = next;
  }

  function selectRate(categoryId, reductionRate) {
    if (hasStartedMissions.value) return;
    selectedRates.value = {
      ...selectedRates.value,
      [String(categoryId)]: Number(reductionRate),
    };
  }

  async function startMissions() {
    if (selectedCount.value === 0) {
      errorMessage.value = '시작할 미션을 한 개 이상 선택해 주세요.';
      return null;
    }

    submitting.value = true;
    errorMessage.value = '';
    try {
      const request = Object.entries(selectedRates.value).map(
        ([categoryId, reductionRate]) => ({
          categoryId: Number(categoryId),
          reductionRate: Number(reductionRate),
        }),
      );
      selections.value = await saveMissionSelections(
        analysisYearMonth.value,
        request,
      );
      missions.value = await createSavingMissions(targetYearMonth.value);
      return missions.value;
    } catch (error) {
      errorMessage.value = errorMessageOf(
        error,
        '저축 미션을 시작하지 못했어요.',
      );
      return null;
    } finally {
      submitting.value = false;
    }
  }

  return {
    analysisYearMonth,
    targetYearMonth,
    options,
    selections,
    missions,
    selectedRates,
    loading,
    submitting,
    errorMessage,
    hasStartedMissions,
    selectedCount,
    selectedOptions,
    expectedSavingAmount,
    load,
    toggleCategory,
    selectRate,
    startMissions,
  };
});
