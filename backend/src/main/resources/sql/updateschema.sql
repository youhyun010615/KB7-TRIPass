USE tripass;

SET FOREIGN_KEY_CHECKS = 0;

-- ===== DROP TABLES =====
DROP TABLE IF EXISTS receipt_items;
DROP TABLE IF EXISTS receipts;
DROP TABLE IF EXISTS trip_reports;
DROP TABLE IF EXISTS trip_checklist_items;
DROP TABLE IF EXISTS pre_expenses;
DROP TABLE IF EXISTS trip_schedules;
DROP TABLE IF EXISTS trip_countries;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS notification_settings;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS saving_plans;
DROP TABLE IF EXISTS financial_schedules;
DROP TABLE IF EXISTS fixed_expenses;
DROP TABLE IF EXISTS income_sources;
DROP TABLE IF EXISTS category_budgets;
DROP TABLE IF EXISTS exchange_rate_alerts;
DROP TABLE IF EXISTS exchange_rates;
DROP TABLE IF EXISTS codef_connected_institutions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS codef_connections;
DROP TABLE IF EXISTS trips;
DROP TABLE IF EXISTS savings_product_options;
DROP TABLE IF EXISTS savings_products;
DROP TABLE IF EXISTS spending_categories;
DROP TABLE IF EXISTS checklist_templates;
DROP TABLE IF EXISTS countries;
DROP TABLE IF EXISTS currencies;
DROP TABLE IF EXISTS exchange_bank_branches;
DROP TABLE IF EXISTS travel_cards;
DROP TABLE IF EXISTS supported_institutions;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS phone_verifications;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;


-- ===== CREATE TABLES =====

-- 1. 회원
CREATE TABLE users (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
    login_id             VARCHAR(255) NOT NULL                COMMENT '로그인 아이디',
    password          VARCHAR(255) NULL                    COMMENT '비밀번호(소셜 전용 계정은 NULL)',
    name              VARCHAR(100) NOT NULL                COMMENT '이름',
    phone_number      VARCHAR(30)  NULL                    COMMENT '휴대전화번호(소셜 가입 시 미등록 가능)',
    login_provider    VARCHAR(20)  NOT NULL DEFAULT 'LOCAL'
    COMMENT '로그인 제공자(LOCAL/KAKAO/GOOGLE)',
    provider_key      VARCHAR(255) NULL
    COMMENT '소셜 로그인 제공자의 사용자 고유 식별값',
    current_view_mode VARCHAR(20)  NOT NULL DEFAULT 'SAVING'
    COMMENT '현재 화면 모드(SAVING/TRAVEL)',
    is_deleted        TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '탈퇴 여부',
    deleted_at        DATETIME     NULL                    COMMENT '탈퇴일시',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    -- 동일한 이메일이라도 로그인 방식이 다르면 별도 계정으로 허용
    UNIQUE KEY uk_users_provider_login_id (login_provider, login_id),
    -- 동일 로그인 제공자의 동일 사용자가 중복 가입되는 것을 방지
    UNIQUE KEY uk_users_provider_key (login_provider, provider_key)

) COMMENT '회원';
CREATE TABLE refresh_tokens (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Refresh Token ID',
    user_id            BIGINT       NOT NULL COMMENT '회원 ID',
    token_id           VARCHAR(100) NOT NULL COMMENT 'JWT 고유 식별값(jti)',
    token_hash         CHAR(64)     NOT NULL COMMENT 'Refresh Token SHA-256 해시값',
    expires_at         DATETIME     NOT NULL COMMENT '만료일시',
    revoked_at         DATETIME     NULL COMMENT '폐기일시',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP
                                    COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token_id (token_id),
    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_expires_at (expires_at),
    CONSTRAINT fk_refresh_tokens_users
    FOREIGN KEY (user_id) REFERENCES users(id)

) COMMENT 'Refresh Token';
CREATE TABLE phone_verifications (
    id                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '휴대전화 인증 ID',
    request_id             VARCHAR(100) NOT NULL COMMENT '인증 요청 식별값(UUID)',
    phone_number           VARCHAR(30)  NOT NULL COMMENT '인증 대상 휴대전화번호',
    verification_purpose   VARCHAR(30)  NOT NULL COMMENT '인증 목적(SIGNUP/FIND_ID/RESET_PASSWORD)',
    verification_code_hash VARCHAR(255) NOT NULL COMMENT '인증번호 해시값',
    expires_at             DATETIME     NOT NULL COMMENT '인증번호 만료일시',
    verified_at            DATETIME     NULL COMMENT '인증 성공일시',
    used_at                DATETIME     NULL COMMENT '인증 결과 사용일시',
    attempt_count          INT          NOT NULL DEFAULT 0 COMMENT '인증번호 확인 시도 횟수',
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '인증 요청일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone_verifications_request_id (request_id),
    INDEX idx_phone_verifications_lookup (phone_number, verification_purpose, created_at),
    INDEX idx_phone_verifications_expiration (expires_at)
) COMMENT '휴대전화 인증';

