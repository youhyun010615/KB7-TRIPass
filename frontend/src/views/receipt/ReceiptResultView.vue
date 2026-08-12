<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useReceiptStore} from '@/stores/receipt'
import {
  createReceipt,
  deleteReceipt,
  getReceipt,
  getReceiptImage,
  updateReceipt,
} from '@/api/receipt'

const route = useRoute()
const router = useRouter()
const store = useReceiptStore()

// URL의 receiptId가 있으면 저장된 영수증 상세 화면이다.
const receiptId = computed(() => {
  const value = Number(
      route.params.receiptId,
  )

  return Number.isInteger(value) &&
  value > 0
      ? value
      : null
})

const isExistingReceipt = computed(() =>
    receiptId.value !== null,
)

const loading = ref(false)
const loadErrorMessage = ref('')

const tripId = computed(() =>
    Number(route.query.tripId || 1),
)

const trip = computed(() =>
    store.trip(tripId.value),
)

const draft = store.draft
const sourceFile = draft?.sourceFile ?? null

const originalImageUrl = ref(
    sourceFile
        ? URL.createObjectURL(sourceFile)
        : '',
)

function separatePaymentDateTime(value) {
  if (!value) {
    return {
      date: '',
      time: '',
    }
  }

  const [date = '', time = ''] =
      String(value).split('T')

  return {
    date,
    time: time.substring(0, 5),
  }
}

const paymentDateTime =
    separatePaymentDateTime(
        draft?.paymentDateTime,
    )

const form = reactive({
  tripId:
      draft?.tripId ?? null,

  countryId:
      draft?.countryId ?? null,

  currencyId:
      draft?.currencyId ?? null,

  merchantOriginalName:
      draft?.originalMerchantName ?? '',

  merchantTranslatedName:
      draft?.translatedMerchantName ?? '',

  paymentDate: paymentDateTime.date,
  paymentTime: paymentDateTime.time,

  currencyCode:
      draft?.currencyCode ?? '',

  totalAmount:
      draft?.totalAmount ?? 0,

  taxAmount:
      draft?.taxAmount ?? null,

  splitCount:
      draft?.splitCount ?? 1,

  rawText:
      draft?.rawText ?? '',

  items: (draft?.items ?? []).map(
      (item, index) => ({
        originalName:
            item.originalName ?? '',

        translatedName:
            item.translatedName ?? '',

        quantity:
            item.quantity ?? 1,

        amount:
            item.amount ?? 0,

        displayOrder:
            item.displayOrder ?? index + 1,
      }),
  ),
})

// OCR 분석 응답 또는 상세 조회 응답을 화면 입력값에 적용한다.
function applyReceiptData(data) {
  const dateTime =
      separatePaymentDateTime(
          data.paymentDateTime,
      )

  form.tripId =
      data.tripId ?? null

  form.countryId =
      data.countryId ?? null

  form.currencyId =
      data.currencyId ?? null

  form.merchantOriginalName =
      data.merchantOriginalName ??
      data.originalMerchantName ??
      ''

  form.merchantTranslatedName =
      data.merchantTranslatedName ??
      data.translatedMerchantName ??
      ''

  form.paymentDate = dateTime.date
  form.paymentTime = dateTime.time
  form.currencyCode =
      data.currencyCode ?? ''
  form.totalAmount =
      data.totalAmount ?? 0
  form.taxAmount =
      data.taxAmount ?? null
  form.splitCount =
      data.splitCount ?? 1

  form.rawText =
      data.ocrRawText ??
      data.rawText ??
      ''

  form.items = (data.items ?? []).map(
      (item, index) => ({
        id: item.id ?? null,
        originalName:
            item.originalName ?? '',
        translatedName:
            item.translatedName ?? '',
        quantity:
            item.quantity ?? 1,
        amount:
            item.amount ?? 0,
        displayOrder:
            item.displayOrder ??
            index + 1,
      }),
  )
}


const editing = ref(false)
const translated = ref(true)
const showOriginal = ref(false)

