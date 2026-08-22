<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { logout as logoutApi } from '@/api/auth'
import { resetAccount as resetAccountApi, setOverrideDate, clearOverrideDate } from '@/api/mypage'
import { getAccounts } from '@/api/asset'
import { fetchMyTrips } from '@/api/travel'
import { useAuthStore } from '@/stores/auth'
import { useCardStore } from '@/stores/cardStore'
import BottomNav from '@/components/common/BottomNav.vue'
import NotificationBell from '@/components/common/NotificationBell.vue'
import { countryPresentation, useTravelStore } from '@/stores/travel'
import {
  effectiveDate,
  initDevDate,
  isOverridden,
  realDate,
} from '@/utils/devDate'
import { daysUntilTrip, tripPhase } from '@/utils/tripLifecycle'

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

const isTripActive = computed(() => trips.value.some((trip) => tripStatus(trip) === '여행 중' || tripStatus(trip).startsWith('D-')))
const completedTripCount = computed(() => trips.value.filter((trip) => tripPhase(trip) === 'ENDED').length)
const visitedCountryCount = computed(() => {
  const countries = new Set()
  trips.value
    .filter((trip) => ['ENDED', 'TRAVELING'].includes(tripPhase(trip)))
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

function formatTripName(name) {
  if (!name) return '여행'
  // 10글자로 제한 (넘칠 경우 CSS 2줄 line-clamp가 말줄임 처리)
  return name.length > 10 ? name.slice(0, 10) : name
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

function tripStatus(trip) {
  const phase = tripPhase(trip)
  if (phase === 'ENDED') return '완료'
  if (phase === 'TRAVELING') return '여행 중'
  const days = daysUntilTrip(trip)
  return days === null ? '준비 중' : `D-${Math.max(0, days)}`
}

function goToTripDetail(tripId) {
  router.push(`/mypage/travel/${tripId}`)
}

function startNewTrip() {
  if (isTripActive.value) return
  travelStore.resetGoal()
  router.push({ name: 'TravelRegister' })
}

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
    await logoutApi()
  } catch (error) {
  } finally {
    authStore.logout()
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
    trips.value = (await fetchMyTrips()) || []
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
    trips.value = (await fetchMyTrips()) || []
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
                traveling: tripStatus(trip) === '여행 중',
              }"
              @click="goToTripDetail(trip.tripId)"
          >
            <span class="trip-circle">
              <span
                  class="trip-cover"
                  :style="tripCover(trip) ? { backgroundImage: `linear-gradient(rgba(11,42,107,.1),rgba(11,42,107,.2)),url(${tripCover(trip)})` } : {}"
              >
                <span v-if="!tripCover(trip)" class="trip-cover-fallback" aria-hidden="true"></span>
              </span>
            </span>
            <!-- 10글자 초과 시 말줄임 처리된 여행명 -->
            <span class="trip-circle-title" :title="trip.tripName">
              {{ formatTripName(trip.tripName) }}
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
            <span class="trip-circle-title">여행 추가</span>
          </button>
        </div>

        <p v-if="!trips.length" class="trip-empty">등록된 여행이 없어요.</p>
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
.trip-circle-item { display:flex;width:66px;padding-bottom: 4px;min-width:66px;flex-direction:column;align-items:center;gap:6px;border:0;background:transparent;color:#9aa5b5;scroll-snap-align:start;cursor:pointer; }
.trip-circle-title {
  display: -webkit-box;
  -webkit-line-clamp: 2;          /* 최대 2줄까지만 표시 */
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: keep-all;           /* 단어 단위 자연스러운 줄바꿈 */
  overflow-wrap: break-word;
  
  width: 100%;
  min-height: 30px;               /* 2줄 기준 최소 높이 */
  max-height: 36px;               /* 👈 2줄 글자가 잘리지 않도록 넉넉하게 확장 (기존 28px -> 36px) */
  padding: 1px 2px 2px;           /* 👈 글자 아래쪽(디센더) 잘림 방지용 내부 여백 */
  box-sizing: border-box;
  
  color: #475467;
  font-size: 10.5px;
  font-weight: 800;
  text-align: center;
  line-height: 1.35;              /* 👈 줄 간격을 1.35로 살짝 넓혀 가독성 향상 */
}
.trip-circle { position:relative;isolation:isolate;display:grid;width:58px;height:58px;place-items:center;border:3px solid #fff;border-radius:50%;background:#dfe6f0;box-shadow:0 0 0 2px #dfe6f0;transition:transform .22s ease,box-shadow .22s ease; }
.trip-circle-item:active .trip-circle { transform:scale(0.96); }
.trip-circle-item.traveling .trip-circle { border-color:#fff;box-shadow:0 5px 15px rgba(18,167,102,.14); }
.trip-circle-item.traveling .trip-circle::before { position:absolute;z-index:-1;inset:-3px;border-radius:50%;background:conic-gradient(from 0deg,#0da668 0 58%,#bff5d9 70%,#24ca82 80%,#0da668 100%);content:'';animation:traveling-circle-spin 2.1s linear infinite; }
.trip-circle-item.traveling .trip-circle-title { color: #0da668; font-weight: 900; }
.trip-cover { position:relative;display:grid;width:100%;height:100%;place-items:center;overflow:hidden;border-radius:50%;background:#dce8f7 center/cover no-repeat; }
.trip-cover::after { position:absolute;inset:0;background:linear-gradient(180deg,rgba(15,44,99,.03),rgba(15,44,99,.18));content:''; }
.trip-cover-fallback { position:absolute;inset:0;background:linear-gradient(145deg,#b9d4f4 0%,#dfeafa 42%,#8bb2df 100%); }
.add-trip .trip-circle { border:2px dashed #cad5e7;background:#fff;box-shadow:none;color:#2f70f2;font-size:25px;font-weight:400; }
.add-trip-plus { display:grid;width:100%;height:100%;place-items:center;font-family:Arial,sans-serif;font-size:25px;font-style:normal;font-weight:400;line-height:1;transform:none; }
.add-trip:active .trip-circle { transform:scale(.96); }
.add-trip.blocked { cursor:not-allowed;color:#aab3c1;opacity:.82; }
.add-trip.blocked .trip-circle { border-style:solid;border-color:#d8dee8;background:#e8ebf0;color:#aeb6c2;box-shadow:inset 0 0 0 1px rgba(137,148,164,.08); }
.add-trip.blocked .add-trip-plus { color:#929dab; }
.trip-empty { padding:22px 16px;border:1px dashed #cfdbed;border-radius:19px;background:#fff;color:#95a2b5;font-size:10px;text-align:center; }
@keyframes traveling-circle-spin {
  to { transform:rotate(360deg); }
}
@media (prefers-reduced-motion:reduce) {
  .trip-circle-item.traveling .trip-circle::before { animation:none; }
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
