<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import ScheduleCard from '@/components/schedule/ScheduleCard.vue'
import { useTravelScheduleStore } from '@/stores/travelSchedule'

const router = useRouter()
const store = useTravelScheduleStore()
const todayModalOpen = ref(false)
const todaySchedules = computed(() => store.sortedSchedules.filter(item => item.date === store.demoToday))
const alertedTodaySchedules = computed(() => todaySchedules.value.filter(item => store.normalizeSchedule(item).notificationTriggered || item.alert))
const upcomingGroups = computed(() => {
  const groups = new Map()
  store.sortedSchedules.filter(item => item.date > store.demoToday).forEach(item => {
    if (!groups.has(item.date)) groups.set(item.date, [])
    groups.get(item.date).push(item)
  })
  return [...groups.entries()].map(([date, items]) => ({ date, items }))
})
const dateLabel = date => new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short' }).format(new Date(`${date}T00:00:00`))
const travelDays = computed(() => Math.max(1, Math.floor((new Date(`${store.travelEnd}T00:00:00`) - new Date(`${store.travelStart}T00:00:00`)) / 86_400_000) + 1))
const openDetail = id => router.push(`/schedule/${id}`)
</script>

<template>
  <main class="schedule-page">
    <header class="page-header"><button type="button" @click="router.back()">‹</button><h1>여행 일정 목록</h1><span /></header>
    <section class="period-card"><span>▣</span><b>{{ store.travelStart }} ~ {{ store.travelEnd }}</b><em>{{ travelDays }}일</em></section>

    <section v-if="todaySchedules.length" class="today-ticket" role="button" tabindex="0" @click="todayModalOpen = true" @keydown.enter="todayModalOpen = true">
      <div class="ticket-head"><b>오늘 일정</b><small>{{ store.demoToday.replaceAll('-', '.') }}</small></div>
      <div class="cut"><i/><span/><i/></div>
      <p v-if="alertedTodaySchedules.length">1시간 전 알림이 도착한 일정이에요.</p>
      <p v-else>아직 알림이 도착한 일정이 없어요.</p>
      <div class="today-preview-list"><ScheduleCard v-for="item in alertedTodaySchedules" :key="item.id" :schedule="item" compact @detail="openDetail" /></div>
      <div class="all-link">오늘 일정 전체 보기 <span>›</span></div>
      <div class="cut bottom"><i/><span/><i/></div><div class="barcode">||||||||||||||||||||</div>
    </section>
    <section v-else class="empty-ticket"><span>▣</span><b>등록된 오늘 일정이 없어요</b><small>새로운 여행 일정을 추가해 보세요.</small></section>

    <section class="upcoming-card">
      <div class="section-title"><h2>다가오는 여행 일정</h2><span>총 {{ store.sortedSchedules.length }}개</span></div>
      <div v-for="group in upcomingGroups" :key="group.date" class="date-group">
        <h3>{{ dateLabel(group.date) }}</h3>
        <ScheduleCard v-for="item in group.items" :key="item.id" :schedule="item" @detail="openDetail" />
      </div>
      <p v-if="!upcomingGroups.length" class="empty">다가오는 여행 일정이 없어요.</p>
    </section>

    <button class="add-button" type="button" @click="router.push({ path: '/schedule/new', query: router.currentRoute.value.query })"><span>＋</span>새 여행 일정 추가하기</button>
    <BottomNav />

    <Teleport to="body"><Transition name="modal"><div v-if="todayModalOpen" class="modal-wrap" role="dialog" aria-modal="true" aria-label="오늘 일정 전체 보기"><button class="modal-backdrop" aria-label="닫기" @click="todayModalOpen = false"/><section class="today-modal"><header><div><small>{{ store.demoToday.replaceAll('-', '.') }}</small><h2>오늘 일정 전체</h2></div><button type="button" aria-label="닫기" @click="todayModalOpen = false">×</button></header><div class="modal-list"><ScheduleCard v-for="item in todaySchedules" :key="item.id" :schedule="item" @detail="openDetail" /></div></section></div></Transition></Teleport>
  </main>
</template>

