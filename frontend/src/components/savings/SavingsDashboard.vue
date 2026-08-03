<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import SavingsGoalTicket from '@/components/savings/SavingsGoalTicket.vue'
import { useTravelStore } from '@/stores/travel'
import { useSavingsPlanStore } from '@/stores/savingsPlan'

const router = useRouter()
const travel = useTravelStore()
const plan = useSavingsPlanStore()
const money = (value) => `${Number(value).toLocaleString('ko-KR')}원`
const firstPlan = computed(() => travel.selectedPlans[0] || { countryName: '프랑스', cityName: '파리' })
const statusCopy = computed(() => ({
  unset: { icon: '⚠', title: '현재 저축 계획으로는 목표 달성이 부족해요', amount: plan.recommendedMonthlySavings, badge: '계산 불가', detail: '지금은 월 저축 금액이 없어 지연 출국 날짜를 계산할 수 없어요.' },
  success: { icon: '✓', title: '현재 계획으로 목표 달성 가능', amount: 0, badge: `예상 출국 ${plan.expectedDeparture}`, detail: '목표로 설정한 날짜에 출국할 수 있어요.' },
  warning: { icon: '!', title: '월 저축액을 조금 더 늘려볼까요?', amount: plan.additionalRecommendedAmount, badge: `예상 출국 ${plan.expectedDeparture}`, detail: `현재 계획은 목표 출국일보다 약 ${plan.delayMonths}개월 늦게 달성할 수 있어요.` },
  error: { icon: '⚠', title: '현재 여유 자금보다 저축 금액이 커요', amount: plan.insufficientFunds, badge: '설정 불가', detail: `${money(plan.availableFunds)} 이하로 다시 입력해 주세요.` },
}[plan.status]))
const linkedAccounts = computed(() => travel.accountAllocations.filter((item) => item.selected))

function openPrimaryAction() {
  if (plan.status === 'success') router.push('/asset')
  else router.push('/savings/plan')
}
</script>

<template>
  <main class="dashboard-page">
    <header class="page-header">
      <button aria-label="뒤로가기" @click="router.back()">‹</button>
      <h1>여행 목표 자금 관리</h1>
      <button class="text-action" @click="router.push('/travel/register')">계획 수정</button>
    </header>

    <SavingsGoalTicket
      :city="firstPlan.cityName"
      d-day="D-230"
      :target="plan.totalTargetAmount"
      :secured="plan.securedAmount"
      :shortage="plan.shortageAmount"
      :percent="plan.securedPercent"
    />

    <button class="account-summary" @click="router.push('/savings/accounts')">
      <span class="account-icon">▣</span>
      <div><strong>반영 중인 계좌</strong><small>{{ linkedAccounts.map((item) => item.productName).join(' · ') || '반영 계좌를 확인해 주세요' }}</small></div>
      <div class="account-value"><b>{{ linkedAccounts.length || 2 }}개</b><span>{{ money(plan.securedAmount) }}</span></div><i>›</i>
    </button>

    <section class="timeline-card" :class="plan.status">
      <div class="section-title"><strong>저축 가능 기간</strong><b>{{ plan.expectedMonths ? `총 ${plan.expectedMonths}개월 · ${plan.expectedMonths}회 저축` : '기간 계산 불가' }}</b></div>
      <div class="timeline"><span></span><span></span><span></span></div>
      <div class="timeline-label"><small>현재<br><b>2026.07</b></small><small>월 저축 × {{ plan.expectedMonths || 0 }}<br><b>{{ money(plan.monthlySavings) }}</b></small><small>예상 출국<br><b>{{ plan.expectedDeparture || '계산 불가' }}</b></small></div>
    </section>

    <section class="forecast-card" :class="plan.status">
      <div class="forecast-title"><span>{{ statusCopy.icon }}</span><strong>{{ statusCopy.title }}</strong></div>
      <hr>
      <small>{{ plan.status === 'error' ? '여유 자금 부족액' : '추천 월 저축액' }}</small>
      <div class="forecast-value"><strong>{{ money(statusCopy.amount) }}</strong><b>{{ statusCopy.badge }}</b></div>
      <p><span>예상 출국</span>{{ statusCopy.detail }}</p>
    </section>

    <section class="monthly-card">
      <span class="calendar-icon">▢</span>
      <div><small>월 저축 플랜</small><strong>{{ money(plan.monthlySavings) }}</strong></div>
      <div class="monthly-state"><b>{{ plan.status === 'success' ? '정상 진행' : plan.status === 'warning' ? '추가 진행 필요' : '미설정' }}</b><small>{{ plan.monthlySavings ? '다음 저축일 8월 25일' : '다음 저축일 없음' }}</small></div>
    </section>

    <button class="primary-cta" @click="openPrimaryAction">
      {{ plan.status === 'success' ? '고정비 목록 보러가기' : plan.status === 'unset' ? '월 저축 방식 선택' : '월 저축액 조정하기' }}
    </button>
  </main>
</template>

