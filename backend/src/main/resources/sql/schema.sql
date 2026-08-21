USE tripass;

SET FOREIGN_KEY_CHECKS = 0;

-- ===== DROP TABLES =====
DROP TABLE IF EXISTS user_travel_cards;
DROP TABLE IF EXISTS wallet_exchange_transaction;
DROP TABLE IF EXISTS wallet_card_topup;
DROP TABLE IF EXISTS travel_card_ledger;
DROP TABLE IF EXISTS travel_card_balance;
DROP TABLE IF EXISTS wallet_travel_card;
DROP TABLE IF EXISTS wallet_auto_saving_logs;
DROP TABLE IF EXISTS wallet_auto_saving_rule;
DROP TABLE IF EXISTS wallet_ledger;
DROP TABLE IF EXISTS wallet_account;
DROP TABLE IF EXISTS wallet_withdraw_recipient;
DROP TABLE IF EXISTS wallet;
DROP TABLE IF EXISTS receipt_participants;
DROP TABLE IF EXISTS receipt_items;
DROP TABLE IF EXISTS receipts;
DROP TABLE IF EXISTS trip_reports;
DROP TABLE IF EXISTS trip_checklist_items;
DROP TABLE IF EXISTS pre_expenses;
DROP TABLE IF EXISTS trip_schedules;
DROP TABLE IF EXISTS trip_budget_recommendations;
DROP TABLE IF EXISTS country_budget_baselines;
DROP TABLE IF EXISTS trip_wallets;
DROP TABLE IF EXISTS trip_countries;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS notification_settings;
DROP TABLE IF EXISTS weekly_saving_missions;
DROP TABLE IF EXISTS monthly_saving_missions;
DROP TABLE IF EXISTS mission_category_selections;
DROP TABLE IF EXISTS monthly_category_analyses;
DROP TABLE IF EXISTS monthly_spending_analyses;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS saving_plans;
DROP TABLE IF EXISTS financial_schedules;
DROP TABLE IF EXISTS fixed_expenses;
DROP TABLE IF EXISTS income_sources;
DROP TABLE IF EXISTS category_budgets;
DROP TABLE IF EXISTS exchange_rate_alerts;
DROP TABLE IF EXISTS exchange_market_data;
DROP TABLE IF EXISTS exchange_rates;
DROP TABLE IF EXISTS codef_connected_institutions;
DROP TABLE IF EXISTS cards;
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
DROP TABLE IF EXISTS travel_card_currencies;
DROP TABLE IF EXISTS travel_cards;
DROP TABLE IF EXISTS supported_institutions;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS phone_verifications;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS user_fcm_tokens;

SET FOREIGN_KEY_CHECKS = 1;


-- ===== CREATE TABLES =====

-- 1. 회원
CREATE TABLE users
(
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
    login_id          VARCHAR(255) NOT NULL COMMENT '로그인 아이디',
    password          VARCHAR(255) NULL COMMENT '비밀번호(소셜 전용 계정은 NULL)',
    name              VARCHAR(100) NOT NULL COMMENT '이름',
    phone_number      VARCHAR(30)  NULL COMMENT '휴대전화번호(소셜 가입 시 미등록 가능)',
    login_provider    VARCHAR(20)  NOT NULL DEFAULT 'LOCAL'
        COMMENT '로그인 제공자(LOCAL/KAKAO/GOOGLE)',
    provider_key      VARCHAR(255) NULL
        COMMENT '소셜 로그인 제공자의 사용자 고유 식별값',
    current_view_mode VARCHAR(20)  NOT NULL DEFAULT 'SAVING'
        COMMENT '현재 화면 모드(SAVING/TRAVEL)',
    onboarding_shown_at DATETIME NULL COMMENT '여행/계좌 등록 온보딩 최초 노출 일시(NULL이면 아직 미노출)',
    is_deleted        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '탈퇴 여부',
    deleted_at        DATETIME     NULL COMMENT '탈퇴일시',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '생성일자',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '수정일자',
    PRIMARY KEY (id),
    -- 동일한 아이디라도 로그인 방식이 다르면 별도 계정으로 허용
    UNIQUE KEY uk_users_provider_login_id (login_provider, login_id),
    -- 동일 로그인 제공자의 동일 사용자가 중복 가입되는 것을 방지
    UNIQUE KEY uk_users_provider_key (login_provider, provider_key)

) COMMENT '회원';
CREATE TABLE refresh_tokens
(
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Refresh Token ID',
    user_id    BIGINT       NOT NULL COMMENT '회원 ID',
    token_id   VARCHAR(100) NOT NULL COMMENT 'JWT 고유 식별값(jti)',
    token_hash CHAR(64)     NOT NULL COMMENT 'Refresh Token SHA-256 해시값',
    expires_at DATETIME     NOT NULL COMMENT '만료일시',
    revoked_at DATETIME     NULL COMMENT '폐기일시',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token_id (token_id),
    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_expires_at (expires_at),
    CONSTRAINT fk_refresh_tokens_users
        FOREIGN KEY (user_id) REFERENCES users (id)

) COMMENT 'Refresh Token';
CREATE TABLE phone_verifications
(
    id                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '휴대전화 인증 ID',
    request_id             VARCHAR(100) NOT NULL COMMENT '인증 요청 식별값(UUID)',
    phone_number           VARCHAR(30)  NOT NULL COMMENT '인증 대상 휴대전화번호',
    verification_purpose   VARCHAR(30)  NOT NULL COMMENT '인증 목적(SIGNUP/FIND_ID/RESET_PASSWORD)',
    verification_code_hash VARCHAR(255) NOT NULL COMMENT '인증번호 해시값',
    expires_at             DATETIME     NOT NULL COMMENT '인증번호 만료일시',
    verified_at            DATETIME     NULL COMMENT '인증 성공일시',
    used_at                DATETIME     NULL COMMENT '인증 결과 사용일시',
    attempt_count          INT          NOT NULL DEFAULT 0 COMMENT '인증번호 확인 시도 횟수',
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '인증 요청 생성일시',
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone_verifications_request_id (request_id),
    INDEX idx_phone_verifications_lookup (phone_number, verification_purpose, created_at),
    INDEX idx_phone_verifications_expiration (expires_at)
) COMMENT '휴대전화 인증';


-- 2. 통화
CREATE TABLE currencies
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '통화 ID',
    currency_code VARCHAR(3)   NOT NULL COMMENT '통화 코드(ISO 4217, 예: USD/KRW/EUR)',
    currency_name VARCHAR(100) NOT NULL COMMENT '통화명',
    symbol        VARCHAR(10)  NULL COMMENT '통화 기호',
    unit          INT          NOT NULL DEFAULT 1 COMMENT '환율 표시 기준 단위',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_currencies_code (currency_code)
) COMMENT '통화';


-- 3. 국가
CREATE TABLE countries
(
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '국가 ID',
    country_name VARCHAR(100) NOT NULL COMMENT '국가명',
    currency_id  BIGINT       NOT NULL COMMENT '기본 통화 ID',
    time_zone    VARCHAR(50)  NOT NULL COMMENT 'IANA 시간대(예: Asia/Seoul)',
    flag_url     VARCHAR(500) NULL COMMENT '국기 이미지 URL',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_countries_currency FOREIGN KEY (currency_id) REFERENCES currencies (id)
) COMMENT '국가';


-- 4. 지출 카테고리
CREATE TABLE spending_categories
(
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
    category_code VARCHAR(30) NOT NULL COMMENT '카테고리 식별 코드',
    category_name VARCHAR(50) NOT NULL COMMENT '카테고리명',
    display_order INT         NOT NULL DEFAULT 0 COMMENT '표시 순서',
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_spending_categories_code (category_code)
) COMMENT '지출 카테고리';


-- 5. 체크리스트 템플릿
CREATE TABLE checklist_templates
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '체크리스트 템플릿 ID',
    checklist_type VARCHAR(20)  NOT NULL COMMENT '체크리스트 유형(PRE_TRAVEL/RETURN)',
    dday_stage     VARCHAR(10)  NULL COMMENT 'D-Day 단계(D30/D7/D1)',
    item_name      VARCHAR(200) NOT NULL COMMENT '항목명',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id)
) COMMENT '체크리스트 템플릿';


-- 6. 지원 금융기관
CREATE TABLE supported_institutions
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '금융기관 ID',
    organization_code    VARCHAR(20)  NOT NULL COMMENT 'CODEF 기관코드',
    institution_name     VARCHAR(100) NOT NULL COMMENT '금융기관명',
    business_type        VARCHAR(10)  NOT NULL DEFAULT 'BK' COMMENT '업권 구분(BK:은행, CD:카드)',
    supported_auth_types VARCHAR(200) NULL COMMENT '지원 인증서 유형',
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '상태(ACTIVE/MAINTENANCE)',
    logo_url             VARCHAR(500) NULL COMMENT '로고 URL',
    display_order        INT          NOT NULL DEFAULT 0 COMMENT '표시 순서',
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '사용 여부',
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supported_institutions_code (organization_code)
) COMMENT '지원 금융기관';


-- 7. 환전 은행 지점
CREATE TABLE exchange_bank_branches
(
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '환전 은행 지점 ID',
    bank_name      VARCHAR(100)   NOT NULL COMMENT '은행명',
    branch_name    VARCHAR(150)   NOT NULL COMMENT '지점명',
    address        VARCHAR(500)   NULL COMMENT '주소',
    latitude       DECIMAL(10, 7) NULL COMMENT '위도',
    longitude      DECIMAL(10, 7) NULL COMMENT '경도',
    business_hours VARCHAR(200)   NULL COMMENT '영업시간',
    phone_number   VARCHAR(30)    NULL COMMENT '전화번호',
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE COMMENT '사용 여부',
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id)
) COMMENT '환전 은행 지점';


