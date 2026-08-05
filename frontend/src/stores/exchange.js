import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY = 'tripass-exchange'
const currencies = [
  { code:'EUR', name:'유로', flag:'🇫🇷', unit:1, rate:1486.20, change:-3.10, decimals:2, chart:[1477,1481,1488,1485,1491,1488,1486.2] },
  { code:'CHF', name:'스위스 프랑', flag:'🇨🇭', unit:1, rate:1704.60, change:2.40, decimals:2, chart:[1695,1698,1701,1700,1706,1703,1704.6] },
  { code:'JPY', name:'일본 엔', flag:'🇯🇵', unit:100, rate:923.10, change:-1.20, decimals:2, chart:[928,926,924,925,922,924,923.1] },
  { code:'HKD', name:'홍콩 달러', flag:'🇭🇰', unit:1, rate:184.2, change:-0.18, decimals:2, chart:[184.7,184.5,184.3,184.4,184.1,184.3,184.2] },
]
const banks = [
  { id:'kb-gangnam', name:'KB국민은행 강남역지점', distance:350, walk:5, address:'서울 강남구 강남대로 396', phone:'02-0000-0000', hours:'09:00 - 16:00', lat:44, top:43, preferentialRate:1480.10 },
  { id:'kb-seolleung', name:'KB국민은행 선릉지점', distance:620, walk:8, address:'서울 강남구 테헤란로 412', phone:'02-1111-1111', hours:'09:00 - 16:00', lat:70, top:28, preferentialRate:1482.30 },
  { id:'shinhan-gangnam', name:'신한은행 강남중앙지점', distance:780, walk:11, address:'서울 강남구 역삼로 152', phone:'02-2222-2222', hours:'09:00 - 16:00', lat:28, top:68, preferentialRate:1484.50 },
]

function loadState() { try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') } catch { return null } }

export const useExchangeStore = defineStore('exchange', () => {
  const saved=loadState()
  const selectedCode=ref(saved?.selectedCode || 'EUR')
  const period=ref(saved?.period || '1w')
  const krwAmount=ref(saved?.krwAmount || 100_000)
  const alerts=ref(saved?.alerts || [
    { id:1, code:'EUR', target:1480, amount:100_000, enabled:true },
    { id:2, code:'CHF', target:1700, amount:150_000, enabled:true },
  ])
  const selectedBankId=ref(saved?.selectedBankId || 'kb-gangnam')
  const selectedCurrency=computed(()=>currencies.find((item)=>item.code===selectedCode.value) || currencies[0])
  const selectedBank=computed(()=>banks.find((item)=>item.id===selectedBankId.value) || banks[0])
  const foreignAmount=computed(()=>krwAmount.value / selectedCurrency.value.rate * selectedCurrency.value.unit)
  function convertForeign(value,currency=selectedCurrency.value){ return Number(value||0) / currency.unit * currency.rate }
  function expectedForeign(amount,rate,unit=1){ return Number(amount||0) / Number(rate||1) * unit }
  function saveAlert(payload){
    if(payload.id){const index=alerts.value.findIndex((item)=>item.id===payload.id);if(index>=0) alerts.value[index]={...payload}}
    else alerts.value.push({...payload,id:Date.now(),enabled:true})
  }
  function removeAlert(id){alerts.value=alerts.value.filter((item)=>item.id!==id)}
  function getCurrency(code){return currencies.find((item)=>item.code===code)}
  function getBank(id){return banks.find((item)=>item.id===id)}
  watch([selectedCode,period,krwAmount,alerts,selectedBankId],()=>localStorage.setItem(STORAGE_KEY,JSON.stringify({selectedCode:selectedCode.value,period:period.value,krwAmount:krwAmount.value,alerts:alerts.value,selectedBankId:selectedBankId.value})),{deep:true})
  return {currencies,banks,selectedCode,period,krwAmount,alerts,selectedBankId,selectedCurrency,selectedBank,foreignAmount,convertForeign,expectedForeign,saveAlert,removeAlert,getCurrency,getBank}
})
