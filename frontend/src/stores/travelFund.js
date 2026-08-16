import { computed, reactive, watch } from 'vue'
import { defineStore } from 'pinia'
import { useTravelStore } from '@/stores/travel'

const categorySeed = [
  { id: 'food', name: '식비', icon: '🍴', color: '#2378ea', description:'식사, 배달, 식료품' },
  { id: 'cafe', name: '카페', icon: '☕', color: '#7248df', description:'커피와 디저트' },
  { id: 'living', name: '생활비', icon: '📦', color: '#22ad6f', description:'마트, 편의점, 생활용품' },
  { id: 'shopping', name: '쇼핑', icon: '🛍️', color: '#ef3b86', description:'의류, 화장품, 기념품' },
  { id: 'hobby', name: '취미·여가', icon: '🎨', color: '#ff912f', description:'관광, 공연, 액티비티' },
  { id: 'other', name: '기타', icon: '•••', color: '#98a7ba', description:'그 외 여행 지출' },
]

const countrySeed = [
  { code: 'FR', name: '프랑스', city: '파리', flag: '🇫🇷', theme: '#124a9b', target: 2_000_000, prepaid: 810_000, defaultStart: '2026-08-15', defaultEnd: '2026-08-22' },
  { code: 'CH', name: '스위스', city: '인터라켄', flag: '🇨🇭', theme: '#a51333', target: 3_000_000, prepaid: 990_000, defaultStart: '2026-08-23', defaultEnd: '2026-08-26' },
  { code: 'DE', name: '독일', city: '베를린', flag: '🇩🇪', theme: '#1b1b1b', target: 2_500_000, prepaid: 830_000, defaultStart: '2026-08-27', defaultEnd: '2026-08-30' },
  { code: 'JP', name: '일본', city: '도쿄', flag: '🇯🇵', theme: '#ce2b72', target: 2_200_000, prepaid: 760_000, defaultStart: '2026-09-01', defaultEnd: '2026-09-05' },
  { code: 'HK', name: '홍콩', city: '홍콩', flag: '🇭🇰', theme: '#b8202e', target: 1_800_000, prepaid: 620_000, defaultStart: '2026-09-06', defaultEnd: '2026-09-10' },
]

const templates = {
  FR: [
    ['Café de Flore', 68_000, 'food', '🍽️', 0, '파리에서 즐긴 첫 저녁 식사'], ['Boulangerie Utopie', 42_000, 'food', '🥐', 2, '아침 식사'], ['Le Comptoir', 40_000, 'food', '🍴', 4, '점심 식사'],
    ['Merci Coffee', 55_000, 'cafe', '☕', 1, '커피와 디저트'], ['Angelina Paris', 65_000, 'cafe', '🍰', 5, '몽블랑 디저트'], ['Monoprix', 180_000, 'living', '🛒', 2, '여행 생활용품'],
    ['Galeries Lafayette', 80_000, 'shopping', '🛍️', 3, '기념품 구매'], ['Seine Cruise', 60_000, 'hobby', '🛥️', 4, '센강 크루즈'], ['Metro Ticket', 10_000, 'other', '🚇', 1, '교통권 구매'],
  ],
  CH: [
    ['Swiss Fondue House', 250_000, 'food', '🫕', 0, '퐁뒤 저녁 식사'], ['Coop Interlaken', 200_000, 'food', '🥗', 2, '식료품 구매'], ['Velo Cafe', 200_000, 'cafe', '☕', 1, '브런치 카페'],
    ['Migros', 250_000, 'living', '🛒', 1, '여행 생활용품'], ['Interlaken Souvenir', 50_000, 'shopping', '🛍️', 2, '기념품 구매'], ['Harder Kulm', 50_000, 'hobby', '🚞', 0, '전망대 이용'], ['Locker', 10_000, 'other', '🧳', 2, '보관함 이용'],
  ],
  DE: [
    ['Zeit für Brot', 140_000, 'food', '🥨', 0, '베를린 식사'], ['Markthalle Neun', 140_000, 'food', '🍴', 2, '마켓 식사'], ['The Barn', 130_000, 'cafe', '☕', 1, '커피'],
    ['dm Drogerie', 260_000, 'living', '🧴', 1, '생활용품'], ['KaDeWe', 110_000, 'shopping', '🛍️', 2, '기념품'], ['Museum Island', 100_000, 'hobby', '🏛️', 0, '박물관 관람'], ['BVG', 70_000, 'other', '🚇', 2, '교통권'],
  ],
  JP: [
    ['이치란 라멘', 120_000, 'food', '🍜', 0, '도쿄 첫 식사'], ['스시다이', 110_000, 'food', '🍣', 2, '스시 식사'], ['블루보틀 시부야', 100_000, 'cafe', '☕', 1, '커피'],
    ['돈키호테', 190_000, 'living', '🛒', 2, '여행 생활용품'], ['시부야 PARCO', 120_000, 'shopping', '🛍️', 3, '쇼핑'], ['도쿄 스카이트리', 90_000, 'hobby', '🗼', 1, '전망대'], ['Suica', 60_000, 'other', '🚃', 0, '교통카드 충전'],
  ],
  HK: [
    ['Tim Ho Wan', 110_000, 'food', '🥟', 0, '딤섬 식사'], ['Mak Man Kee', 100_000, 'food', '🍜', 2, '완탕면 식사'], ['NOC Coffee', 90_000, 'cafe', '☕', 1, '커피'],
    ['Market Place', 160_000, 'living', '🛒', 2, '생활용품'], ['K11 MUSEA', 100_000, 'shopping', '🛍️', 3, '쇼핑'], ['Victoria Peak', 80_000, 'hobby', '🚠', 1, '피크 트램'], ['Octopus', 60_000, 'other', '🚇', 0, '옥토퍼스 충전'],
  ],
}

