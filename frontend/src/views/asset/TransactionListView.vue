<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore } from '@/stores/asset'

const router = useRouter()
const asset = useAssetStore()
const tabs = [{ id: 'all', label: '전체' }, { id: 'deposit', label: '입금' }, { id: 'withdrawal', label: '출금' }]
const filter = ref('all')
const startDate = ref('2024-06-20')
const endDate = ref('2024-07-19')
const filteredTransactions = computed(() => asset.transactions.filter((item) => {
  if (item.date < startDate.value || item.date > endDate.value) return false
  if (filter.value === 'deposit') return item.amount > 0
  if (filter.value === 'withdrawal') return item.amount < 0
  return true
}))
const groups = computed(() => filteredTransactions.value.reduce((result, item) => {
  const group = result.find((entry) => entry.date === item.date)
  if (group) group.items.push(item)
  else result.push({ date: item.date, label: item.dateLabel, items: [item] })
  return result
}, []))
</script>

<template>
  <main class="list-page">
    <header><button type="button" @click="router.back()">‹</button><h1>전체 계좌 거래내역</h1><span /></header>
    <section class="summary"><small>연동 계좌 {{ asset.accounts.length }}개</small><b>총 {{ filteredTransactions.length }}건의 거래내역</b></section>
    <section class="date-filter"><label><span>시작일</span><input v-model="startDate" type="date" :max="endDate"></label><i>~</i><label><span>종료일</span><input v-model="endDate" type="date" :min="startDate"></label><button type="button" aria-label="거래내역 캘린더" @click="router.push('/asset/transactions/calendar')"><svg width="21" height="21" viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2" stroke="currentColor" stroke-width="1.8"/><path d="M8 3V7M16 3V7M3 10H21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M8 14H8.01M12 14H12.01M16 14H16.01M8 18H8.01M12 18H12.01" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg></button></section>
    <nav class="filter-tabs"><button v-for="tab in tabs" :key="tab.id" :class="{ active: filter === tab.id }" @click="filter = tab.id">{{ tab.label }}</button></nav>
    <TransactionGroups :groups="groups" :show-icons="false" @select="router.push(`/asset/transactions/${$event.id}`)" />
    <BottomNav />
  </main>
</template>

<style scoped>
.list-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 100px;background:#f4f6fc;color:#10192d}.list-page>header{display:grid;grid-template-columns:30px 1fr 30px;align-items:center;margin-bottom:16px}.list-page>header button{font-size:24px;text-align:left}.list-page>header h1{text-align:center;font-size:18px;font-weight:900}.summary{padding:15px;border-radius:14px;background:linear-gradient(135deg,#173f8d,#2866bd);color:#fff}.summary small,.summary b{display:block}.summary small{color:#cbdcf7;font-size:9px}.summary b{margin-top:5px;font-size:14px}.date-filter{display:grid;grid-template-columns:1fr auto 1fr 34px;align-items:end;gap:7px;margin-top:10px;padding:10px 12px;border:1px solid #dbe3ef;border-radius:12px;background:#fff}.date-filter label span{display:block;margin-bottom:5px;color:#94a3b8;font-size:8px}.date-filter input{width:100%;font-size:9px}.date-filter i{padding-bottom:2px;color:#94a3b8;font-size:9px;font-style:normal}.date-filter>button{display:grid;width:34px;height:34px;place-items:center;border-radius:9px;background:#edf4ff;color:#286dd8}.filter-tabs{display:grid;grid-template-columns:repeat(3,1fr);margin:12px 0 16px}.filter-tabs button{padding:11px 0;border-bottom:2px solid #dce3ee;color:#b0bac9;font-size:11px;font-weight:900}.filter-tabs .active{border-color:#3475f4;color:#3475f4}
</style>
