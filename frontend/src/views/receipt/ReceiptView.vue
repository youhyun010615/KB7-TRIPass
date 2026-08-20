<script setup>
import {
  computed,
  onMounted,
  ref,
  watch,
} from 'vue'
import {
  useRoute,
  useRouter,
} from 'vue-router'
import {
  CalendarRange,
  Camera,
  Check,
  ChevronLeft,
  ChevronRight,
  HandCoins,
  PenLine,
  ReceiptText,
  Users,
} from '@lucide/vue'

import BottomNav from '@/components/common/BottomNav.vue'

import {
  getReceipts,
} from '@/api/receipt'

import {
  fetchTripGoal,
} from '@/api/travel'

const route = useRoute()
const router = useRouter()

const tripId = computed(() => {
  const value = Number(route.params.tripId)

  return Number.isInteger(value) && value > 0
      ? value
      : null
})

const trip = ref(null)
const receipts = ref([])
const totalReceiptCount = ref(0)

const selectedCountryId = ref(null)
const dateFilterEnabled = ref(false)
const startDate = ref('')
const endDate = ref('')
const dateFilterError = ref('')

const loading = ref(false)
const errorMessage = ref('')

const tripTitle = computed(() =>
    trip.value?.tripName ||
    '여행 정보 확인 중',
)

const countries = computed(() => {
  const tripCountries =
      trip.value?.countries ?? []

  return [
    {
      countryId: null,
      countryName: '전체',
    },

    ...tripCountries.map(country => ({
      countryId:
          Number(country.countryId),

      countryName:
      country.countryName,

      currencyCode:
      country.currencyCode,
    })),
  ]
})

const filteredReceipts = computed(() => {
  if (selectedCountryId.value === null) {
    return receipts.value
  }

  return receipts.value.filter(
      receipt =>
          Number(receipt.countryId) ===
          Number(selectedCountryId.value),
  )
})

const groups = computed(() => {
  const dateMap = new Map()

  filteredReceipts.value.forEach(receipt => {
    if (!dateMap.has(receipt.date)) {
      dateMap.set(receipt.date, [])
    }

    dateMap.get(receipt.date).push(receipt)
  })

  return [...dateMap.entries()]
      .sort(
          ([firstDate], [secondDate]) =>
              secondDate.localeCompare(
                  firstDate,
              ),
      )
      .map(([date, items]) => ({
        date,

        items: [...items].sort(
            (first, second) =>
                second.time.localeCompare(
                    first.time,
                ),
        ),
      }))
})

/*
 * 현재 필터 결과를 통화별로 합산합니다.
 *
 * 국가가 전체인 경우 여러 통화가 섞일 수 있으므로
 * 임의로 서로 다른 통화를 더하지 않습니다.
 */
const currencySummaries = computed(() => {
  const summaryMap = new Map()

  filteredReceipts.value.forEach(receipt => {
    const currencyCode =
        receipt.currencyCode ||
        '통화 미지정'

    const currentAmount =
        summaryMap.get(currencyCode) ?? 0

    summaryMap.set(
        currencyCode,
        currentAmount +
        Number(receipt.totalAmount || 0),
    )
  })

  return [...summaryMap.entries()].map(
      ([currencyCode, totalAmount]) => ({
        currencyCode,
        totalAmount,
      }),
  )
})

const selectedCountryName = computed(() => {
  if (selectedCountryId.value === null) {
    return '전체 국가'
  }

  return (
      countries.value.find(
          country =>
              Number(country.countryId) ===
              Number(selectedCountryId.value),
      )?.countryName ||
      '선택 국가'
  )
})

function formatTripDateRange() {
  const start = trip.value?.startDate
  const end = trip.value?.endDate
  if (!start && !end) return '여행 일정을 확인하고 있어요'

  const format = (value, includeYear = false) => {
    if (!value) return ''
    const [year, month, day] = String(value).split('-')
    return includeYear ? `${year}. ${month}. ${day}` : `${month}. ${day}`
  }

  if (!start) return format(end, true)
  if (!end) return format(start, true)
  return `${format(start, true)} — ${format(end)}`
}

function formatGroupDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value))) return value
  const [year, month, day] = String(value).split('-').map(Number)
  const week = ['일', '월', '화', '수', '목', '금', '토']
  const dayOfWeek = week[new Date(year, month - 1, day).getDay()]
  return `${month}.${String(day).padStart(2, '0')} (${dayOfWeek})`
}

function separatePaymentDateTime(value) {
  if (!value) {
    return {
      date: '결제일 미확인',
      time: '시간 미확인',
    }
  }

  const [date = '', time = ''] =
      String(value).split('T')

  return {
    date:
        date ||
        '결제일 미확인',

    time:
        time.substring(0, 5) ||
        '시간 미확인',
  }
}

