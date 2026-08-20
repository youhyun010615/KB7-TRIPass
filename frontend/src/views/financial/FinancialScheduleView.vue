<script setup>
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import FinancialScheduleCard from '@/components/financial/FinancialScheduleCard.vue'
import { useFinancialScheduleStore } from '@/stores/financialSchedule'

const router = useRouter()
const schedule = useFinancialScheduleStore()
const tabs = [{id:'all',label:'전체'},{id:'income',label:'입금'},{id:'expense',label:'지출'}]

function openSchedule(event) {
  router.push(event.source === 'fixed' ? `/asset/fixed-expenses/${event.sourceId}` : `/financial-schedule/${event.id}`)
}
</script>

<template><main class="page"><div class="shell">
  <header><button @click="router.back()">‹</button><h1>다가오는 금융 일정</h1></header>
  <section class="ticket"><div class="ticket-main"><span class="calendar">□</span><div><small>이번 달 예정</small><strong>{{ schedule.counts.all }}건</strong></div><nav><button v-for="tab in tabs" :key="tab.id" :class="{active:schedule.filter===tab.id}" @click="schedule.filter=tab.id">{{ tab.label }}</button></nav></div><div class="barcode">|||| ||| ||||</div></section>
  <div class="section-title"><h2>예정된 일정</h2><button @click="router.push('/financial-schedule/calendar')"><span>▣</span> 캘린더 보기</button></div>
  <section class="list"><FinancialScheduleCard v-for="event in schedule.upcomingEvents" :key="event.id" :event="event" @select="openSchedule"/><div v-if="!schedule.upcomingEvents.length" class="empty"><span>□</span><b>예정된 금융 일정이 없어요</b><small>등록한 월급과 고정지출이 이곳에 표시돼요.</small></div></section>
  <BottomNav />
</div></main></template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:auto;padding:14px 18px 105px;background:#f7f5ef}header{display:flex;align-items:center;margin-bottom:18px}header>button:first-child{width:30px;font-size:26px;text-align:left}header h1{flex:1;padding-right:30px;text-align:center;font-size:18px;font-weight:900}.ticket{position:relative;padding:14px 13px 9px;border-radius:16px;background:linear-gradient(135deg,#244d9d,#173576);color:#fff;box-shadow:0 8px 18px #17357633}.ticket:before,.ticket:after{position:absolute;top:50%;width:12px;height:12px;border-radius:50%;background:#f7f5ef;content:''}.ticket:before{left:-6px}.ticket:after{right:-6px}.ticket-main{display:grid;grid-template-columns:40px 65px 1fr;align-items:center;gap:8px}.calendar{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;background:#eaf4ff;color:#2f80ed}.ticket small,.ticket strong{display:block}.ticket small{color:#ffd26c;font-size:8px}.ticket strong{margin-top:2px;font-size:20px}.ticket nav{display:flex;justify-content:flex-end;gap:6px}.ticket nav button{padding:8px 11px;border-radius:16px;background:#fff;color:#65738a;font-size:8px}.ticket nav .active{background:#a9c6f9;color:#174494;font-weight:900}.barcode{text-align:right;color:#bdd0f7;font-size:8px;letter-spacing:1px}.section-title{display:flex;align-items:center;justify-content:space-between;margin:22px 3px 11px}.section-title h2{font-size:13px}.section-title button{display:flex;min-height:38px;align-items:center;gap:6px;padding:0 12px;border:1px solid #d6e2f5;border-radius:12px;background:#fff;color:#174494;font-size:10px;font-weight:800}.section-title button span{font-size:14px}.empty{padding:45px 20px;border:1px solid #e1e6ed;border-radius:16px;background:#fff;text-align:center}.empty span,.empty b,.empty small{display:block}.empty span{font-size:24px}.empty b{margin-top:10px;font-size:12px}.empty small{margin-top:5px;color:#94a3b8;font-size:8px}
</style>
