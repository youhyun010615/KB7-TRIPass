import api from '@/api';

const unwrap = (response) => response.data.data;

export async function fetchTripCountries(keyword = '') {
  const response = await api.get('/trips/countries', {
    params: keyword ? { keyword } : undefined,
  });
  return unwrap(response);
}

export async function fetchMyTrips() {
  const response = await api.get('/trips');
  return unwrap(response);
}

export async function createTripGoal(payload) {
  const response = await api.post('/trips', payload);
  return unwrap(response);
}

export async function updateTripGoal(tripId, payload) {
  const response = await api.patch(`/trips/${tripId}`, payload);
  return unwrap(response);
}

export async function fetchActiveTripGoal() {
  const response = await api.get('/trips/active');
  return unwrap(response);
}

export async function fetchCurrentTripLifecycle() {
  const response = await api.get('/trips/current-lifecycle');
  return unwrap(response);
}

export async function acknowledgeTripOnboarding() {
  await api.post('/trips/onboarding/ack');
}

export async function resolveWalletReflect(tripId, reflect) {
  const response = await api.post(`/trips/${tripId}/wallet-reflect`, {
    reflect: Boolean(reflect),
  });
  return response.data?.data ?? null;
}

export async function archiveTrip(tripId) {
  const response = await api.post(`/trips/${tripId}/archive`);
  return unwrap(response);
}

export async function acknowledgeTripStartReport(tripId) {
  const response = await api.post(`/trips/${tripId}/start-report/acknowledge`);
  return unwrap(response);
}

export async function fetchActiveTripHome() {
  const response = await api.get('/trips/active/home');
  return unwrap(response);
}

export async function fetchTripGoal(tripId) {
  const response = await api.get(`/trips/${tripId}`);
  return unwrap(response);
}

export async function generateTripBudget(tripId) {
  const response = await api.post(`/trips/${tripId}/budget-recommendations`);
  return unwrap(response);
}

export async function fetchTripBudget(tripId) {
  const response = await api.get(`/trips/${tripId}/budget-recommendations`);
  return unwrap(response);
}

export async function confirmTripBudget(tripId, payload) {
  const response = await api.put(
    `/trips/${tripId}/budget-recommendations`,
    payload,
  );
  return unwrap(response);
}

export async function switchTravelMode(tripId, request) {
  const response = await api.patch(`/trips/${tripId}/start`, {
    isTravelMode: request,
  });
  return unwrap(response);
}

export async function fetchTripStatus(tripId, countryId) {
  const response = await api.get(`/trips/${tripId}/travel-status`, {
    params: countryId ? { countryId } : undefined,
  });
  return unwrap(response);
}

export async function fetchTripTransactions(tripId, countryId, categoryName) {
  const response = await api.get(`/trips/${tripId}/transactions`, {
    params: {
      countryId: countryId || undefined,
      categoryName: categoryName || undefined,
    },
  });
  return unwrap(response);
}

export async function fetchBudgetCheck(tripId) {
  const response = await api.get(`/trips/${tripId}/budget-check`);
  return unwrap(response);
}
