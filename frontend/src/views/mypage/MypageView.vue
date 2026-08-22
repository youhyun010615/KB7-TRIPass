<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { logout as logoutApi } from '@/api/auth'
import { resetAccount as resetAccountApi, setOverrideDate, clearOverrideDate } from '@/api/mypage'
import { getAccounts } from '@/api/asset'
import { fetchMyTrips } from '@/api/travel'
import { fetchPreTripReport } from '@/api/report'
import { getReceipts } from '@/api/receipt'
import { useAuthStore } from '@/stores/auth'
import { useCardStore } from '@/stores/cardStore'
import BottomNav from '@/components/common/BottomNav.vue'
import NotificationBell from '@/components/common/NotificationBell.vue'
import TravelManagementMenu from '@/components/mypage/TravelManagementMenu.vue'
import { countryPresentation, flagIconClass, useTravelStore } from '@/stores/travel'
import {
  effectiveDate,
  initDevDate,
  isOverridden,
  realDate,
  today as currentDate,
} from '@/utils/devDate'

const router = useRouter()
const authStore = useAuthStore()
const cardStore = useCardStore()
const travelStore = useTravelStore()

const isLoggingOut = ref(false)
const isResetting = ref(false)
const overrideDate = ref('')
const isSettingDate = ref(false)
const accounts = ref([])
const trips = ref([])
const selectedTripId = ref(null)
const tripMenuOpen = ref(false)
const selectedTripReport = ref(null)
const selectedTripReceiptCount = ref(0)

const selectedTrip = computed(() =>
  trips.value.find((trip) => Number(trip.tripId) === Number(selectedTripId.value)) || trips.value[0] || null,
)
const isTripActive = computed(() => trips.value.some((trip) => tripStatus(trip) === '여행 중' || tripStatus(trip).startsWith('D-')))
const completedTripCount = computed(() => trips.value.filter((trip) => trip.status === 'ENDED').length)
const visitedCountryCount = computed(() => {
  const countries = new Set()
  trips.value
    .filter((trip) => trip.status === 'ENDED' || tripStatus(trip) === '여행 중')
    .forEach((trip) => splitCountryNames(trip.countryNames).forEach((name) => countries.add(name)))
  return countries.size
})

const memberIdentity = computed(() => {
  const provider = authStore.user?.loginProvider ?? 'LOCAL'
  if (provider !== 'LOCAL') {
    return authStore.user?.email ?? authStore.user?.loginId ?? authStore.user?.id ?? 'tripass'
  }
  return authStore.user?.loginId ?? authStore.user?.id ?? 'tripass'
})

const totalAssets = computed(() =>
  accounts.value.reduce((sum, a) => sum + (Number(a.balance) || 0), 0),
)

const accountCount = computed(() => accounts.value.length)
const cardCount = computed(() => cardStore.cards.length)

function formatWon(amount) {
  return amount.toLocaleString('ko-KR') + '원'
}

function splitCountryNames(joined) {
  return (joined || '').split(' · ').map((name) => name.trim()).filter(Boolean)
}

function countryCodeOf(countryName) {
  return countryPresentation[countryName]?.code || ''
}

const homeCountryImages = {
  프랑스: '/images/france.png',
  스위스: '/images/switzerland.webp',
  독일: '/images/germany.png',
  일본: '/images/japan.webp',
  홍콩: '/images/Hong%20Kong.png',
}

function tripCover(trip) {
  const countries = splitCountryNames(trip?.countryNames)
  for (const country of countries) {
    const image = homeCountryImages[country] || countryPresentation[country]?.image
    if (image) return image
  }
  return ''
}

function formatDateRange(startDate, endDate) {
  const format = (date) => (date ? date.replaceAll('-', '.') : '')
  return `${format(startDate)} - ${format(endDate)}`
}

function tripStatus(trip) {
  if (trip?.status === 'ENDED') return '완료'
  if (trip?.status === 'TRAVELING') return '여행 중'
  if (!trip?.startDate) return '준비 중'
  const today = currentDate()
  const start = new Date(`${trip.startDate}T00:00:00`)
  const days = Math.ceil((start - today) / 86400000)
  return days <= 0 ? '여행 중' : `D-${days}`
}

function selectTrip(trip) {
  if (Number(selectedTripId.value) === Number(trip.tripId)) return
  selectedTripId.value = trip.tripId
  tripMenuOpen.value = false
}

