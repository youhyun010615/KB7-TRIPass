<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore } from '@/stores/asset'
import api from '@/api'

const router = useRouter()
const asset = useAssetStore()
const tabs = [{ id: 'all', label: '전체' }, { id: 'deposit', label: '입금' }, { id: 'withdrawal', label: '출금' }]
const filter = ref('all')
const today = new Date().toISOString().slice(0, 10)
const threeMonthsAgo = (() => { const d = new Date(); d.setMonth(d.getMonth() - 3); return d.toISOString().slice(0, 10) })()
const startDate = ref(threeMonthsAgo)
const endDate = ref(today)
const loading = ref(false)
const realTransactions = ref([])
const accountMap = ref({})
const DAYS = ['일', '월', '화', '수', '목', '금', '토']

onMounted(async () => {
  loading.value = true
  try {
    const [txRes, accRes] = await Promise.all([
      api.get('/transactions'),
      api.get('/accounts'),
    ])
    realTransactions.value = txRes.data.data ?? []
    const accounts = accRes.data.data ?? []
    accountMap.value = Object.fromEntries(accounts.map(a => [a.id, a.accountName]))
  } catch (e) {
    console.error('거래내역 조회 실패', e)
  } finally {
    loading.value = false
  }
})

const groups = computed(() => {
  const allItems = []

  // mock 거래내역
  asset.transactions.forEach((item) => {
    if (item.date < startDate.value || item.date > endDate.value) return
    if (filter.value === 'deposit' && item.amount <= 0) return
    if (filter.value === 'withdrawal' && item.amount >= 0) return
    allItems.push({ ...item, _isReal: false })
  })

  // 실제 거래내역
  realTransactions.value.forEach((t) => {
    const [y, mo, d] = Array.isArray(t.transactionDate) ? t.transactionDate : t.transactionDate.split('-').map(Number)
    const date = `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    if (date < startDate.value || date > endDate.value) return
    const amount = t.transactionType === 'DEPOSIT' ? Number(t.amount) : -Number(t.amount)
    if (filter.value === 'deposit' && amount <= 0) return
    if (filter.value === 'withdrawal' && amount >= 0) return
    const jsDate = new Date(y, mo - 1, d)
    const label = `${date.replaceAll('-', '.')} (${DAYS[jsDate.getDay()]})`
    const [h = 0, m = 0] = Array.isArray(t.transactionTime) ? t.transactionTime : (t.transactionTime ?? '00:00').split(':').map(Number)
    allItems.push({
      id: t.id, date, dateLabel: label,
      merchant: t.merchantName ?? '(내용없음)',
      category: '기타', method: accountMap.value[t.accountId] ?? '',
      amount, time: `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`,
      balanceAfter: Number(t.balanceAfter ?? 0), memo: t.memo ?? '',
      _isReal: true,
    })
  })

  allItems.sort((a, b) => b.date.localeCompare(a.date) || b.time?.localeCompare(a.time ?? '') || 0)

  return allItems.reduce((result, item) => {
    const group = result.find((g) => g.date === item.date)
    if (group) group.items.push(item)
    else result.push({ date: item.date, label: item.dateLabel, items: [item] })
    return result
  }, [])
})

const totalCount = computed(() => groups.value.reduce((sum, g) => sum + g.items.length, 0))
</script>

<template>
  <main class="list-page">
    <header><button type="button" @click="router.back()">‹</button><h1>전체 계좌 거래내역</h1><span /></header>
    <section class="summary"><small>연동 계좌 {{ asset.accounts.length }}개</small><b>총 {{ totalCount }}건의 거래내역</b></section>
    <section class="date-filter"><label><span>시작일</span><input v-model="startDate" type="date" :max="endDate"></label><i>~</i><label><span>종료일</span><input v-model="endDate" type="date" :min="startDate"></label><button type="button" aria-label="거래내역 캘린더" @click="router.push('/asset/transactions/calendar')"><svg width="21" height="21" viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2" stroke="currentColor" stroke-width="1.8"/><path d="M8 3V7M16 3V7M3 10H21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M8 14H8.01M12 14H12.01M16 14H16.01M8 18H8.01M12 18H12.01" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg></button></section>
    <nav class="filter-tabs"><button v-for="tab in tabs" :key="tab.id" :class="{ active: filter === tab.id }" @click="filter = tab.id">{{ tab.label }}</button></nav>
    <TransactionGroups :groups="groups" :show-icons="false" :loading="loading" @select="$event._isReal ? router.push({ path: `/asset/transactions/${$event.id}`, state: { item: $event } }) : router.push(`/asset/transactions/${$event.id}`)" />
    <BottomNav />
  </main>
</template>

<style scoped>
.list-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 100px;background:#f4f6fc;color:#10192d}.list-page>header{display:grid;grid-template-columns:30px 1fr 30px;align-items:center;margin-bottom:16px}.list-page>header button{font-size:24px;text-align:left}.list-page>header h1{text-align:center;font-size:18px;font-weight:900}.summary{padding:15px;border-radius:14px;background:linear-gradient(135deg,#173f8d,#2866bd);color:#fff}.summary small,.summary b{display:block}.summary small{color:#cbdcf7;font-size:9px}.summary b{margin-top:5px;font-size:14px}.date-filter{display:grid;grid-template-columns:1fr auto 1fr 34px;align-items:end;gap:7px;margin-top:10px;padding:10px 12px;border:1px solid #dbe3ef;border-radius:12px;background:#fff}.date-filter label span{display:block;margin-bottom:5px;color:#94a3b8;font-size:8px}.date-filter input{width:100%;font-size:9px}.date-filter i{padding-bottom:2px;color:#94a3b8;font-size:9px;font-style:normal}.date-filter>button{display:grid;width:34px;height:34px;place-items:center;border-radius:9px;background:#edf4ff;color:#286dd8}.filter-tabs{display:grid;grid-template-columns:repeat(3,1fr);margin:12px 0 16px}.filter-tabs button{padding:11px 0;border-bottom:2px solid #dce3ee;color:#b0bac9;font-size:11px;font-weight:900}.filter-tabs .active{border-color:#3475f4;color:#3475f4}
</style>