function mapReceipt(item) {
  const paymentDateTime =
      separatePaymentDateTime(
          item.paymentDateTime,
      )

  return {
    id:
    item.id,

    tripId:
    item.tripId,

    countryId:
    item.countryId,

    countryName:
        item.countryName ||
        '국가 미지정',

    merchant:
        item.merchantTranslatedName ||
        item.merchantOriginalName ||
        '상호명 미인식',

    originalMerchant:
        item.merchantOriginalName ||
        '',

    date:
    paymentDateTime.date,

    time:
    paymentDateTime.time,

    currencyCode:
        item.currencyCode ||
        '',

    currencySymbol:
        item.currencySymbol ||
        '',

    totalAmount:
        Number(item.totalAmount) || 0,

    splitCount:
        Number(item.splitCount) || 1,

    participantNames:
        item.participantNames || null,

    splitAmount:
        item.splitAmount != null &&
        Number.isFinite(
            Number(item.splitAmount),
        )
            ? Number(item.splitAmount)
            : (
                Number(item.splitCount) > 1
                    ? Number(
                        item.totalAmount || 0,
                    ) /
                    Number(item.splitCount)
                    : null
            ),

    fileUrl:
        item.fileUrl ||
        '',
  }
}

function formatAmount(value) {
  return Number(value || 0)
      .toLocaleString(
          'ko-KR',
          {
            maximumFractionDigits: 2,
          },
      )
}

