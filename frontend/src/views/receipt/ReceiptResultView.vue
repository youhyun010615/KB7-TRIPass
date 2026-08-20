<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useReceiptStore } from '@/stores/receipt'
import { fetchTripGoal } from '@/api/travel'
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

const tripId = computed(() => {
  const value = Number(route.params.tripId)

  return Number.isInteger(value) && value > 0
      ? value
      : null
})

const receiptId = computed(() => {
  const value = Number(route.params.receiptId)

  return Number.isInteger(value) && value > 0
      ? value
      : null
})

const isExistingReceipt = computed(
    () => receiptId.value !== null,
)

const isOcrResult = computed(
    () => !isExistingReceipt.value,
)

const trip = ref(null)
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const loadErrorMessage = ref('')



const draft = store.draft
const sourceFile =
    draft?.sourceFile ?? null

const missingOcrDraft = computed(
    () =>
        isOcrResult.value &&
        (
            !draft ||
            !sourceFile
        ),
)

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

const initialDateTime =
    separatePaymentDateTime(
        draft?.paymentDateTime,
    )

const form = reactive({
  countryId:
      draft?.countryId ?? null,

  merchantOriginalName:
      draft?.originalMerchantName ?? '',

  merchantTranslatedName:
      draft?.translatedMerchantName ?? '',

  paymentDate:
  initialDateTime.date,

  paymentTime:
  initialDateTime.time,

  currencyCode:
      draft?.currencyCode ?? '',

  totalAmount:
      draft?.totalAmount ?? '',

  ocrRawText:
      draft?.rawText ?? '',

  items:
      (draft?.items ?? []).map(
          (item, index) => ({
            originalName:
                item.originalName ?? '',

            translatedName:
                item.translatedName ?? '',

            quantity:
                item.quantity ?? 1,

            amount:
                item.amount ?? '',

            displayOrder:
                item.displayOrder ??
                index + 1,
          }),
      ),

  sharedPayment:
      Number(draft?.splitCount ?? 1) > 1,

  splitCount:
      Math.max(
          Number(draft?.splitCount ?? 1),
          1,
      ),

  splitAmount:
      draft?.splitAmount ?? '',

  participants: [],

  memo:
      draft?.memo ?? '',
})

/*
 * OCR에서 품목을 인식하지 못한 경우에도
 * 사용자가 품목을 직접 입력할 수 있게 합니다.
 */
if (!form.items.length) {
  form.items.push(
      createEmptyItem(1),
  )
}

const editing = ref(
    route.meta.receiptMode === 'edit',
)

const editSnapshot = ref(null)
const translated = ref(true)
const showOriginal = ref(false)

const selectedCountry = computed(() => {
  return (
      trip.value?.countries?.find(
          country =>
              Number(country.countryId) ===
              Number(form.countryId),
      ) || null
  )
})

const selectedCountryName = computed(() => {
  return (
      selectedCountry.value?.countryName ||
      '국가 미지정'
  )
})

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
  const savedAmount = Number(form.splitAmount)

  if (
    !editing.value &&
    Number.isFinite(savedAmount) &&
    savedAmount > 0
  ) {
    return savedAmount
  }

  const amount =
      Number(form.totalAmount)

  const count =
      Number(form.splitCount)

  if (
      !Number.isFinite(amount) ||
      amount <= 0 ||
      !Number.isInteger(count) ||
      count <= 0
  ) {
    return 0
  }

  return amount / count
})

function createEmptyItem(displayOrder) {
  return {
    originalName: '',
    translatedName: '',
    quantity: 1,
    amount: '',
    displayOrder,
  }
}

function normalizeCurrencyCode() {
  form.currencyCode =
      String(form.currencyCode || '')
          .trim()
          .toUpperCase()
          .replace(/[^A-Z]/g, '')
          .slice(0, 3)
}

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
  form.items.push(
      createEmptyItem(
          form.items.length + 1,
      ),
  )
}

function removeItem(index) {
  if (form.items.length <= 1) {
    window.alert(
        '품목은 최소 한 개 이상 필요합니다.',
    )
    return
  }

  form.items.splice(index, 1)

  form.items.forEach(
      (item, itemIndex) => {
        item.displayOrder =
            itemIndex + 1
      },
  )
}

function updateParticipantCount() {
  const participantCount =
      form.sharedPayment
          ? Math.max(
              Number(form.splitCount) - 1,
              0,
          )
          : 0

  while (
      form.participants.length <
      participantCount
      ) {
    form.participants.push({
      id: null,
      participantName: '',
    })
  }

  if (
      form.participants.length >
      participantCount
  ) {
    form.participants.splice(
        participantCount,
    )
  }
}

function toggleSharedPayment() {
  form.splitCount =
      form.sharedPayment ? 2 : 1

  updateParticipantCount()
}

function increaseSplitCount() {
  /*
 * 로그인 사용자를 제외한 참여자는 최대 19명이며,
 * 로그인 사용자를 포함한 전체 인원은 최대 20명이다.
 */
  if (form.splitCount >= 20) {
    return
  }

  form.splitCount += 1
  updateParticipantCount()
}

function decreaseSplitCount() {
  if (form.splitCount <= 2) {
    return
  }

  form.splitCount -= 1
  updateParticipantCount()
}

