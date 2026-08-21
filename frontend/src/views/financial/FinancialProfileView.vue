<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/api'
import { getCardInstitutions, linkCard } from '@/api/card'
import kbBankLogo from '@/assets/bank-logos/kb-user.jpg'
import nhBankLogo from '@/assets/bank-logos/nh-user.gif'
import wooriBankLogo from '@/assets/bank-logos/woori-bank.svg'
import hanaBankLogo from '@/assets/bank-logos/hana-bank.svg'
import shinhanBankLogo from '@/assets/bank-logos/shinhan-user.png'
import ibkBankLogo from '@/assets/bank-logos/ibk-bank-official.svg'
import kbankLogo from '@/assets/bank-logos/kbank-user.png'
import imBankLogo from '@/assets/bank-logos/im-user.jpg'
import tripassTransparentSymbol from '@/assets/brand/tripass-symbol-transparent-v2.png'
import kbCardLogo from '@/assets/card-company-logos/kb-card.jpg'
import hyundaiCardLogo from '@/assets/card-company-logos/hyundai-card.svg'
import samsungCardLogo from '@/assets/card-company-logos/samsung-card.png'
import nhCardLogo from '@/assets/card-company-logos/nh-card.jpg'
import bcCardLogo from '@/assets/card-company-logos/bc-card.png'
import shinhanCardLogo from '@/assets/card-company-logos/shinhan-card.gif'
import lotteCardLogo from '@/assets/card-company-logos/lotte-card.jpg'
import hanaCardLogo from '@/assets/card-company-logos/hana-card.png'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const isOnboarding = computed(() => route.query.onboarding === '1')

const cardCompanyLogos = {
  '0301': kbCardLogo,
  '0302': hyundaiCardLogo,
  '0303': samsungCardLogo,
  '0304': nhCardLogo,
  '0305': bcCardLogo,
  '0306': shinhanCardLogo,
  '0311': lotteCardLogo,
  '0313': hanaCardLogo,
}

function cardCompanyLogo(institution) {
  return cardCompanyLogos[institution?.organizationCode] || institution?.logoUrl || ''
}

const CARD_COMPANY_STEP = 9
const CARD_LOGIN_STEP = 10
const CARD_CONNECTING_STEP = 11
const CARD_COMPLETE_STEP = 12

const requestedStep = Number(route.query.step)
const step = ref(Number.isInteger(requestedStep) && requestedStep >= 0 && requestedStep <= CARD_COMPLETE_STEP ? requestedStep : 0)

const banks = ref([])
const banksError = ref(false)
const selectedBank = ref(null)
const loginId = ref('')
const password = ref('')
const linkedAccounts = ref([])
const connectionProgress = ref(0)
const connectionError = ref('')
const cardInstitutions = ref([])
const selectedCardInstitution = ref(null)
const cardLoginId = ref('')
const cardPassword = ref('')
const linkedCards = ref([])
const cardConnectionProgress = ref(0)
const cardConnectionError = ref('')
const isCardInstitutionLoading = ref(false)
const isCardConnecting = ref(false)

const bankLogoDomains = {
  '0004': 'kbstar.com',
  '0011': 'nonghyup.com',
  '0020': 'wooribank.com',
  '0081': 'kebhana.com',
  '0088': 'shinhan.com',
  '0003': 'ibk.co.kr',
  '0089': 'kbanknow.com',
  '0031': 'imbank.co.kr',
}

const localBankLogos = {
  '0004': kbBankLogo,
  '0011': nhBankLogo,
  '0020': wooriBankLogo,
  '0081': hanaBankLogo,
  '0088': shinhanBankLogo,
  '0003': ibkBankLogo,
  '0089': kbankLogo,
  '0031': imBankLogo,
}

function bankLogoUrl(bank) {
  const code = String(bank?.organizationCode || '').padStart(4, '0')
  if (localBankLogos[code]) return localBankLogos[code]
  if (bank?.logoUrl) return bank.logoUrl
  const domain = bankLogoDomains[code]
  return domain
    ? `https://www.google.com/s2/favicons?domain_url=https://${domain}&sz=128`
    : ''
}

function bankLogoClass(bank) {
  return `bank-logo-${String(bank?.organizationCode || '').padStart(4, '0')}`
}

function hideBrokenBankLogo(event) {
  event.currentTarget.style.display = 'none'
}

const salaryItems = ref([
  {
    id: 1,
    name: '플리보험 월급',
    payDay: '25',
    amount: '2,600,000',
    memo: '메모입니다',
    account: '하나은행 급여계좌 ****4821',
  },
])
const fixedExpenses = ref([
  { id: 1, name: '월세', day: '28', amount: '800,000', account: '하나은행 급여계좌 ****4821', memo: '관리비 포함', icon: '⌂', tone: 'red' },
  { id: 2, name: '통신비', day: '31', amount: '50,000', account: 'KB국민은행 ****1234', memo: '자동이체', icon: '▤', tone: 'blue' },
  { id: 3, name: '공과금 (전기·수도·가스)', day: '25', amount: '50,000', account: '신한은행 ****8888', memo: '전기 + 가스', icon: '●', tone: 'orange' },
])
const categories = ref([
  { id: 1, name: '식비', icon: '🍴', amount: '450,000', tone: 'violet' },
  { id: 2, name: '카페', icon: '☕', amount: '100,000', tone: 'orange' },
  { id: 3, name: '생활비', icon: '●', amount: '300,000', tone: 'green' },
  { id: 4, name: '쇼핑', icon: '♜', amount: '150,000', tone: 'pink' },
  { id: 5, name: '취미·여가', icon: '⌂', amount: '100,000', tone: 'orange' },
  { id: 6, name: '기타', icon: '…', amount: '100,000', tone: 'gray' },
])


const salaryTotal = computed(() => salaryItems.value.reduce((sum, item) => sum + toNumber(item.amount), 0))
const fixedTotal = computed(() => fixedExpenses.value.reduce((sum, item) => sum + toNumber(item.amount), 0))
const hasValidFixedExpenseNames = computed(() => fixedExpenses.value.every((item) => item.name.trim()))
const categoryTotal = computed(() => categories.value.reduce((sum, item) => sum + toNumber(item.amount), 0))

function toNumber(value) {
  return Number(String(value ?? '').replace(/[^0-9]/g, '')) || 0
}

function formatNumber(value) {
  return toNumber(value).toLocaleString('ko-KR')
}

function backFromInstitutionSelect(defaultStep) {
  if (route.query.from === 'asset') {
    router.push('/mypage/assets')
    return
  }
  step.value = defaultStep
}

function updateAmount(item, event) {
  item.amount = formatNumber(event.target.value)
}

async function goToBankSelect() {
  step.value = 2
  if (banks.value.length > 0) return
  banksError.value = false
  try {
    const res = await api.get('/accounts/institutions')
    banks.value = res.data.data
  } catch (e) {
    console.error('은행 목록 로딩 실패', e)
    banksError.value = true
  }
}

async function startConnection() {
  connectionError.value = ''
  step.value = 4
  connectionProgress.value = 20

  const enteredPassword = password.value
  password.value = ''

  try {
    connectionProgress.value = 50
    const res = await api.post('/accounts/codef/connect', {
      organizationCode: selectedBank.value.organizationCode,
      organizationName: selectedBank.value.institutionName,
      businessType: selectedBank.value.businessType,
      loginType: '1',
      loginId: loginId.value,
      password: enteredPassword,
    })
    linkedAccounts.value = res.data.data
    connectionProgress.value = 100
    setTimeout(() => { step.value = 5 }, 350)
  } catch (e) {
    const msg = e.response?.data?.message ?? '연동에 실패했어요. 아이디/비밀번호를 확인해주세요.'
    connectionError.value = msg
    step.value = 3
    connectionProgress.value = 0
  }
}

