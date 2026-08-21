import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import kbTravelersTosimiImage from '@/assets/cards/kb-travelers-tosimi.png'
import {
  chargeWallet,
  deleteWalletAutoSaving,
  estimateWalletExchange,
  fetchUserTravelCardOptions,
  fetchWalletAccountOptions,
  fetchWalletAccounts,
  fetchWalletAutoSaving,
  fetchWalletAutoSavingLogs,
  fetchWalletCurrencies,
  fetchWalletForeignBalances,
  fetchWalletLedgers,
  fetchWalletMain,
  fetchWalletMonthlySavingDetail,
  fetchWalletTravelCardBalances,
  fetchWalletWithdrawOptions,
  linkWalletAccount,
  linkWalletTravelCard,
  saveWalletAutoSaving,
  sellWalletExchange,
  setWalletPrimaryAccount,
  topupWalletTravelCard,
  unlinkWalletAccount,
  unlinkWalletTravelCard,
  withdrawWallet,
} from '@/api/wallet'

const STORAGE_KEY = 'tripass-trip-wallet'
const DEFAULT_TARGET_AMOUNT = 5_000_000
const CARD_IMAGE_BASE = '/images/travel-cards'

const cardImages = (...files) => files.map(file => `${CARD_IMAGE_BASE}/${file}`)

const initialMonthlySavings = [
  { month: '5월', monthKey: '2026-05', amount: 0, available: true },
  { month: '6월', monthKey: '2026-06', amount: 0, available: true },
  { month: '7월', monthKey: '2026-07', amount: 0, available: true },
  { month: '8월', monthKey: '2026-08', amount: 0, available: true, isCurrent: true },
]

