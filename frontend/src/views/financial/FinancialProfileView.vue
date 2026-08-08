<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useFinancialScheduleStore } from '@/stores/financialSchedule'
import api from '@/api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const scheduleStore = useFinancialScheduleStore()

const requestedStep = Number(route.query.step)
const step = ref(Number.isInteger(requestedStep) && requestedStep >= 0 && requestedStep <= 8 ? requestedStep : 0)

const banks = ref([])
const banksError = ref(false)
const selectedBank = ref(null)
const loginId = ref('')
const password = ref('')
const linkedAccounts = ref([])
const connectionProgress = ref(0)
const connectionError = ref('')

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

  try {
    connectionProgress.value = 50
    const res = await api.post('/accounts/codef/connect', {
      organizationCode: selectedBank.value.organizationCode,
      organizationName: selectedBank.value.institutionName,
      businessType: selectedBank.value.businessType,
      loginType: '1',
      loginId: loginId.value,
      password: password.value,
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
  scheduleStore.replaceSalaries(salaryItems.value)
  authStore.completeProfile()
  router.replace('/')
}

function skipProfile() {
  authStore.completeProfile()
  router.replace('/')
}

onMounted(() => {
  if (step.value === 2) goToBankSelect()
})

</script>

<template>
  <main class="finance-profile">
    <section class="finance-shell" :class="{ 'is-tall': step === 7 }">
      <div class="device-status" aria-hidden="true">
        <strong>9:41</strong>
        <span class="dynamic-island"></span>
        <span>▮ ◉ ▰</span>
      </div>

      <template v-if="step === 0">
        <div class="intro-content">
          <p class="eyebrow">TRIPASS FINANCE</p>
          <h1>{{ authStore.user?.name ?? '로드투제주' }}님,<br>여행 자금 계획을 위해<br>금융 프로필을 등록해요</h1>
          <p class="intro-description">3분이면 충분해요. 수입과 지출을 알려주시면<br>출국일까지 저축 계획을 자동으로 계산해드려요.</p>

          <div class="boarding-ticket">
            <p>FINANCIAL BOARDING PASS</p>
            <div class="ticket-main">
              <strong>3분</strong>
              <span>이면 여행 저축 계획 완성</span>
            </div>
            <div class="ticket-route">수입 → 고정지출 → 카테고리 목표</div>
          </div>

          <div class="intro-list">
            <article>
              <span class="feature-icon blue">▣</span>
              <div><strong>이번 달 수입 등록</strong><p>급여일과 월 급여를 알려주세요</p></div>
            </article>
            <article>
              <span class="feature-icon orange">₩</span>
              <div><strong>고정지출 등록</strong><p>월세·통신비 등 매달 나가는 돈</p></div>
            </article>
            <article>
              <span class="feature-icon violet">◎</span>
              <div><strong>카테고리별 목표 설정</strong><p>식비·쇼핑 등 쓰고 싶은 만큼</p></div>
            </article>
          </div>
        </div>
        <div class="sticky-action">
          <button class="primary-button" @click="step = 1">금융 프로필 등록하기</button>
          <button class="skip-button" @click="skipProfile">건너뛰기</button>
        </div>
      </template>

      <template v-else-if="step === 1">
        <header class="simple-header"><button @click="step = 0">‹</button><strong>은행 계좌 불러오기</strong></header>
        <div class="page-content account-intro">
          <h2>{{ authStore.user?.name ?? '아영' }}님이 쓰는<br>은행 계좌 정보를 불러올게요</h2>
          <p class="subcopy">연동할 금융사를 선택해 주세요</p>
          <button class="load-bank-card" @click="goToBankSelect">
            <span class="bank-building">▦</span>
            <span><strong>은행</strong><small>모든 금융사</small></span>
            <b>›</b>
          </button>
          <div class="security-note"><span>▣</span><div><strong>안전하게 연결해요</strong><p>인증 정보는 연결 과정에서만 사용됩니다</p></div></div>
        </div>
        <div class="sticky-action"><button class="primary-button" @click="goToBankSelect">다음</button></div>
      </template>

      <template v-else-if="step === 2">
        <header class="simple-header"><button @click="route.query.from === 'asset' ? router.back() : step = 1">‹</button><strong>은행 선택</strong></header>
        <div class="page-content">
          <h2>{{ authStore.user?.name ?? '아영' }}님이 쓰는<br>은행 계좌 정보를 불러올게요</h2>
          <div class="selection-caption"><span>연동할 금융사를 선택해 주세요</span></div>
          <div v-if="banksError" style="text-align:center; padding: 40px 0; color: #94a3b8; font-size: 12px;">
            은행 목록을 불러오지 못했어요.<br>
            <button type="button" style="margin-top:12px; padding:8px 16px; border-radius:8px; background:#edf4ff; color:#286dd8; font-size:12px; font-weight:700;" @click="banks = []; goToBankSelect()">다시 시도</button>
          </div>
          <div v-else-if="banks.length === 0" style="text-align:center; padding: 40px 0; color: #94a3b8; font-size: 12px;">
            불러오는 중...
          </div>
          <div v-else class="bank-grid">
            <button
                v-for="bank in banks"
                :key="bank.organizationCode"
                class="bank-option"
                :class="{ selected: selectedBank?.organizationCode === bank.organizationCode }"
                @click="selectedBank = bank"
            >
              <span class="bank-mark navy">{{ bank.institutionName.charAt(0) }}</span>
              <strong>{{ bank.institutionName }}</strong>
              <i v-if="selectedBank?.organizationCode === bank.organizationCode">✓</i>
            </button>
          </div>
        </div>
        <div class="sticky-action">
          <button class="primary-button" :disabled="!selectedBank" @click="step = 3">확인</button>
        </div>
      </template>

      <template v-else-if="step === 3">
        <header class="simple-header"><button @click="step = 2">‹</button><strong>계좌 연동</strong></header>
        <div class="page-content">
          <h2>{{ selectedBank?.institutionName }} 로그인 정보를<br>입력해 주세요</h2>
          <p class="subcopy">인증 정보는 연결 과정에서만 사용됩니다</p>
          <p v-if="connectionError" style="margin-top: 12px; color: #e5484d; font-size: 12px;">{{ connectionError }}</p>
          <div style="margin-top: 24px; display: grid; gap: 12px;">
            <label class="full-field">
              아이디
              <input v-model="loginId" placeholder="인터넷뱅킹 아이디" autocomplete="off" />
            </label>
            <label class="full-field">
              비밀번호
              <input v-model="password" type="password" placeholder="인터넷뱅킹 비밀번호" autocomplete="off" />
            </label>
          </div>
        </div>
        <div class="sticky-action">
          <button class="primary-button" :disabled="!loginId || !password" @click="startConnection">연동하기</button>
        </div>
      </template>

      <template v-else-if="step === 4">
        <header class="simple-header"><button @click="step = 3">‹</button><strong>자산 연결</strong></header>
        <div class="connection-content">
          <div class="connection-ticket">
            <p>ASSET CONNECTION · BOARDING</p>
            <h2>자산 정보를 불러오고 있어요</h2>
            <span>안전하게 연결 중이에요. 잠시만 기다려 주세요.</span>
            <div class="flight-path"><i>· · · · ·</i><b>✈</b></div>
            <div class="progress-label"><span>연결 진행</span><strong>{{ connectionProgress }}%</strong></div>
            <div class="progress-track"><i :style="{ width: `${connectionProgress}%` }"></i></div>
            <small>{{ connectionProgress }}% · 계좌와 자산 정보를 연결하는 중</small>
          </div>
          <div class="connection-steps">
            <p class="done">✓ 인증서 확인 완료</p>
            <p :class="{ done: connectionProgress > 40 }">✓ 금융기관 연결 완료</p>
            <p :class="{ active: connectionProgress <= 90 }">⌁ 자산 정보를 가져오고 있어요</p>
          </div>
        </div>
      </template>

      <template v-else-if="step === 5">
        <header class="simple-header"><button @click="step = 2">‹</button><strong>자산 연결 완료</strong></header>
        <div class="connection-content">
          <div class="connection-ticket complete">
            <p>FINANCE ARRIVAL PASS</p>
            <div class="completion-check">✓</div>
            <h2>자산 연결이 완료됐어요</h2>
            <span>여행 자금 계획에 사용할 자산을 확인했어요.</span>
            <div class="complete-stat"><span>연결 금융기관</span><strong>{{ selectedBank?.institutionName }}</strong></div>
            <div class="complete-stat"><span>불러온 계좌 수</span><strong>{{ linkedAccounts.length }}개</strong></div>
            <div class="barcode">|||| ||| ||||| || ||||</div>
          </div>
        </div>
        <div class="sticky-action split">
          <button class="secondary-button" @click="selectedBank = null; loginId = ''; password = ''; step = 2">자산연결추가</button>
          <button class="primary-button" @click="route.query.from === 'asset' ? router.replace('/asset') : step = 6">확인</button>
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

      <template v-else>
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
    </section>
  </main>
</template>

<style scoped>
:global(body) { margin: 0; color: #111827; font-family: 'Noto Sans KR', Pretendard, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }
* { box-sizing: border-box; }
button, input, select { font: inherit; }
button { border: 0; cursor: pointer; }
.finance-profile { min-height: 100vh; background: #edf0f4; display: flex; justify-content: center; }
.finance-shell { position: relative; width: 100%; max-width: 405px; min-height: 100vh; overflow: hidden; background: #f9f6f0; }
.finance-shell.is-tall { padding-bottom: 92px; }
.device-status { height: 52px; padding: 17px 24px 0; display: flex; justify-content: space-between; align-items: flex-start; font-size: 11px; }
.dynamic-island { position: absolute; top: 14px; left: 50%; width: 74px; height: 23px; border-radius: 20px; background: #050505; transform: translateX(-50%); }
.eyebrow { margin: 0 0 25px; color: #263f8c; font-size: 10px; font-weight: 800; letter-spacing: .08em; }
.intro-content { padding: 34px 24px 110px; }
.intro-content h1, .page-content h2 { margin: 0; font-size: 24px; line-height: 1.35; letter-spacing: -.045em; }
.intro-description { margin: 12px 0 24px; color: #64748b; font-size: 12px; line-height: 1.6; }
.boarding-ticket, .connection-ticket, .summary-ticket { color: white; background: linear-gradient(145deg, #263f8c, #172f6b); box-shadow: 0 8px 20px rgba(23,47,107,.2); }
.boarding-ticket { position: relative; margin: 18px 0 24px; padding: 18px; border-radius: 18px; }
.boarding-ticket::before, .boarding-ticket::after, .connection-ticket::before, .connection-ticket::after { content: ''; position: absolute; top: 54%; width: 13px; height: 13px; border-radius: 50%; background: #f9f6f0; }
.boarding-ticket::before, .connection-ticket::before { left: -7px; }.boarding-ticket::after, .connection-ticket::after { right: -7px; }
.boarding-ticket > p, .connection-ticket > p { margin: 0 0 17px; color: #b8c8f7; font-size: 8px; letter-spacing: .08em; }
.ticket-main { display: flex; align-items: baseline; gap: 9px; }.ticket-main strong { font-size: 31px; }.ticket-main span { font-size: 12px; font-weight: 700; }
.ticket-route { margin-top: 14px; padding-top: 12px; border-top: 1px dashed rgba(255,255,255,.36); color: #c8d6ff; font-size: 10px; }
.intro-list { display: grid; gap: 10px; }.intro-list article { display: flex; gap: 13px; align-items: center; padding: 13px; border-radius: 15px; background: #fff; box-shadow: 0 5px 14px rgba(15,23,42,.07); }
.intro-list strong { display: block; font-size: 12px; }.intro-list p { margin: 4px 0 0; color: #64748b; font-size: 9px; }
.feature-icon { display: inline-flex; flex: 0 0 auto; width: 38px; height: 38px; border-radius: 11px; align-items: center; justify-content: center; font-style: normal; font-weight: 800; }
.feature-icon.blue { color: #0066ff; background: #e6f0ff; }.feature-icon.orange { color: #ff8a3d; background: #fff0e6; }.feature-icon.violet { color: #8b5cf6; background: #f0eaff; }.feature-icon.red { color: #e5484d; background: #ffebec; }.feature-icon.green { color: #10b981; background: #e5f8f2; }.feature-icon.pink { color: #ec4899; background: #fde9f3; }.feature-icon.gray { color: #94a3b8; background: #f1f5f9; }
.sticky-action { position: absolute; z-index: 10; left: 0; right: 0; bottom: 0; padding: 12px 24px 20px; background: linear-gradient(180deg, rgba(249,246,240,0), #f9f6f0 22%); }
.sticky-action.split { display: grid; grid-template-columns: 1fr 1.2fr; gap: 10px; }
.primary-button, .secondary-button { width: 100%; min-height: 56px; border-radius: 16px; font-size: 15px; font-weight: 800; }
.primary-button { color: #fff; background: #263f8c; }.primary-button:disabled { color: #94a3b8; background: #dfe4eb; cursor: not-allowed; }.secondary-button { color: #263f8c; background: #e8edfb; }
.skip-button { display: block; width: 100%; padding: 10px 0 0; background: transparent; color: #94a3b8; font-size: 13px; text-align: center; }
.simple-header { height: 49px; padding: 0 20px; display: flex; align-items: center; gap: 9px; }.simple-header button { width: 25px; padding: 0; color: #111827; background: transparent; font-size: 27px; line-height: 1; }.simple-header strong { font-size: 16px; }
.page-content { padding: 25px 20px 100px; }.page-content h2 { font-size: 22px; }.subcopy { margin: 7px 0 0; color: #64748b; font-size: 11px; }
.account-intro { padding-top: 42px; }.load-bank-card { width: 100%; margin-top: 28px; padding: 17px; display: flex; align-items: center; gap: 13px; text-align: left; border-radius: 15px; background: white; box-shadow: 0 7px 17px rgba(15,23,42,.09); }.load-bank-card > span:nth-child(2) { flex: 1; }.load-bank-card strong, .load-bank-card small { display: block; }.load-bank-card strong { font-size: 13px; }.load-bank-card small { margin-top: 3px; color: #64748b; font-size: 9px; }.load-bank-card b { color: #263f8c; font-size: 20px; }.bank-building { display: grid; width: 39px; height: 39px; place-items: center; border-radius: 10px; color: #0066ff; background: #e6f0ff; }
.security-note { margin-top: 16px; padding: 18px; display: flex; gap: 12px; border-radius: 14px; background: #eaf0ff; }.security-note > span { color: #d97706; }.security-note strong { font-size: 11px; }.security-note p { margin: 4px 0 0; color: #64748b; font-size: 9px; }
.selection-caption { margin: 28px 0 12px; display: flex; justify-content: space-between; color: #64748b; font-size: 9px; }.selection-caption b { color: #94a3b8; }
.bank-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }.bank-option { position: relative; height: 78px; padding: 12px; display: flex; flex-direction: column; align-items: flex-start; justify-content: space-between; border: 1px solid #e5eaf2; border-radius: 12px; background: #fff; }.bank-option.selected { border: 1.5px solid #0066ff; background: #eef5ff; }.bank-option.pending { opacity: .6; cursor: not-allowed; }.bank-option strong { font-size: 10px; }.bank-option i { position: absolute; top: 9px; right: 9px; width: 18px; height: 18px; border-radius: 50%; color: white; background: #0066ff; font-size: 11px; font-style: normal; display: grid; place-items: center; }.bank-option small { position: absolute; top: 10px; right: 9px; color: #e5484d; font-size: 8px; }.bank-mark, .certificate-mark { display: grid; place-items: center; border-radius: 7px; font-weight: 900; }.bank-mark { width: 25px; height: 25px; font-size: 10px; }.bank-mark.mint { color: #047857; background: #d1fae5; }.bank-mark.yellow { color: #3f3100; background: #ffe44d; }.bank-mark.blue { color: white; background: #1688e8; }.bank-mark.green { color: white; background: #10b981; }.bank-mark.navy { color: white; background: #263f8c; }
.terms-backdrop { height: 100vh; color: rgba(17,24,39,.75); background: #999; }.terms-copy { padding: 24px 31px; }.terms-copy h3 { margin: 0 0 13px; font-size: 14px; }.terms-copy p, .terms-copy small { display: block; margin: 0 0 5px; font-size: 10px; }
.certificate-sheet { position: absolute; z-index: 5; left: 9px; right: 9px; bottom: 10px; padding: 20px; border-radius: 24px; background: white; box-shadow: 0 -8px 30px rgba(0,0,0,.12); }.sheet-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 13px; }.sheet-heading h2 { margin: 0; font-size: 18px; }.sheet-heading button { color: #64748b; background: transparent; font-size: 23px; }
.certificate-option { position: relative; width: 100%; min-height: 49px; margin-top: 8px; padding: 10px 12px; display: flex; gap: 11px; align-items: center; text-align: left; border: 1px solid #e5e7eb; border-radius: 11px; background: white; }.certificate-option.selected { align-items: flex-start; border: 1.5px solid #0754a6; background: #dceeff; }.certificate-option > span:nth-child(2) { flex: 1; }.certificate-option strong { font-size: 12px; }.certificate-option em { margin-left: 8px; padding: 3px 7px; border-radius: 8px; color: #1688e8; background: white; font-size: 8px; font-style: normal; }.certificate-option small { display: block; margin-top: 8px; color: #555; font-size: 9px; }.certificate-option b { color: #15803d; font-size: 20px; }.certificate-mark { width: 22px; height: 22px; font-size: 11px; }.certificate-mark.kakao { color: #111; background: #fee500; }.certificate-mark.naver { color: white; background: #03c75a; }.certificate-mark.neutral { color: #64748b; background: #f1f5f9; }
.agreement-row { margin: -35px 13px 10px 48px; padding-top: 9px; display: grid; grid-template-columns: auto 1fr auto; gap: 6px; align-items: center; border-top: 1px solid #76b8ed; color: #073985; font-size: 9px; font-weight: 800; }.agreement-row input { accent-color: #0754a6; }.sheet-button { margin-top: 10px; min-height: 48px; border-radius: 8px; background: #0c4594; }
.connection-content { padding: 25px 20px 105px; }.connection-ticket { position: relative; min-height: 360px; padding: 20px; border-radius: 18px; }.connection-ticket h2 { margin: 0 0 6px; font-size: 19px; }.connection-ticket > span { color: #c8d6ff; font-size: 10px; }.flight-path { height: 130px; display: flex; align-items: center; justify-content: center; color: #86aaf8; }.flight-path i { letter-spacing: 12px; opacity: .75; }.flight-path b { margin-left: -60px; color: #fff; font-size: 30px; transform: rotate(-8deg); }.progress-label { display: flex; justify-content: space-between; color: #c8d6ff; font-size: 9px; }.progress-label strong { color: white; }.progress-track { height: 7px; margin: 8px 0; overflow: hidden; border-radius: 10px; background: rgba(255,255,255,.2); }.progress-track i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #2f8dff, #32d6e8); transition: width .2s ease; }.connection-ticket > small { color: #b8c8f7; font-size: 8px; }.connection-steps { margin-top: 20px; padding: 18px; border-radius: 15px; background: white; box-shadow: 0 6px 16px rgba(15,23,42,.08); }.connection-steps p { margin: 0 0 12px; color: #94a3b8; font-size: 11px; font-weight: 700; }.connection-steps p:last-child { margin: 0; }.connection-steps .done { color: #10b981; }.connection-steps .active { color: #0066ff; }
.connection-ticket.complete { min-height: 375px; }.completion-check { margin: 25px 0 8px; color: #26d6a4; font-size: 38px; }.complete-stat { padding: 14px 0; display: flex; justify-content: space-between; border-top: 1px dashed rgba(255,255,255,.3); }.complete-stat span { color: #c8d6ff; font-size: 10px; }.complete-stat strong { font-size: 16px; }.barcode { margin-top: 12px; color: #fff; text-align: right; font-family: monospace; letter-spacing: 1px; }
.step-progress { padding: 0 24px 16px; display: grid; grid-template-columns: repeat(3,1fr); gap: 14px; }.step-progress i { height: 4px; border-radius: 4px; background: #dde3ee; }.step-progress i:first-child, .step-progress i.filled { background: #263f8c; }
.form-content, .category-content { padding: 0 20px 100px; }.form-content h2, .category-content h2 { margin: 0; font-size: 19px; line-height: 1.45; }.summary-ticket { position: relative; margin: 16px 0 18px; padding: 15px 18px; min-height: 88px; border-radius: 18px; }.summary-ticket small { display: block; margin-bottom: 7px; color: #c8d6ff; font-size: 9px; }.summary-ticket strong { display: block; font-size: 25px; }.summary-ticket > span { position: absolute; right: 18px; bottom: 16px; font-size: 10px; }
.form-card { margin-bottom: 10px; padding: 15px; border-radius: 17px; background: white; box-shadow: 0 6px 18px rgba(15,23,42,.08); }.form-card-title { display: flex; align-items: center; gap: 10px; margin-bottom: 13px; }.form-card-title input { min-width: 0; flex: 1; border: 0; outline: none; color: #111827; font-size: 13px; font-weight: 800; }.form-card-title button { padding: 5px; color: #e5484d; background: transparent; font-size: 9px; }.field-grid { display: grid; gap: 10px; }.field-grid.two { grid-template-columns: .75fr 1.25fr; }.field-grid.account-memo { grid-template-columns: 1.8fr .8fr; margin-top: 10px; }.field-grid label, .full-field { position: relative; color: #64748b; font-size: 9px; }.field-grid input, .field-grid select, .full-field input, .full-field select { width: 100%; height: 43px; margin-top: 6px; padding: 0 13px; border: 1px solid #e5eaf2; border-radius: 10px; outline: 0; color: #111827; background: white; font-size: 11px; }.field-grid em { position: absolute; right: 11px; bottom: 13px; color: #94a3b8; font-size: 9px; font-style: normal; }.field-grid input { padding-right: 27px; }.full-field { display: block; margin-top: 10px; }.add-row { width: 100%; height: 48px; border-radius: 14px; color: #0066ff; background: #dde5ff; font-size: 12px; font-weight: 800; }
.fixed-form { padding-bottom: 90px; }.expense-card .feature-icon { width: 38px; height: 38px; }.expense-card .form-card-title { margin-bottom: 10px; }.expense-card .form-card-title strong { flex: 1; color: #111827; font-size: 13px; }.expense-name { margin: 0 0 10px; }.expense-name > em { color: #e5484d; font-style: normal; }.expense-name input:focus { border-color: #263f8c; }
.category-content > .subcopy { margin-bottom: 23px; }.category-row { height: 66px; margin-bottom: 15px; padding: 10px 11px; display: flex; align-items: center; gap: 14px; border: 1px solid #e5eaf2; border-radius: 14px; background: white; }.category-row > div { flex: 1; }.category-row strong, .category-row small { display: block; }.category-row strong { font-size: 13px; }.category-row small { margin-top: 5px; color: #64748b; font-size: 9px; }.category-row label { display: flex; align-items: center; gap: 6px; }.category-row input { width: 130px; height: 44px; padding: 0 12px; border: 1.5px solid #d6dce7; border-radius: 11px; outline: none; color: #111827; background: white; text-align: right; font-size: 16px; font-weight: 800; }.category-row em { color: #96a1b5; font-size: 11px; font-style: normal; }.category-total { min-height: 76px; margin-top: 38px; padding: 15px 18px; display: flex; justify-content: space-between; align-items: flex-start; border-radius: 16px; color: #0066ff; background: #eef2ff; font-size: 11px; font-weight: 800; }.category-total strong { color: #263f8c; font-size: 20px; }
@media (min-width: 500px) { .finance-shell { min-height: 879px; margin: 20px 0; border-radius: 28px; box-shadow: 0 10px 28px rgba(15,23,42,.16); } }
</style>
