<script setup>
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import TravelTicket from '@/components/savings/TravelTicket.vue'
import { useTravelStore } from '@/stores/travel'

const router = useRouter()
const travelStore = useTravelStore()
const money = (value) => `${Number(value).toLocaleString('ko-KR')}원`
</script>

<template>
  <main class="goal-page">
    <header class="page-header">
      <button aria-label="뒤로가기" @click="router.back()">‹</button>
      <h1>여행 목표 자금 관리</h1>
      <button class="text-action" @click="travelStore.resetGoal()">초기화</button>
    </header>

    <TravelTicket title="여행지 미설정" meta="D-미정">
      <div class="goal-overview">
        <div class="progress-ring"><strong>0%</strong><span>현재 확보율</span></div>
        <dl>
          <div><dt>총 목표 금액</dt><dd>0원</dd></div>
          <div><dt>확보한 여행 자금</dt><dd>0원</dd></div>
          <div><dt>부족 금액</dt><dd>0원</dd></div>
        </dl>
      </div>
    </TravelTicket>

    <section class="account-summary">
      <span class="account-icon">▣</span>
      <div><strong>반영 중인 계좌</strong><small>여행 자금으로 사용하는 계좌예요</small></div>
      <div class="account-value"><b>0개</b><span>0원</span></div>
    </section>

    <section class="empty-card">
      <div class="flight-visual">
        <span class="dashed-route">··········</span><span class="plane">✈</span><span>🌐</span>
      </div>
      <h2>아직 등록된 여행 목표가 없어요</h2>
      <p>여행지와 목표 금액, 목표 계좌를 등록하면<br>필요한 월 저축액과 예상 달성 시기를 계산해 드려요.</p>
      <ol>
        <li><b>1</b><span>여행지 선택</span></li>
        <li><b>2</b><span>목표 금액 설정</span></li>
        <li><b>3</b><span>계좌 연결</span></li>
      </ol>
    </section>

    <button class="primary-cta" @click="router.push('/travel/register')">여행 목표 등록하기</button>
    <BottomNav />
  </main>
</template>

<style scoped>
.goal-page { min-height: 100vh; padding: 0 18px 92px; color: #111827; background: #f7f4ee; }
.page-header { height: 76px; display: grid; grid-template-columns: 38px 1fr 48px; align-items: end; padding-bottom: 14px; }
.page-header h1 { font-size: 17px; font-weight: 800; }
.page-header button { border: 0; background: none; text-align: left; font-size: 24px; }
.page-header .text-action { color: #0066ff; text-align: right; font-size: 11px; font-weight: 700; }
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
