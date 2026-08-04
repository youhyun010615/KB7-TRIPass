import { computed, reactive, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useAssetStore } from '@/stores/asset'

const STORAGE_KEY = 'tripass-financial-schedule'

function loadState() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') }
  catch { return null }
}

function lastDay(year, month) {
  return new Date(year, month, 0).getDate()
}

function dateKey(year, month, day) {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

export const useFinancialScheduleStore = defineStore('financialSchedule', () => {
  const saved = loadState()
  const asset = useAssetStore()
  const salaries = reactive(saved?.salaries ?? [
    { id: 1, name: '급여일', payDay: 25, amount: 2_600_000, account: 'KB국민은행 여행통장 ****4821', memo: '월 급여' },
  ])
  const filter = ref(saved?.filter ?? 'all')
  const viewYear = ref(saved?.viewYear ?? 2026)
  const viewMonth = ref(saved?.viewMonth ?? 7)
  const selectedDate = ref(saved?.selectedDate ?? '2026-07-28')
  const referenceDate = ref(saved?.referenceDate ?? '2026-07-24')

  const events = computed(() => {
    const maxDay = lastDay(viewYear.value, viewMonth.value)
    const income = salaries
      .filter((item) => Number(item.amount) > 0 && Number(item.payDay) > 0)
      .map((item) => {
        const day = Math.min(maxDay, Number(item.payDay))
        return { id:`salary-${item.id}-${viewYear.value}-${viewMonth.value}`, sourceId:item.id, source:'salary', type:'income', title:item.name || '급여일', amount:Number(item.amount), day, date:dateKey(viewYear.value, viewMonth.value, day), account:item.account, memo:item.memo, icon:'▣' }
      })
    const expense = asset.fixedExpenses
      .filter((item) => item.active && Number(item.amount) > 0)
      .map((item) => {
        const day = Math.min(maxDay, Number(item.day))
        return { id:`fixed-${item.id}-${viewYear.value}-${viewMonth.value}`, sourceId:item.id, source:'fixed', type:'expense', title:item.name, amount:Number(item.amount), day, date:dateKey(viewYear.value, viewMonth.value, day), account:asset.getAccount(item.accountId)?.name || '연결 계좌', memo:item.memo, icon:item.icon || '▦' }
      })
    return [...income, ...expense].sort((a, b) => a.day - b.day || (a.type === 'income' ? -1 : 1))
  })

  const filteredEvents = computed(() => events.value.filter((item) => filter.value === 'all' || item.type === filter.value))
  const upcomingEvents = computed(() => filteredEvents.value.filter((item) => item.date >= referenceDate.value))
  const selectedEvents = computed(() => filteredEvents.value.filter((item) => item.date === selectedDate.value))
  const counts = computed(() => ({
    all: events.value.length,
    income: events.value.filter((item) => item.type === 'income').length,
    expense: events.value.filter((item) => item.type === 'expense').length,
  }))
  const notifications = computed(() => events.value.map((item) => {
    const eventDate = new Date(`${item.date}T00:00:00`)
    eventDate.setDate(eventDate.getDate() - 1)
    return { id:`alert-${item.id}`, eventId:item.id, date:eventDate.toISOString().slice(0, 10), title:`내일 ${item.title} 일정이 있어요`, message:`${item.amount.toLocaleString('ko-KR')}원 ${item.type === 'income' ? '입금' : '출금'} 예정`, enabled:true }
  }))

  function moveMonth(offset) {
    const date = new Date(viewYear.value, viewMonth.value - 1 + offset, 1)
    viewYear.value = date.getFullYear()
    viewMonth.value = date.getMonth() + 1
    selectedDate.value = dateKey(viewYear.value, viewMonth.value, 1)
  }

  function selectDay(day) {
    selectedDate.value = dateKey(viewYear.value, viewMonth.value, day)
  }

  function replaceSalaries(items) {
    salaries.splice(0, salaries.length, ...items.map((item) => ({
      id:item.id,
      name:item.name,
      payDay:Number(item.payDay),
      amount:Number(String(item.amount).replace(/[^0-9]/g, '')) || 0,
      account:item.account,
      memo:item.memo || '',
    })))
  }

  watch(() => ({ salaries:salaries.map((item) => ({...item})), filter:filter.value, viewYear:viewYear.value, viewMonth:viewMonth.value, selectedDate:selectedDate.value, referenceDate:referenceDate.value }), (value) => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)), { deep:true })

  return { salaries, filter, viewYear, viewMonth, selectedDate, referenceDate, events, filteredEvents, upcomingEvents, selectedEvents, counts, notifications, moveMonth, selectDay, replaceSalaries }
})
