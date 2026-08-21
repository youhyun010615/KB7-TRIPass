<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DatePickerSheet from '@/components/savings/DatePickerSheet.vue'
import TravelTicket from '@/components/savings/TravelTicket.vue'
import calendarIcon from '@/assets/icons/calendar.svg'
import { fetchWalletAccounts, withdrawWallet } from '@/api/wallet'
import { useTravelStore, flagIconClass } from '@/stores/travel'

const router = useRouter()
const route = useRoute()
const store = useTravelStore()
const isEditMode = computed(() => route.query.mode === 'edit')
const isOnboarding = computed(() => route.query.onboarding === '1')
const usesGuidedRegisterDesign = computed(() => isOnboarding.value || isEditMode.value)
const registrationQuery = computed(() => ({
  ...(isEditMode.value ? { mode: 'edit' } : {}),
  ...(isOnboarding.value ? { onboarding: '1' } : {}),
}))
// code가 커뮤니티 제공 ISO 2자리(예: FR)면 flag-icons를, 아니면(통화코드 폴백 등) null을 반환한다.
function planFlagClass(code) {
  return code && code.length === 2 ? flagIconClass(code) : null
}
const step = ref(route.query.walletStep === '1' ? 5 : (route.name === 'TravelRegisterSchedule' ? 2 : 1))
const dateTarget = ref(null)
const showValidation = ref(false)
const searchKeyword = ref('')
const draggedIndex = ref(null)
const countryDropdownOpen = ref(false)
const walletAccounts = ref([])
const selectedWalletAccountId = ref(null)
const walletStepLoading = ref(false)
const walletStepError = ref('')
const showWalletWithdrawSheet = ref(false)
const walletResolution = ref(null)
const activeBudgetCountryId = ref(null)
let searchTimer

function toggleCountryFromDropdown(countryId) {
  const changed = store.toggleCountry(countryId)
  if (!changed) return
  store.clearError()
  searchKeyword.value = ''
  countryDropdownOpen.value = false
}

const sortedCountries = computed(() => {
  const keyword = searchKeyword.value.trim().toLocaleLowerCase('ko-KR')
  return [...store.countries]
    .filter((country) => !keyword || [country.name, country.currencyCode, country.code]
      .some((value) => String(value || '').toLocaleLowerCase('ko-KR').includes(keyword)))
    .sort((a, b) => {
    if (a.selectable !== b.selectable) return a.selectable ? -1 : 1
    return (a.name || '').localeCompare(b.name || '', 'ko-KR')
    })
})

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
const stepTitle = computed(() => ['여행 계획 등록', '여행 일정 입력', '여행 예산', '여행 목표 확인', '월렛 자금 선택'][step.value - 1])
const registrationHeroTitle = computed(() => {
  if (step.value === 5) return '마지막으로 월렛 자금을 정해 주세요'
  if (step.value === 3) return '국가별 예산을 확인해 주세요'
  if (isEditMode.value) {
    return step.value === 4 ? '수정할 여행 목표를 최종 확인해 주세요' : '현재 여행 계획을 다시 확인해 볼까요?'
  }
  return step.value === 4 ? '여행 목표를 최종 확인해 주세요' : '나만의 여행 계획을 만들어 볼까요?'
})
const registrationHeroDescription = computed(() => {
  if (step.value === 5) return '남아 있는 월렛 잔액을 이번 여행에 사용할지 선택하면 등록이 완료돼요.'
  if (step.value === 3) return 'AI가 추천한 예산이에요. 필요한 항목만 바꿀 수 있어요.'
  if (isEditMode.value) {
    return step.value === 4
      ? '변경한 여행 일정과 목표 예산을 반영하기 전 한 번 더 확인해요.'
      : '여행지와 일정, 목표 예산을 현재 계획에 맞게 수정해요.'
  }
  return step.value === 4
    ? '국가별 목표와 총 여행 저축 목표를 한 번 더 확인해요.'
    : '여행지와 일정에 맞춰 필요한 목표 예산까지 함께 준비해요.'
})

const countryLocalTotal = (plan) => ['foodAmount', 'activityAmount', 'transportAmount', 'otherAmount']
  .reduce((sum, field) => sum + Number(plan.budget[field] || 0), 0)
const countryPrepaidTotal = (plan) => Number(plan.budget.airfareAmount || 0) + Number(plan.budget.lodgingAmount || 0)
const recommendedLocalTotal = (plan) => ['foodAmount', 'activityAmount', 'transportAmount', 'otherAmount']
  .reduce((sum, field) => sum + Number(plan.recommendedBudget[field] || 0), 0)
const recommendedPrepaidTotal = (plan) => Number(plan.recommendedBudget.airfareAmount || 0) + Number(plan.recommendedBudget.lodgingAmount || 0)
// 예산 입력 단계에서는 과거에 확정된 completion 값이 아니라 현재 입력 중인
// 국가별 예산을 합산해야 각 입력 변경이 하단 총액에 즉시 반영된다.
const liveTargetAmount = computed(() =>
  store.selectedPlans.reduce((sum, plan) => sum + countryLocalTotal(plan), 0),
)
const livePrepaidExpenseTotal = computed(() =>
  store.selectedPlans.reduce((sum, plan) => sum + countryPrepaidTotal(plan), 0),
)
const activeBudgetPlan = computed(() =>
  store.selectedPlans.find(plan => plan.countryId === activeBudgetCountryId.value)
    ?? store.selectedPlans[0]
    ?? null,
)
const walletDecisionAmount = computed(() => Number(
  store.lifecycle?.walletReflectAmount ?? store.currentWalletBalance ?? 0,
))
const walletDecisionRequired = computed(() => Boolean(store.lifecycle?.needsWalletReflectPrompt))
const walletStatusLabel = computed(() => {
  if (walletDecisionRequired.value) return walletDecisionAmount.value > 0 ? '선택 필요' : '0원 시작'
  if (walletResolution.value === 'included') return '포함 완료'
  if (walletResolution.value === 'withdrawn') return '송금 완료'
  return walletDecisionAmount.value > 0 ? '선택 완료' : '0원 시작'
})
const selectedWalletAccount = computed(() => walletAccounts.value.find(
  account => Number(account.accountId ?? account.id) === Number(selectedWalletAccountId.value),
))

onMounted(async () => {
  if (isEditMode.value) {
    // 홈에서 수정으로 진입하거나 수정 URL을 새로고침해도 서버 값을 다시 채운다.
    await store.loadActiveGoal({ force: true })
  } else if (!store.tripId || !store.selectedPlans.length) {
    await store.loadActiveGoal({ force: store.initialized })
  }
  if (!store.countries.length) await store.loadCountries()
  if (step.value === 5) await loadWalletStep()
})

onBeforeUnmount(() => {
  window.clearTimeout(searchTimer)
  stopPointerReorder()
})

watch(searchKeyword, (keyword) => {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => store.loadCountries(keyword.trim()), 250)
})

watch(
  () => store.selectedPlans.map(plan => plan.countryId),
  (countryIds) => {
    if (!countryIds.includes(activeBudgetCountryId.value)) {
      activeBudgetCountryId.value = countryIds[0] ?? null
    }
  },
  { immediate: true },
)

watch(() => route.name, (routeName) => {
  if (routeName === 'TravelRegisterSchedule') step.value = 2
  if (routeName === 'TravelRegister') step.value = route.query.walletStep === '1' ? 5 : 1
  showValidation.value = false
  store.clearError()
  window.requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
})

