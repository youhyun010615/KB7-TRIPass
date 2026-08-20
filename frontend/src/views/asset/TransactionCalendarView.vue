<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore } from '@/stores/asset'
import api from '@/api'

const router = useRouter()
const asset = useAssetStore()
const filter = ref('all')
const tabs = [{ id: 'all', label: '전체' }, { id: 'deposit', label: '입금' }, { id: 'withdrawal', label: '지출' }]
const initialDate = new Date(`${asset.selectedDate}T00:00:00`)
const displayYear = ref(initialDate.getFullYear())
const displayMonth = ref(initialDate.getMonth() + 1)
const monthKey = computed(() => `${displayYear.value}-${String(displayMonth.value).padStart(2, '0')}`)
const monthLabel = computed(() => `${displayYear.value}년 ${displayMonth.value}월`)
const calendarDays = computed(() => {
  const firstDay = new Date(displayYear.value, displayMonth.value - 1, 1).getDay()
  const lastDate = new Date(displayYear.value, displayMonth.value, 0).getDate()
  return [...Array(firstDay).fill(null), ...Array.from({ length: lastDate }, (_, index) => index + 1)]
})

const loading = ref(false)
const calendarData = ref([])
const realTransactions = ref([])
const accountMap = ref({})
const DAYS = ['일', '월', '화', '수', '목', '금', '토']

async function fetchCalendar() {
  try {
    const params = { year: displayYear.value, month: displayMonth.value }
    if (filter.value !== 'all') params.type = filter.value.toUpperCase()
    const res = await api.get('/transactions/calendar', { params })
    calendarData.value = res.data.data ?? []
  } catch (e) { console.error('캘린더 조회 실패', e) }
}

function monthRange() {
  const y = displayYear.value
  const m = String(displayMonth.value).padStart(2, '0')
  const lastDay = new Date(y, displayMonth.value, 0).getDate()
  return { startDate: `${y}-${m}-01`, endDate: `${y}-${m}-${String(lastDay).padStart(2, '0')}` }
}

async function fetchTransactions() {
  try {
    const res = await api.get('/transactions', { params: monthRange() })
    realTransactions.value = res.data.data ?? []
  } catch (e) { console.error('거래내역 조회 실패', e) }
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchCalendar(),
      fetchTransactions(),
      api.get('/accounts').then(res => {
        accountMap.value = Object.fromEntries((res.data.data ?? []).map(a => [a.id, a.accountName]))
      }),
    ])
  } catch (e) { console.error('데이터 조회 실패', e) }
  finally { loading.value = false }
})

watch([displayYear, displayMonth, filter], fetchCalendar)
watch([displayYear, displayMonth], fetchTransactions)

const dailyTotals = computed(() => {
  const map = {}
  calendarData.value.forEach(item => {
    const day = Number(item.date.slice(-2))
    const net = Number(item.totalDeposit || 0) - Number(item.totalWithdrawal || 0)
    map[day] = (map[day] || 0) + net
  })
  asset.transactions.forEach(item => {
    if (!item.date.startsWith(monthKey.value)) return
    if (filter.value === 'deposit' && item.amount <= 0) return
    if (filter.value === 'withdrawal' && item.amount >= 0) return
    const day = Number(item.date.slice(-2))
    map[day] = (map[day] || 0) + item.amount
  })
  return map
})

