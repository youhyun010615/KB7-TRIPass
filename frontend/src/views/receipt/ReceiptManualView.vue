<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createReceipt } from '@/api/receipt'
import { fetchTripGoal } from '@/api/travel'

import {
  receiptCategories as categories,
} from '@/constants/receiptCategories'

const route = useRoute()
const router = useRouter()

const tripId = computed(() => {
  const value = Number(route.params.tripId)

  return Number.isInteger(value) && value > 0
      ? value
      : null
})

const trip = ref(null)
const loadingTrip = ref(false)
const saving = ref(false)
const loadError = ref('')


function getToday() {
  const current = new Date()
  const offset =
      current.getTimezoneOffset() * 60 * 1000

  return new Date(current.getTime() - offset)
      .toISOString()
      .slice(0, 10)
}

function createEmptyItem(displayOrder) {
  return {
    originalName: '',
    translatedName: '',
    quantity: 1,

    // 품목 한 행의 전체 금액
    amount: '',

    displayOrder,
  }
}

const form = reactive({
  countryId: null,
  categoryId: null,

  merchantOriginalName: '',

  paymentDate: getToday(),
  paymentTime: '',

  currencyCode: '',

  /*
   * 품목 합계와 별도로 관리합니다.
   * 세금·팁·할인 등이 반영된 실제 결제 금액입니다.
   */
  totalAmount: '',

  items: [
    createEmptyItem(1),
  ],

  sharedPayment: false,

  // 로그인 사용자를 포함한 전체 결제 인원
  splitCount: 1,

  // 로그인 사용자를 제외한 공동결제 참여자
  participantNames: [],

  memo: '',
})

const selectedCountry = computed(() => {
  return (
      trip.value?.countries?.find(
          country =>
              Number(country.countryId) ===
              Number(form.countryId),
      ) || null
  )
})

/*
 * 품목 합계는 참고용입니다.
 * 최종 결제 금액을 강제로 변경하지 않습니다.
 */
const itemTotalAmount = computed(() => {
  return form.items.reduce((total, item) => {
    const amount = Number(item.amount)

    return total + (
        Number.isFinite(amount)
            ? amount
            : 0
    )
  }, 0)
})