-- 2. 통화
CREATE TABLE currencies (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '통화 ID',
    currency_code VARCHAR(3)   NOT NULL                COMMENT '통화 코드(ISO 4217, 예: USD/KRW/EUR)',
    currency_name VARCHAR(100) NOT NULL                COMMENT '통화명',
    symbol        VARCHAR(10)  NULL                    COMMENT '통화 기호',
    unit          INT          NOT NULL DEFAULT 1      COMMENT '환율 표시 기준 단위',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_currencies_code (currency_code)
) COMMENT '통화';


-- 3. 국가
CREATE TABLE countries (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '국가 ID',
    country_name VARCHAR(100) NOT NULL                COMMENT '국가명',
    currency_id  BIGINT       NOT NULL                COMMENT '기본 통화 ID',
    flag_url     VARCHAR(500) NULL                    COMMENT '국기 이미지 URL',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_countries_currency FOREIGN KEY (currency_id) REFERENCES currencies (id)
) COMMENT '국가';


-- 4. 지출 카테고리
CREATE TABLE spending_categories (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
    category_name VARCHAR(50) NOT NULL                COMMENT '카테고리명',
    display_order INT         NOT NULL DEFAULT 0      COMMENT '표시 순서',
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id)
) COMMENT '지출 카테고리';


-- 5. 체크리스트 템플릿
CREATE TABLE checklist_templates (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '체크리스트 템플릿 ID',
    checklist_type VARCHAR(20)  NOT NULL                COMMENT '체크리스트 유형(PRE_TRAVEL/RETURN)',
    dday_stage     VARCHAR(10)  NULL                    COMMENT 'D-Day 단계(D30/D7/D1)',
    item_name      VARCHAR(200) NOT NULL                COMMENT '항목명',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id)
) COMMENT '체크리스트 템플릿';


-- 6. 지원 금융기관
CREATE TABLE supported_institutions (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '금융기관 ID',
    organization_code    VARCHAR(20)  NOT NULL                COMMENT 'CODEF 기관코드',
    institution_name     VARCHAR(100) NOT NULL                COMMENT '금융기관명',
    business_type        VARCHAR(10)  NOT NULL DEFAULT 'BK'  COMMENT '업권 구분(BK:은행, CD:카드)',
    supported_auth_types VARCHAR(200) NULL                    COMMENT '지원 인증서 유형',
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '상태(ACTIVE/MAINTENANCE)',
    logo_url             VARCHAR(500) NULL                    COMMENT '로고 URL',
    display_order        INT          NOT NULL DEFAULT 0      COMMENT '표시 순서',
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE   COMMENT '사용 여부',
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supported_institutions_code (organization_code)
) COMMENT '지원 금융기관';


-- 7. 환전 은행 지점
CREATE TABLE exchange_bank_branches (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '환전 은행 지점 ID',
    bank_name         VARCHAR(100)   NOT NULL                COMMENT '은행명',
    branch_name       VARCHAR(150)   NOT NULL                COMMENT '지점명',
    address           VARCHAR(500)   NULL                    COMMENT '주소',
    latitude          DECIMAL(10, 7) NULL                    COMMENT '위도',
    longitude         DECIMAL(10, 7) NULL                    COMMENT '경도',
    business_hours    VARCHAR(200)   NULL                    COMMENT '영업시간',
    phone_number      VARCHAR(30)    NULL                    COMMENT '전화번호',
    preferential_rate DECIMAL(5, 2)  NULL                    COMMENT '환율 우대율(%)',
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE   COMMENT '사용 여부',
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id)
) COMMENT '환전 은행 지점';