function addDays(iso, offset, endDate) {
  const date = new Date(`${iso}T00:00:00`)
  date.setDate(date.getDate() + offset)
  const result = date.toISOString().slice(0, 10)
  return result > endDate ? endDate : result
}

export const useTravelFundStore = defineStore('travelFund', () => {
  const travel = useTravelStore()
  const countries = countrySeed
  const categories = categorySeed
  let savedOverrides = {}
  try { savedOverrides = JSON.parse(localStorage.getItem('tripass-travel-fund-overrides') || '{}') } catch { savedOverrides = {} }
  const transactionOverrides = reactive(savedOverrides)

  function period(code) {
    const country = countries.find(item => item.code === code)
    const plan = travel.selectedPlans.find(item => item.code === code)
    if (!country) {
      return {
        startDate: plan?.startDate || '2000-01-01',
        endDate: plan?.endDate || '2099-12-31',
      }
    }
    return {
      startDate: plan?.startDate || country.defaultStart,
      endDate: plan?.endDate || country.defaultEnd,
    }
  }

  const transactions = computed(() => countries.flatMap(country => {
    const range = period(country.code)
    return templates[country.code].map((item, index) => ({
      id: `${country.code}-${index + 1}`,
      countryCode: country.code,
      merchant: item[0], amount: item[1], categoryId: item[2], icon: item[3],
      date: addDays(range.startDate, item[4], range.endDate), memo: item[5], ...transactionOverrides[`${country.code}-${index + 1}`],
    }))
  }))

  function getCountry(code) { return countries.find(item => item.code === code) }
  function getCategory(id) { return categories.find(item => item.id === id) }
  function countryTransactions(code) {
    if (code === 'all') return transactions.value
    const range = period(code)
    return transactions.value.filter(item => item.countryCode === code && item.date >= range.startDate && item.date <= range.endDate)
  }
  function categoryTransactions(code, categoryId) {
    return countryTransactions(code).filter(item => item.categoryId === categoryId)
  }
  function getTransaction(id) { return transactions.value.find(item => item.id === id) }
  function updateTransaction(id, patch) {
    if (!getTransaction(id)) return false
    transactionOverrides[id] = { ...(transactionOverrides[id] || {}), ...patch }
    return true
  }
  watch(transactionOverrides, value => localStorage.setItem('tripass-travel-fund-overrides', JSON.stringify(value)), { deep:true })

  return { countries, categories, transactions, period, getCountry, getCategory, countryTransactions, categoryTransactions, getTransaction, updateTransaction }
})
