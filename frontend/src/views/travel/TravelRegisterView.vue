<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import DatePickerSheet from '@/components/savings/DatePickerSheet.vue'
import TravelTicket from '@/components/savings/TravelTicket.vue'
import { useTravelStore } from '@/stores/travel'

const router = useRouter()
const store = useTravelStore()
const step = ref(1)
const dateTarget = ref(null)
const showValidation = ref(false)
const searchKeyword = ref('')
const draggedIndex = ref(null)
let searchTimer

const budgetCategories = [
  { field: 'airfareAmount', label: '항공', icon: '✈', prepaid: true },
  { field: 'lodgingAmount', label: '숙소', icon: '🏨', prepaid: true },
  { field: 'foodAmount', label: '식비', icon: '🍴', prepaid: false },
  { field: 'activityAmount', label: '활동', icon: '🎟', prepaid: false },
  { field: 'transportAmount', label: '교통', icon: '🚆', prepaid: false },
  { field: 'otherAmount', label: '기타', icon: '•••', prepaid: false },
]

const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
const dateLabel = (value) => value ? value.replaceAll('-', '.') : '여행 날짜 선택'
const stepTitle = computed(() => ['여행 계획 등록', '여행 일정 입력', '여행 예산', '여행 목표 확인'][step.value - 1])

const countryLocalTotal = (plan) => ['foodAmount', 'activityAmount', 'transportAmount', 'otherAmount']
  .reduce((sum, field) => sum + Number(plan.budget[field] || 0), 0)
const countryPrepaidTotal = (plan) => Number(plan.budget.airfareAmount || 0) + Number(plan.budget.lodgingAmount || 0)
const recommendedLocalTotal = (plan) => ['foodAmount', 'activityAmount', 'transportAmount', 'otherAmount']
  .reduce((sum, field) => sum + Number(plan.recommendedBudget[field] || 0), 0)
const recommendedPrepaidTotal = (plan) => Number(plan.recommendedBudget.airfareAmount || 0) + Number(plan.recommendedBudget.lodgingAmount || 0)

onMounted(async () => {
  await store.loadActiveGoal({ force: true })
  if (!store.countries.length) await store.loadCountries()
})

onBeforeUnmount(() => {
  window.clearTimeout(searchTimer)
  stopPointerReorder()
})

watch(searchKeyword, (keyword) => {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => store.loadCountries(keyword.trim()), 250)
})

