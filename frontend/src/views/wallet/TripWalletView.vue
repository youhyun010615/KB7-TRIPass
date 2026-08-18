<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronRight, CreditCard } from '@lucide/vue'
import BottomNav from '@/components/common/BottomNav.vue'
import NotificationBell from '@/components/common/NotificationBell.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import TravelCardVisual from '@/components/common/TravelCardVisual.vue'
import { useTripWalletStore } from '@/stores/tripWallet'
import { getAccountInstitutions } from '@/api/asset'

const router = useRouter()
const wallet = useTripWalletStore()

// 앱 프레임(App.vue)의 overflow:hidden 때문에 sticky 대신 fixed로 헤더를 고정한다.
const walletHeaderEl = ref(null)
const walletHeaderHeight = ref(0)
let walletHeaderResizeObserver = null

function syncWalletHeaderHeight() {
  if (walletHeaderEl.value) {
    walletHeaderHeight.value = walletHeaderEl.value.offsetHeight
  }
}

watch(walletHeaderEl, (el) => {
  walletHeaderResizeObserver?.disconnect()
  walletHeaderResizeObserver = null
  if (!el) return

  syncWalletHeaderHeight()
  if (window.ResizeObserver) {
    walletHeaderResizeObserver = new ResizeObserver(syncWalletHeaderHeight)
    walletHeaderResizeObserver.observe(el)
  }
})

onBeforeUnmount(() => {
  walletHeaderResizeObserver?.disconnect()
})

// 로딩이 3초 넘게 걸릴 때만 스피너를 보여준다.
const showLoadingSpinner = ref(false)
let loadingSpinnerTimer = null
watch(() => wallet.isLoading, (loading) => {
  window.clearTimeout(loadingSpinnerTimer)
  if (loading) {
    loadingSpinnerTimer = window.setTimeout(() => {
      showLoadingSpinner.value = true
    }, 3000)
  } else {
    showLoadingSpinner.value = false
  }
}, { immediate: true })
const showTransfer = ref(false)
const showAutoChargeSetting = ref(false)
const showUnlinkConfirm = ref(false)
const showMonthlyDetail = ref(false)
const monthlyDetail = ref(null)
const transferMode = ref('charge')
const amount = ref('')
const autoChargeDay = ref(wallet.autoCharge.day)
const autoChargeAmount = ref(wallet.autoCharge.amount)
const selectedAccount = ref(null)
const withdrawMode = ref('registered') // 'registered' | 'recent' | 'manual'
const selectedRecentRecipientId = ref(null)
const bankInstitutions = ref([])
const bankInstitutionsError = ref('')
const manualBankCode = ref('')
const manualAccountNumber = ref('')
const manualAccountHolderName = ref('')
const notice = ref('')
let noticeTimer
const accounts = computed(() => wallet.linkedAccounts)

watch(accounts, (items) => {
  if (!selectedAccount.value && items.length) {
    selectedAccount.value = items.find(account => account.isPrimary)?.accountId ?? items[0].accountId
  }
}, { immediate: true })

const money = value => `${Math.abs(Number(value || 0)).toLocaleString('ko-KR')}원`
const shortMoney = value => `${Math.abs(Math.floor(Number(value || 0) / 10000)).toLocaleString('ko-KR')}만`
const maxChart = computed(() => Math.max(...wallet.monthlySavings.map(item => Math.abs(item.amount || 0)), 1))
const isCurrentMonth = item => item.isCurrent || item.month === '8월'
const isAvailableMonth = item => item.available !== false
const monthKey = item => item.monthKey || `2026-${String(item.month).replace(/[^0-9]/g, '').padStart(2, '0')}`

// 0선을 기준으로 위(양수)는 74px, 아래(음수)는 34px까지 뻗어나가는 막대 그래프다.
// 금액 라벨은 막대 위치와 무관하게 항상 막대 위쪽 고정 자리에 표시해서, 월 텍스트와 겹치지 않게 한다.
const CHART_POSITIVE_ZONE = 74
const CHART_NEGATIVE_ZONE = 34
const CHART_MIN_BAR = 4

function chartBarStyle(item) {
  if (!isAvailableMonth(item)) {
    return { bottom: `${CHART_NEGATIVE_ZONE}px`, height: '18px' }
  }

  const amount = Number(item.amount || 0)

  if (amount > 0) {
    const height = Math.max(CHART_MIN_BAR, Math.round(amount / maxChart.value * CHART_POSITIVE_ZONE))
    return { bottom: `${CHART_NEGATIVE_ZONE}px`, height: `${height}px` }
  }

  if (amount < 0) {
    // 0선(bottom: CHART_NEGATIVE_ZONE)에 막대 윗변이 딱 붙어서 아래로 뻗어나가야 하므로,
    // top이 아니라 (0선 위치 - 막대 높이)를 bottom으로 잡는다.
    const height = Math.max(CHART_MIN_BAR, Math.round(Math.abs(amount) / maxChart.value * CHART_NEGATIVE_ZONE))
    return { bottom: `${CHART_NEGATIVE_ZONE - height}px`, height: `${height}px` }
  }

  return { bottom: `${CHART_NEGATIVE_ZONE - CHART_MIN_BAR / 2}px`, height: `${CHART_MIN_BAR}px` }
}

function chartColorClass(item) {
  if (!isAvailableMonth(item)) return 'disabled'
  if (isCurrentMonth(item)) return 'current'

  const amount = Number(item.amount || 0)
  if (amount < 0) return 'negative'
  if (amount === 0) return 'zero'
  return ''
}
const currencyMeta = {
  EUR: { name: '유로', flag: '🇪🇺', rate: 1486 },
  JPY: { name: '일본 엔', flag: '🇯🇵', rate: 9.42 },
  CHF: { name: '스위스 프랑', flag: '🇨🇭', rate: 1606 },
  USD: { name: '미국 달러', flag: '🇺🇸', rate: 1375 },
}
const foreignBalances = computed(() => wallet.foreignBalances.map(item => ({
  ...item,
  code: item.code || item.currencyCode,
  name: item.name || item.currencyName || currencyMeta[item.code || item.currencyCode]?.name || item.code || item.currencyCode,
  flag: item.flag || currencyMeta[item.code || item.currencyCode]?.flag || '🌐',
  amount: item.amount ?? item.balanceAmount,
  krwAmount: item.krwAmount ?? item.krwEstimatedAmount,
  rate: item.rate || currencyMeta[item.code || item.currencyCode]?.rate || Math.round((item.krwAmount || item.krwEstimatedAmount || 0) / Math.max(Number(item.amount || item.balanceAmount || 1), 1)),
})))
const recentForeignBalances = computed(() => foreignBalances.value.slice(0, 3))

