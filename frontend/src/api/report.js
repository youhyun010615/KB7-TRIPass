import api from '@/api';

/**
 * 1. 여행 대비 리포트 조회
 */
export const fetchPreTripReport = async (tripId) => {
  const response = await api.get(`/trips/${tripId}/report/pre-trip`);
  return response.data.data;
};

/**
 * 2. 여행 후 리포트 조회
 */
export const fetchPostTripReport = async (tripId) => {
  const response = await api.get(`/trips/${tripId}/report/post-trip`);
  return response.data.data;
};
