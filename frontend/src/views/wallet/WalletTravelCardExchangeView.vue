<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from '@lucide/vue'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTripWalletStore } from '@/stores/tripWallet'
import { countryPresentation, useTravelStore } from '@/stores/travel'

const router = useRouter()
const wallet = useTripWalletStore()
const travel = useTravelStore()

const fallbackCurrencies = [
  { code: 'EUR', name: '유로', symbol: '€', rate: 1486.2, unit: 1, flagClass: 'fi fi-eu' },
  { code: 'JPY', name: '일본 엔', symbol: '¥', rate: 942, unit: 100, flagClass: 'fi fi-jp' },
  { code: 'CHF', name: '스위스 프랑', symbol: '₣', rate: 1606, unit: 1, flagClass: 'fi fi-ch' },
  { code: 'USD', name: '미국 달러', symbol: '$', rate: 1375, unit: 1, flagClass: 'fi fi-us' },
]

const flagClassMap = { EUR: 'fi fi-eu', JPY: 'fi fi-jp', CHF: 'fi fi-ch', USD: 'fi fi-us', GBP: 'fi fi-gb', CNY: 'fi fi-cn' }
const exchangeMode = ref('BUY')
const selectedCurrency = ref('JPY')
const foreignInput = ref('')
const krwInput = ref('100000')
const lastEdited = ref('krw')
const notice = ref('')
const estimate = ref(null)
let noticeTimer
let estimateTimer

const activeCountryCurrencies = computed(() => {
  const countries = travel.activeTrip?.countries || travel.selectedPlans || []
  return countries.map((country) => {
    const countryName = country.countryName || country.name || ''
    const presentation = countryPresentation[countryName] || {}
    return {
      countryName,
      currencyCode: String(country.currencyCode || country.currency || '').toUpperCase(),
      flagClass: country.flagClass || (presentation.code ? `fi fi-${String(presentation.code).toLowerCase()}` : ''),
    }
  }).filter(item => item.currencyCode && item.currencyCode !== 'KRW')
})

const currencies = computed(() => {
  const source = wallet.currencies?.length ? wallet.currencies : fallbackCurrencies
  const byCode = new Map(source.map(item => [String(item.code).toUpperCase(), item]))
  const options = activeCountryCurrencies.value.length
    ? activeCountryCurrencies.value.map(country => ({ ...byCode.get(country.currencyCode), ...country }))
    : source.map(item => ({ ...item, currencyCode: String(item.code).toUpperCase(), countryName: item.name || item.currencyName || item.code }))

  return options
    .filter(item => item.currencyCode && item.currencyCode !== 'KRW')
    .map((item, index) => ({
      key: `${item.countryName}:${item.currencyCode}:${index}`,
      code: item.currencyCode,
      name: item.countryName,
      symbol: item.symbol || item.currencyCode,
      // /wallet/currencies 목록 API는 rate를 내려주지 않는다(통화 기본 정보만 제공).
      // 여기서 1로 기본값을 주면 예상 환율(estimate) 응답이 오기 전 "가짜 환율 1"이
      // 화면에 잠깐 표시·애니메이션되고, 이후 실제 환율로 다시 튀어 두 번 카운팅되는 것처럼 보인다.
      // 실제 환율이 없을 때는 0으로 둬서 estimate가 도착했을 때 한 번만 움직이게 한다.
      rate: Number(item.rate || item.dealBaseRate || 0),
      unit: Number(item.unit || 1),
      flagClass: item.flagClass || flagClassMap[item.currencyCode] || 'fi fi-un',
    }))
})

const selected = computed(() => currencies.value.find(item => item.key === selectedCurrency.value || item.code === selectedCurrency.value) ?? currencies.value[0] ?? fallbackCurrencies[0])
const walletBalance = computed(() => Number(wallet.balance || 0))
const krwValue = computed(() => numberOnly(krwInput.value))
const foreignValue = computed(() => Number(String(foreignInput.value).replace(/,/g, '')) || 0)
const feeRate = computed(() => Number(estimate.value?.feeRate ?? 0))
// appliedRate는 항상 "1단위당 원화" 환율이다(예: JPY 9.4원/1엔).
// selected.unit(예: JPY=100)은 "100엔 = 940원"처럼 보기 좋게 묶어 보여주기 위한 표시 전용 값이라,
// 실제 금액 환산에는 절대 곱하거나 나누면 안 된다.
const appliedRate = computed(() => Number(estimate.value?.appliedExchangeRate || selected.value?.rate || 0))
const foreignBalance = computed(() => Number(wallet.foreignBalances.find(item => item.code === selected.value?.code)?.amount || 0))
const maxForeignCharge = computed(() => walletBalance.value / (appliedRate.value || 1))
const canSubmit = computed(() => exchangeMode.value === 'BUY'
  ? krwValue.value > 0 && krwValue.value <= walletBalance.value && foreignValue.value > 0
  : foreignValue.value > 0 && foreignValue.value <= foreignBalance.value && krwValue.value > 0)