function goToSchedule() {
  showValidation.value = true
  store.clearError()
  if (!store.tripName.trim() || !store.selectedPlans.length) return
  step.value = 2
  showValidation.value = false
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function reorderTo(targetIndex) {
  if (draggedIndex.value === null || draggedIndex.value === targetIndex) return
  store.reorderCountries(draggedIndex.value, targetIndex)
  draggedIndex.value = targetIndex
}

function startNativeReorder(index, event) {
  draggedIndex.value = index
  event.dataTransfer.effectAllowed = 'move'
}

function finishNativeReorder() {
  draggedIndex.value = null
}

function movePointerReorder(event) {
  event.preventDefault()
  const row = document.elementFromPoint(event.clientX, event.clientY)?.closest('[data-order-index]')
  if (row) reorderTo(Number(row.dataset.orderIndex))
}

function stopPointerReorder() {
  window.removeEventListener('pointermove', movePointerReorder)
  window.removeEventListener('pointerup', stopPointerReorder)
  window.removeEventListener('pointercancel', stopPointerReorder)
  draggedIndex.value = null
}

function startPointerReorder(index, event) {
  draggedIndex.value = index
  event.preventDefault()
  window.addEventListener('pointermove', movePointerReorder, { passive: false })
  window.addEventListener('pointerup', stopPointerReorder)
  window.addEventListener('pointercancel', stopPointerReorder)
}

async function requestRecommendation() {
  showValidation.value = true
  if (!store.canReviewPlan) return
  if (await store.savePlanAndRecommend()) {
    step.value = 3
    showValidation.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

async function confirmBudget() {
  showValidation.value = true
  if (!store.canCompleteGoal) return
  if (await store.completeGoal()) {
    step.value = 4
    showValidation.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function back() {
  store.clearError()
  if (step.value > 1) {
    step.value -= 1
    showValidation.value = false
    return
  }
  router.back()
}

function finish() {
  router.push('/')
}
</script>

<template>
  <main class="register-page" :aria-busy="store.loading">
    <header class="page-header">
      <button aria-label="뒤로가기" @click="back">‹</button>
      <h1>{{ stepTitle }}</h1>
      <span />
    </header>
    <div class="steps"><i v-for="index in 4" :key="index" :class="{ active: index <= step }" /></div>

    <div v-if="store.errorMessage" class="error-banner" role="alert">
      <span>!</span>
      <p>{{ store.errorMessage }}</p>
      <button aria-label="오류 닫기" @click="store.clearError">×</button>
    </div>

    <template v-if="step === 1">
      <section class="guide-card">
        <b>어디로 떠나시나요?</b>
        <p>여행 이름과 방문 국가를 선택해 주세요. 선택한 순서가 방문 순서로 저장돼요.</p>
      </section>
      <label class="field">
        <span>여행명</span>
        <input v-model="store.tripName" maxlength="150" placeholder="예: 8월 유럽 휴가">
        <small v-if="showValidation && !store.tripName.trim()">여행명을 입력해 주세요.</small>
      </label>
      <section class="country-section">
        <div class="section-title"><h2>여행 국가 선택</h2><span>복수 선택 가능 · 최대 5개국</span></div>
        <label class="country-search">
          <span>⌕</span>
          <input v-model="searchKeyword" placeholder="국가 또는 통화 검색">
          <i v-if="store.countryLoading" class="mini-spinner" />
        </label>
        <div v-if="store.countries.length" class="chips">
          <button
            v-for="country in store.countries"
            :key="country.countryId"
            type="button"
            :class="{ selected: store.selectedCountryCodes.includes(String(country.countryId)) }"
            @click="store.toggleCountry(country.countryId)"
          >
            <img v-if="country.flagUrl" :src="country.flagUrl" alt="">
            <span v-else>{{ country.flag }}</span>
            {{ country.name }}
            <small>{{ country.currencyCode }}</small>
          </button>
        </div>
        <p v-else-if="!store.countryLoading" class="empty-copy">검색 결과가 없어요.</p>
        <p v-if="showValidation && !store.selectedPlans.length" class="error-text">여행 국가를 한 개 이상 선택해 주세요.</p>
      </section>
      <section v-if="store.selectedPlans.length" class="selection-summary">
        <div class="selection-head"><strong>방문 순서</strong><span>≡ 손잡이를 드래그해 순서를 바꿔보세요</span></div>
        <TransitionGroup name="country-order" tag="ol" class="selected-country-list">
          <li
            v-for="(plan, index) in store.selectedPlans"
            :key="plan.countryId"
            :data-order-index="index"
            :class="{ dragging: draggedIndex === index }"
            draggable="true"
            @dragstart="startNativeReorder(index, $event)"
            @dragover.prevent="reorderTo(index)"
            @dragend="finishNativeReorder"
          >
            <b>{{ index + 1 }}</b><span>{{ plan.flag }}</span><strong>{{ plan.name }}</strong><small>{{ plan.currencyCode }}</small>
            <button type="button" aria-label="방문 순서 드래그" @pointerdown="startPointerReorder(index, $event)">≡</button>
          </li>
        </TransitionGroup>
      </section>
      <button class="primary-cta" :disabled="!store.tripName.trim() || !store.selectedPlans.length" @click="goToSchedule">여행 일정 입력하기</button>
    </template>

    <template v-else-if="step === 2">
      <section class="plan-guide">
        <div><b>{{ store.tripName }}</b><span>{{ store.selectedPlans.length }}개국 여행</span></div>
        <button @click="step = 1">국가 수정</button>
      </section>
      <section class="ai-guide">
        <span>📅</span>
        <div><b>방문 순서대로 일정을 입력해 주세요</b><p>국가 간 일정은 겹치지 않아야 하며, 같은 날 다음 국가로 이동할 수 있어요.</p></div>
      </section>
      <section
        v-for="(plan, index) in store.selectedPlans"
        :key="plan.countryId"
        class="country-card schedule-card"
        :style="{ '--accent': plan.accent }"
      >
        <header>
          <div><b class="order">{{ index + 1 }}</b><span>{{ plan.flag }}</span><strong>{{ plan.name }}</strong><small>{{ plan.currencyCode }}</small></div>
          <button @click="store.toggleCountry(plan.countryId)">삭제</button>
        </header>
        <button class="date-row" @click="dateTarget = plan">
          <span>여행 날짜</span>
          <b>{{ plan.startDate ? `${dateLabel(plan.startDate)} ~ ${dateLabel(plan.endDate)}` : '날짜 선택' }}</b>
          <span>▣</span>
        </button>
        <p v-if="showValidation && store.planError(plan)" class="card-error">⚠ {{ store.planError(plan) }}</p>
      </section>
      <p v-if="showValidation && store.hasDateCollision" class="collision">⚠ 방문 순서와 국가별 일정을 다시 확인해 주세요.</p>
      <button class="primary-cta" :disabled="!store.canReviewPlan || store.loading" @click="requestRecommendation">
        {{ store.loading ? '추천 예산을 계산하고 있어요…' : '여행 예산 선택하기' }}
      </button>
    </template>

    <template v-else-if="step === 3">
      <section class="budget-hero">
        <span class="ai-orbit"><i>AI</i></span>
        <div><small>AI 추천 완료</small><h2>여행 예산을 확인해 주세요</h2><p>필요한 항목만 아래에서 바꿀 수 있어요.</p></div>
      </section>
      <section
        v-for="plan in store.selectedPlans"
        :key="plan.countryId"
        class="country-card budget-card"
        :style="{ '--accent': plan.accent }"
      >
        <header>
          <div class="budget-country"><span>{{ plan.flag }}</span><strong>{{ plan.name }}</strong><small>{{ dateLabel(plan.startDate) }} ~ {{ dateLabel(plan.endDate) }}</small></div>
        </header>

        <section class="ai-recommendation">
          <div class="ai-recommendation-head"><span><i>AI</i> 추천 예산</span><b>{{ money(recommendedLocalTotal(plan)) }}</b></div>
          <div class="ai-values">
            <span>사전 지출 <b>{{ money(recommendedPrepaidTotal(plan)) }}</b></span>
            <span>현지 여행 자금 <b>{{ money(recommendedLocalTotal(plan)) }}</b></span>
          </div>
        </section>

        <section class="editable-budget">
          <div class="manual-heading"><div><b><i>✎</i> 직접 수정</b><span>입력창을 눌러 금액을 바꾸세요</span></div><button type="button" @click="store.resetBudgetToRecommendation(plan.countryId)">추천값 복원</button></div>
          <div class="budget-section-title"><b>사전 지출</b><span>저축 목표 제외</span></div>
          <div class="budget-grid prepaid-grid">
            <label v-for="category in budgetCategories.filter((item) => item.prepaid)" :key="category.field">
              <span>{{ category.icon }} {{ category.label }}</span>
              <div><input type="number" min="0" max="100000000" :aria-label="`${plan.name} ${category.label} 예산`" :value="plan.budget[category.field]" @input="store.updateBudget(plan.countryId, category.field, $event.target.value)"><em>원</em></div>
            </label>
          </div>
          <div class="subtotal prepaid"><span>합계</span><b>{{ money(countryPrepaidTotal(plan)) }}</b></div>
          <div class="budget-section-title local"><b>현지 여행 자금</b><span>TRIP 월렛 저축</span></div>
          <div class="budget-grid">
            <label v-for="category in budgetCategories.filter((item) => !item.prepaid)" :key="category.field">
              <span>{{ category.icon }} {{ category.label }}</span>
              <div><input type="number" min="0" max="100000000" :aria-label="`${plan.name} ${category.label} 예산`" :value="plan.budget[category.field]" @input="store.updateBudget(plan.countryId, category.field, $event.target.value)"><em>원</em></div>
            </label>
          </div>
          <div class="subtotal local-total"><span>{{ plan.name }} 저축 목표</span><b>{{ money(countryLocalTotal(plan)) }}</b></div>
        </section>
      </section>
      <section class="total-preview">
        <div><span>사전 지출 총액</span><b>{{ money(store.prepaidExpenseTotal) }}</b></div>
        <div class="goal"><span>여행 저축 목표</span><b>{{ money(store.totalTargetAmount) }}</b></div>
      </section>
      <p v-if="showValidation && !store.canCompleteGoal" class="collision">⚠ 현지 여행 자금은 0원보다 커야 해요.</p>
      <button class="primary-cta" :disabled="!store.canCompleteGoal || store.loading" @click="confirmBudget">
        {{ store.loading ? '여행 목표를 확정하고 있어요…' : '이 예산으로 여행 목표 확정하기' }}
      </button>
    </template>

    <template v-else>
      <section class="complete-guide"><span>✓</span><p>여행 목표가 완성되었어요</p><small>TRIP 월렛으로 저축을 시작해 보세요.</small></section>
      <section class="country-summary">
        <h2>여행 국가별 목표</h2>
        <article v-for="plan in store.selectedPlans" :key="plan.countryId" :style="{ '--accent': plan.accent }">
          <span>{{ plan.flag }}</span>
          <div><strong>{{ plan.name }}</strong><small>{{ dateLabel(plan.startDate) }} ~ {{ dateLabel(plan.endDate) }}</small></div>
          <b>{{ money(countryLocalTotal(plan)) }}</b>
        </article>
      </section>
      <TravelTicket title="총 여행 저축 목표" :meta="`현지 여행 자금 · ${store.selectedPlans.length}개국 합산`">
        <div class="grand-total">{{ money(store.totalTargetAmount) }}</div>
        <p class="ticket-note">사전 지출 {{ money(store.prepaidExpenseTotal) }}은 별도로 기록돼요.</p>
      </TravelTicket>
      <section class="completion-stats">
        <div><span>현재 TRIP 월렛</span><b>{{ money(store.currentWalletBalance) }}</b></div>
        <div><span>남은 저축 기간</span><b>{{ store.remainingMonths }}개월</b></div>
      </section>
      <section class="monthly-preview"><span>매달 저축하면 돼요</span><b>{{ money(store.monthlySavingTarget) }}</b><small>백엔드가 목표 금액·월렛 잔액·남은 기간으로 계산했어요.</small></section>
      <button class="primary-cta" @click="finish">TRIP 월렛 저축 시작하기</button>
    </template>

    <DatePickerSheet
      :open="Boolean(dateTarget)"
      :plan="dateTarget"
      :periods="store.selectedPlans"
      @close="dateTarget = null"
      @confirm="store.updatePlan(dateTarget.countryId, $event); dateTarget = null"
    />
  </main>
</template>

<style scoped>
.register-page{min-height:100vh;padding:0 18px 96px;color:#111827;background:#f4f7ff}.page-header{height:72px;display:grid;grid-template-columns:36px 1fr 36px;align-items:end;padding-bottom:12px}.page-header button{border:0;background:none;text-align:left;font-size:24px}.page-header h1{font-size:17px;font-weight:800;text-align:center}.steps{display:flex;gap:7px;margin:0 0 15px}.steps i{flex:1;height:3px;border-radius:2px;background:#dce4f1}.steps i.active{background:#2469e8}.error-banner{display:flex;align-items:flex-start;gap:9px;margin:10px 0;padding:11px 12px;border:1px solid #ffc9cc;border-radius:12px;color:#b4232a;background:#fff2f2;font-size:10px;line-height:1.5}.error-banner>span{display:grid;place-items:center;width:18px;height:18px;border-radius:50%;color:#fff;background:#e5484d;font-weight:800}.error-banner p{flex:1}.error-banner button{border:0;color:#b4232a;background:none;font-size:17px}.guide-card,.ai-guide{margin-top:14px;padding:15px;border-radius:15px;background:#e9f1ff}.guide-card b,.ai-guide b{font-size:13px}.guide-card p,.ai-guide p{margin-top:4px;color:#62779e;font-size:10px;line-height:1.5}.field{display:block;margin-top:17px}.field>span,.section-title h2,.country-summary h2{font-size:13px;font-weight:800}.field input{width:100%;height:50px;margin-top:8px;padding:0 13px;border:1px solid #d7e1f0;border-radius:13px;background:#fff;font-size:14px}.field small,.error-text{display:block;margin-top:5px;color:#e5484d;font-size:9px}.country-section{margin-top:18px}.section-title{display:flex;justify-content:space-between}.section-title span{color:#94a3b8;font-size:9px}.country-search{display:flex;gap:8px;align-items:center;height:46px;margin-top:10px;padding:0 14px;border:1px solid #d7e1f0;border-radius:13px;background:#fff;color:#2469e8;font-size:21px}.country-search input{min-width:0;flex:1;border:0;outline:0;color:#334155;background:transparent;font-size:11px}.country-search input::placeholder{color:#8393ad}.mini-spinner{width:14px;height:14px;border:2px solid #dbe7fb;border-top-color:#2469e8;border-radius:50%;animation:spin .7s linear infinite}.chips{display:flex;flex-wrap:wrap;gap:7px;margin-top:10px}.chips button{display:flex;align-items:center;gap:4px;padding:8px 11px;border:1px solid #dbe3ee;border-radius:18px;color:#64748b;background:#fff;font-size:10px}.chips button img{width:15px;height:15px;border-radius:50%;object-fit:cover}.chips button small{color:#a0aec0;font-size:7px}.chips button.selected{border-color:#173b86;color:#fff;background:#173b86}.chips button.selected small{color:#cbd9ff}.empty-copy{padding:25px 0;color:#94a3b8;text-align:center;font-size:10px}.selection-summary{margin-top:16px;padding:14px;border:1px solid #bfd2f4;border-radius:14px;background:#fff}.selection-head{display:flex;align-items:center;justify-content:space-between}.selection-head strong{color:#173b86;font-size:12px}.selection-head span{color:#7284a2;font-size:8px}.selected-country-list{display:grid;gap:7px;margin-top:10px}.selected-country-list li{display:flex;align-items:center;gap:8px;padding:9px 10px;border:1px solid #e1e8f3;border-radius:10px;background:#f8fbff;transition:transform .2s,box-shadow .2s,opacity .2s}.selected-country-list li.dragging{opacity:.65;box-shadow:0 7px 16px rgba(36,105,232,.18)}.selected-country-list li>b{display:grid;place-items:center;width:20px;height:20px;border-radius:50%;color:#fff;background:#2469e8;font-size:9px}.selected-country-list li>span{font-size:16px}.selected-country-list li>strong{font-size:11px}.selected-country-list li>small{color:#94a3b8;font-size:8px}.selected-country-list li>button{margin-left:auto;border:0;color:#6b7f9e;background:none;font-size:21px;line-height:1;cursor:grab;touch-action:none}.country-order-move{transition:transform .22s}.plan-guide{display:flex;align-items:center;justify-content:space-between;margin-top:14px;padding:13px 14px;border-radius:14px;background:#fff;box-shadow:0 4px 12px rgba(20,35,70,.05)}.plan-guide div{display:flex;flex-direction:column;gap:3px}.plan-guide b{font-size:12px}.plan-guide span{color:#64748b;font-size:9px}.plan-guide button{border:0;color:#2469e8;background:none;font-size:9px;font-weight:700}.ai-guide{display:flex;gap:12px;align-items:center}.ai-guide>span{display:grid;place-items:center;width:42px;height:42px;border-radius:50%;background:#ffda7d;font-size:22px}.country-card{margin-top:12px;padding:14px;border:1px solid #e0e7f0;border-left:4px solid var(--accent);border-radius:16px;background:#fff;box-shadow:0 5px 14px rgba(20,35,70,.05)}.country-card header,.country-card header div{display:flex;align-items:center;gap:7px}.country-card header small{color:#94a3b8;font-size:9px}.country-card header button{margin-left:auto;border:0;color:#e5484d;background:none;font-size:9px}.order{display:grid;place-items:center;width:19px;height:19px;border-radius:50%;color:#fff;background:var(--accent);font-size:9px}.date-row{display:flex;width:100%;align-items:center;gap:8px;margin-top:13px;padding:12px;border:1px solid #e4eaf2;border-radius:11px;background:#fafcff}.date-row span{color:#64748b;font-size:9px}.date-row b{margin-left:auto;color:#173b86;font-size:10px}.card-error,.collision{margin-top:8px;padding:8px;border-radius:8px;color:#e5484d;background:#fff0f0;font-size:9px}.budget-hero{display:flex;align-items:center;gap:15px;margin-top:14px;padding:16px 15px;border-radius:18px;color:#fff;background:linear-gradient(135deg,#183b87,#2868dc);box-shadow:0 10px 22px rgba(35,83,170,.2);animation:rise-in .55s ease both}.budget-hero small{color:#bdd4ff;font-size:8px;font-weight:800}.budget-hero h2{margin-top:3px;font-size:16px;font-weight:900}.budget-hero p{margin-top:4px;color:#d9e6ff;font-size:9px}.ai-orbit{position:relative;display:grid;flex:0 0 52px;height:52px;place-items:center;border:1px solid rgba(255,255,255,.45);border-radius:50%}.ai-orbit::before{content:'';position:absolute;inset:-5px;border:2px dashed #82b2ff;border-radius:50%;animation:orbit 5s linear infinite}.ai-orbit i{display:grid;place-items:center;width:39px;height:39px;border-radius:50%;color:#173b86;background:#fff;font-size:15px;font-style:normal;font-weight:900}.budget-card{animation:rise-in .45s ease both}.budget-card:nth-of-type(2){animation-delay:.08s}.budget-card:nth-of-type(3){animation-delay:.16s}.budget-country>span{font-size:24px}.budget-country>strong{font-size:15px}.ai-recommendation{margin-top:12px;padding:12px;border:1px solid #b7d4ff;border-radius:13px;background:linear-gradient(135deg,#eef5ff,#f8fbff)}.ai-recommendation-head{display:flex;align-items:center;justify-content:space-between}.ai-recommendation-head span{color:#174fae;font-size:11px;font-weight:800}.ai-recommendation-head i{display:inline-grid;place-items:center;width:24px;height:24px;margin-right:5px;border-radius:7px;color:#fff;background:#2469e8;font-size:9px;font-style:normal}.ai-recommendation-head>b{color:#174fae;font-size:17px}.ai-values{display:grid;grid-template-columns:1fr 1fr;gap:7px;margin-top:9px}.ai-values span{padding:8px;border-radius:8px;color:#64748b;background:#fff;font-size:8px}.ai-values b{display:block;margin-top:3px;color:#1e3a6e;font-size:10px}.editable-budget{margin-top:12px;padding:12px;border:2px solid #5a82d0;border-radius:14px;background:#f8fbff}.manual-heading{display:flex;align-items:center;justify-content:space-between}.manual-heading div{display:flex;flex-direction:column;gap:2px}.manual-heading b{color:#173b86;font-size:13px}.manual-heading b i{display:inline-grid;place-items:center;width:21px;height:21px;border-radius:7px;color:#fff;background:#2469e8;font-style:normal}.manual-heading span{color:#64748b;font-size:8px}.manual-heading button{border:0;color:#2469e8;background:none;font-size:8px;font-weight:800}.budget-section-title{display:flex;justify-content:space-between;margin-top:13px}.budget-section-title b{color:#9b6117;font-size:10px}.budget-section-title span{color:#b8874d;font-size:8px}.budget-section-title.local b{color:#174fae}.budget-section-title.local span{color:#5b7fb9}.budget-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:7px}.budget-grid label{padding:9px;border:1px solid #aac6f1;border-radius:10px;background:#fff;box-shadow:0 2px 5px rgba(36,105,232,.06)}.budget-grid.prepaid-grid label{border-color:#e9c98f}.budget-grid label>span{color:#52637c;font-size:9px;font-weight:700}.budget-grid label div{display:flex;align-items:center;margin-top:6px;padding:6px 4px 2px;border-top:1px solid #dce6f5;background:#fff}.budget-grid input{min-width:0;width:100%;border:0;outline:0;background:transparent;text-align:right;font-size:13px;font-weight:900}.budget-grid input:focus{color:#174fae}.budget-grid label:focus-within{border-color:#2469e8;box-shadow:0 0 0 3px rgba(36,105,232,.12)}.budget-grid em{margin-left:3px;color:#64748b;font-size:8px;font-style:normal}.subtotal{display:flex;justify-content:space-between;margin-top:7px;padding:8px 10px;border-radius:9px;font-size:9px}.subtotal.prepaid{color:#9b6117;background:#fff4e7}.subtotal.local-total{color:#174fae;background:#eaf2ff}.total-preview{margin-top:15px;padding:14px;border-radius:15px;background:#fff;box-shadow:0 5px 14px rgba(20,35,70,.06)}.total-preview div{display:flex;justify-content:space-between;color:#64748b;font-size:10px}.total-preview .goal{margin-top:10px;padding-top:10px;border-top:1px dashed #dbe3ee;color:#173b86;font-size:13px}.complete-guide{text-align:center;margin:14px 0;padding:15px;border-radius:16px;background:#e7f9f1}.complete-guide span{display:grid;place-items:center;width:34px;height:34px;margin:0 auto;border-radius:50%;color:#fff;background:#10aa82;font-size:20px}.complete-guide p{margin-top:7px;color:#087e5b;font-size:15px;font-weight:800}.complete-guide small{color:#438e76;font-size:9px}.country-summary{margin:18px 0 14px}.country-summary h2{margin-bottom:10px}.country-summary article{display:flex;align-items:center;gap:9px;margin-top:8px;padding:13px;border-left:4px solid var(--accent);border-radius:12px;background:#fff;box-shadow:0 4px 13px rgba(20,35,70,.06)}.country-summary article div{display:flex;flex-direction:column;gap:4px}.country-summary article small{color:#94a3b8;font-size:8px}.country-summary article>b{margin-left:auto;font-size:12px}.grand-total{margin:14px 0 5px;text-align:center;font-size:28px;font-weight:800}.ticket-note{color:#cbd9ff;text-align:center;font-size:9px}.completion-stats{display:grid;grid-template-columns:1fr 1fr;gap:9px;margin-top:14px}.completion-stats div{display:flex;flex-direction:column;gap:5px;padding:13px;border:1px solid #dbe3ee;border-radius:13px;background:#fff}.completion-stats span{color:#64748b;font-size:9px}.completion-stats b{font-size:13px}.monthly-preview{display:flex;flex-direction:column;gap:5px;margin-top:10px;padding:16px;border:1px solid #a9d9cb;border-radius:15px;background:#effcf7}.monthly-preview span{color:#13856a;font-size:10px;font-weight:700}.monthly-preview b{color:#087e5b;font-size:20px}.monthly-preview small{color:#52947f;font-size:9px}.primary-cta{position:fixed;z-index:10;left:50%;bottom:18px;transform:translateX(-50%);width:min(354px,calc(100% - 36px));height:54px;border:0;border-radius:14px;color:#fff;background:#173b86;font-size:14px;font-weight:800}.primary-cta:disabled{background:#aeb9cc}.primary-cta:not(:disabled):active{transform:translateX(-50%) scale(.99)}@keyframes spin{to{transform:rotate(360deg)}}@keyframes rise-in{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}@keyframes orbit{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.budget-hero,.budget-card,.ai-orbit::before{animation:none}}
</style>
