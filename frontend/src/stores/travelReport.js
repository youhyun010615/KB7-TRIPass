import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchPreTripReport, fetchPostTripReport } from '@/api/report'
import { fetchTripGoal } from '@/api/travel'
import { useTravelStore } from '@/stores/travel'

const COUNTRY_COLORS = ['#1767dc', '#c8173c', '#25ad72', '#7143e8', '#ff922b']
const CATEGORY_COLORS = ['#2675ea', '#7143e8', '#25ad72', '#ef3d91', '#ff922b', '#93a4ba']

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
  if (status === 'ENDED') return '완료'
  if (status === 'TRAVELING') return '여행 중'
  return '예정'
}

export const useTravelReportStore = defineStore('travelReport', () => {
  const travelStore = useTravelStore()

  const tripBasic = ref(null)
  const preTripReport = ref(null)
  const postTripReport = ref(null)
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
    return {
      title: r.tripName,
      flags: flagsFor(r.countryNames),
      countries: (r.countryNames || []).join(' · '),
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
    return {
      trip: {
        title: r.tripName,
        flags: flagsFor(r.countryNames),
        countries: (r.countryNames || []).join(' · '),
        dateRange: formatDateRange(r.startDate, r.endDate),
        dDay: r.daysUntilTrip,
      },
      targetBudget: r.targetBudget,
      securedFund: r.securedFund,
      savingsPercent: r.savingsPercent,
      savingHistory: (r.savingHistory || []).map(h => ({
        date: formatShortDate(h.date),
        label: h.label,
        amount: h.amount,
      })),
      countrySpend: (r.countryBudgets || []).map((c, i) => ({
        name: c.countryName,
        flag: travelStore.countryFlagMap[c.countryName]?.emoji || '🌍',
        budget: c.budget,
        color: COUNTRY_COLORS[i % COUNTRY_COLORS.length],
      })),
      checklistCompleted: r.checklistCompleted,
      checklistTotal: r.checklistTotal,
      schedules: r.scheduleCount,
      paidSchedules: r.prepaidScheduleCount,
      pendingSchedules: r.onsiteScheduleCount,
    }
  })

  // 여행 후 리포트를 조회한다.
  async function loadPostTripReport(tripId) {
    loading.value = true
    errorMessage.value = ''
    postTripReport.value = null
    try {
      postTripReport.value = await fetchPostTripReport(tripId)
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
        amount: d.amount,
      })),
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
