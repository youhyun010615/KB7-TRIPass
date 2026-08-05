<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelScheduleStore } from '@/stores/travelSchedule'

const router = useRouter()
const store = useTravelScheduleStore()
const todaySchedules = computed(() => store.sortedSchedules.filter(item => item.date === store.demoToday))
const upcomingGroups = computed(() => {
  const groups = new Map()
  store.sortedSchedules.filter(item => item.date > store.demoToday).forEach(item => {
    if (!groups.has(item.date)) groups.set(item.date, [])
    groups.get(item.date).push(item)
  })
  return [...groups.entries()].map(([date, items]) => ({ date, items }))
})
const country = code => store.countries.find(item => item.code === code)
const dateLabel = date => new Intl.DateTimeFormat('ko-KR', { year:'numeric', month:'2-digit', day:'2-digit', weekday:'short' }).format(new Date(`${date}T00:00:00`))
const periodMonths = computed(() => Math.max(1, Math.ceil((new Date(store.travelEnd) - new Date(store.travelStart)) / 2_592_000_000)))
const unreadCount = computed(() => store.schedules.filter(item => !item.notificationRead).length)
</script>

<template>
  <main class="schedule-page">
    <header class="page-header"><button type="button" @click="router.back()">‹</button><h1>여행 일정 목록</h1><button class="notice-button" type="button" aria-label="여행 일정 알림" @click="router.push('/schedule/notifications')">♢<i v-if="unreadCount" /></button></header>
    <section class="period-card"><span>▣</span><b>{{ store.travelStart }} ~ {{ store.travelEnd }}</b><em>{{ periodMonths }}개월</em></section>

    <section v-if="todaySchedules.length" class="today-ticket">
      <div class="ticket-head"><b>오늘 일정</b><small>{{ store.demoToday.replaceAll('-', '.') }}</small></div>
      <div class="cut"><i/><span/><i/></div>
      <p>오늘의 여행 계획도 모두 잊지 마세요.</p>
      <button v-for="item in todaySchedules" :key="item.id" type="button" :aria-label="`${item.title} 상세 보기`" @click="router.push({ name:'ScheduleDetail', params:{ scheduleId:item.id } })">
        <time>{{ item.time }}</time><span><b>{{ country(item.countryCode).flag }} {{ item.title }}</b><small>{{ item.currency }} {{ item.amount.toLocaleString() }} · {{ item.paymentStatus === 'prepaid' ? '사전결제 완료' : '현장결제 필요' }}</small></span><em>{{ item.completed ? '일정 완료' : '일정 예정' }}</em>
      </button>
      <div class="cut bottom"><i/><span/><i/></div><div class="barcode">||||||||||||||||||||</div>
    </section>

    <section v-else class="empty-ticket"><span>▣</span><b>등록된 오늘 일정이 없어요</b><small>새로운 여행 일정을 추가해 보세요.</small></section>

    <section class="upcoming-card">
      <div class="section-title"><h2>다가오는 여행 일정</h2><span>총 {{ store.sortedSchedules.length }}개</span></div>
      <div v-for="group in upcomingGroups" :key="group.date" class="date-group">
        <h3>{{ dateLabel(group.date) }}</h3>
        <button v-for="item in group.items" :key="item.id" type="button" :class="{ onsite: item.paymentStatus === 'onsite' }" @click="router.push(`/schedule/${item.id}`)">
          <span class="schedule-icon">{{ country(item.countryCode).flag }}</span>
          <span class="copy"><b>{{ item.title }}</b><small>{{ item.time }} · {{ item.currency }} {{ item.amount.toLocaleString() }}<br>{{ item.place }}</small></span>
          <em>{{ item.paymentStatus === 'prepaid' ? '사전결제 완료' : '현장결제 필요' }}</em><strong>›</strong>
        </button>
      </div>
      <p v-if="!upcomingGroups.length" class="empty">다가오는 여행 일정이 없어요.</p>
    </section>

    <button class="add-button" type="button" @click="router.push('/schedule/new')"><span>＋</span>새 여행 일정 추가하기</button>
    <BottomNav />
  </main>
</template>