-- 8. 트래블카드
CREATE TABLE travel_cards
(
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '트래블카드 ID',
    card_name             VARCHAR(150)   NOT NULL COMMENT '카드명',
    card_company          VARCHAR(100)   NOT NULL COMMENT '카드사',
    bank_name             VARCHAR(100)   NULL COMMENT '발급/연계 은행',
    required_account      VARCHAR(150)   NULL COMMENT '해외 결제에 필요한 외화계좌/외화서비스',
    instant_use           BOOLEAN        NOT NULL DEFAULT FALSE COMMENT '기존 계좌/서비스 연결로 이용 가능 여부(별도 계좌 신규 개설 불필요)',
    applied_rate_info     VARCHAR(255)   NULL COMMENT '적용 환율 정보',
    settlement_type       VARCHAR(30)    NOT NULL COMMENT '해외 결제 통화 처리 방식(DIRECT/USD_CONVERSION)',
    foreign_currency_holding_limit VARCHAR(255) NULL COMMENT '연결 외화머니/외화계좌의 외화 보유한도',
    exchange_fee          VARCHAR(200)   NULL COMMENT '환전 수수료',
    re_exchange_fee       VARCHAR(200)   NULL COMMENT '재환전 수수료',
    payment_fee           VARCHAR(200)   NULL COMMENT '결제 수수료',
    withdrawal_fee        VARCHAR(200)   NULL COMMENT '출금 수수료',
    auto_charge_supported BOOLEAN        NOT NULL DEFAULT FALSE COMMENT '자동충전 지원 여부',
    is_transit_card       BOOLEAN        NOT NULL DEFAULT FALSE COMMENT '교통카드 기능 지원 여부(선택 발급 포함)',
    is_active             BOOLEAN        NOT NULL DEFAULT TRUE COMMENT '사용 여부',
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    is_deleted             TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '논리 삭제 여부',
    deleted_at             DATETIME       NULL COMMENT '논리 삭제일시',
    PRIMARY KEY (id)
) COMMENT '트래블카드';

CREATE TABLE travel_card_currencies
(
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '트래블카드 지원 통화 ID',
    card_id       BIGINT      NOT NULL COMMENT '트래블카드 ID',
    currency_code VARCHAR(3)  NOT NULL COMMENT '직접 보유/차감 지원 통화 코드(ISO 4217)',

    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    is_deleted   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '논리 삭제 여부',
    deleted_at   DATETIME   NULL COMMENT '논리 삭제일시',

    PRIMARY KEY (id),

    CONSTRAINT uq_travel_card_currencies_card_currency
        UNIQUE (card_id, currency_code),

    CONSTRAINT fk_travel_card_currencies_travel_card
        FOREIGN KEY (card_id)
            REFERENCES travel_cards (id),

    INDEX idx_travel_card_currencies_card_id (card_id)

) COMMENT '트래블카드 직접 지원 통화';


-- 9. 적금 상품
CREATE TABLE savings_products
(
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '적금 상품 ID',
    fin_co_no        VARCHAR(20)  NOT NULL COMMENT '금융회사 코드',
    kor_co_nm        VARCHAR(100) NOT NULL COMMENT '금융회사명',
    fin_prdt_cd      VARCHAR(100) NOT NULL COMMENT '금융상품 코드',
    fin_prdt_nm      VARCHAR(255) NOT NULL COMMENT '상품명',
    join_way         VARCHAR(255) NULL COMMENT '가입 방법',
    mtrt_int         TEXT         NULL COMMENT '만기 후 이자율',
    spcl_cnd         TEXT         NULL COMMENT '우대 조건',
    max_limit        BIGINT       NULL COMMENT '월 납입한도',
    is_travel_saving BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '여행 적금 여부',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_savings_products_code (fin_co_no, fin_prdt_cd)
) COMMENT '적금 상품';


-- 10. 적금 상품 옵션
CREATE TABLE savings_product_options
(
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '상품 옵션 ID',
    product_id        BIGINT        NOT NULL COMMENT '적금 상품 ID',
    intr_rate_type    VARCHAR(1)    NULL COMMENT '금리 유형(S:단리, M:복리)',
    intr_rate_type_nm VARCHAR(20)   NULL COMMENT '금리 유형명',
    rsrv_type         VARCHAR(1)    NULL COMMENT '적립 유형',
    rsrv_type_nm      VARCHAR(20)   NULL COMMENT '적립 유형명(정액/자유)',
    save_trm          INT           NOT NULL COMMENT '저축 기간(개월)',
    intr_rate         DECIMAL(7, 4) NULL COMMENT '기본 금리',
    intr_rate2        DECIMAL(7, 4) NULL COMMENT '최고 우대 금리',
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_savings_product_options_product FOREIGN KEY (product_id) REFERENCES savings_products (id)
) COMMENT '적금 상품 옵션';


-- 11. CODEF 연동
CREATE TABLE codef_connections
(
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'CODEF 연동 ID',
    user_id           BIGINT       NOT NULL COMMENT '회원 ID',
    connected_id      VARCHAR(255) NOT NULL COMMENT 'CODEF connectedId(암호화 저장)',
    connection_status VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '연동 상태',
    last_synced_at    TIMESTAMP    NULL COMMENT '최근 동기화 일시',
    is_deleted        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at        DATETIME     NULL COMMENT '삭제일시',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_codef_connections_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT 'CODEF 연동';


-- 12. CODEF 연동 기관
CREATE TABLE codef_connected_institutions
(
    id                           BIGINT       NOT NULL AUTO_INCREMENT,
    codef_connection_id          BIGINT       NOT NULL COMMENT 'CODEF 연동 ID',
    organization_code            VARCHAR(20)  NOT NULL,
    organization_name            VARCHAR(100) NULL,
    bank_codef_name              VARCHAR(100) NULL,
    business_type                VARCHAR(10)  NOT NULL DEFAULT 'BK',
    connection_status            VARCHAR(20)  NOT NULL DEFAULT 'CONNECTED',
    last_registration_checked_at TIMESTAMP    NULL,
    last_synced_at               TIMESTAMP    NULL,
    is_active                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_codef_connected_institutions_conn_org_type (codef_connection_id, organization_code, business_type),
    CONSTRAINT fk_codef_connected_institutions_connection FOREIGN KEY (codef_connection_id) REFERENCES codef_connections (id)
) COMMENT 'CODEF 연동 기관';


-- 13. 계좌
CREATE TABLE accounts
(
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT '계좌 ID',
    user_id                 BIGINT         NOT NULL COMMENT '회원 ID',
    codef_connection_id     BIGINT         NULL COMMENT 'CODEF 연동 ID',
    organization_code       VARCHAR(20)    NULL COMMENT 'CODEF 기관코드',
    account_name            VARCHAR(150)   NOT NULL COMMENT '계좌명',
    account_number          VARCHAR(255)   NULL COMMENT '계좌번호(암호화)',
    account_type            VARCHAR(20)    NOT NULL COMMENT '계좌 유형(CHECKING/DEPOSIT/SAVING/FOREIGN)',
    balance                 DECIMAL(18, 2) NULL COMMENT '잔액',
    withdrawable_amount     DECIMAL(18, 2) NULL COMMENT '출금가능액',
    recognized_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '여행 자금 인정 금액',
    interest_rate           DECIMAL(7, 4)  NULL,
    maturity_date           DATE           NULL,
    is_travel_fund_included BOOLEAN        NOT NULL DEFAULT FALSE,
    connection_type         VARCHAR(20)    NOT NULL DEFAULT 'CODEF' COMMENT '연결 유형(CODEF/DEMO/MANUAL)',
    external_account_key    VARCHAR(255)   NULL,
    last_synced_at          TIMESTAMP      NULL,
    is_active               BOOLEAN        NOT NULL DEFAULT TRUE COMMENT '연동 활성 여부',
    is_deleted              TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at              DATETIME       NULL COMMENT '삭제일시',
    created_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_user_org_number (user_id, organization_code, account_number),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_accounts_codef_connection FOREIGN KEY (codef_connection_id) REFERENCES codef_connections (id)
) COMMENT '계좌';


-- 14. 연동 카드
CREATE TABLE cards
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '카드 ID',
    user_id             BIGINT       NOT NULL                COMMENT '회원 ID',
    codef_connection_id BIGINT       NULL                    COMMENT 'CODEF 연동 ID',
    card_name           VARCHAR(150) NOT NULL                COMMENT '카드명',
    masked_card_number  VARCHAR(30)  NULL                    COMMENT '마스킹된 카드번호',
    card_type           VARCHAR(20)  NOT NULL DEFAULT 'CREDIT' COMMENT '카드 유형(CREDIT:신용/CHECK:체크)',
    organization_code   VARCHAR(20)  NULL                    COMMENT 'CODEF 기관코드',
    payment_account_number VARCHAR(50) NULL                  COMMENT '카드 결제계좌 번호(CODEF 응답 기준)',
    linked_account_id   BIGINT       NULL                    COMMENT '서비스에 연동된 카드 결제계좌 ID',
    last_synced_at      TIMESTAMP    NULL                    COMMENT '마지막 거래내역 동기화 시각',
    is_deleted          TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at          DATETIME     NULL                    COMMENT '삭제일시',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cards_user_masked_number (user_id, masked_card_number),
    KEY idx_cards_linked_account (linked_account_id),
    CONSTRAINT fk_cards_user             FOREIGN KEY (user_id)             REFERENCES users (id),
    CONSTRAINT fk_cards_codef_connection FOREIGN KEY (codef_connection_id) REFERENCES codef_connections (id),
    CONSTRAINT fk_cards_linked_account    FOREIGN KEY (linked_account_id)   REFERENCES accounts (id)
) COMMENT '연동 카드';


