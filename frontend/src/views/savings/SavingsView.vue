<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import TravelTicket from '@/components/savings/TravelTicket.vue'
import SavingsDashboard from '@/components/savings/SavingsDashboard.vue'
import { useTravelStore } from '@/stores/travel'

const router = useRouter()
const travelStore = useTravelStore()

onMounted(() => {
  travelStore.loadActiveGoal({ force: true })
})
</script>

<template>
  <SavingsDashboard v-if="travelStore.hasTravelGoal" />
  <main v-else class="goal-page">
    <header class="page-header">
      <button aria-label="뒤로가기" @click="router.back()">‹</button>
      <h1>여행 목표 저축</h1>
      <span />
    </header>

    <section class="empty-ticket">
      <div class="ticket-visual"><span>●</span><i>✈</i><b>●</b></div>
      <small>TRIPASS · START JOURNEY</small>
      <h2>아직 등록된 여행 목표가 없어요</h2>
      <p>여행 국가와 일정을 등록하면 AI가 필요한<br>여행 목표 금액과 월 저축액을 제안해 드려요.</p>
      <ol>
        <li><b>1</b><span>여행지·일정</span></li>
        <li><b>2</b><span>AI 예산 추천</span></li>
        <li><b>3</b><span>TRIP 월렛 저축</span></li>
      </ol>
    </section>

    <button class="primary-cta" @click="router.push('/travel/register')">여행 계획 등록하기</button>
    <BottomNav />
  </main>
</template>

<style scoped>
.goal-page { height: 100vh; height: 100dvh; min-height: 0; padding: 0 18px 92px; overflow-x: hidden; overflow-y: auto; overscroll-behavior-y: none; scrollbar-width: none; color: #111827; background: #f4f7ff; }
.goal-page::-webkit-scrollbar { display: none; }
.page-header { height: 56px; display: grid; grid-template-columns: 36px 1fr 36px; align-items: end; padding-bottom: 14px; }
.page-header h1 { font-size: 17px; font-weight: 800; text-align:center; }
.page-header button { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 12px; background: #fff; color: #193d82; font-weight: 700; box-shadow: 0 5px 16px rgba(36, 72, 117, 0.07); font-size: 24px; }
.empty-ticket { position:relative; overflow:hidden; margin-top:22px; padding:24px 20px 21px; border-radius:24px; color:#fff; text-align:center; background:linear-gradient(145deg,#173b86,#29499d); box-shadow:0 14px 25px rgba(31,59,130,.2); }
.empty-ticket::before,.empty-ticket::after { content:''; position:absolute; top:42%; width:22px; height:22px; border-radius:50%; background:#f4f7ff; }.empty-ticket::before { left:-11px; }.empty-ticket::after { right:-11px; }
.ticket-visual { display:flex; align-items:center; justify-content:space-between; height:74px; padding:0 32px; border-radius:17px; background:#edf3ff; color:#1e469b; font-size:19px; }.ticket-visual i { position:relative; font-size:31px; font-style:normal; transform:rotate(18deg); }.ticket-visual i::before { content:''; position:absolute; top:50%; right:29px; width:80px; border-top:2px dashed #9bb8f6; transform:rotate(-8deg); }.ticket-visual i::after { content:''; position:absolute; top:50%; left:29px; width:80px; border-top:2px dashed #9bb8f6; transform:rotate(8deg); }
.empty-ticket>small { display:block; margin-top:18px; color:#b9ccff; font-size:9px; letter-spacing:.5px; }.empty-ticket h2 { margin-top:8px; font-size:18px; font-weight:800; }.empty-ticket p { margin-top:8px; color:#d7e4ff; font-size:11px; line-height:1.5; }
.empty-ticket ol { display:flex; gap:4px; margin-top:18px; padding:13px 5px; border-radius:13px; background:#fff; color:#405373; }.empty-ticket li { flex:1; display:flex; flex-direction:column; align-items:center; gap:6px; font-size:9px; list-style:none; }.empty-ticket li b { display:grid; place-items:center; width:21px; height:21px; border-radius:50%; color:#fff; background:#2469e8; }
.goal-overview { display: flex; align-items: center; gap: 24px; padding: 2px 7px 0; }
.progress-ring { width: 90px; height: 90px; border: 9px solid #d8e4ff; border-radius: 50%; display: flex; flex-direction: column; align-items: center; justify-content: center; flex: 0 0 auto; }
.progress-ring strong { font-size: 21px; color: #fff; }.progress-ring span { font-size: 8px; color: #cbd9ff; }
.goal-overview dl { flex: 1; display: grid; gap: 6px; }.goal-overview dl div { display: flex; justify-content: space-between; align-items: end; }
.goal-overview dt { font-size: 9px; color: #cbd9ff; }.goal-overview dd { font-size: 14px; font-weight: 800; }
.account-summary,.empty-card { border: 1px solid #e2e8f0; background: #fff; box-shadow: 0 5px 15px rgba(20,35,70,.06); }
.account-summary { display: flex; align-items: center; gap: 10px; margin-top: 14px; padding: 13px; border-radius: 15px; }
.account-icon { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 50%; color: #d97706; background: #fff3d6; }
.account-summary strong { display: block; font-size: 12px; }.account-summary small { display: block; margin-top: 3px; color: #94a3b8; font-size: 8px; }
.account-value { margin-left: auto; text-align: right; }.account-value b,.account-value span { display:block; font-size: 11px; }.account-value b { color:#0066ff; }
.empty-card { margin-top: 14px; padding: 24px 18px 19px; border-radius: 20px; text-align: center; }
.flight-visual { display:flex; align-items:center; justify-content:center; gap:8px; height:58px; color:#0066ff; font-size:25px; }.dashed-route { color:#9ab9f5; font-size:19px; }.plane { transform:rotate(10deg); }
.empty-card h2 { margin-top:8px; font-size:15px; font-weight:800; }.empty-card p { margin-top:8px; color:#64748b; font-size:10px; line-height:1.55; }
.empty-card ol { display:flex; margin-top:20px; padding:13px 8px 0; border-top:1px dashed #d6deea; }
.empty-card li { position:relative; flex:1; display:flex; flex-direction:column; align-items:center; gap:6px; color:#64748b; font-size:9px; list-style:none; }
.empty-card li:not(:last-child)::after { content:''; position:absolute; top:10px; left:65%; width:70%; border-top:1px solid #dbe5f5; }
.empty-card li b { z-index:1; display:grid; place-items:center; width:22px; height:22px; border-radius:50%; color:#fff; background:#2b4b9b; }
.primary-cta { position:fixed; z-index:5; left:50%; bottom:76px; transform:translateX(-50%); width:min(354px, calc(100% - 36px)); height:54px; border:0; border-radius:14px; color:#fff; background:#173b86; font-size:14px; font-weight:800; }
@media (min-width: 391px) { .primary-cta { width:354px; } }
</style>