const monthlyGroups = computed(() => {
  const items = []
  asset.transactions.forEach(item => {
    if (!item.date.startsWith(monthKey.value)) return
    if (filter.value === 'deposit' && item.amount <= 0) return
    if (filter.value === 'withdrawal' && item.amount >= 0) return
    items.push({ ...item, _isReal: false })
  })
  realTransactions.value.forEach(t => {
    const [y, mo, d] = Array.isArray(t.transactionDate) ? t.transactionDate : t.transactionDate.split('-').map(Number)
    const date = `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    if (!date.startsWith(monthKey.value)) return
    const amount = t.transactionType === 'DEPOSIT' ? Number(t.amount) : -Number(t.amount)
    if (filter.value === 'deposit' && amount <= 0) return
    if (filter.value === 'withdrawal' && amount >= 0) return
    const jsDate = new Date(y, mo - 1, d)
    const label = `${date.replaceAll('-', '.')} (${DAYS[jsDate.getDay()]})`
    const [h = 0, m = 0] = Array.isArray(t.transactionTime) ? t.transactionTime : (t.transactionTime ?? '00:00').split(':').map(Number)
    items.push({
      id: t.id, date, dateLabel: label,
      merchant: t.merchantName ?? '(내용없음)',
      category: '기타', method: accountMap.value[t.accountId] ?? '',
      amount, time: `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`,
      balanceAfter: Number(t.balanceAfter ?? 0), memo: t.memo ?? '',
      _isReal: true,
    })
  })
  return items
    .sort((a, b) => b.date.localeCompare(a.date) || (b.time ?? '').localeCompare(a.time ?? ''))
    .reduce((groups, item) => {
      const group = groups.find(e => e.date === item.date)
      if (group) group.items.push(item)
      else groups.push({ date: item.date, label: item.dateLabel, items: [item] })
      return groups
    }, [])
})

function selectDay(day) { asset.selectedDate = `${monthKey.value}-${String(day).padStart(2, '0')}` }
function moveMonth(offset) {
  const next = new Date(displayYear.value, displayMonth.value - 1 + offset, 1)
  displayYear.value = next.getFullYear()
  displayMonth.value = next.getMonth() + 1
  asset.selectedDate = `${monthKey.value}-01`
}
</script>

<template>
  <main class="calendar-page">
    <header><button @click="router.back()">‹</button><h1>거래내역 캘린더</h1></header>
    <nav class="filter-tabs"><button v-for="tab in tabs" :key="tab.id" :class="{ active: filter === tab.id }" type="button" @click="filter = tab.id">{{ tab.label }}</button></nav>
    <div class="month"><button type="button" aria-label="이전 달" @click="moveMonth(-1)">‹</button><span class="month-center"><b>{{ monthLabel }}</b><span class="legend-inline"><i class="withdrawal-dot"></i>지출<i class="deposit-dot"></i>입금</span></span><button type="button" aria-label="다음 달" @click="moveMonth(1)">›</button></div>
    <section class="calendar">
      <div class="week"><b v-for="label in ['일','월','화','수','목','금','토']" :key="label">{{ label }}</b></div>
      <div class="days">
        <span v-for="(day, index) in calendarDays" :key="`${monthKey}-${index}`" class="day-cell">
          <button v-if="day" :class="{ selected: asset.selectedDate === `${monthKey}-${String(day).padStart(2,'0')}` }" @click="selectDay(day)">
            <b>{{ day }}</b><small v-if="dailyTotals[day]" :class="dailyTotals[day] > 0 ? 'deposit' : 'withdrawal'">{{ dailyTotals[day] > 0 ? '+' : '-' }}{{ Math.round(Math.abs(dailyTotals[day])/10000) }}만</small>
          </button>
        </span>
      </div>
    </section>
    <TransactionGroups :groups="monthlyGroups" :loading="loading" @select="$event._isReal ? router.push({ path: `/asset/transactions/${$event.id}`, state: { item: $event } }) : router.push(`/asset/transactions/${$event.id}`)" />
  </main>
</template>

<style scoped>
.calendar-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:14px 20px 30px;background:#f4f6fc;color:#10192d}header{display:grid;grid-template-columns:30px 1fr;align-items:center}header button{font-size:26px;text-align:left}h1{font-size:18px;font-weight:900}.filter-tabs{display:grid;grid-template-columns:repeat(3,1fr);margin-top:20px}.filter-tabs button{padding:11px 0;border-bottom:2px solid #dce3ee;color:#a4afbf;font-size:11px;font-weight:900}.filter-tabs button.active{border-color:#3475f4;color:#3475f4}.month{display:flex;align-items:center;justify-content:space-between;margin:18px 0 15px;padding:12px 16px;border-radius:14px;background:#172f6b;color:#fff}.month-center{display:flex;flex-direction:column;align-items:center;gap:5px}.month b{font-size:16px;color:#fff}.month button{font-size:26px;color:#fff}.legend-inline{display:flex;align-items:center;gap:8px;font-size:9px;color:#c8d6ff}.legend-inline i{width:7px;height:7px;border-radius:50%;display:inline-block}.withdrawal-dot{background:#ed4545}.deposit-dot{background:#10a88d}.calendar{margin-bottom:18px;padding:14px 10px;border:1px solid #d8e0ec;border-radius:16px;background:white}.week,.days{display:grid;grid-template-columns:repeat(7,1fr)}.week b{text-align:center;color:#94a3b8;font-size:9px}.day-cell{height:51px}.days button{width:100%;height:51px;padding-top:9px;border-radius:8px}.days b,.days small{display:block;font-size:10px}.days small{margin-top:4px;font-size:7px}.days .selected{background:#172f6b;border-radius:10px;color:#fff}.days .selected small{color:#fff}.deposit{color:#10a88d}.withdrawal{color:#ed4545}
</style>
