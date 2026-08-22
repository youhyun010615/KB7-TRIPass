<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref,
} from 'vue'

import {
  useRoute,
  useRouter,
} from 'vue-router'

import {
  analyzeReceipt,
} from '@/api/receipt'

import {
  fetchTripGoal,
} from '@/api/travel'

import {
  useReceiptStore,
} from '@/stores/receipt'

import receiptIcon from '@/assets/icons/receipt.svg'

const route = useRoute()
const router = useRouter()
const store = useReceiptStore()

const tripId = computed(() => {
  const value = Number(route.params.tripId)

  return Number.isInteger(value) && value > 0
      ? value
      : null
})

const trip = ref(null)

const loadingTrip = ref(false)
const analyzing = ref(false)

const step = ref('upload')

const selectedFile = ref(null)
const fileName = ref('')
const previewUrl = ref('')

const progress = ref(0)
const errorMessage = ref('')

const fileInput = ref(null)
const cameraInput = ref(null)

let progressTimer = null

function openFilePicker() {
  if (loadingTrip.value || analyzing.value) {
    return
  }

  fileInput.value?.click()
}

function openCamera() {
  if (loadingTrip.value || analyzing.value) {
    return
  }

  cameraInput.value?.click()
}

function revokePreviewUrl() {
  if (!previewUrl.value) {
    return
  }

  URL.revokeObjectURL(
      previewUrl.value,
  )

  previewUrl.value = ''
}

function selectFile(event) {
  const file =
      event.target.files?.[0]

  if (!file) {
    return
  }

  errorMessage.value = ''

  const allowedTypes = [
    'image/jpeg',
    'image/png',
  ]

  if (!allowedTypes.includes(file.type)) {
    errorMessage.value =
        'JPG, JPEG, PNG 이미지 파일만 등록할 수 있습니다.'

    event.target.value = ''
    return
  }

  const maxFileSize =
      10 * 1024 * 1024

  if (file.size > maxFileSize) {
    errorMessage.value =
        '영수증 이미지는 최대 10MB까지 등록할 수 있습니다.'

    event.target.value = ''
    return
  }

  revokePreviewUrl()

  selectedFile.value = file
  fileName.value = file.name

  previewUrl.value =
      URL.createObjectURL(file)

  step.value = 'preview'
}

function resetFile() {
  selectedFile.value = null
  fileName.value = ''

  errorMessage.value = ''
  progress.value = 0

  revokePreviewUrl()

  if (fileInput.value) {
    fileInput.value.value = ''
  }

  if (cameraInput.value) {
    cameraInput.value.value = ''
  }

  step.value = 'upload'
}

function startProgress() {
  stopProgress()

  progress.value = 10

  progressTimer =
      window.setInterval(() => {
        if (progress.value < 90) {
          progress.value += 5
        }
      }, 300)
}

function stopProgress() {
  if (!progressTimer) {
    return
  }

  window.clearInterval(
      progressTimer,
  )

  progressTimer = null
}

async function loadTrip() {
  if (!tripId.value) {
    trip.value = null

    errorMessage.value =
        '여행 정보를 확인해 주세요.'

    return
  }

  loadingTrip.value = true
  errorMessage.value = ''

  try {
    trip.value =
        await fetchTripGoal(
            tripId.value,
        )
  } catch (error) {
    trip.value = null

    errorMessage.value =
        error.response?.data?.message ||
        error.message ||
        '여행 정보를 불러오지 못했습니다.'
  } finally {
    loadingTrip.value = false
  }
}

async function usePhoto() {
  if (analyzing.value) {
    return
  }

  if (!tripId.value) {
    errorMessage.value =
        '여행 정보를 확인해 주세요.'

    return
  }

  if (!trip.value) {
    errorMessage.value =
        '저장할 여행 정보를 먼저 확인해 주세요.'

    return
  }

  if (!selectedFile.value) {
    errorMessage.value =
        '분석할 영수증 이미지를 선택해 주세요.'

    return
  }

  analyzing.value = true
  step.value = 'analyzing'

  errorMessage.value = ''

  startProgress()

  try {
    const response =
        await analyzeReceipt(
            selectedFile.value,
        )

    const analysisData =
        response.data?.data

    if (!analysisData) {
      throw new Error(
          '영수증 분석 응답이 올바르지 않습니다.',
      )
    }

    progress.value = 100

    /*
     * OCR 분석 데이터와 원본 File 객체를
     * 결과 화면에서 사용할 수 있도록 Pinia에 보관합니다.
     *
     * 이 단계에서는 이미지가 서버 저장소에
     * 영구 저장되지 않습니다.
     */
    store.$patch({
      draft: {
        ...analysisData,

        tripId:
        tripId.value,

        sourceFile:
        selectedFile.value,
      },
    })

    await router.push({
      name: 'ReceiptOcrResult',

      params: {
        tripId:
        tripId.value,
      },
    })
  } catch (error) {
    errorMessage.value =
        error.response?.data?.message ||
        error.message ||
        '영수증 분석에 실패했습니다.'

    step.value = 'preview'
  } finally {
    stopProgress()
    analyzing.value = false
  }
}

