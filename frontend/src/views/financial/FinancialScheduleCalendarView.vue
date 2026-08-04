<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import FinancialScheduleCard from '@/components/financial/FinancialScheduleCard.vue'
import { useFinancialScheduleStore } from '@/stores/financialSchedule'

const router = useRouter()
const schedule = useFinancialScheduleStore()
const tabs = [{id:'all',label:'전체'},{id:'income',label:'입금'},{id:'expense',label:'지출'}]
const week = ['월','화','수','목','금','토','일']
const cells = computed(() => {
  const first = new Date(schedule.viewYear, schedule.viewMonth - 1, 1)
  const offset = (first.getDay() + 6) % 7
  const currentLast = new Date(schedule.viewYear, schedule.viewMonth, 0).getDate()
  const previousLast = new Date(schedule.viewYear, schedule.viewMonth - 1, 0).getDate()
  return Array.from({length:42}, (_, index) => {
    const relative = index - offset + 1
    if (relative < 1) return { day:previousLast + relative, current:false, key:`prev-${index}` }
    if (relative > currentLast) return { day:relative-currentLast, current:false, key:`next-${index}` }
    const date = `${schedule.viewYear}-${String(schedule.viewMonth).padStart(2,'0')}-${String(relative).padStart(2,'0')}`
    return { day:relative, current:true, date, key:date, events:schedule.filteredEvents.filter((item)=>item.date===date) }
  })
})
const selectedLabel = computed(() => `${Number(schedule.selectedDate.slice(5,7))}/${Number(schedule.selectedDate.slice(8,10))}`)
const selectedDateEvents = computed(() => schedule.events.filter((item) => item.date === schedule.selectedDate))

function openSchedule(event) {
  router.push(event.source === 'fixed' ? `/asset/fixed-expenses/${event.sourceId}` : `/financial-schedule/${event.id}`)
}
</script>

<template><main class="page"><div class="shell">
  <header><button @click="router.back()">‹</button><h1>금융 일정 캘린더</h1></header>
  <nav class="filters"><button v-for="tab in tabs" :key="tab.id" :class="{active:schedule.filter===tab.id}" @click="schedule.filter=tab.id">{{ tab.label }}</button></nav>
  <section class="calendar-card">
    <div class="month"><button @click="schedule.moveMonth(-1)">‹</button><h2>{{ schedule.viewYear }}년 {{ schedule.viewMonth }}월</h2><button @click="schedule.moveMonth(1)">›</button></div>
    <div class="week"><b v-for="label in week" :key="label">{{ label }}</b></div>
    <div class="grid"><button v-for="cell in cells" :key="cell.key" :disabled="!cell.current" :class="{outside:!cell.current,selected:cell.date===schedule.selectedDate,sunday:cell.current && (cells.indexOf(cell)+1)%7===0}" @click="cell.current && schedule.selectDay(cell.day)"><b>{{ cell.day }}</b><span v-if="cell.events?.length" class="dots"><i v-if="cell.events.some((item)=>item.type==='income')" class="income"></i><i v-if="cell.events.some((item)=>item.type==='expense')" class="expense"></i></span><small v-if="cell.events?.[0]">{{ cell.events[0].title }}</small></button></div>
    <div class="legend"><span><i class="income"></i>입금</span><span><i class="expense"></i>지출</span></div>
  </section>
  <section class="selected-card"><h2><strong>{{ selectedLabel }}</strong> 선택한 날짜 일정 <small>{{ selectedDateEvents.length }}건</small></h2><div class="selected-list"><FinancialScheduleCard v-for="event in selectedDateEvents" :key="event.id" :event="event" compact @select="openSchedule"/><p v-if="!selectedDateEvents.length">선택한 날짜에 예정된 일정이 없어요.</p></div></section>
  <section class="summary"><span><small>이번 달 총 예정</small><b>{{ schedule.counts.all }}건</b></span><span><small><i class="income"></i>입금</small><b>{{ schedule.counts.income }}건</b></span><span><small><i class="expense"></i>지출</small><b>{{ schedule.counts.expense }}건</b></span></section>
  <BottomNav />
</div></main></template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:auto;padding:52px 18px 105px;background:#f7f5ef}header{display:flex;align-items:center;margin-bottom:14px}header button{width:26px;font-size:26px;text-align:left}header h1{flex:1;padding-right:26px;text-align:center;font-size:18px;font-weight:900}.filters{display:flex;gap:8px;margin-bottom:13px}.filters button{min-width:72px;padding:10px;border:1px solid #dbe3ee;border-radius:18px;background:#fff;color:#64748b;font-size:9px}.filters .active{border-color:#174494;background:#174494;color:#fff;font-weight:900}.calendar-card,.selected-card,.summary{border:1px solid #dce3ec;border-radius:16px;background:#fff}.calendar-card{overflow:hidden}.month{display:flex;align-items:center;justify-content:center;gap:30px;padding:17px}.month button{font-size:24px;color:#173f8d}.month h2{font-size:17px}.week,.grid{display:grid;grid-template-columns:repeat(7,1fr)}.week{padding:0 10px 8px;border-bottom:1px solid #edf0f4}.week b{text-align:center;color:#718096;font-size:8px}.grid{padding:8px 10px 2px}.grid button{position:relative;height:48px;padding-top:6px;border-radius:9px}.grid button>b{display:block;font-size:9px}.grid .outside{color:#bdc5d1}.grid .sunday{color:#e5484d}.grid .selected{background:#ede7ff;color:#7431ff}.grid small{display:block;max-width:42px;margin:3px auto 0;overflow:hidden;color:#7d8796;font-size:5px;text-overflow:ellipsis;white-space:nowrap}.dots{display:flex;justify-content:center;gap:2px;height:4px;margin-top:3px}.dots i,.legend i,.summary i{display:inline-block;width:5px;height:5px;border-radius:50%}.income{background:#2f80ed}.expense{background:#d96c5f}.legend{display:flex;justify-content:center;gap:18px;padding:9px;border-top:1px solid #edf0f4;color:#7c8797;font-size:7px}.legend i,.summary i{margin-right:4px}.selected-card{margin-top:13px;padding:13px}.selected-card h2{padding-bottom:11px;border-bottom:1px solid #edf0f4;font-size:11px}.selected-card h2 strong{margin-right:6px;color:#7431ff;font-size:19px}.selected-card h2 small{float:right;color:#94a3b8}.selected-list{padding-top:10px}.selected-list>p{padding:30px 0;text-align:center;color:#94a3b8;font-size:9px}.summary{display:grid;grid-template-columns:repeat(3,1fr);margin-top:10px;padding:13px 8px}.summary span{text-align:center}.summary span+span{border-left:1px solid #e7ebf0}.summary small,.summary b{display:block}.summary small{color:#8b96a6;font-size:7px}.summary b{margin-top:5px;font-size:16px}
</style>
