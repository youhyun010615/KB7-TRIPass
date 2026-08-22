import { readonly, ref } from 'vue'
import api from '@/api'

const effectiveDateRef = ref('')
const realDateRef = ref('')
const overriddenRef = ref(false)
const initializedRef = ref(false)

let initializationPromise = null

function unwrapResponse(response) {
  return response?.data?.data ?? response?.data ?? {}
}

function parseLocalDate(value) {
  if (!value) return null

  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number)
  if (![year, month, day].every(Number.isFinite)) return null

  const date = new Date(year, month - 1, day)
  if (
    date.getFullYear() !== year
    || date.getMonth() !== month - 1
    || date.getDate() !== day
  ) return null

  return date
}

export async function initDevDate() {
  if (initializationPromise) return initializationPromise

  initializationPromise = (async () => {
    try {
      const response = await api.get('/dev/current-date')
      const data = unwrapResponse(response)

      effectiveDateRef.value = data.effectiveDate || data.overrideDate || ''
      realDateRef.value = data.realDate || ''
      overriddenRef.value = Boolean(data.isOverridden && effectiveDateRef.value)

      return data
    } catch {
      // 개발 날짜 API를 사용할 수 없는 환경에서도 앱은 실제 날짜 기준으로 동작한다.
      effectiveDateRef.value = ''
      realDateRef.value = ''
      overriddenRef.value = false
      return null
    } finally {
      initializedRef.value = true
      initializationPromise = null
    }
  })()

  return initializationPromise
}

export function today() {
  const overriddenDate = overriddenRef.value
    ? parseLocalDate(effectiveDateRef.value)
    : null
  const date = overriddenDate || new Date()

  date.setHours(0, 0, 0, 0)
  return date
}

export function now() {
  const date = today()

  if (overriddenRef.value) {
    const realNow = new Date()
    date.setHours(
      realNow.getHours(),
      realNow.getMinutes(),
      realNow.getSeconds(),
      realNow.getMilliseconds(),
    )
  }

  return date
}

export function todayIso() {
  const date = today()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function isDevDateInitialized() {
  return initializedRef.value
}

export const effectiveDate = readonly(effectiveDateRef)
export const realDate = readonly(realDateRef)
export const isOverridden = readonly(overriddenRef)
export const devDateInitialized = readonly(initializedRef)