-- 15. 여행
CREATE TABLE trips
(
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    user_id             BIGINT         NOT NULL,
    trip_name           VARCHAR(150)   NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'PLANNING' COMMENT '여행 상태(PLANNING/TRAVELING/ENDED/ARCHIVED)',
    start_date          DATE           NULL,
    end_date            DATE           NULL,
    total_target_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    savings_tracking_started_at DATETIME NULL COMMENT '여행+계좌 둘 다 등록되어 여행 저축 집계가 시작된 시각',
    wallet_reflect_resolved TINYINT(1) NOT NULL DEFAULT 0 COMMENT '집계 시작 시점 월렛 잔액 반영 여부 프롬프트 처리 완료 여부',
    start_report_viewed_at DATETIME NULL COMMENT '여행 시작 저축 리포트 팝업 확인 시각',
    is_deleted          TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at          DATETIME       NULL COMMENT '삭제일시',
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_trips_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '여행';


-- 15. 월렛
CREATE TABLE wallet
(
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '월렛 ID',
    user_id        BIGINT         NOT NULL COMMENT '회원 ID',
    balance_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '월렛 원화 잔액',
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '월렛 상태(ACTIVE/INACTIVE/CLOSED)',
    version        BIGINT         NOT NULL DEFAULT 0 COMMENT '동시성 제어 버전',
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_user (user_id),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '월렛';


-- 16. 월렛 연동 계좌
CREATE TABLE wallet_account
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '월렛 연동 계좌 ID',
    wallet_id   BIGINT      NOT NULL COMMENT '월렛 ID',
    account_id  BIGINT      NOT NULL COMMENT '연동 계좌 ID',
    is_primary  BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '대표 계좌 여부',
    status      VARCHAR(20) NOT NULL DEFAULT 'LINKED' COMMENT '연동 상태(LINKED/UNLINKED)',
    linked_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '연동일시',
    unlinked_at DATETIME    NULL COMMENT '연동해제일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_account_wallet_account (wallet_id, account_id),
    CONSTRAINT fk_wallet_account_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_wallet_account_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) COMMENT '월렛 연동 계좌';


-- 16-1. 월렛 최근 출금 계좌
CREATE TABLE wallet_withdraw_recipient
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '최근 출금 계좌 ID',
    user_id             BIGINT       NOT NULL COMMENT '회원 ID',
    bank_code           VARCHAR(20)  NOT NULL COMMENT '금융기관 코드',
    bank_name           VARCHAR(100) NOT NULL COMMENT '금융기관명',
    account_number      VARCHAR(100) NOT NULL COMMENT '수취 계좌번호',
    account_holder_name VARCHAR(100) NULL COMMENT '예금주명',
    last_used_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최근 출금 시각',
    use_count           INT          NOT NULL DEFAULT 1 COMMENT '출금 사용 횟수',
    is_deleted          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at          DATETIME     NULL COMMENT '삭제일시',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_withdraw_recipient_user_account (user_id, bank_code, account_number),
    KEY idx_wallet_withdraw_recipient_recent (user_id, last_used_at),
    CONSTRAINT fk_wallet_withdraw_recipient_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '월렛 최근 출금 계좌';


-- 17. 월렛 원화 원장
CREATE TABLE wallet_ledger
(
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '월렛 원장 ID',
    wallet_id         BIGINT         NOT NULL COMMENT '월렛 ID',
    trip_id           BIGINT         NULL COMMENT '이 거래가 귀속되는 여행 ID(집계 시작 전 거래는 NULL)',
    direction         VARCHAR(10)    NOT NULL COMMENT '입출금 방향(IN/OUT)',
    transaction_type  VARCHAR(30)    NOT NULL COMMENT '거래 유형(CHARGE/WITHDRAW/CARD_TOPUP/EXCHANGE_SELL/MISSION_REWARD/REFUND/ADJUST)',
    transfer_method   VARCHAR(30)    NULL COMMENT '충전 방식(MANUAL/AUTO_SAVING)',
    amount            DECIMAL(18, 2) NOT NULL COMMENT '원화 거래 금액',
    balance_before    DECIMAL(18, 2) NOT NULL COMMENT '거래 전 월렛 원화 잔액',
    balance_after     DECIMAL(18, 2) NOT NULL COMMENT '거래 후 월렛 원화 잔액',
    source_type       VARCHAR(30)    NOT NULL COMMENT '출발 대상 유형(ACCOUNT/WALLET/TRAVEL_CARD/MISSION/SYSTEM)',
    source_id         BIGINT         NULL COMMENT '출발 대상 ID',
    target_type       VARCHAR(30)    NOT NULL COMMENT '도착 대상 유형(ACCOUNT/WALLET/TRAVEL_CARD/SYSTEM)',
    target_id         BIGINT         NULL COMMENT '도착 대상 ID',
    idempotency_key   VARCHAR(100)   NULL COMMENT '중복 요청 방지 키',
    memo              VARCHAR(500)   NULL COMMENT '메모',
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_ledger_idempotency (idempotency_key),
    KEY idx_wallet_ledger_trip (trip_id),
    CONSTRAINT fk_wallet_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_wallet_ledger_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
) COMMENT '월렛 원화 원장';


-- 18. 월렛 자동 저축 설정
CREATE TABLE wallet_auto_saving_rule
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '자동 저축 설정 ID',
    wallet_id          BIGINT         NOT NULL COMMENT '월렛 ID',
    source_account_id  BIGINT         NOT NULL COMMENT '자동 송금 출금 계좌 ID',
    amount             DECIMAL(18, 2) NOT NULL COMMENT '자동 송금 금액',
    day_of_month       INT            NOT NULL COMMENT '자동 송금일(1~28)',
    enabled            BOOLEAN        NOT NULL DEFAULT TRUE COMMENT '활성 여부',
    next_transfer_date DATE           NULL COMMENT '다음 송금 예정일',
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_auto_saving_wallet (wallet_id),
    CONSTRAINT chk_wallet_auto_saving_day CHECK (day_of_month BETWEEN 1 AND 28),
    CONSTRAINT fk_wallet_auto_saving_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_wallet_auto_saving_account FOREIGN KEY (source_account_id) REFERENCES accounts (id)
) COMMENT '월렛 자동 저축 설정';


-- 18-1. 월렛 자동 채우기 실행 기록(성공/실패)
CREATE TABLE wallet_auto_saving_logs
(
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '자동 채우기 실행 기록 ID',
    wallet_id   BIGINT         NOT NULL COMMENT '월렛 ID',
    status      VARCHAR(10)    NOT NULL COMMENT '실행 결과(SUCCESS/FAILED)',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '자동 송금 시도 금액',
    reason      VARCHAR(200)   NULL COMMENT '실패 사유(실패 시에만)',
    executed_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '실행 일시',
    PRIMARY KEY (id),
    CONSTRAINT fk_wallet_auto_saving_logs_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id)
) COMMENT '월렛 자동 채우기 성공/실패 기록';


-- 19. 사용자 보유 트래블카드
CREATE TABLE user_travel_cards
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사용자 보유 트래블카드 ID',
    user_id            BIGINT       NOT NULL COMMENT '회원 ID',
    travel_card_id     BIGINT       NULL COMMENT '트래블카드 상품 ID',
    card_name          VARCHAR(150) NOT NULL COMMENT '카드명',
    issuer_name        VARCHAR(100) NOT NULL COMMENT '카드사명',
    masked_card_number VARCHAR(50)  NOT NULL COMMENT '마스킹 카드번호',
    brand_name         VARCHAR(50)  NULL COMMENT '카드 브랜드/서비스명',
    card_color         VARCHAR(20)  NULL COMMENT '화면 표시용 카드 색상',
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '보유 카드 상태(ACTIVE/INACTIVE/EXPIRED)',
    external_card_key  VARCHAR(255) NULL COMMENT '외부/목데이터 카드 식별 키',
    is_deleted         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at         DATETIME     NULL COMMENT '삭제일시',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_travel_cards_user_external (user_id, external_card_key),
    CONSTRAINT fk_user_travel_cards_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_travel_cards_travel_card FOREIGN KEY (travel_card_id) REFERENCES travel_cards (id)
) COMMENT '사용자 보유 트래블카드';