async function loadSelectedTripMenuStats(trip) {
  selectedTripReport.value = null
  selectedTripReceiptCount.value = 0
  if (!trip?.tripId || trip.status === 'ENDED') return
  const [reportResult, receiptResult] = await Promise.allSettled([
    fetchPreTripReport(trip.tripId),
    getReceipts(trip.tripId),
  ])
  if (reportResult.status === 'fulfilled') selectedTripReport.value = reportResult.value
  if (receiptResult.status === 'fulfilled') {
    selectedTripReceiptCount.value = receiptResult.value.data?.data?.length ?? 0
  }
}

function startNewTrip() {
  if (isTripActive.value) return
  travelStore.resetGoal()
  router.push({ name: 'TravelRegister' })
}

const travelMenuItems = computed(() => {
  if (!selectedTrip.value) return []
  const id = selectedTrip.value.tripId
  const ended = selectedTrip.value.status === 'ENDED'
  const badge = (value) => (ended ? undefined : value)
  return [
    { label: '여행 리포트', desc: '저축 기록과 여행 후 지출 분석', icon: 'report', path: `/mypage/reports?tripId=${id}`, badge: badge(tripStatus(selectedTrip.value) === '여행 중' ? '열람 가능' : '준비 중') },
    { label: '체크리스트', desc: '여행 전 · 귀국 준비', icon: 'checklist', path: `/mypage/checklists?tripId=${id}`, badge: badge(`${selectedTripReport.value?.checklistCompleted ?? 0}/${selectedTripReport.value?.checklistTotal ?? 0}`) },
    { label: '여행 일정', desc: '등록한 일정 확인', icon: 'schedule', path: `/mypage/travel/${id}/schedules?tripId=${id}`, badge: badge(`${selectedTripReport.value?.scheduleCount ?? 0}개`) },
    { label: '영수증 보관함', desc: 'OCR 영수증과 지출 기록', icon: 'receipt', path: `/mypage/travel/${id}/receipts`, badge: badge(`${selectedTripReceiptCount.value}장`) },
    { label: '완료 미션', desc: '매달 진행했던 미션 기록', icon: 'mission', path: `/mypage/missions?tripId=${id}`, badge: badge('-') },
  ]
})

watch(selectedTrip, (trip) => loadSelectedTripMenuStats(trip), { immediate: true })

onMounted(async () => {
  try {
    const res = await getAccounts()
    accounts.value = res.data?.data ?? []
  } catch {
    accounts.value = []
  }
  await cardStore.loadCards()
  await initDevDate()
  if (isOverridden.value) overrideDate.value = effectiveDate.value
  try {
    trips.value = (await fetchMyTrips()) || []
    selectedTripId.value = trips.value[0]?.tripId ?? null
  } catch (error) {
    console.error('마이페이지 여행 목록 조회 실패:', error)
    trips.value = []
  }
})

async function logout() {
  if (isLoggingOut.value) return

  const confirmed = window.confirm('로그아웃할까요?')
  if (!confirmed) return

  isLoggingOut.value = true

  try {
    // 서버의 Refresh Token을 폐기하고 HttpOnly 쿠키를 삭제한다.
    await logoutApi()
  } catch (error) {
    // 서버 요청이 실패하더라도 현재 브라우저의 로그인 상태는 제거한다.
  } finally {
    // Access Token과 사용자 정보를 프론트에서 제거한다.
    authStore.logout()

    // 뒤로 가기로 보호 화면에 돌아가지 않도록 replace를 사용한다.
    await router.replace('/login')

    isLoggingOut.value = false
  }
}

async function resetAccount() {
  if (isResetting.value) return
  const confirmed = window.confirm('계정의 모든 데이터(여행, 계좌, 카드, 거래내역, 미션 등)가 삭제됩니다.\n\n정말 초기화할까요?')
  if (!confirmed) return
  isResetting.value = true
  try {
    await resetAccountApi()
    window.alert('계정 데이터가 초기화되었습니다.')
    window.location.reload()
  } catch (error) {
    window.alert('초기화에 실패했습니다: ' + (error.response?.data?.message || error.message))
  } finally {
    isResetting.value = false
  }
}