function applyReceiptData(data) {
  const dateTime =
      separatePaymentDateTime(
          data.paymentDateTime,
      )

  form.countryId =
      data.countryId ?? null

  form.merchantOriginalName =
      data.merchantOriginalName ?? ''

  form.merchantTranslatedName =
      data.merchantTranslatedName ?? ''

  form.paymentDate =
      dateTime.date

  form.paymentTime =
      dateTime.time

  form.currencyCode =
      data.currencyCode ?? ''

  form.totalAmount =
      data.totalAmount ?? ''

  form.ocrRawText =
      data.ocrRawText ?? ''

  form.memo =
      data.memo ?? ''

  form.items =
      (data.items ?? []).map(
          (item, index) => ({
            id:
                item.id ?? null,

            originalName:
                item.originalName ?? '',

            translatedName:
                item.translatedName ?? '',

            quantity:
                item.quantity ?? 1,

            amount:
                item.amount ?? '',

            displayOrder:
                item.displayOrder ??
                index + 1,
          }),
      )

  if (!form.items.length) {
    form.items.push(
        createEmptyItem(1),
    )
  }

  form.participants =
      (data.participants ?? []).map(
          participant => ({
            id: participant.id ?? null,
            participantName:
                participant.participantName ?? '',
          }),
      )

  form.splitCount = Math.max(
      Number(data.splitCount) ||
      (form.participants.length
          ? form.participants.length + 1
          : 1),
      1,
  )

  form.sharedPayment =
      form.splitCount > 1

  form.splitAmount =
      data.splitAmount ?? ''
}

function startEditing() {
  editSnapshot.value =
      JSON.parse(
          JSON.stringify(form),
      )

  const hasTranslatedMerchant =
      Boolean(
          form.merchantTranslatedName?.trim(),
      )

  const hasTranslatedItem =
      form.items.some(
          item =>
              Boolean(
                  item.translatedName?.trim(),
              ),
      )

  /*
   * 수기 입력처럼 번역 데이터가 없으면
   * 실제 저장된 원문 값을 수정하도록
   * 원문 탭으로 자동 전환합니다.
   */
  if (
      !hasTranslatedMerchant &&
      !hasTranslatedItem
  ) {
    translated.value = false
  }

  editing.value = true
}

function cancelEditing() {
  if (editSnapshot.value) {
    Object.assign(
        form,
        JSON.parse(
            JSON.stringify(
                editSnapshot.value,
            ),
        ),
    )
  }

  editSnapshot.value = null
  editing.value = false
}

function toggleEditing() {
  if (editing.value) {
    cancelEditing()
    return
  }

  startEditing()
}

function validateForm() {
  if (!tripId.value) {
    return '여행 정보를 확인해 주세요.'
  }

  if (!form.countryId) {
    return '결제 국가를 선택해 주세요.'
  }

  if (!form.paymentDate) {
    return '결제 날짜를 입력해 주세요.'
  }

  normalizeCurrencyCode()

  if (
      !/^[A-Z]{3}$/.test(
          form.currencyCode,
      )
  ) {
    return (
        '통화 코드는 EUR, CHF처럼 ' +
        '영문 3자리로 입력해 주세요.'
    )
  }

  if (
      !Number.isFinite(
          Number(form.totalAmount),
      ) ||
      Number(form.totalAmount) <= 0
  ) {
    return (
        '최종 결제 금액은 ' +
        '0보다 커야 합니다.'
    )
  }

  if (!form.items.length) {
    return '품목을 한 개 이상 입력해 주세요.'
  }

  if (
      form.items.some(
          item =>
              !item.originalName.trim() &&
              !item.translatedName.trim() &&
              (
                  item.amount === '' ||
                  item.amount === null
              ),
      )
  ) {
    return '품목명 또는 품목 금액을 입력해 주세요.'
  }

  if (
      form.items.some(
          item =>
              !Number.isInteger(
                  Number(item.quantity),
              ) ||
              Number(item.quantity) < 1,
      )
  ) {
    return '품목 수량은 1 이상이어야 합니다.'
  }

  if (
      form.items.some(
          item =>
              item.amount !== '' &&
              item.amount !== null &&
              (
                  !Number.isFinite(
                      Number(item.amount),
                  ) ||
                  Number(item.amount) < 0
              ),
      )
  ) {
    return (
        '품목 금액은 0 이상의 ' +
        '숫자로 입력해 주세요.'
    )
  }

  if (
      form.sharedPayment &&
      form.participants.some(
          participant =>
              !participant.participantName.trim(),
      )
  ) {
    return (
        '공동결제 참여자 이름을 ' +
        '모두 입력해 주세요.'
    )
  }

  return ''
}

