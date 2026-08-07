<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore } from '@/stores/asset'

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
const visibleTransactions = computed(() => asset.transactions.filter((item) => {
  if (filter.value === 'deposit') return item.amount > 0
  if (filter.value === 'withdrawal') return item.amount < 0
  return true
}))
const dailyTotals = computed(() => visibleTransactions.value.filter((item) => item.date.startsWith(monthKey.value)).reduce((map, item) => {
  const day = Number(item.date.slice(-2))
  map[day] = (map[day] || 0) + item.amount
  return map
}, {}))
const monthlyGroups = computed(() => visibleTransactions.value
  .filter((item) => item.date.startsWith(monthKey.value))
  .sort((a, b) => b.date.localeCompare(a.date) || b.time.localeCompare(a.time))
  .reduce((groups, item) => {
    const group = groups.find((entry) => entry.date === item.date)
    if (group) group.items.push(item)
    else groups.push({ date: item.date, label: item.dateLabel, items: [item] })
    return groups
  }, []))
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
    <div class="legend"><span><i class="withdrawal-dot"></i>지출</span><span><i class="deposit-dot"></i>입금</span></div>
    <div class="month"><button type="button" aria-label="이전 달" @click="moveMonth(-1)">‹</button><b>{{ monthLabel }}</b><button type="button" aria-label="다음 달" @click="moveMonth(1)">›</button></div>
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
    <TransactionGroups :groups="monthlyGroups" @select="router.push(`/asset/transactions/${$event.id}`)" />
  </main>
</template>

<style scoped>
.calendar-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 30px;background:#f4f6fc;color:#10192d}header{display:grid;grid-template-columns:30px 1fr;align-items:center}header button{font-size:26px;text-align:left}h1{font-size:18px;font-weight:900}.filter-tabs{display:grid;grid-template-columns:repeat(3,1fr);margin-top:20px}.filter-tabs button{padding:11px 0;border-bottom:2px solid #dce3ee;color:#a4afbf;font-size:11px;font-weight:900}.filter-tabs button.active{border-color:#3475f4;color:#3475f4}.legend{display:flex;justify-content:flex-end;gap:12px;margin-top:12px;color:#64748b;font-size:9px}.legend span{display:flex;align-items:center;gap:5px}.legend i{width:7px;height:7px;border-radius:50%}.withdrawal-dot{background:#ed4545}.deposit-dot{background:#10a88d}.month{display:flex;align-items:center;justify-content:space-between;margin:18px 5px 15px}.month b{font-size:18px}.month button{font-size:26px}.calendar{margin-bottom:18px;padding:14px 10px;border:1px solid #d8e0ec;border-radius:16px;background:white}.week,.days{display:grid;grid-template-columns:repeat(7,1fr)}.week b{text-align:center;color:#94a3b8;font-size:9px}.day-cell{height:51px}.days button{width:100%;height:51px;padding-top:9px;border-radius:8px}.days b,.days small{display:block;font-size:10px}.days small{margin-top:4px;font-size:7px}.days .selected{background:#edf3ff;color:#3475f4}.deposit{color:#10a88d}.withdrawal{color:#ed4545}
</style>