// "100 JPY = 940원"처럼 표시 단위(unit)만큼 묶은 원화 금액. 실제 환전 계산과는 무관한 표시 전용 값이다.
const unitRateAmount = computed(() => (selected.value?.unit || 1) * appliedRate.value)
// estimate 응답이 오기 전에는 appliedRate가 0이라 "0원"이 잠깐 보이게 된다.
// 그 대신 실제 환율이 준비될 때까지 스켈레톤을 보여준다.
const rateReady = computed(() => appliedRate.value > 0)

function numberOnly(value) {
  return Number(String(value || '').replace(/[^0-9]/g, '')) || 0
}

function formatKrw(value) {
  return `${Math.round(Number(value || 0)).toLocaleString('ko-KR')}원`
}

function formatForeign(value = 0) {
  return Number(value || 0).toLocaleString('ko-KR', {
    minimumFractionDigits: selected.value?.code === 'JPY' ? 0 : 2,
    maximumFractionDigits: selected.value?.code === 'JPY' ? 0 : 2,
  })
}

function calcForeignFromKrw(krw) {
  return Number(krw || 0) / (appliedRate.value || 1)
}

function calcKrwFromForeign(foreign) {
  return Number(foreign || 0) * (appliedRate.value || 1)
}

function setKrw(value) {
  lastEdited.value = 'krw'
  krwInput.value = value
}

function setForeign(value) {
  lastEdited.value = 'foreign'
  foreignInput.value = value
}

function syncFromKrw() {
  const next = estimate.value?.foreignAmount ?? calcForeignFromKrw(krwValue.value)
  foreignInput.value = krwValue.value ? formatForeign(next) : ''
}

function syncFromForeign() {
  const next = estimate.value?.krwAmount ?? calcKrwFromForeign(foreignValue.value)
  krwInput.value = foreignValue.value ? String(Math.round(next)) : ''
}

function fillAll() {
  if (exchangeMode.value === 'BUY') setKrw(String(walletBalance.value))
  else setForeign(String(foreignBalance.value))
}

function showNotice(message) {
  notice.value = message
  window.clearTimeout(noticeTimer)
  noticeTimer = window.setTimeout(() => {
    notice.value = ''
  }, 3000)
}

function scheduleEstimate() {
  window.clearTimeout(estimateTimer)
  estimateTimer = window.setTimeout(requestEstimate, 250)
}

async function requestEstimate() {
  const amount = lastEdited.value === 'foreign' ? foreignValue.value : krwValue.value
  if (!amount || !selected.value?.code) return

  try {
    estimate.value = await wallet.estimateExchange({
      exchangeType: exchangeMode.value,
      currencyCode: selected.value.code,
      amount,
    })
    lastEdited.value === 'foreign' ? syncFromForeign() : syncFromKrw()
  } catch {
    lastEdited.value === 'foreign' ? syncFromForeign() : syncFromKrw()
  }
}

onBeforeUnmount(() => {
  window.clearTimeout(noticeTimer)
  window.clearTimeout(estimateTimer)
})

async function submitExchange() {
  if (!canSubmit.value) {
    const message = exchangeMode.value === 'BUY'
      ? (krwValue.value > walletBalance.value ? '트립월렛 잔액 안에서 충전해 주세요.' : '충전할 금액을 입력해 주세요.')
      : (foreignValue.value > foreignBalance.value ? '보유 외화 안에서 빼 주세요.' : '뺄 외화 금액을 입력해 주세요.')
    showNotice(message)
    return
  }

  try {
    const result = exchangeMode.value === 'BUY'
      ? await wallet.exchangeToTravelCard({
          currencyCode: selected.value.code,
          krwAmount: krwValue.value,
          walletTravelCardId: wallet.travelCard?.walletTravelCardId,
        })
      : await wallet.sellFromTravelCard({
          currencyCode: selected.value.code,
          foreignAmount: foreignValue.value,
          walletTravelCardId: wallet.travelCard?.walletTravelCardId,
        })

    if (!result) {
      showNotice(exchangeMode.value === 'BUY' ? '외화 충전에 실패했어요.' : '외화 빼기에 실패했어요.')
      return
    }

    if (result.status === 'FAILED') {
      showNotice('카드사 처리 중 일시 오류가 발생했어요. 잠시 후 자동으로 재시도돼요.')
      return
    }

    showNotice(exchangeMode.value === 'BUY'
      ? `${selected.value.name} ${selected.value.code} ${formatForeign(foreignValue.value)}이 트래블카드에 충전됐어요.`
      : `${selected.value.name} ${selected.value.code} ${formatForeign(foreignValue.value)}을 월렛 원화로 옮겼어요.`)
    setTimeout(() => router.push('/wallet'), 700)
  } catch {
    showNotice(wallet.errorMessage || (exchangeMode.value === 'BUY' ? '외화 충전에 실패했어요.' : '외화 빼기에 실패했어요.'))
  }
}