async function applyOverrideDate() {
  if (!overrideDate.value || isSettingDate.value) return
  isSettingDate.value = true
  try {
    await setOverrideDate(overrideDate.value)
    await initDevDate()
    window.alert(`가상 날짜가 ${overrideDate.value}로 설정되었습니다.`)
  } catch (e) {
    window.alert('설정 실패: ' + (e.response?.data?.message || e.message))
  } finally { isSettingDate.value = false }
}

async function removeOverrideDate() {
  isSettingDate.value = true
  try {
    await clearOverrideDate()
    overrideDate.value = ''
    await initDevDate()
    window.alert('가상 날짜가 해제되었습니다.')
  } catch (e) {
    window.alert('해제 실패: ' + (e.response?.data?.message || e.message))
  } finally { isSettingDate.value = false }
}

const myManageItems = computed(() => [
  {
    label: '회원정보',
    sub: '연락처와 비밀번호 관리',
    path: '/mypage/profile',
    icon: 'user',
  },
  {
    label: '알림 설정',
    sub: '알림 종류와 수신 시간 관리',
    path: '/mypage/notification-settings',
    icon: 'notification',
  },
])
</script>

<template>
  <div class="mypage-page" style="background: #eef2f8">

    <!-- 헤더 -->
    <div class="mypage-header-fixed">
      <div class="flex items-start justify-between px-5 pb-3" style="padding-top: 14px">
        <div>
          <p class="mypage-header-eyebrow">
            <img src="@/assets/brand/tripass-text.png" class="header-wordmark" alt="TRIPASS" />
          </p>
          <h1 class="mypage-header-title">MyPage</h1>
        </div>
        <NotificationBell />
      </div>
    </div>
    <div class="mypage-scroll tab-scroll-surface pb-20" data-tab-scroll>
      <div class="px-4 flex flex-col gap-[22px]">

      <!-- 멤버 패스 카드 -->
      <section class="member-card">
        <span class="member-card-glow" aria-hidden="true"></span>
        <header class="member-card-head">
          <span>TRIPASS MEMBER CARD</span>
        </header>

        <div class="member-profile">
          <span class="member-avatar"><img src="@/assets/icons/blue_profile.svg" alt="" /></span>
          <div class="member-profile-copy">
            <strong>{{ authStore.user?.name ?? '고객' }}님</strong>
            <small>{{ memberIdentity }}</small>
          </div>
        </div>

        <div class="member-assets">
          <small>연결 자산</small>
          <strong>{{ formatWon(totalAssets) }}</strong>
        </div>

        <button type="button" class="member-link" @click="router.push('/mypage/assets')">
          <span>계좌 {{ accountCount }} · 카드 {{ cardCount }}</span>
          <b>자산관리</b>
          <i>›</i>
        </button>
      </section>

      <!-- 나의 관리 -->
      <div class="flex flex-col gap-[11px]">
        <h2 class="text-base font-black text-gray-900 px-0.5">나의 관리</h2>
        <div class="bg-white rounded-[20px] overflow-hidden" style="box-shadow: 0 4px 14px rgba(16,25,43,0.07)">
          <button
            v-for="(item, i) in myManageItems"
            :key="item.label"
            class="w-full flex items-center gap-[13px] px-[18px] py-[15px] active:bg-gray-50"
            :class="i < myManageItems.length - 1 ? 'border-b' : ''"
            style="border-color: #F1F3F8"
            @click="router.push(item.path)"
          >
            <div class="w-[38px] h-[38px] rounded-[11px] flex items-center justify-center flex-shrink-0" style="background: #EAF1FF">
              <img v-if="item.icon === 'user'" src="@/assets/icons/blue_profile.svg" width="19" height="19" alt="" />
              <img v-if="item.icon === 'travel'" src="@/assets/icons/blue_airplane.svg" width="19" height="19" alt="" />
              <img v-if="item.icon === 'notification'" src="@/assets/icons/mingcute_notification-fill.svg" width="19" height="19" alt="" />
              <svg v-if="item.icon === 'card'" width="19" height="19" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="6" width="18" height="13" rx="2.5" stroke="#2F6FED" stroke-width="1.8"/>
                <path d="M3 10.5h18" stroke="#2F6FED" stroke-width="1.8"/>
              </svg>
            </div>
            <div class="flex-1 text-left">
              <p class="text-[14px] font-extrabold text-gray-900">{{ item.label }}</p>
              <p class="text-[11.5px] text-gray-400 mt-[3px]">{{ item.sub }}</p>
            </div>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M9 6l6 6-6 6" stroke="#C7CDD8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 여행 관리 -->
      <section class="trip-management">
        <header class="trip-section-head">
          <div>
            <h2>내 여행</h2>
            <span>방문 국가 {{ visitedCountryCount }} · 완료 여행 {{ completedTripCount }}</span>
          </div>
          <button type="button" @click="router.push('/mypage/travel')">전체보기 ›</button>
        </header>

        <div class="trip-selector">
          <button
              v-for="trip in trips"
              :key="trip.tripId"
              type="button"
              class="trip-circle-item"
              :class="{
                selected: Number(selectedTripId) === Number(trip.tripId),
                traveling: tripStatus(trip) === '여행 중',
              }"
              @click="selectTrip(trip)"
          >
            <span class="trip-circle">
              <span
                  class="trip-cover"
                  :style="tripCover(trip) ? { backgroundImage: `linear-gradient(rgba(11,42,107,.1),rgba(11,42,107,.2)),url(${tripCover(trip)})` } : {}"
              >
                <span v-if="!tripCover(trip)" class="trip-cover-fallback" aria-hidden="true"></span>
              </span>
            </span>
            <span class="trip-circle-flags" :aria-label="`${trip.tripName} 여행 국가`">
              <span
                v-for="name in splitCountryNames(trip.countryNames)"
                :key="name"
                :class="flagIconClass(countryCodeOf(name))"
                class="fi-inline"
              ></span>
              <small v-if="!splitCountryNames(trip.countryNames).length">국가 미정</small>
            </span>
          </button>

          <button
            type="button"
            class="trip-circle-item add-trip"
            :class="{ blocked: isTripActive }"
            :disabled="isTripActive"
            :title="isTripActive ? '이미 진행 중이거나 준비 중인 여행이 있습니다.' : ''"
            @click="startNewTrip"
          >
            <span class="trip-circle"><span class="add-trip-plus">+</span></span>
            <b>여행 추가</b>
          </button>
        </div>

        <p v-if="!trips.length" class="trip-empty">등록된 여행이 없어요.</p>

        <article v-else-if="selectedTrip" class="selected-trip-card">
          <button type="button" class="selected-trip-summary" @click="tripMenuOpen = !tripMenuOpen">
            <div class="selected-trip-main">
              <div class="selected-trip-name">
                <strong>{{ selectedTrip.tripName }}</strong>
                <span class="selected-flags">
                  <span v-for="name in splitCountryNames(selectedTrip.countryNames)" :key="name" :class="flagIconClass(countryCodeOf(name))" class="fi-inline"></span>
                </span>
              </div>
              <small>{{ formatDateRange(selectedTrip.startDate, selectedTrip.endDate) }} · {{ selectedTrip.totalDays || '' }}일</small>
            </div>
            <em :class="{ traveling: tripStatus(selectedTrip) === '여행 중' }">{{ tripStatus(selectedTrip) }}</em>
            <svg :class="{ open: tripMenuOpen }" viewBox="0 0 24 24" fill="none"><path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>

          <Transition name="trip-menu">
            <div v-if="tripMenuOpen" class="trip-menu-grid">
              <TravelManagementMenu compact :items="travelMenuItems" @select="router.push($event.path)" />
            </div>
          </Transition>
        </article>
      </section>

      <!-- 개발용: 가상 날짜 설정 -->
      <section class="dev-date-section">
        <h3>가상 날짜 설정 <span class="dev-badge">DEV</span></h3>
        <p class="dev-date-desc">비즈니스 로직에만 적용됩니다 (환율·Codef 등 외부 API 무관)</p>
        <div v-if="isOverridden" class="dev-date-active">
          현재 적용: <strong>{{ effectiveDate }}</strong>
          <small>(실제: {{ realDate }})</small>
        </div>
        <div class="dev-date-controls">
          <input type="date" v-model="overrideDate" class="dev-date-input" />
          <button type="button" class="dev-date-btn apply" :disabled="!overrideDate || isSettingDate" @click="applyOverrideDate">적용</button>
          <button type="button" class="dev-date-btn clear" :disabled="isSettingDate || !isOverridden" @click="removeOverrideDate">해제</button>
        </div>
      </section>

      <button
          type="button"
          class="reset-button"
          :disabled="isResetting"
          @click="resetAccount"
      >
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 12a8 8 0 0 1 14.25-5M20 12a8 8 0 0 1-14.25 5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M20 3v4h-4M4 21v-4h4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
        {{ isResetting ? '초기화 중...' : '계정 데이터 초기화' }}
      </button>

      <button
          type="button"
          class="logout-button"
          :disabled="isLoggingOut"
          @click="logout"
      >
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M10 5H6.5A1.5 1.5 0 0 0 5 6.5v11A1.5 1.5 0 0 0 6.5 19H10m4-4 3-3-3-3m3 3H9" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
        {{ isLoggingOut ? '로그아웃 중...' : '로그아웃' }}
      </button>
      </div>
    </div>

    <BottomNav />
  </div>
