import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import {
  closeMonthlyAnalysis,
  fetchMonthlyAnalysis,
  markMonthlyAnalysisViewed,
} from '@/api/monthlyAnalysis';

function previousYearMonth(baseDate = new Date()) {
  const previousMonth = new Date(
    baseDate.getFullYear(),
    baseDate.getMonth() - 1,
    1,
  );
  return `${previousMonth.getFullYear()}-${String(previousMonth.getMonth() + 1).padStart(2, '0')}`;
}

function apiErrorMessage(error, fallback) {
  return error.response?.data?.message || fallback;
}

export const useMonthlyAnalysisStore = defineStore('monthlyAnalysis', () => {
  const report = ref(null);
  const selectedYearMonth = ref('');
  const loading = ref(false);
  const updatingStatus = ref(false);
  const notFound = ref(false);
  const errorMessage = ref('');

  const reportStatus = computed(() => report.value?.reportStatus || '');
  const isClosed = computed(() => reportStatus.value === 'CLOSED');
  const hasVisibleReport = computed(
    () => Boolean(report.value) && !isClosed.value,
  );

  function resetAnalysis() {
    report.value = null;
    selectedYearMonth.value = '';
    loading.value = false;
    updatingStatus.value = false;
    notFound.value = false;
    errorMessage.value = '';
  }

  async function loadAnalysis(yearMonth, { force = false } = {}) {
    if (!yearMonth) return null;
    if (
      !force &&
      selectedYearMonth.value === yearMonth &&
      (report.value || notFound.value)
    ) {
      return report.value;
    }

    loading.value = true;
    errorMessage.value = '';
    notFound.value = false;
    selectedYearMonth.value = yearMonth;

    try {
      report.value = await fetchMonthlyAnalysis(yearMonth);
      return report.value;
    } catch (error) {
      if (
        error.response?.status === 404 ||
        error.response?.data?.code === 'MONTHLY_ANALYSIS_NOT_FOUND'
      ) {
        report.value = null;
        notFound.value = true;
        return null;
      }

      report.value = null;
      errorMessage.value = apiErrorMessage(
        error,
        '월간 분석 리포트를 불러오지 못했어요.',
      );
      return null;
    } finally {
      loading.value = false;
    }
  }

  function loadLatestAnalysis(options) {
    return loadAnalysis(previousYearMonth(), options);
  }

  async function markViewed() {
    if (!report.value || report.value.reportStatus !== 'PENDING') {
      return report.value;
    }

    updatingStatus.value = true;
    errorMessage.value = '';
    try {
      await markMonthlyAnalysisViewed(report.value.analysisYearMonth);
      report.value = { ...report.value, reportStatus: 'VIEWED' };
      return report.value;
    } catch (error) {
      errorMessage.value = apiErrorMessage(
        error,
        '리포트 확인 상태를 저장하지 못했어요.',
      );
      throw error;
    } finally {
      updatingStatus.value = false;
    }
  }

  async function closeReport() {
    if (!report.value || report.value.reportStatus === 'CLOSED') {
      return report.value;
    }

    updatingStatus.value = true;
    errorMessage.value = '';
    try {
      await closeMonthlyAnalysis(report.value.analysisYearMonth);
      report.value = { ...report.value, reportStatus: 'CLOSED' };
      return report.value;
    } catch (error) {
      errorMessage.value = apiErrorMessage(
        error,
        '리포트를 닫지 못했어요. 잠시 후 다시 시도해 주세요.',
      );
      throw error;
    } finally {
      updatingStatus.value = false;
    }
  }

  return {
    report,
    selectedYearMonth,
    loading,
    updatingStatus,
    notFound,
    errorMessage,
    reportStatus,
    isClosed,
    hasVisibleReport,
    resetAnalysis,
    loadAnalysis,
    loadLatestAnalysis,
    markViewed,
    closeReport,
  };
});