async function loadPage() {
  if (!tripId.value) {
    trip.value = null
    receipts.value = []

    errorMessage.value =
        '여행 정보를 확인해 주세요.'

    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const [
      tripResponse,
      receiptResponse,
    ] = await Promise.all([
      fetchTripGoal(tripId.value),
      getReceipts(tripId.value, dateFilterEnabled.value
        ? {
            startDate: startDate.value || undefined,
            endDate: endDate.value || undefined,
          }
        : {}),
    ])

    trip.value =
        tripResponse ?? null

    const receiptData =
        receiptResponse.data?.data

    receipts.value =
        Array.isArray(receiptData)
            ? receiptData.map(mapReceipt)
            : []

    if (!dateFilterEnabled.value) {
      totalReceiptCount.value = receipts.value.length
    }

    /*
     * 현재 선택한 국가가 변경된 여행 정보에 없다면
     * 전체 필터로 되돌립니다.
     */
    if (
        selectedCountryId.value !== null &&
        !countries.value.some(
            country =>
                Number(country.countryId) ===
                Number(selectedCountryId.value),
        )
    ) {
      selectedCountryId.value = null
    }
  } catch (error) {
    trip.value = null
    receipts.value = []

    errorMessage.value =
        error.response?.data?.message ||
        error.message ||
        '영수증 보관함을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function showDateFilter() {
  dateFilterEnabled.value = true
  startDate.value ||= trip.value?.startDate || ''
  endDate.value ||= trip.value?.endDate || ''
  dateFilterError.value = ''
}

async function applyDateFilter() {
  if (startDate.value && endDate.value && startDate.value > endDate.value) {
    dateFilterError.value = '시작일은 종료일보다 빠르게 선택해 주세요.'
    return
  }

  dateFilterError.value = ''
  await loadPage()
}

async function resetDateFilter() {
  dateFilterEnabled.value = false
  startDate.value = ''
  endDate.value = ''
  dateFilterError.value = ''
  await loadPage()
}

function openSettlements() {
  if (!tripId.value) return
  router.push({ name: 'ReceiptSettlements', params: { tripId: tripId.value } })
}

function openReceipt(receiptId) {
  if (!tripId.value) {
    return
  }

  router.push({
    name: 'ReceiptDetail',

    params: {
      tripId:
      tripId.value,

      receiptId,
    },
  })
}

function openCapture() {
  if (!tripId.value) {
    return
  }

  router.push({
    name: 'ReceiptCapture',

    params: {
      tripId:
      tripId.value,
    },
  })
}

function openManualEntry() {
  if (!tripId.value) {
    return
  }

  router.push({
    name: 'ReceiptManualNew',

    params: {
      tripId:
      tripId.value,
    },
  })
}

watch(
    tripId,
    (currentTripId, previousTripId) => {
      if (
          currentTripId &&
          currentTripId !== previousTripId
      ) {
        selectedCountryId.value = null
        loadPage()
      }
    },
)

onMounted(loadPage)
</script>

<template>
  <main class="receipt-page">
    <header class="page-header">
      <button type="button" aria-label="뒤로 가기" @click="router.back()">
        <ChevronLeft :size="24" :stroke-width="2.4" />
      </button>
      <div>
        <small>TRIP RECORD</small>
        <h1>영수증 보관함</h1>
      </div>
      <span />
    </header>

    <section class="receipt-paper">
      <div class="paper-topline">
        <span>TRIPASS</span>
        <span>RECEIPT NO. {{ String(tripId || 0).padStart(4, '0') }}</span>
      </div>

      <div class="paper-brand">
        <span class="paper-brand-icon"><ReceiptText :size="24" :stroke-width="2.2" /></span>
        <div>
          <small>MY RECEIPT VAULT</small>
          <h2>{{ tripTitle }}</h2>
        </div>
      </div>

      <dl class="paper-trip-info">
        <div>
          <dt>TRIP DATE</dt>
          <dd>{{ formatTripDateRange() }}</dd>
        </div>
        <div>
          <dt>STATUS</dt>
          <dd><Check :size="12" :stroke-width="3" /> 보관 중</dd>
        </div>
      </dl>

      <div class="paper-divider"><span>RECEIPT SUMMARY</span></div>

      <div class="paper-total">
        <div>
          <small>보관된 영수증</small>
          <strong>{{ totalReceiptCount }}<em>장</em></strong>
        </div>
        <span class="stored-stamp">
          <Check :size="17" :stroke-width="3" />
          <span><b>보관 완료</b><small>안전하게 저장했어요</small></span>
        </span>
      </div>

      <p class="paper-footer">THANK YOU FOR TRAVELING WITH TRIPASS</p>
    </section>

    <nav class="vault-tabs" aria-label="영수증 보관함 메뉴">
      <button type="button" class="active">
        <ReceiptText :size="16" /> 영수증
      </button>
      <button type="button" @click="openSettlements">
        <HandCoins :size="17" /> 정산
      </button>
    </nav>

    <section class="filter-section">
      <div class="section-heading">
        <div>
          <small>COUNTRY FILTER</small>
          <h2>어디에서 사용했나요?</h2>
        </div>
        <span>{{ filteredReceipts.length }}장</span>
      </div>
      <div class="country-chips" role="group" aria-label="영수증 조회 국가 선택">
        <button
            v-for="country in countries"
            :key="country.countryId ?? 'all'"
            type="button"
            :class="{ active: selectedCountryId === country.countryId }"
            :disabled="loading"
            @click="selectedCountryId = country.countryId"
        >
          <Check v-if="selectedCountryId === country.countryId" :size="12" :stroke-width="3" />
          {{ country.countryName }}
        </button>
      </div>

      <div class="period-filter">
        <div>
          <span><CalendarRange :size="16" /> 조회 기간</span>
          <div class="period-toggle">
            <button
                type="button"
                :class="{ active: !dateFilterEnabled }"
                @click="resetDateFilter"
            >
              전체
            </button>
            <button
                type="button"
                :class="{ active: dateFilterEnabled }"
                @click="showDateFilter"
            >
              날짜 범위
            </button>
          </div>
        </div>

        <div v-if="dateFilterEnabled" class="date-range-fields">
          <label><small>시작일</small><input v-model="startDate" type="date" :max="endDate || undefined"></label>
          <i>—</i>
          <label><small>종료일</small><input v-model="endDate" type="date" :min="startDate || undefined"></label>
          <button type="button" :disabled="loading" @click="applyDateFilter">조회</button>
        </div>
        <p v-if="dateFilterError">{{ dateFilterError }}</p>
      </div>
    </section>

    <section
        v-if="
          !loading &&
          !errorMessage &&
          currencySummaries.length
        "
        class="summary-section"
    >
      <div class="summary-title">
        <div>
          <small>SELECTED AREA</small>
          <strong>{{ selectedCountryName }} 지출</strong>
        </div>
        <ReceiptText :size="22" />
      </div>

      <div class="currency-summary-list">
        <div
            v-for="summary in currencySummaries"
            :key="summary.currencyCode"
            class="currency-summary"
        >
          <small>{{ summary.currencyCode }}</small>
          <strong>{{ formatAmount(summary.totalAmount) }}</strong>
          <span>총 결제 금액</span>
        </div>
      </div>
    </section>

    <section class="receipt-list">
      <div class="title">
        <div>
          <small>RECEIPT HISTORY</small>
          <h2>보관된 영수증</h2>
        </div>
        <span>최신 결제순</span>
      </div>

      <div
          v-if="loading"
          class="empty"
      >
        <span>⌛</span>

        <b>
          영수증을 불러오고 있어요
        </b>
      </div>

      <div
          v-else-if="errorMessage"
          class="empty"
      >
        <span>!</span>

        <b>{{ errorMessage }}</b>

        <button
            type="button"
            class="retry-button"
            @click="loadPage"
        >
          다시 시도
        </button>
      </div>

      <template v-else>
        <div
            v-for="group in groups"
            :key="group.date"
            class="date-group"
        >
          <h3><span>{{ formatGroupDate(group.date) }}</span><i /></h3>

          <button
              v-for="item in group.items"
              :key="item.id"
              type="button"
              :class="{ shared: item.splitCount > 1 }"
              @click="openReceipt(item.id)"
          >
            <span class="category-icon">
              <Users v-if="item.splitCount > 1" :size="20" :stroke-width="2.2" />
              <ReceiptText v-else :size="20" :stroke-width="2.2" />
            </span>

            <div class="receipt-info">
              <div class="merchant-line">
                <b>{{ item.merchant }}</b>
                <span v-if="item.splitCount > 1">공동결제 {{ item.splitCount }}명</span>
              </div>
              <small>
                {{ item.countryName }}
                <i>·</i>
                {{ item.time }}
              </small>
              <i
                  v-if="item.splitCount > 1"
              >
                <template v-if="item.participantNames">함께: {{ item.participantNames }}</template>
                <template v-else>공동결제 참여자 확인</template>
              </i>
            </div>
            <div class="receipt-payment">
              <strong>{{ item.currencySymbol || item.currencyCode }} {{ formatAmount(item.totalAmount) }}</strong>
              <small v-if="item.splitCount > 1 && item.splitAmount != null">
                1인 {{ item.currencyCode }} {{ formatAmount(item.splitAmount) }}
              </small>
            </div>
            <ChevronRight class="row-chevron" :size="18" />
          </button>
        </div>

        <div
            v-if="!filteredReceipts.length"
            class="empty"
        >
          <span>🧾</span>

          <b>
            보관된 영수증이 없어요
          </b>

          <small>
            {{
              selectedCountryId === null
                  ? '이 여행에 등록된 영수증이 없습니다.'
                  : '선택한 국가에 등록된 영수증이 없습니다.'
            }}
          </small>
        </div>
      </template>
    </section>

    <div class="receipt-actions">
      <button
          type="button"
          class="manual-button"
          :disabled="loading || !tripId"
          @click="openManualEntry"
      >
        <PenLine :size="18" />
        <span>직접 입력</span>
      </button>

      <button
          type="button"
          class="scan-button"
          :disabled="loading || !tripId"
          @click="openCapture"
      >
        <Camera :size="19" />
        <span>영수증 촬영</span>
      </button>
    </div>

    <BottomNav />
  </main>
</template>

<style scoped>
.receipt-page {
  min-height: 100vh;
  padding: 0 18px 150px;
  background: #f8f6f1;
  color: #111a2d
}

.receipt-page > header {
  display: grid;
  height: 66px;
  grid-template-columns:36px 1fr 36px;
  align-items: end;
  padding-bottom: 17px
}

.receipt-page > header button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  background: #fff;
  color: #193d82;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 5px 16px rgba(36, 72, 117, 0.07);
}

