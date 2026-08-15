import api from '@/api'

const dataOf = response => response.data?.data

export async function createTrip(payload) {
  return dataOf(await api.post('/trips', payload))
}

export async function fetchCurrentTrip() {
  return dataOf(await api.get('/trips/current'))
}
