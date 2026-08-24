import api from '@/api';
import { demoTransactions, isLay1217Demo, overlayBudgetCheck, overlayTravelStatus } from '@/mocks/lay1217TravelDemo';
import { todayIso } from '@/utils/devDate';

const unwrap = (response) => response.data.data;
const DEMO_TRIP_NAME = '유럽 3개국 여행';
const DEMO_REPORT_RESET_VERSION = 'budget-without-emergency-v3';

function canonicalDemoTrips(trips = []) {
  if (!isLay1217Demo()) return trips;
  const matches = trips
    .filter((trip) => trip.tripName === DEMO_TRIP_NAME)
    .sort((a, b) => Number(a.tripId) - Number(b.tripId));
  return matches.length ? [matches[0]] : [];
}

function isDemoJapanTrip(trip = {}) {
  if (String(trip.tripName || '').includes('일본')) return true;
  return (trip.countries || []).some((country) =>
    String(country.countryName || country.name || '').includes('일본'));
}

async function removeDemoJapanTrips(trips = []) {
  if (!isLay1217Demo()) return trips;
  const japanTrips = trips.filter(isDemoJapanTrip);
  if (japanTrips.length) {
    await Promise.allSettled(japanTrips.map((trip) => api.delete(`/trips/${trip.tripId}`)));
  }
  return trips.filter((trip) => !isDemoJapanTrip(trip));
}

function demoStartReportKey(tripId) {
  return `tripass-demo-start-report:${DEMO_REPORT_RESET_VERSION}:${tripId}`;
}

function demoArchivedTripKey(tripId) {
  return `tripass-demo-archived:${DEMO_REPORT_RESET_VERSION}:${tripId}`;
}

export async function fetchTripCountries(keyword = '') {
  const response = await api.get('/trips/countries', {
    params: keyword ? { keyword } : undefined,
  });
  return unwrap(response);
}

export async function fetchMyTrips() {
  const response = await api.get('/trips');
  return canonicalDemoTrips(await removeDemoJapanTrips(unwrap(response)));
}

export async function createTripGoal(payload) {
  const response = await api.post('/trips', payload);
  return unwrap(response);
}

export async function updateTripGoal(tripId, payload) {
  const response = await api.patch(`/trips/${tripId}`, payload);
  return unwrap(response);
}

export async function deleteTrip(tripId) {
  await api.delete(`/trips/${tripId}`);
}

export async function fetchActiveTripGoal() {
  const response = await api.get('/trips/active');
  return unwrap(response);
}

export async function fetchCurrentTripLifecycle() {
  const response = await api.get('/trips/current-lifecycle');
  const data = unwrap(response);
  if (!isLay1217Demo()) return data;

  const tripsResponse = await api.get('/trips');
  const demoTrips = await removeDemoJapanTrips(unwrap(tripsResponse));
  const demoTrip = canonicalDemoTrips(demoTrips)[0];
  if (!demoTrip) return data;
  if (localStorage.getItem(demoArchivedTripKey(demoTrip.tripId)) === 'true') {
    return {
      ...data,
      tripId: null,
      lifecycle: 'ARCHIVED',
      status: 'ARCHIVED',
      hasTrip: false,
      travelModeAvailable: false,
      missionAvailable: false,
      startReportAvailable: false,
      endingReviewRequired: false,
    };
  }

  const today = todayIso();
  const lifecycle = today < demoTrip.startDate
    ? 'PREPARING'
    : today <= demoTrip.endDate ? 'TRAVELING' : 'REVIEW';
  return {
    ...data,
    tripId: demoTrip.tripId,
    tripName: demoTrip.tripName,
    status: lifecycle === 'PREPARING' ? 'PLANNING' : lifecycle === 'TRAVELING' ? 'TRAVELING' : 'ENDED',
    lifecycle,
    startDate: demoTrip.startDate,
    endDate: demoTrip.endDate,
    hasTrip: true,
    travelModeAvailable: lifecycle === 'TRAVELING',
    missionAvailable: lifecycle === 'PREPARING',
    startReportAvailable: lifecycle === 'TRAVELING',
    startReportAcknowledged: localStorage.getItem(demoStartReportKey(demoTrip.tripId)) === 'true',
    endingReviewRequired: lifecycle === 'REVIEW',
  };
}

export async function acknowledgeTripOnboarding() {
  await api.post('/trips/onboarding/ack');
}

export async function resolveWalletReflect(tripId, reflect, targetAccountId = null) {
  const response = await api.post(`/trips/${tripId}/wallet-reflect`, {
    reflect: Boolean(reflect),
    ...(targetAccountId ? { targetAccountId } : {}),
  });
  return response.data?.data ?? null;
}

export async function archiveTrip(tripId) {
  const response = await api.post(`/trips/${tripId}/archive`);
  if (isLay1217Demo()) localStorage.setItem(demoArchivedTripKey(tripId), 'true');
  return unwrap(response);
}

export async function acknowledgeTripStartReport(tripId) {
  const response = await api.post(`/trips/${tripId}/start-report/acknowledge`);
  if (isLay1217Demo()) localStorage.setItem(demoStartReportKey(tripId), 'true');
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
  const data = unwrap(response);
  return isLay1217Demo() ? overlayTravelStatus(data, countryId) : data;
}

export async function fetchTripTransactions(tripId, countryId, categoryName) {
  const response = await api.get(`/trips/${tripId}/transactions`, {
    params: {
      countryId: countryId || undefined,
      categoryName: categoryName || undefined,
    },
  });
  const data = unwrap(response);
  return isLay1217Demo() ? demoTransactions(data, countryId, categoryName) : data;
}

export async function fetchBudgetCheck(tripId) {
  const response = await api.get(`/trips/${tripId}/budget-check`);
  const data = unwrap(response);
  return isLay1217Demo() ? overlayBudgetCheck(data) : data;
}