-- 8. 트래블카드
CREATE TABLE travel_cards (
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '트래블카드 ID',
    card_name             VARCHAR(150)   NOT NULL                COMMENT '카드명',
    card_company          VARCHAR(100)   NOT NULL                COMMENT '카드사',
    bank_name             VARCHAR(100)   NULL                    COMMENT '발급/연계 은행',
    required_account      VARCHAR(150)   NULL                    COMMENT '필요 외화통장',
    instant_use           BOOLEAN        NOT NULL DEFAULT FALSE  COMMENT '즉시 사용 가능 여부',
    applied_rate_info     VARCHAR(255)   NULL                    COMMENT '적용 환율 정보',
    holding_limit         DECIMAL(18, 2) NULL                    COMMENT '외화 보유 한도',
    payment_limit         DECIMAL(18, 2) NULL                    COMMENT '결제 한도',
    atm_limit             DECIMAL(18, 2) NULL                    COMMENT 'ATM 한도',
    exchange_fee          VARCHAR(100)   NULL                    COMMENT '환전 수수료',
    re_exchange_fee       VARCHAR(100)   NULL                    COMMENT '재환전 수수료',
    payment_fee           VARCHAR(100)   NULL                    COMMENT '결제 수수료',
    withdrawal_fee        VARCHAR(100)   NULL                    COMMENT '출금 수수료',
    auto_charge_supported BOOLEAN        NOT NULL DEFAULT FALSE  COMMENT '자동충전 지원 여부',
    is_transit_card       BOOLEAN        NOT NULL DEFAULT FALSE  COMMENT '교통카드 여부',
    is_active             BOOLEAN        NOT NULL DEFAULT TRUE   COMMENT '사용 여부',
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id)
) COMMENT '트래블카드';


-- 9. 적금 상품
CREATE TABLE savings_products (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '적금 상품 ID',
    fin_co_no        VARCHAR(20)  NOT NULL                COMMENT '금융회사 코드',
    kor_co_nm        VARCHAR(100) NOT NULL                COMMENT '금융회사명',
    fin_prdt_cd      VARCHAR(100) NOT NULL                COMMENT '금융상품 코드',
    fin_prdt_nm      VARCHAR(255) NOT NULL                COMMENT '상품명',
    join_way         VARCHAR(255) NULL                    COMMENT '가입 방법',
    mtrt_int         TEXT         NULL                    COMMENT '만기 후 이자율',
    spcl_cnd         TEXT         NULL                    COMMENT '우대 조건',
    max_limit        BIGINT       NULL                    COMMENT '월 납입한도',
    is_travel_saving BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '여행 적금 여부',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_savings_products_code (fin_co_no, fin_prdt_cd)
) COMMENT '적금 상품';


-- 10. 적금 상품 옵션
CREATE TABLE savings_product_options (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '상품 옵션 ID',
    product_id       BIGINT        NOT NULL                COMMENT '적금 상품 ID',
    intr_rate_type   VARCHAR(1)    NULL                    COMMENT '금리 유형(S:단리, M:복리)',
    intr_rate_type_nm VARCHAR(20)  NULL                    COMMENT '금리 유형명',
    rsrv_type        VARCHAR(1)    NULL                    COMMENT '적립 유형',
    rsrv_type_nm     VARCHAR(20)   NULL                    COMMENT '적립 유형명(정액/자유)',
    save_trm         INT           NOT NULL                COMMENT '저축 기간(개월)',
    intr_rate        DECIMAL(7, 4) NULL                    COMMENT '기본 금리',
    intr_rate2       DECIMAL(7, 4) NULL                    COMMENT '최고 우대 금리',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_savings_product_options_product FOREIGN KEY (product_id) REFERENCES savings_products (id)
) COMMENT '적금 상품 옵션';


