<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import DatePickerSheet from '@/components/savings/DatePickerSheet.vue'
import TravelTicket from '@/components/savings/TravelTicket.vue'
import { useTravelStore } from '@/stores/travel'

const router = useRouter()
const store = useTravelStore()
const step = ref(1)
const dateTarget = ref(null)
const showValidation = ref(false)
const money = (value) => Number(value || 0).toLocaleString('ko-KR')
const dateLabel = (value) => value ? value.replaceAll('-', '.') : '여행 날짜 선택'
const stepTitle = computed(() => ['여행 계획 등록하기', '총 여행 목표 금액', '여행 자금 반영 계좌'][step.value - 1])

function next() {
  showValidation.value = true
  if (step.value === 1 && store.canReviewPlan) { step.value = 2; showValidation.value = false }
  else if (step.value === 2) step.value = 3
}

function back() {
  if (step.value > 1) step.value -= 1
  else router.back()
}

function finish() {
  if (store.completeGoal()) router.push('/')
}
</script>

<template>
  <main class="register-page">
    <header class="page-header"><button @click="back">‹</button><h1>{{ stepTitle }}</h1><span /></header>
    <div class="steps"><i v-for="index in 3" :key="index" :class="{ active: index <= step }" /></div>

    <template v-if="step === 1">
      <TravelTicket :title="`선택 국가 총 목표 금액`" :meta="`${store.selectedPlans.length}개국 선택`">
        <strong class="total-money">{{ money(store.totalTargetAmount) }}원</strong>
      </TravelTicket>

      <label class="field"><span>여행명</span><input v-model="store.tripName" placeholder="여행명을 입력해 주세요"><small v-if="showValidation && !store.tripName.trim()">여행명을 입력해 주세요.</small></label>
      <section class="country-section"><div class="section-title"><h2>여행 국가 선택</h2><span>최대 5개국</span></div>
        <div class="chips"><button v-for="country in store.countries" :key="country.code" :class="{ selected: store.selectedCountryCodes.includes(country.code) }" @click="store.toggleCountry(country.code)">{{ country.flag }} {{ country.name }}</button></div>
        <p v-if="showValidation && !store.selectedPlans.length" class="error-text">여행 국가를 한 개 이상 선택해 주세요.</p>
      </section>

      <section v-for="plan in store.selectedPlans" :key="plan.code" class="country-card" :style="{ '--accent': plan.accent }">
        <header><div><span>{{ plan.flag }}</span><strong>{{ plan.name }}</strong><small>{{ plan.city }}</small></div><button @click="store.toggleCountry(plan.code)">삭제</button></header>
        <button class="date-row" @click="dateTarget = plan"><span>여행 날짜</span><b>{{ plan.startDate ? `${dateLabel(plan.startDate)} ~ ${dateLabel(plan.endDate)}` : '날짜 선택' }}</b><span>▣</span></button>
        <div class="budget-info"><span>사전 확보 예산 <b>{{ money(plan.securedBudget) }}원</b></span></div>
        <label class="budget-input"><span>목표 예산</span><input type="number" min="0" :value="plan.targetBudget" @input="store.updatePlan(plan.code, { targetBudget: Number($event.target.value) })"><em>원</em></label>
        <p v-if="showValidation && store.planError(plan)" class="card-error">⚠ {{ store.planError(plan) }}</p>
      </section>
      <p v-if="showValidation && store.hasDateCollision" class="collision">⚠ 국가별 여행 날짜가 겹쳐요. 일정을 다시 확인해 주세요.</p>
      <button class="primary-cta" :disabled="!store.canReviewPlan" @click="next">다음</button>
    </template>

    <template v-else-if="step === 2">
      <section class="country-summary">
        <h2>국가별 목표 예산</h2>
        <article v-for="plan in store.selectedPlans" :key="plan.code" :style="{ '--accent': plan.accent }">
          <span>{{ plan.flag }}</span><div><strong>{{ plan.name }}</strong><small>{{ dateLabel(plan.startDate) }} ~ {{ dateLabel(plan.endDate) }}</small></div><b>{{ money(plan.targetBudget) }}원</b>
        </article>
      </section>
      <TravelTicket title="총 여행 목표 금액" :meta="`${store.selectedPlans.length}개국 합산`">
        <div class="grand-total">{{ money(store.totalTargetAmount) }}원</div>
        <p class="ticket-note">국가별 목표 예산을 모두 더한 금액이에요.</p>
      </TravelTicket>
      <button class="primary-cta" @click="next">여행 자금 반영 계좌 선택하기</button>
    </template>

    <template v-else>
      <TravelTicket title="연동 계좌 선택" :meta="`${store.selectedAccountCount}개 계좌 선택`">
        <span class="ticket-label">여행 자금으로 반영할 금액</span><strong class="total-money">{{ money(store.totalAllocatedAmount) }}원</strong>
      </TravelTicket>
      <section class="account-list"><h2>연결 계좌</h2><p>여행 자금으로 사용할 금액을 입력해 주세요.</p>
        <article v-for="account in store.accounts" :key="account.id" :class="{ selected: store.allocations[account.id] > 0 }">
          <div class="account-head"><span class="check">{{ store.allocations[account.id] > 0 ? '✓' : '' }}</span><div><strong>{{ account.name }}</strong><small>{{ account.bank }} {{ account.number }}</small></div></div>
          <div class="balance">현재 잔액 <b>{{ money(account.balance) }}원</b></div>
          <label><span>여행 자금으로 사용</span><input type="number" min="0" :max="account.balance" :value="store.allocations[account.id] || ''" placeholder="0" @input="store.setAllocation(account.id, $event.target.value)"><em>원</em></label>
        </article>
      </section>
      <button class="primary-cta" :disabled="!store.canCompleteGoal" @click="finish">확인</button>
    </template>

    <DatePickerSheet :open="Boolean(dateTarget)" :plan="dateTarget" @close="dateTarget = null" @confirm="store.updatePlan(dateTarget.code, $event); dateTarget = null" />
  </main>
