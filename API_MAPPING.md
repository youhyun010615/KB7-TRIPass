# API_MAPPING — TRIPass 요구사항 · API · 테이블 매핑표

> 이 문서가 실제 개발 진척도의 기준입니다.  
> 개발 완료 시 **상태** 컬럼을 `완료`로 업데이트하고 PR에 명시하세요.

상태: `예정` → `개발중` → `완료`

---

## AUTH — 인증 (담당: 송형진)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| AUTH-001 | 회원가입 | 회원가입 화면 | `POST /api/v1/auth/register` | `users` | 예정 |
| AUTH-002 | 로그인 | 로그인 화면 | `POST /api/v1/auth/login` | `users` | 예정 |
| AUTH-003 | 로그아웃 | - | `POST /api/v1/auth/logout` | - | 예정 |
| AUTH-004 | Access Token 재발급 | - | `POST /api/v1/auth/refresh` | - | 예정 |

---

## MYP — 마이페이지 (담당: 송형진)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| MYP-001 | 내 정보 조회 | 마이페이지 홈 | `GET /api/v1/users/me` | `users` | 예정 |
| MYP-002 | 프로필 수정 | 정보 수정 | `PUT /api/v1/users/me` | `users` | 예정 |
| MYP-003 | 비밀번호 변경 | 비밀번호 변경 | `PUT /api/v1/users/me/password` | `users` | 예정 |
| MYP-004 | 회원 탈퇴 | 탈퇴 확인 | `DELETE /api/v1/users/me` | `users` | 예정 |

---

## PRO — 금융 프로필 (담당: 송형진)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| PRO-001 | 금융 프로필 조회 | 금융 프로필 | `GET /api/v1/profiles/financial` | `financial_profiles` | 예정 |
| PRO-002 | 금융 프로필 등록/수정 | 프로필 설정 | `PUT /api/v1/profiles/financial` | `financial_profiles` | 예정 |

---

## GDS — 금융 상품 (담당: 송형진)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| GDS-001 | 추천 금융상품 목록 조회 | 금융상품 추천 | `GET /api/v1/products` | `financial_products` | 예정 |
| GDS-002 | 금융상품 상세 조회 | 금융상품 상세 | `GET /api/v1/products/{id}` | `financial_products` | 예정 |

---

## AST — 자산관리 (담당: 이아영)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| AST-001 | 보유 자산 목록 조회 | 자산 현황 | `GET /api/v1/assets` | `assets` | 예정 |
| AST-002 | 외부 자산 연동 (CODEF) | 자산 연동 | `POST /api/v1/assets/link` | `assets`, `asset_accounts` | 예정 |
| AST-003 | 자산 상세 조회 | 자산 상세 | `GET /api/v1/assets/{id}` | `assets`, `asset_accounts` | 예정 |
| AST-004 | 자산 연동 해제 | - | `DELETE /api/v1/assets/{id}` | `assets` | 예정 |

---

## SAV — 저축/홈 (담당: 권유현)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| SAV-001 | 홈 대시보드 조회 | 홈 화면 | `GET /api/v1/saving/dashboard` | `saving_goals`, `assets` | 예정 |
| SAV-002 | 저축 목표 생성 | 저축 목표 설정 | `POST /api/v1/saving/goals` | `saving_goals` | 예정 |
| SAV-003 | 저축 목표 목록 조회 | 저축 현황 | `GET /api/v1/saving/goals` | `saving_goals` | 예정 |
| SAV-004 | 저축 목표 수정 | 목표 편집 | `PUT /api/v1/saving/goals/{id}` | `saving_goals` | 예정 |
| SAV-005 | 저축 목표 삭제 | - | `DELETE /api/v1/saving/goals/{id}` | `saving_goals` | 예정 |
| SAV-006 | 저축 입출금 내역 조회 | 저축 히스토리 | `GET /api/v1/saving/history` | `saving_histories` | 예정 |

---

## TRV — 여행모드 (담당: 권원영)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| TRV-001 | 여행 등록 | 여행 계획 등록 | `POST /api/v1/trips` | `trips`, `trip_countries` | 예정 |
| TRV-002 | 여행 목록 조회 | 여행 목록 | `GET /api/v1/trips` | `trips` | 예정 |
| TRV-003 | 진행 중인 여행 조회 | 여행 홈 | `GET /api/v1/trips/current` | `trips` | 예정 |
| TRV-004 | 여행 상세 조회 | 여행 상세 | `GET /api/v1/trips/{id}` | `trips`, `trip_countries` | 예정 |
| TRV-005 | 여행 종료 | - | `PUT /api/v1/trips/{id}/end` | `trips` | 예정 |
| TRV-006 | 여행 삭제 | - | `DELETE /api/v1/trips/{id}` | `trips` | 예정 |

---

## BUD — 예산 (담당: 권원영)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| BUD-001 | 카테고리별 예산 설정 | 예산 설정 | `POST /api/v1/trips/{tripId}/budgets` | `budgets`, `budget_categories` | 예정 |
| BUD-002 | 예산 현황 조회 | 예산 현황 | `GET /api/v1/trips/{tripId}/budgets` | `budgets`, `budget_categories` | 예정 |
| BUD-003 | 예산 수정 | 예산 수정 | `PUT /api/v1/trips/{tripId}/budgets/{id}` | `budgets` | 예정 |

---

