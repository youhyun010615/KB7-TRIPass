<script setup>
import {
  computed,
  onBeforeUnmount,
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
import NotificationBell from '@/components/common/NotificationBell.vue'
import TravelModeMeta from '@/components/travel/TravelModeMeta.vue'
import ReceiptSettlementView from '@/views/receipt/ReceiptSettlementView.vue'
import receiptIcon from '@/assets/icons/receipt.svg'

import {
  getReceiptDates,
  getReceipts,
} from '@/api/receipt'

import {
  fetchTripGoal,
} from '@/api/travel'

const route = useRoute()
const router = useRouter()
const isArchiveView = computed(() => Boolean(route.meta.receiptArchive))
const receiptHeaderEl = ref(null)
const receiptHeaderHeight = ref(0)
let receiptHeaderResizeObserver = null

watch(receiptHeaderEl, element => {
  receiptHeaderResizeObserver?.disconnect()
  receiptHeaderResizeObserver = null
  if (!element) return
  receiptHeaderHeight.value = element.offsetHeight
  if (window.ResizeObserver) {
    receiptHeaderResizeObserver = new ResizeObserver(() => {
      receiptHeaderHeight.value = element.offsetHeight
    })
    receiptHeaderResizeObserver.observe(element)
  }
})

onBeforeUnmount(() => receiptHeaderResizeObserver?.disconnect())

const activeVaultView = ref(
    ['ReceiptSettlements', 'ReceiptParticipantSettlement']
        .includes(String(route.name))
        ? 'settlements'
        : 'receipts',
)

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
const dateFilterOpen = ref(false)
const receiptSortOrder = ref('latest')
const startDate = ref('')
const endDate = ref('')
const availableReceiptDates = ref([])
const dateFilterError = ref('')

const loading = ref(false)
const errorMessage = ref('')

const tripTitle = computed(() =>
    trip.value?.tripName ||
    '여행 정보 확인 중',
)

const countryCodeFallback = {
  프랑스: 'FR', 독일: 'DE', 스위스: 'CH', 일본: 'JP', 홍콩: 'HK',
  이탈리아: 'IT', 스페인: 'ES', 네덜란드: 'NL', 벨기에: 'BE',
  오스트리아: 'AT', 포르투갈: 'PT', 그리스: 'GR', 영국: 'GB',
  미국: 'US', 캐나다: 'CA', 호주: 'AU', 뉴질랜드: 'NZ', 태국: 'TH',
  베트남: 'VN', 싱가포르: 'SG', 대만: 'TW', 중국: 'CN',
}

function countryIsoCode(country) {
  const code = String(
      country?.countryCode ||
      country?.code ||
      country?.iso2 ||
      countryCodeFallback[country?.countryName] ||
      '',
  ).toUpperCase()
  return /^[A-Z]{2}$/.test(code) ? code : 'UN'
}

const tripCountryCodes = computed(() =>
    (trip.value?.countries || []).slice(0, 3).map(countryIsoCode),
)
const receiptTravelDay = computed(() => {
  if (!trip.value?.startDate) return 0
  const start = new Date(`${trip.value.startDate}T00:00:00`).getTime()
  const today = new Date(); today.setHours(0, 0, 0, 0)
  return Math.max(0, Math.floor((today.getTime() - start) / 86400000))
})
const receiptCurrentCountry = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  const item = (trip.value?.countries || []).find(country => {
    const start = country.arrivalDate || country.startDate
    const end = country.departureDate || country.endDate
    return start && end && start <= today && today <= end
  }) || trip.value?.countries?.[0]
  return { name: item?.countryName || '', code: countryIsoCode(item) }
})

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
              receiptSortOrder.value === 'latest'
                  ? secondDate.localeCompare(firstDate)
                  : firstDate.localeCompare(secondDate),
      )
      .map(([date, items]) => ({
        date,

        items: [...items].sort(
            (first, second) =>
                receiptSortOrder.value === 'latest'
                    ? second.time.localeCompare(first.time)
                    : first.time.localeCompare(second.time),
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
      dateResponse,
    ] = await Promise.all([
      fetchTripGoal(tripId.value),
      getReceipts(tripId.value, dateFilterEnabled.value
        ? {
            startDate: startDate.value || undefined,
            endDate: endDate.value || undefined,
          }
        : {}),
      getReceiptDates(tripId.value),
    ])

    trip.value =
        tripResponse ?? null

    const receiptData =
        receiptResponse.data?.data

    receipts.value =
        Array.isArray(receiptData)
            ? receiptData.map(mapReceipt)
            : []

    const dateData =
        dateResponse.data?.data ??
        dateResponse.data ??
        []

    availableReceiptDates.value =
        Array.isArray(dateData)
            ? dateData
                .filter(date => /^\d{4}-\d{2}-\d{2}$/.test(String(date)))
                .sort((first, second) => second.localeCompare(first))
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
  dateFilterOpen.value = true
  startDate.value ||= availableReceiptDates.value.at(-1) || ''
  endDate.value ||= availableReceiptDates.value[0] || ''
  dateFilterError.value = ''
}

async function applyDateFilter() {
  const invalidSelectedDate =
      [startDate.value, endDate.value]
          .filter(Boolean)
          .some(date => !availableReceiptDates.value.includes(date))

  if (invalidSelectedDate) {
    dateFilterError.value = '영수증이 등록된 날짜만 선택할 수 있어요.'
    return
  }

  if (startDate.value && endDate.value && startDate.value > endDate.value) {
    dateFilterError.value = '시작일은 종료일보다 빠르게 선택해 주세요.'
    return
  }

  dateFilterError.value = ''
  dateFilterEnabled.value = true
  await loadPage()
  dateFilterOpen.value = false
}

async function resetDateFilter() {
  dateFilterEnabled.value = false
  startDate.value = ''
  endDate.value = ''
  dateFilterError.value = ''
  await loadPage()
  dateFilterOpen.value = false
}

function openSettlements() {
  activeVaultView.value = 'settlements'
}

function openReceipts() {
  activeVaultView.value = 'receipts'
}

function openReceipt(receiptId) {
  if (!tripId.value) {
    return
  }

  router.push({
    name: isArchiveView.value ? 'TravelReceiptArchiveDetail' : 'ReceiptDetail',

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
    name: isArchiveView.value ? 'TravelReceiptArchiveCapture' : 'ReceiptCapture',

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
    name: isArchiveView.value ? 'TravelReceiptArchiveNew' : 'ReceiptManualNew',

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

watch(
    () => route.name,
    name => {
      if (
          ['ReceiptSettlements', 'ReceiptParticipantSettlement']
              .includes(String(name))
      ) {
        activeVaultView.value = 'settlements'
      }
    },
)

onMounted(loadPage)
</script>

<template>
  <main class="receipt-page">
    <header v-if="isArchiveView" class="archive-page-header">
      <button type="button" aria-label="여행 관리로 돌아가기" @click="router.back()">
        <ChevronLeft :size="24" />
      </button>
      <h1>영수증 보관함</h1>
      <span aria-hidden="true" />
    </header>
    <div
      v-if="!isArchiveView"
      ref="receiptHeaderEl"
      class="receipt-header-fixed"
    >
      <header class="receipt-header">
        <div>
          <img src="@/assets/brand/tripass-text.png" class="header-wordmark" alt="TRIPASS" />
          <h1>RECEIPT</h1>
        </div>
        <NotificationBell />
      </header>
      <TravelModeMeta
        :trip-name="tripTitle"
        :date-range="formatTripDateRange()"
        :day="receiptTravelDay"
        :country-name="receiptCurrentCountry.name"
        :country-code="receiptCurrentCountry.code"
        :country-codes="tripCountryCodes"
      />
    </div>
    <div v-if="!isArchiveView" class="receipt-header-spacer" :style="{ height: `${receiptHeaderHeight}px` }" aria-hidden="true" />

    <nav class="vault-tabs" aria-label="영수증 보관함 메뉴">
      <button
          type="button"
          :class="{ active: activeVaultView === 'receipts' }"
          @click="openReceipts"
      >
        <ReceiptText :size="16" /> 영수증
      </button>
      <button
          type="button"
          :class="{ active: activeVaultView === 'settlements' }"
          @click="openSettlements"
      >
        <HandCoins :size="17" /> 정산
      </button>
    </nav>

    <ReceiptSettlementView
        v-if="activeVaultView === 'settlements' && tripId"
        :trip-id="tripId"
    />

    <template v-else>
    <section class="trip-receipt-summary">
      <div class="trip-summary-metrics">
        <div>
          <span>총 결제 금액</span>
          <strong v-if="currencySummaries.length">
            {{ currencySummaries[0].currencyCode }} {{ formatAmount(currencySummaries[0].totalAmount) }}
          </strong>
          <strong v-else>0</strong>
        </div>
        <div>
          <span>보관된 영수증</span>
          <strong>{{ totalReceiptCount }}<em>장</em></strong>
        </div>
      </div>
    </section>

    <section class="filter-section">
      <div class="country-filter-row">
        <div class="country-chips" role="group" aria-label="영수증 조회 국가 선택">
          <button
              v-for="country in countries"
              :key="country.countryId ?? 'all'"
              type="button"
              :class="{ active: selectedCountryId === country.countryId }"
              :disabled="loading"
              @click="selectedCountryId = country.countryId"
          >
            <Check
              v-if="country.countryId !== null && selectedCountryId === country.countryId"
              :size="12"
              :stroke-width="3"
            />
            {{ country.countryName }}
          </button>
        </div>
        <button
            type="button"
            class="calendar-filter-button"
            :class="{ active: dateFilterEnabled }"
            aria-label="날짜 필터 열기"
            @click="showDateFilter"
        >
          <CalendarRange :size="18" :stroke-width="2" />
        </button>
      </div>

      <div v-if="dateFilterOpen" class="date-filter-overlay" @click.self="dateFilterOpen = false">
        <section class="date-filter-popup" role="dialog" aria-modal="true" aria-labelledby="date-filter-title">
          <header>
            <div>
              <small>DATE FILTER</small>
              <h3 id="date-filter-title">조회 기간 선택</h3>
            </div>
            <button type="button" aria-label="날짜 필터 닫기" @click="dateFilterOpen = false">×</button>
          </header>
          <div class="date-range-fields">
          <label>
            <small>시작일</small>
            <select v-model="startDate">
              <option value="">선택</option>
              <option
                  v-for="date in [...availableReceiptDates].reverse()"
                  :key="`start-${date}`"
                  :value="date"
                  :disabled="Boolean(endDate && date > endDate)"
              >
                {{ formatGroupDate(date) }}
              </option>
            </select>
          </label>
          <i>—</i>
          <label>
            <small>종료일</small>
            <select v-model="endDate">
              <option value="">선택</option>
              <option
                  v-for="date in availableReceiptDates"
                  :key="`end-${date}`"
                  :value="date"
                  :disabled="Boolean(startDate && date < startDate)"
              >
                {{ formatGroupDate(date) }}
              </option>
            </select>
          </label>
          </div>
          <small v-if="!availableReceiptDates.length" class="no-receipt-dates">
            선택할 수 있는 영수증 등록일이 없어요.
          </small>
          <p v-if="dateFilterError" class="date-filter-error">{{ dateFilterError }}</p>
          <footer>
            <button type="button" :disabled="loading" @click="resetDateFilter">초기화</button>
            <button type="button" :disabled="loading || !availableReceiptDates.length" @click="applyDateFilter">적용</button>
          </footer>
        </section>
      </div>
    </section>

    <section class="receipt-list">
      <div class="receipt-list-topline">
        <b>RECEIPT HISTORY</b>
        <label class="receipt-sort-select">
          <span class="sr-only">영수증 정렬 방식</span>
          <select v-model="receiptSortOrder" aria-label="영수증 정렬 방식">
            <option value="latest">최신 결제순</option>
            <option value="oldest">오래된 순</option>
          </select>
        </label>
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
              <ReceiptText :size="20" :stroke-width="2.2" />
            </span>

            <div class="receipt-info">
              <div class="merchant-line">
                <b>{{ item.merchant }}</b>
                <span v-if="item.splitCount > 1">
                  <Users :size="11" :stroke-width="2.4" />
                  공동결제 {{ item.splitCount }}명
                </span>
              </div>
              <small>
                {{ item.countryName }}
                <i>·</i>
                {{ item.time }}
              </small>
              <i
                  v-if="item.splitCount > 1"
              >
                <template v-if="item.participantNames">공동 인원: {{ item.participantNames }}</template>
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
            class="empty receipt-empty"
        >
          <span class="empty-receipt-icon" aria-hidden="true">
            <img :src="receiptIcon" alt="" />
          </span>

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
      <p class="receipt-list-footer"><span>TRIPASS</span> THANK YOU FOR TRAVELING WITH TRIPASS</p>
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
    </template>

    <BottomNav v-if="!isArchiveView" />
  </main>
</template>

<style scoped>
.receipt-page {
  min-height: 100vh;
  padding: 0 18px 150px;
  background: #f8f6f1;
  color: #111a2d
}

.archive-page-header {
  display: grid;
  min-height: 76px;
  grid-template-columns: 42px 1fr 42px;
  align-items: center;
  padding-top: 8px;
}

.archive-page-header button {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 14px;
  background: #fff;
  color: #173f8d;
  box-shadow: 0 7px 20px rgba(26, 63, 132, 0.09);
}

.archive-page-header h1 {
  color: #10192b;
  font-size: 20px;
  font-weight: 950;
  text-align: center;
  letter-spacing: -0.04em;
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
.receipt-header-fixed{position:fixed;top:0;left:50%;z-index:60;width:100%;max-width:390px;padding:14px 18px;background:#f4f7fc;transform:translateX(-50%)}
.receipt-header-fixed.archive-header-fixed { padding: 0; background: #f4f7fc; }
.archive-header { display: grid; min-height: 70px; padding: 14px 18px; grid-template-columns: 36px 1fr 36px; align-items: center; }
.archive-header button { display: grid; width: 36px; height: 36px; padding: 0; place-items: center; border: 0; border-radius: 12px; color: #173f8d; background: #fff; font-size: 30px; font-weight: 500; line-height: 1; }
.archive-header h1 { margin: 0; color: #10192b; font-size: 20px; font-weight: 900; text-align: center; }
.receipt-header{display:flex;align-items:flex-start;justify-content:space-between}.receipt-header .header-wordmark{display:block;width:88px;height:auto;object-fit:contain}.receipt-header h1{margin-top:6px;color:#29466f;font-size:17px;font-weight:400;letter-spacing:normal}.receipt-header-spacer{height:132px}
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

.date-range-fields input,
.date-range-fields select {
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

.no-receipt-dates {
  display: block;
  margin-top: 8px;
  color: #8b99ad;
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
  margin: 0;
  padding: 13px 0;
  border: 0;
  border-bottom: 1px solid #eef2f7;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.date-group > button.shared > .category-icon {
  background: #eef3fb;
  color: #5f7598;
}

.merchant-line {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 5px;
}

.merchant-line b { min-width: 0; }

.merchant-line > span {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 3px;
  padding: 4px 7px;
  border-radius: 999px;
  background: #fff0ae;
  color: #8b6100;
  font-size: 8px;
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

/* Receipt archive — compact paper layout */

.vault-tabs {
  margin-top: 4px;
  border: 0;
  border-radius: 18px;
  background: #e8edf6;
}

.vault-tabs button { height: 44px; font-size: 12px; }

.trip-receipt-summary {
  position: relative;
  overflow: visible;
  margin-top: 16px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.trip-summary-heading {
  display: flex;
  align-items: center;
  gap: 12px;
}

.trip-summary-heading small {
  display: block;
  margin-top: 5px;
  color: #8290a6;
  font-size: 10px;
  font-weight: 850;
  letter-spacing: .03em;
}

.trip-summary-heading h2 {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  color: #142440;
  font-size: 17px;
  font-weight: 950;
  letter-spacing: -.035em;
}

.trip-country-flags { display: inline-flex; align-items: center; gap: 3px; }
.trip-country-flags i {
  display: block;
  width: 22px;
  height: 15px;
  border-radius: 3px;
  background-size: cover;
  font-style: normal;
  filter: drop-shadow(0 2px 3px rgba(0, 0, 0, .18));
}

.trip-summary-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
  margin-top: 0;
}

.trip-summary-metrics > div {
  display: grid;
  min-width: 0;
  gap: 5px;
  min-height: 66px;
  padding: 13px;
  border-radius: 14px;
  background: rgba(255, 255, 255, .58);
}

.trip-summary-metrics span {
  color: #8997aa;
  font-size: 8px;
  font-weight: 850;
}

.trip-summary-metrics strong {
  overflow: hidden;
  color: #173f8d;
  font-size: 14px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trip-summary-metrics em {
  margin-left: 2px;
  color: #72829a;
  font-size: 9px;
  font-style: normal;
}

.filter-section {
  margin-top: 12px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.country-filter-row {
  display: flex;
  align-items: center;
  gap: 9px;
}

.country-filter-row .country-chips {
  flex: 1;
  min-width: 0;
  margin: 0;
  padding: 2px 0;
}

.country-chips button {
  height: 38px;
  padding: 0 15px;
  background: #fff;
  font-size: 11px;
}

.calendar-filter-button {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  place-items: center;
  border: 1px solid #dce5f2;
  border-radius: 50%;
  background: #fff;
  color: #66758d;
  box-shadow: 0 4px 12px rgba(31, 64, 120, .05);
}

.calendar-filter-button.active {
  border-color: #2f6fed;
  background: #eef4ff;
  color: #2f6fed;
}

.date-filter-overlay {
  position: fixed;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 22px;
  background: rgba(10, 26, 54, .42);
  backdrop-filter: blur(3px);
  inset: 0;
}

.date-filter-popup {
  width: min(100%, 320px);
  padding: 18px;
  border: 1px solid rgba(207, 220, 240, .9);
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 20px 55px rgba(12, 36, 78, .24);
  animation: date-filter-popup-in .2s ease-out;
}

.date-filter-popup > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.date-filter-popup > header small {
  display: block;
  margin-bottom: 4px;
  color: #2f6fed;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: .14em;
}

.date-filter-popup > header h3 {
  color: #11203a;
  font-size: 16px;
  font-weight: 900;
}

.date-filter-popup > header > button {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 50%;
  background: #f1f4f9;
  color: #68768a;
  font-size: 18px;
  line-height: 1;
}

.date-filter-popup .date-range-fields {
  grid-template-columns: 1fr 12px 1fr;
  gap: 7px;
  margin-top: 17px;
}

.date-filter-popup .date-range-fields label small {
  margin-bottom: 6px;
  color: #6d7d94;
  font-size: 9px;
  font-weight: 850;
}

.date-filter-popup .date-range-fields select {
  height: 44px;
  padding: 0 9px;
  border-color: #d7e0ed;
  border-radius: 11px;
  background: #f8faff;
  color: #1d2b42;
  font-size: 10px;
  font-weight: 750;
}

.date-filter-popup .date-range-fields > i {
  padding-bottom: 15px;
  font-size: 10px;
}

.date-filter-popup .no-receipt-dates,
.date-filter-popup .date-filter-error {
  display: block;
  margin-top: 9px;
  font-size: 9px;
}

.date-filter-popup .date-filter-error { color: #e25555; }

.date-filter-popup > footer {
  display: grid;
  grid-template-columns: .8fr 1.2fr;
  gap: 8px;
  margin-top: 17px;
}

.date-filter-popup > footer button {
  height: 44px;
  border-radius: 12px;
  background: #edf2f9;
  color: #66758b;
  font-size: 11px;
  font-weight: 900;
}

.date-filter-popup > footer button:last-child {
  background: #173f8d;
  color: #fff;
}

.date-filter-popup > footer button:disabled {
  cursor: not-allowed;
  opacity: .48;
}

@keyframes date-filter-popup-in {
  from { opacity: 0; transform: translateY(8px) scale(.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.filter-section .period-filter {
  margin-top: 10px;
  padding: 12px;
  border: 1px solid #e2e9f4;
  border-radius: 15px;
  background: #fff;
}

.receipt-list {
  position: relative;
  overflow: visible;
  margin-top: 20px;
  padding: 0 16px 18px;
  border: 0;
  border-radius: 0 0 8px 8px;
  background: #fff;
}

.receipt-list::before {
  position: absolute;
  top: -7px;
  right: -1px;
  left: -1px;
  height: 8px;
  background: radial-gradient(circle at 7px 7px, #fff 7px, transparent 7.5px) 0 0 / 14px 8px repeat-x;
  content: '';
  pointer-events: none;
}

.receipt-list::after {
  position: absolute;
  right: 7px;
  bottom: -8px;
  left: 7px;
  height: 9px;
  background: radial-gradient(circle at 50% 0, #fff 0 7px, transparent 7.5px) center top / 14px 9px repeat-x;
  content: '';
  pointer-events: none;
}

.receipt-list-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 -16px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #123b83, #2059b2);
  color: #cfe0ff;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: .13em;
}

.receipt-list-topline b { color: #ffd466; font-size: 9px; }
.receipt-sort-select { position: relative; }

.receipt-sort-select select {
  color-scheme: light;
  height: 30px;
  padding: 0 27px 0 10px;
  border: 1px solid rgba(255, 255, 255, .24);
  border-radius: 9px;
  appearance: none;
  background:
    linear-gradient(45deg, transparent 50%, #d8e6ff 50%) calc(100% - 13px) 12px / 4px 4px no-repeat,
    linear-gradient(135deg, #d8e6ff 50%, transparent 50%) calc(100% - 9px) 12px / 4px 4px no-repeat,
    rgba(255, 255, 255, .1);
  color: #fff;
  font-size: 9px;
  font-weight: 850;
}

.receipt-sort-select select option {
  background-color: #fff;
  color: #17243a;
}
.receipt-sort-select .sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}
.receipt-list .title { padding-top: 17px; }
.receipt-list .title h2 { font-size: 16px; }
.receipt-list .title span { font-size: 9px; }

.receipt-list-footer {
  display: grid;
  gap: 8px;
  margin: 18px 0 2px;
  padding-top: 13px;
  border-top: 1px dashed #d6deea;
  color: #b2bdcc;
  font-size: 6px;
  font-weight: 850;
  letter-spacing: .12em;
  text-align: center;
}

.receipt-list-footer span { color: #9ba9bc; font-size: 7px; }

.receipt-empty { min-height: 285px; }

.empty-receipt-icon {
  position: relative;
  isolation: isolate;
  animation: empty-receipt-float 2.6s ease-in-out infinite;
}

.empty-receipt-icon img {
  display: block;
  width: 27px;
  height: 27px;
}

.empty-receipt-icon::before,
.empty-receipt-icon::after {
  position: absolute;
  z-index: -1;
  border: 1px solid rgba(47, 111, 237, .22);
  border-radius: 20px;
  content: '';
  inset: -1px;
  animation: empty-receipt-pulse 2.6s ease-out infinite;
}

.empty-receipt-icon::after { animation-delay: 1.3s; }

.receipt-empty b { font-size: 13px; }
.receipt-empty small { margin-top: 8px; font-size: 9px; }

@keyframes empty-receipt-float {
  0%, 100% { transform: translateY(0) rotate(-1deg); }
  45% { transform: translateY(-7px) rotate(1.5deg); }
  55% { transform: translateY(-7px) rotate(-1deg); }
}

@keyframes empty-receipt-pulse {
  0% { opacity: .7; transform: scale(.9); }
  75%, 100% { opacity: 0; transform: scale(1.48); }
}

@media (prefers-reduced-motion: reduce) {
  .empty-receipt-icon,
  .empty-receipt-icon::before,
  .empty-receipt-icon::after { animation: none; }
}

.date-group h3 { font-size: 11px; }
.date-group > button { padding: 13px 0; }
.date-group .receipt-info b { font-size: 13px; }
.date-group .receipt-info small { font-size: 9px; }
.date-group .receipt-info > i { font-size: 8px; }
.date-group .receipt-payment strong { font-size: 12px; }
.date-group .receipt-payment small { font-size: 9px; }

@media (max-width: 350px) {
  .trip-summary-metrics { grid-template-columns: 1fr 1fr; }
  .trip-summary-metrics strong { font-size: 12px; }
}
</style>
