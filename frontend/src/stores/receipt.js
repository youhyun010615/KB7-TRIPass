import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY = 'tripass-receipts'

export const receiptTrips = [
  { id:1, title:'총 2개국 배낭여행', countries:['프랑스','이탈리아'], dateRange:'2025.07.10 ~ 2025.07.24' },
  { id:2, title:'동남아 단기 여행', countries:['태국','홍콩'], dateRange:'2025.09.01 ~ 2025.09.08' },
  { id:3, title:'일본 오사카 여행', countries:['일본'], dateRange:'2024.12.20 ~ 2024.12.25' },
]

const seed = [
  { id:101, tripId:1, country:'프랑스', flag:'🇫🇷', merchant:'Ristorante Pizzeria da RITA', date:'2025.07.12', currency:'EUR', amount:17, wonAmount:25300, category:'식비', confidence:96, memo:'파리 저녁 식사' },
  { id:102, tripId:1, country:'프랑스', flag:'🇫🇷', merchant:'Hotel Florence Central', date:'2025.07.13', currency:'EUR', amount:125, wonAmount:185750, category:'숙박', confidence:94, memo:'' },
  { id:103, tripId:1, country:'이탈리아', flag:'🇮🇹', merchant:'Firenze SMN', date:'2025.07.20', currency:'EUR', amount:28, wonAmount:41608, category:'교통', confidence:92, memo:'기차표' },
  { id:201, tripId:2, country:'홍콩', flag:'🇭🇰', merchant:'Victoria Peak Tram', date:'2025.09.04', currency:'HKD', amount:108, wonAmount:19872, category:'관광', confidence:95, memo:'' },
  { id:301, tripId:3, country:'일본', flag:'🇯🇵', merchant:'OSAKA TAKOYAKI', date:'2024.12.22', currency:'JPY', amount:1800, wonAmount:16614, category:'식비', confidence:97, memo:'' },
]

function load() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') ?? seed }
  catch { return seed }
}

export const useReceiptStore = defineStore('receipt', () => {
  const receipts = ref(load())
  const draft = ref(null)
  const trip = id => receiptTrips.find(item => item.id === Number(id)) || receiptTrips[0]
  const byTrip = id => computed(() => receipts.value.filter(item => item.tripId === Number(id)).sort((a,b) => b.date.localeCompare(a.date)))
  const get = id => receipts.value.find(item => item.id === Number(id))
  function save(payload) {
    const item = { ...payload, id:payload.id || Date.now() }
    const index = receipts.value.findIndex(receipt => receipt.id === item.id)
    if (index >= 0) receipts.value[index] = item
    else receipts.value.unshift(item)
    return item
  }
  function remove(id) { receipts.value = receipts.value.filter(item => item.id !== Number(id)) }
  watch(receipts, value => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)), { deep:true })
  return { receipts, draft, trip, byTrip, get, save, remove }
})