function createReceiptRequestData() {
  return {
    countryId:
        Number(form.countryId),

    currencyCode:
        form.currencyCode
            .trim()
            .toUpperCase(),

    paymentDateTime:
        `${form.paymentDate}T` +
        `${form.paymentTime || '00:00'}:00`,

    memo:
        form.memo.trim() || null,

    merchantOriginalName:
        form.merchantOriginalName
            .trim() ||
        null,

    merchantTranslatedName:
        form.merchantTranslatedName
            .trim() ||
        null,

    totalAmount:
        Number(form.totalAmount),

    ocrRawText:
        form.ocrRawText || null,

    items: form.items.map(item => ({
      /*
       * 상세 조회로 받은 기존 품목은 ID 전송,
       * 수정 화면에서 새로 추가한 품목은 null 전송
       */
      id: item.id ?? null,

      originalName:
          item.originalName.trim() || null,

      translatedName:
          item.translatedName.trim() ||
          null,

      quantity:
          Number(item.quantity),

      amount:
          item.amount === '' ||
          item.amount === null
              ? null
              : Number(item.amount),
    })),

    /*
     * 로그인 사용자는 포함하지 않고
     * 추가 참여자의 이름만 전송합니다.
     */
    participants:
        form.sharedPayment
            ? form.participants.map(
                participant => ({
                  id: participant.id,
                  participantName:
                      participant.participantName.trim(),
                }),
            )
            : [],
  }
}

function goToReceiptList() {
  return router.replace({
    name: 'Receipt',
    params: {
      tripId: tripId.value,
    },
  })
}

function discard() {
  if (isOcrResult.value) {
    store.$patch({
      draft: null,
    })
  }

  goToReceiptList()
}