// backend/src/main/resources/sql/travel_card_seed.sql 의 13개 카드와 1:1로 대응한다.
// 실제 API가 카드 이미지를 내려주지 않으므로, cardName으로 매칭해 이 목록의 이미지를 붙여 쓴다.
const defaultTravelCardOptions = [
  { id: 1, travelCardId: 1, userTravelCardId: 1, issuer: 'KB국민카드', name: '트래블러스 체크카드', number: '1234 · 5678 · **** · 3456', brand: 'TRAVELERS', color: '#153783', images: [kbTravelersTosimiImage] },
  { id: 2, travelCardId: 2, userTravelCardId: 2, issuer: '하나카드', name: '트래블로그 체크카드', number: '4567 · 1234 · **** · 9081', brand: 'TRAVLOG', color: '#12a997', images: cardImages('travlog_1.png', 'travlog_2.png', 'travlog_3.png') },
  { id: 3, travelCardId: 3, userTravelCardId: 3, issuer: '하나카드', name: '트래블GO 체크카드', number: '2231 · 5567 · **** · 7789', brand: 'TRAVLGO', color: '#f0b429', images: cardImages('travlgo_1.png', 'travlgo_2.png') },
  { id: 4, travelCardId: 4, userTravelCardId: 4, issuer: '우리카드', name: '위비트래블 체크카드', number: '3345 · 8890 · **** · 4412', brand: 'WIBEE TRAVEL', color: '#7cb9e8', images: cardImages('wibee_travel_1.png', 'wibee_travel_2.png') },
  { id: 5, travelCardId: 5, userTravelCardId: 5, issuer: '신한카드', name: 'SOL트래블 체크카드', number: '9012 · 7788 · **** · 1024', brand: 'SOL Travel', color: '#1768f2', images: cardImages('sol_travel_1.png', 'sol_travel_2.png', 'sol_travel_3.png', 'sol_travel_4.png', 'sol_travel_5.png', 'sol_travel_6.png') },
  { id: 6, travelCardId: 6, userTravelCardId: 6, issuer: '우리카드', name: 'FC EXPRESS 체크', number: '5567 · 2234 · **** · 9981', brand: 'FC EXPRESS', color: '#334155', images: cardImages('fc_express.png') },
  { id: 7, travelCardId: 7, userTravelCardId: 7, issuer: '우리카드', name: '위비트래블 J 체크카드', number: '6678 · 1123 · **** · 5567', brand: 'WIBEE TRAVEL J', color: '#f472b6', images: cardImages('wibee_travel_J_1.png', 'wibee_travel_J_2.png') },
  { id: 8, travelCardId: 8, userTravelCardId: 8, issuer: '신한카드', name: 'Change-Up 체크', number: '7789 · 4456 · **** · 2234', brand: 'Change-Up', color: '#0ea5e9', images: cardImages('change-up.webp') },
  { id: 9, travelCardId: 9, userTravelCardId: 9, issuer: '신한카드', name: 'SOL트래블J 체크', number: '8890 · 5567 · **** · 3345', brand: 'SOL Travel', color: '#1768f2', images: cardImages('sol_travel_J.png') },
  { id: 10, travelCardId: 10, userTravelCardId: 10, issuer: '신한카드', name: 'SOL트립앤J 체크', number: '4456 · 7789 · **** · 6678', brand: 'SOL Travel', color: '#2563eb', images: cardImages('sol_trip&J_1.png', 'sol_trip&J_2.png', 'sol_trip&J_3.png') },
  { id: 11, travelCardId: 11, userTravelCardId: 11, issuer: '신한카드', name: 'SOL트립앤샵 체크', number: '5567 · 8890 · **** · 7789', brand: 'SOL Travel', color: '#4f46e5', images: cardImages('sol_trip&shop_1.png', 'sol_trip&shop_2.png', 'sol_trip&shop_3.png') },
  { id: 12, travelCardId: 12, userTravelCardId: 12, issuer: '하나카드', name: '카카오페이 트래블로그 체크카드', number: '2234 · 9981 · **** · 1123', brand: 'TRAVLOG', color: '#ffd429', images: cardImages('kakaopay_travlog_1.png', 'kakaopay_travlog_2.png') },
  { id: 13, travelCardId: 13, userTravelCardId: 13, issuer: '하나카드', name: '삼성월렛 하나 트래블로그 체크카드', number: '9981 · 6678 · **** · 8890', brand: 'TRAVLOG', color: '#12a997', images: cardImages('samsung_wallet_travlog.gif') },
]

const currencyFlags = {
  EUR: '🇪🇺',
  JPY: '🇯🇵',
  CHF: '🇨🇭',
  USD: '🇺🇸',
  GBP: '🇬🇧',
  CNY: '🇨🇳',
}

export const transactionTitleMap = {
  CHARGE: '채우기',
  WITHDRAW: '빼기',
  CARD_TRANSFER: '트래블카드 송금',
  CARD_TOPUP: '트래블카드 충전',
  EXCHANGE_BUY: '외화 환전',
  EXCHANGE_SELL: '외화 재환전',
  MISSION_REWARD: '미션 보상',
  REFUND: '환불',
  ADJUST: '시스템 보정',
}

function savedData() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') } catch { return null }
}

function toNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

function toPositiveNumber(value, fallback) {
  const number = toNumber(value)
  return number > 0 ? number : fallback
}

function clampPercent(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return 0
  return Math.max(0, Math.min(100, Math.round(number)))
}

function toMonthLabel(value) {
  if (!value) return ''
  const month = String(value).includes('-') ? Number(String(value).split('-')[1]) : Number(value)
  return Number.isFinite(month) && month > 0 ? `${month}월` : String(value)
}

export function toDayLabel(value) {
  if (!value) return ''
  const date = new Date(normalizeDateTime(value))
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10)
  return `${date.getMonth() + 1}월 ${date.getDate()}일`
}

