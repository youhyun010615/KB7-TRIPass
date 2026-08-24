import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchPreTripReport, fetchPostTripReport } from '@/api/report'
import { fetchTripGoal, fetchBudgetCheck } from '@/api/travel'
import { useTravelStore } from '@/stores/travel'

const COUNTRY_COLORS = ['#1767dc', '#c8173c', '#25ad72', '#7143e8', '#ff922b']
const CATEGORY_COLORS = ['#2675ea', '#7143e8', '#25ad72', '#ef3d91', '#ff922b', '#93a4ba']

const DEMO_PRE_TRIP_REPORT = {
  tripName: '유럽 3개국 여행',
  startDate: '2027-04-04',
  endDate: '2027-04-18',
  targetBudget: 3338000,
  securedFund: 3938008,
  countryBudgets: [
    { countryName: '프랑스', currencyCode: 'EUR', budget: 1228000 },
    { countryName: '스위스', currencyCode: 'CHF', budget: 1170000 },
    { countryName: '포르투갈', currencyCode: 'EUR', budget: 940000 },
  ],
  savingsTrend: [
    { month: '2026-08', savedAmount: 509751, cumulativeAmount: 509751 },
    { month: '2026-09', savedAmount: 459751, cumulativeAmount: 969502 },
    { month: '2026-10', savedAmount: 459751, cumulativeAmount: 1429253 },
    { month: '2026-11', savedAmount: 499751, cumulativeAmount: 1929004 },
    { month: '2026-12', savedAmount: 459751, cumulativeAmount: 2388755 },
    { month: '2027-01', savedAmount: 529751, cumulativeAmount: 2918506 },
    { month: '2027-02', savedAmount: 459751, cumulativeAmount: 3378257 },
    { month: '2027-03', savedAmount: 559751, cumulativeAmount: 3938008 },
  ],
  checklistStages: [
    { stage: 'D30', completed: 16, total: 16, message: 'D-30 준비를 모두 완료했어요.' },
    { stage: 'D7', completed: 14, total: 14, message: 'D-7 준비를 모두 완료했어요.' },
    { stage: 'D1', completed: 10, total: 10, message: '출발 전 최종 준비를 모두 완료했어요.' },
  ],
}

function isDemoPersonaReport(report) {
  return report?.tripName === DEMO_PRE_TRIP_REPORT.tripName
    && report?.startDate === DEMO_PRE_TRIP_REPORT.startDate
    && report?.endDate === DEMO_PRE_TRIP_REPORT.endDate
}

function formatDateRange(startDate, endDate) {
  const fmt = d => (d ? d.replaceAll('-', '.') : '')
  return `${fmt(startDate)} ~ ${fmt(endDate)}`
}

function formatShortDate(dateStr) {
  if (!dateStr) return ''
  const [, m, d] = dateStr.split('-')
  return `${Number(m)}.${Number(d)}`
}

function statusLabel(status) {
  if (status === 'ENDED' || status === 'ARCHIVED') return '여행 완료'
  if (status === 'TRAVELING') return '여행 중'
  return '여행 전'
}