function cardNumberLines(number) {
  const groups = String(number || '').split(' · ')
  const mid = Math.ceil(groups.length / 2)
  return [groups.slice(0, mid).join(' · '), groups.slice(mid).join(' · ')]
}

async function openTransfer(mode) {
  transferMode.value = mode
  amount.value = ''
  withdrawMode.value = 'registered'
  selectedRecentRecipientId.value = null
  manualBankCode.value = ''
  manualAccountNumber.value = ''
  manualAccountHolderName.value = ''
  bankInstitutionsError.value = ''

  // linkedAccounts는 페이지 진입 시 로컬 캐시로 먼저 채워지고 잠시 뒤 최신 데이터로 교체된다.
  // 그 사이에 시트를 열면 오래된 잔액으로 주계좌가 선택될 수 있어, 열기 전에 최신 계좌 정보를 받아온다.
  try {
    await wallet.loadAccounts()
    if (mode === 'withdraw') {
      await wallet.loadWithdrawOptions()
      selectedRecentRecipientId.value = wallet.withdrawRecentAccounts[0]?.recipientId ?? null
      await loadBankInstitutions()
    }
  } catch {
    // 최신 데이터를 못 받아오면 캐시된 값으로라도 계속 진행한다.
  }

  const accountsForSelection = mode === 'withdraw' && wallet.withdrawRegisteredAccounts.length
    ? wallet.withdrawRegisteredAccounts
    : accounts.value
  selectedAccount.value = accountsForSelection.find(account => account.isPrimary)?.accountId ?? accountsForSelection[0]?.accountId ?? null
  showTransfer.value = true
}

async function loadBankInstitutions() {
  if (bankInstitutions.value.length) return
  try {
    const res = await getAccountInstitutions()
    bankInstitutions.value = res.data?.data ?? []
    bankInstitutionsError.value = ''
  } catch {
    bankInstitutionsError.value = '은행 목록을 불러오지 못했어요.'
  }
}

const quickAmountValues = [10000, 50000, 100000, 1000000]

const maxTransferAmount = computed(() => {
  if (transferMode.value === 'withdraw') return wallet.balance
  return accounts.value.find(account => account.accountId === selectedAccount.value)?.withdrawableAmount ?? 0
})

function applyQuickAmount(value) {
  if (value === 'full') {
    amount.value = String(maxTransferAmount.value)
    return
  }

  const current = Number(String(amount.value).replace(/[^0-9]/g, '')) || 0
  amount.value = String(current + value)
}

function openAutoChargeSetting() {
  autoChargeDay.value = wallet.autoCharge.day
  autoChargeAmount.value = wallet.autoCharge.amount
  showAutoChargeSetting.value = true
}

function showNotice(message) {
  notice.value = message
  window.clearTimeout(noticeTimer)
  noticeTimer = window.setTimeout(() => {
    notice.value = ''
  }, 3000)
}

function ledgerTitle(item) {
  if (item.memo) return item.memo
  if (item.transactionType === 'CHARGE') return item.transferMethod === 'AUTO_SAVING' ? '자동 채우기' : '월렛 채우기'
  if (item.transactionType === 'WITHDRAW') return '월렛 빼기'
  if (item.transactionType === 'CARD_TOPUP') return '트래블카드 충전'
  if (item.transactionType === 'MISSION_REWARD') return '미션 보상'
  if (item.transactionType === 'REFUND') return '환불'
  if (item.transactionType === 'ADJUST') return '시스템 보정'
  return item.transactionType || '월렛 거래'
}

