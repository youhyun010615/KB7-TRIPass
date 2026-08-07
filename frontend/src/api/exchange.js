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
