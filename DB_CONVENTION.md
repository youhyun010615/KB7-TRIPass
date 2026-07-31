# DB_CONVENTION — TRIPass 데이터베이스 설계 규칙

> 모든 테이블 설계는 이 문서의 규칙을 따릅니다. DDL 작성 전 반드시 숙지하세요.

---

## 1. 테이블 · 컬럼 이름 규칙

### snake_case 사용

모든 테이블명과 컬럼명은 **소문자 + 언더바(`_`)** 만 사용합니다.

```sql
-- 올바른 예
CREATE TABLE saving_goals ( ... );
CREATE TABLE trip_countries ( ... );
ALTER TABLE expenses ADD COLUMN category_id BIGINT;

-- 잘못된 예
savingGoals     -- camelCase 금지
SavingGoals     -- PascalCase 금지
SAVING_GOALS    -- 대문자 금지
```

### 테이블명 규칙

| 규칙 | 예시 |
|---|---|
| 복수형 명사 | `trips`, `expenses`, `schedules` |
| 연결 테이블 | `<테이블A>_<테이블B>` 형식: `trip_countries` |
| 도메인 접두사는 생략 | `goals` (O), `saving_saving_goals` (X) |

---

## 2. PK · FK 이름 규칙

### Primary Key

- 모든 테이블은 `id` 컬럼을 PK로 사용
- 타입: `BIGINT AUTO_INCREMENT`

```sql
id BIGINT AUTO_INCREMENT PRIMARY KEY
```

### Foreign Key

형식: `<참조 테이블 단수형>_id`

```sql
user_id      BIGINT NOT NULL,   -- users 테이블 참조
trip_id      BIGINT NOT NULL,   -- trips 테이블 참조
category_id  BIGINT,            -- categories 테이블 참조 (nullable)

CONSTRAINT fk_expenses_user  FOREIGN KEY (user_id)  REFERENCES users(id),
CONSTRAINT fk_expenses_trip  FOREIGN KEY (trip_id)  REFERENCES trips(id)
```

FK 제약조건 이름 형식: `fk_<현재테이블>_<참조테이블단수>`

---

## 3. 공통 컬럼 (모든 테이블 필수)

```sql
created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

- **모든 테이블**에 `created_at`, `updated_at` 포함 필수
- 애플리케이션에서 수동으로 설정하지 않고 DB 기본값 사용

---

## 4. 논리 삭제 (Soft Delete)

TRIPass는 **논리 삭제**를 원칙으로 합니다.

```sql
is_deleted  TINYINT(1) NOT NULL DEFAULT 0,
deleted_at  DATETIME DEFAULT NULL
```

| 상황 | 처리 방식 |
|---|---|
| 사용자 삭제 요청 | `is_deleted = 1`, `deleted_at = NOW()` |
| 조회 쿼리 | 반드시 `WHERE is_deleted = 0` 조건 추가 |
| 물리 삭제 | 허용하지 않음 (admin 작업 제외) |

**논리 삭제 적용 대상**: `users`, `trips`, `expenses`, `schedules`, `saving_goals`, `prepay_items`  
**논리 삭제 불필요**: `exchange_rates` (외부 데이터), `ocr_receipts` (영수증 원본)

---

## 5. 금액 타입과 소수점 기준

### 원화(KRW) 금액

```sql
amount  INT NOT NULL DEFAULT 0
```

- 소수점 없이 **정수** 로 저장 (원 단위)
- `INT` 최대 약 21억: 일반 지출/저축에 충분
- 초과 가능성 있는 자산 금액은 `BIGINT` 사용

### 외화 금액

```sql
amount          DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
currency_code   VARCHAR(3) NOT NULL DEFAULT 'KRW'
```

- `DECIMAL(15, 2)`: 정수 13자리 + 소수 2자리
- 통화 코드는 별도 컬럼 필수

---

## 6. 통화 금액 저장 기준

- 통화가 다른 금액은 **원화 환산금액과 원본 금액을 함께** 저장

```sql
original_amount   DECIMAL(15, 2) NOT NULL,   -- 원본 통화 금액
currency_code     VARCHAR(3)     NOT NULL,   -- 원본 통화 코드 (ISO 4217)
krw_amount        INT            NOT NULL,   -- KRW 환산 금액 (당시 환율 기준)
exchange_rate     DECIMAL(10, 4) NOT NULL    -- 적용된 환율
```

---

## 7. Enum 저장 방식

Enum 값은 **문자열(VARCHAR)** 로 저장합니다. 숫자 코드 저장 금지.

```sql
-- 올바른 예
status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',   -- 'ACTIVE', 'INACTIVE', 'DELETED'
category    VARCHAR(30) NOT NULL,                    -- 'FOOD', 'TRANSPORT', 'ACCOMMODATION'
mode        VARCHAR(20) NOT NULL DEFAULT 'SAVING',   -- 'SAVING', 'TRAVEL'

-- 잘못된 예
status  TINYINT   -- 0, 1, 2 의미 불명확
```

- Java Enum 이름과 DB 저장 문자열을 **동일**하게 유지
- MyBatis `mapUnderscoreToCamelCase=true` 적용 중이므로 Enum 핸들러 별도 등록 주의

---

## 8. UNIQUE 및 인덱스 기준

### UNIQUE 제약 조건

```sql
-- 사용자 이메일
CONSTRAINT uq_users_email UNIQUE (email)

-- 여행 내 카테고리 중복 방지
CONSTRAINT uq_budget_trip_category UNIQUE (trip_id, category)
```

### 인덱스 생성 기준

성능이 필요한 조회 컬럼에만 추가합니다. 과도한 인덱스는 쓰기 성능 저하를 유발합니다.

```sql
-- FK 컬럼은 기본적으로 인덱스 추가
INDEX idx_expenses_user_id  (user_id),
INDEX idx_expenses_trip_id  (trip_id),

-- 날짜 범위 조회가 잦은 컬럼
INDEX idx_expenses_date     (expense_date),

-- 복합 인덱스 (자주 함께 조회되는 경우)
INDEX idx_expenses_trip_date (trip_id, expense_date)
```

인덱스 이름 형식: `idx_<테이블>_<컬럼>`

---

## 9. 트랜잭션 경계

### 원칙

- 트랜잭션은 **Service 레이어**에서 `@Transactional` 로 관리
- Controller와 Mapper에서는 트랜잭션 선언 금지

```java
// Service 레이어 예시
@Service
@Transactional(readOnly = true)   // 기본값: 읽기 전용
public class ExpenseService {

    @Transactional   // 쓰기 작업만 별도 선언
    public void createExpense(ExpenseDto dto) { ... }
}
```

### 여러 테이블 변경 시

복수 테이블을 수정하는 작업은 반드시 단일 트랜잭션으로 묶습니다.

```java
@Transactional
public void endTrip(Long tripId) {
    tripMapper.updateTripStatus(tripId, "ENDED");     // trips 수정
    budgetMapper.closeBudgets(tripId);                 // budgets 마감
    reportMapper.generateSummary(tripId);              // reports 생성
    // 하나라도 실패하면 전체 롤백
}
```

### 트랜잭션 금지 대상

- `@Transactional` 을 외부 API 호출 (환율 API, OCR API) 을 포함하는 메서드에 사용하지 않습니다.
- 외부 API 호출 실패 시 DB 롤백이 의미 없으므로 보상 로직으로 처리합니다.
