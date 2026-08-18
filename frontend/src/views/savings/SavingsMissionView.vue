<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import NotificationBell from '@/components/common/NotificationBell.vue'
import { useSavingMissionsStore } from '@/stores/savingMissions'
import { useSavingReadinessStore } from '@/stores/savingReadiness'
import { useMonthlyFundStore } from '@/stores/monthlyFund'
import foodIcon from '@/assets/icons/food.svg'
import cafeIcon from '@/assets/icons/cafe.svg'
import shoppingIcon from '@/assets/icons/shopping-cart.svg'
import taxiIcon from '@/assets/icons/taxi.svg'
import leisureIcon from '@/assets/icons/hobby_drink.svg'
import aiIcon from '@/assets/icons/ai.svg'

const route = useRoute()
const router = useRouter()
const missionStore = useSavingMissionsStore()
const readinessStore = useSavingReadinessStore()
const monthlyFundStore = useMonthlyFundStore()

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
const showSelection = ref(route.query.from === 'analysis')

// 추천 미션을 선택/추가하는 화면인지 여부: 이 상태에서만 뒤로가기 헤더를 쓴다.
const showSelectionFlow = computed(
  () => showSelection.value || missionStore.addingMissions,
)

// 미션 시작 여부와 무관하게 보여줄 메인 탭 대시보드 여부
const showDashboard = computed(
  () =>
    readinessStore.isReady &&
    !readinessStore.loading &&
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
const dashboardCategories = computed(() =>
  monthlyFundStore.categorySummaries.map((item) => {
    const code = fundCategoryCodeMap[item.id] || 'OTHER'
    const meta = metaOf(code)
    return {
      code: item.id,
      name: item.name,
      icon: meta.icon,
      iconSrc: meta.iconSrc,
      amount: Number(item.spent || 0),
      ratio: Number(item.percent || 0),
    }
  }),
)
const dashboardTotalSpending = computed(() =>
  monthlyFundStore.categorySummaries.reduce((sum, item) => sum + Number(item.spent || 0), 0),
)

function openSelection() {
  showSelection.value = true
}

const categoryMeta = {
  FOOD: { icon: '🍴', iconSrc: foodIcon, color: '#ff7548', soft: '#fff0e9' },
  CAFE: { icon: '☕', iconSrc: cafeIcon, color: '#d88b22', soft: '#fff5dc' },
  SHOPPING: { icon: '🛍️', iconSrc: shoppingIcon, color: '#8e63d4', soft: '#f4edff' },
  LIVING: { icon: '🏠', color: '#19a88b', soft: '#e5f8f2' },
  TRANSPORT: { icon: '🚌', iconSrc: taxiIcon, color: '#3478e5', soft: '#eaf2ff' },
  LEISURE: { icon: '🎮', iconSrc: leisureIcon, color: '#e25283', soft: '#ffedf3' },
  OTHER: { icon: '📦', color: '#718096', soft: '#edf2f7' },
}

const analysisMonthLabel = computed(() => monthLabel(missionStore.analysisYearMonth))
const targetMonthLabel = computed(() => monthLabel(missionStore.targetYearMonth))

onMounted(async () => {
  await loadReadinessAndMissions()
})

async function loadReadinessAndMissions() {
  const yearMonth = String(route.query.yearMonth || '')
  await readinessStore.load({ force: true })
  if (readinessStore.isReady) {
    missionStore.load(yearMonth || undefined)
  }
}

function retryReadiness() {
  loadReadinessAndMissions()
}

function goTravelGoalSetup() {
  router.push({ name: 'TravelRegister' })
}

function goFinancialSources() {
  router.push('/profile/financial?step=1&from=asset')
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
  if (showSelection.value && !missionStore.hasStartedMissions) {
    showSelection.value = false
    return
  }
  if (window.history.length > 1) router.back()
  else router.push({ name: 'Home' })
}
</script>

<template>
  <main class="mission-page">
    <div ref="missionHeaderEl" class="mission-header-fixed">
      <header v-if="!showSelectionFlow" class="dashboard-header">
        <div>
          <p>
            <img src="@/assets/icons/blue_airplane.svg" class="header-plane" alt="" />
            TRIPASS
          </p>
          <h1>MISSION</h1>
        </div>
        <NotificationBell />
      </header>
      <header v-else class="page-header">
        <button type="button" aria-label="뒤로 가기" @click="goBack">‹</button>
        <div>
          <p>AI SAVING COACH</p>
          <h1>저축 미션</h1>
        </div>
        <button
          type="button"
          class="report-link"
          :disabled="!missionStore.analysisYearMonth"
          @click="openAnalysis"
        >
          리포트
        </button>
      </header>
    </div>
    <div :style="{ height: missionHeaderHeight + 'px' }" aria-hidden="true" />

    <section v-if="readinessStore.loading" class="state-card loading-card">
      <span class="loading-plane">✈</span>
      <h2>AI 미션을 준비하고 있어요</h2>
      <p>지난달 소비 분석과 저장된 미션을 확인하고 있어요.</p>
    </section>

    <section v-else-if="readinessStore.errorMessage" class="state-card error-card">
      <span>!</span>
      <h2>준비 상태를 확인하지 못했어요</h2>
      <p>{{ readinessStore.errorMessage }}</p>
      <button type="button" @click="retryReadiness">다시 시도</button>
    </section>

    <section v-else-if="readinessStore.needsTravelGoalAndFinancialAsset" class="mission-home-setup">
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

    <section v-else-if="readinessStore.needsTravelGoal" class="state-card guide-card">
      <span>＋</span>
      <h2>아직 여행 목표를 설정하지 않았어요</h2>
      <p>여행 목표를 설정하면 맞춤 저축 미션을 확인할 수 있어요.</p>
      <button type="button" @click="goTravelGoalSetup">여행 목표 설정하기</button>
    </section>

    <section v-else-if="readinessStore.needsFinancialAsset" class="state-card guide-card">
      <span>＋</span>
      <h2>계좌나 카드를 연결해 주세요</h2>
      <p>거래내역이 쌓이면 소비 분석과 맞춤 저축 미션을 확인할 수 있어요.</p>
      <button type="button" @click="goFinancialSources">금융 데이터 연결하기</button>
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
        </div>
      </section>

      <section class="fund-check-card">
        <span class="fund-check-title">이달의 자금 체크</span>
        <div class="fund-check-total-label">총 지출</div>
        <div class="fund-check-total">{{ formatCurrency(dashboardTotalSpending) }}</div>
        <div class="fund-check-divider"></div>
        <div class="fund-check-row-heading">
          <span>카테고리별 지출</span>
          <span>이번 달</span>
        </div>
        <div class="fund-check-list">
          <div v-for="category in dashboardCategories" :key="category.code" class="fund-check-row">
            <span class="fund-check-icon">
              <img v-if="category.iconSrc" :src="category.iconSrc" alt="" />
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
          <span class="active-missions-title">수행 중 미션</span>
          <span v-if="missionStore.hasStartedMissions" class="active-missions-month">{{ targetMonthLabel }}</span>
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
                <img v-if="metaOf(mission.categoryCode).iconSrc" :src="metaOf(mission.categoryCode).iconSrc" alt="" />
                <template v-else>{{ metaOf(mission.categoryCode).icon }}</template>
              </span>
              <div>
                <small>{{ mission.categoryName }} · {{ mission.reductionRate }}% 절감</small>
                <h3>{{ mission.categoryName }} 소비 줄이기</h3>
              </div>
              <span :class="['status-chip', statusClass(mission.status)]">
                {{ statusLabel(mission.status) }}
              </span>
            </div>

            <template v-if="displayedWeek(mission)">
              <div class="week-heading">
                <b>{{ displayedWeek(mission).weekNumber }}주차 미션</b>
                <span>
                  {{ formatDate(displayedWeek(mission).periodStartDate) }}~{{ formatDate(displayedWeek(mission).periodEndDate) }}
                </span>
              </div>
              <p class="mission-message">{{ displayedWeek(mission).missionMessage }}</p>
              <div class="spending-numbers">
                <div><small>사용 금액</small><strong>{{ formatCurrency(displayedWeek(mission).actualSpending) }}</strong></div>
                <div><small>남은 한도</small><strong>{{ formatCurrency(remainingLimit(displayedWeek(mission))) }}</strong></div>
              </div>
              <div class="spending-progress">
                <i :style="{ width: `${weekProgress(displayedWeek(mission))}%` }"></i>
              </div>
              <p class="limit-caption">
                주간 사용 한도 {{ formatCurrency(displayedWeek(mission).weeklyUsageLimit) }}
              </p>
            </template>

            <div class="week-dots">
              <span
                v-for="week in mission.weeklyMissions"
                :key="week.id"
                :class="statusClass(week.status)"
                :title="`${week.weekNumber}주차 ${statusLabel(week.status)}`"
              >{{ week.weekNumber }}</span>
            </div>
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

      <button
        type="button"
        class="dashboard-cta outline"
        :disabled="!missionStore.analysisYearMonth"
        @click="openAnalysis"
      >
        지난 달 리포트 보러가기
      </button>
    </template>

    <template v-else>
      <section class="recommendation-hero">
        <div class="ai-badge">AI</div>
        <div>
          <p>{{ analysisMonthLabel }} 소비 분석 완료</p>
          <h2>{{ missionStore.hasStartedMissions ? '새로 도전할 항목을' : '줄일 항목과 절감률을' }}<br />직접 선택해 보세요</h2>
          <span v-if="missionStore.hasStartedMissions">진행 중인 미션은 유지하고, 선택한 목표만 새로 추가돼요.</span>
          <span v-else>선택한 목표는 {{ targetMonthLabel }} 주간 미션으로 만들어져요.</span>
        </div>
      </section>

      <section v-if="missionStore.options.length" class="option-section">
        <div class="section-title">
          <div>
            <small>AI RECOMMENDATION</small>
            <h2>추천 절약 카테고리</h2>
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
              <img v-if="metaOf(category.categoryCode).iconSrc" :src="metaOf(category.categoryCode).iconSrc" alt="" />
              <template v-else>{{ metaOf(category.categoryCode).icon }}</template>
            </span>
            <div>
              <small>추천 {{ category.recommendationRank }}순위</small>
              <h3>{{ category.categoryName }}</h3>
              <p>{{ category.recommendationReason }}</p>
            </div>
            <i>{{ isStarted(category.categoryId) ? '진행' : (isSelected(category.categoryId) ? '✓' : '+') }}</i>
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
        <div>
          <small>선택한 미션을 모두 달성하면</small>
          <strong>{{ formatCurrency(missionStore.hasStartedMissions ? missionStore.newExpectedSavingAmount : missionStore.expectedSavingAmount) }}</strong>
          <p>{{ targetMonthLabel }} 여행 자금으로 더 모을 수 있어요.</p>
        </div>
        <p v-if="missionStore.errorMessage" class="inline-error">{{ missionStore.errorMessage }}</p>
        <button
          type="button"
          :disabled="(missionStore.hasStartedMissions ? missionStore.newSelectedCount : missionStore.selectedCount) === 0 || missionStore.submitting"
          @click="missionStore.startMissions"
        >
          {{ missionStore.submitting ? '미션을 만들고 있어요...' : `${missionStore.hasStartedMissions ? missionStore.newSelectedCount : missionStore.selectedCount}개 미션 ${missionStore.hasStartedMissions ? '추가하기' : '시작하기'}` }}
        </button>
        <button
          v-if="missionStore.hasStartedMissions"
          type="button"
          class="cancel-add-button"
          :disabled="missionStore.submitting"
          @click="missionStore.cancelAddingMissions"
        >기존 미션으로 돌아가기</button>
      </section>
    </template>

    <BottomNav />
  </main>
</template>

<style scoped>
.mission-page{--navy:#153b86;min-height:100vh;padding:0 18px 104px;background:linear-gradient(180deg,#f4f7ff 0%,#eef3fc 100%);color:#132348}.mission-header-fixed{position:fixed;top:0;left:50%;z-index:60;width:100%;padding:42px 18px 14px;background:#f4f7ff;transform:translateX(-50%)}@media(min-width:600px){.mission-header-fixed{max-width:430px}}.page-header{display:flex;align-items:center;justify-content:space-between}.page-header>button{width:42px;height:42px;border-radius:14px;background:#fff;color:#193d82;font-size:28px;font-weight:700;box-shadow:0 5px 16px #24487512}.page-header>div{text-align:center}.page-header p,.section-title small{color:#f07638;font-size:9px;font-weight:900;letter-spacing:.16em}.page-header h1{margin-top:3px;font-size:22px;font-weight:900;letter-spacing:-.05em}.page-header .report-link{font-size:11px}.page-header .report-link:disabled{opacity:.45}.state-card{margin-top:28px;padding:34px 24px;border:1px solid #d5e2f8;border-radius:24px;background:#fff;text-align:center;box-shadow:0 10px 30px #1e40700b}.state-card>span{display:grid;width:54px;height:54px;margin:0 auto;place-items:center;border-radius:18px;background:#edf3ff;color:#316fe0;font-size:25px;font-weight:900}.state-card h2{margin-top:15px;font-size:17px;font-weight:900}.state-card p{margin-top:7px;color:#73829d;font-size:11px;line-height:1.6}.state-card button{margin-top:19px;padding:11px 22px;border-radius:12px;background:#245ec4;color:#fff;font-size:12px;font-weight:900}.loading-plane{animation:fly 1.4s ease-in-out infinite}@keyframes fly{50%{transform:translate(8px,-5px)}}.recommendation-hero{display:flex;gap:15px;margin-top:24px;padding:22px;border-radius:25px;background:linear-gradient(135deg,#173c89,#2f70d9);color:#fff;box-shadow:0 13px 30px #163f8d2b}.ai-badge{display:grid;flex:0 0 52px;height:52px;place-items:center;border:1px solid #ffffff60;border-radius:18px;background:#ffffff18;font-size:18px;font-weight:900}.recommendation-hero p{color:#ffc36b;font-size:10px;font-weight:900}.recommendation-hero h2{margin-top:7px;font-size:20px;font-weight:900;line-height:1.35;letter-spacing:-.04em}.recommendation-hero span{display:block;margin-top:8px;color:#cbdcff;font-size:10px;line-height:1.45}.option-section{margin-top:27px}.section-title{display:flex;align-items:end;justify-content:space-between}.section-title h2{margin-top:4px;font-size:19px;font-weight:900}.section-title>span{padding:6px 10px;border-radius:20px;background:#e1ebfc;color:#3966aa;font-size:10px;font-weight:800}.option-card,.progress-card{--accent:#3478e5;--soft:#eaf2ff;margin-top:12px;overflow:hidden;border:1px solid #dce5f3;border-radius:21px;background:#fff;box-shadow:0 8px 22px #263e650b}.option-card.selected{border-color:var(--accent);box-shadow:0 10px 25px color-mix(in srgb,var(--accent) 16%,transparent)}.option-toggle{display:flex;width:100%;align-items:center;gap:12px;padding:16px;text-align:left}.category-icon{display:grid;flex:0 0 43px;height:43px;place-items:center;border-radius:14px;background:var(--soft);font-size:20px}.option-toggle>div,.mission-card-head>div{min-width:0;flex:1}.option-toggle small,.mission-card-head small{color:var(--accent);font-size:9px;font-weight:900}.option-toggle h3,.mission-card-head h3{margin-top:2px;font-size:15px;font-weight:900}.option-toggle p{margin-top:5px;color:#78869f;font-size:10px;line-height:1.4}.option-toggle>i{display:grid;width:27px;height:27px;place-items:center;border-radius:50%;background:#edf2fa;color:#557092;font-style:normal;font-weight:900}.selected .option-toggle>i{background:var(--accent);color:#fff}.rate-panel{padding:15px 16px 17px;border-top:1px dashed #dbe3ef;background:color-mix(in srgb,var(--soft) 46%,white)}.rate-panel>p{font-size:11px;font-weight:900}.rate-buttons{display:flex;gap:8px;margin-top:10px}.rate-buttons button{flex:1;padding:10px;border:1px solid #cfdaea;border-radius:10px;background:#fff;color:#6a7890;font-size:12px;font-weight:900}.rate-buttons button.active{border-color:var(--accent);background:var(--accent);color:#fff}.rate-result{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:12px}.rate-result>div{padding:11px;border-radius:12px;background:#fff}.rate-result small,.spending-numbers small{display:block;color:#8491a6;font-size:9px}.rate-result strong,.spending-numbers strong{display:block;margin-top:4px;color:#173d82;font-size:13px;font-weight:900}.start-summary{position:sticky;bottom:73px;z-index:5;margin-top:24px;padding:18px;border-radius:22px;background:#102f72;color:#fff;box-shadow:0 14px 32px #0f2f7250}.start-summary small{color:#adc5f2;font-size:9px}.start-summary strong{display:block;margin-top:5px;color:#ffd26d;font-size:22px;font-weight:900}.start-summary div>p{margin-top:3px;color:#c8d8f6;font-size:10px}.start-summary>button{width:100%;margin-top:14px;padding:13px;border-radius:13px;background:linear-gradient(90deg,#ff8843,#ffb44c);font-size:13px;font-weight:900}.start-summary>button:disabled{background:#6b7d9e;color:#d4dceb}.inline-error{margin-top:10px;color:#ffb9b9;font-size:10px}.spending-progress{height:8px;margin-top:12px;overflow:hidden;border-radius:20px;background:#e8edf5}.spending-progress i{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#ff8245,#ffd45e)}.progress-card{padding:16px;margin-top:0}.mission-card-head{display:flex;align-items:center;gap:11px}.status-chip{padding:6px 8px;border-radius:20px;background:#e7edf6;color:#65758e;font-size:9px;font-weight:900}.status-chip.in-progress,.status-chip.success{background:#e4f7f1;color:#149477}.status-chip.failed{background:#ffeded;color:#db5050}.week-heading{display:flex;justify-content:space-between;margin-top:17px;padding-top:14px;border-top:1px dashed #dbe3ef}.week-heading b{font-size:12px}.week-heading span{color:#7d8ba1;font-size:10px}.mission-message{margin-top:7px;color:#62728d;font-size:10px}.spending-numbers{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:13px}.spending-numbers>div{padding:11px;border-radius:12px;background:#f5f8fd}.limit-caption{margin-top:7px;text-align:right;color:#8290a5;font-size:9px}.week-dots{display:flex;gap:7px;margin-top:14px}.week-dots span{display:grid;width:25px;height:25px;place-items:center;border-radius:50%;background:#e9eef6;color:#7d8a9e;font-size:9px;font-weight:900}.week-dots .in-progress{background:#dceaff;color:#2666cb}.week-dots .success{background:#def5ed;color:#118e70}.week-dots .failed{background:#ffe5e5;color:#d84848}.empty-card{margin-bottom:16px}.category-icon img{width:20px;height:20px}
.dashboard-header{display:flex;align-items:flex-start;justify-content:space-between}
.dashboard-header p{display:flex;align-items:center;gap:4px;font-family:'Space Mono',monospace;font-size:9.5px;font-weight:800;letter-spacing:.15em;color:#0b2a6b;margin-bottom:4px}
.header-plane{width:12px;height:12px;animation:header-plane-fly 2.6s ease-in-out infinite}
@keyframes header-plane-fly{0%,100%{transform:translateY(0) rotate(0deg);filter:brightness(1) drop-shadow(0 0 0 rgba(47,112,242,0))}25%{transform:translateY(-1.5px) rotate(-8deg)}50%{transform:translateY(0) rotate(0deg);filter:brightness(1.6) drop-shadow(0 0 3px rgba(47,112,242,.55))}75%{transform:translateY(1.5px) rotate(6deg)}}
.dashboard-header h1{margin-top:2px;font-size:19px;font-weight:900;color:#10192b}
.dashboard-header .bell{margin-top:0}
.mission-savings-card{position:relative;margin-top:24px;padding:18px 20px;border-radius:18px;background:linear-gradient(155deg,#0b2a6b 0%,#123c94 60%,#17459f 100%);color:#fff;box-shadow:0 10px 24px rgba(11,42,107,.24)}
.mission-savings-card>p{font-size:11px;font-weight:800;letter-spacing:.1em;color:#ffd466}
.mission-savings-amount{display:flex;align-items:baseline;gap:8px;margin-top:8px}
.mission-savings-amount strong{font-family:'Space Mono',monospace;font-size:28px;font-weight:700}
.mission-savings-amount span{font-size:12px;font-weight:700;color:rgba(255,255,255,.65)}
.mission-savings-divider{height:1px;background:rgba(255,255,255,.16);margin:14px 0}
.mission-savings-foot{display:flex;align-items:center;justify-content:space-between}
.mission-savings-foot span{font-size:12px;font-weight:700;color:rgba(255,255,255,.7)}
.fund-check-card{margin-top:16px;padding:20px;border-radius:16px;background:#fff;box-shadow:0 4px 14px rgba(16,25,43,.06)}
.fund-check-title{font-size:15px;font-weight:900;color:#10192b}
.fund-check-total-label{font-size:11.5px;font-weight:700;color:#98a2b3;margin-top:16px}
.fund-check-total{font-family:'Space Mono',monospace;font-size:27px;font-weight:800;color:#10192b;margin-top:6px}
.fund-check-divider{height:1px;background:#f1f3f8;margin:18px 0}
.fund-check-row-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.fund-check-row-heading>span:first-child{font-size:13.5px;font-weight:900;color:#10192b}
.fund-check-row-heading>span:last-child{font-size:11px;font-weight:700;color:#98a2b3}
.fund-check-list{display:flex;flex-direction:column;gap:13px}
.fund-check-row{display:flex;align-items:center;gap:10px}
.fund-check-icon{display:grid;width:20px;height:20px;flex-shrink:0;place-items:center;font-size:14px}
.fund-check-icon img{width:16px;height:16px}
.fund-check-name{width:42px;flex-shrink:0;font-size:12.5px;font-weight:700;color:#10192b}
.fund-check-bar{flex:1;height:6px;border-radius:99px;background:#edf0f6;overflow:hidden}
.fund-check-bar>div{height:100%;border-radius:inherit;background:#0b2a6b}
.fund-check-amount{flex-shrink:0;font-family:'Space Mono',monospace;font-size:11px;color:#5a6478;white-space:nowrap}
.fund-check-ratio{flex-shrink:0;width:30px;text-align:right;font-family:'Space Mono',monospace;font-size:12px;font-weight:800;color:#98a2b3}
.active-missions-card{display:flex;flex-direction:column;gap:14px;margin-top:16px;padding:20px;border-radius:16px;background:#fff;box-shadow:0 4px 14px rgba(16,25,43,.06)}
.active-missions-head{display:flex;align-items:center;justify-content:space-between}
.active-missions-title{font-size:15px;font-weight:900;color:#10192b}
.active-missions-month{padding:6px 10px;border-radius:20px;background:#e1ebfc;color:#3966aa;font-size:10px;font-weight:800}
.active-missions-empty{font-size:12.5px;font-weight:600;color:#5a6478;line-height:1.5}
.dashboard-cta{border-radius:99px;padding:13px;text-align:center;font-size:13px;font-weight:800;background:#0b2a6b;color:#fff}
.dashboard-cta.outline{width:100%;margin-top:16px;padding:14px;font-size:13.5px}
.mission-home-setup{position:relative;margin-top:28px;padding:18px;border:1px solid #d6e3fa;border-radius:22px;background:#fff;box-shadow:0 10px 24px rgb(36 80 153 / 7%)}.mission-home-label{display:block;margin-bottom:10px;color:#286ce0;font-size:9px;font-weight:950;letter-spacing:.12em}.mission-home-setup-body{display:flex;min-height:210px;padding:22px 18px 18px;flex-direction:column;align-items:center;justify-content:center;border:1px dashed #c9d8ef;border-radius:17px;background:linear-gradient(180deg,#f7faff,#f3f7fd);text-align:center}.mission-flight{position:relative;width:126px;height:45px;margin-bottom:13px}.mission-flight-route{position:absolute;top:22px;left:8px;right:8px;border-top:2px dashed #b9ccef}.mission-flight-start,.mission-flight-end{position:absolute;top:18px;width:10px;height:10px;border:2px solid #8eafe5;border-radius:50%;background:#f6f9ff}.mission-flight-start{left:2px}.mission-flight-end{right:2px}.mission-flight-end::after{position:absolute;inset:-6px;border:1px solid rgb(40 108 224 / 28%);border-radius:50%;content:'';animation:mission-destination-pulse 1.9s ease-out infinite}.mission-flight img{position:absolute;z-index:2;top:10px;left:7px;width:25px;height:25px;filter:drop-shadow(0 5px 5px rgb(40 108 224 / 22%));animation:mission-plane-travel 2.8s ease-in-out infinite}.mission-home-setup-body h2{color:#26334d;font-size:14px;font-weight:900}.mission-home-setup-body p{max-width:290px;margin-top:7px;color:#8190a9;font-size:10px;line-height:1.55;word-break:keep-all}.mission-home-setup-body button{margin-top:17px;padding:11px 22px;border-radius:12px;background:#245ec4;color:#fff;font-size:12px;font-weight:900}@keyframes mission-plane-travel{0%{opacity:.35;transform:translate(0,4px) rotate(-8deg)}18%{opacity:1}50%{transform:translate(47px,-5px) rotate(2deg)}82%{opacity:1}100%{opacity:.35;transform:translate(94px,2px) rotate(9deg)}}@keyframes mission-destination-pulse{0%{opacity:.8;transform:scale(.55)}100%{opacity:0;transform:scale(1.45)}}@media(prefers-reduced-motion:reduce){.mission-flight img,.mission-flight-end::after{animation:none}.mission-flight img{left:50%;transform:translateX(-50%)}}
.mission-ai-stage{position:relative;width:82px;height:72px;margin-bottom:10px}.mission-ai-core{position:absolute;top:8px;left:13px;z-index:2;display:grid;width:56px;height:56px;place-items:center;border-radius:20px;background:linear-gradient(145deg,#dbe8ff,#fff);box-shadow:0 10px 24px rgb(40 108 224 / 20%);animation:mission-ai-float 2.6s ease-in-out infinite}.mission-ai-core img{width:30px;height:30px;filter:invert(34%) sepia(94%) saturate(1272%) hue-rotate(199deg) brightness(91%)}.mission-ai-orbit{position:absolute;inset:0;border:1.5px dashed #9fb9e8;border-radius:50%;animation:mission-ai-orbit 7s linear infinite}.mission-ai-spark{position:absolute;z-index:3;color:#4a82df;font-size:13px;animation:mission-ai-spark 1.8s ease-in-out infinite}.mission-ai-spark.one{top:0;right:2px}.mission-ai-spark.two{bottom:2px;left:0;animation-delay:.8s}@keyframes mission-ai-float{0%,100%{transform:translateY(0) rotate(-2deg)}50%{transform:translateY(-5px) rotate(2deg)}}@keyframes mission-ai-orbit{to{transform:rotate(360deg)}}@keyframes mission-ai-spark{0%,100%{opacity:.2;transform:scale(.65) rotate(0)}50%{opacity:1;transform:scale(1.2) rotate(90deg)}}@media(prefers-reduced-motion:reduce){.mission-ai-core,.mission-ai-orbit,.mission-ai-spark{animation:none}}
@media (min-width:600px){.mission-page{max-width:430px;margin:0 auto}}
.option-card.locked{border-color:#dce5f3;background:#f8fafe;box-shadow:none}.option-card.locked .option-toggle{cursor:default}.option-card.locked .option-toggle>i{background:#e4f7f1;color:#149477;font-size:9px}.add-mission-button{display:flex;width:100%;margin-top:18px;padding:15px 16px;align-items:center;gap:12px;border:1px dashed #8badde;border-radius:18px;background:#f7faff;color:#1d4f9f;text-align:left;transition:transform .2s ease,background .2s ease}.add-mission-button:hover{background:#edf4ff;transform:translateY(-2px)}.add-mission-button>span{display:grid;flex:0 0 38px;height:38px;place-items:center;border-radius:12px;background:#e4eeff;font-size:20px;font-weight:800}.add-mission-button>div{min-width:0;flex:1}.add-mission-button small{display:block;color:#7284a2;font-size:9px}.add-mission-button strong{display:block;margin-top:3px;font-size:13px;font-weight:900}.add-mission-button>b{font-size:24px}.cancel-add-button{margin-top:8px!important;background:transparent!important;color:#c8d8f6!important;box-shadow:none!important}.cancel-add-button:hover{color:#fff!important}
</style>
