import { computed } from 'vue'
import { defineStore } from 'pinia'
import { useChecklistStore } from '@/stores/checklist'
import { useReceiptStore } from '@/stores/receipt'

const reports = {
  1: {
    trip: { id:1, title:'유럽 2개국 배낭여행', flags:'🇫🇷 🇨🇭', countries:'프랑스 · 스위스', dateRange:'2026.08.15 ~ 2026.08.25', dDay:10, status:'완료', days:14 },
    targetBudget:5_000_000, securedFund:4_620_000, prepaid:1_800_000, localSpent:2_820_000,
    accounts:6, schedules:14, paidSchedules:6, pendingSchedules:8,
    countrySpend:[{name:'프랑스',flag:'🇫🇷',amount:2_650_000,budget:3_000_000,color:'#1767dc'},{name:'스위스',flag:'🇨🇭',amount:1_970_000,budget:2_000_000,color:'#c8173c'}],
    categories:[{name:'식비',amount:1_450_000,color:'#2675ea'},{name:'카페',amount:960_000,color:'#7143e8'},{name:'생활비',amount:730_000,color:'#25ad72'},{name:'쇼핑',amount:580_000,color:'#ef3d91'},{name:'여가',amount:510_000,color:'#ff922b'},{name:'기타',amount:390_000,color:'#93a4ba'}],
    daily:[280_000,410_000,670_000,520_000,390_000,610_000,430_000],
    payments:[{name:'카드',amount:3_240_000,color:'#2675ea'},{name:'현금',amount:1_120_000,color:'#7143e8'},{name:'기타',amount:260_000,color:'#ff922b'}],
  },
  2: {
    trip: { id:2, title:'홍콩 도심 여행', flags:'🇭🇰', countries:'홍콩', dateRange:'2026.09.01 ~ 2026.09.08', dDay:27, status:'예정', days:8 },
    targetBudget:1_800_000, securedFund:1_200_000, prepaid:620_000, localSpent:0, accounts:2, schedules:5, paidSchedules:2, pendingSchedules:3,
    countrySpend:[{name:'홍콩',flag:'🇭🇰',amount:0,budget:1_800_000,color:'#c8173c'}], categories:[], daily:[], payments:[],
  },
}

export const useTravelReportStore = defineStore('travelReport', () => {
  const checklistStore = useChecklistStore()
  const receiptStore = useReceiptStore()
  const getReport = (id=1) => reports[Number(id)] || reports[1]
  const checklistProgress = id => checklistStore.progress(checklistStore.preparationItems(id))
  const receiptCount = id => receiptStore.receipts.filter(item => item.tripId === Number(id)).length
  const usedAmount = id => { const r=getReport(id); return r.prepaid+r.localSpent }
  const budgetPercent = id => { const r=getReport(id); return Math.round(usedAmount(id)/r.targetBudget*100) }
  const remaining = id => getReport(id).targetBudget-usedAmount(id)
  const savingsPercent = id => { const r=getReport(id); return Math.round(r.securedFund/r.targetBudget*100) }
  return { reports, getReport, checklistProgress, receiptCount, usedAmount, budgetPercent, remaining, savingsPercent }
})