async function saveReceipt() {
  if (saving.value) {
    return
  }

  if (
      isOcrResult.value &&
      !sourceFile
  ) {
    window.alert(
        '저장할 영수증 원본 이미지가 없습니다.',
    )
    return
  }

  const validationMessage =
      validateForm()

  if (validationMessage) {
    window.alert(validationMessage)
    return
  }

  saving.value = true

  try {
    const requestData =
        createReceiptRequestData()

    if (isExistingReceipt.value) {
      await updateReceipt(
          tripId.value,
          receiptId.value,
          requestData,
      )

      window.alert(
          '영수증이 수정되었습니다.',
      )
    } else {
      await createReceipt(
          tripId.value,
          requestData,
          sourceFile,
      )

      store.$patch({
        draft: null,
      })

      window.alert(
          '영수증이 저장되었습니다.',
      )
    }

    await goToReceiptList()
  } catch (error) {
    console.error(
        '영수증 저장 실패:',
        error,
    )

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

async function removeReceipt() {
  if (!receiptId.value) {
    discard()
    return
  }

  const confirmed =
      window.confirm(
          '이 영수증을 삭제할까요?',
      )

  if (!confirmed) {
    return
  }

  deleting.value = true

  try {
    await deleteReceipt(
        tripId.value,
        receiptId.value,
    )

    window.alert(
        '영수증이 삭제되었습니다.',
    )

    await goToReceiptList()
  } catch (error) {
    window.alert(
        error.response?.data?.message ||
        '영수증 삭제에 실패했습니다.',
    )
  } finally {
    deleting.value = false
  }
}

async function loadOriginalImage() {
  if (!receiptId.value) {
    return
  }

  const response =
      await getReceiptImage(
          tripId.value,
          receiptId.value,
      )

  const imageBlob =
      response.data

  if (
      !(imageBlob instanceof Blob) ||
      !imageBlob.type.startsWith('image/')
  ) {
    throw new Error(
        '영수증 원본 이미지 응답이 올바르지 않습니다.',
    )
  }

  if (originalImageUrl.value) {
    URL.revokeObjectURL(
        originalImageUrl.value,
    )
  }

  originalImageUrl.value =
      URL.createObjectURL(imageBlob)
}

async function loadReceiptDetail() {
  const response =
      await getReceipt(
          tripId.value,
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
        '영수증 원본 이미지 조회 실패:',
        imageError,
    )

    originalImageUrl.value = ''
  }
}

function handleLoadErrorAction() {
  if (missingOcrDraft.value) {
    return router.replace({
      name: 'ReceiptCapture',
      params: {
        tripId: tripId.value,
      },
    })
  }

  return initialize()
}

async function initialize() {
  if (!tripId.value) {
    loadErrorMessage.value =
        '여행 정보를 확인해 주세요.'
    return
  }

  if (missingOcrDraft.value) {
    loadErrorMessage.value =
        'OCR 분석 결과가 없습니다. 영수증을 다시 촬영해 주세요.'
    return
  }

  loading.value = true
  loadErrorMessage.value = ''

  try {
    trip.value =
        await fetchTripGoal(
            tripId.value,
        )

    if (!trip.value?.tripId) {
      throw new Error(
          '여행 상세 응답이 올바르지 않습니다.',
      )
    }

    if (isExistingReceipt.value) {
      await loadReceiptDetail()
    } else if (
        trip.value.countries?.length === 1
    ) {
      /*
       * OCR 응답에는 국가가 없으므로
       * 여행 국가가 하나인 경우 자동 선택합니다.
       */
      form.countryId =
          trip.value.countries[0].countryId

      if (!form.currencyCode) {
        form.currencyCode =
            trip.value.countries[0]
                .currencyCode || ''
      }
    }
  } catch (error) {
    console.error(
        '영수증 화면 초기화 실패:',
        error,
    )

    loadErrorMessage.value =
        error.response?.data?.message ||
        error.message ||
        '영수증 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(initialize)

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
    <header class="page-header">
      <button
          type="button"
          aria-label="뒤로 가기"
          @click="router.back()"
      >
        ‹
      </button>

      <h1>
        <template v-if="editing">
          인식 결과 수정
        </template>

        <template v-else-if="isExistingReceipt">
          영수증 상세
        </template>

        <template v-else>
          영수증 인식 결과
        </template>
      </h1>

      <span aria-hidden="true" />
    </header>

    <!-- 로딩 -->
    <section
        v-if="loading"
        class="detail-state"
    >
      영수증 정보를 불러오고 있습니다.
    </section>

    <!-- 조회 실패 -->
    <section
        v-else-if="loadErrorMessage"
        class="detail-state error"
    >
      <p>{{ loadErrorMessage }}</p>

      <button
          type="button"
          @click="handleLoadErrorAction"
      >
        {{
          missingOcrDraft
              ? '영수증 다시 촬영'
              : '다시 시도'
        }}
      </button>
    </section>

    <template v-else>
      <!-- 원문·번역 전환 -->
      <div class="translation-toggle">
        <button
            type="button"
            :class="{ active: !translated }"
            @click="translated = false"
        >
          원문
        </button>

        <button
            type="button"
            :class="{ active: translated }"
            @click="translated = true"
        >
          번역
        </button>
      </div>

      <!-- 영수증 인식 결과 -->
      <section class="receipt-paper tripass-receipt-document">
        <div class="receipt-paper-heading">
          <span>TRIPASS</span>
          <b>여행 영수증</b>
          <button type="button" @click="toggleEditing">
            {{ editing ? '취소' : '수정' }}
          </button>
        </div>

        <!-- 상호명 -->
        <label class="field merchant-field">
          <span>상호명</span>

          <input
              v-if="editing && translated"
              v-model.trim="
              form.merchantTranslatedName
            "
              class="merchant-input"
              type="text"
              maxlength="255"
              placeholder="번역된 상호명"
          >

          <input
              v-else-if="editing"
              v-model.trim="
              form.merchantOriginalName
            "
              class="merchant-input"
              type="text"
              maxlength="255"
              placeholder="원문 상호명"
          >

          <div v-else class="readonly-value merchant-value">
            {{ displayedMerchantName }}
          </div>
        </label>

        <label class="field trip-field">
          <span>여행</span>
          <div class="readonly-trip">
            ✈️ {{ trip?.tripName || '여행 정보 확인 중' }}
          </div>
        </label>

        <!-- 국가·날짜·시간 -->
        <label class="field country-field">
          <span>국가</span>
          <select
              v-if="editing"
              v-model.number="form.countryId"
              aria-label="결제 국가"
          >
            <option :value="null">
              국가 선택
            </option>

            <option
                v-for="country in
                trip?.countries || []"
                :key="country.countryId"
                :value="country.countryId"
            >
              {{ country.countryName }}
            </option>
          </select>

          <div v-else class="readonly-value">
            {{ selectedCountryName }}
          </div>
        </label>

        <div class="field-grid datetime-field">
          <label class="field">
            <span>결제 날짜</span>
            <input v-if="editing" v-model="form.paymentDate" type="date">
            <div v-else class="readonly-value">
              {{ form.paymentDate || '날짜 미지정' }}
            </div>
          </label>

          <label class="field">
            <span>결제 시간</span>
            <input v-if="editing" v-model="form.paymentTime" type="time">
            <div v-else class="readonly-value">
              {{ form.paymentTime || '시간 미지정' }}
            </div>
          </label>
        </div>

        <label class="field currency-field">
          <span>통화 코드</span>
          <input
              v-if="editing"
              v-model="form.currencyCode"
              type="text"
              maxlength="3"
              placeholder="예: EUR"
              @input="normalizeCurrencyCode"
          >
          <div v-else class="readonly-value">
            {{ form.currencyCode || '통화 미지정' }}
          </div>
        </label>

        <div class="items-heading result-items-heading">
          <div>
            <b>결제 품목</b>
            <small>영수증에 기록된 결제 항목이에요.</small>
          </div>
          <strong>{{ form.items.length }}개 품목</strong>
        </div>

        <div class="dash" />

        <!-- 품목 목록 -->
        <article
            v-for="(item, index) in form.items"
            :key=" item.id != null
            ? `saved-item-${item.id}`
            : `new-item-${index}`"
            class="receipt-item"
        >
          <template v-if="editing">
            <div class="item-name-area">
              <input
                  v-if="translated"
                  v-model.trim="
                  item.translatedName
                "
                  type="text"
                  maxlength="255"
                  placeholder="번역 품목명"
              >

              <input
                  v-else
                  v-model.trim="
                  item.originalName
                "
                  type="text"
                  maxlength="255"
                  placeholder="원문 품목명"
              >

              <small
                  v-if="
                  translated &&
                  item.originalName
                "
              >
                {{ item.originalName }}
              </small>
            </div>

            <div class="item-edit-area">
              <label>
                <span>수량</span>

                <input
                    v-model.number="
                    item.quantity
                  "
                    type="number"
                    min="1"
                    step="1"
                    inputmode="numeric"
                >
              </label>

              <label>
                <span>금액</span>

                <input
                    v-model="item.amount"
                    type="number"
                    min="0"
                    step="0.01"
                    inputmode="decimal"
                >
              </label>

              <button
                  type="button"
                  :disabled="
                  form.items.length <= 1
                "
                  @click="removeItem(index)"
              >
                삭제
              </button>
            </div>
          </template>

          <template v-else>
            <div class="item-name-area">
              <b>
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
            </div>

            <strong>
              {{ form.currencyCode }}
              {{
                Number(
                    item.amount || 0,
                ).toFixed(2)
              }}
            </strong>
          </template>
        </article>

        <div class="dash" />

        <!-- 최종 결제 금액 -->
        <div class="total">
          <span>
            {{ translated ? '최종 결제 금액' : 'TOTALE' }}
          </span>

          <strong>
            <template v-if="editing">
              <span class="total-currency">{{ form.currencyCode || '통화' }}</span>

              <input
                  v-model="form.totalAmount"
                  class="total-input"
                  type="number"
                  min="0.01"
                  step="0.01"
                  inputmode="decimal"
              >
            </template>

            <template v-else>
              {{ form.currencyCode }}
              {{
                Number(
                    form.totalAmount || 0,
                ).toFixed(2)
              }}
            </template>
          </strong>
        </div>

        <p
            v-if="
            !editing &&
            form.splitCount > 1
          "
            class="split-summary"
        >
          {{ form.splitCount }}명 분할
          · 1인당
          {{ form.currencyCode }}
          {{ splitAmount.toFixed(2) }}
        </p>

        <!-- 최종 결제 금액과 원본 사진 사이 -->
        <button
            v-if="editing"
            type="button"
            class="add-item-button"
            @click="addItem"
        >
          ＋ 품목 추가
        </button>

        <p class="receipt-document-footer">
          THANK YOU FOR TRAVELING WITH TRIPASS
        </p>
      </section>

      <!-- 실제 영수증 사진 -->
      <button
          class="original-button"
          type="button"
          @click="showOriginal = true"
      >
        🖼 실제 영수증 원본 사진 보기
      </button>

      <section
          v-if="!editing && form.splitCount > 1"
          class="shared-payment-summary"
      >
        <div class="shared-summary-heading">
          <div>
            <small>SHARED PAYMENT</small>
            <h2>공동결제 정보</h2>
          </div>
          <strong>{{ form.splitCount }}명</strong>
        </div>

        <div class="shared-summary-amount">
          <span>1인당 정산 금액</span>
          <b>
            {{ form.currencyCode }}
            {{ splitAmount.toFixed(2) }}
          </b>
        </div>

        <div class="shared-summary-participants">
          <span>함께 결제한 사람</span>
          <div>
            <em>나</em>
            <em
                v-for="participant in form.participants"
                :key="participant.id ?? participant.participantName"
            >
              {{ participant.participantName }}
            </em>
          </div>
        </div>
      </section>

      <!-- 수정 상태에서만 공동결제 표시 -->
      <section
          v-if="editing"
          class="shared-payment-card"
      >
        <div class="shared-heading">
          <div>
            <b>♧ 공동 인원 추가</b>

            <small>
              로그인 사용자를 포함한 전체
              결제 인원을 설정합니다.
            </small>
          </div>

          <label class="switch">
            <input
                v-model="form.sharedPayment"
                type="checkbox"
                @change="
                toggleSharedPayment
              "
            >

            <i />
          </label>
        </div>

        <template v-if="form.sharedPayment">
          <div class="people-count">
            <span>전체 인원 수</span>

            <div>
              <button
                  type="button"
                  aria-label="인원 감소"
                  @click="
                  decreaseSplitCount
                "
              >
                −
              </button>

              <b>{{ form.splitCount }}</b>

              <button
                  type="button"
                  aria-label="인원 증가"
                  @click="
                  increaseSplitCount
                "
              >
                +
              </button>
            </div>
          </div>

          <div class="per-person">
            <span>1인당 결제 금액</span>

            <b>
              {{ form.currencyCode }}
              {{ splitAmount.toFixed(2) }}
            </b>
          </div>

          <div class="participants">
            <b>결제 인원</b>

            <small>
              로그인 사용자를 제외한
              참여자 이름
            </small>

            <input
                v-for="(participant, index) in
                form.participants"
                :key="participant.id ?? `new-participant-${index}`"
                v-model.trim="participant.participantName"
                type="text"
                maxlength="100"
                :placeholder="`참여자 ${index + 1}`"
            />
          </div>
        </template>
      </section>

      <!-- 모든 상태에서 메모 표시 -->
      <section class="memo-card">
        <span>메모</span>

        <textarea
            v-if="editing"
            v-model.trim="form.memo"
            maxlength="500"
            placeholder="메모를 입력하세요"
        />

        <p v-else>
          {{
            form.memo ||
            '작성된 메모가 없습니다.'
          }}
        </p>
      </section>

      <!-- 하단 작업 버튼 -->
      <div class="actions">
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
                ? saveReceipt()
                : startEditing()
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
              @click="saveReceipt"
          >
            {{
              saving
                  ? '저장 중...'
                  : '보관함에 추가'
            }}
          </button>
        </template>
      </div>
    </template>

    <!-- 원본 이미지 모달 -->
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
  height: 63px;
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
.page-header {
  display: grid;
  height: 88px;
  grid-template-columns: 50px 1fr 50px;
  align-items: end;
  padding-bottom: 16px;
}

.page-header h1 {
  font-size: 18px;
  font-weight: 900;
  text-align: center;
}

.page-header > button:first-child {
  font-size: 29px;
  text-align: left;
}

.page-header > button:last-child {
  color: #2670dd;
  font-size: 11px;
  text-align: right;
}

.translation-toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-bottom: 14px;
}

.translation-toggle button {
  height: 36px;
  border: 1px solid #dce3ed;
  color: #8491a3;
  font-size: 10px;
}

.translation-toggle button:first-child {
  border-radius: 10px 0 0 10px;
}

.translation-toggle button:last-child {
  border-radius: 0 10px 10px 0;
}

.translation-toggle .active {
  border-color: #173f8c;
  background: #173f8c;
  color: #fff;
}

.merchant-input {
  width: 100%;
  padding: 11px;
  border: 1px solid #cfdbea;
  border-radius: 9px;
  font-weight: 800;
  text-align: center;
}

.receipt-information {
  display: block;
  margin-top: 8px;
  color: #8793a4;
  font-size: 8px;
  text-align: center;
}

.receipt-information-edit {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr)
    minmax(0, 1.25fr)
    minmax(0, 1fr);
  gap: 7px;
  margin-top: 9px;
}

.receipt-information-edit select,
.receipt-information-edit input {
  width: 100%;
  min-width: 0;
  padding: 7px 4px;
  border: 1px solid #cfdbea;
  border-radius: 9px;
  background: #fff;
  font-size: 8px;
  text-align: center;
}

.receipt-item {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
  text-align: left;
}

.receipt-item strong {
  font-size: 9px;
}

.item-name-area {
  min-width: 0;
}

.item-name-area b,
.item-name-area small {
  display: block;
}

.item-name-area b {
  font-size: 10px;
}

.item-name-area small {
  margin-top: 4px;
  color: #8995a6;
  font-size: 7px;
}

.item-name-area input {
  width: 100%;
  padding: 8px;
  border: 1px solid #cfdbea;
  border-radius: 8px;
}

.item-edit-area {
  display: grid;
  grid-template-columns: 55px 72px auto;
  align-items: end;
  gap: 5px;
}

.item-edit-area label span {
  display: block;
  margin-bottom: 3px;
  color: #7d8a9c;
  font-size: 7px;
}

.item-edit-area input {
  width: 100%;
  min-width: 0;
  padding: 8px 4px;
  border: 1px solid #cfdbea;
  border-radius: 8px;
}

.item-edit-area button {
  padding: 8px 2px;
  color: #e5484d;
  font-size: 8px;
}

.item-edit-area button:disabled {
  opacity: 0.35;
}

.total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.total > span {
  color: #536175;
  font-size: 8px;
  font-weight: 800;
}

.total strong {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e67a22;
  font-size: 15px;
}

.currency-input {
  width: 54px;
  padding: 8px 4px;
  border: 1px solid #cfdbea;
  border-radius: 8px;
  text-align: center;
}

.total-input {
  width: 78px;
  padding: 8px;
  border: 1px solid #e67a22;
  border-radius: 8px;
  color: #e67a22;
  text-align: right;
}

.split-summary {
  margin-top: 8px;
  color: #7d8a9c;
  font-size: 8px;
  text-align: right;
}

.add-item-button {
  width: 100%;
  margin-top: 20px;
  padding: 11px;
  border: 1px dashed #2670dd;
  border-radius: 10px;
  color: #2670dd;
  font-size: 10px;
  font-weight: 800;
}

.category-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  padding: 15px 16px;
  border-radius: 14px;
  background: #fff;
}

