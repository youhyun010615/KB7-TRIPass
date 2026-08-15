import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useTravelStore } from '@/stores/travel'

const STORAGE_KEY = 'tripass-travel-schedules-v2'
const countries = [
  { code: 'FR', name: '프랑스', city: '파리', flag: '🇫🇷', currency: 'EUR', defaultStart: '2026-08-15', defaultEnd: '2026-08-22' },
  { code: 'CH', name: '스위스', city: '인터라켄', flag: '🇨🇭', currency: 'CHF', defaultStart: '2026-08-23', defaultEnd: '2026-08-26' },
  { code: 'DE', name: '독일', city: '베를린', flag: '🇩🇪', currency: 'EUR', defaultStart: '2026-08-27', defaultEnd: '2026-08-30' },
  { code: 'JP', name: '일본', city: '도쿄', flag: '🇯🇵', currency: 'JPY', defaultStart: '2026-09-01', defaultEnd: '2026-09-05' },
  { code: 'HK', name: '홍콩', city: '홍콩', flag: '🇭🇰', currency: 'HKD', defaultStart: '2026-09-06', defaultEnd: '2026-09-10' },
]

const seedTemplates = {
  FR: [
    ['루브르 박물관 가이드 투어', '10:30', 85, 'prepaid', 'Musée du Louvre, 75001 Paris, France', '1시간 전', '리슐리외관 입구에서 가이드 미팅'],
    ['파리 → 인터라켄 TGV 열차', '14:00', 65, 'prepaid', 'Paris Gare de Lyon', '2시간 전', '좌석 번호와 플랫폼 확인'],
    ['센강 유람선 야경 투어', '19:30', 42, 'undecided', 'Port de la Conférence, Paris, France', '1시간 전', '탑승 20분 전 선착장 도착'],
  ],
  CH: [
    ['체르마트 마터호른 샬레 숙소', '15:00', 220, 'onsite', 'Zermatt, Switzerland', '1일 전', '체크인 여권 준비'],
    ['마터호른 글레이셔 파라다이스', '09:00', 95, 'prepaid', 'Zermatt Bergbahnen', '1시간 전', '방한복 준비'],
  ],
  DE: [['브란덴부르크 문 워킹 투어', '10:00', 35, 'prepaid', 'Pariser Platz, Berlin', '1시간 전', '이어폰 준비']],
  JP: [['도쿄 스카이트리 전망대', '18:00', 2500, 'prepaid', 'Tokyo Skytree', '2시간 전', 'QR 티켓 확인']],
  HK: [['빅토리아 피크 야경 투어', '18:30', 320, 'onsite', 'The Peak, Hong Kong', '1시간 전', '옥토퍼스 카드 준비']],
}

function loadSaved() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') }
  catch { return null }
}

function dateWithOffset(start, offset, end) {
  const date = new Date(`${start}T00:00:00`)
  date.setDate(date.getDate() + offset)
  const result = date.toISOString().slice(0, 10)
  return result > end ? end : result
}

export const useTravelScheduleStore = defineStore('travelSchedule', () => {
  const travel = useTravelStore()

  function period(code) {
    const country = countries.find(item => item.code === code)
    const plan = travel.selectedPlans.find(item => item.code === code)
    return { startDate: plan?.startDate || country.defaultStart, endDate: plan?.endDate || country.defaultEnd }
  }

  function makeSeeds() {
    let id = 1
    return countries.flatMap(country => seedTemplates[country.code].map((template, index) => ({
      id: id++, countryCode: country.code, title: template[0], date: dateWithOffset(period(country.code).startDate, country.code === 'FR' ? 0 : index * 2, period(country.code).endDate),
      time: template[1], currency: country.currency, amount: template[2], paymentStatus: template[3],
      placeName: template[0].split(' ')[0], placeAddress: template[4], memo: template[6], completed: false,
      notificationTriggered: country.code === 'FR' || index === 0,
    })))
  }

  const schedules = ref(loadSaved() ?? makeSeeds())
  const sortedSchedules = computed(() => [...schedules.value].sort((a, b) => `${a.date}${a.time}`.localeCompare(`${b.date}${b.time}`)))
  const configuredPeriods = computed(() => travel.selectedPlans
    .filter(plan => plan?.startDate && plan?.endDate)
    .map(plan => ({ code: plan.code, startDate: plan.startDate, endDate: plan.endDate })))
  const displayPeriods = computed(() => configuredPeriods.value.length
    ? configuredPeriods.value
    : countries.map(item => ({ code: item.code, ...period(item.code) })))
  const travelStart = computed(() => displayPeriods.value.map(item => item.startDate).sort()[0])
  const travelEnd = computed(() => displayPeriods.value.map(item => item.endDate).sort().at(-1))
  const demoToday = computed(() => sortedSchedules.value[0]?.date ?? travelStart.value)

  function countryForDate(date) {
    const matchedPeriod = displayPeriods.value.find(item => date >= item.startDate && date <= item.endDate)
    return countries.find(country => country.code === matchedPeriod?.code)
  }
  function normalizeSchedule(item) {
    if (!item) return item
    return { ...item, placeName: item.placeName || item.place || '', placeAddress: item.placeAddress || '', notificationTriggered: Boolean(item.notificationTriggered) }
  }
  function getSchedule(id) { return normalizeSchedule(schedules.value.find(item => item.id === Number(id))) }
  function save(payload) {
    const country = countryForDate(payload.date)
    if (!country) return false
    schedules.value.push({ ...payload, id: Date.now(), countryCode: country.code, completed: false, notificationTriggered: false })
    return true
  }
  function update(id, payload) {
    const index = schedules.value.findIndex(item => item.id === Number(id))
    const country = countryForDate(payload.date)
    if (index < 0 || !country) return false
    schedules.value[index] = { ...schedules.value[index], ...payload, countryCode: country.code }
    return true
  }
  function remove(id) {
    const index = schedules.value.findIndex(item => item.id === Number(id))
    if (index < 0) return false
    schedules.value.splice(index, 1)
    return true
  }
  function toggleComplete(id) {
    const schedule = getSchedule(id)
    if (schedule) schedule.completed = !schedule.completed
  }
  function markNotificationRead(id) {
    const schedule = getSchedule(id)
    if (schedule) schedule.notificationRead = true
  }

  watch(schedules, value => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)), { deep: true })

  return { countries, schedules, sortedSchedules, configuredPeriods, travelStart, travelEnd, demoToday, period, countryForDate, getSchedule, save, update, remove, toggleComplete, markNotificationRead, normalizeSchedule }
})
