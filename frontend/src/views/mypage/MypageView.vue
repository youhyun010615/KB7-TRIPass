<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { logout as logoutApi } from '@/api/auth'
import { getAccounts } from '@/api/asset'
import { useAuthStore } from '@/stores/auth'
import { useCardStore } from '@/stores/cardStore'
import { useMypageStore } from '@/stores/mypage'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()
const authStore = useAuthStore()
const cardStore = useCardStore()
const mypageStore = useMypageStore()

const isLoggingOut = ref(false)
const accounts = ref([])

const memberIdentity = computed(() => {
  const provider = authStore.user?.loginProvider ?? 'LOCAL'
  if (provider !== 'LOCAL') return authStore.user?.email ?? '이메일 미등록'
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
  {
    label: '계좌·카드 연동',
    sub: `통장 ${accountCount.value}개 · 카드 ${cardCount.value}장 연동 중`,
    path: '/mypage/assets',
    icon: 'card',
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
  <div class="min-h-screen pb-20 flex flex-col" style="background: #F4F5F9">

    <!-- 헤더 -->
    <div class="flex items-center px-5 pt-14 pb-3">
      <h1 class="text-2xl font-bold text-gray-900">마이페이지</h1>
    </div>

    <div class="px-4 flex flex-col gap-[22px]">

      <!-- 멤버 패스 카드 -->
      <div class="relative rounded-[22px] overflow-hidden text-white" style="background: linear-gradient(155deg, #0B2A6B 0%, #123C94 62%, #17459F 100%); box-shadow: 0 10px 24px rgba(11,42,107,0.22)">
        <div class="absolute rounded-full" style="top:-58px; right:-40px; width:150px; height:150px; background: rgba(255,212,102,0.1)"></div>

        <div class="relative flex items-center justify-between px-5 py-[13px]" style="background: rgba(255,255,255,0.07); border-bottom: 1px solid rgba(255,255,255,0.12)">
          <div class="flex items-center gap-2">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none"><path d="M2 16l7-2 3.5-8 2 .5-2 7.5 5.5-1.5.5 1.5-5 3 2 5-2-.5-2-4-3 3.5-.5 2-1.5-.5.5-3-6.5 2z" fill="#FFD466"/></svg>
            <span class="text-[10.5px] font-extrabold tracking-[0.12em]" style="color:#FFD466">TRIPASS MEMBER PASS</span>
          </div>
          <span class="font-mono text-[10.5px] font-bold" style="color: rgba(255,255,255,0.65)">TP-{{ authStore.user?.id ?? '000000' }}</span>
        </div>

        <div class="relative px-5 py-[18px]">
          <div class="flex items-center gap-[11px]">
            <div class="w-9 h-9 rounded-full flex items-center justify-center text-sm font-black flex-shrink-0" style="background: rgba(255,255,255,0.14)">
              {{ authStore.user?.name?.[0] ?? '고' }}
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-[15px] font-black">{{ authStore.user?.name ?? '고객' }}</div>
              <div class="text-[10.5px] mt-0.5" style="color: rgba(255,255,255,0.55)">{{ memberIdentity }}</div>
            </div>
          </div>

          <div class="text-[11px] font-extrabold mt-[18px]" style="color:#FFD466; letter-spacing:0.06em">전체 보유금액</div>
          <div class="font-mono text-[33px] font-bold mt-1.5" style="letter-spacing:-0.02em">{{ formatWon(totalAssets) }}</div>

          <div class="flex items-center gap-2.5 mt-[18px] pt-4" style="border-top: 1px solid rgba(255,255,255,0.16)">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" class="flex-shrink-0"><rect x="3" y="6" width="18" height="13" rx="2.5" stroke="rgba(255,255,255,0.75)" stroke-width="1.8"/><path d="M3 10.5h18" stroke="rgba(255,255,255,0.75)" stroke-width="1.8"/></svg>
            <span class="flex-1 text-[12.5px] font-extrabold">통장 {{ accountCount }}개 · 카드 {{ cardCount }}장 연동 중</span>
            <button type="button" class="flex-shrink-0 text-[11.5px] font-extrabold active:opacity-70" style="color:#FFD466" @click="router.push('/mypage/assets')">
              연동된 계좌·카드 ›
            </button>
          </div>
        </div>
      </div>

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
              <svg v-if="item.icon === 'user'" width="19" height="19" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="8" r="3.4" stroke="#2F6FED" stroke-width="1.9"/>
                <path d="M5 20c1.2-3.8 4-5.6 7-5.6s5.8 1.8 7 5.6" stroke="#2F6FED" stroke-width="1.9" stroke-linecap="round"/>
              </svg>
              <svg v-if="item.icon === 'travel'" width="19" height="19" viewBox="0 0 24 24" fill="none">
                <path d="M2 16l7-2 3.5-8 2 .5-2 7.5 5.5-1.5.5 1.5-5 3 2 5-2-.5-2-4-3 3.5-.5 2-1.5-.5.5-3-6.5 2z" fill="#2F6FED"/>
              </svg>
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
        <div class="bg-white rounded-2xl px-[18px] py-[14px] flex items-center gap-3" style="box-shadow: 0 4px 14px rgba(16,25,43,0.07)">
          <div class="flex-1">
            <div class="text-[11.5px] font-bold" style="color:#98A2B3">알림 수신 시간</div>
            <div class="text-[14px] font-extrabold text-gray-900 mt-1">오전 9:00 – 오후 10:00</div>
          </div>
        </div>
      </div>

      <button
          type="button"
          class="w-full py-3.5 mb-4 rounded-2xl text-sm font-semibold text-center"
          style="color: #B4BCC9"
          :disabled="isLoggingOut"
          @click="logout"
      >
        {{ isLoggingOut ? '로그아웃 중...' : '로그아웃' }}
      </button>
    </div>

    <BottomNav />
  </div>
</template>
