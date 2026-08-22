import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useTravelStore } from '@/stores/travel'
import { createSchedule, deleteSchedule, fetchScheduleDetail, fetchSchedules, updateSchedule } from '@/api/schedule'

const STORAGE_KEY = 'tripass-travel-schedules-v2'
const countries = [
  { code: 'FR', name: '프랑스', city: '파리', flag: '🇫🇷', currency: 'EUR', defaultStart: '2026-08-15', defaultEnd: '2026-08-22' },
  { code: 'CH', name: '스위스', city: '인터라켄', flag: '🇨🇭', currency: 'CHF', defaultStart: '2026-08-23', defaultEnd: '2026-08-26' },
  { code: 'DE', name: '독일', city: '베를린', flag: '🇩🇪', currency: 'EUR', defaultStart: '2026-08-27', defaultEnd: '2026-08-30' },
  { code: 'JP', name: '일본', city: '도쿄', flag: '🇯🇵', currency: 'JPY', defaultStart: '2026-09-01', defaultEnd: '2026-09-05' },
  { code: 'HK', name: '홍콩', city: '홍콩', flag: '🇭🇰', currency: 'HKD', defaultStart: '2026-09-06', defaultEnd: '2026-09-10' },
]

function loadSaved() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') }
  catch { return null }
}

// 백엔드는 국가를 country_name(한글)으로만 구분한다. 화면은 code(FR/CH/...)로 다루므로
// 응답의 countryName을 우리 쪽 code로 되돌려 매핑한다.
function codeForCountryName(name) {
  return countries.find(item => item.name === name)?.code || ''
}

function toLocalPaymentStatus(apiStatus) {
  return String(apiStatus || 'UNDECIDED').toLowerCase()
}

function toApiPaymentStatus(localStatus) {
  return String(localStatus || 'undecided').toUpperCase()
}

// API의 scheduledAt(현지 시간, 오프셋 포함 가능)을 화면에서 쓰는 date/time 문자열로 쪼갠다.
function splitScheduledAt(scheduledAt) {
  const [date, rest] = String(scheduledAt || '').split('T')
  const time = (rest || '00:00:00').slice(0, 5)
  return { date: date || '', time: time || '00:00' }
}

// 그 날짜가 속한 시간대(여행 국가 현지, 없으면 한국)의 "오늘"과 같은 날짜인지 비교하기 위한 함수.
// new Date().toISOString()은 항상 UTC라서 한국 자정~오전 9시 사이엔 실제 날짜보다 하루 이전으로 계산되는
// 문제가 있었다. Intl.DateTimeFormat으로 해당 시간대의 실제 달력 날짜를 구한다.
function todayInZone(timeZone) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: timeZone || 'Asia/Seoul' }).format(new Date())
}

function normalizeApiSchedule(row) {
  const { date, time } = splitScheduledAt(row.scheduledAt)
  return {
    id: row.id,
    tripCountryId: row.tripCountryId,
    countryName: row.countryName || '',
    countryCode: codeForCountryName(row.countryName),
    timeZone: row.timeZone || '',
    title: row.scheduleName,
    date,
    time,
    currency: row.currencyCode || '',
    amount: Number(row.amount || 0),
    paymentStatus: toLocalPaymentStatus(row.paymentStatus),
    scheduleStatus: row.scheduleStatus || 'UPCOMING',
    completed: row.scheduleStatus === 'DONE',
    placeName: row.placeName || '',
    placeAddress: row.placeAddress || '',
    memo: row.memo || '',
    notificationTriggered: false,
    notificationRead: false,
  }
}

