<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AssetTicket from '@/components/asset/AssetTicket.vue'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore } from '@/stores/asset'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const asset = useAssetStore()
const isReal = route.query.isReal === 'true'
const filter = ref('all')
const today = new Date().toISOString().slice(0, 10)
const threeMonthsAgo = (() => { const d = new Date(); d.setMonth(d.getMonth() - 3); return d.toISOString().slice(0, 10) })()
const startDate = ref(isReal ? threeMonthsAgo : '2024-06-20')
const endDate = ref(isReal ? today : '2024-07-19')
const tabs = [{ id: 'all', label: '전체' }, { id: 'deposit', label: '입금' }, { id: 'withdrawal', label: '출금' }]
const realAccount = ref({ name: '', number: '', type: '', bank: '', balance: 0 })
const realTransactions = ref([])

const DAYS = ['일', '월', '화', '수', '목', '금', '토']

async function fetchRealTransactions() {
  try {
    const params = { startDate: startDate.value, endDate: endDate.value }
    if (filter.value !== 'all') params.type = filter.value.toUpperCase()
    const res = await api.get(`/accounts/${route.params.accountId}/transactions`, { params })
    const data = res.data.data
    realAccount.value.balance = data.balance
    realTransactions.value = data.transactions ?? []
  } catch (e) {
    console.error('거래내역 조회 실패', e)
  }
}

const loading = ref(false)
const syncing = ref(false)

async function fetchRealTransactionsWithLoading() {
  loading.value = true
  await fetchRealTransactions()
  loading.value = false
}

async function syncTransactions() {
  syncing.value = true
  try {
    await api.post('/accounts/transactions', {
      accountId: Number(route.params.accountId),
      startDate: startDate.value,
      endDate: endDate.value,
    })
    await fetchRealTransactions()
  } catch (e) {
    console.error('거래내역 동기화 실패', e)
  } finally {
    syncing.value = false
  }
}

onMounted(async () => {
  if (!isReal) return
  realAccount.value = {
    name: route.query.name ?? '',
    number: route.query.number ?? '',
    type: route.query.type ?? '',
    bank: (route.query.name ?? '').split(' ')[0],
    balance: 0,
  }
  loading.value = true
  await fetchRealTransactions()
  if (realTransactions.value.length === 0) await syncTransactions()
  loading.value = false
})

watch([startDate, endDate, filter], () => {
  if (isReal) fetchRealTransactions()
})

const account = computed(() => isReal ? realAccount.value : (asset.getAccount(route.params.accountId) || asset.accounts[0]))

const accountTransactions = computed(() => isReal ? [] : asset.transactionsByAccount(account.value?.id))
const travelRecognizedAmount = computed(() => accountTransactions.value
  .filter((item) => item.country || item.category === '여행비')
  .reduce((sum, item) => sum + Math.abs(Math.min(0, item.amount)), 0))

const groups = computed(() => {
  if (isReal) {
    return realTransactions.value.reduce((result, t) => {
      const [y, mo, d] = Array.isArray(t.transactionDate) ? t.transactionDate : t.transactionDate.split('-').map(Number)
      const date = `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`
      const jsDate = new Date(y, mo - 1, d)
      const label = `${date.replaceAll('-', '.')} (${DAYS[jsDate.getDay()]})`
      const [h = 0, m = 0] = Array.isArray(t.transactionTime) ? t.transactionTime : (t.transactionTime ?? '00:00').split(':').map(Number)
      const time = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
      const amount = t.transactionType === 'DEPOSIT' ? Number(t.amount) : -Number(t.amount)
      const item = {
        id: t.id,
        merchant: t.merchantName ?? '(내용없음)',
        category: '기타',
        method: route.query.name ?? '',
        amount,
        time,
        dateLabel: label,
        balanceAfter: Number(t.balanceAfter ?? 0),
        memo: t.memo ?? '',
        isReal: true,
      }
      const group = result.find((g) => g.date === date)
      if (group) group.items.push(item)
      else result.push({ date, label, items: [item] })
      return result
    }, [])
  }
  return accountTransactions.value
    .filter((item) => {
      if (item.date < startDate.value || item.date > endDate.value) return false
      if (filter.value === 'deposit') return item.amount > 0
      if (filter.value === 'withdrawal') return item.amount < 0
      return true
    })
    .reduce((result, item) => {
      const group = result.find((entry) => entry.date === item.date)
      if (group) group.items.push(item)
      else result.push({ date: item.date, label: item.dateLabel, items: [item] })
      return result
    }, [])
})
</script>

<template>
  <main class="account-page">
    <header><button @click="router.back()">‹</button><h1>계좌내역 · {{ account.bank }}</h1></header>
    <AssetTicket :label="account.name" :amount="account.balance" :caption="`${account.number} · ${account.type}`" />
    <section class="travel-recognized"><small>여행 자금 인정 금액</small><b>{{ travelRecognizedAmount.toLocaleString('ko-KR') }}원</b></section>
    <section class="date-filter"><label><span>시작일</span><input v-model="startDate" type="date" :max="endDate"></label><i>~</i><label><span>종료일</span><input v-model="endDate" type="date" :min="startDate"></label><button type="button" aria-label="거래내역 캘린더" @click="router.push('/asset/transactions/calendar')"><svg width="21" height="21" viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2" stroke="currentColor" stroke-width="1.8"/><path d="M8 3V7M16 3V7M3 10H21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M8 14H8.01M12 14H12.01M16 14H16.01M8 18H8.01M12 18H12.01" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg></button></section>
    <div class="tabs"><button v-for="tab in tabs" :key="tab.id" :class="{ active: filter === tab.id }" type="button" @click="filter = tab.id">{{ tab.label }}</button></div>
    <TransactionGroups :groups="groups" :show-icons="false" :loading="loading" @select="isReal ? router.push({ path: `/asset/transactions/${$event.id}`, state: { item: $event } }) : router.push(`/asset/transactions/${$event.id}`)" />
  </main>
</template>

<style scoped>
.account-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 30px;background:#f4f6fc;color:#10192d}header{display:grid;grid-template-columns:30px 1fr;align-items:center;margin-bottom:17px}header button{font-size:26px;text-align:left}h1{font-size:17px;font-weight:900}.travel-recognized{display:flex;align-items:center;justify-content:space-between;margin-top:12px;padding:11px 13px;border-radius:11px;background:#eaf2ff;color:#315786}.travel-recognized small{font-size:9px}.travel-recognized b{font-size:12px}.date-filter{display:grid;grid-template-columns:1fr auto 1fr 34px;align-items:end;gap:7px;margin-top:10px;padding:10px 12px;border:1px solid #dbe3ef;border-radius:12px;background:#fff}.date-filter label span{display:block;margin-bottom:5px;color:#94a3b8;font-size:8px}.date-filter input{width:100%;font-size:9px}.date-filter i{padding-bottom:2px;color:#94a3b8;font-size:9px;font-style:normal}.date-filter>button{display:grid;width:34px;height:34px;place-items:center;border-radius:9px;background:#edf4ff;color:#286dd8}.tabs{display:grid;grid-template-columns:repeat(3,1fr);margin:12px 0 16px;text-align:center}.tabs button{padding:11px 0;border-bottom:2px solid #dce3ee;color:#b0bac9;font-size:11px;font-weight:900}.tabs button.active{border-color:#3475f4;color:#3475f4}.sync-btn{display:block;width:100%;margin-bottom:12px;padding:10px;border-radius:10px;background:#edf4ff;color:#1a56db;font-size:12px;font-weight:700}.sync-btn:disabled{opacity:.6}
</style>