-- 11. CODEF 연동
CREATE TABLE codef_connections (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'CODEF 연동 ID',
    user_id           BIGINT       NOT NULL                COMMENT '회원 ID',
    connected_id      VARCHAR(255) NOT NULL                COMMENT 'CODEF connectedId(암호화 저장)',
    connection_status VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '연동 상태',
    last_synced_at    TIMESTAMP    NULL                    COMMENT '최근 동기화 일시',
    is_deleted        TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at        DATETIME     NULL                    COMMENT '삭제일시',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_codef_connections_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT 'CODEF 연동';


-- 12. CODEF 연동 기관
CREATE TABLE codef_connected_institutions (
    id                           BIGINT      NOT NULL AUTO_INCREMENT,
    codef_connection_id          BIGINT      NOT NULL COMMENT 'CODEF 연동 ID',
    organization_code            VARCHAR(20) NOT NULL,
    organization_name            VARCHAR(100) NULL,
    bank_codef_name              VARCHAR(100) NULL,
    business_type                VARCHAR(10) NOT NULL DEFAULT 'BK',
    connection_status            VARCHAR(20) NOT NULL DEFAULT 'CONNECTED',
    last_registration_checked_at TIMESTAMP   NULL,
    last_synced_at               TIMESTAMP   NULL,
    is_active                    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at                   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_codef_connected_institutions_connection FOREIGN KEY (codef_connection_id) REFERENCES codef_connections (id)
) COMMENT 'CODEF 연동 기관';


-- 13. 계좌
CREATE TABLE accounts (
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT '계좌 ID',
    user_id                 BIGINT         NOT NULL                COMMENT '회원 ID',
    codef_connection_id     BIGINT         NULL                    COMMENT 'CODEF 연동 ID',
    account_name            VARCHAR(150)   NOT NULL                COMMENT '계좌명',
    account_number          VARCHAR(255)   NULL                    COMMENT '계좌번호(암호화)',
    account_type            VARCHAR(20)    NOT NULL                COMMENT '계좌 유형(CHECKING/DEPOSIT/SAVING/FOREIGN)',
    balance                 DECIMAL(18, 2) NULL                    COMMENT '잔액',
    withdrawable_amount     DECIMAL(18, 2) NULL                    COMMENT '출금가능액',
    recognized_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0      COMMENT '여행 자금 인정 금액',
    interest_rate           DECIMAL(7, 4)  NULL,
    maturity_date           DATE           NULL,
    is_travel_fund_included BOOLEAN        NOT NULL DEFAULT FALSE,
    connection_type         VARCHAR(20)    NOT NULL DEFAULT 'CODEF' COMMENT '연결 유형(CODEF/DEMO/MANUAL)',
    external_account_key    VARCHAR(255)   NULL,
    last_synced_at          TIMESTAMP      NULL,
    is_active               BOOLEAN        NOT NULL DEFAULT TRUE   COMMENT '연동 활성 여부',
    is_deleted              TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at              DATETIME       NULL                    COMMENT '삭제일시',
    created_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_accounts_codef_connection FOREIGN KEY (codef_connection_id) REFERENCES codef_connections (id)
) COMMENT '계좌';


-- 14. 여행
CREATE TABLE trips (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    user_id             BIGINT         NOT NULL,
    trip_name           VARCHAR(150)   NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'PLANNING' COMMENT '여행 상태(PLANNING/TRAVELING/ENDED)',
    start_date          DATE           NULL,
    end_date            DATE           NULL,
    total_target_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    is_deleted          TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at          DATETIME       NULL                    COMMENT '삭제일시',
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_trips_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '여행';


-- 15. 여행 국가
CREATE TABLE trip_countries (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '여행 국가 ID',
    trip_id        BIGINT         NOT NULL                COMMENT '여행 ID',
    country_id     BIGINT         NOT NULL                COMMENT '국가 ID',
    arrival_date   DATE           NOT NULL                COMMENT '도착일',
    departure_date DATE           NOT NULL                COMMENT '출발일',
    target_budget  DECIMAL(18, 2) NOT NULL                COMMENT '국가별 목표 예산',
    display_order  INT            NOT NULL DEFAULT 0      COMMENT '방문 순서',
    is_deleted     TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at     DATETIME       NULL                    COMMENT '삭제일시',
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_trip_countries_trip    FOREIGN KEY (trip_id)    REFERENCES trips (id),
    CONSTRAINT fk_trip_countries_country FOREIGN KEY (country_id) REFERENCES countries (id)
) COMMENT '여행 국가';


-- 16. 거래 내역
CREATE TABLE transactions (
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '거래 ID',
    account_id            BIGINT         NOT NULL                COMMENT '계좌 ID',
    category_id           BIGINT         NULL                    COMMENT '카테고리 ID',
    trip_id               BIGINT         NULL                    COMMENT '여행 ID',
    trip_country_id       BIGINT         NULL                    COMMENT '여행 국가 ID',
    currency_id           BIGINT         NULL                    COMMENT '현지 통화 ID',
    transaction_date      DATE           NOT NULL                COMMENT '거래일자',
    transaction_time      TIME           NULL                    COMMENT '거래시각',
    transaction_type      VARCHAR(20)    NOT NULL                COMMENT '거래 구분(DEPOSIT/WITHDRAWAL)',
    transaction_region    VARCHAR(20)    NOT NULL                COMMENT '국내/해외 구분(DOMESTIC/OVERSEAS)',
    amount                DECIMAL(18, 2) NOT NULL                COMMENT '거래 금액(원화 기준)',
    balance_after         DECIMAL(18, 2) NULL                    COMMENT '거래 후 잔액',
    merchant_name         VARCHAR(255)   NULL                    COMMENT '거래처',
    original_amount       DECIMAL(18, 2) NULL                    COMMENT '현지 통화 금액',
    applied_exchange_rate DECIMAL(15, 4) NULL                    COMMENT '적용 환율',
    payment_method        VARCHAR(50)    NULL                    COMMENT '결제수단',
    is_pre_expense        BOOLEAN        NOT NULL DEFAULT FALSE  COMMENT '사전지출 전환 여부',
    memo                  VARCHAR(500)   NULL                    COMMENT '메모',
    external_key          VARCHAR(255)   NOT NULL                COMMENT '중복 수집 방지 키',
    is_deleted            TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at            DATETIME       NULL                    COMMENT '삭제일시',
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transactions_account_key (account_id, external_key),
    CONSTRAINT fk_transactions_account      FOREIGN KEY (account_id)      REFERENCES accounts (id),
    CONSTRAINT fk_transactions_category     FOREIGN KEY (category_id)     REFERENCES spending_categories (id),
    CONSTRAINT fk_transactions_trip         FOREIGN KEY (trip_id)         REFERENCES trips (id),
    CONSTRAINT fk_transactions_trip_country FOREIGN KEY (trip_country_id) REFERENCES trip_countries (id),
    CONSTRAINT fk_transactions_currency     FOREIGN KEY (currency_id)     REFERENCES currencies (id)
) COMMENT '거래 내역';


-- 17. 환율
-- base_currency_id: 기준 통화(KRW), target_currency_id: 대상 통화(USD 등)
CREATE TABLE exchange_rates (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '환율 ID',
    base_currency_id   BIGINT         NOT NULL                COMMENT '기준 통화 ID(KRW)',
    target_currency_id BIGINT         NOT NULL                COMMENT '대상 통화 ID(USD 등)',
    currency_unit      INT            NOT NULL DEFAULT 1      COMMENT '통화 단위',
    deal_base_rate     DECIMAL(20, 8) NOT NULL                COMMENT '매매기준율',
    prev_rate          DECIMAL(20, 8) NULL                    COMMENT '전일 매매기준율',
    rate_date          DATE           NOT NULL                COMMENT '환율 기준일',
    fetched_at         TIMESTAMP      NOT NULL                COMMENT 'API 수신 시각',
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exchange_rates_target_date (target_currency_id, rate_date),
    CONSTRAINT fk_exchange_rates_base_currency   FOREIGN KEY (base_currency_id)   REFERENCES currencies (id),
    CONSTRAINT fk_exchange_rates_target_currency FOREIGN KEY (target_currency_id) REFERENCES currencies (id)
) COMMENT '환율';


-- 18. 관심 환율 알림
CREATE TABLE exchange_rate_alerts (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '관심 환율 알림 ID',
    user_id       BIGINT         NOT NULL                COMMENT '회원 ID',
    currency_id   BIGINT         NOT NULL                COMMENT '관심 통화 ID',
    target_rate   DECIMAL(20, 8) NULL                    COMMENT '목표 환율',
    target_amount DECIMAL(18, 2) NULL                    COMMENT '환전 희망 금액',
    is_deleted    TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at    DATETIME       NULL                    COMMENT '삭제일시',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_exchange_rate_alerts_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_exchange_rate_alerts_currency FOREIGN KEY (currency_id) REFERENCES currencies (id)
) COMMENT '관심 환율 알림';


-- 19. 저축 계획
CREATE TABLE saving_plans (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '저축 계획 ID',
    trip_id        BIGINT         NOT NULL                COMMENT '여행 ID',
    account_id     BIGINT         NULL                    COMMENT '저축 계좌 ID',
    monthly_amount DECIMAL(18, 2) NOT NULL DEFAULT 0      COMMENT '월 저축 금액',
    goal_status    VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '상태(ACTIVE/COMPLETED/CANCELLED)',
    is_deleted     TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at     DATETIME       NULL                    COMMENT '삭제일시',
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_saving_plans_trip    FOREIGN KEY (trip_id)    REFERENCES trips (id),
    CONSTRAINT fk_saving_plans_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) COMMENT '저축 계획';


-- 20. 카테고리 예산
CREATE TABLE category_budgets (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    user_id       BIGINT         NOT NULL,
    category_id   BIGINT         NOT NULL,
    target_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    is_deleted    TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at    DATETIME       NULL                COMMENT '삭제일시',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_budgets_user_category (user_id, category_id),
    CONSTRAINT fk_category_budgets_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_category_budgets_category FOREIGN KEY (category_id) REFERENCES spending_categories (id)
) COMMENT '카테고리 예산';


-- 21. 수입 출처
CREATE TABLE income_sources (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    user_id      BIGINT         NOT NULL,
    account_id   BIGINT         NOT NULL,
    payment_name VARCHAR(150)   NOT NULL,
    payment_day  INT            NOT NULL COMMENT '급여일(1~31)',
    memo         VARCHAR(500)   NULL,
    gross_amount DECIMAL(18, 2) NOT NULL,
    is_deleted   TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at   DATETIME       NULL                COMMENT '삭제일시',
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_income_sources_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_income_sources_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) COMMENT '수입 출처';


-- 22. 고정 지출
CREATE TABLE fixed_expenses (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    user_id      BIGINT         NOT NULL,
    account_id   BIGINT         NULL,
    expense_name VARCHAR(150)   NOT NULL COMMENT '항목명(월세/통신비 등)',
    amount       DECIMAL(18, 2) NOT NULL,
    payment_day  INT            NOT NULL COMMENT '납부일(1~31)',
    memo         VARCHAR(500)   NULL,
    is_deleted   TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at   DATETIME       NULL                COMMENT '삭제일시',
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_fixed_expenses_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_fixed_expenses_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) COMMENT '고정 지출';


-- 23. 금융 일정
CREATE TABLE financial_schedules (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '금융 일정 ID',
    user_id          BIGINT      NOT NULL                COMMENT '회원 ID',
    fixed_expense_id BIGINT      NULL,
    income_source_id BIGINT      NULL,
    schedule_type    VARCHAR(20) NOT NULL                COMMENT '일정 유형(INCOME/EXPENSE)',
    is_deleted       TINYINT(1)  NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at       DATETIME    NULL                    COMMENT '삭제일시',
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_financial_schedules_user          FOREIGN KEY (user_id)          REFERENCES users (id),
    CONSTRAINT fk_financial_schedules_fixed_expense FOREIGN KEY (fixed_expense_id) REFERENCES fixed_expenses (id),
    CONSTRAINT fk_financial_schedules_income_source FOREIGN KEY (income_source_id) REFERENCES income_sources (id)
) COMMENT '금융 일정';


-- 24. 여행 일정
CREATE TABLE trip_schedules (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '여행 일정 ID',
    trip_id         BIGINT          NOT NULL                COMMENT '여행 ID',
    trip_country_id BIGINT          NULL                    COMMENT '여행 국가 ID',
    currency_id     BIGINT          NULL                    COMMENT '통화 ID',
    schedule_name   VARCHAR(200)    NOT NULL                COMMENT '일정명',
    scheduled_at    TIMESTAMP       NOT NULL                COMMENT '일정 일시',
    amount          DECIMAL(18, 2)  NULL                    COMMENT '현지 통화 금액',
    payment_status  VARCHAR(20)     NOT NULL DEFAULT 'UNDECIDED' COMMENT '결제 상태(PREPAID/ONSITE/UNDECIDED)',
    schedule_status VARCHAR(20)     NOT NULL DEFAULT 'UPCOMING' COMMENT '진행 상태(UPCOMING/DONE)',
    place_name      VARCHAR(200)    NULL                    COMMENT '장소명',
    place_address   VARCHAR(500)    NULL                    COMMENT '장소 주소',
    memo            VARCHAR(1000)   NULL                    COMMENT '메모',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at      DATETIME        NULL                    COMMENT '삭제일시',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_trip_schedules_trip         FOREIGN KEY (trip_id)         REFERENCES trips (id),
    CONSTRAINT fk_trip_schedules_trip_country FOREIGN KEY (trip_country_id) REFERENCES trip_countries (id),
    CONSTRAINT fk_trip_schedules_currency     FOREIGN KEY (currency_id)     REFERENCES currencies (id)
) COMMENT '여행 일정';


-- 25. 여행 체크리스트
CREATE TABLE trip_checklist_items (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '여행 체크리스트 항목 ID',
    trip_id         BIGINT       NOT NULL                COMMENT '여행 ID',
    template_id     BIGINT       NULL                    COMMENT '템플릿 ID(사용자 추가 항목은 NULL)',
    checklist_type  VARCHAR(20)  NOT NULL                COMMENT '체크리스트 유형(PRE_TRAVEL/RETURN)',
    dday_stage      VARCHAR(10)  NULL                    COMMENT 'D-Day 단계(D30/D7/D1)',
    item_name       VARCHAR(200) NOT NULL                COMMENT '항목명',
    is_completed    BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '완료 여부',
    is_excluded     BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '기본 항목 제외 여부',
    is_custom       BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '사용자 추가 항목 여부',
    is_carried_over BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '이전 단계 미완료 이월 여부',
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at      DATETIME     NULL                    COMMENT '삭제일시',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_trip_checklist_items_trip     FOREIGN KEY (trip_id)     REFERENCES trips (id),
    CONSTRAINT fk_trip_checklist_items_template FOREIGN KEY (template_id) REFERENCES checklist_templates (id)
) COMMENT '여행 체크리스트';


-- 26. 사전 지출
CREATE TABLE pre_expenses (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    trip_id         BIGINT         NOT NULL                COMMENT '여행 ID',
    transaction_id  BIGINT         NOT NULL                COMMENT '원본 거래 ID',
    trip_country_id BIGINT         NULL                    COMMENT '대상 여행 국가 ID(공통은 NULL)',
    scope           VARCHAR(10)    NOT NULL                COMMENT '적용 구분(COMMON/COUNTRY)',
    direct_amount   DECIMAL(18, 2) NOT NULL                COMMENT '직접 사전지출액',
    is_deleted      TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at      DATETIME       NULL                    COMMENT '삭제일시',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_pre_expenses_trip         FOREIGN KEY (trip_id)         REFERENCES trips (id),
    CONSTRAINT fk_pre_expenses_transaction  FOREIGN KEY (transaction_id)  REFERENCES transactions (id),
    CONSTRAINT fk_pre_expenses_trip_country FOREIGN KEY (trip_country_id) REFERENCES trip_countries (id)
) COMMENT '사전 지출';


-- 27. 해외 영수증
CREATE TABLE receipts (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '해외 영수증 ID',
    user_id       BIGINT         NOT NULL                COMMENT '회원 ID',
    trip_id       BIGINT         NULL                    COMMENT '여행 ID',
    country_id    BIGINT         NULL                    COMMENT '국가 ID',
    currency_id   BIGINT         NULL                    COMMENT '통화 ID',
    payment_date  DATE           NOT NULL                COMMENT '결제일',
    file_name     VARCHAR(255)   NOT NULL                COMMENT '파일명',
    file_url      VARCHAR(1000)  NOT NULL                COMMENT '파일 URL',
    file_type     VARCHAR(10)    NOT NULL                COMMENT '파일 형식(JPG/PNG/PDF, 10MB 이하)',
    status        VARCHAR(20)    NOT NULL DEFAULT 'UPLOADED' COMMENT '처리 상태(UPLOADED/PROCESSING/COMPLETED/FAILED)',
    merchant_name VARCHAR(255)   NULL                    COMMENT '상호명',
    total_amount  DECIMAL(18, 2) NULL                    COMMENT '현지 총액',
    tax_amount    DECIMAL(18, 2) NULL                    COMMENT '부가세',
    ocr_raw_text  TEXT           NULL                    COMMENT 'OCR 원문 텍스트',
    error_message TEXT           NULL                    COMMENT '오류 메시지',
    is_deleted    TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at    DATETIME       NULL                    COMMENT '삭제일시',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    processed_at  TIMESTAMP      NULL                    COMMENT 'OCR 처리 완료 일시',
    PRIMARY KEY (id),
    CONSTRAINT fk_receipts_user     FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_receipts_trip     FOREIGN KEY (trip_id)    REFERENCES trips (id),
    CONSTRAINT fk_receipts_country  FOREIGN KEY (country_id) REFERENCES countries (id),
    CONSTRAINT fk_receipts_currency FOREIGN KEY (currency_id) REFERENCES currencies (id)
) COMMENT '해외 영수증';


-- 28. 영수증 품목
CREATE TABLE receipt_items (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '영수증 품목 ID',
    receipt_id      BIGINT         NOT NULL                COMMENT '해외 영수증 ID',
    original_name   VARCHAR(255)   NOT NULL                COMMENT '원문 품목명',
    translated_name VARCHAR(255)   NULL                    COMMENT '번역 품목명',
    quantity        INT            NULL                    COMMENT '수량',
    amount          DECIMAL(18, 2) NULL                    COMMENT '현지 금액',
    display_order   INT            NOT NULL                COMMENT '표시 순서',
    is_deleted      TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at      DATETIME       NULL                    COMMENT '삭제일시',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_receipt_items_receipt FOREIGN KEY (receipt_id) REFERENCES receipts (id)
) COMMENT '영수증 품목';


-- 29. 여행 리포트
CREATE TABLE trip_reports (
    id                        BIGINT         NOT NULL AUTO_INCREMENT COMMENT '여행 리포트 ID',
    trip_id                   BIGINT         NOT NULL                COMMENT '여행 ID',
    report_type               VARCHAR(20)    NOT NULL                COMMENT '리포트 유형(PRE_TRAVEL/FINAL)',
    total_expense             DECIMAL(18, 2) NULL                    COMMENT '총 지출',
    target_budget             DECIMAL(18, 2) NULL                    COMMENT '목표 예산',
    saved_amount              DECIMAL(18, 2) NULL                    COMMENT '절감액',
    pre_expense_total         DECIMAL(18, 2) NULL                    COMMENT '사전 지출 합계',
    available_fund            DECIMAL(18, 2) NULL                    COMMENT '출국 시 사용 가능 자금',
    goal_achievement_rate     DECIMAL(5, 2)  NULL                    COMMENT '목표 달성률(%)',
    checklist_completion_rate DECIMAL(5, 2)  NULL                    COMMENT '체크리스트 완료율(%)',
    breakdown_data            TEXT           NULL                    COMMENT '카테고리별·국가별 지출 상세(JSON)',
    pdf_url                   VARCHAR(1000)  NULL                    COMMENT 'PDF 파일 URL',
    generated_at              TIMESTAMP      NOT NULL                COMMENT '리포트 생성일시',
    created_at                TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at                TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_trip_reports_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
) COMMENT '여행 리포트';


-- 30. 알림
CREATE TABLE notifications (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '알림 ID',
    user_id           BIGINT       NOT NULL                COMMENT '회원 ID',
    notification_type VARCHAR(30)  NOT NULL                COMMENT '알림 유형',
    title             VARCHAR(200) NOT NULL                COMMENT '제목',
    message           TEXT         NOT NULL                COMMENT '내용',
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '읽음 여부',
    is_deleted        TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at        DATETIME     NULL                    COMMENT '삭제일시',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '알림';


-- 31. 알림 설정
CREATE TABLE notification_settings (
    id                         BIGINT    NOT NULL AUTO_INCREMENT COMMENT '알림 설정 ID',
    user_id                    BIGINT    NOT NULL                COMMENT '회원 ID',
    all_enabled                BOOLEAN   NOT NULL DEFAULT TRUE   COMMENT '전체 알림 사용 여부',
    financial_schedule_enabled BOOLEAN   NOT NULL DEFAULT TRUE   COMMENT '금융 일정 알림',
    travel_schedule_enabled    BOOLEAN   NOT NULL DEFAULT TRUE   COMMENT '여행 일정 알림',
    exchange_rate_enabled      BOOLEAN   NOT NULL DEFAULT TRUE   COMMENT '환율 알림',
    checklist_enabled          BOOLEAN   NOT NULL DEFAULT TRUE   COMMENT '체크리스트 알림',
    travel_report_enabled      BOOLEAN   NOT NULL DEFAULT TRUE   COMMENT '여행 레포트 알림',
    updated_at                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_settings_user (user_id),
    CONSTRAINT fk_notification_settings_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '알림 설정';


-- ===== INDEXES =====
CREATE INDEX idx_transactions_account_id      ON transactions (account_id);
CREATE INDEX idx_transactions_trip_id         ON transactions (trip_id);
CREATE INDEX idx_transactions_transaction_date ON transactions (transaction_date);
CREATE INDEX idx_accounts_user_id             ON accounts (user_id);
CREATE INDEX idx_trips_user_id                ON trips (user_id);
CREATE INDEX idx_trip_countries_trip_id       ON trip_countries (trip_id);
CREATE INDEX idx_trip_schedules_trip_id       ON trip_schedules (trip_id);
CREATE INDEX idx_trip_schedules_scheduled_at  ON trip_schedules (scheduled_at);
CREATE INDEX idx_trip_checklist_items_trip_id ON trip_checklist_items (trip_id);
CREATE INDEX idx_receipts_user_id             ON receipts (user_id);
CREATE INDEX idx_receipts_trip_id             ON receipts (trip_id);
CREATE INDEX idx_notifications_user_id        ON notifications (user_id);
CREATE INDEX idx_notifications_is_read        ON notifications (user_id, is_read);
CREATE INDEX idx_exchange_rates_rate_date      ON exchange_rates (rate_date);
CREATE INDEX idx_pre_expenses_trip_id         ON pre_expenses (trip_id);
CREATE INDEX idx_saving_plans_trip_id         ON saving_plans (trip_id);
