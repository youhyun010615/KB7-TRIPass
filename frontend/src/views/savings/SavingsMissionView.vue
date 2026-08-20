<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import NotificationBell from '@/components/common/NotificationBell.vue'
import { useSavingMissionsStore } from '@/stores/savingMissions'
import { useMonthlyFundStore } from '@/stores/monthlyFund'
import { useTravelStore } from '@/stores/travel'
import { getAccounts } from '@/api/asset'
import { getCards } from '@/api/card'
import foodIcon from '@/assets/icons/food.svg'
import cafeIcon from '@/assets/icons/cafe.svg'
import shoppingIcon from '@/assets/icons/shopping-cart.svg'
import taxiIcon from '@/assets/icons/taxi.svg'
import leisureIcon from '@/assets/icons/hobby_drink.svg'
import livingIcon from '@/assets/icons/home-dollar.svg'
import aiIcon from '@/assets/icons/ai.svg'
import aiReportIcon from '@/assets/icons/ai_report.svg'
import foodIconRaw from '@/assets/icons/food.svg?raw'
import cafeIconRaw from '@/assets/icons/cafe.svg?raw'
import shoppingIconRaw from '@/assets/icons/shopping-cart.svg?raw'
import taxiIconRaw from '@/assets/icons/taxi.svg?raw'
import leisureIconRaw from '@/assets/icons/hobby_drink.svg?raw'
import livingIconRaw from '@/assets/icons/home-dollar.svg?raw'

const route = useRoute()
const router = useRouter()
const missionStore = useSavingMissionsStore()
const monthlyFundStore = useMonthlyFundStore()
const travelStore = useTravelStore()
const isTraveling = computed(() => travelStore.lifecycle?.lifecycle === 'TRAVELING')
const linkedAccountCount = ref(0)
const linkedCardCount = ref(0)
const financialSourcesLoading = ref(true)
const financialSourcesError = ref('')
const hasLinkedFinancialSources = computed(
  () => linkedAccountCount.value + linkedCardCount.value > 0,
)

// 앱 프레임(App.vue)의 overflow:hidden 때문에 sticky 대신 fixed로 헤더를 고정한다.
const missionHeaderEl = ref(null)
const missionHeaderHeight = ref(0)
let missionHeaderResizeObserver = null

function syncMissionHeaderHeight() {
  if (missionHeaderEl.value) {
    missionHeaderHeight.value = missionHeaderEl.value.offsetHeight
  }
}

watch(missionHeaderEl, (el) => {
  missionHeaderResizeObserver?.disconnect()
  missionHeaderResizeObserver = null
  if (!el) return

  syncMissionHeaderHeight()
  if (window.ResizeObserver) {
    missionHeaderResizeObserver = new ResizeObserver(syncMissionHeaderHeight)
    missionHeaderResizeObserver.observe(el)
  }
})

onBeforeUnmount(() => {
  missionHeaderResizeObserver?.disconnect()
})

// 리포트의 "추천 미션 보러가기"에서 넘어온 경우, 미션 미등록 상태여도
// 대시보드가 아니라 바로 카테고리 선택 화면을 보여준다.
const showSelection = ref(
  route.query.from === 'analysis' || route.query.view === 'recommendations',
)

// 추천 미션을 선택/추가하는 화면인지 여부: 이 상태에서만 뒤로가기 헤더를 쓴다.
const showSelectionFlow = computed(
  () => showSelection.value || missionStore.addingMissions,
)

// 미션 시작 여부와 무관하게 보여줄 메인 탭 대시보드 여부
const showDashboard = computed(
  () =>
    hasLinkedFinancialSources.value &&
    !financialSourcesLoading.value &&
    !missionStore.loading &&
    !missionStore.errorMessage &&
    !showSelectionFlow.value,
)

const completedMissionCount = computed(
  () => (missionStore.missions?.missions || []).filter((mission) => mission.status === 'SUCCESS').length,
)

// monthlyFund 스토어의 카테고리 id를 저축 미션 카테고리 코드로 매핑한다.
const fundCategoryCodeMap = {
  food: 'FOOD',
  cafe: 'CAFE',
  living: 'LIVING',
  shopping: 'SHOPPING',
  hobby: 'LEISURE',
  etc: 'OTHER',
}
const dashboardTotalSpending = computed(() =>
  monthlyFundStore.categorySummaries.reduce((sum, item) => sum + Number(item.spent || 0), 0),
)
const dashboardCategories = computed(() => {
  const total = dashboardTotalSpending.value
  return monthlyFundStore.categorySummaries
    .map((item) => {
      const code = fundCategoryCodeMap[item.id] || 'OTHER'
      const meta = metaOf(code)
      const amount = Number(item.spent || 0)
      return {
        code: item.id,
        categoryCode: code,
        name: item.name,
        icon: meta.icon,
        iconSrc: meta.iconSrc,
        amount,
        ratio: total > 0 ? Math.round((amount / total) * 100) : 0,
      }
    })
    .sort((a, b) => b.amount - a.amount)
})

function openSelection() {
  showSelection.value = true
}

const FUND_HISTORY_MONTHS = 6
const showFundCheckModal = ref(false)
const selectedFundMonthIndex = ref(0)
const fundDropdownOpen = ref(false)

const currentFundMonthLabel = computed(() => `${new Date().getMonth() + 1}월`)

const fundHistoryMonths = computed(() => {
  const now = new Date()
  return Array.from({ length: FUND_HISTORY_MONTHS }, (_, monthsAgo) => {
    const date = new Date(now.getFullYear(), now.getMonth() - monthsAgo, 1)
    const summaries = monthlyFundStore.categorySummariesForMonth(monthsAgo)
    const totalSpending = summaries.reduce((sum, item) => sum + Number(item.spent || 0), 0)
    const categories = summaries
      .map((item) => {
        const code = fundCategoryCodeMap[item.id] || 'OTHER'
        const meta = metaOf(code)
        const amount = Number(item.spent || 0)
        return {
          code: item.id,
          categoryCode: code,
          name: item.name,
          icon: meta.icon,
          iconSrc: meta.iconSrc,
          amount,
          ratio: totalSpending > 0 ? Math.round((amount / totalSpending) * 100) : 0,
        }
      })
      .sort((a, b) => b.amount - a.amount)
    return {
      label: date.getFullYear() === now.getFullYear()
        ? `${date.getMonth() + 1}월`
        : `${date.getFullYear()}년 ${date.getMonth() + 1}월`,
      categories,
      totalSpending,
    }
  })
})

const selectedFundMonth = computed(() => fundHistoryMonths.value[selectedFundMonthIndex.value])

function openFundCheckModal() {
  showFundCheckModal.value = true
  selectedFundMonthIndex.value = 0
  fundDropdownOpen.value = false
}

function closeFundCheckModal() {
  showFundCheckModal.value = false
  fundDropdownOpen.value = false
}