export const useTravelScheduleStore = defineStore('travelSchedule', () => {
  const travel = useTravelStore()
  const saved = loadSaved()

  function period(code) {
    const country = countries.find(item => item.code === code)
    const plan = travel.selectedPlans.find(item => item.code === code)
    return { startDate: plan?.startDate || country.defaultStart, endDate: plan?.endDate || country.defaultEnd }
  }

  // 서버에서 다시 불러오기 전까지 이전 화면 데이터로 먼저 그려서 빈 화면이 보이지 않게 한다.
  const schedules = ref(saved ?? [])
  const isLoading = ref(false)
  const errorMessage = ref('')

  const sortedSchedules = computed(() => [...schedules.value].sort((a, b) => `${a.date}${a.time}`.localeCompare(`${b.date}${b.time}`)))
  const configuredPeriods = computed(() => travel.selectedPlans
    .filter(plan => plan?.startDate && plan?.endDate)
    .map(plan => ({ code: plan.code, startDate: plan.startDate, endDate: plan.endDate })))
  const displayPeriods = computed(() => configuredPeriods.value.length
    ? configuredPeriods.value
    : countries.map(item => ({ code: item.code, ...period(item.code) })))
  const travelStart = computed(() => displayPeriods.value.map(item => item.startDate).sort()[0])
  const travelEnd = computed(() => displayPeriods.value.map(item => item.endDate).sort().at(-1))
  // 화면 상단 라벨 표시용(한국 기준 오늘). "오늘 일정" 여부 판단은 일정마다 다른 여행 국가
  // 시간대를 써야 해서 todayFor(schedule.timeZone)로 개별 비교한다.
  const today = computed(() => todayInZone('Asia/Seoul'))
  const todayFor = timeZone => todayInZone(timeZone)

  function countryForDate(date) {
    const matchedPeriod = displayPeriods.value.find(item => date >= item.startDate && date <= item.endDate)
    return countries.find(country => country.code === matchedPeriod?.code)
  }
  function normalizeSchedule(item) {
    if (!item) return item
    return { ...item, placeName: item.placeName || item.place || '', placeAddress: item.placeAddress || '', notificationTriggered: Boolean(item.notificationTriggered) }
  }
  function getSchedule(id) { return normalizeSchedule(schedules.value.find(item => item.id === Number(id))) }

  // 여행일정 페이지(목록/상세/등록)로 직접 들어오거나 새로고침하면 홈을 거치지 않아
  // travel.selectedPlans(국가별 tripCountryId)가 아직 비어있을 수 있다.
  // loadActiveGoal이 selectedPlans를 채우는 유일한 함수이므로 이것으로 부트스트랩한다.
  // tripId만으로 판단하면 안 된다 — 저축모드 홈의 loadHomeDashboard가 먼저 실행돼 tripId만
  // 채워놓고 selectedPlans는 비워둔 채로 남아있는 경우가 있어(두 로더가 동시에 실행되는 레이스),
  // selectedPlans 자체가 비어있는지로 판단해야 loadActiveGoal이 확실히 실행된다.
  async function ensureTripLoaded() {
    if (travel.selectedPlans.length) return
    // loadActiveGoal은 travel.initialized가 이미 true면(홈의 다른 로더가 먼저 실행됐을 때도
    // 마찬가지) 아무 것도 안 하고 캐시된 값을 그대로 반환한다. selectedPlans가 비어있다고
    // 이미 확인했으니 force로 실제 조회를 강제해야 한다.
    await travel.loadActiveGoal({ force: true }).catch(() => {})
  }

  async function loadSchedules(tripIdOverride = null) {
    if (!tripIdOverride) await ensureTripLoaded()
    const targetTripId = Number(tripIdOverride || travel.tripId)
    if (!targetTripId) {
      schedules.value = []
      return schedules.value
    }

    isLoading.value = true
    errorMessage.value = ''
    try {
      const rows = await fetchSchedules(targetTripId)
      schedules.value = (rows ?? []).map(normalizeApiSchedule)
      return schedules.value
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '여행 일정을 불러오지 못했어요.'
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 목록 조회 API(findAllByTripId)는 memo를 내려주지 않는다(ScheduleListResponseDto에 필드 자체가 없음).
  // 상세 화면에서는 이 함수로 상세 조회 API를 한 번 더 불러 memo를 포함한 전체 정보로 덮어써야 한다.
  async function loadScheduleDetail(id, tripIdOverride = null) {
    await ensureTripLoaded()
    const targetTripId = Number(tripIdOverride || travel.tripId)
    if (!targetTripId) return null

    errorMessage.value = ''
    try {
      const row = await fetchScheduleDetail(targetTripId, Number(id))
      const normalized = normalizeApiSchedule(row)
      const index = schedules.value.findIndex(item => item.id === normalized.id)
      if (index >= 0) {
        const existing = schedules.value[index]
        schedules.value[index] = {
          ...existing,
          ...normalized,
          countryCode: normalized.countryCode || existing.countryCode || '',
          countryName: normalized.countryName || existing.countryName || '',
          tripCountryId: normalized.tripCountryId || existing.tripCountryId,
        }
      }
      else schedules.value.push(normalized)
      return getSchedule(normalized.id)
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '여행 일정 상세 정보를 불러오지 못했어요.'
      throw error
    }
  }

  // save/update/remove 모두 성공 시 서버 목록을 다시 받아와 화면을 최신 상태로 맞춘다.
  // (국가명·시간대처럼 서버에서만 계산되는 값이 있어, 낙관적으로 로컬 배열만 고치지 않는다.)
  function tripCountryIdFor(countryCode) {
    return travel.selectedPlans.find(item => item.code === countryCode)?.tripCountryId
  }

  function toSchedulePayload(payload) {
    return {
      tripCountryId: tripCountryIdFor(payload.countryCode),
      scheduleName: payload.title,
      scheduledAt: `${payload.date}T${payload.time}:00`,
      amount: Number(payload.amount) || 0,
      currencyCode: payload.currency || undefined,
      paymentStatus: toApiPaymentStatus(payload.paymentStatus),
      placeName: payload.placeName,
      placeAddress: payload.placeAddress,
      memo: payload.memo,
    }
  }

  async function save(payload) {
    const tripCountryId = tripCountryIdFor(payload.countryCode)
    if (!travel.tripId || !tripCountryId) {
      errorMessage.value = '등록된 여행 국가가 아니에요. 여행 목표에서 국가를 먼저 추가해 주세요.'
      return false
    }

    try {
      await createSchedule(travel.tripId, toSchedulePayload(payload))
      await loadSchedules()
      return true
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '여행 일정 등록에 실패했어요.'
      return false
    }
  }

  async function update(id, payload) {
    const tripCountryId = tripCountryIdFor(payload.countryCode)
    if (!travel.tripId || !tripCountryId) {
      errorMessage.value = '등록된 여행 국가가 아니에요. 여행 목표에서 국가를 먼저 추가해 주세요.'
      return false
    }

    const current = getSchedule(id)

    try {
      await updateSchedule(travel.tripId, Number(id), {
        ...toSchedulePayload(payload),
        scheduleStatus: current?.scheduleStatus || 'UPCOMING',
      })
      await loadSchedules()
      return true
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '여행 일정 수정에 실패했어요.'
      return false
    }
  }

  async function remove(id) {
    if (!travel.tripId) return false

    try {
      await deleteSchedule(travel.tripId, Number(id))
      await loadSchedules()
      return true
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '여행 일정 삭제에 실패했어요.'
      return false
    }
  }

  async function toggleComplete(id) {
    const schedule = getSchedule(id)
    if (!schedule || !travel.tripId) return false

    const nextStatus = schedule.scheduleStatus === 'DONE' ? 'UPCOMING' : 'DONE'

    try {
      await updateSchedule(travel.tripId, Number(id), {
        ...toSchedulePayload(schedule),
        scheduleStatus: nextStatus,
      })
      await loadSchedules()
      return true
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '여행 일정 상태 변경에 실패했어요.'
      return false
    }
  }

  // 일정 알림 읽음 표시는 서버 개념이 아닌 화면 전용 상태라 로컬에서만 관리한다.
  function markNotificationRead(id) {
    const schedule = schedules.value.find(item => item.id === Number(id))
    if (schedule) schedule.notificationRead = true
  }

  watch(schedules, value => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)), { deep: true })

  return {
    countries,
    schedules,
    isLoading,
    errorMessage,
    sortedSchedules,
    configuredPeriods,
    travelStart,
    travelEnd,
    todayFor,
    today,
    period,
    countryForDate,
    getSchedule,
    ensureTripLoaded,
    loadSchedules,
    loadScheduleDetail,
    save,
    update,
    remove,
    toggleComplete,
    markNotificationRead,
    normalizeSchedule,
  }
})