function goToTripInfo() {
  router.push({ name: 'TravelRegister', query: registrationQuery.value })
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

function reviewGoal() {
  showValidation.value = true
  if (!store.canCompleteGoal) return
  step.value = 4
  showValidation.value = false
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function confirmBudget() {
  showValidation.value = true
  if (!store.canCompleteGoal) return
  if (await store.completeGoal()) {
    step.value = 5
    showValidation.value = false
    await loadWalletStep()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

async function loadWalletStep() {
  walletStepLoading.value = true
  walletStepError.value = ''
  try {
    await store.loadLifecycle()
    walletAccounts.value = await fetchWalletAccounts() || []
    const preferred = walletAccounts.value.find(account => account.primary || account.isPrimary) ?? walletAccounts.value[0]
    selectedWalletAccountId.value = preferred?.accountId ?? preferred?.id ?? null
  } catch (error) {
    walletStepError.value = error.response?.data?.message || '월렛 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    walletStepLoading.value = false
  }
}

async function includeWalletBalance() {
  if (walletStepLoading.value) return
  walletStepLoading.value = true
  walletStepError.value = ''
  try {
    if (walletDecisionRequired.value) {
      await store.resolveWalletBalanceReflect(true)
      walletResolution.value = 'included'
    }
  } catch (error) {
    walletStepError.value = error.response?.data?.message || '월렛 선택을 처리하지 못했어요.'
  } finally {
    walletStepLoading.value = false
  }
}

function openWalletWithdrawal() {
  walletStepError.value = ''
  if (!walletAccounts.value.length) {
    walletStepError.value = '돈을 받을 계좌가 없어요. 먼저 계좌를 연결해 주세요.'
    return
  }
  showWalletWithdrawSheet.value = true
}

async function withdrawWalletBalance() {
  if (!selectedWalletAccount.value || walletStepLoading.value) return
  walletStepLoading.value = true
  walletStepError.value = ''
  try {
    await withdrawWallet({
      targetAccountId: selectedWalletAccount.value.accountId ?? selectedWalletAccount.value.id,
      amount: walletDecisionAmount.value,
      idempotencyKey: `trip-goal-wallet-${store.tripId}-${selectedWalletAccount.value.accountId ?? selectedWalletAccount.value.id}`,
    })
    await store.resolveWalletBalanceReflect(false)
    walletResolution.value = 'withdrawn'
    showWalletWithdrawSheet.value = false
  } catch (error) {
    walletStepError.value = error.response?.data?.message || '월렛 잔액을 계좌로 보내지 못했어요.'
  } finally {
    walletStepLoading.value = false
  }
}

async function completeWalletStep() {
  if (walletStepLoading.value || store.homeLoading) return
  if (walletDecisionRequired.value) {
    await includeWalletBalance()
    if (walletDecisionRequired.value) return
  }
  await finish()
}

function back() {
  store.clearError()
  if (step.value > 1) {
    if (step.value === 2) {
      goToTripInfo()
      return
    }
    step.value -= 1
    showValidation.value = false
    return
  }
  if (isOnboarding.value) {
    goToOnboardingHub()
    return
  }
  router.replace({ name: 'Home' })
}

async function finish() {
  await store.loadHomeDashboard({ force: true })
  if (isOnboarding.value) {
    goToOnboardingHub()
    return
  }
  router.push('/')
}

function goToOnboardingHub() {
  router.replace({ name: 'TripOnboarding' })
}
</script>

<template>
  <main
    class="register-page"
    :class="{
      'onboarding-register': usesGuidedRegisterDesign,
      'budget-summary-step': step === 3,
    }"
    :aria-busy="store.loading"
  >
    <header class="page-header">
      <button aria-label="뒤로가기" @click="back">‹</button>
      <h1>{{ isEditMode ? stepTitle.replace('등록', '수정') : stepTitle }}</h1>
      <button v-if="isOnboarding" type="button" class="onboarding-skip" @click="goToOnboardingHub">준비 화면</button>
      <span v-else />
    </header>
    <div class="steps"><i v-for="index in 5" :key="index" :class="{ active: index <= step }" /></div>

    <section v-if="usesGuidedRegisterDesign" :key="`hero-${step}`" class="registration-hero">
      <span>TRIP PLAN BOARDING PASS</span>
      <small>STEP {{ step }} / 5</small>
      <h2>{{ registrationHeroTitle }}</h2>
      <p>{{ registrationHeroDescription }}</p>
    </section>

    <section :key="step" class="register-content">

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
        <div class="country-dropdown">
          <label class="country-search country-search-main">
            <span>⌕</span>
            <input
              v-model="searchKeyword"
              placeholder="국가 또는 통화 검색"
              @focus="countryDropdownOpen = true"
              @input="countryDropdownOpen = true"
            >
            <i v-if="store.countryLoading" class="mini-spinner" />
          </label>

          <template v-if="countryDropdownOpen">
            <div class="country-dropdown-panel">
              <ul v-if="sortedCountries.length" class="country-dropdown-list country-search-results">
                <li v-for="country in sortedCountries" :key="country.countryId">
                  <button
                    type="button"
                    :class="{
                      selected: store.selectedCountryCodes.includes(String(country.countryId)),
                      disabled: !country.selectable,
                    }"
                    :disabled="!country.selectable"
                    :aria-disabled="!country.selectable"
                    @click="toggleCountryFromDropdown(country.countryId)"
                  >
                    <span class="country-dropdown-check">✓</span>
                    <span v-if="country.code?.length === 2" :class="flagIconClass(country.code)" class="fi-inline" />
                    <span v-else>{{ country.flag }}</span>
                    {{ country.name }}
                    <small v-if="!country.selectable">선택 불가</small>
                    <small v-else>{{ country.currencyCode }}</small>
                  </button>
                </li>
              </ul>
              <p v-else-if="!store.countryLoading" class="empty-copy">검색 결과가 없어요.</p>
            </div>
          </template>
        </div>
        <p v-if="showValidation && !store.selectedPlans.length" class="error-text">여행 국가를 한 개 이상 선택해 주세요.</p>
      </section>
      <section v-if="store.selectedPlans.length && !countryDropdownOpen" class="selection-summary">
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
            <b>{{ index + 1 }}</b><span v-if="planFlagClass(plan.code)" :class="planFlagClass(plan.code)" class="fi-inline"></span><span v-else>{{ plan.flag }}</span><strong>{{ plan.name }}</strong><small>{{ plan.currencyCode }}</small>
            <button
              type="button"
              class="country-remove-button"
              :aria-label="`${plan.name} 삭제`"
              @pointerdown.stop
              @click.stop="store.toggleCountry(plan.countryId)"
            >×</button>
            <button
              type="button"
              class="country-drag-button"
              aria-label="방문 순서 드래그"
              @pointerdown="startPointerReorder(index, $event)"
            >≡</button>
          </li>
        </TransitionGroup>
      </section>
      <RouterLink
        v-if="!countryDropdownOpen && store.tripName.trim() && store.selectedPlans.length"
        class="primary-cta"
        :to="{ name: 'TravelRegisterSchedule', query: registrationQuery }"
      >여행 일정 입력하기</RouterLink>
      <button
        v-else-if="!countryDropdownOpen"
        type="button"
        class="primary-cta"
        disabled
      >여행 일정 입력하기</button>
    </template>

    <template v-else-if="step === 2">
      <section class="plan-guide">
        <div><b>{{ store.tripName }}</b><span>{{ store.selectedPlans.length }}개국 여행</span></div>
        <button @click="goToTripInfo">국가 수정</button>
      </section>
      <section class="ai-guide">
        <div><b>방문 순서대로 일정을 입력해 주세요</b><p>국가 간 일정은 겹치지 않아야 하며, 같은 날 다음 국가로 이동할 수 있어요.</p></div>
      </section>
      <section
        v-for="(plan, index) in store.selectedPlans"
        :key="plan.countryId"
        class="country-card schedule-card"
        :style="{ '--accent': plan.accent }"
      >
        <header>
          <div><b class="order">{{ index + 1 }}</b><span v-if="planFlagClass(plan.code)" :class="planFlagClass(plan.code)" class="fi-inline"></span><span v-else>{{ plan.flag }}</span><strong>{{ plan.name }}</strong><small>{{ plan.currencyCode }}</small></div>
          <button @click="store.toggleCountry(plan.countryId)">삭제</button>
        </header>
        <button class="date-row" @click="dateTarget = plan">
          <span>여행 날짜</span>
          <b>{{ plan.startDate ? `${dateLabel(plan.startDate)} ~ ${dateLabel(plan.endDate)}` : '날짜 선택' }}</b>
          <img :src="calendarIcon" alt="" aria-hidden="true">
        </button>
        <p v-if="showValidation && store.planError(plan)" class="card-error">⚠ {{ store.planError(plan) }}</p>
      </section>
      <p v-if="showValidation && store.hasDateCollision" class="collision">⚠ 방문 순서와 국가별 일정을 다시 확인해 주세요.</p>
      <button class="primary-cta" :disabled="!store.canReviewPlan || store.loading" @click="requestRecommendation">
        {{ store.loading ? '추천 예산을 계산하고 있어요…' : '여행 예산 선택하기' }}
      </button>
    </template>

    <template v-else-if="step === 3">
      <nav class="budget-country-tabs" aria-label="국가별 여행 예산">
        <button
          v-for="plan in store.selectedPlans"
          :key="plan.countryId"
          type="button"
          :class="{ active: activeBudgetPlan?.countryId === plan.countryId }"
          @click="activeBudgetCountryId = plan.countryId"
        >
          <span v-if="planFlagClass(plan.code)" :class="planFlagClass(plan.code)" class="fi-inline"></span>
          <span v-else>{{ plan.flag }}</span>
          <b>{{ plan.name }}</b>
        </button>
      </nav>
      <section
        v-if="activeBudgetPlan"
        :key="activeBudgetPlan.countryId"
        class="country-card budget-card budget-card-active"
        :style="{ '--accent': activeBudgetPlan.accent }"
      >
        <header class="budget-country-header">
          <div class="budget-country">
            <strong>{{ activeBudgetPlan.name }}</strong>
            <small>{{ dateLabel(activeBudgetPlan.startDate) }} ~ {{ dateLabel(activeBudgetPlan.endDate) }}</small>
          </div>
          <span class="budget-recommendation-status">
            <i class="budget-ai-motion"><b>AI</b></i>
            추천 완료
          </span>
        </header>

        <section class="budget-recommendation-panel">
          <header class="budget-recommendation-title">
            <i aria-hidden="true">AI</i>
            <b>AI 추천 예산</b>
          </header>
          <div class="budget-recommendation-summary">
            <div><span>사전 지출 <small>(AI 추천)</small></span><b>{{ money(recommendedPrepaidTotal(activeBudgetPlan)) }}</b></div>
            <div><span>현지 여행 자금 <small>(AI 추천)</small></span><b>{{ money(recommendedLocalTotal(activeBudgetPlan)) }}</b></div>
          </div>
        </section>

        <section class="editable-budget">
          <div class="manual-heading"><div><b><i>✎</i> 아래 금액을 눌러 직접 수정할 수 있어요</b></div><button type="button" @click="store.resetBudgetToRecommendation(activeBudgetPlan.countryId)">추천값 복원</button></div>
          <div class="budget-section-title"><b>사전 지출</b><span>저축 목표 제외</span></div>
          <div class="budget-grid prepaid-grid">
            <label v-for="category in budgetCategories.filter((item) => item.prepaid)" :key="category.field">
              <span>{{ category.icon }} {{ category.label }}</span>
              <div><input type="number" min="0" max="100000000" :aria-label="`${activeBudgetPlan.name} ${category.label} 예산`" :value="activeBudgetPlan.budget[category.field]" @input="store.updateBudget(activeBudgetPlan.countryId, category.field, $event.target.value)"><em>원</em></div>
            </label>
          </div>
          <div class="subtotal prepaid"><span>합계</span><b>{{ money(countryPrepaidTotal(activeBudgetPlan)) }}</b></div>
          <div class="budget-section-title local"><b>현지 여행 자금</b><span>TRIP 월렛 저축</span></div>
          <div class="budget-grid">
            <label v-for="category in budgetCategories.filter((item) => !item.prepaid)" :key="category.field">
              <span>{{ category.icon }} {{ category.label }}</span>
              <div><input type="number" min="0" max="100000000" :aria-label="`${activeBudgetPlan.name} ${category.label} 예산`" :value="activeBudgetPlan.budget[category.field]" @input="store.updateBudget(activeBudgetPlan.countryId, category.field, $event.target.value)"><em>원</em></div>
            </label>
          </div>
          <div class="subtotal local-total"><span>{{ activeBudgetPlan.name }} 저축 목표</span><b>{{ money(countryLocalTotal(activeBudgetPlan)) }}</b></div>
        </section>
      </section>
      <section class="budget-fixed-summary">
        <section class="total-preview">
          <div><span>사전 지출 총액</span><b>{{ money(livePrepaidExpenseTotal) }}</b></div>
          <div class="goal">
            <span>여행 저축 목표 · {{ store.selectedPlans.length }}개국 합산</span>
            <b>{{ money(liveTargetAmount) }}</b>
          </div>
        </section>
        <p v-if="showValidation && !store.canCompleteGoal" class="collision">⚠ 현지 여행 자금은 0원보다 커야 해요.</p>
        <button class="primary-cta" :disabled="!store.canCompleteGoal || store.loading" @click="reviewGoal">
          여행 목표 최종 확인하기
        </button>
      </section>
    </template>

    <template v-else-if="step === 4">
      <section class="country-summary">
        <h2>여행 국가별 목표</h2>
        <article v-for="plan in store.selectedPlans" :key="plan.countryId" :style="{ '--accent': plan.accent }">
          <span v-if="planFlagClass(plan.code)" :class="planFlagClass(plan.code)" class="fi-inline"></span><span v-else>{{ plan.flag }}</span>
          <div><strong>{{ plan.name }}</strong><small>{{ dateLabel(plan.startDate) }} ~ {{ dateLabel(plan.endDate) }}</small></div>
          <b>{{ money(countryLocalTotal(plan)) }}</b>
        </article>
      </section>
      <TravelTicket title="총 여행 저축 목표" :meta="`현지 여행 자금 · ${store.selectedPlans.length}개국 합산`">
        <div class="grand-total">{{ money(liveTargetAmount) }}</div>
        <p class="ticket-note">사전 지출 {{ money(livePrepaidExpenseTotal) }}은 별도로 기록돼요.</p>
      </TravelTicket>
      <section class="goal-review-note">
        <b>이 금액으로 여행 목표를 만들까요?</b>
        <p>목표를 확정하면 마지막 단계에서 현재 월렛 잔액을 포함할지 선택해요.</p>
      </section>
      <button class="primary-cta" :disabled="!store.canCompleteGoal || store.loading" @click="confirmBudget">
        {{ store.loading ? '여행 목표를 확정하고 있어요…' : '여행 목표 확정하기' }}
      </button>
    </template>

    <template v-else>
      <TravelTicket title="총 여행 저축 목표" :meta="`현지 여행 자금 · ${store.selectedPlans.length}개국 합산`">
        <div class="grand-total">{{ money(store.totalTargetAmount || liveTargetAmount) }}</div>
        <p class="ticket-note">사전 지출 {{ money(store.prepaidExpenseTotal || livePrepaidExpenseTotal) }}은 별도로 기록돼요.</p>
      </TravelTicket>
      <section v-if="walletStepLoading && !store.lifecycle" class="wallet-loading">월렛 정보를 확인하고 있어요…</section>
      <section v-else class="onboarding-wallet-card" :class="{ resolved: !walletDecisionRequired }">
        <div class="onboarding-wallet-top">
          <div><small>TRIPASS WALLET</small><b>월렛 잔액</b></div>
          <span>{{ walletStatusLabel }}</span>
        </div>
        <strong class="onboarding-wallet-balance">{{ money(walletDecisionAmount) }}</strong>
        <p v-if="walletDecisionRequired && walletDecisionAmount > 0">현재 월렛에 남아 있는 잔액을 여행 목표 금액에 포함하시겠어요? 원하지 않으면 연결 계좌로 보낼 수 있어요.</p>
        <p v-else-if="walletResolution === 'included'">
          월렛 잔액을 여행 저축에 포함했어요. 앞으로 {{ store.remainingMonths }}개월 동안 매월 {{ money(store.monthlySavingTarget) }}씩 모으면 돼요.
        </p>
        <p v-else-if="walletResolution === 'withdrawn'">선택한 연결 계좌로 잔액을 보냈어요. 이번 여행 월렛은 0원부터 시작해요.</p>
        <p v-else-if="walletDecisionAmount === 0">현재 월렛에 남은 돈이 없어요. 추가 금액 없이 이번 여행 월렛은 0원으로 시작해요.</p>
        <p v-else>월렛 자금 선택이 완료됐어요. 다음 단계에서 남은 여행 준비를 이어갈 수 있어요.</p>
        <div class="wallet-decision-actions">
          <button v-if="walletDecisionRequired && walletDecisionAmount > 0" type="button" class="secondary" :disabled="walletStepLoading" @click="openWalletWithdrawal">연결 계좌로 보내기</button>
          <button v-if="walletDecisionRequired && walletDecisionAmount > 0" type="button" :disabled="walletStepLoading" @click="includeWalletBalance">여행 목표에 포함하기</button>
        </div>
      </section>
      <p v-if="walletStepError" class="wallet-step-error">{{ walletStepError }}</p>
      <button
        v-if="!walletDecisionRequired || walletDecisionAmount === 0"
        class="primary-cta"
        :disabled="walletStepLoading || store.homeLoading"
        @click="completeWalletStep"
      >
        {{ walletStepLoading || store.homeLoading ? '처리하고 있어요…' : '다음' }}
      </button>
    </template>
    </section>

    <DatePickerSheet
      :open="Boolean(dateTarget)"
      :plan="dateTarget"
      :periods="store.selectedPlans"
      @close="dateTarget = null"
      @confirm="store.updatePlan(dateTarget.countryId, $event); dateTarget = null"
    />

    <Teleport to="body">
      <div v-if="showWalletWithdrawSheet" class="wallet-withdraw-backdrop" @click.self="showWalletWithdrawSheet = false">
        <section class="wallet-withdraw-sheet" role="dialog" aria-modal="true" aria-labelledby="wallet-withdraw-title">
          <i />
          <small>WALLET TRANSFER</small>
          <h2 id="wallet-withdraw-title">월렛 잔액을 어디로 보낼까요?</h2>
          <p>{{ money(walletDecisionAmount) }} 전액을 선택한 연결 계좌로 보내요.</p>
          <div class="wallet-account-list">
            <button
              v-for="account in walletAccounts"
              :key="account.accountId ?? account.id"
              type="button"
              :class="{ selected: Number(selectedWalletAccountId) === Number(account.accountId ?? account.id) }"
              @click="selectedWalletAccountId = account.accountId ?? account.id"
            >
              <span>{{ (account.bankName || account.accountName || '계').slice(0, 1) }}</span>
              <div><b>{{ account.accountName || account.bankName || '연결 계좌' }}</b><small>{{ account.bankName }} {{ account.maskedAccountNumber || account.number }}</small></div>
              <em>{{ Number(selectedWalletAccountId) === Number(account.accountId ?? account.id) ? '선택' : '' }}</em>
            </button>
          </div>
          <p v-if="walletStepError" class="wallet-step-error">{{ walletStepError }}</p>
          <div class="wallet-withdraw-actions">
            <button type="button" class="secondary" :disabled="walletStepLoading" @click="showWalletWithdrawSheet = false">취소</button>
            <button type="button" :disabled="!selectedWalletAccount || walletStepLoading" @click="withdrawWalletBalance">
              {{ walletStepLoading ? '송금 중…' : '이 계좌로 송금하기' }}
            </button>
          </div>
        </section>
      </div>
    </Teleport>
  </main>
</template>

<style scoped>
.register-page{min-height:100vh;padding:0 18px 96px;color:#111827;background:#f4f7ff}.page-header{height:54px;display:grid;grid-template-columns:36px 1fr 36px;align-items:end;padding-bottom:12px}.page-header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}.page-header h1{font-size:17px;font-weight:800;text-align:center}.steps{display:flex;gap:7px;margin:0 0 15px}.steps i{flex:1;height:3px;border-radius:2px;background:#dce4f1}.steps i.active{background:#2469e8}.error-banner{display:flex;align-items:flex-start;gap:9px;margin:10px 0;padding:11px 12px;border:1px solid #ffc9cc;border-radius:12px;color:#b4232a;background:#fff2f2;font-size:10px;line-height:1.5}.error-banner>span{display:grid;place-items:center;width:18px;height:18px;border-radius:50%;color:#fff;background:#e5484d;font-weight:800}.error-banner p{flex:1}.error-banner button{border:0;color:#b4232a;background:none;font-size:17px}.guide-card,.ai-guide{margin-top:14px;padding:15px;border-radius:15px;background:#e9f1ff}.guide-card b,.ai-guide b{font-size:13px}.guide-card p,.ai-guide p{margin-top:4px;color:#62779e;font-size:10px;line-height:1.5}.field{display:block;margin-top:17px}.field>span,.section-title h2,.country-summary h2{font-size:13px;font-weight:800}.field input{width:100%;height:50px;margin-top:8px;padding:0 13px;border:1px solid #d7e1f0;border-radius:13px;background:#fff;font-size:14px}.field small,.error-text{display:block;margin-top:5px;color:#e5484d;font-size:9px}.country-section{margin-top:18px}.section-title{display:flex;justify-content:space-between}.section-title span{color:#94a3b8;font-size:9px}.country-search{display:flex;gap:8px;align-items:center;height:46px;margin-top:10px;padding:0 14px;border:1px solid #d7e1f0;border-radius:13px;background:#fff;color:#2469e8;font-size:21px}.country-search input{min-width:0;flex:1;border:0;outline:0;color:#334155;background:transparent;font-size:11px}.country-search input::placeholder{color:#8393ad}.mini-spinner{width:14px;height:14px;border:2px solid #dbe7fb;border-top-color:#2469e8;border-radius:50%;animation:spin .7s linear infinite}.country-dropdown{position:relative;margin-top:10px}.country-dropdown-trigger{display:flex;width:100%;align-items:center;gap:8px;height:46px;padding:0 14px;border:1px solid #d7e1f0;border-radius:13px;background:#fff;text-align:left}.country-dropdown-value{overflow:hidden;color:#173b86;font-size:11px;font-weight:800;text-overflow:ellipsis;white-space:nowrap}.country-dropdown-placeholder{color:#94a3b8;font-size:11px}.country-dropdown-chevron{flex:none;margin-left:auto;color:#8393ad;font-size:11px;font-style:normal;transition:transform .18s ease}.country-dropdown-chevron.open{transform:rotate(180deg)}.country-dropdown-backdrop{position:fixed;inset:0;z-index:20}.country-dropdown-panel{position:absolute;top:calc(100% + 8px);left:0;right:0;z-index:21;overflow:hidden;padding:10px;border:1px solid #e1e8f3;border-radius:16px;background:#fff;box-shadow:0 16px 34px rgba(20,35,70,.16)}.country-dropdown-panel .country-search{margin-top:0}.country-dropdown-list{display:grid;max-height:260px;overflow-y:auto;gap:4px;margin-top:8px}.country-dropdown-list button{display:flex;width:100%;align-items:center;gap:8px;padding:10px;border-radius:11px;color:#334155;background:none;font-size:11px;text-align:left}.country-dropdown-list button img{width:18px;height:18px;border-radius:50%;object-fit:cover}.country-dropdown-list button small{margin-left:auto;color:#a0aec0;font-size:8px}.country-dropdown-check{visibility:hidden;flex:none;width:14px;color:#173b86;font-size:11px;font-weight:900}.country-dropdown-list button.selected{background:#eef3ff}.country-dropdown-list button.selected .country-dropdown-check{visibility:visible}.country-dropdown-list button.selected small{color:#5c78ad}.country-dropdown-list button.disabled{opacity:.45;filter:grayscale(1);cursor:not-allowed}.country-dropdown-list button.disabled small{color:#d4232a}.country-dropdown-done{width:100%;margin-top:8px;padding:11px;border-radius:11px;color:#fff;background:#173b86;font-size:11px;font-weight:800}.empty-copy{padding:25px 0;color:#94a3b8;text-align:center;font-size:10px}.selection-summary{margin-top:16px;padding:14px;border:1px solid #bfd2f4;border-radius:14px;background:#fff}.selection-head{display:flex;align-items:center;justify-content:space-between}.selection-head strong{color:#173b86;font-size:12px}.selection-head span{color:#7284a2;font-size:8px}.selected-country-list{display:grid;gap:7px;margin-top:10px}.selected-country-list li{display:flex;align-items:center;gap:8px;padding:9px 10px;border:1px solid #e1e8f3;border-radius:10px;background:#f8fbff;transition:transform .2s,box-shadow .2s,opacity .2s}.selected-country-list li.dragging{opacity:.65;box-shadow:0 7px 16px rgba(36,105,232,.18)}.selected-country-list li>b{display:grid;place-items:center;width:20px;height:20px;border-radius:50%;color:#fff;background:#2469e8;font-size:9px}.selected-country-list li>span{font-size:16px}.selected-country-list li>strong{font-size:11px}.selected-country-list li>small{color:#94a3b8;font-size:8px}.selected-country-list li>button{margin-left:auto;border:0;color:#6b7f9e;background:none;font-size:21px;line-height:1;cursor:grab;touch-action:none}.country-order-move{transition:transform .22s}.plan-guide{display:flex;align-items:center;justify-content:space-between;margin-top:14px;padding:13px 14px;border-radius:14px;background:#fff;box-shadow:0 4px 12px rgba(20,35,70,.05)}.plan-guide div{display:flex;flex-direction:column;gap:3px}.plan-guide b{font-size:12px}.plan-guide span{color:#64748b;font-size:9px}.plan-guide button{border:0;color:#2469e8;background:none;font-size:9px;font-weight:700}.ai-guide{display:flex;gap:12px;align-items:center}.ai-guide>span{display:grid;place-items:center;width:42px;height:42px;border-radius:50%;background:#ffda7d;font-size:22px}.country-card{margin-top:12px;padding:14px;border:1px solid #e0e7f0;border-left:4px solid var(--accent);border-radius:16px;background:#fff;box-shadow:0 5px 14px rgba(20,35,70,.05)}.country-card header,.country-card header div{display:flex;align-items:center;gap:7px}.country-card header small{color:#94a3b8;font-size:9px}.country-card header button{margin-left:auto;border:0;color:#e5484d;background:none;font-size:9px}.order{display:grid;place-items:center;width:19px;height:19px;border-radius:50%;color:#fff;background:var(--accent);font-size:9px}.date-row{display:flex;width:100%;align-items:center;gap:8px;margin-top:13px;padding:12px;border:1px solid #e4eaf2;border-radius:11px;background:#fafcff}.date-row span{color:#64748b;font-size:9px}.date-row b{margin-left:auto;color:#173b86;font-size:10px}.card-error,.collision{margin-top:8px;padding:8px;border-radius:8px;color:#e5484d;background:#fff0f0;font-size:9px}.budget-hero{display:flex;align-items:center;gap:15px;margin-top:14px;padding:16px 15px;border-radius:18px;color:#fff;background:linear-gradient(135deg,#183b87,#2868dc);box-shadow:0 10px 22px rgba(35,83,170,.2);animation:rise-in .55s ease both}.budget-hero small{color:#bdd4ff;font-size:8px;font-weight:800}.budget-hero h2{margin-top:3px;font-size:16px;font-weight:900}.budget-hero p{margin-top:4px;color:#d9e6ff;font-size:9px}.ai-orbit{position:relative;display:grid;flex:0 0 52px;height:52px;place-items:center;border:1px solid rgba(255,255,255,.45);border-radius:50%}.ai-orbit::before{content:'';position:absolute;inset:-5px;border:2px dashed #82b2ff;border-radius:50%;animation:orbit 5s linear infinite}.ai-orbit i{display:grid;place-items:center;width:39px;height:39px;border-radius:50%;color:#173b86;background:#fff;font-size:15px;font-style:normal;font-weight:900}.budget-card{animation:rise-in .45s ease both}.budget-card:nth-of-type(2){animation-delay:.08s}.budget-card:nth-of-type(3){animation-delay:.16s}.budget-country>span{font-size:24px}.budget-country>strong{font-size:15px}.ai-recommendation{margin-top:12px;padding:12px;border:1px solid #b7d4ff;border-radius:13px;background:linear-gradient(135deg,#eef5ff,#f8fbff)}.ai-recommendation-head{display:flex;align-items:center;justify-content:space-between}.ai-recommendation-head span{color:#174fae;font-size:11px;font-weight:800}.ai-recommendation-head i{display:inline-grid;place-items:center;width:24px;height:24px;margin-right:5px;border-radius:7px;color:#fff;background:#2469e8;font-size:9px;font-style:normal}.ai-recommendation-head>b{color:#174fae;font-size:17px}.ai-values{display:grid;grid-template-columns:1fr 1fr;gap:7px;margin-top:9px}.ai-values span{padding:8px;border-radius:8px;color:#64748b;background:#fff;font-size:8px}.ai-values b{display:block;margin-top:3px;color:#1e3a6e;font-size:10px}.editable-budget{margin-top:12px;padding:12px;border:2px solid #5a82d0;border-radius:14px;background:#f8fbff}.manual-heading{display:flex;align-items:center;justify-content:space-between}.manual-heading div{display:flex;flex-direction:column;gap:2px}.manual-heading b{color:#173b86;font-size:13px}.manual-heading b i{display:inline-grid;place-items:center;width:21px;height:21px;border-radius:7px;color:#fff;background:#2469e8;font-style:normal}.manual-heading span{color:#64748b;font-size:8px}.manual-heading button{border:0;color:#2469e8;background:none;font-size:8px;font-weight:800}.budget-section-title{display:flex;justify-content:space-between;margin-top:13px}.budget-section-title b{color:#9b6117;font-size:10px}.budget-section-title span{color:#b8874d;font-size:8px}.budget-section-title.local b{color:#174fae}.budget-section-title.local span{color:#5b7fb9}.budget-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:7px}.budget-grid label{padding:9px;border:1px solid #aac6f1;border-radius:10px;background:#fff;box-shadow:0 2px 5px rgba(36,105,232,.06)}.budget-grid.prepaid-grid label{border-color:#e9c98f}.budget-grid label>span{color:#52637c;font-size:9px;font-weight:700}.budget-grid label div{display:flex;align-items:center;margin-top:6px;padding:6px 4px 2px;border-top:1px solid #dce6f5;background:#fff}.budget-grid input{min-width:0;width:100%;border:0;outline:0;background:transparent;text-align:right;font-size:13px;font-weight:900}.budget-grid input:focus{color:#174fae}.budget-grid label:focus-within{border-color:#2469e8;box-shadow:0 0 0 3px rgba(36,105,232,.12)}.budget-grid em{margin-left:3px;color:#64748b;font-size:8px;font-style:normal}.subtotal{display:flex;justify-content:space-between;margin-top:7px;padding:8px 10px;border-radius:9px;font-size:9px}.subtotal.prepaid{color:#9b6117;background:#fff4e7}.subtotal.local-total{color:#174fae;background:#eaf2ff}.total-preview{margin-top:15px;padding:14px;border-radius:15px;background:#fff;box-shadow:0 5px 14px rgba(20,35,70,.06)}.total-preview div{display:flex;justify-content:space-between;color:#64748b;font-size:10px}.total-preview .goal{margin-top:10px;padding-top:10px;border-top:1px dashed #dbe3ee;color:#173b86;font-size:13px}.complete-guide{text-align:center;margin:14px 0;padding:15px;border-radius:16px;background:#e7f9f1}.complete-guide span{display:grid;place-items:center;width:34px;height:34px;margin:0 auto;border-radius:50%;color:#fff;background:#10aa82;font-size:20px}.complete-guide p{margin-top:7px;color:#087e5b;font-size:15px;font-weight:800}.complete-guide small{color:#438e76;font-size:9px}.country-summary{margin:18px 0 14px}.country-summary h2{margin-bottom:10px}.country-summary article{display:flex;align-items:center;gap:9px;margin-top:8px;padding:13px;border-left:4px solid var(--accent);border-radius:12px;background:#fff;box-shadow:0 4px 13px rgba(20,35,70,.06)}.country-summary article div{display:flex;flex-direction:column;gap:4px}.country-summary article small{color:#94a3b8;font-size:8px}.country-summary article>b{margin-left:auto;font-size:12px}.grand-total{margin:14px 0 5px;text-align:center;font-size:28px;font-weight:800}.ticket-note{color:#cbd9ff;text-align:center;font-size:9px}.completion-stats{display:grid;grid-template-columns:1fr 1fr;gap:9px;margin-top:14px}.completion-stats div{display:flex;flex-direction:column;gap:5px;padding:13px;border:1px solid #dbe3ee;border-radius:13px;background:#fff}.completion-stats span{color:#64748b;font-size:9px}.completion-stats b{font-size:13px}.monthly-preview{display:flex;flex-direction:column;gap:5px;margin-top:10px;padding:16px;border:1px solid #a9d9cb;border-radius:15px;background:#effcf7}.monthly-preview span{color:#13856a;font-size:10px;font-weight:700}.monthly-preview b{color:#087e5b;font-size:20px}.monthly-preview small{color:#52947f;font-size:9px}.primary-cta{position:fixed;z-index:10;left:50%;bottom:18px;transform:translateX(-50%);width:min(354px,calc(100% - 36px));height:54px;border:0;border-radius:14px;color:#fff;background:#173b86;font-size:14px;font-weight:800}.primary-cta:disabled{background:#aeb9cc}.primary-cta:not(:disabled):active{transform:translateX(-50%) scale(.99)}@keyframes spin{to{transform:rotate(360deg)}}@keyframes rise-in{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}@keyframes orbit{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.budget-hero,.budget-card,.ai-orbit::before{animation:none}}
.primary-cta{display:grid;place-items:center;text-decoration:none}
.page-header:has(.onboarding-skip){grid-template-columns:78px 1fr 78px}.page-header .onboarding-skip{display:block;width:auto;height:32px;padding:0;background:transparent;color:#60718d;font-size:10px;font-weight:800;box-shadow:none;white-space:nowrap}
.register-content{display:contents}.onboarding-register{padding:0 0 104px;background:#f3f6fb}.onboarding-register .page-header{height:62px;padding:0 20px 12px;color:#fff;background:#0b2a6b}.onboarding-register .page-header>button:first-child{color:#fff;background:rgba(255,255,255,.12);box-shadow:none}.onboarding-register .page-header .onboarding-skip{color:rgba(255,255,255,.72)}.onboarding-register .steps{gap:6px;margin:0;padding:3px 22px 13px;background:#0b2a6b}.onboarding-register .steps i{background:rgba(255,255,255,.18)}.onboarding-register .steps i.active{background:#ffd45e}.registration-hero{position:relative;overflow:hidden;padding:15px 24px 73px;color:#fff;background:linear-gradient(155deg,#0b2a6b 0%,#123c94 62%,#17459f 100%)}.registration-hero::after{position:absolute;right:-48px;bottom:-72px;width:180px;height:180px;border-radius:50%;background:rgba(255,255,255,.06);content:''}.registration-hero>span{font-size:9px;font-weight:900;letter-spacing:.13em;color:#ffd45e}.registration-hero>small{float:right;color:rgba(255,255,255,.58);font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:9px;font-weight:800}.registration-hero h2{position:relative;z-index:1;margin-top:15px;font-size:20px;line-height:1.35;letter-spacing:-.04em}.registration-hero p{position:relative;z-index:1;margin-top:7px;color:rgba(255,255,255,.66);font-size:10px;font-weight:600;line-height:1.55}.onboarding-register .register-content{position:relative;z-index:2;display:block;margin:-48px 18px 0;padding:18px 16px 26px;border-radius:20px;background:#fff;box-shadow:0 14px 30px rgba(11,42,107,.16)}.onboarding-register .register-content::before,.onboarding-register .register-content::after{position:absolute;top:35px;width:18px;height:18px;border-radius:50%;background:#123c94;content:''}.onboarding-register .register-content::before{left:-9px}.onboarding-register .register-content::after{right:-9px}.onboarding-register .guide-card{margin-top:2px;background:#eef4ff}.onboarding-register .country-card,.onboarding-register .selection-summary,.onboarding-register .total-preview{box-shadow:none}.onboarding-register .complete-guide{margin-top:2px}.onboarding-register .primary-cta{box-shadow:0 10px 24px rgba(23,59,134,.24)}
.goal-review-note{margin-top:13px;padding:14px;border:1px solid #cbdaf2;border-radius:14px;background:#f4f8ff}.goal-review-note b{color:#173b86;font-size:12px}.goal-review-note p{margin-top:5px;color:#70809a;font-size:9px;line-height:1.55}.wallet-loading{margin-top:12px;padding:20px;border-radius:15px;background:#fff;color:#70809a;text-align:center;font-size:11px}.wallet-decision-card{position:relative;overflow:hidden;margin-top:13px;padding:20px 17px;border:1px solid #bcd2f5;border-radius:20px;background:linear-gradient(145deg,#eef5ff,#fff);text-align:center}.wallet-decision-card::after{position:absolute;right:-34px;top:-34px;width:112px;height:112px;border-radius:50%;background:#dceaff;content:''}.wallet-decision-icon{position:relative;z-index:1;display:grid;width:48px;height:48px;margin:0 auto 10px;place-items:center;border-radius:15px;color:#fff;background:#2469e8;font-size:20px;font-weight:900}.wallet-decision-card>small{position:relative;z-index:1;color:#2f70e9;font-size:8px;font-weight:900;letter-spacing:.15em}.wallet-decision-card h2{position:relative;z-index:1;margin-top:7px;font-size:16px;font-weight:900}.wallet-decision-card>p{position:relative;z-index:1;margin-top:6px;color:#70809a;font-size:10px}.wallet-decision-actions{position:relative;z-index:1;display:grid;grid-template-columns:1fr 1.15fr;gap:8px;margin-top:17px}.wallet-decision-actions button,.wallet-withdraw-actions button{height:47px;border-radius:13px;color:#fff;background:#173b86;font-size:11px;font-weight:900}.wallet-decision-actions button.secondary,.wallet-withdraw-actions button.secondary{color:#53627a;background:#e9eef6}.wallet-decision-actions button:disabled,.wallet-withdraw-actions button:disabled{opacity:.55}.account-connect-link{position:relative;z-index:1;margin-top:12px;color:#2469e8;background:none;font-size:9px;font-weight:800}.wallet-empty-card{display:flex;align-items:center;gap:12px;margin-top:13px;padding:16px;border:1px solid #dce5f2;border-radius:16px;background:#fff}.wallet-empty-card>span{display:grid;flex:0 0 42px;height:42px;place-items:center;border-radius:13px;color:#2469e8;background:#eaf2ff;font-size:17px;font-weight:900}.wallet-empty-card b{font-size:12px}.wallet-empty-card p{margin-top:4px;color:#8190a6;font-size:9px}.wallet-empty-card.resolved>span{color:#078568;background:#e2f7f0}.wallet-step-error{margin-top:10px;padding:10px 12px;border-radius:10px;color:#c5353b;background:#fff0f1;font-size:9px;line-height:1.5}.wallet-withdraw-backdrop{position:fixed;z-index:240;inset:0;display:flex;align-items:flex-end;justify-content:center;background:rgba(9,22,49,.52)}.wallet-withdraw-sheet{width:min(100%,390px);padding:12px 20px calc(22px + env(safe-area-inset-bottom));border-radius:26px 26px 0 0;background:#fff;box-shadow:0 -16px 40px rgba(12,35,78,.2)}.wallet-withdraw-sheet>i{display:block;width:40px;height:4px;margin:0 auto 18px;border-radius:99px;background:#d8e0ed}.wallet-withdraw-sheet>small{color:#2f70e9;font-size:9px;font-weight:900;letter-spacing:.14em}.wallet-withdraw-sheet h2{margin-top:7px;font-size:19px;font-weight:900}.wallet-withdraw-sheet>p{margin-top:7px;color:#75849a;font-size:10px}.wallet-account-list{display:grid;gap:8px;margin-top:16px}.wallet-account-list>button{display:grid;grid-template-columns:38px 1fr auto;align-items:center;gap:10px;width:100%;padding:12px;border:1px solid #e1e8f2;border-radius:14px;background:#f8faff;text-align:left}.wallet-account-list>button.selected{border:2px solid #2f70e9;background:#eef5ff}.wallet-account-list>button>span{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;color:#173b86;background:#fff;font-size:12px;font-weight:900}.wallet-account-list b{display:block;font-size:11px}.wallet-account-list small{display:block;margin-top:3px;color:#8a98ad;font-size:8px}.wallet-account-list em{color:#2f70e9;font-size:9px;font-style:normal;font-weight:900}.wallet-withdraw-actions{display:grid;grid-template-columns:1fr 1.5fr;gap:8px;margin-top:18px}
.onboarding-register .registration-hero{animation:hero-change .48s cubic-bezier(.22,1,.36,1) both}.onboarding-register .register-content{animation:content-change .5s cubic-bezier(.22,1,.36,1) both}.onboarding-register .register-content>*{animation:stagger-in .42s cubic-bezier(.22,1,.36,1) both}.onboarding-register .register-content>*:nth-child(2){animation-delay:.05s}.onboarding-register .register-content>*:nth-child(3){animation-delay:.1s}.onboarding-register .register-content>*:nth-child(4){animation-delay:.15s}.onboarding-register .register-content>*:nth-child(5){animation-delay:.2s}.onboarding-wallet-card{position:relative;overflow:hidden;margin-top:16px;padding:20px 18px 18px;border-radius:22px;color:#fff;background:linear-gradient(145deg,#0d2f76 0%,#174ca8 62%,#2d6fd9 100%);box-shadow:0 14px 30px rgba(18,58,133,.24);animation:wallet-arrive .62s cubic-bezier(.22,1,.36,1) .16s both}.onboarding-wallet-card::before{position:absolute;inset:10px;border:1px solid rgba(168,198,249,.34);border-radius:18px;content:'';pointer-events:none}.onboarding-wallet-card::after{position:absolute;right:-42px;top:-52px;width:150px;height:150px;border-radius:50%;background:rgba(255,255,255,.08);content:''}.onboarding-wallet-top,.onboarding-wallet-balance,.onboarding-wallet-card>p,.onboarding-wallet-card .wallet-decision-actions{position:relative;z-index:1}.onboarding-wallet-top{display:flex;align-items:flex-start;justify-content:space-between}.onboarding-wallet-top div{display:flex;flex-direction:column;gap:4px}.onboarding-wallet-top small{color:#ffd45e;font-size:8px;font-weight:900;letter-spacing:.14em}.onboarding-wallet-top b{font-size:14px}.onboarding-wallet-top>span{padding:6px 9px;border-radius:999px;color:#173f8d;background:#ffd45e;font-size:8px;font-weight:900}.onboarding-wallet-card.resolved .onboarding-wallet-top>span{color:#087e5b;background:#dff8ef}.onboarding-wallet-balance{display:block;margin-top:22px;font-family:'Space Mono',ui-monospace,monospace;font-size:32px;line-height:1;font-weight:800;letter-spacing:-.05em}.onboarding-wallet-card>p{margin-top:13px;padding-top:13px;border-top:1px dashed rgba(255,255,255,.35);color:rgba(255,255,255,.72);font-size:9px;line-height:1.55;text-align:left}.onboarding-wallet-card .wallet-decision-actions{margin-top:16px}.onboarding-wallet-card .wallet-decision-actions button{border:1px solid rgba(255,255,255,.3);background:#fff;color:#173f8d}.onboarding-wallet-card .wallet-decision-actions button.secondary{color:#fff;background:rgba(255,255,255,.14)}@keyframes hero-change{from{opacity:0;transform:translateY(-9px)}to{opacity:1;transform:translateY(0)}}@keyframes content-change{from{opacity:0;transform:translateY(22px) scale(.985)}to{opacity:1;transform:translateY(0) scale(1)}}@keyframes stagger-in{from{opacity:0;transform:translateY(13px)}to{opacity:1;transform:translateY(0)}}@keyframes wallet-arrive{from{opacity:0;transform:translateY(24px) scale(.96)}to{opacity:1;transform:translateY(0) scale(1)}}@media(prefers-reduced-motion:reduce){.onboarding-register .registration-hero,.onboarding-register .register-content,.onboarding-register .register-content>*,.onboarding-wallet-card{animation:none}}
.primary-cta{
  position:relative;
  left:auto;
  bottom:auto;
  width:100%;
  margin-top:18px;
  transform:none;
}
.primary-cta:not(:disabled):active{transform:scale(.99)}
.onboarding-register .register-content>.primary-cta{animation:none}
.selected-country-list li>.country-remove-button{
  display:grid;
  width:25px;
  height:25px;
  margin-left:auto;
  place-items:center;
  border-radius:50%;
  color:#d9484f;
  background:#fff0f1;
  font-size:17px;
  line-height:1;
  cursor:pointer;
  touch-action:manipulation;
}
.selected-country-list li>.country-drag-button{
  margin-left:0;
}
.country-dropdown-backdrop{display:none}
.country-dropdown-panel{
  position:relative;
  top:auto;
  left:auto;
  right:auto;
  z-index:auto;
  margin-top:8px;
  box-shadow:0 10px 24px rgba(20,35,70,.12);
}
.budget-country-tabs{display:flex;gap:5px;overflow-x:auto;margin:-4px -3px 13px;padding:4px 3px;border-radius:999px;background:#f1f4f9;scrollbar-width:none}.budget-country-tabs::-webkit-scrollbar{display:none}.budget-country-tabs button{display:flex;flex:1 0 auto;min-width:96px;height:40px;align-items:center;justify-content:center;gap:6px;padding:0 13px;border:0;border-radius:999px;color:#9aa8bd;background:transparent;font-size:10px;white-space:nowrap;transition:color .2s,background .2s,box-shadow .2s,transform .2s}.budget-country-tabs button .fi-inline{width:18px;height:13px;border-radius:2px}.budget-country-tabs button b{font-size:10px}.budget-country-tabs button.active{color:#fff;background:#173b86;box-shadow:0 6px 13px rgba(23,59,134,.2);transform:translateY(-1px)}.budget-card.budget-card-active{margin-top:0;padding:0;border:0;border-left:0;border-radius:0;background:transparent;box-shadow:none}.budget-country-header{display:flex!important;align-items:center!important;justify-content:space-between;padding:3px 0 12px}.budget-country-header .budget-country{display:flex;flex-direction:column;align-items:flex-start;gap:4px}.budget-country-header .budget-country strong{font-size:15px}.budget-country-header .budget-country small{font-size:9px}.budget-recommendation-status{display:flex;align-items:center;gap:7px;padding:6px 9px;border-radius:999px;color:#2469e8;background:#edf4ff;font-size:9px;font-weight:900;white-space:nowrap}.budget-ai-motion{position:relative;display:grid;width:21px;height:21px;place-items:center;border:2px dotted #4384f1;border-radius:50%;font-style:normal;animation:budget-ai-spin 2.4s linear infinite}.budget-ai-motion::after{position:absolute;top:-3px;right:-2px;width:5px;height:5px;border-radius:50%;background:#2469e8;box-shadow:0 0 0 3px rgba(36,105,232,.14);content:''}.budget-ai-motion b{font-size:6px;animation:budget-ai-counter-spin 2.4s linear infinite}.budget-recommendation-summary{display:grid;grid-template-columns:1fr 1fr;margin-bottom:11px;border-radius:14px;background:#f5f7fb}.budget-recommendation-summary div{padding:15px 9px;text-align:center}.budget-recommendation-summary div+div{border-left:1px dashed #d7dfeb}.budget-recommendation-summary span{display:block;color:#7d8ba1;font-size:8px;font-weight:800}.budget-recommendation-summary span small{font-size:7px}.budget-recommendation-summary b{display:block;margin-top:8px;color:#101c32;font-size:14px}.budget-recommendation-summary div:last-child b{color:#173b86}.budget-card-active .editable-budget{margin-top:0;padding:0;border:0;background:transparent}.budget-card-active .manual-heading{margin-bottom:13px;padding:10px 11px;border:1px solid #f2d18e;border-radius:11px;background:#fff9eb}.budget-card-active .manual-heading b{color:#b96d05;font-size:9px}.budget-card-active .manual-heading b i{width:auto;height:auto;color:inherit;background:none}.budget-card-active .manual-heading button{font-size:8px}.budget-card-active .budget-grid.prepaid-grid{grid-template-columns:1fr}.budget-card-active .budget-grid.prepaid-grid label{display:flex;align-items:center}.budget-card-active .budget-grid.prepaid-grid label>span{flex:1}.budget-card-active .budget-grid.prepaid-grid label div{width:54%;margin-top:0;border-top:0}.budget-card-active .subtotal{border-radius:0;background:transparent}.budget-card-active .subtotal.prepaid{color:#b96d05}.budget-card-active .subtotal.local-total{color:#173b86}.total-preview{border-top:1px solid #e4e9f2;border-radius:0;box-shadow:none}@keyframes budget-ai-spin{to{transform:rotate(360deg)}}@keyframes budget-ai-counter-spin{to{transform:rotate(-360deg)}}@media(prefers-reduced-motion:reduce){.budget-ai-motion,.budget-ai-motion b{animation:none}}
.register-page.budget-summary-step{padding-bottom:0}
.budget-fixed-summary{position:sticky;z-index:30;bottom:0;box-sizing:border-box;width:100%;margin-top:18px;padding:12px 20px calc(14px + env(safe-area-inset-bottom));border-top:1px solid #e4e9f2;background:#fff;box-shadow:0 -10px 28px rgba(16,25,43,.07)}
.onboarding-register .budget-fixed-summary{width:calc(100% + 32px);margin-right:-16px;margin-bottom:-26px;margin-left:-16px}
.budget-fixed-summary .total-preview{margin:0;padding:0;border:0;border-radius:0;background:transparent;box-shadow:none}
.budget-fixed-summary .total-preview div{display:flex;align-items:center;justify-content:space-between;color:#8891a0;font-size:11px;font-weight:700;line-height:1.35}
.budget-fixed-summary .total-preview div>b{color:#10192b;font-family:'Space Mono',ui-monospace,SFMono-Regular,Menlo,monospace;font-size:12.5px;font-weight:700;letter-spacing:-.025em}
.budget-fixed-summary .total-preview .goal{margin-top:8px;padding-top:0;border-top:0;color:#10192b;font-size:12.5px;font-weight:800}
.budget-fixed-summary .total-preview .goal>b{color:#0b2a6b;font-size:16px;font-weight:800}
.budget-fixed-summary .collision{margin:7px 0 0;padding:6px 9px}
.budget-fixed-summary .primary-cta{height:48px;margin-top:10px;border-radius:14px;font-size:14px;font-weight:800;box-shadow:none}
.ai-guide>div{flex:1}.date-row img{flex:0 0 18px;width:18px;height:18px}
.budget-recommendation-panel{margin-bottom:12px;padding:12px;border:1.5px solid #a9ccff;border-radius:16px;background:#eef5ff}
.budget-recommendation-title{display:flex!important;align-items:center!important;justify-content:flex-start!important;gap:8px!important;padding:0 0 10px!important}
.budget-recommendation-title i{display:grid;width:30px;height:30px;place-items:center;border-radius:9px;color:#fff;background:#2469e8;font-size:10px;font-style:normal;font-weight:900;box-shadow:0 5px 12px rgba(36,105,232,.2)}
.budget-recommendation-title b{color:#174fae;font-size:12px;font-weight:900}
.budget-recommendation-panel .budget-recommendation-summary{margin:0;background:transparent}
.budget-recommendation-panel .budget-recommendation-summary>div{border-radius:11px;background:#fff}
.budget-recommendation-panel .budget-recommendation-summary>div+div{margin-left:7px;border-left:0}
.budget-card-active .budget-section-title b{font-size:12px}
.budget-card-active .subtotal{align-items:center;font-size:12px}
.budget-card-active .subtotal b{font-size:12px}
</style>