const perPersonAmount = computed(() => {
  const amount = Number(form.totalAmount)
  const count = Number(form.splitCount)

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

const selectedFile = ref(null)
const previewUrl = ref('')
const fileInput = ref(null)

async function loadTrip() {
  if (!tripId.value) {
    loadError.value =
        '올바른 여행 정보가 전달되지 않았습니다.'
    return
  }

  loadingTrip.value = true
  loadError.value = ''

  try {
    const data =
        await fetchTripGoal(tripId.value)

    if (!data?.tripId) {
      throw new Error(
          '여행 상세 응답이 올바르지 않습니다.',
      )
    }

    trip.value = {
      ...data,
      countries: Array.isArray(data.countries)
          ? data.countries
          : [],
    }

    /*
     * 여행 국가가 한 개라면 자동으로 선택합니다.
     * 여러 국가라면 사용자가 결제 국가를 선택합니다.
     */
    if (trip.value.countries.length === 1) {
      form.countryId =
          trip.value.countries[0].countryId
    }
  } catch (error) {
    console.error(
        '여행 정보 조회 실패:',
        error,
    )

    loadError.value =
        error.response?.data?.message ||
        error.message ||
        '여행 정보를 불러오지 못했습니다.'
  } finally {
    loadingTrip.value = false
  }
}

function openFilePicker() {
  fileInput.value?.click()
}

function selectImage(event) {
  const file = event.target.files?.[0]

  if (!file) {
    return
  }

  const allowedTypes = [
    'image/jpeg',
    'image/png',
  ]

  if (!allowedTypes.includes(file.type)) {
    window.alert(
        'JPG, JPEG, PNG 이미지만 첨부할 수 있습니다.',
    )

    event.target.value = ''
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    window.alert(
        '영수증 이미지는 최대 10MB까지 첨부할 수 있습니다.',
    )

    event.target.value = ''
    return
  }

  removeImage()

  selectedFile.value = file
  previewUrl.value =
      URL.createObjectURL(file)
}

function removeImage() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }

  selectedFile.value = null
  previewUrl.value = ''

  if (fileInput.value) {
    fileInput.value.value = ''
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

function addItem() {
  form.items.push(
      createEmptyItem(form.items.length + 1),
  )
}

function removeItem(index) {
  if (form.items.length <= 1) {
    window.alert(
        '품목은 최소 한 개 이상 입력해야 합니다.',
    )
    return
  }

  form.items.splice(index, 1)

  form.items.forEach(
      (item, itemIndex) => {
        item.displayOrder = itemIndex + 1
      },
  )
}

function applyItemTotal() {
  form.totalAmount =
      itemTotalAmount.value.toFixed(2)
}

function increaseSplitCount() {
  if (form.splitCount >= 20) {
    return
  }

  form.splitCount += 1
}

function decreaseSplitCount() {
  if (form.splitCount <= 2) {
    return
  }

  form.splitCount -= 1
}

function updateParticipantCount() {
  const participantCount =
      form.sharedPayment
          ? Math.max(form.splitCount - 1, 0)
          : 0

  while (
      form.participantNames.length <
      participantCount
      ) {
    form.participantNames.push('')
  }

  if (
      form.participantNames.length >
      participantCount
  ) {
    form.participantNames.splice(
        participantCount,
    )
  }
}

/*
 * 국가를 선택하면 여행 국가의 기본 통화 코드를 넣습니다.
 * 사용자가 다른 통화 코드로 직접 변경하는 것도 가능합니다.
 */
watch(
    selectedCountry,
    country => {
      form.currencyCode =
          country?.currencyCode || ''
    },
)

watch(
    () => form.sharedPayment,
    enabled => {
      form.splitCount = enabled ? 2 : 1
      updateParticipantCount()
    },
)

watch(
    () => form.splitCount,
    updateParticipantCount,
)

function validateForm() {
  if (!tripId.value) {
    return '여행 정보를 확인해 주세요.'
  }

  if (!trip.value) {
    return '여행 정보를 불러온 후 저장해 주세요.'
  }

  if (!form.merchantOriginalName.trim()) {
    return '상호명을 입력해 주세요.'
  }

  if (!form.countryId) {
    return '결제 국가를 선택해 주세요.'
  }

  if (!form.categoryId) {
    return '카테고리를 선택해 주세요.'
  }

  if (!form.paymentDate) {
    return '결제 날짜를 입력해 주세요.'
  }

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

  if (!form.items.length) {
    return '품목을 한 개 이상 입력해 주세요.'
  }

  if (
      form.items.some(
          item =>
              !item.originalName.trim(),
      )
  ) {
    return '모든 품목명을 입력해 주세요.'
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
              item.amount === '' ||
              !Number.isFinite(
                  Number(item.amount),
              ) ||
              Number(item.amount) <= 0,
      )
  ) {
    return (
        '모든 품목 금액은 ' +
        '0보다 크게 입력해 주세요.'
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

  if (
      form.sharedPayment &&
      (
          form.splitCount < 2 ||
          form.splitCount > 20
      )
  ) {
    return (
        '공동결제 인원은 ' +
        '2명 이상 20명 이하여야 합니다.'
    )
  }

  if (
      form.sharedPayment &&
      form.participantNames.some(
          name => !name.trim(),
      )
  ) {
    return (
        '공동결제 참여자 이름을 ' +
        '모두 입력해 주세요.'
    )
  }

  if (
      form.sharedPayment &&
      form.participantNames.length !==
      form.splitCount - 1
  ) {
    return (
        '공동결제 인원 정보가 ' +
        '올바르지 않습니다.'
    )
  }

  return ''
}

function createRequestData() {
  return {
    countryId:
        Number(form.countryId),

    categoryId:
        Number(form.categoryId),

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
        form.merchantOriginalName.trim(),

    merchantTranslatedName: null,

    // 세금·팁·할인 등이 포함된 실제 결제액
    totalAmount:
        Number(form.totalAmount),

    // 수기입력이므로 OCR 원문 없음
    ocrRawText: null,

    items: form.items.map(
        item => ({
          originalName:
              item.originalName.trim(),

          translatedName:
              item.translatedName.trim() || null,

          quantity:
              Number(item.quantity),

          amount:
              Number(item.amount),
        }),
    ),

    /*
     * 백엔드가 participantNames 개수로
     * splitCount를 계산합니다.
     *
     * 로그인 사용자는 포함하지 않고
     * 나머지 참여자만 전송합니다.
     */
    participants:
        form.sharedPayment
            ? form.participantNames.map(
                name => ({
                  id: null,
                  participantName: name.trim(),
                }),
            )
            : [],
  }
}

async function saveReceipt() {
  if (saving.value) {
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
    const response =
        await createReceipt(
            tripId.value,
            createRequestData(),
            selectedFile.value,
        )

    const savedReceipt =
        response.data?.data

    if (!savedReceipt?.id) {
      throw new Error(
          '영수증 저장 응답에서 ID를 확인할 수 없습니다.',
      )
    }

    window.alert(
        '영수증이 저장되었습니다.',
    )

    await router.replace({
      name: 'Receipt',
      params: {
        tripId: tripId.value,
      },
    })
  } catch (error) {
    console.error(
        '영수증 저장 실패:',
        error,
    )

    window.alert(
        error.response?.data?.message ||
        error.message ||
        '영수증 저장 중 오류가 발생했습니다.',
    )
  } finally {
    saving.value = false
  }
}

onMounted(loadTrip)

onBeforeUnmount(removeImage)
</script>

<template>
  <main class="manual-page">
    <header class="page-header">
      <button
          type="button"
          aria-label="뒤로 가기"
          @click="router.back()"
      >
        ‹
      </button>

      <h1>영수증 수기입력</h1>

      <button
          type="button"
          class="cancel-button"
          @click="router.back()"
      >
        취소
      </button>
    </header>

    <form @submit.prevent="saveReceipt">
      <section class="receipt-form-card">
        <!-- 선택적인 영수증 이미지 첨부 -->
        <div class="image-section">
          <input
              ref="fileInput"
              type="file"
              accept="image/jpeg,image/png"
              hidden
              @change="selectImage"
          >

          <template v-if="previewUrl">
            <img
                :src="previewUrl"
                alt="첨부한 영수증 미리보기"
            >

            <div>
              <b>{{ selectedFile?.name }}</b>

              <small>
                저장할 때 원본 이미지도 함께 등록됩니다.
              </small>
            </div>

            <button
                type="button"
                class="remove-image-button"
                @click="removeImage"
            >
              삭제
            </button>
          </template>

          <template v-else>
            <span class="camera-icon">
              ▣
            </span>

            <div>
              <b>영수증 사진 첨부</b>

              <small>
                선택 사항 · 이미지 없이도 저장할 수 있어요
              </small>
            </div>

            <button
                type="button"
                class="add-image-button"
                @click="openFilePicker"
            >
              추가
            </button>
          </template>
        </div>

        <!-- 현재 여행 -->
        <label class="field">
          <span>여행</span>

          <div class="readonly-trip">
            <template v-if="loadingTrip">
              여행 정보 불러오는 중...
            </template>

            <template v-else-if="loadError">
              {{ loadError }}
            </template>

            <template v-else>
              ✈️ {{ trip?.tripName }}
            </template>
          </div>
        </label>

        <!-- 상호명 -->
        <label class="field">
          <span>상호명</span>

          <input
              v-model.trim="
              form.merchantOriginalName
            "
              type="text"
              maxlength="200"
              placeholder="예: Hotel Zurich Central"
          >
        </label>

        <!-- 국가와 카테고리 -->
        <div class="field-grid">
          <label class="field">
            <span>국가</span>

            <select
                v-model.number="
                form.countryId
              "
            >
              <option :value="null">
                국가 선택
              </option>

              <option
                  v-for="country in trip?.countries || []"
                  :key="country.countryId"
                  :value="country.countryId"
              >
                {{ country.countryName }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>카테고리</span>

            <select
                v-model.number="
                form.categoryId
              "
            >
              <option :value="null">
                카테고리 선택
              </option>

              <option
                  v-for="category in categories"
                  :key="category.id"
                  :value="category.id"
              >
                {{ category.name }}
              </option>
            </select>
          </label>
        </div>

        <!-- 결제 날짜와 시간 -->
        <div class="field-grid">
          <label class="field">
            <span>결제 날짜</span>

            <input
                v-model="form.paymentDate"
                type="date"
            >
          </label>

          <label class="field">
            <span>결제 시간</span>

            <input
                v-model="form.paymentTime"
                type="time"
            >
          </label>
        </div>

        <!-- 통화 코드 -->
        <label class="field">
          <span>통화 코드</span>

          <input
              v-model="form.currencyCode"
              type="text"
              maxlength="3"
              autocomplete="off"
              placeholder="예: EUR"
              @input="normalizeCurrencyCode"
          >
        </label>

        <!-- 결제 품목 -->
        <section class="items-section">
          <div class="items-heading">
            <div>
              <b>결제 품목</b>

              <small>
                품목별 수량과 금액을 입력해 주세요.
              </small>
            </div>

            <strong>
              {{ form.items.length }}개 품목
            </strong>
          </div>

          <article
              v-for="(item, index) in
              form.items"
              :key="index"
              class="item-card"
          >
            <div class="item-card-heading">
              <b>품목 {{ index + 1 }}</b>

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

            <label class="item-name">
              <span>품목명</span>

              <input
                  v-model.trim="
                  item.originalName
                "
                  type="text"
                  maxlength="200"
                  placeholder="예: 다이볼라 피자"
              >
            </label>

            <div class="item-detail-grid">
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
                <span>품목 금액</span>

                <div class="item-amount-input">
                  <b>
                    {{
                      form.currencyCode ||
                      '통화'
                    }}
                  </b>

                  <input
                      v-model="item.amount"
                      type="number"
                      min="0"
                      step="0.01"
                      inputmode="decimal"
                      placeholder="0.00"
                  >
                </div>
              </label>
            </div>
          </article>

          <button
              type="button"
              class="add-item-button"
              @click="addItem"
          >
            ＋ 품목 추가
          </button>
        </section>

        <!-- 최종 결제 금액 -->
        <section class="total-amount-section">
          <div class="total-amount-heading">
            <div>
              <span>최종 결제 금액</span>

              <small>
                세금·팁·할인 등이 반영된 실제 결제 금액을 입력해 주세요.
              </small>
            </div>

            <em>
              품목 합계
              {{ form.currencyCode || '통화' }}
              {{ itemTotalAmount.toFixed(2) }}
            </em>
          </div>

          <div class="editable-total-amount">
            <b>
              {{ form.currencyCode || '통화' }}
            </b>

            <input
                v-model="form.totalAmount"
                type="number"
                min="0.01"
                step="0.01"
                inputmode="decimal"
                placeholder="0.00"
            >
          </div>

          <button
              type="button"
              class="apply-item-total-button"
              @click="applyItemTotal"
          >
            품목 합계를 최종 결제 금액에 적용
          </button>
        </section>
      </section>

      <!-- 공동결제 -->
      <section class="shared-payment-card">
        <div class="shared-heading">
          <div>
            <b>♧ 공동 인원 추가</b>

            <small>
              로그인 사용자를 포함한 전체 결제 인원을 설정합니다.
            </small>
          </div>

          <label class="switch">
            <input
                v-model="form.sharedPayment"
                type="checkbox"
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
                  @click="decreaseSplitCount"
              >
                −
              </button>

              <b>{{ form.splitCount }}</b>

              <button
                  type="button"
                  aria-label="인원 증가"
                  @click="increaseSplitCount"
              >
                +
              </button>
            </div>
          </div>

          <div class="per-person">
            <span>1인당 결제 금액</span>

            <b>
              {{ form.currencyCode || '통화' }}
              {{ perPersonAmount.toFixed(2) }}
            </b>
          </div>

          <div class="participants">
            <b>결제 인원</b>

            <small>
              로그인 사용자를 제외한 참여자 이름
            </small>

            <input
                v-for="(_, index) in
                form.participantNames"
                :key="index"
                v-model.trim="
                form.participantNames[index]
              "
                type="text"
                maxlength="100"
                :placeholder="
                `참여자 ${index + 1} 이름`
              "
            >
          </div>
        </template>
      </section>

      <!-- 메모 -->
      <label class="memo-field">
        <span>메모</span>

        <textarea
            v-model.trim="form.memo"
            maxlength="500"
            placeholder="메모를 입력하세요"
        />
      </label>

      <div class="save-area">
        <button
            type="submit"
            :disabled="
            saving ||
            loadingTrip ||
            !trip ||
            Boolean(loadError)">
          {{ saving ? '저장 중...' : '영수증 저장' }}
        </button>
      </div>
    </form>
  </main>
</template>

<style scoped>
.manual-page {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: 0 auto;
  padding-bottom: 104px;
  background: #f5f7fc;
  color: #111a2d;
}

.page-header {
  display: grid;
  height: 67px;
  grid-template-columns: 48px 1fr 48px;
  align-items: end;
  padding: 0 20px 18px;
}

.page-header > button:first-child {
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

.page-header h1 {
  font-size: 19px;
  font-weight: 900;
  text-align: center;
}

.cancel-button {
  color: #8390a5;
  font-size: 11px;
  text-align: right;
}

form {
  padding: 0 20px;
}

.receipt-form-card {
  padding: 14px;
  border-radius: 20px;
  background: #fff9eb;
}

.image-section {
  display: grid;
  min-height: 76px;
  grid-template-columns:
    42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px dashed #a9c9fc;
  border-radius: 16px;
  background: #fff;
}

.image-section img,
.camera-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 12px;
  background: #eaf2ff;
  color: #2670e8;
  object-fit: cover;
}

.image-section b,
.image-section small {
  display: block;
}

.image-section b {
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-section small {
  margin-top: 5px;
  color: #95a1b3;
  font-size: 8px;
}

.add-image-button,
.remove-image-button {
  color: #2670e8;
  font-size: 11px;
  font-weight: 900;
}

.remove-image-button {
  color: #e24e4e;
}

.field {
  display: block;
  margin-top: 14px;
}

.field > span,
.memo-field > span {
  display: block;
  margin-bottom: 7px;
  color: #536077;
  font-size: 10px;
  font-weight: 800;
}

.field input,
.field select,
.readonly-trip {
  width: 100%;
  height: 48px;
  border: 1px solid #d5deeb;
  border-radius: 12px;
  background: #fff;
  font-size: 12px;
}

.field input,
.field select {
  padding: 0 12px;
}

.readonly-trip {
  display: flex;
  align-items: center;
  padding: 0 13px;
  font-weight: 800;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.items-section {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed #d8cdb5;
}

.items-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.items-heading b,
.items-heading small {
  display: block;
}

.items-heading b {
  font-size: 13px;
}

.items-heading small {
  margin-top: 5px;
  color: #8c98aa;
  font-size: 8px;
}

.items-heading strong {
  color: #2670e8;
  font-size: 10px;
}

.item-card {
  margin-top: 13px;
  padding: 12px;
  border: 1px solid #e1e5eb;
  border-radius: 14px;
  background: #fff;
}

.item-card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-card-heading b {
  color: #65738a;
  font-size: 9px;
}

.item-card-heading button {
  color: #e55353;
  font-size: 9px;
  font-weight: 800;
}

.item-card-heading button:disabled {
  color: #c8ced8;
}

.item-card label {
  display: block;
  margin-top: 10px;
}

.item-card label > span {
  display: block;
  margin-bottom: 5px;
  color: #718096;
  font-size: 8px;
  font-weight: 800;
}

.item-card input {
  width: 100%;
  height: 42px;
  padding: 0 10px;
  border: 1px solid #d5deeb;
  border-radius: 10px;
  background: #fff;
  font-size: 11px;
}

.item-detail-grid {
  display: grid;
  grid-template-columns: 74px 1fr;
  gap: 9px;
}

.item-detail-grid input {
  text-align: right;
}

.item-amount-input {
  display: flex;
  align-items: center;
  border: 1px solid #d5deeb;
  border-radius: 10px;
  background: #fff;
}

.item-amount-input b {
  padding-left: 10px;
  color: #173f8d;
  font-size: 9px;
}

.item-amount-input input {
  border: 0;
}

.add-item-button {
  width: 100%;
  height: 44px;
  margin-top: 13px;
  border: 1px dashed #2670e8;
  border-radius: 11px;
  color: #2670e8;
  font-size: 10px;
  font-weight: 900;
}

.total-amount-section {
  margin-top: 16px;
  padding: 15px 12px;
  border-top: 1px dashed #d8cdb5;
}

.total-amount-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}

.total-amount-heading span,
.total-amount-heading small {
  display: block;
}

.total-amount-heading span {
  color: #536077;
  font-size: 10px;
  font-weight: 900;
}

.total-amount-heading small {
  margin-top: 5px;
  color: #99a3b3;
  font-size: 8px;
}

.total-amount-heading em {
  color: #7c899b;
  font-size: 8px;
  font-style: normal;
  white-space: nowrap;
}

.editable-total-amount {
  display: flex;
  align-items: center;
  margin-top: 10px;
  border: 1px solid #ef9700;
  border-radius: 11px;
  background: #fff;
}

.editable-total-amount b {
  padding-left: 12px;
  color: #e66b00;
  font-size: 12px;
}

.editable-total-amount input {
  width: 100%;
  height: 48px;
  padding: 0 12px;
  border: 0;
  outline: 0;
  background: transparent;
  color: #e66b00;
  font-size: 17px;
  font-weight: 900;
  text-align: right;
}

.apply-item-total-button {
  width: 100%;
  margin-top: 9px;
  padding: 9px;
  border: 1px solid #cbd8ea;
  border-radius: 9px;
  background: #f7faff;
  color: #47709f;
  font-size: 9px;
  font-weight: 800;
}

.shared-payment-card {
  margin-top: 10px;
  padding: 16px;
  border-radius: 18px;
  background: #fff1ad;
}

.shared-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.shared-heading b,
.shared-heading small {
  display: block;
}

.shared-heading b {
  color: #173f8d;
  font-size: 12px;
}

.shared-heading small {
  margin-top: 6px;
  color: #68758b;
  font-size: 8px;
}

.switch input {
  display: none;
}

.switch i {
  position: relative;
  display: block;
  width: 44px;
  height: 24px;
  border-radius: 20px;
  background: #d7dfeb;
  transition: background .2s;
}

.switch i::after {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  content: '';
  transition: transform .2s;
}

.switch input:checked + i {
  background: #2670e8;
}

.switch input:checked + i::after {
  transform: translateX(20px);
}

.people-count,
.per-person {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding: 11px 12px;
  border-radius: 12px;
  background: #fff;
  font-size: 10px;
}

.people-count > div {
  display: flex;
  align-items: center;
  gap: 12px;
}

.people-count button {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: #173f8d;
  color: #fff;
  font-size: 18px;
}

.people-count button:first-child {
  background: #edf1f7;
  color: #718096;
}

.per-person b {
  color: #174691;
  font-size: 13px;
}

.participants {
  margin-top: 14px;
}

.participants > b,
.participants > small {
  display: block;
}

.participants > b {
  font-size: 11px;
}

.participants > small {
  margin-top: 4px;
  color: #77859a;
  font-size: 8px;
}

.participants input {
  width: 100%;
  height: 42px;
  margin-top: 8px;
  padding: 0 12px;
  border-radius: 11px;
  background: #fff;
  font-size: 11px;
}

.memo-field {
  display: block;
  margin-top: 16px;
}

.memo-field textarea {
  width: 100%;
  min-height: 82px;
  padding: 13px;
  resize: vertical;
  border: 1px solid #d5deeb;
  border-radius: 12px;
  background: #fff;
  font-size: 11px;
}

.save-area {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  padding: 14px 20px 24px;
  border-top: 1px solid #e1e6ef;
  background: #fff;
}

.save-area button {
  display: block;
  width: min(calc(100% - 40px), 350px);
  height: 54px;
  margin: 0 auto;
  border-radius: 14px;
  background: #173f8d;
  color: #fff;
  font-size: 14px;
  font-weight: 900;
}

@media (max-width: 350px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  .item-detail-grid {
    grid-template-columns: 64px 1fr;
  }
}
</style>