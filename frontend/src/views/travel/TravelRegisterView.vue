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
const isSubmitting = ref(false)
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
const dateLabel = (value) => value ? value.replaceAll('-', '.') : '여행 날짜 선택'
const stepTitle = computed(() => ['여행 계획 등록', 'AI 여행 예산', '여행 목표 확인'][step.value - 1])
const monthlyAmount = computed(() => Math.ceil(store.totalTargetAmount / 6 / 10000) * 10000)

function next() {
  showValidation.value = true
  if (step.value === 1 && store.tripName.trim() && store.selectedPlans.length) {
    step.value = 2
  } else if (step.value === 2 && store.canReviewPlan) {
    step.value = 3
  }
  if (step.value > 1) {
    showValidation.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function back() {
  if (step.value > 1) {
    step.value -= 1
    showValidation.value = false
    return
  }
  router.back()
}

async function finish() {
  if (isSubmitting.value) return
  isSubmitting.value = true
  try {
    if (await store.completeGoal()) router.push('/')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="register-page">
    <header class="page-header"><button @click="back">‹</button><h1>{{ stepTitle }}</h1><span /></header>
    <div class="steps"><i v-for="index in 3" :key="index" :class="{ active: index <= step }" /></div>

    <template v-if="step === 1">
      <section class="guide-card"><b>어디로 떠나시나요?</b><p>여행 이름과 방문 국가를 선택해 주세요. 여러 나라는 방문 순서대로 정리할 수 있어요.</p></section>
      <label class="field"><span>여행명</span><input v-model="store.tripName" placeholder="예: 8월 유럽 휴가"><small v-if="showValidation && !store.tripName.trim()">여행명을 입력해 주세요.</small></label>
      <section class="country-section">
        <div class="section-title"><h2>여행 국가 선택</h2><span>복수 선택 가능 · 최대 5개국</span></div>
        <div class="country-search">⌕ <span>국가 또는 통화 검색</span></div>
        <div class="chips"><button v-for="country in store.countries" :key="country.code" :class="{ selected: store.selectedCountryCodes.includes(country.code) }" @click="store.toggleCountry(country.code)">{{ country.flag }} {{ country.name }}</button></div>
        <p v-if="showValidation && !store.selectedPlans.length" class="error-text">여행 국가를 한 개 이상 선택해 주세요.</p>
      </section>
      <div v-if="store.selectedPlans.length" class="selection-summary"><strong>{{ store.selectedPlans.length }}개국을 선택했어요</strong><span>⋮⋮ 드래그해서 방문 순서를 바꿀 수 있어요</span></div>
      <button class="primary-cta" :class="{ inactive: !store.tripName.trim() || !store.selectedPlans.length }" @click="next">일정과 예산 입력하기</button>
    </template>

    <template v-else-if="step === 2">
      <section class="plan-guide"><div><b>{{ store.tripName }}</b><span>{{ store.selectedPlans.length }}개국 여행</span></div><button @click="step = 1">국가 수정</button></section>
      <section class="ai-guide"><span>✈</span><div><b>TRIP AI가 여행 예산을 제안해요</b><p>일정과 현지 소비 기준으로 추천했어요. 금액은 직접 수정할 수 있어요.</p></div></section>
      <p class="detail-help">국가별 여행 날짜와 목표 예산을 입력해 주세요.</p>
      <section v-for="plan in store.selectedPlans" :key="plan.code" class="country-card" :style="{ '--accent': plan.accent }">
        <header><div><span>{{ plan.flag }}</span><strong>{{ plan.name }}</strong><small>{{ plan.city }}</small></div><button @click="store.toggleCountry(plan.code)">삭제</button></header>
        <button class="date-row" @click="dateTarget = plan"><span>여행 날짜</span><b>{{ plan.startDate ? `${dateLabel(plan.startDate)} ~ ${dateLabel(plan.endDate)}` : '날짜 선택' }}</b><span>▣</span></button>
        <div class="ai-budget"><div><span>✈ 항공권 · 숙소</span><b>사전 지출 {{ money(Math.round(plan.securedBudget * .35)) }}</b></div><small>여행 목표 금액에 포함되지 않아요</small></div>
        <div class="budget-info"><span>AI 추천 현지 여행 예산 <b>{{ money(plan.securedBudget) }}</b></span></div>
        <label class="budget-input"><span>목표 예산</span><input type="number" min="0" :value="plan.targetBudget" @input="store.updatePlan(plan.code, { targetBudget: Number($event.target.value) })"><em>원</em></label>
        <p v-if="showValidation && store.planError(plan)" class="card-error">⚠ {{ store.planError(plan) }}</p>
      </section>
      <p v-if="showValidation && store.hasDateCollision" class="collision">⚠ 국가별 여행 날짜가 겹쳐요. 일정을 다시 확인해 주세요.</p>
      <button class="primary-cta" :class="{ inactive: !store.canReviewPlan }" @click="next">AI 추천 예산 확인하기</button>
    </template>

    <template v-else>
      <section class="complete-guide"><span>✈</span><p>여행 목표가 완성되었어요</p><small>TRIP 월렛으로 저축을 시작해 보세요.</small></section>
      <section class="country-summary"><h2>여행 국가별 목표</h2><article v-for="plan in store.selectedPlans" :key="plan.code" :style="{ '--accent': plan.accent }"><span>{{ plan.flag }}</span><div><strong>{{ plan.name }}</strong><small>{{ dateLabel(plan.startDate) }} ~ {{ dateLabel(plan.endDate) }}</small></div><b>{{ money(plan.targetBudget) }}</b></article></section>
      <TravelTicket title="총 여행 목표 금액" :meta="`현지 여행 자금 · ${store.selectedPlans.length}개국 합산`"><div class="grand-total">{{ money(store.totalTargetAmount) }}</div><p class="ticket-note">항공권과 숙소 등 사전 지출은 별도로 기록돼요.</p></TravelTicket>
      <section class="monthly-preview"><span>매달 저축하면 돼요</span><b>{{ money(monthlyAmount) }}</b><small>TRIP 월렛 입금액으로 저축 현황을 계산해요.</small></section>
      <p v-if="store.errorMessage" class="collision">⚠ {{ store.errorMessage }}</p>
      <button class="primary-cta" :disabled="!store.canCompleteGoal || isSubmitting" @click="finish">{{ isSubmitting ? '등록 중...' : '여행 목표 저축 시작하기' }}</button>
    </template>

    <DatePickerSheet :open="Boolean(dateTarget)" :plan="dateTarget" @close="dateTarget = null" @confirm="store.updatePlan(dateTarget.code, $event); dateTarget = null" />
  </main>
</template>

<style scoped>
.register-page{min-height:100vh;padding:0 18px 96px;color:#111827;background:#f4f7ff}.page-header{height:72px;display:grid;grid-template-columns:36px 1fr 36px;align-items:end;padding-bottom:12px}.page-header button{border:0;background:none;text-align:left;font-size:24px}.page-header h1{font-size:17px;font-weight:800;text-align:center}.steps{display:flex;gap:7px;margin:0 0 15px}.steps i{flex:1;height:3px;border-radius:2px;background:#dce4f1}.steps i.active{background:#2469e8}.guide-card,.ai-guide{margin-top:14px;padding:15px;border-radius:15px;background:#e9f1ff}.guide-card b,.ai-guide b{font-size:13px}.guide-card p,.ai-guide p{margin-top:4px;color:#62779e;font-size:10px;line-height:1.5}.field{display:block;margin-top:17px}.field>span,.section-title h2,.country-summary h2{font-size:13px;font-weight:800}.field input{width:100%;height:50px;margin-top:8px;padding:0 13px;border:1px solid #d7e1f0;border-radius:13px;background:#fff;font-size:14px}.field small,.error-text{display:block;margin-top:5px;color:#e5484d;font-size:9px}.country-section{margin-top:18px}.section-title{display:flex;justify-content:space-between}.section-title span{color:#94a3b8;font-size:9px}.country-search{display:flex;gap:8px;align-items:center;height:46px;margin-top:10px;padding:0 14px;border:1px solid #d7e1f0;border-radius:13px;background:#fff;color:#2469e8;font-size:21px}.country-search span{color:#8393ad;font-size:11px}.chips{display:flex;flex-wrap:wrap;gap:7px;margin-top:10px}.chips button{padding:8px 11px;border:1px solid #dbe3ee;border-radius:18px;color:#64748b;background:#fff;font-size:10px}.chips button.selected{border-color:#173b86;color:#fff;background:#173b86}.selection-summary{display:flex;flex-direction:column;gap:4px;margin-top:16px;padding:14px;border:1px solid #bfd2f4;border-radius:14px;background:#fff}.selection-summary strong{color:#173b86;font-size:12px}.selection-summary span{color:#7284a2;font-size:9px}.plan-guide{display:flex;align-items:center;justify-content:space-between;margin-top:14px;padding:13px 14px;border-radius:14px;background:#fff;box-shadow:0 4px 12px rgba(20,35,70,.05)}.plan-guide div{display:flex;flex-direction:column;gap:3px}.plan-guide b{font-size:12px}.plan-guide span{color:#64748b;font-size:9px}.plan-guide button{border:0;color:#2469e8;background:none;font-size:9px;font-weight:700}.ai-guide{display:flex;gap:12px;align-items:center}.ai-guide>span{display:grid;place-items:center;width:42px;height:42px;border-radius:50%;background:#ffda7d;font-size:22px}.detail-help{margin:14px 0 2px;color:#64748b;font-size:10px}.country-card{margin-top:12px;padding:14px;border:1px solid #e0e7f0;border-left:4px solid var(--accent);border-radius:16px;background:#fff;box-shadow:0 5px 14px rgba(20,35,70,.05)}.country-card header,.country-card header div{display:flex;align-items:center;gap:7px}.country-card header small{color:#94a3b8;font-size:9px}.country-card header button{margin-left:auto;border:0;color:#e5484d;background:none;font-size:9px}.date-row{display:flex;width:100%;align-items:center;gap:8px;margin-top:13px;padding:11px;border:1px solid #e4eaf2;border-radius:11px;background:#fafcff}.date-row span{color:#64748b;font-size:9px}.date-row b{margin-left:auto;color:#173b86;font-size:10px}.ai-budget{margin-top:10px;padding:9px 10px;border-radius:10px;background:#fff4e7}.ai-budget div{display:flex;justify-content:space-between;font-size:9px}.ai-budget b{color:#c46b11}.ai-budget small{display:block;margin-top:4px;color:#ac7b43;font-size:8px}.budget-info{margin-top:10px;color:#64748b;font-size:9px}.budget-info b{margin-left:5px;color:#2469e8}.budget-input{display:flex;align-items:center;gap:8px;margin-top:8px;padding:10px;border:1px solid #a8c5f4;border-radius:10px}.budget-input span{font-size:9px}.budget-input input{min-width:0;flex:1;border:0;outline:none;text-align:right;font-weight:800}.budget-input em{color:#94a3b8;font-size:9px;font-style:normal}.card-error,.collision{margin-top:8px;padding:8px;border-radius:8px;color:#e5484d;background:#fff0f0;font-size:9px}.complete-guide{text-align:center;margin:14px 0;padding:15px;border-radius:16px;background:#e7f9f1}.complete-guide span{font-size:24px}.complete-guide p{margin-top:4px;color:#087e5b;font-size:15px;font-weight:800}.complete-guide small{color:#438e76;font-size:9px}.country-summary{margin:18px 0 14px}.country-summary h2{margin-bottom:10px}.country-summary article{display:flex;align-items:center;gap:9px;margin-top:8px;padding:13px;border-left:4px solid var(--accent);border-radius:12px;background:#fff;box-shadow:0 4px 13px rgba(20,35,70,.06)}.country-summary article div{display:flex;flex-direction:column;gap:4px}.country-summary article small{color:#94a3b8;font-size:8px}.country-summary article>b{margin-left:auto;font-size:12px}.grand-total{margin:14px 0 5px;text-align:center;font-size:28px;font-weight:800}.ticket-note{color:#cbd9ff;text-align:center;font-size:9px}.monthly-preview{display:flex;flex-direction:column;gap:5px;margin-top:14px;padding:16px;border:1px solid #a9d9cb;border-radius:15px;background:#effcf7}.monthly-preview span{color:#13856a;font-size:10px;font-weight:700}.monthly-preview b{color:#087e5b;font-size:20px}.monthly-preview small{color:#52947f;font-size:9px}.primary-cta{position:fixed;z-index:10;left:50%;bottom:18px;transform:translateX(-50%);width:min(354px,calc(100% - 36px));height:54px;border:0;border-radius:14px;color:#fff;background:#173b86;font-size:14px;font-weight:800}.primary-cta.inactive,.primary-cta:disabled{background:#aeb9cc}
</style>
