<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore } from '@/stores/asset'

const router = useRouter()
const asset = useAssetStore()
const days = Array.from({ length: 31 }, (_, index) => index + 1)
const dailyTotals = computed(() => asset.transactions.reduce((map, item) => {
  const day = Number(item.date.slice(-2))
  map[day] = (map[day] || 0) + item.amount
  return map
}, {}))
const selectedGroups = computed(() => {
  const items = asset.transactions.filter((item) => item.date === asset.selectedDate)
  return items.length ? [{ date: asset.selectedDate, label: items[0].dateLabel, items }] : []
})
function selectDay(day) { asset.selectedDate = `2024-07-${String(day).padStart(2, '0')}` }
</script>

<template>
  <main class="calendar-page">
    <header><button @click="router.back()">‹</button><h1>거래내역 캘린더</h1></header>
    <div class="month"><button>‹</button><b>2024년 7월</b><button>›</button></div>
    <section class="calendar">
      <div class="week"><b v-for="label in ['일','월','화','수','목','금','토']" :key="label">{{ label }}</b></div>
      <div class="days">
        <button v-for="day in days" :key="day" :class="{ selected: asset.selectedDate.endsWith(String(day).padStart(2,'0')) }" @click="selectDay(day)">
          <b>{{ day }}</b><small v-if="dailyTotals[day]" :class="dailyTotals[day] > 0 ? 'deposit' : 'withdrawal'">{{ dailyTotals[day] > 0 ? '+' : '-' }}{{ Math.round(Math.abs(dailyTotals[day])/10000) }}만</small>
        </button>
      </div>
    </section>
    <TransactionGroups :groups="selectedGroups" @select="router.push(`/asset/transactions/${$event.id}`)" />
  </main>
</template>

<style scoped>
.calendar-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 30px;background:#f4f6fc;color:#10192d}header{display:grid;grid-template-columns:30px 1fr;align-items:center}header button{font-size:26px;text-align:left}h1{font-size:18px;font-weight:900}.month{display:flex;align-items:center;justify-content:space-between;margin:24px 5px 15px}.month b{font-size:18px}.month button{font-size:26px}.calendar{margin-bottom:18px;padding:14px 10px;border:1px solid #d8e0ec;border-radius:16px;background:white}.week,.days{display:grid;grid-template-columns:repeat(7,1fr)}.week b{text-align:center;color:#94a3b8;font-size:9px}.days button{height:51px;padding-top:9px;border-radius:8px}.days b,.days small{display:block;font-size:10px}.days small{margin-top:4px;font-size:7px}.days .selected{background:#edf3ff;color:#3475f4}.deposit{color:#10a88d}.withdrawal{color:#ed4545}
</style>