function handleBack() {
  if (step.value === 'analyzing') {
    return
  }

  if (step.value === 'preview') {
    resetFile()
    return
  }

  router.back()
}

onMounted(loadTrip)

onBeforeUnmount(() => {
  stopProgress()
  revokePreviewUrl()
})
</script>

<template>
  <main
      :class="[
        'capture-page',
        {
          camera:
              step === 'preview' ||
              step === 'analyzing',
        },
      ]"
  >
    <header>
      <button
          type="button"
          aria-label="뒤로 가기"
          :disabled="analyzing"
          @click="handleBack"
      >
        ‹
      </button>

      <h1>
        {{
          step === 'preview'
              ? '촬영 결과 확인'
              : step === 'analyzing'
                  ? '영수증 분석'
                  : '해외 영수증 등록'
        }}
      </h1>

      <span />
    </header>

    <template v-if="step === 'upload'">
      <section
          v-if="
      !loadingTrip &&
      !trip &&
      errorMessage
    "
          class="trip-load-error"
      >
        <p>{{ errorMessage }}</p>

        <button
            type="button"
            @click="loadTrip"
        >
          다시 시도
        </button>
      </section>

      <section class="upload-card">
        <div
            class="receipt-animation"
            aria-hidden="true"
        >
          <span class="receipt-orbit" />
          <span class="receipt-spark spark-one">✦</span>
          <span class="receipt-spark spark-two">✦</span>

          <div class="receipt-icon">
            <img
                :src="receiptIcon"
                alt=""
            >

            <i />
          </div>
        </div>

        <h2>
          영수증을 촬영하거나 업로드해 주세요
        </h2>

        <p>
          해외 결제 영수증의 품목과 금액을
          <br>
          자동으로 인식하고 번역해 드려요.
        </p>

        <button
            type="button"
            class="file-select-button"
            :disabled="
              loadingTrip ||
              !trip
            "
            @click="openFilePicker"
        >
          파일 업로드
        </button>

        <input
            ref="fileInput"
            class="hidden-file-input"
            type="file"
            accept="image/jpeg,image/png"
            @change="selectFile"
        >

        <input
            ref="cameraInput"
            class="hidden-file-input"
            type="file"
            accept="image/jpeg,image/png"
            capture="environment"
            @change="selectFile"
        >

        <small>
          JPG, JPEG, PNG · 최대 10MB
        </small>

        <p
            v-if="errorMessage"
            class="error-message"
        >
          {{ errorMessage }}
        </p>
      </section>

      <button
          type="button"
          class="camera-button"
          :disabled="
            loadingTrip ||
            !trip
          "
          @click="openCamera"
      >
        📷 촬영하기
      </button>
    </template>

    <template v-else-if="step === 'preview'">
      <section class="preview">
        <img
            v-if="previewUrl"
            :src="previewUrl"
            :alt="
              fileName ||
              '선택한 영수증 이미지'
            "
            class="receipt-preview-image"
        >
      </section>

      <p
          v-if="fileName"
          class="selected-file-name"
      >
        {{ fileName }}
      </p>

      <p
          v-if="errorMessage"
          class="error-message preview-error"
      >
        {{ errorMessage }}
      </p>

      <div class="preview-actions">
        <button
            type="button"
            :disabled="analyzing"
            @click="resetFile"
        >
          다시 선택
        </button>

        <button
            type="button"
            :disabled="analyzing"
            @click="usePhoto"
        >
          이 사진 사용
        </button>
      </div>
    </template>

    <template v-else-if="step === 'analyzing'">
      <section class="analysis">
        <div class="analysis-preview">
          <img
              v-if="previewUrl"
              :src="previewUrl"
              :alt="
                fileName ||
                '분석 중인 영수증 이미지'
              "
          >

          <span
              class="scan-line"
              :style="{
                top: `${progress}%`,
              }"
          />
        </div>

        <h2>
          영수증을 읽고 있어요
        </h2>

        <p>
          상호명, 품목과 결제 금액을
          인식하고 번역하는 중이에요.
        </p>

        <div class="progress">
          <i
              :style="{
                width: `${progress}%`,
              }"
          />
        </div>

        <b>{{ progress }}%</b>
      </section>
    </template>
  </main>
