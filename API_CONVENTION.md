# API_CONVENTION — TRIPass API 설계 규칙

> 모든 API는 이 문서의 규칙을 따릅니다. 설계 전 반드시 숙지하세요.

---

## 1. Base URL

```
/api/v1
```

- 모든 REST API는 `/api/v1` 프리픽스를 사용합니다.
- 버전 업 시 팀 전체 합의 후 `/api/v2` 로 전환합니다.

---

## 2. URL 명명 규칙

### 기본 규칙

| 규칙 | 올바른 예 | 잘못된 예 |
|---|---|---|
| 소문자 + 하이픈(`-`) 사용 | `/api/v1/saving-goals` | `/api/v1/savingGoals` |
| 명사 사용 (동사 금지) | `/api/v1/expenses` | `/api/v1/getExpenses` |
| 복수형 사용 | `/api/v1/trips` | `/api/v1/trip` |
| 계층 관계는 슬래시(`/`)로 표현 | `/api/v1/trips/{id}/budgets` | `/api/v1/tripBudgets` |

### 도메인별 URL 프리픽스

| 도메인 | 프리픽스 |
|---|---|
| 인증 | `/api/v1/auth` |
| 사용자 | `/api/v1/users` |
| 금융 프로필 | `/api/v1/profiles` |
| 금융 상품 | `/api/v1/products` |
| 자산 | `/api/v1/assets` |
| 저축 | `/api/v1/saving` |
| 여행 | `/api/v1/trips` |
| 환율/환전 | `/api/v1/exchange-rates` |
| 사전지출 | `/api/v1/prepay` |
| 일정 | `/api/v1/schedules` |
| 지출 | `/api/v1/expenses` |
| OCR | `/api/v1/ocr` |
| 리포트 | `/api/v1/reports` |

---

## 3. Path · Query · Body 사용 기준

### Path Variable — 리소스 식별자

```
GET /api/v1/trips/{tripId}
GET /api/v1/trips/{tripId}/budgets/{budgetId}
```

- 특정 리소스 하나를 식별할 때 사용
- 변수명은 `camelCase` + `Id` 접미사

### Query Parameter — 필터/정렬/페이징

```
GET /api/v1/expenses?tripId=1&category=FOOD&page=0&size=20&sort=date,desc
GET /api/v1/exchange-rates?base=KRW&target=USD
```

- 필터링, 정렬, 검색, 페이징에 사용
- 선택적 파라미터는 모두 Query로 처리

### Request Body — 생성/수정 데이터

```
POST /api/v1/trips
Content-Type: application/json

{
  "title": "일본 여행",
  "startDate": "2025-08-01",
  "endDate": "2025-08-10",
  "totalBudget": 1000000
}
```

- POST, PUT 요청의 데이터는 JSON Body로 전달
- GET, DELETE 요청에는 Body 사용 금지

---

## 4. HTTP 상태 코드

| 상황 | 코드 |
|---|---|
| 조회 성공 | `200 OK` |
| 생성 성공 | `201 Created` |
| 수정/삭제 성공 (응답 없음) | `204 No Content` |
| 잘못된 요청 (유효성 오류) | `400 Bad Request` |
| 인증 실패 / 토큰 없음 | `401 Unauthorized` |
| 권한 없음 (다른 사용자 리소스) | `403 Forbidden` |
| 리소스 없음 | `404 Not Found` |
| 서버 내부 오류 | `500 Internal Server Error` |

---

## 5. 공통 응답 구조

모든 API 응답은 `ApiResponse<T>` 래퍼를 사용합니다.

### 성공 응답

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": { ... }
}
```

### 에러 응답

```json
{
  "code": "TRIP_NOT_FOUND",
  "message": "해당 여행을 찾을 수 없습니다.",
  "data": null
}
```

### Java 사용 예시 (Controller)

```java
// 성공
return ResponseEntity.ok(ApiResponse.success(tripDto));
return ResponseEntity.status(201).body(ApiResponse.success("여행이 등록되었습니다.", tripDto));

// 실패 (GlobalExceptionHandler 에서 처리)
throw new CustomException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "해당 여행을 찾을 수 없습니다.");
```

---

## 6. 페이징 규칙

### 요청 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `page` | int | `0` | 0부터 시작하는 페이지 번호 |
| `size` | int | `20` | 페이지당 아이템 수 |
| `sort` | string | `createdAt,desc` | `필드명,방향` 형식 |

### 응답 구조

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 57,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

---

## 7. 날짜 · 금액 · 통화 형식

### 날짜 / 시간

| 형식 | 예시 | 사용처 |
|---|---|---|
| `yyyy-MM-dd` | `2025-08-01` | 날짜만 (여행 날짜, 지출 날짜) |
| `yyyy-MM-dd'T'HH:mm:ss` | `2025-08-01T14:30:00` | 날짜+시간 (생성/수정 시각) |

- 시간대: **Asia/Seoul (KST)** 기준 저장 및 반환
- DB 저장: `DATETIME` 타입, 시간대 변환은 애플리케이션 레이어에서 처리

### 금액

- API 요청/응답에서 금액은 **정수 (Integer/Long)** 로 전달 (소수점 없음)
- 소수점이 필요한 경우 (환율 등) `BigDecimal` 사용, 소수점 4자리까지
- 단위는 필드명 또는 별도 `currency` 필드로 명시

```json
{
  "amount": 50000,
  "currency": "KRW"
}
```

### 통화 코드

- ISO 4217 기준 3자리 코드 사용: `KRW`, `USD`, `JPY`, `EUR`, `CNY` 등

---

## 8. 예외 코드 명명 규칙

형식: `<도메인>_<설명>_<상태>`

```
TRIP_NOT_FOUND
USER_NOT_FOUND
AUTH_INVALID_TOKEN
AUTH_TOKEN_EXPIRED
EXPENSE_BUDGET_EXCEEDED
EXCHANGE_RATE_UNAVAILABLE
```

| 규칙 | 상세 |
|---|---|
| 전부 대문자 + 언더바 | `TRIP_NOT_FOUND` (O), `tripNotFound` (X) |
| 도메인 접두사 | 어느 도메인의 오류인지 명확히 |
| 마지막 키워드 | `NOT_FOUND`, `INVALID`, `EXPIRED`, `DUPLICATED`, `EXCEEDED` 등 |

---

## 9. 인증 헤더

```
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

- 인증이 필요한 모든 API 요청에 포함
- 토큰 만료 시 `401 Unauthorized` 반환 → 클라이언트에서 `/api/v1/auth/refresh` 호출
- 인증 불필요 엔드포인트: `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/exchange-rates`

---

## 10. 파일 업로드 방식

### 단일 파일 업로드 (영수증 이미지 등)

```
POST /api/v1/ocr/receipts
Content-Type: multipart/form-data

file: <binary>
```

### 응답

```json
{
  "code": "SUCCESS",
  "message": "영수증 OCR 처리가 완료되었습니다.",
  "data": {
    "receiptId": 42,
    "storeName": "편의점",
    "totalAmount": 3500,
    "currency": "KRW",
    "items": [ ... ]
  }
}
```

### 제약 조건

| 항목 | 제한 |
|---|---|
| 허용 확장자 | `jpg`, `jpeg`, `png` |
| 최대 파일 크기 | `10MB` |
| 저장 경로 | `upload.path` 환경변수로 설정 |