const displayedMerchantName = computed(() => {
  if (translated.value) {
    return (
        form.merchantTranslatedName ||
        form.merchantOriginalName ||
        '상호명 미인식'
    )
  }

  return (
      form.merchantOriginalName ||
      '상호명 미인식'
  )
})

const splitAmount = computed(() => {
  const totalAmount =
      Number(form.totalAmount) || 0

  const splitCount =
      Math.max(Number(form.splitCount) || 1, 1)

  return totalAmount / splitCount
})

function getDisplayedItemName(item) {
  if (translated.value) {
    return (
        item.translatedName ||
        item.originalName ||
        '품목명 미인식'
    )
  }

  return (
      item.originalName ||
      '품목명 미인식'
  )
}

function addItem() {
  form.items.push({
    originalName: '',
    translatedName: '',
    quantity: 1,
    amount: 0,
    displayOrder: form.items.length + 1,
  })
}

function removeItem(index) {
  form.items.splice(index, 1)

  form.items.forEach((item, itemIndex) => {
    item.displayOrder = itemIndex + 1
  })
}

function discard() {
  if (!isExistingReceipt.value) {
    store.$patch({
      draft: null,
    })
  }

  router.push({
    path: '/receipt',
    query: {
      tripId: tripId.value,
    },
  })
}

const currencyIdMap = {
  KRW: 1,
  AED: 2,
  AUD: 3,
  BHD: 4,
  BND: 5,
  CAD: 6,
  CHF: 7,
  CNH: 8,
  DKK: 9,
  EUR: 10,
  GBP: 11,
  HKD: 12,
  IDR: 13,
  JPY: 14,
  KWD: 15,
  MYR: 16,
  NOK: 17,
  NZD: 18,
  SAR: 19,
  SEK: 20,
  SGD: 21,
  THB: 22,
  USD: 23,
}

const saving = ref(false)
function createReceiptRequestData() {
  const normalizedCurrencyCode =
      form.currencyCode
          .trim()
          .toUpperCase()

  const currencyId =
      currencyIdMap[normalizedCurrencyCode]

  if (!currencyId) {
    throw new Error(
        '등록되지 않은 통화 코드입니다.',
    )
  }

  if (
      !form.paymentDate ||
      Number(form.totalAmount) <= 0
  ) {
    throw new Error(
        '결제일과 결제 금액을 확인해 주세요.',
    )
  }

  return {
    tripId:
        form.tripId ?? null,

    countryId:
        form.countryId ?? null,

    currencyId,

    paymentDateTime:
        `${form.paymentDate}T` +
        `${form.paymentTime || '00:00'}:00`,

    merchantOriginalName:
        form.merchantOriginalName || null,

    merchantTranslatedName:
        form.merchantTranslatedName || null,

    totalAmount:
        Number(form.totalAmount),

    taxAmount:
        form.taxAmount === null ||
        form.taxAmount === ''
            ? null
            : Number(form.taxAmount),

    ocrRawText:
        form.rawText || null,

    splitCount:
        Math.max(
            Number(form.splitCount) || 1,
            1,
        ),

    items: form.items.map(
        (item, index) => ({
          originalName:
              item.originalName ||
              '미인식 품목',

          translatedName:
              item.translatedName || null,

          quantity:
              Math.max(
                  Number(item.quantity) || 1,
                  1,
              ),

          amount:
              item.amount === null ||
              item.amount === ''
                  ? null
                  : Number(item.amount),

          displayOrder: index + 1,
        }),
    ),
  }
}

async function prepareSave() {
  if (
      !isExistingReceipt.value &&
      !sourceFile
  ) {
    window.alert(
        '저장할 영수증 원본 이미지가 없습니다.',
    )
    return
  }

  saving.value = true

  try {
    const receiptData =
        createReceiptRequestData()

    if (isExistingReceipt.value) {
      await updateReceipt(
          receiptId.value,
          receiptData,
      )

      window.alert(
          '영수증이 수정되었습니다.',
      )
    } else {
      await createReceipt(
          receiptData,
          sourceFile,
      )

      store.$patch({
        draft: null,
      })

      window.alert(
          '영수증이 저장되었습니다.',
      )
    }

    editing.value = false

    await router.push({
      path: '/receipt',
      query: {
        tripId: tripId.value,
      },
    })
  } catch (error) {
    window.alert(
        error.response?.data?.message ||
        error.message ||
        (
            isExistingReceipt.value
                ? '영수증 수정에 실패했습니다.'
                : '영수증 저장에 실패했습니다.'
        ),
    )
  } finally {
    saving.value = false
  }
}

