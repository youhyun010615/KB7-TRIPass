# DOMAIN_BOUNDARIES — 도메인 담당 경계 정의

> 이 문서는 각 팀원이 수정 가능한 패키지와 테이블, 그리고 공통 코드 사용 규칙을 정의합니다.  
> **담당 도메인 외의 코드를 임의 수정하면 충돌과 Reject의 원인이 됩니다.**

---

## 담당 도메인 요약

| 팀원 | 도메인 코드 | Java 패키지 | 테이블 |
|---|---|---|---|
| 송형진 | AUTH, MYP, PRO, GDS | `auth`, `mypage`, `profile`, `financial` | `users`, `financial_profiles`, `financial_products` |
| 이아영 | AST | `asset` | `assets`, `asset_accounts` |
| 권유현 | SAV | `saving` | `saving_goals`, `saving_histories` |
| 권원영 | TRV, BUD, FXC, PRE | `travel`, `exchange`, `prepay` | `trips`, `trip_countries`, `budgets`, `budget_categories`, `exchange_rates`, `exchange_logs`, `prepay_items` |
| 홍유진 | SCH, EXP, OCR, REP | `schedule`, `expense`, `ocr`, `report` | `schedules`, `expenses`, `expense_categories`, `ocr_receipts` |

---

## 팀원별 담당 경계 상세

---

### 송형진 — AUTH · MYP · PRO · GDS

**담당 패키지 (읽기/쓰기 모두 가능)**

```
backend/src/main/java/com/tripass/
├── auth/
│   ├── controller/   AuthController.java
│   ├── service/      AuthService.java
│   ├── mapper/       AuthMapper.java
│   └── dto/          LoginRequest.java, LoginResponse.java, RegisterRequest.java ...
├── mypage/
│   ├── controller/   MyPageController.java
│   ├── service/      MyPageService.java
│   ├── mapper/       UserMapper.java
│   └── dto/          UserDto.java, UpdateProfileRequest.java ...
├── profile/
│   ├── controller/   FinancialProfileController.java
│   ├── service/      FinancialProfileService.java
│   ├── mapper/       FinancialProfileMapper.java
│   └── dto/          FinancialProfileDto.java ...
└── financial/
    ├── controller/   FinancialProductController.java
    ├── service/      FinancialProductService.java
    ├── mapper/       FinancialProductMapper.java
    └── dto/          FinancialProductDto.java ...
```

**담당 Mapper XML**

```
backend/src/main/resources/mapper/auth/
backend/src/main/resources/mapper/mypage/
backend/src/main/resources/mapper/profile/
backend/src/main/resources/mapper/financial/
```

**담당 테이블**

```sql
users                 -- 사용자 기본 정보, 인증 정보
financial_profiles    -- 금융 성향, 목표 국가, 여행 횟수 등
financial_products    -- 추천 금융상품 목록
```

**추가 역할**

- `SecurityConfig.java` 의 JWT 필터 구현 담당
- 다른 팀원이 사용하는 `users.id` (FK) 참조는 허용, 테이블 구조 변경은 사전 공유 필수

---

### 이아영 — AST

**담당 패키지**

```
backend/src/main/java/com/tripass/
└── asset/
    ├── controller/   AssetController.java
    ├── service/      AssetService.java
    ├── mapper/       AssetMapper.java
    └── dto/          AssetDto.java, AssetAccountDto.java ...
```

**담당 Mapper XML**

```
backend/src/main/resources/mapper/asset/
```

**담당 테이블**

```sql
assets          -- 연동된 자산 (계좌, 카드, 증권 등)
asset_accounts  -- 자산별 세부 계좌 정보
```

**외부 의존성**

- CODEF API (자산 연동): `CODEF_CLIENT_ID`, `CODEF_CLIENT_SECRET` 환경변수 사용
- 외부 API 호출은 Service 레이어에서 처리, Controller로 노출하지 않음

---

### 권유현 — SAV

**담당 패키지**

```
backend/src/main/java/com/tripass/
└── saving/
    ├── controller/   SavingController.java
    ├── service/      SavingService.java
    ├── mapper/       SavingMapper.java
    └── dto/          SavingGoalDto.java, SavingDashboardDto.java ...
```

**담당 Mapper XML**

```
backend/src/main/resources/mapper/saving/
```

**담당 테이블**

```sql
saving_goals      -- 여행 목표 금액, 현재 저축 금액, 달성률
saving_histories  -- 저축 입출금 내역
```

**크로스 도메인 참조 (읽기 전용)**

- `assets` 테이블: 홈 대시보드에서 총 자산 조회 시 참조 (AssetMapper 호출 또는 JOIN)
  - 변경이 필요한 경우 이아영에게 요청

---

### 권원영 — TRV · BUD · FXC · PRE

**담당 패키지**

```
backend/src/main/java/com/tripass/
├── travel/
│   ├── controller/   TravelController.java
│   ├── service/      TravelService.java
│   ├── mapper/       TravelMapper.java
│   └── dto/          TripDto.java, TripCreateRequest.java ...
├── exchange/
│   ├── controller/   ExchangeController.java
│   ├── service/      ExchangeService.java
│   ├── mapper/       ExchangeMapper.java
│   └── dto/          ExchangeRateDto.java, ExchangeLogDto.java ...
└── prepay/
    ├── controller/   PrepayController.java
    ├── service/      PrepayService.java
    ├── mapper/       PrepayMapper.java
    └── dto/          PrepayItemDto.java ...
```