-- 20. 월렛 트래블카드 연동
CREATE TABLE wallet_travel_card
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '월렛 트래블카드 연동 ID',
    wallet_id          BIGINT       NOT NULL COMMENT '월렛 ID',
    user_travel_card_id BIGINT      NULL COMMENT '사용자 보유 트래블카드 ID',
    travel_card_id     BIGINT       NOT NULL COMMENT '트래블카드 ID',
    card_name          VARCHAR(150) NOT NULL COMMENT '카드명',
    issuer_name        VARCHAR(100) NOT NULL COMMENT '카드사명',
    masked_card_number VARCHAR(50)  NULL COMMENT '마스킹 카드번호',
    status             VARCHAR(20)  NOT NULL DEFAULT 'LINKED' COMMENT '카드 연동 상태(LINKED/UNLINKED/INACTIVE)',
    linked_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '연동일시',
    unlinked_at        DATETIME     NULL COMMENT '연동해제일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_travel_card_wallet_card (wallet_id, travel_card_id),
    UNIQUE KEY uk_wallet_travel_card_user_card (wallet_id, user_travel_card_id),
    CONSTRAINT fk_wallet_travel_card_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_wallet_travel_card_user_card FOREIGN KEY (user_travel_card_id) REFERENCES user_travel_cards (id),
    CONSTRAINT fk_wallet_travel_card_card FOREIGN KEY (travel_card_id) REFERENCES travel_cards (id)
) COMMENT '월렛 트래블카드 연동';


-- 21. 트래블카드 외화 잔액
CREATE TABLE travel_card_balance
(
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '트래블카드 외화 잔액 ID',
    wallet_travel_card_id BIGINT         NOT NULL COMMENT '월렛 트래블카드 연동 ID',
    currency_code         VARCHAR(3)     NOT NULL COMMENT '통화 코드',
    balance_amount        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '보유 외화 금액',
    krw_estimated_amount  DECIMAL(18, 2) NULL COMMENT '원화 환산 추정 금액',
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_travel_card_balance_card_currency (wallet_travel_card_id, currency_code),
    CONSTRAINT fk_travel_card_balance_card FOREIGN KEY (wallet_travel_card_id) REFERENCES wallet_travel_card (id),
    CONSTRAINT fk_travel_card_balance_currency FOREIGN KEY (currency_code) REFERENCES currencies (currency_code)
) COMMENT '트래블카드 외화 잔액';


-- 22. 트래블카드 외화 원장
CREATE TABLE travel_card_ledger
(
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '트래블카드 외화 원장 ID',
    wallet_travel_card_id BIGINT         NOT NULL COMMENT '월렛 트래블카드 연동 ID',
    currency_code         VARCHAR(3)     NOT NULL COMMENT '통화 코드',
    direction             VARCHAR(10)    NOT NULL COMMENT '입출금 방향(IN/OUT)',
    transaction_type      VARCHAR(30)    NOT NULL COMMENT '외화 거래 유형(CARD_TOPUP/CARD_WITHDRAW/REFUND/ADJUST)',
    foreign_amount        DECIMAL(18, 2) NOT NULL COMMENT '외화 거래 금액',
    balance_before        DECIMAL(18, 2) NOT NULL COMMENT '거래 전 외화 잔액',
    balance_after         DECIMAL(18, 2) NOT NULL COMMENT '거래 후 외화 잔액',
    source_type           VARCHAR(30)    NOT NULL COMMENT '출발 대상 유형(WALLET/TRAVEL_CARD/SYSTEM)',
    source_id             BIGINT         NULL COMMENT '출발 대상 ID',
    target_type           VARCHAR(30)    NOT NULL COMMENT '도착 대상 유형(WALLET/TRAVEL_CARD/SYSTEM)',
    target_id             BIGINT         NULL COMMENT '도착 대상 ID',
    idempotency_key       VARCHAR(100)   NULL COMMENT '중복 요청 방지 키',
    memo                  VARCHAR(500)   NULL COMMENT '메모',
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_travel_card_ledger_idempotency (idempotency_key),
    CONSTRAINT fk_travel_card_ledger_card FOREIGN KEY (wallet_travel_card_id) REFERENCES wallet_travel_card (id),
    CONSTRAINT fk_travel_card_ledger_currency FOREIGN KEY (currency_code) REFERENCES currencies (currency_code)
) COMMENT '트래블카드 외화 원장';


-- 23. 월렛 카드 외화 충전 요청
CREATE TABLE wallet_card_topup
(
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '카드 충전 ID',
    wallet_id             BIGINT         NOT NULL COMMENT '월렛 ID',
    wallet_travel_card_id BIGINT         NOT NULL COMMENT '월렛 트래블카드 연동 ID',
    currency_code         VARCHAR(3)     NOT NULL COMMENT '충전 통화 코드',
    krw_amount            DECIMAL(18, 2) NOT NULL COMMENT '월렛에서 차감할 원화 금액',
    foreign_amount        DECIMAL(18, 2) NOT NULL COMMENT '카드에 충전할 외화 금액',
    status                VARCHAR(20)    NOT NULL DEFAULT 'REQUESTED' COMMENT '충전 상태(REQUESTED/PROCESSING/COMPLETED/FAILED/CANCELED/REFUNDED)',
    idempotency_key       VARCHAR(100)   NOT NULL COMMENT '중복 요청 방지 키',
    external_transaction_id VARCHAR(100) NULL COMMENT '목 카드사 외부 거래 ID',
    retry_count           INT            NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    last_tried_at         DATETIME       NULL COMMENT '마지막 시도일시',
    requested_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '충전 요청일시',
    completed_at          DATETIME       NULL COMMENT '충전 완료일시',
    failed_at             DATETIME       NULL COMMENT '충전 실패일시',
    failure_reason        VARCHAR(500)   NULL COMMENT '실패 사유',
    wallet_ledger_id      BIGINT         NULL COMMENT '월렛 원화 원장 ID',
    card_ledger_id        BIGINT         NULL COMMENT '트래블카드 외화 원장 ID',
    refunded              BOOLEAN        NOT NULL DEFAULT FALSE COMMENT '환불 처리 여부',
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_card_topup_idempotency (idempotency_key),
    CONSTRAINT fk_wallet_card_topup_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_wallet_card_topup_card FOREIGN KEY (wallet_travel_card_id) REFERENCES wallet_travel_card (id),
    CONSTRAINT fk_wallet_card_topup_wallet_ledger FOREIGN KEY (wallet_ledger_id) REFERENCES wallet_ledger (id),
    CONSTRAINT fk_wallet_card_topup_card_ledger FOREIGN KEY (card_ledger_id) REFERENCES travel_card_ledger (id),
    CONSTRAINT fk_wallet_card_topup_currency FOREIGN KEY (currency_code) REFERENCES currencies (currency_code)
) COMMENT '월렛 카드 외화 충전 요청';

-- 24. 월렛 환전 거래
CREATE TABLE wallet_exchange_transaction
(
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '환전 거래 ID',
    wallet_id             BIGINT         NOT NULL COMMENT '월렛 ID',
    wallet_travel_card_id BIGINT         NOT NULL COMMENT '월렛 트래블카드 연동 ID',
    currency_code         VARCHAR(3)     NOT NULL COMMENT '환전 통화 코드',
    exchange_type         VARCHAR(10)    NOT NULL COMMENT '환전 유형(BUY:원화→외화/SELL:외화→원화)',
    krw_amount            DECIMAL(18, 2) NOT NULL COMMENT '원화 금액',
    foreign_amount        DECIMAL(18, 2) NOT NULL COMMENT '외화 금액',
    base_exchange_rate    DECIMAL(20, 8) NOT NULL COMMENT '기준 환율',
    applied_exchange_rate DECIMAL(20, 8) NOT NULL COMMENT '적용 환율',
    fee_rate              DECIMAL(7, 4)  NULL COMMENT '수수료율',
    fee_amount            DECIMAL(18, 2) NULL COMMENT '수수료 금액',
    status                VARCHAR(20)    NOT NULL DEFAULT 'REQUESTED' COMMENT '환전 상태(REQUESTED/PROCESSING/COMPLETED/FAILED/CANCELED)',
    requested_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '환전 요청일시',
    completed_at          DATETIME       NULL COMMENT '환전 완료일시',
    failed_at             DATETIME       NULL COMMENT '환전 실패일시',
    failure_reason        VARCHAR(500)   NULL COMMENT '실패 사유',
    wallet_ledger_id      BIGINT         NULL COMMENT '원화 월렛 원장 ID',
    card_ledger_id        BIGINT         NULL COMMENT '트래블카드 외화 원장 ID',
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_wallet_exchange_transaction_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_wallet_exchange_transaction_card FOREIGN KEY (wallet_travel_card_id) REFERENCES wallet_travel_card (id),
    CONSTRAINT fk_wallet_exchange_transaction_currency FOREIGN KEY (currency_code) REFERENCES currencies (currency_code),
    CONSTRAINT fk_wallet_exchange_transaction_wallet_ledger FOREIGN KEY (wallet_ledger_id) REFERENCES wallet_ledger (id),
    CONSTRAINT fk_wallet_exchange_transaction_card_ledger FOREIGN KEY (card_ledger_id) REFERENCES travel_card_ledger (id)
) COMMENT '월렛 환전 거래';

-- 26. 여행 국가
CREATE TABLE trip_countries
(
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '여행 국가 ID',
    trip_id        BIGINT         NOT NULL COMMENT '여행 ID',
    country_id     BIGINT         NOT NULL COMMENT '국가 ID',
    arrival_date   DATE           NOT NULL COMMENT '도착일',
    departure_date DATE           NOT NULL COMMENT '출발일',
    target_budget  DECIMAL(18, 2) NOT NULL COMMENT '국가별 목표 예산',
    display_order  INT            NOT NULL DEFAULT 0 COMMENT '방문 순서',
    is_deleted     TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at     DATETIME       NULL COMMENT '삭제일시',
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_trip_countries_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_countries_country FOREIGN KEY (country_id) REFERENCES countries (id)
) COMMENT '여행 국가';