export const useTravelReportStore = defineStore('travelReport', () => {
  const travelStore = useTravelStore()

  const tripBasic = ref(null)
  const preTripReport = ref(null)
  const postTripReport = ref(null)
  const postTripBudgetCheck = ref([])
  const loading = ref(false)
  const errorMessage = ref('')

  function flagsFor(countryNames = []) {
    return countryNames
      .map(name => travelStore.countryFlagMap[name]?.emoji || '🌍')
      .join(' ')
  }

  // 여행 리포트 목록 화면의 상태(완료/예정) 배지용 기본 정보를 조회한다.
  async function loadTripBasic(tripId) {
    tripBasic.value = null
    try {
      tripBasic.value = await fetchTripGoal(tripId)
    } catch (error) {
      console.error('여행 기본 정보 조회 실패:', error)
    }
  }

  const tripSummary = computed(() => {
    const r = preTripReport.value
    if (!r) return null
    const basicCountries = tripBasic.value?.countries || []
    const countryNames = basicCountries.length
      ? basicCountries.map(country => country.countryName || country.name).filter(Boolean)
      : (r.countryNames || [])
    const countryCodes = basicCountries.length
      ? basicCountries.map(country => {
          const name = country.countryName || country.name
          const catalog = travelStore.countries.find(item => Number(item.countryId) === Number(country.countryId))
          return catalog?.code || travelStore.countryFlagMap[name]?.code
        }).filter(Boolean)
      : countryNames.map(name => travelStore.countryFlagMap[name]?.code).filter(Boolean)
    return {
      title: r.tripName,
      flags: flagsFor(countryNames),
      countryCodes: [...new Set(countryCodes)],
      countries: countryNames.join(' · '),
      dateRange: formatDateRange(r.startDate, r.endDate),
      dDay: r.daysUntilTrip,
      status: statusLabel(tripBasic.value?.status),
    }
  })

  // 여행 대비 리포트를 조회한다.
  async function loadPreTripReport(tripId) {
    loading.value = true
    errorMessage.value = ''
    preTripReport.value = null
    try {
      preTripReport.value = await fetchPreTripReport(tripId)
    } catch (error) {
      errorMessage.value = '여행 대비 리포트를 불러오지 못했습니다.'
      console.error('여행 대비 리포트 조회 실패:', error)
    } finally {
      loading.value = false
    }
  }

  const preTripView = computed(() => {
    const r = preTripReport.value
    if (!r) return null
    const demo = isDemoPersonaReport(r) ? DEMO_PRE_TRIP_REPORT : null
    const targetBudget = demo?.targetBudget ?? r.targetBudget
    const securedFund = demo?.securedFund ?? r.securedFund
    const countryBudgets = demo?.countryBudgets ?? r.countryBudgets ?? []
    const savingsTrend = demo?.savingsTrend ?? r.savingsTrend ?? []
    const checklistStages = demo?.checklistStages ?? r.checklistStages ?? []
    return {
      trip: {
        title: r.tripName,
        flags: flagsFor(r.countryNames),
        countryCodes: (r.countryNames || [])
          .map(name => travelStore.countryFlagMap[name]?.code)
          .filter(Boolean),
        countries: (r.countryNames || []).join(' · '),
        dateRange: formatDateRange(r.startDate, r.endDate),
        dDay: r.daysUntilTrip,
      },
      targetBudget,
      securedFund,
      savingsPercent: demo ? Math.round((securedFund / targetBudget) * 100) : r.savingsPercent,
      emergencyFund: demo ? securedFund - targetBudget : r.emergencyFund || 0,
      countrySpend: countryBudgets.map((c, i) => ({
        name: c.countryName,
        flag: travelStore.countryFlagMap[c.countryName]?.emoji || '🌍',
        currencyCode: c.currencyCode,
        budget: c.budget || 0,
        foreignAmount: c.foreignAmount ?? null,
        color: COUNTRY_COLORS[i % COUNTRY_COLORS.length],
      })),
      savingsTrend: savingsTrend.map(m => ({
        month: m.month,
        monthLabel: `${Number(m.month?.split('-')[1])}월`,
        savedAmount: m.savedAmount || 0,
        cumulativeAmount: m.cumulativeAmount || 0,
      })),
      checklistStages: checklistStages.map(s => {
        const total = s.total || 0
        const completed = s.completed || 0
        return {
          stage: s.stage,
          stageLabel: s.stage === 'D30' ? 'D-30' : s.stage === 'D7' ? 'D-7' : 'D-1',
          completed,
          total,
          percent: total > 0 ? Math.round((completed / total) * 100) : 0,
          message: s.message,
        }
      }),
      insights: r.insights || [],
    }
  })

  // 여행 후 리포트를 조회한다.
  async function loadPostTripReport(tripId) {
    loading.value = true
    errorMessage.value = ''
    postTripReport.value = null
    postTripBudgetCheck.value = []
    try {
      const [report, budgetCheck, basic] = await Promise.all([
        fetchPostTripReport(tripId),
        fetchBudgetCheck(tripId).catch(error => {
          console.error('국가별 지출 분석 조회 실패:', error)
          return []
        }),
        fetchTripGoal(tripId).catch(error => {
          console.error('여행 국가 일정 조회 실패:', error)
          return null
        }),
      ])
      postTripReport.value = report
      postTripBudgetCheck.value = budgetCheck || []
      tripBasic.value = basic
    } catch (error) {
      errorMessage.value = '여행 후 리포트를 불러오지 못했습니다.'
      console.error('여행 후 리포트 조회 실패:', error)
    } finally {
      loading.value = false
    }
  }

  const postTripView = computed(() => {
    const r = postTripReport.value
    if (!r) return null
    const countryTopCategories = (r.countryTopCategories || []).length
      ? r.countryTopCategories
      : postTripBudgetCheck.value.map(country => {
          const topCategory = (country.categoryBreakdown || [])
            .filter(category => category.categoryName && Number(category.amount) > 0)
            .sort((a, b) => Number(b.amount) - Number(a.amount))[0]
          return topCategory ? {
            countryName: country.countryName,
            categoryName: topCategory.categoryName,
            amount: topCategory.amount,
            countryTotal: country.travelExpenseTotal,
          } : null
        }).filter(Boolean)

    return {
      trip: {
        title: r.tripName,
        flags: flagsFor(r.countryNames),
        countries: (r.countryNames || []).join(' · '),
        dateRange: formatDateRange(r.startDate, r.endDate),
        days: r.days,
      },
      targetBudget: r.targetBudget,
      spent: r.spent,
      remaining: r.remaining,
      dailyAverage: r.dailyAverage,
      savingsRate: r.savingsRate,
      daily: (r.dailySpending || []).map(d => ({
        date: formatShortDate(d.date),
        rawDate: d.date,
        amount: d.amount,
      })),
      countryPeriods: (tripBasic.value?.countries || []).map((country, i) => {
        const name = country.countryName || country.name
        return {
          name,
          flag: travelStore.countryFlagMap[name]?.emoji || '🌍',
          startDate: country.arrivalDate || country.startDate,
          endDate: country.departureDate || country.endDate,
          color: COUNTRY_COLORS[i % COUNTRY_COLORS.length],
        }
      }).filter(country => country.name && country.startDate && country.endDate)
        .sort((a, b) => a.startDate.localeCompare(b.startDate)),
      categories: (r.categorySpending || []).map((c, i) => ({
        name: c.categoryName,
        amount: c.amount,
        color: CATEGORY_COLORS[i % CATEGORY_COLORS.length],
      })),
      countrySpend: (r.countrySpending || []).map((c, i) => ({
        name: c.countryName,
        flag: travelStore.countryFlagMap[c.countryName]?.emoji || '🌍',
        amount: c.amount,
        budget: c.budget,
        color: COUNTRY_COLORS[i % COUNTRY_COLORS.length],
      })),
      countryTopCategories: countryTopCategories.map((c, i) => ({
        name: c.countryName,
        flag: travelStore.countryFlagMap[c.countryName]?.emoji || '🌍',
        category: c.categoryName,
        amount: c.amount,
        percent: c.countryTotal ? Math.round((c.amount / c.countryTotal) * 100) : 0,
        color: COUNTRY_COLORS[i % COUNTRY_COLORS.length],
      })),
      receiptCount: r.receiptCount,
      nextTripMonths: r.nextTripMonths,
      nextTripMonthly: r.nextTripMonthlySuggestion,
    }
  })

  return {
    loading,
    errorMessage,
    tripBasic,
    tripSummary,
    loadTripBasic,
    preTripReport,
    preTripView,
    loadPreTripReport,
    postTripReport,
    postTripView,
    loadPostTripReport,
  }
})
