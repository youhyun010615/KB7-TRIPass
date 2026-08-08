import api from '@/api';

export const fetchExchangeRates = async () => {
  const response = await api.get('/exchange-rates');
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

  console.log('은행 영업점 상세 조회: ', response.data);
  return response.data.data;
};
