import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

const PROFILE_KEY = 'tripass-mypage-financial-profile'
const NOTICE_KEY = 'tripass-notifications'
const SETTING_KEY = 'tripass-notification-settings'

const read = (key, fallback) => {
  try { return JSON.parse(localStorage.getItem(key) || 'null') ?? fallback }
  catch { return fallback }
}

export const useMypageStore = defineStore('mypage', () => {
  const incomes = ref(read(PROFILE_KEY, [{ id:1, name:'큐브리빛 월급', payDay:25, amount:2_600_000, memo:'보너스나 성과급 제외', account:'하나은행 급여계좌 · ****4821' }]))
  const notifications = ref(read(NOTICE_KEY, [
    { id:1, type:'schedule', title:'루브르 박물관 일정이 곧 시작돼요', message:'오늘 10:30 일정이 1시간 남았어요.', time:'방금 전', path:'/schedule/1', read:false },
    { id:2, type:'finance', title:'내일 월세 납부 예정이에요', message:'KB국민은행 여행통장에서 350,000원 출금 예정', time:'10분 전', path:'/financial-schedule', read:false },
    { id:3, type:'saving', title:'이번 달 여행 저축일이에요', message:'설정한 월 저축액 500,000원을 확인해 주세요.', time:'오늘 09:00', path:'/savings/plan', read:false },
    { id:4, type:'exchange', title:'EUR 목표 환율에 도달했어요', message:'1 EUR가 설정한 1,480원 이하로 내려왔어요.', time:'어제', path:'/exchange/alerts', read:true },
    { id:5, type:'checklist', title:'D-7 준비 항목을 확인해 주세요', message:'이월된 체크리스트 2개가 남아 있어요.', time:'2일 전', path:'/mypage/checklists?tripId=2', read:true },
  ]))
  const settings = ref(read(SETTING_KEY, { all:true, schedule:true, finance:true, saving:true, exchange:true, checklist:true, report:true }))
  const totalIncome = computed(() => incomes.value.reduce((sum, item) => sum + Number(item.amount || 0), 0))
  const unreadCount = computed(() => notifications.value.filter(item => !item.read).length)

  function saveIncomes(items) { incomes.value = items.map(item => ({ ...item, amount:Number(item.amount || 0) })) }
  function markRead(id) { const item = notifications.value.find(item => item.id === id); if (item) item.read = true }
  function markAllRead() { notifications.value.forEach(item => { item.read = true }) }
  function toggleSetting(key) { settings.value[key] = !settings.value[key] }

  watch(incomes, value => localStorage.setItem(PROFILE_KEY, JSON.stringify(value)), { deep:true })
  watch(notifications, value => localStorage.setItem(NOTICE_KEY, JSON.stringify(value)), { deep:true })
  watch(settings, value => localStorage.setItem(SETTING_KEY, JSON.stringify(value)), { deep:true })
  return { incomes, notifications, settings, totalIncome, unreadCount, saveIncomes, markRead, markAllRead, toggleSetting }
})