</template>

<style scoped>
.register-page { min-height:100vh; padding:0 18px 96px; color:#111827; background:#f7f4ee; }.page-header { height:72px; display:grid; grid-template-columns:36px 1fr 36px; align-items:end; padding-bottom:12px; }.page-header button { border:0; background:none; text-align:left; font-size:24px; }.page-header h1 { font-size:17px; font-weight:800; }.steps { display:flex; gap:7px; margin:0 0 15px; }.steps i { flex:1; height:3px; border-radius:2px; background:#dce4f1; }.steps i.active { background:#263f8c; }
.total-money { display:block; margin:9px 0 2px; font-size:25px; }.field { display:block; margin-top:16px; }.field>span,.section-title h2,.country-summary h2,.account-list h2 { font-size:13px; font-weight:800; }.field input { width:100%; height:48px; margin-top:8px; padding:0 13px; border:1px solid #e1e7f0; border-radius:12px; background:#fff; }.field small,.error-text { display:block; margin-top:5px; color:#e5484d; font-size:9px; }
.country-section { margin-top:17px; }.section-title { display:flex; justify-content:space-between; }.section-title span { color:#94a3b8; font-size:9px; }.chips { display:flex; flex-wrap:wrap; gap:7px; margin-top:10px; }.chips button { padding:8px 11px; border:1px solid #dbe3ee; border-radius:18px; color:#64748b; background:#fff; font-size:10px; }.chips button.selected { border-color:#263f8c; color:#fff; background:#263f8c; }
.country-card { margin-top:12px; padding:14px; border:1px solid #e0e7f0; border-left:4px solid var(--accent); border-radius:16px; background:#fff; box-shadow:0 5px 14px rgba(20,35,70,.05); }.country-card header,.country-card header div { display:flex; align-items:center; gap:7px; }.country-card header small { color:#94a3b8; font-size:9px; }.country-card header button { margin-left:auto; border:0; color:#e5484d; background:none; font-size:9px; }.date-row { display:flex; width:100%; align-items:center; gap:8px; margin-top:13px; padding:11px; border:1px solid #e4eaf2; border-radius:11px; background:#fafcff; }.date-row span { color:#64748b; font-size:9px; }.date-row b { margin-left:auto; color:#263f8c; font-size:10px; }.budget-info { margin-top:10px; color:#64748b; font-size:9px; }.budget-info b { margin-left:5px; color:#0066ff; }.budget-input { display:flex; align-items:center; gap:8px; margin-top:8px; padding:7px 10px; border:1px solid #cbd9ee; border-radius:10px; }.budget-input span { font-size:9px; }.budget-input input { min-width:0; flex:1; border:0; outline:none; text-align:right; font-weight:800; }.budget-input em { color:#94a3b8; font-size:9px; font-style:normal; }.card-error,.collision { margin-top:8px; padding:8px; border-radius:8px; color:#e5484d; background:#fff0f0; font-size:9px; }.collision { margin-bottom:8px; }
.country-summary,.account-list { margin-bottom:14px; }.country-summary h2,.account-list h2 { margin-bottom:10px; }.country-summary article { display:flex; align-items:center; gap:9px; margin-top:8px; padding:13px; border-left:4px solid var(--accent); border-radius:12px; background:#fff; box-shadow:0 4px 13px rgba(20,35,70,.06); }.country-summary article div { display:flex; flex-direction:column; gap:4px; }.country-summary article small { color:#94a3b8; font-size:8px; }.country-summary article>b { margin-left:auto; font-size:12px; }.grand-total { margin:14px 0 5px; text-align:center; font-size:28px; font-weight:800; }.ticket-note { color:#cbd9ff; text-align:center; font-size:9px; }
.ticket-label { display:block; margin-top:7px; color:#cbd9ff; font-size:9px; }.account-list { margin-top:17px; }.account-list>p { margin-top:-5px; color:#64748b; font-size:9px; }.account-list article { margin-top:10px; padding:14px; border:1px solid #e0e7f0; border-radius:15px; background:#fff; }.account-list article.selected { border:2px solid #0066ff; }.account-head { display:flex; align-items:center; gap:9px; }.check { display:grid; place-items:center; width:21px; height:21px; border:1px solid #ccd6e5; border-radius:6px; color:#fff; background:#fff; }.selected .check { border-color:#0066ff; background:#0066ff; }.account-head div { display:flex; flex-direction:column; gap:3px; }.account-head strong { font-size:11px; }.account-head small,.balance { color:#94a3b8; font-size:8px; }.balance { margin:10px 0; }.balance b { float:right; color:#111827; }.account-list label { display:flex; align-items:center; padding:9px 10px; border-radius:9px; background:#f6f8fc; }.account-list label span { color:#64748b; font-size:8px; }.account-list input { min-width:0; flex:1; border:0; outline:none; text-align:right; color:#0066ff; background:transparent; font-weight:800; }.account-list em { margin-left:4px; color:#94a3b8; font-size:9px; font-style:normal; }
.primary-cta { position:fixed; z-index:10; left:50%; bottom:18px; transform:translateX(-50%); width:min(354px,calc(100% - 36px)); height:54px; border:0; border-radius:14px; color:#fff; background:#263f8c; font-size:14px; font-weight:800; }.primary-cta:disabled { color:#fff; background:#aab6ca; }
</style>
