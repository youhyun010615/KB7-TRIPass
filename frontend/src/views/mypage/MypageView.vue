<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { logout as logoutApi } from '@/api/auth'
import { getAccounts } from '@/api/asset'
import { useAuthStore } from '@/stores/auth'
import { useCardStore } from '@/stores/cardStore'
import { useMypageStore } from '@/stores/mypage'
import BottomNav from '@/components/common/BottomNav.vue'
import NotificationBell from '@/components/common/NotificationBell.vue'

const router = useRouter()
const authStore = useAuthStore()
const cardStore = useCardStore()
const mypageStore = useMypageStore()

// 앱 프레임(App.vue)의 overflow:hidden 때문에 sticky 대신 fixed로 헤더를 고정한다.
const mypageHeaderEl = ref(null)
const mypageHeaderHeight = ref(0)
let mypageHeaderResizeObserver = null

function syncMypageHeaderHeight() {
  if (mypageHeaderEl.value) {
    mypageHeaderHeight.value = mypageHeaderEl.value.offsetHeight
  }
}

watch(mypageHeaderEl, (el) => {
  mypageHeaderResizeObserver?.disconnect()
  mypageHeaderResizeObserver = null
  if (!el) return

  syncMypageHeaderHeight()
  if (window.ResizeObserver) {
    mypageHeaderResizeObserver = new ResizeObserver(syncMypageHeaderHeight)
    mypageHeaderResizeObserver.observe(el)
  }
})

onBeforeUnmount(() => {
  mypageHeaderResizeObserver?.disconnect()
})

const isLoggingOut = ref(false)
const accounts = ref([])
const notificationStartTime = ref(localStorage.getItem('tripass-notification-start') || '09:00')
const notificationEndTime = ref(localStorage.getItem('tripass-notification-end') || '22:00')