## FXC — 환율/환전 (담당: 권원영)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| FXC-001 | 전체 환율 목록 조회 | 환율 조회 | `GET /api/v1/exchange-rates` | `exchange_rates` | 예정 |
| FXC-002 | 특정 통화 환율 조회 | 환율 상세 | `GET /api/v1/exchange-rates/{currency}` | `exchange_rates` | 예정 |
| FXC-003 | 환전 금액 계산 | 환전 계산기 | `POST /api/v1/exchange-rates/calculator` | `exchange_rates` | 예정 |
| FXC-004 | 환전 내역 등록 | 환전 완료 | `POST /api/v1/exchange-rates/logs` | `exchange_logs` | 예정 |
| FXC-005 | 환전 내역 조회 | 환전 내역 목록 | `GET /api/v1/exchange-rates/logs` | `exchange_logs` | 예정 |

---

## PRE — 사전지출 (담당: 권원영)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| PRE-001 | 사전지출 항목 등록 | 사전지출 등록 | `POST /api/v1/prepay` | `prepay_items` | 예정 |
| PRE-002 | 사전지출 목록 조회 | 사전지출 목록 | `GET /api/v1/prepay` | `prepay_items` | 예정 |
| PRE-003 | 사전지출 수정 | 사전지출 수정 | `PUT /api/v1/prepay/{id}` | `prepay_items` | 예정 |
| PRE-004 | 사전지출 삭제 | - | `DELETE /api/v1/prepay/{id}` | `prepay_items` | 예정 |

---

## SCH — 일정 (담당: 홍유진)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| SCH-001 | 일정 등록 | 일정 추가 | `POST /api/v1/schedules` | `schedules` | 예정 |
| SCH-002 | 일정 목록 조회 | 일정 캘린더 | `GET /api/v1/schedules` | `schedules` | 예정 |
| SCH-003 | 일정 상세 조회 | 일정 상세 | `GET /api/v1/schedules/{id}` | `schedules` | 예정 |
| SCH-004 | 일정 수정 | 일정 수정 | `PUT /api/v1/schedules/{id}` | `schedules` | 예정 |
| SCH-005 | 일정 삭제 | - | `DELETE /api/v1/schedules/{id}` | `schedules` | 예정 |

---

## EXP — 지출 (담당: 홍유진)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| EXP-001 | 지출 등록 | 지출 추가 | `POST /api/v1/expenses` | `expenses` | 예정 |
| EXP-002 | 지출 목록 조회 | 지출 내역 | `GET /api/v1/expenses` | `expenses` | 예정 |
| EXP-003 | 지출 상세 조회 | 지출 상세 | `GET /api/v1/expenses/{id}` | `expenses` | 예정 |
| EXP-004 | 지출 수정 | 지출 수정 | `PUT /api/v1/expenses/{id}` | `expenses` | 예정 |
| EXP-005 | 지출 삭제 | - | `DELETE /api/v1/expenses/{id}` | `expenses` | 예정 |
| EXP-006 | 카테고리별 지출 합계 조회 | 지출 통계 | `GET /api/v1/expenses/summary` | `expenses` | 예정 |

---

## OCR — 영수증 OCR (담당: 홍유진)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| OCR-001 | 영수증 이미지 업로드 + OCR 처리 | 영수증 스캔 | `POST /api/v1/ocr/receipts` | `ocr_receipts` | 예정 |
| OCR-002 | OCR 결과 조회 | 영수증 결과 확인 | `GET /api/v1/ocr/receipts/{id}` | `ocr_receipts` | 예정 |
| OCR-003 | OCR 결과로 지출 자동 등록 | 영수증 → 지출 변환 | `POST /api/v1/ocr/receipts/{id}/expense` | `ocr_receipts`, `expenses` | 예정 |

---

## REP — 리포트 (담당: 홍유진)

| 요구사항 ID | 기능 | 화면 | API | 테이블 | 상태 |
|---|---|---|---|---|---|
| REP-001 | 여행 종합 리포트 조회 | 여행 리포트 | `GET /api/v1/reports/{tripId}` | `expenses`, `budgets` | 예정 |
| REP-002 | 카테고리별 지출 통계 | 지출 분석 | `GET /api/v1/reports/{tripId}/categories` | `expenses` | 예정 |
| REP-003 | 일별 지출 추이 | 일별 지출 차트 | `GET /api/v1/reports/{tripId}/daily` | `expenses` | 예정 |
| REP-004 | 저축 달성률 리포트 | 저축 리포트 | `GET /api/v1/reports/saving` | `saving_goals`, `saving_histories` | 예정 |

---

## 전체 진척도 요약

| 도메인 | 담당자 | 전체 API 수 | 완료 | 개발중 | 예정 |
|---|---|---|---|---|---|
| AUTH | 송형진 | 4 | 0 | 0 | 4 |
| MYP | 송형진 | 4 | 0 | 0 | 4 |
| PRO | 송형진 | 2 | 0 | 0 | 2 |
| GDS | 송형진 | 2 | 0 | 0 | 2 |
| AST | 이아영 | 4 | 0 | 0 | 4 |
| SAV | 권유현 | 6 | 0 | 0 | 6 |
| TRV | 권원영 | 6 | 0 | 0 | 6 |
| BUD | 권원영 | 3 | 0 | 0 | 3 |
| FXC | 권원영 | 5 | 0 | 0 | 5 |
| PRE | 권원영 | 4 | 0 | 0 | 4 |
| SCH | 홍유진 | 5 | 0 | 0 | 5 |
| EXP | 홍유진 | 6 | 0 | 0 | 6 |
| OCR | 홍유진 | 3 | 0 | 0 | 3 |
| REP | 홍유진 | 4 | 0 | 0 | 4 |
| **합계** | | **58** | **0** | **0** | **58** |
