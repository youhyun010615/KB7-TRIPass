-- #146: 카드 연동 기능 — develop 기준 DB(cards 테이블이 아직 없는 상태)에 이 브랜치의 스키마 변경을 전부 반영하는 마이그레이션.
-- 이미 로컬에서 이 브랜치 작업을 진행하며 cards 테이블을 만들어둔 경우 1)~2) 구간은 건너뛰고 3)부터 실행하세요.

-- 1) cards 테이블 신규 생성 (UNIQUE 제약 포함)
CREATE TABLE cards
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '카드 ID',
    user_id             BIGINT       NOT NULL                COMMENT '회원 ID',
    codef_connection_id BIGINT       NULL                    COMMENT 'CODEF 연동 ID',
    card_name           VARCHAR(150) NOT NULL                COMMENT '카드명',
    masked_card_number  VARCHAR(30)  NULL                    COMMENT '마스킹된 카드번호',
    card_type           VARCHAR(20)  NOT NULL DEFAULT 'CREDIT' COMMENT '카드 유형(CREDIT:신용/CHECK:체크)',
    organization_code   VARCHAR(20)  NULL                    COMMENT 'CODEF 기관코드',
    is_deleted          TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '삭제 여부',
    deleted_at          DATETIME     NULL                    COMMENT '삭제일시',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cards_user_masked_number (user_id, masked_card_number),
    CONSTRAINT fk_cards_user             FOREIGN KEY (user_id)             REFERENCES users (id),
    CONSTRAINT fk_cards_codef_connection FOREIGN KEY (codef_connection_id) REFERENCES codef_connections (id)
) COMMENT '연동 카드';

-- 2) transactions 테이블 카드 연동 구조 변경
--    account_id를 카드 전용 거래를 위해 NULL 허용으로 변경하고, card_id 컬럼/FK/인덱스를 추가한다.
--    기존 (account_id, external_key) 복합 UNIQUE를 external_key 단일 UNIQUE로 교체한다.
--    주의: 기존 복합 UNIQUE에서는 서로 다른 account_id가 같은 external_key를 가질 수 있었으므로,
--    아래 UNIQUE KEY 추가 전 반드시 중복 여부를 먼저 확인하고, 중복이 있으면 정리한 뒤 진행한다.
--
--    SELECT external_key, COUNT(*) AS duplicate_count
--    FROM transactions
--    GROUP BY external_key
--    HAVING COUNT(*) > 1;
--
--    중복이 있다면 낮은 id를 남기고 나머지를 정리한다 (필요 시 정책에 맞게 조정):
--
--    DELETE t1 FROM transactions t1
--    INNER JOIN transactions t2
--      ON t1.external_key = t2.external_key AND t1.id > t2.id;
ALTER TABLE transactions
    MODIFY COLUMN account_id BIGINT NULL COMMENT '계좌 ID(카드 전용 거래는 NULL)',
    ADD COLUMN card_id BIGINT NULL COMMENT '카드 ID(계좌 거래는 NULL)' AFTER account_id;

ALTER TABLE transactions
    DROP INDEX uk_transactions_account_key,
    ADD UNIQUE KEY uk_transactions_external_key (external_key);

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_card FOREIGN KEY (card_id) REFERENCES cards (id);

CREATE INDEX idx_transactions_card_id ON transactions (card_id);

-- 3) codef_connected_institutions 중복 기관 등록 방지용 UNIQUE 제약 추가
ALTER TABLE codef_connected_institutions
    ADD UNIQUE KEY uk_codef_connected_institutions_conn_org_type (codef_connection_id, organization_code, business_type);

-- 4) CODEF 가맹점 업종 저장용 컬럼 추가
ALTER TABLE transactions
    ADD COLUMN merchant_type VARCHAR(100) NULL COMMENT 'CODEF 가맹점 업종' AFTER merchant_name;