.receipt-page > header h1 {
  text-align: center;
  font-size: 20px;
  font-weight: 900
}

.vault-ticket {
  position: relative;
  overflow: hidden;
  padding: 19px 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, #12347b, #1466cb);
  color: #fff;
  box-shadow: 0 8px 20px #173f8d24
}

.vault-ticket:before, .vault-ticket:after {
  position: absolute;
  top: 62px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #f8f6f1;
  content: ''
}

.vault-ticket:before {
  left: -11px
}

.vault-ticket:after {
  right: -11px
}

.vault-ticket small {
  color: #afcaf7;
  font-size: 8px;
  letter-spacing: 1px
}

.vault-ticket h2 {
  margin-top: 12px;
  font-size: 18px
}

.vault-ticket p {
  margin-top: 5px;
  color: #c9daf7;
  font-size: 10px
}

.vault-ticket div {
  display: flex;
  align-items: end;
  gap: 8px;
  margin-top: 22px;
  padding-top: 14px;
  border-top: 1px dashed #ffffff77
}

.vault-ticket div b {
  font-size: 26px
}

.vault-ticket div span {
  padding-bottom: 4px;
  color: #c9daf7;
  font-size: 10px
}

.vault-ticket i {
  position: absolute;
  right: 18px;
  bottom: 18px;
  font-style: normal;
  letter-spacing: -1px
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 2px
}

.filter-row b {
  font-size: 13px
}

.filter-row select {
  min-width: 108px;
  padding: 9px 12px;
  border: 1px solid #d7e0ed;
  border-radius: 12px;
  background: #fff;
  font-size: 11px
}

.receipt-list {
  padding: 18px;
  border: 1px solid #d8e1ec;
  border-radius: 20px;
  background: #fff
}

.title {
  display: flex;
  align-items: center;
  justify-content: space-between
}

.title h2 {
  font-size: 17px;
  font-weight: 900
}

.title span {
  color: #94a3b8;
  font-size: 9px
}

.receipt-list > button {
  display: grid;
  width: 100%;
  grid-template-columns:38px minmax(0, 1fr) auto 10px;
  align-items: center;
  gap: 9px;
  padding: 15px 2px;
  border-bottom: 1px solid #edf0f4;
  text-align: left
}

.receipt-list > button > span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  background: #f1f5ff;
  font-size: 18px
}

.receipt-list button b, .receipt-list button small {
  display: block
}

.receipt-list button b {
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap
}

