import api from '@/api';

// 통화 단위(EUR 하나로 뭉뚱그려짐) 대신 국가 단위로 환율을 내려주는 신규 API.
// 응답 각 항목에 countryId/countryName/flagUrl이 포함돼 EUR을 쓰는 국가들도 각각 구분된다.
export const fetchExchangeRates = async () => {
  const response = await api.get('/exchange-rates/countries');
  return response.data.data;
};

export const fetchExchangeRatesHistory = async (currencyCode, days) => {
  const response = await api.get('/exchange-rates/history', {
    params: { currencyCode, days },
  });
  return response.data.data;
};

export const fetchNearbyBanks = async (latitude, longitude, radius) => {
  const response = await api.get('/banks', {
    params: { latitude, longitude, radius },
  });
  return response.data.data;
};

export const fetchBankDetail = async (bankId) => {
  const response = await api.get(`/banks/${bankId}`);
  return response.data.data;
};

export const fetchExchangeEstimate = async (amount, currencyCode) => {
  const response = await api.get('/banks/estimate', {
    params: { amount, currencyCode },
  });
  return response.data.data;
};

export const getExchangeAlerts = async () => {
  const response = await api.get('/exchange-rates/alerts');
  return response.data.data;
};

export const registerExchangeAlert = async (alertData) => {
  const response = await api.post('/exchange-rates/alerts', alertData);
  return response.data.data;
};

export const updateExchangeAlert = async (id, alertData) => {
  const response = await api.put(`/exchange-rates/alerts/${id}`, alertData);
  return response.data.data;
};

export const deleteExchangeAlert = async (id) => {
  const response = await api.delete(`/exchange-rates/alerts/${id}`);
  return response.data.data;
};
