import api from '@/api';

/**
 * 1. 여행 체크리스트 전체 현황 요약 조회 (대시보드 메인 카드용)
 */
export const fetchChecklistSummary = async (tripId) => {
  const response = await api.get(`/trips/${tripId}/checklists/summary`);
  return response.data.data;
};

/**
 * 2. 단계별 체크리스트 상세 목록 조회
 */
export const fetchChecklists = async (tripId, type, ddayStage) => {
  const response = await api.get(`/trips/${tripId}/checklists`, {
    params: { type, ddayStage },
  });
  return response.data.data;
};

/**
 * 3. 체크리스트 항목 완료 상태 토글/수정
 */
export const toggleChecklistItem = async (tripId, itemId, isCompleted) => {
  const response = await api.patch(`/trips/${tripId}/checklists/${itemId}`, {
    isCompleted,
  });
  return response.data.data;
};

/**
 * 4. 커스텀 체크리스트 항목 추가
 */
export const createChecklistItem = async (tripId, itemData) => {
  const response = await api.post(`/trips/${tripId}/checklists`, itemData);
  return response.data.data;
};

/**
 * 5. 커스텀 체크리스트 항목 삭제
 */
export const deleteChecklistItem = async (tripId, itemId) => {
  const response = await api.delete(`/trips/${tripId}/checklists/${itemId}`);
  return response.data.data;
};