.receipt-list button small {
  margin-top: 5px;
  color: #8a97aa;
  font-size: 8px
}

.receipt-list button strong {
  font-size: 10px;
  white-space: nowrap
}

.receipt-list button em {
  color: #94a3b8;
  font-size: 20px;
  font-style: normal
}

.empty {
  display: flex;
  min-height: 190px;
  flex-direction: column;
  align-items: center;
  justify-content: center
}

.empty span {
  font-size: 30px;
  color: #2e70dc
}

.empty b {
  margin-top: 12px;
  font-size: 13px
}

.empty small {
  margin-top: 6px;
  color: #8b98a9;
  font-size: 9px
}

.receipt-actions {
  position: fixed;
  right: max(calc((100vw - 390px) / 2 + 18px), 18px);
  bottom: 78px;
  left: max(calc((100vw - 390px) / 2 + 18px), 18px);
  z-index: 40;
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 8px;
}

.receipt-actions button {
  height: 55px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 900;
  box-shadow: 0 8px 18px #173f8d2e;
}

.manual-button {
  border: 1px solid #19489c;
  background: #fff;
  color: #19489c;
}

.scan-button {
  background: #19489c;
  color: #fff;
}

.date-group h3 {
  margin: 20px 2px 4px;
  color: #246dd7;
  font-size: 11px
}

.date-group > button {
  display: grid;
  width: 100%;
  grid-template-columns:38px minmax(0, 1fr) auto 10px;
  align-items: center;
  gap: 9px;
  padding: 15px 2px;
  border-bottom: 1px solid #edf0f4;
  text-align: left
}

.date-group > button > span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  background: #f1f5ff;
  font-size: 18px
}

.date-group button b, .date-group button small, .date-group button i {
  display: block
}

.date-group button b {
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap
}

.date-group button small {
  margin-top: 5px;
  color: #8a97aa;
  font-size: 8px
}

.date-group button i {
  margin-top: 3px;
  color: #7a8aa0;
  font-size: 7px;
  font-style: normal
}

.date-group button strong {
  font-size: 10px;
  white-space: nowrap
}

.date-group button em {
  color: #94a3b8;
  font-size: 20px;
  font-style: normal
}

.retry-button {
  margin-top: 12px;
  padding: 8px 14px;
  border: 1px solid #2670dd;
  border-radius: 9px;
  color: #2670dd;
  font-size: 10px;
  font-weight: 800;
}
.summary-section {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #d8e1ec;
  border-radius: 18px;
  background: #fff;
}

.summary-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-title div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-title small {
  color: #8a97aa;
  font-size: 9px;
}

.summary-title strong {
  font-size: 14px;
}

.summary-title > span {
  padding: 5px 9px;
  border-radius: 999px;
  background: #eaf2ff;
  color: #246dd7;
  font-size: 9px;
  font-weight: 800;
}

.currency-summary-list {
  display: grid;
  grid-template-columns:
      repeat(
          auto-fit,
          minmax(120px, 1fr)
      );
  gap: 8px;
  margin-top: 14px;
}

.currency-summary {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
  padding: 12px;
  border-radius: 12px;
  background: #f5f8fd;
}

.currency-summary small {
  color: #8a97aa;
  font-size: 9px;
}