</template>

<style scoped>
.mypage-page {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 100%;
  max-width: 390px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  transform: translateX(-50%);
}
.mypage-scroll {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior-y: contain;
  touch-action: pan-y;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}
.mypage-scroll::-webkit-scrollbar { display: none; }
.member-card {
  position: relative;
  overflow: hidden;
  padding: 18px;
  border: 1px solid rgba(105, 151, 232, .22);
  border-radius: 22px;
  background: linear-gradient(145deg, #0b2a6b 0%, #123c94 62%, #174da7 100%);
  box-shadow: 0 12px 26px rgba(11, 42, 107, .22);
  color: #fff;
}
.member-card-glow {
  position: absolute;
  top: -70px;
  right: -50px;
  width: 170px;
  height: 170px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .07);
}
.member-card-head {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  text-align: right;
}
.member-card-head strong {
  font-size: 14px;
  font-weight: 800;
}
.member-card-head span {
  color: #ffd466;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .1em;
}
.member-profile {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  margin-top: 8px;
}
.member-profile-copy { min-width: 0; }
.member-profile-copy strong { display: block; color: #fff; font-size: 14px; font-weight: 800; line-height: 1.25; }
.member-avatar {
  display: grid;
  width: 38px;
  height: 38px;
  flex: none;
  place-items: center;
  border-radius: 13px;
  background: #ffd466;
  color: #0b2a6b;
  font-size: 14px;
  font-weight: 800;
  box-shadow: 0 6px 13px rgba(2, 18, 50, .2);
}
.member-avatar img { width: 21px; height: 21px; }
.member-profile small { display: block; overflow: hidden; margin-top: 4px; color: rgba(255, 255, 255, .62); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.member-assets { position: relative; margin-top: 19px; }
.member-assets small { display: block; color: #ffd466; font-size: 10px; font-weight: 700; }
.member-assets strong {
  display: block;
  margin-top: 5px;
  color: #fff;
  font-size: 27px;
  font-weight: 800;
  letter-spacing: -.03em;
}
.member-link {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  margin-top: 17px;
  padding: 12px 13px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, .12);
  background: rgba(255, 255, 255, .09);
  color: rgba(255, 255, 255, .7);
  text-align: left;
}
.member-link span { font-size: 11px; font-weight: 600; }
.member-link b { color: #ffd466; font-size: 11px; font-weight: 800; }
.member-link i { color: #ffd466; font-size: 18px; font-style: normal; line-height: 1; }
.mypage-header-fixed {
  position: relative;
  z-index: 60;
  width: 100%;
  flex: none;
  padding-top: env(safe-area-inset-top, 0px);
  background: #eef2f8;
}
.mypage-header-eyebrow {
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: 'Space Mono', monospace;
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.15em;
  color: #0b2a6b;
  margin-bottom: 4px;
}
.mypage-header-title {
  margin-top: 2px;
  font-size: 17px;
  font-weight: 400;
  color: #29466f;
}
.header-wordmark {
  display: block;
  width: 88px;
  height: auto;
  object-fit: contain;
}
.header-plane {
  width: 12px;
  height: 12px;
  animation: header-plane-fly 2.6s ease-in-out infinite;
}
@keyframes header-plane-fly {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
    filter: brightness(1) drop-shadow(0 0 0 rgba(47, 112, 242, 0));
  }
  25% {
    transform: translateY(-1.5px) rotate(-8deg);
  }
  50% {
    transform: translateY(0) rotate(0deg);
    filter: brightness(1.6) drop-shadow(0 0 3px rgba(47, 112, 242, 0.55));
  }
  75% {
    transform: translateY(1.5px) rotate(6deg);
  }
}
.notification-time-card {
  padding: 15px 16px;
  border: 1px solid #e8edf5;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(16,25,43,.055);
}
.notification-time-head { display:flex;align-items:flex-start;justify-content:space-between;gap:12px; }
.notification-time-head b,.notification-time-head small { display:block; }
.notification-time-head b { color:#111827;font-size:12px;font-weight:800; }
.notification-time-head small { margin-top:3px;color:#9aa5b6;font-size:9px; }
.notification-time-head>span { padding:4px 7px;border-radius:99px;background:#edf3ff;color:#3970d0;font-family:'Space Mono',monospace;font-size:7px;font-weight:800;letter-spacing:.08em; }
.notification-time-fields { display:grid;grid-template-columns:minmax(0,1fr) 12px minmax(0,1fr);align-items:end;gap:7px;margin-top:12px; }
.notification-time-fields label { padding:8px 10px;border:1px solid #e5eaf2;border-radius:12px;background:#f7f9fc; }
.notification-time-fields label>span { display:block;margin-bottom:3px;color:#8b98ab;font-size:8px;font-weight:700; }
.notification-time-fields input { width:100%;border:0;outline:0;background:transparent;color:#17387f;font-size:12px;font-weight:800; }
.notification-time-fields i { padding-bottom:11px;color:#a5afbd;font-style:normal;text-align:center; }
.reset-button { display:flex;align-items:center;justify-content:center;gap:7px;width:100%;min-height:48px;border:1px solid #dde3ed;border-radius:16px;background:#fff;color:#6b7a90;font-size:12px;font-weight:800;box-shadow:0 5px 14px rgba(16,25,43,.04); }
.reset-button:active { background:#f5f7fb; }
.reset-button:disabled { opacity:.55; }
.reset-button svg { width:17px;height:17px; }
.logout-button { display:flex;align-items:center;justify-content:center;gap:7px;width:100%;min-height:48px;margin:0 0 16px;border:1px solid #f0d9dd;border-radius:16px;background:#fff;color:#c94e5b;font-size:12px;font-weight:800;box-shadow:0 5px 14px rgba(116,31,45,.045); }
.logout-button:active { background:#fff5f6; }
.logout-button:disabled { opacity:.55; }
.logout-button svg { width:17px;height:17px; }
.trip-management { display:flex;flex-direction:column;gap:12px;margin-top:4px; }
.trip-section-head { display:flex;align-items:center;justify-content:space-between;gap:12px; }
.trip-section-head>div { display:flex;min-width:0;align-items:baseline;gap:8px; }
.trip-section-head h2 { margin:0;color:#111b30;font-size:15px;font-weight:800;letter-spacing:-.03em; }
.trip-section-head>div>span { overflow:hidden;color:#98a4b6;font-size:9px;font-weight:700;text-overflow:ellipsis;white-space:nowrap; }
.trip-section-head button { flex:none;border:0;background:transparent;color:#2f70f2;font-size:10px;font-weight:800; }
.trip-selector { display:flex;gap:13px;overflow-x:auto;padding:5px 4px 10px;scrollbar-width:none;scroll-snap-type:x proximity; }
.trip-selector::-webkit-scrollbar { display:none; }
.trip-circle-item { display:flex;width:66px;min-width:66px;flex-direction:column;align-items:center;gap:7px;border:0;background:transparent;color:#9aa5b5;scroll-snap-align:start; }
.trip-circle-item b { display:block;overflow:hidden;width:100%;font-size:9.5px;font-weight:800;text-align:center;text-overflow:ellipsis;white-space:nowrap; }
.trip-circle-flags { display:flex;min-height:13px;align-items:center;justify-content:center;gap:3px; }
.trip-circle-flags .fi-inline { display:block;width:18px;height:12px;border-radius:2px;background-size:cover;box-shadow:0 1px 3px rgba(15,34,68,.16); }
.trip-circle-flags small { color:#9aa5b5;font-size:8px;font-weight:700; }
.trip-circle { position:relative;isolation:isolate;display:grid;width:58px;height:58px;place-items:center;border:3px solid #fff;border-radius:50%;background:#dfe6f0;box-shadow:0 0 0 2px #dfe6f0;transition:transform .22s ease,box-shadow .22s ease; }
.trip-circle-item.selected { color:#111b30; }
.trip-circle-item.selected .trip-circle { box-shadow:0 0 0 3px #2f70f2;transform:scale(1.03); }
.trip-circle-item.selected.traveling .trip-circle { border-color:#fff;box-shadow:0 5px 15px rgba(18,167,102,.14);transform:scale(1.03); }
.trip-circle-item.selected.traveling .trip-circle::before { position:absolute;z-index:-1;inset:-3px;border-radius:50%;background:conic-gradient(from 0deg,#0da668 0 58%,#bff5d9 70%,#24ca82 80%,#0da668 100%);content:'';animation:traveling-circle-spin 2.1s linear infinite; }
.trip-cover { position:relative;display:grid;width:100%;height:100%;place-items:center;overflow:hidden;border-radius:50%;background:#dce8f7 center/cover no-repeat; }
.trip-cover::after { position:absolute;inset:0;background:linear-gradient(180deg,rgba(15,44,99,.03),rgba(15,44,99,.18));content:''; }
.trip-cover-fallback { position:absolute;inset:0;background:linear-gradient(145deg,#b9d4f4 0%,#dfeafa 42%,#8bb2df 100%); }
.circle-flags { position:relative;z-index:1;display:flex;max-width:45px;flex-wrap:wrap;align-items:center;justify-content:center;gap:2px; }
.circle-flags .fi { width:18px;height:12px;border-radius:2px;box-shadow:0 1px 3px rgba(15,34,68,.18); }
.add-trip .trip-circle { border:2px dashed #cad5e7;background:#fff;box-shadow:none;color:#2f70f2;font-size:25px;font-weight:400; }
.add-trip-plus { display:grid;width:100%;height:100%;place-items:center;font-family:Arial,sans-serif;font-size:25px;font-style:normal;font-weight:400;line-height:1;transform:none; }
.add-trip:active .trip-circle { transform:scale(.96); }
.add-trip.blocked { cursor:not-allowed;color:#aab3c1;opacity:.82; }
.add-trip.blocked .trip-circle { border-style:solid;border-color:#d8dee8;background:#e8ebf0;color:#aeb6c2;box-shadow:inset 0 0 0 1px rgba(137,148,164,.08); }
.add-trip.blocked .add-trip-plus { color:#929dab; }
.trip-empty { padding:22px 16px;border:1px dashed #cfdbed;border-radius:19px;background:#fff;color:#95a2b5;font-size:10px;text-align:center; }
.selected-trip-card { overflow:hidden;border:1px solid #e8edf5;border-radius:20px;background:#fff;box-shadow:0 8px 23px rgba(26,51,93,.075); }
.selected-trip-summary { display:grid;width:100%;grid-template-columns:minmax(0,1fr) auto 18px;align-items:center;gap:9px;padding:16px;border:0;background:#fff;text-align:left; }
.selected-trip-main { min-width:0; }
.selected-trip-name { display:flex;align-items:center;gap:7px; }
.selected-trip-name strong { overflow:hidden;color:#111b30;font-size:14px;font-weight:800;text-overflow:ellipsis;white-space:nowrap; }
.selected-flags { display:flex;align-items:center;gap:2px; }
.selected-flags .fi { width:14px;height:10px;border-radius:2px;box-shadow:0 1px 2px rgba(15,34,68,.15); }
.selected-trip-main small { display:block;margin-top:5px;color:#95a2b5;font-family:'Space Mono',monospace;font-size:8.5px;font-weight:700; }
.selected-trip-summary>em { padding:6px 9px;border-radius:99px;background:#edf3ff;color:#2866d4;font-size:9px;font-style:normal;font-weight:800;white-space:nowrap; }
.selected-trip-summary>em.traveling { position:relative;background:#e8f8ef;color:#138454;animation:traveling-pill-glow 1.8s ease-in-out infinite; }
.selected-trip-summary>em.traveling::before { display:inline-block;width:5px;height:5px;margin-right:5px;border-radius:50%;background:#1dbf73;box-shadow:0 0 0 0 rgba(29,191,115,.38);content:'';vertical-align:1px;animation:traveling-dot-pulse 1.4s ease-out infinite; }
.selected-trip-summary>svg { width:18px;height:18px;color:#9aa6b8;transition:transform .22s ease; }
.selected-trip-summary>svg.open { transform:rotate(180deg); }
.trip-menu-grid { display:block;padding:13px;border-top:1px solid #edf1f7;background:#f6f8fc; }
.trip-menu-item { position:relative;display:flex;min-width:0;min-height:112px;flex-direction:column;align-items:flex-start;padding:13px;border:0;border-radius:16px;background:#f6f8fc;color:#111b30;text-align:left;opacity:0;transform:translateY(16px) scale(.94);animation:trip-menu-card-reveal .46s cubic-bezier(.2,.82,.28,1.18) forwards;will-change:transform,opacity; }
.trip-menu-item:nth-child(1) { animation-delay:.05s; }
.trip-menu-item:nth-child(2) { animation-delay:.11s; }
.trip-menu-item:nth-child(3) { animation-delay:.17s; }
.trip-menu-item:nth-child(4) { animation-delay:.23s; }
.trip-menu-item:nth-child(5) { animation-delay:.29s; }
.trip-menu-item:active { background:#edf3ff;transform:scale(.985); }
.trip-menu-item.wide { grid-column:1/-1;min-height:auto;display:grid;grid-template-columns:36px minmax(0,1fr);align-items:center;gap:10px; }
.trip-menu-icon { display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#eaf1ff;color:#2f70f2; }
.trip-menu-icon svg { width:18px;height:18px; }
.trip-menu-icon img { width:19px;height:19px;object-fit:contain; }
.trip-menu-copy { min-width:0; }
.trip-menu-item:not(.wide) .trip-menu-copy { margin-top:13px; }
.trip-menu-item b { display:block;font-size:11px;font-weight:800;line-height:1.35; }
.trip-menu-item small { display:block;margin-top:4px;color:#98a4b6;font-size:8.5px;line-height:1.45; }
.trip-menu-enter-active,.trip-menu-leave-active { overflow:hidden;transition:max-height .3s ease,opacity .2s ease; }
.trip-menu-enter-from,.trip-menu-leave-to { max-height:0;opacity:0; }
.trip-menu-enter-to,.trip-menu-leave-from { max-height:420px;opacity:1; }
@keyframes traveling-pill-glow {
  0%,100% { box-shadow:0 0 0 0 rgba(29,191,115,0); }
  50% { box-shadow:0 0 0 5px rgba(29,191,115,.09); }
}
@keyframes traveling-dot-pulse {
  0% { box-shadow:0 0 0 0 rgba(29,191,115,.42); }
  70%,100% { box-shadow:0 0 0 6px rgba(29,191,115,0); }
}
@keyframes traveling-circle-spin {
  to { transform:rotate(360deg); }
}
@keyframes trip-menu-card-reveal {
  0% { opacity:0;transform:translateY(16px) scale(.94); }
  68% { opacity:1;transform:translateY(-2px) scale(1.015); }
  100% { opacity:1;transform:translateY(0) scale(1); }
}
@media (prefers-reduced-motion:reduce) {
  .trip-menu-item { opacity:1;transform:none;animation:none; }
  .trip-circle-item.selected.traveling .trip-circle::before { animation:none; }
}
.dev-date-section {
  background: #fff;
  border: 2px dashed #f59e0b;
  border-radius: 16px;
  padding: 16px;
}
.dev-date-section h3 {
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 6px;
}
.dev-badge {
  font-size: 10px;
  font-weight: 700;
  background: #f59e0b;
  color: #fff;
  padding: 1px 6px;
  border-radius: 6px;
}
.dev-date-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}
.dev-date-active {
  margin-top: 10px;
  padding: 8px 12px;
  background: #fef3c7;
  border-radius: 10px;
  font-size: 13px;
  color: #92400e;
}
.dev-date-active strong { font-weight: 800; }
.dev-date-active small { display: block; font-size: 11px; color: #b45309; margin-top: 2px; }
.dev-date-controls {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  align-items: center;
}
.dev-date-input {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 13px;
  color: #1e293b;
  background: #f8fafc;
}
.dev-date-btn {
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  border: none;
  cursor: pointer;
}
.dev-date-btn.apply { background: #2563eb; color: #fff; }
.dev-date-btn.apply:disabled { background: #94a3b8; }
.dev-date-btn.clear { background: #fee2e2; color: #dc2626; }
.dev-date-btn.clear:disabled { background: #f1f5f9; color: #cbd5e1; }
</style>