watch([krwInput, selectedCurrency], () => {
  if (lastEdited.value === 'krw') scheduleEstimate()
})

watch(foreignInput, () => {
  if (lastEdited.value === 'foreign') scheduleEstimate()
})

watch(selectedCurrency, () => {
  estimate.value = null
  scheduleEstimate()
})

watch(exchangeMode, () => {
  estimate.value = null
  lastEdited.value = exchangeMode.value === 'BUY' ? 'krw' : 'foreign'
  if (exchangeMode.value === 'SELL' && !foreignInput.value) foreignInput.value = String(foreignBalance.value || '')
  scheduleEstimate()
})

onMounted(async () => {
  try {
    await Promise.all([wallet.loadWalletMain(), wallet.loadCurrencies(), wallet.loadForeignBalances(), travel.loadActiveGoal({ force: true })])
  } catch {
    showNotice(wallet.errorMessage || '외화 충전 정보를 불러오지 못했어요.')
  }
  if (!currencies.value.find(item => item.code === selectedCurrency.value)) {
    selectedCurrency.value = currencies.value[0]?.key || 'EUR'
  }
  await requestEstimate()
})
</script>

<template>
  <main class="exchange-page">
    <header class="exchange-header">
      <button type="button" class="back-button" @click="router.back()"><ChevronLeft :size="22" /></button>
      <div>
        <h1>트래블카드 외화</h1>
      </div>
      <span aria-hidden="true"></span>
    </header>

    <div class="exchange-mode-tabs" role="tablist" aria-label="외화 이동 방식">
      <button type="button" :class="{ active: exchangeMode === 'BUY' }" @click="exchangeMode = 'BUY'">외화 충전</button>
      <button type="button" :class="{ active: exchangeMode === 'SELL' }" @click="exchangeMode = 'SELL'">외화 빼기</button>
    </div>

    <section class="intro-card">
      <p>{{ exchangeMode === 'BUY' ? '트립월렛 원화를 외화로 바꿔 카드에 바로 충전해요.' : '트래블카드 외화를 원화로 바꿔 트립월렛에 돌려놓아요.' }}</p>
      <div class="rate-strip">
        <strong>
          {{ selected.unit ? selected.unit.toLocaleString('ko-KR') : 1 }} {{ selected.code }} =
          <span v-if="rateReady">{{ formatKrw(unitRateAmount) }}</span>
          <span v-else class="rate-skeleton" aria-hidden="true" />
        </strong>
        <em>환율 우대 100%</em>
      </div>
    </section>

    <section class="charge-card">
      <div class="money-box krw-box">
        <div class="money-head">
          <div class="wallet-label">
            <i>TW</i>
            <b>트립월렛</b>
          </div>
          <input
            :value="krwInput"
            inputmode="numeric"
            placeholder="원화로 입력"
            @input="setKrw($event.target.value)"
          >
          <strong>원</strong>
        </div>
        <div class="money-foot">
          <small>{{ exchangeMode === 'BUY' ? '보유' : '받을 금액' }} {{ formatKrw(exchangeMode === 'BUY' ? walletBalance : krwValue) }}</small>
          <button v-if="exchangeMode === 'BUY'" type="button" @click="fillAll">전액 입력</button>
        </div>
      </div>

      <div class="flow-arrow">
        <b>{{ exchangeMode === 'BUY' ? '↓' : '↑' }}</b>
      </div>

      <div class="money-box foreign-box">
        <div class="money-head">
          <div class="currency-select">
            <span :class="selected.flagClass" />
            <label>
              <b>{{ selected.code }}</b>
              <small>{{ selected.name }}</small>
              <i class="dropdown-arrow">▾</i>
              <select v-model="selectedCurrency">
                <option v-for="item in currencies" :key="item.key" :value="item.key">
                  {{ item.code }} {{ item.name }}
                </option>
              </select>
            </label>
          </div>
          <input
            :value="foreignInput"
            inputmode="decimal"
            placeholder="외화로 입력"
            @input="setForeign($event.target.value)"
          >
        </div>
        <div class="money-foot">
          <small>보유 {{ formatForeign(foreignBalance) }} {{ selected.code }}</small>
          <button v-if="exchangeMode === 'SELL'" type="button" @click="fillAll">전액 입력</button>
          <small v-else>충전 가능 {{ formatForeign(maxForeignCharge) }} {{ selected.code }}</small>
        </div>
      </div>

      <dl class="summary">
        <div>
          <dt>{{ exchangeMode === 'BUY' ? '월렛 차감 금액' : '월렛 입금 예정' }}</dt>
          <dd>{{ formatKrw(krwValue) }}</dd>
        </div>
        <div>
          <dt>{{ exchangeMode === 'BUY' ? '트래블카드 충전 외화' : '트래블카드에서 뺄 외화' }}</dt>
          <dd>{{ selected.code }} {{ formatForeign(foreignValue) }}</dd>
        </div>
        <div>
          <dt>환전 수수료</dt>
          <dd>{{ feeRate }}%</dd>
        </div>
      </dl>

    </section>

    <button type="button" class="submit-button" @click="submitExchange">{{ exchangeMode === 'BUY' ? '외화 충전하기' : '외화 빼기' }}</button>
    <p v-if="notice" class="exchange-toast">{{ notice }}</p>

    <BottomNav />
  </main>