export function toTimeLabel(value) {
  if (!value) return ''
  const date = new Date(normalizeDateTime(value))
  if (Number.isNaN(date.getTime())) return ''
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

export function toDateKey(value) {
  if (!value) return ''
  return normalizeDateTime(value).slice(0, 10)
}

function normalizeDateTime(value) {
  if (Array.isArray(value)) {
    const [year, month = 1, day = 1, hour = 0, minute = 0, second = 0] = value
    return [
      String(year).padStart(4, '0'),
      String(month).padStart(2, '0'),
      String(day).padStart(2, '0'),
    ].join('-') + `T${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
  }

  return String(value)
}

function parseAmount(value) {
  return Number(String(value ?? '').replace(/[^0-9.-]/g, '')) || 0
}

function createIdempotencyKey(prefix = 'wallet') {
  if (globalThis.crypto?.randomUUID) return `${prefix}-${globalThis.crypto.randomUUID()}`
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function findCardVisual(travelCardId, cardName) {
  // travelCardId는 travel_cards.id와 1:1로 대응하는 안정적인 값이라 이름 텍스트보다 우선 매칭한다.
  // (user_travel_cards에 저장된 card_name은 카드사마다 표기가 달라 이름만으로는 매칭이 깨질 수 있다.)
  if (travelCardId != null) {
    const byId = defaultTravelCardOptions.find(option => option.travelCardId === Number(travelCardId))
    if (byId) return byId
  }

  if (!cardName) return defaultTravelCardOptions[0]
  // 이름이 서로를 포함하는 카드가 있어(예: "트래블로그 체크카드"↔"카카오페이 트래블로그 체크카드")
  // 부분 일치보다 정확히 같은 이름을 먼저 찾는다.
  return defaultTravelCardOptions.find(option => option.name === cardName)
    ?? defaultTravelCardOptions.find(option => cardName.includes(option.name) || option.name.includes(cardName))
    ?? defaultTravelCardOptions[0]
}

function normalizeAccount(account) {
  return {
    id: account.accountId ?? account.id,
    accountId: account.accountId ?? account.id,
    logo: (account.bankName || account.accountName || '계').slice(0, 1),
    name: account.accountName || account.bankName || '연동 계좌',
    bankName: account.bankName || '',
    type: account.primary ? '주계좌' : '연동계좌',
    number: account.maskedAccountNumber || account.number || '',
    balance: toNumber(account.balance ?? account.withdrawableAmount),
    withdrawableAmount: toNumber(account.withdrawableAmount ?? account.balance),
    isLinked: account.linked !== false,
    linked: account.linked !== false,
    isPrimary: account.primary === true || account.isPrimary === true,
  }
}

function normalizeRecentAccount(item) {
  return {
    recipientId: item.recipientId ?? item.id,
    bankName: item.bankName || '',
    accountNumber: item.maskedAccountNumber || item.accountNumber || '',
    accountHolderName: item.accountHolderName || item.holderName || '',
    lastUsedAt: item.lastUsedAt || item.usedAt || null,
  }
}

function normalizeForeignBalance(item) {
  const code = item.currencyCode || item.code
  return {
    code,
    currencyCode: code,
    name: item.currencyName || item.name || code,
    currencyName: item.currencyName || item.name || code,
    symbol: item.symbol || '',
    flag: item.flag || currencyFlags[code] || '🌐',
    amount: toNumber(item.balanceAmount ?? item.amount),
    balanceAmount: toNumber(item.balanceAmount ?? item.amount),
    krwAmount: toNumber(item.krwEstimatedAmount ?? item.krwAmount),
    krwEstimatedAmount: toNumber(item.krwEstimatedAmount ?? item.krwAmount),
    rate: toNumber(item.rate ?? item.appliedExchangeRate),
  }
}

function normalizeTravelCard(card) {
  if (!card) return null
  const visual = findCardVisual(card.travelCardId, card.cardName || card.name)
  return {
    id: card.userTravelCardId ?? card.walletTravelCardId ?? card.travelCardId ?? card.id,
    walletTravelCardId: card.walletTravelCardId ?? null,
    userTravelCardId: card.userTravelCardId ?? null,
    travelCardId: card.travelCardId ?? card.id ?? null,
    issuer: card.issuerName || card.issuer || visual.issuer,
    issuerName: card.issuerName || card.issuer || visual.issuer,
    name: card.cardName || card.name || visual.name,
    cardName: card.cardName || card.name || visual.name,
    number: card.maskedCardNumber || card.number || visual.number,
    maskedCardNumber: card.maskedCardNumber || card.number || visual.number,
    brand: card.brandName || card.brand || visual.brand,
    color: card.cardColor || card.color || visual.color,
    images: card.images?.length ? card.images : visual.images,
    frozenIndex: card.frozenIndex ?? null,
    status: card.status,
  }
}

function normalizeLedger(item) {
  const direction = item.direction || (toNumber(item.amount) >= 0 ? 'IN' : 'OUT')
  const amount = Math.abs(toNumber(item.amount))
  return {
    ledgerId: item.ledgerId ?? item.id,
    id: item.ledgerId ?? item.id,
    direction,
    transactionType: item.transactionType,
    transferMethod: item.transferMethod,
    amount,
    signedAmount: direction === 'OUT' ? -amount : amount,
    balanceBefore: toNumber(item.balanceBefore),
    balanceAfter: toNumber(item.balanceAfter),
    memo: item.memo,
    accountName: item.accountName || '',
    createdAt: normalizeDateTime(item.createdAt),
  }
}

function normalizeAutoSavingLog(item) {
  return {
    id: item.id,
    status: item.status,
    isSuccess: item.status === 'SUCCESS',
    amount: toNumber(item.amount),
    reason: item.reason || '',
    executedAt: normalizeDateTime(item.executedAt),
  }
}

export const useTripWalletStore = defineStore('tripWallet', () => {
  const saved = savedData()
  const walletId = ref(saved?.walletId ?? null)
  const isLoading = ref(false)
  const isMonthlyDetailLoading = ref(false)
  const errorMessage = ref('')
  const balance = ref(Number(saved?.balance ?? 0))
  const targetAmount = ref(toPositiveNumber(saved?.targetAmount, DEFAULT_TARGET_AMOUNT))
  const emergencyAmount = ref(Number(saved?.emergencyAmount ?? 0))
  const goalAvailableAmount = ref(Number(saved?.goalAvailableAmount ?? 0))
  const externalChargeAmount = ref(Number(saved?.externalChargeAmount ?? 0))
  const overTargetSpentAmount = ref(Number(saved?.overTargetSpentAmount ?? 0))
  const apiSavingRate = ref(saved?.apiSavingRate ?? null)
  const linkedAccount = ref(saved?.linkedAccount ?? '')
  const linkedAccounts = ref(saved?.linkedAccounts ?? [])
  const accountOptions = ref(saved?.accountOptions ?? [])
  const withdrawRegisteredAccounts = ref([])
  const withdrawRecentAccounts = ref([])
  const autoCharge = ref(saved?.autoCharge ?? { enabled: false, day: 25, amount: 0, accountId: null })
  const monthlySavings = ref(saved?.monthlySavings ?? initialMonthlySavings)
  const transactions = ref(saved?.transactions ?? [])
  const ledgers = ref(saved?.ledgers ?? [])
  const isTravelCardLinked = ref(saved?.isTravelCardLinked ?? false)
  const travelCard = ref(saved?.travelCard ?? normalizeTravelCard(defaultTravelCardOptions[0]))
  const foreignBalances = ref(saved?.foreignBalances ?? [])
  const travelCardOptions = ref(saved?.travelCardOptions ?? defaultTravelCardOptions)
  const currencies = ref(saved?.currencies ?? [])
  const cardFreezeIndex = ref(saved?.cardFreezeIndex ?? {})
  const mainScrollY = ref(0)
  const autoSavingLogs = ref([])

  const userTravelCards = computed(() => travelCardOptions.value)
  const monthlyDeposits = computed(() => monthlySavings.value.map(item => item.amount))
  const monthDeposit = computed(() => monthlySavings.value.find(item => item.isCurrent)?.amount ?? 0)
  const savingRate = computed(() => {
    if (apiSavingRate.value !== null && apiSavingRate.value !== undefined) return clampPercent(apiSavingRate.value)
    if (targetAmount.value <= 0) return 0
    return clampPercent(balance.value / targetAmount.value * 100)
  })

  function applyWalletMain(data) {
    const primaryAccount = data?.primaryAccount
    const linkedTravelCard = data?.travelCard
    const normalizedTravelCard = normalizeTravelCard(linkedTravelCard)

    walletId.value = data?.walletId ?? walletId.value
    balance.value = toNumber(data?.balanceAmount)
    targetAmount.value = toPositiveNumber(data?.targetAmount, DEFAULT_TARGET_AMOUNT)
    emergencyAmount.value = toNumber(data?.emergencyAmount)
    goalAvailableAmount.value = toNumber(data?.goalAvailableAmount ?? Math.min(balance.value, targetAmount.value))
    externalChargeAmount.value = toNumber(data?.externalChargeAmount)
    overTargetSpentAmount.value = toNumber(data?.overTargetSpentAmount)
    apiSavingRate.value = data?.savingRate ?? null
    linkedAccount.value = primaryAccount
      ? `${primaryAccount.bankName ?? ''} ${primaryAccount.maskedAccountNumber ?? ''}`.trim()
      : ''

    if (Array.isArray(data?.linkedAccounts)) {
      linkedAccounts.value = data.linkedAccounts.map(normalizeAccount)
    } else if (primaryAccount) {
      const normalized = normalizeAccount(primaryAccount)
      linkedAccounts.value = [normalized, ...linkedAccounts.value.filter(account => account.accountId !== normalized.accountId)]
    }

    if (Array.isArray(data?.monthlySavings) && data.monthlySavings.length > 0) {
      monthlySavings.value = data.monthlySavings.map(item => ({
        month: toMonthLabel(item.month),
        monthKey: item.month,
        amount: toNumber(item.savedAmount),
        available: item.available !== false,
        isCurrent: item.currentMonth === true,
      }))
    }

    isTravelCardLinked.value = linkedTravelCard?.linked === true || Boolean(normalizedTravelCard?.walletTravelCardId)
    if (normalizedTravelCard) {
      travelCard.value = {
        ...normalizedTravelCard,
        frozenIndex: travelCard.value?.frozenIndex ?? null,
      }
    }

    if (Array.isArray(data?.foreignBalances)) {
      foreignBalances.value = data.foreignBalances.map(normalizeForeignBalance)
    }
  }

  async function loadWalletMain() {
    isLoading.value = true
    errorMessage.value = ''
    try {
      applyWalletMain(await fetchWalletMain())
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '월렛 정보를 불러오지 못했어요.'
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function loadLedgers() {
    errorMessage.value = ''
    try {
      ledgers.value = (await fetchWalletLedgers() ?? []).map(normalizeLedger)
      return ledgers.value
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '월렛 내역을 불러오지 못했어요.'
      throw error
    }
  }

  async function loadAccounts() {
    errorMessage.value = ''
    try {
      linkedAccounts.value = (await fetchWalletAccounts() ?? []).map(normalizeAccount)
      return linkedAccounts.value
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '연동계좌를 불러오지 못했어요.'
      throw error
    }
  }

  async function loadAccountOptions() {
    errorMessage.value = ''
    try {
      accountOptions.value = (await fetchWalletAccountOptions() ?? []).map(normalizeAccount)
      return accountOptions.value
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '추가 가능한 계좌를 불러오지 못했어요.'
      throw error
    }
  }

  async function loadWithdrawOptions() {
    errorMessage.value = ''
    try {
      const data = await fetchWalletWithdrawOptions()
      withdrawRegisteredAccounts.value = (data?.registeredAccounts ?? []).map(normalizeAccount)
      withdrawRecentAccounts.value = (data?.recentAccounts ?? []).map(normalizeRecentAccount)
      return { registeredAccounts: withdrawRegisteredAccounts.value, recentAccounts: withdrawRecentAccounts.value }
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '출금 가능한 계좌를 불러오지 못했어요.'
      throw error
    }
  }

  async function addAccount(accountId, primary = false) {
    const data = await linkWalletAccount({ accountId, isPrimary: primary })
    await Promise.all([loadAccounts(), loadAccountOptions()])
    return data
  }

  async function removeAccount(accountId) {
    const data = await unlinkWalletAccount(accountId)
    linkedAccounts.value = linkedAccounts.value.filter(account => account.accountId !== accountId && account.id !== accountId)
    await loadAccountOptions().catch(() => {})
    return data
  }

  async function setPrimaryAccount(accountId) {
    const data = await setWalletPrimaryAccount(accountId)
    await loadAccounts()
    return data
  }

  async function loadAutoSaving() {
    errorMessage.value = ''
    try {
      const data = await fetchWalletAutoSaving()
      if (data) {
        autoCharge.value = {
          id: data.id,
          enabled: data.enabled === true,
          day: data.dayOfMonth ?? data.day ?? autoCharge.value.day,
          amount: toNumber(data.amount),
          accountId: data.sourceAccountId ?? autoCharge.value.accountId,
          sourceAccountName: data.sourceAccountName,
          nextTransferDate: data.nextTransferDate,
        }
      }
      return autoCharge.value
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '자동 채우기 설정을 불러오지 못했어요.'
      throw error
    }
  }

  async function updateAutoCharge(payload) {
    // 출금 계좌는 항상 주계좌로 고정되어 서버에서 직접 결정하므로 요청에 포함하지 않는다.
    const next = { ...autoCharge.value, ...payload }
    const data = await saveWalletAutoSaving({
      amount: parseAmount(next.amount),
      dayOfMonth: Number(next.day ?? next.dayOfMonth),
      enabled: next.enabled !== false,
    })
    await loadAutoSaving()
    return data
  }

  async function disableAutoCharge() {
    const data = await deleteWalletAutoSaving()
    autoCharge.value = { ...autoCharge.value, enabled: false }
    return data
  }

  async function loadMonthlySavingDetail(month) {
    isMonthlyDetailLoading.value = true
    errorMessage.value = ''
    try {
      const data = await fetchWalletMonthlySavingDetail(month)
      return {
        month: data?.month,
        savedAmount: toNumber(data?.savedAmount),
        chargeAmount: toNumber(data?.chargeAmount),
        withdrawAmount: toNumber(data?.withdrawAmount),
        ledgers: (data?.ledgers ?? []).map(normalizeLedger),
      }
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '월별 저축 상세를 불러오지 못했어요.'
      throw error
    } finally {
      isMonthlyDetailLoading.value = false
    }
  }

  async function deposit(amount, sourceAccountId = null) {
    const normalizedSourceAccountId = Number.isFinite(Number(sourceAccountId)) ? Number(sourceAccountId) : null
    const accountId = normalizedSourceAccountId ?? linkedAccounts.value.find(account => account.isPrimary)?.accountId ?? linkedAccounts.value[0]?.accountId
    if (!accountId) return false
    const value = parseAmount(amount)
    if (value <= 0) return false

    await chargeWallet({ sourceAccountId: accountId, amount: value, idempotencyKey: createIdempotencyKey('charge') })
    await Promise.all([loadWalletMain(), loadAccounts(), loadLedgers()])
    return true
  }

  // mode: 'registered'(등록 계좌) | 'recent'(최근 계좌) | 'manual'(직접 입력).
  // 세 방식 모두 서버가 받는 요청 바디 형태가 서로 달라서(targetAccountId / recipientId /
  // bankCode·accountNumber·accountHolderName), 여기서 mode에 맞춰 조립한다.
  async function withdraw({
    mode = 'registered',
    amount,
    targetAccountId = null,
    recipientId = null,
    bankCode = '',
    bankName = '',
    accountNumber = '',
    accountHolderName = '',
  } = {}) {
    const value = parseAmount(amount)
    if (value <= 0 || value > balance.value) return false

    const idempotencyKey = createIdempotencyKey('withdraw')
    let payload

    if (mode === 'recent') {
      const normalizedRecipientId = Number.isFinite(Number(recipientId)) ? Number(recipientId) : null
      if (!normalizedRecipientId) return false
      payload = { recipientId: normalizedRecipientId, amount: value, idempotencyKey }
    } else if (mode === 'manual') {
      if (!bankCode || !accountNumber.trim() || !accountHolderName.trim()) return false
      payload = { bankCode, bankName, accountNumber: accountNumber.trim(), accountHolderName: accountHolderName.trim(), amount: value, idempotencyKey }
    } else {
      const normalizedTargetAccountId = Number.isFinite(Number(targetAccountId)) ? Number(targetAccountId) : null
      const accountId = normalizedTargetAccountId ?? linkedAccounts.value.find(account => account.isPrimary)?.accountId ?? linkedAccounts.value[0]?.accountId
      if (!accountId) return false
      payload = { targetAccountId: accountId, amount: value, idempotencyKey }
    }

    await withdrawWallet(payload)
    await Promise.all([loadWalletMain(), loadAccounts(), loadLedgers()])
    return true
  }

  async function loadTravelCardOptions() {
    errorMessage.value = ''
    try {
      const data = await fetchUserTravelCardOptions()
      travelCardOptions.value = (data || []).map(normalizeTravelCard).filter(Boolean)
      return travelCardOptions.value
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '보유 트래블카드를 불러오지 못했어요.'
      throw error
    }
  }

  async function linkTravelCard(card) {
    const data = await linkWalletTravelCard({
      travelCardId: card.travelCardId,
      userTravelCardId: card.userTravelCardId,
      maskedCardNumber: card.maskedCardNumber || card.number,
    })
    isTravelCardLinked.value = true
    travelCard.value = { ...card, walletTravelCardId: data?.walletTravelCardId ?? data?.id ?? card.walletTravelCardId }
    await loadWalletMain()
    return data
  }

  async function unlinkTravelCard() {
    const data = await unlinkWalletTravelCard()
    isTravelCardLinked.value = false
    foreignBalances.value = []
    await loadWalletMain().catch(() => {})
    return data
  }

  async function loadForeignBalances() {
    errorMessage.value = ''
    try {
      // /wallet/foreign-balances 는 최근 충전순으로 정렬되어 있어 우선 사용하고,
      // 실패했을 때만 통화코드순인 /wallet/travel-card/balances 로 대체한다.
      const walletBalances = await fetchWalletForeignBalances().catch(() => [])
      const cardBalances = await fetchWalletTravelCardBalances().catch(() => [])
      const source = walletBalances?.length ? walletBalances : cardBalances
      foreignBalances.value = (source ?? []).map(normalizeForeignBalance)
      return foreignBalances.value
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '외화 머니를 불러오지 못했어요.'
      throw error
    }
  }

  async function loadAutoSavingLogs() {
    const data = await fetchWalletAutoSavingLogs()
    autoSavingLogs.value = (data ?? []).map(normalizeAutoSavingLog)
    return autoSavingLogs.value
  }

  async function loadCurrencies() {
    errorMessage.value = ''
    try {
      currencies.value = (await fetchWalletCurrencies() ?? []).map(item => ({
        code: item.currencyCode || item.code,
        name: item.currencyName || item.name,
        symbol: item.symbol || '',
        unit: toNumber(item.unit) || 1,
      }))
      return currencies.value
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '통화 목록을 불러오지 못했어요.'
      throw error
    }
  }

  async function estimateExchange(payload) {
    return estimateWalletExchange(payload)
  }

  async function exchangeToTravelCard(payload) {
    const value = parseAmount(payload.krwAmount)
    const walletTravelCardId = payload.walletTravelCardId ?? travelCard.value?.walletTravelCardId
    if (!walletTravelCardId || value <= 0 || value > balance.value) return null

    const result = await topupWalletTravelCard({
      walletTravelCardId,
      currencyCode: payload.currencyCode,
      krwAmount: value,
      idempotencyKey: createIdempotencyKey('card-topup'),
    })
    await Promise.all([loadWalletMain(), loadForeignBalances(), loadLedgers()])
    return result
  }

  async function sellFromTravelCard(payload) {
    const foreignAmount = Number(String(payload.foreignAmount ?? '').replace(/,/g, '')) || 0
    const walletTravelCardId = payload.walletTravelCardId ?? travelCard.value?.walletTravelCardId
    if (!walletTravelCardId || foreignAmount <= 0) return null

    const result = await sellWalletExchange({
      walletTravelCardId,
      currencyCode: payload.currencyCode,
      foreignAmount,
      idempotencyKey: createIdempotencyKey('exchange-sell'),
    })
    await Promise.all([loadWalletMain(), loadForeignBalances(), loadLedgers()])
    return result
  }

  watch(() => ({
    walletId: walletId.value,
    balance: balance.value,
    targetAmount: targetAmount.value,
    emergencyAmount: emergencyAmount.value,
    apiSavingRate: apiSavingRate.value,
    linkedAccount: linkedAccount.value,
    linkedAccounts: linkedAccounts.value,
    accountOptions: accountOptions.value,
    autoCharge: autoCharge.value,
    monthlySavings: monthlySavings.value,
    transactions: transactions.value,
    ledgers: ledgers.value,
    isTravelCardLinked: isTravelCardLinked.value,
    travelCard: travelCard.value,
    foreignBalances: foreignBalances.value,
    travelCardOptions: travelCardOptions.value,
    currencies: currencies.value,
    cardFreezeIndex: cardFreezeIndex.value,
  }), value => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)), { deep: true })

  return {
    walletId,
    isLoading,
    isMonthlyDetailLoading,
    errorMessage,
    balance,
    targetAmount,
    emergencyAmount,
    goalAvailableAmount,
    externalChargeAmount,
    overTargetSpentAmount,
    linkedAccount,
    linkedAccounts,
    accountOptions,
    withdrawRegisteredAccounts,
    withdrawRecentAccounts,
    autoCharge,
    monthlySavings,
    monthlyDeposits,
    transactions,
    ledgers,
    monthDeposit,
    savingRate,
    isTravelCardLinked,
    travelCard,
    foreignBalances,
    travelCardOptions,
    userTravelCards,
    currencies,
    cardFreezeIndex,
    mainScrollY,
    autoSavingLogs,
    loadWalletMain,
    loadLedgers,
    loadAccounts,
    loadAccountOptions,
    loadWithdrawOptions,
    addAccount,
    removeAccount,
    setPrimaryAccount,
    loadAutoSaving,
    updateAutoCharge,
    disableAutoCharge,
    loadMonthlySavingDetail,
    deposit,
    withdraw,
    loadTravelCardOptions,
    linkTravelCard,
    unlinkTravelCard,
    loadForeignBalances,
    loadCurrencies,
    estimateExchange,
    exchangeToTravelCard,
    sellFromTravelCard,
    loadAutoSavingLogs,
  }
})