-- 26-1. TRIP 월렛
-- 서비스 내부의 가상 여행 저축 잔액을 관리한다.
CREATE TABLE trip_wallets
(
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'TRIP 월렛 ID',
    user_id       BIGINT         NOT NULL COMMENT '회원 ID',
    balance       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '현재 월렛 잔액',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trip_wallets_user (user_id),
    CONSTRAINT fk_trip_wallets_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT 'TRIP 월렛';


-- 26-2. 국가별 여행 예산 기준값
-- 조사한 기준 단가를 사용해 여행 예산을 재현 가능하게 계산한다.
CREATE TABLE country_budget_baselines
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '국가별 예산 기준값 ID',
    country_id         BIGINT         NOT NULL COMMENT '국가 ID',
    round_trip_airfare DECIMAL(18, 2) NOT NULL COMMENT '왕복 항공권 평균가(원화)',
    lodging_per_night  DECIMAL(18, 2) NOT NULL COMMENT '숙소 1박 평균가(원화)',
    food_per_day       DECIMAL(18, 2) NOT NULL COMMENT '1일 식비 평균(원화)',
    activity_per_day   DECIMAL(18, 2) NOT NULL COMMENT '1일 액티비티 평균(원화)',
    transport_per_day  DECIMAL(18, 2) NOT NULL COMMENT '1일 현지 교통비 평균(원화)',
    misc_per_day       DECIMAL(18, 2) NOT NULL COMMENT '1일 기타 평균(원화)',
    data_source        VARCHAR(500)   NULL COMMENT '조사 출처',
    reference_date     DATE           NOT NULL COMMENT '기준 조사일',
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_country_budget_baselines_country (country_id),
    CONSTRAINT fk_country_budget_baselines_country
        FOREIGN KEY (country_id) REFERENCES countries (id)
) COMMENT '국가별 여행 예산 기준값';


-- 26-3. 여행 국가별 예산 추천
-- 항공·숙소는 사전지출, 액티비티·식비·교통·기타는 여행 목표 금액에 포함한다.
CREATE TABLE trip_budget_recommendations
(
    id                         BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'AI 예산 추천 ID',
    trip_country_id            BIGINT         NOT NULL COMMENT '여행 국가 ID',
    traveler_count             INT            NOT NULL DEFAULT 1 COMMENT '여행 인원(현재 성인 1인 고정)',
    travel_style               VARCHAR(20)    NOT NULL DEFAULT 'MID_RANGE' COMMENT '여행 스타일',
    recommended_airfare_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'AI 추천 항공 사전지출',
    recommended_lodging_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'AI 추천 숙소 사전지출',
    recommended_activity_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'AI 추천 액티비티 현지지출',
    recommended_transport_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'AI 추천 교통비 현지지출',
    recommended_food_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'AI 추천 식비 현지지출',
    recommended_other_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'AI 추천 기타 현지지출',
    confirmed_airfare_amount   DECIMAL(18, 2) NULL COMMENT '사용자 확정 항공 사전지출',
    confirmed_lodging_amount   DECIMAL(18, 2) NULL COMMENT '사용자 확정 숙소 사전지출',
    confirmed_activity_amount  DECIMAL(18, 2) NULL COMMENT '사용자 확정 액티비티 현지지출',
    confirmed_transport_amount DECIMAL(18, 2) NULL COMMENT '사용자 확정 교통비 현지지출',
    confirmed_food_amount      DECIMAL(18, 2) NULL COMMENT '사용자 확정 식비 현지지출',
    confirmed_other_amount     DECIMAL(18, 2) NULL COMMENT '사용자 확정 기타 현지지출',
    ai_reason                  VARCHAR(1000) NULL COMMENT 'AI 추천 근거',
    ai_model                   VARCHAR(100)  NULL COMMENT '추천 생성 모델',
    is_confirmed               TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '사용자 확정 여부',
    created_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trip_budget_recommendations_country (trip_country_id),
    CONSTRAINT fk_trip_budget_recommendations_country
        FOREIGN KEY (trip_country_id) REFERENCES trip_countries (id)
) COMMENT '여행 국가별 AI 예산 추천';


-- 27. 거래 내역
CREATE TABLE transactions
(
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '거래 ID',
    account_id            BIGINT         NULL COMMENT '계좌 ID(카드 전용 거래는 NULL)',
    card_id               BIGINT         NULL COMMENT '카드 ID(계좌 거래는 NULL)',
    category_id           BIGINT         NULL COMMENT '카테고리 ID',
    category_source        VARCHAR(20)    NULL COMMENT '카테고리 분류 출처(USER/CODEF_TYPE/AI_MODEL/FALLBACK)',
    category_confidence    DECIMAL(5, 4)  NULL COMMENT '자동분류 신뢰도(0~1)',
    category_classified_at DATETIME       NULL COMMENT '카테고리 자동분류 시각',
    trip_id               BIGINT         NULL COMMENT '여행 ID',
    trip_country_id       BIGINT         NULL COMMENT '여행 국가 ID',
    currency_id           BIGINT         NULL COMMENT '현지 통화 ID',
    transaction_date      DATE           NOT NULL COMMENT '거래일자',
    transaction_time      TIME           NULL COMMENT '거래시각',
    transaction_type      VARCHAR(20)    NOT NULL COMMENT '거래 구분(DEPOSIT/WITHDRAWAL)',
    transaction_region    VARCHAR(20)    NOT NULL COMMENT '국내/해외 구분(DOMESTIC/OVERSEAS)',
    amount                DECIMAL(18, 2) NOT NULL COMMENT '거래 금액(원화 기준)',
    balance_after         DECIMAL(18, 2) NULL COMMENT '거래 후 잔액',
    merchant_name         VARCHAR(255)   NULL COMMENT '거래처',
    merchant_type         VARCHAR(100)   NULL COMMENT 'CODEF 가맹점 업종',
    original_amount       DECIMAL(18, 2) NULL COMMENT '현지 통화 금액',
    applied_exchange_rate DECIMAL(15, 4) NULL COMMENT '적용 환율',
    payment_method        VARCHAR(50)    NULL COMMENT '결제수단',
    is_pre_expense        BOOLEAN        NOT NULL DEFAULT FALSE COMMENT '사전지출 전환 여부',
    memo                  VARCHAR(500)   NULL COMMENT '메모',
    external_key          VARCHAR(255)   NOT NULL COMMENT '중복 수집 방지 키',
    is_deleted            TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at            DATETIME       NULL COMMENT '삭제일시',
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transactions_external_key (external_key),
    CONSTRAINT fk_transactions_account  FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transactions_card     FOREIGN KEY (card_id)    REFERENCES cards (id),
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES spending_categories (id),
    CONSTRAINT fk_transactions_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_transactions_trip_country FOREIGN KEY (trip_country_id) REFERENCES trip_countries (id),
    CONSTRAINT fk_transactions_currency FOREIGN KEY (currency_id) REFERENCES currencies (id)
) COMMENT '거래 내역';


-- 28. 환율
-- base_currency_id: 기준 통화(KRW), target_currency_id: 대상 통화(USD 등)
CREATE TABLE exchange_rates
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '환율 ID',
    base_currency_id   BIGINT         NOT NULL COMMENT '기준 통화 ID(KRW)',
    target_currency_id BIGINT         NOT NULL COMMENT '대상 통화 ID(USD 등)',
    currency_unit      INT            NOT NULL DEFAULT 1 COMMENT '통화 단위',
    deal_base_rate     DECIMAL(20, 8) NOT NULL COMMENT '매매기준율',
    prev_rate          DECIMAL(20, 8) NULL COMMENT '전일 매매기준율',
    rate_date          DATE           NOT NULL COMMENT '환율 기준일',
    fetched_at         TIMESTAMP      NOT NULL COMMENT 'API 수신 시각',
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exchange_rates_target_date (target_currency_id, rate_date),
    CONSTRAINT fk_exchange_rates_base_currency FOREIGN KEY (base_currency_id) REFERENCES currencies (id),
    CONSTRAINT fk_exchange_rates_target_currency FOREIGN KEY (target_currency_id) REFERENCES currencies (id)
) COMMENT '환율';


-- 29. 관심 환율 알림
CREATE TABLE exchange_rate_alerts
(
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '관심 환율 알림 ID',
    user_id       BIGINT         NOT NULL COMMENT '회원 ID',
    currency_id   BIGINT         NOT NULL COMMENT '관심 통화 ID',
    target_rate   DECIMAL(20, 8) NULL COMMENT '목표 환율',
    is_deleted    TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at    DATETIME       NULL COMMENT '삭제일시',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_exchange_rate_alerts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_exchange_rate_alerts_currency FOREIGN KEY (currency_id) REFERENCES currencies (id)
) COMMENT '관심 환율 알림';


-- 30. 저축 계획
CREATE TABLE saving_plans
(
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '저축 계획 ID',
    trip_id        BIGINT         NOT NULL COMMENT '여행 ID',
    account_id     BIGINT         NULL COMMENT '저축 계좌 ID',
    monthly_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '월 저축 금액',
    goal_status    VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '상태(ACTIVE/COMPLETED/CANCELLED)',
    is_deleted     TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at     DATETIME       NULL COMMENT '삭제일시',
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_saving_plans_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_saving_plans_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) COMMENT '저축 계획';


-- 31. 카테고리 예산
CREATE TABLE category_budgets
(
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    user_id       BIGINT         NOT NULL,
    category_id   BIGINT         NOT NULL,
    target_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    is_deleted    TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at    DATETIME       NULL COMMENT '삭제일시',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_budgets_user_category (user_id, category_id),
    CONSTRAINT fk_category_budgets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_category_budgets_category FOREIGN KEY (category_id) REFERENCES spending_categories (id)
) COMMENT '카테고리 예산';