async function goToCardSelect() {
  step.value = CARD_COMPANY_STEP
  cardConnectionError.value = ''
  if (cardInstitutions.value.length > 0) return

  isCardInstitutionLoading.value = true
  try {
    const res = await getCardInstitutions()
    cardInstitutions.value = res.data?.data ?? []
  } catch (e) {
    console.error('카드사 목록 로딩 실패', e)
    cardConnectionError.value = e.response?.data?.message ?? '카드사 목록을 불러오지 못했어요.'
  } finally {
    isCardInstitutionLoading.value = false
  }
}

function goToCardLogin() {
  if (!selectedCardInstitution.value) return
  cardLoginId.value = ''
  cardPassword.value = ''
  cardConnectionError.value = ''
  step.value = CARD_LOGIN_STEP
}

async function startCardConnection() {
  if (!selectedCardInstitution.value || !cardLoginId.value.trim() || !cardPassword.value) return

  isCardConnecting.value = true
  cardConnectionError.value = ''
  cardConnectionProgress.value = 20
  step.value = CARD_CONNECTING_STEP

  const enteredCardPassword = cardPassword.value
  cardPassword.value = ''

  try {
    cardConnectionProgress.value = 50
    const res = await linkCard({
      organizationCode: selectedCardInstitution.value.organizationCode,
      organizationName: selectedCardInstitution.value.institutionName,
      cardType: 'CF',
      loginType: '1',
      loginId: cardLoginId.value.trim(),
      password: enteredCardPassword,
    })
    linkedCards.value = res.data?.data ?? []
    cardConnectionProgress.value = 100
    setTimeout(() => { step.value = CARD_COMPLETE_STEP }, 350)
  } catch (e) {
    console.error('카드 연동 실패', e)
    cardConnectionError.value = e.response?.data?.message ?? '카드 연동에 실패했어요. 아이디와 비밀번호를 확인해 주세요.'
    cardConnectionProgress.value = 0
    step.value = CARD_LOGIN_STEP
  } finally {
    isCardConnecting.value = false
  }
}

function resetCardConnection() {
  selectedCardInstitution.value = null
  cardLoginId.value = ''
  cardPassword.value = ''
  linkedCards.value = []
  cardConnectionError.value = ''
  cardConnectionProgress.value = 0
  goToCardSelect()
}

function cardTypeName(cardType) {
  return cardType === 'CHECK' ? '체크카드' : '신용카드'
}


function addSalary() {
  salaryItems.value.push({
    id: Date.now(),
    name: '추가 급여',
    payDay: '',
    amount: '',
    memo: '',
    account: '연결 계좌를 선택해 주세요',
  })
}

function addFixedExpense() {
  fixedExpenses.value.push({
    id: Date.now(),
    name: '',
    day: '',
    amount: '',
    account: '연결 계좌를 선택해 주세요',
    memo: '',
    icon: '+',
    tone: 'blue',
  })
}

function removeItem(items, id) {
  if (items.length <= 1) return
  const index = items.findIndex((item) => item.id === id)
  if (index >= 0) items.splice(index, 1)
}

function completeProfile() {
  if (route.query.from === 'asset') {
    router.replace('/mypage/assets')
    return
  }
  authStore.completeProfile()
  if (isOnboarding.value) {
    router.replace({ name: 'TripOnboarding' })
    return
  }
  router.replace('/')
}

function skipProfile() {
  if (isOnboarding.value) {
    router.replace({ name: 'TripOnboarding' })
    return
  }
  authStore.completeProfile()
  router.replace('/')
}

function skipOnboardingAccount() {
  router.replace({ name: 'TripOnboarding' })
}

onMounted(() => {
  if (step.value === 2) goToBankSelect()
  if (step.value === CARD_COMPANY_STEP) goToCardSelect()
})

</script>