<style scoped>
.dashboard-page { min-height:100vh; padding:0 18px 100px; color:#111827; background:#f7f4ee; }.page-header { height:76px; display:grid; grid-template-columns:38px 1fr 56px; align-items:end; padding-bottom:14px; }.page-header h1 { font-size:17px; font-weight:800; }.page-header button { border:0; background:none; font-size:24px; text-align:left; }.page-header .text-action { color:#0066ff; font-size:10px; font-weight:700; text-align:right; }
.account-summary { width:100%; display:flex; align-items:center; gap:10px; margin-top:13px; padding:13px; border:1px solid #dde5f1; border-radius:15px; background:#fff; text-align:left; box-shadow:0 5px 14px rgba(20,35,70,.06); }.account-icon { display:grid; place-items:center; width:34px; height:34px; flex:none; border-radius:50%; color:#d97706; background:#fff3d6; }.account-summary strong,.account-summary small { display:block; }.account-summary strong { font-size:11px; }.account-summary small { max-width:170px; overflow:hidden; margin-top:3px; color:#94a3b8; font-size:7px; text-overflow:ellipsis; white-space:nowrap; }.account-value { margin-left:auto; text-align:right; }.account-value b,.account-value span { display:block; font-size:10px; }.account-value b { color:#0066ff; }.account-summary i { color:#173b86; font-style:normal; }
.timeline-card,.forecast-card,.monthly-card { margin-top:12px; border:1px solid #dde5f1; border-radius:15px; background:#fff; box-shadow:0 5px 14px rgba(20,35,70,.06); }.timeline-card { padding:15px 14px 13px; }.section-title { display:flex; justify-content:space-between; align-items:center; }.section-title strong { font-size:11px; }.section-title b { color:#0066ff; font-size:8px; }.timeline { position:relative; display:flex; justify-content:space-between; margin:23px 8px 5px; border-top:3px solid #0066ff; }.timeline span { width:11px; height:11px; margin-top:-7px; border-radius:50%; background:#24458f; }.timeline-label { display:flex; justify-content:space-between; text-align:center; }.timeline-label small { width:33%; color:#64748b; font-size:7px; line-height:1.5; }.timeline-label b { color:#111827; font-size:8px; }.timeline-card.warning .section-title b,.timeline-card.warning .timeline-label small:nth-child(2) b { color:#d97706; }.timeline-card.warning .timeline { border-color:#e78a16; }.timeline-card.warning .timeline span { background:#d97706; }.timeline-card.unset .timeline,.timeline-card.error .timeline { border-color:#cbd5e1; }.timeline-card.unset .timeline span,.timeline-card.error .timeline span { background:#cbd5e1; }.timeline-card.unset .section-title b,.timeline-card.error .section-title b { color:#e5484d; }
.forecast-card { padding:14px; }.forecast-card.unset,.forecast-card.error { border-color:#ffc9cc; background:#fff2f2; }.forecast-card.warning { border-color:#ffd59b; background:#fff6e9; }.forecast-card.success { border-color:#bdeadd; background:#eaf9f4; }.forecast-title { display:flex; align-items:center; gap:7px; color:#d9363e; }.forecast-title span { display:grid; place-items:center; width:22px; height:22px; border-radius:50%; color:#fff; background:#e5484d; font-size:11px; }.warning .forecast-title { color:#b96300; }.warning .forecast-title span { background:#ee921f; }.success .forecast-title { color:#07875f; }.success .forecast-title span { background:#10aa82; }.forecast-title strong { font-size:10px; }.forecast-card hr { margin:10px 0; border:0; border-top:1px solid rgba(120,130,150,.16); }.forecast-card > small { color:#64748b; font-size:7px; }.forecast-value { display:flex; justify-content:space-between; align-items:center; margin-top:3px; }.forecast-value strong { color:#e5484d; font-size:20px; }.warning .forecast-value strong { color:#d97706; }.success .forecast-value strong { color:#07875f; }.forecast-value b { padding:5px 15px; border-radius:99px; color:#d9363e; background:#fff; font-size:7px; }.warning .forecast-value b { color:#b96300; }.success .forecast-value b { color:#07875f; }.forecast-card p { margin-top:9px; padding:9px; border-radius:9px; background:#fff; color:#64748b; font-size:8px; line-height:1.45; }.forecast-card p span { display:block; color:#94a3b8; font-size:7px; }
.monthly-card { display:flex; align-items:center; gap:10px; padding:13px; }.calendar-icon { display:grid; place-items:center; width:30px; height:30px; border-radius:50%; color:#0066ff; background:#edf4ff; }.monthly-card small,.monthly-card strong { display:block; }.monthly-card small { color:#94a3b8; font-size:7px; }.monthly-card strong { margin-top:3px; color:#0066ff; font-size:15px; }.monthly-state { margin-left:auto; text-align:right; }.monthly-state b { display:block; color:#07875f; font-size:7px; }.monthly-state small { margin-top:4px; }
.primary-cta { position:fixed; z-index:5; left:50%; bottom:22px; width:min(354px,calc(100% - 36px)); height:54px; transform:translateX(-50%); border:0; border-radius:14px; color:#fff; background:#173b86; font-size:14px; font-weight:800; box-shadow:0 8px 18px rgba(23,59,134,.18); }
</style>
