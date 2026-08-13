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

const selectedCountryId = ref(null)

const loading = ref(false)
const errorMessage = ref('')

const tripTitle = computed(() =>
    trip.value?.tripName ||
    '여행 정보 확인 중',
)

const tripDateRange = computed(() => {
  const startDate =
      trip.value?.startDate

  const endDate =
      trip.value?.endDate

  if (!startDate && !endDate) {
    return ''
  }

  if (!startDate) {
    return endDate
  }

  if (!endDate) {
    return startDate
  }

  return `${startDate} ~ ${endDate}`
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

    categoryId:
    item.categoryId,

    categoryName:
        item.categoryName ||
        '카테고리 미지정',

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
      getReceipts(tripId.value),
    ])

    trip.value =
        tripResponse ?? null

    const receiptData =
        receiptResponse.data?.data

    receipts.value =
        Array.isArray(receiptData)
            ? receiptData.map(mapReceipt)
            : []

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
    <header>
      <button
          type="button"
          aria-label="뒤로 가기"
          @click="router.back()"
      >
        ‹
      </button>

      <h1>영수증 보관함</h1>

      <span />
    </header>

    <section class="vault-ticket">
      <small>TRIPASS RECEIPT VAULT</small>

      <h2>{{ tripTitle }}</h2>

      <p>{{ tripDateRange }}</p>

      <div>
        <b>{{ receipts.length }}건</b>
        <span>보관 완료</span>
      </div>

      <i>||||||||||||||||||||</i>
    </section>

    <section class="filter-row">
      <b>국가 선택</b>

      <select
          v-model="selectedCountryId"
          :disabled="loading"
      >
        <option
            v-for="country in countries"
            :key="country.countryId ?? 'all'"
            :value="country.countryId"
        >
          {{ country.countryName }}
        </option>
      </select>
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
          <small>선택 범위</small>
          <strong>
            {{ selectedCountryName }}
          </strong>
        </div>

        <span>
          {{ filteredReceipts.length }}건
        </span>
      </div>

      <div class="currency-summary-list">
        <div
            v-for="summary in currencySummaries"
            :key="summary.currencyCode"
            class="currency-summary"
        >
          <small>
            지출 합계
          </small>

          <strong>
            {{ summary.currencyCode }}
            {{
              formatAmount(
                  summary.totalAmount,
              )
            }}
          </strong>
        </div>
      </div>
    </section>

    <section class="receipt-list">
      <div class="title">
        <h2>최근 영수증</h2>

        <span>
          최신 결제순
        </span>
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
          <h3>{{ group.date }}</h3>

          <button
              v-for="item in group.items"
              :key="item.id"
              type="button"
              @click="openReceipt(item.id)"
          >
            <span>🧾</span>

            <div>
              <b>{{ item.merchant }}</b>

              <small>
                {{ item.countryName }}
                ·
                {{ item.categoryName }}
                ·
                {{ item.time }}
              </small>

              <i
                  v-if="
                    item.splitCount > 1 &&
                    item.splitAmount != null
                  "
              >
                {{ item.splitCount }}명 공동결제
                · 1인당
                {{ item.currencyCode }}
                {{
                  formatAmount(
                      item.splitAmount,
                  )
                }}
              </i>
            </div>

            <strong>
              {{
                item.currencySymbol ||
                item.currencyCode
              }}
              {{
                formatAmount(
                    item.totalAmount,
                )
              }}
            </strong>

            <em>›</em>
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
            선택한 국가에 등록된 영수증이 없습니다.
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
        + 영수증 수기입력
      </button>

      <button
          type="button"
          class="scan-button"
          :disabled="loading || !tripId"
          @click="openCapture"
      >
        + 영수증 촬영
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
  height: 88px;
  grid-template-columns:36px 1fr 36px;
  align-items: end;
  padding-bottom: 17px
}

.receipt-page > header button {
  font-size: 30px;
  text-align: left
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
</style>
