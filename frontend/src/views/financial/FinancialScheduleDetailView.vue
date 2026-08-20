<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useFinancialScheduleStore } from '@/stores/financialSchedule'

const route = useRoute()
const router = useRouter()
const schedule = useFinancialScheduleStore()
const event = computed(() => schedule.events.find((item) => item.id === route.params.eventId))
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
const dateLabel = computed(() => {
  if (!event.value) return '-'
  const [year, month, day] = event.value.date.split('-')
  return `${year}.${month}.${day}`
})
</script>

<template>
  <main class="page"><div class="shell">
    <header><button @click="router.back()">‹</button><h1>금융 일정 상세</h1><span aria-hidden="true"></span></header>
    <template v-if="event">
      <section class="hero" :class="event.type">
        <span>{{ event.icon }}</span>
        <div><small>{{ dateLabel }} 예정</small><h2>{{ event.title }}</h2><strong>{{ money(event.amount) }}</strong></div>
        <em>{{ event.type === 'income' ? '입금' : '지출' }}</em>
      </section>
      <section class="details">
        <dl>
          <div><dt>예정일</dt><dd>매월 {{ event.day }}일</dd></div>
          <div><dt>연결 계좌</dt><dd>{{ event.account || '연결 계좌 없음' }}</dd></div>
          <div><dt>거래 구분</dt><dd :class="event.type">{{ event.type === 'income' ? '입금' : '지출' }}</dd></div>
          <div><dt>알림 시점</dt><dd>1일 전</dd></div>
          <div><dt>상태</dt><dd class="active">예정</dd></div>
          <div><dt>메모</dt><dd>{{ event.memo || '-' }}</dd></div>
        </dl>
        <p>등록된 금융 정보를 기준으로 매월 자동 생성되는 일정이에요.</p>
      </section>
    </template>
    <section v-else class="missing"><span>□</span><b>금융 일정을 찾을 수 없어요</b><button @click="router.replace('/financial-schedule')">일정 목록으로 돌아가기</button></section>
  </div></main>
</template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:auto;padding:14px 18px 30px;background:#f7f5ef}header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center;margin-bottom:18px}header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}header h1{text-align:center;font-size:18px;font-weight:900}.hero{position:relative;display:flex;align-items:center;gap:14px;padding:18px;border:1px solid;border-radius:17px}.hero.income{border-color:#b7d8ff;background:#eaf4ff}.hero.expense{border-color:#f3c4be;background:#fff3f1}.hero>span{display:grid;width:52px;height:52px;place-items:center;border-radius:50%;font-size:21px}.hero.income>span{background:#d5eaff;color:#2f80ed}.hero.expense>span{background:#ffe0dc;color:#d96c5f}.hero small{color:#8290a4;font-size:9px}.hero h2{margin:3px 0;font-size:16px}.hero strong{font-size:22px}.hero.income strong{color:#2f80ed}.hero.expense strong{color:#d96c5f}.hero em{position:absolute;top:15px;right:15px;padding:5px 9px;border-radius:12px;background:#fff;font-size:8px;font-style:normal;font-weight:900}.hero.income em{color:#2f80ed}.hero.expense em{color:#d96c5f}.details{margin-top:14px;padding:8px 16px 14px;border:1px solid #e1e6ee;border-radius:17px;background:#fff}.details dl>div{display:flex;justify-content:space-between;gap:20px;padding:14px 0;border-bottom:1px solid #edf0f4;font-size:11px}.details dt{flex:none;color:#94a3b8}.details dd{text-align:right;font-weight:800}.details dd.income{color:#2f80ed}.details dd.expense{color:#d96c5f}.details dd.active{color:#0fa874}.details>p{margin-top:12px;padding:10px;border-radius:9px;background:#fff7df;color:#9a6a10;font-size:9px;line-height:1.5}.missing{padding:70px 20px;text-align:center}.missing span,.missing b{display:block}.missing span{font-size:28px}.missing b{margin-top:12px}.missing button{margin-top:20px;padding:12px 18px;border-radius:10px;background:#173f8d;color:#fff;font-weight:800}
</style>