**담당 Mapper XML**

```
backend/src/main/resources/mapper/travel/
backend/src/main/resources/mapper/exchange/
backend/src/main/resources/mapper/prepay/
```

**담당 테이블**

```sql
trips              -- 여행 기본 정보 (제목, 날짜, 총 예산)
trip_countries     -- 여행 국가 목록 (여행 1개 = 국가 복수)
budgets            -- 카테고리별 예산 배분
budget_categories  -- 예산 카테고리 (FOOD, TRANSPORT, ACCOMMODATION 등)
exchange_rates     -- 실시간 환율 데이터 (외부 API → 캐싱)
exchange_logs      -- 사용자 환전 내역
prepay_items       -- 사전지출 항목 (여행 전 계획 지출)
```

**외부 의존성**

- 환율 API: `EXCHANGE_API_KEY` 환경변수 사용
- `trips.id` 는 홍유진(SCH, EXP)이 FK로 참조 — 테이블 구조 변경 시 사전 공유 필수

---

### 홍유진 — SCH · EXP · OCR · REP

**담당 패키지**

```
backend/src/main/java/com/tripass/
├── schedule/
│   ├── controller/   ScheduleController.java
│   ├── service/      ScheduleService.java
│   ├── mapper/       ScheduleMapper.java
│   └── dto/          ScheduleDto.java ...
├── expense/
│   ├── controller/   ExpenseController.java
│   ├── service/      ExpenseService.java
│   ├── mapper/       ExpenseMapper.java
│   └── dto/          ExpenseDto.java, ExpenseSummaryDto.java ...
├── ocr/
│   ├── controller/   OcrController.java
│   ├── service/      OcrService.java
│   ├── mapper/       OcrMapper.java
│   └── dto/          OcrReceiptDto.java ...
└── report/
    ├── controller/   ReportController.java
    ├── service/      ReportService.java
    ├── mapper/       ReportMapper.java
    └── dto/          TripReportDto.java, CategorySummaryDto.java ...
```

**담당 Mapper XML**

```
backend/src/main/resources/mapper/schedule/
backend/src/main/resources/mapper/expense/
backend/src/main/resources/mapper/ocr/
backend/src/main/resources/mapper/report/
```

**담당 테이블**

```sql
schedules           -- 여행 일정 (날짜, 장소, 메모)
expenses            -- 지출 내역 (금액, 카테고리, 날짜)
expense_categories  -- 지출 카테고리 코드
ocr_receipts        -- OCR 처리된 영수증 원본 및 파싱 결과
```

**외부 의존성**

- OCR API: `OCR_API_KEY` 환경변수 사용
- `trips` 테이블 FK 참조 (읽기 전용): 여행별 지출/일정 조회

---

## 공통 코드 (common 패키지) 사용 규칙

```
backend/src/main/java/com/tripass/common/
├── config/     WebAppInitializer, RootConfig, WebMvcConfig, SecurityConfig
├── response/   ApiResponse<T>
├── exception/  CustomException, GlobalExceptionHandler
└── util/       (공통 유틸리티)
```

### 규칙

| 행동 | 규칙 |
|---|---|
| `ApiResponse<T>` 사용 | 모든 팀원이 자유롭게 사용 가능 |
| `CustomException` 사용 | 모든 팀원이 자유롭게 사용 가능 |
| `common/config/` 수정 | **팀 전체 합의 후** 팀장이 수정 |
| `common/util/` 추가 | 공통으로 필요한 경우 팀장에게 요청 |
| `SecurityConfig.java` | 송형진 전담 (JWT 필터 구현) |

---

## 프론트엔드 도메인 경계

```
frontend/src/
├── views/
│   ├── auth/          -- 송형진
│   ├── mypage/        -- 송형진
│   ├── profile/       -- 송형진
│   ├── financial/     -- 송형진
│   ├── asset/         -- 이아영
│   ├── saving/        -- 권유현
│   ├── travel/        -- 권원영
│   ├── exchange/      -- 권원영
│   ├── prepay/        -- 권원영
│   ├── schedule/      -- 홍유진
│   ├── expense/       -- 홍유진
│   ├── ocr/           -- 홍유진
│   └── report/        -- 홍유진
├── stores/
│   ├── auth.js        -- 송형진 (기본 구조 완성, 수정 필요 시 합의)
│   └── travel.js      -- 권원영 (기본 구조 완성, 수정 필요 시 합의)
└── components/common/ -- 팀 전체 합의 후 추가
```

---

## 충돌 예방 체크리스트

작업 시작 전 확인:

- [ ] 내가 수정하려는 파일이 내 담당 패키지에 있는가?
- [ ] `common/` 수정이 필요한가? → 팀장/전체에게 먼저 알림
- [ ] 다른 팀원의 테이블 구조에 의존하는가? → 해당 담당자에게 확인
- [ ] `develop` 브랜치를 최신으로 `pull` 했는가?
