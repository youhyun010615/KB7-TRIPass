<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useSavingMissionsStore } from '@/stores/savingMissions'

const route = useRoute()
const router = useRouter()
const missionStore = useSavingMissionsStore()

const categoryMeta = {
  FOOD: { icon: '🍴', color: '#ff7548', soft: '#fff0e9' },
  CAFE: { icon: '☕', color: '#d88b22', soft: '#fff5dc' },
  SHOPPING: { icon: '🛍️', color: '#8e63d4', soft: '#f4edff' },
  LIVING: { icon: '🏠', color: '#19a88b', soft: '#e5f8f2' },
  TRANSPORT: { icon: '🚌', color: '#3478e5', soft: '#eaf2ff' },
  LEISURE: { icon: '🎮', color: '#e25283', soft: '#ffedf3' },
  OTHER: { icon: '📦', color: '#718096', soft: '#edf2f7' },
}

const analysisMonthLabel = computed(() => monthLabel(missionStore.analysisYearMonth))
const targetMonthLabel = computed(() => monthLabel(missionStore.targetYearMonth))
const progressPercent = computed(() => {
  const planned = Number(missionStore.missions?.totalPlannedSavingAmount || 0)
  const reward = Number(missionStore.missions?.totalRewardAmount || 0)
  if (!planned) return 0
  return Math.min(100, Math.round((reward / planned) * 100))
})

onMounted(() => {
  const yearMonth = String(route.query.yearMonth || '')
  missionStore.load(yearMonth || undefined)
})

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
  if (window.history.length > 1) router.back()
  else router.push({ name: 'Home' })
}
</script>

<template>
  <main class="mission-page">
    <header class="page-header">
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

    <section v-if="missionStore.loading" class="state-card loading-card">
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

    <template v-else-if="missionStore.hasStartedMissions">
      <section class="mission-hero">
        <p>{{ targetMonthLabel }} MISSION SAVINGS</p>
        <div class="hero-amount">
          <strong>{{ formatCurrency(missionStore.missions.totalRewardAmount) }}</strong>
          <span>/ {{ formatCurrency(missionStore.missions.totalPlannedSavingAmount) }}</span>
        </div>
        <div class="hero-progress">
          <i :style="{ width: `${progressPercent}%` }"></i>
        </div>
        <div class="hero-foot">
          <span>미션 {{ missionStore.missions.missionCount }}개 진행</span>
          <b>{{ progressPercent }}%</b>
        </div>
      </section>

      <section class="progress-section">
        <div class="section-title">
          <div>
            <small>ACTIVE MISSIONS</small>
            <h2>수행 중인 미션</h2>
          </div>
          <span>{{ targetMonthLabel }}</span>
        </div>

        <article
          v-for="mission in missionStore.missions.missions"
          :key="mission.id"
          class="progress-card"
          :style="{ '--accent': metaOf(mission.categoryCode).color, '--soft': metaOf(mission.categoryCode).soft }"
        >
          <div class="mission-card-head">
            <span class="category-icon">{{ metaOf(mission.categoryCode).icon }}</span>
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
      </section>
    </template>

    <template v-else>
      <section class="recommendation-hero">
        <div class="ai-badge">AI</div>
        <div>
          <p>{{ analysisMonthLabel }} 소비 분석 완료</p>
          <h2>줄일 항목과 절감률을<br />직접 선택해 보세요</h2>
          <span>선택한 목표는 {{ targetMonthLabel }} 주간 미션으로 만들어져요.</span>
        </div>
      </section>

      <section v-if="missionStore.options.length" class="option-section">
        <div class="section-title">
          <div>
            <small>AI RECOMMENDATION</small>
            <h2>추천 절약 카테고리</h2>
          </div>
          <span>{{ missionStore.selectedCount }}/3 선택</span>
        </div>

        <article
          v-for="category in missionStore.options"
          :key="category.categoryId"
          :class="['option-card', { selected: isSelected(category.categoryId) }]"
          :style="{ '--accent': metaOf(category.categoryCode).color, '--soft': metaOf(category.categoryCode).soft }"
        >
          <button
            type="button"
            class="option-toggle"
            :aria-pressed="isSelected(category.categoryId)"
            @click="missionStore.toggleCategory(category)"
          >
            <span class="category-icon">{{ metaOf(category.categoryCode).icon }}</span>
            <div>
              <small>추천 {{ category.recommendationRank }}순위</small>
              <h3>{{ category.categoryName }}</h3>
              <p>{{ category.recommendationReason }}</p>
            </div>
            <i>{{ isSelected(category.categoryId) ? '✓' : '+' }}</i>
          </button>

          <div v-if="isSelected(category.categoryId)" class="rate-panel">
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
          <strong>{{ formatCurrency(missionStore.expectedSavingAmount) }}</strong>
          <p>{{ targetMonthLabel }} 여행 자금으로 더 모을 수 있어요.</p>
        </div>
        <p v-if="missionStore.errorMessage" class="inline-error">{{ missionStore.errorMessage }}</p>
        <button
          type="button"
          :disabled="missionStore.selectedCount === 0 || missionStore.submitting"
          @click="missionStore.startMissions"
        >
          {{ missionStore.submitting ? '미션을 만들고 있어요...' : `${missionStore.selectedCount}개 미션 시작하기` }}
        </button>
      </section>
    </template>

    <BottomNav />
  </main>