watch(showFundCheckModal, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

onBeforeUnmount(() => {
  document.body.style.overflow = ''
})

function selectFundMonth(idx) {
  selectedFundMonthIndex.value = idx
  fundDropdownOpen.value = false
}

const categoryMeta = {
  FOOD: { icon: '🍴', iconSrc: foodIcon, iconRaw: foodIconRaw, color: '#ff7548', soft: '#fff0e9' },
  CAFE: { icon: '☕', iconSrc: cafeIcon, iconRaw: cafeIconRaw, color: '#d88b22', soft: '#fff5dc' },
  SHOPPING: { icon: '🛍️', iconSrc: shoppingIcon, iconRaw: shoppingIconRaw, color: '#8e63d4', soft: '#f4edff' },
  LIVING: { icon: '🏠', iconSrc: livingIcon, iconRaw: livingIconRaw, color: '#19a88b', soft: '#e5f8f2' },
  TRANSPORT: { icon: '🚌', iconSrc: taxiIcon, iconRaw: taxiIconRaw, color: '#3478e5', soft: '#eaf2ff' },
  LEISURE: { icon: '🎮', iconSrc: leisureIcon, iconRaw: leisureIconRaw, color: '#e25283', soft: '#ffedf3' },
  OTHER: { icon: '•••', color: '#718096', soft: '#edf2f7' },
}

// 카테고리 SVG 아이콘의 black 채우기/선 색을 카테고리 색보다 진하게 바꿔 넣는다.
function darken(hex, amount = 0.3) {
  const clean = (hex || '').replace('#', '')
  const num = parseInt(clean, 16)
  if (Number.isNaN(num)) return hex

  const r = Math.max(0, Math.round(((num >> 16) & 255) * (1 - amount)))
  const g = Math.max(0, Math.round(((num >> 8) & 255) * (1 - amount)))
  const b = Math.max(0, Math.round((num & 255) * (1 - amount)))

  return `rgb(${r}, ${g}, ${b})`
}

function coloredCategoryIcon(categoryCode) {
  const meta = metaOf(categoryCode)
  if (!meta.iconRaw) return ''
  return meta.iconRaw.replaceAll('black', darken(meta.color))
}

const analysisMonthLabel = computed(() => monthLabel(missionStore.analysisYearMonth))
const targetMonthLabel = computed(() => monthLabel(missionStore.targetYearMonth))

onMounted(async () => {
  await travelStore.loadLifecycle()
  await loadFinancialSourcesAndMissions()
  // 리포트/홈의 '추천 미션 보러가기'는 쿼리로 바로 선택 화면(showSelection)을 띄우는데,
  // 이미 진행 중인 미션이 있는 상태에서는 store의 addingMissions 플래그가 없으면
  // toggleCategory/selectRate가 조용히 무시돼 카테고리를 선택할 수 없었다.
  // 대시보드의 '추천 미션 추가하기' 버튼과 동일하게 addingMissions를 켜서 맞춰준다.
  if (showSelection.value && missionStore.hasStartedMissions && !missionStore.addingMissions) {
    missionStore.beginAddingMissions()
  }
})

async function loadFinancialSourcesAndMissions() {
  const yearMonth = String(route.query.yearMonth || '')
  financialSourcesLoading.value = true
  financialSourcesError.value = ''

  const [accountResult, cardResult] = await Promise.allSettled([
    getAccounts(),
    getCards(),
  ])

  linkedAccountCount.value = accountResult.status === 'fulfilled'
    ? accountResult.value.data?.data?.length ?? 0
    : 0
  linkedCardCount.value = cardResult.status === 'fulfilled'
    ? cardResult.value.data?.data?.length ?? 0
    : 0

  if (accountResult.status === 'rejected' && cardResult.status === 'rejected') {
    financialSourcesError.value = '금융 데이터 연결 상태를 확인하지 못했어요.'
  }
  financialSourcesLoading.value = false

  if (hasLinkedFinancialSources.value) {
    await missionStore.load(yearMonth || undefined)
  }
}

function retryReadiness() {
  loadFinancialSourcesAndMissions()
}

function goFinancialSources() {
  router.push('/profile/financial?step=1')
}

function monthLabel(yearMonth) {
  if (!yearMonth) return ''
  const [, month] = yearMonth.split('-')
  return `${Number(month)}월`
}

function metaOf(code) {
  return categoryMeta[code] || categoryMeta.OTHER
}

function formatCurrency(value) {
  return `${Number(value || 0).toLocaleString('ko-KR')}원`
}

function formatDate(value) {
  if (!value) return '-'
  const parts = Array.isArray(value) ? value : String(value).split('-')
  return `${Number(parts[1])}.${String(parts[2]).padStart(2, '0')}`
}

function isSelected(categoryId) {
  return Boolean(missionStore.selectedRates[String(categoryId)])
}

function isStarted(categoryId) {
  return missionStore.startedCategoryIds.has(String(categoryId))
}

function selectedRate(categoryId) {
  return Number(missionStore.selectedRates[String(categoryId)] || 0)
}

function displayedWeek(mission) {
  const weeks = mission.weeklyMissions || []
  return (
    weeks.find((week) => week.status === 'IN_PROGRESS') ||
    weeks.find((week) => week.status === 'PENDING') ||
    weeks.at(-1) ||
    null
  )
}

const currentDisplayedWeek = computed(() => {
  const missions = missionStore.missions?.missions || []
  for (const mission of missions) {
    const week = displayedWeek(mission)
    if (week) return week
  }
  return null
})

function remainingLimit(week) {
  return Math.max(
    0,
    Number(week?.weeklyUsageLimit || 0) - Number(week?.actualSpending || 0),
  )
}

function weekProgress(week) {
  const limit = Number(week?.weeklyUsageLimit || 0)
  if (!limit) return 0
  return Math.min(100, Math.round((Number(week.actualSpending || 0) / limit) * 100))
}

function statusLabel(status) {
  return {
    PENDING: '시작 전',
    IN_PROGRESS: '진행 중',
    SUCCESS: '성공',
    FAILED: '실패',
    CANCELED: '중단',
  }[status] || status
}

function statusClass(status) {
  return String(status || '').toLowerCase().replace('_', '-')
}

function openAnalysis() {
  router.push({
    name: 'MonthlyAnalysisReport',
    params: { yearMonth: missionStore.analysisYearMonth },
  })
}

function goBack() {
  if (missionStore.addingMissions) {
    missionStore.cancelAddingMissions()
    return
  }
  if (route.query.from === 'analysis' && route.query.yearMonth) {
    router.push({
      name: 'MonthlyAnalysisReport',
      params: { yearMonth: route.query.yearMonth },
    })
    return
  }
  if (showSelection.value && !missionStore.hasStartedMissions) {
    showSelection.value = false
    return
  }
  if (window.history.length > 1) router.back()
  else router.push({ name: 'Home' })
}

function closeSelectionFlow() {
  if (missionStore.addingMissions) {
    missionStore.cancelAddingMissions()
  }
  showSelection.value = false
}
</script>

<template>
  <main class="mission-page">
    <div v-if="showSelectionFlow" class="report-backdrop" @click="closeSelectionFlow"></div>

    <div :class="showSelectionFlow ? 'report-modal' : 'page-shell'">
      <div v-if="!showSelectionFlow" ref="missionHeaderEl" class="mission-header-fixed">
        <header class="dashboard-header">
          <div>
            <p>
              <img src="@/assets/brand/tripass-text.png" class="header-wordmark" alt="TRIPASS" />
            </p>
            <h1>MISSION</h1>
          </div>
          <NotificationBell />
        </header>
      </div>
      <header v-else class="report-header">
        <button type="button" class="back-button" aria-label="뒤로 가기" @click="goBack">
          <b v-if="route.query.from === 'analysis'">리포트 보기</b>
          <span v-else aria-hidden="true">‹</span>
        </button>
        <div>
          <small>AI SAVING COACH</small>
          <h1>AI 추천 미션</h1>
        </div>
        <button
          type="button"
          class="close-icon"
          aria-label="닫기"
          @click="closeSelectionFlow"
        >
          ×
        </button>
      </header>

      <div v-if="!showSelectionFlow" :style="{ height: missionHeaderHeight + 'px' }" aria-hidden="true" />

      <div :class="{ 'report-modal-body': showSelectionFlow }">

    <section v-if="isTraveling" class="state-card travel-mission-lock">
      <span>✈</span>
      <h2>현재 여행 중이어서<br>진행할 수 있는 미션이 없어요</h2>
      <p>완료한 미션은 마이페이지의 여행 관리에서 확인할 수 있어요.</p>
      <button type="button" @click="router.push(`/mypage/travel/${travelStore.lifecycle?.tripId}`)">완료 미션 보기</button>
    </section>

    <section v-else-if="financialSourcesLoading" class="state-card loading-card">
      <span class="loading-plane">✈</span>
      <h2>AI 미션을 준비하고 있어요</h2>
      <p>지난달 소비 분석과 저장된 미션을 확인하고 있어요.</p>
    </section>

    <section v-else-if="financialSourcesError" class="state-card error-card">
      <span>!</span>
      <h2>준비 상태를 확인하지 못했어요</h2>
      <p>{{ financialSourcesError }}</p>
      <button type="button" @click="retryReadiness">다시 시도</button>
    </section>

    <section v-else-if="!hasLinkedFinancialSources" class="mission-home-setup">
      <span class="mission-home-label">AI SAVING MISSION</span>
      <div class="mission-home-setup-body">
        <div class="mission-ai-stage" aria-hidden="true">
          <span class="mission-ai-orbit"></span>
          <span class="mission-ai-spark one">✦</span>
          <span class="mission-ai-spark two">✦</span>
          <span class="mission-ai-core"><img :src="aiIcon" alt="" /></span>
        </div>
        <h2>AI 추천 미션을 받아보세요!</h2>
        <p>계좌나 카드를 연결하면 거래내역을 분석해 맞춤 저축 미션을 추천해 드려요.</p>
        <button type="button" @click="goFinancialSources">금융 데이터 연결하기</button>
      </div>
    </section>

    <section v-else-if="missionStore.loading" class="state-card loading-card">
      <span class="loading-plane">✈</span>
      <h2>AI 미션을 준비하고 있어요</h2>
      <p>지난달 소비 분석과 저장된 미션을 확인하고 있어요.</p>
    </section>

    <section v-else-if="missionStore.errorMessage" class="state-card error-card">
      <span>!</span>
      <h2>미션 정보를 불러오지 못했어요</h2>
      <p>{{ missionStore.errorMessage }}</p>
      <button type="button" @click="missionStore.load(missionStore.analysisYearMonth)">
        다시 시도
      </button>
    </section>

    <template v-else-if="showDashboard">
      <section class="mission-savings-card">
        <p>MISSION SAVINGS</p>
        <div class="mission-savings-amount">
          <strong>{{ formatCurrency(missionStore.missions?.totalRewardAmount) }}</strong>
          <span>미션으로 모은 저축액</span>
        </div>
        <div class="mission-savings-divider"></div>
        <div class="mission-savings-foot">
          <span>수행 완료 미션 {{ completedMissionCount }}개</span>
          <button
            type="button"
            class="mission-report-link"
            :disabled="!missionStore.analysisYearMonth"
            @click="openAnalysis"
          >
            <img :src="aiReportIcon" alt="" aria-hidden="true" />
            {{ analysisMonthLabel }} 리포트
          </button>
        </div>
      </section>

      <section class="fund-check-card">
        <div class="fund-check-head-row fund-check-head-row--static">
          <span class="fund-check-title">{{ currentFundMonthLabel }} 자금체크</span>
          <button type="button" class="fund-check-history-button" @click="openFundCheckModal">
            지난 자금체크 보기
          </button>
        </div>
        <div class="fund-check-total-block fund-check-total-block--stacked">
          <div class="fund-check-total-label">총 지출</div>
          <div class="fund-check-total">{{ formatCurrency(dashboardTotalSpending) }}</div>
        </div>
        <div class="fund-check-divider"></div>
        <div class="fund-check-row-heading">
          <span>카테고리별 지출</span>
          <span>이번 달</span>
        </div>
        <div class="fund-check-list">
          <div v-for="category in dashboardCategories" :key="category.code" class="fund-check-row">
            <span
              class="fund-check-icon"
              :style="{ background: `${metaOf(category.categoryCode).color}18` }"
            >
              <span
                v-if="metaOf(category.categoryCode).iconRaw"
                class="category-icon-glyph"
                v-html="coloredCategoryIcon(category.categoryCode)"
              ></span>
              <template v-else>{{ category.icon }}</template>
            </span>
            <span class="fund-check-name">{{ category.name }}</span>
            <div class="fund-check-bar">
              <div :style="{ width: `${category.ratio}%` }"></div>
            </div>
            <span class="fund-check-amount">{{ formatCurrency(category.amount) }}</span>
            <span class="fund-check-ratio">{{ category.ratio }}%</span>
          </div>
        </div>
      </section>

      <section class="active-missions-card">
        <div class="active-missions-head">
          <span class="active-missions-title">
            {{
              missionStore.hasStartedMissions && currentDisplayedWeek
                ? `${targetMonthLabel} ${currentDisplayedWeek.weekNumber}주차 저축 미션`
                : '수행 중 미션'
            }}
            <span class="mission-title-ai-badge" aria-hidden="true"><img :src="aiIcon" alt="" /></span>
          </span>
        </div>

        <template v-if="missionStore.hasStartedMissions">
          <article
            v-for="mission in missionStore.missions.missions"
            :key="mission.id"
            class="progress-card"
            :style="{ '--accent': metaOf(mission.categoryCode).color, '--soft': metaOf(mission.categoryCode).soft }"
          >
            <div class="mission-card-head">
              <span class="category-icon">
                <span
                  v-if="metaOf(mission.categoryCode).iconRaw"
                  class="category-icon-glyph"
                  v-html="coloredCategoryIcon(mission.categoryCode)"
                ></span>
                <template v-else>{{ metaOf(mission.categoryCode).icon }}</template>
              </span>
              <div>
                <h3>{{ mission.categoryName }} {{ mission.reductionRate }}% 줄이기</h3>
                <small v-if="displayedWeek(mission)">{{ displayedWeek(mission).missionMessage }}</small>
              </div>
              <span :class="['status-chip', statusClass(mission.status)]">
                {{ statusLabel(mission.status) }}
              </span>
            </div>

            <template v-if="displayedWeek(mission)">
              <div class="spending-caption">
                <span>사용 금액</span>
                <strong>{{ formatCurrency(displayedWeek(mission).actualSpending) }}</strong>
              </div>
              <div class="spending-progress">
                <i :style="{ width: `${weekProgress(displayedWeek(mission))}%` }"></i>
              </div>
              <div class="limit-caption">
                <span>남은 한도 {{ formatCurrency(remainingLimit(displayedWeek(mission))) }}</span>
                <span>주간 사용 한도 {{ formatCurrency(displayedWeek(mission).weeklyUsageLimit) }}</span>
              </div>
            </template>
          </article>

          <button
            v-if="missionStore.canAddMissions"
            type="button"
            class="add-mission-button"
            @click="missionStore.beginAddingMissions"
          >
            <span>＋</span>
            <div>
              <small>아직 남은 추천 항목이 있어요</small>
              <strong>추천 미션 추가하기</strong>
            </div>
            <b>›</b>
          </button>
        </template>

        <template v-else>
          <p class="active-missions-empty">수행중인 미션이 없어요.<br />수행할 미션을 선택하세요.</p>
          <button type="button" class="dashboard-cta" @click="openSelection">추천 미션 보러가기 ›</button>
        </template>
      </section>
    </template>

    <template v-else>
      <section class="recommendation-hero">
        <div>
          <p>{{ analysisMonthLabel }} 소비 분석 완료</p>
          <h2>
            AI 추천 미션을 선택해보세요
            <span class="mission-title-ai-badge" aria-hidden="true"><img :src="aiIcon" alt="" /></span>
          </h2>
          <span v-if="missionStore.hasStartedMissions">진행 중인 미션은 유지하고, 선택한 목표만 새로 추가돼요.</span>
          <span v-else>선택한 목표는 {{ targetMonthLabel }} 주간 미션으로 만들어져요.</span>
        </div>
      </section>

      <section v-if="missionStore.options.length" class="option-section">
        <div class="section-title">
          <div>
            <small>AI RECOMMENDATION</small>
            <h2>추천 절약 카테고리</h2>
            <span v-if="missionStore.hasStartedMissions" class="started-count"
              >미션 {{ missionStore.missions?.missionCount || 0 }}개 진행중</span
            >
          </div>
          <span v-if="missionStore.hasStartedMissions">{{ missionStore.newSelectedCount }}개 추가</span>
          <span v-else>{{ missionStore.selectedCount }}/3 선택</span>
        </div>

        <article
          v-for="category in missionStore.options"
          :key="category.categoryId"
          :class="['option-card', { selected: isSelected(category.categoryId), locked: isStarted(category.categoryId) }]"
          :style="{ '--accent': metaOf(category.categoryCode).color, '--soft': metaOf(category.categoryCode).soft }"
        >
          <button
            type="button"
            class="option-toggle"
            :aria-pressed="isSelected(category.categoryId)"
            :disabled="isStarted(category.categoryId)"
            @click="missionStore.toggleCategory(category)"
          >
            <span class="category-icon">
              <span
                v-if="metaOf(category.categoryCode).iconRaw"
                class="category-icon-glyph"
                v-html="coloredCategoryIcon(category.categoryCode)"
              ></span>
              <template v-else>{{ metaOf(category.categoryCode).icon }}</template>
            </span>
            <div>
              <small>추천 {{ category.recommendationRank }}순위</small>
              <h3>{{ category.categoryName }}</h3>
              <p>{{ category.recommendationReason }}</p>
            </div>
            <i>{{ isStarted(category.categoryId) ? '진행중' : (isSelected(category.categoryId) ? '✓' : '+') }}</i>
          </button>

          <div v-if="isSelected(category.categoryId) && !isStarted(category.categoryId)" class="rate-panel">
            <p>얼마나 줄여볼까요?</p>
            <div class="rate-buttons">
              <button
                v-for="option in category.options"
                :key="option.reductionRate"
                type="button"
                :class="{ active: selectedRate(category.categoryId) === Number(option.reductionRate) }"
                @click="missionStore.selectRate(category.categoryId, option.reductionRate)"
              >
                {{ option.reductionRate }}%
              </button>
            </div>
            <div
              v-for="option in category.options.filter(item => Number(item.reductionRate) === selectedRate(category.categoryId))"
              :key="option.reductionRate"
              class="rate-result"
            >
              <div><small>월 절약 목표</small><strong>{{ formatCurrency(option.monthlyReductionTarget) }}</strong></div>
              <div><small>주간 사용 한도</small><strong>{{ formatCurrency(option.weeklyUsageLimit) }}</strong></div>
            </div>
          </div>
        </article>
      </section>

      <section v-else class="state-card empty-card">
        <span>📊</span>
        <h2>추천할 소비 데이터가 부족해요</h2>
        <p>지난달 카드 거래가 쌓이면 AI가 줄이기 좋은 카테고리를 추천해 드려요.</p>
        <button type="button" @click="openAnalysis">분석 리포트 확인</button>
      </section>

      <section v-if="missionStore.options.length" class="start-summary">
        <div class="start-summary-row">
          <small>선택한 미션 {{ missionStore.hasStartedMissions ? missionStore.newSelectedCount : missionStore.selectedCount }}개 · 예상 절감액</small>
          <strong>{{ formatCurrency(missionStore.hasStartedMissions ? missionStore.newExpectedSavingAmount : missionStore.expectedSavingAmount) }}</strong>
        </div>
        <p v-if="missionStore.errorMessage" class="inline-error">{{ missionStore.errorMessage }}</p>
        <button
          type="button"
          :disabled="(missionStore.hasStartedMissions ? missionStore.newSelectedCount : missionStore.selectedCount) === 0 || missionStore.submitting"
          @click="missionStore.startMissions"
        >
          {{ missionStore.submitting ? '미션을 만들고 있어요...' : `${missionStore.hasStartedMissions ? missionStore.newSelectedCount : missionStore.selectedCount}개 미션 ${missionStore.hasStartedMissions ? '추가하기' : '시작하기'}` }}
        </button>
      </section>
    </template>
      </div>
    </div>

    <BottomNav v-if="!showSelectionFlow" />

    <div v-if="showFundCheckModal" class="fund-modal-overlay" @click.self="closeFundCheckModal">
      <div class="fund-modal">
        <div class="fund-modal-body">
          <div class="fund-check-head-row fund-check-head-row--static">
            <div class="fund-modal-title-group">
              <div class="fund-modal-select">
                <button
                  type="button"
                  class="fund-modal-select-trigger"
                  @click="fundDropdownOpen = !fundDropdownOpen"
                >
                  <span>{{ selectedFundMonth.label }}</span>
                  <span class="fund-modal-select-chevron" :class="{ open: fundDropdownOpen }">▾</span>
                </button>
                <template v-if="fundDropdownOpen">
                  <div class="fund-modal-select-backdrop" @click="fundDropdownOpen = false"></div>
                  <ul class="fund-modal-select-list">
                    <li v-for="(month, idx) in fundHistoryMonths" :key="month.label">
                      <button
                        type="button"
                        :class="{ active: idx === selectedFundMonthIndex }"
                        @click="selectFundMonth(idx)"
                      >
                        {{ month.label }}
                      </button>
                    </li>
                  </ul>
                </template>
              </div>
              <span class="fund-check-title">{{ selectedFundMonth.label }} 자금체크</span>
            </div>
            <button type="button" class="fund-modal-close close-icon" aria-label="닫기" @click="closeFundCheckModal">×</button>
          </div>
          <div class="fund-check-total-block fund-check-total-block--stacked">
            <div class="fund-check-total-label">총 지출</div>
            <div class="fund-check-total">{{ formatCurrency(selectedFundMonth.totalSpending) }}</div>
          </div>
          <div class="fund-check-divider"></div>
          <div class="fund-check-row-heading">
            <span>카테고리별 지출</span>
            <span>{{ selectedFundMonth.label }}</span>
          </div>
          <div class="fund-check-list">
            <div v-for="category in selectedFundMonth.categories" :key="category.code" class="fund-check-row">
              <span
                class="fund-check-icon"
                :style="{ background: `${metaOf(category.categoryCode).color}18` }"
              >
                <span
                  v-if="metaOf(category.categoryCode).iconRaw"
                  class="category-icon-glyph"
                  v-html="coloredCategoryIcon(category.categoryCode)"
                ></span>
                <template v-else>{{ category.icon }}</template>
              </span>
              <span class="fund-check-name">{{ category.name }}</span>
              <div class="fund-check-bar">
                <div :style="{ width: `${category.ratio}%` }"></div>
              </div>
              <span class="fund-check-amount">{{ formatCurrency(category.amount) }}</span>
              <span class="fund-check-ratio">{{ category.ratio }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.mission-page{--navy:#153b86;min-height:100vh;padding:0 18px 104px;background:#eef2f8;color:#132348}.mission-header-fixed{position:fixed;top:0;left:50%;z-index:60;width:100%;max-width:390px;padding:14px 18px;background:#eef2f8;transform:translateX(-50%)}
.report-backdrop{position:fixed;inset:0;z-index:50;background:rgba(9,18,38,.55);animation:report-backdrop-enter .3s ease both}
.report-modal{position:fixed;z-index:51;top:50%;left:50%;transform:translate(-50%,-50%);width:calc(100% - 32px);max-width:358px;max-height:90vh;overflow:hidden;display:flex;flex-direction:column;border-radius:24px;background:#f3f6ff;box-shadow:0 24px 60px rgba(9,18,38,.35);animation:report-modal-enter .28s cubic-bezier(.22,1,.36,1) both}
.report-modal-body{overflow-y:auto;flex:1;padding:0 18px 30px}
@keyframes report-backdrop-enter{from{opacity:0}to{opacity:1}}
@keyframes report-modal-enter{from{transform:translate(-50%,-50%) scale(.94);opacity:0}to{transform:translate(-50%,-50%) scale(1);opacity:1}}
.report-header{position:relative;flex:none;display:flex;align-items:center;justify-content:space-between;padding:18px 18px 13px;border-bottom:1px solid #e0e8f5;border-radius:24px 24px 0 0;background:#f3f6ff}
.report-header .back-button{display:inline-flex;align-items:center;gap:1px;width:auto;height:34px;padding:0 10px 0 5px;border-radius:12px;background:#fff;color:#193d82;font-weight:700;box-shadow:0 5px 16px #24487512}
.report-header .back-button>span{font-size:22px;line-height:1}
.report-header .back-button>b{font-size:11.5px;font-weight:800}
.report-header .close-icon{width:34px;height:34px;background:transparent;color:#173f8d;font-size:21px;font-weight:400;box-shadow:none}
.report-header>div{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);text-align:center}
.report-header small,.section-title small{color:#f07638;font-size:9px;font-weight:900;letter-spacing:.16em}
.report-header h1{margin-top:3px;font-size:17px;font-weight:900;letter-spacing:-.03em}.state-card{margin-top:28px;padding:34px 24px;border:1px solid #d5e2f8;border-radius:24px;background:#fff;text-align:center;box-shadow:0 10px 30px #1e40700b}.state-card>span{display:grid;width:54px;height:54px;margin:0 auto;place-items:center;border-radius:18px;background:#edf3ff;color:#316fe0;font-size:25px;font-weight:900}.state-card h2{margin-top:15px;font-size:17px;font-weight:900}.state-card p{margin-top:7px;color:#73829d;font-size:12px;line-height:1.6}.state-card button{margin-top:19px;padding:12px 24px;border-radius:12px;background:#245ec4;color:#fff;font-size:13px;font-weight:900}.loading-plane{animation:fly 1.4s ease-in-out infinite}@keyframes fly{50%{transform:translate(8px,-5px)}}.recommendation-hero{position:relative;display:flex;overflow:hidden;gap:10px;margin-top:18px;padding:14px;border-radius:18px;background:linear-gradient(145deg,#0c2d72 0%,#174ca7 62%,#2f70d9 100%);color:#fff;box-shadow:0 13px 30px #163f8d2b}.recommendation-hero::after{content:'';position:absolute;top:-58px;right:-46px;width:160px;height:160px;border-radius:50%;background:rgba(255,255,255,.08);pointer-events:none}.recommendation-hero p{color:#ffc36b;font-size:9.5px;font-weight:900}.recommendation-hero h2{display:flex;flex-wrap:wrap;align-items:center;gap:6px;margin-top:5px;font-size:14px;font-weight:900;line-height:1.35;letter-spacing:-.03em}.recommendation-hero span{display:block;margin-top:6px;color:#cbdcff;font-size:9.5px;line-height:1.5}.option-section{margin-top:20px}.section-title{display:flex;align-items:end;justify-content:space-between}.section-title h2{margin-top:3px;font-size:15px;font-weight:900}.started-count{display:inline-block;margin-top:6px;padding:4px 9px;border-radius:20px;background:#e4f7f1;color:#149477;font-size:10px;font-weight:800}.section-title>span{padding:6px 10px;border-radius:20px;background:#e6f0ff;color:#1768f2;font-size:10.5px;font-weight:800}.option-card,.progress-card{--accent:#3478e5;--soft:#eaf2ff;margin-top:10px;overflow:hidden;border:1px solid #dce5f3;border-radius:16px;background:#fff;box-shadow:0 8px 22px #263e650b}.option-card.selected{border-color:var(--accent);box-shadow:0 10px 25px color-mix(in srgb,var(--accent) 16%,transparent)}.option-toggle{display:flex;width:100%;align-items:center;gap:10px;padding:12px;text-align:left}.category-icon{position:relative;display:grid;flex:0 0 34px;height:34px;place-items:center;border-radius:11px;background:var(--soft);font-size:15px}.mission-title-ai-badge{display:inline-flex;flex:none;align-items:center;animation:mission-title-ai-pulse 1.6s ease-in-out infinite}.mission-title-ai-badge img{width:18px;height:18px;object-fit:contain}@keyframes mission-title-ai-pulse{0%,100%{opacity:.6;transform:scale(.85) rotate(-3deg);filter:invert(34%) sepia(94%) saturate(1272%) hue-rotate(199deg) brightness(91%) drop-shadow(0 0 0 rgba(23,104,242,0))}50%{opacity:1;transform:scale(1.25) rotate(3deg);filter:invert(34%) sepia(94%) saturate(1272%) hue-rotate(199deg) brightness(91%) drop-shadow(0 0 7px rgba(23,104,242,.6))}}.recommendation-hero .mission-title-ai-badge img{animation:recommendation-ai-pulse 1.6s ease-in-out infinite}@keyframes recommendation-ai-pulse{0%,100%{opacity:.7;transform:scale(.85) rotate(-3deg);filter:brightness(0) invert(1) drop-shadow(0 0 0 rgba(255,255,255,0))}50%{opacity:1;transform:scale(1.2) rotate(3deg);filter:brightness(0) invert(1) drop-shadow(0 0 6px rgba(255,255,255,.7))}}.option-toggle>div,.mission-card-head>div{min-width:0;flex:1}.option-toggle small,.mission-card-head small{color:var(--accent);font-size:9.5px;font-weight:900}.option-toggle h3,.mission-card-head h3{margin-top:2px;font-size:13px;font-weight:900}.option-toggle p{margin-top:4px;color:#78869f;font-size:10px;line-height:1.4}.option-toggle>i{display:grid;width:24px;height:24px;place-items:center;border-radius:50%;background:#edf2fa;color:#557092;font-style:normal;font-weight:900}.selected .option-toggle>i{background:var(--accent);color:#fff}.rate-panel{padding:12px 14px 14px;border-top:1px dashed #dbe3ef;background:color-mix(in srgb,var(--soft) 46%,white)}.rate-panel>p{font-size:11px;font-weight:900}.rate-buttons{display:flex;gap:7px;margin-top:9px}.rate-buttons button{display:flex;flex:1;height:36px;align-items:center;justify-content:center;padding:0 9px;border:1px solid #cfdaea;border-radius:9px;background:#fff;color:#6a7890;font-size:11.5px;font-weight:900}.rate-buttons button.active{border-color:var(--accent);background:var(--accent);color:#fff}.rate-result{display:grid;grid-template-columns:1fr 1fr;gap:9px;margin-top:10px}.rate-result>div{padding:9px;border-radius:11px;background:#fff}.rate-result small{display:block;color:#8491a6;font-size:9.5px}.rate-result strong{display:block;margin-top:3px;color:#173d82;font-size:13px;font-weight:900}.spending-caption{display:flex;align-items:baseline;justify-content:space-between;margin-top:17px;padding-top:14px;border-top:1px dashed #dbe3ef}.spending-caption span{color:#8491a6;font-size:9px;font-weight:700}.spending-caption strong{color:#173d82;font-size:13px;font-weight:900}.start-summary{position:sticky;bottom:0;z-index:5;margin-top:18px;padding:14px 15px;border-radius:16px;background:#fff;border:1px solid #e3eaf5;box-shadow:0 10px 26px rgba(16,25,43,.08)}.start-summary-row{display:flex;align-items:baseline;justify-content:space-between;gap:10px}.start-summary-row small{overflow:hidden;color:#8491a6;font-size:10px;font-weight:700;white-space:nowrap;text-overflow:ellipsis}.start-summary-row strong{flex:none;color:#173d82;font-size:16px;font-weight:900}.start-summary>button{width:100%;margin-top:12px;padding:12px;border-radius:12px;background:#173f8d;color:#fff;font-size:12px;font-weight:900}.start-summary>button:disabled{background:#c3cddd;color:#8896ab}.inline-error{margin-top:10px;color:#ffb9b9;font-size:10px}.spending-progress{height:8px;margin-top:12px;overflow:hidden;border-radius:20px;background:#e8edf5}.spending-progress i{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#ff8245,#ffd45e)}.progress-card{padding:16px;margin-top:0;background:linear-gradient(165deg,#ffffff 0%,color-mix(in srgb,var(--soft) 55%,white) 100%);border-color:color-mix(in srgb,var(--accent) 18%,#dce5f3);animation:mission-dash-enter .45s cubic-bezier(.22,1,.36,1) both;transition:transform .15s ease}
.progress-card:active{transform:scale(.985)}
.active-missions-card .progress-card:nth-of-type(1){animation-delay:.1s}
.active-missions-card .progress-card:nth-of-type(2){animation-delay:.17s}
.active-missions-card .progress-card:nth-of-type(3){animation-delay:.24s}
.active-missions-card .progress-card:nth-of-type(4){animation-delay:.31s}
.status-chip.in-progress{animation:status-pulse 2.4s ease-in-out infinite}
@keyframes mission-dash-enter{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}
@keyframes fund-bar-grow{from{transform:scaleX(0)}to{transform:scaleX(1)}}
@keyframes status-pulse{0%,100%{box-shadow:0 0 0 0 rgba(20,148,119,.25)}50%{box-shadow:0 0 0 5px rgba(20,148,119,0)}}
@keyframes empty-hero-cta-pulse{0%{box-shadow:0 0 0 0 rgba(255,212,102,.5)}70%,100%{box-shadow:0 0 0 9px rgba(255,212,102,0)}}
@media(prefers-reduced-motion:reduce){.mission-savings-card,.fund-check-card,.active-missions-card,.progress-card,.fund-check-bar>div,.status-chip.in-progress,.start-summary>button,.mission-title-ai-badge,.recommendation-hero .mission-title-ai-badge img{animation:none}}.mission-card-head{display:flex;align-items:center;gap:11px}.status-chip{padding:6px 8px;border-radius:20px;background:#e7edf6;color:#65758e;font-size:9px;font-weight:900}.status-chip.in-progress,.status-chip.success{background:#e4f7f1;color:#149477}.status-chip.failed{background:#ffeded;color:#db5050}.limit-caption{display:flex;justify-content:space-between;gap:8px;margin-top:9px;color:#8290a5;font-size:9px}.mission-card-head h3{font-size:13.5px}.mission-card-head small{display:block;margin-top:3px;color:#5a6478;font-size:10px;font-weight:600;line-height:1.4}.empty-card{margin-bottom:16px}.category-icon-glyph{display:block;width:20px;height:20px}.category-icon-glyph :deep(svg){display:block;width:100%;height:100%}
.dashboard-header{display:flex;align-items:flex-start;justify-content:space-between}
.dashboard-header p{display:flex;align-items:center;gap:4px;font-family:'Space Mono',monospace;font-size:9.5px;font-weight:800;letter-spacing:.15em;color:#0b2a6b;margin-bottom:4px}
.header-wordmark{display:block;width:88px;height:auto;object-fit:contain}
.header-plane{width:12px;height:12px;animation:header-plane-fly 2.6s ease-in-out infinite}
@keyframes header-plane-fly{0%,100%{transform:translateY(0) rotate(0deg);filter:brightness(1) drop-shadow(0 0 0 rgba(47,112,242,0))}25%{transform:translateY(-1.5px) rotate(-8deg)}50%{transform:translateY(0) rotate(0deg);filter:brightness(1.6) drop-shadow(0 0 3px rgba(47,112,242,.55))}75%{transform:translateY(1.5px) rotate(6deg)}}
.dashboard-header h1{margin-top:2px;font-size:17px;font-weight:400;color:#29466f}
.dashboard-header .bell{margin-top:0}
.mission-savings-card{position:relative;overflow:hidden;margin-top:24px;padding:16px 18px;border-radius:16px;background:linear-gradient(145deg,#0c2d72 0%,#174ca7 62%,#2f70d9 100%);color:#fff;box-shadow:0 10px 24px rgba(11,42,107,.24);animation:mission-dash-enter .5s cubic-bezier(.22,1,.36,1) both}
.mission-savings-card::after{content:'';position:absolute;top:-56px;right:-40px;width:150px;height:150px;border-radius:50%;background:rgba(255,255,255,.07);pointer-events:none}
.mission-savings-card>p{font-size:11px;font-weight:800;letter-spacing:.1em;color:#ffd466}
.mission-savings-amount{display:flex;align-items:baseline;gap:8px;margin-top:8px}
.mission-savings-amount strong{font-family:'Space Mono',monospace;font-size:24px;font-weight:700}
.mission-savings-amount span{font-size:12px;font-weight:700;color:rgba(255,255,255,.65)}
.mission-savings-divider{height:1px;background:rgba(255,255,255,.16);margin:14px 0}
.mission-savings-foot{position:relative;z-index:1;display:flex;align-items:center;justify-content:space-between;gap:8px}
.mission-savings-foot span{font-size:12px;font-weight:700;color:rgba(255,255,255,.7)}
.mission-report-link{display:inline-flex;flex:none;align-items:center;gap:5px;padding:7px 12px;border-radius:999px;background:#ffd466;color:#0b2a6b;font-size:10.5px;font-weight:800;white-space:nowrap}
.mission-report-link img{width:12px;height:12px;object-fit:contain}
.mission-report-link:disabled{opacity:.5}
.fund-check-card{margin-top:16px;padding:20px;border:1px solid #e7edf9;border-radius:20px;background:linear-gradient(165deg,#ffffff 0%,#f6f9ff 100%);box-shadow:0 8px 22px rgba(16,25,43,.07);animation:mission-dash-enter .5s .08s cubic-bezier(.22,1,.36,1) both}
.fund-check-head-row{display:flex;width:100%;align-items:flex-start;justify-content:space-between;gap:12px;padding:0;border:none;background:none;font:inherit;text-align:left;cursor:pointer}
.fund-check-title{font-size:17px;font-weight:900;color:#173f8d}
.fund-check-history-button{flex:none;color:#286ce0;font-size:10.5px;font-weight:900}
.fund-check-total-block{flex:none;text-align:right}
.fund-check-total-label{font-size:11.5px;font-weight:700;color:#98a2b3}
.fund-check-total{font-family:'Space Mono',monospace;font-size:19px;font-weight:800;color:#10192b;margin-top:6px}
.fund-check-divider{height:1px;background:#f1f3f8;margin:18px 0}
.fund-check-row-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.fund-check-row-heading>span:first-child{font-size:13.5px;font-weight:900;color:#10192b}
.fund-check-row-heading>span:last-child{font-size:11px;font-weight:700;color:#98a2b3}
.fund-check-list{display:flex;flex-direction:column;gap:13px}
.fund-check-row{display:flex;align-items:center;gap:10px}
.fund-check-icon{display:grid;width:32px;height:32px;flex-shrink:0;place-items:center;border-radius:11px;font-size:15px}
.fund-check-name{width:42px;flex-shrink:0;font-size:12.5px;font-weight:700;color:#10192b}
.fund-check-bar{flex:1;height:6px;border-radius:99px;background:#edf0f6;overflow:hidden}
.fund-check-bar>div{height:100%;border-radius:inherit;background:linear-gradient(90deg,#0b2a6b,#2f70d9);transform-origin:left;animation:fund-bar-grow .9s cubic-bezier(.22,1,.36,1) both}
.fund-check-row:nth-child(1) .fund-check-bar>div{animation-delay:.08s}
.fund-check-row:nth-child(2) .fund-check-bar>div{animation-delay:.15s}
.fund-check-row:nth-child(3) .fund-check-bar>div{animation-delay:.22s}
.fund-check-row:nth-child(4) .fund-check-bar>div{animation-delay:.29s}
.fund-check-row:nth-child(5) .fund-check-bar>div{animation-delay:.36s}
.fund-check-amount{flex-shrink:0;font-family:'Space Mono',monospace;font-size:11px;color:#5a6478;white-space:nowrap}
.fund-check-ratio{flex-shrink:0;width:30px;text-align:right;font-family:'Space Mono',monospace;font-size:12px;font-weight:800;color:#98a2b3}
.fund-modal-overlay{position:fixed;top:0;bottom:0;left:50%;width:100%;max-width:390px;transform:translateX(-50%);z-index:80;display:flex;align-items:center;justify-content:center;padding:24px;background:rgba(10,18,36,.5);animation:fund-modal-fade .2s ease both}
.fund-modal{position:relative;width:100%;max-width:460px;max-height:86vh;overflow:hidden;display:flex;flex-direction:column;padding:22px 0 26px;border-radius:26px;background:#eaf4ff;animation:fund-modal-rise .24s cubic-bezier(.22,1,.36,1) both}
.fund-modal-close{flex:none;width:30px;height:30px;background:transparent;color:#173f8d;font-size:21px;font-weight:400;box-shadow:none}
.fund-check-total-block--stacked{display:flex;align-items:baseline;gap:6px;margin-top:10px;text-align:left}
.fund-check-total-block--stacked .fund-check-total{margin-top:0}
.fund-modal-body{overflow-y:auto;overscroll-behavior:contain;padding:18px 22px 6px}
.fund-check-head-row--static{align-items:center;cursor:default}
.fund-modal-title-group{display:flex;align-items:center;gap:8px;min-width:0}
.fund-modal-select{position:relative;flex:none}
.fund-modal-select-trigger{display:flex;align-items:center;gap:4px;padding:5px 9px;border:1px solid #dbe3ef;border-radius:9px;background:#fff;color:#10192b;font-size:11.5px;font-weight:800}
.fund-modal-select-chevron{color:#557092;font-size:9px;transition:transform .18s ease}
.fund-modal-select-chevron.open{transform:rotate(180deg)}
.fund-modal-select-backdrop{position:fixed;inset:0;z-index:1}
.fund-modal-select-list{position:absolute;top:calc(100% + 6px);left:0;z-index:2;width:150px;max-height:240px;overflow-y:auto;margin:0;padding:6px;list-style:none;border:1px solid #e7edf9;border-radius:14px;background:#fff;box-shadow:0 14px 30px rgba(16,25,43,.18)}
.fund-modal-select-list li+li{margin-top:2px}
.fund-modal-select-list button{display:block;width:100%;padding:10px 11px;border-radius:9px;background:none;color:#334361;font-size:13.5px;font-weight:700;text-align:left}
.fund-modal-select-list button.active{background:#eaf2ff;color:#1768f2;font-weight:900}
@keyframes fund-modal-fade{from{opacity:0}to{opacity:1}}
@keyframes fund-modal-rise{from{transform:scale(.95);opacity:0}to{transform:scale(1);opacity:1}}
.active-missions-card{display:flex;flex-direction:column;gap:14px;margin-top:16px;padding:20px;border:1px solid #bcd2ff;border-radius:20px;background:linear-gradient(150deg,#eff5ff 0%,#e6f0ff 100%);box-shadow:0 8px 22px rgba(16,25,43,.07);animation:mission-dash-enter .5s .16s cubic-bezier(.22,1,.36,1) both}
.active-missions-head{display:flex;align-items:center}
.active-missions-title{display:inline-flex;align-items:center;gap:2px;font-size:14px;font-weight:900;color:#173f8d}
.active-missions-empty{font-size:12.5px;font-weight:600;color:#5a6478;line-height:1.5}
.dashboard-cta{border-radius:99px;padding:13px;text-align:center;font-size:13px;font-weight:800;background:#0b2a6b;color:#fff}
.mission-home-setup{position:relative;margin-top:28px;padding:18px;border:1px solid #d6e3fa;border-radius:22px;background:#fff;box-shadow:0 10px 24px rgb(36 80 153 / 7%)}.mission-home-label{display:block;margin-bottom:10px;color:#286ce0;font-size:9px;font-weight:950;letter-spacing:.12em}.mission-home-setup-body{display:flex;min-height:210px;padding:22px 18px 18px;flex-direction:column;align-items:center;justify-content:center;border:1px dashed #c9d8ef;border-radius:17px;background:linear-gradient(180deg,#f7faff,#f3f7fd);text-align:center}.mission-flight{position:relative;width:126px;height:45px;margin-bottom:13px}.mission-flight-route{position:absolute;top:22px;left:8px;right:8px;border-top:2px dashed #b9ccef}.mission-flight-start,.mission-flight-end{position:absolute;top:18px;width:10px;height:10px;border:2px solid #8eafe5;border-radius:50%;background:#f6f9ff}.mission-flight-start{left:2px}.mission-flight-end{right:2px}.mission-flight-end::after{position:absolute;inset:-6px;border:1px solid rgb(40 108 224 / 28%);border-radius:50%;content:'';animation:mission-destination-pulse 1.9s ease-out infinite}.mission-flight img{position:absolute;z-index:2;top:10px;left:7px;width:25px;height:25px;filter:drop-shadow(0 5px 5px rgb(40 108 224 / 22%));animation:mission-plane-travel 2.8s ease-in-out infinite}.mission-home-setup-body h2{color:#26334d;font-size:14px;font-weight:900}.mission-home-setup-body p{max-width:290px;margin-top:7px;color:#8190a9;font-size:10px;line-height:1.55;word-break:keep-all}.mission-home-setup-body button{margin-top:17px;padding:11px 22px;border-radius:12px;background:#245ec4;color:#fff;font-size:12px;font-weight:900}@keyframes mission-plane-travel{0%{opacity:.35;transform:translate(0,4px) rotate(-8deg)}18%{opacity:1}50%{transform:translate(47px,-5px) rotate(2deg)}82%{opacity:1}100%{opacity:.35;transform:translate(94px,2px) rotate(9deg)}}@keyframes mission-destination-pulse{0%{opacity:.8;transform:scale(.55)}100%{opacity:0;transform:scale(1.45)}}@media(prefers-reduced-motion:reduce){.mission-flight img,.mission-flight-end::after{animation:none}.mission-flight img{left:50%;transform:translateX(-50%)}}
.mission-ai-stage{position:relative;width:82px;height:72px;margin-bottom:10px}.mission-ai-core{position:absolute;top:8px;left:13px;z-index:2;display:grid;width:56px;height:56px;place-items:center;border-radius:20px;background:linear-gradient(145deg,#dbe8ff,#fff);box-shadow:0 10px 24px rgb(40 108 224 / 20%);animation:mission-ai-float 2.6s ease-in-out infinite}.mission-ai-core img{width:30px;height:30px;filter:invert(34%) sepia(94%) saturate(1272%) hue-rotate(199deg) brightness(91%)}.mission-ai-orbit{position:absolute;inset:0;border:1.5px dashed #9fb9e8;border-radius:50%;animation:mission-ai-orbit 7s linear infinite}.mission-ai-spark{position:absolute;z-index:3;color:#4a82df;font-size:13px;animation:mission-ai-spark 1.8s ease-in-out infinite}.mission-ai-spark.one{top:0;right:2px}.mission-ai-spark.two{bottom:2px;left:0;animation-delay:.8s}@keyframes mission-ai-float{0%,100%{transform:translateY(0) rotate(-2deg)}50%{transform:translateY(-5px) rotate(2deg)}}@keyframes mission-ai-orbit{to{transform:rotate(360deg)}}@keyframes mission-ai-spark{0%,100%{opacity:.2;transform:scale(.65) rotate(0)}50%{opacity:1;transform:scale(1.2) rotate(90deg)}}@media(prefers-reduced-motion:reduce){.mission-ai-core,.mission-ai-orbit,.mission-ai-spark{animation:none}}
.mission-page{max-width:390px;margin:0 auto}
.option-card.locked{border-color:#dce5f3;background:#f8fafe;box-shadow:none}.option-card.locked .option-toggle{cursor:default}.option-card.locked .option-toggle>i{width:auto;min-width:27px;padding:0 7px;border-radius:12px;background:#e4f7f1;color:#149477;font-size:9px}.add-mission-button{display:flex;width:100%;margin-top:18px;padding:15px 16px;align-items:center;gap:12px;border:1px dashed #8badde;border-radius:18px;background:#f7faff;color:#1d4f9f;text-align:left;transition:transform .2s ease,background .2s ease}.add-mission-button:hover{background:#edf4ff;transform:translateY(-2px)}.add-mission-button>span{display:grid;flex:0 0 38px;height:38px;place-items:center;border-radius:12px;background:#e4eeff;font-size:20px;font-weight:800}.add-mission-button>div{min-width:0;flex:1}.add-mission-button small{display:block;color:#7284a2;font-size:10px}.add-mission-button strong{display:block;margin-top:3px;font-size:14px;font-weight:900}.add-mission-button>b{font-size:24px}</style>