<style scoped>
.schedule-page{min-height:100vh;padding:0 20px 150px;background:#f8f6f1;color:#10192d}.page-header{display:grid;grid-template-columns:36px 1fr 36px;align-items:end;height:92px;padding-bottom:17px}.page-header button{font-size:28px;text-align:left}.page-header h1{text-align:center;font-size:21px;font-weight:900}.period-card{display:grid;grid-template-columns:30px 1fr auto;align-items:center;padding:14px 16px;border:1px solid #dce4ef;border-radius:14px;background:#fff;box-shadow:0 3px 10px #1525470c}.period-card span{color:#2474e8;font-size:18px}.period-card b{font-size:13px}.period-card em{padding:7px 13px;border-radius:13px;background:#eaf3ff;color:#246dd7;font-size:11px;font-style:normal}.today-ticket{display:block;width:100%;margin-top:18px;overflow:hidden;border-radius:22px;background:linear-gradient(135deg,#0c337f,#1c61ca);color:#fff;box-shadow:0 8px 18px #173f8d24;text-align:left}.ticket-head{display:flex;justify-content:space-between;padding:18px 22px 15px}.ticket-head b{font-size:20px}.ticket-head small{color:#ffffff99;font-size:11px}.cut{display:grid;grid-template-columns:15px 1fr 15px;align-items:center;height:0}.cut i{width:20px;height:20px;border-radius:50%;background:#f8f6f1}.cut i:first-child{transform:translateX(-10px)}.cut i:last-child{transform:translateX(5px)}.cut span{border-top:1px dashed #ffffff80}.today-ticket>p{padding:18px 22px 8px;color:#dbe8ff;font-size:11px}.today-ticket :deep(.schedule-card){width:calc(100% - 36px);margin:10px 18px}.all-link{padding:12px 22px 4px;text-align:right;color:#e1ecff;font-size:11px;font-weight:900}.all-link span{font-size:18px}.cut.bottom{margin-top:12px}.barcode{height:34px;padding:9px 22px;text-align:right;letter-spacing:-1px}.empty-ticket{display:flex;min-height:150px;flex-direction:column;align-items:center;justify-content:center;margin-top:18px;border-radius:22px;background:#174596;color:#fff}.empty-ticket b{margin-top:10px;font-size:13px}.empty-ticket small{margin-top:5px;color:#d7e4fb;font-size:9px}.upcoming-card{margin-top:18px;padding:20px;border:1px solid #dce4ee;border-radius:22px;background:#fff}.section-title{display:flex;align-items:center;justify-content:space-between}.section-title h2{font-size:20px;font-weight:900}.section-title span{color:#94a3b8;font-size:11px}.date-group h3{margin:24px 4px 11px;color:#286ee0;font-size:14px}.date-group :deep(.schedule-card){margin-top:10px}.empty{padding:40px;text-align:center;color:#94a3b8;font-size:10px}.add-button{position:fixed;right:max(calc((100vw - 390px)/2 + 20px),20px);bottom:78px;left:max(calc((100vw - 390px)/2 + 20px),20px);z-index:40;height:56px;border-radius:15px;background:#18489f;color:#fff;font-size:14px;font-weight:900;box-shadow:0 8px 18px #173f8d2e}.add-button span{float:left;margin-left:16px;font-size:20px}.modal-wrap{position:fixed;inset:0;z-index:100;display:flex;align-items:flex-end;justify-content:center}.modal-backdrop{position:absolute;inset:0;background:#10182780}.today-modal{position:relative;width:min(100%,430px);max-height:82vh;padding:20px;border-radius:24px 24px 0 0;background:#f8f6f1;box-shadow:0 -10px 35px #1018272e}.today-modal>header{display:flex;align-items:center;justify-content:space-between;padding:2px 2px 16px}.today-modal small{color:#276ed6;font-size:10px;font-weight:800}.today-modal h2{margin-top:4px;font-size:21px}.today-modal header>button{font-size:28px;color:#657184}.modal-list{display:grid;gap:10px;max-height:64vh;overflow:auto;padding-bottom:20px}.modal-enter-active,.modal-leave-active{transition:opacity .2s}.modal-enter-from,.modal-leave-to{opacity:0}
.today-preview-list{max-height:224px;overflow-y:auto;overscroll-behavior:contain;scrollbar-width:thin;scrollbar-color:#ffffff70 transparent}.today-preview-list :deep(.schedule-card){width:calc(100% - 36px);margin:10px 18px}.modal-list{overflow-y:auto;overscroll-behavior:contain;scrollbar-width:thin;scrollbar-color:#9eabc0 transparent}
</style>