<style scoped>
.schedule-page{min-height:100vh;padding:0 16px 150px;background:#f8f6f1;color:#10192d}.page-header{display:grid;grid-template-columns:32px 1fr 32px;align-items:end;height:78px;padding-bottom:13px}.page-header button{font-size:28px;text-align:left}.page-header h1{text-align:center;font-size:18px;font-weight:900}.period-card{display:grid;grid-template-columns:24px 1fr auto;align-items:center;padding:11px 13px;border:1px solid #dce4ef;border-radius:12px;background:#fff;box-shadow:0 3px 10px #1525470c}.period-card span{color:#2474e8}.period-card b{font-size:10px}.period-card em{padding:5px 10px;border-radius:10px;background:#eaf3ff;color:#246dd7;font-size:8px;font-style:normal}.today-ticket{position:relative;margin-top:13px;overflow:hidden;border-radius:17px;background:linear-gradient(135deg,#0c337f,#1c61ca);color:#fff;box-shadow:0 8px 18px #173f8d24}.ticket-head{display:flex;justify-content:space-between;padding:14px 17px 12px}.ticket-head b{font-size:14px}.ticket-head small{color:#ffffff99;font-size:8px}.cut{display:grid;grid-template-columns:15px 1fr 15px;align-items:center;height:0}.cut i{width:20px;height:20px;border-radius:50%;background:#f8f6f1}.cut i:first-child{transform:translateX(-10px)}.cut i:last-child{transform:translateX(5px)}.cut span{border-top:1px dashed #ffffff80}.today-ticket>p{padding:13px 17px 7px;color:#dbe8ff;font-size:8px}.today-ticket>button{display:grid;width:calc(100% - 28px);grid-template-columns:43px 1fr auto;align-items:center;gap:8px;margin:8px 14px;padding:11px;border-radius:11px;background:#ffffff16;text-align:left}.today-ticket time{padding:7px 4px;border-radius:7px;background:#ffffff19;text-align:center;font-size:10px;font-weight:900}.today-ticket button span>*{display:block}.today-ticket button b{font-size:10px}.today-ticket button small{margin-top:4px;color:#d7e4fb;font-size:7px}.today-ticket button em{padding:5px 8px;border-radius:9px;background:#fff;color:#2772dd;font-size:7px;font-style:normal}.cut.bottom{margin-top:12px}.barcode{height:24px;padding:7px 18px;text-align:right;letter-spacing:-1px}.empty-ticket{display:flex;min-height:150px;flex-direction:column;align-items:center;justify-content:center;margin-top:13px;border-radius:17px;background:#174596;color:#fff}.empty-ticket span{font-size:28px}.empty-ticket b{margin-top:10px;font-size:13px}.empty-ticket small{margin-top:5px;color:#d7e4fb;font-size:8px}.upcoming-card{margin-top:14px;padding:16px;border:1px solid #dce4ee;border-radius:17px;background:#fff}.section-title{display:flex;align-items:center;justify-content:space-between}.section-title h2{font-size:15px;font-weight:900}.section-title span{color:#94a3b8;font-size:8px}.date-group h3{margin:19px 2px 9px;color:#286ee0;font-size:10px}.date-group>button{display:grid;width:100%;grid-template-columns:35px 1fr auto 9px;align-items:center;gap:8px;margin-top:7px;padding:11px;border:1px solid #dce7f4;border-radius:12px;background:#f8fbff;text-align:left}.date-group>button.onsite{border-color:#f5c8c2;background:#fff6f4}.schedule-icon{display:grid;width:31px;height:31px;place-items:center;border-radius:50%;background:#fff}.copy>*{display:block}.copy b{font-size:10px}.copy small{margin-top:4px;color:#7e8b9e;font-size:7px;line-height:1.5}.date-group em{padding:4px 6px;border-radius:8px;background:#e6f1ff;color:#2472dd;font-size:6px;font-style:normal}.date-group .onsite em{background:#fff0ee;color:#db6258}.date-group strong{color:#8796aa;font-size:18px}.empty{padding:40px;text-align:center;color:#94a3b8;font-size:10px}.add-button{position:fixed;right:max(calc((100vw - 390px)/2 + 16px),16px);bottom:78px;left:max(calc((100vw - 390px)/2 + 16px),16px);z-index:40;height:52px;border-radius:13px;background:#18489f;color:#fff;font-size:13px;font-weight:900;box-shadow:0 8px 18px #173f8d2e}.add-button span{float:left;margin-left:16px;font-size:20px}
.notice-button{position:relative!important;font-size:21px!important;text-align:center!important}.notice-button i{position:absolute;top:0;right:1px;width:7px;height:7px;border:2px solid #f8f6f1;border-radius:50%;background:#ff6b35}
.schedule-page{padding-right:20px;padding-left:20px}.page-header{height:92px;grid-template-columns:36px 1fr 36px;padding-bottom:17px}.page-header h1{font-size:21px}.period-card{grid-template-columns:30px 1fr auto;padding:14px 16px;border-radius:14px}.period-card span{font-size:18px}.period-card b{font-size:13px}.period-card em{padding:7px 13px;border-radius:13px;font-size:11px}.today-ticket{margin-top:18px;border-radius:22px}.ticket-head{padding:18px 22px 15px}.ticket-head b{font-size:20px}.ticket-head small{font-size:11px}.today-ticket>p{padding:18px 22px 8px;font-size:11px}.today-ticket>button{width:calc(100% - 36px);min-height:86px;grid-template-columns:55px minmax(0,1fr) auto;gap:12px;margin:12px 18px;padding:14px 15px;border:1px solid #ffffff18;border-radius:16px;background:#ffffff18;cursor:pointer}.today-ticket time{padding:10px 6px;border-radius:10px;font-size:14px}.today-ticket button b{font-size:13px;line-height:1.35}.today-ticket button small{margin-top:6px;font-size:10px;line-height:1.4}.today-ticket button em{padding:7px 10px;border-radius:12px;font-size:9px;font-weight:900;white-space:nowrap}.cut.bottom{margin-top:17px}.barcode{height:34px;padding:9px 22px}.upcoming-card{margin-top:18px;padding:20px;border-radius:22px}.section-title h2{font-size:20px}.section-title span{font-size:11px}.date-group h3{margin:24px 4px 11px;font-size:14px}.date-group>button{min-height:86px;grid-template-columns:44px minmax(0,1fr) auto 10px;gap:10px;margin-top:10px;padding:14px;border-radius:16px;cursor:pointer}.schedule-icon{width:42px;height:42px;font-size:19px}.copy b{font-size:13px;line-height:1.35}.copy small{margin-top:6px;font-size:10px}.date-group em{padding:6px 8px;border-radius:10px;font-size:8px;white-space:nowrap}.date-group strong{font-size:22px}.add-button{right:max(calc((100vw - 390px)/2 + 20px),20px);left:max(calc((100vw - 390px)/2 + 20px),20px);height:56px;border-radius:15px;font-size:14px}
</style>
