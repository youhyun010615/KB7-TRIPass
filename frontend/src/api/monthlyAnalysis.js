import api from '@/api';

const unwrap = (response) => response.data.data;

/** 저장된 월간 AI 소비 분석 리포트를 조회한다. */
export async function fetchMonthlyAnalysis(yearMonth) {
  const response = await api.get(`/saving/analyses/${yearMonth}`);
  return unwrap(response);
}

/** 처음 상세 화면을 연 리포트를 확인 상태로 변경한다. */
export async function markMonthlyAnalysisViewed(yearMonth) {
  await api.patch(`/saving/analyses/${yearMonth}/view`);
}

/** 사용자가 리포트를 닫으면 다음 리포트 생성 전까지 노출하지 않는다. */
export async function closeMonthlyAnalysis(yearMonth) {
  await api.patch(`/saving/analyses/${yearMonth}/close`);
}
