import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY = 'tripass-financial-products'

const savingsProducts = [
  { id:'shinhan-travel', bank:'신한은행', bankCode:'S', name:'신한 여행비치 자유적금', type:'travel', period:6, maxMonthly:1_000_000, baseRate:3.2, maxRate:3.8, earlyTermination:true, color:'#1672f9', description:'여행 목표 달성을 위한 6개월 맞춤 적금' },
  { id:'kb-travel', bank:'KB국민은행', bankCode:'KB', name:'KB 여행플러스 정기적금', type:'travel', period:6, maxMonthly:3_000_000, baseRate:3.0, maxRate:3.5, earlyTermination:true, color:'#f5b800', description:'여행 준비 고객을 위한 우대금리 적금' },
  { id:'nh-travel', bank:'NH농협은행', bankCode:'NH', name:'NH 여행자유적금', type:'travel', period:12, maxMonthly:2_000_000, baseRate:2.8, maxRate:3.2, earlyTermination:false, color:'#19a66a', description:'긴 여행 계획에 맞춘 자유 납입 적금' },
  { id:'woori-wonder', bank:'우리은행', bankCode:'W', name:'우리 여행드림 적금', type:'general', period:6, maxMonthly:500_000, baseRate:2.7, maxRate:3.0, earlyTermination:true, color:'#2785db', description:'간편하게 시작하는 단기 목표 적금' },
]

const cardProducts = [
  { id:'hana-travellog', company:'하나카드', code:'H', name:'하나 트래블로그 카드', network:'TRAVELLOG', rank:1, maxCharge:3_000_000, overseasFee:0, reloadFee:1.0, atmLimit:1_000_000, currencies:26, transit:true, color:'#11b79c', benefits:['해외 결제 수수료 무료','환전 지원 26개 통화','해외 ATM 출금 지원'] },
  { id:'shinhan-sol', company:'신한카드', code:'S', name:'신한 SOL 트래블 체크', network:'SOL', rank:2, maxCharge:2_000_000, overseasFee:0, reloadFee:1.5, atmLimit:1_000_000, currencies:30, transit:true, color:'#246cff', benefits:['해외 결제 수수료 무료','공항 라운지 연 2회','일본 교통카드 지원'] },
  { id:'kb-travellers', company:'KB국민카드', code:'KB', name:'KB 트래블러스 체크', network:'TRAVELLERS', rank:3, maxCharge:2_000_000, overseasFee:0.25, reloadFee:1.0, atmLimit:700_000, currencies:33, transit:false, color:'#e9ad00', benefits:['33개 통화 환전 지원','KB Pay 간편 충전','해외 쇼핑 할인'] },
]

function loadCompare() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? JSON.parse(saved) : ['hana-travellog', 'shinhan-sol']
  }
  catch { return ['hana-travellog', 'shinhan-sol'] }
}

export const useFinancialProductsStore = defineStore('financialProducts', () => {
  const savingsQuery = ref('')
  const savingsFilter = ref('all')
  const cardQuery = ref('')
  const cardFilter = ref('all')
  const comparedCardIds = ref(loadCompare())
  const monthlySavings = ref(500_000)
  const remainingMonths = ref(6)
  const securedAmount = ref(2_000_000)
  const targetAmount = ref(5_000_000)

  const filteredSavings = computed(() => savingsProducts
    .filter((item) => savingsFilter.value === 'all' || item.type === savingsFilter.value)
    .filter((item) => `${item.bank} ${item.name}`.toLowerCase().includes(savingsQuery.value.trim().toLowerCase()))
    .sort((a, b) => b.maxRate - a.maxRate))

  const filteredCards = computed(() => cardProducts
    .filter((item) => cardFilter.value === 'all' || (cardFilter.value === 'overseas' && item.overseasFee === 0))
    .filter((item) => `${item.company} ${item.name}`.toLowerCase().includes(cardQuery.value.trim().toLowerCase())))

  const comparedCards = computed(() => comparedCardIds.value.map((id) => cardProducts.find((item) => item.id === id)).filter(Boolean))
  const expectedSavings = computed(() => monthlySavings.value * remainingMonths.value)
  const canReachGoal = computed(() => securedAmount.value + expectedSavings.value >= targetAmount.value)

  function getSaving(id) { return savingsProducts.find((item) => item.id === id) }
  function getCard(id) { return cardProducts.find((item) => item.id === id) }
  function toggleCompare(id) {
    if (comparedCardIds.value.includes(id)) comparedCardIds.value = comparedCardIds.value.filter((item) => item !== id)
    else if (comparedCardIds.value.length < 3) comparedCardIds.value.push(id)
  }

  watch(comparedCardIds, (value) => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)), { deep:true })

  return { savingsProducts, cardProducts, savingsQuery, savingsFilter, cardQuery, cardFilter, comparedCardIds, monthlySavings, remainingMonths, securedAmount, targetAmount, filteredSavings, filteredCards, comparedCards, expectedSavings, canReachGoal, getSaving, getCard, toggleCompare }
})
