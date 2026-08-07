<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AssetTicket from '@/components/asset/AssetTicket.vue'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore } from '@/stores/asset'

const route = useRoute()
const router = useRouter()
const asset = useAssetStore()
const account = computed(() => asset.getAccount(route.params.accountId) || asset.accounts[0])
const filter = ref('all')
const startDate = ref('2024-06-20')
const endDate = ref('2024-07-19')
const tabs = [{ id: 'all', label: '전체' }, { id: 'deposit', label: '입금' }, { id: 'withdrawal', label: '출금' }]
const accountTransactions = computed(() => asset.transactionsByAccount(account.value.id))
const filteredTransactions = computed(() => accountTransactions.value.filter((item) => {
  if (item.date < startDate.value || item.date > endDate.value) return false
  if (filter.value === 'deposit') return item.amount > 0
  if (filter.value === 'withdrawal') return item.amount < 0
  return true
}))
const travelRecognizedAmount = computed(() => accountTransactions.value
  .filter((item) => item.country || item.category === '여행비')
  .reduce((sum, item) => sum + Math.abs(Math.min(0, item.amount)), 0))
const groups = computed(() => filteredTransactions.value.reduce((result, item) => {
  const group = result.find((entry) => entry.date === item.date)
  if (group) group.items.push(item)
  else result.push({ date: item.date, label: item.dateLabel, items: [item] })
  return result
}, []))
</script>

<template>
  <main class="account-page">
    <header><button @click="router.back()">‹</button><h1>계좌내역 · {{ account.bank }}</h1></header>
    <AssetTicket :label="account.name" :amount="account.balance" :caption="`${account.number} · ${account.type}`" />
    <section class="travel-recognized"><small>여행 자금 인정 금액</small><b>{{ travelRecognizedAmount.toLocaleString('ko-KR') }}원</b></section>
    <section class="date-filter"><label><span>시작일</span><input v-model="startDate" type="date" :max="endDate"></label><i>~</i><label><span>종료일</span><input v-model="endDate" type="date" :min="startDate"></label><button type="button" aria-label="거래내역 캘린더" @click="router.push('/asset/transactions/calendar')"><svg width="21" height="21" viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2" stroke="currentColor" stroke-width="1.8"/><path d="M8 3V7M16 3V7M3 10H21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M8 14H8.01M12 14H12.01M16 14H16.01M8 18H8.01M12 18H12.01" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg></button></section>
    <div class="tabs"><button v-for="tab in tabs" :key="tab.id" :class="{ active: filter === tab.id }" type="button" @click="filter = tab.id">{{ tab.label }}</button></div>
    <TransactionGroups :groups="groups" :show-icons="false" @select="router.push(`/asset/transactions/${$event.id}`)" />
  </main>
</template>

<style scoped>
.account-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 30px;background:#f4f6fc;color:#10192d}header{display:grid;grid-template-columns:30px 1fr;align-items:center;margin-bottom:17px}header button{font-size:26px;text-align:left}h1{font-size:17px;font-weight:900}.travel-recognized{display:flex;align-items:center;justify-content:space-between;margin-top:12px;padding:11px 13px;border-radius:11px;background:#eaf2ff;color:#315786}.travel-recognized small{font-size:9px}.travel-recognized b{font-size:12px}.date-filter{display:grid;grid-template-columns:1fr auto 1fr 34px;align-items:end;gap:7px;margin-top:10px;padding:10px 12px;border:1px solid #dbe3ef;border-radius:12px;background:#fff}.date-filter label span{display:block;margin-bottom:5px;color:#94a3b8;font-size:8px}.date-filter input{width:100%;font-size:9px}.date-filter i{padding-bottom:2px;color:#94a3b8;font-size:9px;font-style:normal}.date-filter>button{display:grid;width:34px;height:34px;place-items:center;border-radius:9px;background:#edf4ff;color:#286dd8}.tabs{display:grid;grid-template-columns:repeat(3,1fr);margin:12px 0 16px;text-align:center}.tabs button{padding:11px 0;border-bottom:2px solid #dce3ee;color:#b0bac9;font-size:11px;font-weight:900}.tabs button.active{border-color:#3475f4;color:#3475f4}
</style>