.category-card span {
  color: #536175;
  font-size: 10px;
  font-weight: 800;
}

.category-card strong {
  font-size: 11px;
}

.category-card select {
  min-width: 130px;
  padding: 9px;
  border: 1px solid #d8e1ec;
  border-radius: 9px;
  background: #fff;
}

.shared-payment-card {
  margin-top: 12px;
  padding: 16px;
  border-radius: 16px;
  background: #fff2ad;
}

.shared-payment-summary {
  margin-top: 12px;
  padding: 17px;
  border: 1px solid #dce6f3;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 9px 25px rgba(32, 65, 120, .07);
}

.shared-summary-heading,
.shared-summary-amount {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.shared-summary-heading small {
  color: #2f6fed;
  font-size: 7px;
  font-weight: 900;
  letter-spacing: .14em;
}

.shared-summary-heading h2 {
  margin-top: 3px;
  font-size: 14px;
  font-weight: 950;
}

.shared-summary-heading > strong {
  padding: 6px 9px;
  border-radius: 999px;
  background: #eef4ff;
  color: #235fac;
  font-size: 9px;
}

.shared-summary-amount {
  margin-top: 14px;
  padding: 12px;
  border-radius: 12px;
  background: #f3f7fd;
}

.shared-summary-amount span,
.shared-summary-participants > span {
  color: #718099;
  font-size: 8px;
  font-weight: 900;
}

.shared-summary-amount b {
  color: #17499c;
  font-size: 13px;
}

.shared-summary-participants {
  margin-top: 13px;
}

.shared-summary-participants > div {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.shared-summary-participants em {
  padding: 6px 9px;
  border-radius: 999px;
  background: #f5f7fb;
  color: #43536b;
  font-size: 8px;
  font-style: normal;
  font-weight: 900;
}

.shared-summary-participants em:first-child {
  background: #eaf2ff;
  color: #2464c4;
}

.shared-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.shared-heading b,
.shared-heading small {
  display: block;
}

.shared-heading b {
  color: #155fbb;
  font-size: 11px;
}

.shared-heading small {
  margin-top: 5px;
  color: #5f6d80;
  font-size: 8px;
}

.switch {
  position: relative;
  width: 44px;
  height: 25px;
  flex: 0 0 auto;
}

.switch input {
  position: absolute;
  opacity: 0;
}

.switch i {
  position: absolute;
  border-radius: 14px;
  background: #cbd6e5;
  inset: 0;
}

.switch i::after {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 19px;
  height: 19px;
  border-radius: 50%;
  background: #fff;
  content: '';
  transition: transform 0.2s;
}

.switch input:checked + i {
  background: #2670dd;
}

.switch input:checked + i::after {
  transform: translateX(19px);
}

.people-count,
.per-person {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
}

.people-count > span,
.per-person > span {
  font-size: 9px;
  font-weight: 800;
}

.people-count > div {
  display: flex;
  align-items: center;
  gap: 11px;
}

.people-count button {
  display: grid;
  width: 27px;
  height: 27px;
  place-items: center;
  border-radius: 50%;
  background: #fff;
  color: #173f8c;
}

.people-count button:last-child {
  background: #173f8c;
  color: #fff;
}

.per-person b {
  color: #173f8c;
  font-size: 11px;
}

.participants {
  display: grid;
  gap: 7px;
  margin-top: 14px;
}

.participants > b {
  font-size: 10px;
}

.participants > small {
  color: #6f7b8d;
  font-size: 8px;
}

.participants input {
  width: 100%;
  padding: 10px;
  border: 1px solid #e3d893;
  border-radius: 9px;
  background: #fff;
}

.memo-card {
  margin-top: 12px;
}

.memo-card > span {
  display: block;
  margin-bottom: 7px;
  color: #536175;
  font-size: 10px;
  font-weight: 800;
}

.memo-card textarea,
.memo-card p {
  width: 100%;
  min-height: 70px;
  padding: 12px;
  border: 1px solid #d8e1ec;
  border-radius: 11px;
  background: #fff;
  color: #111a2d;
  font-size: 10px;
  line-height: 1.5;
}

.memo-card textarea {
  resize: vertical;
}

.memo-card p {
  margin: 0;
  color: #667487;
}

.actions {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 10px;
  margin-top: 18px;
}

.actions button {
  height: 52px;
  border: 1px solid #f0aaa7;
  border-radius: 13px;
  color: #e5484d;
  font-size: 12px;
  font-weight: 900;
}

.actions button:last-child {
  border: 0;
  background: #173f8c;
  color: #fff;
}

.actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

/* Receipt detail — travel receipt concept */
.result-page {
  min-height: 100dvh;
  padding: 0 16px 108px;
  background:
    radial-gradient(circle at 100% 0, rgba(47, 111, 237, .07), transparent 260px),
    #f4f7fc;
}

.result-page > .page-header {
  position: sticky;
  top: 0;
  z-index: 30;
  height: 72px;
  align-items: center;
  padding: 0;
  background: rgba(244, 247, 252, .94);
  backdrop-filter: blur(14px);
}

.result-page > .page-header h1 { font-size: 18px; }
.result-page > .page-header button:first-child {
  width: 38px;
  height: 38px;
  border-radius: 13px;
}

.translation-toggle {
  gap: 5px;
  margin: 6px 0 15px;
  padding: 5px;
  border-radius: 16px;
  background: #e8edf6;
}

.translation-toggle button {
  height: 42px;
  border: 0;
  border-radius: 12px !important;
  color: #8b98ad;
  font-size: 11px;
  font-weight: 900;
}

.translation-toggle .active {
  background: #173f8d;
  color: #fff;
  box-shadow: 0 6px 16px rgba(23, 63, 141, .2);
}

.result-page .tripass-receipt-document {
  overflow: visible;
  margin: 0 0 26px;
  padding: 0 19px 24px;
  border-top: 0;
  border-color: #173f8d;
  border-radius: 0 0 7px 7px;
  background: #fffaf0;
  box-shadow: 0 15px 34px rgba(45, 54, 74, .1);
}

.result-page .tripass-receipt-document::before {
  position: absolute;
  top: -7px;
  right: -1px;
  left: -1px;
  height: 8px;
  background:
    radial-gradient(circle at 7px 7px, #fffaf0 6px, transparent 6.5px) 0 1px / 14px 8px repeat-x,
    radial-gradient(circle at 7px 7px, #173f8d 7px, transparent 7.5px) 0 0 / 14px 8px repeat-x;
  content: '';
  pointer-events: none;
}

.result-page .tripass-receipt-document::after {
  background:
    radial-gradient(circle at 7px 0, #fffaf0 6px, transparent 6.5px) 0 0 / 14px 11px repeat-x,
    radial-gradient(circle at 7px 0, #173f8d 7px, transparent 7.5px) 0 1px / 14px 11px repeat-x;
}

.result-page .receipt-paper-heading {
  padding: 18px 1px 16px;
  border-color: #d8cfba;
}

.result-page .receipt-paper-heading span { color: #173f8d; font-size: 8px; }
.result-page .receipt-paper-heading b { font-size: 14px; }
.result-page .receipt-paper-heading small { font-size: 10px; }
.result-page .receipt-paper-heading button {
  justify-self: end;
  padding: 5px 8px;
  border-radius: 8px;
  background: #eaf1ff;
  color: #2f6fed;
  font-size: 11px;
  font-weight: 900;
}

.result-page .tripass-receipt-document .field { margin-top: 16px; }
.result-page .tripass-receipt-document .field > span {
  margin-bottom: 7px;
  color: #93a0b5;
  font-size: 10.5px;
  text-align: center;
}

.result-page .tripass-receipt-document .readonly-trip,
.result-page .tripass-receipt-document .readonly-value {
  height: 48px;
  justify-content: center;
  border-radius: 13px;
  background: rgba(35, 53, 82, .055);
  color: #17243a;
  font-size: 14px;
  font-weight: 800;
}

.result-page .tripass-receipt-document .merchant-value {
  height: 58px;
  font-size: 17px;
}

/* 상단 결제 정보도 하단 품목 영역과 같은 영수증 톤으로 연결 */
.result-page .tripass-receipt-document.tripass-receipt-document .readonly-trip,
.result-page .tripass-receipt-document.tripass-receipt-document .readonly-value {
  border: 1px solid #e5decd;
  background: #f7f3e9;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .7);
}

.result-page .tripass-receipt-document.tripass-receipt-document .merchant-value {
  border-color: #ddd4bf;
  background: #f2ede1;
}

.result-page .tripass-receipt-document .currency-field {
  padding-bottom: 18px;
  border-bottom: 1px dashed #d8cfba;
}

.result-page .tripass-receipt-document .result-items-heading {
  margin-top: 0;
  padding-top: 16px;
  border-top: 0;
}

.result-page .tripass-receipt-document .result-items-heading {
  border-color: #d8cfba;
}

.result-page .tripass-receipt-document .result-items-heading b { font-size: 14px; }
.result-page .tripass-receipt-document .result-items-heading small { font-size: 11px; }
.result-page .tripass-receipt-document .result-items-heading strong { font-size: 11.5px; }

.result-page .tripass-receipt-document .receipt-item {
  border-color: #ddd5c3;
}

.result-page .tripass-receipt-document .item-name-area b { font-size: 14px; }
.result-page .tripass-receipt-document .item-name-area small { font-size: 10.5px; }
.result-page .tripass-receipt-document .receipt-item > strong { font-size: 14px; }
.result-page .tripass-receipt-document .total { border-color: #d8cfba; }
.result-page .tripass-receipt-document .total > span { color: #5a6478; font-size: 12.5px; }
.result-page .tripass-receipt-document .total strong { color: #0b2a6b; font-size: 20px; }
.result-page .tripass-receipt-document .split-summary { font-size: 11px; }
.result-page .tripass-receipt-document > .receipt-document-footer {
  margin-top: 20px;
  color: #c7b98f;
  font-size: 8px;
}

.original-button,
.shared-payment-summary,
.memo-card {
  margin-right: 0 !important;
  margin-left: 0 !important;
}

.original-button {
  height: 54px;
  margin-top: 0;
  padding: 0 14px;
  border-color: #e4e8f0;
  border-radius: 14px;
  color: #5a6478;
  font-size: 13px;
}

.shared-payment-summary,
.memo-card {
  margin-top: 14px;
  padding: 18px;
  border: 0;
  border-radius: 18px;
  box-shadow: 0 4px 14px rgba(16, 25, 43, .07);
}

.shared-payment-summary { display: flex; flex-direction: column; gap: 14px; }
.shared-summary-heading small { font-size: 9.5px; }
.shared-summary-heading h2 { margin-top: 5px; font-size: 15px; }
.shared-summary-heading > strong {
  padding: 5px 11px;
  background: #eaf1ff;
  color: #0b2a6b;
  font-size: 11px;
}
.shared-summary-amount { margin-top: 0; padding: 14px 16px; background: #f6f8fc; }
.shared-summary-amount span,
.shared-summary-participants > span { color: #5a6478; font-size: 11.5px; }
.shared-summary-amount b { color: #0b2a6b; font-size: 15px; }
.shared-summary-participants { margin-top: 0; }
.shared-summary-participants > div { margin-top: 8px; }
.shared-summary-participants em {
  display: grid;
  min-width: 34px;
  height: 34px;
  place-items: center;
  padding: 0 10px;
  font-size: 12.5px;
}

.memo-card > span {
  color: #17243a;
  font-size: 13.5px;
  font-weight: 950;
}

.memo-card p {
  min-height: 70px;
  padding: 13px 14px;
  border: 0;
  background: #f6f8fc;
  color: #98a2b3;
  font-size: 12.5px;
}

.result-page > .actions {
  position: fixed;
  right: auto;
  bottom: 0;
  left: 50%;
  z-index: 40;
  width: 100%;
  max-width: 390px;
  margin: 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid #eceff5;
  background: #fff;
  transform: translateX(-50%);
}

.result-page > .actions button {
  height: 54px;
  border: 0;
  border-radius: 14px;
  font-size: 14px;
}
.result-page > .actions button:first-child { background: #fdeeee; color: #d64545; }
.result-page > .actions button:last-child { background: #0b2a6b; }

@media (max-width: 360px) {
  .receipt-information-edit {
    grid-template-columns: 1fr;
  }

  .receipt-item {
    grid-template-columns: 1fr;
  }

  .item-edit-area {
    grid-template-columns: 1fr 1fr auto;
  }
}
</style>

<style src="../../assets/receipt-document.css"></style>