function ledgerDate(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

async function openMonthlyDetail(item) {
  if (!isAvailableMonth(item)) return

  try {
    monthlyDetail.value = await wallet.loadMonthlySavingDetail(monthKey(item))
    showMonthlyDetail.value = true
  } catch {
    showNotice(wallet.errorMessage)
  }
}

onBeforeUnmount(() => {
  window.clearTimeout(noticeTimer)
  window.clearTimeout(loadingSpinnerTimer)
  wallet.mainScrollY = window.scrollY
})

onMounted(async () => {
  // 데이터를 다시 불러오기 전에 먼저 스크롤을 복원해서 맨 위로 갔다가 되돌아오는
  // 깜빡임 없이 이전 위치 그대로 이어서 보이게 한다.
  if (wallet.mainScrollY > 0) {
    await nextTick()
    window.scrollTo(0, wallet.mainScrollY)
  }

  try {
    await Promise.all([wallet.loadWalletMain(), wallet.loadAccounts(), wallet.loadAutoSaving(), wallet.loadForeignBalances()])
  } catch {
    showNotice(wallet.errorMessage)
  }
})

async function saveAutoChargeSetting() {
  try {
    await wallet.updateAutoCharge({
      day: Number(autoChargeDay.value),
      amount: Number(String(autoChargeAmount.value).replace(/[^0-9]/g, '')) || 0,
      enabled: true,
    })
    showAutoChargeSetting.value = false
    showNotice('자동 채우기 설정을 저장했어요.')
  } catch {
    showNotice(wallet.errorMessage || '자동 채우기 설정 저장에 실패했어요.')
  }
}

async function submitTransfer() {
  const chargeValue = Number(String(amount.value).replace(/[^0-9]/g, '')) || 0
  if (transferMode.value === 'charge' && chargeValue > maxTransferAmount.value) {
    showNotice('계좌 잔액 안에서 채울 금액을 입력해 주세요.')
    return
  }

  try {
    let ok
    if (transferMode.value === 'charge') {
      ok = await wallet.deposit(amount.value, selectedAccount.value)
    } else if (withdrawMode.value === 'recent') {
      ok = await wallet.withdraw({ mode: 'recent', amount: amount.value, recipientId: selectedRecentRecipientId.value })
    } else if (withdrawMode.value === 'manual') {
      const bank = bankInstitutions.value.find(item => item.organizationCode === manualBankCode.value)
      ok = await wallet.withdraw({
        mode: 'manual',
        amount: amount.value,
        bankCode: manualBankCode.value,
        bankName: bank?.institutionName ?? '',
        accountNumber: manualAccountNumber.value,
        accountHolderName: manualAccountHolderName.value,
      })
    } else {
      ok = await wallet.withdraw({ mode: 'registered', amount: amount.value, targetAccountId: selectedAccount.value })
    }

    if (!ok) {
      showNotice(transferMode.value === 'charge' ? '채울 금액을 입력해 주세요.' : '출금 정보를 확인해 주세요.')
      return
    }

    showNotice(transferMode.value === 'charge'
      ? `${money(amount.value)}을 월렛에 채웠어요.`
      : `${money(amount.value)}을 계좌로 뺐어요.`)
    showTransfer.value = false
  } catch {
    showNotice(wallet.errorMessage || '월렛 거래 처리에 실패했어요.')
  }
}

async function confirmUnlinkTravelCard() {
  try {
    await wallet.unlinkTravelCard()
    showUnlinkConfirm.value = false
    showNotice('트래블카드 연결을 해제했어요.')
  } catch {
    showNotice(wallet.errorMessage || '트래블카드 연결 해제에 실패했어요.')
  }
}
</script>

<template>
  <main class="wallet-page">
    <LoadingSpinner v-if="showLoadingSpinner" size="lg" full-screen />

    <div ref="walletHeaderEl" class="wallet-header-fixed">
      <header class="wallet-header">
        <div>
          <p>
            <img src="@/assets/brand/tripass-text.png" class="header-wordmark" alt="TRIPASS" />
          </p>
          <h1>WALLET</h1>
        </div>
        <NotificationBell />
      </header>
    </div>
    <div :style="{ height: walletHeaderHeight + 'px' }" aria-hidden="true" />

    <section class="wallet-card">
      <div class="wallet-card-tab" />
      <div class="wallet-card-inner">
        <div class="wallet-card-top">
          <b>TRIP Wallet</b>
          <button type="button" class="link-button" @click="router.push('/wallet/accounts')">연결계좌 설정<ChevronRight :size="14" /></button>
        </div>
        <div class="wallet-balance">
          <strong>{{ wallet.balance.toLocaleString('ko-KR') }}</strong>
          <span>원</span>
        </div>
        <p class="emergency">비상금: {{ money(wallet.emergencyAmount) }}</p>
        <div class="wallet-actions">
          <button type="button" @click="openTransfer('charge')">채우기</button>
          <button type="button" @click="openTransfer('withdraw')">빼기</button>
          <button type="button" class="history-button" @click="router.push('/wallet/ledgers')">내역</button>
        </div>
      </div>
    </section>

    <section class="saving-card white-card">
      <div class="section-title">
        <h2>월별 합산 금액</h2>
      </div>
      <div class="saving-chart">
        <button
          v-for="item in wallet.monthlySavings"
          :key="item.month"
          type="button"
          class="chart-item"
          :disabled="!isAvailableMonth(item)"
          @click="openMonthlyDetail(item)"
        >
          <b class="chart-amount" :class="chartColorClass(item)">
            {{ isAvailableMonth(item) ? `${item.amount < 0 ? '-' : ''}${shortMoney(item.amount)}` : '-' }}
          </b>
          <div class="bar-wrap">
            <span class="zero-line" :style="{ bottom: `${CHART_NEGATIVE_ZONE}px` }" />
            <i
              :class="chartColorClass(item)"
              :style="chartBarStyle(item)"
            />
          </div>
          <span :class="chartColorClass(item)">{{ item.month }}</span>
        </button>
      </div>
      <p class="saving-hint">최근 6개월간 월렛 입출금 합계예요.</p>
    </section>

    <section v-if="!wallet.isTravelCardLinked" class="empty-card white-card">
      <div class="empty-icon"><CreditCard :size="30" /></div>
      <h2>연결된 트래블카드가 없어요</h2>
      <p>트래블카드를 연결하면 <br> 월렛에서 꺼내 환전하고 보관할 수 있어요.</p>
      <button type="button" class="primary-button" @click="router.push('/wallet/travel-card/link')">등록하기</button>
      <div class="recommend-row">
        <RouterLink class="recommend-link" to="/financial/cards">
          트래블카드 추천 보러가기
          <ChevronRight :size="12" />
        </RouterLink>
      </div>
    </section>

    <section v-else class="linked-card white-card">
      <div class="linked-title">
        <h2>내 트래블카드</h2>
        <button type="button" @click="showUnlinkConfirm = true">연결 해제</button>
      </div>
      <div class="travel-card-box">
        <div class="card-visual">
          <TravelCardVisual
            v-model:frozen-index="wallet.travelCard.frozenIndex"
            :images="wallet.travelCard.images"
            :color="wallet.travelCard.color"
            :issuer="wallet.travelCard.issuer"
            :brand="wallet.travelCard.brand"
          />
        </div>
        <div class="card-info">
          <h3>{{ wallet.travelCard.name }}</h3>
          <dl class="travel-card-meta">
            <div>
              <dt>카드사</dt>
              <dd>{{ wallet.travelCard.issuer }}</dd>
            </div>
            <div>
              <dt>카드번호</dt>
              <dd>{{ cardNumberLines(wallet.travelCard.number)[0] }}<br>{{ cardNumberLines(wallet.travelCard.number)[1] }}</dd>
            </div>
          </dl>
          <button type="button" @click="router.push('/wallet/travel-card/exchange')">외화 충전</button>
        </div>
      </div>
      <hr>
      <div class="currency-head">
        <h3 class="currency-title">보유 외화</h3>
        <button type="button" class="link-button" @click="router.push('/wallet/foreign-balances')">전체 보기<ChevronRight :size="14" /></button>
      </div>
      <div v-if="recentForeignBalances.length" class="currency-list">
        <button v-for="item in recentForeignBalances" :key="item.code" type="button" class="currency-row">
          <span class="flag">{{ item.flag }}</span>
          <b>{{ item.code }} <small>{{ item.name }}</small></b>
          <em>현재 환율 {{ Number(item.rate || 0).toLocaleString('ko-KR') }}원</em>
          <strong>{{ item.amount.toLocaleString('ko-KR', { minimumFractionDigits: item.code === 'EUR' || item.code === 'CHF' ? 2 : 0 }) }}</strong>
          <small class="krw">약 {{ money(item.krwAmount) }}</small>
        </button>
      </div>
      <p v-else class="list-empty">보유한 외화가 없어요.</p>
    </section>


    <Transition name="sheet">
      <div v-if="showTransfer" class="wallet-sheet-backdrop" @click.self="showTransfer = false">
        <section class="wallet-sheet">
          <i class="sheet-handle" />
          <div class="wallet-sheet-head">
            <h2>{{ transferMode === 'charge' ? '월렛 채우기' : '월렛 빼기' }}</h2>
            <button type="button" class="sheet-close" @click="showTransfer = false">×</button>
          </div>
          <p>{{ transferMode === 'charge' ? '연결된 계좌에서 월렛으로 여행 자금을 옮겨요.' : '월렛 잔액을 계좌로 돌려보내요.' }}</p>

          <label v-if="transferMode === 'charge'">
            연결 계좌 선택
            <select v-model="selectedAccount">
              <option v-for="account in accounts" :key="account.accountId" :value="account.accountId">
                {{ account.name }} · {{ account.number }}
              </option>
            </select>
          </label>

          <template v-else>
            <div class="withdraw-mode-tabs">
              <button type="button" :class="{ active: withdrawMode === 'registered' }" @click="withdrawMode = 'registered'">등록 계좌</button>
              <button type="button" :class="{ active: withdrawMode === 'recent' }" @click="withdrawMode = 'recent'">최근 계좌</button>
              <button type="button" :class="{ active: withdrawMode === 'manual' }" @click="withdrawMode = 'manual'">직접 입력</button>
            </div>

            <label v-if="withdrawMode === 'registered'">
              출금 받을 계좌
              <select v-model="selectedAccount">
                <option
                  v-for="account in (wallet.withdrawRegisteredAccounts.length ? wallet.withdrawRegisteredAccounts : accounts)"
                  :key="account.accountId"
                  :value="account.accountId"
                >
                  {{ account.name }} · {{ account.number }}
                </option>
              </select>
            </label>

            <label v-else-if="withdrawMode === 'recent'">
              최근 사용한 계좌
              <select v-model="selectedRecentRecipientId">
                <option v-for="item in wallet.withdrawRecentAccounts" :key="item.recipientId" :value="item.recipientId">
                  {{ item.bankName }} {{ item.accountNumber }} · {{ item.accountHolderName }}
                </option>
              </select>
              <p v-if="!wallet.withdrawRecentAccounts.length" class="empty-inline-hint">최근 사용한 계좌가 없어요.</p>
            </label>

            <template v-else>
              <label>
                은행 선택
                <select v-model="manualBankCode" :disabled="!bankInstitutions.length">
                  <option value="" disabled>은행을 선택해 주세요</option>
                  <option v-for="bank in bankInstitutions" :key="bank.organizationCode" :value="bank.organizationCode">
                    {{ bank.institutionName }}
                  </option>
                </select>
              </label>
              <p v-if="bankInstitutionsError" class="empty-inline-hint">
                {{ bankInstitutionsError }}
                <button type="button" style="margin-left:6px;color:#2f70e9;font-weight:700;text-decoration:underline" @click="loadBankInstitutions">다시 시도</button>
              </p>
              <label>
                계좌번호
                <div class="amount-field"><input v-model="manualAccountNumber" inputmode="numeric" placeholder="'-' 없이 숫자만 입력"></div>
              </label>
              <label>
                예금주명
                <div class="amount-field"><input v-model="manualAccountHolderName" placeholder="예금주명을 입력해 주세요"></div>
              </label>
            </template>
          </template>

          <label>
            금액
            <div class="amount-field"><input v-model="amount" inputmode="numeric" placeholder="0"><b>원</b></div>
          </label>
          <div class="quick-amounts">
            <button v-for="value in quickAmountValues" :key="value" type="button" @click="applyQuickAmount(value)">+{{ value / 10000 }}만</button>
            <button type="button" @click="applyQuickAmount('full')">전액</button>
          </div>
          <button class="confirm-transfer" type="button" @click="submitTransfer">{{ transferMode === 'charge' ? '채우기' : '빼기' }}</button>
          <div v-if="transferMode === 'charge'" class="auto-charge-preview compact">
            <div>
              <h3>자동 채우기 진행 중</h3>
              <p>매월 {{ wallet.autoCharge.day }}일 · {{ money(wallet.autoCharge.amount) }}</p>
            </div>
            <button type="button" @click="openAutoChargeSetting">설정</button>
          </div>
        </section>
      </div>
    </Transition>


    <Transition name="sheet">
      <div v-if="showAutoChargeSetting" class="auto-modal-backdrop" @click.self="showAutoChargeSetting = false">
        <section class="auto-modal">
          <div class="auto-modal-head">
            <h2>자동 채우기 설정</h2>
            <button type="button" @click="showAutoChargeSetting = false">×</button>
          </div>
          <label>
            송금일
            <div class="modal-field"><span>매월</span><input v-model="autoChargeDay" inputmode="numeric"><b>일</b></div>
          </label>
          <label>
            송금 금액
            <div class="modal-field"><input v-model="autoChargeAmount" inputmode="numeric"><b>원</b></div>
          </label>
          <p>자동으로 주계좌에서 월렛으로 들어와요.</p>
          <button class="modal-save" type="button" @click="saveAutoChargeSetting">설정 저장</button>
        </section>
      </div>
    </Transition>

    <Transition name="sheet">
      <div v-if="showMonthlyDetail" class="wallet-sheet-backdrop" @click.self="showMonthlyDetail = false">
        <section class="wallet-sheet monthly-detail-sheet">
          <i class="sheet-handle" />
          <div class="monthly-detail-head">
            <div>
              <p>{{ monthlyDetail?.month }}</p>
              <h2>월별 저축 상세</h2>
            </div>
            <button type="button" @click="showMonthlyDetail = false">×</button>
          </div>
          <div class="monthly-detail-total">
            <span>모은 금액</span>
            <strong>{{ money(monthlyDetail?.savedAmount) }}</strong>
          </div>
          <div class="monthly-detail-summary">
            <div>
              <span>채우기</span>
              <b>+{{ money(monthlyDetail?.chargeAmount) }}</b>
            </div>
            <div>
              <span>빼기</span>
              <b>-{{ money(monthlyDetail?.withdrawAmount) }}</b>
            </div>
          </div>
          <div class="monthly-ledger-list">
            <p v-if="wallet.isMonthlyDetailLoading">내역을 불러오는 중이에요.</p>
            <p v-else-if="!monthlyDetail?.ledgers?.length">이 달에는 월렛 자금 이동 내역이 없어요.</p>
            <article v-for="item in monthlyDetail?.ledgers || []" :key="item.ledgerId">
              <div>
                <b>{{ ledgerTitle(item) }}</b>
                <span>{{ ledgerDate(item.createdAt) }}<template v-if="item.accountName"> · {{ item.accountName }}</template></span>
              </div>
              <strong :class="{ out: item.direction === 'OUT' }">
                {{ item.direction === 'OUT' ? '-' : '+' }}{{ money(item.amount) }}
              </strong>
            </article>
          </div>
        </section>
      </div>
    </Transition>

    <Transition name="sheet">
      <div v-if="showUnlinkConfirm" class="confirm-backdrop" @click.self="showUnlinkConfirm = false">
        <section class="confirm-modal">
          <h2>트래블카드 연결을 해제할까요?</h2>
          <p>해제하면 월렛에서 바로 외화 환전하기와 <br> 보유 외화 확인을 사용할 수 없어요.</p>
          <div class="confirm-actions">
            <button type="button" @click="showUnlinkConfirm = false">취소</button>
            <button type="button" class="danger" @click="confirmUnlinkTravelCard">해제하기</button>
          </div>
        </section>
      </div>
    </Transition>

    <p v-if="notice" class="wallet-toast">{{ notice }}</p>
    <BottomNav />
  </main>
</template>

<style scoped>
.wallet-page{max-width:430px;min-height:100vh;margin:0 auto;padding:0 16px 88px;background:#eef2f8;color:#111827}.wallet-header-fixed{position:fixed;top:0;left:50%;z-index:60;width:100%;max-width:430px;padding:42px 16px 14px;background:#eef2f8;transform:translateX(-50%)}.wallet-header{display:flex;align-items:flex-start;justify-content:space-between}.wallet-header p{display:flex;align-items:center;gap:4px;font-family:'Space Mono',monospace;color:#0b2a6b;font-size:9.5px;font-weight:800;letter-spacing:.15em;margin-bottom:4px}.wallet-header h1{margin-top:2px;font-size:19px;font-weight:900;color:#10192b;letter-spacing:normal}.header-plane{width:12px;height:12px;animation:header-plane-fly 2.6s ease-in-out infinite}@keyframes header-plane-fly{0%,100%{transform:translateY(0) rotate(0deg);filter:brightness(1) drop-shadow(0 0 0 rgba(47,112,242,0))}25%{transform:translateY(-1.5px) rotate(-8deg)}50%{transform:translateY(0) rotate(0deg);filter:brightness(1.6) drop-shadow(0 0 3px rgba(47,112,242,.55))}75%{transform:translateY(1.5px) rotate(6deg)}}.wallet-card{position:relative;margin-top:20px}.wallet-card-tab{position:absolute;top:-13px;left:42px;width:150px;height:42px;border-radius:18px 18px 0 0;background:#1f5ab9}.wallet-card-inner{position:relative;z-index:1;padding:22px 24px 18px;border-radius:24px;background:#14357f;box-shadow:0 16px 28px rgba(24,51,99,.2);color:#fff}.wallet-card-inner:before{content:"";position:absolute;inset:12px;border:1px solid rgba(124,164,232,.42);border-radius:21px;pointer-events:none}.wallet-card-top,.wallet-balance,.emergency,.goal-row,.goal-bar,.wallet-actions{position:relative;z-index:1}.wallet-card-top{display:flex;align-items:center;justify-content:space-between}.wallet-card-top b{color:#ffd34d;font-size:17px;font-weight:800}.wallet-card-top button{color:#c4d4f5;font-size:12px;font-weight:600}.link-button{display:inline-flex;align-items:center;gap:1px}.wallet-balance{display:flex;align-items:flex-end;gap:10px;margin-top:26px}.wallet-balance strong{font-size:42px;font-weight:800;line-height:.95;letter-spacing:-.05em}.wallet-balance span{font-size:17px;font-weight:700}.emergency{margin-top:11px;color:#ffd34d;font-size:13px;font-weight:700}.goal-row{display:flex;justify-content:space-between;margin-top:18px;font-size:14px;font-weight:600}.goal-row b{color:#ffd34d;font-weight:800}.goal-bar{height:9px;margin-top:12px;overflow:hidden;border-radius:99px;background:rgba(160,183,225,.48)}.goal-bar i{display:block;height:100%;border-radius:inherit;background:#ffd45a}.wallet-actions{display:grid;grid-template-columns:1fr 1fr 58px;gap:10px;margin-top:24px}.wallet-actions button{height:50px;border:1px solid rgba(163,194,248,.55);border-radius:999px;background:#4169af;color:#fff;font-size:17px;font-weight:700}.wallet-actions .history-button{font-size:14px;background:#234d9a}.white-card{margin-top:16px;border-radius:24px;background:#fff;box-shadow:0 10px 24px rgba(18,43,82,.07)}.saving-card{padding:24px 20px}.section-title h2,.linked-title h2{font-size:22px;font-weight:800;letter-spacing:-.04em}.chart-legend{display:flex;align-items:center;gap:10px;margin-top:14px;color:#7c8aa2;font-size:12px;font-weight:600}.chart-legend span{display:flex;align-items:center;gap:6px}.chart-legend i{width:11px;height:11px;border-radius:4px}.chart-legend .target-line:before{content:"";display:inline-block;width:23px;border-top:1.5px dashed #aab8ce}.success{background:#079b84;color:#079b84}.fail{background:#f15b3b;color:#f15b3b}.chart-legend i.success{background:#079b84;color:#079b84}.chart-legend i.fail{background:#f15b3b;color:#f15b3b}.bar-wrap>b.success{color:#079b84}.bar-wrap>b.fail{color:#f15b3b}.chart-item i.success{background:#079b84;color:#079b84}.chart-item i.fail{background:#f15b3b;color:#f15b3b}.saving-chart{display:grid;grid-template-columns:repeat(6,1fr);align-items:end;gap:7px;margin-top:25px}.chart-item{display:flex;min-width:0;align-items:center;flex-direction:column;gap:8px}.bar-wrap{position:relative;display:flex;width:48px;height:124px;align-items:flex-end;justify-content:center}.bar-wrap>b{position:absolute;left:50%;z-index:2;transform:translateX(-50%);font-size:10px;font-weight:600;color:#7d8ba2;background:transparent!important;white-space:nowrap}.bar-wrap>b.current-amount{color:#64748b}.target-marker{position:absolute;left:0;right:0;border-top:1.5px dashed #aab8ce}.chart-item i{display:block;width:34px;border-radius:8px;background:currentColor}.chart-item i.current{width:34px;background:#94a3b8;color:#94a3b8;box-shadow:none;outline:3px solid #eef2f7}.chart-item>span{color:#5f6c82;font-size:13px;font-weight:600}.chart-item>span.current{color:#1d66ed;font-weight:800}.chart-item>em{padding:5px 10px;border-radius:999px;font-size:11px;font-style:normal;font-weight:700;white-space:nowrap}.chart-item>em.success{background:#e4f8f2;color:#079b84}.chart-item>em.fail{background:#fff0e9;color:#ef4f32}.chart-item>em.current-status{background:#eef2f7;color:#64748b}.saving-summary{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:24px}.saving-summary span{display:block;padding:13px 8px;border-radius:14px;text-align:center;font-size:13px;font-weight:700}.saving-summary .good{background:#ddf7ee;color:#009a82}.saving-summary .bad{background:#fff0e9;color:#ef4f32}.empty-card{padding:40px 22px 26px;text-align:center}.empty-icon{display:grid;width:72px;height:72px;margin:0 auto 20px;place-items:center;border-radius:22px;background:linear-gradient(135deg,#eaf1ff,#dbe7ff);color:#2f6fe9;box-shadow:inset 0 0 0 1px rgba(47,111,233,.12)}.empty-card h2{font-size:21px;font-weight:800;letter-spacing:-.04em}.empty-card p{margin-top:10px;color:#94a3b8;font-size:14px;font-weight:500;line-height:1.55}.primary-button{width:100%;height:58px;margin-top:26px;border-radius:16px;background:#2e6ee9;color:#fff;font-size:18px;font-weight:800;box-shadow:0 12px 24px rgba(46,110,233,.2)}.recommend-row{display:flex;justify-content:flex-end;margin-top:14px}.recommend-link{display:inline-flex;align-items:center;gap:1px;color:#94a3b8;font-size:12px;font-weight:700}.linked-card{padding:23px 22px 22px}.linked-title{display:flex;align-items:center;justify-content:space-between}.linked-title button{color:#94a3b8;font-size:12px;font-weight:600}.travel-card-box{display:grid;grid-template-columns:136px 1fr;gap:16px;align-items:stretch;margin-top:22px}.card-visual{position:relative;width:100%;aspect-ratio:.63;border-radius:18px;overflow:hidden}.card-info h3{font-size:20px;font-weight:800;letter-spacing:-.04em}.card-info p{margin-top:8px;color:#94a3b8;font-size:13px;font-weight:500}.card-info button{width:100%;height:50px;margin-top:auto;border-radius:15px;background:#2f70e9;color:#fff;font-size:17px;font-weight:800}.linked-card hr{height:1px;margin:23px 0;border:0;background:#dfe6f1}.currency-list{display:grid;gap:12px;margin-top:18px}.currency-row{display:grid;grid-template-columns:42px 1fr auto 10px;align-items:center;gap:12px;padding:16px;border-radius:17px;background:#f6f8fc;text-align:left}.currency-row>span{display:grid;width:38px;height:38px;place-items:center;border-radius:50%;color:#fff;font-size:16px;font-weight:700}.currency-row>b{font-size:18px;font-weight:800}.currency-row>strong{grid-column:3;align-self:end;font-size:19px;font-weight:800}.currency-row>small{grid-column:3;color:#9aa8bd;font-size:12px;font-weight:600}.currency-row>em{grid-column:4;grid-row:1/3;color:#c5cfdd;font-size:20px;font-style:normal}.wallet-sheet-backdrop{position:fixed;z-index:100;inset:0;display:flex;align-items:flex-end;justify-content:center;background:rgba(23,35,60,.4)}.wallet-sheet{width:min(100%,430px);padding:12px 20px 28px;border-radius:24px 24px 0 0;background:#fff}.sheet-handle{display:block;width:36px;height:4px;margin:0 auto 17px;border-radius:9px;background:#d5ddeb}.wallet-sheet h2{font-size:20px;font-weight:800}.wallet-sheet-head{display:flex;align-items:flex-start;justify-content:space-between}.sheet-close{padding:4px 8px;color:#64748b;font-size:22px}.auto-charge-preview{margin-top:18px;padding:18px;border-radius:18px;background:#f7f9fd}.auto-charge-preview.compact{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:14px;padding:14px 15px;border:1px solid #e0e8f4;background:#f8fbff}.auto-charge-preview h3{font-size:17px;font-weight:800}.auto-charge-preview.compact h3{font-size:14px;font-weight:800}.auto-charge-preview p{margin-top:7px;color:#8b98ad;font-size:12px;font-weight:500;line-height:1.45}.auto-charge-preview.compact p{margin-top:4px;font-size:12px}.auto-charge-preview dl{display:grid;gap:12px;margin-top:17px}.auto-charge-preview dl div{display:flex;align-items:center;justify-content:space-between}.auto-charge-preview dt{color:#8b98ad;font-size:13px;font-weight:600}.auto-charge-preview dd{color:#111827;font-size:16px;font-weight:800}.auto-charge-preview button{width:100%;height:43px;margin-top:16px;border-radius:12px;background:#2f70e9;color:#fff;font-size:14px;font-weight:700}.auto-charge-preview.compact button{width:auto;height:34px;margin-top:0;padding:0 14px;border-radius:999px;background:#eaf3ff;color:#2167e8;font-size:12px;font-weight:800}.wallet-sheet>p{margin-top:7px;color:#8290a6;font-size:12px}.withdraw-mode-tabs{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin-top:16px}.withdraw-mode-tabs button{height:38px;border:1px solid #dce4f1;border-radius:11px;background:#fff;color:#63718a;font-size:12px;font-weight:700}.withdraw-mode-tabs button.active{border-color:#193d8c;background:#193d8c;color:#fff}.empty-inline-hint{margin-top:8px;color:#94a3b8;font-size:11px}.wallet-sheet label{display:block;margin-top:18px;color:#63718a;font-size:12px;font-weight:600}.wallet-sheet select,.amount-field{width:100%;height:50px;margin-top:8px;padding:0 14px;border:1px solid #dce4f1;border-radius:13px;background:#fff;color:#182849;font-size:14px;font-weight:600}.amount-field{display:flex;align-items:center}.amount-field input{width:100%;font-size:19px;font-weight:700;outline:0}.amount-field b{color:#7d8ba2}.quick-amounts{display:flex;flex-wrap:wrap;gap:8px;margin-top:9px}.quick-amounts button{padding:7px 12px;border:1px solid #e2e5ea;border-radius:99px;background:#f4f5f7;color:#111827;font-size:12px;font-weight:700}.confirm-transfer{width:100%;height:52px;margin-top:24px;border-radius:14px;background:#193d8c;color:#fff;font-size:15px;font-weight:800}.auto-modal-backdrop,.confirm-backdrop{position:fixed;z-index:130;inset:0;display:flex;align-items:center;justify-content:center;padding:20px;background:rgba(23,35,60,.42)}.auto-modal,.confirm-modal{width:min(100%,360px);padding:20px;border-radius:22px;background:#fff;box-shadow:0 18px 44px rgba(17,24,39,.22)}.auto-modal-head{display:flex;align-items:center;justify-content:space-between}.auto-modal-head h2{font-size:19px;font-weight:800}.auto-modal-head button{width:30px;height:30px;border-radius:50%;background:#f1f4f9;color:#64748b;font-size:19px}.auto-modal label{display:block;margin-top:18px;color:#63718a;font-size:12px;font-weight:700}.modal-field{display:flex;align-items:center;gap:8px;height:48px;margin-top:8px;padding:0 14px;border:1px solid #dce4f1;border-radius:14px;background:#f9fbff;color:#111827}.modal-field input{min-width:0;flex:1;font-size:17px;font-weight:800;text-align:right;outline:0}.modal-field span,.modal-field b{color:#7d8ba2;font-size:13px;font-weight:700}.auto-modal p{margin-top:14px;color:#8b98ad;font-size:12px;line-height:1.45}.modal-save{width:100%;height:48px;margin-top:18px;border-radius:14px;background:#2f70e9;color:#fff;font-size:15px;font-weight:800}.confirm-modal{text-align:center}.confirm-icon{display:grid;width:56px;height:56px;margin:0 auto 14px;place-items:center;border-radius:18px;background:#eef4ff;font-size:24px}.confirm-modal h2{font-size:20px;font-weight:800;letter-spacing:-.04em}.confirm-modal p{margin-top:10px;color:#64748b;font-size:13px;font-weight:500;line-height:1.5}.confirm-actions{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:22px}.confirm-actions button{height:48px;border-radius:14px;background:#f1f5f9;color:#64748b;font-size:15px;font-weight:800}.confirm-actions .danger{background:#ffe8e8;color:#ef4444}.wallet-toast{position:fixed;bottom:78px;left:50%;z-index:120;width:max-content;max-width:calc(100% - 40px);transform:translateX(-50%);padding:11px 15px;border-radius:99px;background:#172644;color:#fff;font-size:12px;box-shadow:0 5px 18px rgba(17,24,39,.2)}.sheet-enter-active,.sheet-leave-active{transition:opacity .2s}.sheet-enter-active .wallet-sheet,.sheet-leave-active .wallet-sheet{transition:transform .25s}.sheet-enter-from,.sheet-leave-to{opacity:0}.sheet-enter-from .wallet-sheet,.sheet-leave-to .wallet-sheet{transform:translateY(100%)}@media(max-width:380px){.wallet-balance strong{font-size:36px}.travel-card-box{grid-template-columns:112px 1fr}.saving-summary{grid-template-columns:1fr}.chart-item>i{width:28px}.chart-item>b{font-size:9px}}
.saving-chart{margin-top:20px}.bar-wrap>b{font-size:11px;font-weight:700;color:#2563eb}.bar-wrap>b.current{padding:3px 7px;border-radius:999px;background:#eaf2ff!important;color:#1768f2}.bar-wrap>b.negative{color:#ef4444}.bar-wrap>b.zero{color:#4b5563}.bar-wrap .zero-line{position:absolute;left:2px;right:2px;height:1px;background:#e3e9f3}.chart-item i{position:absolute;left:50%;width:34px;border-radius:10px;transform:translateX(-50%);background:linear-gradient(180deg,#2f70e9,#1d54c6)}.chart-item i.current{background:linear-gradient(180deg,#ffcf5a,#ffb020);outline:4px solid #fff6dd}.chart-item i.negative{background:linear-gradient(180deg,#ef4444,#c92e2e)}.chart-item i.zero{background:#4b5563}.chart-item>span.current{color:#111827}.chart-item>span.negative{color:#ef4444}.chart-item>span.zero{color:#111827}.card-info{min-width:0;display:flex;flex-direction:column;align-items:flex-start}.card-info h3{font-size:18px;line-height:1.22;word-break:keep-all}.card-label{display:inline-block;margin-bottom:5px;color:#94a3b8;font-size:11px;font-weight:700}
.bar-wrap>b.disabled{color:#b6c1d1}.chart-item i.disabled{background:#e4eaf3;outline:none}.chart-item>span.disabled{color:#b6c1d1}.saving-hint{margin-top:16px;padding:11px 13px;border-radius:13px;background:#f5f8fc;color:#8290a6;font-size:12px;font-weight:600;text-align:center}.list-empty{margin-top:18px;padding:20px;border-radius:14px;background:#f7f9fd;color:#9aa8bd;font-size:12px;font-weight:600;text-align:center}
.currency-head{display:flex;align-items:center;justify-content:space-between}.currency-title{color:#64748b;font-size:17px;font-weight:800;letter-spacing:-.02em}.currency-head button{color:#1768f2;font-size:13px;font-weight:800}.currency-list{gap:8px;margin-top:12px}.currency-row{grid-template-columns:34px minmax(0,1fr) auto!important;grid-template-rows:auto auto!important;gap:3px 10px!important;padding:11px 12px!important;border:1px solid #edf2f8;border-radius:14px!important;background:#f8fbff!important}.currency-row .flag{grid-row:1/3;display:grid!important;width:32px!important;height:32px!important;place-items:center!important;border-radius:50%!important;background:#fff!important;font-size:18px!important;box-shadow:0 0 0 1px #e4ebf5!important}.currency-row>b{grid-column:2!important;display:flex;min-width:0;align-items:baseline;gap:5px;font-size:15px!important;font-weight:800!important}.currency-row>b small{overflow:hidden;color:#8b98ad;font-size:11px;font-weight:600;text-overflow:ellipsis;white-space:nowrap}.currency-row>em{grid-column:2!important;grid-row:2!important;color:#94a3b8!important;font-size:11px!important;font-style:normal!important;font-weight:600!important}.currency-row>strong{grid-column:3!important;grid-row:1!important;align-self:end!important;font-size:16px!important;font-weight:800!important}.currency-row>.krw{grid-column:3!important;grid-row:2!important;color:#9aa8bd!important;font-size:11px!important;font-weight:600!important;text-align:right}
.travel-card-meta{display:grid;width:100%;gap:5px;margin-top:9px}.travel-card-meta div{display:grid;grid-template-columns:44px minmax(0,1fr);align-items:center;gap:8px}.travel-card-meta dt{color:#94a3b8;font-size:11px;font-weight:700}.travel-card-meta dd{min-width:0;color:#475569;font-size:12px;font-weight:600;line-height:1.35;word-break:keep-all}.card-info p{display:none}
.chart-item{border:0;background:transparent;text-align:center}.chart-item:disabled{cursor:default}.monthly-detail-sheet{display:flex;flex-direction:column;max-height:82vh;padding-bottom:0}.monthly-detail-sheet>.sheet-handle,.monthly-detail-sheet>.monthly-detail-head,.monthly-detail-sheet>.monthly-detail-total,.monthly-detail-sheet>.monthly-detail-summary{flex:none}.monthly-detail-head{display:flex;align-items:flex-start;justify-content:space-between}.monthly-detail-head p{color:#1768f2;font-size:13px;font-weight:800}.monthly-detail-head h2{margin-top:3px;font-size:22px;font-weight:800;letter-spacing:-.04em}.monthly-detail-head button{padding:4px 8px;color:#64748b;font-size:22px}.monthly-detail-total{margin-top:18px;padding:18px;border-radius:18px;background:#f4f8ff}.monthly-detail-total span{display:block;color:#64748b;font-size:12px;font-weight:700}.monthly-detail-total strong{display:block;margin-top:6px;color:#111827;font-size:30px;font-weight:800;letter-spacing:-.04em}.monthly-detail-summary{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:12px}.monthly-detail-summary div{padding:14px;border-radius:16px;background:#f8fbff;border:1px solid #e4ebf5}.monthly-detail-summary span{display:block;color:#8b98ad;font-size:12px;font-weight:700}.monthly-detail-summary b{display:block;margin-top:5px;color:#1768f2;font-size:18px;font-weight:800}.monthly-detail-summary div:last-child b{color:#ef4444}.monthly-ledger-list{flex:1 1 auto;min-height:0;display:grid;align-content:start;gap:10px;margin-top:16px;padding-bottom:34px;overflow-y:auto}.monthly-ledger-list>p{padding:18px;border-radius:16px;background:#f8fbff;color:#8b98ad;font-size:13px;font-weight:700;text-align:center}.monthly-ledger-list article{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:14px;border-radius:15px;background:#fff;border:1px solid #e4ebf5}.monthly-ledger-list article b{display:block;font-size:14px;font-weight:800;color:#111827}.monthly-ledger-list article span{display:block;margin-top:4px;color:#94a3b8;font-size:11px;font-weight:600}.monthly-ledger-list article strong{color:#1768f2;font-size:15px;font-weight:800;white-space:nowrap}.monthly-ledger-list article strong.out{color:#ef4444}
.wallet-page{word-break:keep-all}.wallet-page h1,.wallet-page h2,.wallet-page h3{text-wrap:balance}.wallet-page p{text-wrap:pretty}
.chart-amount{display:block;font-size:11px;font-weight:700;color:#2563eb;white-space:nowrap}.chart-amount.current,.chart-item>span.current{color:#b8860b}.chart-amount.negative,.chart-item>span.negative{color:#ef4444}.chart-amount.zero,.chart-item>span.zero{color:#4b5563}.chart-amount.disabled,.chart-item>span.disabled{color:#b6c1d1}
.header-wordmark{display:block;width:88px;height:auto;object-fit:contain}
.wallet-header h1{font-size:17px;font-weight:400;color:#29466f;letter-spacing:normal}
</style>