.currency-summary strong {
  overflow: hidden;
  color: #246dd7;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-row select:disabled,
.receipt-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

/* TRIPASS receipt vault renewal */
.receipt-page {
  min-height: 100vh;
  padding: 0 18px 164px;
  background:
    radial-gradient(circle at 100% 0, rgba(47, 111, 237, 0.08), transparent 260px),
    #f4f7fc;
  color: #10192b;
}

.receipt-page > .page-header {
  display: grid;
  height: 82px;
  grid-template-columns: 42px 1fr 42px;
  align-items: center;
  padding-top: 10px;
}

.receipt-page > .page-header button {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 14px;
  background: #fff;
  color: #173f8d;
  box-shadow: 0 7px 20px rgba(26, 63, 132, 0.09);
}

.receipt-page > .page-header > div { text-align: center; }

.page-header small,
.section-heading small,
.title small,
.summary-title small {
  color: #2f6fed;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.16em;
}

.receipt-page > .page-header h1 {
  margin-top: 2px;
  font-size: 20px;
  font-weight: 950;
  letter-spacing: -0.04em;
}

.vault-ticket {
  position: relative;
  overflow: hidden;
  padding: 17px 18px 18px;
  border-radius: 25px;
  background:
    radial-gradient(circle at 92% 8%, rgba(255, 255, 255, 0.14) 0 66px, transparent 67px),
    linear-gradient(145deg, #0c2d72 0%, #174ca7 58%, #2876d8 100%);
  color: #fff;
  box-shadow: 0 18px 38px rgba(23, 73, 156, 0.24);
}

.vault-ticket::before,
.vault-ticket::after {
  top: 91px;
  width: 20px;
  height: 20px;
  background: #f4f7fc;
}

.vault-ticket::before { left: -10px; }
.vault-ticket::after { right: -10px; }

.vault-ticket .vault-ticket-head {
  display: flex;
  justify-content: space-between;
  gap: 0;
  margin: 0;
  padding: 0;
  border: 0;
  color: #bfd3f7;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.vault-ticket .vault-ticket-body {
  display: flex;
  align-items: center;
  gap: 13px;
  min-height: 82px;
  padding: 15px 0 12px;
  margin: 0;
  border-top: 0;
  border-bottom: 1px dashed rgba(210, 226, 255, 0.52);
}

.vault-ticket .vault-ticket-body > div {
  display: block;
  margin: 0;
  padding: 0;
  border: 0;
}

.vault-icon {
  display: grid;
  flex: 0 0 48px;
  height: 48px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffd466;
}

.vault-ticket-body small,
.vault-ticket-foot small {
  color: #98baf0;
  font-size: 7px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.vault-ticket h2 {
  margin-top: 3px;
  font-size: 18px;
  font-weight: 950;
  letter-spacing: -0.03em;
}

.vault-ticket-body p {
  margin-top: 5px;
  color: #cbdcf7;
  font-size: 9px;
  font-weight: 700;
}

.vault-ticket .vault-ticket-foot {
  position: relative;
  display: flex;
  min-height: 54px;
  align-items: end;
  justify-content: space-between;
  padding-top: 13px;
  border: 0;
}

.vault-ticket .vault-ticket-foot > div {
  display: grid;
  margin: 0;
  padding: 0;
  border: 0;
}

.vault-ticket .vault-ticket-foot b {
  margin-top: 1px;
  font-size: 25px;
  line-height: 1;
}

.vault-ticket .vault-ticket-foot b em {
  margin-left: 2px;
  font-size: 10px;
  font-style: normal;
}

.vault-complete {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: 76px;
  padding: 0;
  color: #dce9ff;
  font-size: 8px;
  font-weight: 800;
}

.vault-ticket-foot > i {
  position: absolute;
  right: 0;
  bottom: 1px;
  display: flex;
  gap: 2px;
  height: 28px;
  font-style: normal;
}

.vault-ticket-foot > i span { width: 2px; background: rgba(255, 255, 255, 0.9); }
.vault-ticket-foot > i span:nth-child(3n) { width: 1px; }

.filter-section,
.summary-section,
.receipt-list {
  border: 1px solid #e2e9f4;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 28px rgba(30, 64, 125, 0.07);
}

.filter-section {
  margin-top: 18px;
  padding: 17px;
  border-radius: 21px;
}

.section-heading,
.summary-title,
.title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-heading h2,
.title h2 {
  margin-top: 3px;
  font-size: 15px;
  font-weight: 950;
  letter-spacing: -0.03em;
}

.section-heading > span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef4ff;
  color: #2f6fed;
  font-size: 9px;
  font-weight: 900;
}

.country-chips {
  display: flex;
  overflow-x: auto;
  gap: 7px;
  margin: 14px -2px -2px;
  padding: 2px;
  scrollbar-width: none;
}

.country-chips::-webkit-scrollbar { display: none; }

.country-chips button {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 4px;
  height: 34px;
  padding: 0 13px;
  border: 1px solid #dce5f2;
  border-radius: 999px;
  background: #f8faff;
  color: #687891;
  font-size: 10px;
  font-weight: 850;
}

.country-chips button.active {
  border-color: #2f6fed;
  background: #2f6fed;
  color: #fff;
  box-shadow: 0 5px 13px rgba(47, 111, 237, 0.2);
}

.receipt-paper {
  position: relative;
  margin-bottom: 7px;
  padding: 0 20px 17px;
  border: 1px solid #e0e7f1;
  border-radius: 22px 22px 7px 7px;
  background:
    linear-gradient(rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0.94)),
    repeating-linear-gradient(0deg, #fff 0, #fff 3px, #f4f6f9 4px);
  box-shadow: 0 16px 34px rgba(37, 61, 105, 0.12);
}

.receipt-paper::after {
  position: absolute;
  right: -1px;
  bottom: -8px;
  left: -1px;
  height: 9px;
  background: radial-gradient(circle at 8px 0, #fff 7px, transparent 7.5px) 0 0 / 16px 9px repeat-x;
  content: '';
}

.paper-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 -20px;
  padding: 13px 18px;
  border-radius: 20px 20px 0 0;
  background: linear-gradient(135deg, #103579, #205bb9);
  color: #cfe0ff;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.13em;
}

.paper-topline span:first-child {
  color: #ffd466;
  font-size: 10px;
}

.paper-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 0 15px;
}

.paper-brand-icon {
  display: grid;
  width: 47px;
  height: 47px;
  place-items: center;
  border-radius: 15px;
  background: #eaf2ff;
  color: #205cb9;
}

.paper-brand small,
.paper-total > div > small {
  color: #8d9bb0;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.paper-brand h2 {
  margin-top: 3px;
  color: #10192b;
  font-size: 18px;
  font-weight: 950;
  letter-spacing: -0.04em;
}

.paper-trip-info {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr;
  gap: 10px;
  margin: 0;
  padding: 12px 13px;
  border-radius: 13px;
  background: #f6f8fc;
}

.paper-trip-info div { min-width: 0; }

.paper-trip-info dt {
  color: #9aa6b8;
  font-size: 7px;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.paper-trip-info dd {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 4px 0 0;
  overflow: hidden;
  color: #334158;
  font-size: 9px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.paper-trip-info div:last-child dd { color: #168374; }

.paper-divider {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 16px 0 12px;
  color: #a0adbf;
  font-size: 7px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.paper-divider::before,
.paper-divider::after {
  flex: 1;
  border-top: 1px dashed #cad4e1;
  content: '';
}

.paper-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.paper-total > div { display: grid; }

.paper-total strong {
  margin-top: 1px;
  color: #123b83;
  font-size: 29px;
  font-weight: 950;
  line-height: 1;
}

.paper-total strong em {
  margin-left: 3px;
  color: #67778f;
  font-size: 10px;
  font-style: normal;
}

.stored-stamp {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 11px;
  border: 1px solid #c7d9f5;
  border-radius: 12px;
  background: #f0f6ff;
  color: #245fb9;
}

.stored-stamp > span { display: grid; }
.stored-stamp b { font-size: 9px; font-weight: 950; }
.stored-stamp small { margin-top: 1px; color: #8296b4; font-size: 7px; }

.paper-footer {
  margin: 15px 0 0;
  padding-top: 10px;
  border-top: 1px solid #edf0f4;
  color: #a7b1c0;
  font-size: 6px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-align: center;
}

.vault-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5px;
  margin-top: 19px;
  padding: 5px;
  border: 1px solid #dfe7f3;
  border-radius: 16px;
  background: #eaf0f9;
}

.vault-tabs button {
  display: flex;
  height: 38px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 12px;
  color: #78879d;
  font-size: 10px;
  font-weight: 900;
}

.vault-tabs button.active {
  background: #fff;
  color: #17499c;
  box-shadow: 0 4px 12px rgba(28, 67, 137, 0.1);
}

.period-filter {
  margin-top: 14px;
  padding-top: 13px;
  border-top: 1px solid #edf1f6;
}

.period-filter > div:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.period-filter > div:first-child > span {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #42536d;
  font-size: 10px;
  font-weight: 900;
}

.period-toggle {
  display: flex;
  gap: 3px;
  padding: 3px;
  border-radius: 10px;
  background: #edf2f8;
}

.period-toggle button {
  height: 27px;
  padding: 0 9px;
  border-radius: 8px;
  color: #7f8da1;
  font-size: 8px;
  font-weight: 900;
}

.period-toggle button.active {
  background: #fff;
  color: #2f6fed;
  box-shadow: 0 2px 7px rgba(29, 65, 126, 0.1);
}

.date-range-fields {
  display: grid;
  grid-template-columns: 1fr 10px 1fr auto;
  align-items: end;
  gap: 6px;
  margin-top: 11px;
}

.date-range-fields label { min-width: 0; }

.date-range-fields label small {
  display: block;
  margin-bottom: 4px;
  color: #93a0b3;
  font-size: 7px;
  font-weight: 800;
}

.date-range-fields input {
  width: 100%;
  height: 34px;
  min-width: 0;
  padding: 0 5px;
  border: 1px solid #dce4ef;
  border-radius: 9px;
  background: #fff;
  color: #2f3d53;
  font-size: 8px;
}

.date-range-fields > i {
  padding-bottom: 11px;
  color: #b2bdcc;
  font-size: 8px;
  font-style: normal;
  text-align: center;
}

.date-range-fields > button {
  height: 34px;
  padding: 0 10px;
  border-radius: 9px;
  background: #2f6fed;
  color: #fff;
  font-size: 8px;
  font-weight: 900;
}

.period-filter > p {
  margin-top: 7px;
  color: #e25555;
  font-size: 8px;
}

.summary-section {
  margin: 12px 0 0;
  padding: 17px;
  border-radius: 21px;
}

.summary-title strong {
  display: block;
  margin-top: 3px;
  font-size: 14px;
  font-weight: 950;
}

.summary-title > svg { color: #a8b8d0; }

.currency-summary-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(105px, 1fr));
  gap: 8px;
  margin-top: 13px;
}

.currency-summary {
  display: grid;
  min-width: 0;
  gap: 0;
  padding: 12px 13px;
  border-radius: 14px;
  background: linear-gradient(145deg, #f3f7ff, #edf3fe);
}

.currency-summary small { color: #2f6fed; font-size: 8px; font-weight: 900; }

.currency-summary strong {
  margin-top: 3px;
  overflow: hidden;
  color: #173f8d;
  font-size: 17px;
  font-weight: 950;
  text-overflow: ellipsis;
}

.currency-summary span { margin-top: 2px; color: #8b99ae; font-size: 8px; }

.receipt-list {
  margin-top: 12px;
  padding: 18px 16px 10px;
  border-radius: 23px;
}

.title span { color: #98a5b8; font-size: 8px; font-weight: 800; }

.date-group { animation: receipt-group-enter 0.45s cubic-bezier(0.22, 1, 0.36, 1) both; }
.date-group:nth-of-type(2) { animation-delay: 0.06s; }
.date-group:nth-of-type(3) { animation-delay: 0.12s; }

@keyframes receipt-group-enter {
  from { opacity: 0; transform: translateY(10px); }
}

.date-group h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 20px 1px 7px;
  color: #2f6fed;
  font-size: 10px;
  font-weight: 900;
}

.date-group h3 > i {
  display: block;
  flex: 1;
  height: 1px;
  margin: 0;
  background: #e8edf5;
}

.date-group > button {
  display: grid;
  width: 100%;
  grid-template-columns: 44px minmax(0, 1fr) auto 18px;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
}

.date-group > button > .category-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 14px;
}

.receipt-info { min-width: 0; }

.date-group .receipt-info b {
  display: block;
  overflow: hidden;
  font-size: 11px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.date-group .receipt-info small {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  color: #8795aa;
  font-size: 8px;
  font-weight: 700;
}

.date-group .receipt-info small i { color: #c0cad8; font-style: normal; }

.date-group .receipt-info > i {
  display: block;
  margin-top: 4px;
  color: #2f6fed;
  font-size: 7px;
  font-style: normal;
  font-weight: 800;
}

.receipt-payment { display: grid; justify-items: end; white-space: nowrap; }
.date-group .receipt-payment strong { color: #17243a; font-size: 10px; font-weight: 950; }
.date-group .receipt-payment small { margin-top: 4px; color: #9aa6b7; font-size: 8px; }
.row-chevron { color: #a9b6c9; }
.date-group > button:active { transform: scale(0.985); }

.date-group > button > .category-icon {
  background: #eef3fb;
  color: #5f7598;
}

.date-group > button.shared {
  margin: 4px 0;
  padding: 12px 9px;
  border: 1px solid #cfe0fb;
  border-radius: 15px;
  background: linear-gradient(135deg, #f8fbff, #eef5ff);
}

.date-group > button.shared > .category-icon {
  background: #dfeaff;
  color: #2f6fed;
}

.merchant-line {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 5px;
}

.merchant-line b { min-width: 0; }

.merchant-line > span {
  flex: none;
  padding: 3px 6px;
  border-radius: 999px;
  background: #dce9ff;
  color: #2466c6;
  font-size: 6px;
  font-weight: 900;
}

.empty { text-align: center; }

.empty span {
  display: grid;
  width: 50px;
  height: 50px;
  place-items: center;
  border-radius: 17px;
  background: #eef4ff;
  color: #2e70dc;
  font-size: 24px;
}

.empty b { margin-top: 12px; font-size: 12px; }
.empty small { margin-top: 6px; color: #8b98a9; font-size: 8px; }

.receipt-actions {
  position: fixed;
  right: max(calc((100vw - 390px) / 2 + 18px), 18px);
  bottom: 78px;
  left: max(calc((100vw - 390px) / 2 + 18px), 18px);
  z-index: 40;
  display: grid;
  grid-template-columns: 0.85fr 1.35fr;
  gap: 8px;
  padding: 7px;
  border: 1px solid rgba(213, 224, 240, 0.9);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 34px rgba(23, 63, 141, 0.19);
  backdrop-filter: blur(14px);
}

.receipt-actions button {
  display: flex;
  height: 48px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 14px;
  font-size: 11px;
  font-weight: 900;
  box-shadow: none;
}

.manual-button { border: 0; background: #edf3ff; color: #17499c; }

.scan-button {
  background: linear-gradient(135deg, #17499c, #2f6fed);
  color: #fff;
  box-shadow: 0 7px 17px rgba(47, 111, 237, 0.25) !important;
}

.country-chips button:disabled,
.receipt-actions button:disabled { cursor: not-allowed; opacity: 0.55; }

@media (max-width: 350px) {
  .vault-complete { display: none; }
  .paper-trip-info { grid-template-columns: 1fr; }
  .paper-total { align-items: end; }
  .stored-stamp { padding: 8px; }
  .stored-stamp small { display: none; }
  .receipt-actions { grid-template-columns: 1fr 1.2fr; }
  .date-group > button { grid-template-columns: 40px minmax(0, 1fr) auto 14px; gap: 8px; }
  .date-group > button > .category-icon { width: 39px; height: 39px; }
}
</style>
