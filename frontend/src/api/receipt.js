import api from '@/api'

const OCR_REQUEST_TIMEOUT = 60000

// 영수증 이미지를 OCR로 분석하고 번역한다.
export function analyzeReceipt(file) {
  const formData = new FormData()
  formData.append('file', file)

  return api.post(
    '/ocr/receipts/analyze',
    formData,
    {
      timeout: OCR_REQUEST_TIMEOUT,
    },
  )
}

// 사용자가 확인·수정한 영수증 정보와 선택적인 원본 이미지를 저장한다.
export function createReceipt(tripId, data, file = null) {
  const formData = new FormData()

  formData.append(
    'data',
    new Blob(
      [JSON.stringify(data)],
      { type: 'application/json' },
    ),
  )

  if (file) {
    formData.append('file', file)
  }

  return api.post(
    `/trips/${tripId}/receipts`,
    formData,
    {
      timeout: OCR_REQUEST_TIMEOUT,
    },
  )
}

// 로그인 회원의 특정 여행 영수증 목록을 조회한다.
export function getReceipts(tripId, params = {}) {
  return api.get(`/trips/${tripId}/receipts`, { params })
}

// 여행 영수증의 참여자별 정산 요약을 조회한다.
export function getReceiptSettlements(tripId) {
  return api.get(`/trips/${tripId}/receipts/settlements`)
}

// 특정 공동결제 참여자에게 연결된 영수증과 총 정산 금액을 조회한다.
export function getParticipantSettlement(tripId, participantName) {
  return api.get(
    `/trips/${tripId}/receipts/settlements/${encodeURIComponent(participantName)}`,
  )
}

// 공동결제 참여자의 정산 완료 상태를 변경한다.
export function toggleReceiptSettlement(tripId, participantName, settled) {
  return api.put(
    `/trips/${tripId}/receipts/settlements/${encodeURIComponent(participantName)}/toggle`,
    null,
    { params: { settled } },
  )
}

// 영수증이 실제로 등록된 결제 날짜 목록을 조회한다.
export function getReceiptDates(tripId) {
  return api.get(`/trips/${tripId}/receipts/dates`)
}

// 로그인 회원이 소유한 특정 여행의 영수증 상세 정보를 조회한다.
export function getReceipt(tripId, receiptId) {
  return api.get(
    `/trips/${tripId}/receipts/${receiptId}`,
  )
}

// 영수증 기본 정보, 품목 및 공동결제 참여자를 수정한다.
export function updateReceipt(tripId, receiptId, data) {
  return api.put(
    `/trips/${tripId}/receipts/${receiptId}`,
    data,
  )
}

// 영수증과 하위 데이터를 논리 삭제한다.
export function deleteReceipt(tripId, receiptId) {
  return api.delete(
    `/trips/${tripId}/receipts/${receiptId}`,
  )
}

// 회원과 여행 소유권을 검증한 후 저장된 원본 이미지를 조회한다.
export function getReceiptImage(tripId, receiptId) {
  return api.get(
    `/trips/${tripId}/receipts/${receiptId}/image`,
    {
      responseType: 'blob',
    },
  )
}