<template>
  <main class="finance-profile">
    <button v-if="isOnboarding && step !== 5" type="button" class="onboarding-skip-floating" @click="skipOnboardingAccount">준비 화면</button>
    <section class="finance-shell" :class="{ 'is-tall': step === 7, 'is-navy-shell': step === 0 }">
      <template v-if="step === 0">
        <div class="intro-hero">
          <span class="hero-orbit" aria-hidden="true"></span>
          <p class="hero-mark">
            <img :src="tripassTransparentSymbol" alt="" aria-hidden="true">
            <span>TRIPASS</span>
          </p>
          <h1 class="hero-title">여행 준비를 위해<br>주거래 계좌를 연결해요</h1>
          <p class="hero-description">연결한 계좌의 거래내역을 분석해<br>여행자금 절약 코칭을 제공해요</p>
        </div>
        <div class="intro-content">
          <div class="intro-card">
            <span class="card-notch left" aria-hidden="true"></span>
            <span class="card-notch right" aria-hidden="true"></span>
            <div class="insight-pass-bar">
              <span class="pass-badge">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="#FFD466" stroke-width="1.8"/><path d="M8 12.5l2.5 2.5L16 9.5" stroke="#FFD466" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
                은행 선택 → 인증 → 계좌 선택
              </span>
              <span class="pass-step">STEP 1/3</span>
            </div>
            <div class="card-divider"></div>
            <div class="intro-list">
              <article>
                <span class="benefit-icon blue"><svg width="17" height="17" viewBox="0 0 24 24" fill="none"><rect x="4" y="4" width="16" height="16" rx="3" stroke="#2F6FED" stroke-width="1.8"/><rect x="9" y="9" width="6" height="6" fill="#2F6FED"/></svg></span>
                <div><strong>주거래 계좌 연결</strong><p>소비 패턴을 분석할 계좌를 선택해요</p></div>
              </article>
              <article>
                <span class="benefit-icon orange"><svg class="benefit-plane" width="17" height="17" viewBox="0 0 24 24" fill="currentColor"><path d="M21.7 11.2 14 7.1V3.6a2 2 0 0 0-4 0v3.5l-7.7 4.1a1.5 1.5 0 0 0-.8 1.3v1.2l8.5-2.2v4.2l-2.3 1.8v1l4.3-1 4.3 1v-1L14 15.7v-4.2l8.5 2.2v-1.2a1.5 1.5 0 0 0-.8-1.3Z"/></svg></span>
                <div><strong>여행 자금 코칭</strong><p>줄이면 좋은 소비를 알려드려요</p></div>
              </article>
              <article>
                <span class="benefit-icon violet"><svg width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="7" stroke="#8B5CF6" stroke-width="1.8"/><circle cx="12" cy="12" r="2.5" fill="#8B5CF6"/></svg></span>
                <div><strong>언제든 계좌 추가</strong><p>자산관리에서 더 연결할 수 있어요</p></div>
              </article>
            </div>
            <div class="intro-action">
              <button class="primary-button" @click="step = 1">계좌 연결하기</button>
              <button class="skip-button" @click="skipProfile">나중에 할게요</button>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="step === 1">
        <div class="intro-hero compact">
          <span class="hero-orbit" aria-hidden="true"></span>
          <header class="simple-header on-dark"><button @click="step = 0">‹</button><strong>은행 계좌 불러오기</strong></header>
          <h2 class="hero-heading">{{ authStore.user?.name ?? '고객' }}님이 쓰는<br>은행 계좌 정보를 불러올게요</h2>
        </div>
        <div class="intro-content">
          <div class="intro-card">
            <span class="card-notch left" aria-hidden="true"></span>
            <span class="card-notch right" aria-hidden="true"></span>
            <div class="insight-pass-bar">
              <span class="pass-badge">
                <svg class="pass-plane" width="12" height="12" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M21.7 11.2 14 7.1V3.6a2 2 0 0 0-4 0v3.5l-7.7 4.1a1.5 1.5 0 0 0-.8 1.3v1.2l8.5-2.2v4.2l-2.3 1.8v1l4.3-1 4.3 1v-1L14 15.7v-4.2l8.5 2.2v-1.2a1.5 1.5 0 0 0-.8-1.3Z"/></svg>
                TRIPASS BOARDING PASS
              </span>
              <span class="pass-step">LINK</span>
            </div>
            <div class="card-divider"></div>
            <div class="card-body">
              <p class="subcopy">연동할 금융사를 선택해 주세요</p>
              <div class="asset-link-options">
                <button class="load-bank-card" @click="goToBankSelect">
                  <span class="bank-building">▦</span>
                  <span><strong>은행 계좌 연동</strong><small>잔액과 입출금 내역을 불러와요</small></span>
                  <b>›</b>
                </button>
                <button class="load-bank-card" @click="goToCardSelect">
                  <span class="card-building">▰</span>
                  <span><strong>카드 연동</strong><small>가맹점명과 카드 결제내역을 불러와요</small></span>
                  <b>›</b>
                </button>
              </div>
              <div class="security-note"><span>▣</span><div><strong>안전하게 연결해요</strong><p>인증 정보는 연결 과정에서만 사용됩니다</p></div></div>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="step === 2">
        <div class="intro-hero compact">
          <span class="hero-orbit" aria-hidden="true"></span>
          <header class="simple-header on-dark"><button @click="backFromInstitutionSelect(1)">‹</button><strong>은행 선택</strong></header>
          <h2 class="hero-heading">소비 분석에 사용할<br>은행을 선택해 주세요</h2>
        </div>
        <div class="intro-content">
          <div class="intro-card">
            <span class="card-notch left" aria-hidden="true"></span>
            <span class="card-notch right" aria-hidden="true"></span>
            <div class="insight-pass-bar">
              <span class="pass-badge">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="12" r="3" fill="currentColor"/></svg>
                SPENDING INSIGHT PASS
              </span>
              <span class="pass-step">STEP 2/3</span>
            </div>
            <div class="card-divider"></div>
            <div class="card-body">
              <div v-if="banksError" class="connection-empty">
                은행 목록을 불러오지 못했어요.<br>
                <button type="button" class="retry-button" @click="banks = []; goToBankSelect()">다시 시도</button>
              </div>
              <div v-else-if="banks.length === 0" class="connection-empty">불러오는 중...</div>
              <div v-else class="bank-grid">
                <button
                    v-for="bank in banks"
                    :key="bank.organizationCode"
                    class="bank-option"
                    :class="{ selected: selectedBank?.organizationCode === bank.organizationCode }"
                    @click="selectedBank = bank"
                >
                  <span class="bank-logo-mark" :class="bankLogoClass(bank)">
                    <span v-if="!bankLogoUrl(bank)" class="bank-logo-fallback">{{ bank.institutionName.charAt(0) }}</span>
                    <img
                      v-if="bankLogoUrl(bank)"
                      :src="bankLogoUrl(bank)"
                      :alt="`${bank.institutionName} 로고`"
                      @error="hideBrokenBankLogo"
                    >
                  </span>
                  <strong>{{ bank.institutionName }}</strong>
                  <i v-if="selectedBank?.organizationCode === bank.organizationCode">✓</i>
                </button>
              </div>
              <button class="primary-button" style="margin-top:18px" :disabled="!selectedBank" @click="step = 3">다음</button>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="step === 3">
        <div class="intro-hero compact">
          <span class="hero-orbit" aria-hidden="true"></span>
          <header class="simple-header on-dark"><button @click="step = 2">‹</button><strong>은행 인증</strong></header>
          <h2 class="hero-heading">{{ selectedBank?.institutionName }} 계정으로<br>안전하게 연결해요</h2>
        </div>
        <div class="intro-content">
          <div class="intro-card">
            <span class="card-notch left" aria-hidden="true"></span>
            <span class="card-notch right" aria-hidden="true"></span>
            <div class="insight-pass-bar">
              <span class="pass-badge">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="12" r="3" fill="currentColor"/></svg>
                SPENDING INSIGHT PASS
              </span>
              <span class="pass-step">STEP 3/3</span>
            </div>
            <div class="card-divider"></div>
            <div class="card-body">
              <p class="subcopy">은행 아이디와 비밀번호를 입력해 주세요</p>
              <p v-if="connectionError" class="connection-error">{{ connectionError }}</p>
              <div class="credential-form">
                <label class="full-field">
                  은행 아이디
                  <input v-model="loginId" placeholder="은행 아이디를 입력해 주세요" autocomplete="off" />
                </label>
                <label class="full-field">
                  비밀번호
                  <input v-model="password" type="password" placeholder="비밀번호를 입력해 주세요" autocomplete="off" />
                </label>
              </div>
              <div class="security-note"><span>▣</span><strong>인증 정보는 연결 과정에서만 암호화하여 사용해요</strong></div>
              <button class="primary-button" style="margin-top:18px" :disabled="!loginId || !password" @click="startConnection">다음</button>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="step === 4">
        <header class="simple-header"><button @click="step = 3">‹</button><strong>자산 연결</strong></header>
        <div class="connection-content">
          <div class="connection-ticket">
            <div class="loading-head">
              <p>ASSET CONNECTION · BOARDING</p>
              <h2>자산 정보를 불러오고 있어요</h2>
              <span>안전하게 연결 중이에요. 잠시만 기다려 주세요.</span>
            </div>
            <div class="wave-divider" aria-hidden="true"><svg viewBox="0 0 320 52" preserveAspectRatio="none"><path d="M4 40C60 44 90 12 150 16C210 20 250 44 316 12" fill="none" stroke="rgba(255,255,255,.3)" stroke-width="1.6" stroke-dasharray="4 6" stroke-linecap="round"/></svg></div>
            <div class="progress-label"><span>인증 완료</span><span>자산 연결</span></div>
            <div class="progress-block">
              <div class="progress-track gold"><i :style="{ width: `${connectionProgress}%` }"></i></div>
              <small>{{ connectionProgress }}% · 계좌와 자산 정보를 연결하는 중</small>
            </div>
            <div class="ticket-footnote-divider"></div>
            <p class="ticket-footnote">암호화된 연결로 금융 정보를 안전하게 불러옵니다</p>
          </div>
          <div class="connection-steps">
            <div class="step-row done">
              <span class="step-icon"><svg width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#0FAE96"/><path d="M8 12.5l3 3 5.5-6.5" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg></span>
              <span>인증서 확인 완료</span>
            </div>
            <div class="step-row" :class="{ done: connectionProgress > 40 }">
              <span class="step-icon">
                <svg v-if="connectionProgress > 40" width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#0FAE96"/><path d="M8 12.5l3 3 5.5-6.5" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#EAF1FF"/><path d="M2 16l20-7-7 20-3-8-8-3-2-2z" fill="#2F6FED" transform="translate(6,6) scale(0.5)"/></svg>
              </span>
              <span>금융기관 연결 완료</span>
            </div>
            <div class="step-row" :class="{ done: connectionProgress >= 100, active: connectionProgress < 100 }">
              <span class="step-icon">
                <svg v-if="connectionProgress >= 100" width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#0FAE96"/><path d="M8 12.5l3 3 5.5-6.5" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#EAF1FF"/><path d="M2 16l20-7-7 20-3-8-8-3-2-2z" fill="#2F6FED" transform="translate(6,6) scale(0.5)"/></svg>
              </span>
              <span>자산 정보를 가져오고 있어요</span>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="step === 5">
        <header class="simple-header"><button @click="step = 2">‹</button><strong>계좌 연결 완료</strong></header>
        <div class="connection-content">
          <div class="connection-ticket complete">
            <div class="ticket-band-row">
              <span class="pass-badge">
                <svg class="pass-plane" width="12" height="12" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M21.7 11.2 14 7.1V3.6a2 2 0 0 0-4 0v3.5l-7.7 4.1a1.5 1.5 0 0 0-.8 1.3v1.2l8.5-2.2v4.2l-2.3 1.8v1l4.3-1 4.3 1v-1L14 15.7v-4.2l8.5 2.2v-1.2a1.5 1.5 0 0 0-.8-1.3Z"/></svg>
                TRIPASS BOARDING PASS
              </span>
              <span class="ticket-status">COMPLETE</span>
            </div>
            <div class="complete-heading-block">
              <div class="complete-heading">
                <span class="completion-check">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="10" fill="#0FAE96"/><path d="M8 12.5l3 3 5.5-6.5" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </span>
                <h2>주거래 계좌 연결이 완료됐어요</h2>
              </div>
              <span class="complete-description">이제 소비 분석을 바탕으로<br>여행자금 절약 코칭을 받아보세요</span>
            </div>
            <div class="ticket-tear" aria-hidden="true">
              <span class="card-notch left"></span>
              <span class="card-notch right"></span>
            </div>
            <div class="complete-stats-block">
              <div class="complete-stats">
                <div class="complete-stat"><span>연결 은행</span><strong>{{ selectedBank?.institutionName }}</strong></div>
                <div class="complete-stat"><span>분석 계좌</span><strong class="highlight">{{ linkedAccounts.length }}개</strong></div>
              </div>
              <div class="ticket-barcode"></div>
            </div>
          </div>
        </div>
        <div class="sticky-action solid">
          <button class="primary-button" @click="completeProfile">TRIPass 시작하기</button>
        </div>
      </template>

      <template v-else-if="step === 6">
        <header class="simple-header"><button @click="step = 5">‹</button><strong>기본 정보 입력</strong></header>
        <div class="step-progress"><i></i><i></i><i></i></div>
        <div class="form-content">
          <h2>급여 정보</h2><p class="subcopy">받는 만큼 자유롭게 추가하세요</p>
          <div class="summary-ticket"><small>등록된 월 수입</small><strong>{{ formatNumber(salaryTotal) }}원</strong><span>{{ salaryItems.length }}건</span></div>
          <article v-for="item in salaryItems" :key="item.id" class="form-card">
            <div class="form-card-title"><span class="feature-icon blue">▣</span><input v-model="item.name" aria-label="급여명"><button @click="removeItem(salaryItems, item.id)">삭제</button></div>
            <div class="field-grid two"><label>급여일<input v-model="item.payDay" inputmode="numeric"><em>일</em></label><label>월 급여액<input :value="item.amount" inputmode="numeric" @input="updateAmount(item, $event)"><em>원</em></label></div>
            <label class="full-field">메모 (선택)<input v-model="item.memo"></label>
            <label class="full-field">연결 계좌<select v-model="item.account"><option>하나은행 급여계좌 ****4821</option><option>KB국민은행 ****1234</option></select></label>
          </article>
          <button class="add-row" @click="addSalary">＋ 급여 추가하기</button>
        </div>
        <div class="sticky-action"><button class="primary-button" :disabled="salaryTotal === 0" @click="step = 7">다음</button></div>
      </template>

      <template v-else-if="step === 7">
        <header class="simple-header"><button @click="step = 6">‹</button><strong>고정비 등록</strong></header>
        <div class="step-progress"><i class="filled"></i><i class="filled"></i><i></i></div>
        <div class="form-content fixed-form">
          <h2>매달 정해진 날짜에 나가는<br>고정 지출을 등록해 주세요</h2>
          <div class="summary-ticket"><small>월 고정지출 합계</small><strong>{{ formatNumber(fixedTotal) }}원</strong></div>
          <article v-for="item in fixedExpenses" :key="item.id" class="form-card expense-card">
            <div class="form-card-title"><span class="feature-icon" :class="item.tone">{{ item.icon }}</span><strong>고정지출 항목</strong><button @click="removeItem(fixedExpenses, item.id)">삭제</button></div>
            <label class="full-field expense-name">고정지출 이름 <em>*</em><input v-model.trim="item.name" placeholder="예: 월세, 통신비" aria-label="고정지출 이름"></label>
            <div class="field-grid two"><label>납부일<input v-model="item.day" inputmode="numeric"><em>일</em></label><label>금액<input :value="item.amount" inputmode="numeric" @input="updateAmount(item, $event)"><em>원</em></label></div>
            <div class="field-grid account-memo"><label>연결 계좌<select v-model="item.account"><option>하나은행 급여계좌 ****4821</option><option>KB국민은행 ****1234</option><option>신한은행 ****8888</option></select></label><label>메모 (선택)<input v-model="item.memo"></label></div>
          </article>
          <button class="add-row" @click="addFixedExpense">＋ 고정지출 항목 추가하기</button>
        </div>
        <div class="sticky-action"><button class="primary-button" :disabled="!hasValidFixedExpenseNames" @click="step = 8">다음</button></div>
      </template>

      <template v-else-if="step === 8">
        <header class="simple-header"><button @click="step = 7">‹</button><strong>카테고리별 목표 설정</strong></header>
        <div class="step-progress"><i class="filled"></i><i class="filled"></i><i class="filled"></i></div>
        <div class="category-content">
          <h2>카테고리별 목표 금액 입력</h2><p class="subcopy">여행 전까지 자유롭게 쓰고 싶은 만큼 설정해주세요</p>
          <article v-for="item in categories" :key="item.id" class="category-row">
            <span class="feature-icon" :class="item.tone">{{ item.icon }}</span><div><strong>{{ item.name }}</strong><small>목표 금액</small></div>
            <label><input :value="item.amount" inputmode="numeric" @input="updateAmount(item, $event)"><em>원</em></label>
          </article>
          <div class="category-total"><span>ⓘ 총 합산 금액</span><strong>{{ formatNumber(categoryTotal) }}원</strong></div>
        </div>
        <div class="sticky-action"><button class="primary-button" :disabled="categoryTotal === 0" @click="completeProfile">설정 완료</button></div>
      </template>

      <template v-else-if="step === CARD_COMPANY_STEP">
        <div class="intro-hero compact">
          <span class="hero-orbit" aria-hidden="true"></span>
          <header class="simple-header on-dark"><button @click="backFromInstitutionSelect(1)">‹</button><strong>카드사 선택</strong></header>
          <h2 class="hero-heading">결제내역을 불러올<br>카드사를 선택해 주세요</h2>
        </div>
        <div class="intro-content">
          <div class="intro-card">
            <span class="card-notch left" aria-hidden="true"></span>
            <span class="card-notch right" aria-hidden="true"></span>
            <div class="insight-pass-bar">
              <span class="pass-badge">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="12" r="3" fill="currentColor"/></svg>
                SPENDING INSIGHT PASS
              </span>
              <span class="pass-step">STEP 1/2</span>
            </div>
            <div class="card-divider"></div>
            <div class="card-body">
              <p class="subcopy">여러 카드사를 사용한다면 연동 완료 후 추가할 수 있어요.</p>
              <p v-if="cardConnectionError" class="connection-error">{{ cardConnectionError }}</p>
              <div v-if="isCardInstitutionLoading" class="connection-empty">카드사 목록을 불러오는 중...</div>
              <div v-else-if="cardInstitutions.length === 0" class="connection-empty">
                <p>연동 가능한 카드사를 불러오지 못했어요.</p>
                <button type="button" class="retry-button" @click="goToCardSelect">다시 시도</button>
              </div>
              <div v-else class="bank-grid card-company-grid">
                <button
                  v-for="institution in cardInstitutions"
                  :key="`${institution.businessType}-${institution.organizationCode}`"
                  type="button"
                  class="bank-option"
                  :class="{ selected: selectedCardInstitution?.organizationCode === institution.organizationCode }"
                  @click="selectedCardInstitution = institution"
                >
                  <span v-if="cardCompanyLogo(institution)" class="card-company-logo-frame">
                    <img
                      :src="cardCompanyLogo(institution)"
                      :alt="`${institution.institutionName} 로고`"
                      class="institution-logo card-company-logo"
                      :class="`card-company-logo--${institution.organizationCode}`"
                    >
                  </span>
                  <span v-else class="bank-mark card-mark">{{ institution.institutionName.charAt(0) }}</span>
                  <strong>{{ institution.institutionName }}</strong>
                  <i v-if="selectedCardInstitution?.organizationCode === institution.organizationCode">✓</i>
                </button>
              </div>
              <button class="primary-button" style="margin-top:18px" :disabled="!selectedCardInstitution" @click="goToCardLogin">다음</button>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="step === CARD_LOGIN_STEP">
        <div class="intro-hero compact">
          <span class="hero-orbit" aria-hidden="true"></span>
          <header class="simple-header on-dark"><button @click="step = CARD_COMPANY_STEP">‹</button><strong>카드 인증</strong></header>
          <h2 class="hero-heading">{{ selectedCardInstitution?.institutionName }} 홈페이지 계정으로<br>안전하게 연결해요</h2>
        </div>
        <div class="intro-content">
          <div class="intro-card">
            <span class="card-notch left" aria-hidden="true"></span>
            <span class="card-notch right" aria-hidden="true"></span>
            <div class="insight-pass-bar">
              <span class="pass-badge">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="12" r="3" fill="currentColor"/></svg>
                SPENDING INSIGHT PASS
              </span>
              <span class="pass-step">STEP 2/2</span>
            </div>
            <div class="card-divider"></div>
            <div class="card-body">
              <p class="subcopy">카드사 앱 간편 비밀번호가 아닌 홈페이지 아이디와 비밀번호를 입력해 주세요.</p>
              <p v-if="cardConnectionError" class="connection-error">{{ cardConnectionError }}</p>
              <div class="credential-form">
                <label class="full-field">카드사 홈페이지 아이디<input v-model.trim="cardLoginId" placeholder="아이디를 입력해 주세요" autocomplete="username"></label>
                <label class="full-field">카드사 홈페이지 비밀번호<input v-model="cardPassword" type="password" placeholder="비밀번호를 입력해 주세요" autocomplete="current-password" @keyup.enter="startCardConnection"></label>
              </div>
              <div class="security-note"><span>▣</span><strong>비밀번호는 연동 요청 과정에서만 암호화하여 사용해요</strong></div>
              <button class="primary-button" style="margin-top:18px" :disabled="!cardLoginId.trim() || !cardPassword || isCardConnecting" @click="startCardConnection">{{ isCardConnecting ? '연동 중...' : '다음' }}</button>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="step === CARD_CONNECTING_STEP">
        <header class="simple-header"><span></span><strong>카드 연결</strong></header>
        <div class="connection-content">
          <div class="connection-ticket">
            <div class="loading-head">
              <p>CARD CONNECTION · BOARDING</p>
              <h2>카드 정보를 불러오고 있어요</h2>
              <span>카드사 로그인과 보유 카드 목록을 확인하고 있어요.</span>
            </div>
            <div class="wave-divider" aria-hidden="true"><svg viewBox="0 0 320 52" preserveAspectRatio="none"><path d="M4 40C60 44 90 12 150 16C210 20 250 44 316 12" fill="none" stroke="rgba(255,255,255,.3)" stroke-width="1.6" stroke-dasharray="4 6" stroke-linecap="round"/></svg></div>
            <div class="progress-label"><span>인증 완료</span><span>카드 연결</span></div>
            <div class="progress-block">
              <div class="progress-track gold"><i :style="{ width: `${cardConnectionProgress}%` }"></i></div>
              <small>창을 닫지 말고 잠시 기다려 주세요.</small>
            </div>
            <div class="ticket-footnote-divider"></div>
            <p class="ticket-footnote">암호화된 연결로 금융 정보를 안전하게 불러옵니다</p>
          </div>
          <div class="connection-steps">
            <div class="step-row" :class="{ done: cardConnectionProgress >= 20 }">
              <span class="step-icon">
                <svg v-if="cardConnectionProgress >= 20" width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#0FAE96"/><path d="M8 12.5l3 3 5.5-6.5" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#EAF1FF"/><path d="M2 16l20-7-7 20-3-8-8-3-2-2z" fill="#2F6FED" transform="translate(6,6) scale(0.5)"/></svg>
              </span>
              <span>카드사 로그인 정보 확인</span>
            </div>
            <div class="step-row" :class="{ done: cardConnectionProgress >= 50 }">
              <span class="step-icon">
                <svg v-if="cardConnectionProgress >= 50" width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#0FAE96"/><path d="M8 12.5l3 3 5.5-6.5" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#EAF1FF"/><path d="M2 16l20-7-7 20-3-8-8-3-2-2z" fill="#2F6FED" transform="translate(6,6) scale(0.5)"/></svg>
              </span>
              <span>CODEF 카드사 연결</span>
            </div>
            <div class="step-row" :class="{ done: cardConnectionProgress >= 100, active: cardConnectionProgress < 100 }">
              <span class="step-icon">
                <svg v-if="cardConnectionProgress >= 100" width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#0FAE96"/><path d="M8 12.5l3 3 5.5-6.5" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="#EAF1FF"/><path d="M2 16l20-7-7 20-3-8-8-3-2-2z" fill="#2F6FED" transform="translate(6,6) scale(0.5)"/></svg>
              </span>
              <span>보유 카드 정보를 불러오고 있어요</span>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="step === CARD_COMPLETE_STEP">
        <header class="simple-header"><button @click="step = CARD_COMPANY_STEP">‹</button><strong>카드 연결 완료</strong></header>
        <div class="connection-content">
          <div class="connection-ticket complete">
            <div class="ticket-band-row">
              <span class="pass-badge">
                <svg class="pass-plane" width="12" height="12" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M21.7 11.2 14 7.1V3.6a2 2 0 0 0-4 0v3.5l-7.7 4.1a1.5 1.5 0 0 0-.8 1.3v1.2l8.5-2.2v4.2l-2.3 1.8v1l4.3-1 4.3 1v-1L14 15.7v-4.2l8.5 2.2v-1.2a1.5 1.5 0 0 0-.8-1.3Z"/></svg>
                TRIPASS BOARDING PASS
              </span>
              <span class="ticket-status">COMPLETE</span>
            </div>
            <div class="complete-heading-block">
              <div class="complete-heading">
                <span class="completion-check">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="10" fill="#0FAE96"/><path d="M8 12.5l3 3 5.5-6.5" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </span>
                <h2>카드 연결이 완료됐어요</h2>
              </div>
              <span class="complete-description">연결한 카드의 결제내역을<br>소비 분석에 활용할 수 있어요</span>
            </div>
            <div class="ticket-tear" aria-hidden="true">
              <span class="card-notch left"></span>
              <span class="card-notch right"></span>
            </div>
            <div class="complete-stats-block">
              <div class="complete-stats">
                <div class="complete-stat"><span>연결 카드사</span><strong>{{ selectedCardInstitution?.institutionName }}</strong></div>
                <div class="complete-stat"><span>불러온 카드</span><strong class="highlight">{{ linkedCards.length }}개</strong></div>
              </div>
              <div class="ticket-barcode"></div>
            </div>
          </div>
          <section class="linked-card-list">
            <article v-for="card in linkedCards" :key="card.id ?? card.maskedCardNumber" class="linked-card-item">
              <span class="linked-card-icon">▰</span><div><strong>{{ card.cardName }}</strong><small>{{ cardTypeName(card.cardType) }} · {{ card.maskedCardNumber || '카드번호 비공개' }}</small></div>
            </article>
          </section>
        </div>
        <div class="sticky-action split solid"><button class="secondary-button" @click="resetCardConnection">카드 추가 연동</button><button class="primary-button" @click="completeProfile">완료</button></div>
      </template>
    </section>
  </main>
