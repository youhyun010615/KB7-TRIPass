<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useChecklistStore } from '@/stores/checklist'

const route = useRoute()
const router = useRouter()
const store = useChecklistStore()
const tripId = computed(() => Number(route.query.tripId || 1))
const data = computed(() => store.getTrip(tripId.value))
const prepProgress = computed(() => store.progress(store.preparationItems(tripId.value)))
const returnProgress = computed(() => store.progress(data.value.returns))
const allProgress = computed(() => store.progress([...store.preparationItems(tripId.value), ...data.value.returns]))
</script>

<template>
  <main class="checklist-page">
    <header><button type="button" @click="router.back()">‹</button><h1>체크리스트</h1><span /></header>
    <section class="ticket">
      <small>TRIP CHECKLIST PASS</small>
      <div><h2>{{ data.trip.flags }} {{ data.trip.title }}</h2><b>D-{{ data.trip.dDay }}</b></div>
      <i />
      <p>전체 완료율</p>
      <div class="total"><strong>{{ allProgress.done }} / {{ allProgress.total }}</strong><em>{{ allProgress.percent }}% 완료</em></div>
      <div class="bar"><span :style="{ width: `${allProgress.percent}%` }" /></div>
    </section>
    <h3>체크리스트 목록</h3>
    <button class="menu-card" @click="router.push(`/mypage/checklists/preparation?tripId=${tripId}`)">
      <span class="menu-icon preparation">✓</span><span class="copy"><b>여행 준비 체크리스트</b><small>D-30 · D-7 · D-1 준비 항목</small></span><em>{{ prepProgress.done }}/{{ prepProgress.total }}</em><strong>›</strong>
    </button>
    <button class="menu-card" @click="router.push(`/mypage/checklists/return?tripId=${tripId}`)">
      <span class="menu-icon returning">↩</span><span class="copy"><b>귀국 체크리스트</b><small>귀국일 점검 및 정리 항목</small></span><em :class="{ scheduled: !returnProgress.done }">{{ returnProgress.done ? `${returnProgress.done}/${returnProgress.total}` : '예정' }}</em><strong>›</strong>
    </button>
    <aside><span>✈️</span><span><b>완료하지 못한 준비 항목은 다음 단계로 이월돼요</b><small>체크리스트는 직접 추가할 수도 있어요.</small></span></aside>
    <BottomNav />
  </main>
</template>

<style scoped>
.checklist-page { min-height: 100vh; padding: 0 18px 96px; background: #f8f6f1; color: #111a2d; }
.checklist-page > header { display: grid; height: 94px; grid-template-columns: 40px 1fr 40px; align-items: end; padding-bottom: 18px; }
.checklist-page > header button { font-size: 31px; text-align: left; }
.checklist-page > header h1 { text-align: center; font-size: 20px; font-weight: 900; }
.ticket { position: relative; padding: 22px 20px; border-radius: 20px; background: linear-gradient(135deg, #17397f, #102b66); color: #fff; box-shadow: 0 12px 24px #19386d24; }
.ticket::before, .ticket::after { position: absolute; top: 48%; width: 18px; height: 18px; border-radius: 50%; background: #f8f6f1; content: ''; }
.ticket::before { left: -9px; } .ticket::after { right: -9px; }
.ticket small { color: #c7d7f6; font-size: 10px; font-weight: 800; letter-spacing: .08em; }
.ticket > div { display: flex; align-items: center; justify-content: space-between; }
.ticket h2 { margin-top: 18px; font-size: 17px; } .ticket > div > b { margin-top: 18px; color: #ffb21c; font-size: 13px; }
.ticket i { display: block; margin: 18px 0 14px; border-top: 1px dashed #8fa9d5; }
.ticket p { color: #b9cae7; font-size: 10px; }.ticket .total { margin-top: 7px; }.ticket .total strong { font-size: 22px; }
.ticket .total em { color: #58c9ff; font-size: 12px; font-style: normal; font-weight: 900; }
.bar { height: 5px; margin-top: 13px; border-radius: 8px; background: #ffffff24; }.bar span { display: block; height: 100%; border-radius: 8px; background: linear-gradient(90deg, #45d7ff, #1e8bff); }
.checklist-page > h3 { margin: 25px 2px 14px; font-size: 17px; }
.menu-card { display: grid; width: 100%; grid-template-columns: 50px 1fr auto 12px; gap: 13px; align-items: center; margin-bottom: 12px; padding: 20px 16px; border: 1px solid #e4e8ef; border-radius: 20px; background: #fff; box-shadow: 0 8px 18px #1727490c; text-align: left; }
.menu-icon { display: grid; width: 48px; height: 48px; border-radius: 15px; place-items: center; font-size: 23px; font-weight: 900; }.preparation { background: #eaf3ff; color: #0767e9; }.returning { background: #e9faf4; color: #13a17c; }
.copy b, .copy small { display: block; }.copy b { font-size: 14px; }.copy small { margin-top: 6px; color: #7f8da2; font-size: 10px; }
.menu-card em { padding: 7px 11px; border-radius: 15px; background: #e9f3ff; color: #0869eb; font-size: 10px; font-style: normal; font-weight: 900; }.menu-card em.scheduled { background: #e8faf4; color: #10a17c; }.menu-card > strong { color: #7d8999; font-size: 24px; }
aside { display: flex; gap: 13px; margin-top: 22px; padding: 17px; border-radius: 16px; background: #e6f1ff; }aside b, aside small { display: block; }aside b { color: #143879; font-size: 11px; }aside small { margin-top: 7px; color: #71839f; font-size: 9px; }
</style>
