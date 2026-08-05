import { computed, reactive, watch } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY = 'tripass-checklists'

const seed = {
  1: {
    trip: { id: 1, title: '유럽 2개국 배낭여행', flags: '🇫🇷 🇨🇭', countries: '프랑스 · 스위스', departureDate: '2026.08.15', returnDate: '2026.08.25', dDay: 10, flightTime: '18:20', flight: 'AZ796' },
    preparation: {
      d30: [
        { id: 'p301', text: '항공권 예약 확인', note: '예약번호와 탑승객 정보 확인', done: true, origin: 'D-30' },
        { id: 'p302', text: '숙소 예약하기', note: '체크인 날짜와 주소 저장', done: true, origin: 'D-30' },
        { id: 'p303', text: '여권 유효기간 확인', note: '만료일까지 6개월 이상', done: false, origin: 'D-30' },
        { id: 'p304', text: '국제운전면허증 발급', note: '렌터카 이용 시 필요', done: false, origin: 'D-30' },
        { id: 'p305', text: '여행자보험 가입', note: '보장 범위 확인', done: true, origin: 'D-30' },
      ],
      d7: [
        { id: 'p701', text: '환전 완료하기', note: '목표 환율과 필요 현금 확인', done: true, origin: 'D-7' },
        { id: 'p702', text: '지도·번역 앱 설치', note: '오프라인 지도 저장', done: true, origin: 'D-7' },
        { id: 'p703', text: '해외 결제 카드 확인', note: '해외사용 잠금 해제', done: true, origin: 'D-7' },
      ],
      d1: [
        { id: 'p101', text: '여권 챙기기', note: '기내 가방에 보관', done: true, origin: 'D-1' },
        { id: 'p102', text: '항공권·탑승 정보 확인', note: '터미널과 탑승 시간 확인', done: true, origin: 'D-1' },
        { id: 'p103', text: '수하물 무게 확인', note: '항공사 허용 기준 확인', done: true, origin: 'D-1' },
      ],
    },
    returns: [
      { id: 'r1', text: '여권·지갑·소지품 확인', note: '숙소 금고와 객실 점검', done: true },
      { id: 'r2', text: '모바일 탑승권 저장', note: 'AZ796 탑승권 준비', done: true },
      { id: 'r3', text: '택스 리펀 서류 챙기기', note: '도장과 영수증 원본 확인', done: false },
      { id: 'r4', text: '보조배터리 기내 소지', note: '위탁 수하물 금지', done: false },
      { id: 'r5', text: '트래블카드 자동충전 OFF', note: '귀국 후 불필요한 충전 방지', done: false },
    ],
  },
  2: {
    trip: { id: 2, title: '홍콩 도심 여행', flags: '🇭🇰', countries: '홍콩', departureDate: '2026.09.01', returnDate: '2026.09.08', dDay: 27, flightTime: '20:10', flight: 'KE177' },
    preparation: { d30: [], d7: [], d1: [] },
    returns: [],
  },
}

function load() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') || seed }
  catch { return seed }
}

export const useChecklistStore = defineStore('checklist', () => {
  const trips = reactive(load())

  function getTrip(id = 1) { return trips[Number(id)] || trips[1] }

  function preparationItems(id) {
    const prep = getTrip(id).preparation
    return [...prep.d30, ...prep.d7, ...prep.d1]
  }

  function rolledItems(id, stage) {
    const prep = getTrip(id).preparation
    if (stage === 'd30') return prep.d30
    if (stage === 'd7') return [...prep.d7, ...prep.d30.filter((item) => !item.done)]
    return [...prep.d1, ...prep.d7.filter((item) => !item.done), ...prep.d30.filter((item) => !item.done)]
  }

  function progress(items) {
    const total = items.length
    const done = items.filter((item) => item.done).length
    return { done, total, percent: total ? Math.round((done / total) * 100) : 0 }
  }

  function toggle(id, type, itemId) {
    const trip = getTrip(id)
    const items = type === 'return' ? trip.returns : Object.values(trip.preparation).flat()
    const item = items.find((entry) => entry.id === itemId)
    if (item) item.done = !item.done
  }

  function add(id, type, stage, text) {
    const value = text.trim()
    if (!value) return false
    const item = { id: `${type}-${Date.now()}`, text: value, note: '직접 추가한 항목', done: false, origin: stage?.toUpperCase() }
    if (type === 'return') getTrip(id).returns.push(item)
    else getTrip(id).preparation[stage].push(item)
    return true
  }

  watch(trips, (value) => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)), { deep: true })

  return { trips, getTrip, preparationItems, rolledItems, progress, toggle, add }
})