-- 32. 수입 출처
CREATE TABLE income_sources
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    user_id      BIGINT         NOT NULL,
    account_id   BIGINT         NOT NULL,
    payment_name VARCHAR(150)   NOT NULL,
    payment_day  INT            NOT NULL COMMENT '급여일(1~31)',
    memo         VARCHAR(500)   NULL,
    gross_amount DECIMAL(18, 2) NOT NULL,
    is_deleted   TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at   DATETIME       NULL COMMENT '삭제일시',
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_income_sources_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_income_sources_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) COMMENT '수입 출처';


-- 33. 고정 지출
CREATE TABLE fixed_expenses
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    user_id      BIGINT         NOT NULL,
    account_id   BIGINT         NULL,
    expense_name VARCHAR(150)   NOT NULL COMMENT '항목명(월세/통신비 등)',
    amount       DECIMAL(18, 2) NOT NULL,
    payment_day  INT            NOT NULL COMMENT '납부일(1~31)',
    memo         VARCHAR(500)   NULL,
    is_deleted   TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at   DATETIME       NULL COMMENT '삭제일시',
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_fixed_expenses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_fixed_expenses_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) COMMENT '고정 지출';


-- 34. 금융 일정
CREATE TABLE financial_schedules
(
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '금융 일정 ID',
    user_id          BIGINT      NOT NULL COMMENT '회원 ID',
    fixed_expense_id BIGINT      NULL,
    income_source_id BIGINT      NULL,
    schedule_type    VARCHAR(20) NOT NULL COMMENT '일정 유형(INCOME/EXPENSE)',
    is_deleted       TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at       DATETIME    NULL COMMENT '삭제일시',
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_financial_schedules_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_financial_schedules_fixed_expense FOREIGN KEY (fixed_expense_id) REFERENCES fixed_expenses (id),
    CONSTRAINT fk_financial_schedules_income_source FOREIGN KEY (income_source_id) REFERENCES income_sources (id)
) COMMENT '금융 일정';


-- 35. 여행 일정
CREATE TABLE trip_schedules
(
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '여행 일정 ID',
    trip_id         BIGINT         NOT NULL COMMENT '여행 ID',
    trip_country_id BIGINT         NULL COMMENT '여행 국가 ID',
    currency_id     BIGINT         NULL COMMENT '통화 ID',
    schedule_name   VARCHAR(200)   NOT NULL COMMENT '일정명',
    scheduled_at    TIMESTAMP      NOT NULL COMMENT '일정 시작 일시',
    amount          DECIMAL(18, 2) NULL COMMENT '현지 통화 금액',
    payment_status  VARCHAR(20)    NOT NULL DEFAULT 'UNDECIDED' COMMENT '결제 상태(PREPAID/ONSITE/UNDECIDED)',
    schedule_status VARCHAR(20)    NOT NULL DEFAULT 'UPCOMING' COMMENT '진행 상태(UPCOMING/DONE)',
    place_name      VARCHAR(200)   NULL COMMENT '장소명',
    place_address   VARCHAR(500)   NULL COMMENT '장소 주소',
    memo            VARCHAR(1000)  NULL COMMENT '메모',
    is_deleted      TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at      DATETIME       NULL COMMENT '삭제일시',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_trip_schedules_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_schedules_trip_country FOREIGN KEY (trip_country_id) REFERENCES trip_countries (id),
    CONSTRAINT fk_trip_schedules_currency FOREIGN KEY (currency_id) REFERENCES currencies (id)
) COMMENT '여행 일정';


-- 36. 여행 체크리스트
CREATE TABLE trip_checklist_items
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '여행 체크리스트 항목 ID',
    trip_id         BIGINT       NOT NULL COMMENT '여행 ID',
    template_id     BIGINT       NULL COMMENT '템플릿 ID(사용자 추가 항목은 NULL)',
    checklist_type  VARCHAR(20)  NOT NULL COMMENT '체크리스트 유형(PRE_TRAVEL/RETURN)',
    dday_stage      VARCHAR(10)  NULL COMMENT 'D-Day 단계(D30/D7/D1)',
    item_name       VARCHAR(200) NOT NULL COMMENT '항목명',
    is_completed    BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '완료 여부',
    is_excluded     BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '기본 항목 제외 여부',
    is_custom       BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '사용자 추가 항목 여부',
    is_carried_over BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '이전 단계 미완료 이월 여부',
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at      DATETIME     NULL COMMENT '삭제일시',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_trip_checklist_items_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_checklist_items_template FOREIGN KEY (template_id) REFERENCES checklist_templates (id)
) COMMENT '여행 체크리스트';


-- 37. 사전 지출
CREATE TABLE pre_expenses
(
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    trip_id         BIGINT         NOT NULL COMMENT '여행 ID',
    transaction_id  BIGINT         NOT NULL COMMENT '원본 거래 ID',
    trip_country_id BIGINT         NULL COMMENT '대상 여행 국가 ID(공통은 NULL)',
    scope           VARCHAR(10)    NOT NULL COMMENT '적용 구분(COMMON/COUNTRY)',
    direct_amount   DECIMAL(18, 2) NOT NULL COMMENT '직접 사전지출액',
    is_deleted      TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at      DATETIME       NULL COMMENT '삭제일시',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_pre_expenses_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_pre_expenses_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id),
    CONSTRAINT fk_pre_expenses_trip_country FOREIGN KEY (trip_country_id) REFERENCES trip_countries (id)
) COMMENT '사전 지출';


-- 38. 해외 영수증
CREATE TABLE receipts
(
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '해외 영수증 ID',
    user_id       BIGINT         NOT NULL COMMENT '회원 ID',
    trip_id       BIGINT         NOT NULL COMMENT '여행 ID',
    country_id    BIGINT         NOT NULL COMMENT '여행 국가 ID',
    category_id   BIGINT         NOT NULL COMMENT '지출 카테고리 ID',
    currency_id   BIGINT         NOT NULL COMMENT '통화 ID',
    payment_datetime DATETIME NOT NULL COMMENT '결제일시',
    file_name     VARCHAR(255)   NULL COMMENT '파일명',
    file_url      VARCHAR(1000)  NULL COMMENT '파일 URL',
    file_type     VARCHAR(10)    NULL COMMENT '파일 형식(JPG/JPEG/PNG, 10MB 이하)',
    memo          VARCHAR(500)   NULL COMMENT '영수증 메모',
    status        VARCHAR(20)    NOT NULL DEFAULT 'UPLOADED' COMMENT '처리 상태(UPLOADED/PROCESSING/COMPLETED/FAILED)',
    merchant_original_name   VARCHAR(255) NULL COMMENT '원문 상호명',
    merchant_translated_name VARCHAR(255) NULL COMMENT '번역 상호명',
    total_amount  DECIMAL(15, 2) NOT NULL COMMENT '현지 총액',
    ocr_raw_text  TEXT           NULL COMMENT 'OCR 원문 텍스트',
    split_count INT NOT NULL DEFAULT 1 COMMENT '금액 분할 인원수',
    error_message TEXT           NULL COMMENT '오류 메시지',
    is_deleted    TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at    DATETIME       NULL COMMENT '삭제일시',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    processed_at  DATETIME      NULL COMMENT 'OCR 처리 완료 일시',
    PRIMARY KEY (id),
    INDEX idx_receipts_user_datetime
        (user_id, is_deleted, payment_datetime),

    INDEX idx_receipts_trip_datetime
        (trip_id, is_deleted, payment_datetime),
    CONSTRAINT chk_receipts_split_count CHECK (split_count >= 1),
    CONSTRAINT chk_receipts_file_info
        CHECK (
            (file_name IS NULL AND file_url IS NULL AND file_type IS NULL)
                OR
            (file_name IS NOT NULL AND file_url IS NOT NULL AND file_type IS NOT NULL)
        ),
    CONSTRAINT fk_receipts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_receipts_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_receipts_country FOREIGN KEY (country_id) REFERENCES countries (id),
    CONSTRAINT fk_receipts_currency FOREIGN KEY (currency_id) REFERENCES currencies (id),
    CONSTRAINT fk_receipts_category FOREIGN KEY (category_id) REFERENCES spending_categories (id)
) COMMENT '해외 영수증';
-- 공동결제 참여자 테이블 --
CREATE TABLE receipt_participants
(
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '영수증 공동결제 참여자 ID',
    receipt_id       BIGINT       NOT NULL COMMENT '해외 영수증 ID',
    participant_name VARCHAR(100) NOT NULL COMMENT '로그인 회원을 제외한 공동결제 참여자 이름',
    display_order    INT          NOT NULL COMMENT '표시 순서',
    is_deleted       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at       DATETIME     NULL COMMENT '삭제일시',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',

    PRIMARY KEY (id),

    INDEX idx_receipt_participants_receipt
        (receipt_id, is_deleted, display_order),

    CONSTRAINT fk_receipt_participants_receipt
        FOREIGN KEY (receipt_id)
            REFERENCES receipts (id)
) COMMENT '영수증 공동결제 참여자';


