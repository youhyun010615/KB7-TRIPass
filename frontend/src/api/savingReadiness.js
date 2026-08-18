import api from '@/api';

const unwrap = (response) => response.data.data;

/** 여행 목표·금융 자산 연동 상태를 기준으로 저축 미션 준비 상태를 조회한다. */
export async function fetchSavingReadiness() {
  const response = await api.get('/saving/readiness');
  return unwrap(response);
}
