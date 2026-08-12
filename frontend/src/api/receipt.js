import api from '@/api'

const OCR_REQUEST_TIMEOUT = 60000

// 해외 영수증 이미지를 OCR로 분석하고 번역한다.
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

// 사용자가 확인·수정한 영수증 정보와 원본 이미지를 저장한다.
export function createReceipt(data, file) {
  const formData = new FormData()

  formData.append(
    'data',
    new Blob(
      [JSON.stringify(data)],
      { type: 'application/json' },
    ),
  )
  formData.append('file', file)

  return api.post(
    '/ocr/receipts',
    formData,
    {
      timeout: OCR_REQUEST_TIMEOUT,
    },
  )
}

// 로그인 회원이 저장한 영수증 목록을 조회한다.
export function getReceipts() {
  return api.get('/ocr/receipts')
}

// 로그인 회원 소유의 영수증 상세 정보를 조회한다.
export function getReceipt(receiptId) {
  return api.get(`/ocr/receipts/${receiptId}`)
}

// 영수증 기본 정보와 품목 목록을 수정한다.
export function updateReceipt(receiptId, data) {
  return api.put(
    `/ocr/receipts/${receiptId}`,
    data,
  )
}

// 영수증과 품목을 논리 삭제한다.
export function deleteReceipt(receiptId) {
  return api.delete(`/ocr/receipts/${receiptId}`)
}

// 회원 소유권 검증을 거쳐 저장된 원본 이미지를 조회한다.
export function getReceiptImage(receiptId) {
  return api.get(
    `/ocr/receipts/${receiptId}/image`,
    {
      responseType: 'blob',
    },
  )
}