</template>

<style scoped>
:global(body) { margin: 0; color: #111827; font-family: Pretendard, 'Noto Sans KR', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }
* { box-sizing: border-box; }
button, input, select { font: inherit; }
button { border: 0; cursor: pointer; }
.finance-profile { min-height: 100vh; background: #f3f6fb; }
.onboarding-skip-floating{position:fixed;top:18px;right:max(18px,calc((100vw - 390px)/2 + 18px));z-index:80;padding:8px 11px;border-radius:999px;background:rgba(255,255,255,.92);color:#52637e;font-size:11px;font-weight:800;box-shadow:0 5px 16px rgba(19,45,91,.12)}
.finance-shell { position: relative; width: 100%; min-height: 100vh; overflow: hidden; background: #f3f6fb; }
.finance-shell.is-tall { padding-bottom: 92px; }
.finance-shell.is-navy-shell { background: linear-gradient(155deg, #0b2a6b 0%, #123c94 60%, #17459f 100%); }
.finance-shell.is-navy-shell .intro-hero { background: none; }
.finance-shell.is-navy-shell .card-notch { background: #123c94; }
.page-content h2 { margin: 0; font-size: 24px; line-height: 1.35; letter-spacing: -.045em; }
.intro-hero { position: relative; padding: 58px 24px 64px; overflow: hidden; color: #fff; background: linear-gradient(155deg, #0b2a6b 0%, #123c94 60%, #17459f 100%); }
.hero-orbit { position: absolute; top: -50px; right: -45px; width: 170px; height: 170px; border-radius: 50%; background: rgba(255,255,255,.06); }
.hero-mark { position: relative; margin: 0; display: flex; align-items: center; gap: 10px; color: #fff; font-size: 16px; font-weight: 900; letter-spacing: .12em; }
.hero-mark img { width: 54px; height: 42px; object-fit: contain; }
.hero-plane { color: #ffd45e; transform: rotate(45deg); }
.benefit-plane { color: #ffd45e; transform: rotate(45deg); }
.pass-plane { transform: rotate(45deg); }
.hero-title { position: relative; margin: 18px 0 0; font-size: 19px; line-height: 1.35; letter-spacing: -.01em; }
.hero-description { position: relative; margin: 8px 0 0; color: rgba(255,255,255,.6); font-size: 12.5px; font-weight: 600; line-height: 1.5; }
.intro-content { margin-top: -46px; padding: 0 22px; }
.intro-card { position: relative; border-radius: 18px; overflow: hidden; background: #fff; box-shadow: 0 14px 30px rgba(11,42,107,.16); }
.card-notch { position: absolute; top: 52px; width: 18px; height: 18px; border-radius: 50%; background: #f3f6fb; z-index: 1; }
.card-notch.left { left: -9px; }
.card-notch.right { right: -9px; }
.insight-pass-bar { position: relative; padding: 14px 22px; display: flex; justify-content: space-between; align-items: center; border-radius: 18px 18px 0 0; color: #ffd466; background: #0b2a6b; font-size: 10px; font-weight: 800; letter-spacing: .08em; }
.pass-badge { display: inline-flex; align-items: center; gap: 6px; }
.pass-step { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: rgba(255,255,255,.65); letter-spacing: 0; }
.card-divider { margin: 0 22px; border-top: 1.5px dashed #e4e8f0; }
.connection-ticket, .summary-ticket { color: white; background: linear-gradient(145deg, #263f8c, #172f6b); box-shadow: 0 8px 20px rgba(23,47,107,.2); }
.intro-list { padding: 18px 22px 0; display: grid; gap: 11px; }
.intro-list article { display: flex; gap: 12px; align-items: center; padding: 13px 14px; border: 1px solid #eceff5; border-radius: 14px; background: #fff; }
.intro-list strong { display: block; font-size: 13px; color: #10192b; }.intro-list p { margin: 2px 0 0; color: #98a2b3; font-size: 10.5px; }
.benefit-icon { display: inline-flex; flex: 0 0 auto; width: 34px; height: 34px; border-radius: 10px; align-items: center; justify-content: center; }
.benefit-icon.blue { background: #eaf1ff; }.benefit-icon.orange { background: #fff0e6; }.benefit-icon.violet { background: #f3f0fc; }
.feature-icon { display: inline-flex; flex: 0 0 auto; width: 44px; height: 44px; border-radius: 11px; align-items: center; justify-content: center; font-style: normal; font-weight: 800; }
.feature-icon.blue { color: #0066ff; background: #e6f0ff; }.feature-icon.orange { color: #ff8a3d; background: #fff0e6; }.feature-icon.violet { color: #8b5cf6; background: #f0eaff; }.feature-icon.red { color: #e5484d; background: #ffebec; }.feature-icon.green { color: #10b981; background: #e5f8f2; }.feature-icon.pink { color: #ec4899; background: #fde9f3; }.feature-icon.gray { color: #94a3b8; background: #f1f5f9; }
.sticky-action { position: absolute; z-index: 10; left: 0; right: 0; bottom: 0; padding: 12px 24px 20px; background: linear-gradient(180deg, rgba(249,246,240,0), #f9f6f0 22%); }
.intro-action { padding: 6px 22px 22px; }
.sticky-action.split { display: grid; grid-template-columns: 1fr 1.2fr; gap: 10px; }
.primary-button, .secondary-button { width: 100%; min-height: 56px; border-radius: 16px; font-size: 15px; font-weight: 800; }
.primary-button { color: #fff; background: #123578; }.primary-button:disabled { color: #94a3b8; background: #dfe4eb; cursor: not-allowed; }.secondary-button { color: #263f8c; background: #e8edfb; }
.skip-button { display: block; width: 100%; padding: 18px 0 0; background: transparent; color: #9ba8bd; font-size: 13px; font-weight: 700; text-align: center; }
.simple-header { height: 49px; padding: 0 20px; display: flex; align-items: center; gap: 9px; }.simple-header button { width: 25px; padding: 0; color: #111827; background: transparent; font-size: 27px; line-height: 1; }.simple-header strong { font-size: 16px; }
.intro-hero.compact { padding: 0 24px 64px; }
.intro-hero.compact .simple-header { position: relative; margin: 0 -24px 22px; padding: 18px 24px 0; }
.simple-header.on-dark button, .simple-header.on-dark strong { color: #fff; }
.hero-heading { position: relative; margin: 0; font-size: 19px; font-weight: 800; line-height: 1.35; letter-spacing: -.01em; color: #fff; }
.card-body { padding: 18px 22px 22px; }
.page-content { padding: 25px 20px 100px; }.page-content h2 { font-size: 22px; }.subcopy { margin: 7px 0 0; color: #64748b; font-size: 11px; }
.account-intro { padding-top: 42px; }.load-bank-card { width: 100%; padding: 13px 14px; display: flex; align-items: center; gap: 13px; text-align: left; border: 1px solid #eceff5; border-radius: 14px; background: white; }.load-bank-card > span:nth-child(2) { flex: 1; }.load-bank-card strong, .load-bank-card small { display: block; }.load-bank-card strong { font-size: 13px; color: #10192b; }.load-bank-card small { margin-top: 2px; color: #98a2b3; font-size: 10.5px; }.load-bank-card b { color: #98a2b3; font-size: 18px; font-weight: 400; }.bank-building { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 10px; color: #2f6fed; background: #eaf1ff; }
.security-note { margin-top: 16px; padding: 18px; display: flex; gap: 12px; border-radius: 14px; background: #eaf1ff; }.security-note > span { color: #2f6fed; }.security-note strong { font-size: 11px; }.security-note p { margin: 4px 0 0; color: #64748b; font-size: 9px; }
.bank-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }.bank-option { position: relative; height: 88px; padding: 12px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; border: 1.5px solid transparent; border-radius: 14px; background: #f4f5f9; }.bank-option.selected { border-color: #2f6fed; background: #fff; box-shadow: 0 4px 12px rgba(47,111,237,.12); }.bank-option.pending { opacity: .6; cursor: not-allowed; }.bank-option strong { font-size: 11px; color: #10192b; }.bank-option i { position: absolute; top: 8px; right: 8px; width: 18px; height: 18px; border-radius: 50%; color: white; background: #2f6fed; font-size: 11px; font-style: normal; display: grid; place-items: center; }.bank-option small { position: absolute; top: 10px; right: 9px; color: #e5484d; font-size: 8px; }.bank-mark, .certificate-mark { display: grid; place-items: center; border-radius: 9px; font-weight: 900; }.bank-mark { width: 30px; height: 30px; font-size: 11px; }.bank-mark.mint { color: #047857; background: #d1fae5; }.bank-mark.yellow { color: #3f3100; background: #ffe44d; }.bank-mark.blue { color: white; background: #1688e8; }.bank-mark.green { color: white; background: #10b981; }.bank-mark.navy { color: white; background: #263f8c; }
.bank-logo-mark { position: relative; display: block; width: 38px; height: 40px; overflow: hidden; border: 0; border-radius: 0; background: transparent; box-shadow: none; }
.bank-logo-mark img { position: absolute; top: 50%; left: 0; width: auto; max-width: none; height: 34px; object-fit: initial; background: transparent; transform: translateY(-50%); }
.bank-logo-0011 img { left: 50%; height: 38px; transform: translate(-50%, -50%); }
.bank-logo-0004 { width: 40px; }
.bank-logo-0004 img { height: 39px; }
.bank-logo-0004 img,
.bank-logo-0088 img,
.bank-logo-0031 img { mix-blend-mode: multiply; }
.bank-logo-0020 { width: 38px; }
.bank-logo-0081 { width: 38px; }
.bank-logo-0031 img { top: -12px; left: -20px; width: 80px; height: 80px; transform: none; }
.bank-logo-0089 { width: 62px; }
.bank-logo-0089 img { left: 50%; width: 60px; height: auto; transform: translate(-50%, -50%); }
.bank-logo-fallback { display: grid; width: 100%; height: 100%; place-items: center; border-radius: 10px; color: #263f8c; background: transparent; font-size: 12px; font-weight: 900; }
.terms-backdrop { height: 100vh; color: rgba(17,24,39,.75); background: #999; }.terms-copy { padding: 24px 31px; }.terms-copy h3 { margin: 0 0 13px; font-size: 14px; }.terms-copy p, .terms-copy small { display: block; margin: 0 0 5px; font-size: 10px; }
.certificate-sheet { position: absolute; z-index: 5; left: 9px; right: 9px; bottom: 10px; padding: 20px; border-radius: 24px; background: white; box-shadow: 0 -8px 30px rgba(0,0,0,.12); }.sheet-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 13px; }.sheet-heading h2 { margin: 0; font-size: 18px; }.sheet-heading button { color: #64748b; background: transparent; font-size: 23px; }
.certificate-option { position: relative; width: 100%; min-height: 49px; margin-top: 8px; padding: 10px 12px; display: flex; gap: 11px; align-items: center; text-align: left; border: 1px solid #e5e7eb; border-radius: 11px; background: white; }.certificate-option.selected { align-items: flex-start; border: 1.5px solid #0754a6; background: #dceeff; }.certificate-option > span:nth-child(2) { flex: 1; }.certificate-option strong { font-size: 12px; }.certificate-option em { margin-left: 8px; padding: 3px 7px; border-radius: 8px; color: #1688e8; background: white; font-size: 8px; font-style: normal; }.certificate-option small { display: block; margin-top: 8px; color: #555; font-size: 9px; }.certificate-option b { color: #15803d; font-size: 20px; }.certificate-mark { width: 22px; height: 22px; font-size: 11px; }.certificate-mark.kakao { color: #111; background: #fee500; }.certificate-mark.naver { color: white; background: #03c75a; }.certificate-mark.neutral { color: #64748b; background: #f1f5f9; }
.agreement-row { margin: -35px 13px 10px 48px; padding-top: 9px; display: grid; grid-template-columns: auto 1fr auto; gap: 6px; align-items: center; border-top: 1px solid #76b8ed; color: #073985; font-size: 9px; font-weight: 800; }.agreement-row input { accent-color: #0754a6; }.sheet-button { margin-top: 10px; min-height: 48px; border-radius: 8px; background: #0c4594; }
.connection-content { padding: 25px 20px 105px; }
.connection-ticket { position: relative; padding: 0; border-radius: 18px; overflow: hidden; }
.connection-ticket h2 { margin: 0; font-size: 19px; }
.loading-head { padding: 20px 20px 4px; }
.loading-head > p { margin: 0; color: #ffd466; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9.5px; font-weight: 800; letter-spacing: .1em; }
.loading-head > h2 { margin: 12px 0 0; }
.loading-head > span { display: block; margin: 6px 0 0; color: rgba(255,255,255,.65); font-size: 12px; font-weight: 600; }
.wave-divider { position: relative; height: 52px; margin-top: 6px; }.wave-divider svg { display: block; width: 100%; height: 100%; }
.progress-label { display: flex; justify-content: space-between; padding: 0 20px; color: rgba(255,255,255,.75); font-size: 11px; font-weight: 800; }
.progress-block { padding: 10px 20px 4px; }
.progress-track { height: 6px; overflow: hidden; border-radius: 999px; background: rgba(255,255,255,.16); }.progress-track i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #2f8dff, #32d6e8); transition: width .2s ease; }
.progress-track.gold i { background: #ffd466; }
.progress-block small { display: block; margin-top: 9px; color: rgba(255,255,255,.8); font-size: 12px; font-weight: 700; }
.ticket-footnote-divider { margin: 14px 20px 0; border-top: 1.5px dashed rgba(255,255,255,.25); }
.connection-ticket > p.ticket-footnote { margin: 0; padding: 12px 20px 18px; color: rgba(255,255,255,.55); font-size: 11px; font-weight: 600; letter-spacing: normal; line-height: 1.6; }
.connection-steps { margin-top: 14px; padding: 18px; display: flex; flex-direction: column; gap: 14px; border-radius: 16px; background: white; box-shadow: 0 4px 14px rgba(16,25,43,.06); }
.step-row { display: flex; align-items: center; gap: 10px; color: #94a3b8; font-size: 14px; font-weight: 800; }
.step-row.done, .step-row.active { color: #10192b; }
.step-icon { display: flex; flex: 0 0 auto; width: 17px; height: 17px; }.step-icon svg { display: block; }
.connection-ticket.complete { padding: 0; min-height: 0; overflow: hidden; }
.connection-ticket.complete .ticket-band-row { margin: 0; padding: 14px 20px; background: rgba(255,255,255,.07); border-bottom: 1px solid rgba(255,255,255,.14); }
.ticket-band-row { display: flex; justify-content: space-between; align-items: center; font-size: 8px; font-weight: 800; letter-spacing: .08em; }
.ticket-band-row .pass-badge { display: inline-flex; align-items: center; gap: 6px; color: #ffd45e; }
.ticket-status { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: rgba(255,255,255,.6); }
.complete-heading-block { padding: 20px 20px 16px; }
.complete-heading { display: flex; align-items: center; gap: 9px; }
.complete-heading h2 { margin: 0; font-size: 17px; }
.completion-check { display: flex; flex: 0 0 auto; width: 22px; height: 22px; }
.completion-check svg { display: block; }
.complete-description { display: block; margin: 8px 0 0; color: rgba(255,255,255,.7); font-size: 12px; font-weight: 600; line-height: 1.6; }
.ticket-tear { position: relative; height: 18px; }
.ticket-tear .card-notch { top: 0; }
.ticket-tear::after { content: ''; position: absolute; left: 16px; right: 16px; top: 9px; height: 1px; background: repeating-linear-gradient(90deg, rgba(255,255,255,.4) 0 5px, transparent 5px 10px); }
.complete-stats-block { padding: 14px 20px 20px; }
.complete-stats { display: grid; gap: 0; }
.complete-stat { padding: 8px 0; display: flex; justify-content: space-between; align-items: center; }.complete-stat span { color: rgba(255,255,255,.65); font-size: 12.5px; font-weight: 700; }.complete-stat strong { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 14px; font-weight: 800; }.complete-stat strong.highlight { color: #ffd466; }
.ticket-barcode { height: 22px; margin-top: 12px; opacity: .5; background: repeating-linear-gradient(90deg, #fff 0 2px, transparent 2px 4px, #fff 4px 5px, transparent 5px 9px, #fff 9px 12px, transparent 12px 14px); }
.sticky-action.solid { background: #fff; border-top: 1px solid #eceff5; padding-top: 14px; }
.step-progress { padding: 0 24px 16px; display: grid; grid-template-columns: repeat(3,1fr); gap: 14px; }.step-progress i { height: 4px; border-radius: 4px; background: #dde3ee; }.step-progress i:first-child, .step-progress i.filled { background: #263f8c; }
.form-content, .category-content { padding: 0 20px 100px; }.form-content h2, .category-content h2 { margin: 0; font-size: 19px; line-height: 1.45; }.summary-ticket { position: relative; margin: 16px 0 18px; padding: 15px 18px; min-height: 88px; border-radius: 18px; }.summary-ticket small { display: block; margin-bottom: 7px; color: #c8d6ff; font-size: 9px; }.summary-ticket strong { display: block; font-size: 25px; }.summary-ticket > span { position: absolute; right: 18px; bottom: 16px; font-size: 10px; }
.form-card { margin-bottom: 10px; padding: 15px; border-radius: 17px; background: white; box-shadow: 0 6px 18px rgba(15,23,42,.08); }.form-card-title { display: flex; align-items: center; gap: 10px; margin-bottom: 13px; }.form-card-title input { min-width: 0; flex: 1; border: 0; outline: none; color: #111827; font-size: 13px; font-weight: 800; }.form-card-title button { padding: 5px; color: #e5484d; background: transparent; font-size: 9px; }.field-grid { display: grid; gap: 10px; }.field-grid.two { grid-template-columns: .75fr 1.25fr; }.field-grid.account-memo { grid-template-columns: 1.8fr .8fr; margin-top: 10px; }.field-grid label, .full-field { position: relative; color: #64748b; font-size: 9px; }.field-grid input, .field-grid select, .full-field input, .full-field select { width: 100%; height: 43px; margin-top: 6px; padding: 0 13px; border: 1px solid #e5eaf2; border-radius: 10px; outline: 0; color: #111827; background: white; font-size: 11px; }.field-grid em { position: absolute; right: 11px; bottom: 13px; color: #94a3b8; font-size: 9px; font-style: normal; }.field-grid input { padding-right: 27px; }.full-field { display: block; margin-top: 10px; }.add-row { width: 100%; height: 48px; border-radius: 14px; color: #0066ff; background: #dde5ff; font-size: 12px; font-weight: 800; }
.fixed-form { padding-bottom: 90px; }.expense-card .feature-icon { width: 38px; height: 38px; }.expense-card .form-card-title { margin-bottom: 10px; }.expense-card .form-card-title strong { flex: 1; color: #111827; font-size: 13px; }.expense-name { margin: 0 0 10px; }.expense-name > em { color: #e5484d; font-style: normal; }.expense-name input:focus { border-color: #263f8c; }
.category-content > .subcopy { margin-bottom: 23px; }.category-row { height: 66px; margin-bottom: 15px; padding: 10px 11px; display: flex; align-items: center; gap: 14px; border: 1px solid #e5eaf2; border-radius: 14px; background: white; }.category-row > div { flex: 1; }.category-row strong, .category-row small { display: block; }.category-row strong { font-size: 13px; }.category-row small { margin-top: 5px; color: #64748b; font-size: 9px; }.category-row label { display: flex; align-items: center; gap: 6px; }.category-row input { width: 130px; height: 44px; padding: 0 12px; border: 1.5px solid #d6dce7; border-radius: 11px; outline: none; color: #111827; background: white; text-align: right; font-size: 16px; font-weight: 800; }.category-row em { color: #96a1b5; font-size: 11px; font-style: normal; }.category-total { min-height: 76px; margin-top: 38px; padding: 15px 18px; display: flex; justify-content: space-between; align-items: flex-start; border-radius: 16px; color: #0066ff; background: #eef2ff; font-size: 11px; font-weight: 800; }.category-total strong { color: #263f8c; font-size: 20px; }
.asset-link-options { display: grid; gap: 12px; }.asset-link-options .load-bank-card + .load-bank-card { margin-top: 0; }.card-building { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 10px; color: #8b5cf6; background: #f3f0fc; }.asset-selection-guide { margin: 0; padding: 16px 0; color: #64748b; font-size: 12px; text-align: center; }.card-company-grid { margin-top: 24px; }.card-mark { color: #fff; background: linear-gradient(145deg, #263f8c, #4f6ec4); }.institution-logo { width: 27px; height: 27px; border-radius: 7px; object-fit: contain; }.card-company-logo-frame { display: grid; width: 112px; height: 42px; overflow: hidden; place-items: center; }.card-company-logo { width: 104px; height: 36px; border-radius: 0; object-fit: contain; mix-blend-mode: multiply; }.card-company-logo--0301 { transform: scale(1.42); }.card-company-logo--0304 { transform: scale(1.38); }.card-company-logo--0313 { filter: brightness(0) saturate(100%) invert(42%) sepia(89%) saturate(672%) hue-rotate(127deg) brightness(85%) contrast(101%); transform: scale(1.04); }.connection-empty { padding: 48px 0; color: #94a3b8; font-size: 12px; line-height: 1.6; text-align: center; }.retry-button { margin-top: 12px; padding: 9px 18px; border-radius: 9px; color: #286dd8; background: #edf4ff; font-size: 12px; font-weight: 700; }.connection-error { margin: 14px 0 0; padding: 12px 14px; border-radius: 10px; color: #c62828; background: #ffebee; font-size: 11px; line-height: 1.5; }.selected-institution { margin-bottom: 30px; padding: 14px; display: flex; align-items: center; gap: 12px; border: 1px solid #dce5f5; border-radius: 14px; background: #fff; }.selected-institution small, .selected-institution strong { display: block; }.selected-institution small { margin-bottom: 3px; color: #94a3b8; font-size: 9px; }.selected-institution strong { font-size: 13px; }.credential-form { margin-top: 25px; display: grid; gap: 13px; }.linked-card-list { margin-top: 16px; display: grid; gap: 10px; }.linked-card-item { padding: 15px; display: flex; align-items: center; gap: 12px; border-radius: 14px; background: #fff; box-shadow: 0 6px 16px rgba(15,23,42,.08); }.linked-card-icon { display: grid; width: 38px; height: 38px; flex: 0 0 auto; place-items: center; border-radius: 11px; color: #8b5cf6; background: #f0eaff; }.linked-card-item div { min-width: 0; }.linked-card-item strong, .linked-card-item small { display: block; }.linked-card-item strong { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.linked-card-item small { margin-top: 4px; color: #64748b; font-size: 9px; }
@media (max-width: 360px) { .intro-hero { padding-inline: 20px; }.intro-content { padding-inline: 16px; }.insight-pass-bar, .intro-list, .intro-action { padding-inline: 16px; }.hero-title { font-size: 17px; } }
</style>