watch([notificationStartTime, notificationEndTime], ([start, end]) => {
  localStorage.setItem('tripass-notification-start', start)
  localStorage.setItem('tripass-notification-end', end)
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

onMounted(async () => {
  try {
    const res = await getAccounts()
    accounts.value = res.data?.data ?? []
  } catch {
    accounts.value = []
  }
  await cardStore.loadCards()
  await mypageStore.fetchSettings()
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

const myManageItems = computed(() => [
  {
    label: '회원정보',
    sub: '연락처와 비밀번호 관리',
    path: '/mypage/profile',
    icon: 'user',
  },
  {
    label: '여행 관리',
    sub: '등록한 여행과 관련 기록',
    path: '/mypage/travel',
    icon: 'travel',
  },
])

const notificationRows = [
  { key: 'travelScheduleEnabled', label: '여행 일정', sub: '출국 D-day와 예약 일정' },
  { key: 'exchangeRateEnabled', label: '환율 및 환전', sub: '목표 환율 도달 시' },
  { key: 'checklistEnabled', label: '체크리스트', sub: '준비물·서류 리마인드' },
  { key: 'travelReportEnabled', label: '여행 리포트', sub: '월간 지출 요약' },
]
</script>

<template>
  <div class="min-h-screen pb-20 flex flex-col" style="background: #eef2f8">

    <!-- 헤더 -->
    <div ref="mypageHeaderEl" class="mypage-header-fixed">
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
    <div :style="{ height: mypageHeaderHeight + 'px' }" aria-hidden="true" />

    <div class="px-4 flex flex-col gap-[22px]">

      <!-- 멤버 패스 카드 -->
      <section class="member-card">
        <span class="member-card-glow" aria-hidden="true"></span>
        <header class="member-card-head">
          <img src="@/assets/brand/tripass-text.png" alt="TRIPASS" />
          <span>MEMBER · {{ authStore.user?.id ?? '000000' }}</span>
        </header>

        <div class="member-profile">
          <span class="member-avatar"><img src="@/assets/icons/blue_profile.svg" alt="" /></span>
          <div>
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
          <b>자산 관리하기</b>
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

      <!-- 알림 설정 -->
      <div class="flex flex-col gap-[11px]">
        <div class="flex items-center justify-between px-0.5">
          <span class="text-base font-black text-gray-900">알림 설정</span>
          <div class="flex items-center gap-2">
            <span class="text-[11.5px] font-extrabold" style="color:#98A2B3">전체 알림</span>
            <button
              type="button"
              class="relative w-[42px] h-6 rounded-full flex items-center px-[3px] transition-colors"
              :style="{ background: mypageStore.settings.allEnabled ? '#2F6FED' : '#DDE2EC', justifyContent: mypageStore.settings.allEnabled ? 'flex-end' : 'flex-start' }"
              :aria-pressed="mypageStore.settings.allEnabled"
              aria-label="전체 알림"
              @click="mypageStore.toggleSetting('allEnabled')"
            >
              <span class="w-[18px] h-[18px] rounded-full bg-white"></span>
            </button>
          </div>
        </div>
        <div class="bg-white rounded-[20px] overflow-hidden" style="box-shadow: 0 4px 14px rgba(16,25,43,0.07)">
          <div
            v-for="(row, i) in notificationRows"
            :key="row.key"
            class="flex items-center gap-3 px-[18px] py-[14px]"
            :class="i < notificationRows.length - 1 ? 'border-b' : ''"
            style="border-color: #F1F3F8"
          >
            <div class="flex-1">
              <div class="text-[14px] font-extrabold text-gray-900">{{ row.label }}</div>
              <div class="text-[11px] text-gray-400 mt-[3px]">{{ row.sub }}</div>
            </div>
            <button
              type="button"
              class="relative w-[42px] h-6 rounded-full flex items-center px-[3px] flex-shrink-0 transition-colors"
              :disabled="mypageStore.settings.allEnabled"
              :style="{ background: (mypageStore.settings.allEnabled || mypageStore.settings[row.key]) ? '#2F6FED' : '#DDE2EC', justifyContent: (mypageStore.settings.allEnabled || mypageStore.settings[row.key]) ? 'flex-end' : 'flex-start' }"
              :aria-pressed="mypageStore.settings.allEnabled || mypageStore.settings[row.key]"
              :aria-label="row.label"
              @click="mypageStore.toggleSetting(row.key)"
            >
              <span class="w-[18px] h-[18px] rounded-full bg-white"></span>
            </button>
          </div>
        </div>
        <div class="notification-time-card">
          <div class="notification-time-head">
            <div><b>알림 수신 시간</b><small>설정한 시간 안에서 알림을 받아요</small></div>
            <span>TIME</span>
          </div>
          <div class="notification-time-fields">
            <label><span>시작</span><input v-model="notificationStartTime" type="time" aria-label="알림 시작 시간" /></label>
            <i>–</i>
            <label><span>종료</span><input v-model="notificationEndTime" type="time" aria-label="알림 종료 시간" /></label>
          </div>
        </div>
      </div>

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

    <BottomNav />
  </div>
</template>

<style scoped>
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
  justify-content: space-between;
}
.member-card-head img {
  width: 76px;
  height: auto;
  filter: brightness(0) invert(1);
}
.member-card-head span {
  color: rgba(255, 255, 255, .52);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: .08em;
}
.member-profile {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  margin-top: 19px;
}
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
.member-profile strong { display: block; font-size: 14px; font-weight: 800; }
.member-profile small { display: block; margin-top: 2px; color: rgba(255, 255, 255, .55); font-size: 10px; }
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
  position: fixed;
  top: 0;
  left: 50%;
  z-index: 60;
  width: 100%;
  max-width: 390px;
  background: #eef2f8;
  transform: translateX(-50%);
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
.logout-button { display:flex;align-items:center;justify-content:center;gap:7px;width:100%;min-height:48px;margin:0 0 16px;border:1px solid #f0d9dd;border-radius:16px;background:#fff;color:#c94e5b;font-size:12px;font-weight:800;box-shadow:0 5px 14px rgba(116,31,45,.045); }
.logout-button:active { background:#fff5f6; }
.logout-button:disabled { opacity:.55; }
.logout-button svg { width:17px;height:17px; }
</style>
