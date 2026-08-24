import api from '@/api'
import { demoTravelScheduleDetail, demoTravelSchedules, isLay1217Demo } from '@/mocks/lay1217TravelDemo'

const dataOf = response => response.data?.data

export async function fetchSchedules(tripId) {
  if (isLay1217Demo()) return demoTravelSchedules()
  return dataOf(await api.get(`/trips/${tripId}/schedules`))
}

export async function fetchScheduleDetail(tripId, scheduleId) {
  if (isLay1217Demo()) return demoTravelScheduleDetail(scheduleId)
  return dataOf(await api.get(`/trips/${tripId}/schedules/${scheduleId}`))
}

export async function createSchedule(tripId, payload) {
  return dataOf(await api.post(`/trips/${tripId}/schedules`, payload))
}

export async function updateSchedule(tripId, scheduleId, payload) {
  return dataOf(await api.put(`/trips/${tripId}/schedules/${scheduleId}`, payload))
}

export async function deleteSchedule(tripId, scheduleId) {
  return dataOf(await api.delete(`/trips/${tripId}/schedules/${scheduleId}`))
}
