<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore } from '@/stores/asset'

const router = useRouter()
const asset = useAssetStore()
const tabs = [{ id: 'all', label: '전체' }, { id: 'deposit', label: '입금' }, { id: 'withdrawal', label: '출금' }]
const periods = [
  { id: '1week', label: '최근 1주', start: '2024-07-13', end: '2024-07-19' },
  { id: '1month', label: '최근 1개월', start: '2024-06-20', end: '2024-07-19' },
  { id: '3months', label: '최근 3개월', start: '2024-04-20', end: '2024-07-19' },
  { id: '6months', label: '최근 6개월', start: '2024-01-20', end: '2024-07-19' },
  { id: 'custom', label: '직접 설정' },
]
const customOpen = ref(asset.transactionPeriod === 'custom')
const customStart = ref(asset.transactionStartDate)
const customEnd = ref(asset.transactionEndDate)
const counts = computed(() => ({ all: asset.periodTransactions.length, deposit: asset.depositCount, withdrawal: asset.withdrawalCount }))
const formatDate = value => value.replaceAll('-', '.')
const selectedPeriodLabel = computed(() => periods.find(item => item.id === asset.transactionPeriod)?.label || '기간 설정')

function selectPeriod(event) {
  const selected = periods.find(item => item.id === event.target.value)
  if (!selected) return
  if (selected.id === 'custom') {
    asset.transactionPeriod = 'custom'
    customOpen.value = true
    return
  }
  customOpen.value = false
  asset.setTransactionPeriod(selected.id, selected.start, selected.end)
}
function applyCustomPeriod() {
  if (!customStart.value || !customEnd.value || customStart.value > customEnd.value) return
  asset.setTransactionPeriod('custom', customStart.value, customEnd.value)
  customOpen.value = false
}
</script>

<template>
  <main class="list-page">
    <header><button type="button" @click="router.back()">‹</button><h1>전체 계좌 거래내역</h1><span /></header>
    <section class="period-control">
      <div><small>{{ formatDate(asset.transactionStartDate) }} ~ {{ formatDate(asset.transactionEndDate) }}</small><b>{{ selectedPeriodLabel }}</b></div>
      <label><span class="sr-only">조회 기간</span><select :value="asset.transactionPeriod" @change="selectPeriod"><option v-for="period in periods" :key="period.id" :value="period.id">{{ period.label }}</option></select></label>
    </section>
    <div class="calendar-actions">
      <button type="button" @click="customOpen = !customOpen">▣ 날짜 직접 선택</button>
      <button type="button" @click="router.push('/asset/transactions/calendar')">거래내역 캘린더 ›</button>
    </div>
    <section v-if="customOpen" class="custom-period"><label><span>시작일</span><input v-model="customStart" type="date"></label><label><span>종료일</span><input v-model="customEnd" type="date"></label><button type="button" :disabled="!customStart || !customEnd || customStart > customEnd" @click="applyCustomPeriod">적용</button></section>
    <nav class="filter-tabs"><button v-for="tab in tabs" :key="tab.id" :class="{ active: asset.transactionFilter === tab.id }" @click="asset.transactionFilter = tab.id">{{ tab.label }}</button></nav>
    <section class="counts"><span v-for="tab in tabs" :key="tab.id"><small>{{ tab.label === 'all' ? '전체 거래' : tab.label }}</small><b>{{ counts[tab.id] }}건</b></span></section>
    <TransactionGroups :groups="asset.groupedTransactions" :show-icons="false" @select="router.push(`/asset/transactions/${$event.id}`)" />
    <BottomNav />
  </main>
</template>

<style scoped>
.list-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 100px;background:#f4f6fc;color:#10192d}.list-page>header{display:grid;grid-template-columns:30px 1fr 30px;align-items:center;margin-bottom:16px}.list-page>header button{font-size:24px;text-align:left}.list-page>header h1{text-align:center;font-size:18px;font-weight:900}.period-control{display:flex;align-items:center;justify-content:space-between;padding:12px 14px;border-radius:14px;background:#e9edf5;color:#64748b}.period-control div>*{display:block}.period-control small{font-size:9px;font-weight:800}.period-control b{margin-top:4px;color:#334155;font-size:11px}.period-control select{padding:8px 26px 8px 10px;border:1px solid #d4dce8;border-radius:10px;background:#fff;color:#315786;font-size:10px;font-weight:900;outline:none}.custom-period{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:9px;padding:12px;border:1px solid #dde4ee;border-radius:13px;background:#fff}.custom-period label span{display:block;margin-bottom:5px;color:#718096;font-size:8px}.custom-period input{width:100%;padding:8px;border:1px solid #dbe2ec;border-radius:8px;font-size:9px}.custom-period button{grid-column:1/-1;height:36px;border-radius:9px;background:#173f8d;color:#fff;font-size:10px;font-weight:900}.custom-period button:disabled{background:#aab4c3}.filter-tabs{display:grid;grid-template-columns:repeat(3,1fr);margin:12px -20px 0;background:#fff}.filter-tabs button{height:46px;border-bottom:2px solid #e7eaf1;color:#b0bac9;font-size:12px;font-weight:800}.filter-tabs .active{border-color:#3475f4;color:#3475f4}.counts{display:grid;grid-template-columns:repeat(3,1fr);margin:0 -20px 18px;padding:12px 20px;background:#fff}.counts span{text-align:center}.counts small,.counts b{display:block}.counts small{color:#94a3b8;font-size:9px}.counts b{margin-top:5px;font-size:17px}.sr-only{position:absolute;width:1px;height:1px;padding:0;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
.calendar-actions{display:flex;justify-content:space-between;margin:10px 2px 0}.calendar-actions button{color:#315786;font-size:9px;font-weight:900}
</style>