</template>

<style scoped>
.capture-page {
  min-height: 100vh;
  padding: 0 18px 40px;
  background: #f3f6fc;
  color: #111a2d
}

.capture-page > header {
  display: grid;
  height: 59px;
  grid-template-columns:36px 1fr 36px;
  align-items: end;
  padding-bottom: 16px
}

.capture-page > header button {
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

.capture-page > header h1 {
  text-align: center;
  font-size: 18px;
  font-weight: 900
}

.capture-page > header span {
  text-align: right;
  color: #f8b400;
  font-size: 10px
}

.upload-card {
  display: flex;
  min-height: 330px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-top: 8px;
  padding: 28px 20px;
  border: 1px solid #cfddf4;
  border-radius: 24px;
  background: linear-gradient(150deg, #fff 0%, #f2f7ff 100%);
  box-shadow: 0 16px 36px rgba(29, 67, 132, .09);
  text-align: center
}

.receipt-animation {
  position: relative;
  display: grid;
  width: 112px;
  height: 112px;
  place-items: center;
}

.receipt-orbit {
  position: absolute;
  inset: 5px;
  border: 1.5px dashed #8db2ed;
  border-radius: 50%;
  animation: receipt-orbit 8s linear infinite;
}

.receipt-icon {
  position: relative;
  display: grid;
  width: 70px;
  height: 70px;
  overflow: hidden;
  place-items: center;
  border: 1px solid rgba(38, 98, 234, .12);
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 12px 25px rgba(38, 98, 234, .15);
  animation: receipt-float 2.8s ease-in-out infinite;
}

.receipt-icon img {
  width: 34px;
  height: 34px;
  object-fit: contain;
}

.receipt-icon i {
  position: absolute;
  right: 13px;
  left: 13px;
  height: 2px;
  border-radius: 2px;
  background: #ffd25d;
  box-shadow: 0 0 8px rgba(255, 194, 49, .65);
  animation: receipt-scan 2.2s ease-in-out infinite;
}

.receipt-spark {
  position: absolute;
  z-index: 2;
  color: #ffd25d;
  font-size: 14px;
  animation: receipt-spark 1.8s ease-in-out infinite;
}

.spark-one { top: 11px; right: 5px; }
.spark-two {
  bottom: 10px;
  left: 7px;
  color: #4e83e7;
  animation-delay: .7s;
}

.upload-card h2 {
  margin-top: 19px;
  font-size: 15px
}

.upload-card p {
  margin-top: 9px;
  color: #7d8a9c;
  font-size: 10px;
  line-height: 1.55
}

.upload-card label {
  margin-top: 24px
}

.upload-card input {
  display: none
}

.upload-card label span {
  display: block;
  padding: 12px 24px;
  border: 1px dashed #2e73df;
  border-radius: 12px;
  background: #f4f8ff;
  color: #2368d5;
  font-size: 11px;
  font-weight: 900
}

.upload-card > small {
  margin-top: 10px;
  color: #a0aaba;
  font-size: 8px
}

.camera-button {
  width: 100%;
  height: 54px;
  margin-top: 14px;
  border-radius: 14px;
  background: #173f8d;
  color: #fff;
  font-size: 13px;
  font-weight: 900
}

.camera {
  background: #08172e;
  color: #fff
}

.camera-frame {
  position: relative;
  display: grid;
  height: 510px;
  place-items: center;
  border-radius: 20px;
  background: #0e223e
}

.corner {
  position: absolute;
  width: 34px;
  height: 34px;
  border-color: #27d9ef
}

.tl {
  top: 20px;
  left: 20px;
  border-top: 3px solid;
  border-left: 3px solid
}

.tr {
  top: 20px;
  right: 20px;
  border-top: 3px solid;
  border-right: 3px solid
}

.bl {
  bottom: 20px;
  left: 20px;
  border-bottom: 3px solid;
  border-left: 3px solid
}

.br {
  right: 20px;
  bottom: 20px;
  border-right: 3px solid;
  border-bottom: 3px solid
}

.mock-receipt, .paper {
  display: flex;
  width: 220px;
  min-height: 350px;
  flex-direction: column;
  padding: 28px 22px;
  background: #fff7e7;
  color: #172033;
  text-align: center
}

.mock-receipt b, .paper b {
  font-size: 10px
}

.mock-receipt i, .paper i {
  height: 8px;
  margin-top: 13px;
  border-bottom: 1px solid #c7c2b5
}

.mock-receipt strong, .paper strong {
  margin-top: auto;
  font-size: 10px
}

.guide {
  text-align: center;
  margin: 17px 0;
  color: #b7c5da;
  font-size: 10px
}

.shutter-row {
  display: grid;
  grid-template-columns:1fr 1fr 1fr;
  align-items: center;
  text-align: center
}

.shutter-row button {
  color: #fff
}

.shutter-row .shutter {
  width: 68px;
  height: 68px;
  justify-self: center;
  border: 5px solid #fff;
  border-radius: 50%;
  background: #1972ed;
  box-shadow: inset 0 0 0 4px #1972ed
}

.preview {
  display: grid;
  height: 610px;
  place-items: center;
  border-radius: 18px;
  background: #0f213c
}

.preview-actions {
  display: grid;
  grid-template-columns:1fr 1.4fr;
  gap: 10px;
  margin-top: 14px
}

.preview-actions button {
  height: 54px;
  border: 1px solid #53657e;
  border-radius: 13px;
  color: #fff;
  font-size: 12px;
  font-weight: 900
}

.preview-actions button:last-child {
  border-color: #1475ed;
  background: #1475ed
}

.analysis {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 20px
}

.analysis .paper {
  position: relative;
  overflow: hidden
}

.scan-line {
  position: absolute;
  right: 0;
  left: 0;
  height: 3px;
  background: #21c8ff;
  box-shadow: 0 0 16px #21c8ff;
  transition: top .25s
}

.analysis h2 {
  margin-top: 28px;
  font-size: 17px
}

.analysis p {
  margin-top: 7px;
  color: #78879b;
  font-size: 10px
}

.progress {
  width: 250px;
  height: 6px;
  margin-top: 25px;
  overflow: hidden;
  border-radius: 3px;
  background: #dce5ef
}

.progress i {
  display: block;
  height: 100%;
  background: #2673e8
}

.analysis > b {
  margin-top: 9px;
  color: #2673e8;
  font-size: 11px
}

.receipt-preview-image {
  display: block;
  width: 100%;
  height: 100%;
  max-height: 610px;
  object-fit: contain;
  border-radius: 18px;
}

.error-message {
  margin-top: 12px;
  color: #e5484d;
  font-size: 11px;
  line-height: 1.5;
  text-align: center;
}

.preview-error {
  margin: 12px 0 0;
}
.hidden-file-input {
  display: none;
}

.file-select-button {
  margin-top: 24px;
  padding: 12px 24px;
  border: 0;
  border-radius: 12px;
  background: #2662ea;
  color: #fff;
  font-size: 11px;
  font-weight: 900;
}

@keyframes receipt-float {
  0%, 100% { transform: translateY(2px) rotate(-1deg); }
  50% { transform: translateY(-6px) rotate(1deg); }
}

@keyframes receipt-orbit {
  to { transform: rotate(360deg); }
}

@keyframes receipt-scan {
  0%, 100% { top: 18px; opacity: .45; }
  50% { top: 50px; opacity: 1; }
}

@keyframes receipt-spark {
  0%, 100% { opacity: .35; transform: scale(.75) rotate(0deg); }
  50% { opacity: 1; transform: scale(1.15) rotate(90deg); }
}

@media (prefers-reduced-motion: reduce) {
  .receipt-orbit,
  .receipt-icon,
  .receipt-icon i,
  .receipt-spark { animation: none; }
}

.file-select-button:disabled,
.camera-button:disabled,
.preview-actions button:disabled,
.capture-page > header button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.selected-file-name {
  overflow: hidden;
  margin-top: 10px;
  color: #b7c5da;
  font-size: 10px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.analysis-preview {
  position: relative;
  width: 240px;
  height: 380px;
  overflow: hidden;
  border-radius: 14px;
  background: #0f213c;
}

.analysis-preview img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.analysis-preview .scan-line {
  position: absolute;
  right: 0;
  left: 0;
  height: 3px;
  background: #21c8ff;
  box-shadow: 0 0 16px #21c8ff;
  transition: top 0.25s;
}

.trip-load-error {
  margin-top: 12px;
  padding: 16px;
  border: 1px solid #f3b8ba;
  border-radius: 12px;
  background: #fff7f7;
  text-align: center;
}

.trip-load-error p {
  margin: 0;
  color: #e5484d;
  font-size: 12px;
  line-height: 1.5;
}

.trip-load-error button {
  margin-top: 12px;
  padding: 8px 18px;
  border: 1px solid #2458d3;
  border-radius: 8px;
  background: #fff;
  color: #2458d3;
  font-weight: 700;
  cursor: pointer;
}
</style>
