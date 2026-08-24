import api from '@/api'
import {
  demoParticipantSettlement,
  demoReceiptDates,
  demoReceiptDetail,
  demoReceipts,
  demoReceiptSettlements,
  isLay1217Demo,
  toggleDemoReceiptSettlement,
} from '@/mocks/lay1217TravelDemo'

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
      skipLoadingOverlay: true,
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
const demoCountryMapCache = new Map()
const demoResponse = data => Promise.resolve({ data: { data } })

async function getDemoCountryMap(tripId) {
  if (demoCountryMapCache.has(Number(tripId))) return demoCountryMapCache.get(Number(tripId))
  const response = await api.get(`/trips/${tripId}`)
  const trip = response.data?.data ?? response.data ?? {}
  const result = Object.fromEntries((trip.countries || []).map(country => [
    country.countryName,
    Number(country.countryId ?? country.id) || null,
  ]))
  demoCountryMapCache.set(Number(tripId), result)
  return result
}

export async function getReceipts(tripId, params = {}) {
  if (isLay1217Demo()) return demoResponse(demoReceipts(tripId, await getDemoCountryMap(tripId), params))
  return api.get(`/trips/${tripId}/receipts`, { params })
}

// 여행 영수증의 참여자별 정산 요약을 조회한다.
export async function getReceiptSettlements(tripId) {
  if (isLay1217Demo()) return demoResponse(demoReceiptSettlements(tripId, await getDemoCountryMap(tripId)))
  return api.get(`/trips/${tripId}/receipts/settlements`)
}

// 특정 공동결제 참여자에게 연결된 영수증과 총 정산 금액을 조회한다.
export async function getParticipantSettlement(tripId, participantName) {
  if (isLay1217Demo()) return demoResponse(demoParticipantSettlement(tripId, participantName, await getDemoCountryMap(tripId)))
  return api.get(
    `/trips/${tripId}/receipts/settlements/${encodeURIComponent(participantName)}`,
  )
}

// 공동결제 참여자의 정산 완료 상태를 변경한다.
export function toggleReceiptSettlement(tripId, participantName, settled) {
  if (isLay1217Demo()) {
    toggleDemoReceiptSettlement(participantName, settled)
    return demoResponse({ participantName, settled })
  }
  return api.put(
    `/trips/${tripId}/receipts/settlements/${encodeURIComponent(participantName)}/toggle`,
    null,
    { params: { settled } },
  )
}

// 영수증이 실제로 등록된 결제 날짜 목록을 조회한다.
export function getReceiptDates(tripId) {
  if (isLay1217Demo()) return demoResponse(demoReceiptDates())
  return api.get(`/trips/${tripId}/receipts/dates`)
}

// 로그인 회원이 소유한 특정 여행의 영수증 상세 정보를 조회한다.
export async function getReceipt(tripId, receiptId) {
  if (isLay1217Demo()) return demoResponse(demoReceiptDetail(tripId, receiptId, await getDemoCountryMap(tripId)))
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
