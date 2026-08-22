import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import {
  createSavingMissions,
  fetchMissionOptions,
  fetchMissionSelections,
  fetchSavingMissions,
  saveMissionSelections,
} from '@/api/savingMissions';
import {
  fetchMonthlyAnalysis,
  generateMonthlyAnalysis,
} from '@/api/monthlyAnalysis';

function isAnalysisNotFound(error) {
  return (
    error.response?.status === 404 ||
    error.response?.data?.code === 'MONTHLY_ANALYSIS_NOT_FOUND'
  );
}

// 해당 월 분석 리포트가 없으면(404) 카드 거래 데이터를 집계해 새로 생성한다.
// mission-options/mission-selections는 분석 리포트가 있어야 조회되므로
// 이 단계를 먼저 거쳐야 한다.
async function ensureAnalysisExists(yearMonth) {
  let analysis;
  try {
    analysis = await fetchMonthlyAnalysis(yearMonth);
  } catch (error) {
    if (!isAnalysisNotFound(error)) throw error;
    analysis = await generateMonthlyAnalysis(yearMonth);
  }
  return analysis;
}

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
  const missionStatusLoading = ref(false);
  const submitting = ref(false);
  const errorMessage = ref('');
  const addingMissions = ref(false);

  const hasStartedMissions = computed(
    () => Number(missions.value?.missionCount || 0) > 0,
  );
  const selectedCount = computed(
    () => Object.keys(selectedRates.value).length,
  );
  const startedCategoryIds = computed(
    () => new Set((missions.value?.missions || []).map((mission) => String(mission.categoryId))),
  );
  const newSelectedCount = computed(
    () => Object.keys(selectedRates.value).filter(
      (categoryId) => !startedCategoryIds.value.has(categoryId),
    ).length,
  );
  const canAddMissions = computed(
    () => options.value.some(
      (category) => !startedCategoryIds.value.has(String(category.categoryId)),
    ),
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
  // 추천 카테고리 중 이미 진행 중인 미션의 목표 절감액 합계.
  // (진행 중 미션은 selectedRates에 담기지 않으므로 실제 진행 중인 절감률로 옵션을 찾아 합산한다.)
  const startedSelectedSavingAmount = computed(() => {
    const startedMissionsByCategory = new Map(
      (missions.value?.missions || []).map((mission) => [String(mission.categoryId), mission]),
    );
    return options.value.reduce((sum, category) => {
      const startedMission = startedMissionsByCategory.get(String(category.categoryId));
      if (!startedMission) return sum;
      const matchedOption = category.options?.find(
        (option) => Number(option.reductionRate) === Number(startedMission.reductionRate),
      );
      return sum + Number(matchedOption?.monthlyReductionTarget || 0);
    }, 0);
  });
  const expectedSavingAmount = computed(() =>
    selectedOptions.value.reduce(
      (sum, item) =>
        sum + Number(item.selectedOption.monthlyReductionTarget || 0),
      0,
    ) + startedSelectedSavingAmount.value,
  );
  const newExpectedSavingAmount = computed(() =>
    selectedOptions.value
      .filter((item) => !startedCategoryIds.value.has(String(item.categoryId)))
      .reduce(
        (sum, item) => sum + Number(item.selectedOption.monthlyReductionTarget || 0),
        0,
      ) + startedSelectedSavingAmount.value,
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

    // 진행 중인 미션 현황은 핵심 데이터이므로 실패 시 에러 화면을 보여준다.
    try {
      missions.value = await fetchSavingMissions(targetYearMonth.value);
    } catch (error) {
      missions.value = null;
      loading.value = false;
      errorMessage.value = errorMessageOf(
        error,
        '저축 미션 정보를 불러오지 못했어요.',
      );
      return;
    }

    // 지난달 소비 분석/추천 데이터는 계좌 연동 직후처럼 아직 쌓이지 않았을 수 있어
    // 실패하더라도 위에서 이미 불러온 미션 현황(메인 대시보드)은 그대로 보여준다.
    try {
      await ensureAnalysisExists(yearMonth);
      const [optionData, selectionData] = await Promise.all([
        fetchMissionOptions(yearMonth),
        fetchMissionSelections(yearMonth),
      ]);
      options.value = optionData || [];
      selections.value = selectionData;
      applySelections(selectionData?.selections || []);
    } catch {
      options.value = [];
      selections.value = null;
      selectedRates.value = {};
      analysisYearMonth.value = '';
    }

    addingMissions.value = false;
    loading.value = false;
  }

  async function loadMissionStatus(yearMonth = previousYearMonth()) {
    analysisYearMonth.value = yearMonth;
    targetYearMonth.value = nextYearMonth(yearMonth);
    missionStatusLoading.value = true;

    try {
      missions.value = await fetchSavingMissions(targetYearMonth.value);
      return missions.value;
    } catch (error) {
      missions.value = null;
      return null;
    } finally {
      missionStatusLoading.value = false;
    }
  }

  function toggleCategory(category) {
    const key = String(category.categoryId);
    if (startedCategoryIds.value.has(key)) return;
    if (hasStartedMissions.value && !addingMissions.value) return;
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
    const key = String(categoryId);
    if (startedCategoryIds.value.has(key)) return;
    if (hasStartedMissions.value && !addingMissions.value) return;
    selectedRates.value = {
      ...selectedRates.value,
      [key]: Number(reductionRate),
    };
  }

  function beginAddingMissions() {
    if (!canAddMissions.value) return;
    errorMessage.value = '';
    addingMissions.value = true;
  }

  function cancelAddingMissions() {
    applySelections(selections.value?.selections || []);
    errorMessage.value = '';
    addingMissions.value = false;
  }

  async function startMissions() {
    const countToStart = hasStartedMissions.value ? newSelectedCount.value : selectedCount.value;
    if (countToStart === 0) {
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
      addingMissions.value = false;
      return missions.value;
    } catch (error) {
      const code = error.response?.data?.code || error.response?.data?.errorCode;
      const guardedMessages = {
        TRIP_REQUIRED_FOR_MISSION: '여행 계획을 등록해야 미션을 진행할 수 있어요',
        ACCOUNT_REQUIRED_FOR_MISSION: '계좌를 등록해야 미션을 진행할 수 있어요',
        MISSION_REPORT_NOT_VIEWED: '이번 달 분석 리포트를 먼저 확인해 주세요',
      };
      errorMessage.value = guardedMessages[code] || errorMessageOf(
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
    missionStatusLoading,
    submitting,
    errorMessage,
    addingMissions,
    hasStartedMissions,
    selectedCount,
    startedCategoryIds,
    newSelectedCount,
    canAddMissions,
    selectedOptions,
    expectedSavingAmount,
    newExpectedSavingAmount,
    load,
    loadMissionStatus,
    toggleCategory,
    selectRate,
    beginAddingMissions,
    cancelAddingMissions,
    startMissions,
  };
});
