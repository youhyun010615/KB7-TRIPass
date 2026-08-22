import { today as currentDate } from '@/utils/devDate'

function parseDateOnly(value, endOfDay = false) {
  if (!value) return null

  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number)
  if (![year, month, day].every(Number.isFinite)) return null

  const date = new Date(year, month - 1, day)
  if (
    date.getFullYear() !== year
    || date.getMonth() !== month - 1
    || date.getDate() !== day
  ) return null

  date.setHours(endOfDay ? 23 : 0, endOfDay ? 59 : 0, endOfDay ? 59 : 0, endOfDay ? 999 : 0)
  return date
}

/**
 * 서버의 저장 status는 가상 날짜를 과거로 되돌릴 때 즉시 복원되지 않을 수 있다.
 * 화면에서는 여행 기간과 현재 가상 날짜를 기준으로 상태를 일관되게 계산한다.
 */
export function tripPhase(trip, referenceDate = currentDate()) {
  const start = parseDateOnly(trip?.startDate)
  const end = parseDateOnly(trip?.endDate, true)

  if (start && referenceDate < start) return 'PLANNING'
  if (end && referenceDate > end) return 'ENDED'
  if (start && referenceDate >= start && (!end || referenceDate <= end)) return 'TRAVELING'

  if (trip?.status === 'ENDED') return 'ENDED'
  if (trip?.status === 'TRAVELING') return 'TRAVELING'
  return 'PLANNING'
}

export function daysUntilTrip(trip, referenceDate = currentDate()) {
  const start = parseDateOnly(trip?.startDate)
  if (!start) return null
  return Math.ceil((start - referenceDate) / 86400000)
}