</template>

<style scoped>
.exchange-page{max-width:430px;min-height:100vh;margin:0 auto;padding:46px 18px 96px;background:#f2f5fa;color:#111827}.exchange-header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center}.exchange-header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;box-shadow:0 5px 16px rgba(36,72,117,.07)}.exchange-header p{color:#1f5ab9;font-size:11px;font-weight:800;letter-spacing:.04em;text-align:center}.exchange-header h1{margin-top:2px;font-size:26px;font-weight:800;letter-spacing:-.04em;text-align:center}.intro-card{margin-top:22px}.intro-card>p{display:inline-flex;padding:9px 13px;border-radius:999px;background:#eef5ff;color:#2764d7;font-size:13px;font-weight:700;line-height:1.35}.rate-strip{display:flex;align-items:center;justify-content:space-between;gap:14px;margin-top:14px;padding:18px;border:1px solid #dce6f3;border-radius:18px;background:#fff;box-shadow:0 8px 18px rgba(18,43,82,.04)}.rate-strip strong{font-size:20px;font-weight:700;letter-spacing:-.04em}.rate-skeleton{display:inline-block;width:72px;height:.9em;border-radius:6px;background:linear-gradient(90deg,#e5eaf2 25%,#eef2f8 37%,#e5eaf2 63%);background-size:400% 100%;animation:rate-skeleton-shimmer 1.4s ease infinite;vertical-align:middle}@keyframes rate-skeleton-shimmer{0%{background-position:100% 50%}100%{background-position:0 50%}}.rate-strip em{flex:none;padding:9px 13px;border-radius:12px;background:#7a45ee;color:#fff;font-size:13px;font-style:normal;font-weight:800}.charge-card{margin-top:18px;padding:22px 18px;border-radius:28px;background:#fff;box-shadow:0 12px 28px rgba(18,43,82,.08)}.money-box{padding:16px 0 15px}.money-head{display:grid;width:100%;min-width:0;align-items:center;gap:8px}.krw-box .money-head{grid-template-columns:118px minmax(0,1fr) 18px}.foreign-box .money-head{grid-template-columns:132px minmax(0,1fr)}.currency-select{display:flex;width:132px;min-width:0;align-items:center;gap:8px;overflow:visible}.currency-select>span{flex:0 0 26px;width:26px;height:26px;border-radius:50%;box-shadow:0 0 0 1px #d9e1ec}.currency-select label{position:relative;display:flex;min-width:0;flex:1;align-items:baseline;gap:4px;overflow:visible;white-space:nowrap}.currency-select label b{color:#1f2937;font-size:15px;font-weight:800}.currency-select small{color:#8b98ad;font-size:10px;font-weight:600;white-space:nowrap}.currency-select .dropdown-arrow{flex:none;color:#8b98ad;font-size:11px;font-style:normal;line-height:1;pointer-events:none}.currency-select select{position:absolute;inset:0;width:100%;height:100%;opacity:0;cursor:pointer}.wallet-label{display:flex;min-width:0;width:100%;align-items:center;gap:9px;overflow:hidden}.wallet-label i{display:grid;flex:0 0 30px;width:30px;height:30px;place-items:center;border-radius:50%;background:#ffd429;color:#111827;font-size:11px;font-style:normal;font-weight:800}.wallet-label b{min-width:0;overflow:hidden;font-size:17px;font-weight:700;text-overflow:ellipsis;white-space:nowrap}.money-head input{display:block;width:100%;min-width:0;max-width:100%;box-sizing:border-box;color:#111827;font-size:clamp(17px,5vw,23px);font-weight:800;text-align:right;outline:0}.money-head input::placeholder{color:#c7ccd5;font-size:clamp(13px,3.4vw,16px);font-weight:700}.money-head strong{font-size:17px;font-weight:800}.money-foot{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-top:13px}.money-foot small{min-width:0;color:#5f6775;font-size:14px;font-weight:500}.money-foot button{flex:none;padding:6px 11px;border-radius:999px;background:#edf4ff;color:#1768f2;font-size:12px;font-weight:800}.flow-arrow{display:flex;justify-content:center;padding:14px 0;color:#64748b;text-align:center}.flow-arrow b{display:grid;width:42px;height:42px;place-items:center;border-radius:50%;background:#eef5ff;color:#1768f2;font-size:24px}.summary{display:grid;gap:12px;margin-top:18px;padding:16px;border-radius:18px;background:#f7f9fd}.summary div{display:flex;align-items:center;justify-content:space-between}.summary dt{color:#7b8798;font-size:13px;font-weight:600}.summary dd{font-size:15px;font-weight:800}.guide{margin-top:16px;color:#8b98ad;font-size:12px;font-weight:600;line-height:1.5}.submit-button{width:100%;height:58px;margin-top:18px;border-radius:17px;background:#1768f2;color:#fff;font-size:18px;font-weight:800;box-shadow:0 12px 24px rgba(23,104,242,.22)}.exchange-toast{position:fixed;bottom:78px;left:50%;z-index:120;width:max-content;max-width:calc(100% - 40px);transform:translateX(-50%);padding:11px 15px;border-radius:99px;background:#172644;color:#fff;font-size:12px;box-shadow:0 5px 18px rgba(17,24,39,.2)}@media(max-width:380px){.rate-strip{align-items:flex-start;flex-direction:column}.krw-box .money-head{grid-template-columns:104px minmax(0,1fr) 16px}.foreign-box .money-head{grid-template-columns:122px minmax(0,1fr)}.currency-select{width:122px}.currency-select>span{flex-basis:24px;width:24px;height:24px}.currency-select label b{font-size:14px}.currency-select small{font-size:9px}.wallet-label b{font-size:15px}.money-head input{font-size:17px}}
.exchange-page{word-break:keep-all}.exchange-page h1,.exchange-page h2,.exchange-page h3{text-wrap:balance}.exchange-page p{text-wrap:pretty}
.exchange-mode-tabs{display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-top:18px;padding:4px;border-radius:16px;background:#e6ebf4}.exchange-mode-tabs button{height:44px;border-radius:13px;color:#8290a6;font-size:13px;font-weight:800}.exchange-mode-tabs button.active{background:#fff;color:#174496;box-shadow:0 5px 14px rgba(20,54,105,.1)}
.exchange-page{padding:14px 16px 88px;background:#f2f5fa}.exchange-header button{width:36px;height:36px}.exchange-header h1{color:#29466f;font-size:20px;font-weight:700;letter-spacing:-.025em}.exchange-header p{font-size:9.5px}.intro-card{margin-top:17px}.intro-card>p{padding:7px 10px;font-size:10.5px}.rate-strip{margin-top:11px;padding:13px;border-radius:14px}.rate-strip strong{font-size:16px}.rate-strip em{padding:7px 10px;border-radius:9px;font-size:10.5px}.charge-card{margin-top:14px;padding:17px 15px;border-radius:21px}.money-box{padding:12px 0}.currency-select label b{font-size:13px}.wallet-label b{font-size:14px}.money-head input{font-size:clamp(15px,4.4vw,19px)}.money-head strong{font-size:14px}.money-foot{margin-top:9px}.money-foot small{font-size:11px}.money-foot button{padding:5px 9px;font-size:10px}.flow-arrow{padding:10px 0}.flow-arrow b{width:34px;height:34px;font-size:19px}.summary{gap:9px;margin-top:14px;padding:13px;border-radius:14px}.summary dt{font-size:10.5px}.summary dd{font-size:12px}.guide{margin-top:12px;font-size:10.5px}.submit-button{height:44px;margin-top:15px;border-radius:13px;font-size:13px;font-weight:700}
</style>