</template>

<style scoped>
.mission-page{--navy:#153b86;min-height:100vh;padding:28px 18px 104px;background:linear-gradient(180deg,#f4f7ff 0%,#eef3fc 100%);color:#132348}.page-header{display:flex;align-items:center;justify-content:space-between}.page-header>button{width:42px;height:42px;border-radius:14px;background:#fff;color:#193d82;font-size:28px;font-weight:700;box-shadow:0 5px 16px #24487512}.page-header>div{text-align:center}.page-header p,.section-title small,.mission-hero>p{color:#f07638;font-size:9px;font-weight:900;letter-spacing:.16em}.page-header h1{margin-top:3px;font-size:22px;font-weight:900;letter-spacing:-.05em}.page-header .report-link{font-size:11px}.page-header .report-link:disabled{opacity:.45}.state-card{margin-top:28px;padding:34px 24px;border:1px solid #d5e2f8;border-radius:24px;background:#fff;text-align:center;box-shadow:0 10px 30px #1e40700b}.state-card>span{display:grid;width:54px;height:54px;margin:0 auto;place-items:center;border-radius:18px;background:#edf3ff;color:#316fe0;font-size:25px;font-weight:900}.state-card h2{margin-top:15px;font-size:17px;font-weight:900}.state-card p{margin-top:7px;color:#73829d;font-size:11px;line-height:1.6}.state-card button{margin-top:19px;padding:11px 22px;border-radius:12px;background:#245ec4;color:#fff;font-size:12px;font-weight:900}.loading-plane{animation:fly 1.4s ease-in-out infinite}@keyframes fly{50%{transform:translate(8px,-5px)}}.recommendation-hero{display:flex;gap:15px;margin-top:24px;padding:22px;border-radius:25px;background:linear-gradient(135deg,#173c89,#2f70d9);color:#fff;box-shadow:0 13px 30px #163f8d2b}.ai-badge{display:grid;flex:0 0 52px;height:52px;place-items:center;border:1px solid #ffffff60;border-radius:18px;background:#ffffff18;font-size:18px;font-weight:900}.recommendation-hero p{color:#ffc36b;font-size:10px;font-weight:900}.recommendation-hero h2{margin-top:7px;font-size:20px;font-weight:900;line-height:1.35;letter-spacing:-.04em}.recommendation-hero span{display:block;margin-top:8px;color:#cbdcff;font-size:10px;line-height:1.45}.option-section,.progress-section{margin-top:27px}.section-title{display:flex;align-items:end;justify-content:space-between}.section-title h2{margin-top:4px;font-size:19px;font-weight:900}.section-title>span{padding:6px 10px;border-radius:20px;background:#e1ebfc;color:#3966aa;font-size:10px;font-weight:800}.option-card,.progress-card{--accent:#3478e5;--soft:#eaf2ff;margin-top:12px;overflow:hidden;border:1px solid #dce5f3;border-radius:21px;background:#fff;box-shadow:0 8px 22px #263e650b}.option-card.selected{border-color:var(--accent);box-shadow:0 10px 25px color-mix(in srgb,var(--accent) 16%,transparent)}.option-toggle{display:flex;width:100%;align-items:center;gap:12px;padding:16px;text-align:left}.category-icon{display:grid;flex:0 0 43px;height:43px;place-items:center;border-radius:14px;background:var(--soft);font-size:20px}.option-toggle>div,.mission-card-head>div{min-width:0;flex:1}.option-toggle small,.mission-card-head small{color:var(--accent);font-size:9px;font-weight:900}.option-toggle h3,.mission-card-head h3{margin-top:2px;font-size:15px;font-weight:900}.option-toggle p{margin-top:5px;color:#78869f;font-size:10px;line-height:1.4}.option-toggle>i{display:grid;width:27px;height:27px;place-items:center;border-radius:50%;background:#edf2fa;color:#557092;font-style:normal;font-weight:900}.selected .option-toggle>i{background:var(--accent);color:#fff}.rate-panel{padding:15px 16px 17px;border-top:1px dashed #dbe3ef;background:color-mix(in srgb,var(--soft) 46%,white)}.rate-panel>p{font-size:11px;font-weight:900}.rate-buttons{display:flex;gap:8px;margin-top:10px}.rate-buttons button{flex:1;padding:10px;border:1px solid #cfdaea;border-radius:10px;background:#fff;color:#6a7890;font-size:12px;font-weight:900}.rate-buttons button.active{border-color:var(--accent);background:var(--accent);color:#fff}.rate-result{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:12px}.rate-result>div{padding:11px;border-radius:12px;background:#fff}.rate-result small,.spending-numbers small{display:block;color:#8491a6;font-size:9px}.rate-result strong,.spending-numbers strong{display:block;margin-top:4px;color:#173d82;font-size:13px;font-weight:900}.start-summary{position:sticky;bottom:73px;z-index:5;margin-top:24px;padding:18px;border-radius:22px;background:#102f72;color:#fff;box-shadow:0 14px 32px #0f2f7250}.start-summary small{color:#adc5f2;font-size:9px}.start-summary strong{display:block;margin-top:5px;color:#ffd26d;font-size:22px;font-weight:900}.start-summary div>p{margin-top:3px;color:#c8d8f6;font-size:10px}.start-summary>button{width:100%;margin-top:14px;padding:13px;border-radius:13px;background:linear-gradient(90deg,#ff8843,#ffb44c);font-size:13px;font-weight:900}.start-summary>button:disabled{background:#6b7d9e;color:#d4dceb}.inline-error{margin-top:10px;color:#ffb9b9;font-size:10px}.mission-hero{margin-top:24px;padding:21px;border-radius:25px;background:linear-gradient(135deg,#153a84,#2768d3);color:#fff;box-shadow:0 13px 30px #163f8d2b}.hero-amount{display:flex;align-items:baseline;gap:6px;margin-top:11px}.hero-amount strong{font-size:25px;font-weight:900}.hero-amount span{color:#bcd0f5;font-size:11px}.hero-progress,.spending-progress{height:8px;margin-top:15px;overflow:hidden;border-radius:20px;background:#ffffff2b}.hero-progress i,.spending-progress i{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#ff8245,#ffd45e)}.hero-foot{display:flex;justify-content:space-between;margin-top:9px;color:#bfd1f3;font-size:10px}.hero-foot b{color:#ffd56f}.progress-card{padding:16px}.mission-card-head{display:flex;align-items:center;gap:11px}.status-chip{padding:6px 8px;border-radius:20px;background:#e7edf6;color:#65758e;font-size:9px;font-weight:900}.status-chip.in-progress,.status-chip.success{background:#e4f7f1;color:#149477}.status-chip.failed{background:#ffeded;color:#db5050}.week-heading{display:flex;justify-content:space-between;margin-top:17px;padding-top:14px;border-top:1px dashed #dbe3ef}.week-heading b{font-size:12px}.week-heading span{color:#7d8ba1;font-size:10px}.mission-message{margin-top:7px;color:#62728d;font-size:10px}.spending-numbers{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:13px}.spending-numbers>div{padding:11px;border-radius:12px;background:#f5f8fd}.spending-progress{margin-top:12px;background:#e8edf5}.limit-caption{margin-top:7px;text-align:right;color:#8290a5;font-size:9px}.week-dots{display:flex;gap:7px;margin-top:14px}.week-dots span{display:grid;width:25px;height:25px;place-items:center;border-radius:50%;background:#e9eef6;color:#7d8a9e;font-size:9px;font-weight:900}.week-dots .in-progress{background:#dceaff;color:#2666cb}.week-dots .success{background:#def5ed;color:#118e70}.week-dots .failed{background:#ffe5e5;color:#d84848}.empty-card{margin-bottom:16px}@media (min-width:600px){.mission-page{max-width:430px;margin:0 auto}}
</style>
