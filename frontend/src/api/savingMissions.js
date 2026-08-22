import api from '@/api';

const unwrap = (response) => response.data.data;

/** 월간 분석 TOP 3 카테고리의 10/30/50% 절감 옵션을 조회한다. */
export async function fetchMissionOptions(analysisYearMonth) {
  const response = await api.get(
    `/saving/analyses/${analysisYearMonth}/mission-options`,
  );
  return unwrap(response);
}

/** 월간 분석에서 사용자가 저장한 카테고리별 절감률을 조회한다. */
export async function fetchMissionSelections(analysisYearMonth) {
  const response = await api.get(
    `/saving/analyses/${analysisYearMonth}/mission-selections`,
  );
  return unwrap(response);
}

/** 카테고리별 절감률 선택을 전체 교체 방식으로 저장한다. */
export async function saveMissionSelections(analysisYearMonth, selections) {
  const response = await api.put(
    `/saving/analyses/${analysisYearMonth}/mission-selections`,
    { selections },
  );
  return unwrap(response);
}

/** 저장한 절감률을 기준으로 월간·주간 미션을 생성한다. */
export async function createSavingMissions(targetYearMonth) {
  const response = await api.post(`/saving/missions/${targetYearMonth}`);
  return unwrap(response);
}

/** 적용월의 월간·주간 미션과 판정 상태를 조회한다. */
export async function fetchSavingMissions(targetYearMonth) {
  const response = await api.get(`/saving/missions/${targetYearMonth}`);
  return unwrap(response);
}