-- 39. 영수증 품목
CREATE TABLE receipt_items
(
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '영수증 품목 ID',
    receipt_id      BIGINT         NOT NULL COMMENT '해외 영수증 ID',
    original_name   VARCHAR(255)   NULL COMMENT '원문 품목명',
    translated_name VARCHAR(255)   NULL COMMENT '번역 품목명',
    quantity        INT            NULL COMMENT '수량',
    amount          DECIMAL(15, 2) NULL COMMENT '현지 금액',
    display_order   INT            NOT NULL COMMENT '표시 순서',
    is_deleted      TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at      DATETIME       NULL COMMENT '삭제일시',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    INDEX idx_receipt_items_receipt (receipt_id, is_deleted, display_order),
    CONSTRAINT fk_receipt_items_receipt FOREIGN KEY (receipt_id) REFERENCES receipts (id)
) COMMENT '영수증 품목';


-- 40. 여행 리포트
CREATE TABLE trip_reports
(
    id                        BIGINT         NOT NULL AUTO_INCREMENT COMMENT '여행 리포트 ID',
    trip_id                   BIGINT         NOT NULL COMMENT '여행 ID',
    report_type               VARCHAR(20)    NOT NULL COMMENT '리포트 유형(PRE_TRAVEL/FINAL)',
    total_expense             DECIMAL(18, 2) NULL COMMENT '총 지출',
    target_budget             DECIMAL(18, 2) NULL COMMENT '목표 예산',
    saved_amount              DECIMAL(18, 2) NULL COMMENT '절감액',
    pre_expense_total         DECIMAL(18, 2) NULL COMMENT '사전 지출 합계',
    available_fund            DECIMAL(18, 2) NULL COMMENT '출국 시 사용 가능 자금',
    goal_achievement_rate     DECIMAL(5, 2)  NULL COMMENT '목표 달성률(%)',
    checklist_completion_rate DECIMAL(5, 2)  NULL COMMENT '체크리스트 완료율(%)',
    breakdown_data            TEXT           NULL COMMENT '카테고리별·국가별 지출 상세(JSON)',
    pdf_url                   VARCHAR(1000)  NULL COMMENT 'PDF 파일 URL',
    generated_at              TIMESTAMP      NOT NULL COMMENT '리포트 생성일시',
    created_at                TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at                TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_trip_reports_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
) COMMENT '여행 리포트';