const deleting = ref(false)

async function removeReceipt() {
  if (!receiptId.value) {
    return
  }

  const confirmed = window.confirm(
      '이 영수증을 삭제할까요?',
  )

  if (!confirmed) {
    return
  }

  deleting.value = true

  try {
    await deleteReceipt(
        receiptId.value,
    )

    window.alert(
        '영수증이 삭제되었습니다.',
    )

    await router.push({
      path: '/receipt',
      query: {
        tripId: tripId.value,
      },
    })
  } catch (error) {
    window.alert(
        error.response?.data?.message ||
        '영수증 삭제에 실패했습니다.',
    )
  } finally {
    deleting.value = false
  }
}

// 서버에 저장된 원본 영수증 이미지를 조회한다.
async function loadOriginalImage() {
  if (!receiptId.value) {
    return
  }

  const response =
      await getReceiptImage(
          receiptId.value,
      )

  if (originalImageUrl.value) {
    URL.revokeObjectURL(
        originalImageUrl.value,
    )
  }

  originalImageUrl.value =
      URL.createObjectURL(
          response.data,
      )
}

// 저장된 영수증 상세 정보와 품목을 조회한다.
async function loadReceiptDetail() {
  if (!receiptId.value) {
    return
  }

  loading.value = true
  loadErrorMessage.value = ''

  try {
    const response =
        await getReceipt(
            receiptId.value,
        )

    const receiptData =
        response.data?.data

    if (!receiptData) {
      throw new Error(
          '영수증 상세 응답이 올바르지 않습니다.',
      )
    }

    applyReceiptData(receiptData)

    try {
      await loadOriginalImage()
    } catch (imageError) {
      console.error(
          '영수증 원본 이미지 조회 실패',
          imageError,
      )

      originalImageUrl.value = ''
    }
  } catch (error) {
    loadErrorMessage.value =
        error.response?.data?.message ||
        error.message ||
        '영수증 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}


onMounted(() => {
  if (isExistingReceipt.value) {
    loadReceiptDetail()
  }
})

onBeforeUnmount(() => {
  if (originalImageUrl.value) {
    URL.revokeObjectURL(
        originalImageUrl.value,
    )
  }
})
</script>

<template>
  <main class="result-page">
    <header>
      <button @click="router.back()">‹</button>
      <h1>
        {{
          isExistingReceipt
              ? '영수증 상세'
              : '영수증 인식 결과'
        }}
      </h1>
      <button @click="editing=!editing">{{ editing ? '취소' : '수정' }}</button>
    </header>
    <div
        v-if="loading"
        class="detail-state"
    >
      영수증 정보를 불러오고 있습니다.
    </div>

    <div
        v-else-if="loadErrorMessage"
        class="detail-state error"
    >
      <p>{{ loadErrorMessage }}</p>

      <button
          type="button"
          @click="loadReceiptDetail"
      >
        다시 시도
      </button>
    </div>

    <div
        v-if="
    !loading &&
    !loadErrorMessage
  "
        class="toggle"
    >
      <button :class="{active:!translated}" @click="translated=false">원문</button>
      <button :class="{active:translated}" @click="translated=true">번역</button>
    </div>
    <section
        v-if="
    !loading &&
    !loadErrorMessage
  "
        class="receipt-paper"
    ><input
        v-if="editing && translated"
        v-model="form.merchantTranslatedName"
    >

      <input
          v-else-if="editing"
          v-model="form.merchantOriginalName"
      >

      <h2 v-else>
        {{ displayedMerchantName }}
      </h2>

      <small>
        {{ form.paymentDate || '결제일 미인식' }}
        ·
        {{ form.paymentTime || '시간 미인식' }}
      </small>
      <div class="dash"/>
      <div
          v-for="(item, index) in form.items"
          :key="item.displayOrder"
          class="item"
      >
  <span>
    <template v-if="editing &&
  !loading &&
  !loadErrorMessage">
      <input
          v-if="translated"
          v-model="item.translatedName"
          placeholder="번역 품목명"
      >

      <input
          v-else
          v-model="item.originalName"
          placeholder="원문 품목명"
      >
    </template>

    <b v-else>
      {{ getDisplayedItemName(item) }}
    </b>

    <small
        v-if="
        translated &&
        item.originalName
      "
    >
      {{ item.originalName }}
    </small>

    <small>
      수량 {{ item.quantity }}
    </small>
  </span>

        <div v-if="editing &&
  !loading &&
  !loadErrorMessage">
          <input
              v-model.number="item.quantity"
              type="number"
              min="1"
              placeholder="수량"
          >

          <input
              v-model.number="item.amount"
              type="number"
              min="0"
              step="0.01"
              placeholder="금액"
          >

          <button
              type="button"
              @click="removeItem(index)"
          >
            삭제
          </button>
        </div>

        <strong v-else>
          {{ form.currencyCode }}
          {{ Number(item.amount).toFixed(2) }}
        </strong>
      </div>

      <button
          v-if="editing &&
  !loading &&
  !loadErrorMessage"
          type="button"
          class="add-item-button"
          @click="addItem"
      >
        + 품목 추가
      </button>
      <div class="dash"/>

      <div class="total">
        <span>최종 결제 금액</span>

        <strong>
          {{ form.currencyCode }}

          <input
              v-if="editing &&
  !loading &&
  !loadErrorMessage"
              v-model.number="form.totalAmount"
              type="number"
              min="0"
              step="0.01"
          >

          <template v-else>
            {{ Number(form.totalAmount).toFixed(2) }}
          </template>
        </strong>
      </div>

      <p v-if="form.splitCount > 1">
        {{ form.splitCount }}명 분할 ·
        1인당
        {{ form.currencyCode }}
        {{ splitAmount.toFixed(2) }}
      </p>
    </section>
    <button v-if="
    !loading &&
    !loadErrorMessage" class="original-button" type="button" @click="showOriginal=true">▧ 실제 영수증 원본 사진 보기
    </button>
    <section
        v-if="editing &&
  !loading &&
  !loadErrorMessage"
        class="meta"
    >
      <label>
        <span>결제 날짜</span>

        <input
            v-model="form.paymentDate"
            type="date"
        >
      </label>

      <label>
        <span>결제 시간</span>

        <input
            v-model="form.paymentTime"
            type="time"
        >
      </label>

      <label>
        <span>통화 코드</span>

        <input
            v-model.trim="
        form.currencyCode
      "
            maxlength="3"
            placeholder="EUR"
        >
      </label>

      <label>
        <span>결제 금액</span>

        <div class="amount-input">
          <b>{{ form.currencyCode }}</b>

          <input
              v-model.number="
          form.totalAmount
        "
              min="0"
              step="0.01"
              type="number"
          >
        </div>
      </label>

      <label>
        <span>세금</span>

        <input
            v-model.number="form.taxAmount"
            min="0"
            step="0.01"
            type="number"
        >
      </label>

      <label>
        <span>공동 결제 인원</span>

        <input
            v-model.number="form.splitCount"
            min="1"
            type="number"
        >
      </label>
    </section>
    <div
        v-if="
    !loading &&
    !loadErrorMessage
  "
        class="actions"
    >
      <!-- 기존에 저장된 영수증 상세 화면 -->
      <template v-if="isExistingReceipt">
        <button
            type="button"
            :disabled="saving || deleting"
            @click="removeReceipt"
        >
          {{
            deleting
                ? '삭제 중...'
                : '삭제'
          }}
        </button>

        <button
            type="button"
            :disabled="saving || deleting"
            @click="
        editing
          ? prepareSave()
          : editing = true
      "
        >
          {{
            saving
                ? '저장 중...'
                : editing
                    ? '수정 저장'
                    : '인식 결과 수정'
          }}
        </button>
      </template>

      <!-- 새 OCR 분석 결과 화면 -->
      <template v-else>
        <button
            type="button"
            :disabled="saving"
            @click="discard"
        >
          추가하지 않기
        </button>

        <button
            type="button"
            :disabled="saving"
            @click="prepareSave"
        >
          {{
            saving
                ? '저장 중...'
                : '보관함에 추가'
          }}
        </button>
      </template>
    </div>
    <div
        v-if="showOriginal"
        class="original-modal"
        @click.self="showOriginal = false"
    >
      <section>
        <header>
          <b>실제 영수증 원본</b>

          <button
              type="button"
              @click="showOriginal = false"
          >
            ×
          </button>
        </header>

        <img
            v-if="originalImageUrl"
            :src="originalImageUrl"
            alt="영수증 원본 이미지"
            class="original-image"
        >

        <p v-else>
          원본 이미지를 확인할 수 없습니다.
        </p>
      </section>
    </div>
  </main>
</template>

<style scoped>
.result-page {
  min-height: 100vh;
  padding: 0 18px 50px;
  background: #f8f6f1;
  color: #111a2d
}

.result-page > header {
  display: grid;
  height: 88px;
  grid-template-columns:50px 1fr 50px;
  align-items: end;
  padding-bottom: 16px
}

.result-page > header h1 {
  text-align: center;
  font-size: 18px;
  font-weight: 900
}

.result-page > header button:first-child {
  font-size: 29px;
  text-align: left
}

.result-page > header button:last-child {
  text-align: right;
  color: #2670dd;
  font-size: 11px
}

.toggle {
  display: flex;
  justify-content: center;
  margin-bottom: 14px
}

.toggle button {
  padding: 8px 18px;
  border: 1px solid #dce3ed;
  color: #8491a3;
  font-size: 10px
}

.toggle button:first-child {
  border-radius: 10px 0 0 10px
}

.toggle button:last-child {
  border-radius: 0 10px 10px 0
}

.toggle .active {
  border-color: #246dd7;
  background: #246dd7;
  color: #fff
}

.receipt-paper {
  position: relative;
  margin: 0 auto;
  padding: 26px 22px;
  border-radius: 3px;
  background: #fffdf6;
  box-shadow: 0 7px 24px #263a5e18;
  text-align: center
}

.receipt-paper:after {
  position: absolute;
  right: 0;
  bottom: -7px;
  left: 0;
  height: 14px;
  background: linear-gradient(135deg, transparent 7px, #fffdf6 0) 0 0/14px 14px repeat-x;
  content: ''
}

.receipt-paper h2 {
  font-size: 13px
}

.receipt-paper > input {
  width: 100%;
  padding: 9px;
  border: 1px solid #cfdbea;
  text-align: center
}

.receipt-paper > small {
  display: block;
  margin-top: 6px;
  color: #8793a4;
  font-size: 8px
}

.dash {
  margin: 20px 0;
  border-top: 1px dashed #bfc5cc
}

.item {
  display: grid;
  grid-template-columns:1fr auto;
  align-items: center;
  margin-top: 17px;
  text-align: left
}

.item span b, .item span small {
  display: block
}

.item b {
  font-size: 10px
}

.item small {
  margin-top: 4px;
  color: #8995a6;
  font-size: 7px
}

.item strong {
  font-size: 9px
}

.item input {
  max-width: 145px;
  padding: 6px;
  border: 1px solid #d8e1ec
}

.item > input {
  width: 62px
}

.total {
  display: flex;
  align-items: center;
  justify-content: space-between
}

.total span {
  font-size: 8px
}

.total strong {
  color: #e67a22;
  font-size: 15px
}

.total input {
  width: 70px;
  padding: 5px;
  border: 1px solid #d8e1ec
}

.receipt-paper > p {
  margin-top: 6px;
  text-align: right;
  color: #8290a3;
  font-size: 8px
}

.receipt-paper > em {
  display: block;
  margin-top: 24px;
  color: #18a879;
  font-size: 8px;
  font-style: normal
}

.original-button {
  width: 100%;
  margin-top: 20px;
  padding: 13px;
  border: 1px solid #cfdbea;
  border-radius: 12px;
  background: #fff;
  color: #235fac;
  font-size: 10px;
  font-weight: 900
}

.meta {
  display: grid;
  gap: 11px;
  margin-top: 14px;
  padding: 16px;
  border-radius: 16px;
  background: #fff
}

.meta label {
  display: grid;
  grid-template-columns:90px 1fr;
  align-items: center;
  font-size: 10px;
  font-weight: 800
}

.meta label span small {
  display: block;
  margin-top: 3px;
  color: #2670dd;
  font-size: 7px
}

.meta input, .meta select {
  width: 100%;
  padding: 10px;
  border: 1px solid #dae2ec;
  border-radius: 9px
}

.amount-input {
  display: grid;
  grid-template-columns:42px 1fr;
  align-items: center
}

.amount-input b {
  font-size: 9px
}

.fixed-category {
  display: grid;
  grid-template-columns:1fr auto auto;
  align-items: center;
  padding: 11px;
  border-radius: 10px;
  background: #f3f6fb
}

.fixed-category span {
  font-size: 9px
}

.fixed-category b {
  font-size: 10px
}

.fixed-category em {
  margin-left: 7px;
  padding: 4px 6px;
  border-radius: 8px;
  background: #e6ecf5;
  color: #7c899b;
  font-size: 7px;
  font-style: normal
}

.actions {
  display: grid;
  grid-template-columns:1fr 2fr;
  gap: 10px;
  margin-top: 18px
}

.actions button {
  height: 52px;
  border: 1px solid #dce3ed;
  border-radius: 13px;
  color: #e5484d;
  font-size: 12px;
  font-weight: 900
}

.actions button:last-child {
  border: 0;
  background: #19489c;
  color: #fff
}

.original-modal {
  position: fixed;
  z-index: 100;
  display: grid;
  inset: 0;
  place-items: center;
  padding: 25px;
  background: #09172cbb
}

.original-modal > section {
  width: 100%;
  max-width: 340px;
  padding: 16px;
  border-radius: 18px;
  background: #f8f6f1
}

.original-modal header {
  display: flex;
  align-items: center;
  justify-content: space-between
}

.original-modal header b {
  font-size: 14px
}

.original-modal header button {
  font-size: 24px
}

.raw-paper {
  display: flex;
  width: 230px;
  min-height: 390px;
  flex-direction: column;
  margin: 18px auto 8px;
  padding: 30px 24px;
  background: #fff8e8;
  text-align: center
}

.raw-paper > b {
  font-size: 10px
}

.raw-paper > small {
  margin-top: 7px;
  font-size: 7px
}

.raw-paper i {
  height: 10px;
  margin-top: 12px;
  border-bottom: 1px solid #c8c2b4
}

.raw-paper strong {
  margin-top: auto;
  font-size: 10px
}

.item > div {
  display: grid;
  grid-template-columns: 58px 72px auto;
  gap: 5px;
  align-items: center;
}

.item > div input {
  width: 100%;
}

.item > div button {
  color: #e5484d;
  font-size: 9px;
}

.add-item-button {
  width: 100%;
  margin-top: 18px;
  padding: 10px;
  border: 1px dashed #2670dd;
  border-radius: 9px;
  color: #2670dd;
  font-size: 10px;
  font-weight: 800;
}

.original-image {
  display: block;
  width: 100%;
  max-height: 70vh;
  margin-top: 16px;
  object-fit: contain;
  border-radius: 10px;
}

.actions button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.detail-state {
  display: flex;
  min-height: 260px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #7d8a9c;
  font-size: 12px;
  text-align: center;
}

.detail-state.error {
  color: #e5484d;
}

.detail-state button {
  margin-top: 12px;
  padding: 8px 14px;
  border: 1px solid #2670dd;
  border-radius: 9px;
  color: #2670dd;
  font-size: 10px;
}
</style>