-- 41. 알림
CREATE TABLE notifications
(
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '알림 ID',
    user_id           BIGINT       NOT NULL COMMENT '회원 ID',
    notification_type VARCHAR(30)  NOT NULL COMMENT '알림 유형',
    title             VARCHAR(200) NOT NULL COMMENT '제목',
    message           TEXT         NOT NULL COMMENT '내용',
    url               VARCHAR(500) NULL COMMENT '알림 클릭 시 이동 URL',
    reference_id      BIGINT       NULL COMMENT '참조 ID (여행ID, 일정ID 등)',
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '읽음 여부',
    is_deleted        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at        DATETIME     NULL COMMENT '삭제일시',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '알림';


-- 42. 알림 설정
CREATE TABLE notification_settings
(
    id                         BIGINT    NOT NULL AUTO_INCREMENT COMMENT '알림 설정 ID',
    user_id                    BIGINT    NOT NULL COMMENT '회원 ID',
    all_enabled                BOOLEAN   NOT NULL DEFAULT TRUE COMMENT '전체 알림 사용 여부',
    travel_schedule_enabled    BOOLEAN   NOT NULL DEFAULT TRUE COMMENT '여행 일정 알림',
    exchange_rate_enabled      BOOLEAN   NOT NULL DEFAULT TRUE COMMENT '환율 알림',
    checklist_enabled          BOOLEAN   NOT NULL DEFAULT TRUE COMMENT '체크리스트 알림',
    travel_report_enabled      BOOLEAN   NOT NULL DEFAULT TRUE COMMENT '여행 레포트 알림',
    updated_at                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_settings_user (user_id),
    CONSTRAINT fk_notification_settings_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '알림 설정';

-- 43. 환전 시장 데이터
CREATE TABLE exchange_market_data
(
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'ID',
    currency_id   BIGINT         NOT NULL COMMENT '통화 ID',
    unit          INT            NOT NULL DEFAULT 1 COMMENT '통화 단위 (예: 100)',
    base_rate     DECIMAL(20, 8) NOT NULL COMMENT '매매기준율 (Raw)',
    buy_rate      DECIMAL(20, 8) NOT NULL COMMENT '살 때 환율 (Raw)',
    buy_fee_rate  DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '살 때 수수료율 (%)',
    sell_rate     DECIMAL(20, 8) NOT NULL COMMENT '팔 때 환율 (Raw)',
    sell_fee_rate DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '팔 때 수수료율 (%)',
    fetched_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수신 일시',
    PRIMARY KEY (id),
    CONSTRAINT fk_exchange_market_data_currency FOREIGN KEY (currency_id) REFERENCES currencies (id),
    UNIQUE KEY uk_market_data_currency (currency_id)
) COMMENT '환전 시장 데이터(환율+수수료)';

-- 33. FCM 디바이스 토큰 관리
CREATE TABLE user_fcm_tokens
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'FCM 토큰 ID',
    user_id       BIGINT       NOT NULL COMMENT '회원 ID',
    device_token  VARCHAR(500) NOT NULL COMMENT 'FCM 디바이스 토큰',
    device_type   VARCHAR(20)  NOT NULL COMMENT '디바이스 유형(WEB/AOS/IOS)',
    last_used_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최근 사용 일시',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_fcm_tokens_token (device_token),
    INDEX idx_user_fcm_tokens_user (user_id),
    CONSTRAINT fk_user_fcm_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT 'FCM 디바이스 토큰';

-- 33. 월간 AI 소비 분석 리포트
CREATE TABLE monthly_spending_analyses
(
    id                       BIGINT         NOT NULL AUTO_INCREMENT COMMENT '월간 분석 ID',
    user_id                  BIGINT         NOT NULL COMMENT '회원 ID',
    analysis_year_month      CHAR(7)        NOT NULL COMMENT '분석 대상 연월(YYYY-MM, 지난달)',
    target_year_month        CHAR(7)        NOT NULL COMMENT '미션 적용 연월(YYYY-MM, 이번달)',
    total_spending           INT            NOT NULL DEFAULT 0 COMMENT '지난달 총지출(1일~말일, 원)',
    saving_target_amount     INT            NULL COMMENT '저축 목표 금액(원)',
    actual_saving_amount     INT            NULL COMMENT '실제 저축 금액(원)',
    saving_difference_amount INT            NULL COMMENT '초과·부족 금액(실제-목표, 원)',
    saving_result_message    VARCHAR(200)   NULL COMMENT '저축 결과 문구',
    report_status            VARCHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT '리포트 상태(PENDING/VIEWED/CLOSED)',
    report_viewed_at         DATETIME       NULL COMMENT '리포트 확인 시각',
    report_closed_at         DATETIME       NULL COMMENT '리포트 종료 시각',
    created_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_spending_analyses_user_month (user_id, analysis_year_month),
    CONSTRAINT fk_monthly_spending_analyses_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '월간 AI 소비 분석 리포트';


-- 34. 월간 카테고리별 소비·절감 추천 분석
CREATE TABLE monthly_category_analyses
(
    id                            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '카테고리별 분석 ID',
    monthly_spending_analysis_id  BIGINT         NOT NULL COMMENT '월간 분석 ID',
    category_id                   BIGINT         NOT NULL COMMENT '카테고리 ID',
    spending_amount               INT            NOT NULL DEFAULT 0 COMMENT '지난달 지출액(1일~말일, 소비순위 표시용, 원)',
    spending_ratio                DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '전체 지출 대비 비율(%)',
    transaction_count             INT            NOT NULL DEFAULT 0 COMMENT '거래 횟수(1일~말일)',
    weekly_average                INT            NULL COMMENT '주간 평균 지출(원)',
    daily_average                 INT            NULL COMMENT '일 평균 지출(원)',
    previous_month_change         DECIMAL(6, 2)  NULL COMMENT '전월 대비 증감률(%)',
    spending_rank                 INT            NULL COMMENT '소비 순위(1일~말일 기준, 기타 포함)',
    mission_period_spending       INT            NOT NULL DEFAULT 0 COMMENT '미션 기준 지출액(1~28일, 원)',
    mission_transaction_count     INT            NOT NULL DEFAULT 0 COMMENT '미션 기준 거래 횟수(1~28일)',
    spending_share_score       DECIMAL(6, 4)  NULL COMMENT '지출 비율 점수(0~1)',
    increase_score             DECIMAL(6, 4)  NULL COMMENT '최근 3개월 대비 증가 점수(0~1)',
    amount_rank_score          DECIMAL(6, 4)  NULL COMMENT '지출 순위 점수(0~1)',
    recommendation_score       DECIMAL(6, 4)  NULL COMMENT '최종 추천 점수(0~1)',
    recommendation_rank        INT            NULL COMMENT '절감 추천 순위(TOP 3)',
    recommendation_eligible    TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '추천 후보 필터 통과 여부',
    exclusion_reason           VARCHAR(50)    NULL COMMENT '추천 제외 사유',
    recommendation_reason      VARCHAR(500)   NULL COMMENT '추천 선정 근거',
    coaching_message           VARCHAR(500)   NULL COMMENT 'AI 코칭 문구',
    created_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_category_analyses_analysis_category (monthly_spending_analysis_id, category_id),
    CONSTRAINT fk_monthly_category_analyses_analysis FOREIGN KEY (monthly_spending_analysis_id) REFERENCES monthly_spending_analyses (id),
    CONSTRAINT fk_monthly_category_analyses_category FOREIGN KEY (category_id) REFERENCES spending_categories (id)
) COMMENT '월간 카테고리별 소비·절감 추천 분석';


-- 35. 카테고리별 절감률 선택
-- monthly_category_analyses와 분리한다. #210 리포트가 재계산될 때 monthly_category_analyses는
-- delete 후 재삽입되므로, 같은 테이블에 두면 재계산할 때마다 사용자의 선택이 사라진다.
-- 행이 존재하면 선택된 것으로 간주한다(별도 선택 여부 컬럼을 두지 않는다).
CREATE TABLE mission_category_selections
(
    id                            BIGINT    NOT NULL AUTO_INCREMENT COMMENT '미션 카테고리 선택 ID',
    monthly_spending_analysis_id  BIGINT    NOT NULL COMMENT '월간 분석 ID',
    category_id                   BIGINT    NOT NULL COMMENT '소비 카테고리 ID',
    reduction_rate                TINYINT   NOT NULL COMMENT '절감률(10/30/50, %)',
    baseline_spending_amount      INT       NOT NULL COMMENT '선택 당시 monthly_category_analyses.mission_period_spending 스냅샷(원)',
    monthly_reduction_target      INT       NOT NULL COMMENT '월 절감 목표 금액(원)',
    monthly_usage_target          INT       NOT NULL COMMENT '월 사용 목표 금액(원)',
    created_at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mission_category_selections_analysis_category (monthly_spending_analysis_id, category_id),
    CONSTRAINT fk_mission_category_selections_analysis
        FOREIGN KEY (monthly_spending_analysis_id) REFERENCES monthly_spending_analyses (id),
    CONSTRAINT fk_mission_category_selections_category
        FOREIGN KEY (category_id) REFERENCES spending_categories (id),
    CONSTRAINT chk_mission_category_selections_reduction_rate CHECK (reduction_rate IN (10, 30, 50))
) COMMENT '카테고리별 절감률 선택';


-- 36. 월간 절감 미션
CREATE TABLE monthly_saving_missions
(
    id                            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '월간 미션 ID',
    user_id                       BIGINT      NOT NULL COMMENT '회원 ID',
    trip_id                       BIGINT      NULL COMMENT '이 미션이 귀속되는 여행 ID',
    monthly_spending_analysis_id  BIGINT      NOT NULL COMMENT '월간 분석 ID',
    mission_category_selection_id BIGINT      NOT NULL COMMENT '카테고리 절감률 선택 ID',
    category_id                   BIGINT      NOT NULL COMMENT '소비 카테고리 ID',
    target_year_month             CHAR(7)     NOT NULL COMMENT '미션 적용 연월(YYYY-MM)',
    reduction_rate                TINYINT     NOT NULL COMMENT '절감률(10/30/50, %)',
    baseline_spending_amount      INT         NOT NULL COMMENT '선택 시점 기준 지출액(원)',
    monthly_reduction_target      INT         NOT NULL COMMENT '월 절감 목표 금액(원)',
    monthly_usage_target          INT         NOT NULL COMMENT '월 사용 목표 금액(원)',
    planned_saving_amount         INT         NOT NULL COMMENT '실제 생성된 주차의 예상 절약액 합계(원)',
    start_week                    TINYINT     NOT NULL COMMENT '미션 시작 주차(1~4)',
    status                        VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '상태(IN_PROGRESS/COMPLETED/CANCELLED)',
    created_at                    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at                    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_saving_missions_user_month_category (user_id, target_year_month, category_id),
    UNIQUE KEY uk_monthly_saving_missions_selection (mission_category_selection_id),
    KEY idx_monthly_saving_missions_trip (trip_id),
    CONSTRAINT fk_monthly_saving_missions_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_monthly_saving_missions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_monthly_saving_missions_analysis FOREIGN KEY (monthly_spending_analysis_id) REFERENCES monthly_spending_analyses (id),
    CONSTRAINT fk_monthly_saving_missions_selection FOREIGN KEY (mission_category_selection_id) REFERENCES mission_category_selections (id),
    CONSTRAINT fk_monthly_saving_missions_category FOREIGN KEY (category_id) REFERENCES spending_categories (id),
    CONSTRAINT chk_monthly_saving_missions_reduction_rate CHECK (reduction_rate IN (10, 30, 50)),
    CONSTRAINT chk_monthly_saving_missions_start_week CHECK (start_week BETWEEN 1 AND 4)
) COMMENT '월간 카테고리 절감 미션';


-- 37. 주간 절감 미션
CREATE TABLE weekly_saving_missions
(
    id                       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '주간 미션 ID',
    monthly_saving_mission_id BIGINT      NOT NULL COMMENT '월간 미션 ID',
    week_number               TINYINT     NOT NULL COMMENT '주차(1~4)',
    period_start_date         DATE        NOT NULL COMMENT '주차 시작일',
    period_end_date           DATE        NOT NULL COMMENT '주차 종료일',
    weekly_usage_limit        INT         NOT NULL COMMENT '주간 사용 한도(원)',
    weekly_expected_saving    INT         NOT NULL COMMENT '주간 예상 절약 금액(원)',
    actual_spending           INT         NULL COMMENT '주간 실제 지출액(원)',
    actual_saving             INT         NULL COMMENT '주간 실제 절약액(원)',
    reward_amount             INT         NOT NULL DEFAULT 0 COMMENT 'TRIP 월렛에 실제 적립한 미션 보상액(원)',
    wallet_ledger_id          BIGINT      NULL COMMENT '미션 보상 월렛 원장 ID',
    status                    VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '상태(PENDING/SUCCESS/FAILED)',
    evaluated_at              DATETIME    NULL COMMENT '판정 시각',
    rewarded_at               DATETIME    NULL COMMENT 'TRIP 월렛 보상 적립 시각',
    created_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_weekly_saving_missions_monthly_week (monthly_saving_mission_id, week_number),
    UNIQUE KEY uk_weekly_saving_missions_wallet_ledger (wallet_ledger_id),
    CONSTRAINT fk_weekly_saving_missions_monthly FOREIGN KEY (monthly_saving_mission_id) REFERENCES monthly_saving_missions (id),
    CONSTRAINT fk_weekly_saving_missions_wallet_ledger FOREIGN KEY (wallet_ledger_id) REFERENCES wallet_ledger (id),
    CONSTRAINT chk_weekly_saving_missions_week CHECK (week_number BETWEEN 1 AND 4)
) COMMENT '주간 카테고리 절감 미션';


-- ===== INDEXES =====
CREATE INDEX idx_transactions_account_id ON transactions (account_id);
CREATE INDEX idx_transactions_card_id ON transactions (card_id);
CREATE INDEX idx_transactions_trip_id ON transactions (trip_id);
CREATE INDEX idx_transactions_transaction_date ON transactions (transaction_date);
CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_trips_user_id ON trips (user_id);
CREATE INDEX idx_trip_countries_trip_id ON trip_countries (trip_id);
CREATE INDEX idx_trip_budget_recommendations_country ON trip_budget_recommendations (trip_country_id);
CREATE INDEX idx_country_budget_baselines_country ON country_budget_baselines (country_id);
CREATE INDEX idx_trip_schedules_trip_id ON trip_schedules (trip_id);
CREATE INDEX idx_trip_schedules_scheduled_at ON trip_schedules (scheduled_at);
CREATE INDEX idx_trip_schedules_trip_deleted_scheduled ON trip_schedules (trip_id, is_deleted, scheduled_at);
CREATE INDEX idx_trip_checklist_items_trip_id ON trip_checklist_items (trip_id);
CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_is_read ON notifications (user_id, is_read);
CREATE INDEX idx_exchange_rates_rate_date ON exchange_rates (rate_date);
CREATE INDEX idx_pre_expenses_trip_id ON pre_expenses (trip_id);
CREATE INDEX idx_saving_plans_trip_id ON saving_plans (trip_id);
CREATE INDEX idx_wallet_user_id ON wallet (user_id);
CREATE INDEX idx_wallet_account_wallet_id ON wallet_account (wallet_id);
CREATE INDEX idx_wallet_ledger_wallet_created ON wallet_ledger (wallet_id, created_at);
CREATE INDEX idx_wallet_auto_saving_next_date ON wallet_auto_saving_rule (enabled, next_transfer_date);
CREATE INDEX idx_user_travel_cards_user_id ON user_travel_cards (user_id, status, is_deleted);
CREATE INDEX idx_wallet_travel_card_wallet_id ON wallet_travel_card (wallet_id);
CREATE INDEX idx_travel_card_balance_card_id ON travel_card_balance (wallet_travel_card_id);
CREATE INDEX idx_travel_card_ledger_card_created ON travel_card_ledger (wallet_travel_card_id, created_at);
CREATE INDEX idx_wallet_card_topup_wallet_status ON wallet_card_topup (wallet_id, status);
CREATE INDEX idx_wallet_exchange_transaction_wallet_status ON wallet_exchange_transaction (wallet_id, status);
CREATE INDEX idx_monthly_spending_analyses_user_id ON monthly_spending_analyses (user_id);
CREATE INDEX idx_monthly_category_analyses_monthly_spending_analysis_id ON monthly_category_analyses (monthly_spending_analysis_id);
CREATE INDEX idx_monthly_saving_missions_user_month ON monthly_saving_missions (user_id, target_year_month);
CREATE INDEX idx_weekly_saving_missions_period ON weekly_saving_missions (period_start_date, period_end_date);
